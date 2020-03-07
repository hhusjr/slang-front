package com.slang.semantic.symbol;

import com.slang.lexer.CodeAxis;
import com.slang.utils.Panic;

public class SymbolTableManager {
    private SymbolTable currentSymbolTable = new SymbolTable();

    public SymbolTable getCurrentSymbolTable() {
        return currentSymbolTable;
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

    public void addSymbol(String identifier, Symbol symbol) {
        this.currentSymbolTable.addSymbol(identifier, symbol);
    }

    public void enterScope() {
        SymbolTable newSymbolTable = new SymbolTable();
        newSymbolTable.next = this.currentSymbolTable;
        this.currentSymbolTable = newSymbolTable;
    }

    public void leaveScope() {
        this.currentSymbolTable = this.currentSymbolTable.next;
    }
}
