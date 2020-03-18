package com.slang.codegen;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.ast.node.Node;
import com.slang.semantic.ast.node.expression.*;
import com.slang.semantic.ast.node.statement.*;
import com.slang.semantic.symbol.Symbol;
import com.slang.semantic.type.BasicType;
import com.slang.semantic.type.Type;
import com.slang.utils.Pair;
import com.slang.utils.Panic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;

class UpdateFlowControlRequest {
    LoopStatement loopStatement;
    int instructionPos;
    FlowControlType flowControlType;

    public UpdateFlowControlRequest(LoopStatement loopStatement, int instructionPos, FlowControlType flowControlType) {
        this.loopStatement = loopStatement;
        this.instructionPos = instructionPos;
        this.flowControlType = flowControlType;
    }
}

class FunctionAttribute {
    String name;
    public ArrayList<Symbol> paramSymbols;
    public Statement body;
    public int headAddr = -1;

    public FunctionAttribute(String name, ArrayList<Symbol> paramSymbols, Statement body) {
        this.name = name;
        this.paramSymbols = paramSymbols;
        this.body = body;
    }

    public void setHeadAddr(int headAddr) {
        this.headAddr = headAddr;
    }
}

public class CodeGen {
    private Statements ast;
    private HashMap<String, FunctionAttribute> functions = new HashMap<>();
    private Stack<Pair<String, Integer>> updateCallAddrRequestsStack = new Stack<>();
    private int addr = 0;
    private ArrayList<Instruction> instructions = new ArrayList<>();
    private HashMap<Pair<Integer, String>, Pair<Integer, Integer>> constants = new HashMap<>();
    private HashMap<String, Integer> globalVariablesNames = new HashMap<>();
    private HashMap<String, Integer> localVariablesNames = new HashMap<>();
    private Stack<UpdateFlowControlRequest> updateFlowControlRequests = new Stack<>();
    private Stack<LoopStatement> loopStatements = new Stack<>();
    private boolean inGlobalContext = true;

    private int getConstantAddr(int type, String val) {
        Pair<Integer, String> constant = new Pair<>(type, val);
        if (this.constants.containsKey(constant)) {
            Pair<Integer, Integer> o = this.constants.get(constant);
            o.second++;
            return o.first;
        }
        int addr = this.constants.size();
        this.constants.put(constant, new Pair<>(addr, 1));
        return addr;
    }

    public CodeGen(Node ast) {
        // 根节点一定是Statements
        this.ast = (Statements) ast;
    }

    public void generate() {
        // 分配全局变量空间 0
        this.newInstruction(InstructionCode.VMALLOC, this.globalVariablesNames.size());
        this.genStatements(this.ast);
        this.newInstruction(InstructionCode.HALT);
        // 生成所有函数
        this.genFunctions();
    }

