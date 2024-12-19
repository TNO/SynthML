
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.common.util.EList;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;

/**
 * Flattens all properties that are instantiations of a data type, and deletes all data types. A data type property
 * represents a canonical "object" class, e.g. a robot, may contain only properties and nothing else.
 */
public class DataTypeInliner {
    private DataTypeInliner() {
    }

    public static void inlineDataTypes(Model model, List<String> warnings) {
        CifContext context = new CifContext(model);
        Class activeClass = getSingleActiveClass(context);
        List<DataType> dataTypes = getAllDataTypes(context);

        // Find all properties of the main class that are instances of a data class, store them into a map with a unique
        // flattened name, the original reference name, and the property itself.
        Map<String, Pair<String, Property>> orderedFlattenedNames = getOrderedFlattenedNames(activeClass, warnings);

        // Create new static properties with flattened names, and add them to the active class.
        rewriteProperties(activeClass, orderedFlattenedNames);

        // Delete the data-only classes and related properties.
        deleteDataTypes(activeClass, dataTypes);

        // Updates the opaque behaviors, abstract and concrete activities of the active class with the flattened names.
        rewriteOwnedBehaviors(activeClass, orderedFlattenedNames);
    }

    /**
     * Gets the only active class of the UML model. The supported UML model contains a unique active class, which
     * contain all the behaviors (activities and opaque behaviors) of the UML model. The uniqueness check is performed
     * in PokaYokeProfileValidator.
     *
     * @param context The CIF context.
     * @return A single active class.
     */
    private static Class getSingleActiveClass(CifContext context) {
        return context.getAllClasses(c -> !(c instanceof Behavior) && c.isActive()).get(0);
    }

    /**
     * Gets all data type objects. A data type object may contain only properties either as a data type or as
     * Enumeration, integer or boolean, and nothing else.
     *
     * @param context The CIF context.
     * @return A list containing all data types contained in context.
     */
    private static List<DataType> getAllDataTypes(CifContext context) {
        // Return only the objects belonging to DataType, not to the subclasses.
        return context.getAllDataTypes(d -> !(d instanceof Enumeration || d instanceof PrimitiveType));
    }

    /**
     * Recursively computes a map of the nested properties, where the keys are the new flattened names, and the values
     * are a tuple of the old dotted name and the property itself. The map is order by key length, so that the name
     * substitution avoids incomplete replacing.
     *
     * @param activeClass The main active class of the UML model.
     * @param warnings A list of strings containing all the synthesis chain warnings.
     * @return A map containing the old and new names, along with the property they refer to, with items sorted by key
     *     length in descending order.
     */
    private static Map<String, Pair<String, Property>> getOrderedFlattenedNames(Class activeClass,
            List<String> warnings)
    {
        // Creates a map where the keys are the new flattened name, and the values are a pair of the old reference name
        // (with dots) and the property itself.
        Map<String, Pair<String, Property>> flattenedNamesMap = new LinkedHashMap<>();

        // Loop over all properties. If they are of type DataType, store them into a map composed of a unique
        // flattened name, the original reference name, and the corresponding property.
        for (Property umlProperty: activeClass.getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isDataTypeOnlyType(umlProperty.getType())) {
                getChildPropertyName((DataType)umlProperty.getType(), umlProperty.getName(), umlProperty.getName(),
                        flattenedNamesMap, warnings);
            }
        }

