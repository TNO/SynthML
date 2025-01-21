
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
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

/**
 * Flattens all properties that are instantiations of a data type, and deletes all data types. A data type property
 * represents a canonical "object" class, e.g. a robot, may contain only properties and nothing else.
 */
public class DataTypeInliner {
    private DataTypeInliner() {
    }

    /**
     * Finds the instantiations of (nested) data types, and inlines their properties with flattened names. Rewrites the
     * properties and owned behaviors of the active class, and removes the data types.
     *
     * @param model The UML model.
     */
    public static void inlineNestedDataTypes(Model model) {
        CifContext context = new CifContext(model);
        Class activeClass = getSingleActiveClass(context);
        List<DataType> dataTypes = context.getAllDataTypes(d -> PokaYokeTypeUtil.isDataTypeOnlyType(d));

        // Unfold all behaviors' elements that involve a data type assignment or comparison.
        unfoldBehaviors(activeClass, context.getContextMap());

        // Find all properties of the main class that are instances of a data class, recursively rewrites them with a
        // flattened name, and return a map linking to the original reference name.
        Map<String, Pair<String, Property>> orderedFlattenedNames = renameAndFlattenProperties(activeClass);

        // Delete the data types and related properties.
        deleteDataTypes(activeClass, dataTypes);

        // Updates the opaque behaviors, abstract and concrete activities of the active class with the flattened names.
        rewriteOwnedBehaviors(activeClass, orderedFlattenedNames);
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
     * Unfolds any behavior containing data type assignments or comparison with the corresponding leaf properties.
     *
     * @param clazz The active class that contains the behaviors.
     * @param ctx The Cif context.
     */
    private static void unfoldBehaviors(Class clazz, Map<String, NamedElement> ctx) {
        for (Behavior classBehavior: clazz.getOwnedBehaviors()) {
            if (classBehavior instanceof OpaqueBehavior element) {
                unfoldGuardAndEffects(element, ctx);
            } else if (classBehavior instanceof Activity activity && activity.isAbstract()) {
                unfoldAbstractActivity(activity, ctx);
            } else if (classBehavior instanceof Activity activity && !activity.isAbstract()) {
                unfoldConcreteActivity(activity, ctx);
            } else {
                throw new RuntimeException(
                        String.format("Unfolding properties of class '%s' not supported.", classBehavior.getClass()));
            }
        }
    }

    /**
     * Unfolds guards and effects of an opaque behavior.
     *
     * @param element The opaque behavior.
     * @param ctx The Cif context.
     */
    private static void unfoldGuardAndEffects(RedefinableElement element, Map<String, NamedElement> ctx) {
        // Perform the guard unfolding, assuming only guard of class ABinaryExpression are relevant. Skips other classes
        // of guards.
        AExpression guardExpr = CifParserHelper.parseGuard(element);
        if (guardExpr instanceof ABinaryExpression binExpr) {
            AExpression newGuard = unfoldACifExpression(binExpr, ctx);
            String newGuardString = ACifObjectTranslator.toString(newGuard);
            PokaYokeUmlProfileUtil.setGuard(element, newGuardString);
        }

        // Perform the name unfolding for the effects (list of list of updates).
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        List<String> unfoldedEffects = new LinkedList<>();
        for (String effect: effects) {
            List<AUpdate> updates = CifParserHelper.parseUpdates(effect, element);
            List<String> updateStrings = new LinkedList<>();
            for (AUpdate update: updates) {
                if (update instanceof AAssignmentUpdate assignExpr) {
                    List<AAssignmentUpdate> newUpdates = unfoldACifAssignmentExpression(assignExpr, ctx);
                    for (ACifObject unfoldedUpdate: newUpdates) {
                        String unfoldedUpdateString = ACifObjectTranslator.toString(unfoldedUpdate);
                        updateStrings.add(unfoldedUpdateString);
                    }
                } else if (update instanceof AIfUpdate ifUpdateExpr) {
                    ACifObject unfoldedIfUpdate = unfoldACifIfUpdateExpression(ifUpdateExpr, ctx);
                    String unfoldedIfUpdateString = ACifObjectTranslator.toString(unfoldedIfUpdate);
                    updateStrings.add(unfoldedIfUpdateString);
                } else {
                    String unfoldedUpdateString = ACifObjectTranslator.toString(update);
                    updateStrings.add(unfoldedUpdateString);
                }
            }

            // Join the updates with a comma, and store them into the unfolded effects list.
            String unfoldedEffect = String.join(", ", updateStrings);
            unfoldedEffects.add(unfoldedEffect);
        }

        PokaYokeUmlProfileUtil.setEffects(element, unfoldedEffects);
    }

    /**
     * Unfolds a Cif AExpression: substitutes the comparisons between data types with the respective leaf properties.
     *
     * @param expression A Cif AExpression to be unfolded.
     * @param ctx A Map containing the name and the UML NamedElement of every element of the UML model.
     * @return The unfolded Cif AExpression.
     */
    private static AExpression unfoldACifExpression(AExpression expression, Map<String, NamedElement> ctx) {
        // If instance of binary expression, unfold left and right components and perform a recursive call; otherwise,
        // return the expression as is.
        if (expression instanceof ABinaryExpression binExpr) {
            AExpression unfoldedLeft = null;
            AExpression unfoldedRight = null;

            if (binExpr.left instanceof ABinaryExpression binaryLeft) {
                unfoldedLeft = unfoldACifExpression(binaryLeft, ctx);
            }
            if (binExpr.right instanceof ABinaryExpression binaryRight) {
                unfoldedRight = unfoldACifExpression(binaryRight, ctx);
            }

            // If both left and right attributes are ANameExpression, unfold. This assumes that only a ABinaryExpression
            // with ANameExpression as left and right attributes is the relevant one.
            if (binExpr.left instanceof ANameExpression leftNameExpr
                    && binExpr.right instanceof ANameExpression rightNameExpr)
            {
                ABinaryExpression unfoldedExpression = getUnfoldedBinaryExpression(leftNameExpr.name.name,
                        rightNameExpr.name.name, binExpr.operator, binExpr.position, ctx);
                return unfoldedExpression;
            }

            // Combine the unfolded left and right components to form a new binary expression.
            if (unfoldedLeft != null && unfoldedRight != null) {
                ABinaryExpression unfoldedExpression = new ABinaryExpression(binExpr.operator, unfoldedLeft,
                        unfoldedRight, expression.position);
                return unfoldedExpression;
            } else if (unfoldedLeft == null && unfoldedRight != null) {
                ABinaryExpression unfoldedExpression = new ABinaryExpression(binExpr.operator, binExpr.left,
                        unfoldedRight, expression.position);
                return unfoldedExpression;
            } else if (unfoldedRight == null && unfoldedLeft != null) {
                ABinaryExpression unfoldedExpression = new ABinaryExpression(binExpr.operator, unfoldedLeft,
                        binExpr.right, expression.position);
                return unfoldedExpression;
            }
            // If both unfolded expressions are null, neither of them is a binary expression, so there is no unfolding
            // to be done. Return the expression as is.
            return expression;
        } else {
            // If not binary expression, there can not be a hierarchical assignment. Return the expression as is.
            return expression;
        }
    }

    private static ABinaryExpression getUnfoldedBinaryExpression(String lhsName, String rhsName, String operator,
            TextPosition position, Map<String, NamedElement> ctx)
    {
        Property lhsProperty = (Property)ctx.get(lhsName);
        Property rhsProperty;
        try {
            rhsProperty = (Property)ctx.get(rhsName);
        } catch (Exception e) {
            // If right hand side is not a variable, then left hand side is of leaf type. Thus, there is no unfolding to
            // be done.
            return createABinaryExpression(lhsName, rhsName, operator, position);
        }

        // Find all leaves children of left and right hand side data type.
        Set<String> leavesLeft = new LinkedHashSet<>();
        PokaYokeTypeUtil.findAllLeavesProperty(lhsProperty, "", leavesLeft);
        Set<String> leavesRight = new LinkedHashSet<>();
        PokaYokeTypeUtil.findAllLeavesProperty(rhsProperty, "", leavesRight);

        // Sanity check: leaves of the left and right expressions should be the same.
        if (!leavesLeft.equals(leavesRight)) {
            throw new RuntimeException("Trying to compare or assign two different data types.");
        }

        // If the leaves set is empty, the expression refers to a leaf type; there is no unfolding to be done.
        if (leavesLeft.isEmpty()) {
            return createABinaryExpression(lhsName, rhsName, operator, position);
        }

        // Create the new binary expression of the unfolded properties for both left and right hand side.
        ABinaryExpression unfoldedBinaryExpression = null;
        for (String leaf: leavesLeft) {
            ABinaryExpression currentBinaryExpression = createABinaryExpression(lhsName + leaf, rhsName + leaf,
                    operator, position);
            if (unfoldedBinaryExpression == null) {
                unfoldedBinaryExpression = currentBinaryExpression;
            } else {
                unfoldedBinaryExpression = new ABinaryExpression("and", unfoldedBinaryExpression,
                        currentBinaryExpression, position);
            }
        }

        return unfoldedBinaryExpression;
    }

    private static ABinaryExpression createABinaryExpression(String lhsName, String rhsName, String operator,
            TextPosition position)
    {
        ANameExpression leftExpression = new ANameExpression(new AName(lhsName, position), false, position);
        ANameExpression rightExpression = new ANameExpression(new AName(rhsName, position), false, position);
        return new ABinaryExpression(operator, leftExpression, rightExpression, position);
    }

    private static List<AAssignmentUpdate> unfoldACifAssignmentExpression(AAssignmentUpdate assignExpr,
            Map<String, NamedElement> ctx)
    {
        if (assignExpr.addressable instanceof ANameExpression aNameAddressable
                && assignExpr.value instanceof ANameExpression aNameValue)
        {
            String lhsName = aNameAddressable.name.name;
            String rhsName = aNameValue.name.name;
            List<AAssignmentUpdate> unfoldedAssignment = getUnfoldedAssignmentExpression(lhsName, rhsName,
                    assignExpr.position, ctx);
            return unfoldedAssignment;
        }
        // If 'addressable' and 'value' are not both ANameExpression, skip the unfolding and return the expression.
        return new LinkedList<>(List.of(assignExpr));
    }

    private static List<AAssignmentUpdate> getUnfoldedAssignmentExpression(String lhsName, String rhsName,
            TextPosition position, Map<String, NamedElement> ctx)
    {
        Property lhsProperty = (Property)ctx.get(lhsName);
        Property rhsProperty;
        try {
            rhsProperty = (Property)ctx.get(rhsName);
        } catch (Exception e) {
            // If right hand side is not a variable, then it is a leaf type. Thus, there is no unfolding to be done.
            return new LinkedList<>(List.of(getNewAssignementUpdate(lhsName, rhsName, position)));
        }

        // Find all leaves children of left and right hand side data type.
        Set<String> leavesLeft = new LinkedHashSet<>();
        PokaYokeTypeUtil.findAllLeavesProperty(lhsProperty, "", leavesLeft);
        Set<String> leavesRight = new LinkedHashSet<>();
        PokaYokeTypeUtil.findAllLeavesProperty(rhsProperty, "", leavesRight);

        // Sanity check: leaves of the left and right hand sides should be the same.
        if (!leavesLeft.equals(leavesRight)) {
            throw new RuntimeException(String
                    .format("Trying to compare or assign two different data types: '%s' and '%s'.", lhsName, rhsName));
        }

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

    private static void unfoldAbstractActivity(Activity activity, Map<String, NamedElement> ctx) {
        List<Constraint> preconditions = activity.getPreconditions();
        List<Constraint> postconditions = activity.getPostconditions();

        unfoldPrePostConditions(preconditions, ctx);
        unfoldPrePostConditions(postconditions, ctx);
    }

    private static void unfoldPrePostConditions(List<Constraint> umlConstraints, Map<String, NamedElement> ctx) {
        for (Constraint constraint: umlConstraints) {
            if (constraint.getSpecification() instanceof OpaqueExpression opaqueSpec) {
                unfoldGuardBodies(opaqueSpec, ctx);
            } else {
                throw new RuntimeException(
                        "Constraint specification " + constraint.getSpecification() + " is not an opaque expression.");
            }
        }
    }

    private static void unfoldGuardBodies(OpaqueExpression constraintSpec, Map<String, NamedElement> ctx) {
        List<AExpression> constraintBodyExpressions = CifParserHelper.parseBodies(constraintSpec);
        EList<String> constraintBodyStrings = constraintSpec.getBodies();

        for (int i = 0; i < constraintBodyExpressions.size(); i++) {
            // Get the current body, unfold it, and substitute the corresponding string.
            AExpression currentBody = constraintBodyExpressions.get(i);
            AExpression unfoldedBody = unfoldACifExpression(currentBody, ctx);
            String newBodyString = ACifObjectTranslator.toString(unfoldedBody);
            constraintBodyStrings.set(i, newBodyString);
        }
    }

    private static void unfoldConcreteActivity(Activity activity, Map<String, NamedElement> ctx) {
        // Unfold the behaviors of every control flow, call behavior, and opaque action.
        for (Element ownedElement: activity.getOwnedElements()) {
            if (ownedElement instanceof ControlFlow controlEdge) {
                ValueSpecification guard = controlEdge.getGuard();
                if (guard instanceof OpaqueExpression opaqueGuard) {
                    unfoldGuardBodies(opaqueGuard, ctx);
                }
            } else if (ownedElement instanceof CallBehaviorAction callBehavior) {
                Behavior guard = callBehavior.getBehavior();
                if (guard instanceof OpaqueBehavior opaqueGuard) {
                    unfoldGuardBodies(null, ctx);
                } else {
                    throw new RuntimeException(
                            String.format("Call behavior of class %s is not supported.", guard.getClass()));
                }
            } else if (ownedElement instanceof OpaqueAction internalAction) {
                unfoldGuardAndEffects(internalAction, ctx);
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

        unfoldPrePostConditions(preconditions, ctx);
        unfoldPrePostConditions(postconditions, ctx);
    }

    private static AUpdate unfoldACifIfUpdateExpression(AIfUpdate ifUpdateExpr, Map<String, NamedElement> ctx) {
        // Process the if statements. The element of the list represent the conditions separated by a comma.
        List<AExpression> ifStatements = ifUpdateExpr.guards;
        List<AExpression> unfoldedIfStatements = new LinkedList<>();
        for (AExpression ifStatement: ifStatements) {
            AExpression unfoldedIfStatement = unfoldACifExpression(ifStatement, ctx);
            unfoldedIfStatements.add(unfoldedIfStatement);
        }

        // Process the elif statements. Each element of the list represents a complete elif statement.
        List<AElifUpdate> elifStatements = ifUpdateExpr.elifs;
        List<AElifUpdate> unfoldedElifs = new LinkedList<>();
        for (AElifUpdate elifStatement: elifStatements) {
            AElifUpdate unfoldedElifStatement = unfoldACifElifUpdateExpression(elifStatement, ctx);
            unfoldedElifs.add(unfoldedElifStatement);
        }

        // Process the else statements as Cif AUpdates. Each element of the list represents a different assignment,
        // syntactically separated by a comma.
        List<AUpdate> elseStatements = ifUpdateExpr.elses;
        List<AUpdate> unfoldedElses = new LinkedList<>();
        for (AUpdate elseStatement: elseStatements) {
            if (elseStatement instanceof AAssignmentUpdate elseAssignment) {
                List<AAssignmentUpdate> unfoldedElseStatement = unfoldACifAssignmentExpression(elseAssignment, ctx);
                unfoldedElses.addAll(unfoldedElseStatement);
            } else {
                AUpdate unfoldedElseStatement = unfoldACifIfUpdateExpression((AIfUpdate)elseStatement, ctx);
                unfoldedElses.add(unfoldedElseStatement);
            }
        }

        // Process the then statements as Cif AUpdates. Each element of the list represents a different assignment,
        // syntactically separated by a comma.
        List<AUpdate> thenStatements = ifUpdateExpr.thens;
        List<AUpdate> unfoldedThens = new LinkedList<>();
        for (AUpdate thenStatement: thenStatements) {
            if (thenStatement instanceof AAssignmentUpdate thenAssignment) {
                List<AAssignmentUpdate> unfoldedThenStatement = unfoldACifAssignmentExpression(thenAssignment, ctx);
                unfoldedThens.addAll(unfoldedThenStatement);
            } else {
                AUpdate unfoldedThenStatement = unfoldACifIfUpdateExpression((AIfUpdate)thenStatement, ctx);
                unfoldedThens.add(unfoldedThenStatement);
            }
        }
        return new AIfUpdate(unfoldedIfStatements, unfoldedThens, unfoldedElifs, unfoldedElses, ifUpdateExpr.position);
    }

    private static AElifUpdate unfoldACifElifUpdateExpression(AElifUpdate elifUpdateExpr,
            Map<String, NamedElement> ctx)
    {
        // Process the elif guards as Cif AExpressions.
        List<AExpression> elifGuards = elifUpdateExpr.guards;
        List<AExpression> unfoldedElifGuards = new LinkedList<>();
        for (AExpression elifGuard: elifGuards) {
            AExpression unfoldedElifGuard = unfoldACifExpression(elifGuard, ctx);
            unfoldedElifGuards.add(unfoldedElifGuard);
        }

        // Process the elif thens as Cif AUpdates.
        List<AUpdate> elifThens = elifUpdateExpr.thens;
        List<AUpdate> unfoldedElifThens = new LinkedList<>();
        for (AUpdate elifThen: elifThens) {
            if (elifThen instanceof AAssignmentUpdate elifThenAssignment) {
                List<AAssignmentUpdate> unfoldedElifThen = unfoldACifAssignmentExpression(elifThenAssignment, ctx);
                unfoldedElifThens.addAll(unfoldedElifThen);
            } else {
                AUpdate unfoldedElifThen = unfoldACifIfUpdateExpression((AIfUpdate)elifThen, ctx);
                unfoldedElifThens.add(unfoldedElifThen);
            }
        }
        return new AElifUpdate(unfoldedElifGuards, unfoldedElifThens, elifUpdateExpr.position);
    }

    /**
     * Recursively computes a map of the nested properties, where the keys are the new flattened names, and the values
     * are the old dotted names. The map is ordered by key length, so that the name substitution avoids incomplete
     * replacing.
     *
     * @param activeClass The main active class of the UML model.
     * @return A map containing the old and new names, along with the property they refer to, with items sorted by key
     *     length in descending order.
     */
    private static Map<String, Pair<String, Property>> renameAndFlattenProperties(Class activeClass) {
        Map<String, Pair<String, Property>> renamingMap = new LinkedHashMap<>();

        // Add the names of the properties defined at the model level.
        for (Property property: activeClass.getOwnedAttributes()) {
            // If property is of leaf type, add it to the renaming map.
            if (PokaYokeTypeUtil.isSupportedType(property.getType())
                    && !PokaYokeTypeUtil.isDataTypeOnlyType(property.getType()))
            {
                renamingMap.put(property.getName(), Pair.of(property.getName(), property));
            }
        }

        flattenProperties(activeClass, renamingMap);

        return orderMapByKeyLength(renamingMap);
    }

    private static Map<String, Pair<String, Property>> flattenProperties(Class clazz,
            Map<String, Pair<String, Property>> renamingMap)
    {
        for (Property property: clazz.getOwnedAttributes()) {
            flattenProperties(property, renamingMap);
        }

        // Collect the properties of the class to ensure there are no name clashes with the renamed children.
        List<Property> parentAttributes = clazz.getOwnedAttributes();

        // Get the renamed properties and add them to the class attributes. The children are not deleted from the data
        // type attributes, as other instances of the same data type need to use the data type structure.
        boolean deleteChildren = false;
        Set<Property> propertiesToAdd = getRenamedProperties(parentAttributes, renamingMap, deleteChildren);
        clazz.getOwnedAttributes().addAll(propertiesToAdd);
        return renamingMap;
    }

    private static void flattenProperties(Property parentProperty, Map<String, Pair<String, Property>> renamingMap) {
        for (Property property: ((DataType)parentProperty.getType()).getOwnedAttributes()) {
            if (PokaYokeTypeUtil.isDataTypeOnlyType(property.getType())) {
                flattenProperties(property, renamingMap);
            }
        }

        // Collect the properties of the class to ensure there are no name clashes with the renamed children.
        List<Property> parentAttributes = ((DataType)parentProperty.getType()).getOwnedAttributes();

        // Get the renamed properties and add them to the class attributes. The children are deleted from the data
        // type attributes, as other instances of the same data type use the structure established at the top level.
        boolean deleteChildren = true;
        Set<Property> propertiesToAdd = getRenamedProperties(parentAttributes, renamingMap, deleteChildren);
        ((DataType)parentProperty.getType()).getOwnedAttributes().addAll(propertiesToAdd);
    }

    /**
     * Recursively finds every (in)direct data type attribute and renames them with a flattened name.
     *
     * @param parentAttributes List of properties of the parent data type or class.
     * @param renamingMap The map linking the new flattened names to the old dotted names.
     * @param deleteChildren If {@code true} removes the children properties from the dependency tree.
     * @return A set of renamed properties.
     */
    private static Set<Property> getRenamedProperties(List<Property> parentAttributes,
            Map<String, Pair<String, Property>> renamingMap, boolean deleteChildren)
    {
        Set<Property> propertiesToAdd = new LinkedHashSet<>();

        // Get the names of the siblings (parent's children) to check for naming clashes. Loop over the siblings and
        // rename their children.
        Set<String> localNames = parentAttributes.stream().map(Property::getName).collect(Collectors.toSet());
        for (Property property: parentAttributes) {
            if (PokaYokeTypeUtil.isDataTypeOnlyType(property.getType())) {
                Set<Property> renamedProperties = renameChildProperties(property, renamingMap, localNames,
                        deleteChildren);
                propertiesToAdd.addAll(renamedProperties);

                // Update local names with the newly created, renamed properties.
                localNames.addAll(propertiesToAdd.stream().map(Property::getName).collect(Collectors.toSet()));
            }
        }
        return propertiesToAdd;
    }

    private static Set<Property> renameChildProperties(Property property,
            Map<String, Pair<String, Property>> renamingMap, Set<String> existingNames, boolean deleteChildren)
    {
        List<Property> childProperties = ((DataType)property.getType()).getOwnedAttributes();
        Set<Property> renamedProperties = new LinkedHashSet<>();
        for (Property child: childProperties) {
            // Find a new name ensuring it does not clash with the existing names, create a new property with the
            // flattened name, and add it to the renamed properties set.
            String flattenedName = findNewPropertyName(child.getName(), property.getName(), existingNames);
            Property renamedProperty = renameProperty(child, flattenedName);
            renamedProperties.add(renamedProperty);

            // Find the child name for the "dotted" name part. If the child has already been renamed, i.e. it is not a
            // leaf, find its name in the renaming map; otherwise, get its actual name.
            Pair<String, Property> childNameAndProperty = renamingMap.get(child.getName());
            String childName;
            if (childNameAndProperty == null) {
                childName = child.getName();
            } else {
                childName = childNameAndProperty.getLeft();
            }

            // Store the pair (old name, property) together with the new name key in the map.
            String dottedName = property.getName() + "." + childName;
            renamingMap.put(flattenedName, Pair.of(dottedName, child));
        }
        if (deleteChildren) {
            ((DataType)property.getType()).getOwnedAttributes().removeAll(childProperties);
        }
        return renamedProperties;
    }

    private static String findNewPropertyName(String childName, String parentName, Collection<String> existingNames) {
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

    private static Map<String, Pair<String, Property>>
            orderMapByKeyLength(Map<String, Pair<String, Property>> renamingMap)
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
        orderedMap.putAll(renamingMap);
        return orderedMap;
    }

    private static Property renameProperty(Property originalProperty, String newName) {
        Property rewrittenProperty = EcoreUtil.copy(originalProperty);
        rewrittenProperty.setName(newName);
        return rewrittenProperty;
    }

    /**
     * Deletes properties of the model that are data types.
     *
     * @param activeClass The main active class.
     * @param dataTypes List of data types.
     */
    private static void deleteDataTypes(Class activeClass, List<DataType> dataTypes) {
        // Delete the nested classifiers of the active class that are of type DataType.
        for (Classifier classifier: new ArrayList<>(activeClass.getNestedClassifiers())) {
            if (dataTypes.contains(classifier)) {
                activeClass.getNestedClassifiers().remove(classifier);
            }
        }

        // Delete the properties of the active class that are of type DataType.
        for (Property property: new ArrayList<>(activeClass.getOwnedAttributes())) {
            if (dataTypes.contains(property.getType())) {
                activeClass.getOwnedAttributes().remove(property);
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
                Pair<String, Property> oldNameAndProperty = entry.getValue();
                String oldName = oldNameAndProperty.getLeft();

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
            if (constraint.getSpecification() instanceof OpaqueExpression opaqueSpec) {
                EList<String> constraintBodies = opaqueSpec.getBodies();
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
