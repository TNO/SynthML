package com.github.tno.pokayoke.uml.profile.design;

import static com.github.tno.pokayoke.uml.profile.util.GuardEffectsUtil.QN_GUARD_EFFECTS_ACTION;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.exceptions.ParseException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.uml.CifToPythonTranslator;
import com.github.tno.pokayoke.transform.uml.ModelTyping;
import com.github.tno.pokayoke.uml.profile.util.GuardEffectsUtil;
import com.google.common.base.Strings;

import PokaYoke.GuardEffectsAction;

/**
 * The services class used by VSM.
 */
public class PokaYokeProfileServices {
	/**
	 * Returns <code>true</code> if {@link GuardEffectsAction} stereotype is applied
	 * on {@link Action action}.
	 * 
	 * @param action the action to interrogate.
	 * @return <code>true</code> if {@link GuardEffectsAction} stereotype is applied
	 * on action.
	 */
	public static boolean isGuardEffectsAction(Action action) {
		return GuardEffectsUtil.getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION).isPresent();
	}
	
	/**
	 * Returns the {@link GuardEffectsAction#getGuard()} property value if
	 * <code>action</code> is stereotype, <code>null</code> otherwise.
	 * 
	 * @param action the action to interrogate.
	 * @return the {@link GuardEffectsAction#getGuard()} property value if
	 * <code>action</code> is stereotype, <code>null</code> otherwise.
	 */
	public static String getGuard(Action action) {
		return GuardEffectsUtil.getGuard(action);
	}

	/**
	 * Applies the {@link GuardEffectsAction} stereotype and set its
	 * {@link GuardEffectsAction#setGuard(String) guard} property for
	 * <code>action</code>.
	 * <p>
	 * The {@link GuardEffectsAction} stereotype is removed if <code>newValue</code>
	 * is <code>null</code> or {@link String#isEmpty() empty} and
	 * {@link #getEffects(Action)} also is <code>null</code> or
	 * {@link String#isEmpty() empty}.
	 * </p>
	 * 
	 * @param action the action to set the property on.
	 * @param newValue the new property value.
	 */
	public static void setGuard(Action action, String newValue) {
		if (Strings.isNullOrEmpty(newValue)) {
			String effects = getEffects(action);
			if (Strings.isNullOrEmpty(effects)) {
				GuardEffectsUtil.unapplyStereotype(action, QN_GUARD_EFFECTS_ACTION);
				return;
			}
		}
		GuardEffectsUtil.setGuard(action, newValue);
	}

	/**
	 * Validates the {@link GuardEffectsAction#getGuard()} property and returns
	 * <code>true</code> if it is a valid CIF expression.
	 * 
	 * @param action the action to interrogate.
	 * @return <code>true</code> if {@link GuardEffectsAction#getGuard()} is a valid
	 *         CIF expression.
	 */
	public boolean isValidGuard(Action action) {
		return getValidGuardErrorMessage(action) == null;
	}

	/**
	 * Validates the {@link GuardEffectsAction#getGuard()} property and returns
	 * <code>null</code> if it is a valid CIF expression, or a non <code>null</code>
	 * string that contains a user message that explains the violation.
	 * 
	 * @param action the action to interrogate.
	 * @return <code>true</code> if {@link GuardEffectsAction#getGuard()} is a valid
	 *         CIF expression.
	 */
	public String getValidGuardErrorMessage(Action action) {
		try {
			AExpression guardExpr = GuardEffectsUtil.getGuardExpression(action);
			if (guardExpr == null) {
				// Not stereotyped or guard not set, skip validation.
				return null;
			}
			CifToPythonTranslator cifToPythonTranslator = new CifToPythonTranslator(new ModelTyping(action.getModel()));
			cifToPythonTranslator.translateExpression(guardExpr);
		} catch (ParseException pe) {
			return "Parsing of \"" + GuardEffectsUtil.getGuard(action) + "\" failed: " + pe.getLocalizedMessage();
		} catch (RuntimeException re) {
			return re.getLocalizedMessage();
		}
		return null;
	}
	
	/**
	 * Returns the {@link GuardEffectsAction#getEffects()} property value if
	 * <code>action</code> is stereotype, <code>null</code> otherwise.
	 * 
	 * @param action the action to interrogate.
	 * @return the {@link GuardEffectsAction#getEffects()} property value if
	 * <code>action</code> is stereotype, <code>null</code> otherwise.
	 */
	public static String getEffects(Action action) {
		return GuardEffectsUtil.getEffects(action);
	}

	/**
	 * Applies the {@link GuardEffectsAction} stereotype and set its
	 * {@link GuardEffectsAction#setEffects(String) effects} property for
	 * <code>action</code>.
	 * <p>
	 * The {@link GuardEffectsAction} stereotype is removed if <code>newValue</code>
	 * is <code>null</code> or {@link String#isEmpty() empty} and
	 * {@link #getEffects(Action)} also is <code>null</code> or
	 * {@link String#isEmpty() empty}.
	 * </p>
	 * 
	 * @param action the action to set the property on.
	 * @param newValue the new property value.
	 */
	public static void setEffects(Action action, String newValue) {
		if (Strings.isNullOrEmpty(newValue)) {
			String effects = getEffects(action);
			if (Strings.isNullOrEmpty(effects)) {
				GuardEffectsUtil.unapplyStereotype(action, QN_GUARD_EFFECTS_ACTION);
				return;
			}
		}
		GuardEffectsUtil.setEffects(action, newValue);
	}

	/**
	 * Validates the {@link GuardEffectsAction#getEffects()} property and returns
	 * <code>true</code> if it is a valid CIF expression.
	 * 
	 * @param action the action to interrogate.
	 * @return <code>true</code> if {@link GuardEffectsAction#getEffects()} is a valid
	 *         CIF expression.
	 */
	public boolean isValidEffects(Action action) {
		return getValidEffectsErrorMessage(action) == null;
	}

	/**
	 * Validates the {@link GuardEffectsAction#getEffects()} property and returns
	 * <code>null</code> if it is a valid CIF expression, or a non <code>null</code>
	 * string that contains a user message that explains the violation.
	 * 
	 * @param action the action to interrogate.
	 * @return <code>true</code> if {@link GuardEffectsAction#getEffects()} is a valid
	 *         CIF expression.
	 */
	public String getValidEffectsErrorMessage(Action action) {
		try {
			List<AUpdate> effectsUpdates = GuardEffectsUtil.getEffectsUpdates(action);
			if (effectsUpdates == null) {
				// Not stereotyped or effects not set, skip validation.
				return null;
			}
			CifToPythonTranslator cifToPythonTranslator = new CifToPythonTranslator(new ModelTyping(action.getModel()));
			for (AUpdate effectsUpdate : effectsUpdates) {
				cifToPythonTranslator.translateUpdate(effectsUpdate);
			}
		} catch (ParseException pe) {
			return "Parsing of \"" + GuardEffectsUtil.getEffects(action) + "\" failed: " + pe.getLocalizedMessage();
		} catch (RuntimeException re) {
			return re.getLocalizedMessage();
		}
		return null;
	}

	/**
	 * Returns the {@link ControlFlow#getGuard()} value as a
	 * {@link ValueSpecification#stringValue() String}.
	 * 
	 * @param controlFlow the control-flow to interrogate.
	 * @return the {@link ControlFlow#getGuard()} value as a
	 *         {@link ValueSpecification#stringValue() String}.
	 * @see GuardEffectsUtil#getGuard(ControlFlow)
	 */
	public static String getGuard(ControlFlow controlFlow) {
		return GuardEffectsUtil.getGuard(controlFlow);
	}

	/**
	 * Set the {@link ControlFlow#getGuard()} value to <code>newValue</code>.
	 * 
	 * @param controlFlow the control-flow to set the guard value on.
	 * @param newValue    the new guard value.
	 * @see GuardEffectsUtil#setGuard(ControlFlow, String)
	 */
	public static void setGuard(ControlFlow controlFlow, String newValue) {
		GuardEffectsUtil.setGuard(controlFlow, newValue);
	}
}
