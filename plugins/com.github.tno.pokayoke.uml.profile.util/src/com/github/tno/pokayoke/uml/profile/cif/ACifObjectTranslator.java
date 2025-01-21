/**
 *
 */

package com.github.tno.pokayoke.uml.profile.cif;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;

/**
 * Translates a Cif object expression into the corresponding string.
 */
public class ACifObjectTranslator {
    private ACifObjectTranslator() {
    }

    public static String toString(ACifObject expression) {
        if (expression instanceof AExpression aExpr) {
            return aExpressionToString(aExpr);
        } else if (expression instanceof AUpdate upExpr) {
            return aUpdateToString(upExpr);
        } else if (expression instanceof AElifUpdate elifExpr) {
            return aElifUpdateToString(elifExpr);
        } else {
            throw new RuntimeException(String.format("Unupported Cif class: %s.", expression.getClass()));
        }
    }

    public static String aExpressionToString(AExpression expression) {
        if (expression instanceof ANameExpression namExpr) {
            return namExpr.name.name;
        } else if (expression instanceof ABoolExpression boolExpr) {
            return boolExpr.value ? "true" : "false";
        } else if (expression instanceof ABinaryExpression binExpr) {
            return toString(binExpr.left) + " " + binExpr.operator.toString() + " " + toString(binExpr.right);
        } else if (expression instanceof AUnaryExpression unExpr) {
            return unExpr.operator.toString() + " " + toString(unExpr.child);
        } else if (expression instanceof AIntExpression intExpr) {
            return intExpr.value;
        } else {
            throw new RuntimeException(String.format("Unsupported expression class %s.", expression.getClass()));
        }
    }

    public static String aUpdateToString(AUpdate expression) {
        if (expression instanceof AAssignmentUpdate assignExpr) {
            return toString(assignExpr.addressable) + " := " + toString(assignExpr.value);
        } else if (expression instanceof AIfUpdate ifExpr) {
            // Translate to string the 'if' guards.
            List<String> ifStrings = new LinkedList<>();
            for (AExpression guard: ifExpr.guards) {
                ifStrings.add(toString(guard));
            }
            String unfoldedIfs = String.join(", ", ifStrings);

            // Translate to string the 'then' updates.
            List<String> thenStrings = new LinkedList<>();
            for (AUpdate then: ifExpr.thens) {
                thenStrings.add(toString(then));
            }
            String unfoldedThens = String.join(", ", thenStrings);

            // Translate to string the 'elif' updates.
            List<String> elifStrings = new LinkedList<>();
            for (AElifUpdate elif: ifExpr.elifs) {
                elifStrings.add(toString(elif));
            }
            String unfoldedElifs = String.join(" ", elifStrings);

            // Translate to string the 'else' updates.
            List<String> elseStrings = new LinkedList<>();
            for (AUpdate elseUpdate: ifExpr.elses) {
                elseStrings.add(toString(elseUpdate));
            }
            String unfoldedElses = String.join(", ", elseStrings);

            // Compose the final string.
            return "if " + unfoldedIfs + " : " + unfoldedThens + unfoldedElifs + " else " + unfoldedElses + " end";
        } else {
            throw new RuntimeException(String.format("Unsupported expression class %s.", expression.getClass()));
        }
    }

    public static String aElifUpdateToString(AElifUpdate elifExpr) {
        // Syntactically the AElifUpdate guards are separated by commas, equivalent to a conjunction; similarly for the
        // then updates.
        List<String> ifStrings = new LinkedList<>();
        for (AExpression guard: elifExpr.guards) {
            ifStrings.add(toString(guard));
        }
        String unfoldedIfs = String.join(", ", ifStrings);

        List<String> thenStrings = new LinkedList<>();
        for (AUpdate then: elifExpr.thens) {
            thenStrings.add(toString(then));
        }
        String unfoldedThens = String.join(", ", thenStrings);
        return " elif " + unfoldedIfs + " : " + unfoldedThens;
    }
}
