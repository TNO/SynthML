
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralNull;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.activitysynthesis.CifSourceSinkLocationTransformer;
import com.github.tno.pokayoke.uml.profile.util.UmlPrimitiveType;
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
        int numberOfRemovedActions = removeOpaqueActions(CifSourceSinkLocationTransformer.START_EVENT_NAME, activity);
        Preconditions.checkArgument(numberOfRemovedActions == 1,
                "Expected that there is exactly one 'start' action removed.");
        numberOfRemovedActions = removeOpaqueActions(CifSourceSinkLocationTransformer.END_EVENT_NAME, activity);
        Preconditions.checkArgument(numberOfRemovedActions == 1,
                "Expected that there is exactly one 'end' action removed.");
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
            ControlFlow newEdge = PNML2UMLActivityHelper.createControlFlow(activity, source, target);
            newEdge.setGuard(combineGuards(incomingEdge.getGuard(), outgoingEdge.getGuard()));

            // Destroy the action and its incoming and outgoing edges.
            incomingEdge.destroy();
            outgoingEdge.destroy();
            action.destroy();
        }
        return numerOfActions;
    }

    /**
     * Combines two given edge guards into a single edge guard.
     *
     * @param left The first edge guard to combine.
     * @param right The second edge guard to combine.
     * @return The combined edge guard.
     */
    private static ValueSpecification combineGuards(ValueSpecification left, ValueSpecification right) {
        if (left == null || left instanceof LiteralNull) {
            return right;
        }
        if (right == null || right instanceof LiteralNull) {
            return left;
        }
        if (left instanceof LiteralBoolean leftBool && right instanceof LiteralBoolean rightBool) {
            LiteralBoolean result = UMLFactory.eINSTANCE.createLiteralBoolean();
            result.setType(UmlPrimitiveType.BOOLEAN.load(left));
            result.setValue(leftBool.isValue() && rightBool.isValue());
            return result;
        }
        if (left instanceof OpaqueExpression leftExpr && right instanceof OpaqueExpression rightExpr) {
            OpaqueExpression result = UMLFactory.eINSTANCE.createOpaqueExpression();
            result.setType(UmlPrimitiveType.BOOLEAN.load(left));

            // Combine languages.
            Set<String> languages = new LinkedHashSet<>();
            languages.addAll(leftExpr.getLanguages());
            languages.addAll(rightExpr.getLanguages());
            result.getLanguages().addAll(languages);

            // Combine bodies.
            Set<String> bodies = new LinkedHashSet<>();
            bodies.addAll(leftExpr.getBodies());
            bodies.addAll(rightExpr.getBodies());
            Optional<String> body = bodies.stream().reduce((l, r) -> String.format("(%s) and (%s)", l, r));
            body.ifPresent(b -> result.getBodies().add(b));

            return result;
        }

        throw new RuntimeException("Could not combine " + left + " with " + right);
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
