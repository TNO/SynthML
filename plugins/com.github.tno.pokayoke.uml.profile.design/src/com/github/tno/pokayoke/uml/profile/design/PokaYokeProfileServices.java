package com.github.tno.pokayoke.uml.profile.design;

import static com.github.tno.pokayoke.uml.profile.util.GuardEffectsUtil.QN_GUARD_EFFECTS_ACTION;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.exceptions.ParseException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.uml.CifToPythonTranslator;
import com.github.tno.pokayoke.transform.uml.ModelTyping;
import com.github.tno.pokayoke.uml.profile.util.GuardEffectsUtil;

/**
 * The services class used by VSM.
 */
public class PokaYokeProfileServices {
	public static boolean isGuardEffectsAction(Action action) {
		return GuardEffectsUtil.getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION).isPresent();
	}
	
	public static String getGuard(Action action) {
		return GuardEffectsUtil.getGuard(action);
	}

	public static void setGuard(Action action, String newValue) {
		if (newValue == null || newValue.isEmpty()) {
			String effects = getEffects(action);
			if (effects == null || effects.isEmpty()) {
				GuardEffectsUtil.unapplyStereotype(action, QN_GUARD_EFFECTS_ACTION);
				return;
			}
		}
		GuardEffectsUtil.setGuard(action, newValue);
	}

	public boolean isValidGuard(Action action) {
		return getValidGuardErrorMessage(action) == null;
	}

	public String getValidGuardErrorMessage(Action action) {
		try {
			AExpression guardExpr = GuardEffectsUtil.getGuardExpression(action);
			if (guardExpr == null) {
				// Not stereotyped or guard not set, skip validation
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
	
	public static String getEffects(Action action) {
		return GuardEffectsUtil.getEffects(action);
	}

	public static void setEffects(Action action, String newValue) {
		if (newValue == null || newValue.isEmpty()) {
			String guard = getGuard(action);
			if (guard == null || guard.isEmpty()) {
				GuardEffectsUtil.unapplyStereotype(action, QN_GUARD_EFFECTS_ACTION);
				return;
			}
		}
		GuardEffectsUtil.setEffects(action, newValue);
	}

	public boolean isValidEffects(Action action) {
		return getValidEffectsErrorMessage(action) == null;
	}

	public String getValidEffectsErrorMessage(Action action) {
		try {
			List<AUpdate> effectsUpdates = GuardEffectsUtil.getEffectsUpdates(action);
			if (effectsUpdates == null) {
				// Not stereotyped or effects not set, skip validation
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

	public static String getGuard(ControlFlow contolFlow) {
		ValueSpecification guard = contolFlow.getGuard();
		return guard == null ? null : guard.stringValue();
	}

	public static void setGuard(ControlFlow contolFlow, String newValue) {
		if (newValue == null || newValue.isEmpty()) {
			if (contolFlow.getGuard() != null) {
				contolFlow.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
			}
			return;
		} else if ("true".equals(newValue) || "false".equals(newValue)) {
			// Supports resetting the value to the default 'true'
			ValueSpecification guard = contolFlow.getGuard();
			if (!(guard instanceof LiteralBoolean)) {
				guard = UMLFactory.eINSTANCE.createLiteralBoolean();
				contolFlow.setGuard(guard);
			}
			((LiteralBoolean)guard).setValue(Boolean.valueOf(newValue));
			return;
		}
		ValueSpecification guard = contolFlow.getGuard();
		if (!(guard instanceof OpaqueExpression)) {
			guard = UMLFactory.eINSTANCE.createOpaqueExpression();
			contolFlow.setGuard(guard);
		}
		((OpaqueExpression) guard).getBodies().clear();
		((OpaqueExpression) guard).getBodies().add(newValue);
	}
}
