
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.metamodel.cif.InvKind;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.ElifExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EnumLiteralExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IfExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifInvariantParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AElifExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIfExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;

import com.google.common.base.Preconditions;

public class CifAnnotatedUml2CifTranslator extends Uml2CifTranslator {
    private final String inputPath;

    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final CifInvariantParser invariantParser = new CifInvariantParser();

    private final CifUpdateParser updateParser = new CifUpdateParser();

    public CifAnnotatedUml2CifTranslator(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public Expression parseExpression(String expr) {
        return parseExpression(expressionParser.parseString(expr, inputPath));
    }

    public Expression parseExpression(AExpression expr) {
        if (expr instanceof ABinaryExpression binExpr) {
            return parseBinaryExpression(binExpr);
        } else if (expr instanceof ABoolExpression boolExpr) {
            return parseBooleanExpression(boolExpr);
        } else if (expr instanceof AIfExpression ifExpr) {
            return parseIfExpression(ifExpr);
        } else if (expr instanceof ANameExpression nameExpr) {
            return parseNameExpression(nameExpr);
        } else if (expr instanceof AUnaryExpression unaryExpr) {
            return parseUnaryExpression(unaryExpr);
        } else {
            throw new RuntimeException("Unsupported expression: " + expr);
        }
    }

    public BinaryExpression parseBinaryExpression(ABinaryExpression expr) {
        BinaryExpression cifExpr = CifConstructors.newBinaryExpression();
        cifExpr.setLeft(parseExpression(expr.left));
        cifExpr.setOperator(parseBinaryOperator(expr.operator));
        cifExpr.setRight(parseExpression(expr.right));
        cifExpr.setType(CifConstructors.newBoolType());
        return cifExpr;
    }

    public BinaryOperator parseBinaryOperator(String operator) {
        return switch (operator) {
            case "and" -> BinaryOperator.CONJUNCTION;
            case "or" -> BinaryOperator.DISJUNCTION;
            case "=" -> BinaryOperator.EQUAL;
            default -> throw new RuntimeException("Unsupported binary operator: " + operator);
        };
    }

    public BoolExpression parseBooleanExpression(ABoolExpression boolExpr) {
        return createBoolExpression(boolExpr.value);
    }

    public IfExpression parseIfExpression(AIfExpression expr) {
        IfExpression ifExpr = CifConstructors.newIfExpression();
        ifExpr.getElifs().addAll(expr.elifs.stream().map(this::parseElifExpression).toList());
        ifExpr.getGuards().addAll(expr.guards.stream().map(this::parseExpression).toList());
        ifExpr.setElse(parseExpression(expr.elseExpr));
        ifExpr.setThen(parseExpression(expr.then));
        ifExpr.setType(EcoreUtil.copy(ifExpr.getThen().getType()));
        return ifExpr;
    }

    public ElifExpression parseElifExpression(AElifExpression expr) {
        ElifExpression elifExpr = CifConstructors.newElifExpression();
        elifExpr.getGuards().addAll(expr.guards.stream().map(this::parseExpression).toList());
        elifExpr.setThen(parseExpression(expr.then));
        return elifExpr;
    }

    public Expression parseNameExpression(ANameExpression nameExpr) {
        // Try to parse the given expression as an enumeration literal.
        for (Entry<EnumerationLiteral, EnumLiteral> entry: enumLiteralMap.entrySet()) {
            EnumerationLiteral umlLiteral = entry.getKey();

            if (umlLiteral.getLabel().equals(nameExpr.name.name)) {
                EnumLiteralExpression cifExpr = CifConstructors.newEnumLiteralExpression();
                cifExpr.setLiteral(entry.getValue());
                cifExpr.setType(translateEnumerationType(umlLiteral.getEnumeration()));
                return cifExpr;
            }
        }

        // Try to parse the given expression as a discrete variable.
        for (Entry<Property, DiscVariable> entry: variableMap.entrySet()) {
            Property umlProperty = entry.getKey();

            if (umlProperty.getLabel().equals(nameExpr.name.name)) {
                DiscVariable cifVariable = entry.getValue();
                DiscVariableExpression cifExpr = CifConstructors.newDiscVariableExpression();
                cifExpr.setType(EcoreUtil.copy(cifVariable.getType()));
                cifExpr.setVariable(cifVariable);
                return cifExpr;
            }
        }

        throw new RuntimeException("Unsupported name expression: " + nameExpr);
    }

    public UnaryExpression parseUnaryExpression(AUnaryExpression expr) {
        UnaryExpression cifExpr = CifConstructors.newUnaryExpression();
        cifExpr.setChild(parseExpression(expr.child));
        cifExpr.setOperator(parseUnaryOperator(expr.operator));
        cifExpr.setType(CifConstructors.newBoolType());
        return cifExpr;
    }

    public UnaryOperator parseUnaryOperator(String operator) {
        return switch (operator) {
            case "not" -> UnaryOperator.INVERSE;
            default -> throw new RuntimeException("Unsupported unary operator: " + operator);
        };
    }

    @Override
    public Update parseUpdate(String update) {
        return parseUpdate(updateParser.parseString(update, inputPath));
    }

    public Update parseUpdate(AUpdate update) {
        if (update instanceof AAssignmentUpdate assignmentUpdate) {
            return parseAssignmentUpdate(assignmentUpdate);
        } else {
            throw new RuntimeException("Unsupported update: " + update);
        }
    }

    public Assignment parseAssignmentUpdate(AAssignmentUpdate update) {
        Assignment cifAssignment = CifConstructors.newAssignment();
        cifAssignment.setAddressable(parseExpression(update.addressable));
        cifAssignment.setValue(parseExpression(update.value));
        return cifAssignment;
    }

    @Override
    public Invariant parseInvariant(String invariant) {
        return parseInvariant(invariantParser.parseString(invariant, inputPath));
    }

    public Invariant parseInvariant(AInvariant invariant) {
        Invariant cifInvariant = CifConstructors.newInvariant();

        // Parse the invariant kind.
        if (invariant.invKind == null) {
            cifInvariant.setInvKind(InvKind.STATE);
        } else if (invariant.invKind.text.equals("needs")) {
            cifInvariant.setInvKind(InvKind.EVENT_NEEDS);
        } else if (invariant.invKind.text.equals("disables")) {
            cifInvariant.setInvKind(InvKind.EVENT_DISABLES);
        } else {
            throw new RuntimeException("Unsupported invariant kind: " + invariant.invKind);
        }

        if (invariant.events != null) {
            // Parse the event. For now only a single event is supported.
            Preconditions.checkArgument(invariant.events.size() == 1, "Expected exactly one event.");
            String eventName = invariant.events.get(0).name;
            List<Event> events = eventMap.entrySet().stream().filter(e -> e.getKey().getLabel().equals(eventName))
                    .map(Entry::getValue).toList();
            Preconditions.checkArgument(events.size() == 1,
                    "Expected to find exactly one event named '" + eventName + "', but found '" + events.size() + "'.");
            Event event = events.get(0);

            EventExpression eventExpr = CifConstructors.newEventExpression();
            eventExpr.setEvent(event);
            eventExpr.setType(CifConstructors.newBoolType());
            cifInvariant.setEvent(eventExpr);
        }

        cifInvariant.setPredicate(parseExpression(invariant.predicate));
        cifInvariant.setSupKind(SupKind.REQUIREMENT);

        return cifInvariant;
    }
}
