package com.slang.semantic.ast.node.expression;

import com.slang.semantic.symbol.Symbol;
import com.slang.semantic.type.Type;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class Identifier implements Expression {
    public Symbol symbol;
    public ArrayMember arrayMember = null;

    public Identifier(Symbol symbol) {
        this.symbol = symbol;
    }

    public Identifier(Symbol symbol, ArrayList<Expression> arrayMember) {
        this.symbol = symbol;
        this.arrayMember = new ArrayMember(arrayMember);
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("identifier", this.symbol.identifier));
        properties.add(new Pair<>("name", this.symbol.getName()));
        properties.add(new Pair<>("type", this.getType()));
        if (this.arrayMember != null) {
            properties.add(new Pair<>("arrayMember", this.arrayMember));
        }
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "Identifier";
    }

    @Override
    public Type getType() {
        if (this.arrayMember == null) {
            return this.symbol.type;
        }
        return this.symbol.type.elementType;
    }
}
