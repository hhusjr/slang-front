package com.slang.semantic.ast.node.statement;

import com.slang.semantic.symbol.Symbol;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class OpcodeStatement implements Statement {
    public String opcode;
    public Integer param = null;
    public Symbol symbol = null;

    public OpcodeStatement(String opcode) {
        this.opcode = opcode;
    }

    public OpcodeStatement(String opcode, Integer param) {
        this.opcode = opcode;
        this.param = param;
    }

    public OpcodeStatement(String opcode, Symbol symbol) {
        this.opcode = opcode;
        this.symbol = symbol;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("opcode", this.opcode));
        if (this.symbol != null) {
            properties.add(new Pair<>("symbol", this.symbol));
        }
        if (this.param != null) {
            properties.add(new Pair<>("param", this.param));
        }
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "OpcodeStatement";
    }
}
