/**
 *
 */

package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignment;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newUnaryExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;

import com.google.common.base.Preconditions;

/** Translates basic CIF expressions and updates to Python. */
public class CifToCifTranslator {
    private final DataStore nameMapping;

    public CifToCifTranslator(DataStore nameMapping) {
        this.nameMapping = nameMapping;
    }

    public List<Object> translateExpressions(Collection<AExpression> exprs) {
        List<Object> expressions = new ArrayList<>();
        if (!exprs.isEmpty()) {
            expressions = exprs.stream().map(e -> translateExpression(e)).collect(Collectors.toList());
        }

        return expressions;
    }

    public Expression translateExpression(AExpression expr) {
        if (expr instanceof ABinaryExpression binExpr) {
            return translateBinaryExpression(binExpr);
        } else if (expr instanceof ABoolExpression boolExpr) {
            return translateBoolExpression(boolExpr);
        } else if (expr instanceof ANameExpression nameExpr) {
            // return null;
            return translateNameExpression(nameExpr);
        } else if (expr instanceof AUnaryExpression unaryExpr) {
            return translateUnaryExpression(unaryExpr);
        } else {
            throw new RuntimeException("Unsupported expression: " + expr);
        }
    }

    public Expression translateBinaryExpression(ABinaryExpression expr) {
        return newBinaryExpression(translateExpression(expr.left), translateBinaryOperator(expr.operator),
                null, translateExpression(expr.right), null);
    }

    public BinaryOperator translateBinaryOperator(String operator) {
        return switch (operator) {
            case "and" -> BinaryOperator.CONJUNCTION;
            case "or" -> BinaryOperator.DISJUNCTION;
            case "=" -> BinaryOperator.EQUAL;
            default -> throw new RuntimeException("Unsupported operator: " + operator);
        };
    }

    public UnaryOperator translateUnaryOperator(String operator) {
        return switch (operator) {
            case "not" -> UnaryOperator.INVERSE;
            default -> throw new RuntimeException("Unsupported operator: " + operator);
        };
    }

    public BoolExpression translateBoolExpression(ABoolExpression expr) {
        return expr.value ? newBoolExpression(null, newBoolType(), true)
                : newBoolExpression(null, newBoolType(), false);
    }

    public Expression translateNameExpression(ANameExpression expr) {
        Preconditions.checkArgument(!expr.derivative, "Expected a non-derivative name expression.");

        String name = expr.name.name;

        if (nameMapping.isEnumerationLiteral(name)) {
            return newEnumLiteralExpression(nameMapping.getEnumerationLiteral(name), null, null);
        } else if (nameMapping.isVariable(name)) {
            return newDiscVariableExpression(null, EcoreUtil.copy(nameMapping.getVariable(name).getType()),
                    nameMapping.getVariable(name));
        } else {
            throw new RuntimeException("Unsupported name expression: " + expr);
        }
    }

    public Expression translateUnaryExpression(AUnaryExpression expr) {
        return newUnaryExpression(translateExpression(expr.child), translateUnaryOperator(expr.operator), null, null);
    }

    public Assignment translateUpdate(AUpdate update) {
        if (update instanceof AAssignmentUpdate assignmentUpdate) {
            return translateAssignmentUpdate(assignmentUpdate);
        } else {
            throw new RuntimeException("Unsupported update: " + update);
        }
    }

    public Assignment translateAssignmentUpdate(AAssignmentUpdate update) {
        return newAssignment(translateExpression(update.addressable), null, translateExpression(update.value));
    }
}
