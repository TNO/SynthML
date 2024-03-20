
package com.github.tno.pokayoke.transform.app;

import java.util.List;
import java.util.Map;

import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;

import com.google.common.base.Preconditions;

/** Helper for adding guards to the incoming edges of opaque actions. */
public class OpaqueActionHelper{
    private static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private OpaqueActionHelper() {
    }

    /**
     * Add guards to the incoming edges of opaque actions.
     *
     * @param actionToGuard The map from the opaque actions to the CIF expressions of the guards.
     */
    public static void addGuards(Map<OpaqueAction, String> actionToGuard) {
        actionToGuard.forEach((action, expression) -> addGuard(action, expression));
    }

    private static void addGuard(OpaqueAction action, String expression) {
        List<ActivityEdge> incomingEdges = action.getIncomings();
        Preconditions.checkArgument(incomingEdges.size() == 1,
                "Expected that each opaque action has exactly one incoming edge.");
        ActivityEdge incomingEdge = incomingEdges.get(0);

        OpaqueExpression guard = FACTORY.createOpaqueExpression();
        guard.getBodies().add(expression);
        incomingEdge.setGuard(guard);
    }
}
