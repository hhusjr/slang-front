package com.slang.parser.generator;

import com.slang.parser.symbol.GrammarSymbol;
import com.slang.parser.symbol.NonTerminal;
import com.slang.parser.symbol.Production;
import com.slang.utils.Pair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

public class ProductionsGenerator {
    private ArrayList<Production> productions = new ArrayList<>();
    private HashSet<GrammarSymbol> usedGrammarSymbols = new HashSet<>();

    public ProductionsGenerator(ArrayList<InputStream> grammarUrls) {
        for (InputStream inputStream : grammarUrls) {
            GrammarScanner scanner = new GrammarScanner(inputStream);
            ArrayList<Production> productions = scanner.getProductions();
            this.productions.addAll(productions);
            for (Production production : productions) {
                this.usedGrammarSymbols.add(production.getLeftHandSide());
                this.usedGrammarSymbols.addAll(production.getRightHandSide());
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
