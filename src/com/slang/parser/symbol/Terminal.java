package com.slang.parser.symbol;

import java.util.Objects;

public class Terminal implements GrammarSymbol {
    public String tokenName;
    public boolean passToAstBuilder;

    public Terminal(String tokenName, boolean passToAstBuilder) {
        this.tokenName = tokenName;
        this.passToAstBuilder = passToAstBuilder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal = (Terminal) o;
        return tokenName.equals(terminal.tokenName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenName);
    }

    @Override
    public String toString() {
        return this.tokenName;
    }
}