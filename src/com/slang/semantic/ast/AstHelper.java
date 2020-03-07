package com.slang.semantic.ast;

import com.slang.semantic.ast.node.Node;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class AstHelper {
    private static void printAstTree(Node root, int indent) {
        String prefixSpacingUnit = "\t";
        String operator = root.getReadableOperator();
        String indentString = prefixSpacingUnit.repeat(Math.max(0, indent));
        System.out.println(String.format("%s[%s]", indentString, operator));
        ArrayList<Pair<String, Object>> properties = root.getReadableProperties();
        for (Pair<String, Object> property : properties) {
            if (property.second instanceof Node) {
                System.out.println(String.format("%s%s: ", indentString, property.first));
                AstHelper.printAstTree((Node) property.second, indent + 1);
            } else {
                System.out.println(String.format("%s%s: \"%s\"", indentString, property.first, property.second.toString()));
            }
        }
    }

    public static void printAstTree(Node root) {
        System.out.println("AST Tree:");
        AstHelper.printAstTree(root, 0);
    }
}
