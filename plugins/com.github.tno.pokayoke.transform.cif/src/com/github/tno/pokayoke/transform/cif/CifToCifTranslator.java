
package com.github.tno.pokayoke.transform.cif;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignment;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newUnaryExpression;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
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

/** Translates CIF expression texts into CIF expressions. */
public class CifToCifTranslator {
    private final DataStore dataStore;

    public CifToCifTranslator(DataStore nameMapping) {
        this.dataStore = nameMapping;
    }

    public Expression translateExpression(AExpression expr) {
        if (expr instanceof ABinaryExpression binExpr) {
            return translateBinaryExpression(binExpr);
        } else if (expr instanceof ABoolExpression boolExpr) {
            return translateBoolExpression(boolExpr);
        } else if (expr instanceof ANameExpression nameExpr) {
            return translateNameExpression(nameExpr);
        } else if (expr instanceof AUnaryExpression unaryExpr) {
            return translateUnaryExpression(unaryExpr);
        } else {
            throw new RuntimeException("Unsupported expression: " + expr);
        }
    }

    private Expression translateBinaryExpression(ABinaryExpression expr) {
        return newBinaryExpression(translateExpression(expr.left), translateBinaryOperator(expr.operator), null,
                translateExpression(expr.right), newBoolType());
    }

    private BinaryOperator translateBinaryOperator(String operator) {
        return switch (operator) {
            case "and" -> BinaryOperator.CONJUNCTION;
            case "or" -> BinaryOperator.DISJUNCTION;
            case "=" -> BinaryOperator.EQUAL;
            default -> throw new RuntimeException("Unsupported operator: " + operator);
        };
    }

    private UnaryOperator translateUnaryOperator(String operator) {
        return switch (operator) {
            case "not" -> UnaryOperator.INVERSE;
            default -> throw new RuntimeException("Unsupported operator: " + operator);
        };
    }

    private BoolExpression translateBoolExpression(ABoolExpression expr) {
        return expr.value ? newBoolExpression(null, newBoolType(), true)
                : newBoolExpression(null, newBoolType(), false);
    }

    private Expression translateNameExpression(ANameExpression expr) {
        Preconditions.checkArgument(!expr.derivative, "Expected a non-derivative name expression.");

        String name = expr.name.name;

        if (dataStore.isEnumerationLiteral(name)) {
            EnumLiteral enumLiteral = dataStore.getEnumerationLiteral(name);
            EnumDecl enumDecl = dataStore.getEnumeration(enumLiteral);
            return newEnumLiteralExpression(enumLiteral, null, newEnumType(enumDecl, null));
        } else if (dataStore.isVariable(name)) {
            return newDiscVariableExpression(null, EcoreUtil.copy(dataStore.getVariable(name).getType()),
                    dataStore.getVariable(name));
        } else {
            throw new RuntimeException("Unsupported name expression: " + expr);
        }
    }

    private Expression translateUnaryExpression(AUnaryExpression expr) {
        return newUnaryExpression(translateExpression(expr.child), translateUnaryOperator(expr.operator), null,
                newBoolType());
    }

    public Assignment translateUpdate(AUpdate update) {
        if (update instanceof AAssignmentUpdate assignmentUpdate) {
            return translateAssignmentUpdate(assignmentUpdate);
        } else {
            throw new RuntimeException("Unsupported update: " + update);
        }
    }

    private Assignment translateAssignmentUpdate(AAssignmentUpdate update) {
        return newAssignment(translateExpression(update.addressable), null, translateExpression(update.value));
    }
}
