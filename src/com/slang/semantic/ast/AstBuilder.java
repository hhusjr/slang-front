package com.slang.semantic.ast;

import com.slang.lexer.CodeAxis;
import com.slang.lexer.Token;
import com.slang.parser.ParseTreeNode;
import com.slang.parser.symbol.NonTerminal;
import com.slang.semantic.ast.node.Node;
import com.slang.semantic.ast.node.expression.*;
import com.slang.semantic.ast.node.statement.*;
import com.slang.semantic.symbol.Symbol;
import com.slang.semantic.symbol.SymbolTableManager;
import com.slang.semantic.type.BasicType;
import com.slang.semantic.type.CodeTypeMapping;
import com.slang.semantic.type.Type;
import com.slang.semantic.type.TypeFactory;
import com.slang.utils.Panic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * 根据语法树构造抽象语法树
 */
public class AstBuilder {
    private SymbolTableManager symbolTableManager = new SymbolTableManager();
    
    public Node invokeAstBuilderMethod(ParseTreeNode root) {
        Method method = null;
        NonTerminal symbol = (NonTerminal) root.getGrammarSymbol();
        String builderName = String.format("%s%s", root.getProductionName(), symbol.name);
        try {
            method = this.getClass().getDeclaredMethod(String.format("build%s", builderName), ParseTreeNode.class);
        } catch (NoSuchMethodException e) {
            Panic panic = new Panic(String.format("AST: Node building method build%s does not exist.", builderName), new CodeAxis());
            panic.show();
        }
        Node createdNode = null;
        try {
            assert method != null;
            createdNode = (Node) method.invoke(this, root);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            Panic panic = new Panic(String.format("AST: Can not invoke node building method build%s", builderName), new CodeAxis());
            panic.show();
        }
        return createdNode;
    }

    /*
     * 总构造器
     */
    public Node buildMainSLang(ParseTreeNode root) {
        return this.invokeAstBuilderMethod(root.getChildren().get(0));
    }

    private Node getBlockStatements(ParseTreeNode root) {
        ParseTreeNode currentRoot = root;
        Statements statements = new Statements();
        while (!currentRoot.isFinal()) {
            Node statementNode = this.invokeAstBuilderMethod(currentRoot.getChildren().get(0));
            if (statementNode != null) {
                assert statementNode instanceof Statement;
                statements.addStatement((Statement) statementNode);
            }
            currentRoot = currentRoot.getChildren().get(1);
        }
        return statements;
    }

    /*
     * 程序块构造器
     * 一个Program为一个程序块，一个程序块包含了一个Element，一个Element要么是一个函数定义块，要么是一条语句
     * Main <Program> ::== <Element> <Program>
     * Epsilon <Program> ::== $
     */
    public Node buildMainProgram(ParseTreeNode root) {
        return getBlockStatements(root);
    }

    public Node buildCommonElement(ParseTreeNode root) {
        return this.invokeAstBuilderMethod(root.getChildren().get(0));
    }

