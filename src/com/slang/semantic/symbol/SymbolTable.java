package com.slang.semantic.symbol;

import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, Symbol> symbols = new HashMap<>();
    public SymbolTable next = null;

    public boolean hasSymbol(String identifier) {
        return this.symbols.containsKey(identifier);
    }

    public Symbol getSymbol(String identifier) {
        return this.symbols.get(identifier);
    }

    public void addSymbol(String identifier, Symbol symbol) {
        this.symbols.put(identifier, symbol);
    }
}
