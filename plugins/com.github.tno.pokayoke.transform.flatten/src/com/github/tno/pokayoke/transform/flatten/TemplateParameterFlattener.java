
package com.github.tno.pokayoke.transform.flatten;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AElifExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIfExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.TemplateSignature;

import com.github.tno.synthml.uml.profile.cif.ACifObjectToString;
import com.github.tno.synthml.uml.profile.cif.CifParserHelper;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

/** Template parameter flattener. */
public class TemplateParameterFlattener {
    private TemplateParameterFlattener() {
    }

    /**
     * Unfolds guards and effects of a redefinable element.
     *
     * @param element The redefinable element.
     * @param nameToArgument The mapping of template parameter names to concrete arguments.
     */
    private static void unfoldRedefinableElement(RedefinableElement element, Map<String, AExpression> nameToArgument) {
        // Perform the guard unfolding. Skip if there is no guard.
        AExpression guardExpr = CifParserHelper.parseGuard(element);
        if (guardExpr != null) {
            AExpression newGuard = unfoldAExpression(guardExpr, nameToArgument);
            String newGuardString = ACifObjectToString.toString(newGuard);
            PokaYokeUmlProfileUtil.setGuard(element, newGuardString);
        }

        // Perform the unfolding of the effects.
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        List<String> newEffects = new ArrayList<>();
        for (String effect: effects) {
            List<AUpdate> updates = CifParserHelper.parseUpdates(effect, element);
            String newEffect = updates.stream().flatMap(update -> unfoldAUpdate(update, nameToArgument).stream())
                    .map(newUpdate -> ACifObjectToString.toString(newUpdate)).collect(Collectors.joining(", "));
            newEffects.add(newEffect);
        }
        PokaYokeUmlProfileUtil.setEffects(element, newEffects);
    }

    /**
     * Unfolds a CIF {@link AExpression}: replaces template parameters with the given arguments.
     *
     * @param expression A CIF {@link AExpression} to be unfolded.
     * @param nameToArgument The mapping of template parameter names to concrete arguments.
     * @return The unfolded CIF {@link AExpression}.
     */
    private static AExpression unfoldAExpression(AExpression expression, Map<String, AExpression> nameToArgument) {
        if (expression instanceof ABinaryExpression binExpr) {
            AExpression unfoldedLhsExpression = unfoldAExpression(binExpr.left, nameToArgument);
            AExpression unfoldedRhsExpression = unfoldAExpression(binExpr.right, nameToArgument);

            // Combine the unfolded left and right components to form a new binary expression.
            return new ABinaryExpression(binExpr.operator, unfoldedLhsExpression, unfoldedRhsExpression,
                    expression.position);
        } else if (expression instanceof AUnaryExpression unaryExpr) {
            return new AUnaryExpression(unaryExpr.operator, unfoldAExpression(unaryExpr.child, nameToArgument),
                    unaryExpr.position);
        } else if (expression instanceof ANameExpression nameExpr) {
            return nameToArgument.getOrDefault(nameExpr.name.name, nameExpr);
        } else if (expression instanceof ABoolExpression || expression instanceof AIntExpression) {
            // Expressions without children don't need unfolding.
            return expression;
        } else if (expression instanceof AIfExpression ifExpr) {
            List<AExpression> guardExprs = ifExpr.guards.stream().map(guard -> unfoldAExpression(guard, nameToArgument))
                    .toList();
            AExpression thenExpr = unfoldAExpression(ifExpr.then, nameToArgument);
            List<AElifExpression> elifExprs = ifExpr.elifs.stream()
                    .map(elif -> unfoldAElifExpression(elif, nameToArgument)).toList();
            AExpression elseExpr = unfoldAExpression(ifExpr.elseExpr, nameToArgument);
            return new AIfExpression(guardExprs, thenExpr, elifExprs, elseExpr, ifExpr.position);
        } else {
            throw new RuntimeException(String.format("Unfolding expressions of class '%s' is not supported.",
                    expression.getClass().getSimpleName()));
        }
    }

    private static AElifExpression unfoldAElifExpression(AElifExpression elifExpr,
            Map<String, AExpression> nameToArgument)
    {
        List<AExpression> guardExprs = elifExpr.guards.stream().map(guard -> unfoldAExpression(guard, nameToArgument))
                .toList();
        AExpression thenExpr = unfoldAExpression(elifExpr.then, nameToArgument);
        return new AElifExpression(guardExprs, thenExpr, elifExpr.position);
    }

    /**
     * Unfolds a CIF {@link AUpdate}: Replaces template parameters with the given arguments.
     *
     * @param update A CIF {@link AUpdate} to be unfolded.
     * @param nameToArgument The mapping of template parameter names to concrete arguments.
     * @return The list containing the unfolded CIF {@link AUpdate}.
     */
    private static List<AUpdate> unfoldAUpdate(AUpdate update, Map<String, AExpression> nameToArgument) {
        if (update instanceof AAssignmentUpdate assign) {
            return unfoldAAssignmentUpdate(assign, nameToArgument);
        } else if (update instanceof AIfUpdate ifUpdate) {
            AUpdate newIfUpdate = unfoldAIfUpdate(ifUpdate, nameToArgument);
            return List.of(newIfUpdate);
        } else {
            throw new RuntimeException(
                    String.format("Unfolding updates of class '%s' not supported.", update.getClass().getSimpleName()));
        }
    }

