package com.slang.semantic.ast.node.expression;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.type.Type;
import com.slang.semantic.type.TypeFactory;
import com.slang.utils.Pair;
import com.slang.utils.Panic;

import java.util.ArrayList;

public class ArrayExpression implements Expression {
    public Type type;
    public ArrayList<Expression> elements;

    public ArrayExpression(ArrayList<Expression> elements, CodeAxis codeAxis) {
        int nonArrayElementCount = 0;
        Type elementType = null;
        ArrayList<Integer> dim = new ArrayList<>();
        // 首先检查数组内所有元素的类型是否一致（不关注维度的问题，但是会顺便更新非数组元素的个数）
        for (Expression initializerElement : elements) {
            Type compareType;
            // 此处千万注意，type为Array的不一定是ArrayExpression
            if (!(initializerElement instanceof ArrayExpression)) {
                nonArrayElementCount++;
                compareType = initializerElement.getType();
            } else {
                compareType = ((ArrayExpression) initializerElement).type.elementType;
            }
            if (elementType == null) {
                elementType = compareType;
            } else if (elementType != compareType && compareType != null) {
                // 类型不一致的情况报错
                Panic panic = new Panic(String.format("Each element in the array must be of the same type, unexpected %s (expected %s)",
                        compareType.toString(), elementType.toString()), codeAxis);
                panic.show();
            }
        }
        // 要么全是数组元素，要么全是非数组元素
        if (nonArrayElementCount != 0 && nonArrayElementCount != elements.size()) {
            Panic panic = new Panic("Wrong array initializer, Non-Array element occurred in the array sequence", codeAxis);
            panic.show();
        }
        dim.add(elements.size());
        if (nonArrayElementCount == 0) {
            // 需要注意，如果数组非空（数组内套数组），则剩余各个维度的大小为子数组在这个维度的大小的最大值
            if (elements.size() > 0) {
                int dimSize = ((ArrayExpression) elements.get(0)).type.dim.size();
                for (Expression element : elements) {
                    if (((ArrayExpression) element).type.dim.size() != dimSize) {
                        Panic panic = new Panic("Wrong array dimension in array expression", codeAxis);
                        panic.show();
                    }
                }
                /*
                 * 自动推导数组维度
                 * 比如，{{{1,2,3},{1,2},{1,2,3,4}},{{}, {1,2}, {1,2,3,4,5}, {}}}
                 * 第一个维度的维数是2
                 * 第二个维度的维数是第一个元素的的第一个维度的最大值与第二个元素的第一个维度的最大值取最大，为4（递归过程）
                 * 第三个维度的维数是...（类似），为5
                 */
                for (int i = 0; i < dimSize; i++) {
                    int maxDim = 0;
                    for (Expression element : elements) {
                        maxDim = Math.max(maxDim, ((ArrayExpression) element).type.dim.get(i));
                    }
                    dim.add(maxDim);
                }
            }
        }
        this.elements = elements;
        this.type = TypeFactory.type(elementType, dim, true);
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        for (int i = 0; i < this.elements.size(); i++) {
            properties.add(new Pair<>(String.format("[element #%s]", i), this.elements.get(i)));
        }
        properties.add(new Pair<>("type", this.type));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "ArrayExpression";
    }
}
