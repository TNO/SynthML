
package com.github.tno.pokayoke.uml.profile.validation;

import static org.eclipse.lsat.common.queries.QueryableIterable.from;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
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
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.espilce.periksa.validation.Check;
import org.espilce.periksa.validation.ContextAwareDeclarativeValidator;

import com.github.tno.pokayoke.transform.common.NameHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifTypeChecker;
import com.github.tno.pokayoke.uml.profile.cif.TypeException;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import PokaYoke.FormalElement;
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
     * Validates if the names of all {@link CifContext#queryUniqueNameElements(Model) unique name elements} are unique
     * within the {@code model}.
     *
     * @param model The model to validate
     * @see CifContext
     */
    @Check
    private void checkGlobalUniqueNames(Model model) {
        if (!isPokaYokaUmlProfileApplied(model)) {
            return;
        }
        Map<String, List<NamedElement>> contextElements = CifContext.queryUniqueNameElements(model)
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

        if (clazz.getOwnedRules().stream().anyMatch(IntervalConstraint.class::isInstance)) {
            error("Expected no interval constraints to be defined in classes.", UMLPackage.Literals.NAMESPACE__MEMBER);
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
            if (minValue.getValue() < 0) {
                error("Expected integer type ranges to not include negative values.", minConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }
            if (minValue.getValue() > maxValue.getValue()) {
                error("Minimum value cannot be greater than maximum value.", minConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }
        } else {
            // Null values for getSpecification() are reported by default UML validator.
            if (minConstraint.getSpecification() != null) {
                error("Only literal integer is supported for minimum value constraint.", minConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }
            if (maxConstraint.getSpecification() != null) {
                error("Only literal integer is supported for maximum value constraint.", maxConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
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

        if (activity.getClassifierBehavior() != null) {
            error("Expected activity to not have a classifier behavior.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
        }

        if (activity.isAbstract()) {
            Set<NamedElement> members = new LinkedHashSet<>(activity.getMembers());

            Set<Constraint> preAndPostconditions = new LinkedHashSet<>();
            preAndPostconditions.addAll(activity.getPreconditions());
            preAndPostconditions.addAll(activity.getPostconditions());

            Set<IntervalConstraint> intervalConstraints = activity.getOwnedRules().stream()
                    .filter(IntervalConstraint.class::isInstance).map(IntervalConstraint.class::cast)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (!members.equals(Sets.union(preAndPostconditions, intervalConstraints))) {
                error("Expected abstract activity to contain only precondition, postcondition, and interval constraint members.",
                        UMLPackage.Literals.NAMESPACE__MEMBER);
            }

            if (!activity.getNodes().isEmpty()) {
                error("Expected abstract activity to not have any nodes.", UMLPackage.Literals.ACTIVITY__NODE);
            }

            if (!activity.getEdges().isEmpty()) {
                error("Expected abstract activity to not have any edges.", UMLPackage.Literals.ACTIVITY__EDGE);
            }
        } else {
            if (!activity.getMembers().isEmpty()) {
                error("Expected activity to not have any members.", UMLPackage.Literals.NAMESPACE__MEMBER);
            }

            QueryableIterable<InitialNode> initialNodes = from(activity.getNodes()).objectsOfKind(InitialNode.class);
            if (initialNodes.size() != 1) {
                error("Expected exactly one initial node but got " + initialNodes.size(), activity, null);
            }
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

        if (node instanceof Action action && PokaYokeUmlProfileUtil.isFormalElement(action)) {
            checkNamingConventions(node, NamingConvention.MANDATORY);
        } else {
            checkNamingConventions(node, NamingConvention.OPTIONAL);
        }
    }

    /**
     * Validates that the behavior that is called by {@code action} does not have guards and effects whenever
     * {@code action} itself has {@link PokaYokeUmlProfileUtil#isFormalElement(org.eclipse.uml2.uml.RedefinableElement)
     * guards and effects}.
     *
     * @param action The action to validate.
     */
    @Check
    private void checkShadowedFormalElement(CallBehaviorAction action) {
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
        } else if (behavior instanceof OpaqueBehavior opaqueBehavior) {
            // Shadowing is not allowed in case of call opaque behavior actions, as such behaviors directly have guards
            // and effects.
            if (PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
                error("Cannot override the guards and effects of an opaque behavior in a call opaque behavior action.",
                        UMLPackage.Literals.CALL_BEHAVIOR_ACTION__BEHAVIOR);
            }
        } else {
            // Added this validation here, as we're already checking the behavior type.
            error("Expected the called behavior to be an activity or an opaque behavior",
                    UMLPackage.Literals.CALL_BEHAVIOR_ACTION__BEHAVIOR);
        }
    }

    private static boolean isGuardEffectsActivity(Activity activity, Set<Activity> history) {
        boolean containsGuardEffectsActions = from(activity.getOwnedNodes()).objectsOfKind(Action.class)
                .exists(a -> PokaYokeUmlProfileUtil.isSetGuard(a) || PokaYokeUmlProfileUtil.isSetEffects(a));
        if (containsGuardEffectsActions) {
            return true;
        } else if (!history.add(activity)) {
            // Cope with cycles
            return false;
        }

        return from(activity.getOwnedNodes()).objectsOfKind(CallBehaviorAction.class)
                .xcollectOne(CallBehaviorAction::getBehavior).exists(b -> b instanceof OpaqueBehavior
                        || b instanceof Activity a && isGuardEffectsActivity(a, history));
    }

    /**
     * Validates the {@link FormalElement#getGuard()} property if set.
     *
     * @param element The element to validate.
     */
    @Check
    private void checkValidGuard(RedefinableElement element) {
        try {
            AExpression guardExpr = CifParserHelper.parseGuard(element);
            if (guardExpr == null) {
                return;
            }
            new CifTypeChecker(element).checkBooleanAssignment(guardExpr);
        } catch (RuntimeException e) {
            error("Invalid guard: " + e.getLocalizedMessage(), null);
        }
    }

    /**
     * Validates the {@link FormalElement#getEffects()} property if set.
     *
     * @param element The element to validate.
     */
    @Check
    private void checkValidEffects(RedefinableElement element) {
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        for (int i = 0; i < effects.size(); i++) {
            try {
                checkValidUpdates(CifParserHelper.parseUpdates(effects.get(i), element), element);
            } catch (RuntimeException re) {
                String prefix = "Invalid effects: ";
                if (effects.size() > 1) {
                    prefix = String.format("Invalid effects (%d of %d): ", i + 1, effects.size());
                }
                error(prefix + re.getLocalizedMessage(), null);
            }
        }
    }

    private void checkValidUpdates(List<AUpdate> updates, Element element) {
        // Type check all updates.
        CifTypeChecker typeChecker = new CifTypeChecker(element);

        for (AUpdate update: updates) {
            typeChecker.checkUpdate(update);
        }

        // Ensure that no variable is assigned more than once by the given list of updates.
        checkUniqueAddressables(updates, new LinkedHashSet<>());
    }

    /**
     * Checks that no variable is assigned more than once by the given list of {@code updates}, where {@code addrVars}
     * is the set of variables that are already known to be assigned.
     *
     * @param updates The updates to check.
     * @param addrVars The set of assigned variables, which is modified in-place.
     */
    private void checkUniqueAddressables(List<AUpdate> updates, Set<String> addrVars) {
        for (AUpdate update: updates) {
            checkUniqueAddressables(update, addrVars);
        }
    }

    /**
     * Checks that no variable is assigned more than once by the given {@code update}, where {@code addrVars} is the set
     * of variables that are already known to be assigned.
     *
     * @param update The update to check.
     * @param addrVars The set of assigned variables, which is modified in-place.
     */
    private void checkUniqueAddressables(AUpdate update, Set<String> addrVars) {
        if (update instanceof AAssignmentUpdate assignment) {
            ANameExpression varExpr = (ANameExpression)assignment.addressable;
            String varName = varExpr.name.name;
            boolean added = addrVars.add(varName);

            if (!added) {
                throw new CustomSyntaxException(String.format("Variable '%s' is updated multiple times.", varName),
                        varExpr.position);
            }
        } else if (update instanceof AIfUpdate ifUpdate) {
            Set<String> newAddrVars = new LinkedHashSet<>(addrVars);

            Set<String> addrVarsThens = new LinkedHashSet<>(addrVars);
            checkUniqueAddressables(ifUpdate.thens, addrVarsThens);
            newAddrVars.addAll(addrVarsThens);

            for (AElifUpdate elifUpdate: ifUpdate.elifs) {
                Set<String> addrVarsElifs = new LinkedHashSet<>(addrVars);
                checkUniqueAddressables(elifUpdate.thens, addrVarsElifs);
                newAddrVars.addAll(addrVarsElifs);
            }

            Set<String> addrVarsElses = new LinkedHashSet<>(addrVars);
            checkUniqueAddressables(ifUpdate.elses, addrVarsElses);
            newAddrVars.addAll(addrVarsElses);

            addrVars.addAll(newAddrVars);
        } else {
            error("Unsupported update: " + update, UMLPackage.Literals.TYPED_ELEMENT__TYPE);
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
    }

    @Check
    private void checkValidConstraint(Constraint constraint) {
        checkNamingConventions(constraint, NamingConvention.IDENTIFIER);

        if (CifContext.isActivityPrePostconditionConstraint(constraint)) {
            checkValidActivityPrePostconditionConstraint(constraint);
        } else if (CifContext.isClassConstraint(constraint)) {
            checkValidClassConstraint(constraint);
        } else if (CifContext.isOccurrenceConstraint(constraint)) {
            checkValidOccurrenceConstraint((IntervalConstraint)constraint);
        } else if (CifContext.isPrimitiveTypeConstraint(constraint)) {
            // The constraints for primitive types are validated in #checkValidPrimitiveType(PrimitiveType)
        } else {
            error("Unsupported constraint", UMLPackage.Literals.CONSTRAINT__CONTEXT);
        }
    }

    private void checkValidActivityPrePostconditionConstraint(Constraint constraint) {
        try {
            AInvariant invariant = CifParserHelper.parseInvariant(constraint);
            new CifTypeChecker(constraint).checkInvariant(invariant);

            // Activity preconditions and postconditions are constraints and therefore parsed as invariants.
            // Make sure that they are state invariants.
            if (invariant.invKind != null || invariant.events != null) {
                error("Expected all activity preconditions and postconditions to be state predicates.",
                        UMLPackage.Literals.NAMESPACE__MEMBER);
            }
        } catch (RuntimeException e) {
            error("Invalid invariant: " + e.getLocalizedMessage(), null);
        }
    }

    private void checkValidClassConstraint(Constraint constraint) {
        try {
            new CifTypeChecker(constraint).checkInvariant(CifParserHelper.parseInvariant(constraint));
        } catch (RuntimeException e) {
            error("Invalid invariant: " + e.getLocalizedMessage(), null);
        }
    }

    private void checkValidOccurrenceConstraint(IntervalConstraint constraint) {
        if (constraint.getSpecification() instanceof Interval interval) {
            int min;
            int max;

            if (interval.getMin() instanceof LiteralInteger minLiteral) {
                min = minLiteral.getValue();
            } else {
                error("Invalid interval min value.", UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                return;
            }

            if (interval.getMax() instanceof LiteralInteger maxLiteral) {
                max = maxLiteral.getValue();
            } else {
                error("Invalid interval max value.", UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                return;
            }

            if (min < 0) {
                error("Expected the interval min value to be at least 0.",
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }

            if (min > max) {
                error("Expected the interval min value to not exceed the max value.",
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }

            if (constraint.getConstrainedElements().isEmpty()) {
                error("Expected interval constraints to constrain at least one element.",
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                return;
            }

            CifContext context = new CifContext(constraint);

            for (Element element: constraint.getConstrainedElements()) {
                if (element instanceof OpaqueBehavior || element instanceof Activity) {
                    if (!context.hasElement(element)) {
                        error("Expected the constrained behavior to be in scope.",
                                UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                    }
                } else {
                    error("Expected interval constraints to constrain only opaque behaviors or activities.",
                            UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                }
            }
        } else {
            error("Expected interval constraint specifications to be intervals.",
                    UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
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

    /**
     * Checks if the names that are being used in the given UML model are not reserved keywords in CIF, GAL, or Petrify.
     *
     * @param model The UML model to check.
     */
    @Check
    private void checkReservedKeywords(Model model) {
        QueryableIterable<NamedElement> elements = CifContext.queryContextElements(model);

        for (NamedElement element: elements) {
            // Primitive integer types are bounded between a min and a max value. These automatically generate
            // constraints named 'min' and 'max', which clash with the reserved keywords. Skip the check for these.
            if (element instanceof Constraint constraint && constraint.getContext() instanceof PrimitiveType) {
                continue;
            }
            if (NameHelper.isReservedKeyword(element.getName())) {
                error("Name matching keyword \"" + element.getName() + "\"", element,
                        UMLPackage.Literals.NAMED_ELEMENT__NAME);
            }
        }
    }
}
