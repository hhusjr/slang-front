package com.slang.semantic.ast.node.statement;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.ast.node.expression.Expression;
import com.slang.semantic.type.BasicType;
import com.slang.utils.Pair;
import com.slang.utils.Panic;

import java.util.ArrayList;

public class IfStatement implements Statement {
    public Expression condition;
    public Statement trueBlock;
    public Statement falseBlock;
    public CodeAxis codeAxis;

    public IfStatement(Expression condition, Statement trueBlock, Statement falseBlock, CodeAxis codeAxis) {
        if (!condition.getType().is(BasicType.BOOLEAN)) {
            Panic panic = new Panic(String.format("The condition expression of the If statement must be Boolean type, got %s", condition.getType()), codeAxis);
            panic.show();
        }
        this.condition = condition;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
        this.codeAxis = codeAxis;
    }

    @Override
    public ArrayList<Pair<String, Object>> getReadableProperties() {
        ArrayList<Pair<String, Object>> properties = new ArrayList<>();
        properties.add(new Pair<>("condition", this.condition));
        properties.add(new Pair<>("trueBlock", this.trueBlock));
        if (this.falseBlock != null) {
            properties.add(new Pair<>("falseBlock", this.falseBlock));
        }
        return properties;
    }

    @Override
    public String getReadableOperator() {
        return "IfStatement";
    }
}
