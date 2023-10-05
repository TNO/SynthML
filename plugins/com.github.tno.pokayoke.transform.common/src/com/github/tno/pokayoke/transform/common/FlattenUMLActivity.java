
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
import org.eclipse.uml2.uml.PackageableElement;

/** Flattens nested UML activities. */
public class FlattenUMLActivity {
    private final Model model;

    private final StructureInfoHelper structureInfoHelper;

    public FlattenUMLActivity(Model model) {
        this.model = model;
        this.structureInfoHelper = new StructureInfoHelper();
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        new FlattenUMLActivity(model).transformModel();
        FileHelper.storeModel(model, targetPath);
    }

    public void transformModel() {
        // Check whether the model has the expected structure, particularly that no double underscores exist in the
        // names of relevant model elements.
        new UMLValidatorSwitch().doSwitch(model);

        // Give each element a name.
        NameHelper.giveNameToModelElements(model);

        // Ensure that all names are locally unique within their scope.
        NameHelper.ensureUniqueNameForEnumerationsPropertiesActivities(model);
        NameHelper.ensureUniqueNameForEnumerationLiteralsInEnumerations(model);

        // Give every element an ID.
        IDHelper.addIDTracingCommentToModelElements(model);

        // Transform all classes in the model.
        for (PackageableElement element: model.getPackagedElements()) {
            if (element instanceof Class classElement) {
                transformClass(classElement);
            }
        }

        // Check that the names of the model elements are unique globally.
        NameHelper.checkUniquenessOfNames(model);
    }

    private void transformClass(Class classElement) {
        // Clean the irrelevant info from edges so that double underscores do not exist in the default name of Boolean
        // literals of guards on edges that are not the outgoing edges of decision nodes. These guards do not have a
        // clear meaning and are automatically added by UML Designer.
        for (Behavior behavior: classElement.getOwnedBehaviors()) {
            if (behavior instanceof Activity activity) {
                UMLActivityUtils.removeIrrelevantInformation(activity);
            }
        }

        // Ensure that all names are locally unique within their scope.
        NameHelper.ensureUniqueNameForElementsInActivities(classElement);

        // Flatten all activity behaviors of the context class.
        for (Behavior behavior: new ArrayList<>(classElement.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                transformActivity(activity, null);
            }
        }

        // Prepend the name of the outer activity to the model elements in activities.
        NameHelper.prependOuterActivityNameToNodesAndEdgesInActivities(classElement);

        // Add structure comments to the outgoing edges of the initial nodes and the incoming edges of the final nodes
        // in the outermost activities.
        structureInfoHelper.addStructureInfoInActivities(classElement);
    }

