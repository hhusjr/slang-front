package com.slang;

import com.slang.codegen.CodeGen;
import com.slang.lexer.TokenStream;
import com.slang.parser.RdScanner;
import com.slang.parser.generator.PredictTableGenerator;
import com.slang.parser.generator.ProductionsGenerator;
import com.slang.semantic.ast.AstBuilder;
import com.slang.semantic.ast.AstHelper;
import com.slang.semantic.ast.node.Node;
import gnu.getopt.Getopt;

import java.io.InputStream;
import java.util.ArrayList;

enum MODE {
    COMPILE,
    PRINT_AST,
    PRINT_PARSE_TABLE
}

public class Main {
    /*
     * slang -c <path> [-i <path>] -- Compile file to SVM IR code
     * slang -a <path> -- Print AST Tree
     * slang -t -- Print Parse Table
     */
    public static void main(String[] args) {
        Getopt getopt = new Getopt("slang", args, "c:a:t:i:h");
        int ch;
        String path = "./prog.sl";
        String outputPath = "./out.sli";
        MODE mode = MODE.COMPILE;
        while ((ch = getopt.getopt()) != -1) {
            switch (ch) {
                case 'c':
                    path = getopt.getOptarg();
                    mode = MODE.COMPILE;
                    break;
                case 'a':
                    path = getopt.getOptarg();
                    mode = MODE.PRINT_AST;
                    break;
                case 't':
                    mode = MODE.PRINT_PARSE_TABLE;
                    break;
                case 'i':
                    outputPath = getopt.getOptarg();
                    break;
                case 'h':
                default:
                    System.out.println("\n" +
                            "$ slang -c <path> [-i <path>] -- Compile file to SVM IR code\n" +
                            "$ slang -a <path> -- Print AST Tree\n" +
                            "$ slang -t -- Print Parse Table\n");
                    return;
            }
        }
        ClassLoader classLoader = Main.class.getClassLoader();
        ArrayList<InputStream> grammars = new ArrayList<>();
        grammars.add(classLoader.getResourceAsStream("grammars/SLang.slg"));
        grammars.add(classLoader.getResourceAsStream("grammars/Expression.slg"));
        grammars.add(classLoader.getResourceAsStream("grammars/Statement.slg"));
        ProductionsGenerator productionsGenerator = new ProductionsGenerator(grammars);
        PredictTableGenerator predictTableGenerator = new PredictTableGenerator(productionsGenerator);
        if (mode == MODE.PRINT_PARSE_TABLE) {
            predictTableGenerator.printParseTable();
            return;
        }
        TokenStream tokenStream = new TokenStream(path);
        RdScanner rdScanner = new RdScanner(tokenStream, predictTableGenerator);
        AstBuilder ast = new AstBuilder();
        Node root = ast.invokeAstBuilderMethod(rdScanner.buildParseTree());
        if (mode == MODE.PRINT_AST) {
            AstHelper.printAstTree(root);
            return;
        }
        CodeGen codeGen = new CodeGen(root);
        codeGen.generate();
        codeGen.save(outputPath);
        System.out.println("Done");
    }
}