    /**
     * Unfolds a CIF {@link AAssignmentUpdate}: Replaces template parameters with the given arguments.
     *
     * @param assignUpdate A CIF {@link AAssignmentUpdate} to be unfolded.
     * @param nameToArgument The mapping of template parameter names to concrete arguments.
     * @return The unfolded CIF {@link AAssignmentUpdate}.
     */
    private static List<AUpdate> unfoldAAssignmentUpdate(AAssignmentUpdate assignUpdate,
            Map<String, AExpression> nameToArgument)
    {
        // Sanity check: 'addressable' must be a name expression which must not be a parameter.
        Verify.verify(assignUpdate.addressable instanceof ANameExpression adressable
                && !nameToArgument.containsKey(adressable.name.name));

        return List.of(new AAssignmentUpdate(assignUpdate.addressable,
                unfoldAExpression(assignUpdate.value, nameToArgument), assignUpdate.position));
    }

    /**
     * Unfolds a CIF {@link AIfUpdate}: Replaces template parameters with the given arguments.
     *
     * @param ifUpdate A CIF {@link AIfUpdate} to be unfolded.
     * @param nameToArgument The mapping of template parameter names to concrete arguments.
     * @return The unfolded CIF {@link AIfUpdate}.
     */
    private static AUpdate unfoldAIfUpdate(AIfUpdate ifUpdate, Map<String, AExpression> nameToArgument) {
        // Process the 'if' statements.
        List<AExpression> unfoldedGuards = ifUpdate.guards.stream().map(u -> unfoldAExpression(u, nameToArgument))
                .toList();

        // Process the 'then' statements.
        List<AUpdate> unfoldedThens = ifUpdate.thens.stream().flatMap(u -> unfoldAUpdate(u, nameToArgument).stream())
                .toList();

        // Process the 'elif' statements.
        List<AElifUpdate> unfoldedElifs = ifUpdate.elifs.stream().map(u -> unfoldAElifUpdate(u, nameToArgument))
                .toList();

        // Process the 'else' statements.
        List<AUpdate> unfoldedElses = ifUpdate.elses.stream().flatMap(u -> unfoldAUpdate(u, nameToArgument).stream())
                .toList();

        return new AIfUpdate(unfoldedGuards, unfoldedThens, unfoldedElifs, unfoldedElses, ifUpdate.position);
    }

    private static AElifUpdate unfoldAElifUpdate(AElifUpdate elifUpdate, Map<String, AExpression> nameToArgument) {
        // Process the 'guards'.
        List<AExpression> unfoldedElifGuards = elifUpdate.guards.stream().map(u -> unfoldAExpression(u, nameToArgument))
                .toList();

        // Process the 'thens'.
        List<AUpdate> unfoldedElifThens = elifUpdate.thens.stream()
                .flatMap(u -> unfoldAUpdate(u, nameToArgument).stream()).toList();

        return new AElifUpdate(unfoldedElifGuards, unfoldedElifThens, elifUpdate.position);
    }

    public static void unfoldActivity(Activity activity, CallBehaviorAction callBehaviorActionToReplace) {
        List<AAssignmentUpdate> activityArguments = CifParserHelper.parseArguments(callBehaviorActionToReplace);
        Map<String, AExpression> nameToArgument = activityArguments.stream().collect(
                Collectors.toMap(update -> ((ANameExpression)update.addressable).name.name, update -> update.value));

        // Unfold the guards and effects of owned elements.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                // Get the incoming and outgoing guards, unfold them, and substitute the corresponding string.
                AExpression incomingGuard = CifParserHelper.parseIncomingGuard(controlEdge);
                if (incomingGuard != null) {
                    AExpression unfoldedIncoming = unfoldAExpression(incomingGuard, nameToArgument);
                    PokaYokeUmlProfileUtil.setIncomingGuard(controlEdge, ACifObjectToString.toString(unfoldedIncoming));
                }

                AExpression outgoingGuard = CifParserHelper.parseOutgoingGuard(controlEdge);
                if (outgoingGuard != null) {
                    AExpression unfoldedOutgoing = unfoldAExpression(outgoingGuard, nameToArgument);
                    PokaYokeUmlProfileUtil.setOutgoingGuard(controlEdge, ACifObjectToString.toString(unfoldedOutgoing));
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                if (PokaYokeUmlProfileUtil.isFormalElement(callBehavior)) {
                    // Shadowed call, process guards and effects of the call behavior.
                    unfoldRedefinableElement(callBehavior, nameToArgument);
                }
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                unfoldRedefinableElement(internalAction, nameToArgument);
            } else if (ownedElement instanceof Constraint) {
                // Constraints cannot be parameterized.
                continue;
            } else if (ownedElement instanceof ActivityNode) {
                // Nodes in activities should not refer to properties.
                continue;
            } else if (ownedElement instanceof TemplateSignature) {
                // Template signatures of parameterized activities should not refer to properties.
                continue;
            } else if (ownedElement instanceof Comment) {
                // Comments are ignored by this translator.
                continue;
            } else {
                throw new RuntimeException(String.format("Unfolding elements of class '%s' not supported",
                        ownedElement.getClass().getSimpleName()));
            }
        }
    }
}
