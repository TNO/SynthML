
package com.github.tno.pokayoke.transform.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
    private static final String TRACING_IDENTIFIER = "Original-ID-Path";

    private NameIDTracingHelper() {
    }

    /**
     * Gives name to all model elements. The name of the class is given to the element as its name if it does not have a
     * name yet. Otherwise, the original name is kept.
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
     * Ensures unique name for all enumerations in a model.
     *
     * @param model The model which contains enumerations.
     */
    public static void ensureUniqueNameForEnumerations(Model model) {
        // Collect the name of enumerations.
        List<String> names = new ArrayList<>();
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration) {
                names.add(member.getName());
            }
        }
        // Ensure each enumeration has a locally unique name within a set of enumerations.
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
     * Ensures unique name for all activities in a class.
     *
     * @param contextClass The context class which contains the activities.
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
     * Ensures locally unique name for all nodes and edges in an activity.
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
            names.add(newName);
            elemet.setName(newName);
        }
    }

    /**
     * Adds the ID of model elements to their comments.
     *
     * @param model The model that contains elements.
     */
    public static void addIDTracingCommentToModelElements(Model model) {
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                addTracingComment(namedElement, getID(namedElement));
            }
        }
    }

    /**
     * Prepends a prefix to the name of the nodes and edges in the activity called by the call behavior action.
     *
     * @param activity The activity in which the name of nodes and edges is prepended.
     * @param prefix The prefix to prepend.
     */
    public static void prependPrefixNameToNodesAndEdges(Activity activity, String prefix) {
        for (ActivityNode node: activity.getNodes()) {
            prependPrefixName(node, prefix);
        }

        for (ActivityEdge edge: activity.getEdges()) {
            prependPrefixName(edge, prefix);
        }
    }

    /**
     * Adds a tracing comment to a model element.
     *
     * @param element The model element.
     * @param id The ID added as a tracing comment of the model element.
     */
    public static void addTracingComment(NamedElement element, String id) {
        Comment comment = FileHelper.FACTORY.createComment();
        comment.setBody(TRACING_IDENTIFIER + ":" + id);
        element.getOwnedComments().add(comment);
    }

    /**
     * Prepends the ID to the comments of the nodes and edges in an activity.
     *
     * @param activity The activity that contains nodes and edges.
     * @param id The ID to prepend.
     */
    public static void prependPrefixIDToNodesAndEdgesInActivity(Activity activity, String id) {
        for (ActivityNode node: activity.getNodes()) {
            prependPrefixID(node, id);
        }

        for (ActivityEdge edge: activity.getEdges()) {
            prependPrefixID(edge, id);
        }
    }

    /**
     * Gets the ID of a model element.
     *
     * @param element The element.
     * @return The ID of the element.
     */
    public static String getID(NamedElement element) {
        return EcoreUtil.getURI(element).fragment();
    }

    /**
     * Extracts the ID from the tracing comment of the element.
     *
     * @param element The element that contains a tracing comment.
     * @return The ID in the tracing comment.
     */
    public static String extractIDFromTracingComment(NamedElement element) {
        List<String> tracingComments = new ArrayList<>();
        for (Comment comment: element.getOwnedComments()) {
            if (isTracingComment(comment)) {
                tracingComments.add(comment.getBody().split(":")[1]);
            }
        }
        return tracingComments.get(0);
    }

    /**
     * Prepends prefix name to the name of an element.
     *
     * @param element The element.
     * @param prefix The prefix name to prepend.
     */
    public static void prependPrefixName(NamedElement element, String prefix) {
        element.setName(prefix + "__" + element.getName());
    }

    private static String generateUniqueName(String originalName, List<String> names) {
        int i = 1;
        String generatedName = originalName + "_" + String.valueOf(i);
        while (names.contains(generatedName)) {
            i++;
            generatedName = originalName + "_" + String.valueOf(i);
        }

        return generatedName;
    }

    private static boolean isTracingComment(Comment comment) {
        return comment.getBody().split(":")[0].equals(TRACING_IDENTIFIER);
    }

    private static void prependPrefixID(NamedElement element, String absoluteID) {
        List<Comment> comments = element.getOwnedComments().stream().filter(c -> isTracingComment(c)).toList();

        for (Comment comment: comments) {
            // Split the comment body into header and ID chain.
            String[] commentBody = comment.getBody().split(":");
            comment.setBody(commentBody[0] + ":" + absoluteID + " " + commentBody[1]);
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
     * Checks the uniqueness of the name of the model elements.
     *
     * @param model The model to check.
     * @return {@code true} if all the elements have a unique name, otherwise, {@code false}.
     */
    public static boolean isNameOfModelElementsUnique(Model model) {
        Set<String> names = new HashSet<>();
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                String name = namedElement.getName();
                if (names.contains(name)) {
                    return false;
                } else {
                    names.add(name);
                }
            }
        }
        return true;
    }
}
