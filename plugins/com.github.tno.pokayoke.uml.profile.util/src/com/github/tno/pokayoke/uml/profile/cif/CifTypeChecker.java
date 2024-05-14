
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;

/**
 * Type checker for CIF annotated UML models.
 */
public class CifTypeChecker extends ACifObjectWalker<Type> {
    private final CifContext ctx;

    private final PrimitiveType booleanType;

    private final PrimitiveType integerType;

    /**
     * Constructs a new type CIF checker.
     *
     * @param elem The context for evaluating the expression.
     */
    public CifTypeChecker(Element elem) {
        this.ctx = new CifContext(elem);
        this.booleanType = PokaYokeTypeUtil.loadPrimitiveType(PokaYokeTypeUtil.PRIMITIVE_TYPE_BOOLEAN, elem);
        this.integerType = PokaYokeTypeUtil.loadPrimitiveType(PokaYokeTypeUtil.PRIMITIVE_TYPE_INTEGER, elem);
    }

    /**
     * Returns the type of the result when {@code expr} is evaluated and checks if this type is boolean.
     *
     * @param expr The expression to evaluate.
     * @return The type of the expression result.
     * @throws TypeException If {@code expr} cannot be evaluated or if the result type is not a boolean.
     */
    public Type checkBooleanExpression(AExpression expr) throws TypeException {
        Type exprType = visit(expr, ctx);
        if (!booleanType.equals(exprType)) {
            throw new TypeException("expected Boolean return type but got " + PokaYokeTypeUtil.getLabel(exprType),
                    expr.position);
        }
        return exprType;
    }

    /**
     * Returns the type of the result when {@code expr} is evaluated and checks if this type is supported.
     *
     * @param expr The expression to evaluate.
     * @return The type of the expression result.
     * @throws TypeException If {@code expr} cannot be evaluated or if the result type is not supported.
     */
    public Type checkExpression(AExpression expr) throws TypeException {
        Type exprType = visit(expr, ctx);
        if (!PokaYokeTypeUtil.isSupportedType(exprType)) {
            throw new TypeException("unsupported return type: " + PokaYokeTypeUtil.getLabel(exprType), expr.position);
        }
        return exprType;
    }

    /**
     * Returns the type of the result when {@code upd} is evaluated and checks if this type is supported.
     *
     * @param upd The update to evaluate.
     * @return The type of the update addressable.
     * @throws TypeException If {@code expr} cannot be evaluated, if the result type is not supported or if the update
     *     value type and update addressable type are not equal.
     */
    public Type checkUpdate(AUpdate upd) throws TypeException {
        Type updateType = visit(upd, ctx);
        if (!PokaYokeTypeUtil.isSupportedType(updateType)) {
            throw new TypeException("unsupported return type: " + PokaYokeTypeUtil.getLabel(updateType), upd.position);
        }
        return updateType;
    }

    @Override
    protected Type visit(ABoolExpression expr, CifContext ctx) {
        return booleanType;
    }

    @Override
    protected Type visit(AIntExpression expr, CifContext ctx) {
        return integerType;
    }

    @Override
    protected Type visit(EnumerationLiteral literal, TextPosition literalPos, CifContext ctx) {
        return literal.getEnumeration();
    }

    @Override
    protected Type visit(Property property, TextPosition propertyPos, CifContext ctx) {
        return property.getType();
    }

    @Override
    protected Type visit(AAssignmentUpdate update, CifContext ctx) {
        return super.visit(update, ctx);
    }

    @Override
    protected Type visit(Type addressable, TextPosition assignmentPos, Type value, CifContext ctx) {
        if (!Objects.equals(addressable, value)) {
            throw new TypeException(String.format("expected %s but got %s", PokaYokeTypeUtil.getLabel(addressable),
                    PokaYokeTypeUtil.getLabel(value)), assignmentPos);
        }
        return addressable;
    }

    @Override
    protected Type visit(BinaryOperator operator, TextPosition operatorPos, Type left, Type right, CifContext ctx) {
        String errorMsg = String.format("the operator '%s' is undefined for the argument type(s) %s, %s",
                operator.cifValue(), PokaYokeTypeUtil.getLabel(left), PokaYokeTypeUtil.getLabel(right));
        if (!Objects.equals(left, right)) {
            throw new TypeException(errorMsg, operatorPos);
        }

        final Type compareType = left;
        return switch (operator) {
            case AND, OR -> {
                if (!booleanType.equals(compareType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield booleanType;
            }
            case PLUS, MINUS -> {
                if (!integerType.equals(compareType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield integerType;
            }
            case GE, GT, LE, LT -> {
                if (!integerType.equals(compareType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield booleanType;
            }
            // Equality supports all types
            case EQ, NE -> booleanType;
        };
    }

    @Override
    protected Type visit(UnaryOperator operator, TextPosition operatorPos, Type child, CifContext ctx) {
        switch (operator) {
            case NOT:
                if (!booleanType.equals(child)) {
                    throw new TypeException(String.format("Expected Boolean but got %s near operator '%s'",
                            PokaYokeTypeUtil.getLabel(child), operator.cifValue()), operatorPos);
                }
                break;
            case MINUS:
                if (!integerType.equals(child)) {
                    throw new TypeException(String.format("Expected Integer but got %s near operator '%s'",
                            PokaYokeTypeUtil.getLabel(child), operator.cifValue()), operatorPos);
                }
                break;
        }
        return child;
    }

    @Override
    protected Type visit(Optional<String> invKind, List<String> events, TextPosition operatorPos, Type predicate,
            CifContext ctx)
    {
        if (!PokaYokeTypeUtil.isBooleanType(predicate)) {
            throw new TypeException("Expected Boolean but got " + PokaYokeTypeUtil.getLabel(predicate), operatorPos);
        }

        return predicate;
    }
}
