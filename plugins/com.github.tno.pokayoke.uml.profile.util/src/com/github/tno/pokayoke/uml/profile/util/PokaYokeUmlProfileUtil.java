
package com.github.tno.pokayoke.uml.profile.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.CallBehaviorAction;
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

    public static boolean isGuardEffectsAction(Action action) {
        return isSetGuard(action) || isSetEffects(action);
    }

    public static boolean isSetGuard(RedefinableElement element) {
        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> element.hasValue(st, PROP_FORMAL_ELEMENT_GUARD)).orElse(false);
    }

    public static String getGuard(RedefinableElement element) {
        if (element instanceof ActivityEdge edge) {
            throw new RuntimeException(
                    "Cannot get guard of activity edge. Incoming or outgoing guard getter must be used.");
        }

        return getAppliedStereotype(element, FORMAL_ELEMENT_STEREOTYPE)
                .map(st -> (String)element.getValue(st, PROP_FORMAL_ELEMENT_GUARD)).orElse(null);
    }

    public static String getIncomingGuard(ActivityEdge activityEdge) {
        ValueSpecification guard = activityEdge.getGuard();
        if (guard instanceof OpaqueExpression opaqueExprGuard) {
            // Backward compatibility: if there is a single body, assume it is an outgoing guard. Create a new opaque
            // expression with two bodies, and return the first one (null).
            if (opaqueExprGuard.getBodies().size() == 1) {
                activityEdge.setGuard(
                        createOpaqueExpressionWithBodies(Stream.of(null, opaqueExprGuard.getBodies().get(0)).toList()));
                guard.destroy();
                return null;
            }
            return opaqueExprGuard.getBodies().get(0);
        }
        return (guard == null) ? null : guard.stringValue();
    }

    public static String getOutgoingGuard(ActivityEdge activityEdge) {
        ValueSpecification guard = activityEdge.getGuard();
        if (guard instanceof OpaqueExpression opaqueExprGuard) {
            // Backward compatibility: if there is a single body, assume it is an outgoing guard. Create a new opaque
            // expression with two bodies, and return the second one.
            if (opaqueExprGuard.getBodies().size() == 1) {
                activityEdge.setGuard(
                        createOpaqueExpressionWithBodies(Stream.of(null, opaqueExprGuard.getBodies().get(0)).toList()));
                guard.destroy();
                return ((OpaqueExpression)activityEdge.getGuard()).getBodies().get(1);
            }
            return opaqueExprGuard.getBodies().get(1);
        }
        return (guard == null) ? null : guard.stringValue();
    }

    public static void setGuard(RedefinableElement element, String newValue) {
        if (element instanceof ActivityEdge edge) {
            throw new RuntimeException(
                    "Cannot set guard of activity edge. Incoming or outgoing guard setter must be used.");
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
     * {@link ActivityEdge#setGuard(org.eclipse.uml2.uml.ValueSpecification) incoming guard} for {@code activityEdge}.
     *
     * @param activityEdge The activity edge to set the incoming guard value on.
     * @param newValue The new value of the incoming guard.
     */
    public static void setIncomingGuard(ActivityEdge activityEdge, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            if (activityEdge.getGuard() instanceof OpaqueExpression opaqueExprGuard) {
                List<String> currentBodies = opaqueExprGuard.getBodies();
                if (currentBodies.size() == 2 && currentBodies.get(1) == null) {
                    // Resetting a value to null causes a model-element deletion popup in UML designer.
                    // Avoiding this by setting a LiteralNull value.
                    activityEdge.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
                    return;
                }
            }
            newValue = null;
        }

        // Get current guard, and update the first element of the body of the opaque expression.
        ValueSpecification currentGuard = activityEdge.getGuard();
        if (currentGuard instanceof OpaqueExpression opaqueExprGuard) {
            List<String> currentBodies = opaqueExprGuard.getBodies();
            if (currentBodies.size() == 2) {
                activityEdge
                        .setGuard(createOpaqueExpressionWithBodies(Stream.of(newValue, currentBodies.get(1)).toList()));
            } else if (currentBodies.size() == 1) {
                // Backward compatibility: if there is only one body, consider it as an outgoing guard. Create a new
                // expression with two bodies.
                activityEdge
                        .setGuard(createOpaqueExpressionWithBodies(Stream.of(newValue, currentBodies.get(0)).toList()));
            }
        } else {
            if (newValue == null) {
                activityEdge.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
            } else {
                activityEdge.setGuard(createOpaqueExpressionWithBodies(Stream.of(newValue, null).toList()));
            }
        }
    }

    /**
     * Applies the Poka Yoke UML Profile and sets the
     * {@link ActivityEdge#setGuard(org.eclipse.uml2.uml.ValueSpecification) outgoing guard} for {@code activityEdge}.
     *
     * @param activityEdge The activity edge to set the outgoing guard value on.
     * @param newValue The new value of the outgoing guard.
     */
    public static void setOutgoingGuard(ActivityEdge activityEdge, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            if (activityEdge.getGuard() instanceof OpaqueExpression opaqueExprGuard) {
                List<String> currentBodies = opaqueExprGuard.getBodies();
                if (currentBodies.size() == 2 && currentBodies.get(0) == null) {
                    // Resetting a value to null causes a model-element deletion popup in UML designer.
                    // Avoiding this by setting a LiteralNull value.
                    activityEdge.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
                    return;
                }
            }
            newValue = null;
        }

        // Get current guard, and update the second element of the body of the opaque expression.
        ValueSpecification currentGuard = activityEdge.getGuard();
        if (currentGuard instanceof OpaqueExpression opaqueExprGuard) {
            List<String> currentBodies = opaqueExprGuard.getBodies();

            if (currentBodies.size() == 2) {
                activityEdge
                        .setGuard(createOpaqueExpressionWithBodies(Stream.of(currentBodies.get(0), newValue).toList()));
            } else if (currentBodies.size() == 1) {
                // Backward compatibility: if there is only one body, consider it as an outgoing guard. Replace it, and
                // create a new expression with two bodies.
                activityEdge.setGuard(createOpaqueExpressionWithBodies(Stream.of(null, newValue).toList()));
            }
        } else {
            if (newValue == null) {
                activityEdge.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
            } else {
                activityEdge.setGuard(createOpaqueExpressionWithBodies(Stream.of(null, newValue).toList()));
            }
        }
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

    private static OpaqueExpression createOpaqueExpressionWithBodies(List<String> newValues) {
        OpaqueExpression expression = UMLFactory.eINSTANCE.createOpaqueExpression();
        for (String newValue: newValues) {
            expression.getLanguages().add("CIF");
            expression.getBodies().add(newValue);
        }
        return expression;
    }
}
