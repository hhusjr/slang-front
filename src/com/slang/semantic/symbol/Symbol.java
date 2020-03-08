package com.slang.semantic.symbol;

import com.slang.semantic.type.Type;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class Symbol {
    public String identifier;
    public Type type;
    public ArrayList<Pair<Type, Type>> overloadableTypes;

    public Symbol(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
    }

    public Symbol(String identifier, ArrayList<Pair<Type, Type>> overloadableTypes) {
        this.identifier = identifier;
        this.overloadableTypes = overloadableTypes;
    }

    public void newOverload(Type types, Type returnType) {
        assert this.overloadableTypes != null;
        if (this.getOverload(types) == null) {
            this.overloadableTypes.add(new Pair<>(types, returnType));
        }
    }

    public Pair<Type, Type> getOverload(Type types) {
        for (Pair<Type, Type> overload : overloadableTypes) {
            if (overload.first.equals(types)) {
                return overload;
            }
        }
        return null;
    }
}
