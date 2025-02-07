
package com.github.tno.pokayoke.transform.app;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AUnaryExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.AttributeOwner;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.uml.profile.cif.ACifObjectToString;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

/** Composite data type flattener. */
public class CompositeDataTypeFlattener {
    private CompositeDataTypeFlattener() {
    }

    /**
     * Flatten all properties with a composite data type as their type, flatten all comparisons and assignments on
     * composite data types, and remove all composite data types.
     *
     * @param model The UML model.
     */
    public static void flattenCompositeDataTypes(Model model) {
        CifContext context = new CifContext(model);
        Class activeClass = context.getAllClasses(c -> !(c instanceof Behavior) && c.isActive()).get(0);
        List<DataType> dataTypes = context.getAllCompositeDataTypes();

        // Step 1:
        // Find and store the leaves of all properties of the main class that are instances of a composite data class.
        // Recursively rewrite the properties with a flattened name, returning a map to the original reference name.
        // Flip the map from absolute names to flattened names.
        Map<String, Set<String>> propertyLeaves = getLeavesForAllCompositeProperties(activeClass, "",
                new LinkedHashMap<>());
        Map<String, String> flatToAbsoluteNames = renameAndFlattenProperties(activeClass, new LinkedHashMap<>());

        Map<String, String> absoluteToFlatNames = flatToAbsoluteNames.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        Verify.verify(flatToAbsoluteNames.size() == absoluteToFlatNames.size());

        // Step 2:
        // Unfold all references to properties with a composite data type in assignments and comparisons.
        unfoldCompositeDataTypeReferences(activeClass, context.getReferenceableElements(), propertyLeaves,
                absoluteToFlatNames);

        // Step 3:
        // Delete the composite data types.
        model.getPackagedElements().removeAll(dataTypes);
    }

    // STEP 1 METHODS START HERE.

