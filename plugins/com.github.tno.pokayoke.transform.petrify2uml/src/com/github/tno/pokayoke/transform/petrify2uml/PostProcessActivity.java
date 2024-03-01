
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.OpaqueAction;

public class PostProcessActivity {
    private PostProcessActivity() {
    }

    public static void removeAction(String actionName, Activity activity) {
        OpaqueAction action = (OpaqueAction)activity.getNode(actionName);
        List<ActivityEdge> incomingEdges = action.getIncomings();
        List<ActivityEdge> outgoingEdges = action.getOutgoings();
        List<ActivityNode> sources = incomingEdges.stream().map(incomingEdge -> incomingEdge.getSource()).toList();
        List<ActivityNode> targets = outgoingEdges.stream().map(outgoingEdge -> outgoingEdge.getTarget()).toList();

        // Add new control flows from each source to the targets.
        for (ActivityNode source: sources) {
            targets.stream().forEach(target -> PetriNet2ActivityHelper.createControlFlow(activity, source, target));
        }

        // Destroy the action and its incoming and outgoing edges.
        new ArrayList<>(incomingEdges).stream().forEach(edge -> edge.destroy());
        new ArrayList<>(outgoingEdges).stream().forEach(edge -> edge.destroy());
        action.destroy();
    }
}
