package com.slang.semantic.ast.node.expression;

import com.slang.semantic.ast.node.Node;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class ArrayMember implements Node {
    public ArrayList<Expression> arrayMemberExpression;

    public ArrayMember(ArrayList<Expression> arrayMemberExpression) {
        this.arrayMemberExpression = arrayMemberExpression;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        int dim = 1;
        for (Expression expression : arrayMemberExpression) {
            properties.add(new Pair<>(String.format("Dimension %s", dim), expression));
            dim++;
        }
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "ArrayMember";
    }
}
