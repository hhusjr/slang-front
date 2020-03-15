package com.slang.codegen;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.ast.node.Node;
import com.slang.semantic.ast.node.expression.*;
import com.slang.semantic.ast.node.statement.*;
import com.slang.semantic.symbol.Symbol;
import com.slang.semantic.type.BasicType;
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

public class CodeGen {
    private Statements ast;
    private HashMap<String, Pair<ArrayList<String>, Statement>> functions = new HashMap<>();
    private int addr = 0;
    private ArrayList<Instruction> instructions = new ArrayList<>();
    private HashMap<Pair<Integer, String>, Pair<Integer, Integer>> constants = new HashMap<>();
    private HashMap<String, Integer> globalVariablesNames = new HashMap<>();
    private Stack<UpdateFlowControlRequest> updateFlowControlRequests = new Stack<>();
    private Stack<LoopStatement> loopStatements = new Stack<>();
    private int maxVarAddr = 0;

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

    private Instruction getInstruction(int pos) {
        return this.instructions.get(pos);
    }

    private int getCurrentInstructionPos() {
        return this.instructions.size() - 1;
    }

    private void scanFunctions() {
        for (Statement statement : this.ast.statements) {
            if (statement instanceof FunctionDeclarationStatement) {
                FunctionDeclarationStatement functionDeclarationStatement = (FunctionDeclarationStatement) statement;
                Pair<ArrayList<String>, Statement> paramsAndBody = new Pair<>(functionDeclarationStatement.paramIdentifiers, functionDeclarationStatement.body);
                this.functions.put(functionDeclarationStatement.getFunctionName(), paramsAndBody);
            }
        }
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

    private void genArrayExpression(ArrayExpression arrayExpression, ArrayList<Integer> dim) {
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
        int from = this.newInstruction(InstructionCode.LOAD_NAME_GLOBAL, this.globalVariablesNames.get(identifier.symbol.getName()));
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
        }
        return null;
    }

    private Pair<Integer, Integer> genAssignExpression(AssignExpression assignExpression, boolean noPop) {
        int from, to;
        String name = assignExpression.leftHandSide.symbol.getName();
        if (assignExpression.rightHandSide instanceof ArrayExpression) {
            from = this.newInstruction(InstructionCode.LOAD_NAME_GLOBAL, this.globalVariablesNames.get(name));
            this.genArrayExpression((ArrayExpression) assignExpression.rightHandSide, assignExpression.leftHandSide.symbol.type.dim);
            to = this.newInstruction(InstructionCode.POP_OP);
            return new Pair<>(from, to);
        }
        if (assignExpression.leftHandSide.arrayMember == null) {
            from = Objects.requireNonNull(this.genExpression(assignExpression.rightHandSide)).first;
            if (!noPop) {
                to = this.newInstruction(InstructionCode.STORE_NAME_GLOBAL, this.globalVariablesNames.get(name));
            } else {
                to = this.newInstruction(InstructionCode.STORE_NAME_GLOBAL_NOPOP, this.globalVariablesNames.get(name));
            }
        } else {
            from = this.newInstruction(InstructionCode.LOAD_NAME_GLOBAL, this.globalVariablesNames.get(name));
            this.genArrayMember(assignExpression.leftHandSide.symbol.type.dim, assignExpression.leftHandSide.arrayMember);
            this.genExpression(assignExpression.rightHandSide);
            if (!noPop) {
                to = this.newInstruction(InstructionCode.STORE_SUBSCR);
            } else {
                to = this.newInstruction(InstructionCode.STORE_SUBSCR_NOPOP);
            }
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
        this.genExpression(forStatement.delta);
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
                    newPos = jmpCond;
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
            if (declaration.second != null) {
                if (!declaration.first.type.is(BasicType.ARRAY)) {
                    int addr = Objects.requireNonNull(this.genExpression(declaration.second)).first;
                    if (from == -1) {
                        from = addr;
                    }
                }
                useDefault = false;
            }
            switch (declaration.first.type.basicType) {
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
                    int arrayElementType = -1;
                    switch (declaration.first.type.elementType.basicType) {
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
                    int addr = this.newInstruction(InstructionCode.LOAD_INT, this.getArraySize(declaration.first.type.dim));
                    if (from == -1) {
                        from = addr;
                    }
                    this.newInstruction(InstructionCode.BUILD_ARR, arrayElementType);
                    if (!useDefault) {
                        this.genArrayExpression((ArrayExpression) declaration.second, declaration.first.type.dim);
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
            int addr = this.maxVarAddr++;
            this.globalVariablesNames.put(name, addr);
            to = this.newInstruction(InstructionCode.STORE_NAME_GLOBAL, addr);
        }
        return new Pair<>(from, to);
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
        }
        return null;
    }

    private Pair<Integer, Integer> genStatements(Statements statements) {
        if (statements.statements.size() < 1) {
            int addr = this.newInstruction(InstructionCode.NOOP);
            return new Pair<>(addr, addr);
        }
        int from = -1, to = -1;
        for (Statement statement : statements.statements) {
            Pair<Integer, Integer> addr = this.genStatement(statement);
            assert addr != null;
            if (from == -1) {
                from = addr.first;
            }
            to = addr.second;
        }
        return new Pair<>(from, to);
    }
}
