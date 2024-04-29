
package com.github.tno.pokayoke.uml.profile.util;

import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPlugin;

import com.google.common.base.Strings;

import PokaYoke.GuardEffectsAction;
import PokaYoke.PokaYokePackage;

public class PokaYokeUmlProfileUtil {
    private static final String ST_GUARD_EFFECTS_ACTION = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION.getName();

    private static final String PROP_GUARD_EFFECTS_ACTION_GUARD = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__GUARD
            .getName();

    private static final String PROP_GUARD_EFFECTS_ACTION_EFFECTS = PokaYokePackage.Literals.GUARD_EFFECTS_ACTION__EFFECTS
            .getName();

    /** Qualified name for the {@link PokaYokePackage Poka Yoke} profile. */
    public static final String POKA_YOKE_PROFILE = PokaYokePackage.eNAME;

    /** Qualified name for the {@link GuardEffectsAction} stereotype. */
    public static final String GUARD_EFFECTS_ACTION_STEREOTYPE = POKA_YOKE_PROFILE + NamedElement.SEPARATOR
            + ST_GUARD_EFFECTS_ACTION;

    private PokaYokeUmlProfileUtil() {
        // Empty for utility classes
    }

    /**
     * Returns <code>true</code> if {@link GuardEffectsAction} stereotype is applied on {@link Action action}.
     *
     * @param action The action to interrogate.
     * @return <code>true</code> if {@link GuardEffectsAction} stereotype is applied on action.
     */
    public static boolean isGuardEffectsAction(Action action) {
        return PokaYokeUmlProfileUtil.getAppliedStereotype(action, GUARD_EFFECTS_ACTION_STEREOTYPE).isPresent();
    }

    public static String getGuard(Action action) {
        return getAppliedStereotype(action, GUARD_EFFECTS_ACTION_STEREOTYPE)
                .map(st -> (String)action.getValue(st, PROP_GUARD_EFFECTS_ACTION_GUARD)).orElse(null);
    }

    public static void setGuard(Action action, String newValue) {
        Stereotype st = applyStereotype(action, getPokaYokeProfile(action).getOwnedStereotype(ST_GUARD_EFFECTS_ACTION));
        action.setValue(st, PROP_GUARD_EFFECTS_ACTION_GUARD, newValue);
    }

    public static String getEffects(Action action) {
        return getAppliedStereotype(action, GUARD_EFFECTS_ACTION_STEREOTYPE)
                .map(st -> (String)action.getValue(st, PROP_GUARD_EFFECTS_ACTION_EFFECTS)).orElse(null);
    }

    public static void setEffects(Action action, String newValue) {
        Stereotype st = applyStereotype(action, getPokaYokeProfile(action).getOwnedStereotype(ST_GUARD_EFFECTS_ACTION));
        action.setValue(st, PROP_GUARD_EFFECTS_ACTION_EFFECTS, newValue);
    }

    /**
     * Applies the Poka Yoke UML Profile and sets the
     * {@link ControlFlow#setGuard(org.eclipse.uml2.uml.ValueSpecification) guard} for {@code controlFlow}.
     *
     * @param controlFlow The control flow to set the guard value on.
     * @param newValue The new value of the guard.
     */
    public static void setGuard(ControlFlow controlFlow, String newValue) {
        applyPokaYokeProfile(controlFlow);
        if (Strings.isNullOrEmpty(newValue)) {
            if (controlFlow.getGuard() != null) {
                // Resetting a value to null causes a model-element deletion popup in UML designer.
                // Avoiding this by setting a LiteralNull value.
                controlFlow.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
            }
            return;
        }
        OpaqueExpression expression = UMLFactory.eINSTANCE.createOpaqueExpression();
        expression.getLanguages().add("CIF");
        expression.getBodies().add(newValue);
        controlFlow.setGuard(expression);
    }

    /**
     * Applies the Poka Yoke UML Profile and sets the {@link Property#setDefault(String) default value} for
     * {@code property}.
     *
     * @param property The property to set the default value on.
     * @param newValue The new default value of the property.
     */
    public static void setDefaultValue(Property property, String newValue) {
        applyPokaYokeProfile(property);
        if (Strings.isNullOrEmpty(newValue)) {
            if (property.getDefaultValue() != null) {
                // Resetting a value to null causes a model-element deletion popup in UML designer.
                // Avoiding this by setting a LiteralNull value.
                property.setDefaultValue(UMLFactory.eINSTANCE.createLiteralNull());
            }
            return;
        }
        OpaqueExpression expression = UMLFactory.eINSTANCE.createOpaqueExpression();
        expression.getLanguages().add("CIF");
        expression.getBodies().add(newValue);
        property.setDefaultValue(expression);
    }

    public static Optional<Profile> getAppliedProfile(Element element, String qualifiedName) {
        Package pkg = element.getNearestPackage();
        return Optional.ofNullable(pkg.getAppliedProfile(qualifiedName));
    }

    public static Optional<Stereotype> getAppliedStereotype(Element element, String qualifiedName) {
        return Optional.ofNullable(element.getAppliedStereotype(qualifiedName));
    }

    public static void unapplyStereotype(Element element, String qualifiedName) {
        getAppliedStereotype(element, qualifiedName).ifPresent(st -> element.unapplyStereotype(st));
    }

    private static Profile getPokaYokeProfile(Element context) {
        URI uri = UMLPlugin.getEPackageNsURIToProfileLocationMap().get(PokaYokePackage.eNS_URI);
        return Profile.class.cast(context.eResource().getResourceSet().getEObject(uri, true));
    }

    private static Profile applyProfile(Element element, Profile profile) {
        Package pkg = element.getNearestPackage();
        if (!pkg.isProfileApplied(profile)) {
            pkg.applyProfile(profile);
        }
        return profile;
    }

    private static Profile applyPokaYokeProfile(Element element) {
        return applyProfile(element, getPokaYokeProfile(element));
    }

    private static Stereotype applyStereotype(Element element, Stereotype stereotype) {
        if (!element.isStereotypeApplied(stereotype)) {
            applyProfile(element, stereotype.getProfile());
            element.applyStereotype(stereotype);
        }
        return stereotype;
    }
}
