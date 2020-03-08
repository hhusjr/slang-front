package com.slang.semantic.ast.node.statement;

import com.slang.semantic.type.Type;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class FunctionDeclarationStatement implements Statement {
    public String identifier;
    public Type type;
    public Type returnType;
    public Statements body;
    public ArrayList<String> paramIdentifiers;

    public FunctionDeclarationStatement(String identifier, Type type, Type returnType, Statements body, ArrayList<String> paramIdentifiers) {
        this.identifier = identifier;
        this.type = type;
        this.returnType = returnType;
        this.body = body;
        this.paramIdentifiers = paramIdentifiers;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("identifier", this.identifier));
        properties.add(new Pair<>("type", String.format("%s %s", this.returnType.toString(), this.type.toString())));
        properties.add(new Pair<>("body", this.body));
        properties.add(new Pair<>("paramIdentifiers", String.join(", ", this.paramIdentifiers)));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "FunctionDeclarationStatement";
    }
}
