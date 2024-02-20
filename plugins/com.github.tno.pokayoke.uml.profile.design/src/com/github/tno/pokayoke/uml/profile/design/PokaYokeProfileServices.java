package com.github.tno.pokayoke.uml.profile.design;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.exceptions.ParseException;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Stereotype;
import org.obeonetwork.dsl.uml2.core.api.services.ReusedDescriptionServices;

import com.github.tno.pokayoke.transform.uml.CifToPythonTranslator;
import com.github.tno.pokayoke.transform.uml.ModelTyping;

import PokaYoke.PokaYokePackage;

/**
 * The services class used by VSM.
 */
public class PokaYokeProfileServices {
	// TODO: Can't we get this as a constant somewhere?!?
	private static final String QN_GUARD_EFFECTS_ACTION = "PokaYoke::GuardEffectsAction";
	private static final String QN_GUARD_EFFECTS_ACTION__GUARD = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__GUARD.getName();
	private static final String QN_GUARD_EFFECTS_ACTION__EFFECTS = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__EFFECTS.getName();

    private final CifUpdateParser updateParser = new CifUpdateParser();

    private final CifExpressionParser expressionParser = new CifExpressionParser();
	
	public String getGuard(OpaqueAction action) {
		return getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__GUARD)).orElse(null);
	}

	public void setGuard(OpaqueAction action, String newValue) {
		Stereotype st = applyStereotype(action, QN_GUARD_EFFECTS_ACTION);
		action.setValue(st, QN_GUARD_EFFECTS_ACTION__GUARD, newValue);
	}

	public boolean isValidGuard(OpaqueAction action) {
		return getValidGuardErrorMessage(action) == null;
	}

	public String getValidGuardErrorMessage(OpaqueAction action) {
		Optional<String> guard = getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__GUARD));
		if (guard.isEmpty() || guard.get().isEmpty()) {
			// Not stereotyped or guard not set, skip validation
			return null;
		}
		try {
			AExpression guardExpr = expressionParser.parseString(guard.get(), action.eResource().getURI().toString());
			CifToPythonTranslator cifToPythonTranslator = new CifToPythonTranslator(new ModelTyping(action.getModel()));
			cifToPythonTranslator.translateExpression(guardExpr);
		} catch (ParseException pe) {
			return "Parsing of \"" + guard.get() + "\" failed: " + pe.getLocalizedMessage();
		} catch (RuntimeException re) {
			return re.getLocalizedMessage();
		}
		return null;
	}
	
	public String getEffects(OpaqueAction action) {
		return getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__EFFECTS)).orElse(null);
	}

	public void setEffects(OpaqueAction action, String newValue) {
		Stereotype st = applyStereotype(action, QN_GUARD_EFFECTS_ACTION);
		action.setValue(st, QN_GUARD_EFFECTS_ACTION__EFFECTS, newValue);
	}
	
	public boolean isValidEffects(OpaqueAction action) {
		return getValidEffectsErrorMessage(action) == null;
	}

	public String getValidEffectsErrorMessage(OpaqueAction action) {
		Optional<String> effects = getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__EFFECTS));
		if (effects.isEmpty() || effects.get().isEmpty()) {
			// Not stereotyped or effects not set, skip validation
			return null;
		}
		try {
			AUpdate effectsUpdate = updateParser.parseString(effects.get(), action.eResource().getURI().toString());
			CifToPythonTranslator cifToPythonTranslator = new CifToPythonTranslator(new ModelTyping(action.getModel()));
			cifToPythonTranslator.translateUpdate(effectsUpdate);
		} catch (ParseException pe) {
			return "Parsing of \"" + effects.get() + "\" failed: " + pe.getLocalizedMessage();
		} catch (RuntimeException re) {
			return re.getLocalizedMessage();
		}
		return null;
	}

	private Optional<Stereotype> getAppliedStereotype(Element element, String qualifiedName) {
		Stereotype stereotype =  element.getAppliedStereotype(qualifiedName);
		return stereotype == null ? Optional.empty() : Optional.of(stereotype);
	}

	private Stereotype applyStereotype(Element element, String qualifiedName) {
		Optional<Stereotype> st = getAppliedStereotype(element, qualifiedName);
		if (st.isPresent()) {
			return st.get();
		}

		ReusedDescriptionServices rds = new ReusedDescriptionServices();
		st = rds.getAllStereotypesAndProfiles(element).stream()
				.filter(sp -> sp instanceof Stereotype
						&& Objects.equals(((Stereotype) sp).getQualifiedName(), qualifiedName))
				.map(Stereotype.class::cast)
				.findAny();
		if (st.isEmpty()) {
			throw new IllegalArgumentException("Stereotype not found: " + qualifiedName);
		}
		
		Stereotype stereotype = st.get();
		org.eclipse.uml2.uml.Package elementPackage = element.getNearestPackage();
		if (!elementPackage.isProfileApplied(stereotype.getProfile())) {
			elementPackage.applyProfile(stereotype.getProfile());
		}
		element.applyStereotype(stereotype);
		return stereotype;
	}
}
