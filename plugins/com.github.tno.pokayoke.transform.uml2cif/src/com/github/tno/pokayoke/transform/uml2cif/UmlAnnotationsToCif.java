
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifTypeUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.InvKind;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.IntType;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.github.tno.pokayoke.uml.profile.cif.ACifObjectWalker;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.google.common.base.Verify;

/** Translates UML annotations like guards and effects in UML synthesis specifications to CIF. */
public class UmlAnnotationsToCif extends ACifObjectWalker<Object> {
    /** The context that allows querying the input model. */
    private final CifContext context;

    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    private final Map<Enumeration, EnumDecl> enumMap;

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    private final Map<EnumerationLiteral, EnumLiteral> enumLiteralMap;

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    private final Map<Property, DiscVariable> variableMap;

    /** The mapping from UML opaque behaviors to corresponding translated CIF (controllable start) events. */
    private final Map<OpaqueBehavior, Event> eventMap;

    public UmlAnnotationsToCif(CifContext context, Map<Enumeration, EnumDecl> enumMap,
            Map<EnumerationLiteral, EnumLiteral> enumLiteralMap, Map<Property, DiscVariable> variableMap,
            Map<OpaqueBehavior, Event> eventMap)
    {
        this.context = context;
        this.enumMap = enumMap;
        this.enumLiteralMap = enumLiteralMap;
        this.variableMap = variableMap;
        this.eventMap = eventMap;
    }

    /**
     * Translates an expression.
     *
     * @param expr The parsed expression to translate.
     * @return The translated CIF expression.
     */
    public Expression translate(AExpression expr) {
        return (Expression)visit(expr, context);
    }

    /**
     * Translates an update.
     *
     * @param update The parsed update to translate.
     * @return The translated CIF update.
     */
    public Update translate(AUpdate update) {
        return (Update)visit(update, context);
    }

    /**
     * Translates a collection of updates.
     *
     * @param updates The parsed updates to translate.
     * @return The translated CIF updates.
     */
    public List<Update> translate(Collection<AUpdate> updates) {
        return updates.stream().map(this::translate).toList();
    }

    /**
     * Translates an invariant.
     *
     * @param invariant The parsed invariant to translate.
     * @return The translated CIF invariant.
     */
    @SuppressWarnings("unchecked")
    public List<Invariant> translate(AInvariant invariant) {
        return (List<Invariant>)visit(invariant, context);
    }

    /**
     * Translates a UML type to a CIF type.
     *
     * @param umlType The UML type to translate.
     * @return The translated CIF type.
     */
    public CifType translateType(Type umlType) {
        if (umlType instanceof Enumeration umlEnum) {
            return CifConstructors.newEnumType(enumMap.get(umlEnum), null);
        } else if (PokaYokeTypeUtil.isBooleanType(umlType)) {
            return CifConstructors.newBoolType();
        } else if (PokaYokeTypeUtil.isIntegerType(umlType)) {
            return CifConstructors.newIntType(PokaYokeTypeUtil.getMinValue(umlType), null,
                    PokaYokeTypeUtil.getMaxValue(umlType));
        } else {
            throw new RuntimeException("Unsupported type: " + umlType);
        }
    }

    @Override
    protected Object visit(Object addressable, TextPosition assignmentPos, Object value, CifContext ctx) {
        return CifConstructors.newAssignment((Expression)addressable, null, (Expression)value);
    }

    @Override
    protected Object visit(BinaryOperator operator, TextPosition operatorPos, Object left, Object right,
            CifContext ctx)
    {
        Expression leftExpr = (Expression)left;
        Expression rightExpr = (Expression)right;

        CifType type = switch (operator) {
            case AND, EQ, GE, GT, LE, LT, NE, OR -> CifConstructors.newBoolType();

            case MINUS -> {
                IntType leftType = (IntType)leftExpr.getType();
                IntType rightType = (IntType)rightExpr.getType();

                Verify.verify(!CifTypeUtils.isRangeless(leftType), "Expected integer types to have a range.");
                Verify.verify(!CifTypeUtils.isRangeless(rightType), "Expected integer types to have a range.");

                long leftLower = leftType.getLower();
                long leftUpper = leftType.getUpper();
                long rightLower = rightType.getLower();
                long rightUpper = rightType.getUpper();

                long lower = leftLower - rightUpper;
                long upper = leftUpper - rightLower;

                if (lower < Integer.MIN_VALUE || upper > Integer.MAX_VALUE) {
                    throw new RuntimeException("Unexpected error: integer type range went out of bounds.");
                }

                yield CifConstructors.newIntType((int)lower, null, (int)upper);
            }

            case PLUS -> {
                IntType leftType = (IntType)leftExpr.getType();
                IntType rightType = (IntType)rightExpr.getType();

                Verify.verify(!CifTypeUtils.isRangeless(leftType), "Expected integer types to have a range.");
                Verify.verify(!CifTypeUtils.isRangeless(rightType), "Expected integer types to have a range.");

                long leftLower = leftType.getLower();
                long leftUpper = leftType.getUpper();
                long rightLower = rightType.getLower();
                long rightUpper = rightType.getUpper();

                long lower = leftLower + rightLower;
                long upper = leftUpper + rightUpper;

                if (lower < Integer.MIN_VALUE || upper > Integer.MAX_VALUE) {
                    throw new RuntimeException("Unexpected error: integer type range went out of bounds.");
                }

                yield CifConstructors.newIntType((int)lower, null, (int)upper);
            }
        };

        return CifConstructors.newBinaryExpression(leftExpr, translateOperator(operator), null, rightExpr, type);
    }

