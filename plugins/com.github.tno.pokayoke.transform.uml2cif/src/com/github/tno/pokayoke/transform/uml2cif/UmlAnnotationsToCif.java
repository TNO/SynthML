
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.emf.ecore.util.EcoreUtil;
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
import org.eclipse.escet.cif.metamodel.cif.expressions.SetExpression;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.IntType;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;

import com.github.tno.pokayoke.uml.profile.cif.ACifObjectWalker;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;

/** Translates UML annotations like guards and effects in UML synthesis specifications to CIF. */
public class UmlAnnotationsToCif extends ACifObjectWalker<PositionObject> {
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
    public Invariant translate(AInvariant invariant) {
        return (Invariant)visit(invariant, context);
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
    protected PositionObject visit(PositionObject addressable, TextPosition assignmentPos, PositionObject value,
            CifContext ctx)
    {
        return CifConstructors.newAssignment((Expression)addressable, null, (Expression)value);
    }

    @Override
    protected PositionObject visit(BinaryOperator operator, TextPosition operatorPos, PositionObject left,
            PositionObject right, CifContext ctx)
    {
        return CifConstructors.newBinaryExpression((Expression)left, translateOperator(operator), null,
                (Expression)right, CifConstructors.newBoolType());
    }

    @Override
    protected PositionObject visit(EnumerationLiteral literal, TextPosition literalPos, CifContext ctx) {
        return CifConstructors.newEnumLiteralExpression(enumLiteralMap.get(literal), null,
                translateType(literal.getEnumeration()));
    }

    @Override
    protected Expression visit(Property property, TextPosition propertyPos, CifContext ctx) {
        DiscVariable cifVariable = variableMap.get(property);
        return CifConstructors.newDiscVariableExpression(null, EcoreUtil.copy(cifVariable.getType()), cifVariable);
    }

    @Override
    protected Expression visit(UnaryOperator operator, TextPosition operatorPos, PositionObject child, CifContext ctx) {
        return CifConstructors.newUnaryExpression((Expression)child, translateOperator(operator), null, null);
    }

    @Override
    protected Expression visit(ABoolExpression expr, CifContext ctx) {
        return CifConstructors.newBoolExpression(null, CifConstructors.newBoolType(), expr.value);
    }

    @Override
    protected Expression visit(AIntExpression expr, CifContext ctx) {
        int value = Integer.parseInt(expr.value);
        IntType type = CifConstructors.newIntType(value, null, value);
        return CifConstructors.newIntExpression(null, type, value);
    }

    @Override
    protected PositionObject visit(Optional<String> invKind, List<String> events, TextPosition operatorPos,
            PositionObject predicate, CifContext ctx)
    {
        Invariant cifInvariant = CifConstructors.newInvariant();
        cifInvariant.setInvKind(invKind.map(this::translateInvKind).orElse(InvKind.STATE));
        cifInvariant.setPredicate((Expression)predicate);
        cifInvariant.setSupKind(SupKind.REQUIREMENT);

        if (!events.isEmpty()) {
            SetExpression cifSetExpr = CifConstructors.newSetExpression();
            cifSetExpr.setType(CifConstructors.newBoolType());
            cifInvariant.setEvent(cifSetExpr);

            for (String event: events) {
                Event cifEvent = eventMap.entrySet().stream().filter(e -> e.getKey().getName().equals(event))
                        .map(Entry::getValue).findFirst().get();
                EventExpression cifEventExpr = CifConstructors.newEventExpression(cifEvent, null,
                        CifConstructors.newBoolType());
                cifSetExpr.getElements().add(cifEventExpr);
            }
        }

        return cifInvariant;
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
