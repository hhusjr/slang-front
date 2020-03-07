package com.slang.semantic.type;

import java.util.ArrayList;
import java.util.HashMap;

public class Type {
    public BasicType basicType;
    // 数组
    public Type elementType;
    public ArrayList<Integer> dim;
    public boolean isArrayExpression;
    public boolean ignoreDimNumber;
    // 函数形式参数类型列表
    public ArrayList<Type> formalParameterTypes;

    Type(BasicType basicType) {
        this.basicType = basicType;
    }

    Type(Type elementType, ArrayList<Integer> dim, boolean isArrayExpression) {
        assert !elementType.is(BasicType.ARRAY) && !elementType.is(BasicType.FUNCTION);
        assert isArrayExpression || !elementType.is(BasicType.VOID);
        this.basicType = BasicType.ARRAY;
        this.elementType = elementType;
        this.dim = dim;
        this.isArrayExpression = isArrayExpression;
    }

    Type(ArrayList<Type> formalParameterTypes) {
        this.formalParameterTypes = formalParameterTypes;
    }

    public boolean is(BasicType with) {
        return this.basicType == with;
    }

    public boolean compatibleWith(Type another) {
        switch (this.basicType) {
            case INT:
            case FLOAT:
            case CHAR:
            case BOOLEAN:
                return TypeEvaluator.isBasicTypeCompatible(this.basicType, another.basicType);
            case ARRAY: {
                assert this.dim != null;
                assert this.elementType != null;
                if (this.isArrayExpression) {
                    return false;
                }
                // 非数组常量之间类型兼容性，需要保证元素类型、维度严格相等
                if (!another.isArrayExpression) {
                    return this.elementType.compatibleWith(another.elementType) && this.dim.equals(another.dim);
                }
                // 数组常量
                if (another.elementType != null && !another.elementType.compatibleWith(this.elementType)) {
                    return false;
                }
                if (this.dim.size() != another.dim.size()) {
                    return false;
                }
                if (!ignoreDimNumber) {
                    for (int i = 0; i < this.dim.size(); i++) {
                        if (another.dim.get(i) > this.dim.get(i)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        HashMap<BasicType, String> readableType = new HashMap<>() {{
            put(BasicType.INT, "int");
            put(BasicType.CHAR, "char");
            put(BasicType.BOOLEAN, "bool");
            put(BasicType.FLOAT, "float");
            put(BasicType.VOID, "void");
        }};
        switch (this.basicType) {
            case ARRAY: {
                assert this.dim != null;
                assert this.elementType != null;
                StringBuilder dimStr = new StringBuilder();
                if (!this.ignoreDimNumber) {
                    for (Integer d : this.dim) {
                        dimStr.append(String.format("[%s]", d));
                    }
                } else {
                    dimStr.append(String.format("{%sd}", this.dim.size()));
                }
                String typeStr = String.format("%s%s", readableType.get(this.elementType.basicType), dimStr);
                if (this.isArrayExpression) {
                    return "(ArrayExpression) ".concat(typeStr);
                }
                return typeStr;
            }
            case FUNCTION:
                assert this.formalParameterTypes != null;
                ArrayList<String> paramsStr = new ArrayList<>();
                for (Type t : this.formalParameterTypes) {
                    paramsStr.add(t.toString());
                }
                return String.format("(%s)", String.join(", ", paramsStr));
            default:
                return readableType.get(this.basicType);
        }
    }
}
