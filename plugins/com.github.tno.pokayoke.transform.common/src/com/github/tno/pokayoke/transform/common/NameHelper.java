
package com.github.tno.pokayoke.transform.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ValueSpecification;

import com.google.common.base.Verify;

/** Helper class for renaming model elements. */

public class NameHelper {
    private NameHelper() {
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

    // TODO update
    /**
     * Ensures locally unique names for enumerations, properties and activities in a model, within their scope.
     *
     * @param model The model to check.
     */
    public static void ensureUniqueNameForEnumerationsPropertiesActivities(Model model) {
        Map<String, Integer> namesWithinModelScope = new HashMap<>();

        // Collect names of enumerations.
        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration enumeration) {
                updateNameMap(enumeration, namesWithinModelScope);
            }
        }

        // Iterate over all classes, collect all class-local names, and check their uniqueness.
        for (PackageableElement element: model.getPackagedElements()) {
            Map<String, Integer> namesWithinClassScope = new HashMap<>(namesWithinModelScope);

            if (element instanceof Class classElement) {
                // Collect names of class properties.
                for (NamedElement attribute: classElement.getAllAttributes()) {
                    if (attribute instanceof Property property) {
                        updateNameMap(property, namesWithinClassScope);
                    }
                }

                // Collect names of class activities.
                for (Behavior behavior: classElement.getOwnedBehaviors()) {
                    if (behavior instanceof Activity activity) {
                        updateNameMap(activity, namesWithinClassScope);
                    }
                }

                // Ensure unique names for the enumerations.
                for (NamedElement member: model.getMembers()) {
                    if (member instanceof Enumeration enumeration) {
                        ensureUniqueNameForElement(enumeration, namesWithinClassScope);
                    }
                }

                // Ensure unique names for the class properties.
                for (NamedElement attribute: classElement.getAllAttributes()) {
                    if (attribute instanceof Property property) {
                        ensureUniqueNameForElement(property, namesWithinClassScope);
                    }
                }

                // Ensure unique names for the class activities.
                for (Behavior behavior: classElement.getOwnedBehaviors()) {
                    if (behavior instanceof Activity activity) {
                        ensureUniqueNameForElement(activity, namesWithinClassScope);
                    }
                }
            }
        }
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
        // Collect names of enumeration literals.
        Map<String, Integer> names = new HashMap<>();
        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            updateNameMap(literal, names);
        }

        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            ensureUniqueNameForElement(literal, names);
        }

        // Prepend the names of enumeration to the names of enumeration literals.
        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            prependPrefixName(literal, enumeration.getName());
        }
    }

    /**
     * Ensures locally unique names for all elements in each activity in the given UML element.
     *
     * @param element The UML element that contains activities, either directly or nested in models or classes.
     */
    public static void ensureUniqueNameForElementsInActivities(Element element) {
        if (element instanceof Model modelElement) {
            modelElement.getOwnedElements().forEach(NameHelper::ensureUniqueNameForElementsInActivities);
        } else if (element instanceof Activity activityElement) {
            ensureUniqueNameForNodesAndEdges(activityElement);
        } else if (element instanceof Class classElement) {
            for (Behavior behavior: classElement.getOwnedBehaviors()) {
                if (behavior instanceof Activity activity) {
                    ensureUniqueNameForNodesAndEdges(activity);
                }
            }
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
        if (element instanceof Model modelElement) {
            modelElement.getOwnedElements().forEach(NameHelper::prependOuterActivityNameToNodesAndEdgesInActivities);
        } else if (element instanceof Activity activityElement) {
            prependPrefixNameToNodesAndEdgesInActivity(activityElement, activityElement.getName());
        } else if (element instanceof Class classElement) {
            for (Behavior behavior: new ArrayList<>(classElement.getOwnedBehaviors())) {
                if (behavior instanceof Activity activity) {
                    prependPrefixNameToNodesAndEdgesInActivity(activity, activity.getName());
                }
            }
        }
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
                if (shouldBeNamed(namedElement)) {
                    String name = namedElement.getName();
                    boolean added = names.add(name);
                    Verify.verify(added, String.format("Model name %s is not globally unique.", name));
                }
            }
        }
    }
}
