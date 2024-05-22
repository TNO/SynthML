
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;

import com.google.common.base.Preconditions;

/** Helper for adding guards to the incoming edges of opaque actions. */
public class OpaqueActionHelper {
    private static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private OpaqueActionHelper() {
    }

    /**
     * Add guards to the incoming edges of opaque actions.
     *
     * @param actionToGuard The map from the opaque actions to the CIF expressions of the guards.
     */
    public static void addGuardToIncomingEdges(Map<OpaqueAction, String> actionToGuard) {
        actionToGuard.forEach((action, expression) -> addGuardToSingleIncomingEdge(action, expression));
    }

    private static void addGuardToSingleIncomingEdge(OpaqueAction action, String expression) {
        List<ActivityEdge> incomingEdges = action.getIncomings();
        Preconditions.checkArgument(incomingEdges.size() == 1,
                String.format("Expected that opaque action %s has exactly one incoming edge.", action.getName()));
        ActivityEdge incomingEdge = incomingEdges.get(0);

        OpaqueExpression guard = FACTORY.createOpaqueExpression();
        guard.getBodies().add(expression);
        incomingEdge.setGuard(guard);
    }

    /**
     * Add guard strings to the bodies of opaque actions translated from events.
     *
     * @param activity The activity in which to add guard strings to the bodies of opaque actions.
     * @param eventToString The map from events to guard strings.
     */
    public static void addGuardStringsToOpaqueActionBodies(Activity activity, Map<Event, String> eventToString) {
        List<OpaqueAction> actions = getOpaqueActions(activity);
        actions.stream().forEach(action -> action.getBodies().add(getString(action, eventToString)));
    }

    private static List<OpaqueAction> getOpaqueActions(Activity activity) {
        return activity.getNodes().stream().filter(OpaqueAction.class::isInstance).map(OpaqueAction.class::cast)
                .toList();
    }

    private static String getString(OpaqueAction action, Map<Event, String> eventToString) {
        List<String> strings = eventToString.entrySet().stream()
                .filter(e -> e.getKey().getName().equals(action.getName())).map(e -> e.getValue()).toList();
        Preconditions.checkArgument(strings.size() == 1,
                String.format("Expected that there is exactly one CIF expression string corresponding to action %s.",
                        action.getName()));
        return strings.get(0);
    }

    /**
     * Add update strings to the bodies of opaque actions translated from events.
     *
     * @param activity The activity in which to add update strings to the bodies of opaque actions.
     * @param eventToString The map from events to update strings.
     */
    public static void addUpdateStringsToOpaqueActionBodies(Activity activity, Map<Event, String> eventToString) {
        List<OpaqueAction> actions = getOpaqueActions(activity);

        for (OpaqueAction action: actions) {
            List<String> strings = new ArrayList<>();

            // If the corresponding controllable event of the action has uncontrollable events, the updates of these
            // uncontrollable events are added to the action. Otherwise, the updates of the controllable event is added
            // to the action.
            strings = eventToString.entrySet().stream()
                    .filter(e -> e.getKey().getName().startsWith(action.getName() + "_result_")).map(e -> e.getValue())
                    .toList();
            if (!strings.isEmpty()) {
                Preconditions.checkArgument(strings.size() != 1, String.format(
                        "Expected that there is more than one possible effect for action %s.", action.getName()));
                action.getBodies().addAll(strings);
            } else {
                action.getBodies().add(getString(action, eventToString));
            }
        }
    }
}
