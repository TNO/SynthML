
package com.github.tno.pokayoke.transform.flatten;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueBehavior;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.common.IDHelper;
import com.github.tno.pokayoke.transform.common.NameHelper;
import com.github.tno.pokayoke.transform.common.StructureInfoHelper;
import com.github.tno.pokayoke.transform.common.ValidationHelper;
import com.github.tno.pokayoke.uml.profile.util.UMLActivityUtils;

/** Flattens nested UML activities. */
public class FlattenUMLActivity {
    private final Model model;

    private final StructureInfoHelper structureInfoHelper;

    public FlattenUMLActivity(Model model) {
        this.model = model;
        this.structureInfoHelper = new StructureInfoHelper();
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException, CoreException {
        Model model = FileHelper.loadModel(sourcePath);
        new FlattenUMLActivity(model).transform();
        FileHelper.storeModel(model, targetPath);
    }

    public void transform() throws CoreException {
        // Check whether the model has the expected structure, particularly that no double underscores exist in the
        // names of relevant model elements.
        ValidationHelper.validateModel(model);

        // Give each element a name.
        NameHelper.giveNameToModelElements(model);

        // Ensure that all names of nodes and edges within activities are locally unique within their scope.
        NameHelper.ensureUniqueNameForElementsInActivities(model);

        // Give every element an ID.
        IDHelper.addIDTracingCommentToModelElements(model);

        // Transform all elements within the model.
        transform(model);

        // Add structure comments to the outgoing edges of the initial nodes and the incoming edges of the final nodes
        // in all outermost activities.
        structureInfoHelper.addStructureInfoInActivities(model);

        // Prepend the name of the outer activity to the model elements in activities.
        NameHelper.prependOuterActivityNameToNodesAndEdgesInActivities(model);
    }

    private void transform(Element element) {
        if (element instanceof Activity activityElement) {
            transformActivity(activityElement, null);
        } else if (element instanceof Class classElement) {
            classElement.getOwnedMembers().forEach(this::transform);
        } else if (element instanceof Model modelElement) {
            modelElement.getOwnedMembers().forEach(this::transform);
        }
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
            // Check whether the current child node needs to be transformed.
            if (node instanceof CallBehaviorAction action) {
                Behavior behavior = action.getBehavior();

                // Do not flatten stereotyped CallBehaviorActions as they should be considered leafs.
                if (behavior instanceof Activity activity && action.getAppliedStereotypes().isEmpty()) {
                    transformActivity(activity, action);
                } else if (behavior instanceof OpaqueBehavior) {
                    throw new RuntimeException("Call opaque behavior actions are currently unsupported.");
                }
            }
        }

        // Clean the irrelevant info from edges so that double underscores do not exist in the default name of Boolean
        // literals of guards on edges that are not the outgoing edges of decision nodes. These guards do not have a
        // clear meaning and are automatically added by UML Designer.
        UMLActivityUtils.removeIrrelevantInformation(childBehavior);

        // Replace the call behavior action with the objects of this activity. Prepend the name and ID of the call
        // behavior action and the activity to the name and tracing comment of objects in this activity, respectively.
        // Connect the objects properly to the outer activity and add structure info comments.
        if (callBehaviorActionToReplace != null) {
            // Increment the counter for structure info comments, for call behavior actions.
            structureInfoHelper.incrementCounter();

            Activity childBehaviorCopy = copyWithProfiles(childBehavior);

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

                // Creates a decision node to substitute the activity's initial node. This maintains the original
                // structure, and the edges can keep their guards. No tracing comments are needed.
                if (node instanceof InitialNode initialNode) {
                    // Create dummy node to substitute the initial node, and redirect the relevant edges.
                    DecisionNode initialNodeSub = FileHelper.FACTORY.createDecisionNode();
                    for (ActivityEdge outgoingEdge: new ArrayList<>(initialNode.getOutgoings())) {
                        outgoingEdge.setSource(initialNodeSub);
                    }
                    for (ActivityEdge incomingEdge: new ArrayList<>(callBehaviorActionToReplace.getIncomings())) {
                        incomingEdge.setTarget(initialNodeSub);
                    }

                    // Destroy the initial node.
                    initialNode.destroy();
                }

                // Creates a merge node to substitute the activity's final node. This maintains the original structure,
                // and the edges can keep their guards. No tracing comments are needed.
                if (node instanceof ActivityFinalNode finalNode) {
                    // Create dummy node to substitute the activity final node, and redirect the relevant edges.
                    MergeNode finalNodeSub = FileHelper.FACTORY.createMergeNode();
                    for (ActivityEdge incomingEdge: new ArrayList<>(finalNode.getIncomings())) {
                        incomingEdge.setTarget(finalNodeSub);
                    }
                    for (ActivityEdge outgoingEdge: new ArrayList<>(callBehaviorActionToReplace.getOutgoings())) {
                        outgoingEdge.setSource(finalNodeSub);
                    }

                    // Destroy the final node.
                    finalNode.destroy();
                }
            }

            // Destroy the call behavior action being replaced.
            callBehaviorActionToReplace.destroy();
        }
    }

    private static <T extends Element> T copyWithProfiles(T source) {
        Copier copier = new Copier();
        @SuppressWarnings("unchecked")
        T result = (T)copier.copy(source);
        // Also copy the stereotype applications to preserve the profile properties
        List<EObject> stereotypeApplications = source.allOwnedElements().stream()
                .flatMap(e -> e.getStereotypeApplications().stream()).collect(Collectors.toList());
        source.eResource().getContents().addAll(copier.copyAll(stereotypeApplications));
        copier.copyReferences();
        return result;
    }
}
