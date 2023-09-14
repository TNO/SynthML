
package com.github.tno.pokayoke.transform.common;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;

/** Helper class for renaming and tracing model elements in activities. */
public class NameIDTracingHelper {
    private static final List<String> OBJECT_TYPE = List.of("MergeNode", "ForkNode", "JoinNode", "DecisionNode",
            "CallBehaviorAction", "InitialNode", "FlowFinalNode", "ActivityFinalNode", "OpaqueAction", "ActivityEdge");

    private static final Map<String, Integer> OBJECT_COUNT = OBJECT_TYPE.stream()
            .collect(Collectors.toMap(key -> key, key -> 0));

    private final String tracingCommentIdentifier;

    public NameIDTracingHelper(String tracingCommentIdentifier) {
        this.tracingCommentIdentifier = tracingCommentIdentifier;
    }

    /**
     * Sets the name of a model element with a prefix name.
     *
     * @param element The model element to be updated.
     * @param prefix The prefix name of the model element.
     * @param className The class name of this element.
     */
    public static void setName(NamedElement element, String prefix, String className) {
        if (!NameIDTracingHelper.isNamed(element)) {
            NameIDTracingHelper.addName(element, className);
        }
        NameIDTracingHelper.prependPrefixName(element, prefix);
    }

    /**
     * Gets a unique name for an object.
     *
     * @param className The class name of the object.
     * @return A unique name for the object.
     */
    public static String getNameOfObject(String className) {
        String name = className + "_" + String.valueOf(OBJECT_COUNT.get(className) + 1);
        OBJECT_COUNT.put(className, OBJECT_COUNT.get(className) + 1);
        return name;
    }

    /**
     * Prepends the name of the call behavior action to the nodes and edges in the activity called by the call behavior
     * action.
     *
     * @param childBehavior The activity in which the name of nodes and edges is prepended with the name of the call
     *     behavior action.
     * @param name The name of the call behavior action.
     */
    public static void prependCallBehaviorActionNameToNodesAndEdgesInActivity(Activity childBehavior, String name) {
        for (ActivityNode node: childBehavior.getNodes()) {
            prependPrefixName(node, name);
        }

        for (ActivityEdge edge: childBehavior.getEdges()) {
            prependPrefixName(edge, name);
        }
    }

    /**
     * Sets the tracing comment of a model element with a prefix ID.
     *
     * @param element The element to be updated.
     * @param prefix The ID to be prepended to the comment.
     * @param tracingCommentIdentifier The identifier that distinguishes tracing comments from user comments.
     */
    public static void setTracingComment(NamedElement element, String prefix, String tracingCommentIdentifier) {
        if (!NameIDTracingHelper.hasTracingComment(element, tracingCommentIdentifier)) {
            String id = getID(element);
            NameIDTracingHelper.addTracingComment(element, id, tracingCommentIdentifier);
        }
        NameIDTracingHelper.prependPrefixID(element, prefix, tracingCommentIdentifier);
    }

    /**
     * Adds a tracing comment to a model element.
     *
     * @param element The model element.
     * @param id The ID added as a tracing comment of the model element.
     * @param tracingCommentIdentifier The identifier that distinguishes tracing comments from user comments.
     */
    public static void addTracingComment(NamedElement element, String id, String tracingCommentIdentifier) {
        Comment comment = FileHelper.FACTORY.createComment();
        comment.setBody(tracingCommentIdentifier + id);
        element.getOwnedComments().add(comment);
    }

    /**
     * Prepends the ID of a call behavior action to the comments of the nodes and edges in the activity called by this
     * call behavior action.
     *
     * @param childBehavior The activity called by the call behavior action.
     * @param id The ID of the call behavior action.
     * @param tracingCommentIdentifier The identifier that distinguishes tracing comments from user comments.
     */
    public static void prependCallBehaviorActionIDToNodesAndEdgesInActivity(Activity childBehavior, String id,
            String tracingCommentIdentifier)
    {
        for (ActivityNode node: childBehavior.getNodes()) {
            prependPrefixID(node, id, tracingCommentIdentifier);
        }

        for (ActivityEdge edge: childBehavior.getEdges()) {
            prependPrefixID(edge, id, tracingCommentIdentifier);
        }
    }

    /**
     * Gets the ID of a model element.
     *
     * @param element The element.
     * @return The ID of the element.
     */
    public static String getID(NamedElement element) {
        return element.eResource().getURIFragment(element);
    }

    private static boolean isNamed(NamedElement element) {
        return element.getName() != null && !element.getName().isEmpty();
    }

    private static void addName(NamedElement element, String className) {
        element.setName(NameIDTracingHelper.getNameOfObject(className));
    }

    static void prependPrefixName(NamedElement element, String prefix) {
        element.setName(prefix + "__" + element.getName());
    }

    private static boolean hasTracingComment(NamedElement element, String tracingCommentIdentifier) {
        return element.getOwnedComments().stream().filter(c -> isTracingComment(c, tracingCommentIdentifier)).findAny()
                .isPresent();
    }

    private static boolean isTracingComment(Comment comment, String tracingCommentIdentifier) {
        return comment.getBody().split(":")[0].equals(tracingCommentIdentifier);
    }

    private static void prependPrefixID(NamedElement element, String absoluteID, String tracingCommentIdentifier) {
        List<Comment> comments = element.getOwnedComments().stream()
                .filter(c -> isTracingComment(c, tracingCommentIdentifier)).toList();

        for (Comment comment: comments) {
            // Split the comment body into header and ID chain.
            String[] commentBody = comment.getBody().split(":");
            comment.setBody(commentBody[0] + ":" + absoluteID + "__" + commentBody[1]);
        }
    }

    /**
     * Checks if a double underscore is used in the name of any model elements.
     *
     * @param model The model to check.
     * @return {@code true} if the model has a model element whose name contains a double underscore, {@code false}
     *     otherwise.
     */
    public static boolean isDoubleUnderscoreUsed(Model model) {
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                if (namedElement.getName() != null && namedElement.getName().contains("__")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Ensures all named elements in the model have unique names, by renaming elements in case duplicate names are used.
     *
     * @param model The model to check.
     */
    public static void ensureUniquenessOfNames(Model model) {
        Set<String> names = new HashSet<>();
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                if (names.contains(namedElement.getName())) {
                    String newName = namedElement.getName() + "_1";
                    namedElement.setName(newName);
                    names.add(newName);
                }
            }
        }
    }

    /** Gets Tracing comment identifier.
     * @return the tracingCommentIdentifier
     */
    public String getTracingCommentIdentifier() {
        return tracingCommentIdentifier;
    }
}
