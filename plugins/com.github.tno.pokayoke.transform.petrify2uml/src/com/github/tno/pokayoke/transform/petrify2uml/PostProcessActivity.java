
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.List;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.OpaqueAction;

import com.google.common.base.Preconditions;

public class PostProcessActivity {
    private PostProcessActivity() {
    }

    /**
     * Remove the internal actions that were added in CIF specification and petrification.
     *
     * @param activity The activity in which actions to be removed.
     */
    public static void removeInternalActions(Activity activity) {
        int numberOfRemovedActions = removeOpaqueActions("start", activity);
        Preconditions.checkArgument(numberOfRemovedActions == 1,
                "Expected that there is exactly one 'start' action removed.");
        numberOfRemovedActions = removeOpaqueActions("end", activity);
        Preconditions.checkArgument(numberOfRemovedActions == 1,
                "Expected that there is exactly one 'end' action removed.");
        numberOfRemovedActions = removeOpaqueActions("c_satisfied", activity);
        Preconditions.checkArgument(numberOfRemovedActions == 1,
                "Expected that there is exactly one 'c_satisfied' action removed.");
    }

    /**
     * Remove opaque actions from activity.
     *
     * @param actionName The name of the opaque actions to remove.
     * @param activity The activity from which to remove the actions.
     * @return Number of actions that were removed.
     */
    public static int removeOpaqueActions(String actionName, Activity activity) {
        List<ActivityNode> nodes = activity.getNodes().stream().filter(node -> node.getName() != null)
                .filter(node -> node.getName().equals(actionName)).toList();
        List<OpaqueAction> actions = nodes.stream().filter(OpaqueAction.class::isInstance).map(OpaqueAction.class::cast)
                .toList();
        int numerOfActions = actions.size();

        for (OpaqueAction action: actions) {
            List<ActivityEdge> incomingEdges = action.getIncomings();
            Preconditions.checkArgument(incomingEdges.size() == 1,
                    "Expected that an opaque action has exactly one incoming edge.");
            ActivityEdge incomingEdge = incomingEdges.get(0);

            List<ActivityEdge> outgoingEdges = action.getOutgoings();
            Preconditions.checkArgument(outgoingEdges.size() == 1,
                    "Expected that an opaque action has exactly one ougoing edge.");
            ActivityEdge outgoingEdge = outgoingEdges.get(0);

            ActivityNode source = incomingEdge.getSource();
            ActivityNode target = outgoingEdge.getTarget();

            // Add a new control flow from source to target.
            PetriNet2ActivityHelper.createControlFlow(activity, source, target);

            // Destroy the action and its incoming and outgoing edges.
            incomingEdge.destroy();
            outgoingEdge.destroy();
            action.destroy();
        }
        return numerOfActions;
    }

    /**
     * Remove the names of edges, and nodes that are not call behavior actions or opaque actions.
     *
     * @param activity The activity from which to remove names of nodes and edges.
     */
    public static void removeNodesEdgesNames(Activity activity) {
        activity.getEdges().stream().forEach(edge -> edge.setName(null));
        List<ActivityNode> nodes = activity.getNodes().stream()
                .filter(node -> !(node instanceof OpaqueAction) && !(node instanceof CallBehaviorAction)).toList();
        nodes.stream().forEach(node -> node.setName(null));
    }
}
