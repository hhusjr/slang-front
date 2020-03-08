package com.slang.semantic.ast.node.statement;

import com.slang.semantic.ast.node.expression.Expression;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class WhileStatement implements LoopStatement {
    public Expression condition;
    public Statement body;

    public WhileStatement(Expression condition) {
        this.condition = condition;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("condition", this.condition));
        properties.add(new Pair<>("body", this.body));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "WhileStatement";
    }

    @Override
    public void setLoopBody(Statement body) {
        this.body = body;
    }
}
