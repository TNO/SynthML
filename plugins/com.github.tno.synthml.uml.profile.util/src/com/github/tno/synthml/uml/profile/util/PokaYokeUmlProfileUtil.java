
package com.github.tno.synthml.uml.profile.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.LiteralBoolean;
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

import SynthML.FormalCallBehaviorAction;
import SynthML.FormalControlFlow;
import SynthML.FormalElement;
import SynthML.SynthMLPackage;

public class PokaYokeUmlProfileUtil {
    private static final String ST_FORMAL_ELEMENT = SynthMLPackage.Literals.FORMAL_ELEMENT.getName();

    private static final String PROP_FORMAL_ELEMENT_GUARD = SynthMLPackage.Literals.FORMAL_ELEMENT__GUARD.getName();

    private static final String PROP_FORMAL_ELEMENT_EFFECTS = SynthMLPackage.Literals.FORMAL_ELEMENT__EFFECTS.getName();

    private static final String PROP_FORMAL_ELEMENT_ATOMIC = SynthMLPackage.Literals.FORMAL_ELEMENT__ATOMIC.getName();

    private static final String ST_FORMAL_CONTROL_FLOW = SynthMLPackage.Literals.FORMAL_CONTROL_FLOW.getName();

    private static final String PROP_FORMAL_CONTROL_FLOW_OUTGOING_GUARD = SynthMLPackage.Literals.FORMAL_CONTROL_FLOW__OUTGOING_GUARD
            .getName();

    private static final String ST_FORMAL_CALL_BEHAVIOR_ACTION = SynthMLPackage.Literals.FORMAL_CALL_BEHAVIOR_ACTION
            .getName();

    private static final String PROP_FORMAL_CALL_BEHAVIOR_ACTION_ARGUMENTS = SynthMLPackage.Literals.FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS
            .getName();

    /** Qualified name for the {@link SynthMLPackage Poka Yoke} profile. */
    public static final String POKA_YOKE_PROFILE = SynthMLPackage.eNAME;

    /** Qualified name for the {@link FormalElement} stereotype. */
    public static final String FORMAL_ELEMENT_STEREOTYPE = POKA_YOKE_PROFILE + NamedElement.SEPARATOR
            + ST_FORMAL_ELEMENT;

    /** Qualified name for the {@link FormalCallBehaviorAction} stereotype. */
    public static final String FORMAL_CALL_BEHAVIOR_ACTION_STEREOTYPE = POKA_YOKE_PROFILE + NamedElement.SEPARATOR
            + ST_FORMAL_CALL_BEHAVIOR_ACTION;

    /** Qualified name for the {@link FormalControlFlow} stereotype. */
    public static final String FORMAL_CONTROL_FLOW_STEREOTYPE = POKA_YOKE_PROFILE + NamedElement.SEPARATOR
            + ST_FORMAL_CONTROL_FLOW;

    private PokaYokeUmlProfileUtil() {
        // Empty for utility classes
    }

