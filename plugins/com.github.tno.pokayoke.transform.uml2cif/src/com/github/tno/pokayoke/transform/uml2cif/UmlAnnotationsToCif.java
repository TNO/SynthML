
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifTypeUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.InvKind;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.ElifUpdate;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.ElifExpression;
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
import org.eclipse.escet.common.position.common.PositionUtils;
import org.eclipse.escet.common.position.metamodel.position.Position;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.Type;

import com.github.tno.synthml.uml.profile.cif.ACifObjectWalker;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.NamedTemplateParameter;
import com.github.tno.synthml.uml.profile.util.PokaYokeTypeUtil;
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

    /** The mapping from translated CIF start events to their corresponding UML elements for which they were created. */
    private final Map<Event, RedefinableElement> startEventMap;

    public UmlAnnotationsToCif(CifContext context, Map<Enumeration, EnumDecl> enumMap,
            Map<EnumerationLiteral, EnumLiteral> enumLiteralMap, Map<Property, DiscVariable> variableMap,
            Map<Event, RedefinableElement> startEventMap)
    {
        this.context = context;
        this.enumMap = enumMap;
        this.enumLiteralMap = enumLiteralMap;
        this.variableMap = variableMap;
        this.startEventMap = startEventMap;
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
     * @return The translated CIF invariants.
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
    protected Object visit(List<Object> guards, List<Object> thens, List<Object> elifs, List<Object> elses,
            TextPosition updatePos, CifContext ctx)
    {
        List<Expression> guardExprs = guards.stream().map(Expression.class::cast).toList();
        List<Update> thenUpdates = thens.stream().map(Update.class::cast).toList();
        List<ElifUpdate> elifUpdates = elifs.stream().map(ElifUpdate.class::cast).toList();
        List<Update> elseUpdates = elses.stream().map(Update.class::cast).toList();

        return CifConstructors.newIfUpdate(elifUpdates, elseUpdates, guardExprs, null, thenUpdates);
    }

    @Override
    protected Object visit(List<Object> guards, List<Object> thens, TextPosition updatePos, CifContext ctx) {
        List<Expression> guardExprs = guards.stream().map(Expression.class::cast).toList();
        List<Update> thenUpdates = thens.stream().map(Update.class::cast).toList();

        return CifConstructors.newElifUpdate(guardExprs, null, thenUpdates);
    }

    @Override
    protected Object visit(List<Object> guards, Object then, List<Object> elifs, Object _else,
            TextPosition expressionPos, CifContext ctx)
    {
        List<Expression> guardExprs = guards.stream().map(Expression.class::cast).toList();
        Expression thenExpr = (Expression)then;
        List<ElifExpression> elifExprs = elifs.stream().map(ElifExpression.class::cast).toList();
        Expression elseExpr = (Expression)_else;
        Position dummy = PositionUtils.createDummy(expressionPos.location, expressionPos.source);

        CifType combinedType = CifTypeUtils.mergeTypes(thenExpr.getType(), elseExpr.getType(), dummy);

        for (ElifExpression elif: elifExprs) {
            Expression childThen = elif.getThen();
            combinedType = CifTypeUtils.mergeTypes(combinedType, childThen.getType(), dummy);
        }

        return CifConstructors.newIfExpression(elifExprs, elseExpr, guardExprs, dummy, thenExpr, combinedType);
    }

    @Override
    protected Object visit(List<Object> guards, Object thenExpr, TextPosition expressionPos, CifContext ctx) {
        List<Expression> guardExprs = guards.stream().map(Expression.class::cast).toList();
        Expression thenUpdates = (Expression)thenExpr;

        Position dummy = PositionUtils.createDummy(expressionPos.location, expressionPos.source);

        return CifConstructors.newElifExpression(guardExprs, dummy, thenUpdates);
    }

    @Override
    protected Object visit(BinaryOperator operator, TextPosition operatorPos, Object left, Object right,
            CifContext ctx)
    {
        Expression leftExpr = (Expression)left;
        Expression rightExpr = (Expression)right;

        CifType type = switch (operator) {
            case AND, EQ, GE, GT, LE, LT, NE, OR -> CifConstructors.newBoolType();
            case MINUS -> typeForBinaryMinus((IntType)leftExpr.getType(), (IntType)rightExpr.getType());
            case PLUS -> typeForBinaryPlus((IntType)leftExpr.getType(), (IntType)rightExpr.getType());
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
    protected Expression visit(NamedTemplateParameter parameter, TextPosition parameterReferencePos, CifContext ctx) {
        throw new RuntimeException("Translating template parameters to CIF is not supported.");
    }

    @Override
    protected Expression visit(UnaryOperator operator, TextPosition operatorPos, Object child, CifContext ctx) {
        Expression childExpr = (Expression)child;

        CifType type = switch (operator) {
            case MINUS -> typeForUnaryMinus((IntType)childExpr.getType());
            case NOT -> CifConstructors.newBoolType();
        };

        return CifConstructors.newUnaryExpression(childExpr, translateOperator(operator), null, type);
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
                boolean found = false;
                for (var entry: startEventMap.entrySet()) {
                    RedefinableElement umlElement = entry.getValue();

                    if (umlElement.getName() != null && umlElement.getName().equals(event)) {
                        Event cifEvent = entry.getKey();

                        Invariant cifInvariant = CifConstructors.newInvariant();
                        cifInvariant.setInvKind(cifInvKind);
                        cifInvariant.setPredicate(EcoreUtil.copy(cifPredicate));
                        cifInvariant.setSupKind(SupKind.REQUIREMENT);
                        cifInvariants.add(cifInvariant);

                        EventExpression cifEventExpr = CifConstructors.newEventExpression(cifEvent, null,
                                CifConstructors.newBoolType());
                        cifInvariant.setEvent(cifEventExpr);

                        found = true;
                        break;
                    }
                }
                Verify.verify(found, "Could not find a UML element that matches the event: " + event);
            }
        }

        return cifInvariants;
    }

    static IntType typeForBinaryMinus(IntType leftType, IntType rightType) {
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

        return CifConstructors.newIntType((int)lower, null, (int)upper);
    }

    static IntType typeForBinaryPlus(IntType leftType, IntType rightType) {
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

        return CifConstructors.newIntType((int)lower, null, (int)upper);
    }

    static IntType typeForUnaryMinus(IntType childType) {
        Verify.verify(!CifTypeUtils.isRangeless(childType), "Expected integer types to have a range.");

        int lower = childType.getLower();
        int upper = childType.getUpper();

        if (lower == Integer.MIN_VALUE) {
            throw new RuntimeException("Unexpected error: possible integer type range overflow.");
        }

        return CifConstructors.newIntType(-upper, null, -lower);
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
