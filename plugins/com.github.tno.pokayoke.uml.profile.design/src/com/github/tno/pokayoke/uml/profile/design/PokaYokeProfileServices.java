package com.github.tno.pokayoke.uml.profile.design;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.exceptions.ParseException;
import org.eclipse.uml2.uml.OpaqueAction;

import com.github.tno.pokayoke.transform.uml.CifToPythonTranslator;
import com.github.tno.pokayoke.transform.uml.ModelTyping;
import com.github.tno.pokayoke.uml.profile.util.GuardsEffectsUtil;

/**
 * The services class used by VSM.
 */
public class PokaYokeProfileServices {
	public boolean isValidGuard(OpaqueAction action) {
		return getValidGuardErrorMessage(action) == null;
	}

	public String getValidGuardErrorMessage(OpaqueAction action) {
		try {
			AExpression guardExpr = GuardsEffectsUtil.getGuardExpression(action);
			if (guardExpr == null) {
				// Not stereotyped or guard not set, skip validation
				return null;
			}
			CifToPythonTranslator cifToPythonTranslator = new CifToPythonTranslator(new ModelTyping(action.getModel()));
			cifToPythonTranslator.translateExpression(guardExpr);
		} catch (ParseException pe) {
			return "Parsing of \"" + GuardsEffectsUtil.getGuard(action) + "\" failed: " + pe.getLocalizedMessage();
		} catch (RuntimeException re) {
			return re.getLocalizedMessage();
		}
		return null;
	}
	
	public boolean isValidEffects(OpaqueAction action) {
		return getValidEffectsErrorMessage(action) == null;
	}

	public String getValidEffectsErrorMessage(OpaqueAction action) {
		try {
			List<AUpdate> effectsUpdates = GuardsEffectsUtil.getEffectsUpdates(action);
			if (effectsUpdates == null) {
				// Not stereotyped or effects not set, skip validation
				return null;
			}
			CifToPythonTranslator cifToPythonTranslator = new CifToPythonTranslator(new ModelTyping(action.getModel()));
			for (AUpdate effectsUpdate : effectsUpdates) {
				cifToPythonTranslator.translateUpdate(effectsUpdate);
			}
		} catch (ParseException pe) {
			return "Parsing of \"" + GuardsEffectsUtil.getEffects(action) + "\" failed: " + pe.getLocalizedMessage();
		} catch (RuntimeException re) {
			return re.getLocalizedMessage();
		}
		return null;
	}
}
