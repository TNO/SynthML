
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.UmlPrimitiveType;

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
        this.booleanType = UmlPrimitiveType.BOOLEAN.load(elem);
        this.integerType = UmlPrimitiveType.INTEGER.load(elem);
    }

    /**
     * Checks if the evaluated {@code expression} type can be assigned to a boolean.
     *
     * @param expression The expression to evaluate.
     * @throws TypeException If {@code expression} cannot be evaluated or if the result type cannot be assigned to a
     *     boolean.
     */
    public void checkBooleanAssignment(AExpression expression) throws TypeException {
        checkAssignment(booleanType, expression);
    }

    /**
     * Checks if the evaluated {@code value} type can be assigned to the {@code addressable} type.
     *
     * @param addressable The expected addressable type.
     * @param value The value expression to evaluate.
     * @throws TypeException If the {@code value} expression cannot be evaluated or if the value type cannot be assigned
     *     to the {@code addressable} type.
     */
    public void checkAssignment(Type addressable, AExpression value) throws TypeException {
        visit(addressable, null, visit(value, ctx), ctx);
    }

    /**
     * Checks if the predicate type of the evaluated {@code invariant} can be assigned to a boolean.
     *
     * @param invariant The invariant to evaluate.
     * @return The type of the invariant predicate.
     * @throws TypeException If {@code invariant} cannot be evaluated or if the predicate type is not supported.
     */
    public Type checkInvariant(AInvariant invariant) throws TypeException {
        return visit(invariant, ctx);
    }

    /**
     * Checks whether the given update is correctly typed.
     *
     * @param update The update to type check.
     */
    public void checkUpdate(AUpdate update) {
        visit(update, ctx);
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
    protected Type visit(Type addressable, TextPosition assignmentPos, Type value, CifContext ctx) {
        return combineTypesOrThrow(addressable, value, assignmentPos);
    }

    @Override
    protected Type visit(List<Type> guards, List<Type> thens, List<Type> elifs, List<Type> elses,
            TextPosition updatePos, CifContext ctx)
    {
        return visit(guards, thens, updatePos, ctx);
    }

    @Override
    protected Type visit(List<Type> guards, List<Type> thens, TextPosition updatePos, CifContext ctx) {
        return visit(guards, thens.get(0), updatePos, ctx);
    }

    @Override
    protected Type visit(List<Type> guards, Type thenExpr, List<Type> elifs, Type elseExpr, TextPosition expressionPos,
            CifContext ctx)
    {
        Type thenType = visit(guards, thenExpr, expressionPos, ctx);
        Type combinedType = combineTypesOrThrow(thenType, elseExpr, expressionPos);

        for (Type elif: elifs) {
            combinedType = combineTypesOrThrow(combinedType, elif, expressionPos);
        }

        return combinedType;
    }

    @Override
    protected Type visit(List<Type> guards, Type thenExpr, TextPosition expressionPos, CifContext ctx) {
        for (Type type: guards) {
            if (!type.conformsTo(booleanType)) {
                throw new TypeException(String.format("Expected a Boolean but got '%s'", type), expressionPos);
            }
        }

        return thenExpr;
    }

    @Override
    protected Type visit(BinaryOperator operator, TextPosition operatorPos, Type left, Type right, CifContext ctx) {
        final String errorMsg = String.format("the operator '%s' is undefined for the argument type(s) %s, %s",
                operator.cifValue(), PokaYokeTypeUtil.getLabel(left), PokaYokeTypeUtil.getLabel(right));
        final Type compareType = combineTypes(left, right);
        if (compareType == null) {
            throw new TypeException(errorMsg, operatorPos);
        }
        return switch (operator) {
            case AND, OR -> {
                if (!compareType.conformsTo(booleanType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield compareType;
            }
            case PLUS, MINUS -> {
                if (!compareType.conformsTo(integerType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield compareType;
            }
            case GE, GT, LE, LT -> {
                if (!compareType.conformsTo(integerType)) {
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
                if (!child.conformsTo(booleanType)) {
                    throw new TypeException(String.format("Expected Boolean but got %s near operator '%s'",
                            PokaYokeTypeUtil.getLabel(child), operator.cifValue()), operatorPos);
                }
                break;
            case MINUS:
                if (!child.conformsTo(integerType)) {
                    throw new TypeException(String.format("Expected Integer but got %s near operator '%s'",
                            PokaYokeTypeUtil.getLabel(child), operator.cifValue()), operatorPos);
                }
                break;
        }
        return child;
    }

    @Override
    protected Type visit(Optional<String> invKind, List<String> events, TextPosition invariantPos, Type predicate,
            CifContext ctx)
    {
        if (!predicate.conformsTo(booleanType)) {
            throw new TypeException("Expected Boolean but got " + PokaYokeTypeUtil.getLabel(predicate), invariantPos);
        }

        // Validate that the events exist, i.e., refer to declared opaque behaviors.
        for (String event: events) {
            if (ctx.getOpaqueBehavior(event) == null) {
                throw new CustomSyntaxException("Unresolved opaque behavior name " + event, invariantPos);
            }
        }

        return predicate;
    }

    /**
     * Combines the {@code left} and {@code right} types into a compatible return type.
     *
     * @param left One of the types to combine.
     * @param right One of the types to combine.
     * @return The compatible return type, or {@code null} if the types cannot be combined.
     */
    protected Type combineTypes(Type left, Type right) {
        if (left == null || right == null) {
            return null;
        } else if (left.equals(right)) {
            return left;
        }
        if (left.conformsTo(integerType) && right.conformsTo(integerType)) {
            // Both left and right are integers
            return integerType;
        }
        return null;
    }

    /**
     * Combines the {@code left} and {@code right} types into a compatible return type. Throws an exception if the types
     * cannot be combined.
     *
     * @param left One of the types to combine.
     * @param right One of the types to combine.
     * @param combinationPos The text position where type combination traces back to.
     * @return The compatible return type.
     */
    public Type combineTypesOrThrow(Type left, Type right, TextPosition combinationPos) {
        Type combined = combineTypes(left, right);
        if (combined == null) {
            throw new TypeException(String.format("Expected %s but got %s", PokaYokeTypeUtil.getLabel(left),
                    PokaYokeTypeUtil.getLabel(right)), combinationPos);
        }

        return combined;
    }
}
