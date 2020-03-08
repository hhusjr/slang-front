package com.slang.semantic.type;

import java.util.ArrayList;
import java.util.HashMap;

public class TypeFactory {
    private static HashMap<BasicType, Type> mapping = new HashMap<>() {{
        put(BasicType.BOOLEAN, new Type(BasicType.BOOLEAN));
        put(BasicType.INT, new Type(BasicType.INT));
        put(BasicType.CHAR, new Type(BasicType.CHAR));
        put(BasicType.FLOAT, new Type(BasicType.FLOAT));
        put(BasicType.VOID, new Type(BasicType.VOID));
    }};

    public static Type type(BasicType basicType) {
        return TypeFactory.mapping.get(basicType);
    }

    public static Type type(Type elementType, ArrayList<Integer> dim, boolean isArrayExpression) {
        return new Type(elementType, dim, isArrayExpression);
    }

    public static Type type(BasicType elementType, ArrayList<Integer> dim, boolean isArrayExpression) {
        return new Type(TypeFactory.type(elementType), dim, isArrayExpression);
    }

    public static Type type(ArrayList<Type> formalParameterTypes) {
        return new Type(formalParameterTypes);
    }
}
