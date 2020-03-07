package com.slang.parser.symbol;

import java.util.Objects;

public class NonTerminal implements GrammarSymbol {
    public String name;

    public NonTerminal(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NonTerminal that = (NonTerminal) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return String.format("<%s>", this.name);
    }
}
