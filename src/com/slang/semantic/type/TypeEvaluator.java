package com.slang.semantic.type;

import com.slang.lexer.CodeAxis;
import com.slang.semantic.ast.node.expression.BinaryExpressionOperator;
import com.slang.semantic.ast.node.expression.Expression;
import com.slang.semantic.ast.node.expression.Identifier;
import com.slang.semantic.ast.node.expression.UnaryExpressionOperator;
import com.slang.utils.Panic;

public class TypeEvaluator {
    public static boolean isBasicTypeCompatible(BasicType t1, BasicType t2) {
        switch (t1) {
            case INT:
            case FLOAT:
                return t2 == BasicType.INT || t2 == BasicType.FLOAT;
            case CHAR:
                return t2 == BasicType.CHAR;
            case BOOLEAN:
                return t2 == BasicType.BOOLEAN;
        }
        return false;
    }

    public static boolean isNumericType(Expression e) {
        return TypeEvaluator.isNumericType(e.getType());
    }

    public static boolean isNumericType(Type t) {
        return t.is(BasicType.INT) || t.is(BasicType.FLOAT);
    }

    private static Type higherSizeNumericType(Expression e1, Expression e2, CodeAxis operatorCodeAxis) {
        if (!TypeEvaluator.isNumericType(e1) || !TypeEvaluator.isNumericType(e2)) {
            Panic panic = new Panic("Unsupported binary-operator between non-numeric expressions", operatorCodeAxis);
            panic.show();
        }
        // INT + INT = INT
        if (e1.getType().is(BasicType.INT) && e2.getType().is(BasicType.INT)) {
            return TypeFactory.type(BasicType.INT);
        }
        // If contains FLOAT, then FLOAT
        return TypeFactory.type(BasicType.FLOAT);
    }

    public static Type binaryExpressionType(BinaryExpressionOperator operator, Expression leftOperand, Expression rightOperand, CodeAxis operatorCodeAxis) {
        switch (operator) {
            case PLUS:
            case SUB:
            case PROD:
            case DIV:
            case LT:
            case GT:
            case LTE:
            case GTE:
                return TypeEvaluator.higherSizeNumericType(leftOperand, rightOperand, operatorCodeAxis);
            case AND:
            case XOR:
            case OR:
            case MOD:
            case SHL:
            case SHR:
                if (!(leftOperand.getType().is(BasicType.INT) && rightOperand.getType().is(BasicType.INT))) {
                    Panic panic = new Panic("Operands should be integer type", operatorCodeAxis);
                    panic.show();
                }
                return TypeFactory.type(BasicType.INT);
            case LAND:
            case LOR:
                if (!(leftOperand.getType().is(BasicType.BOOLEAN) && rightOperand.getType().is(BasicType.BOOLEAN))) {
                    Panic panic = new Panic("Operands should be boolean type", operatorCodeAxis);
                    panic.show();
                }
                return TypeFactory.type(BasicType.BOOLEAN);
            case IS_EQ:
            case IS_NEQ:
                return TypeFactory.type(BasicType.BOOLEAN);
        }
        return null;
    }

    public static Type unaryExpressionType(UnaryExpressionOperator operator, Expression operand, CodeAxis operatorCodeAxis) {
        switch (operator) {
            case NOT:
                if (!operand.getType().is(BasicType.BOOLEAN)) {
                    Panic panic = new Panic("Operator ! can not be applied to such expression, only boolean expression is supported", operatorCodeAxis);
                    panic.show();
                }
                return TypeFactory.type(BasicType.BOOLEAN);
            case NEGATIVE:
                if (!TypeEvaluator.isNumericType(operand)) {
                    Panic panic = new Panic("Operator - can not be applied to such expression, only numeric expression is supported", operatorCodeAxis);
                    panic.show();
                }
                return operand.getType();
        }
        return null;
    }

    public static void checkAssignExpression(Identifier leftHandSide, Expression rightHandSide, CodeAxis operatorCodeAxis) {
        if (!leftHandSide.getType().compatibleWith(rightHandSide.getType())) {
            Panic panic = new Panic(String.format("Invalid assign statement, type %s and %s is not compatible.", leftHandSide.symbol.type.toString(), rightHandSide.getType().toString()), operatorCodeAxis);
            panic.show();
        }
    }
}
