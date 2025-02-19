
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.ExternalCrossReferencer;
import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
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
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.uml.profile.cif.ACifObjectToString;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Objects;
import com.google.common.base.Verify;

import PokaYoke.PokaYokePackage;

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
        // Only perform flattening if there are any composite data types. This not only prevents unnecessary work, but
        // also prevents normalizing expressions/updates.
        CifContext context = new CifContext(model);
        List<DataType> dataTypes = context.getAllCompositeDataTypes();
        if (!dataTypes.isEmpty()) {
            Class activeClass = context.getAllClasses(c -> !(c instanceof Behavior) && c.isActive()).get(0);

            // Get per absolute name of a property with a composite data type, the relative names of its leaves. We use
            // this later to rewrite references to properties with composite data types.
            Map<String, Set<String>> propertyToLeaves = getLeavesForAllCompositeProperties(activeClass, "",
                    new LinkedHashMap<>());

            // Flatten all properties with composite data types. This also gives us for each original absolute name its
            // flattened name, which we use later to rewrite absolute names to flattened names in expression and
            // updates.
            Map<String, String> flatToAbsoluteNames = renameAndFlattenProperties(activeClass, new LinkedHashMap<>());
            Map<String, String> absoluteToFlatNames = flatToAbsoluteNames.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
            Verify.verify(flatToAbsoluteNames.size() == absoluteToFlatNames.size());

            // Unfold all references to properties with a composite data type in assignments and comparisons.
            unfoldClass(activeClass, propertyToLeaves, absoluteToFlatNames);

            // Delete the composite data types.
            model.getPackagedElements().removeAll(dataTypes);

            // Sanity check: there should not be any references to objects not contained in the model.
            Map<EObject, Collection<Setting>> problems = ExternalCrossReferencer.find(model);
            Map<Object, Object> filteredProblems = problems.entrySet().stream()
                    .filter(entry -> !isPokaYokeProfilePackageOrBoolean(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Verify.verify(filteredProblems.isEmpty());
        }
    }

    private static boolean isPokaYokeProfilePackageOrBoolean(Object o) {
        return (o instanceof Profile profile && profile.getName().equals("PokaYoke")) || o instanceof PokaYokePackage
                || (o instanceof PrimitiveType primitive && primitive.getName().equals("Boolean"));
    }

    /**
     * Get per absolute name of a property with a composite data type, the relative names of its leaves.
     *
     * @param owner The attribute owner, either a class or a composite data type.
     * @param prefix The absolute name of the attribute owner.
     * @param propertyToLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves. Is modified in place.
     * @return {@code propertyToLeaves}
     */
    private static Map<String, Set<String>> getLeavesForAllCompositeProperties(AttributeOwner owner, String prefix,
            Map<String, Set<String>> propertyToLeaves)
    {
        for (Property property: owner.getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                String absName = prefix.isEmpty() ? property.getName() : prefix + "." + property.getName();
                getLeavesForAllCompositeProperties((DataType)property.getType(), absName, propertyToLeaves);

                // Add this attribute owner to the result.
                Set<String> leaves = new LinkedHashSet<>();
                collectRelativeNamesOfLeafProperties(property, "", leaves);
                propertyToLeaves.put(absName, leaves);
            }
        }
        return propertyToLeaves;
    }

    /**
     * Collect the relative names of all leaf properties.
     *
     * @param parentProperty The parent UML property.
     * @param prefix The relative name of the parent property, to use as a prefix.
     * @param collectedNames The relative names collected so far. Is extended in-place.
     */
    private static void collectRelativeNamesOfLeafProperties(Property parentProperty, String prefix,
            Set<String> collectedNames)
    {
        for (Property property: ((DataType)parentProperty.getType()).getOwnedAttributes()) {
            String relName = prefix + "." + property.getName();
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                collectRelativeNamesOfLeafProperties(property, relName, collectedNames);
            } else {
                collectedNames.add(relName);
            }
        }
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
        List<Property> ownerProperties = attributeOwner.getOwnedAttributes();
        for (Property property: ownerProperties) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())
                    && !attributeOwnerRenames.containsKey((AttributeOwner)property.getType()))
            {
                renameAndFlattenProperties((DataType)property.getType(), attributeOwnerRenames);
            }
        }

        // Collect the names of the properties with non-composite data types, to avoid these names when flattening.
        Set<String> primitivePropertiesNames = ownerProperties.stream()
                .filter(p -> !PokaYokeTypeUtil.isCompositeDataType(p.getType())).map(Property::getName)
                .collect(Collectors.toSet());

        // Flatten the attribute owner's properties.
        Set<Property> propertiesToAdd = new LinkedHashSet<>();
        Set<Property> propertiesToRemove = new LinkedHashSet<>();
        Map<String, String> childrenRenames = new LinkedHashMap<>();
        for (Property property: ownerProperties) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                // Get renames for the children of the property's composite data type.
                Map<String, String> dataTypeRenames = attributeOwnerRenames.get((AttributeOwner)property.getType());
                Verify.verifyNotNull(dataTypeRenames);

                // Copy and rename the children.
                for (Property child: ((DataType)property.getType()).getOwnedAttributes()) {
                    Pair<Property, String> flattenedPropertyAndRelName = flattenChildProperty(child, property.getName(),
                            dataTypeRenames, primitivePropertiesNames);
                    Property flattenedProperty = flattenedPropertyAndRelName.getLeft();
                    primitivePropertiesNames.add(flattenedProperty.getName());
                    propertiesToAdd.add(flattenedProperty);
                    childrenRenames.put(flattenedProperty.getName(), flattenedPropertyAndRelName.getRight());
                }
                propertiesToRemove.add(property);
            }
        }
        attributeOwnerRenames.put(attributeOwner, childrenRenames);
        ownerProperties.removeAll(propertiesToRemove);
        ownerProperties.addAll(propertiesToAdd);

        // Return the renames for this attribute owner.
        return childrenRenames;
    }

    /**
     * Copy the given property and give it a unique flattened name within its new parent (attribute owner).
     *
     * @param child The child property of a composite data type to be flattened.
     * @param propertyName The name of the property with a composite data type that contains the child property.
     * @param absoluteToFlatNames Per original absolute name of a flattened property, its flattened name.
     * @param existingNames Names to avoid when renaming, since they already exist in the new parent (attribute owner).
     * @return The flattened property and its original relative name.
     */
    private static Pair<Property, String> flattenChildProperty(Property child, String propertyName,
            Map<String, String> absoluteToFlatNames, Set<String> existingNames)
    {
        // Copy the property and give it a clash-free name.
        String flattenedName = generateNewPropertyName(child.getName(), propertyName, existingNames);
        Property flattenedProperty = EcoreUtil.copy(child);
        flattenedProperty.setName(flattenedName);

        // Get the flattened property's original relative name and return it along with the flattened property.
        String childName = absoluteToFlatNames.getOrDefault(child.getName(), child.getName());
        String origRelName = propertyName + "." + childName;
        return Pair.of(flattenedProperty, origRelName);
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

    /**
     * Unfold all references to properties with composite data type in assignments and comparisons, in the given class.
     *
     * @param clazz The class in which to do the unfolding.
     * @param propertyToLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves.
     * @param absoluteToFlatNames Per original absolute name of a flattened property, its flattened name.
     */
    private static void unfoldClass(Class clazz, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        // Unfold opaque behaviors and activities.
        for (Behavior classBehavior: clazz.getOwnedBehaviors()) {
            if (classBehavior instanceof OpaqueBehavior element) {
                unfoldRedefinableElement(element, propertyToLeaves, absoluteToFlatNames);
            } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                unfoldAbstractActivity(activity, propertyToLeaves, absoluteToFlatNames);
            } else if (classBehavior instanceof Activity activity && !activity.isAbstract()) {
                unfoldConcreteActivity(activity, propertyToLeaves, absoluteToFlatNames);
            } else {
                throw new RuntimeException(String.format("Unfolding behaviors of class '%s' not supported.",
                        classBehavior.getClass().getSimpleName()));
            }
        }

        // Unfold constraints.
        unfoldConstraints(clazz.getOwnedRules(), propertyToLeaves, absoluteToFlatNames);
    }

    /**
     * Unfolds guards and effects of a redefinable element.
     *
     * @param element The redefinable element.
     * @param propertyToLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves.
     * @param absoluteToFlatNames Per original absolute name of a flattened property, its flattened name.
     */
    private static void unfoldRedefinableElement(RedefinableElement element, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        // Perform the guard unfolding. Skip if there is no guard.
        AExpression guardExpr = CifParserHelper.parseGuard(element);
        if (guardExpr != null) {
            AExpression newGuard = unfoldAExpression(guardExpr, propertyToLeaves, absoluteToFlatNames);
            String newGuardString = ACifObjectToString.toString(newGuard);
            PokaYokeUmlProfileUtil.setGuard(element, newGuardString);
        }

        // Perform the unfolding of the effects.
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        List<String> newEffects = new ArrayList<>();
        for (String effect: effects) {
            List<AUpdate> updates = CifParserHelper.parseUpdates(effect, element);
            String newEffect = updates.stream()
                    .flatMap(update -> unfoldAUpdate(update, propertyToLeaves, absoluteToFlatNames).stream())
                    .map(newUpdate -> ACifObjectToString.toString(newUpdate)).collect(Collectors.joining(", "));
            newEffects.add(newEffect);
        }
        PokaYokeUmlProfileUtil.setEffects(element, newEffects);
    }

    /**
     * Unfolds a CIF {@link AExpression}: replaces comparisons between properties with composite data types by
     * comparisons of the respective flattened leaf properties.
     *
     * @param expression A CIF {@link AExpression} to be unfolded.
     * @param propertyToLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves.
     * @param absoluteToFlatNames Per original absolute name of a flattened property, its flattened name.
     * @return The unfolded CIF {@link AExpression}.
     */
    private static AExpression unfoldAExpression(AExpression expression, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        if (expression instanceof ABinaryExpression binExpr) {
            AExpression unfoldedLhsExpression = unfoldAExpression(binExpr.left, propertyToLeaves, absoluteToFlatNames);
            AExpression unfoldedRhsExpression = unfoldAExpression(binExpr.right, propertyToLeaves, absoluteToFlatNames);

            // Unfold comparisons where both are name expressions.
            if (binExpr.left instanceof ANameExpression lhsNameExpr
                    && binExpr.right instanceof ANameExpression rhsNameExpr)
            {
                // Assert that both lhs and rhs are of the same composite data types, or both are not.
                Set<String> lhsLeaves = propertyToLeaves.get(lhsNameExpr.name.name);
                Set<String> rhsLeaves = propertyToLeaves.get(rhsNameExpr.name.name);
                Verify.verify(Objects.equal(lhsLeaves, rhsLeaves));

                // Unfold both if composite data types.
                if (lhsLeaves != null) {
                    return unfoldComparisonExpression(lhsNameExpr.name.name, rhsNameExpr.name.name, lhsLeaves,
                            binExpr.operator, binExpr.position, absoluteToFlatNames);
                } else {
                    String newLhsName = absoluteToFlatNames.getOrDefault(lhsNameExpr.name.name, lhsNameExpr.name.name);
                    String newRhsName = absoluteToFlatNames.getOrDefault(rhsNameExpr.name.name, rhsNameExpr.name.name);
                    Verify.verify(newLhsName != null && newRhsName != null);
                    ANameExpression lhsExpression = new ANameExpression(new AName(newLhsName, binExpr.position), false,
                            binExpr.position);
                    ANameExpression rhsExpression = new ANameExpression(new AName(newRhsName, binExpr.position), false,
                            binExpr.position);
                    return new ABinaryExpression(binExpr.operator, lhsExpression, rhsExpression, binExpr.position);
                }
            }

            // Combine the unfolded left and right components to form a new binary expression.
            return new ABinaryExpression(binExpr.operator, unfoldedLhsExpression, unfoldedRhsExpression,
                    expression.position);
        } else if (expression instanceof AUnaryExpression unaryExpr) {
            return new AUnaryExpression(unaryExpr.operator,
                    unfoldAExpression(unaryExpr.child, propertyToLeaves, absoluteToFlatNames), unaryExpr.position);
        } else if (expression instanceof ANameExpression nameExpr) {
            return unfoldANameExpression(nameExpr.name.name, expression.position, absoluteToFlatNames);
        } else if (expression instanceof ABoolExpression || expression instanceof AIntExpression) {
            // Expressions without children don't need unfolding.
            return expression;
        } else {
            throw new RuntimeException(String.format("Unfolding expressions of class '%s' is not supported.",
                    expression.getClass().getSimpleName()));
        }
    }

    private static ABinaryExpression unfoldComparisonExpression(String lhsName, String rhsName, Set<String> leaves,
            String operator, TextPosition position, Map<String, String> absoluteToFlatNames)
    {
        ABinaryExpression unfoldedBinaryExpression = null;
        String unfoldOperator = operator.equals("=") ? "and" : "or";
        for (String leaf: leaves) {
            String newLhsName = absoluteToFlatNames.get(lhsName + leaf);
            String newRhsName = absoluteToFlatNames.get(rhsName + leaf);
            ANameExpression lhsExpression = new ANameExpression(new AName(newLhsName, position), false, position);
            ANameExpression rhsExpression = new ANameExpression(new AName(newRhsName, position), false, position);
            ABinaryExpression currentBinaryExpression = new ABinaryExpression(operator, lhsExpression, rhsExpression,
                    position);

            // According to the operator, create a new binary expression as a conjunction or disjunction of unfolded
            // expressions.
            if (unfoldedBinaryExpression == null) {
                unfoldedBinaryExpression = currentBinaryExpression;
            } else {
                unfoldedBinaryExpression = new ABinaryExpression(unfoldOperator, unfoldedBinaryExpression,
                        currentBinaryExpression, position);
            }
        }
        return unfoldedBinaryExpression;
    }

    private static ANameExpression unfoldANameExpression(String name, TextPosition position,
            Map<String, String> absoluteToFlatNames)
    {
        String newName = absoluteToFlatNames.getOrDefault(name, name);
        return new ANameExpression(new AName(newName, position), false, position);
    }

    /**
     * Unfolds a CIF {@link AUpdate}: replaces updates between properties with composite data types by updates of the
     * respective flattened leaf properties.
     *
     * @param update A CIF {@link AUpdate} to be unfolded.
     * @param propertyToLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves.
     * @param absoluteToFlatNames Per original absolute name of a flattened property, its flattened name.
     * @return The list containing the unfolded CIF {@link AUpdate}.
     */
    private static List<AUpdate> unfoldAUpdate(AUpdate update, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        if (update instanceof AAssignmentUpdate assign) {
            return unfoldAAssignmentUpdate(assign, propertyToLeaves, absoluteToFlatNames);
        } else if (update instanceof AIfUpdate ifUpdate) {
            AUpdate newIfUpdate = unfoldAIfUpdate(ifUpdate, propertyToLeaves, absoluteToFlatNames);
            return List.of(newIfUpdate);
        } else {
            throw new RuntimeException(
                    String.format("Unfolding updates of class '%s' not supported.", update.getClass().getSimpleName()));
        }
    }

    /**
     * Unfolds a CIF {@link AAssignmentUpdate}: replaces assignments between properties with composite data types by
     * assignments of the respective flattened leaf properties.
     *
     * @param assignUpdate A CIF {@link AAssignmentUpdate} to be unfolded.
     * @param propertyToLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves.
     * @param absoluteToFlatNames Per original absolute name of a flattened property, its flattened name.
     * @return The unfolded CIF {@link AAssignmentUpdate}.
     */
    private static List<AUpdate> unfoldAAssignmentUpdate(AAssignmentUpdate assignUpdate,
            Map<String, Set<String>> propertyToLeaves, Map<String, String> absoluteToFlatNames)
    {
        // Sanity check: 'addressable' must be a name expression.
        Verify.verify(assignUpdate.addressable instanceof ANameExpression);
        Set<String> lhsLeaves = propertyToLeaves.get(((ANameExpression)assignUpdate.addressable).name.name);
        if (lhsLeaves != null) {
            // If 'addressable' is of composite data type, also 'value' must be a name expression and be of the same
            // composite data type. Unfold them together.
            Verify.verify(assignUpdate.value instanceof ANameExpression);
            Set<String> rhsLeaves = propertyToLeaves.get(((ANameExpression)assignUpdate.value).name.name);
            Verify.verify(Objects.equal(lhsLeaves, rhsLeaves));
            return unfoldLeavesOfAAssignmentUpdate(((ANameExpression)assignUpdate.addressable).name.name,
                    ((ANameExpression)assignUpdate.value).name.name, lhsLeaves, assignUpdate.position,
                    absoluteToFlatNames);
        } else {
            return List.of(new AAssignmentUpdate(
                    unfoldAExpression(assignUpdate.addressable, propertyToLeaves, absoluteToFlatNames),
                    unfoldAExpression(assignUpdate.value, propertyToLeaves, absoluteToFlatNames),
                    assignUpdate.position));
        }
    }

    private static List<AUpdate> unfoldLeavesOfAAssignmentUpdate(String lhsName, String rhsName, Set<String> leaves,
            TextPosition position, Map<String, String> absoluteToFlatNames)
    {
        // Create a new assignment update of the unfolded properties for both left and right hand side.
        List<AUpdate> unfoldedAssignmentUpdates = new ArrayList<>();
        for (String leaf: leaves) {
            String newLhsName = absoluteToFlatNames.get(lhsName + leaf);
            String newRhsName = absoluteToFlatNames.get(rhsName + leaf);
            ANameExpression lhsNameExpression = new ANameExpression(new AName(newLhsName, position), false, position);
            ANameExpression rhsNameExpression = new ANameExpression(new AName(newRhsName, position), false, position);
            unfoldedAssignmentUpdates.add(new AAssignmentUpdate(lhsNameExpression, rhsNameExpression, position));
        }
        return unfoldedAssignmentUpdates;
    }

    /**
     * Unfolds a CIF {@link AIfUpdate}: replaces updates between properties with composite data types by updates of the
     * respective flattened leaf properties.
     *
     * @param ifUpdate A CIF {@link AIfUpdate} to be unfolded.
     * @param propertyToLeaves Per absolute name of a property with a composite data type, the relative names of its
     *     leaves.
     * @param absoluteToFlatNames Per original absolute name of a flattened property, its flattened name.
     * @return The unfolded CIF {@link AIfUpdate}.
     */
    private static AUpdate unfoldAIfUpdate(AIfUpdate ifUpdate, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        // Process the 'if' statements.
        List<AExpression> unfoldedGuards = ifUpdate.guards.stream()
                .map(u -> unfoldAExpression(u, propertyToLeaves, absoluteToFlatNames)).toList();

        // Process the 'then' statements.
        List<AUpdate> unfoldedThens = ifUpdate.thens.stream()
                .flatMap(u -> unfoldAUpdate(u, propertyToLeaves, absoluteToFlatNames).stream()).toList();

        // Process the 'elif' statements.
        List<AElifUpdate> unfoldedElifs = ifUpdate.elifs.stream()
                .map(u -> unfoldAElifUpdate(u, propertyToLeaves, absoluteToFlatNames)).toList();

        // Process the 'else' statements.
        List<AUpdate> unfoldedElses = ifUpdate.elses.stream()
                .flatMap(u -> unfoldAUpdate(u, propertyToLeaves, absoluteToFlatNames).stream()).toList();

        return new AIfUpdate(unfoldedGuards, unfoldedThens, unfoldedElifs, unfoldedElses, ifUpdate.position);
    }

    private static AElifUpdate unfoldAElifUpdate(AElifUpdate elifUpdate, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        // Process the 'guards'.
        List<AExpression> unfoldedElifGuards = elifUpdate.guards.stream()
                .map(u -> unfoldAExpression(u, propertyToLeaves, absoluteToFlatNames)).toList();

        // Process the 'thens'.
        List<AUpdate> unfoldedElifThens = elifUpdate.thens.stream()
                .flatMap(u -> unfoldAUpdate(u, propertyToLeaves, absoluteToFlatNames).stream()).toList();

        return new AElifUpdate(unfoldedElifGuards, unfoldedElifThens, elifUpdate.position);
    }

    private static void unfoldAbstractActivity(Activity activity, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        // Unfold the precondition and postcondition constraints. Skip occurrence constraints.
        unfoldConstraints(activity.getPreconditions(), propertyToLeaves, absoluteToFlatNames);
        unfoldConstraints(activity.getPostconditions(), propertyToLeaves, absoluteToFlatNames);
    }

    private static void unfoldConstraints(List<Constraint> umlConstraints, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        for (Constraint constraint: umlConstraints) {
            if (constraint instanceof IntervalConstraint) {
                continue; // Skip occurrence constraints.
            } else if (constraint.getSpecification() instanceof OpaqueExpression opaqueSpec) {
                unfoldOpaqueExpression(opaqueSpec, propertyToLeaves, absoluteToFlatNames);
            } else {
                throw new RuntimeException(
                        String.format("Unfolding constraints of class '%s' with specification '%s' is not supported.",
                                constraint.getClass().getSimpleName(),
                                constraint.getSpecification().getClass().getSimpleName()));
            }
        }
    }

    private static void unfoldOpaqueExpression(OpaqueExpression opaqueExpression,
            Map<String, Set<String>> propertyToLeaves, Map<String, String> absoluteToFlatNames)
    {
        List<AExpression> constraintBodyExpressions = CifParserHelper.parseBodies(opaqueExpression);
        for (int i = 0; i < constraintBodyExpressions.size(); i++) {
            // Get the current body, unfold it, and substitute the corresponding string.
            ACifObject currentBody = constraintBodyExpressions.get(i);
            ACifObject unfoldedBody;
            if (currentBody instanceof AExpression bodyExpression) {
                unfoldedBody = unfoldAExpression(bodyExpression, propertyToLeaves, absoluteToFlatNames);
            } else if (currentBody instanceof AInvariant bodyInvariant) {
                unfoldedBody = unfoldAInvariant(bodyInvariant, propertyToLeaves, absoluteToFlatNames);
            } else {
                throw new RuntimeException(String.format("Unfolding guard bodies of class '%s' is not supported.",
                        currentBody.getClass().getSimpleName()));
            }
            opaqueExpression.getBodies().set(i, ACifObjectToString.toString(unfoldedBody));
        }
    }

    private static AInvariant unfoldAInvariant(AInvariant invariant, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        // Unfold only the invariant predicate.
        return new AInvariant(invariant.name,
                unfoldAExpression(invariant.predicate, propertyToLeaves, absoluteToFlatNames), invariant.invKind,
                invariant.events);
    }

    private static void unfoldConcreteActivity(Activity activity, Map<String, Set<String>> propertyToLeaves,
            Map<String, String> absoluteToFlatNames)
    {
        // Unfold the guards and effects of every control flow, call behavior, and opaque action.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                ValueSpecification guard = controlEdge.getGuard();
                if (guard instanceof OpaqueExpression opaqueGuard) {
                    unfoldOpaqueExpression(opaqueGuard, propertyToLeaves, absoluteToFlatNames);
                } else if (guard instanceof LiteralBoolean) {
                    continue;
                } else {
                    throw new RuntimeException(
                            String.format("Unfolding control flow guards of class '%s' is not supported.",
                                    guard.getClass().getSimpleName()));
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                Behavior behavior = callBehavior.getBehavior();
                if (behavior instanceof OpaqueBehavior opaqueGuard) {
                    unfoldRedefinableElement(opaqueGuard, propertyToLeaves, absoluteToFlatNames);
                } else if (behavior instanceof Activity calledActivity && calledActivity.isAbstract()) {
                    unfoldAbstractActivity(calledActivity, propertyToLeaves, absoluteToFlatNames);
                } else if (behavior instanceof Activity calledActivity && !calledActivity.isAbstract()) {
                    unfoldConcreteActivity(calledActivity, propertyToLeaves, absoluteToFlatNames);
                } else {
                    throw new RuntimeException(
                            String.format("Unfolding call behavior actions of class '%s' is not supported.",
                                    behavior.getClass().getSimpleName()));
                }
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                unfoldRedefinableElement(internalAction, propertyToLeaves, absoluteToFlatNames);
            } else if (ownedElement instanceof List elementList && elementList.get(0) instanceof Constraint) {
                @SuppressWarnings("unchecked")
                List<Constraint> constraints = elementList;
                unfoldConstraints(constraints, propertyToLeaves, absoluteToFlatNames);
            } else if (ownedElement instanceof ActivityNode activityNode) {
                // Nodes in activities should not refer to properties.
                continue;
            } else {
                throw new RuntimeException(String.format("Unfolding elements of class '%s' not supported",
                        ownedElement.getClass().getSimpleName()));
            }
        }
    }
}
