package com.slang.parser.generator;

import com.slang.parser.symbol.*;
import com.slang.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PredictTableGenerator {
    private ProductionsGenerator productionsGenerator;
    private HashMap<GrammarSymbol, HashSet<GrammarSymbol>> firstSet = new HashMap<>();
    private HashMap<NonTerminal, HashSet<Terminal>> followSet = new HashMap<>();
    private HashMap<Pair<NonTerminal, Terminal>, Production> parseTable = new HashMap<>();

    public PredictTableGenerator(ProductionsGenerator productionsGenerator) {
        this.productionsGenerator = productionsGenerator;
        HashSet<GrammarSymbol> grammarSymbols = productionsGenerator.getSymbols();
        for (GrammarSymbol grammarSymbol : grammarSymbols) {
            this.getLeftHandSideFirstSet(grammarSymbol);
        }
        this.calculateFollowSet();
        this.calculateParseTable();
    }

    public void printFirstSet() {
        System.out.println("First set:");
        Set<GrammarSymbol> firstSetLeftHandSideGrammarSymbols = this.firstSet.keySet();
        for (GrammarSymbol leftHandSideGrammarSymbol : firstSetLeftHandSideGrammarSymbols) {
            System.out.print(String.format("FIRST[%s] = {", leftHandSideGrammarSymbol.toString()));
            ArrayList<String> symbolStrings = new ArrayList<>();
            for (GrammarSymbol rightHandSideGrammarSymbol : this.firstSet.get(leftHandSideGrammarSymbol)) {
                symbolStrings.add(rightHandSideGrammarSymbol.toString());
            }
            System.out.println(String.format("%s}", String.join(", ", symbolStrings)));
        }
    }

    private HashSet<GrammarSymbol> getLeftHandSideFirstSet(GrammarSymbol leftHandSideGrammarSymbol) {
        if (this.firstSet.containsKey(leftHandSideGrammarSymbol)) {
            return this.firstSet.get(leftHandSideGrammarSymbol);
        }

        HashSet<GrammarSymbol> result = new HashSet<>();
        if (leftHandSideGrammarSymbol instanceof Terminal || leftHandSideGrammarSymbol instanceof Epsilon) {
            result.add(leftHandSideGrammarSymbol);
        } else {
            assert leftHandSideGrammarSymbol instanceof NonTerminal;
            for (Production production : this.productionsGenerator.getProductions((NonTerminal) leftHandSideGrammarSymbol)) {
                result.addAll(this.getRightHandSideFirstSet(production.getRightHandSide()));
            }
        }
        this.firstSet.put(leftHandSideGrammarSymbol, result);
        return result;
    }

    private HashSet<GrammarSymbol> getRightHandSideFirstSet(ArrayList<GrammarSymbol> rightHandSideGrammarSymbols) {
        HashSet<GrammarSymbol> result = new HashSet<>();
        boolean noEpsilon = false;
        Epsilon epsilon = GrammarSymbolFactory.epsilon();

        for (GrammarSymbol grammarSymbol : rightHandSideGrammarSymbols) {
            HashSet<GrammarSymbol> firstSetOfCurrentGrammarSymbol = this.getLeftHandSideFirstSet(grammarSymbol);
            if (firstSetOfCurrentGrammarSymbol.contains(epsilon)) {
                HashSet<GrammarSymbol> firstSetOfCurrentGrammarSymbolWithoutEps = new HashSet<>(firstSetOfCurrentGrammarSymbol);
                firstSetOfCurrentGrammarSymbolWithoutEps.remove(epsilon);
                result.addAll(firstSetOfCurrentGrammarSymbolWithoutEps);
                continue;
            }
            result.addAll(firstSetOfCurrentGrammarSymbol);
            noEpsilon = true;
            break;
        }

        if (!noEpsilon) {
            result.add(epsilon);
        }

        return result;
    }

    public void printFollowSet() {
        System.out.println("Follow set:");
        Set<NonTerminal> followSetLeftHandSideSymbols = this.followSet.keySet();
        for (NonTerminal leftHandSideSymbol : followSetLeftHandSideSymbols) {
            System.out.print(String.format("FOLLOW[%s] = {", leftHandSideSymbol.toString()));
            ArrayList<String> symbolStrings = new ArrayList<>();
            for (GrammarSymbol rightHandSideGrammarSymbol : this.followSet.get(leftHandSideSymbol)) {
                symbolStrings.add(rightHandSideGrammarSymbol.toString());
            }
            System.out.println(String.format("%s}", String.join(", ", symbolStrings)));
        }
    }

    private void calculateFollowSet() {
        HashMap<NonTerminal, HashSet<Terminal>> lastFollow = new HashMap<>();
        HashSet<GrammarSymbol> grammarSymbols = this.productionsGenerator.getSymbols();
        for (GrammarSymbol grammarSymbol : grammarSymbols) {
            if (!(grammarSymbol instanceof NonTerminal)) {
                continue;
            }
            lastFollow.put((NonTerminal) grammarSymbol, new HashSet<>());
            if (grammarSymbol.equals(GrammarSymbolFactory.start())) {
                lastFollow.get(grammarSymbol).add(GrammarSymbolFactory.endOfFile());
            }
        }

        while (true) {
            HashMap<NonTerminal, HashSet<Terminal>> follow = new HashMap<>();
            for (NonTerminal lastFollowKey : lastFollow.keySet()) {
                follow.put(lastFollowKey, new HashSet<>(lastFollow.get(lastFollowKey)));
            }
            for (GrammarSymbol grammarSymbol : grammarSymbols) {
                if (!(grammarSymbol instanceof NonTerminal)) {
                    continue;
                }
                for (Pair<Production, GrammarSymbol> productionAndNextSymbol : this.productionsGenerator.getProductionsWith((NonTerminal) grammarSymbol)) {
                    HashSet<GrammarSymbol> first = new HashSet<>();

                    if (productionAndNextSymbol.second != null) {
                        first = this.getLeftHandSideFirstSet(productionAndNextSymbol.second);
                        HashSet<Terminal> firstOnlyTerminals = new HashSet<>();
                        for (GrammarSymbol firstInRightHandSide : first) {
                            if (!(firstInRightHandSide instanceof Terminal)) {
                                continue;
                            }
                            firstOnlyTerminals.add((Terminal) firstInRightHandSide);
                        }
                        follow.get(grammarSymbol).addAll(firstOnlyTerminals);
                    }

                    NonTerminal leftHandSideSymbol = productionAndNextSymbol.first.getLeftHandSide();
                    if ((productionAndNextSymbol.second == null || first.contains(GrammarSymbolFactory.epsilon()))
                    && (!leftHandSideSymbol.equals(grammarSymbol))) {
                        follow.get(grammarSymbol).addAll(lastFollow.get(leftHandSideSymbol));
                    }
                }
            }

            if (follow.equals(lastFollow)) {
                this.followSet = follow;
                return;
            }
            lastFollow = follow;
        }
    }

    public void printParseTable() {
        System.out.println("Parse table:");
        for (Pair<NonTerminal, Terminal> predictionKey : this.parseTable.keySet()) {
            System.out.println(String.format("[%s, %s] %s", predictionKey.first, predictionKey.second, this.parseTable.get(predictionKey)));
        }
    }

    private void calculateParseTable() {
        HashMap<Pair<NonTerminal, Terminal>, Production> parseTable = new HashMap<>();
        Epsilon epsilon = GrammarSymbolFactory.epsilon();
        for (Production production : this.productionsGenerator.getProductions()) {
            NonTerminal productionLeftHandSide = production.getLeftHandSide();
            HashSet<GrammarSymbol> first = this.getRightHandSideFirstSet(production.getRightHandSide());
            HashSet<GrammarSymbol> firstWithoutEps = new HashSet<>(first);
            firstWithoutEps.remove(epsilon);
            for (GrammarSymbol lookAheadTerminal : firstWithoutEps) {
                parseTable.put(new Pair<>(productionLeftHandSide, (Terminal) lookAheadTerminal), production);
            }
            if (first.contains(epsilon)) {
                for (GrammarSymbol lookAheadTerminal : this.followSet.get(productionLeftHandSide)) {
                    parseTable.put(new Pair<>(productionLeftHandSide, (Terminal) lookAheadTerminal), production);
                }
            }
        }
        this.parseTable = parseTable;
    }

    public Production getProduction(NonTerminal leftHandSide, Terminal lookAhead) {
        return this.parseTable.get(new Pair<>(leftHandSide, lookAhead));
    }

    public Boolean hasProduction(NonTerminal leftHandSide, Terminal lookAhead) {
        return this.parseTable.containsKey(new Pair<>(leftHandSide, lookAhead));
    }
}
