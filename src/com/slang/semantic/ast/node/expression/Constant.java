package com.slang.semantic.ast.node.expression;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.type.BasicType;
import com.slang.semantic.type.Type;
import com.slang.semantic.type.TypeFactory;
import com.slang.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;

public class Constant implements Expression {
    public ConstantOperator op;
    public String value;
    public CodeAxis codeAxis;
    public Type type;
    public static HashMap<ConstantOperator, String> supportedOperators = new HashMap<>() {{
        put(ConstantOperator.NUMBER_LITERAL, "Number literal");
        put(ConstantOperator.CHAR_LITERAL, "Char literal");
        put(ConstantOperator.TRUE, "Boolean literal True");
        put(ConstantOperator.FALSE, "Boolean literal False");
    }};

    public Constant(ConstantOperator op, String value, CodeAxis codeAxis) {
        this.op = op;
        this.value = value;
        this.codeAxis = codeAxis;
        switch (this.op) {
            case NUMBER_LITERAL:
                if (this.value.indexOf('.') == -1) this.type = TypeFactory.type(BasicType.INT);
                else this.type = TypeFactory.type(BasicType.FLOAT);
                break;
            case CHAR_LITERAL:
                this.type = TypeFactory.type(BasicType.CHAR);
                break;
            case TRUE:
            case FALSE:
                this.type = TypeFactory.type(BasicType.BOOLEAN);
                break;
        }
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("value", this.value));
        properties.add(new Pair<>("type", this.type));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return supportedOperators.get(this.op);
    }

    @Override
    public Type getType() {
        return this.type;
    }
}
