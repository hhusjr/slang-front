package com.slang.semantic.ast.node.expression;

import com.slang.semantic.symbol.Symbol;
import com.slang.semantic.type.Type;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class FunctionExpression implements Expression {
    Symbol symbol;
    Type overloadTypes;
    Type returnType;

    public FunctionExpression(Symbol symbol, Type overloadTypes, Type returnType) {
        this.symbol = symbol;
        this.overloadTypes = overloadTypes;
        this.returnType = returnType;
    }

    @Override
    public Type getType() {
        return returnType;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("identifier", this.symbol.identifier));
        properties.add(new Pair<>("type", String.format("%s %s", this.returnType.toString(), this.overloadTypes.toString())));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "FunctionExpression";
    }
}
