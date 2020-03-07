package com.slang.semantic.ast.node.expression;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.type.Type;
import com.slang.semantic.type.TypeEvaluator;
import com.slang.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;

public class UnaryExpression implements Expression {
    public UnaryExpressionOperator operator;
    public Expression operand;
    public CodeAxis operatorCodeAxis;
    public Type type;

    public static HashMap<UnaryExpressionOperator, String> supportedOperators = new HashMap<>() {{
        put(UnaryExpressionOperator.NOT, "UnaryExpr NOT");
        put(UnaryExpressionOperator.NEGATIVE, "UnaryExpr NEGATIVE");
    }};

    public UnaryExpression(UnaryExpressionOperator operator, Expression operand, CodeAxis operatorCodeAxis) {
        this.operator = operator;
        this.operand = operand;
        this.operatorCodeAxis = operatorCodeAxis;
        this.type = TypeEvaluator.unaryExpressionType(operator, operand, operatorCodeAxis);
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("operand", this.operand));
        properties.add(new Pair<>("type", this.type));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return supportedOperators.get(this.operator);
    }

    @Override
    public Type getType() {
        return this.type;
    }
}
