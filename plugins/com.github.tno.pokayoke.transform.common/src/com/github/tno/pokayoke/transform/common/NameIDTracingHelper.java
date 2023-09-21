
package com.github.tno.pokayoke.transform.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

import com.google.common.base.Verify;

/** Helper class for renaming and tracing model elements in activities. */
public class NameIDTracingHelper {
    private static final String TRACING_IDENTIFIER = "Original-ID-Path";

    private NameIDTracingHelper() {
    }

    /**
     * Gives a name to each model element. The name of the class is given to the element as its name if it does not have
     * a name yet. Otherwise, the original name is kept.
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
     * Ensures unique name for all enumerations and properties in a model.
     *
     * @param model The model which contains enumerations and properties.
     */
    public static void ensureUniqueNameForEnumerationsAndProperties(Model model) {
        // Collect the name of enumerations.
        Map<String, Integer> names = new HashMap<>();

        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration) {
                updateNameMap(member, names);
            }
        }

        // Collect the name of properties.
        Class contextClass = (Class)model.getMember("Context");
        for (NamedElement element: contextClass.getAllAttributes()) {
            if (element instanceof Property property) {
                updateNameMap(property, names);
            }
        }

        // Ensure each enumeration has a locally unique name within a set of enumerations and properties.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration) {
                ensureUniqueNameForElement(member, names);
            }
        }

        // Ensure each property has a locally unique name within a set of enumerations and properties.
        for (NamedElement element: contextClass.getAllAttributes()) {
            if (element instanceof Property property) {
                ensureUniqueNameForElement(property, names);
            }
        }
    }

    private static void updateNameMap(NamedElement member, Map<String, Integer> names) {
        String name = member.getName();
        if (!names.containsKey(name)) {
            names.put(name, 1);
        } else {
            names.put(name, names.get(name) + 1);
        }
    }

    /**
     * Ensures that the name of an element has no duplications in the provided name space.
     *
     * @param element The element.
     * @param names The name space.
     */
    private static void ensureUniqueNameForElement(NamedElement element, Map<String, Integer> names) {
        String originalName = element.getName();
        int count = names.get(originalName);

        // Rename the element if there are duplications.
        if (count > 1) {
            String newName = generateUniqueName(originalName, names);
            names.put(newName, 1);
            element.setName(newName);
        }
    }

    private static String generateUniqueName(String originalName, Map<String, Integer> names) {
        int i = 1;
        String generatedName = originalName + "_" + String.valueOf(i);
        while (names.containsKey(generatedName)) {
            i++;
            generatedName = originalName + "_" + String.valueOf(i);
        }

        return generatedName;
    }

    /**
     * Ensures locally unique name for enumeration literals in all enumerations.
     *
     * @param model The model that contains the enumerations.
     */
    public static void ensureUniqueNameForEnumerationLiteralsInEnumerations(Model model) {
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration enumeration) {
                ensureUniqueNameForEnumerationLiterals(enumeration);
            }
        }
    }

    /**
     * Ensures locally unique name for all enumeration literals in an enumeration.
     *
     * @param enumeration The enumeration.
     */
    private static void ensureUniqueNameForEnumerationLiterals(Enumeration enumeration) {
        // Collect name of enumeration literals.
        Map<String, Integer> names = new HashMap<>();
        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            updateNameMap(literal, names);
        }

        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            ensureUniqueNameForElement(literal, names);
        }
    }

    /**
     * Ensures unique name for all activities in a class.
     *
     * @param contextClass The context class which contains the activities.
     */
    public static void ensureUniqueNameForActivities(Class contextClass) {
        // Collect name of activities.
        Map<String, Integer> names = new HashMap<>();
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity activity) {
                updateNameMap(activity, names);
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
     * Ensures locally unique name for all elements in each activity.
     *
     * @param contextClass The class that contains activities.
     */
    public static void ensureUniqueNameForElementsInActivities(Class contextClass) {
        for (Behavior behavior: contextClass.getOwnedBehaviors()) {
            if (behavior instanceof Activity activity) {
                NameIDTracingHelper.ensureUniqueNameForNodesAndEdges(activity);
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
        Map<String, Integer> nodeNames = new HashMap<>();
        for (ActivityNode node: activity.getNodes()) {
            updateNameMap(node, nodeNames);
        }

        // Ensure unique name for nodes.
        for (ActivityNode node: activity.getNodes()) {
            ensureUniqueNameForElement(node, nodeNames);
        }

        // Collect name of edges.
        Map<String, Integer> edgeNames = new HashMap<>();
        for (ActivityEdge edge: activity.getEdges()) {
            updateNameMap(edge, edgeNames);
        }

        // Ensure unique name for edges.
        for (ActivityEdge edge: activity.getEdges()) {
            ensureUniqueNameForElement(edge, edgeNames);
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
     * Adds a tracing comment to a model element.
     *
     * @param element The model element.
     * @param id The ID to add as a tracing comment of the model element.
     */
    public static void addTracingComment(NamedElement element, String id) {
        Comment comment = FileHelper.FACTORY.createComment();
        comment.setBody(TRACING_IDENTIFIER + ":" + id);
        element.getOwnedComments().add(comment);
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
     * Prepends a prefix to the name of the nodes and edges in the activity called by the call behavior action.
     *
     * @param activity The activity in which the name of nodes and edges is prepended.
     * @param prefix The prefix to prepend.
     */
    public static void prependPrefixNameToNodesAndEdgesInActivity(Activity activity, String prefix) {
        for (ActivityNode node: activity.getNodes()) {
            prependPrefixName(node, prefix);
        }

        for (ActivityEdge edge: activity.getEdges()) {
            prependPrefixName(edge, prefix);
        }
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

    /**
     * Prepends the IDs of the call behavior action and activity to the comments of the nodes and edges in an activity.
     *
     * @param activity The activity that contains nodes and edges.
     * @param action The call behavior action that calls the activity.
     */
    public static void prependPrefixIDToNodesAndEdgesInActivity(Activity activity, CallBehaviorAction action) {
        String id = extractIDFromTracingComment(action) + " " + extractIDFromTracingComment(activity);

        for (ActivityNode node: activity.getNodes()) {
            prependPrefixID(node, id);
        }

        for (ActivityEdge edge: activity.getEdges()) {
            prependPrefixID(edge, id);
        }
    }

    private static void prependPrefixID(NamedElement element, String prefixID) {
        List<Comment> comments = element.getOwnedComments().stream().filter(c -> isTracingComment(c)).toList();

        for (Comment comment: comments) {
            // Split the comment body into header and ID chain.
            String[] commentBody = comment.getBody().split(":");
            comment.setBody(commentBody[0] + ":" + prefixID + " " + commentBody[1]);
        }
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
        return String.join(" ", tracingComments);
    }

    private static boolean isTracingComment(Comment comment) {
        return comment.getBody().split(":")[0].equals(TRACING_IDENTIFIER);
    }

    /** Prepend the name of the outer activity to the nodes and edges in activities.
     * @param contextClass The class that contains activities.
     */
    public static void prependOuterActivityNameToNodesAndEdgesInActivities(Class contextClass) {
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                prependPrefixNameToNodesAndEdgesInActivity(activity, activity.getName());
            }
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
     */
    public static void checkUniquenessOfNames(Model model) {
        Set<String> names = new HashSet<>();
        TreeIterator<EObject> iterator = model.eAllContents();
        while (iterator.hasNext()) {
            EObject eObject = iterator.next();
            if (eObject instanceof NamedElement namedElement) {
                String name = namedElement.getName();
                Verify.verify(!names.contains(name), String.format("Model name %s is not globally unique.", name));
                names.add(name);
            }
        }
    }
}
