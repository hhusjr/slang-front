package com.slang.semantic.ast.node.expression;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.type.Type;
import com.slang.semantic.type.TypeEvaluator;
import com.slang.utils.Pair;

import java.util.ArrayList;

public class AssignExpression implements Expression {
    public Identifier leftHandSide;
    public Expression rightHandSide;
    public CodeAxis codeAxis;

    public AssignExpression(Identifier leftHandSide, Expression rightHandSide, CodeAxis operatorCodeAxis) {
        this.leftHandSide = leftHandSide;
        this.rightHandSide = rightHandSide;
        this.codeAxis = operatorCodeAxis;
        TypeEvaluator.checkAssignExpression(leftHandSide, rightHandSide, operatorCodeAxis);
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("leftHandSide", this.leftHandSide));
        properties.add(new Pair<>("rightHandSide", this.rightHandSide));
        properties.add(new Pair<>("type", this.rightHandSide.getType()));
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "Assign";
    }

    @Override
    public Type getType() {
        return this.rightHandSide.getType();
    }
}
