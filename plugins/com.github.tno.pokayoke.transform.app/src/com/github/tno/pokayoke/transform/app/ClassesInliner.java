
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
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Property;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;

/** Application that flattens all passive classes and rewrites all named elements with the flattened names. */
public class ClassesInliner {
    private ClassesInliner() {
    }

    public static void inlineClasses(Model model) {
        CifContext context = new CifContext(model);
        Class activeClass = getSingleActiveClass(context);
        List<Class> passiveClasses = getAllPassiveClasses(context);

        Map<String, Pair<String, Property>> flattenedNamesMap = new LinkedHashMap<>();

        // Find all properties of the main class that are instances of a data class, store them into a map with a unique
        // flattened name and the original reference name (e.g. robot.arm.position).
        for (Property umlProperty: activeClass.getOwnedAttributes()) {
            if (!PokaYokeTypeUtil.isSupportedType(umlProperty.getType())) {
                flattenedNamesMap = getLeafPrimitiveType((Class)umlProperty.getType(), umlProperty.getName(),
                        umlProperty.getName(), flattenedNamesMap);
            }
        }
        // Sort entries in flattenedNameMap by length of their keys.
        Map<String, Pair<String, Property>> orderedFlattenedNames = orderMapByKeyLength(flattenedNamesMap);

        // Create a new static property with a flattened name, and add them to the active class.
        writeRenamedProperties(flattenedNamesMap, activeClass);

        // Delete the data-only classes and related properties.
        deletePassiveClasses(activeClass, passiveClasses, model);

        // Updates the main class with the flattened names.
        for (Behavior classBehavior: activeClass.getOwnedBehaviors()) {
            for (Entry<String, Pair<String, Property>> entry: orderedFlattenedNames.entrySet()) {
                if (classBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                    rewriteOpaqueBehavior(umlOpaqueBehavior, entry);
                } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                    rewriteAbstractActivity(activity, entry);
                } else if (classBehavior instanceof Activity activity && !activity.isAbstract()) {
                    // TODO: check all named elements, edges, etc.
                }
            }
        }
    }

    /**
     * Implements a DFS to find the primitive type of every attribute of a class.
     *
     * @param clazz The class containing the properties.
     * @param name The string representing the new, flattened name of a property.
     * @param position The string representing the old name of a property, including the dots.
     * @param flattenedNamesMap The map containing the old and new names, along with the property they refer to.
     * @return A map containing the old and new names, along with the property they refer to.
     */
    public static Map<String, Pair<String, Property>> getLeafPrimitiveType(Class clazz, String name, String position,
            Map<String, Pair<String, Property>> flattenedNamesMap)
    {
        // Loop over a class' attributes. If they are boolean, Enum, Integer, add them to the main class; otherwise,
        // recursively call on the children object.
        for (Property umlProperty: clazz.getOwnedAttributes()) {
            String newNameString = name + "_" + umlProperty.getName();
            String newPosiString = position + "." + umlProperty.getName();

            if (PokaYokeTypeUtil.isSupportedType(umlProperty.getType())) {
                // If we find a property that is supported, add it to the map.
                flattenedNamesMap = addFlattenedNameToMap(umlProperty, newNameString, newPosiString, flattenedNamesMap);
            } else {
                // Add the intermediate product of these. For example, if we have robot1.arm.position, we should
                // add also robot1.arm as a property, together with robot1.arm.position.
                flattenedNamesMap = addFlattenedNameToMap(umlProperty, newNameString, newPosiString, flattenedNamesMap);
                // Recursive call.
                getLeafPrimitiveType((Class)umlProperty.getType(), newNameString, newPosiString, flattenedNamesMap);
            }
        }

        return flattenedNamesMap;
    }

    /**
     * Creates a new map entry containing the old and the new property name, along with the property they refer to.
     * Ensures that the new names are unique.
     *
     * @param umlProperty The property to be inserted.
     * @param name The new name.
     * @param position The old name.
     * @param flattenedNamesMap The map containing the old and new names, along with the property.
     * @return The updated map.
     */
    public static Map<String, Pair<String, Property>> addFlattenedNameToMap(Property umlProperty, String name,
            String position, Map<String, Pair<String, Property>> flattenedNamesMap)
    {
        Pair<String, Property> positionAndObject = Pair.of(position, umlProperty);

        if (flattenedNamesMap.get(name) == null) {
            // Add the item to the map.
            flattenedNamesMap.put(name, positionAndObject);
        } else {
            // Find how many keys start with 'name' and add a number at the end.
            Set<String> allKeys = flattenedNamesMap.keySet();
            int count = 0;
            for (String k: allKeys) {
                if (k.startsWith(name)) {
                    count += 1;
                }
            }
            flattenedNamesMap.put(name + String.valueOf(count), positionAndObject);
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

    public static void writeRenamedProperties(Map<String, Pair<String, Property>> newNamesMap, Class activeClass) {
        for (Entry<String, Pair<String, Property>> entry: newNamesMap.entrySet()) {
            String flattenedName = entry.getKey();
            Property originalProperty = entry.getValue().getRight();

            Property rewrittenProperty = FileHelper.FACTORY.createProperty();
            rewrittenProperty.setIsStatic(true);
            rewrittenProperty.setName(flattenedName);
            rewrittenProperty.setType(originalProperty.getType());
            rewrittenProperty.setDefaultValue(originalProperty.getDefaultValue());
            activeClass.getOwnedAttributes().add(rewrittenProperty);
        }
    }

    public static void deletePassiveClasses(Class activeClass, List<Class> passiveClasses, Model model) {
        // Delete the properties of the main class that are of type Class.
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

    public static void rewriteOpaqueBehavior(OpaqueBehavior umlOpaqueBehavior,
            Entry<String, Pair<String, Property>> rewriteEntry)
    {
        String guard = PokaYokeUmlProfileUtil.getGuard(umlOpaqueBehavior);

        List<String> effects = PokaYokeUmlProfileUtil.getEffects(umlOpaqueBehavior);
        // For all flattened names, rewrite the guard and effects.
        if (guard.contains(rewriteEntry.getValue().getLeft())) {
            guard = guard.replaceAll(rewriteEntry.getValue().getLeft(), rewriteEntry.getKey());
        }
        PokaYokeUmlProfileUtil.setGuard(umlOpaqueBehavior, guard);

        ArrayList<String> renamedEffects = new ArrayList<>();
        for (String effect: effects) {
            if (effect.contains(rewriteEntry.getValue().getLeft())) {
                effect = effect.replaceAll(rewriteEntry.getValue().getLeft(), rewriteEntry.getKey());
                renamedEffects.add(effect);
                PokaYokeUmlProfileUtil.setEffects(umlOpaqueBehavior, renamedEffects);
            }
        }
    }

    public static void rewriteAbstractActivity(Activity activity, Entry<String, Pair<String, Property>> rewriteEntry) {
        EList<Constraint> preconditions = activity.getPreconditions();
        EList<Constraint> postconditions = activity.getPostconditions();

        for (Constraint precondition: preconditions) {
            EList<String> preconditionConstraints = ((OpaqueExpression)precondition.getSpecification()).getBodies();
            for (String constraint: preconditionConstraints) {
                // replace names
                if (constraint.contains(rewriteEntry.getValue().getLeft())) {
                    constraint.replaceAll(rewriteEntry.getValue().getLeft(), rewriteEntry.getKey());
                }
            }
        }
        for (Constraint postcondition: postconditions) {
            EList<String> postconditionConstraints = ((OpaqueExpression)postcondition.getSpecification()).getBodies();
            for (String constraint: postconditionConstraints) {
                // replace names
                if (constraint.contains(rewriteEntry.getValue().getLeft())) {
                    constraint.replaceAll(rewriteEntry.getValue().getLeft(), rewriteEntry.getKey());
                }
            }
        }
    }
}
