/**
 * 
 */
package com.github.tno.pokayoke.uml.profile.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdatesParser;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.exceptions.ParseException;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Stereotype;
import org.obeonetwork.dsl.uml2.core.api.services.ReusedDescriptionServices;

import PokaYoke.PokaYokePackage;

/**
 * 
 */
public class GuardEffectsUtil {
	public static final String QN_GUARD_EFFECTS_ACTION = "PokaYoke::GuardEffectsAction";
	private static final String QN_GUARD_EFFECTS_ACTION__GUARD = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__GUARD.getName();
	private static final String QN_GUARD_EFFECTS_ACTION__EFFECTS = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__EFFECTS.getName();

	public static String getGuard(Action action) {
		return getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__GUARD)).orElse(null);
	}

	public static AExpression getGuardExpression(Action action) throws ParseException {
		Optional<String> guard = getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__GUARD));
		if (guard.isEmpty() || guard.get().isEmpty()) {
			// Not stereotyped or guard not set, return null.
			return null;
		}
		CifExpressionParser expressionParser = new CifExpressionParser();
		return expressionParser.parseString(guard.get(), action.eResource().getURI().toString());
	}

	public static void setGuard(Action action, String newValue) {
		Stereotype st = applyStereotype(action, QN_GUARD_EFFECTS_ACTION);
		action.setValue(st, QN_GUARD_EFFECTS_ACTION__GUARD, newValue);
	}

	public static String getEffects(Action action) {
		return getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__EFFECTS)).orElse(null);
	}

	public static List<AUpdate> getEffectsUpdates(Action action) throws ParseException {
		Optional<String> effects = getAppliedStereotype(action, QN_GUARD_EFFECTS_ACTION)
				.map(st -> (String) action.getValue(st, QN_GUARD_EFFECTS_ACTION__EFFECTS));
		if (effects.isEmpty() || effects.get().isEmpty()) {
			// Not stereotyped or effects not set, skip validation.
			return null;
		}
		CifUpdatesParser updatesParser = new CifUpdatesParser();
		return updatesParser.parseString(effects.get(), action.eResource().getURI().toString());
	}

	public static void setEffects(Action action, String newValue) {
		Stereotype st = applyStereotype(action, QN_GUARD_EFFECTS_ACTION);
		action.setValue(st, QN_GUARD_EFFECTS_ACTION__EFFECTS, newValue);
	}

	public static Optional<Stereotype> getAppliedStereotype(Element element, String qualifiedName) {
		Stereotype stereotype = element.getAppliedStereotype(qualifiedName);
		return stereotype == null ? Optional.empty() : Optional.of(stereotype);
	}

	public static Stereotype applyStereotype(Element element, String qualifiedName) {
		Optional<Stereotype> st = getAppliedStereotype(element, qualifiedName);
		if (st.isPresent()) {
			return st.get();
		}

		// TODO: We may need to embed this code to avoid dependency on UML designer plugin
		ReusedDescriptionServices rds = new ReusedDescriptionServices();
		st = rds.getAllStereotypesAndProfiles(element).stream().filter(
				sp -> sp instanceof Stereotype && Objects.equals(((Stereotype) sp).getQualifiedName(), qualifiedName))
				.map(Stereotype.class::cast).findAny();
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

	public static EObject unapplyStereotype(Element element, String qualifiedName) {
		Stereotype stereotype = element.getAppliedStereotype(qualifiedName);
		return stereotype == null? null : element.unapplyStereotype(stereotype);
	}
}
