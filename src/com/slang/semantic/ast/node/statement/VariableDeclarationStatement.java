package com.slang.semantic.ast.node.statement;

import com.slang.semantic.ast.node.expression.Expression;
import com.slang.semantic.symbol.Symbol;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class VariableDeclarationStatement implements Statement {
    public ArrayList<Pair<Symbol, Expression>> declarations = new ArrayList<>();

    public void newDeclaration(Symbol symbol, Expression initializer) {
        this.declarations.add(new Pair<>(symbol, initializer));
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        for (Pair<Symbol, Expression> declaration : this.declarations) {
            properties.add(new Pair<>(String.format("%s %s", declaration.first.type.toString(), declaration.first.identifier), declaration.second == null ? "default" : declaration.second));
        }
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "DeclarationStatement";
    }
}
