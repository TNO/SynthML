
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
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/** Flatten nested UML activity. */
public class FlattenUMLActivity {
    private final Model model;

    public FlattenUMLActivity(Model model) {
        this.model = model;
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        new FlattenUMLActivity(model).transformModel();
        FileHelper.storeModel(model, targetPath);
    }

    private void transformModel() {
        // Extract context class.
        Class contextClass = (Class)model.getMember("Context");

        // Step 1: make sure that no double underscores exist in the names of model elements.

        // Clean the irrelevant info from edges so that double underscores do not exist in the default name of Boolean
        // literal of guards on edges that are not the outgoing edges of decision nodes. These guards do not have a
        // clear meaning and are automatically added by UML designers.
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity activity) {
                UMLActivityUtils.removeIrrelevantInformation(activity);
            }
        }

        // Check that no double underscores exist in the name of any other model elements.
        Preconditions.checkArgument(!NameIDTracingHelper.isDoubleUnderscoreUsed(model),
                "Expected double underscores to not be used in the names of model elements.");

        // Step 2: Give each element a name.
        NameIDTracingHelper.giveNameToModelElements(model);

        // Step 3: Ensure all names are locally unique within their scope.
        // Ensure if each enumeration has a unique local name within a set of enumerations.
        NameIDTracingHelper.ensureUniqueNameForEnumerations(model);

        // Ensure all literals in each enumeration have a unique local name.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration enumeration) {
                NameIDTracingHelper.ensureUniqueNameForEnumerationLiterals(enumeration);
            }
        }

        // Ensure each property has a unique name within a set of properties.
        NameIDTracingHelper.ensureUniqueNameForProperties(model);

        // Ensure each activity has unique name within a set of activities.
        NameIDTracingHelper.ensureUniqueNameForActivities(contextClass);

        // Ensure all elements in each activity have a local unique name.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                NameIDTracingHelper.ensureUniqueNameForNodesAndEdges(activity);
            }
        }

        // Step 4: Give every element an ID.
        NameIDTracingHelper.giveIDToModelElements(model);

        // Step 5: Flatten all activity behaviors of the context class.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                transformActivity(activity, null);
            }
        }

        // Step 6: Check that the element names of the model are unique globally.
        assert NameIDTracingHelper.checkUniquenessofNames(model) : "The name of the model element is not unique.";
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
    public void transformActivity(Activity childBehavior, CallBehaviorAction callBehaviorActionToReplace) {
        // Depth-first recursion. Transform children first, for a bottom-up flattening.
        for (ActivityNode node: new ArrayList<>(childBehavior.getNodes())) {
            if (node instanceof CallBehaviorAction actionNode) {
                Behavior childActivity = actionNode.getBehavior();
                Verify.verifyNotNull(childActivity, String
                        .format("The behavior of the call behavior action %s is unspecified.", actionNode.getName()));

                transformActivity((Activity)childActivity, actionNode);
            }
        }

        // Replace the call behavior action with the objects of this activity. Prepend the name and ID of the call
        // behavior action to the name and tracing comment of objects in this activity, respectively. Connect the
        // objects properly to the outer activity.
        if (callBehaviorActionToReplace != null) {
            Activity childBehaviorCopy = EcoreUtil.copy(childBehavior);

            // Construct the prefix name.
            String prefixName = callBehaviorActionToReplace.getName() + "__" + childBehaviorCopy.getName();

            // Prepend the prefix name to the name of all elements in the activity.
            NameIDTracingHelper.prependPrefixNameToNodesAndEdges(childBehaviorCopy, prefixName);

            // Extract the ID of the call behavior action and the activity.
            String actionTracingComment = NameIDTracingHelper.getIDFromTracingComments(callBehaviorActionToReplace);
            String activityTracingComment = NameIDTracingHelper.getIDFromTracingComments(childBehaviorCopy);

            // Construct the prefix ID.
            String prefixID = actionTracingComment + " " + activityTracingComment;

            // Prepend the prefix ID to the tracing comment of all elements in the activity.
            NameIDTracingHelper.prependPrefixIDToNodesAndEdges(childBehaviorCopy, prefixID);

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
                            newEdge.setName(incomingEdge.getName() + "__" + outgoingEdge.getName());

                            // Get prefix ID of the outerEdge.
                            String outerEdgePrefixID = NameIDTracingHelper.getIDFromTracingComments(incomingEdge);

                            // Prepend the ID for the newly added edge.
                            NameIDTracingHelper.addTracingComment(newEdge, outerEdgePrefixID);

                            // Get prefix ID of the inner edge.
                            String innerEdgePrefixID = NameIDTracingHelper.getIDFromTracingComments(outgoingEdge);

                            // Prepend the ID for the newly added edge.
                            NameIDTracingHelper.addTracingComment(newEdge, innerEdgePrefixID);
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
                            newEdge.setName(outgoingEdge.getName() + "__" + incomingEdge.getName());

                            // Get prefix ID of the outer edge.
                            String outerEdgePrefixID = NameIDTracingHelper.getIDFromTracingComments(outgoingEdge);

                            // Prepend the ID for the newly added edge.
                            NameIDTracingHelper.addTracingComment(newEdge, outerEdgePrefixID);

                            // Get prefix ID of the inner edge.
                            String innerEdgePrefixID = NameIDTracingHelper.getIDFromTracingComments(incomingEdge);

                            // Prepend the ID for the newly added edge.
                            NameIDTracingHelper.addTracingComment(newEdge, innerEdgePrefixID);
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
