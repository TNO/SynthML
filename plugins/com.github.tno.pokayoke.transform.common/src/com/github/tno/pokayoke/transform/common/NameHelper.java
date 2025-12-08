
package com.github.tno.pokayoke.transform.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.parser.CifScanner;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ValueSpecification;

/**
 * Helper class for renaming model elements.
 *
 * <p>
 * This class does not support UML models with composite data types.
 * </p>
 */
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

    /**
     * Get all CIF reserved keywords.
     *
     * @return List containing all CIF keywords.
     */
    private static Set<String> getAllCifKeywords() {
        Set<String> keywords = new LinkedHashSet<>();
        Collections.addAll(keywords, CifScanner.getKeywords("Keywords"));
        Collections.addAll(keywords, CifScanner.getKeywords("SupKind"));
        Collections.addAll(keywords, CifScanner.getKeywords("StdLibFunction"));
        Collections.addAll(keywords, CifScanner.getKeywords("Operator"));
        return Collections.unmodifiableSet(keywords);
    }

    private static final Set<String> CIF_RESERVED_KEYWORDS = getAllCifKeywords();

    /**
     * Get all GAL reserved keywords. Could not find a scanner nor parser for GAL reserved keywords; however, the
     * keywords can be found at <a href=
     * "https://github.com/lip6/ITSTools/blob/ed8570b7c72125043c86f1bfc0e46e580e14ec8c/fr.lip6.move.gal.web/WebRoot/xtext-resources/generated/gal-syntax.js#L2">GAL
     * Keywords</a>.
     */
    private static final Set<String> GAL_RESERVED_KEYWORDS = Collections
            .unmodifiableSet(new LinkedHashSet<>(Arrays.asList("A", "AF", "AG", "AX", "E", "EF", "EG", "EX", "F", "G",
                    "GAL", "M", "R", "TRANSIENT", "U", "W", "X", "abort", "alias", "array", "atom", "bounds",
                    "composite", "ctl", "else", "extends", "false", "fixpoint", "for", "gal", "hotbit", "if", "import",
                    "int", "interface", "invariant", "label", "ltl", "main", "never", "predicate", "property",
                    "reachable", "self", "synchronization", "transition", "true", "typedef")));

    /**
     * Checks whether a name belongs to the set of reserved keywords of CIF, GAL, or Petrify.
     *
     * @param name The string to be checked.
     * @return {@code true} if the string is reserved, {@code false} otherwise.
     */
    public static boolean isReservedKeyword(String name) {
        // Note that Petrify uses the . (dot) before its keywords.
        return CIF_RESERVED_KEYWORDS.contains(name) || GAL_RESERVED_KEYWORDS.contains(name) || name.startsWith(".");
    }

    public static boolean isNullOrTriviallyTrue(String s) {
        return s == null || s.equals("true");
    }

    /**
     * Gives the conjunction of all given strings as a single expression.
     *
     * @param exprs The strings to conjoin.
     * @return The conjunction of all given strings as a single expression.
     */
    public static String conjoinExprs(List<String> exprs) {
        return exprs.stream().filter(e -> e != null)
                .reduce((left, right) -> String.format("(%s) and (%s)", left, right)).orElse("true");
    }
}
