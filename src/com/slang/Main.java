package com.slang;

import com.slang.lexer.TokenStream;
import com.slang.parser.RdScanner;
import com.slang.parser.generator.PredictTableGenerator;
import com.slang.parser.generator.ProductionsGenerator;
import com.slang.semantic.ast.AstBuilder;
import com.slang.semantic.ast.AstHelper;
import com.slang.semantic.ast.node.Node;

public class Main {
    public static void main(String[] args) {
        TokenStream tokenStream = new TokenStream("./examples/test.sl");
        ProductionsGenerator productionsGenerator = new ProductionsGenerator("./grammars/");
        PredictTableGenerator predictTableGenerator = new PredictTableGenerator(productionsGenerator);
        RdScanner rdScanner = new RdScanner(tokenStream, predictTableGenerator);
        AstBuilder ast = new AstBuilder();
        Node root = ast.invokeAstBuilderMethod(rdScanner.buildParseTree());
        AstHelper.printAstTree(root);
    }
}