    @Override
    protected Object visit(EnumerationLiteral literal, TextPosition literalPos, CifContext ctx) {
        return CifConstructors.newEnumLiteralExpression(enumLiteralMap.get(literal), null,
                translateType(literal.getEnumeration()));
    }

    @Override
    protected Expression visit(Property property, TextPosition propertyPos, CifContext ctx) {
        DiscVariable cifVariable = variableMap.get(property);
        return CifConstructors.newDiscVariableExpression(null, EcoreUtil.copy(cifVariable.getType()), cifVariable);
    }

    @Override
    protected Expression visit(UnaryOperator operator, TextPosition operatorPos, Object child, CifContext ctx) {
        Expression childExpr = (Expression)child;

        return CifConstructors.newUnaryExpression(childExpr, translateOperator(operator), null,
                EcoreUtil.copy(childExpr.getType()));
    }

    @Override
    protected Expression visit(ABoolExpression expr, CifContext ctx) {
        return CifConstructors.newBoolExpression(null, CifConstructors.newBoolType(), expr.value);
    }

    @Override
    protected Expression visit(AIntExpression expr, CifContext ctx) {
        return CifValueUtils.makeInt(Integer.parseInt(expr.value));
    }

    @Override
    protected Object visit(Optional<String> invKind, List<String> events, TextPosition operatorPos, Object predicate,
            CifContext ctx)
    {
        Expression cifPredicate = (Expression)predicate;

        List<Invariant> cifInvariants = new ArrayList<>(Math.max(events.size(), 1));

        if (invKind.isEmpty()) {
            Invariant cifInvariant = CifConstructors.newInvariant();
            cifInvariant.setInvKind(InvKind.STATE);
            cifInvariant.setPredicate(cifPredicate);
            cifInvariant.setSupKind(SupKind.REQUIREMENT);
            cifInvariants.add(cifInvariant);
        } else {
            InvKind cifInvKind = translateInvKind(invKind.get());

            for (String event: events) {
                Invariant cifInvariant = CifConstructors.newInvariant();
                cifInvariant.setInvKind(cifInvKind);
                cifInvariant.setPredicate(EcoreUtil.copy(cifPredicate));
                cifInvariant.setSupKind(SupKind.REQUIREMENT);
                cifInvariants.add(cifInvariant);

                Event cifEvent = eventMap.entrySet().stream().filter(e -> e.getKey().getName().equals(event))
                        .map(Entry::getValue).findFirst().get();
                EventExpression cifEventExpr = CifConstructors.newEventExpression(cifEvent, null,
                        CifConstructors.newBoolType());
                cifInvariant.setEvent(cifEventExpr);
            }
        }

        return cifInvariants;
    }

    private org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator translateOperator(BinaryOperator operator) {
        return switch (operator) {
            case AND -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.CONJUNCTION;
            case EQ -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.EQUAL;
            case GE -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.GREATER_EQUAL;
            case GT -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.GREATER_THAN;
            case LE -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.LESS_EQUAL;
            case LT -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.LESS_THAN;
            case MINUS -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.SUBTRACTION;
            case NE -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.UNEQUAL;
            case OR -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.DISJUNCTION;
            case PLUS -> org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.ADDITION;
        };
    }

    private org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator translateOperator(UnaryOperator operator) {
        return switch (operator) {
            case MINUS -> org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator.NEGATE;
            case NOT -> org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator.INVERSE;
        };
    }

    private InvKind translateInvKind(String invKind) {
        if (invKind.equals("disables")) {
            return InvKind.EVENT_DISABLES;
        } else if (invKind.equals("needs")) {
            return InvKind.EVENT_NEEDS;
        } else {
            throw new RuntimeException("Unsupported invariant kind: " + invKind);
        }
    }
}
