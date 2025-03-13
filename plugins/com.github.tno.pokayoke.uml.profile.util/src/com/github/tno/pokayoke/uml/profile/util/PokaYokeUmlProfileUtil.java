
package com.github.tno.pokayoke.uml.profile.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.CallBehaviorAction;
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

import PokaYoke.FormalControlFlow;
import PokaYoke.FormalElement;
import PokaYoke.PokaYokePackage;

public class PokaYokeUmlProfileUtil {
    private static final String ST_FORMAL_ELEMENT = PokaYokePackage.Literals.FORMAL_ELEMENT.getName();

    private static final String PROP_FORMAL_ELEMENT_GUARD = PokaYokePackage.Literals.FORMAL_ELEMENT__GUARD.getName();

    private static final String PROP_FORMAL_ELEMENT_EFFECTS = PokaYokePackage.Literals.FORMAL_ELEMENT__EFFECTS
            .getName();

    private static final String PROP_FORMAL_ELEMENT_ATOMIC = PokaYokePackage.Literals.FORMAL_ELEMENT__ATOMIC.getName();

    private static final String ST_FORMAL_CONTROL_FLOW = PokaYokePackage.Literals.FORMAL_CONTROL_FLOW.getName();

    private static final String PROP_FORMAL_CONTROL_FLOW_OUTGOING_GUARD = PokaYokePackage.Literals.FORMAL_CONTROL_FLOW__OUTGOING_GUARD
            .getName();

    /** Qualified name for the {@link PokaYokePackage Poka Yoke} profile. */
    public static final String POKA_YOKE_PROFILE = PokaYokePackage.eNAME;

    /** Qualified name for the {@link FormalElement} stereotype. */
    public static final String FORMAL_ELEMENT_STEREOTYPE = POKA_YOKE_PROFILE + NamedElement.SEPARATOR
            + ST_FORMAL_ELEMENT;

    /** Qualified name for the {@link FormalControlFlow} stereotype. */
    public static final String FORMAL_CONTROL_FLOW_STEREOTYPE = POKA_YOKE_PROFILE + NamedElement.SEPARATOR
            + ST_FORMAL_CONTROL_FLOW;

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

    /**
     * Returns <code>true</code> if {@link FormalControlFlow} stereotype is applied on {@link ControlFlow controlFlow}.
     *
     * @param controlFlow The control flow to interrogate.
     * @return <code>true</code> if {@link FormalControlFlow} stereotype is applied on the control flow.
     */
    public static boolean isFormalControlFlow(ControlFlow controlFlow) {
        return PokaYokeUmlProfileUtil.getAppliedStereotype(controlFlow, FORMAL_CONTROL_FLOW_STEREOTYPE).isPresent();
    }

    public static boolean isGuardEffectsAction(Action action) {
        return isSetGuard(action) || isSetEffects(action);
    }

