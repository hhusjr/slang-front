package com.slang.lexer;

public class Token {
    public String name;
    public String value = null;
    public CodeAxis codeAxis;

    public Token(String name, CodeAxis codeAxis) {
        this.name = name;
        this.codeAxis = codeAxis;
    }

    public Token(String name, String value, CodeAxis codeAxis) {
        this.name = name;
        this.value = value;
        this.codeAxis = codeAxis;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
