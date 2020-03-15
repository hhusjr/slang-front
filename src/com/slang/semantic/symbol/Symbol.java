package com.slang.semantic.symbol;

import com.slang.semantic.type.Type;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class Symbol {
    public String identifier;
    public SymbolType symbolType;

    // 变量特性
    public int level = 0;
    public Type type;

    // 函数特性
    public ArrayList<Pair<Type, Type>> overloadableTypes;

    public Symbol(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
        this.symbolType = SymbolType.VARIABLE;
    }

    public Symbol(String identifier, ArrayList<Pair<Type, Type>> overloadableTypes) {
        this.identifier = identifier;
        this.overloadableTypes = overloadableTypes;
        this.symbolType = SymbolType.FUNCTION;
    }

    // for variables
    public String getName() {
        return String.format("%s_%s", this.identifier, this.level);
    }

    // for functions
    public String getName(Type types) {
        for (int i = 0; i < overloadableTypes.size(); i++) {
            if (overloadableTypes.get(i).first.equals(types)) {
                return String.format("%s_%s", this.identifier, i);
            }
        }
        return null;
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
