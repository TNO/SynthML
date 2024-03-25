
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
import org.eclipse.escet.cif.metamodel.cif.expressions.ElifExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IfExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifInvariantParser;
import org.eclipse.escet.cif.parser.CifUpdatesParser;
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

/** Translates annotated UML models to CIF specifications where CIF is also the annotation language. */
public class CifAnnotatedUml2CifTranslator extends Uml2CifTranslator {
    private final String inputPath;

    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final CifInvariantParser invariantParser = new CifInvariantParser();

    private final CifUpdatesParser updatesParser = new CifUpdatesParser();

    /**
     * Gives a translator for annotated UML models, with CIF as the annotation language.
     *
     * @param inputPath The location of the source UML file to be translated.
     */
    public CifAnnotatedUml2CifTranslator(String inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public Expression parseExpression(String expr) {
        return parseExpression(expressionParser.parseString(expr, inputPath));
    }

    @Override
    public Invariant parseInvariant(String invariant) {
        return parseInvariant(invariantParser.parseString(invariant, inputPath));
    }

    @Override
    public List<Update> parseUpdates(String updates) {
        return updatesParser.parseString(updates, inputPath).stream().map(this::parseUpdate).toList();
    }

    private Expression parseExpression(AExpression expr) {
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

    private BinaryExpression parseBinaryExpression(ABinaryExpression expr) {
        BinaryExpression cifExpr = CifConstructors.newBinaryExpression();
        cifExpr.setLeft(parseExpression(expr.left));
        cifExpr.setOperator(parseBinaryOperator(expr.operator));
        cifExpr.setRight(parseExpression(expr.right));
        cifExpr.setType(CifConstructors.newBoolType());
        return cifExpr;
    }

    private BinaryOperator parseBinaryOperator(String operator) {
        return switch (operator) {
            case "and" -> BinaryOperator.CONJUNCTION;
            case "or" -> BinaryOperator.DISJUNCTION;
            case "=" -> BinaryOperator.EQUAL;
            default -> throw new RuntimeException("Unsupported binary operator: " + operator);
        };
    }

    private BoolExpression parseBooleanExpression(ABoolExpression expr) {
        return createBoolExpression(expr.value);
    }

    private IfExpression parseIfExpression(AIfExpression expr) {
        IfExpression ifExpr = CifConstructors.newIfExpression();
        ifExpr.getElifs().addAll(expr.elifs.stream().map(this::parseElifExpression).toList());
        ifExpr.getGuards().addAll(expr.guards.stream().map(this::parseExpression).toList());
        ifExpr.setElse(parseExpression(expr.elseExpr));
        ifExpr.setThen(parseExpression(expr.then));
        ifExpr.setType(EcoreUtil.copy(ifExpr.getThen().getType()));
        return ifExpr;
    }

    private ElifExpression parseElifExpression(AElifExpression expr) {
        ElifExpression elifExpr = CifConstructors.newElifExpression();
        elifExpr.getGuards().addAll(expr.guards.stream().map(this::parseExpression).toList());
        elifExpr.setThen(parseExpression(expr.then));
        return elifExpr;
    }

    private Expression parseNameExpression(ANameExpression expr) {
        // Try to parse the expression as an enumeration literal.
        for (Entry<EnumerationLiteral, EnumLiteral> entry: enumLiteralMap.entrySet()) {
            EnumerationLiteral literal = entry.getKey();

            if (literal.getLabel().equals(expr.name.name)) {
                return CifConstructors.newEnumLiteralExpression(entry.getValue(), null,
                        translateEnumerationType(literal.getEnumeration()));
            }
        }

        // Try to parse the expression as a discrete variable.
        for (Entry<Property, DiscVariable> entry: variableMap.entrySet()) {
            Property property = entry.getKey();
            DiscVariable variable = entry.getValue();

            if (property.getLabel().equals(expr.name.name)) {
                return CifConstructors.newDiscVariableExpression(null, EcoreUtil.copy(variable.getType()), variable);
            }
        }

        throw new RuntimeException("Unsupported name expression: " + expr);
    }

    private UnaryExpression parseUnaryExpression(AUnaryExpression expr) {
        return CifConstructors.newUnaryExpression(parseExpression(expr.child), parseUnaryOperator(expr.operator), null,
                CifConstructors.newBoolType());
    }

    private UnaryOperator parseUnaryOperator(String operator) {
        return switch (operator) {
            case "not" -> UnaryOperator.INVERSE;
            default -> throw new RuntimeException("Unsupported unary operator: " + operator);
        };
    }

    private Update parseUpdate(AUpdate update) {
        if (update instanceof AAssignmentUpdate assignmentUpdate) {
            return parseAssignmentUpdate(assignmentUpdate);
        } else {
            throw new RuntimeException("Unsupported update: " + update);
        }
    }

    private Assignment parseAssignmentUpdate(AAssignmentUpdate update) {
        return CifConstructors.newAssignment(parseExpression(update.addressable), null, parseExpression(update.value));
    }

    private Invariant parseInvariant(AInvariant invariant) {
        Invariant cifInvariant = CifConstructors.newInvariant();
        cifInvariant.setPredicate(parseExpression(invariant.predicate));
        cifInvariant.setSupKind(SupKind.REQUIREMENT);

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

        // Parse the event. For now, invariants can have at most one invariant.
        if (invariant.events != null) {
            Preconditions.checkArgument(invariant.events.size() == 1, "Expected at most one event.");
            String eventName = invariant.events.get(0).name;
            Event event = eventMap.entrySet().stream().filter(e -> e.getKey().getLabel().equals(eventName))
                    .map(Entry::getValue).findFirst().get();
            cifInvariant.setEvent(CifConstructors.newEventExpression(event, null, CifConstructors.newBoolType()));
        }

        return cifInvariant;
    }
}
