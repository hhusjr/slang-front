package com.slang.semantic.symbol;

import com.slang.lexer.CodeAxis;
import com.slang.utils.Panic;

public class SymbolTableManager {
    private SymbolTable currentSymbolTable = new SymbolTable();
    private int level = 0;

    public SymbolTable getCurrentSymbolTable() {
        return currentSymbolTable;
    }

    public boolean hasSymbol(String identifier) {
        SymbolTable symbolTable = this.currentSymbolTable;
        while (!symbolTable.hasSymbol(identifier)) {
            if (symbolTable.next == null) {
                return false;
            }
            symbolTable = symbolTable.next;
        }
        return true;
    }

    public boolean hasSymbolInCurrentScope(String identifier) {
        return this.currentSymbolTable.hasSymbol(identifier);
    }

    public Symbol findSymbol(String identifier, CodeAxis codeAxis) {
        SymbolTable symbolTable = this.currentSymbolTable;
        while (!symbolTable.hasSymbol(identifier)) {
            if (symbolTable.next == null) {
                Panic panic = new Panic(String.format("Undeclared identifier %s", identifier), codeAxis);
                panic.show();
            }
            symbolTable = symbolTable.next;
        }
        return symbolTable.getSymbol(identifier);
    }

    public Symbol addSymbol(String identifier, Symbol symbol) {
        symbol.level = this.level;
        this.currentSymbolTable.addSymbol(identifier, symbol);
        return symbol;
    }

    public void enterScope() {
        SymbolTable newSymbolTable = new SymbolTable();
        newSymbolTable.next = this.currentSymbolTable;
        this.currentSymbolTable = newSymbolTable;
        this.level++;
    }

    public void leaveScope() {
        this.currentSymbolTable = this.currentSymbolTable.next;
        this.level--;
    }
}