    /**
     * Recursively transforms the activity, including flattening, renaming and adding structure info as well as adding a
     * chain of IDs to each object comment for tracing the origin of the element in the original model.
     *
     * @param childBehavior The non-{@code null} activity to be flattened.
     * @param callBehaviorActionToReplace The call behavior action that calls the activity. It can be {@code null} only
     *     when it is called to flatten the outer most activity.
     */
    private void transformActivity(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace) {
        // Depth-first recursion. Transform children first, for a bottom-up flattening.
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction actionNode) {
                transformActivity((Activity)actionNode.getBehavior(), actionNode);
            }
        }

        // Replace the call behavior action with the objects of this activity. Prepend the name and ID of the call
        // behavior action and the activity to the name and tracing comment of objects in this activity, respectively.
        // Connect the objects properly to the outer activity and add structure info comments.
        if (callBehaviorActionToReplace != null) {
            // Increment the counter for structure info comments, for call behavior actions.
            structureInfoHelper.incrementCounter();

            Activity childBehaviorCopy = EcoreUtil.copy(childBehavior);

            // Construct the prefix name.
            String prefixName = callBehaviorActionToReplace.getName() + "__" + childBehaviorCopy.getName();

            // Prepend the prefix name to the name of all elements in the activity.
            NameHelper.prependPrefixNameToNodesAndEdgesInActivity(childBehaviorCopy, prefixName);

            // Prepend prefix ID (i.e., the IDs of the activity and the call behavior action) to the tracing comment of
            // all elements in the activity.
            IDHelper.prependPrefixIDToNodesAndEdgesInActivity(childBehaviorCopy, callBehaviorActionToReplace);

            // Get the activity of the call behavior action.
            Activity parentActivity = callBehaviorActionToReplace.getActivity();

            for (ActivityNode node: new ArrayList<>(childBehaviorCopy.getNodes())) {
                // Set the activity for the node.
                node.setActivity(parentActivity);

                // Set the activity for all the edges to the activity of the call behavior action to be replaced.
                for (ActivityEdge edge: node.getOutgoings()) {
                    edge.setActivity(parentActivity);
                }
                for (ActivityEdge edge: node.getIncomings()) {
                    edge.setActivity(parentActivity);
                }
                // Create a new edge for every pair of an outgoing edge from the activity's initial node and an
                // incoming edge to the call behavior action. The edges are properly connected and given the
                // appropriate properties, like guards. Name and tracing comment for the new edges are added.
                if (node instanceof InitialNode initialNode) {
                    for (ActivityEdge outgoingEdge: initialNode.getOutgoings()) {
                        for (ActivityEdge incomingEdge: callBehaviorActionToReplace.getIncomings()) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());

                            // The guard of the new edge is set to the guard of the incoming edge of the call
                            // behavior action. We ignore the guard of the outgoing edge of the initial node because
                            // the source of this edge is not a decision node.
                            newEdge.setGuard(EcoreUtil.copy(incomingEdge.getGuard()));
                            newEdge.setActivity(parentActivity);

                            // Add a name for the newly added edge.
                            newEdge.setName(incomingEdge.getName() + "__" + outgoingEdge.getName());

                            // Extract the IDs of the outer edge.
                            List<String> outerEdgeIDs = IDHelper.extractIDsFromTracingComment(incomingEdge);

                            // Add IDs for the newly added edge with the IDs of the outer edge.
                            for (String outerEdgeID: outerEdgeIDs) {
                                IDHelper.addTracingComment(newEdge, outerEdgeID);
                            }

                            // Extract the IDs of the inner edge.
                            List<String> innerEdgeIDs = IDHelper.extractIDsFromTracingComment(outgoingEdge);

                            // Add IDs for the newly added edge with the IDs of the inner edge.
                            for (String innerEdgeID: innerEdgeIDs) {
                                IDHelper.addTracingComment(newEdge, innerEdgeID);
                            }

                            // Add the structure info as a comment to the new edge.
                            structureInfoHelper.addStructureStartInfo(newEdge);
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
                    for (ActivityEdge incomingEdge: finalNode.getIncomings()) {
                        for (ActivityEdge outgoingEdge: callBehaviorActionToReplace.getOutgoings()) {
                            ControlFlow newEdge = FileHelper.FACTORY.createControlFlow();
                            newEdge.setSource(incomingEdge.getSource());
                            newEdge.setTarget(outgoingEdge.getTarget());

                            // The guard of the new edge is set to the guard of the incoming edge of the final node.
                            // We ignore the guard of the outgoing edge of the behavior action.
                            newEdge.setGuard(EcoreUtil.copy(incomingEdge.getGuard()));
                            newEdge.setActivity(parentActivity);

                            // Add a name for the newly added edge.
                            newEdge.setName(outgoingEdge.getName() + "__" + incomingEdge.getName());

                            // Extract the IDs of the outer edge.
                            List<String> outerEdgeIDs = IDHelper.extractIDsFromTracingComment(outgoingEdge);

                            // Add IDs for the newly added edge with the IDs of the outer edge.
                            for (String outerEdgeID: outerEdgeIDs) {
                                IDHelper.addTracingComment(newEdge, outerEdgeID);
                            }

                            // Extract the IDs of the inner edge.
                            List<String> innerEdgeIDs = IDHelper.extractIDsFromTracingComment(incomingEdge);

                            // Add IDs for the newly added edge with the IDs of the inner edge.
                            for (String innerEdgeID: innerEdgeIDs) {
                                IDHelper.addTracingComment(newEdge, innerEdgeID);
                            }

                            // Add the structure info as a comment to the new edge.
                            structureInfoHelper.addStructureEndInfo(newEdge);
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
