
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.List;
import java.util.Objects;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.TypedElement;

import com.google.common.base.Optional;

/**
 * Type checker for CIF annotated UML models, currently supporting:
 * <ul>
 * <li>Boolean</li>
 * <li>Integer</li>
 * <li>Enumeration</li>
 * </ul>
 */
public class CifTypeChecker extends ACifObjectWalker<Type> {
    /**
     * Returns the type of the result when {@code expr} is evaluated against {@code elem} and checks if this type is
     * boolean.
     *
     * @param elem The context for evaluating the expression.
     * @param expr The expression to evaluate.
     * @return The type of the expression result.
     * @throws TypeException If {@code expr} cannot be evaluated or if the result type is not a boolean.
     */
    public static Type checkBooleanExpression(Element elem, AExpression expr) throws TypeException {
        CifTypeChecker typeChecker = new CifTypeChecker();
        CifContext ctx = new CifContext(elem);
        Type exprType = typeChecker.visit(expr, ctx);
        if (!ctx.getBooleanType().equals(exprType)) {
            throw new TypeException("expected Boolean return type but got " + getLabel(exprType), expr.position);
        }
        return exprType;
    }

    /**
     * Returns the type of the result when {@code expr} is evaluated against {@code elem} and checks if this type is
     * supported.
     *
     * @param elem The context for evaluating the expression.
     * @param expr The expression to evaluate.
     * @return The type of the expression result.
     * @throws TypeException If {@code expr} cannot be evaluated or if the result type is not supported.
     */
    public static Type checkExpression(Element elem, AExpression expr) throws TypeException {
        CifTypeChecker typeChecker = new CifTypeChecker();
        CifContext ctx = new CifContext(elem);
        Type exprType = typeChecker.visit(expr, ctx);
        if (!isSupported(exprType, ctx)) {
            throw new TypeException("unsupported return type: " + getLabel(exprType), expr.position);
        }
        return exprType;
    }

    /**
     * Returns the type of the result when {@code upd} is evaluated against {@code elem} and checks if this type is
     * supported.
     *
     * @param elem The context for evaluating the update.
     * @param upd The update to evaluate.
     * @return The type of the update addressable.
     * @throws TypeException If {@code expr} cannot be evaluated, if the result type is not supported or if the update
     *     value type and update addressable type are not equal.
     */
    public static Type checkUpdate(Element elem, AUpdate upd) throws TypeException {
        CifTypeChecker typeChecker = new CifTypeChecker();
        CifContext ctx = new CifContext(elem);
        Type updateType = typeChecker.visit(upd, ctx);
        if (!isSupported(updateType, ctx)) {
            throw new TypeException("unsupported return type: " + getLabel(updateType), upd.position);
        }
        return updateType;
    }

    /**
     * Returns the {@link TypedElement#getType() type} of the {@code elem} and checks if this type is supported.
     *
     * @param elem The context and type provider.
     * @return The type of the update addressable.
     * @throws TypeException if the result type is not supported.
     */
    public static Type checkSupportedType(TypedElement elem) throws TypeException {
        CifContext ctx = new CifContext(elem);
        Type elemType = elem.getType();
        if (!isSupported(elemType, ctx)) {
            throw new TypeException("Unsupported type: " + getLabel(elemType));
        }
        return elemType;
    }

    /**
     * Returns all Poka Yoke supported types for {@code elem}.
     * <p>
     * The next types are supported: {@link Enumeration enumerations in the model},
     * {@link CifContext#loadPrimitiveType(String, Model) primitive Boolean} and
     * {@link CifContext#loadPrimitiveType(String, Model) primitive Integer}.
     * </p>
     *
     * @param elem The context for which the supported types are queried.
     * @return All Poka Yoke supported types for {@code elem}.
     */
    public static List<Type> getSupportedTypes(TypedElement elem) {
        CifContext ctx = new CifContext(elem);
        return QueryableIterable.from(ctx.getAllElements()).objectsOfKind(Enumeration.class).asType(Type.class)
                .union(ctx.getBooleanType(), ctx.getIntegerType()).asList();
    }

