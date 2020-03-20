package com.slang.parser.generator;

import com.slang.lexer.CodeAxis;
import com.slang.parser.symbol.GrammarSymbol;
import com.slang.parser.symbol.GrammarSymbolFactory;
import com.slang.parser.symbol.NonTerminal;
import com.slang.parser.symbol.Production;
import com.slang.utils.Panic;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class GrammarScanner {
    private ArrayList<Production> productions = new ArrayList<>();

    public GrammarScanner(InputStream inputStream) {
        Scanner scanner = new Scanner(inputStream);
        while (scanner.hasNext()) {
            String grammar = scanner.nextLine().trim();
            if (grammar.length() == 0 || grammar.charAt(0) == '#') {
                continue;
            }
            this.productions.add(this.getProduction(grammar));
        }
    }

    public ArrayList<Production> getProductions() {
        return this.productions;
    }

    private GrammarSymbol getSymbolObject(String grammarSymbol) {
        int len = grammarSymbol.length();
        if (len < 1) {
            Panic panic = new Panic("parser: Invalid grammar symbol.", new CodeAxis());
            panic.show();
        }
        if (grammarSymbol.equals("$")) {
            return GrammarSymbolFactory.epsilon();
        }
        if (len > 2 && grammarSymbol.charAt(0) == '<' && grammarSymbol.charAt(len - 1) == '>') {
            return GrammarSymbolFactory.nonTerminal(grammarSymbol.substring(1, len - 1));
        }
        String name;
        boolean passToSdtHandler;
        if (len > 2 && grammarSymbol.charAt(0) == '[' && grammarSymbol.charAt(len - 1) == ']') {
            name = grammarSymbol.substring(1, len - 1);
            passToSdtHandler = true;
        } else {
            name = grammarSymbol;
            passToSdtHandler = false;
        }
        return GrammarSymbolFactory.terminal(name, passToSdtHandler);
    }

    private Production getProduction(String grammar) {
        String[] grammarSymbols = grammar.split("\\s+");
        int len = grammarSymbols.length;
        if (len < 4) {
            Panic panic = new Panic(String.format("parser: Invalid grammar %s", grammar), new CodeAxis());
            panic.show();
        }
        String name = grammarSymbols[0];
        GrammarSymbol leftHandSide = this.getSymbolObject(grammarSymbols[1]);
        if (!(leftHandSide instanceof NonTerminal)) {
            Panic panic = new Panic(String.format("parser: Invalid left hand side grammar symbol %s in %s", grammarSymbols[1], grammar), new CodeAxis());
            panic.show();
        }
        assert leftHandSide instanceof NonTerminal;
        Production production = new Production(name, (NonTerminal) leftHandSide);
        for (int i = 3; i < len; i++) {
            production.addRightHandSideSymbol(this.getSymbolObject(grammarSymbols[i]));
        }
        return production;
    }
}
