
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.List;
import java.util.stream.Collectors;

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

/** Translates a CIF object expression into the corresponding string. */
public class ACifObjectToString {
    private ACifObjectToString() {
    }

    public static String toString(ACifObject object) {
        if (object instanceof AExpression expr) {
            return toString(expr);
        } else if (object instanceof AUpdate update) {
            return toString(update);
        } else if (object instanceof AElifUpdate elifUpdate) {
            return toString(elifUpdate);
        } else if (object instanceof AInvariant invariant) {
            return toString(invariant);
        } else {
            throw new RuntimeException(String.format("Unsupported CIF object class: %s.", object.getClass()));
        }
    }

    public static String toString(AExpression expression) {
        if (expression instanceof ANameExpression nameExpr) {
            return nameExpr.name.name;
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

    public static String toString(AUpdate update) {
        if (update instanceof AAssignmentUpdate assign) {
            return toString(assign.addressable) + " := " + toString(assign.value);
        } else if (update instanceof AIfUpdate ifUpdate) {
            return "if "
                    + ifUpdate.guards.stream().map(u -> ACifObjectToString.toString(u)).collect(Collectors.joining(","))
                    + " : "
                    + ifUpdate.thens.stream().map(u -> ACifObjectToString.toString(u)).collect(Collectors.joining(","))
                    + (ifUpdate.elifs.isEmpty() ? ""
                            : ifUpdate.elifs.stream().map(u -> ACifObjectToString.toString(u))
                                    .collect(Collectors.joining(",")))
                    + (ifUpdate.elses.isEmpty() ? "" : " else " + ifUpdate.elses.stream()
                            .map(u -> ACifObjectToString.toString(u)).collect(Collectors.joining(",")))
                    + " end";
        } else {
            throw new RuntimeException(String.format("Unsupported update class %s.", update.getClass()));
        }
    }

    public static String toString(AElifUpdate elifUpdate) {
        return " elif "
                + elifUpdate.guards.stream().map(u -> ACifObjectToString.toString(u)).collect(Collectors.joining(","))
                + " : "
                + elifUpdate.thens.stream().map(u -> ACifObjectToString.toString(u)).collect(Collectors.joining(","));
    }

    public static String toString(AInvariant invariant) {
        // Translate the name, if any.
        String nameString = (invariant.name != null) ? invariant.name.id + ": " : "";

        // Translate the predicates.
        String predicateString = toString(invariant.predicate);

        // Translate the events. If more than one, add curly brackets.
        List<String> invEventsNames = invariant.events.stream().map(e -> e.name).collect(Collectors.toList());
        String joinedEvents = (invEventsNames.size() > 1) ? "{ " + String.join(", ", invEventsNames) + " }"
                : invEventsNames.get(0);

        // Get the string for the invariant type.
        String invKindString = (invariant.invKind == null) ? "" : invariant.invKind.text;

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
