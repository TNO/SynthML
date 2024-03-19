
package com.github.tno.pokayoke.transform.app;

import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;

import com.google.common.base.Preconditions;

/** Add computed guards to the choices in the translated activity. */
public class AddGuardsToChoiceActions {
    public static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private AddGuardsToChoiceActions() {
    }

    /**
     * Add guards to the incoming edges of choice actions.
     *
     * @param choiceActionToGuard The map from the choice actions to the CIF expressions of the guards.
     */
    public static void addGuards(Map<OpaqueAction, Expression> choiceActionToGuard) {
        choiceActionToGuard.forEach((action, expression) -> addGuard(action, expression));
    }

    private static void addGuard(OpaqueAction action, Expression expression) {
        List<ActivityEdge> incomingEdges = action.getIncomings();
        Preconditions.checkArgument(incomingEdges.size() == 1,
                "Expected that each opaque action has exactly one incoming edge.");
        ActivityEdge incomingEdge = incomingEdges.get(0);

        OpaqueExpression guard = FACTORY.createOpaqueExpression();
        guard.getBodies().add(CifTextUtils.exprToStr(expression));
        incomingEdge.setGuard(guard);
    }
}