    /*
     * 语句构造器
     */
    public Node buildMainStatements(ParseTreeNode root) {
        return getBlockStatements(root);
    }
    public Node buildEpsilonStatements(ParseTreeNode root) {
        return new Statements();
    }
    // 复合语句
    public Node buildMainCompoundStatement(ParseTreeNode root) {
        return this.invokeAstBuilderMethod(root.getChildren().get(0));
    }
    public Node buildCompoundStatement(ParseTreeNode root) {
        return this.invokeAstBuilderMethod(root.getChildren().get(0));
    }
    // 表达式语句
    public Node buildExpressionStatement(ParseTreeNode root) {
        Node expressionNode = this.invokeAstBuilderMethod(root.getChildren().get(0));
        assert expressionNode instanceof Expression;
        return new ExpressionStatement((Expression) expressionNode);
    }
    // 变量声明语句
    public Node buildVariableDeclarationStatement(ParseTreeNode root) {
        String typeIdentifier = root.getChildren().get(0).getToken().value;
        if (!CodeTypeMapping.codeTypeMapping.containsKey(typeIdentifier)) {
            Panic panic = new Panic(String.format("Unsupported type %s", typeIdentifier), root.getChildren().get(1).getToken().codeAxis);
            panic.show();
        }
        BasicType type = CodeTypeMapping.codeTypeMapping.get(typeIdentifier);

        /*
         * VariableDeclaration <Statement> ::== VAR <VariableDeclaratorList> : [ID] ;
         * Main <VariableDeclaratorList> ::== <VariableDeclarator> <VariableDeclaratorListSuffix>
         * Main <VariableDeclaratorListSuffix> ::== , <VariableDeclarator> <VariableDeclaratorListSuffix>
         * 先遍历VariableDeclaratorList，得到当前的VariableDeclarator
         */
        ParseTreeNode currentDeclaratorList = root.getChildren().get(1);
        VariableDeclarationStatement variableDeclarationStatement = new VariableDeclarationStatement();
        while (!currentDeclaratorList.isFinal()) {
            ParseTreeNode currentDeclarator = currentDeclaratorList.getChildren().get(0);
            Token token = currentDeclarator.getChildren().get(0).getToken();
            String identifier = token.value;

            /*
             * Main <VariableDeclarator> ::== [ID] <VariableArraySizeDeclarator> <InitializerDeclarator>
             * Main    <VariableArraySizeDeclarator> ::== [ [NUMBER_LITERAL] ] <VariableArraySizeDeclarator>
             * Epsilon <VariableArraySizeDeclarator> ::== $
             * Main    <InitializerDeclarator> ::== EQ <Expression>
             * Epsilon <InitializerDeclarator> ::== $
             * 一个VariableDeclarator由ArraySizeDeclarator和InitializerDeclarator组成
             */

            // 解析ArraySizeDeclarator，包含数组的维度和维数情况，存储在dim中，null则代表维度未定义（根据初始化推导）
            ArrayList<Integer> dim = new ArrayList<>();
            ParseTreeNode currentArraySizeDeclarator = currentDeclarator.getChildren().get(1);
            boolean isArray = false;
            boolean hasDimNull = false;

            while (!currentArraySizeDeclarator.isFinal()) {
                isArray = true;
                ParseTreeNode dimNode = currentArraySizeDeclarator.getChildren().get(0);
                Integer currentDim = null;
                if (!dimNode.isFinal()) {
                    Token numToken = dimNode.getChildren().get(0).getToken();
                    currentDim = Integer.parseInt(numToken.value);
                    if (currentDim <= 0) {
                        Panic panic = new Panic("Array dimension definition error, number overflow", numToken.codeAxis);
                        panic.show();
                    }
                } else {
                    hasDimNull = true;
                }
                dim.add(currentDim);
                currentArraySizeDeclarator = currentArraySizeDeclarator.getChildren().get(1);
            }

            // 解析InitializerDeclarator
            ParseTreeNode initializerNode = currentDeclarator.getChildren().get(2);
            Expression initializerExpression = null;
            if (!initializerNode.isFinal()) {
                initializerExpression = (Expression) this.invokeAstBuilderMethod(initializerNode.getChildren().get(0));
            }

            Type complexType;
            if (!isArray) {
                complexType = TypeFactory.type(type);
            } else {
                complexType = TypeFactory.type(type, dim, false);
            }

            /*
             * 类型为Array的时候，存在这些问题：
             * 0、不存在右值的情况下，左值中存在null（直接报错）
             * 1、右边不是数组表达式（直接报错）
             * 2、左值的维度和右值的维度不一样（直接报错）
             * 3、维度一样的情况下，右值存在比左值大的维数（直接报错）
             * 4、维度一样的情况下，左值存在null维数（从右值推导）
             */
            if (isArray) {
                // 0、不存在右值的情况下，左值中存在null
                if (initializerExpression == null) {
                    if (hasDimNull) {
                        Panic panic = new Panic("Has null array dimension, but there are no initializer to refer", token.codeAxis);
                        panic.show();
                    }
                } else {
                    // 1、右边不是数组表达式
                    if (!(initializerExpression instanceof ArrayExpression)) {
                        Panic panic = new Panic("The initializer is not an array expression", token.codeAxis);
                        panic.show();
                    }
                    assert initializerExpression instanceof ArrayExpression;
                    ArrayExpression initializerArray = (ArrayExpression) initializerExpression;
                    // 2、左值的维度和右值的维度不一样
                    if (initializerArray.type.dim.size() != dim.size()) {
                        Panic panic = new Panic(String.format("Array dimension definition (%s) not match the initializer (%s)", dim.size(), initializerArray.type.dim.size()), token.codeAxis);
                        panic.show();
                    }
                    // 3、右值存在比左值大的维数  4、维度一样的情况下，左值存在null维数
                    for (int i = 0; i < dim.size(); i++) {
                        if (dim.get(i) != null) {
                            if (initializerArray.type.dim.get(i) > dim.get(i)) {
                                Panic panic = new Panic("Array dimension definition not match the initializer", token.codeAxis);
                                panic.show();
                            }
                        } else {
                            dim.set(i, initializerArray.type.dim.get(i));
                        }
                    }
                }
            }

            // 初始化表达式需要进行类型检查
            if (initializerExpression != null) {
                if (!isArray) {
                    if (!complexType.compatibleWith(initializerExpression.getType())) {
                        Panic panic = new Panic(String.format("Type mismatch in initializer, expected %s, got %s", complexType.toString(), initializerExpression.getType().toString()), token.codeAxis);
                        panic.show();
                    }
                } else {
                    ArrayExpression initializerArray = ((ArrayExpression) initializerExpression);
                    if (initializerArray.type.elementType != null) {
                        if (!complexType.compatibleWith(initializerExpression.getType())) {
                            Panic panic = new Panic(String.format("Type mismatch in initializer, expected %s, got %s", complexType.toString(), initializerExpression.getType().toString()), token.codeAxis);
                            panic.show();
                        }
                    }
                }
            }

            // 保存到符号表
            Symbol symbol = new Symbol(identifier, complexType);
            this.symbolTableManager.addSymbol(identifier, symbol);

            // 初始化语句加入
            variableDeclarationStatement.newDeclaration(symbol, initializerExpression);

            currentDeclaratorList = currentDeclaratorList.getChildren().get(1);
        }

        return variableDeclarationStatement;
    }
    // IF语句
    public Node buildIfStatement(ParseTreeNode root) {
        Expression condition = (Expression) this.invokeAstBuilderMethod(root.getChildren().get(1));

        this.symbolTableManager.enterScope();
        Statement trueBlock = (Statement) this.invokeAstBuilderMethod(root.getChildren().get(2));
        this.symbolTableManager.leaveScope();

        this.symbolTableManager.enterScope();
        Statement falseBlock = null;
        if (!root.getChildren().get(3).isFinal()) {
            falseBlock = (Statement) this.invokeAstBuilderMethod(root.getChildren().get(3).getChildren().get(0));
        }
        this.symbolTableManager.leaveScope();

        return new IfStatement(condition, trueBlock, falseBlock, root.getChildren().get(0).getToken().codeAxis);
    }

