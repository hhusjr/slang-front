package com.slang.semantic.ast.node.expression;

import com.slang.semantic.ast.node.Node;
import com.slang.semantic.type.Type;

public interface Expression extends Node {
    Type getType();
}
