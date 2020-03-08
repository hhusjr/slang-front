package com.slang.semantic.ast.node.statement;

import com.slang.semantic.ast.node.expression.Expression;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class ForStatement implements LoopStatement {
    public Expression begin;
    public Expression condition;
    public Expression delta;
    public Statement body;

    public ForStatement(Expression begin, Expression condition, Expression delta) {
        this.begin = begin;
        this.condition = condition;
        this.delta = delta;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("begin", this.begin));
        properties.add(new Pair<>("condition", this.condition));
        properties.add(new Pair<>("delta", this.delta));
        properties.add(new Pair<>("body", this.body));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "ForStatement";
    }

    @Override
    public void setLoopBody(Statement body) {
        this.body = body;
    }
}
