package com.slang.semantic.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Type {
    public BasicType basicType;
    // 数组
    public Type elementType;
    public ArrayList<Integer> dim;
    public boolean isArrayExpression;
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
        this.basicType = BasicType.FUNCTION;
        this.formalParameterTypes = formalParameterTypes;
    }

    public boolean is(BasicType with) {
        return this.basicType == with;
    }

    // 忽略第一个维度之后的维度数组
    public ArrayList<Integer> getDimBody() {
        ArrayList<Integer> dimBody = new ArrayList<>();
        for (int i = 1; i < this.dim.size(); i++) {
            dimBody.add(this.dim.get(i));
        }
        return dimBody;
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
                // 对方必须为数组
                if (another.basicType != BasicType.ARRAY) {
                    return false;
                }
                if (this.isArrayExpression) {
                    return false;
                }
                // 非数组常量之间类型兼容性，需要保证元素类型、维度严格相等（除去忽略维度的情况）
                if (!another.isArrayExpression) {
                    if (!this.elementType.compatibleWith(another.elementType)) return false;
                    if (this.dim.size() != another.dim.size()) return false;
                    return this.getDimBody().equals(another.getDimBody());
                }
                // 数组常量
                if (another.elementType != null && !another.elementType.compatibleWith(this.elementType)) {
                    return false;
                }
                if (this.dim.size() != another.dim.size()) {
                    return false;
                }
                // 需要忽略第一个维度进行比较
                for (int i = 1; i < this.dim.size(); i++) {
                    if (another.dim.get(i) > this.dim.get(i)) {
                        return false;
                    }
                }
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        if (type.basicType != this.basicType) return false;
        switch (this.basicType) {
            case ARRAY:
                if (!type.elementType.equals(this.elementType)) return false;
                if (type.dim.size() != this.dim.size()) return false;
                if (!this.getDimBody().equals(type.getDimBody())) return false;
                break;
            case FUNCTION:
                if (!this.formalParameterTypes.equals(type.formalParameterTypes)) return false;
                break;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(basicType, elementType, formalParameterTypes);
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
                for (Integer d : this.dim) {
                    dimStr.append(String.format("[%s]", d));
                }
                String typeStr;
                if (this.elementType != null) {
                    typeStr = String.format("%s%s", readableType.get(this.elementType.basicType), dimStr);
                } else {
                    typeStr = String.format("?%s", dimStr);
                }
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
