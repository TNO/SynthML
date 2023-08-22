
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

/** Translates basic CIF expressions and updates to Python. */
public class CifToPythonTranslator {
    private final ModelTyping modelTyping;

    public CifToPythonTranslator(ModelTyping modelTyping) {
        this.modelTyping = modelTyping;
    }

    public String translateAssignmentUpdate(AAssignmentUpdate update) {
        return String.format("%s = %s", translateExpression(update.addressable), translateExpression(update.value));
    }

    public String translateExpressions(Collection<AExpression> exprs) {
        String pythonExp = exprs.stream().map(e -> "(" + translateExpression(e) + ")")
                .collect(Collectors.joining(" and "));

        if (pythonExp.isEmpty()) {
            pythonExp = "True";
        }

        return pythonExp;
    }

    public String translateExpression(AExpression expr) {
        if (expr instanceof ABinaryExpression) {
            return translateBinaryExpression((ABinaryExpression)expr);
        } else if (expr instanceof ABoolExpression) {
            return translateBoolExpression((ABoolExpression)expr);
        } else if (expr instanceof ANameExpression) {
            return translateNameExpression((ANameExpression)expr);
        } else if (expr instanceof AUnaryExpression) {
            return translateUnaryExpression((AUnaryExpression)expr);
        } else {
            throw new RuntimeException("Unsupported expression: " + expr);
        }
    }

    public String translateBinaryExpression(ABinaryExpression expr) {
        return String.format("(%s) %s (%s)", translateExpression(expr.left), translateOperator(expr.operator),
                translateExpression(expr.right));
    }

    public String translateBoolExpression(ABoolExpression expr) {
        return expr.value ? "True" : "False";
    }

    public String translateNameExpression(ANameExpression expr) {
        String name = expr.name.name;

        if (modelTyping.isEnumerationLiteral(name)) {
            return "'" + name + "'";
        } else if (modelTyping.isVariable(name)) {
            return name;
        } else {
            throw new RuntimeException("Unsupported name expression.");
        }
    }

    public String translateOperator(String operator) {
        if (operator.equals("and") || operator.equals("or") || operator.equals("not")) {
            return operator;
        } else if (operator.equals("=")) {
            return "==";
        } else {
            throw new RuntimeException("Unsupported operator.");
        }
    }

    public String translateUnaryExpression(AUnaryExpression expr) {
        return String.format("%s (%s)", translateOperator(expr.operator), translateExpression(expr.child));
    }

    public String translateUpdate(AUpdate update) {
        if (update instanceof AAssignmentUpdate) {
            return translateAssignmentUpdate((AAssignmentUpdate)update);
        } else {
            throw new RuntimeException("Unsupported update.");
        }
    }
}