    /*
     * 表达式
     */
    public Node buildMainExpression(ParseTreeNode root) {
        return this.invokeAstBuilderMethod(root.getChildren().get(0));
    }

    /*
     * 主要表达式(PrimaryExpression)相关的构造器
     */
    public Node buildNumberLiteralPrimaryExpression(ParseTreeNode root) {
        Token token = root.getChildren().get(0).getToken();
        return new Constant(ConstantOperator.NUMBER_LITERAL, token.value, token.codeAxis);
    }
    public Node buildCharLiteralPrimaryExpression(ParseTreeNode root) {
        Token token = root.getChildren().get(0).getToken();
        return new Constant(ConstantOperator.CHAR_LITERAL, token.value, token.codeAxis);
    }
    public Node buildStringLiteralPrimaryExpression(ParseTreeNode root) {
        Token token = root.getChildren().get(0).getToken();
        return new Constant(ConstantOperator.STRING_LITERAL, token.value, token.codeAxis);
    }
    public Node buildArrayPrimaryExpression(ParseTreeNode root) {
        ParseTreeNode current = root.getChildren().get(1);
        ArrayList<Expression> expressions = new ArrayList<>();
        while (!current.isFinal()) {
            expressions.add((Expression) this.invokeAstBuilderMethod(current.getChildren().get(0)));
            current = current.getChildren().get(1);
        }
        return new ArrayExpression(expressions, root.getChildren().get(0).getToken().codeAxis);
    }
    public Node buildTrueLiteralPrimaryExpression(ParseTreeNode root) {
        Token token = root.getChildren().get(0).getToken();
        return new Constant(ConstantOperator.TRUE, "true", token.codeAxis);
    }
    public Node buildFalseLiteralPrimaryExpression(ParseTreeNode root) {
        Token token = root.getChildren().get(0).getToken();
        return new Constant(ConstantOperator.FALSE, "false", token.codeAxis);
    }
    public Node buildBracketsPrimaryExpression(ParseTreeNode root) {
        return this.invokeAstBuilderMethod(root.getChildren().get(0));
    }
    public Node buildIdentifierPrimaryExpression(ParseTreeNode root) {
        Token identifierToken = root.getChildren().get(0).getToken();
        root.getChildren().get(1).setAttribute("identifierToken", identifierToken);
        return this.invokeAstBuilderMethod(root.getChildren().get(1));
    }

