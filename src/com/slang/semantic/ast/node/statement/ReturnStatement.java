package com.slang.semantic.ast.node.statement;

import com.slang.semantic.ast.node.expression.Expression;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class ReturnStatement implements Statement {
    public Expression expression = null;

    public ReturnStatement() {
    }

    public ReturnStatement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("expression", expression));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "ReturnStatement";
    }
}
