package com.slang.parser.symbol;

import com.slang.lexer.CodeAxis;
import com.slang.utils.Panic;

import java.util.ArrayList;

public class Production {
    private NonTerminal leftHandSide;
    private String name;
    private ArrayList<GrammarSymbol> rightHandSide = new ArrayList<>();

    public Production(String name, NonTerminal leftHandSide) {
        this.leftHandSide = leftHandSide;
        this.name = name;
    }

    public NonTerminal getLeftHandSide() {
        return leftHandSide;
    }

    public String getName() {
        return name;
    }

    public ArrayList<GrammarSymbol> getRightHandSide() {
        return this.rightHandSide;
    }

    public void addRightHandSideSymbol(GrammarSymbol grammarSymbol) {
        if (grammarSymbol instanceof Epsilon
                || grammarSymbol instanceof NonTerminal
                || grammarSymbol instanceof Terminal) {
            this.rightHandSide.add(grammarSymbol);
            return;
        }

        String errorMessage = String.format("Grammar production symbol error, unexcepted right hand side symbol object type %s, only Epsilon, NonTerminal, Terminal are supported",
                grammarSymbol.getClass().getSimpleName());
        Panic panic = new Panic(errorMessage, new CodeAxis());
        panic.show();
    }

    @Override
    public String toString() {
        ArrayList<String> rightHandSideSymbols = new ArrayList<>();
        for (GrammarSymbol rightHandSideGrammarSymbol : this.rightHandSide) {
            rightHandSideSymbols.add(rightHandSideGrammarSymbol.toString());
        }
        return String.format("%s ::= %s", this.leftHandSide, String.join(" ", rightHandSideSymbols));
    }
}
