
package com.github.tno.pokayoke.transform.app;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        // Only perform flattening if there are any composite data types. This not only prevents unnecessary work, but also prevents normalizing expressions/updates.
        CifContext context = new CifContext(model);
        Class activeClass = context.getAllClasses(c -> !(c instanceof Behavior) && c.isActive()).get(0);
        List<DataType> dataTypes = context.getAllCompositeDataTypes();
        if (!dataTypes.isEmpty()) {
            // Find and store the leaves of all composite data type properties. Recursively rewrite the properties with
            // a flattened name, which is mapped to its absolute name.
            Map<String, Set<String>> propertyToLeaves = getLeavesForAllCompositeProperties(activeClass, "",
                    new LinkedHashMap<>());
            Map<String, String> flatToAbsoluteNames = renameAndFlattenProperties(activeClass, new LinkedHashMap<>());
            Map<String, String> absoluteToFlatNames = flatToAbsoluteNames.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
            Verify.verify(flatToAbsoluteNames.size() == absoluteToFlatNames.size());

            // Unfold all references to properties with a composite data type in assignments and comparisons.
            unfoldCompositeDataTypeReferences(activeClass, context.getReferenceableElements(), propertyLeaves,
                    absoluteToFlatNames);

            // Delete the composite data types.
            model.getPackagedElements().removeAll(dataTypes);
        }
    }

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
            Map<String, Set<String>> propertyToLeaves)
    {
        for (Property property: owner.getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                String absName = prefix.isEmpty() ? property.getName() : prefix + "." + property.getName();
                getLeavesForAllCompositeProperties((DataType)property.getType(), absName, propertyLeaves);

                // Add this attribute owner to the result.
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
     * @param attributeOwner The attribute owner, either a class or composite data type, whose properties to flatten.
     * @param attributeOwnerRenames Per attribute owner, per flattened property name, its original relative name. Is
     *     modified in place.
     * @return Per flattened property name of the attribute owner, its original relative name.
     */
    private static Map<String, String> renameAndFlattenProperties(AttributeOwner attributeOwner,
            Map<AttributeOwner, Map<String, String>> attributeOwnerRenames)
    {
        // First, flatten children recursively, if the composite data type has not been processed already.
        for (Property property: attributeOwner.getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())
                    && !attributeOwnerRenames.containsKey((AttributeOwner)property.getType()))
            {
                renameAndFlattenProperties((DataType)property.getType(), attributeOwnerRenames);
            }
        }
        // Collect the names of the properties with non-composite data types, to avoid these names when flattening.
        List<Property> ownerProperties = attributeOwner.getOwnedAttributes();
        Set<String> primitivePropertiesNames = ownerProperties.stream()
                .filter(p -> !PokaYokeTypeUtil.isCompositeDataType(p.getType())).map(Property::getName)
                .collect(Collectors.toSet());

        // Flatten the attribute owner's properties.
        Set<Property> propertiesToAdd = new LinkedHashSet<>();
        Set<Property> propertiesToRemove = new LinkedHashSet<>();
        Map<String, String> childrenRenames = new LinkedHashMap<>();
        for (Property property: ownerProperties) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                Map<String, String> dataTypeRenames = attributeOwnerRenames.getOrDefault(property.getType(),
                        new LinkedHashMap<>());

                // Copy and rename the children.
                for (Property child: ((DataType)property.getType()).getOwnedAttributes()) {
                    Pair<Property, Map<String, String>> flattenedPropertyAndRename = renameChildProperty(child,
                            property.getName(), dataTypeRenames, primitivePropertiesNames);
                    Property flattenedProperty = flattenedPropertyAndRename.getLeft();
                    primitivePropertiesNames.add(flattenedProperty.getName());
                    propertiesToAdd.add(flattenedProperty);
                    childrenRenames.putAll(flattenedPropertyAndRename.getRight());
                }
                propertiesToRemove.add(property);
            }
        }
        attributeOwnerRenames.put(attributeOwner, childrenRenames);
        ownerProperties.removeAll(propertiesToRemove);
        onwerProperties.addAll(propertiesToAdd);

        // Return the renames for this attribute owner.
        return childrenRenames;
    }

    /**
     * Copy the given property and give it a unique flattened name within its new parent (attribute owner).
     *
     * @param child The child property of a composite data type to be flattened.
     * @param propertyName The name of the property with a composite data type that contains the child property.
     * @param ranames Per flattened child property name of the composite data type, its original relative name.
     * @param existingNames Names to avoid when renaming, since they already exist in the new parent (attribute owner).
     * @return The flattened property and its original relative name.
     *
     */
    private static Pair<Property, Map<String, String>> renameChildProperty(Property child, String propertyName,
            Map<String, String> renames, Set<String> existingNames)
    {
        // Copy the property and give it a clash-free name.
        String flattenedName = generateNewPropertyName(child.getName(), propertyName, existingNames);
        Property flattenedProperty = copyAndRenameProperty(child, flattenedName);

        // Get the flattened property's original relative name and return it along with the flattened property.
        String childName = renames.getOrDefault(child.getName(), child.getName());
        String origRelName = propertyName + "." + childName;
        return Pair.of(renamedProperty, Map.of(flattenedName, newName));
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

        // Perform the unfolding of the effects.
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
            AExpression unfoldedLhsExpression = unfoldACifExpression(binExpr.left, referenceableElements,
                    propertyLeaves, renames);
            AExpression unfoldedRhsExpression = unfoldACifExpression(binExpr.right, referenceableElements,
                    propertyLeaves, renames);

            // Unfold comparisons of properties with composite data types.
            if (binExpr.left instanceof ANameExpression lhsNameExpr
                    && binExpr.right instanceof ANameExpression rhsNameExpr)
            {
                // Unfold if left hand side or right hand side are properties.
                NamedElement lhsElement = referenceableElements.get(lhsNameExpr.name.name);
                NamedElement rhsElement = referenceableElements.get(rhsNameExpr.name.name);
                if (lhsElement instanceof Property || rhsElement instanceof Property) {
                    return unfoldComparisonExpression(lhsNameExpr.name.name, rhsNameExpr.name.name, binExpr.operator,
                            binExpr.position, propertyLeaves, renames);
                } else {
                    return new ABinaryExpression(binExpr.operator, unfoldedLhsExpression, unfoldedRhsExpression,
                            expression.position);
                }
            }

            // Combine the unfolded left and right components to form a new binary expression.
            return new ABinaryExpression(binExpr.operator, unfoldedLhsExpression, unfoldedRhsExpression,
                    expression.position);
        } else if (expression instanceof AUnaryExpression unaryExpr) {
            return new AUnaryExpression(unaryExpr.operator,
                    unfoldACifExpression(unaryExpr.child, referenceableElements, propertyLeaves, renames),
                    unaryExpr.position);
        } else if (expression instanceof ANameExpression nameExpr) {
            return unfoldANameExpression(nameExpr.name.name, expression.position, renames);
        } else {
            // Expressions without children don't need unfolding.
            return expression;
        }
    }

    private static ABinaryExpression unfoldComparisonExpression(String lhsName, String rhsName, String operator,
            TextPosition position, Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Collect the names of all leaves children of left (and right) hand side composite data types. If empty,
        // names refer to primitive type. Get flattened name if present.
        Set<String> lhsLeaves = propertyLeaves.get(lhsName);
        Set<String> rhsLeaves = propertyLeaves.get(rhsName);
        if (lhsLeaves == null && rhsLeaves == null) {
            String newLhsName = renames.getOrDefault(lhsName, lhsName);
            String newRhsName = renames.getOrDefault(rhsName, rhsName);
            return createABinaryExpression(newLhsName, newRhsName, operator, position);
        }

        // Create the new binary expression of the unfolded properties for both left and right hand side.
        Set<String> leaves = lhsLeaves == null ? rhsLeaves : lhsLeaves;
        ABinaryExpression unfoldedBinaryExpression = null;
        for (String leaf: leaves) {
            String newLhsName = renames.get(lhsName + leaf);
            String newRhsName = renames.get(rhsName + leaf);
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
    }

    private static ANameExpression unfoldANameExpression(String name, TextPosition position,
            Map<String, String> renames)
    {
        String newName = renames.getOrDefault(name, name);
        return new ANameExpression(new AName(newName, position), false, position);
    }

    private static ABinaryExpression createABinaryExpression(String lhsName, String rhsName, String operator,
            TextPosition position)
    {
        ANameExpression lhsExpression = new ANameExpression(new AName(lhsName, position), false, position);
        ANameExpression rhsExpression = new ANameExpression(new AName(rhsName, position), false, position);
        return new ABinaryExpression(operator, lhsExpression, rhsExpression, position);
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
     * Unfolds a CIF {@link AAssignmentUpdate}: substitutes the assignment between composite data types with the
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
        // Unfold only if 'addressable' is ANameExpression. If also 'value' is ANameExpression, unfold both left and
        // right hand sides.
        if (assignUpdate.addressable instanceof ANameExpression aNameAddressable
                && assignUpdate.value instanceof ANameExpression aNameValue)
        {
            // Unfold if left hand side or right hand side are properties.
            NamedElement lhsElement = referenceableElements.get(aNameAddressable.name.name);
            NamedElement rhsElement = referenceableElements.get(aNameValue.name.name);
            if (lhsElement instanceof Property || rhsElement instanceof Property) {
                return unfoldLeavesOfAssignmentUpdate(aNameAddressable.name.name, aNameValue.name.name,
                        assignUpdate.position, propertyLeaves, renames);
            } else {
                return new LinkedList<>(List.of(assignUpdate));
            }
        } else if (assignUpdate.addressable instanceof ANameExpression aNameAddressable) {
            NamedElement lhsElement = referenceableElements.get(aNameAddressable.name.name);
            if (lhsElement instanceof Property) {
                return new LinkedList<>(List.of(new AAssignmentUpdate(
                        unfoldACifExpression(aNameAddressable, referenceableElements, propertyLeaves, renames),
                        unfoldACifExpression(assignUpdate.value, referenceableElements, propertyLeaves, renames),
                        assignUpdate.position)));
            } else {
                return new LinkedList<>(List.of(assignUpdate));
            }
        }
        return new LinkedList<>(List.of(assignUpdate));
    }

    private static List<AUpdate> unfoldLeavesOfAssignmentUpdate(String lhsName, String rhsName,
            TextPosition position, Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Collect the names of all leaves children of left (and right) hand side composite data types. If empty,
        // names refer to primitive type. Get flattened name if present.
        Set<String> lhsLeaves = propertyLeaves.get(lhsName);
        Set<String> rhsLeaves = propertyLeaves.get(rhsName);
        if (lhsLeaves == null && rhsLeaves == null) {
            String newLhsName = renames.getOrDefault(lhsName, lhsName);
            String newRhsName = renames.getOrDefault(rhsName, rhsName);
            return new LinkedList<>(List.of(createAAssignementUpdate(newLhsName, newRhsName, position)));
        }

        // Create a new assignment update of the unfolded properties for both left and right hand side.
        Set<String> leaves = lhsLeaves == null ? rhsLeaves : lhsLeaves;
        List<AUpdate> unfoldedAssignmentUpdates = new LinkedList<>();
        for (String leaf: leaves) {
            String newLhsName = renames.get(lhsName + leaf);
            String newRhsName = renames.get(rhsName + leaf);
            AUpdate currentAssignmentExpression = createAAssignementUpdate(newLhsName, newRhsName, position);
            unfoldedAssignmentUpdates.add(currentAssignmentExpression);
        }
        return unfoldedAssignmentUpdates;
    }

    private static AAssignmentUpdate createAAssignementUpdate(String lhs, String rhs, TextPosition position) {
        ANameExpression lhsNameExpression = new ANameExpression(new AName(lhs, position), false, position);
        ANameExpression rhsNameExpression = new ANameExpression(new AName(rhs, position), false, position);
        return new AAssignmentUpdate(lhsNameExpression, rhsNameExpression, position);
    }

    private static void unfoldAbstractActivity(Activity activity, Map<String, NamedElement> referenceableElements,
            Map<String, Set<String>> propertyLeaves, Map<String, String> renames)
    {
        // Unfold the precondition and postcondition constraints. Skip occurrence constraints.
        unfoldConstraints(activity.getPreconditions(), referenceableElements, propertyLeaves, renames);
        unfoldConstraints(activity.getPostconditions(), referenceableElements, propertyLeaves, renames);
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
            constraintSpec.getBodies().set(i, ACifObjectToString.toString(unfoldedBody));
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
        unfoldConstraints(activity.getPreconditions(), referenceableElements, propertyLeaves, renames);
        unfoldConstraints(activity.getPostconditions(), referenceableElements, propertyLeaves, renames);
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
        // Process the if statements.
        List<AExpression> unfoldedIfStatements = ifUpdate.guards.stream()
                .map(u -> unfoldACifExpression(u, referenceableElements, propertyLeaves, renames))
                .collect(Collectors.toList());

        // Process the elif statements.
        List<AElifUpdate> unfoldedElifs = ifUpdate.elifs.stream()
                .map(u -> unfoldACifElifUpdate(u, referenceableElements, propertyLeaves, renames))
                .collect(Collectors.toList());

        // Process the else statements.
        List<AUpdate> unfoldedElses = ifUpdate.elses.stream()
                .flatMap(u -> unfoldACifUpdate(u, referenceableElements, propertyLeaves, renames).stream()).toList();

        // Process the then statements.
        List<AUpdate> unfoldedThens = ifUpdate.thens.stream()
                .flatMap(u -> unfoldACifUpdate(u, referenceableElements, propertyLeaves, renames).stream()).toList();

        return new AIfUpdate(unfoldedIfStatements, unfoldedThens, unfoldedElifs, unfoldedElses, ifUpdate.position);
    }

    private static AElifUpdate unfoldACifElifUpdate(AElifUpdate elifUpdate,
            Map<String, NamedElement> referenceableElements, Map<String, Set<String>> propertyLeaves,
            Map<String, String> renames)
    {
        // Process the guards.
        List<AExpression> unfoldedElifGuards = elifUpdate.guards.stream()
                .map(u -> unfoldACifExpression(u, referenceableElements, propertyLeaves, renames))
                .collect(Collectors.toList());

        // Process the thens.
        List<AUpdate> unfoldedElifThens = elifUpdate.thens.stream()
                .flatMap(u -> unfoldACifUpdate(u, referenceableElements, propertyLeaves, renames).stream()).toList();

        return new AElifUpdate(unfoldedElifGuards, unfoldedElifThens, elifUpdate.position);
    }
}
