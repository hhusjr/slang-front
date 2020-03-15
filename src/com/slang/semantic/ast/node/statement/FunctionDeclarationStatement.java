package com.slang.semantic.ast.node.statement;

import com.slang.semantic.symbol.Symbol;
import com.slang.semantic.type.Type;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class FunctionDeclarationStatement implements Statement {
    public String identifier;
    public Type types;
    public Type returnType;
    public Statement body;
    public ArrayList<String> paramIdentifiers;
    public Symbol symbol;

    public FunctionDeclarationStatement(String identifier, Symbol symbol, Type types, Type returnType, Statement body, ArrayList<String> paramIdentifiers) {
        this.identifier = identifier;
        this.types = types;
        this.returnType = returnType;
        this.body = body;
        this.paramIdentifiers = paramIdentifiers;
        this.symbol = symbol;
    }

    public String getFunctionName() {
        return this.symbol.getName(this.types);
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("identifier", this.identifier));
        properties.add(new Pair<>("type", String.format("%s %s", this.returnType.toString(), this.types.toString())));
        properties.add(new Pair<>("body", this.body));
        properties.add(new Pair<>("paramIdentifiers", String.join(", ", this.paramIdentifiers)));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "FunctionDeclarationStatement";
    }
}
