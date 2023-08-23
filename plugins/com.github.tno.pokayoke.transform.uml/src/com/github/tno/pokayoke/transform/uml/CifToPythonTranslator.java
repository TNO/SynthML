
package com.github.tno.pokayoke.transform.uml;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;

import com.google.common.base.Preconditions;

/** Translates basic CIF expressions and updates to Python. */
public class CifToPythonTranslator {
    private final ModelTyping modelTyping;

    public CifToPythonTranslator(ModelTyping modelTyping) {
        this.modelTyping = modelTyping;
    }

    public String translateExpressions(Collection<AExpression> exprs) {
        String pythonExp = "True";

        if (!exprs.isEmpty()) {
            pythonExp = exprs.stream().map(e -> "(" + translateExpression(e) + ")")
                    .collect(Collectors.joining(" and "));
        }

        return pythonExp;
    }

    public String translateExpression(AExpression expr) {
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

    public String translateBinaryExpression(ABinaryExpression expr) {
        return String.format("(%s) %s (%s)", translateExpression(expr.left), translateOperator(expr.operator),
                translateExpression(expr.right));
    }

    public String translateOperator(String operator) {
        return switch (operator) {
            case "and", "or", "not" -> operator;
            case "=" -> "==";
            default -> throw new RuntimeException("Unsupported operator: " + operator);
        };
    }

    public String translateBoolExpression(ABoolExpression expr) {
        return expr.value ? "True" : "False";
    }

    public String translateNameExpression(ANameExpression expr) {
        Preconditions.checkArgument(!expr.derivative, "Expected a non-derivative name expression.");

        String name = expr.name.name;

        if (modelTyping.isEnumerationLiteral(name)) {
            return "'" + name + "'";
        } else if (modelTyping.isVariable(name)) {
            return name;
        } else {
            throw new RuntimeException("Unsupported name expression: " + expr);
        }
    }

    public String translateUnaryExpression(AUnaryExpression expr) {
        return String.format("%s (%s)", translateOperator(expr.operator), translateExpression(expr.child));
    }

    public String translateUpdate(AUpdate update) {
        if (update instanceof AAssignmentUpdate assignmentUpdate) {
            return translateAssignmentUpdate(assignmentUpdate);
        } else {
            throw new RuntimeException("Unsupported update: " + update);
        }
    }

    public String translateAssignmentUpdate(AAssignmentUpdate update) {
        return String.format("%s = %s", translateExpression(update.addressable), translateExpression(update.value));
    }
}
