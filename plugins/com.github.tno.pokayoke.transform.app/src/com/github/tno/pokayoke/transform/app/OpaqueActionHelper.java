
package com.github.tno.pokayoke.transform.app;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
                "Expected that each opaque action has exactly one incoming edge.");
        ActivityEdge incomingEdge = incomingEdges.get(0);

        OpaqueExpression guard = FACTORY.createOpaqueExpression();
        guard.getBodies().add(expression);
        incomingEdge.setGuard(guard);
    }

    /**
     * Add strings to the bodies of actions translated from events.
     *
     * @param activity The activity in which strings are added to the actions.
     * @param eventToString The map from event to string.
     */
    public static void addStringsToActions(Activity activity, Map<Event, String> eventToString) {
        List<OpaqueAction> actions = activity.getNodes().stream().filter(OpaqueAction.class::isInstance)
                .map(OpaqueAction.class::cast).toList();
        for (Entry<Event, String> entry: eventToString.entrySet()) {
            String eventName = entry.getKey().getName();
            String string = entry.getValue();

            List<OpaqueAction> eventActions = actions.stream().filter(action -> action.getName().equals(eventName))
                    .toList();
            Preconditions.checkArgument(eventActions.size() > 0,
                    String.format("Expected there is at least one action corresponding to event %s.", eventName));
            eventActions.stream().forEach(action -> action.getBodies().add(string));
        }
    }
}
