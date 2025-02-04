
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.AInvariant;
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
import org.eclipse.escet.cif.parser.ast.tokens.AName;

/** Translates a CIF object expression into the corresponding string. */
public class ACifObjectToString {
    private ACifObjectToString() {
    }

    public static String toString(ACifObject expression) {
        if (expression instanceof AExpression aExpr) {
            return aExpressionToString(aExpr);
        } else if (expression instanceof AUpdate update) {
            return aUpdateToString(update);
        } else if (expression instanceof AElifUpdate elifUpdate) {
            return aElifUpdateToString(elifUpdate);
        } else if (expression instanceof AInvariant invariant) {
            return aInvariantToString(invariant);
        } else {
            throw new RuntimeException(String.format("Unsupported Cif object class: %s.", expression.getClass()));
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

    public static String aUpdateToString(AUpdate update) {
        if (update instanceof AAssignmentUpdate assign) {
            return toString(assign.addressable) + " := " + toString(assign.value);
        } else if (update instanceof AIfUpdate ifUpdate) {
            // Translate to string the 'if' guards.
            List<String> ifStrings = new LinkedList<>();
            for (AExpression guard: ifUpdate.guards) {
                ifStrings.add(toString(guard));
            }
            String unfoldedIfs = String.join(", ", ifStrings);

            // Translate to string the 'then' updates.
            List<String> thenStrings = new LinkedList<>();
            for (AUpdate then: ifUpdate.thens) {
                thenStrings.add(toString(then));
            }
            String unfoldedThens = String.join(", ", thenStrings);

            // Translate to string the 'elif' updates.
            List<String> elifStrings = new LinkedList<>();
            for (AElifUpdate elif: ifUpdate.elifs) {
                elifStrings.add(toString(elif));
            }
            String unfoldedElifs = String.join(" ", elifStrings);

            // Translate to string the 'else' updates.
            List<String> elseStrings = new LinkedList<>();
            for (AUpdate elseUpdate: ifUpdate.elses) {
                elseStrings.add(toString(elseUpdate));
            }
            String unfoldedElses = String.join(", ", elseStrings);

            // Compose the final string.
            return "if " + unfoldedIfs + " : " + unfoldedThens + unfoldedElifs + " else " + unfoldedElses + " end";
        } else {
            throw new RuntimeException(String.format("Unsupported update class %s.", update.getClass()));
        }
    }

    public static String aElifUpdateToString(AElifUpdate elifExpr) {
        // Translate the guards. Guards and thens are separated by commas if there are more than one.
        List<String> ifStrings = new LinkedList<>();
        for (AExpression guard: elifExpr.guards) {
            ifStrings.add(toString(guard));
        }
        String unfoldedIfs = String.join(", ", ifStrings);

        // Translate the 'thens' updates.
        List<String> thenStrings = new LinkedList<>();
        for (AUpdate then: elifExpr.thens) {
            thenStrings.add(toString(then));
        }
        String unfoldedThens = String.join(", ", thenStrings);
        return " elif " + unfoldedIfs + " : " + unfoldedThens;
    }

    public static String aInvariantToString(AInvariant invariant) {
        // Translate the name, if any.
        String nameString = invariant.name.id != null ? invariant.name.id + ": " : "";

        // Translate the predicates.
        String predicateString = aExpressionToString(invariant.predicate);

        // Translate the events. If more than one, add curly brackets.
        List<String> invEventStrings = new LinkedList<>();
        for (AName eventName: invariant.events) {
            invEventStrings.add(eventName.name);
        }
        String joinedEvents = invEventStrings.size() > 1 ? "{ " + String.join(", ", invEventStrings) + " }"
                : invEventStrings.get(0);

        // Get the string for the invariant type.
        String invKindString = invariant.invKind.toString();

        // Compose the final string.
        if (invariant.events.isEmpty()) {
            return nameString + predicateString;
        } else if (invKindString.equals("disables")) {
            return nameString + predicateString + " disables " + joinedEvents;
        } else if (invKindString.equals("needs")) {
            return nameString + joinedEvents + " needs " + predicateString;
        } else {
            throw new RuntimeException(String.format("Unsupported invariant class %s", invariant.getClass()));
        }
    }
}
