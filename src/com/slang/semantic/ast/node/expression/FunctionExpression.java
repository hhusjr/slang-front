package com.slang.semantic.ast.node.expression;

import com.slang.semantic.symbol.Symbol;
import com.slang.semantic.type.Type;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class FunctionExpression implements Expression {
    public Symbol symbol;
    public Type overloadTypes;
    public ArrayList<Expression> expressions;
    public Type returnType;

    public FunctionExpression(Symbol symbol, Type overloadTypes, Type returnType, ArrayList<Expression> expressions) {
        this.symbol = symbol;
        this.overloadTypes = overloadTypes;
        this.returnType = returnType;
        this.expressions = expressions;
    }

    public String getFunctionName() {
        return this.symbol.getName(this.overloadTypes);
    }

    @Override
    public Type getType() {
        return returnType;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("identifier", this.symbol.identifier));
        properties.add(new Pair<>("name", this.symbol.getName(this.overloadTypes)));
        for (int i = 0; i < this.expressions.size(); i++) {
            properties.add(new Pair<>(String.format("paramExpression #%s", i), this.expressions.get(i)));
        }
        properties.add(new Pair<>("type", String.format("%s %s", this.returnType.toString(), this.overloadTypes.toString())));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "FunctionExpression";
    }
}
