/**
 * 
 */
package com.github.tno.pokayoke.uml.profile.validation;

import static com.google.common.base.Strings.lenientFormat;

import java.util.List;
import java.util.Optional;

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.exceptions.SyntaxException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.ValueSpecification;
import org.espilce.periksa.validation.Check;
import org.espilce.periksa.validation.ContextAwareDeclarativeValidator;

import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifTypeChecker;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Strings;

import PokaYoke.GuardEffectsAction;
import PokaYoke.PokaYokePackage;

public class PokaYokeProfileValidator extends ContextAwareDeclarativeValidator {
	/**
	 * Validates if the {@link Property#getType() property type} is supported and if
	 * the {@link Property#getDefaultValue() property default} is an instance of its
	 * type.
	 * <p>
	 * This validation is only applied if the {@link PokaYokePackage Poka Yoke
	 * profile} is applied.
	 * </p>
	 * 
	 * @param property the property to validate.
	 */
	@Check
	private void checkValidPropertyDefault(Property property) {
		if (PokaYokeUmlProfileUtil.getAppliedProfile(property, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isEmpty()) {
			return;
		}
		Type propType;
		try {
			propType = CifTypeChecker.checkSupportedType(property);
		} catch (RuntimeException re) {
			error("Invalid property: " + re.getLocalizedMessage(), UMLPackage.Literals.TYPED_ELEMENT__TYPE);
			return;
		}
		ValueSpecification propDefault = property.getDefaultValue();
		if (propType == null || propDefault == null) {
			return;
		}
		try {
			AExpression propDefaultExpr = CifParserHelper.parseExpression(propDefault);
			if (propDefaultExpr == null) {
				return;
			}
			Type propDefaultType = CifTypeChecker.checkExpression(property, propDefaultExpr);
			if (!propDefaultType.equals(propType)) {
				error(lenientFormat("Invalid property default: Expected %s but got %s", propType.getLabel(true),
						propDefaultType.getLabel(true)), UMLPackage.Literals.PROPERTY__DEFAULT);
			}
		} catch (SyntaxException se) {
			error("Failed to parse property default: " + se.getLocalizedMessage(),
					UMLPackage.Literals.PROPERTY__DEFAULT);
		} catch (RuntimeException re) {
			error("Invalid property default: " + re.getLocalizedMessage(), null);
		}
	}

	/**
	 * Validates the {@link ControlFlow#getGuard() control-flow guard}.
	 * <p>
	 * This validation is only applied if the {@link PokaYokePackage Poka Yoke
	 * profile} is applied.
	 * </p>
	 * @param controlFlow the control-flow to validate.
	 */
	@Check
	private void checkValidGuard(ControlFlow controlFlow) {
		if (PokaYokeUmlProfileUtil.getAppliedProfile(controlFlow, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isEmpty()) {
			return;
		}
		ValueSpecification guard = controlFlow.getGuard();
		if (guard == null) {
			return;
		}
		try {
			AExpression guardExpr = CifParserHelper.parseExpression(guard);
			if (guardExpr == null) {
				return;
			}
			CifTypeChecker.checkBooleanExpression(controlFlow, guardExpr);
		} catch (SyntaxException se) {
			error("Failed to parse guard: " + se.getLocalizedMessage(), UMLPackage.Literals.ACTIVITY_EDGE__GUARD);
		} catch (RuntimeException re) {
			error("Invalid guard: " + re.getLocalizedMessage(), UMLPackage.Literals.ACTIVITY_EDGE__GUARD);
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
		Optional<Stereotype> stereotype = PokaYokeUmlProfileUtil.getAppliedStereotype(action,
				PokaYokeUmlProfileUtil.GUARD_EFFECTS_ACTION_STEREOTYPE);
		if (stereotype.isPresent()) {
			error(lenientFormat("Stereotype %s can only be applied on call-behavior actions or opaque actions.",
					stereotype.get().getLabel(true)), null);
		}
	}

	/**
	 * Validates the {@link GuardEffectsAction#getGuard()} property if set.
	 * 
	 * @param action the action to validate.
	 */
	@Check
	private void checkValidGuard(Action action) {
		String guard = PokaYokeUmlProfileUtil.getGuard(action);
		if (Strings.isNullOrEmpty(guard)) {
			return;
		}
		try {
			AExpression guardExpr = CifParserHelper.parseExpression(guard, action);
			CifTypeChecker.checkBooleanExpression(action, guardExpr);
		} catch (SyntaxException se) {
			error("Failed to parse guard: " + se.getLocalizedMessage(), null);
		} catch (RuntimeException re) {
			re.printStackTrace();
			error("Invalid guard: " + re.getLocalizedMessage(), null);
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
		if (Strings.isNullOrEmpty(effects)) {
			return;
		}
		try {
			List<AUpdate> effectsUpd = CifParserHelper.parseUpdates(effects, action);
			effectsUpd.forEach(e -> CifTypeChecker.checkUpdate(action, e));
		} catch (SyntaxException se) {
			error("Failed to parse effects: " + se.getLocalizedMessage(), null);
		} catch (RuntimeException re) {
			re.printStackTrace();
			error("Invalid effects: " + re.getLocalizedMessage(), null);
		}
	}
}
