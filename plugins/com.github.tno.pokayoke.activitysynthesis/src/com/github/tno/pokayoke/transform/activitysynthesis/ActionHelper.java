
package com.github.tno.pokayoke.transform.activitysynthesis;

import java.util.Map;

import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;

/** Helper for adding guards to UML control flows. */
public class ActionHelper {
    private static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private ActionHelper() {
    }

    /**
     * Add guards to UML control flows as indicated by the specified mapping.
     *
     * @param controlFlowToGuard The map from UML control flows to their guards as textual CIF expressions.
     */
    public static void addGuardToControlFlows(Map<ControlFlow, String> controlFlowToGuard) {
        controlFlowToGuard.forEach(ActionHelper::addGuardToControlFlow);
    }

    private static void addGuardToControlFlow(ControlFlow controlFlow, String guard) {
        OpaqueExpression expression = FACTORY.createOpaqueExpression();
        expression.getBodies().add(guard);
        controlFlow.setGuard(expression);
    }
}