    private CifTypeChecker() {
        // Empty for utility classes
    }

    @Override
    protected Type visit(ABoolExpression expr, CifContext ctx) {
        return ctx.getBooleanType();
    }

    @Override
    protected Type visit(AIntExpression expr, CifContext ctx) {
        return ctx.getIntegerType();
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
            throw new TypeException(String.format("expected %s but got %s", getLabel(addressable), getLabel(value)),
                    assignmentPos);
        }
        return addressable;
    }

    @Override
    protected Type visit(BinaryOperator operator, TextPosition operatorPos, Type left, Type right, CifContext ctx) {
        String errorMsg = String.format("the operator '%s' is undefined for the argument type(s) %s, %s",
                operator.cifValue(), getLabel(left), getLabel(right));
        if (!Objects.equals(left, right)) {
            throw new TypeException(errorMsg, operatorPos);
        }

        final Type compareType = left;
        return switch (operator) {
            case AND, OR -> {
                if (!ctx.getBooleanType().equals(compareType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield ctx.getBooleanType();
            }
            case PLUS, MINUS -> {
                if (!ctx.getIntegerType().equals(compareType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield compareType;
            }
            case GE, GT, LE, LT -> {
                if (!ctx.getIntegerType().equals(compareType)) {
                    throw new TypeException(errorMsg, operatorPos);
                }
                yield ctx.getBooleanType();
            }
            // Equality supports all types
            case EQ, NE -> ctx.getBooleanType();
        };
    }

    @Override
    protected Type visit(UnaryOperator operator, TextPosition operatorPos, Type child, CifContext ctx) {
        switch (operator) {
            case NOT:
                if (!ctx.getBooleanType().equals(child)) {
                    throw new TypeException(String.format("Expected Boolean but got %s near operator '%s'",
                            getLabel(child), operator.cifValue()), operatorPos);
                }
                break;
            case MINUS:
                if (!ctx.getIntegerType().equals(child)) {
                    throw new TypeException(String.format("Expected Integer but got %s near operator '%s'",
                            getLabel(child), operator.cifValue()), operatorPos);
                }
                break;
        }
        return child;
    }

    @Override
    protected Type visit(TextPosition operatorPos, List<Type> guards, Type then, List<Type> elifs, Type elze,
            CifContext ctx)
    {
        for (Type guard: guards) {
            if (!ctx.getBooleanType().equals(guard)) {
                throw new TypeException("Expected Boolean but got " + getLabel(guard), operatorPos);
            }
        }

        for (Type elif: elifs) {
            if (!Objects.equals(then, elif)) {
                throw new TypeException(String.format("Expected %s but got %s", getLabel(then), getLabel(elif)),
                        operatorPos);
            }
        }

        if (!Objects.equals(then, elze)) {
            throw new TypeException(String.format("Expected %s but got %s", getLabel(then), getLabel(elze)),
                    operatorPos);
        }

        return then;
    }

    @Override
    protected Type visit(TextPosition operatorPos, List<Type> guards, Type then, CifContext ctx) {
        for (Type guard: guards) {
            if (!ctx.getBooleanType().equals(guard)) {
                throw new TypeException("Expected Boolean but got " + getLabel(guard), operatorPos);
            }
        }

        return then;
    }

    private static String getLabel(Type type) {
        return type == null ? "null" : type.getLabel(true);
    }

    private static boolean isSupported(Type type, CifContext ctx) {
        if (type == null) {
            return false;
        }
        return ctx.getBooleanType().equals(type) || ctx.getIntegerType().equals(type) || type instanceof Enumeration;
    }

    @Override
    protected Type visit(Optional<String> invKind, List<String> events, TextPosition operatorPos, Type predicate,
            CifContext ctx)
    {
        if (invKind.isPresent()) {
            String kind = invKind.get();
            if (!kind.equals("needs") && !kind.equals("disables")) {
                throw new TypeException("Expected 'needs' or 'disables', but got " + kind, operatorPos);
            }
        }

        // TODO check events

        return predicate;
    }
}