    /*
     * 成员表达式(MemberExpression)
     *
     */
    public Node buildEpsilonMemberExpressionSuffix(ParseTreeNode root) {
        Token identifierToken = (Token) root.getAttribute("identifierToken");
        Symbol symbol = this.symbolTableManager.findSymbol(identifierToken.value, identifierToken.codeAxis);
        return new Identifier(symbol);
    }
    public Node buildArrayMemberExpressionSuffix(ParseTreeNode root) {
        Token identifierToken = (Token) root.getAttribute("identifierToken");
        Symbol symbol = this.symbolTableManager.findSymbol(identifierToken.value, identifierToken.codeAxis);
        ParseTreeNode current = root;
        ArrayList<Expression> expressions = new ArrayList<>();
        while (!current.isFinal()) {
            Expression expression = (Expression) this.invokeAstBuilderMethod(current.getChildren().get(0));
            if (!expression.getType().is(BasicType.INT)) {
                Panic panic = new Panic("Array index must be integers", identifierToken.codeAxis);
                panic.show();
            }
            expressions.add(expression);
            current = current.getChildren().get(1);
        }
        if (expressions.size() != symbol.type.dim.size()) {
            Panic panic = new Panic("Array dimension not match", identifierToken.codeAxis);
            panic.show();
        }
        return new Identifier(symbol, expressions);
    }

    /*
     * 一元表达式(UnaryExpression)
     */
    public Node buildPrimaryUnaryExpression(ParseTreeNode root) {
        return this.invokeAstBuilderMethod(root.getChildren().get(0));
    }
    public Node buildNegativeUnaryExpression(ParseTreeNode root) {
        Token token = root.getChildren().get(0).getToken();
        Expression operand = (Expression) this.invokeAstBuilderMethod(root.getChildren().get(1));
        return new UnaryExpression(UnaryExpressionOperator.NEGATIVE, operand, token.codeAxis);
    }
    public Node buildNotUnaryExpression(ParseTreeNode root) {
        Token token = root.getChildren().get(0).getToken();
        Expression operand = (Expression) this.invokeAstBuilderMethod(root.getChildren().get(1));
        return new UnaryExpression(UnaryExpressionOperator.NOT, operand, token.codeAxis);
    }


    /**
     * 二元运算的主要表达式（如Main <Expression> ::== <Term> <Plus>）的处理
     * @param root 当前处理的根节点
     * @param operator 二元运算符号
     * @return 返回AST
     */
    private Node handleBinaryExpression(ParseTreeNode root, BinaryExpressionOperator operator) {
        ArrayList<ParseTreeNode> children = root.getChildren();
        Node combination;
        ParseTreeNode rightOperand;
        if (root.hasAttribute("leftOperandInLeft")) {
            Expression rightOperandInLeft = (Expression) this.invokeAstBuilderMethod(children.get(1));
            rightOperand = children.get(2);
            assert root.getAttribute("leftOperandInLeft") instanceof Node;
            Expression leftOperandInLeft = (Expression) root.getAttribute("leftOperandInLeft");
            combination = new BinaryExpression(operator, leftOperandInLeft, rightOperandInLeft, children.get(0).getToken().codeAxis);
        } else {
            Expression leftOperand = (Expression) this.invokeAstBuilderMethod(children.get(0));
            rightOperand = children.get(1);
            combination = leftOperand;
        }
        rightOperand.setAttribute("leftOperandInLeft", combination);
        return rightOperand.isFinal() ? combination : this.invokeAstBuilderMethod(rightOperand);
    }

