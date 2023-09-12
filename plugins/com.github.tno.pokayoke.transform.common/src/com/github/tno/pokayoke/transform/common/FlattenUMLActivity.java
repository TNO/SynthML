
package com.github.tno.pokayoke.transform.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Model;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/** Flatten nested UML activity. */
public class FlattenUMLActivity {
    private final Model model;

    private static final List<Activity> VISITED_ACTIVITIES = new ArrayList<>();

    public FlattenUMLActivity(Model model) {
        this.model = model;
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        new FlattenUMLActivity(model).transformModel();
        FileHelper.storeModel(model, targetPath);
    }

    public void transformModel() {
        // Extract context class.
        Class contextClass = (Class)model.getMember("Context");

        // Clean the relevant info from edges so that double underscore does not exist in the default name of Boolean
        // literal of guards on edges.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                UMLActivityUtils.removeIrrelevantInformation(activity);
            }
        }

        // Check if double underscore is used in the names of any model elements.
        Preconditions.checkArgument(!NameIDTracingHelper.isDoubleUnderscoreUsed(model),
                "Expected double underscores to not be used in the names of model elements.");

        // Transform all activity behaviors of context class.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                String activityID = activity.eResource().getURIFragment(activity);
                transformActivity(activity, null, activity.getName(), activityID);
                VISITED_ACTIVITIES.add(activity);
            }
        }

        // Make sure that the element names of the model are unique. Duplicated names get a postfix.
        NameIDTracingHelper.ensureUniquenessOfNames(model);
    }

    /**
     * Recursively transforms the activity, including flattening and renaming as well as adding a chain of IDs to each
     * object comment for tracing the origin of the element in the original model.
     *
     * @param childBehavior The non-{@code null} activity to be flattened.
     * @param callBehaviorActionToReplace The call behavior action that calls the activity. It can be {@code null} only
     *     when it is called to flatten the outer most activity.
     * @param absoluteName The absolute name of the activity.
     * @param absoluteID The absolute ID of the activity.
     */
    public void transformActivity(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace,
            String absoluteName, String absoluteID)
    {
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction actionNode) {
                Behavior childActivity = actionNode.getBehavior();
                Verify.verifyNotNull(childActivity, String
                        .format("The behavior of the call behavior action %s is unspecified.", actionNode.getName()));
                transformActivity((Activity)childActivity, actionNode, absoluteName, absoluteID);
            }
        }

        // Check if the activity has been visited. If not, the name and ID of model objects are added with the absolute
        // name and ID as prefix.
        if (!VISITED_ACTIVITIES.contains(childBehavior)) {
            for (ActivityNode node: childBehavior.getNodes()) {
                NameIDTracingHelper.setName(node, absoluteName, node.eClass().getName());
                NameIDTracingHelper.setTracingComment(node, absoluteID, "Unique-ID:");
            }

            for (ActivityEdge edge: childBehavior.getEdges()) {
                NameIDTracingHelper.setName(edge, absoluteName, "ActivityEdge");
                NameIDTracingHelper.setTracingComment(edge, absoluteID, "Unique-ID:");
            }
        }

        // Replace the call behavior action with the objects of this activity. Append the name and ID of the call
        // behavior action to the name and tracing comment of objects in this activity, respectively. Connect the
        // objects properly to the outer activity.
        if (callBehaviorActionToReplace != null) {
            Activity childBehaviorCopy = EcoreUtil.copy(childBehavior);

            // Give the call behavior action a unique name.
            String callBehaviorActionPrefix = NameIDTracingHelper.getNameOfObject("CallBehaviorAction");

            // Append the name of the call behavior action to the name of all elements in the activity.
            NameIDTracingHelper.appendCallBehaviorActionName(childBehaviorCopy, callBehaviorActionPrefix);

            // Extract the ID of the call behavior action.
            String callBehaviorActionID = callBehaviorActionToReplace.eResource()
                    .getURIFragment(callBehaviorActionToReplace);

            // Append the ID of the call behavior action to the tracing comment of all elements in the activity.
            NameIDTracingHelper.appendCallBehaviorActionID(childBehaviorCopy, callBehaviorActionID, "Unique-ID:");

            for (ActivityNode node: new ArrayList<>(childBehaviorCopy.getNodes())) {
                // Set the activity for the node.
                node.setActivity(callBehaviorActionToReplace.getActivity());

                // Set the activity for all the edges to the activity of the call behavior action to be replaced.
                for (ActivityEdge edge: node.getOutgoings()) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }
                for (ActivityEdge edge: node.getIncomings()) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }

                // Create a new edge for every pair of an outgoing edge from the activity's initial node and an
                // incoming edge to the call behavior action. The edges are properly connected and given the
                // appropriate properties, like guards. Name and tracing comment for the new edges are added.
                if (node instanceof InitialNode initialNode) {
                    int i = 1;
                    for (ActivityEdge outgoingEdge: initialNode.getOutgoings()) {
                        for (ActivityEdge incomingEdge: callBehaviorActionToReplace.getIncomings()) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());

                            // The guard of the new edge is set to the guard of the incoming edge of the call
                            // behavior action. We ignore the guard of the outgoing edge of the initial node because
                            // the source of this edge is not a decision node.
                            newEdge.setGuard(EcoreUtil.copy(incomingEdge.getGuard()));
                            newEdge.setActivity(callBehaviorActionToReplace.getActivity());

                            // Add name for the newly added edge.
                            newEdge.setName(callBehaviorActionPrefix + "__" + childBehavior.getName()
                                    + "__AddedIncomingEdge_" + String.valueOf(i++));

                            // Add the ID of the edge in the outer activity (e.g., the incoming edge of the initial
                            // node) to the tracing comment of the newly added edge.
                            NameIDTracingHelper.addTracingComment(newEdge,
                                    incomingEdge.eResource().getURIFragment(incomingEdge), "Outer-Edge-Unique-ID:");

                            // Add the concatenation of the ID of the edge in the inner activity (e.g., the outgoing
                            // edge of the initial node), the ID of its activity and the ID of the call behavior action
                            // to the tracing comment of the newly added edge.
                            String innerEdgeID = callBehaviorActionID + "__"
                                    + childBehavior.eResource().getURIFragment(childBehavior) + "__"
                                    + outgoingEdge.eResource().getURIFragment(outgoingEdge);
                            NameIDTracingHelper.addTracingComment(newEdge, innerEdgeID, "Inner-Edge-Unique-ID:");
                        }
                    }

                    // Destroy the initial node and the redundant edges.
                    for (ActivityEdge outgoingEdge: new ArrayList<>(initialNode.getOutgoings())) {
                        outgoingEdge.destroy();
                    }
                    for (ActivityEdge incomingEdge: new ArrayList<>(callBehaviorActionToReplace.getIncomings())) {
                        incomingEdge.destroy();
                    }
                    initialNode.destroy();
                }

                // Create a new edge for every pair of an incoming edge to the activity's final node and an outgoing
                // edge of the call behavior action. The edges are properly connected and given the appropriate
                // properties, like guards. Name and tracing comment for the new edges are added.
                if (node instanceof ActivityFinalNode finalNode) {
                    int i = 1;
                    for (ActivityEdge incomingEdge: finalNode.getIncomings()) {
                        for (ActivityEdge outgoingEdge: callBehaviorActionToReplace.getOutgoings()) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());

                            // The guard of the new edge is set to the guard of the incoming edge of the final node.
                            // We ignore the guard of the outgoing edge of the behavior action.
                            newEdge.setGuard(EcoreUtil.copy(incomingEdge.getGuard()));
                            newEdge.setActivity(callBehaviorActionToReplace.getActivity());

                            // Add name for the newly added edge.
                            newEdge.setName(callBehaviorActionPrefix + "__" + childBehavior.getName()
                                    + "__AddedOutgoingEdge_" + String.valueOf(i++));

                            // Add the ID of the edge in the outer activity (e.g., the outgoing edge of the final
                            // node) to the comment of the newly added edge.
                            NameIDTracingHelper.addTracingComment(newEdge,
                                    outgoingEdge.eResource().getURIFragment(outgoingEdge), "Outer-Edge-Unique-ID:");

                            // Add the concatenation of the ID of the edge in the inner activity (e.g., the incoming
                            // edge of the final node), the ID of its activity and the ID of its call behavior action to
                            // the comment of the newly added edge.
                            String innerEdgeID = callBehaviorActionID + "__"
                                    + childBehavior.eResource().getURIFragment(childBehavior) + "__"
                                    + incomingEdge.eResource().getURIFragment(incomingEdge);
                            NameIDTracingHelper.addTracingComment(newEdge, innerEdgeID, "Inner-Edge-Unique-ID:");
                        }
                    }

                    // Destroy the final node and the redundant edges.
                    for (ActivityEdge incomingEdge: new ArrayList<>(finalNode.getIncomings())) {
                        incomingEdge.destroy();
                    }
                    for (ActivityEdge outgoingEdge: new ArrayList<>(callBehaviorActionToReplace.getOutgoings())) {
                        outgoingEdge.destroy();
                    }
                    finalNode.destroy();
                }
            }

            // Destroy the call behavior action being replaced.
            callBehaviorActionToReplace.destroy();
        }
    }
}
