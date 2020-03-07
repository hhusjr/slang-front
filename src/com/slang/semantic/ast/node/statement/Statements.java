package com.slang.semantic.ast.node.statement;

import com.slang.utils.Pair;

import java.util.ArrayList;

public class Statements implements Statement {
    ArrayList<Statement> statements = new ArrayList<>();

    public void addStatement(Statement statement) {
        this.statements.add(statement);
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        for (Statement statement : this.statements) {
            properties.add(new Pair<>(statement.getReadableOperator(), statement));
        }
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "Statements";
    }
}
