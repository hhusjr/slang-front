package com.slang.utils;

import com.slang.lexer.CodeAxis;

public class Panic {
    private CodeAxis codeAxis;
    private String message;
    public enum ErrorLevel {
        WARNING,
        ERROR,
    }
    private ErrorLevel errorLevel = ErrorLevel.ERROR;

    public Panic(String message, CodeAxis codeAxis) {
        this.message = message;
        this.codeAxis = codeAxis;
    }

    public void show() {
        String header;
        boolean exit;
        if (this.errorLevel == ErrorLevel.WARNING) {
            header = "Warning";
            exit = false;
        } else {
            header = "Error";
            exit = true;
        }
        if (this.codeAxis.path == null) {
            System.out.println(String.format("slang: %s: %s", header, this.message));
        } else if (this.codeAxis.line == -1) {
            System.out.println(String.format("slang: %s: %s at %s", header, this.message, this.codeAxis.path));
        } else if (this.codeAxis.charPos == -1) {
            System.out.println(String.format("slang: %s: %s at %s [#%d]", header, this.message, this.codeAxis.path, this.codeAxis.line));
        } else {
            System.out.println(String.format("slang: %s: %s at %s [%d:%d]", header, this.message, this.codeAxis.path, this.codeAxis.line, this.codeAxis.charPos));
        }

        if (exit) {
            System.out.println("Terminated.");
            System.exit(1);
        }
    }
}
