
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
import org.eclipse.uml2.uml.PackageableElement;

import com.google.common.base.Verify;

/** Flatten nested UML activities. */
public class FlattenUMLActivity {
    private FlattenUMLActivity() {
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        transformModel(model);
        FileHelper.storeModel(model, targetPath);
    }

    /**
     * Flattens all activities within the given model.
     *
     * @param model The non-{@code null} model whose activities to flatten.
     */
    public static void transformModel(Model model) {
        for (PackageableElement element: model.getPackagedElements()) {
            if (element instanceof Activity activity) {
                transformActivity(activity);
            } else if (element instanceof Class modelClass) {
                transformClass(modelClass);
            } else if (element instanceof Model modelElement) {
                transformModel(modelElement);
            }
        }
    }

    /**
     * Flattens all activities within the given class.
     *
     * @param modelClass The non-{@code null} class whose activities to flatten.
     */
    public static void transformClass(Class modelClass) {
        for (Behavior behavior: new ArrayList<>(modelClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                flattenActivity(activity, null);
                UMLActivityUtils.removeIrrelevantInformation(activity);
            }
        }
    }

    /**
     * Recursively flattens the given activity.
     *
     * @param activity The non-{@code null} activity to flatten.
     */
    public static void transformActivity(Activity activity) {
        flattenActivity(activity, null);
    }

    /**
     * Recursively flatten the given activity.
     *
     * @param childBehavior The non-{@code null} activity to be flattened.
     * @param callBehaviorActionToReplace The call behavior action that calls the activity. It can be {@code null} only
     *     when it is called to flatten the outer most activity.
     */
    private static void flattenActivity(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace) {
        // Depth-first recursion. Transform children first, for a bottom-up flattening.
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction actionNode) {
                Behavior actionBehavior = actionNode.getBehavior();
                Verify.verify(actionBehavior != null, String
                        .format("The behavior of the call behavior action %s is unspecified.", actionNode.getName()));
                flattenActivity((Activity)actionBehavior, actionNode);
            }
        }

        // Replace the call behavior action with the content of this activity, and connect it with proper edges.
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
