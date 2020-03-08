package com.slang.semantic.ast.node.statement;

import com.slang.utils.Pair;

import java.util.ArrayList;

public class FlowControlStatement implements Statement {
    public FlowControlType type;
    public LoopStatement target;

    public FlowControlStatement(FlowControlType type, LoopStatement target) {
        this.type = type;
        this.target = target;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        String typeStr;
        switch (this.type) {
            case CONTINUE:
                typeStr = "continue";
                break;
            case BREAK:
                typeStr = "break";
                break;
            default:
                typeStr = "unknown";
                break;
        }
        properties.add(new Pair<>("type", typeStr));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "FlowControlStatement";
    }
}
