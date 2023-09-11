
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

public class FlatteningHelper {
    private static final List<String> OBJECT_TYPE = List.of("MergeNode", "ForkNode", "JoinNode", "DecisionNode",
            "CallBehaviorAction", "InitialNode", "FlowFinalNode", "ActivityFinalNode", "OpaqueAction", "ActivityEdge");

    private static final Map<String, Integer> OBJECT_COUNT = OBJECT_TYPE.stream()
            .collect(Collectors.toMap(key -> key, key -> 0));

    private FlatteningHelper() {
    }

    /**
     * Sets the name of a model element with a prefix name.
     *
     * @param element The model element to be updated.
     * @param prefix The prefix name of the model element.
     * @param className The class name of this element.
     */
    public static void setName(NamedElement element, String prefix, String className) {
        if (!FlatteningHelper.isNamed(element)) {
            FlatteningHelper.addName(element, className);
        }
        FlatteningHelper.appendPrefix(element, prefix);
    }

    /**
     * Gets a unique name for an object.
     *
     * @param className The class name of the object.
     * @return A unique name of the object.
     */
    public static String getNameOfObject(String className) {
        String name = className + "_" + String.valueOf(OBJECT_COUNT.get(className) + 1);
        OBJECT_COUNT.put(className, OBJECT_COUNT.get(className) + 1);
        return name;
    }

    /**
     * Appends the name of the call behavior action to the nodes and edges in the activity called by the call behavior
     * action.
     *
     * @param childBehavior The activity diagram in which the name of nodes and edges is appended with the name of the
     *     call behavior action.
     * @param name The name of the call behavior action.
     */
    public static void appendCallBehaviorActionName(Activity childBehavior, String name) {
        for (ActivityNode node: childBehavior.getNodes()) {
            appendPrefix(node, name);
        }

        for (ActivityEdge edge: childBehavior.getEdges()) {
            appendPrefix(edge, name);
        }
    }

    /**
     * Sets the tracing comment of a model element with a prefix ID.
     *
     * @param element The element to be updated.
     * @param prefix The ID to be appended to the comment.
     * @param tracingCommentIdentifier The identifier that distinguishes tracing comments from user comments.
     */
    public static void setTracingComment(NamedElement element, String prefix, String tracingCommentIdentifier) {
        if (!FlatteningHelper.isTracingCommentExist(element, tracingCommentIdentifier)) {
            String id = element.eResource().getURIFragment(element);
            FlatteningHelper.addTracingComment(element, id, tracingCommentIdentifier);
        }
        FlatteningHelper.appendPrefixToTracingComment(element, prefix, tracingCommentIdentifier);
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
     * Appends the ID of a call behavior action to the comments of the nodes and edges in the activity called by this
     * call behavior action.
     *
     * @param childBehavior The activity called by the call behavior action.
     * @param id The ID of the call behavior action.
     * @param tracingCommentIdentifier The identifier that distinguishes tracing comments from user comments.
     */
    public static void appendCallBehaviorActionID(Activity childBehavior, String id, String tracingCommentIdentifier) {
        for (ActivityNode node: childBehavior.getNodes()) {
            appendPrefixToTracingComment(node, id, tracingCommentIdentifier);
        }

        for (ActivityEdge edge: childBehavior.getEdges()) {
            appendPrefixToTracingComment(edge, id, tracingCommentIdentifier);
        }
    }

    private static boolean isNamed(NamedElement element) {
        return element.getName() != null && !element.getName().isEmpty();
    }

    private static void addName(NamedElement element, String className) {
        element.setName(FlatteningHelper.getNameOfObject(className));
    }

    private static void appendPrefix(NamedElement element, String prefix) {
        element.setName(prefix + "__" + element.getName());
    }

    private static boolean isTracingCommentExist(NamedElement element, String tracingCommentIdentifier) {
        return element.getOwnedComments().stream().filter(c -> c.getBody().contains(tracingCommentIdentifier)).toList()
                .size() != 0;
    }

    private static void appendPrefixToTracingComment(NamedElement element, String absoluteID,
            String tracingCommentIdentifier)
    {
        List<Comment> comments = element.getOwnedComments().stream()
                .filter(c -> c.getBody().contains(tracingCommentIdentifier)).toList();

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
     * Checks if all names are unique in the model and gives a unique name if a duplication is found.
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
                    names.add(namedElement.getName() + "_1");
                }
            }
        }
    }
}