    /**
     * Get per absolute name of a property with a composite data type, the relative names of its leaves.
     *
     * @param owner The attribute owner, either a class or a composite data type.
     * @param prefix The absolute name of the attribute owner.
     * @param propertyLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves. Is modified in place.
     * @return {@code propertyLeaves}
     */
    private static Map<String, Set<String>> getLeavesForAllCompositeProperties(AttributeOwner owner, String prefix,
            Map<String, Set<String>> propertyLeaves)
    {
        for (Property property: owner.getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                String absName = prefix.isEmpty() ? property.getName() : prefix + "." + property.getName();
                getLeavesForAllCompositeProperties((DataType)property.getType(), absName, propertyLeaves);

                // Add leaves of intermediate nodes.
                Set<String> leaves = new LinkedHashSet<>();
                PokaYokeTypeUtil.collectRelativeNamesOfLeafProperties(property, "", leaves);
                propertyLeaves.put(absName, leaves);
            }
        }
        return propertyLeaves;
    }

    /**
     * Recursively flatten properties with a composite data type, renaming flattened properties to avoid duplicate
     * names.
     *
     * @param attributeOwner The attribute owner whose properties to flatten.
     * @param attributeOwnerRenames Pair of flattened name, absolute name for every property of a composite data type.
     *     Is modified in place.
     * @return {@code renames}.
     */
    private static Map<String, String> renameAndFlattenProperties(AttributeOwner attributeOwner,
            Map<AttributeOwner, Map<String, String>> attributeOwnerRenames)
    {
        // First, flatten children recursively, if the data type has not been processed already.
        for (Property property: attributeOwner.getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())
                    && attributeOwnerRenames.get((AttributeOwner)property.getType()) == null)
            {
                renameAndFlattenProperties((DataType)property.getType(), attributeOwnerRenames);
            }
        }

        // Names of the primitive type child properties of the property.
        List<Property> ownerProperties = attributeOwner.getOwnedAttributes();
        Set<String> primitiveNames = ownerProperties.stream()
                .filter(p -> !PokaYokeTypeUtil.isCompositeDataType(p.getType())).map(Property::getName)
                .collect(Collectors.toSet());

        // Loop over the owner's properties and rename the properties' children. Add them to the owner attributes.
        Set<Property> propertiesToAdd = new LinkedHashSet<>();
        List<Property> propertiesToRemove = new LinkedList<>();
        for (Property property: ownerProperties) {
            // Process (i.e. add to rename map and remove from owner) *only* properties of composite data type.
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                Map<String, String> dataTypeRenames = attributeOwnerRenames.getOrDefault(property.getType(),
                        new LinkedHashMap<>());

                // Get children with flattened names and the map flattened to absolute names.
                Pair<Set<Property>, Map<String, String>> flattenedPropertiesAndRenames = renameChildProperties(
                        ((DataType)property.getType()).getOwnedAttributes(), property.getName(), dataTypeRenames,
                        primitiveNames);
                Set<Property> flattenedProperties = flattenedPropertiesAndRenames.getLeft();
                Map<String, String> childRenames = flattenedPropertiesAndRenames.getRight();
                propertiesToAdd.addAll(flattenedProperties);
                propertiesToRemove.add(property);

                // Add the children to the attribute owner renaming map.
                attributeOwnerRenames.computeIfAbsent((AttributeOwner)property.getOwner(), t -> new LinkedHashMap<>())
                        .putAll(childRenames);
            }
        }
        ownerProperties.removeAll(propertiesToRemove);
        attributeOwner.getOwnedAttributes().addAll(propertiesToAdd);

        // If attribute owner is a class, return its rename map. Otherwise, return an empty list.
        List<Map<String, String>> classRenames = attributeOwnerRenames.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof Class).map(Entry::getValue)
                .collect(Collectors.toCollection(LinkedList::new));
        return !classRenames.isEmpty() ? classRenames.get(0) : new LinkedHashMap<>();
    }

    /**
     * Create new properties with a unique flattened name, storing also the corresponding absolute names.
     *
     * @param childProperties The properties whose names are to be flattened.
     * @param propertyName The properties' parent name.
     * @param renames Per flattened name, the absolute name.
     * @param existingNames Names that already exist among the properties' parent.
     * @return Pair of flattened name, absolute name for every property.
     *
     */
    private static Pair<Set<Property>, Map<String, String>> renameChildProperties(List<Property> childProperties,
            String propertyName, Map<String, String> renames, Set<String> existingNames)
    {
        Set<Property> renamedProperties = new LinkedHashSet<>();
        Map<String, String> childRenames = new LinkedHashMap<>();
        for (Property child: childProperties) {
            // Create a new property with a clash-free name, and add it to the renamed properties set.
            String flattenedName = generateNewPropertyName(child.getName(), propertyName, existingNames);
            Property renamedProperty = copyAndRenameProperty(child, flattenedName);
            renamedProperties.add(renamedProperty);
            existingNames.add(flattenedName);

            // Find the child name for the absolute name part, and store it in the map.
            String childName = (renames.get(child.getName()) == null) ? child.getName() : renames.get(child.getName());
            String newName = propertyName + "." + childName;
            childRenames.put(flattenedName, newName);
        }
        return Pair.of(renamedProperties, childRenames);
    }

    private static String generateNewPropertyName(String childName, String parentName,
            Collection<String> existingNames)
    {
        String candidateName = parentName + "_" + childName;
        int count = 1;
        while (existingNames.contains(candidateName)) {
            count++;
            candidateName = parentName + "_" + childName + "_" + String.valueOf(count);
        }
        return candidateName;
    }

    private static Property copyAndRenameProperty(Property originalProperty, String newName) {
        Property rewrittenProperty = EcoreUtil.copy(originalProperty);
        rewrittenProperty.setName(newName);
        return rewrittenProperty;
    }

    // STEP 2 METHODS START HERE.

    /**
     * Unfold all references to properties with composite data type in assignments and comparisons, in the given class.
     *
     * @param clazz The class in which to do the unfolding.
     * @param referenceableElements Per absolute name of a referenceable element, the element that is referenced.
     * @param propertyLeaves The map linking each property to its leaf types.
     * @param renames The map linking the absolute names to the flattened names.
     */
    private static void unfoldCompositeDataTypeReferences(Class clazz, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Unfold opaque behaviors and activities.
        for (Behavior classBehavior: clazz.getOwnedBehaviors()) {
            if (classBehavior instanceof OpaqueBehavior element) {
                unfoldGuardAndEffects(element, referenceableElements, propertyLeaves, renames);
            } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                unfoldAbstractActivity(activity, referenceableElements, propertyLeaves, renames);
            } else if (classBehavior instanceof Activity activity && !activity.isAbstract()) {
                unfoldConcreteActivity(activity, referenceableElements, propertyLeaves, renames);
            } else {
                throw new RuntimeException(String.format("Unfolding behaviors of class '%s' not supported.", clazz));
            }
        }

        // Unfold constraints.
        unfoldConstraints(clazz.getOwnedRules(), referenceableElements, propertyLeaves, renames);
    }

    /**
     * Unfolds guards and effects of a redefinable element.
     *
     * @param element The redefinable element.
     * @param referenceableElements The CIF context as a map.
     * @param propertyLeaves The map linking each property to its leaf types.
     * @param renames The map linking the absolute names to the flattened names.
     */
    private static void unfoldGuardAndEffects(RedefinableElement element,
            Map<String, NamedElement> referenceableElements, Map<String, Set<String>> propertyLeaves,
            Map<String, String> renames)
    {
        // Perform the guard unfolding. Skip if null.
        AExpression guardExpr = CifParserHelper.parseGuard(element);
        if (guardExpr != null) {
            AExpression newGuard = unfoldACifExpression(guardExpr, referenceableElements, propertyLeaves, renames);
            String newGuardString = ACifObjectToString.toString(newGuard);
            PokaYokeUmlProfileUtil.setGuard(element, newGuardString);
        }

        // Perform the unfolding for the effects (list of list of updates).
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        List<String> newEffects = new LinkedList<>();
        for (String effect: effects) {
            List<AUpdate> updates = CifParserHelper.parseUpdates(effect, element);
            List<String> newUpdateStrings = new LinkedList<>();
            for (AUpdate update: updates) {
                List<AUpdate> newUpdates = unfoldACifUpdate(update, referenceableElements, propertyLeaves, renames);
                newUpdateStrings.addAll(
                        newUpdates.stream().map(u -> ACifObjectToString.toString(u)).collect(Collectors.toList()));
            }

            // Join the updates with a comma, and store them into the unfolded effects list.
            String newEffect = String.join(", ", newUpdateStrings);
            newEffects.add(newEffect);
        }
        PokaYokeUmlProfileUtil.setEffects(element, newEffects);
    }

    /**
     * Unfolds a CIF {@link AExpression}: substitutes the comparisons between composite data types with the respective
     * leaf properties.
     *
     * @param expression A CIF {@link AExpression} to be unfolded.
     * @param referenceableElements A Map containing the name and the UML NamedElement of every element of the UML
     *     model.
     * @param propertyLeaves The map linking each property to its leaf types.
     * @param renames The map linking the absolute names to the flattened names.
     * @return The unfolded CIF {@link AExpression}.
     */
    private static AExpression unfoldACifExpression(AExpression expression,
            Map<String, NamedElement> referenceableElements, Map<String, Set<String>> propertyLeaves,
            Map<String, String> renames)
    {
        if (expression instanceof ABinaryExpression binExpr) {
            AExpression unfoldedLeft = unfoldACifExpression(binExpr.left, referenceableElements, propertyLeaves,
                    renames);
            AExpression unfoldedRight = unfoldACifExpression(binExpr.right, referenceableElements, propertyLeaves,
                    renames);

            // Unfold comparisons of properties with composite data types.
            if (binExpr.left instanceof ANameExpression leftNameExpr
                    && binExpr.right instanceof ANameExpression rightNameExpr)
            {
                // Unfold if left hand side or right hand side are properties.
                NamedElement lhsElement = referenceableElements.get(leftNameExpr.name.name);
                NamedElement rhsElement = referenceableElements.get(rightNameExpr.name.name);
                if (lhsElement instanceof Property || rhsElement instanceof Property) {
                    return unfoldComparisonExpression(leftNameExpr.name.name, rightNameExpr.name.name, binExpr.operator,
                            binExpr.position, referenceableElements, propertyLeaves, renames);
                } else {
                    return new ABinaryExpression(binExpr.operator, unfoldedLeft, unfoldedRight, expression.position);
                }
            }

            // Combine the unfolded left and right components to form a new binary expression.
            return new ABinaryExpression(binExpr.operator, unfoldedLeft, unfoldedRight, expression.position);
        } else if (expression instanceof AUnaryExpression unaryExpr) {
            return new AUnaryExpression(unaryExpr.operator,
                    unfoldACifExpression(unaryExpr.child, referenceableElements, propertyLeaves, renames),
                    unaryExpr.position);
        } else {
            // Expressions without children don't need unfolding.
            return expression;
        }
    }

    private static ABinaryExpression unfoldComparisonExpression(String lhsName, String rhsName, String operator,
            TextPosition position, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Unfold if left hand side and right hand side are both composite data types.
        NamedElement lhsElement = referenceableElements.get(lhsName);
        NamedElement rhsElement = referenceableElements.get(rhsName);
        if (lhsElement instanceof Property || rhsElement instanceof Property) {
            // Collect the names of all leaves children of left (and right) hand side composite data types. If empty,
            // names refer to primitive type. Get flattened name if present.
            Set<String> lhsLeaves = propertyLeaves.get(lhsName);
            Set<String> rhsLeaves = propertyLeaves.get(rhsName);
            if (lhsLeaves == null && rhsLeaves == null) {
                String newLhsName = renames.getOrDefault(lhsName, lhsName);
                String newRhsName = renames.getOrDefault(rhsName, rhsName);
                return createABinaryExpression(newLhsName, newRhsName, operator, position);
            }
            Set<String> leaves = lhsLeaves == null ? rhsLeaves : lhsLeaves;

            // Create the new binary expression of the unfolded properties for both left and right hand side.
            ABinaryExpression unfoldedBinaryExpression = null;
            for (String leaf: leaves) {
                // Get flattened names, if present.
                String newLhsName = renames.getOrDefault(lhsName + leaf, lhsName + leaf);
                String newRhsName = renames.getOrDefault(rhsName + leaf, rhsName + leaf);
                ABinaryExpression currentBinaryExpression = createABinaryExpression(newLhsName, newRhsName, operator,
                        position);

                // Create a new binary expression as a conjunction of the expressions generated for every leaf.
                if (unfoldedBinaryExpression == null) {
                    unfoldedBinaryExpression = currentBinaryExpression;
                } else {
                    unfoldedBinaryExpression = new ABinaryExpression("and", unfoldedBinaryExpression,
                            currentBinaryExpression, position);
                }
            }
            return unfoldedBinaryExpression;
        } else {
            // No need to unfold non-properties.
            return createABinaryExpression(lhsName, rhsName, operator, position);
        }
    }

    private static ABinaryExpression createABinaryExpression(String lhsName, String rhsName, String operator,
            TextPosition position)
    {
        ANameExpression leftExpression = new ANameExpression(new AName(lhsName, position), false, position);
        ANameExpression rightExpression = new ANameExpression(new AName(rhsName, position), false, position);
        return new ABinaryExpression(operator, leftExpression, rightExpression, position);
    }

    /**
     * Unfolds a CIF {@link AUpdate}: substitutes the comparisons between composite data types with the respective leaf
     * properties.
     *
     * @param update A CIF {@link AUpdate} to be unfolded.
     * @param referenceableElements A Map containing the name and the UML NamedElement of every element of the UML
     *     model.
     * @param propertyLeaves The map linking each property to its leaf types.
     * @param renames The map linking the absolute names to the flattened names.
     * @return The list containing the unfolded CIF {@link AUpdate}.
     */
    private static List<AUpdate> unfoldACifUpdate(AUpdate update, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        if (update instanceof AAssignmentUpdate assign) {
            return unfoldACifAssignmentUpdate(assign, referenceableElements, propertyLeaves, renames);
        } else if (update instanceof AIfUpdate ifUpdate) {
            AUpdate newIfUpdate = unfoldACifIfUpdate(ifUpdate, referenceableElements, propertyLeaves, renames);
            return new LinkedList<>(List.of(newIfUpdate));
        } else {
            throw new RuntimeException(String.format("Unfolding unsupported update: %s.", update));
        }
    }

    /**
     * Unfolds a CIF {@link AAssignmentUpdate}: substitutes the comparisons between composite data types with the
     * respective leaf properties.
     *
     * @param assignUpdate A CIF {@link AAssignmentUpdate} to be unfolded.
     * @param referenceableElements A Map containing the name and the UML NamedElement of every element of the UML
     *     model.
     * @param propertyLeaves The map linking each property to its leaf types.
     * @param renames The map linking the absolute names to the flattened names.
     * @return The unfolded CIF {@link AAssignmentUpdate}.
     */
    private static List<AUpdate> unfoldACifAssignmentUpdate(AAssignmentUpdate assignUpdate,
            Map<String, NamedElement> referenceableElements, Map<String, Set<String>> propertyLeaves,
            Map<String, String> renames)
    {
        if (assignUpdate.addressable instanceof ANameExpression aNameAddressable
                && assignUpdate.value instanceof ANameExpression aNameValue)
        {
            // Unfold if left hand side or right hand side are properties.
            NamedElement lhsElement = referenceableElements.get(aNameAddressable.name.name);
            NamedElement rhsElement = referenceableElements.get(aNameValue.name.name);
            if (lhsElement instanceof Property || rhsElement instanceof Property) {
                return getUnfoldedAssignmentUpdate(aNameAddressable.name.name, aNameValue.name.name,
                        assignUpdate.position, propertyLeaves, renames);
            } else {
                return new LinkedList<>(List.of(assignUpdate));
            }
        }
        // If 'addressable' and 'value' are not both ANameExpression, skip the unfolding and return the expression.
        return new LinkedList<>(List.of(assignUpdate));
    }

    private static List<AUpdate> getUnfoldedAssignmentUpdate(String lhsName, String rhsName, TextPosition position,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Collect the names of all leaves children of left (and right) hand side composite data types. If empty,
        // names refer to primitive type. Get flattened name if present.
        Set<String> lhsLeaves = propertyLeaves.get(lhsName);
        Set<String> rhsLeaves = propertyLeaves.get(rhsName);
        if (lhsLeaves == null && rhsLeaves == null) {
            String newLhsName = renames.getOrDefault(lhsName, lhsName);
            String newRhsName = renames.getOrDefault(rhsName, rhsName);
            return new LinkedList<>(List.of(getNewAssignementUpdate(newLhsName, newRhsName, position)));
        }
        Set<String> leaves = lhsLeaves == null ? rhsLeaves : lhsLeaves;

        // Create a new assignment update of the unfolded properties for both left and right hand side.
        List<AUpdate> unfoldedAssignmentUpdates = new LinkedList<>();
        for (String leaf: leaves) {
            // Get flattened name, if present.
            String newLhsName = renames.getOrDefault(lhsName + leaf, lhsName + leaf);
            String newRhsName = renames.getOrDefault(rhsName + leaf, rhsName + leaf);
            AUpdate currentAssignmentExpression = getNewAssignementUpdate(newLhsName, newRhsName, position);
            unfoldedAssignmentUpdates.add(currentAssignmentExpression);
        }
        return unfoldedAssignmentUpdates;
    }

    private static AAssignmentUpdate getNewAssignementUpdate(String lhs, String rhs, TextPosition position) {
        ANameExpression leftExpression = new ANameExpression(new AName(lhs, position), false, position);
        ANameExpression rightExpression = new ANameExpression(new AName(rhs, position), false, position);
        return new AAssignmentUpdate(leftExpression, rightExpression, position);
    }

    private static void unfoldAbstractActivity(Activity activity, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        // Unfold the precondition and postcondition constraints.
        unfoldConstraints(preconditions, referenceableElements, propertyLeaves, renames);
        unfoldConstraints(postconditions, referenceableElements, propertyLeaves, renames);
    }

    private static void unfoldConstraints(List<Constraint> umlConstraints,
            Map<String, NamedElement> referenceableElements, Map<String, Set<String>> propertyLeaves,
            Map<String, String> renames)
    {
        for (Constraint constraint: umlConstraints) {
            // Skip occurrence constraints.
            if (constraint instanceof IntervalConstraint) {
                continue;
            } else if (constraint.getSpecification() instanceof OpaqueExpression opaqueSpec) {
                unfoldGuardBodies(opaqueSpec, referenceableElements, propertyLeaves, renames);
            } else {
                throw new RuntimeException(
                        "Constraint specification " + constraint.getSpecification() + " is not an opaque expression.");
            }
        }
    }

    private static void unfoldGuardBodies(OpaqueExpression constraintSpec,
            Map<String, NamedElement> referenceableElements, Map<String, Set<String>> propertyLeaves,
            Map<String, String> renames)
    {
        List<AExpression> constraintBodyExpressions = CifParserHelper.parseBodies(constraintSpec);
        List<String> constraintBodyStrings = constraintSpec.getBodies();

        for (int i = 0; i < constraintBodyExpressions.size(); i++) {
            // Get the current body, unfold it, and substitute the corresponding string.
            ACifObject currentBody = constraintBodyExpressions.get(i);
            ACifObject unfoldedBody;
            if (currentBody instanceof AExpression bodyExpression) {
                unfoldedBody = unfoldACifExpression(bodyExpression, referenceableElements, propertyLeaves, renames);
            } else if (currentBody instanceof AInvariant bodyInvariant) {
                unfoldedBody = unfoldACifInvariant(bodyInvariant, referenceableElements, propertyLeaves, renames);
            } else {
                throw new RuntimeException("Guard body " + currentBody + " is not an expression nor an invariant.");
            }
            String newBodyString = ACifObjectToString.toString(unfoldedBody);
            constraintBodyStrings.set(i, newBodyString);
        }
    }

    private static AInvariant unfoldACifInvariant(AInvariant invariant, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Unfold only the invariant predicate.
        return new AInvariant(invariant.name,
                unfoldACifExpression(invariant.predicate, referenceableElements, propertyLeaves, renames),
                invariant.invKind, invariant.events);
    }

    private static void unfoldConcreteActivity(Activity activity, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Unfold the guards and effects of every control flow, call behavior, and opaque action.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                ValueSpecification guard = controlEdge.getGuard();
                if (guard instanceof OpaqueExpression opaqueGuard) {
                    unfoldGuardBodies(opaqueGuard, referenceableElements, propertyLeaves, renames);
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                Behavior guard = callBehavior.getBehavior();
                if (guard instanceof OpaqueBehavior opaqueGuard) {
                    unfoldGuardAndEffects(opaqueGuard, referenceableElements, propertyLeaves, renames);
                } else {
                    throw new RuntimeException(
                            String.format("Call behavior of class %s is not supported.", guard.getClass()));
                }
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                unfoldGuardAndEffects(internalAction, referenceableElements, propertyLeaves, renames);
            } else if (ownedElement instanceof ActivityNode activityNode) {
                // Nodes in activities have empty names and bodies.
                continue;
            } else {
                throw new RuntimeException(String.format("Renaming flattened properties of class '%s' not supported",
                        ownedElement.getClass()));
            }
        }

        // Unfold pre and postconditions.
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        unfoldConstraints(preconditions, referenceableElements, propertyLeaves, renames);
        unfoldConstraints(postconditions, referenceableElements, propertyLeaves, renames);
    }

    /**
     * Unfolds a CIF {@link AIfUpdate}: substitutes the comparisons between composite data types with the respective
     * leaf properties.
     *
     * @param ifUpdate A CIF {@link AIfUpdate} to be unfolded.
     * @param referenceableElements A Map containing the name and the UML NamedElement of every element of the UML
     *     model.
     * @param propertyLeaves The map linking each property to its leaf types.
     * @param renames The map linking the absolute names to the flattened names.
     * @return The unfolded CIF {@link AIfUpdate}.
     */
    private static AUpdate unfoldACifIfUpdate(AIfUpdate ifUpdate, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Process the if statements. The element of the list represent the conditions separated by a comma.
        List<AExpression> ifStatements = ifUpdate.guards;
        List<AExpression> unfoldedIfStatements = new LinkedList<>();
        for (AExpression ifStatement: ifStatements) {
            unfoldedIfStatements.add(unfoldACifExpression(ifStatement, referenceableElements, propertyLeaves, renames));
        }

        // Process the elif statements. Each element of the list represents a complete elif statement.
        List<AElifUpdate> elifStatements = ifUpdate.elifs;
        List<AElifUpdate> unfoldedElifs = new LinkedList<>();
        for (AElifUpdate elifStatement: elifStatements) {
            unfoldedElifs.add(unfoldACifElifUpdate(elifStatement, referenceableElements, propertyLeaves, renames));
        }

        // Process the else statements as CIF AUpdates.
        List<AUpdate> elseStatements = ifUpdate.elses;
        List<AUpdate> unfoldedElses = new LinkedList<>();
        for (AUpdate elseStatement: elseStatements) {
            unfoldedElses.addAll(unfoldACifUpdate(elseStatement, referenceableElements, propertyLeaves, renames));
        }

        // Process the then statements as CIF AUpdates.
        List<AUpdate> thenStatements = ifUpdate.thens;
        List<AUpdate> unfoldedThens = new LinkedList<>();
        for (AUpdate thenStatement: thenStatements) {
            unfoldedThens.addAll(unfoldACifUpdate(thenStatement, referenceableElements, propertyLeaves, renames));
        }
        return new AIfUpdate(unfoldedIfStatements, unfoldedThens, unfoldedElifs, unfoldedElses, ifUpdate.position);
    }

    private static AElifUpdate unfoldACifElifUpdate(AElifUpdate elifUpdate,
            Map<String, NamedElement> referenceableElements, Map<String, Set<String>> propertyLeaves,
            Map<String, String> renames)
    {
        // Process the elif guards as CIF AExpressions.
        List<AExpression> elifGuards = elifUpdate.guards;
        List<AExpression> unfoldedElifGuards = new LinkedList<>();
        for (AExpression elifGuard: elifGuards) {
            unfoldedElifGuards.add(unfoldACifExpression(elifGuard, referenceableElements, propertyLeaves, renames));
        }

        // Process the elif thens as CIF AUpdates.
        List<AUpdate> elifThens = elifUpdate.thens;
        List<AUpdate> unfoldedElifThens = new LinkedList<>();
        for (AUpdate elifThen: elifThens) {
            unfoldedElifThens.addAll(unfoldACifUpdate(elifThen, referenceableElements, propertyLeaves, renames));
        }
        return new AElifUpdate(unfoldedElifGuards, unfoldedElifThens, elifUpdate.position);
    }
}
