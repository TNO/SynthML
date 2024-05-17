
package com.github.tno.pokayoke.uml.profile.validation;

import static org.eclipse.lsat.common.queries.QueryableIterable.from;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ControlNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.espilce.periksa.validation.Check;
import org.espilce.periksa.validation.ContextAwareDeclarativeValidator;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifTypeChecker;
import com.github.tno.pokayoke.uml.profile.cif.TypeException;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import PokaYoke.GuardEffectsAction;
import PokaYoke.PokaYokePackage;

public class PokaYokeProfileValidator extends ContextAwareDeclarativeValidator {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][0-9a-zA-Z_]*$");

    private enum NamingConvention {
        /**
         * Optional, but when set it should not contain double underscores. Transformations will generate these names if
         * required.
         */
        OPTIONAL,
        /**
         * Name should be set and not contain double underscores.
         */
        MANDATORY,
        /**
         * Name should be set, should not contain double underscores and should match
         * {@link PokaYokeProfileValidator#IDENTIFIER_PATTERN}.
         */
        IDENTIFIER
    }

    /**
     * Reports an error if cycles are found in activities.
     *
     * @param action The action to check
     */
    @Check
    private void checkNoCyclesInActivities(CallBehaviorAction action) {
        if (hasCycle(action, Sets.newHashSet(action.getActivity()))) {
            error("Detected cycle in activities", null);
        }
    }

    private static boolean hasCycle(CallBehaviorAction action, Set<Activity> history) {
        if (action.getBehavior() instanceof Activity activity) {
            if (!history.add(activity)) {
                return true;
            }
            if (from(activity.getNodes()).objectsOfKind(CallBehaviorAction.class).exists(a -> hasCycle(a, history))) {
                return true;
            }
            history.remove(activity);
        }
        return false;
    }

    /**
     * Validates if the names of all {@link CifContext#queryContextElements(Model) context elements} are unique within
     * the {@code model}.
     *
     * @param model The model to validate
     * @see CifContext
     */
    @Check
    private void checkGlobalUniqueNames(Model model) {
        if (!isPokaYokaUmlProfileApplied(model)) {
            return;
        }
        Map<String, List<NamedElement>> contextElements = CifContext.queryContextElements(model)
                .groupBy(NamedElement::getName);
        for (Map.Entry<String, List<NamedElement>> entry: contextElements.entrySet()) {
            // Null or empty strings are reported by #checkNamingConventions(NamedElement, boolean, boolean)
            if (!Strings.isNullOrEmpty(entry.getKey()) && entry.getValue().size() > 1) {
                for (NamedElement duplicate: entry.getValue()) {
                    error("Name should be unique within model.", duplicate, UMLPackage.Literals.NAMED_ELEMENT__NAME);
                }
            }
        }
    }

    @Check
    private void checkValidModel(Model model) {
        if (!isPokaYokaUmlProfileApplied(model)) {
            return;
        }
        checkNamingConventions(model, NamingConvention.MANDATORY);
    }

    @Check
    private void checkValidClass(Class clazz) {
        if (!isPokaYokaUmlProfileApplied(clazz)) {
            return;
        }
        checkNamingConventions(clazz, NamingConvention.IDENTIFIER);

        if (!clazz.getNestedClassifiers().isEmpty()) {
            error("Nested classifiers are not supported.", UMLPackage.Literals.CLASS__NESTED_CLASSIFIER);
        }

        if (clazz instanceof Behavior) {
            // Activities are also a Class in UML. Skip the next validations for all behaviors.
            return;
        }
        if (clazz.getClassifierBehavior() == null) {
            error("Required classifier behavior not set.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
        } else if (!clazz.getOwnedBehaviors().contains(clazz.getClassifierBehavior())) {
            error("Expected class to own its classifier behavior.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__OWNED_BEHAVIOR);
        }
    }

    /**
     * Validates if the {@code property} is a single-valued, mandatory property, that the {@link Property#getType()
     * property type} is supported, and if the {@link Property#getDefaultValue() property default} is an instance of its
     * type.
     * <p>
     * This validation is only applied if the {@link PokaYokePackage Poka Yoke profile} is applied.
     * </p>
     *
     * @param property The property to validate.
     */
    @Check
    private void checkValidProperty(Property property) {
        if (!isPokaYokaUmlProfileApplied(property)) {
            return;
        }
        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(property, NamingConvention.IDENTIFIER);

        if (property.getUpper() != 1) {
            error("Property should be single-valued", UMLPackage.Literals.MULTIPLICITY_ELEMENT__UPPER);
        }
        if (property.getLower() != 1) {
            error("Property should be mandatory", UMLPackage.Literals.MULTIPLICITY_ELEMENT__LOWER);
        }

        Type propType = property.getType();
        if (propType == null) {
            error("Property type not set", UMLPackage.Literals.TYPED_ELEMENT__TYPE);
            return;
        } else if (!PokaYokeTypeUtil.isSupportedType(propType)) {
            error("Unsupported property type: " + propType.getName(), UMLPackage.Literals.TYPED_ELEMENT__TYPE);
            return;
        }

        try {
            AExpression propDefaultExpr = CifParserHelper.parseExpression(property.getDefaultValue());
            if (propDefaultExpr == null) {
                return;
            }
            new PropertyDefaultValueTypeChecker(property).checkAssignment(propType, propDefaultExpr);

            if (PokaYokeTypeUtil.isIntegerType(propType)) {
                // Default value is set and valid, thus can be parsed into an integer
                int propDefaultValue = Integer.parseInt(property.getDefault());
                Integer propMinValue = PokaYokeTypeUtil.getMinValue(propType);
                Integer propMaxValue = PokaYokeTypeUtil.getMaxValue(propType);
                // Only check if type range is valid, also see #checkValidPrimitiveType(PrimitiveType)
                if (propMinValue != null && propMaxValue != null && propMaxValue >= propMinValue
                        && (propDefaultValue < propMinValue || propDefaultValue > propMaxValue))
                {
                    throw new TypeException(String.format("value %d is not within range [%d .. %d]", propDefaultValue,
                            propMinValue, propMaxValue));
                }
            }
        } catch (RuntimeException e) {
            error("Invalid property default: " + e.getLocalizedMessage(), UMLPackage.Literals.PROPERTY__DEFAULT);
        }
    }

    @Check
    private void checkValidEnumeration(Enumeration enumeration) {
        if (!isPokaYokaUmlProfileApplied(enumeration)) {
            return;
        }
        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(enumeration, NamingConvention.IDENTIFIER);

        if (!(enumeration.eContainer() instanceof Model)) {
            error("Expected enumeration to be declared in model.", null);
        } else if (enumeration.eContainer().eContainer() != null) {
            error("Expected enumeration to be declared on the outer-most level.", null);
        }
        if (enumeration.getOwnedLiterals().isEmpty()) {
            error("Expected enumeration to have at least one literal.", null);
        } else {
            // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
            enumeration.getOwnedLiterals().forEach(l -> checkNamingConventions(l, NamingConvention.IDENTIFIER));
        }
    }

    @Check
    private void checkValidPrimitiveType(PrimitiveType primitiveType) {
        if (!isPokaYokaUmlProfileApplied(primitiveType)) {
            return;
        }
        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(primitiveType, NamingConvention.IDENTIFIER);

        if (!(primitiveType.eContainer() instanceof Model)) {
            error("Expected primitive type to be declared in model.", null);
        } else if (primitiveType.eContainer().eContainer() != null) {
            error("Expected primitive type to be declared on the outer-most level.", null);
        }

        if (!PokaYokeTypeUtil.isIntegerType(primitiveType)) {
            error(PokaYokeTypeUtil.getLabel(primitiveType) + " should inherit from primitive Integer type.", null);
            return;
        }

        Constraint minConstraint = PokaYokeTypeUtil.getMinConstraint(primitiveType, false);
        if (minConstraint == null) {
            error("Minimum value constraint not set.", UMLPackage.Literals.NAMESPACE__OWNED_RULE);
            return;
        }
        Constraint maxConstraint = PokaYokeTypeUtil.getMaxConstraint(primitiveType, false);
        if (maxConstraint == null) {
            error("Maximum value constraint not set.", UMLPackage.Literals.NAMESPACE__OWNED_RULE);
            return;
        }

        if (minConstraint.getSpecification() instanceof LiteralInteger minValue
                && maxConstraint.getSpecification() instanceof LiteralInteger maxValue)
        {
            if (minValue.getValue() > maxValue.getValue()) {
                error("Minimum value cannot be greater than maximum value.", minConstraint, null);
            }
        } else {
            // Null values for getSpecification() are reported by default UML validator.
            if (minConstraint.getSpecification() != null) {
                error("Only literal integer is supported for minimum value constraint.", minConstraint, null);
                return;
            }
            if (maxConstraint.getSpecification() != null) {
                error("Only literal integer is supported for maximum value constraint.", maxConstraint, null);
                return;
            }
        }
    }

    @Check
    private void checkValidOpaqueExpression(OpaqueExpression expression) {
        if (!isPokaYokaUmlProfileApplied(expression)) {
            return;
        }
        checkNamingConventions(expression, NamingConvention.OPTIONAL);

        if (expression.getBodies().size() != 1) {
            error("Expected opaque expression to have exactly one expression body.",
                    UMLPackage.Literals.OPAQUE_EXPRESSION__BODY);
        }
    }

    @Check
    private void checkValidActivity(Activity activity) {
        if (!isPokaYokaUmlProfileApplied(activity)) {
            return;
        }
        checkNamingConventions(activity, NamingConvention.MANDATORY);

        if (!activity.getMembers().isEmpty()) {
            error("Expected activity to not have any members.", UMLPackage.Literals.NAMESPACE__MEMBER);
        }
        if (activity.getClassifierBehavior() != null) {
            error("Expected activity to not have a classifier behavior.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
        }
        QueryableIterable<InitialNode> initialNodes = from(activity.getNodes()).objectsOfKind(InitialNode.class);
        if (initialNodes.size() != 1) {
            error("Expected exactly one initial node but got " + initialNodes.size(), activity, null);
        }
    }

    @Check
    private void checkValidActivityEdge(ActivityEdge edge) {
        if (!isPokaYokaUmlProfileApplied(edge)) {
            return;
        }
        if (edge instanceof ControlFlow controlFlow) {
            checkNamingConventions(edge, NamingConvention.OPTIONAL);

            if (controlFlow.getSource() instanceof DecisionNode) {
                try {
                    AExpression guardExpr = CifParserHelper.parseExpression(controlFlow.getGuard());
                    if (guardExpr == null) {
                        return;
                    }
                    new CifTypeChecker(controlFlow).checkBooleanAssignment(guardExpr);
                } catch (RuntimeException e) {
                    error("Invalid guard: " + e.getLocalizedMessage(), UMLPackage.Literals.ACTIVITY_EDGE__GUARD);
                }
            }
        } else {
            error("Unsupported activity edge type: " + edge.eClass().getName(), null);
        }
    }

    @Check
    private void checkValidActivityNode(ActivityNode node) {
        if (!isPokaYokaUmlProfileApplied(node)) {
            return;
        }
        if (!(node instanceof ControlNode || node instanceof CallBehaviorAction || node instanceof OpaqueAction)) {
            error("Unsupported activity node type: " + node.eClass().getName(), null);
            return;
        }

        if (node instanceof Action action && PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
            checkNamingConventions(node, NamingConvention.MANDATORY);
        } else {
            checkNamingConventions(node, NamingConvention.OPTIONAL);
        }
    }

    /**
     * Validates when {@link PokaYokeUmlProfileUtil#isGuardEffectsAction(Action) guards and effects} are set on
     * {@code action}, that its behavioral activity doesn't also have guards and effects specified.
     *
     * @param action The action to validate.
     */
    @Check
    private void checkShadowedGuardEffectsAction(CallBehaviorAction action) {
        if (!isPokaYokaUmlProfileApplied(action)) {
            return;
        }
        Behavior behavior = action.getBehavior();
        if (behavior instanceof Activity subActivity) {
            if (!PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
                // No shadowing
                return;
            }
            if (isGuardEffectsActivity(subActivity, new HashSet<>())) {
                warning("The guard and effects on this call behavior action overrides the guards and effects of its sub-activity",
                        null);
            }
        } else {
            // Added this validation here, as we're already checking the behavior type.
            error("Expected the behavior to be an activity", UMLPackage.Literals.CALL_BEHAVIOR_ACTION__BEHAVIOR);
        }
    }

    private static boolean isGuardEffectsActivity(Activity activity, Set<Activity> history) {
        boolean containsGuardEffectsActions = from(activity.getOwnedNodes()).objectsOfKind(Action.class)
                .exists(PokaYokeUmlProfileUtil::isGuardEffectsAction);
        if (containsGuardEffectsActions) {
            return true;
        } else if (!history.add(activity)) {
            // Cope with cycles
            return false;
        }
        return from(activity.getOwnedNodes()).objectsOfKind(CallBehaviorAction.class)
                .xcollectOne(CallBehaviorAction::getBehavior).objectsOfKind(Activity.class)
                .exists(a -> isGuardEffectsActivity(a, history));
    }

    /**
     * Validates the {@link GuardEffectsAction#getGuard()} property if set.
     *
     * @param action The action to validate.
     */
    @Check
    private void checkValidGuard(Action action) {
        try {
            checkValidGuard(CifParserHelper.parseGuard(action), action);
        } catch (RuntimeException e) {
            error("Invalid guard: " + e.getLocalizedMessage(), null);
        }
    }

    private void checkValidGuard(AExpression guardExpr, Element element) {
        if (guardExpr == null) {
            return;
        }
        new CifTypeChecker(element).checkBooleanAssignment(guardExpr);
    }

    /**
     * Validates the {@link GuardEffectsAction#getEffects()} property if set.
     *
     * @param action The action to validate.
     */
    @Check
    private void checkValidEffects(Action action) {
        try {
            checkValidEffects(CifParserHelper.parseEffects(action), action);
        } catch (RuntimeException e) {
            error("Invalid effects: " + e.getLocalizedMessage(), null);
        }
    }

    private void checkValidEffects(List<AUpdate> updates, Element element) {
        HashSet<String> updatedVariables = new HashSet<>();
        for (AUpdate update: updates) {
            new CifTypeChecker(element).checkAssignment(update);

            // Update is checked above, so ClassCastException is not possible on next lines
            AExpression variableExpr = ((AAssignmentUpdate)update).addressable;
            String variable = ((ANameExpression)variableExpr).name.name;
            if (!updatedVariables.add(variable)) {
                throw new CustomSyntaxException(String.format("variable '%s' is updated multiple times", variable),
                        variableExpr.position);
            }
        }
    }

    /**
     * Validates the name, guard and effects of the given opaque behavior.
     *
     * @param behavior The opaque behavior to validate.
     */
    @Check
    private void checkValidOpaqueBehavior(OpaqueBehavior behavior) {
        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(behavior, NamingConvention.IDENTIFIER);

        // Validates the guard of the given opaque behavior.
        try {
            checkValidGuard(CifParserHelper.parseGuard(behavior), behavior);
        } catch (RuntimeException e) {
            error("Invalid guard: " + e.getLocalizedMessage(), null);
        }

        // Validates the effects of the given opaque behavior.
        try {
            CifParserHelper.parseEffects(behavior).forEach(effect -> checkValidEffects(effect, behavior));
        } catch (RuntimeException e) {
            error("Invalid effects: " + e.getLocalizedMessage(), null);
        }
    }

    @Check
    private void checkValidConstraint(Constraint constraint) {
        checkNamingConventions(constraint, NamingConvention.IDENTIFIER);

        try {
            new CifTypeChecker(constraint).checkInvariant(CifParserHelper.parseInvariant(constraint));
        } catch (RuntimeException e) {
            error("Invalid invariant: " + e.getLocalizedMessage(), null);
        }
    }

    private void checkNamingConventions(NamedElement element, NamingConvention convention) {
        String name = element.getName();
        if (Strings.isNullOrEmpty(name)) {
            if (convention != NamingConvention.OPTIONAL) {
                error("Required name not set.", element, UMLPackage.Literals.NAMED_ELEMENT__NAME);
            }
            return;
        }

        if (name.contains("__")) {
            error("Name cannot not contain '__'", element, UMLPackage.Literals.NAMED_ELEMENT__NAME);
        }

        if (convention == NamingConvention.IDENTIFIER && !IDENTIFIER_PATTERN.matcher(name).matches()) {
            error("Expected name to start with [a-zA-Z_] and then be followed by [0-9a-zA-Z_]*", element,
                    UMLPackage.Literals.NAMED_ELEMENT__NAME);
        }
    }

    private boolean isPokaYokaUmlProfileApplied(Element element) {
        return PokaYokeUmlProfileUtil.getAppliedProfile(element, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isPresent();
    }
}
