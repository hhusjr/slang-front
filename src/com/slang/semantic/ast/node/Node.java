package com.slang.semantic.ast.node;

import com.slang.utils.Pair;

import java.util.ArrayList;

public interface Node {
    ArrayList<Pair<String, Object>> getReadableProperties();
    String getReadableOperator();
}
