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
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralNull;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.ValueSpecification;
import org.obeonetwork.dsl.uml2.core.api.services.ReusedDescriptionServices;

import PokaYoke.PokaYokePackage;

public class GuardEffectsUtil {
	public static final String QN_GUARD_EFFECTS_ACTION = "PokaYoke::GuardEffectsAction";
	private static final String QN_GUARD_EFFECTS_ACTION__GUARD = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__GUARD.getName();
	private static final String QN_GUARD_EFFECTS_ACTION__EFFECTS = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__EFFECTS.getName();

	/**
	 * Returns the {@link ControlFlow#getGuard()} value as a
	 * {@link ValueSpecification#stringValue() String}.
	 * 
	 * @param controlFlow the control-flow to interrogate.
	 * @return the {@link ControlFlow#getGuard()} value as a
	 *         {@link ValueSpecification#stringValue() String}.
	 */
	public static String getGuard(ControlFlow controlFlow) {
		ValueSpecification guard = controlFlow.getGuard();
		return guard == null ? null : guard.stringValue();
	}

	public static AExpression getGuardExpression(ControlFlow controlFlow) throws ParseException {
		String guard = getGuard(controlFlow);
		if (guard == null || guard.isEmpty()) {
			// Guard not set, return null
			return null;
		}
		CifExpressionParser expressionParser = new CifExpressionParser();
		return expressionParser.parseString(guard, controlFlow.eResource().getURI().toString());
	}

	/**
	 * Set the {@link ControlFlow#getGuard()} value to <code>newValue</code> where:
	 * <ul>
	 * <li><code>null</code></li>: <code>null</code> if guard is already
	 * <code>null</code>, {@link LiteralNull} otherwise.
	 * <li><code>true</code> or <code>false</code></li>: {@link LiteralBoolean}
	 * holding the {@link LiteralBoolean#setValue(boolean) value}
	 * <li>default: {@link OpaqueExpression} holding the value in its
	 * {@link OpaqueExpression#getBodies() first body}</li>
	 * </ul>
	 * 
	 * @param controlFlow the control-flow to set the guard value on.
	 * @param newValue    the new guard value.
	 */
	public static void setGuard(ControlFlow controlFlow, String newValue) {
		if (newValue == null || newValue.isEmpty()) {
			if (controlFlow.getGuard() != null) {
				controlFlow.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
			}
			return;
		} else if ("true".equals(newValue) || "false".equals(newValue)) {
			// Supports resetting the value to the default 'true'
			ValueSpecification guard = controlFlow.getGuard();
			if (!(guard instanceof LiteralBoolean)) {
				guard = UMLFactory.eINSTANCE.createLiteralBoolean();
				controlFlow.setGuard(guard);
			}
			((LiteralBoolean)guard).setValue(Boolean.valueOf(newValue));
			return;
		}
		ValueSpecification guard = controlFlow.getGuard();
		if (!(guard instanceof OpaqueExpression)) {
			guard = UMLFactory.eINSTANCE.createOpaqueExpression();
			controlFlow.setGuard(guard);
		}
		((OpaqueExpression) guard).getBodies().clear();
		((OpaqueExpression) guard).getBodies().add(newValue);
	}

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
		return Optional.ofNullable(element.getAppliedStereotype(qualifiedName));
	}

	public static Stereotype applyStereotype(Element element, String qualifiedName) {
		Optional<Stereotype> st = getAppliedStereotype(element, qualifiedName);
		if (st.isPresent()) {
			return st.get();
		}

		// TODO: We may want to embed this code to avoid dependency on UML designer plugin
		ReusedDescriptionServices rds = new ReusedDescriptionServices();
		st = rds.getAllStereotypesAndProfiles(element).stream().filter(
				sp -> sp instanceof Stereotype s && Objects.equals(s.getQualifiedName(), qualifiedName))
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