    public void save(String outputFilePath) {
        File file = new File(outputFilePath);
        StringBuilder codeContent = new StringBuilder();
        String conjunction = "\n";
        this.getInstruction(0).setParams(this.globalVariablesNames.size());
        // add instructions
        for (Instruction instruction : this.instructions) {
            codeContent.append(instruction.dump()).append(conjunction);
        }
        // 分配常量空间
        codeContent.append(String.format("0 CMALLOC %s", this.constants.size())).append(conjunction);
        // 保存所有常量
        // type val
        for (Pair<Integer, String> key : this.constants.keySet()) {
            // addr ref_cnt
            Pair<Integer, Integer> val = this.constants.get(key);
            codeContent.append(String.format("%s CONSTANT %s %s %s", val.first, key.first, key.second, val.second)).append(conjunction);
        }
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(codeContent.toString());
            fileWriter.close();
        } catch (IOException e) {
            Panic panic = new Panic("Can not generate opcode due to IO Error", new CodeAxis(outputFilePath));
            panic.show();
        }
    }

    private int nextAddrVal(int addr) {
        return addr + 2;
    }

    private int nextAddr() {
        int addr = this.addr;
        this.addr = this.nextAddrVal(this.addr);
        return addr;
    }

    private int newInstruction(InstructionCode code, int... params) {
        int addr = this.nextAddr();
        this.instructions.add(new Instruction(addr, code, params));
        return addr;
    }

    private int newInstruction(String code, int... params) {
        int addr = this.nextAddr();
        this.instructions.add(new Instruction(addr, code, params));
        return addr;
    }

    private Instruction getInstruction(int pos) {
        return this.instructions.get(pos);
    }

    private int getCurrentInstructionPos() {
        return this.instructions.size() - 1;
    }

    private int getArraySize(ArrayList<Integer> dim) {
        int a = 1;
        for (Integer d : dim) {
            a *= d;
        }
        return a;
    }

    private ArrayList<Pair<Integer, Expression>> expandArrayExpression(ArrayExpression arrayExpression, ArrayList<Integer> dim, int offset) {
        ArrayList<Pair<Integer, Expression>> res = new ArrayList<>();
        int dimFactor = 1;
        for (int i = offset; i < dim.size(); i++) {
            dimFactor *= dim.get(i);
        }
        for (int i = 0; i < arrayExpression.elements.size(); i++) {
            Expression expression = arrayExpression.elements.get(i);
            if (expression instanceof ArrayExpression) {
                ArrayList<Pair<Integer, Expression>> subArrayExpression = this.expandArrayExpression((ArrayExpression) expression, dim, offset + 1);
                for (Pair<Integer, Expression> subExpression : subArrayExpression) {
                    res.add(new Pair<>(subExpression.first + dimFactor * i, subExpression.second));
                }
                continue;
            }
            res.add(new Pair<>(i, expression));
        }
        return res;
    }

    private void genTypeCvt(Type sourceType, Type targetType) {
        if (sourceType.equals(targetType)) {
            return;
        }
        switch (targetType.basicType) {
            case INT:
                if (!sourceType.is(BasicType.INT)) {
                    this.newInstruction(InstructionCode.TYPE_CVT, 0);
                }
            case FLOAT:
                if (!sourceType.is(BasicType.FLOAT)) {
                    this.newInstruction(InstructionCode.TYPE_CVT, 1);
                }
        }
    }

    private void genArrayExpression(Type targetType, ArrayExpression arrayExpression) {
        ArrayList<Integer> dim = targetType.dim;
        ArrayList<Pair<Integer, Expression>> expanded = this.expandArrayExpression(arrayExpression, dim, 1);
        if (expanded.size() < 1) {
            int pos = this.newInstruction(InstructionCode.NOOP);
            new Pair<>(pos, pos);
            return;
        }
        int from = -1, to = -1, pos;
        for (Pair<Integer, Expression> positionExpressionPair : expanded) {
            pos = this.newInstruction(InstructionCode.LOAD_INT, positionExpressionPair.first);
            if (from == -1) from = pos;
            this.genExpression(positionExpressionPair.second);
            this.genTypeCvt(arrayExpression.type.elementType, targetType);
            pos = this.newInstruction(InstructionCode.STORE_SUBSCR_INPLACE);
            to = pos;
        }
        new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genConstant(Constant constant) {
        Integer addr = null;
        switch (constant.op) {
            case TRUE:
                addr = this.newInstruction(InstructionCode.LOAD_INT, 1);
                break;
            case FALSE:
                addr = this.newInstruction(InstructionCode.LOAD_INT, 0);
                break;
            case NUMBER_LITERAL:
                if (constant.type.is(BasicType.FLOAT)) {
                    addr = this.newInstruction(InstructionCode.LOAD_CONSTANT, this.getConstantAddr(1, constant.value));
                } else if (constant.type.is(BasicType.INT)) {
                    addr = this.newInstruction(InstructionCode.LOAD_CONSTANT, this.getConstantAddr(0, constant.value));
                }
                break;
            case CHAR_LITERAL:
                addr = this.newInstruction(InstructionCode.LOAD_CONSTANT, this.getConstantAddr(2, String.valueOf((int) constant.value.charAt(0))));
                break;
        }
        if (addr != null) {
            return new Pair<>(addr, addr);
        }
        return null;
    }

    private Pair<Integer, Integer> genBinaryExpression(BinaryExpression expression) {
        int op = -1;
        switch (expression.operator) {
            case SHL:
                op = 7;
                break;
            case SHR:
                op = 8;
                break;
            case LAND:
            case AND:
                op = 5;
                break;
            case LOR:
            case OR:
                op = 6;
                break;
            case XOR:
                op = 9;
                break;
            case LT:
                op = 10;
                break;
            case LTE:
                op = 11;
                break;
            case GT:
                op = 12;
                break;
            case GTE:
                op = 13;
                break;
            case DIV:
                op = 4;
                break;
            case MOD:
                op = 3;
                break;
            case SUB:
                op = 1;
                break;
            case PLUS:
                op = 0;
                break;
            case PROD:
                op = 2;
                break;
            case IS_EQ:
                op = 14;
                break;
            case IS_NEQ:
                op = 15;
                break;
            default: {
                Panic panic = new Panic(String.format("Slang virtual machine does not support binary-operator %s", expression.operator), new CodeAxis());
                panic.show();
            }
        }
        Pair<Integer, Integer> leftAddr = this.genExpression(expression.leftOperand);
        this.genExpression(expression.rightOperand);
        int to = this.newInstruction(InstructionCode.BINARY_OP, op);
        assert leftAddr != null;
        return new Pair<>(leftAddr.first, to);
    }

    private Pair<Integer, Integer> genUnaryExpression(UnaryExpression expression) {
        int op = -1;
        switch (expression.operator) {
            case NEGATIVE:
                op = 1;
                break;
            case NOT:
                op = 0;
                break;
            default: {
                Panic panic = new Panic(String.format("Slang virtual machine does not support unary-operator %s", expression.operator), new CodeAxis());
                panic.show();
            }
        }
        Pair<Integer, Integer> addr = this.genExpression(expression.operand);
        int to = this.newInstruction(InstructionCode.UNARY_OP, op);
        assert addr != null;
        return new Pair<>(addr.first, to);
    }

    private void genArrayMember(ArrayList<Integer> dim, ArrayMember arrayMember) {
        int dimFactor = 1;
        for (Integer d : dim) {
            dimFactor *= d;
        }
        int index = 0;
        for (Expression expression : arrayMember.arrayMemberExpression) {
            dimFactor /= dim.get(index);
            this.genExpression(expression);
            if (index != 0) {
                this.newInstruction(InstructionCode.LOAD_INT, dimFactor);
                this.newInstruction(InstructionCode.BINARY_OP, 2);
                this.newInstruction(InstructionCode.BINARY_OP, 0);
            }
            index++;
        }
    }

    private Pair<Integer, Integer> genIdentifier(Identifier identifier) {
        InstructionCode ins;
        String name = identifier.symbol.getName();
        Integer source;
        if (this.inGlobalContext || this.globalVariablesNames.containsKey(name)) {
            ins = InstructionCode.LOAD_NAME_GLOBAL;
            source = this.globalVariablesNames.get(name);
        } else {
            ins = InstructionCode.LOAD_NAME;
            source = this.localVariablesNames.get(name);
        }
        int from = this.newInstruction(ins, source);
        int to = from;
        // 数组成员访问，需要计算下标
        if (identifier.arrayMember != null) {
            this.genArrayMember(identifier.symbol.type.dim, identifier.arrayMember);
            to = this.newInstruction(InstructionCode.BINARY_SUBSCR);
        }
        return new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genExpression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            return this.genBinaryExpression((BinaryExpression) expression);
        } else if (expression instanceof Constant) {
            return this.genConstant((Constant) expression);
        } else if (expression instanceof UnaryExpression) {
            return this.genUnaryExpression((UnaryExpression) expression);
        } else if (expression instanceof Identifier) {
            return this.genIdentifier((Identifier) expression);
        } else if (expression instanceof AssignExpression) {
            return this.genAssignExpression((AssignExpression) expression, true);
        } else if (expression instanceof FunctionExpression) {
            return this.genFunctionExpression((FunctionExpression) expression, false);
        }
        return null;
    }

    private Pair<Integer, Integer> genFunctionExpression(FunctionExpression expression, boolean directlyFromExpressionStatement) {
        /*
         * 函数调用方式是：
         * 将各个参数存储在全局操作数栈
         * PUSH （开辟栈帧）
         * CALL（JMP到函数的首指令地址，并传递返回值地址）
         */
        // 计算FunctionExpression，并转存在全局操作数栈
        int from = -1;
        ArrayList<Type> formalParameterTypes = expression.overloadTypes.formalParameterTypes;
        assert formalParameterTypes.size() == expression.expressions.size();
        for (int i = 0; i < formalParameterTypes.size(); i++) {
            Expression param = expression.expressions.get(i);
            /*
             * 需要单独讨论ArrayExpression的情况
             */
            int target;
            if (param instanceof ArrayExpression) {
                ArrayExpression arrayExpression = (ArrayExpression) param;
                target = this.newInstruction(InstructionCode.LOAD_INT, this.getArraySize(arrayExpression.type.dim));
                this.newInstruction(InstructionCode.BUILD_ARR, this.getArrayElementType(arrayExpression.type.elementType.basicType));
                this.genArrayExpression(arrayExpression.type, arrayExpression);
            } else {
                target = Objects.requireNonNull(this.genExpression(param)).first;
                this.genTypeCvt(param.getType(), formalParameterTypes.get(i));
            }
            if (from == -1) {
                from = target;
            }
            this.newInstruction(InstructionCode.STORE_GLOBAL);
        }
        // 开辟栈帧
        this.newInstruction(InstructionCode.PUSH);
        // CALL
        int to = this.newInstruction(InstructionCode.CALL);
        String name = expression.getFunctionName();
        int currentInstructionPos = this.getCurrentInstructionPos();
        int approximatelyAddr = this.functions.get(name).headAddr;
        if (this.functions.containsKey(name) && approximatelyAddr != -1) {
            this.getInstruction(currentInstructionPos).setParams(approximatelyAddr);
        } else {
            // 如果该函数尚未加载，将该函数放入请求栈中。
            this.updateCallAddrRequestsStack.push(new Pair<>(name, currentInstructionPos));
        }
        // CALL之后就取得了返回值，如果当作Statement调用，返回值无效
        if (directlyFromExpressionStatement || expression.returnType.is(BasicType.VOID)) {
            to = this.newInstruction(InstructionCode.POP_OP);
        }
        return new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genAssignExpression(AssignExpression assignExpression, boolean noPop) {
        int from, to;
        String name = assignExpression.leftHandSide.symbol.getName();
        InstructionCode loadNameIns;
        Integer source;
        InstructionCode storeNameIns;
        InstructionCode storeNameNopopIns;
        if (this.inGlobalContext || this.globalVariablesNames.containsKey(name)) {
            loadNameIns = InstructionCode.LOAD_NAME_GLOBAL;
            storeNameIns = InstructionCode.STORE_NAME_GLOBAL;
            storeNameNopopIns = InstructionCode.STORE_NAME_GLOBAL_NOPOP;
            source = this.globalVariablesNames.get(name);
        } else {
            loadNameIns = InstructionCode.LOAD_NAME;
            storeNameIns = InstructionCode.STORE_NAME;
            storeNameNopopIns = InstructionCode.STORE_NAME_NOPOP;
            source = this.localVariablesNames.get(name);
        }
        Type type = assignExpression.leftHandSide.symbol.type;
        if (assignExpression.rightHandSide instanceof ArrayExpression) {
            from = this.newInstruction(loadNameIns, source);
            this.genArrayExpression(type, (ArrayExpression) assignExpression.rightHandSide);
            to = this.newInstruction(InstructionCode.POP_OP);
            return new Pair<>(from, to);
        }
        if (assignExpression.leftHandSide.arrayMember == null) {
            from = Objects.requireNonNull(this.genExpression(assignExpression.rightHandSide)).first;
            this.genTypeCvt(assignExpression.rightHandSide.getType(), assignExpression.leftHandSide.getType());
            if (!noPop) {
                to = this.newInstruction(storeNameIns, source);
            } else {
                to = this.newInstruction(storeNameNopopIns, source);
            }
        } else {
            // 存在数组成员访问
            from = this.newInstruction(loadNameIns, source);
            this.genArrayMember(type.dim, assignExpression.leftHandSide.arrayMember);
            this.genExpression(assignExpression.rightHandSide);
            this.genTypeCvt(assignExpression.rightHandSide.getType(), type.elementType);
            if (!noPop) {
                to = this.newInstruction(InstructionCode.STORE_SUBSCR);
            } else {
                to = this.newInstruction(InstructionCode.STORE_SUBSCR_NOPOP);
            }
        }
        return new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genWhileStatement(WhileStatement whileStatement) {
        int from, to;
        // 循环条件判断
        int jmpCond = from = Objects.requireNonNull(this.genExpression(whileStatement.condition)).first;
        this.newInstruction(InstructionCode.JMP_FALSE);
        int jmpToLoopEnd = this.getCurrentInstructionPos();
        // 循环体
        this.loopStatements.add(whileStatement);
        this.genStatement(whileStatement.body);
        this.loopStatements.pop();
        int beforeEndAddr = to = this.newInstruction(InstructionCode.JMP, jmpCond);
        int endAddr = this.nextAddrVal(beforeEndAddr);
        this.getInstruction(jmpToLoopEnd).setParams(endAddr);
        while (!updateFlowControlRequests.empty() && updateFlowControlRequests.peek().loopStatement == whileStatement) {
            UpdateFlowControlRequest updateFlowControlRequest = updateFlowControlRequests.pop();
            int newPos = -1;
            switch (updateFlowControlRequest.flowControlType) {
                case BREAK:
                    newPos = endAddr;
                    break;
                case CONTINUE:
                    newPos = jmpCond;
                    break;
            }
            this.getInstruction(updateFlowControlRequest.instructionPos).setParams(newPos);
        }
        return new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genForStatement(ForStatement forStatement) {
        int from, to;
        // 循环起始赋值
        from = Objects.requireNonNull(this.genExpression(forStatement.begin)).first;
        this.newInstruction(InstructionCode.POP_OP); // 第一个语句的值不要用，需要直接POP掉
        // 循环条件判断
        int jmpCond = Objects.requireNonNull(this.genExpression(forStatement.condition)).first;
        this.newInstruction(InstructionCode.JMP_FALSE);
        int jmpToLoopEnd = this.getCurrentInstructionPos();
        // 循环体
        this.loopStatements.add(forStatement);
        this.genStatement(forStatement.body);
        this.loopStatements.pop();
        // 增量更新，注意，需要弹出栈顶元素
        int changeStart = Objects.requireNonNull(this.genExpression(forStatement.delta)).first;
        this.newInstruction(InstructionCode.POP_OP);
        int beforeEndAddr = to = this.newInstruction(InstructionCode.JMP, jmpCond);
        int endAddr = this.nextAddrVal(beforeEndAddr);
        this.getInstruction(jmpToLoopEnd).setParams(endAddr);
        while (!updateFlowControlRequests.empty() && updateFlowControlRequests.peek().loopStatement == forStatement) {
            UpdateFlowControlRequest updateFlowControlRequest = updateFlowControlRequests.pop();
            int newPos = -1;
            switch (updateFlowControlRequest.flowControlType) {
                case BREAK:
                    newPos = endAddr;
                    break;
                case CONTINUE:
                    newPos = changeStart;
                    break;
            }
            this.getInstruction(updateFlowControlRequest.instructionPos).setParams(newPos);
        }
        return new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genVariableDeclarationStatement(VariableDeclarationStatement variableDeclarationStatement) {
        int from = -1, to = -1;
        for (Pair<Symbol, Expression> declaration : variableDeclarationStatement.declarations) {
            boolean useDefault = true;
            // 非数组类型都可以直接进行表达式计算，ArrayExpression类型不可（需要等价转换为赋值语句）
            Type leftHandSideType = declaration.first.type;
            if (declaration.second != null) {
                if (!leftHandSideType.is(BasicType.ARRAY)) {
                    int addr = Objects.requireNonNull(this.genExpression(declaration.second)).first;
                    this.genTypeCvt(declaration.second.getType(), leftHandSideType);
                    if (from == -1) {
                        from = addr;
                    }
                }
                useDefault = false;
            }
            switch (leftHandSideType.basicType) {
                case INT:
                case BOOLEAN:
                case CHAR:
                    if (useDefault) {
                        int addr = this.newInstruction(InstructionCode.LOAD_INT, 0);
                        if (from == -1) {
                            from = addr;
                        }
                    }
                    break;
                case FLOAT:
                    if (useDefault) {
                        int addr = this.newInstruction(InstructionCode.LOAD_FLOAT, 0);
                        if (from == -1) {
                            from = addr;
                        }
                    }
                    break;
                case ARRAY: {
                    int arrayElementType = getArrayElementType(leftHandSideType.elementType.basicType);
                    int addr = this.newInstruction(InstructionCode.LOAD_INT, this.getArraySize(leftHandSideType.dim));
                    if (from == -1) {
                        from = addr;
                    }
                    this.newInstruction(InstructionCode.BUILD_ARR, arrayElementType);
                    if (!useDefault) {
                        this.genArrayExpression(leftHandSideType, (ArrayExpression) declaration.second);
                    }
                    break;
                }
                case VOID:
                    if (useDefault) {
                        this.newInstruction(InstructionCode.LOAD_NULL, 0);
                    }
                    break;
            }
            String name = declaration.first.getName();
            HashMap<String, Integer> source = this.inGlobalContext ? this.globalVariablesNames : this.localVariablesNames;
            int addr = source.size();
            source.put(name, addr);
            to = this.newInstruction(this.inGlobalContext ? InstructionCode.STORE_NAME_GLOBAL
                    : InstructionCode.STORE_NAME, addr);
        }
        return new Pair<>(from, to);
    }

    private int getArrayElementType(BasicType basicType) {
        int arrayElementType = -1;
        switch (basicType) {
            case BOOLEAN:
            case INT:
                arrayElementType = 0;
                break;
            case FLOAT:
                arrayElementType = 1;
                break;
            case CHAR:
                arrayElementType = 2;
                break;
        }
        return arrayElementType;
    }

    private Pair<Integer, Integer> genIfStatement(IfStatement ifStatement) {
        Pair<Integer, Integer> conditionAddr = this.genExpression(ifStatement.condition);
        // 预留JMP_TRUE的位置，如果条件成立，直接跳到下面的IF部分
        this.newInstruction(InstructionCode.JMP_TRUE);
        int jmpTruePos = this.getCurrentInstructionPos();
        // 先构造Else部分
        this.genStatement(ifStatement.falseBlock);
        // 此处需要直接跳转到If部分之后，先预留位置
        this.newInstruction(InstructionCode.JMP);
        int jmp = this.getCurrentInstructionPos();
        // 再构造If部分
        Pair<Integer, Integer> ifAddr = this.genStatement(ifStatement.trueBlock);
        assert ifAddr != null;
        // 刚才预留的位置填充好If部分的起始指令地址
        this.getInstruction(jmpTruePos).setParams(ifAddr.first);
        this.getInstruction(jmp).setParams(this.nextAddrVal(ifAddr.second));
        assert conditionAddr != null;
        return new Pair<>(conditionAddr.first, ifAddr.second);
    }

    private Pair<Integer, Integer> genPrintkStatement(PrintkStatement statement) {
        int from = Objects.requireNonNull(this.genExpression(statement.expression)).first;
        int to = this.newInstruction(InstructionCode.PRINTK);
        return new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genExpressionStatement(ExpressionStatement expressionStatement) {
        // 要单独考虑赋值语句的情况，因为赋值语句右边需要弹栈，其他情况不需要
        if (expressionStatement.expression instanceof AssignExpression) {
            return this.genAssignExpression((AssignExpression) expressionStatement.expression, false);
        }
        // 还要单独考虑函数调用语句的情况
        if (expressionStatement.expression instanceof FunctionExpression) {
            return this.genFunctionExpression((FunctionExpression) expressionStatement.expression, true);
        }
        return this.genExpression(expressionStatement.expression);
    }

    private Pair<Integer, Integer> genFlowControlStatement(FlowControlStatement statement) {
        int addr = this.newInstruction(InstructionCode.JMP);
        this.updateFlowControlRequests.add(new UpdateFlowControlRequest(this.loopStatements.peek(), this.getCurrentInstructionPos(), statement.type));
        return new Pair<>(addr, addr);
    }

    private Pair<Integer, Integer> genStatement(Statement statement) {
        if (statement == null) {
            int addr = this.newInstruction(InstructionCode.NOOP);
            return new Pair<>(addr, addr);
        }
        if (statement instanceof Statements) {
            return this.genStatements((Statements) statement);
        } else if (statement instanceof ExpressionStatement) {
            return this.genExpressionStatement((ExpressionStatement) statement);
        } else if (statement instanceof IfStatement) {
            return this.genIfStatement((IfStatement) statement);
        } else if (statement instanceof VariableDeclarationStatement) {
            return this.genVariableDeclarationStatement((VariableDeclarationStatement) statement);
        } else if (statement instanceof ForStatement) {
            return this.genForStatement((ForStatement) statement);
        } else if (statement instanceof PrintkStatement) {
            return this.genPrintkStatement((PrintkStatement) statement);
        } else if (statement instanceof FlowControlStatement) {
            return this.genFlowControlStatement((FlowControlStatement) statement);
        } else if (statement instanceof FunctionDeclarationStatement) {
            FunctionDeclarationStatement functionDeclarationStatement = (FunctionDeclarationStatement) statement;
            // 函数需要延迟生成，先加入函数集合中
            String functionName = functionDeclarationStatement.getFunctionName();
            FunctionAttribute functionAttribute = new FunctionAttribute(
                    functionName,
                    functionDeclarationStatement.paramIdentifiers,
                    functionDeclarationStatement.body
            );
            this.functions.put(functionName, functionAttribute);
            return null;
        } else if (statement instanceof OpcodeStatement) {
            return this.genOpcodeStatement((OpcodeStatement) statement);
        } else if (statement instanceof ReturnStatement) {
            return this.genReturnStatement((ReturnStatement) statement);
        } else if (statement instanceof WhileStatement) {
            return this.genWhileStatement((WhileStatement) statement);
        }
        return null;
    }

    private Pair<Integer, Integer> genOpcodeStatement(OpcodeStatement opcodeStatement) {
        int pos;
        if (opcodeStatement.param != null) {
            pos = this.newInstruction(opcodeStatement.opcode, opcodeStatement.param);
        } else if (opcodeStatement.symbol != null) {
            String name = opcodeStatement.symbol.getName();
            int source;
            if (this.inGlobalContext || this.globalVariablesNames.containsKey(name)) {
                source = this.globalVariablesNames.get(name);
            } else {
                source = this.localVariablesNames.get(name);
            }
            pos = this.newInstruction(opcodeStatement.opcode, source);
        } else {
            pos = this.newInstruction(opcodeStatement.opcode);
        }
        return new Pair<>(pos, pos);
    }

    private void genFunctions() {
        this.inGlobalContext = false;
        /*
         * 生成函数部分较为复杂
         * 首先，遍历所有函数String, Pair<ArrayList<Symbol>, Statement>> => name, ([symbol, ...], statement)
         * 函数调用方式是：
         * LOAD_GLOBAL 将全局操作数栈的数据压入栈帧的操作数栈
         * ...
         * LOAD_GLOBAL 多次（直到所有参数全部进栈）
         * VMALLOC （为变量分配内存空间，此处为声明变量个数）
         * STORE_NAME ...
         * STORE_NAME ...
         * （保存变量）
         * 本方法的任务不是生成函数调用的部分，而是生成函数指令部分
         */
        while (!this.updateCallAddrRequestsStack.empty()) {
            Pair<String, Integer> request = this.updateCallAddrRequestsStack.pop();
            FunctionAttribute functionAttribute = this.functions.get(request.first);
            if (functionAttribute.headAddr == -1) {
                int addr = -1;
                for (int i = 0; i < functionAttribute.paramSymbols.size(); i++) {
                    int cur = this.newInstruction(InstructionCode.LOAD_GLOBAL);
                    if (addr == -1) {
                        addr = cur;
                    }
                }
                int cur = this.newInstruction(InstructionCode.VMALLOC);
                int vMallocPos = this.getCurrentInstructionPos();
                if (addr == -1) {
                    addr = cur;
                }
                this.localVariablesNames.clear();
                for (Symbol symbol : functionAttribute.paramSymbols) {
                    int size = this.localVariablesNames.size();
                    this.localVariablesNames.put(symbol.getName(), size);
                    this.newInstruction(InstructionCode.STORE_NAME, size);
                }
                this.genStatement(functionAttribute.body);
                functionAttribute.setHeadAddr(addr);
                this.newInstruction(InstructionCode.LOAD_NULL);
                this.newInstruction(InstructionCode.RET);
                this.getInstruction(vMallocPos).setParams(this.localVariablesNames.size());
                this.getInstruction(request.second).setParams(addr);
            }

        }
    }

    private Pair<Integer, Integer> genReturnStatement(ReturnStatement returnStatement) {
        int from = Objects.requireNonNull(this.genExpression(returnStatement.expression)).first;
        int to = this.newInstruction(InstructionCode.RET);
        return new Pair<>(from, to);
    }

    private Pair<Integer, Integer> genStatements(Statements statements) {
        if (statements.statements.size() < 1) {
            int addr = this.newInstruction(InstructionCode.NOOP);
            return new Pair<>(addr, addr);
        }
        int from = -1, to = -1;
        for (Statement statement : statements.statements) {
            Pair<Integer, Integer> addr = this.genStatement(statement);
            if (addr == null) {
                continue;
            }
            if (from == -1) {
                from = addr.first;
            }
            to = addr.second;
        }
        return new Pair<>(from, to);
    }
}
