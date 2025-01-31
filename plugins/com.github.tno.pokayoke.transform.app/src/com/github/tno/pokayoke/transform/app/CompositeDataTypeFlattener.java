
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.github.tno.pokayoke.uml.profile.cif.ACifObjectTranslator;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

/**
 * Composite data type flattener.
 */
public class CompositeDataTypeFlattener {
    private CompositeDataTypeFlattener() {
    }

    /**
     * Finds all properties that are instantiations of a composite data type, inlines them with flattened names,
     * rewrites the constraints and owned behaviors of the active class, and deletes all composite data types. A
     * composite data type property represents a canonical "object" class, e.g. a robot, may contain only properties and
     * nothing else.
     *
     * @param model The UML model.
     */
    public static void inlineNestedDataTypes(Model model) {
        CifContext context = new CifContext(model);
        Class activeClass = getSingleActiveClass(context);
        List<DataType> dataTypes = context.getAllDeclaredDataTypes(d -> PokaYokeTypeUtil.isCompositeDataType(d));

        // Step 1:
        // Unfold all references to properties with composite data type in assignments and comparisons, in the whole UML
        // model.
        unfoldCompositeDataTypeReferences(activeClass, context.getReferenceableElements());

        // Step 2:
        // Find all properties of the main class that are instances of a composite data class, recursively rewrites them
        // with a flattened name, and return a map linking to the original reference name.
        Map<String, String> flatToDottedNames = renameAndFlattenProperties(activeClass, new LinkedHashMap<>());

        // TODO: Temporary fix for duplicate names (discussion point).
        Map<String, String> dottedToFlatNames = flatToDottedNames.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (oldValue, newValue) -> newValue));

        // Step 3:
        // Delete the composite data types and related properties.
        deleteDataTypes(activeClass, dataTypes);

        // Step 4:
        // Updates the opaque behaviors, abstract and concrete activities of the active class with the flattened names.
        rewriteCompositeDataTypeReferences(activeClass, dottedToFlatNames);
    }

    /**
     * Gets the only active class of the UML model. The uniqueness check is performed in PokaYokeProfileValidator.
     *
     * @param context The CIF context.
     * @return A single active class.
     */
    private static Class getSingleActiveClass(CifContext context) {
        return context.getAllClasses(c -> !(c instanceof Behavior) && c.isActive()).get(0);
    }

    /**
     * Unfold all references to properties with composite data type in assignments and comparisons, in the given class.
     *
     * @param clazz The class in which to do the unfolding.
     * @param referenceableElements Per absolute name of a referenceable element, the element that is referenced.
     */
    private static void unfoldCompositeDataTypeReferences(Class clazz,
            Map<String, NamedElement> referenceableElements)
    {
        // Unfold opaque behaviors and activities.
        for (Behavior classBehavior: clazz.getOwnedBehaviors()) {
            if (classBehavior instanceof OpaqueBehavior element) {
                unfoldGuardAndEffects(element, referenceableElements);
            } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                unfoldAbstractActivity(activity, referenceableElements);
            } else if (classBehavior instanceof Activity activity && !activity.isAbstract()) {
                unfoldConcreteActivity(activity, referenceableElements);
            } else {
                throw new RuntimeException(String.format("Unfolding behaviors of class '%s' not supported.", clazz));
            }
        }

        // Unfold constraints.
        unfoldConstraints(clazz.getOwnedRules(), referenceableElements);
    }

    /**
     * Unfolds guards and effects of a redefinable element.
     *
     * @param element The redefinable element.
     * @param referenceableElements The CIF context as a map.
     */
    private static void unfoldGuardAndEffects(RedefinableElement element,
            Map<String, NamedElement> referenceableElements)
    {
        // Perform the guard unfolding.
        AExpression guardExpr = CifParserHelper.parseGuard(element);
        AExpression newGuard = unfoldACifExpression(guardExpr, referenceableElements);
        String newGuardString = ACifObjectTranslator.toString(newGuard);
        PokaYokeUmlProfileUtil.setGuard(element, newGuardString);

        // Perform the unfolding for the effects (list of list of updates).
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        List<String> newEffects = new LinkedList<>();
        for (String effect: effects) {
            List<AUpdate> updates = CifParserHelper.parseUpdates(effect, element);
            List<String> newUpdateStrings = new LinkedList<>();
            for (AUpdate update: updates) {
                if (update instanceof AAssignmentUpdate assign) {
                    List<AAssignmentUpdate> newUpdates = unfoldACifAssignmentUpdate(assign, referenceableElements);
                    for (ACifObject newUpdate: newUpdates) {
                        String newUpdateString = ACifObjectTranslator.toString(newUpdate);
                        newUpdateStrings.add(newUpdateString);
                    }
                } else if (update instanceof AIfUpdate ifUpdate) {
                    ACifObject newIfUpdate = unfoldACifIfUpdate(ifUpdate, referenceableElements);
                    String newIfUpdateString = ACifObjectTranslator.toString(newIfUpdate);
                    newUpdateStrings.add(newIfUpdateString);
                } else {
                    throw new RuntimeException(String.format("Unfolding unsupported update: %s.", update));
                }
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
     * @return The unfolded CIF {@link AExpression}.
     */
    private static AExpression unfoldACifExpression(AExpression expression,
            Map<String, NamedElement> referenceableElements)
    {
        if (expression instanceof ABinaryExpression binExpr) {
            AExpression unfoldedLeft = unfoldACifExpression(binExpr.left, referenceableElements);
            AExpression unfoldedRight = unfoldACifExpression(binExpr.right, referenceableElements);

            // Unfold comparisons of properties with composite data types.
            if (binExpr.left instanceof ANameExpression leftNameExpr
                    && binExpr.right instanceof ANameExpression rightNameExpr)
            {
                ABinaryExpression unfoldedExpression = unfoldComparisonExpression(leftNameExpr.name.name,
                        rightNameExpr.name.name, binExpr.operator, binExpr.position, referenceableElements);
                return unfoldedExpression;
            }

            // Combine the unfolded left and right components to form a new binary expression.
            ABinaryExpression unfoldedExpression = new ABinaryExpression(binExpr.operator, unfoldedLeft, unfoldedRight,
                    expression.position);
            return unfoldedExpression;
        } else if (expression instanceof AUnaryExpression unaryExpr) {
            return new AUnaryExpression(unaryExpr.operator,
                    unfoldACifExpression(unaryExpr.child, referenceableElements), unaryExpr.position);
        } else {
            // Expressions without children don't need unfolding.
            return expression;
        }
    }

    private static ABinaryExpression unfoldComparisonExpression(String lhsName, String rhsName, String operator,
            TextPosition position, Map<String, NamedElement> referenceableElements)
    {
        // Unfold if left hand side and right hand side are both composite data types.
        NamedElement leftElement = referenceableElements.get(lhsName);
        NamedElement rightElement = referenceableElements.get(rhsName);

        if (leftElement instanceof Property leftProperty && PokaYokeTypeUtil.isCompositeDataType(leftProperty.getType())
                && rightElement instanceof Property rightProperty
                && PokaYokeTypeUtil.isCompositeDataType(rightProperty.getType()))
        {
            // Sanity check: left and right composite data types should be the same.
            Verify.verify(leftProperty.getType().equals(rightProperty.getType()), String
                    .format("Trying to compare or assign two different data types: '%s' and '%s'.", lhsName, rhsName));

            // Collect the names of all leaves children of left (and right) hand side composite data types.
            Set<String> leaves = new LinkedHashSet<>();
            PokaYokeTypeUtil.collectPropertyNamesUntilLeaf(leftProperty, "", leaves);

            // Create the new binary expression of the unfolded properties for both left and right hand side.
            ABinaryExpression unfoldedBinaryExpression = null;
            for (String leaf: leaves) {
                ABinaryExpression currentBinaryExpression = createABinaryExpression(lhsName + leaf, rhsName + leaf,
                        operator, position);

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
            // If both are properties, check that both are not composite data types.
            if (leftElement instanceof Property leftProperty && rightElement instanceof Property rightProperty) {
                Verify.verify(!(PokaYokeTypeUtil.isCompositeDataType(leftProperty.getType()))
                        && !(PokaYokeTypeUtil.isCompositeDataType(rightProperty.getType())));
            }
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

    private static List<AAssignmentUpdate> unfoldACifAssignmentUpdate(AAssignmentUpdate assignUpdate,
            Map<String, NamedElement> referenceableElements)
    {
        if (assignUpdate.addressable instanceof ANameExpression aNameAddressable
                && assignUpdate.value instanceof ANameExpression aNameValue)
        {
            String lhsName = aNameAddressable.name.name;
            String rhsName = aNameValue.name.name;
            List<AAssignmentUpdate> unfoldedAssignment = getUnfoldedAssignmentUpdate(lhsName, rhsName,
                    assignUpdate.position, referenceableElements);
            return unfoldedAssignment;
        }
        // If 'addressable' and 'value' are not both ANameExpression, skip the unfolding and return the expression.
        return new LinkedList<>(List.of(assignUpdate));
    }

    private static List<AAssignmentUpdate> getUnfoldedAssignmentUpdate(String lhsName, String rhsName,
            TextPosition position, Map<String, NamedElement> referenceableElements)
    {
        if (!(referenceableElements.get(rhsName) instanceof Property)
                || !(referenceableElements.get(lhsName) instanceof Property))
        {
            // If left hand side or right hand side are not a variable, there is no unfolding to be done.
            return new LinkedList<>(List.of(getNewAssignementUpdate(lhsName, rhsName, position)));
        }
        Property lhsProperty = (Property)referenceableElements.get(lhsName);
        Property rhsProperty = (Property)referenceableElements.get(rhsName);

        // Find all leaves children of left and right hand side data type.
        Set<String> leavesLeft = new LinkedHashSet<>();
        PokaYokeTypeUtil.collectPropertyNamesUntilLeaf(lhsProperty, "", leavesLeft);
        Set<String> leavesRight = new LinkedHashSet<>();
        PokaYokeTypeUtil.collectPropertyNamesUntilLeaf(rhsProperty, "", leavesRight);

        // Sanity check: leaves of the left and right hand sides should be the same.
        Verify.verify(leavesLeft.equals(leavesRight), String
                .format("Trying to compare or assign two different data types: '%s' and '%s'.", lhsName, rhsName));

        // If the leaves set is empty, the expression refers to a leaf type; there is no unfolding to be done.
        if (leavesLeft.isEmpty()) {
            return new LinkedList<>(List.of(getNewAssignementUpdate(lhsName, rhsName, position)));
        }

        // Create a new assignment update of the unfolded properties for both left and right hand side.
        List<AAssignmentUpdate> unfoldedAssignmentUpdates = new LinkedList<>();
        for (String leaf: leavesLeft) {
            AAssignmentUpdate currentAssignmentExpression = getNewAssignementUpdate(lhsName + leaf, rhsName + leaf,
                    position);
            unfoldedAssignmentUpdates.add(currentAssignmentExpression);
        }
        return unfoldedAssignmentUpdates;
    }

    private static AAssignmentUpdate getNewAssignementUpdate(String lhs, String rhs, TextPosition position) {
        ANameExpression leftExpression = new ANameExpression(new AName(lhs, position), false, position);
        ANameExpression rightExpression = new ANameExpression(new AName(rhs, position), false, position);
        return new AAssignmentUpdate(leftExpression, rightExpression, position);
    }

    private static void unfoldAbstractActivity(Activity activity, Map<String, NamedElement> referenceableElements) {
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        // Unfold the precondition and postcondition constraints.
        unfoldConstraints(preconditions, referenceableElements);
        unfoldConstraints(postconditions, referenceableElements);
    }

    private static void unfoldConstraints(List<Constraint> umlConstraints,
            Map<String, NamedElement> referenceableElements)
    {
        for (Constraint constraint: umlConstraints) {
            // Skip occurrence constraints.
            if (constraint instanceof IntervalConstraint) {
                continue;
            } else if (constraint.getSpecification() instanceof OpaqueExpression opaqueSpec) {
                unfoldGuardBodies(opaqueSpec, referenceableElements);
            } else {
                throw new RuntimeException(
                        "Constraint specification " + constraint.getSpecification() + " is not an opaque expression.");
            }
        }
    }

    private static void unfoldGuardBodies(OpaqueExpression constraintSpec,
            Map<String, NamedElement> referenceableElements)
    {
        List<AExpression> constraintBodyExpressions = CifParserHelper.parseBodies(constraintSpec);
        List<String> constraintBodyStrings = constraintSpec.getBodies();

        for (int i = 0; i < constraintBodyExpressions.size(); i++) {
            // Get the current body, unfold it, and substitute the corresponding string.
            ACifObject currentBody = constraintBodyExpressions.get(i);
            ACifObject unfoldedBody;
            if (currentBody instanceof AExpression bodyExpression) {
                unfoldedBody = unfoldACifExpression(bodyExpression, referenceableElements);
            } else if (currentBody instanceof AInvariant bodyInvariant) {
                unfoldedBody = unfoldACifInvariant(bodyInvariant, referenceableElements);
            } else {
                throw new RuntimeException("Guard body is not an expression nor an invariant.");
            }
            String newBodyString = ACifObjectTranslator.toString(unfoldedBody);
            constraintBodyStrings.set(i, newBodyString);
        }
    }

    private static AInvariant unfoldACifInvariant(AInvariant invariant,
            Map<String, NamedElement> referenceableElements)
    {
        // Unfold only the invariant predicate.
        return new AInvariant(invariant.name, unfoldACifExpression(invariant.predicate, referenceableElements),
                invariant.invKind, invariant.events);
    }

    private static void unfoldConcreteActivity(Activity activity, Map<String, NamedElement> referenceableElements) {
        // Unfold the behaviors of every control flow, call behavior, and opaque action.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                ValueSpecification guard = controlEdge.getGuard();
                if (guard instanceof OpaqueExpression opaqueGuard) {
                    unfoldGuardBodies(opaqueGuard, referenceableElements);
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                Behavior guard = callBehavior.getBehavior();
                if (guard instanceof OpaqueBehavior opaqueGuard) {
                    unfoldGuardAndEffects(opaqueGuard, referenceableElements);
                } else {
                    throw new RuntimeException(
                            String.format("Call behavior of class %s is not supported.", guard.getClass()));
                }
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                unfoldGuardAndEffects(internalAction, referenceableElements);
            } else if (ownedElement instanceof ActivityNode activityNode) {
                // This assumes that nodes in activities have empty names and bodies.
                continue;
            } else {
                throw new RuntimeException(String.format("Renaming flattened properties of class '%s' not supported",
                        ownedElement.getClass()));
            }
        }

        // Unfold pre and postconditions.
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        unfoldConstraints(preconditions, referenceableElements);
        unfoldConstraints(postconditions, referenceableElements);
    }

    private static AUpdate unfoldACifIfUpdate(AIfUpdate ifUpdate, Map<String, NamedElement> referenceableElements) {
        // Process the if statements. The element of the list represent the conditions separated by a comma.
        List<AExpression> ifStatements = ifUpdate.guards;
        List<AExpression> unfoldedIfStatements = new LinkedList<>();
        for (AExpression ifStatement: ifStatements) {
            AExpression unfoldedIfStatement = unfoldACifExpression(ifStatement, referenceableElements);
            unfoldedIfStatements.add(unfoldedIfStatement);
        }

        // Process the elif statements. Each element of the list represents a complete elif statement.
        List<AElifUpdate> elifStatements = ifUpdate.elifs;
        List<AElifUpdate> unfoldedElifs = new LinkedList<>();
        for (AElifUpdate elifStatement: elifStatements) {
            AElifUpdate unfoldedElifStatement = unfoldACifElifUpdate(elifStatement, referenceableElements);
            unfoldedElifs.add(unfoldedElifStatement);
        }

        // Process the else statements as CIF AUpdates. Each element of the list represents a different assignment,
        // syntactically separated by a comma.
        List<AUpdate> elseStatements = ifUpdate.elses;
        List<AUpdate> unfoldedElses = new LinkedList<>();
        for (AUpdate elseStatement: elseStatements) {
            if (elseStatement instanceof AAssignmentUpdate elseAssignment) {
                List<AAssignmentUpdate> unfoldedElseStatement = unfoldACifAssignmentUpdate(elseAssignment,
                        referenceableElements);
                unfoldedElses.addAll(unfoldedElseStatement);
            } else {
                AUpdate unfoldedElseStatement = unfoldACifIfUpdate((AIfUpdate)elseStatement, referenceableElements);
                unfoldedElses.add(unfoldedElseStatement);
            }
        }

        // Process the then statements as CIF AUpdates. Each element of the list represents a different assignment,
        // syntactically separated by a comma.
        List<AUpdate> thenStatements = ifUpdate.thens;
        List<AUpdate> unfoldedThens = new LinkedList<>();
        for (AUpdate thenStatement: thenStatements) {
            if (thenStatement instanceof AAssignmentUpdate thenAssignment) {
                List<AAssignmentUpdate> unfoldedThenStatement = unfoldACifAssignmentUpdate(thenAssignment,
                        referenceableElements);
                unfoldedThens.addAll(unfoldedThenStatement);
            } else {
                AUpdate unfoldedThenStatement = unfoldACifIfUpdate((AIfUpdate)thenStatement, referenceableElements);
                unfoldedThens.add(unfoldedThenStatement);
            }
        }
        return new AIfUpdate(unfoldedIfStatements, unfoldedThens, unfoldedElifs, unfoldedElses, ifUpdate.position);
    }

    private static AElifUpdate unfoldACifElifUpdate(AElifUpdate elifUpdate,
            Map<String, NamedElement> referenceableElements)
    {
        // Process the elif guards as CIF AExpressions.
        List<AExpression> elifGuards = elifUpdate.guards;
        List<AExpression> unfoldedElifGuards = new LinkedList<>();
        for (AExpression elifGuard: elifGuards) {
            AExpression unfoldedElifGuard = unfoldACifExpression(elifGuard, referenceableElements);
            unfoldedElifGuards.add(unfoldedElifGuard);
        }

        // Process the elif thens as CIF AUpdates.
        List<AUpdate> elifThens = elifUpdate.thens;
        List<AUpdate> unfoldedElifThens = new LinkedList<>();
        for (AUpdate elifThen: elifThens) {
            if (elifThen instanceof AAssignmentUpdate elifThenAssignment) {
                List<AAssignmentUpdate> unfoldedElifThen = unfoldACifAssignmentUpdate(elifThenAssignment,
                        referenceableElements);
                unfoldedElifThens.addAll(unfoldedElifThen);
            } else {
                AUpdate unfoldedElifThen = unfoldACifIfUpdate((AIfUpdate)elifThen, referenceableElements);
                unfoldedElifThens.add(unfoldedElifThen);
            }
        }
        return new AElifUpdate(unfoldedElifGuards, unfoldedElifThens, elifUpdate.position);
    }

    // STEP 2 METHODS START HERE.

    /**
     * Recursively computes a map of the nested properties, where the keys are the new flattened names, and the values
     * are pairs of the old dotted names and the property.
     *
     * @param attributeOwner The attribute owner whose properties are looped over.
     * @param renamingMap The map linking the new flattened names to the old dotted names and its property.
     * @return A map containing the old and new names, along with the property they refer to.
     */
    private static Map<String, String> renameAndFlattenProperties(AttributeOwner attributeOwner,
            Map<String, String> renamingMap)
    {
        for (Property property: attributeOwner.getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                renameAndFlattenProperties((DataType)property.getType(), renamingMap);
            }
        }

        // Collect the properties of the class to ensure there are no name clashes with the renamed children.
        List<Property> parentAttributes = attributeOwner.getOwnedAttributes();

        // Get the renamed properties and add them to the owner attributes. The children are not deleted from the data
        // type attributes if the parent is the class, as other instances of the same composite data type need to use
        // the type structure.
        Set<Property> propertiesToAdd = getRenamedProperties(parentAttributes, renamingMap);
        attributeOwner.getOwnedAttributes().addAll(propertiesToAdd);
        return renamingMap;
    }

    /**
     * Finds every (in)direct composite data type attribute and renames them with a flattened name.
     *
     * @param parentAttributes List of properties of the parent composite data type or class.
     * @param renamingMap The map linking the new flattened names to the old dotted names and its property.
     * @return A set of renamed properties.
     */
    private static Set<Property> getRenamedProperties(List<Property> parentAttributes,
            Map<String, String> renamingMap)
    {
        Set<Property> propertiesToAdd = new LinkedHashSet<>();

        // Get the names of the siblings (parent's children) to check for naming clashes. Loop over the siblings and
        // rename their children.
        Set<String> localNames = parentAttributes.stream().map(Property::getName).collect(Collectors.toSet());
        Collection<Property> parentAttributesCopy = EcoreUtil.copyAll(parentAttributes);
        for (Property property: parentAttributesCopy) {
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                Set<Property> renamedProperties = renameChildProperties(property, renamingMap, localNames);
                propertiesToAdd.addAll(renamedProperties);

                // Update local names with the newly created, renamed properties.
                localNames.addAll(propertiesToAdd.stream().map(Property::getName).collect(Collectors.toSet()));
            }
            parentAttributes.remove(property);
        }
        return propertiesToAdd;
    }

    private static Set<Property> renameChildProperties(Property property, Map<String, String> renamingMap,
            Set<String> existingNames)
    {
        List<Property> childProperties = ((DataType)property.getType()).getOwnedAttributes();
        Set<Property> renamedProperties = new LinkedHashSet<>();
        for (Property child: childProperties) {
            // Create a new property with a clash-free name and add it to the renamed properties set.
            String flattenedName = generateNewPropertyName(child.getName(), property.getName(), existingNames);
            Property renamedProperty = copyAndRenameProperty(child, flattenedName);
            renamedProperties.add(renamedProperty);

            // Store only the leaf types.
            if (!PokaYokeTypeUtil.isCompositeDataType(child.getType())) {
                // Find the child name for the "dotted" name part.
                String childName = renamingMap.get(child.getName()) == null ? child.getName()
                        : renamingMap.get(child.getName());

                // Store the pair (old name, property) together with the new name key in the map.
                String dottedName = property.getName() + "." + childName;
                renamingMap.put(flattenedName, dottedName);
            }
        }
        return renamedProperties;
    }

    private static String generateNewPropertyName(String childName, String parentName,
            Collection<String> existingNames)
    {
        String candidateName = parentName + "_" + childName;
        if (existingNames.contains(candidateName)) {
            int count = 0;
            candidateName = parentName + String.valueOf(count) + "_" + childName;
            while (existingNames.contains(candidateName)) {
                count += 1;
                candidateName = parentName + String.valueOf(count) + "_" + childName;
            }
        }
        return candidateName;
    }

    private static Property copyAndRenameProperty(Property originalProperty, String newName) {
        Property rewrittenProperty = EcoreUtil.copy(originalProperty);
        rewrittenProperty.setName(newName);
        return rewrittenProperty;
    }

    // STEP 3 STARTS HERE.

    /**
     * Deletes properties of the model that are composite data types.
     *
     * @param activeClass The main active class.
     * @param dataTypes List of composite data types.
     */
    private static void deleteDataTypes(Class activeClass, List<DataType> dataTypes) {
        // Delete the properties of the active class that are of type DataType.
        // This is needed because we do not delete the properties in the outer most layer.
        for (Property property: new ArrayList<>(activeClass.getOwnedAttributes())) {
            if (dataTypes.contains(property.getType())) {
                activeClass.getOwnedAttributes().remove(property);
            }
        }

        // Delete the composite data types located at the same level of the active class.
        Model model = activeClass.getModel();
        model.getPackagedElements().removeAll(dataTypes);
    }

    // STEP 4 STARTS HERE.

    /**
     * Rewrites the abstract and concrete activities, and the opaque behaviors of the active class with the updated
     * names map.
     *
     * @param clazz The class considered.
     * @param renamingMap The map containing the old and new property names.
     * @throws RuntimeException If an element other than an activity or an opaque behavior is detected.
     */
    private static void rewriteCompositeDataTypeReferences(Class clazz, Map<String, String> renamingMap) {
        // Rewrite owned behaviors and activities.
        for (Behavior classBehavior: clazz.getOwnedBehaviors()) {
            if (classBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                rewriteGuardAndEffects(umlOpaqueBehavior, renamingMap);
            } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                rewriteAbstractActivity(activity, renamingMap);
                // TODO
            } else if (classBehavior instanceof Activity activity && !activity.isAbstract()) {
                rewriteConcreteActivity(activity, renamingMap);
                // TODO
            } else {
                throw new RuntimeException(String.format("Renaming flattened properties of class '%s' not supported.",
                        classBehavior.getClass()));
            }
        }

        // Rewrite constraints.
        rewriteConstraints(clazz.getOwnedRules(), renamingMap);
    }

    private static void rewriteGuardAndEffects(RedefinableElement element, Map<String, String> renamingMap) {
        // Update the guard if it is not null.
        AExpression guardExpr = CifParserHelper.parseGuard(element);
        if (guardExpr != null) {
            AExpression newGuardExpr = rewriteACifExpression(guardExpr, renamingMap);
            String newGuardString = ACifObjectTranslator.toString(newGuardExpr);
            PokaYokeUmlProfileUtil.setGuard(element, newGuardString);
        }

        // Update effects.
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        List<String> newEffects = new LinkedList<>();
        for (String effect: effects) {
            List<AUpdate> updates = CifParserHelper.parseUpdates(effect, element);
            List<String> newUpdateStrings = new LinkedList<>();
            for (AUpdate update: updates) {
                if (update instanceof AAssignmentUpdate assign) {
                    AAssignmentUpdate newUpdate = rewriteACifAssignmentUpdate(assign, renamingMap);
                    String newUpdateString = ACifObjectTranslator.toString(newUpdate);
                    newUpdateStrings.add(newUpdateString);
                } else if (update instanceof AIfUpdate ifUpdate) {
                    // TODO
                    ACifObject newIfUpdate = rewriteACifIfUpdate(ifUpdate, renamingMap);
                    String newIfUpdateString = ACifObjectTranslator.toString(newIfUpdate);
                    newUpdateStrings.add(newIfUpdateString);
                } else {
                    throw new RuntimeException(String.format("Rewriting unsupported update: %s.", update));
                }
            }

            // Join the updates with a comma, and store them into the rewritten effects list.
            String newEffect = String.join(", ", newUpdateStrings);
            newEffects.add(newEffect);
        }
        PokaYokeUmlProfileUtil.setEffects(element, newEffects);
    }

    private static AExpression rewriteACifExpression(AExpression expression, Map<String, String> renamingMap) {
        if (expression instanceof ABinaryExpression binExpr) {
            AExpression rewrittenLeft = rewriteACifExpression(binExpr.left, renamingMap);
            AExpression rewrittenRight = rewriteACifExpression(binExpr.right, renamingMap);

            // Rewrite comparisons of properties with composite data types.
            if (binExpr.left instanceof ANameExpression leftNameExpr
                    && binExpr.right instanceof ANameExpression rightNameExpr)
            {
                ABinaryExpression newExpression = rewriteComparisonExpression(leftNameExpr.name.name,
                        rightNameExpr.name.name, binExpr.operator, binExpr.position, renamingMap);
                return newExpression;
            }

            // Combine the rewritten left and right components to form a new binary expression.
            ABinaryExpression newExpression = new ABinaryExpression(binExpr.operator, rewrittenLeft,
                    rewrittenRight, expression.position);
            return newExpression;
        } else if (expression instanceof AUnaryExpression unaryExpr) {
            return new AUnaryExpression(unaryExpr.operator, rewriteACifExpression(unaryExpr.child, renamingMap),
                    unaryExpr.position);
        } else {
            // Expressions without children don't need rewriting.
            return expression;
        }
    }

    private static ABinaryExpression rewriteComparisonExpression(String lhsName, String rhsName, String operator,
            TextPosition position, Map<String, String> renamingMap)
    {
        // Get flattened names, if present.
        String newLhsName = renamingMap.getOrDefault(lhsName, lhsName);
        String newRhsName = renamingMap.getOrDefault(rhsName, rhsName);
        return createABinaryExpression(newLhsName, newRhsName, operator, position);
    }

    private static AAssignmentUpdate rewriteACifAssignmentUpdate(AAssignmentUpdate assign,
            Map<String, String> renamingMap)
    {
        String newLhsName = ACifObjectTranslator.toString(assign.addressable);
        if (assign.addressable instanceof ANameExpression aNameAddressable) {
            newLhsName = renamingMap.getOrDefault(aNameAddressable.name.name, newLhsName);
        }
        String newRhsName = ACifObjectTranslator.toString(assign.value);
        if (assign.value instanceof ANameExpression aNameValue) {
            newRhsName = renamingMap.getOrDefault(aNameValue.name.name, newRhsName);
        }
        return getNewAssignementUpdate(newLhsName, newRhsName, assign.position);
    }

    private static AUpdate rewriteACifIfUpdate(AIfUpdate ifUpdate, Map<String, String> renamingMap) {
        // Process the if statements. The element of the list represent the conditions separated by a comma.
        List<AExpression> ifStatements = ifUpdate.guards;
        List<AExpression> newIfStatements = new LinkedList<>();
        for (AExpression ifStatement: ifStatements) {
            AExpression newIfStatement = rewriteACifExpression(ifStatement, renamingMap);
            newIfStatements.add(newIfStatement);
        }

        // Process the elif statements. Each element of the list represents a complete elif statement.
        List<AElifUpdate> elifStatements = ifUpdate.elifs;
        List<AElifUpdate> newElifs = new LinkedList<>();
        for (AElifUpdate elifStatement: elifStatements) {
            AElifUpdate newElifStatement = rewriteACifElifUpdate(elifStatement, renamingMap);
            newElifs.add(newElifStatement);
        }

        // Process the else statements as CIF AUpdates.
        List<AUpdate> elseStatements = ifUpdate.elses;
        List<AUpdate> newElses = new LinkedList<>();
        for (AUpdate elseStatement: elseStatements) {
            if (elseStatement instanceof AAssignmentUpdate elseAssignment) {
                AAssignmentUpdate newElseStatement = rewriteACifAssignmentUpdate(elseAssignment, renamingMap);
                newElses.add(newElseStatement);
            } else {
                AUpdate newElseStatement = rewriteACifIfUpdate((AIfUpdate)elseStatement, renamingMap);
                newElses.add(newElseStatement);
            }
        }

        // Process the then statements as CIF AUpdates.
        List<AUpdate> thenStatements = ifUpdate.thens;
        List<AUpdate> newThens = new LinkedList<>();
        for (AUpdate thenStatement: thenStatements) {
            if (thenStatement instanceof AAssignmentUpdate thenAssignment) {
                AAssignmentUpdate newThenStatement = rewriteACifAssignmentUpdate(thenAssignment, renamingMap);
                newThens.add(newThenStatement);
            } else {
                AUpdate newThenStatement = rewriteACifIfUpdate((AIfUpdate)thenStatement, renamingMap);
                newThens.add(newThenStatement);
            }
        }
        return new AIfUpdate(newIfStatements, newThens, newElifs, newElses, ifUpdate.position);
    }

    private static AElifUpdate rewriteACifElifUpdate(AElifUpdate elifUpdate, Map<String, String> renamingMap) {
        // Process the elif guards as CIF AExpressions.
        List<AExpression> elifGuards = elifUpdate.guards;
        List<AExpression> newElifGuards = new LinkedList<>();
        for (AExpression elifGuard: elifGuards) {
            AExpression newElifGuard = rewriteACifExpression(elifGuard, renamingMap);
            newElifGuards.add(newElifGuard);
        }

        // Process the elif thens as CIF AUpdates.
        List<AUpdate> elifThens = elifUpdate.thens;
        List<AUpdate> newElifThens = new LinkedList<>();
        for (AUpdate elifThen: elifThens) {
            if (elifThen instanceof AAssignmentUpdate elifThenAssignment) {
                AAssignmentUpdate newElifThen = rewriteACifAssignmentUpdate(elifThenAssignment, renamingMap);
                newElifThens.add(newElifThen);
            } else {
                AUpdate newElifThen = rewriteACifIfUpdate((AIfUpdate)elifThen, renamingMap);
                newElifThens.add(newElifThen);
            }
        }
        return new AElifUpdate(newElifGuards, newElifThens, elifUpdate.position);
    }

    private static void rewriteAbstractActivity(Activity activity, Map<String, String> renamingMap) {
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        rewriteConstraints(preconditions, renamingMap);
        rewriteConstraints(postconditions, renamingMap);
    }

    private static void rewriteConstraints(List<Constraint> umlConstraints, Map<String, String> renamingMap) {
        for (Constraint constraint: umlConstraints) {
            // Skip occurrence constraints.
            if (constraint instanceof IntervalConstraint) {
                continue;
            } else if (constraint.getSpecification() instanceof OpaqueExpression opaqueSpec) {
                rewriteGuardBodies(opaqueSpec, renamingMap);
            } else {
                throw new RuntimeException(
                        "Constraint specification " + constraint.getSpecification() + " is not an opaque expression.");
            }
        }
    }

    private static void rewriteConcreteActivity(Activity activity, Map<String, String> renamingMap) {
        // Update the flattened names of every control flow, call behavior, and opaque action.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                ValueSpecification guard = controlEdge.getGuard();
                if (guard instanceof OpaqueExpression opaqueGuard) {
                    rewriteGuardBodies(opaqueGuard, renamingMap);
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                // Get guards and respective bodies, and updates them with the flattened names.
                Behavior guard = callBehavior.getBehavior();
                if (guard instanceof OpaqueBehavior opaqueGuard) {
                    rewriteGuardAndEffects(opaqueGuard, renamingMap);
                } else {
                    throw new RuntimeException(
                            String.format("Call behavior of class %s is not supported.", guard.getClass()));
                }
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                rewriteGuardAndEffects(internalAction, renamingMap);
            } else if (ownedElement instanceof ActivityNode) {
                // Nodes in activities have empty names and bodies.
                continue;
            } else {
                throw new RuntimeException(String.format("Renaming flattened properties of class '%s' not supported",
                        ownedElement.getClass()));
            }
        }

        // Rewrite pre and postconditions.
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        rewriteConstraints(preconditions, renamingMap);
        rewriteConstraints(postconditions, renamingMap);
    }

    private static void rewriteGuardBodies(OpaqueExpression constraintSpec, Map<String, String> renamingMap) {
        List<AExpression> constraintBodyExpressions = CifParserHelper.parseBodies(constraintSpec);
        List<String> constraintBodyStrings = constraintSpec.getBodies();

        for (int i = 0; i < constraintBodyExpressions.size(); i++) {
            // Get the current body, rewrite it, and substitute the corresponding string.
            ACifObject currentBody = constraintBodyExpressions.get(i);
            ACifObject newBody;
            if (currentBody instanceof AExpression bodyExpression) {
                newBody = rewriteACifExpression(bodyExpression, renamingMap);
            } else if (currentBody instanceof AInvariant bodyInvariant) {
                newBody = rewriteACifInvariant(bodyInvariant, renamingMap);
            } else {
                throw new RuntimeException("Guard body is not an expression nor an invariant.");
            }
            String newBodyString = ACifObjectTranslator.toString(newBody);
            constraintBodyStrings.set(i, newBodyString);
        }
    }

    private static AInvariant rewriteACifInvariant(AInvariant invariant, Map<String, String> renamingMap) {
        // Rewrite only the invariant predicate.
        return new AInvariant(invariant.name, rewriteACifExpression(invariant.predicate, renamingMap),
                invariant.invKind, invariant.events);
    }
}
