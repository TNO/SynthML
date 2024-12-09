
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;

/** Application that flattens all properties that are instantiations of a data class, and deletes all data classes. */
public class ClassesInliner {
    private ClassesInliner() {
    }

    public static void inlineClasses(Model model) {
        CifContext context = new CifContext(model);
        Class activeClass = getSingleActiveClass(context);
        List<Class> passiveClasses = getAllPassiveClasses(context);

        // Find all properties of the main class that are instances of a data class, store them into a map with a unique
        // flattened name, the original reference name, and the property itself.
        Map<String, Pair<String, Property>> orderedFlattenedNames = getOrderedFlattenednames(activeClass);

        // Create new static properties with flattened names, and add them to the active class.
        rewriteProperties(activeClass, orderedFlattenedNames);

        // Delete the data-only classes and related properties.
        deletePassiveClasses(activeClass, passiveClasses, model);

        // Updates the opaque behaviors and abstract activities of the active class with the flattened names.
        rewriteOwnedBehaviors(activeClass, orderedFlattenedNames);

        // Updates the concrete and other abstract activities with the flattened names.
        rewriteNestedClassifiers(activeClass, orderedFlattenedNames);
    }

    public static Map<String, Pair<String, Property>> getOrderedFlattenednames(Class activeClass) {
        Map<String, Pair<String, Property>> flattenedNamesMap = new LinkedHashMap<>();

        // Loop over all properties. If they are of type class, store them into a map composed of a unique
        // flattened name, the original reference name, and the corresponding property.
        for (Property umlProperty: activeClass.getOwnedAttributes()) {
            if (!PokaYokeTypeUtil.isSupportedType(umlProperty.getType())) {
                flattenedNamesMap = getLeafPrimitiveType((Class)umlProperty.getType(), umlProperty.getName(),
                        umlProperty.getName(), flattenedNamesMap);
            }
        }
        // Sort entries in the flattened names map by the length of their keys.
        Map<String, Pair<String, Property>> orderedFlattenedNames = orderMapByKeyLength(flattenedNamesMap);

        return orderedFlattenedNames;
    }

    /**
     * Implements a DFS to find the primitive type of every attribute of a class.
     *
     * @param clazz The class containing the properties.
     * @param flatName The string representing the new, flattened name of a property.
     * @param dotName The string representing the old name of a property, including the dots.
     * @param flattenedNamesMap The map containing the old and new names, along with the property they refer to.
     * @return A map containing the old and new names, along with the property they refer to.
     */
    public static Map<String, Pair<String, Property>> getLeafPrimitiveType(Class clazz, String flatName, String dotName,
            Map<String, Pair<String, Property>> flattenedNamesMap)
    {
        // Loop over a class' attributes. If they are boolean, Enum, Integer, add them to the main class; otherwise,
        // recursively call on the children object.
        for (Property umlProperty: clazz.getOwnedAttributes()) {
            String newFlatName = flatName + "_" + umlProperty.getName();
            String newDotName = dotName + "." + umlProperty.getName();

            if (PokaYokeTypeUtil.isSupportedType(umlProperty.getType())) {
                // If we find a property that is supported, add it to the map.
                flattenedNamesMap = addFlattenedNameToMap(umlProperty, newFlatName, newDotName, flattenedNamesMap);
            } else {
                // Recursive call.
                getLeafPrimitiveType((Class)umlProperty.getType(), newFlatName, newDotName, flattenedNamesMap);
            }
        }

        return flattenedNamesMap;
    }

    /**
     * Creates a new map entry containing the old and the new property name, along with the property they refer to.
     * Ensures that the new names are unique.
     *
     * @param umlProperty The property to be inserted.
     * @param newName The new name.
     * @param oldName The old name.
     * @param flattenedNamesMap The map containing the old and new names, along with the property.
     * @return The updated map.
     */
    public static Map<String, Pair<String, Property>> addFlattenedNameToMap(Property umlProperty, String newName,
            String oldName, Map<String, Pair<String, Property>> flattenedNamesMap)
    {
        Pair<String, Property> positionAndObject = Pair.of(oldName, umlProperty);

        if (flattenedNamesMap.get(newName) == null) {
            // Add the item to the map if name is unused.
            flattenedNamesMap.put(newName, positionAndObject);
        } else {
            // Find how many keys start with 'name' and add a number at the end.
            Set<String> allKeys = flattenedNamesMap.keySet();
            int count = 0;
            for (String k: allKeys) {
                if (k.startsWith(newName)) {
                    count += 1;
                }
            }
            flattenedNamesMap.put(newName + String.valueOf(count), positionAndObject);
        }

        return flattenedNamesMap;
    }

