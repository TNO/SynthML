
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
        // Extract activities.
        Class contextClass = (Class)model.getMember("Context");
        // Transform all activity behaviors of 'contextClass'.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                flattenActivityDiagram(activity, null);
                UMLActivityUtils.removeIrrelevantInformation(activity);
            }
        }
    }

    /**
     * Recursively flatten the activity diagram.
     *
     * @param childBehavior The non-{@code null} activity diagram to be flattened.
     * @param callBehaviorActionToReplace The call behavior action that calls the activity. It can be {@code null} only
     *     when it is called to flatten the outer most activity diagram.
     */
    public void flattenActivityDiagram(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace) {
        // Depth-first recursion. Transform children first, for a bottom-up flattening.
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction actionNode) {
                Behavior childDiagram = actionNode.getBehavior();
                Verify.verify(childDiagram != null, String
                        .format("The behavior of the call behavior action %s is unspecified.", actionNode.getName()));
                flattenActivityDiagram((Activity)childDiagram, actionNode);
            }
        }

        // Replace the call behavior action with the content of this activity diagram, and connect it with proper edges.
        if (callBehaviorActionToReplace != null) {
            Activity childBehaviorCopy = EcoreUtil.copy(childBehavior);

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

                // Create a new edge for every pair of an outgoing edge from the activity's initial node and an incoming
                // edge to the call behavior action. The edges are properly connected and given the appropriate
                // properties, like guards.
                if (node instanceof InitialNode initialNode) {
                    for (ActivityEdge outgoingEdge: initialNode.getOutgoings()) {
                        for (ActivityEdge incomingEdge: callBehaviorActionToReplace.getIncomings()) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());
                            // The guard of the new edge is set to the guard of the incoming edge of the call behavior
                            // action. We ignore the guard of the outgoing edge of the initial node because the source
                            // of this edge is not a decision node.
                            newEdge.setGuard(EcoreUtil.copy(incomingEdge.getGuard()));
                            newEdge.setActivity(callBehaviorActionToReplace.getActivity());
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
                // properties, like guards.
                if (node instanceof ActivityFinalNode finalNode) {
                    for (ActivityEdge incomingEdge: finalNode.getIncomings()) {
                        for (ActivityEdge outgoingEdge: callBehaviorActionToReplace.getOutgoings()) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());
                            // The guard of the new edge is set to the guard of the incoming edge of the final node. We
                            // ignore the guard of the outgoing edge of the behavior action.
                            newEdge.setGuard(EcoreUtil.copy(incomingEdge.getGuard()));
                            newEdge.setActivity(callBehaviorActionToReplace.getActivity());
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
