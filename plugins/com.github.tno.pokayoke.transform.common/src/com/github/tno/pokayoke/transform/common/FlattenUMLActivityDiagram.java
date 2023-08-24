
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
            if (behavior instanceof Activity behaviorActivity) {
                flattenActivityDiagram(behaviorActivity, null);
                UMLActivityUtils.removeIrrelevantInformation(behaviorActivity);
            }
        }
    }

    /**
     * Recursively flatten the activity diagram.
     *
     * @param childBehavior The non-{@code null} activity diagram to be flattened.
     * @param callBehaviorActionToReplace The call behavior action that calls the activity. It can be null only when it
     *     is called to flatten the outer most activity diagram.
     */
    public void flattenActivityDiagram(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace) {
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction actionNode) {
                Behavior childDiagram = actionNode.getBehavior();

                // Depth-first recursion if the activity diagram of the call behavior action is specified. Transform
                // children first, for a bottom-up flattening. Otherwise, fattening cannot be done.
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
                for (ActivityEdge edge: new ArrayList<>(node.getOutgoings())) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }
                for (ActivityEdge edge: new ArrayList<>(node.getIncomings())) {
                    edge.setActivity(callBehaviorActionToReplace.getActivity());
                }

                // Create a new edge for every pair of the outgoing edge from the behavior node and the incoming edge to
                // the call behavior action. The target and source of the edge is set.
                if (node instanceof InitialNode initialNode) {
                    for (ActivityEdge outgoingEdge: new ArrayList<>(initialNode.getOutgoings())) {
                        for (ActivityEdge incomingEdge: new ArrayList<>(callBehaviorActionToReplace.getIncomings())) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());
                            // The guard of the new edge is set to the guard of the incoming edge of the call behavior
                            // action. We ignore the guard of the outgoing edge of the initial node because the source
                            // of this edge is not a decision node.
                            newEdge.setGuard(incomingEdge.getGuard());
                            newEdge.setActivity(callBehaviorActionToReplace.getActivity());
                        }
                    }
                    // Destroy the initial node and the redundant edges.
                    destoryRedundantNodesAndEdges(initialNode, callBehaviorActionToReplace);
                }

                // Create a new edge for every pair of the incoming edge to the final node and the outgoing edge of
                // the call behavior action. The target and source of the edge is set.
                if (node instanceof ActivityFinalNode finalNode) {
                    for (ActivityEdge incomingEdge: new ArrayList<>(finalNode.getIncomings())) {
                        for (ActivityEdge outgoingEdge: new ArrayList<>(callBehaviorActionToReplace.getOutgoings())) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());
                            // The guard of the new edge is set to the guard of the incoming edge of the final node. We
                            // ignore the guard of the outgoing edge of the behavior action.
                            newEdge.setGuard(incomingEdge.getGuard());
                            newEdge.setActivity(callBehaviorActionToReplace.getActivity());
                        }
                    }
                    // Destroy the final node and the redundant edges.
                    destoryRedundantNodesAndEdges(finalNode, callBehaviorActionToReplace);
                }
            }

            // Destroy the call behavior action being replaced.
            callBehaviorActionToReplace.destroy();
        }
    }

    /**
     * Destroy the initial node and its outgoing edges, the final node and its incoming edges, and the incoming and
     * outgoing edges of the call behavior action.
     *
     * @param node The initial node or final node to destroy.
     * @param action The call behavior action to replace
     */
    public static void destoryRedundantNodesAndEdges(ActivityNode node, CallBehaviorAction action) {
        if (node instanceof InitialNode initialNode) {
            for (ActivityEdge outgoingEdge: new ArrayList<>(initialNode.getOutgoings())) {
                outgoingEdge.destroy();
            }
            for (ActivityEdge incomingEdge: new ArrayList<>(action.getIncomings())) {
                incomingEdge.destroy();
            }
            initialNode.destroy();
        }
        if (node instanceof ActivityFinalNode finalNode) {
            for (ActivityEdge incomingEdge: new ArrayList<>(finalNode.getIncomings())) {
                incomingEdge.destroy();
            }
            for (ActivityEdge outgoingEdge: new ArrayList<>(action.getOutgoings())) {
                outgoingEdge.destroy();
            }
            finalNode.destroy();
        }
    }
}
