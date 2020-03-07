package com.slang.lexer;

public class CodeAxis {
    public String path = null;
    public int line = -1;
    public int charPos = -1;

    public CodeAxis() {}

    public CodeAxis(String path) {
        this.path = path;
    }

    public CodeAxis(String path, int line, int charPos) {
        this.path = path;
        this.line = line;
        this.charPos = charPos;
    }
}