    public static boolean isSetGuard(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> element.hasValue(st, PROP_FORMAL_ELEMENT_GUARD)).orElse(false);
    }

    public static String getGuard(RedefinableElement element) {
        if (element instanceof ControlFlow) {
            throw new RuntimeException("Control flow must use incoming or outgoing guard getter.");
        }

        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> (String)element.getValue(st, PROP_FORMAL_ELEMENT_GUARD)).orElse(null);
    }

    public static String getIncomingGuard(ControlFlow controlFlow) {
        ValueSpecification guard = controlFlow.getGuard();
        return guard == null ? null : guard.stringValue();
    }

    public static String getOutgoingGuard(ControlFlow controlFlow) {
        return getAppliedStereotype(controlFlow, FORMAL_CONTROL_FLOW_STEREOTYPE)
                .map(st -> (String)controlFlow.getValue(st, PROP_FORMAL_CONTROL_FLOW_OUTGOING_GUARD)).orElse(null);
    }

    public static void setGuard(RedefinableElement element, String newValue) {
        if (element instanceof ControlFlow) {
            throw new RuntimeException("Control flow must use incoming or outgoing guard setter.");
        } else {
            Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
            element.setValue(st, PROP_FORMAL_ELEMENT_GUARD, newValue);
        }
    }

    public static boolean isSetEffects(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> element.hasValue(st, PROP_FORMAL_ELEMENT_EFFECTS)).orElse(false);
    }

    /**
     * Returns the contents of the {@link FormalElement#getEffects() effects} if the {@link FormalElement} stereotype is
     * applied on {@code element}, and an empty list otherwise. The returned list is a copy of the effects and as such,
     * modifications to the list are not reflected on the {@code element}. Instead, use the
     * {@link #setEffects(RedefinableElement, List)} method to set the new value on the {@code element}.
     *
     * @param element The element to get the property from.
     * @return The new property value.
     * @see #setEffects(RedefinableElement, List)
     */
    @SuppressWarnings("unchecked")
    public static List<String> getEffects(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> new ArrayList<>((List<String>)element.getValue(st, PROP_FORMAL_ELEMENT_EFFECTS)))
                .orElse(new ArrayList<>());
    }

    /**
     * Sets {@code newValue} as contents of the {@link FormalElement#getEffects() effects}. We are using a setter here
     * to deal with the stereotype that is required to set the value. We do not want to implicitly create the stereotype
     * on read, but explicitly create it on write.
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    @SuppressWarnings("unchecked")
    public static void setEffects(RedefinableElement element, List<String> newValue) {
        Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
        EList<String> value = (EList<String>)element.getValue(st, PROP_FORMAL_ELEMENT_EFFECTS);
        if (newValue == null) {
            value.clear();
        } else {
            ECollections.setEList(value, newValue);
        }
    }

    public static boolean isAtomic(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> (Boolean)element.getValue(st, PROP_FORMAL_ELEMENT_ATOMIC)).orElse(false);
    }

    public static void setAtomic(RedefinableElement element, Boolean newValue) {
        Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
        element.setValue(st, PROP_FORMAL_ELEMENT_ATOMIC, newValue);
    }

    public static boolean isDeterministic(RedefinableElement element) {
        return getEffects(element).size() <= 1;
    }

    /**
     * Applies the Poka Yoke UML Profile and sets the
     * {@link ControlFlow#setGuard(org.eclipse.uml2.uml.ValueSpecification) incoming guard} for {@code controlFlow}.
     *
     * @param controlFlow The control flow to set the incoming guard on.
     * @param newGurad The new incoming guard.
     */
    public static void setIncomingGuard(ControlFlow controlFlow, String newValue) {
        applyStereotype(controlFlow, getPokaYokeProfile(controlFlow).getOwnedStereotype(ST_FORMAL_CONTROL_FLOW));
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

    /**
     * Applies the Poka Yoke UML Profile and sets the {@link FormalControlFlow#getOutgoingGuard()} property for
     * {@code controlFlow}.
     *
     * @param controlFlow The control flow to set the outgoing guard on.
     * @param newGurad The new outgoing guard.
     */
    public static void setOutgoingGuard(ControlFlow controlFlow, String newValue) {
        Stereotype st = applyStereotype(controlFlow,
                getPokaYokeProfile(controlFlow).getOwnedStereotype(ST_FORMAL_CONTROL_FLOW));
        controlFlow.setValue(st, PROP_FORMAL_CONTROL_FLOW_OUTGOING_GUARD, newValue);
    }

    /**
     * Determines whether two given action nodes are equivalent, which is the case when either:
     * <ul>
     * <li>Both action nodes are call behavior action nodes that call the same behavior, or</li>
     * <li>Both action nodes are formal elements with the same guard, effects, and atomic property.</li>
     * </ul>
     *
     * @param left The first action node.
     * @param right The second action node.
     * @return {@code true} if the given action nodes are equivalent, {@code false} otherwise.
     */
    public static boolean areEquivalent(Action left, Action right) {
        // Two actions are not equivalent if they have different names.
        if (!left.getName().equals(right.getName())) {
            return false;
        }

        // Two actions are equivalent if they are both call behavior actions that call the same behavior.
        if (left instanceof CallBehaviorAction cbLeft && right instanceof CallBehaviorAction cbRight
                && cbLeft.getBehavior().equals(cbRight.getBehavior()))
        {
            return true;
        }

        // Otherwise they are equivalent only if they are both formal elements and have the same guard, effects, and
        // atomic property.
        return isFormalElement(left) && isFormalElement(right) && getGuard(left).equals(getGuard(right))
                && getEffects(left).equals(getEffects(right)) && isAtomic(left) == isAtomic(right);
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