    public static List<Class> getAllPassiveClasses(CifContext context) {
        List<Class> umlClasses = context.getAllClasses(c -> (!(c instanceof Behavior) && (!c.isActive())));
        return umlClasses;
    }

    public static Class getSingleActiveClass(CifContext context) {
        List<Class> umlClasses = context.getAllClasses(c -> (!(c instanceof Behavior) && (c.isActive())));
        Preconditions.checkArgument(umlClasses.size() == 1,
                "Expected exactly one active class, but got " + umlClasses.size());
        return umlClasses.get(0);
    }

    public static Map<String, Pair<String, Property>>
            orderMapByKeyLength(Map<String, Pair<String, Property>> unorderedMap)
    {
        Map<String, Pair<String, Property>> orderedMap = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                if (s1.length() > s2.length()) {
                    return -1;
                } else if (s1.length() < s2.length()) {
                    return 1;
                } else {
                    return s1.compareTo(s2);
                }
            }
        });

        orderedMap.putAll(unorderedMap);
        return orderedMap;
    }

    public static void rewriteProperties(Class activeClass, Map<String, Pair<String, Property>> newNamesMap) {
        for (Entry<String, Pair<String, Property>> entry: newNamesMap.entrySet()) {
            String flattenedName = entry.getKey();
            Property originalProperty = entry.getValue().getRight();
            // Create a new property and populate the relevant fields.
            Property rewrittenProperty = FileHelper.FACTORY.createProperty();
            rewrittenProperty.setIsStatic(true);
            rewrittenProperty.setName(flattenedName);
            rewrittenProperty.setType(originalProperty.getType());
            rewrittenProperty.setDefaultValue(originalProperty.getDefaultValue());
            activeClass.getOwnedAttributes().add(rewrittenProperty);
        }
    }

    /**
     * Deletes properties of the model in three scenarios: 1) properties of the active class that are of type class; 2)
     * properties of the active class that are classes; 3) passive classes located outside the active class.
     *
     * @param activeClass The main active class.
     * @param passiveClasses List of passive (data) classes.
     * @param model The UML model.
     */
    public static void deletePassiveClasses(Class activeClass, List<Class> passiveClasses, Model model) {
        // Delete the properties of the active class that are of type Class.
        ArrayList<Property> propertiesToBeRemoved = new ArrayList<>();
        for (Property umlProperty: activeClass.getOwnedAttributes()) {
            if (passiveClasses.contains(umlProperty.getType())) {
                propertiesToBeRemoved.add(umlProperty);
            }
        }
        activeClass.getOwnedAttributes().removeAll(propertiesToBeRemoved);

        // Delete passive classes inside the active class.
        for (Class passiveClass: passiveClasses) {
            if (activeClass.getNestedClassifiers().contains(passiveClass)) {
                activeClass.getNestedClassifiers().remove(passiveClass);
            }
        }

        // Delete the passive classes at the same level of the active class.
        for (Class passiveClass: passiveClasses) {
            if (model.getPackagedElements().contains(passiveClass)) {
                model.getPackagedElements().remove(passiveClass);
            }
        }
    }

    public static void rewriteGuardAndEffects(RedefinableElement element,
            Entry<String, Pair<String, Property>> rewriteEntry)
    {
        String guard = PokaYokeUmlProfileUtil.getGuard(element);
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);

        // Update the guard if it is not null and contains the old name.
        if (guard != null && guard.contains(rewriteEntry.getValue().getLeft())) {
            guard = guard.replaceAll(rewriteEntry.getValue().getLeft(), rewriteEntry.getKey());
        }
        PokaYokeUmlProfileUtil.setGuard(element, guard);

        // Update the effects if they contain the old name.
        ArrayList<String> renamedEffects = new ArrayList<>();
        for (String effect: effects) {
            if (effect.contains(rewriteEntry.getValue().getLeft())) {
                effect = effect.replaceAll(rewriteEntry.getValue().getLeft(), rewriteEntry.getKey());
                renamedEffects.add(effect);
                PokaYokeUmlProfileUtil.setEffects(element, renamedEffects);
            }
        }
    }

    /**
     * Rewrites the abstract activity and the opaque behaviors of the active class with the update names map. Throw a
     * RuntimeException is any other element is detected.
     *
     * @param clazz The class considered.
     * @param namesMap The map containing the old and new names.
     */
    public static void rewriteOwnedBehaviors(Class clazz, Map<String, Pair<String, Property>> namesMap) {
        for (Behavior classBehavior: clazz.getOwnedBehaviors()) {
            for (Entry<String, Pair<String, Property>> entry: namesMap.entrySet()) {
                if (classBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                    rewriteGuardAndEffects(umlOpaqueBehavior, entry);
                } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                    rewriteAbstractActivity(activity, entry);
                } else {
                    throw new RuntimeException(String.format(
                            "Renaming flattened properties of class '%s' not supported.", classBehavior.getClass()));
                }
            }
        }
    }

    public static void rewriteAbstractActivity(Activity activity, Entry<String, Pair<String, Property>> rewriteEntry) {
        EList<Constraint> preconditions = activity.getPreconditions();
        EList<Constraint> postconditions = activity.getPostconditions();

        rewritePrePostConditions(preconditions, rewriteEntry);
        rewritePrePostConditions(postconditions, rewriteEntry);
    }

    public static void rewritePrePostConditions(EList<Constraint> umlConstraints,
            Entry<String, Pair<String, Property>> rewriteEntry)
    {
        for (Constraint constraint: umlConstraints) {
            EList<String> constraintBodies = ((OpaqueExpression)constraint.getSpecification()).getBodies();
            for (int i = 0; i < constraintBodies.size(); i++) {
                // Get the current body and substitute the expression if needed.
                String currentConstraint = constraintBodies.get(i);
                if (currentConstraint.contains(rewriteEntry.getValue().getLeft())) {
                    String newConstraint = currentConstraint.replaceAll(rewriteEntry.getValue().getLeft(),
                            rewriteEntry.getKey());
                    constraintBodies.set(i, newConstraint);
                }
            }
        }
    }

    public static void rewriteNestedClassifiers(Class clazz, Map<String, Pair<String, Property>> namesMap) {
        for (Classifier classifier: clazz.getNestedClassifiers()) {
            if (classifier instanceof Activity activity && !activity.isAbstract()) {
                for (Entry<String, Pair<String, Property>> entry: namesMap.entrySet()) {
                    rewriteConcreteActivity(activity, entry);
                }
            } else if (classifier instanceof Activity activity && activity.isAbstract()) {
                for (Entry<String, Pair<String, Property>> entry: namesMap.entrySet()) {
                    rewriteAbstractActivity(activity, entry);
                }
            } else {
                throw new RuntimeException(String.format("Renaming flattened properties of class '%s' not supported",
                        classifier.getClass()));
            }
        }
    }

    public static void rewriteConcreteActivity(Activity activity, Entry<String, Pair<String, Property>> rewriteEntry) {
        // Update the flattened names of every control flow, call behavior, and opaque action.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                ValueSpecification guard = controlEdge.getGuard();
                // If null, avoids entering in this loop.
                if (guard instanceof OpaqueExpression opaqueGuard) {
                    EList<String> guardBodies = opaqueGuard.getBodies();
                    rewriteOpaqueGuards(guardBodies, rewriteEntry);
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                // Get guards and respective bodies, updates with the flattened names.
                Behavior guard = callBehavior.getBehavior();
                EList<String> guardBodies = ((OpaqueBehavior)guard).getBodies();
                rewriteOpaqueGuards(guardBodies, rewriteEntry);
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                rewriteGuardAndEffects(internalAction, rewriteEntry);
            } else if (ownedElement instanceof ActivityNode) {
                continue;
            } else {
                throw new RuntimeException(String.format("Renaming flattened properties of class '%s' not supported",
                        ownedElement.getClass()));
            }
        }

        // Rewrite pre- and post-conditions.
        EList<Constraint> preconditions = activity.getPreconditions();
        EList<Constraint> postconditions = activity.getPostconditions();

        rewritePrePostConditions(preconditions, rewriteEntry);
        rewritePrePostConditions(postconditions, rewriteEntry);
    }

    public static void rewriteOpaqueGuards(EList<String> guardBodies,
            Entry<String, Pair<String, Property>> rewriteEntry)
    {
        for (int i = 0; i < guardBodies.size(); i++) {
            // Get the current body and substitute its expression if needed.
            String currentBody = guardBodies.get(i);
            if (currentBody.contains(rewriteEntry.getValue().getLeft())) {
                String newBodyString = currentBody.replaceAll(rewriteEntry.getValue().getLeft(), rewriteEntry.getKey());
                guardBodies.set(i, newBodyString);
            }
        }
    }
}
