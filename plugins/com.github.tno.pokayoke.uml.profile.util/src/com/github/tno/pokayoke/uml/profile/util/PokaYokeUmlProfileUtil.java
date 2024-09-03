
package com.github.tno.pokayoke.uml.profile.util;

import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.LiteralNull;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPlugin;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.google.common.base.Strings;

import PokaYoke.FormalElement;
import PokaYoke.PokaYokePackage;

public class PokaYokeUmlProfileUtil {
    private static final String ST_FORMAL_ELEMENT = PokaYokePackage.Literals.FORMAL_ELEMENT.getName();

    private static final String PROP_FORMAL_ELEMENT_GUARD = PokaYokePackage.Literals.FORMAL_ELEMENT__GUARD.getName();

    private static final String PROP_FORMAL_ELEMENT_EFFECTS = PokaYokePackage.Literals.FORMAL_ELEMENT__EFFECTS
            .getName();

    private static final String PROP_FORMAL_ELEMENT_ATOMIC = PokaYokePackage.Literals.FORMAL_ELEMENT__ATOMIC.getName();

    /** Qualified name for the {@link PokaYokePackage Poka Yoke} profile. */
    public static final String POKA_YOKE_PROFILE = PokaYokePackage.eNAME;

    /** Qualified name for the {@link FormalElement} stereotype. */
    public static final String FORMAL_ELEMENT_STEREOTYPE = POKA_YOKE_PROFILE + NamedElement.SEPARATOR
            + ST_FORMAL_ELEMENT;

    private PokaYokeUmlProfileUtil() {
        // Empty for utility classes
    }

    /**
     * Returns <code>true</code> if {@link FormalElement} stereotype is applied on {@link RedefinableElement element}.
     *
     * @param element The element to interrogate.
     * @return <code>true</code> if {@link FormalElement} stereotype is applied on element.
     */
    public static boolean isFormalElement(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE).isPresent();
    }

    public static String getGuard(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> (String)element.getValue(st, PROP_FORMAL_ELEMENT_GUARD)).orElse(null);
    }

    public static void setGuard(RedefinableElement element, String newValue) {
        Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
        element.setValue(st, PROP_FORMAL_ELEMENT_GUARD, newValue);
    }

    public static String getEffects(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> (String)element.getValue(st, PROP_FORMAL_ELEMENT_EFFECTS)).orElse(null);
    }

    public static void setEffects(RedefinableElement element, String newValue) {
        Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
        element.setValue(st, PROP_FORMAL_ELEMENT_EFFECTS, newValue);
    }

    public static boolean isAtomic(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> (Boolean)element.getValue(st, PROP_FORMAL_ELEMENT_ATOMIC)).orElse(false);
    }

    public static void setAtomic(RedefinableElement element, Boolean newValue) {
        Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
        element.setValue(st, PROP_FORMAL_ELEMENT_ATOMIC, newValue);
    }

    /**
     * Applies the Poka Yoke UML Profile and sets the
     * {@link ControlFlow#setGuard(org.eclipse.uml2.uml.ValueSpecification) guard} for {@code controlFlow}.
     *
     * @param controlFlow The control flow to set the guard value on.
     * @param newValue The new value of the guard.
     */
    public static void setGuard(ControlFlow controlFlow, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            if (controlFlow.getGuard() != null) {
                // Resetting a value to null causes a model-element deletion popup in UML designer.
                // Avoiding this by setting a LiteralNull value.
                controlFlow.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
            }
            return;
        }
        controlFlow.setGuard(createCifExpression(newValue));
    }

    public static boolean hasDefaultValue(Property property) {
        ValueSpecification valueSpec = property.getDefaultValue();
        return !(valueSpec == null || valueSpec instanceof LiteralNull);
    }

    /**
     * Applies the Poka Yoke UML Profile and sets the {@link Property#setDefault(String) default value} for
     * {@code property}.
     *
     * @param property The property to set the default value on.
     * @param newValue The new default value of the property.
     */
    public static void setDefaultValue(Property property, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            if (property.getDefaultValue() != null) {
                // Resetting a value to null causes a model-element deletion popup in UML designer.
                // Avoiding this by setting a LiteralNull value.
                property.setDefaultValue(UMLFactory.eINSTANCE.createLiteralNull());
            }
            return;
        }
        property.setDefaultValue(createCifExpression(newValue));
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
        Resource resource = context.eResource();
        ResourceSet resourceSet = resource == null ? null : resource.getResourceSet();
        if (resourceSet == null) {
            resourceSet = FileHelper.createModelResourceSet();
        }
        return Profile.class.cast(resourceSet.getEObject(uri, true));
    }

    private static Profile applyProfile(Element element, Profile profile) {
        Package pkg = element.getNearestPackage();
        if (!pkg.isProfileApplied(profile)) {
            pkg.applyProfile(profile);
        }
        return profile;
    }

    public static Profile applyPokaYokeProfile(Element element) {
        return applyProfile(element, getPokaYokeProfile(element));
    }

    private static Stereotype applyStereotype(Element element, Stereotype stereotype) {
        if (!element.isStereotypeApplied(stereotype)) {
            applyProfile(element, stereotype.getProfile());
            element.applyStereotype(stereotype);
        }
        return stereotype;
    }

    private static OpaqueExpression createCifExpression(String newValue) {
        OpaqueExpression expression = UMLFactory.eINSTANCE.createOpaqueExpression();
        expression.getLanguages().add("CIF");
        expression.getBodies().add(newValue);
        return expression;
    }
}
