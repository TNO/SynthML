/**
 * 
 */
package com.github.tno.pokayoke.uml.profile.validation;

import static org.eclipse.lsat.common.queries.QueryableIterable.from;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.espilce.periksa.validation.Check;
import org.espilce.periksa.validation.ContextAwareDeclarativeValidator;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifTypeChecker;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;

import PokaYoke.GuardEffectsAction;
import PokaYoke.PokaYokePackage;

public class PokaYokeProfileValidator extends ContextAwareDeclarativeValidator {
	/**
	 * Validates if the names of all {@link CifContext#queryContextElements(Model)
	 * context elements} are unique within the {@code model}.
	 * 
	 * @param model the model to validate
	 * @see CifContext
	 */
	@Check
	private void checkGlobalUniqueNames(Model model) {
		if (PokaYokeUmlProfileUtil.getAppliedProfile(model, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isEmpty()) {
			return;
		}
		Map<String, List<NamedElement>> contextElements = CifContext.queryContextElements(model)
				.groupBy(NamedElement::getName);
		for (Map.Entry<String, List<NamedElement>> entry : contextElements.entrySet()) {
			if (entry.getValue().size() <= 1) {
				continue;
			}
			for (NamedElement duplicate : entry.getValue()) {
				error("Name should be unique within model", duplicate, UMLPackage.Literals.NAMED_ELEMENT__NAME);
			}
		}
	}

	/**
	 * Validates if the {@code propery} is a single-valued, mandatory property, that
	 * the {@link Property#getType() property type} is supported, and if the
	 * {@link Property#getDefaultValue() property default} is an instance of its
	 * type.
	 * <p>
	 * This validation is only applied if the {@link PokaYokePackage Poka Yoke
	 * profile} is applied.
	 * </p>
	 * 
	 * @param property the property to validate.
	 */
	@Check
	private void checkValidProperty(Property property) {
		if (PokaYokeUmlProfileUtil.getAppliedProfile(property, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isEmpty()) {
			return;
		}
		if (property.getUpper() != 1) {
			error("Property should be single-valued", UMLPackage.Literals.MULTIPLICITY_ELEMENT__UPPER);
		}
		if (property.getLower() != 1) {
			error("Property should be mandatory", UMLPackage.Literals.MULTIPLICITY_ELEMENT__LOWER);
		}
		Type propType;
		try {
			propType = CifTypeChecker.checkSupportedType(property);
		} catch (RuntimeException e) {
			error("Invalid property: " + e.getLocalizedMessage(), UMLPackage.Literals.TYPED_ELEMENT__TYPE);
			return;
		}

		try {
			AExpression propDefaultExpr = CifParserHelper.parseExpression(property.getDefaultValue());
			if (propDefaultExpr == null) {
				return;
			}
			Type propDefaultType = CifTypeChecker.checkExpression(property, propDefaultExpr);
			if (!propDefaultType.equals(propType)) {
				error(String.format("Invalid property default: Expected %s but got %s", propType.getLabel(true),
						propDefaultType.getLabel(true)), UMLPackage.Literals.PROPERTY__DEFAULT);
			}
		} catch (RuntimeException e) {
			error("Invalid property default: " + e.getLocalizedMessage(), UMLPackage.Literals.PROPERTY__DEFAULT);
		}
	}

	/**
	 * Validates the {@link ControlFlow#getGuard() control-flow guard}.
	 * <p>
	 * This validation is only applied if the {@link PokaYokePackage Poka Yoke
	 * profile} is applied.
	 * </p>
	 * 
	 * @param controlFlow the control-flow to validate.
	 */
	@Check
	private void checkValidGuard(ControlFlow controlFlow) {
		if (!(controlFlow.getSource() instanceof DecisionNode)) {
			return;
		}
		if (PokaYokeUmlProfileUtil.getAppliedProfile(controlFlow, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isEmpty()) {
			return;
		}
		try {
			AExpression guardExpr = CifParserHelper.parseExpression(controlFlow.getGuard());
			if (guardExpr == null) {
				return;
			}
			CifTypeChecker.checkBooleanExpression(controlFlow, guardExpr);
		} catch (RuntimeException e) {
			error("Invalid guard: " + e.getLocalizedMessage(), UMLPackage.Literals.ACTIVITY_EDGE__GUARD);
		}
	}

	/**
	 * Validates if {@link GuardEffectsAction} stereotype is only applied to
	 * {@link CallBehaviorAction} or {@link OpaqueAction}.
	 * 
	 * @param action the action to validate.
	 */
	@Check
	private void checkValidStereoType(Action action) {
		if (action instanceof CallBehaviorAction || action instanceof OpaqueAction) {
			return;
		}
		if (PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
			error(String.format("Stereotype %s can only be applied on call-behavior actions or opaque actions.",
					PokaYokeUmlProfileUtil.GUARD_EFFECTS_ACTION_STEREOTYPE), null);
		}
	}

	@Check
	private void checkShadowedGuardEffectsAction(CallBehaviorAction action) {
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
	 * @param action the action to validate.
	 */
	@Check
	private void checkValidGuard(Action action) {
		String guard = PokaYokeUmlProfileUtil.getGuard(action);
		try {
			AExpression guardExpr = CifParserHelper.parseExpression(guard, action);
			if (guardExpr == null) {
				return;
			}
			CifTypeChecker.checkBooleanExpression(action, guardExpr);
		} catch (RuntimeException e) {
			error("Invalid guard: " + e.getLocalizedMessage(), null);
		}
	}

	/**
	 * Validates the {@link GuardEffectsAction#getEffects()} property if set.
	 * 
	 * @param action the action to validate.
	 */
	@Check
	private void checkValidEffects(Action action) {
		String effects = PokaYokeUmlProfileUtil.getEffects(action);
		try {
			HashSet<String> updatedVariables = new HashSet<>();
			for (AUpdate update : CifParserHelper.parseUpdates(effects, action)) {
				CifTypeChecker.checkUpdate(action, update);

				// Update is checked above, so ClassCastException is not possible on next lines
				AExpression variableExpr = ((AAssignmentUpdate) update).addressable;
				String variable = ((ANameExpression) variableExpr).name.name;
				if (!updatedVariables.add(variable)) {
					throw new CustomSyntaxException(String.format("variable '%s' is updated multiple times", variable),
							variableExpr.position);
				}
			}
		} catch (RuntimeException e) {
			error("Invalid effects: " + e.getLocalizedMessage(), null);
		}
	}
}
