package com.slang.parser;

import com.slang.lexer.Token;
import com.slang.parser.symbol.GrammarSymbol;
import com.slang.parser.symbol.GrammarSymbolFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class ParseTreeNode {
    private GrammarSymbol grammarSymbol;
    private ArrayList<ParseTreeNode> children = new ArrayList<>();
    private String productionName = null;
    private Token token = null;
    private HashMap<String, Object> attributes = new HashMap<>();

    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    public Boolean hasAttribute(String key) {
        return this.attributes.containsKey(key);
    }

    public void setAttribute(String key, Object val) {
        this.attributes.put(key, val);
    }

    public String getProductionName() {
        return productionName;
    }

    public Token getToken() {
        return token;
    }

    public Boolean isFinal() {
        if (this.children.size() < 1) {
            return true;
        }
        return this.children.get(0).grammarSymbol.equals(GrammarSymbolFactory.epsilon());
    }

    public ParseTreeNode(GrammarSymbol grammarSymbol) {
        this.grammarSymbol = grammarSymbol;
    }

    public ParseTreeNode(GrammarSymbol grammarSymbol, Token token) {
        this.grammarSymbol = grammarSymbol;
        this.token = token;
    }

    public ParseTreeNode(GrammarSymbol grammarSymbol, String productionName) {
        this.grammarSymbol = grammarSymbol;
        this.productionName = productionName;
    }

    public GrammarSymbol getGrammarSymbol() {
        return grammarSymbol;
    }

    public void addChild(ParseTreeNode symbol) {
        this.children.add(symbol);
    }

    public ArrayList<ParseTreeNode> getChildren() {
        return this.children;
    }
}
