
package com.github.tno.pokayoke.transform.common;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Namespace;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ValueSpecification;

/** Helper class for renaming model elements. */
public class NameHelper {
    private NameHelper() {
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
                if (shouldBeNamed(namedElement)) {
                    String name = namedElement.getName();
                    if (name == null || name.isEmpty()) {
                        namedElement.setName(namedElement.eClass().getName());
                    }
                }
            }
        }
    }

    /**
     * Check whether an element should be named.
     *
     * @param namedElement The element to check.
     * @return {@code true} if the element should have a name, {@code false} otherwise.
     */
    private static boolean shouldBeNamed(NamedElement namedElement) {
        return !(namedElement instanceof ValueSpecification);
    }

    /**
     * Ensures unique names for enumerations, properties and activities in a namespace, within their scope.
     *
     * @param namespace The namespace to check.
     * @param names The names of enumerations, properties and activities in the context of the namespace.
     */
    public static void ensureUniqueNameForEnumerationsPropertiesActivities(Namespace namespace,
            Map<String, Integer> names)
    {
        Map<String, Integer> localNames = new LinkedHashMap<>(names);

        // Helper for iterating over all enumeration, property and activity members directly within the namespace.
        Consumer<Consumer<NamedElement>> memberIterator = elementConsumer -> {
            for (NamedElement member: namespace.getOwnedMembers()) {
                if (member instanceof Enumeration || member instanceof Property || member instanceof Activity) {
                    elementConsumer.accept(member);
                }
            }
        };

        // Collect the names of all enumeration, property and activity members.
        memberIterator.accept(element -> updateNameMap(element, localNames));

        // Ensure unique names for all enumeration, property and activity members.
        memberIterator.accept(element -> ensureUniqueNameForElement(element, localNames));

        // Recursively consider all nested classes and models.
        for (Element member: namespace.getOwnedMembers()) {
            if (member instanceof Class classMember) {
                ensureUniqueNameForEnumerationsPropertiesActivities(classMember, localNames);
            } else if (member instanceof Model modelMember) {
                ensureUniqueNameForEnumerationsPropertiesActivities(modelMember, localNames);
            }
        }
    }

    /**
     * Ensures locally unique names for all elements in each activity in the given UML element.
     *
     * @param element The UML element that contains activities, either directly or nested in models or classes.
     */
    public static void ensureUniqueNameForElementsInActivities(Element element) {
        if (element instanceof Activity activityElement) {
            ensureUniqueNameForNodesAndEdges(activityElement);
        } else if (element instanceof Class classElement) {
            classElement.getOwnedMembers().forEach(NameHelper::ensureUniqueNameForElementsInActivities);
        } else if (element instanceof Model modelElement) {
            modelElement.getOwnedMembers().forEach(NameHelper::ensureUniqueNameForElementsInActivities);
        }
    }

    /**
     * Ensures locally unique name for all nodes and edges in an activity.
     *
     * @param activity The activity.
     */
    public static void ensureUniqueNameForNodesAndEdges(Activity activity) {
        // Collect names of nodes.
        Map<String, Integer> names = new HashMap<>();
        for (ActivityNode node: activity.getNodes()) {
            updateNameMap(node, names);
        }

        // Collect names of edges.
        for (ActivityEdge edge: activity.getEdges()) {
            updateNameMap(edge, names);
        }

        // Ensure unique names for nodes.
        for (ActivityNode node: activity.getNodes()) {
            ensureUniqueNameForElement(node, names);
        }

        // Ensure unique names for edges.
        for (ActivityEdge edge: activity.getEdges()) {
            ensureUniqueNameForElement(edge, names);
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
     * Prepends a prefix name to the name of an element.
     *
     * @param element The element.
     * @param prefix The prefix name to prepend.
     */
    private static void prependPrefixName(NamedElement element, String prefix) {
        element.setName(prefix + "__" + element.getName());
    }

    /**
     * Prepend the name of the outer activity to the nodes and edges in activities within the given UML element.
     *
     * @param element The UML element that contains activities, either directly or nested in models or classes.
     */
    public static void prependOuterActivityNameToNodesAndEdgesInActivities(Element element) {
        if (element instanceof Activity activityElement) {
            prependPrefixNameToNodesAndEdgesInActivity(activityElement, activityElement.getName());
        } else if (element instanceof Class classElement) {
            classElement.getOwnedMembers().forEach(NameHelper::prependOuterActivityNameToNodesAndEdgesInActivities);
        } else if (element instanceof Model modelElement) {
            modelElement.getOwnedMembers().forEach(NameHelper::prependOuterActivityNameToNodesAndEdgesInActivities);
        }
    }
}
