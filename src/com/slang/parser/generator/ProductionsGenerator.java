package com.slang.parser.generator;

import com.slang.lexer.CodeAxis;
import com.slang.parser.symbol.GrammarSymbol;
import com.slang.parser.symbol.NonTerminal;
import com.slang.parser.symbol.Production;
import com.slang.utils.Pair;
import com.slang.utils.Panic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

public class ProductionsGenerator {
    private ArrayList<Production> productions = new ArrayList<>();
    private HashSet<GrammarSymbol> usedGrammarSymbols = new HashSet<>();

    public ProductionsGenerator(String grammarDirectoryPath) {
        File directory = new File(grammarDirectoryPath);
        if (!directory.isDirectory()) {
            Panic panic = new Panic(String.format("parser: Grammar directory %s does not exists.", grammarDirectoryPath), new CodeAxis());
            panic.show();
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                GrammarScanner scanner = new GrammarScanner(file);
                ArrayList<Production> productions = scanner.getProductions();
                this.productions.addAll(productions);
                for (Production production : productions) {
                    this.usedGrammarSymbols.add(production.getLeftHandSide());
                    this.usedGrammarSymbols.addAll(production.getRightHandSide());
                }
            }
        }
    }

    public HashSet<GrammarSymbol> getSymbols() {
        return this.usedGrammarSymbols;
    }

    public ArrayList<Production> getProductions() {
        return this.productions;
    }

    public ArrayList<Production> getProductions(NonTerminal leftHandSideSymbol) {
        ArrayList<Production> filteredProductions = new ArrayList<>();
        for (Production production : this.productions) {
            if (!production.getLeftHandSide().equals(leftHandSideSymbol)) {
                continue;
            }
            filteredProductions.add(production);
        }
        return filteredProductions;
    }

    public ArrayList<Pair<Production, GrammarSymbol>> getProductionsWith(NonTerminal rightHandSideNonTerminalSymbol) {
        ArrayList<Pair<Production, GrammarSymbol>> filteredProductionsAndNextSymbols = new ArrayList<>();
        for (Production production : this.productions) {
            ArrayList<GrammarSymbol> rightHandSideGrammarSymbols = production.getRightHandSide();
            int len = rightHandSideGrammarSymbols.size();
            for (int i = 0; i < len; i++) {
                GrammarSymbol rightHandSideGrammarSymbol = rightHandSideGrammarSymbols.get(i);
                if (!(rightHandSideGrammarSymbol instanceof NonTerminal)
                        || !rightHandSideGrammarSymbol.equals(rightHandSideNonTerminalSymbol)) {
                    continue;
                }
                GrammarSymbol nextGrammarSymbol = (i < len - 1) ? rightHandSideGrammarSymbols.get(i + 1) : null;
                filteredProductionsAndNextSymbols.add(new Pair<>(production, nextGrammarSymbol));
            }
        }
        return filteredProductionsAndNextSymbols;
    }
}
