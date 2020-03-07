package com.slang.semantic.ast.node.expression;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.type.Type;
import com.slang.semantic.type.TypeEvaluator;
import com.slang.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;

public class BinaryExpression implements Expression {
    public BinaryExpressionOperator operator;
    public Expression leftOperand;
    public Expression rightOperand;
    public Type type;
    public CodeAxis operatorCodeAxis;
    public static HashMap<BinaryExpressionOperator, String> supportedOperators = new HashMap<>() {{
        put(BinaryExpressionOperator.PLUS, "BinExpr +");
        put(BinaryExpressionOperator.SUB, "BinExpr -");
        put(BinaryExpressionOperator.PROD, "BinExpr *");
        put(BinaryExpressionOperator.DIV, "BinExpr /");
        put(BinaryExpressionOperator.AND, "BinExpr AND");
        put(BinaryExpressionOperator.OR, "BinExpr OR");
        put(BinaryExpressionOperator.XOR, "BinExpr XOR");
        put(BinaryExpressionOperator.LAND, "BinExpr LOGIC-AND");
        put(BinaryExpressionOperator.LOR, "BinExpr LOGIC-OR");
        put(BinaryExpressionOperator.SHL, "BinExpr SHIFT-LEFT");
        put(BinaryExpressionOperator.SHR, "BinExpr SHIFT-RIGHT");
        put(BinaryExpressionOperator.LT, "BinExpr <");
        put(BinaryExpressionOperator.GT, "BinExpr >");
        put(BinaryExpressionOperator.LTE, "BinExpr <=");
        put(BinaryExpressionOperator.GTE, "BinExpr >=");
        put(BinaryExpressionOperator.IS_EQ, "BinExpr ==");
        put(BinaryExpressionOperator.IS_NEQ, "BinExpr !=");
        put(BinaryExpressionOperator.MOD, "BinExpr MOD");
    }};

    public BinaryExpression(BinaryExpressionOperator operator, Expression leftOperand, Expression rightOperand, CodeAxis operatorCodeAxis) {
        this.leftOperand = leftOperand;
        this.operator = operator;
        this.rightOperand = rightOperand;
        this.type = TypeEvaluator.binaryExpressionType(operator, leftOperand, rightOperand, operatorCodeAxis);
        this.operatorCodeAxis = operatorCodeAxis;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("leftOperand", this.leftOperand));
        properties.add(new Pair<>("rightOperand", this.rightOperand));
        properties.add(new Pair<>("type", this.type));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return supportedOperators.get(this.operator);
    }
}
