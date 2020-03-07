package com.slang.semantic.symbol;

import com.slang.semantic.type.Type;

import java.util.ArrayList;

public class Symbol {
    public String identifier;
    public Type type;
    public ArrayList<Type> overloadableTypes;

    public Symbol(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
    }

    public Symbol(String identifier, ArrayList<Type> overloadableTypes) {
        this.identifier = identifier;
        this.overloadableTypes = overloadableTypes;
    }
}
