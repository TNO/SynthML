
package com.github.tno.pokayoke.transform.common;

import java.io.IOException;
import java.util.ArrayList;

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

/** Flatten nested UML activity diagrams. */
public class FlattenUMLActivityDiagram {
    private final Model model;

    public FlattenUMLActivityDiagram(Model model) {
        this.model = model;
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        new FlattenUMLActivityDiagram(model).transformModel();
        FileHelper.storeModel(model, targetPath);
    }

    public void transformModel() {
        // Extract context class.
        Class contextClass = (Class)model.getMember("Context");

        // Clean the relevant info from edges so that double underscore does not exist in the default naming of
        // the guards of edges.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                UMLActivityUtils.removeIrrelevantInformation(activity);
            }
        }

        // Check if double underscore is used in names of other model elements.
        Preconditions.checkArgument(!UMLActivityUtils.isDoubleUnderscoreUsed(model),
                "Double underscore exists in name of model elements.");

        // Transform all activity behaviors of 'contextClass'.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                transformActivity(activity, null, activity.getName(), activity.eResource().getURIFragment(activity));

                // Clean again to remove the irrelevant info on the newly added edges.
                UMLActivityUtils.removeIrrelevantInformation(activity);
            }
        }

        // Make sure that the element names of the model is unique. Duplicated names are numbered.
        UMLActivityUtils.ensureUniquenessOfNames(model);
    }

    /**
     * Recursively transform the activity diagram, including flattening and renaming as well as adding a chain of IDs to
     * each object comment for tracing the origin of the element in the original model.
     *
     * @param childBehavior The non-{@code null} activity to be flattened.
     * @param callBehaviorActionToReplace The call behavior action that calls the activity. It can be {@code null} only
     *     when it is called to flatten the outer most activity.
     * @param absoluteName The absolute name of callBehaviorActionToReplace.
     * @param absoluteId The absolute id of callBehaviorActionToReplace.
     */
    public void transformActivity(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace,
            String absoluteName, String absoluteId)
    {
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction actionNode) {
                Behavior childActivity = actionNode.getBehavior();
                Verify.verify(childActivity != null, String
                        .format("The behavior of the call behavior action %s is unspecified.", actionNode.getName()));

                // Name the call behavior actions with numbering.
                String absoluteNameOfCallBehaviorAction = UMLActivityUtils.getNameOfObject("CallBehaviorAction");

                // Extract ID of the call behavior action
                String idOfCallBehaviorAction = actionNode.eResource().getURIFragment(actionNode);

                transformActivity((Activity)childActivity, actionNode, absoluteNameOfCallBehaviorAction,
                        idOfCallBehaviorAction);
            }
        }

        // Check if the activity has been visited for naming. If not, set the name for nodes and edges.
        if (!UMLActivityUtils.isNamed(childBehavior)) {
            UMLActivityUtils.setNameForNodesAndEdges(childBehavior, absoluteName);
        }

        // Check if the activity has been visited for tracing. If not, add tracing comments for nodes and edges.
        if (!UMLActivityUtils.isTraced(childBehavior)) {
            UMLActivityUtils.setTracingCommentForNodesAndEdges(childBehavior, absoluteId);
        }

        // Replace the call behavior action with the content of this activity, append absolute name and id of this call
        // behavior action to comments and connect it with proper edges.
        if (callBehaviorActionToReplace != null) {
            Activity childBehaviorCopy = EcoreUtil.copy(childBehavior);

            // Append the absolute name and ID of the call behavior action.
            UMLActivityUtils.appendCallBehaviorActionName(childBehaviorCopy, absoluteName);
            UMLActivityUtils.appendCallBehaviorActionID(childBehaviorCopy, absoluteId);

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
                // appropriate properties, like guards. Name and tracing comment for the new edges are set.
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

                            // Set name for the newly added edge.
                            newEdge.setName(absoluteName + "__" + childBehavior.getName() + "__AddedIncomingEdge_"
                                    + String.valueOf(i++));

                            // Set tracing comment for the newly added edge.
                            UMLActivityUtils.setTracingCommentForAddedEdge(incomingEdge, outgoingEdge, newEdge);
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
                // properties, like guards. Name and tracing comment for the new edges are set.
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

                            // Set name for the newly added edge.
                            newEdge.setName(absoluteName + "__" + childBehavior.getName() + "__AddedOutgoingEdge_"
                                    + String.valueOf(i++));

                            // Set tracing comment for the newly added edge.
                            UMLActivityUtils.setTracingCommentForAddedEdge(outgoingEdge, incomingEdge, newEdge);
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