    /*
     * 二元运算表达式相关的构造器
     */
    // * / %
    public Node buildMainMultiplyingExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildProductMultiplyingExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.PROD);
    }
    public Node buildDivisionMultiplyingExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.DIV);
    }
    public Node buildModMultiplyingExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.MOD);
    }
    // + -
    public Node buildMainLinearExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildPlusLinearExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.PLUS);
    }
    public Node buildSubLinearExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.SUB);
    }
    // << >>
    public Node buildMainShiftExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildShiftLeftShiftExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.SHL);
    }
    public Node buildShiftRightShiftExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.SHR);
    }
    // < > <= >=
    public Node buildMainCompareExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildLessThanCompareExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.LT);
    }
    public Node buildGreaterThanCompareExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.GT);
    }
    public Node buildLessThanOrEqualCompareExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.LTE);
    }
    public Node buildGreaterThanOrEqualCompareExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.GTE);
    }
    // == !=
    public Node buildMainEqualityExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildIsEqualEqualityExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.IS_EQ);
    }
    public Node buildIsNotEqualEqualityExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.IS_NEQ);
    }
    // &
    public Node buildMainBitwiseAndExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildAndBitwiseAndExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.AND);
    }
    // ^
    public Node buildMainBitwiseXorExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildXorBitwiseXorExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.XOR);
    }
    // |
    public Node buildMainBitwiseOrExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildOrBitwiseOrExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.OR);
    }
    // &&
    public Node buildMainBitwiseLogicalAndExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildLogicalAndBitwiseLogicalAndExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.LAND);
    }
    // ||
    public Node buildMainBitwiseLogicalOrExpression(ParseTreeNode root) {
        return this.handleBinaryExpression(root, null);
    }
    public Node buildLogicalOrBitwiseLogicalOrExpressionSuffix(ParseTreeNode root) {
        return this.handleBinaryExpression(root, BinaryExpressionOperator.LOR);
    }

    /*
     * 赋值表达式（注意：右结合性）
     */
    public Node buildMainAssignExpression(ParseTreeNode root) {
        Node leftHandSide = this.invokeAstBuilderMethod(root.getChildren().get(0));
        ParseTreeNode rightHandSide = root.getChildren().get(1);
        if (rightHandSide.isFinal()) {
            return leftHandSide;
        }
        CodeAxis operatorCodeAxis = root.getChildren().get(1).getChildren().get(0).getToken().codeAxis;
        if (!(leftHandSide instanceof Identifier)) {
            Panic panic = new Panic("The left hand side of assign expression must be an identifier.", operatorCodeAxis);
            panic.show();
        }
        assert leftHandSide instanceof Identifier;
        return new AssignExpression((Identifier) leftHandSide, (Expression) this.invokeAstBuilderMethod(rightHandSide), operatorCodeAxis);
    }
    public Node buildAssignAssignExpressionSuffix(ParseTreeNode root) {
        Node leftHandSide = this.invokeAstBuilderMethod(root.getChildren().get(1));
        ParseTreeNode rightHandSide = root.getChildren().get(2);
        if (rightHandSide.isFinal()) {
            return leftHandSide;
        }
        CodeAxis operatorCodeAxis = root.getChildren().get(2).getChildren().get(0).getToken().codeAxis;
        if (!(leftHandSide instanceof Identifier)) {
            Panic panic = new Panic("The left hand side of assign expression must be an identifier.", operatorCodeAxis);
            panic.show();
        }
        assert leftHandSide instanceof Identifier;
        return new AssignExpression((Identifier) leftHandSide, (Expression) this.invokeAstBuilderMethod(rightHandSide), operatorCodeAxis);
    }
}
