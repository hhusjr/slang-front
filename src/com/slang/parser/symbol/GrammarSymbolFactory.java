package com.slang.parser.symbol;

import java.util.HashMap;

public class GrammarSymbolFactory {
    private static HashMap<String, Terminal> meaningfulTerminals = new HashMap<>();
    private static HashMap<String, Terminal> literalTerminals = new HashMap<>();
    private static HashMap<String, NonTerminal> nonTerminals = new HashMap<>();
    private static Epsilon epsilonSymbol = new Epsilon();

    public static String getStartSymbolName() {
        return startSymbolName;
    }

    private static String startSymbolName = "SLang";


    public static Terminal terminal(String name, boolean passToSdtHandler) {
        HashMap<String, Terminal> entity;
        if (passToSdtHandler) {
            entity = GrammarSymbolFactory.meaningfulTerminals;
        } else {
            entity = GrammarSymbolFactory.literalTerminals;
        }
        if (!entity.containsKey(name)) {
            entity.put(name, new Terminal(name, passToSdtHandler));
        }
        return entity.get(name);
    }

    public static NonTerminal nonTerminal(String name) {
        if (!GrammarSymbolFactory.nonTerminals.containsKey(name)) {
            GrammarSymbolFactory.nonTerminals.put(name, new NonTerminal(name));
        }
        return GrammarSymbolFactory.nonTerminals.get(name);
    }

    public static Epsilon epsilon() {
        return GrammarSymbolFactory.epsilonSymbol;
    }

    public static NonTerminal start() {
        return GrammarSymbolFactory.nonTerminal(GrammarSymbolFactory.startSymbolName);
    }

    public static Terminal endOfFile() {
        return GrammarSymbolFactory.terminal("$$", false);
    }
}