        // Sort entries in the flattened names map by the length of their keys.
        return orderMapByKeyLength(flattenedNamesMap);
    }

    /**
     * Performs a DFS to find the old and new names of every (in)direct data type attribute.
     *
     * @param datatype The data type containing the properties.
     * @param flatName The string representing the new, flattened name of a property.
     * @param dotName The string representing the old name of a property, including the dots.
     * @param flattenedNamesMap A mapping from new property names, to their old property names and the property itself.
     * @param warnings A list of strings containing all the synthesis chain warnings.
     */
    private static void getChildPropertyName(DataType datatype, String flatName, String dotName,
            Map<String, Pair<String, Property>> flattenedNamesMap, List<String> warnings)
    {
        // Loop over all data type's attributes. If they are boolean, Enum, Integer, add them to the map;
        // otherwise, recursively call on the children object.
        for (Property umlProperty: datatype.getOwnedAttributes()) {
            String newFlatName = flatName + "_" + umlProperty.getName();
            String newDotName = dotName + "." + umlProperty.getName();

            if (PokaYokeTypeUtil.isDataTypeOnlyType(umlProperty.getType())) {
                // Recursive call.
                getChildPropertyName((DataType)umlProperty.getType(), newFlatName, newDotName, flattenedNamesMap,
                        warnings);
            } else {
                // If we find a property that is supported, add it to the map.
                addFlattenedNameToMap(umlProperty, newFlatName, newDotName, flattenedNamesMap, warnings);
                System.out.println("Update names: " + newDotName + " into " + newFlatName);
            }
        }
    }

    /**
     * Creates a mapping from new property names, to their old property names and the property itself. Ensures that the
     * new names are unique.
     *
     * @param umlProperty The property to be inserted.
     * @param newName The new name.
     * @param oldName The old name.
     * @param flattenedNamesMap A mapping from new property names, to their old property names and the property itself.
     * @param warnings A list of strings containing all the synthesis chain warnings.
     */
    private static void addFlattenedNameToMap(Property umlProperty, String newName, String oldName,
            Map<String, Pair<String, Property>> flattenedNamesMap, List<String> warnings)
    {
        Pair<String, Property> oldNameAndProperty = Pair.of(oldName, umlProperty);

        if (flattenedNamesMap.containsKey(newName)) {
            // Find how many keys start with 'name' and add a number at the end, and adds a warning.
            long count = flattenedNamesMap.keySet().stream().filter(k -> k.startsWith(newName)).count();
            flattenedNamesMap.put(newName + String.valueOf(count), oldNameAndProperty);
            String messageString = "Found a property with a duplicate name: " + newName;
            warnings.add(messageString);
        } else {
            // Add the item to the map if name is unused.
            flattenedNamesMap.put(newName, oldNameAndProperty);
        }
    }

    private static Map<String, Pair<String, Property>>
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

    private static void rewriteProperties(Class activeClass, Map<String, Pair<String, Property>> newNamesMap) {
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
     * Deletes properties of the model that are data types.
     *
     * @param activeClass The main active class.
     * @param dataTypes List of data types.
     */
    private static void deleteDataTypes(Class activeClass, List<DataType> dataTypes) {
        // Delete the properties of the active class that are of type DataType.
        for (Property umlProperty: new ArrayList<>(activeClass.getOwnedAttributes())) {
            if (dataTypes.contains(umlProperty.getType())) {
                activeClass.getOwnedAttributes().remove(umlProperty);
            }
        }

        // Delete the data types located at the same level of the active class.
        Model model = activeClass.getModel();
        for (DataType datatype: dataTypes) {
            if (model.getPackagedElements().contains(datatype)) {
                model.getPackagedElements().remove(datatype);
            }
        }
    }

    /**
     * Rewrites the abstract and concrete activities, and the opaque behaviors of the active class with the updated
     * names map.
     *
     * @param clazz The class considered.
     * @param namesMap The map containing the old and new property names.
     * @throws RuntimeException If an element other than an activity or an opaque behavior is detected.
     */
    private static void rewriteOwnedBehaviors(Class clazz, Map<String, Pair<String, Property>> namesMap) {
        for (Behavior classBehavior: clazz.getOwnedBehaviors()) {
            for (Entry<String, Pair<String, Property>> entry: namesMap.entrySet()) {
                String newName = entry.getKey();
                String oldName = entry.getValue().getLeft();

                if (classBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                    rewriteGuardAndEffects(umlOpaqueBehavior, newName, oldName);
                } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                    rewriteAbstractActivity(activity, newName, oldName);
                } else if (classBehavior instanceof Activity activity && !activity.isAbstract()) {
                    rewriteConcreteActivity(activity, newName, oldName);
                } else {
                    throw new RuntimeException(String.format(
                            "Renaming flattened properties of class '%s' not supported.", classBehavior.getClass()));
                }
            }
        }
    }

    private static void rewriteGuardAndEffects(RedefinableElement element, String newName, String oldName) {
        String guard = PokaYokeUmlProfileUtil.getGuard(element);
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);

        // Update the guard if it is not null and contains the old name.
        if (guard != null && guard.contains(oldName)) {
            guard = guard.replaceAll(oldName, newName);
            PokaYokeUmlProfileUtil.setGuard(element, guard);
        }

        // Update the effects if they contain the old name.
        ArrayList<String> renamedEffects = new ArrayList<>();
        for (String effect: effects) {
            if (effect.contains(oldName)) {
                effect = effect.replaceAll(oldName, newName);
                renamedEffects.add(effect);
            } else {
                renamedEffects.add(effect);
            }
        }
        PokaYokeUmlProfileUtil.setEffects(element, renamedEffects);
    }

    private static void rewriteAbstractActivity(Activity activity, String newName, String oldName) {
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        rewritePrePostConditions(preconditions, newName, oldName);
        rewritePrePostConditions(postconditions, newName, oldName);
    }

    private static void rewritePrePostConditions(List<Constraint> umlConstraints, String newName, String oldName) {
        for (Constraint constraint: umlConstraints) {
            if (constraint.getSpecification() instanceof OpaqueExpression) {
                EList<String> constraintBodies = ((OpaqueExpression)constraint.getSpecification()).getBodies();
                rewriteGuardBodies(constraintBodies, newName, oldName);
            } else {
                throw new RuntimeException(
                        "Constraint specification " + constraint.getSpecification() + " is not an opaque expression.");
            }
        }
    }

    private static void rewriteConcreteActivity(Activity activity, String newName, String oldName) {
        // Update the flattened names of every control flow, call behavior, and opaque action.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                ValueSpecification guard = controlEdge.getGuard();
                if (guard instanceof OpaqueExpression opaqueGuard) {
                    EList<String> guardBodies = opaqueGuard.getBodies();
                    rewriteGuardBodies(guardBodies, newName, oldName);
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                // Get guards and respective bodies, and updates them with the flattened names.
                Behavior guard = callBehavior.getBehavior();
                EList<String> guardBodies = ((OpaqueBehavior)guard).getBodies();
                rewriteGuardBodies(guardBodies, newName, oldName);
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                rewriteGuardAndEffects(internalAction, newName, oldName);
            } else if (ownedElement instanceof ActivityNode) {
                // This assumes that nodes in activities have empty names and bodies.
                continue;
            } else {
                throw new RuntimeException(String.format("Renaming flattened properties of class '%s' not supported",
                        ownedElement.getClass()));
            }
        }

        // Rewrite pre and postconditions.
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        rewritePrePostConditions(preconditions, newName, oldName);
        rewritePrePostConditions(postconditions, newName, oldName);
    }

    private static void rewriteGuardBodies(EList<String> guardBodies, String newName, String oldName) {
        for (int i = 0; i < guardBodies.size(); i++) {
            // Get the current body and substitute its expression if needed.
            String currentBody = guardBodies.get(i);
            if (currentBody.contains(oldName)) {
                String newBodyString = currentBody.replaceAll(oldName, newName);
                guardBodies.set(i, newBodyString);
            }
        }
    }
}
