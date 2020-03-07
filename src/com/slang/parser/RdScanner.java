package com.slang.parser;

import com.slang.lexer.Token;
import com.slang.lexer.TokenStream;
import com.slang.parser.generator.PredictTableGenerator;
import com.slang.parser.symbol.*;
import com.slang.utils.Pair;
import com.slang.utils.Panic;

import java.util.ArrayList;
import java.util.Stack;

public class RdScanner {
    private TokenStream tokenStream;
    private PredictTableGenerator predictTableGenerator;

    public RdScanner(TokenStream tokenStream, PredictTableGenerator predictTableGenerator) {
        this.tokenStream = tokenStream;
        this.predictTableGenerator = predictTableGenerator;
    }

    public ParseTreeNode buildParseTree() {
        Stack<Pair<GrammarSymbol, ParseTreeNode>> parseStack = new Stack<>();
        NonTerminal startSymbol = GrammarSymbolFactory.start();
        Token lookAhead = this.tokenStream.lookAhead();
        Production useProduction = this.predictTableGenerator.getProduction(startSymbol, GrammarSymbolFactory.terminal(lookAhead.name, false));
        ParseTreeNode rootNode = new ParseTreeNode(startSymbol, useProduction.getName());
        parseStack.push(new Pair<>(startSymbol, rootNode));
        while (!parseStack.empty()) {
            lookAhead = this.tokenStream.lookAhead();
            Pair<GrammarSymbol, ParseTreeNode> current = parseStack.peek();

            if (current.first instanceof Terminal) {
                Token token = this.tokenStream.next();
                if (!token.name.equals(((Terminal) current.first).tokenName)) {
                    Panic panic = new Panic(String.format("Unexpected token: %s, expected %s", token.name, ((Terminal) current.first).tokenName),
                            token.codeAxis);
                    panic.show();
                }
                if (((Terminal) current.first).passToAstBuilder) {
                    current.second.addChild(new ParseTreeNode(current.first, token));
                }
                parseStack.pop();
                continue;
            } else if (current.first instanceof Epsilon) {
                current.second.addChild(new ParseTreeNode(current.first));
                parseStack.pop();
                continue;
            }

            Terminal terminal = GrammarSymbolFactory.terminal(lookAhead.name, false);
            assert current.first instanceof NonTerminal;
            if (!this.predictTableGenerator.hasProduction((NonTerminal) current.first, terminal)) {
                Panic panic = new Panic(String.format("Unexpected token: %s", lookAhead.name), lookAhead.codeAxis);
                panic.show();
            }

            useProduction = this.predictTableGenerator.getProduction((NonTerminal) current.first, terminal);
            ParseTreeNode child = new ParseTreeNode(current.first, useProduction.getName());
            current.second.addChild(child);
            parseStack.pop();

            ArrayList<GrammarSymbol> rightHandSide = useProduction.getRightHandSide();
            for (int i = rightHandSide.size() - 1; i >= 0; i--) {
                parseStack.add(new Pair<>(rightHandSide.get(i), child));
            }
        }

        if (!tokenStream.isFinished()) {
            Token lastToken = tokenStream.next();
            while (!tokenStream.isFinished()) lastToken = tokenStream.next();
            Panic panic = new Panic(String.format("Unexpected token %s at the end of the file", lastToken.toString()), lastToken.codeAxis);
            panic.show();
        }

        return rootNode;
    }
}
