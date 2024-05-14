
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
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ControlNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.espilce.periksa.validation.Check;
import org.espilce.periksa.validation.ContextAwareDeclarativeValidator;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifTypeChecker;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import PokaYoke.GuardEffectsAction;
import PokaYoke.PokaYokePackage;

public class PokaYokeProfileValidator extends ContextAwareDeclarativeValidator {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][0-9a-zA-Z_]*$");

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
            return from(activity.getNodes()).objectsOfKind(CallBehaviorAction.class).exists(a -> hasCycle(a, history));
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
            if (entry.getKey() == null) {
                for (NamedElement element: entry.getValue()) {
                    error("Required name not set.", element, UMLPackage.Literals.NAMED_ELEMENT__NAME);
                }
            } else if (entry.getValue().size() > 1) {
                for (NamedElement duplicate: entry.getValue()) {
                    error("Name should be unique within model.", duplicate, UMLPackage.Literals.NAMED_ELEMENT__NAME);
                }
            }
        }
    }

    @Check
    private void checkValidClass(Class clazz) {
        if (!isPokaYokaUmlProfileApplied(clazz)) {
            return;
        }
        // Name is checked by #checkGlobalUniqueNames(Model)

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
        if (property.getUpper() != 1) {
            error("Property should be single-valued", UMLPackage.Literals.MULTIPLICITY_ELEMENT__UPPER);
        }
        if (property.getLower() != 1) {
            error("Property should be mandatory", UMLPackage.Literals.MULTIPLICITY_ELEMENT__LOWER);
        }

        checkNamingConventions(property, true, true);

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
            Type propDefaultType = new PropertyDefaultValueTypeChecker(property).checkExpression(propDefaultExpr);
            if (!propDefaultType.equals(propType)) {
                error(String.format("Invalid property default: Expected %s but got %s",
                        PokaYokeTypeUtil.getLabel(propType), PokaYokeTypeUtil.getLabel(propDefaultType)),
                        UMLPackage.Literals.PROPERTY__DEFAULT);
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

        checkNamingConventions(enumeration, true, true);

        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)

        if (!(enumeration.eContainer() instanceof Model)) {
            error("Expected enumeration to be declared in model.", null);
        } else if (enumeration.eContainer().eContainer() != null) {
            error("Expected enumeration to be declared on the outer-most level.", null);
        }
        if (enumeration.getOwnedLiterals().isEmpty()) {
            error("Expected enumeration to have at least one literal.", null);
        }
    }

    @Check
    private void checkValidEnumerationLiteral(EnumerationLiteral literal) {
        checkNamingConventions(literal, true, true);
    }

    @Check
    private void checkValidOpaqueExpression(OpaqueExpression expression) {
        if (!isPokaYokaUmlProfileApplied(expression)) {
            return;
        }
        checkNamingConventions(expression, true, false);
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
        // Name is checked by #checkGlobalUniqueNames(Model)

        if (!activity.getMembers().isEmpty()) {
            error("Expected activity to not have any members.", UMLPackage.Literals.NAMESPACE__MEMBER);
        }
        if (activity.getClassifierBehavior() != null) {
            error("Expected activity to not have a classifier behavior.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
        }
        QueryableIterable<InitialNode> initialNodes = from(activity.getNodes()).objectsOfKind(InitialNode.class);
        if (initialNodes.size() != 1) {
            for (InitialNode node: initialNodes) {
                error("Expected activity to have exactly one initial node.", node, null);
            }
        }
    }

    @Check
    private void checkValidActivityEdge(ActivityEdge edge) {
        if (!isPokaYokaUmlProfileApplied(edge)) {
            return;
        }
        if (edge instanceof ControlFlow controlFlow) {
            checkNamingConventions(edge, true, false);

            if (controlFlow.getSource() instanceof DecisionNode) {
                try {
                    AExpression guardExpr = CifParserHelper.parseExpression(controlFlow.getGuard());
                    if (guardExpr == null) {
                        return;
                    }
                    new CifTypeChecker(controlFlow).checkBooleanExpression(guardExpr);
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
        boolean nameNotSet = Strings.isNullOrEmpty(node.getName());
        if (nameNotSet && node instanceof Action action && PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
            error("Expected a non-null name.", UMLPackage.Literals.NAMED_ELEMENT__NAME);
        }
        checkNamingConventions(node, true, true);
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
        new CifTypeChecker(element).checkBooleanExpression(guardExpr);
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
            new CifTypeChecker(element).checkUpdate(update);

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
        checkNamingConventions(behavior, true, true);

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

    private void checkNamingConventions(NamedElement element, boolean checkDoubleUnderscore,
            boolean checkProperIdentifierName)
    {
        String name = element.getName();
        if (Strings.isNullOrEmpty(name)) {
            return;
        }
        if (checkDoubleUnderscore && name.contains("__")) {
            error("Name cannot not contain '__'", element, UMLPackage.Literals.NAMED_ELEMENT__NAME);
        }
        if (checkProperIdentifierName && !IDENTIFIER_PATTERN.matcher(name).matches()) {
            error("Expected name to start with [a-zA-Z_] and then be followed by [0-9a-zA-Z_]*", element,
                    UMLPackage.Literals.NAMED_ELEMENT__NAME);
        }
    }

    private boolean isPokaYokaUmlProfileApplied(Element element) {
        return PokaYokeUmlProfileUtil.getAppliedProfile(element, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isPresent();
    }
}
