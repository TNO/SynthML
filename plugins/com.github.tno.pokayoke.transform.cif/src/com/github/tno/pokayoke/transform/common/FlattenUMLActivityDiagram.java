/**
 *
 */

package com.github.tno.pokayoke.transform.common;

import java.io.IOException;
import java.util.LinkedHashSet;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Model;

/**
 * Flatten nested UML activity diagrams.
 */
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
        // Extract activities.
        Class contextClass = (Class)model.getMember("Context");
        // Transform all activity behaviors of 'contextClass'.
        for (Behavior behavior: new LinkedHashSet<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity) {
                flattenActivityDiagram((Activity)behavior, null);
            }
        }
    }

    /** Recursively flatten the activity diagram.
     * @param childBehavior is the activity diagram to be flattened.
     * @param callBehaviorActionToReplace is the call behavior action that calls the activity.
     */
    public void flattenActivityDiagram(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace) {
        // If 'childBehavior' is null then the behavior of the call behavior action is unspecified. Flattening cannot be done.
        if (childBehavior == null)
            return;

        for (ActivityNode node: new LinkedHashSet<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction) {
                CallBehaviorAction actionNode = (CallBehaviorAction) node;
                Behavior childDiagram = actionNode.getBehavior();
                // Recursion to reach the leaf of the tree.
                flattenActivityDiagram((Activity)childDiagram, actionNode);
            }
        }

        // Relocating edges when 'CallBehaviorActionToReplace' is not null.
        if (callBehaviorActionToReplace != null) {
            Activity tmp = EcoreUtil.copy(childBehavior);

            for (ActivityNode node: new LinkedHashSet<>(tmp.getNodes())) {
                // Set the activity for the node.
                node.setActivity(callBehaviorActionToReplace.getActivity());

                // Set the activity for all the edges to the activity of the call behavior action to be replaced.
                for (ActivityEdge edge: new LinkedHashSet<>(node.getOutgoings())) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }
                for (ActivityEdge edge: new LinkedHashSet<>(node.getIncomings())) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }
                // Relocate all outgoing edges from the initial node to the node from which the control flows to the call behavior action to be replaced.
                if (node instanceof InitialNode) {
                    for (ActivityEdge outgoingEdge: new LinkedHashSet<>(node.getOutgoings())) {
                        for (ActivityEdge inComingEdge: new LinkedHashSet<>(
                                callBehaviorActionToReplace.getIncomings()))
                        {
                            inComingEdge.setTarget(outgoingEdge.getTarget());
                        }
                        // Destroy the outgoing edge from the initial node.
                        outgoingEdge.destroy();
                    }
                    // Destroy the initial node.
                    node.destroy();
                }
                // Relocate all incoming edges to the final node to the node receives control signal from the call behavior action to be replaced.
                if (node instanceof ActivityFinalNode) {
                    for (ActivityEdge inComingEdge: new LinkedHashSet<>(node.getIncomings())) {
                        for (ActivityEdge outgoingEdge: new LinkedHashSet<>(
                                callBehaviorActionToReplace.getOutgoings()))
                        {
                            outgoingEdge.setSource(inComingEdge.getSource());
                        }
                        // Destroy the incoming edge of the final node.
                        inComingEdge.destroy();
                    }
                    // Destroy the final node.
                    node.destroy();
                }
            }
            // Destroy the call behavior action being replaced.
            callBehaviorActionToReplace.destroy();
        }
    }

}
