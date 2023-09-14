
package com.github.tno.pokayoke.transform.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

/** Helper class for renaming and tracing model elements in activities. */
public class NameIDTracingHelper {
    private NameIDTracingHelper() {
    }

    /**
     * Gives name to all model elements.
     *
     * @param model The model.
     */
    public static void giveNameToModelElements(Model model) {
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                String name = namedElement.getName();
                if (name == null || name.isEmpty()) {
                    namedElement.setName(namedElement.eClass().getName());
                }
            }
        }
    }

    /**
     * Ensures unique name for enumerations in a model.
     *
     * @param model The model which contains enumerations.
     */
    public static void ensureUniqueNameForEnumerations(Model model) {
        // Collect name of enumerations.
        List<String> names = new ArrayList<>();
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration) {
                names.add(member.getName());
            }
        }
        // Ensure each enumeration has a unique local name within a set of enumerations.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration) {
                ensureUniqueNameForElement(member, names);
            }
        }
    }

    /**
     * Ensures unique name for properties in a model.
     *
     * @param model The model which contains properties.
     */
    public static void ensureUniqueNameForProperties(Model model) {
        // Collect name of properties.
        List<String> names = new ArrayList<>();
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Property) {
                names.add(member.getName());
            }
        }
        // Ensure each property has a unique local name within a set of properties.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Property) {
                ensureUniqueNameForElement(member, names);
            }
        }
    }

    /**
     * Ensures unique name for activities in a model.
     *
     * @param contextClass The context class where activities are found.
     */
    public static void ensureUniqueNameForActivities(Class contextClass) {
        // Collect name of activities.
        List<String> names = new ArrayList<>();
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity) {
                names.add(behavior.getName());
            }
        }
        // Ensure each activity has a unique local name within a set of activities.
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity) {
                ensureUniqueNameForElement(behavior, names);
            }
        }
    }

    /**
     * Ensures locally unique name for all nodes and edges in a activity.
     *
     * @param activity The activity.
     */
    public static void ensureUniqueNameForNodesAndEdges(Activity activity) {
        // Collect name of nodes.
        List<String> nodeNames = new ArrayList<>();
        for (ActivityNode node: activity.getNodes()) {
            nodeNames.add(node.getName());
        }

        // Ensure unique name for nodes.
        for (ActivityNode node: activity.getNodes()) {
            ensureUniqueNameForElement(node, nodeNames);
        }

        // Collect name of edges.
        List<String> edgeNames = new ArrayList<>();
        for (ActivityEdge edge: activity.getEdges()) {
            edgeNames.add(edge.getName());
        }

        // Ensure unique name for edges.
        for (ActivityEdge edge: activity.getEdges()) {
            ensureUniqueNameForElement(edge, edgeNames);
        }
    }

    /**
     * Ensures locally unique name for all enumeration literals in an enumeration.
     *
     * @param enumeration The enumeration.
     */
    public static void ensureUniqueNameForEnumerationLiterals(Enumeration enumeration) {
        // Collect name of enumeration literals.
        List<String> names = new ArrayList<>();
        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            names.add(literal.getName());
        }

        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            ensureUniqueNameForElement(literal, names);
        }
    }

    /**
     * Ensures that the name of an element has no duplications in the provided name space.
     *
     * @param elemet The element.
     * @param names The name space.
     */
    private static void ensureUniqueNameForElement(NamedElement elemet, List<String> names) {
        String originalName = elemet.getName();
        int count = Collections.frequency(names, originalName);
        // Rename the element if there are duplications.
        if (count > 1) {
            String newName = generateUniqueName(originalName, names);
            names.remove(originalName);
            names.add(newName);
            elemet.setName(newName);
        }
    }

    /**
     * Generates a name that has no duplications in the provided name space by modifying the original name.
     *
     * @param originalName The original name.
     * @param names The name space.
     * @return A unique name.
     */
    private static String generateUniqueName(String originalName, List<String> names) {
        int i = 1;
        String generatedName = originalName + "_" + String.valueOf(i);
        while (names.contains(generatedName)) {
            i++;
            generatedName = originalName + "_" + String.valueOf(i);
        }

        return generatedName;
    }

    public static void giveIDToModelElements(Model model) {
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                addTracingComment(namedElement, getID(namedElement), "Original-ID-Path:");
            }
        }
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
}
