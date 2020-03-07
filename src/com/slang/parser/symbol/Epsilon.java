package com.slang.parser.symbol;

import java.util.Objects;

public class Epsilon implements GrammarSymbol {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return Objects.hash("Epsilon");
    }

    @Override
    public String toString() {
        return "ε";
    }
}
