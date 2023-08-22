/**
 *
 */

package com.github.tno.pokayoke.transform.cif;

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
 *
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
        // Extract activities
        Class contextClass = (Class)model.getMember("Context");
        // Transform all activity behaviors of 'contextClass'.
        for (Behavior behavior: new LinkedHashSet<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity) {
                flattenActivityDiagram((Activity)behavior, null);
            }
        }
    }
    // Child behavior is the activity diagram that is being called by the call behavior action parentAction.
    public void flattenActivityDiagram(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace) {
        // The activity is null then we already reached the leaf
        if (childBehavior == null)
            return;

        for (ActivityNode node: new LinkedHashSet<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction) {
                Behavior childDiagram = ((CallBehaviorAction)node).getBehavior();
                //Recursion to reach the leaf of the tree
                flattenActivityDiagram((Activity)childDiagram, (CallBehaviorAction)node);
            }
        }

        // Relocating edges when CallBehaviorActionToReplace is not null
        if (callBehaviorActionToReplace != null) {
            Activity tmp = EcoreUtil.copy(childBehavior);

            for (ActivityNode node: new LinkedHashSet<>(tmp.getNodes())) {
                // Set the activity for the node
                node.setActivity(callBehaviorActionToReplace.getActivity());

                // Set the activity for all the edges to the activity of the call behavior action to be replaced
                for (ActivityEdge edge: new LinkedHashSet<>(node.getOutgoings())) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }
                for (ActivityEdge edge: new LinkedHashSet<>(node.getIncomings())) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }

                if (node instanceof InitialNode) {
                    // Relocate all outgoing edges from 'Initial Node' to the node from which the control flows to the
                    // call behavior action to be replaced.

                    for (ActivityEdge outgoingEdge: new LinkedHashSet<>(node.getOutgoings())) {
                        for (ActivityEdge inComingEdge: new LinkedHashSet<>(
                                callBehaviorActionToReplace.getIncomings()))
                        {
                            inComingEdge.setTarget(outgoingEdge.getTarget());
                        }
                        // Destroy the outgoing edge from the InitialNode
                        outgoingEdge.destroy();
                    }
                    // Destroy the InitialNode
                    node.destroy();
                }
                if (node instanceof ActivityFinalNode) {
                    // Relocate all incoming edges from Initial Node to the node receives control signal from the call
                    // behavior action to be replaced
                    for (ActivityEdge inComingEdge: new LinkedHashSet<>(node.getIncomings())) {
                        for (ActivityEdge outgoingEdge: new LinkedHashSet<>(
                                callBehaviorActionToReplace.getOutgoings()))
                        {
                            outgoingEdge.setSource(inComingEdge.getSource());
                        }
                        // Destroy the incoming edge of the ActivityFinalNode
                        inComingEdge.destroy();
                    }

                    // Destroy the ActivityFinalNode
                    node.destroy();
                }
            }
            // Destroy the call behavior action being replaced
            callBehaviorActionToReplace.destroy();
        }
    }

    public static void main(String args[]) throws IOException {
        String inputPath = "C:\\Users\\nanyang\\workspace\\NestedDiagram\\2023-08-21 - deadlock.uml";
        String outputPath = "C:\\Users\\nanyang\\workspace\\NestedDiagram\\flattened_model.uml";
        transformFile(inputPath, outputPath);
    }
}