    /**
     * Returns {@code true} if {@link FormalElement} stereotype is applied on {@link RedefinableElement element}.
     *
     * @param element The element to interrogate.
     * @return {@code true} if {@link FormalElement} stereotype is applied on element.
     */
    public static boolean isFormalElement(RedefinableElement element) {
        return PokaYokeUmlProfileUtil.getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE).isPresent();
    }

    public static boolean isGuardEffectsAction(Action action) {
        return isSetGuard(action) || isSetEffects(action);
    }

    public static boolean isSetGuard(RedefinableElement element) {
        if (element instanceof ControlFlow) {
            throw new RuntimeException("Control flow must use the formal control flow stereotype.");
        }
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

    public static String getIncomingGuard(ActivityEdge edge) {
        if (edge instanceof ControlFlow controlFlow) {
            ValueSpecification guard = controlFlow.getGuard();
            return guard == null ? null : guard.stringValue();
        } else {
            throw new RuntimeException(String.format("Expected a control flow, but got '%s'.", edge));
        }
    }

    public static String getOutgoingGuard(ActivityEdge edge) {
        if (edge instanceof ControlFlow controlFlow) {
            return getAppliedStereotype(controlFlow, FORMAL_CONTROL_FLOW_STEREOTYPE)
                    .map(st -> (String)controlFlow.getValue(st, PROP_FORMAL_CONTROL_FLOW_OUTGOING_GUARD)).orElse(null);
        } else {
            throw new RuntimeException(String.format("Expected a control flow, but got '%s'.", edge));
        }
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#setGuard(String) guard} property
     * for {@code element}.
     * <p>
     * The {@link FormalElement} stereotype is removed if {@code newValue} is {@code null} or {@link String#isEmpty()
     * empty} and {@link #getEffects(RedefinableElement)} also is {@code null} or {@link String#isEmpty() empty} and
     * {@link #isAtomic(RedefinableElement)} is {@code false}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public static void setGuard(RedefinableElement element, String newValue) {
        if (element instanceof ControlFlow) {
            throw new RuntimeException("Control flow must use incoming or outgoing guard setter.");
        } else {
            if (Strings.isNullOrEmpty(newValue)) {
                List<String> effects = getEffects(element);
                boolean atomic = isAtomic(element);
                if (effects.isEmpty() && !atomic) {
                    PokaYokeUmlProfileUtil.unapplyStereotype(element, FORMAL_ELEMENT_STEREOTYPE);
                    return;
                }
            }

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
     * <p>
     * The {@link FormalElement} stereotype is removed if {@code newValue} is {@code null} or {@link String#isEmpty()
     * empty} and {@link #getGuard(RedefinableElement)} also is {@code null} or {@link String#isEmpty() empty} and
     * {@link #isAtomic(RedefinableElement)} is {@code false}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    @SuppressWarnings("unchecked")
    public static void setEffects(RedefinableElement element, List<String> newValue) {
        if (newValue == null || newValue.isEmpty()) {
            String guard = getGuard(element);
            boolean atomic = isAtomic(element);
            if (Strings.isNullOrEmpty(guard) && !atomic) {
                PokaYokeUmlProfileUtil.unapplyStereotype(element, FORMAL_ELEMENT_STEREOTYPE);
                return;
            }
        }
        Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
        EList<String> value = (EList<String>)element.getValue(st, PROP_FORMAL_ELEMENT_EFFECTS);
        if (newValue == null) {
            value.clear();
        } else {
            ECollections.setEList(value, newValue);
        }
    }

    /**
     * Returns the contents of the {@link FormalCallBehaviorAction#getArguments() arguments} if the
     * {@link FormalCallBehaviorAction} stereotype is applied on {@code element}, and an empty string otherwise. Use the
     * {@link #setArguments(CallBehaviorAction, String)} method to set the new value on the {@code element}.
     *
     * @param element The element to get the arguments from.
     * @return The argument assignments.
     * @see #setArguments(CallBehaviorAction, String)
     */
    public static String getArguments(CallBehaviorAction element) {
        return getAppliedStereotype(element, FORMAL_CALL_BEHAVIOR_ACTION_STEREOTYPE)
                .map(st -> (String)element.getValue(st, PROP_FORMAL_CALL_BEHAVIOR_ACTION_ARGUMENTS)).orElse("");
    }

    /**
     * Sets {@code newValue} as contents of the {@link FormalCallBehaviorAction#getArguments() arguments}. We are using
     * a setter here to deal with the stereotype that is required to set the value. We do not want to implicitly create
     * the stereotype on read, but explicitly create it on write.
     *
     * @param element The element to set the arguments on.
     * @param newValue The new property value.
     */
    @SuppressWarnings("unchecked")
    public static void setArguments(CallBehaviorAction element, String newValue) {
        if (newValue == null || newValue.isEmpty()) {
            PokaYokeUmlProfileUtil.unapplyStereotype(element, FORMAL_CALL_BEHAVIOR_ACTION_STEREOTYPE);
            return;
        }
        Stereotype st = applyStereotype(element,
                getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_CALL_BEHAVIOR_ACTION));
        EList<String> value = (EList<String>)element.getValue(st, PROP_FORMAL_CALL_BEHAVIOR_ACTION_ARGUMENTS);
        ECollections.setEList(value, List.of(newValue));
    }

    public static boolean isAtomic(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> (Boolean)element.getValue(st, PROP_FORMAL_ELEMENT_ATOMIC)).orElse(false);
    }

    /**
     * Applies the {@link FormalElement} stereotype and sets the {@link FormalElement#setAtomic(boolean) atomic}
     * property for {@code element}.
     * <p>
     * The {@link FormalElement} stereotype is removed if {@code newValue} is {@code null} or {@code false} and
     * {@link #getGuard(RedefinableElement)} is {@code null} or {@link String#isEmpty() empty} and
     * {@link #getEffects(RedefinableElement)} also is {@code null} or {@link String#isEmpty() empty}.
     * </p>
     *
     * @param element The element to set the property on.
     * @param newValue The new property value.
     */
    public static void setAtomic(RedefinableElement element, Boolean newValue) {
        if (newValue == null || !newValue) {
            String guard = getGuard(element);
            List<String> effects = getEffects(element);
            if (Strings.isNullOrEmpty(guard) && effects.isEmpty()) {
                PokaYokeUmlProfileUtil.unapplyStereotype(element, FORMAL_ELEMENT_STEREOTYPE);
                return;
            }
        }
        Stereotype st = applyStereotype(element, getPokaYokeProfile(element).getOwnedStereotype(ST_FORMAL_ELEMENT));
        element.setValue(st, PROP_FORMAL_ELEMENT_ATOMIC, newValue);
    }

    public static boolean isDeterministic(RedefinableElement element) {
        return getEffects(element).size() <= 1;
    }

    /**
     * Sets the {@link ControlFlow#setGuard(org.eclipse.uml2.uml.ValueSpecification) incoming guard} for
     * {@code controlFlow}.
     *
     * @param edge The activity edge to set the incoming guard on.
     * @param newGuard The new incoming guard.
     */
    public static void setIncomingGuard(ActivityEdge edge, String newGuard) {
        if (!(edge instanceof ControlFlow)) {
            throw new RuntimeException("Activity edges must be of type control flow.");
        }

        if (Strings.isNullOrEmpty(newGuard)) {
            if (edge.getGuard() != null) {
                // Resetting a value to null causes a model-element deletion popup in UML designer.
                // Avoiding this by setting a LiteralNull value.
                edge.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
            }
            return;
        }
        edge.setGuard(createCifExpression(newGuard));
    }

    /**
     * Sets the {@link ControlFlow#setGuard(org.eclipse.uml2.uml.ValueSpecification) incoming guard} for
     * {@code controlFlow}.
     *
     * @param edge The activity edge to set the incoming guard on.
     * @param valueSpec The value specification to be set as incoming guard.
     */
    public static void setIncomingGuard(ActivityEdge edge, ValueSpecification valueSpec) {
        if (!(edge instanceof ControlFlow)) {
            throw new RuntimeException("Activity edges must be of type control flow.");
        }

        if (valueSpec == null) {
            if (edge.getGuard() != null) {
                // Resetting a value to null causes a model-element deletion popup in UML designer.
                // Avoiding this by setting a LiteralNull value.
                edge.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
            }
            return;
        }

        if (valueSpec instanceof LiteralBoolean || valueSpec instanceof OpaqueExpression) {
            edge.setGuard(valueSpec);
        } else {
            throw new RuntimeException("Unsupported guard type: " + valueSpec.getType().getName());
        }
    }

    /**
     * Applies the Poka Yoke UML Profile and sets the {@link FormalControlFlow#getOutgoingGuard()} property for
     * {@code edge}.
     * <p>
     * The {@link FormalControlFlow} stereotype is removed if {@code newValue} is {@code null} or
     * {@link String#isEmpty() empty}.
     * </p>
     *
     * @param edge The activity edge to set the outgoing guard on.
     * @param newGuard The new outgoing guard.
     */
    public static void setOutgoingGuard(ActivityEdge edge, String newGuard) {
        if (!(edge instanceof ControlFlow)) {
            throw new RuntimeException("Activity edges must be of type control flow.");
        }

        if (Strings.isNullOrEmpty(newGuard)) {
            PokaYokeUmlProfileUtil.unapplyStereotype(edge, FORMAL_CONTROL_FLOW_STEREOTYPE);
            return;
        }
        Stereotype st = applyStereotype(edge, getPokaYokeProfile(edge).getOwnedStereotype(ST_FORMAL_CONTROL_FLOW));
        edge.setValue(st, PROP_FORMAL_CONTROL_FLOW_OUTGOING_GUARD, newGuard);
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
        URI uri = UMLPlugin.getEPackageNsURIToProfileLocationMap().get(SynthMLPackage.eNS_URI);
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
