
package com.github.tno.pokayoke.uml.profile.design;

import static com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil.GUARD_EFFECTS_ACTION_STEREOTYPE;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Generalization;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.LiteralNull;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.github.tno.pokayoke.uml.profile.util.UmlPrimitiveType;
import com.google.common.base.Strings;

import PokaYoke.GuardEffectsAction;

/**
 * The services class used by VSM.
 */
public class PokaYokeProfileServices {
    /**
     * Returns <code>true</code> if {@link GuardEffectsAction} stereotype is applied on {@link Action action}.
     *
     * @param action The action to interrogate.
     * @return <code>true</code> if {@link GuardEffectsAction} stereotype is applied on action.
     */
    public boolean isGuardEffectsAction(Action action) {
        return PokaYokeUmlProfileUtil.isGuardEffectsAction(action);
    }

    /**
     * Returns the {@link GuardEffectsAction#getGuard()} property value if <code>action</code> is stereotype,
     * <code>null</code> otherwise.
     *
     * @param action The action to interrogate.
     * @return The {@link GuardEffectsAction#getGuard()} property value if <code>action</code> is stereotype,
     *     <code>null</code> otherwise.
     */
    public String getGuard(Action action) {
        return PokaYokeUmlProfileUtil.getGuard(action);
    }

    /**
     * Applies the {@link GuardEffectsAction} stereotype and set its {@link GuardEffectsAction#setGuard(String) guard}
     * property for <code>action</code>.
     * <p>
     * The {@link GuardEffectsAction} stereotype is removed if <code>newValue</code> is <code>null</code> or
     * {@link String#isEmpty() empty} and {@link #getEffects(Action)} also is <code>null</code> or
     * {@link String#isEmpty() empty}.
     * </p>
     *
     * @param action The action to set the property on.
     * @param newValue The new property value.
     */
    public void setGuard(Action action, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            String effects = getEffects(action);
            if (Strings.isNullOrEmpty(effects)) {
                PokaYokeUmlProfileUtil.unapplyStereotype(action, GUARD_EFFECTS_ACTION_STEREOTYPE);
                return;
            }
            // Empty values are not allowed, so reset the value
            newValue = null;
        }
        PokaYokeUmlProfileUtil.setGuard(action, newValue);
    }

    /**
     * Returns the {@link GuardEffectsAction#getEffects()} property value if <code>action</code> is stereotype,
     * <code>null</code> otherwise.
     *
     * @param action The action to interrogate.
     * @return The {@link GuardEffectsAction#getEffects()} property value if <code>action</code> is stereotype,
     *     <code>null</code> otherwise.
     */
    public String getEffects(Action action) {
        return PokaYokeUmlProfileUtil.getEffects(action);
    }

    /**
     * Applies the {@link GuardEffectsAction} stereotype and set its {@link GuardEffectsAction#setEffects(String)
     * effects} property for <code>action</code>.
     * <p>
     * The {@link GuardEffectsAction} stereotype is removed if <code>newValue</code> is <code>null</code> or
     * {@link String#isEmpty() empty} and {@link #getEffects(Action)} also is <code>null</code> or
     * {@link String#isEmpty() empty}.
     * </p>
     *
     * @param action The action to set the property on.
     * @param newValue The new property value.
     */
    public void setEffects(Action action, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            String guard = getGuard(action);
            if (Strings.isNullOrEmpty(guard)) {
                PokaYokeUmlProfileUtil.unapplyStereotype(action, GUARD_EFFECTS_ACTION_STEREOTYPE);
                return;
            }
            // Empty values are not allowed, so reset the value
            newValue = null;
        }
        PokaYokeUmlProfileUtil.setEffects(action, newValue);
    }

    /**
     * Returns the {@link ControlFlow#getGuard()} value as a {@link ValueSpecification#stringValue() String}.
     *
     * @param controlFlow The control-flow to interrogate.
     * @return The {@link ControlFlow#getGuard()} value as a {@link ValueSpecification#stringValue() String}.
     */
    public String getGuard(ControlFlow controlFlow) {
        ValueSpecification guard = controlFlow.getGuard();
        return guard == null ? null : guard.stringValue();
    }

    /**
     * Set the {@link ControlFlow#getGuard()} value to <code>newValue</code> where:
     * <ul>
     * <li><code>null</code></li>: <code>null</code> if guard is already <code>null</code>, {@link LiteralNull}
     * otherwise.
     * <li><code>true</code> or <code>false</code></li>: {@link LiteralBoolean} holding the
     * {@link LiteralBoolean#setValue(boolean) value}
     * <li>default: {@link OpaqueExpression} holding the value in its {@link OpaqueExpression#getBodies() first
     * body}</li>
     * </ul>
     *
     * @param controlFlow The control-flow to set the guard value on.
     * @param newValue The new guard value.
     */
    public void setGuard(ControlFlow controlFlow, String newValue) {
        PokaYokeUmlProfileUtil.setGuard(controlFlow, newValue);
    }

    /**
     * Returns all Poka Yoke supported types for {@code property}.
     *
     * @param property The property (i.e. context) for which the supported types are queried.
     * @return All Poka Yoke supported types for {@code property}.
     * @see PokaYokeTypeUtil#getSupportedTypes(org.eclipse.uml2.uml.Element)
     */
    public List<Type> getSupportedPropertyTypes(Property property) {
        return PokaYokeTypeUtil.getSupportedTypes(property);
    }

    /**
     * Applies the Poka Yoke UML Profile and set the {@link Property#setDefault(String) default value} property for
     * {@code property}.
     *
     * @param property The property to set the default value on.
     * @param newValue The new default value of the property.
     */
    public void setPropertyDefaultValue(Property property, String newValue) {
        PokaYokeUmlProfileUtil.setDefaultValue(property, newValue);
    }

    /**
     * Returns the supported super types for the primitive {@code type}.
     * <p>
     * Currently only {@link UmlPrimitiveType#INTEGER primitive integer} is a supported super type.
     * </p>
     *
     * @param type The type context for resolving the available super types.
     * @return The supported super types for {@code type}.
     */
    public List<PrimitiveType> getSupportedSuperTypes(PrimitiveType type) {
        return Arrays.asList(UmlPrimitiveType.INTEGER.load(type));
    }

    public PrimitiveType getSuperType(PrimitiveType type) {
        return type.getGeneralizations().stream().map(Generalization::getGeneral)
                .filter(PrimitiveType.class::isInstance).map(PrimitiveType.class::cast).findAny().orElse(null);
    }

    public void setSuperType(PrimitiveType type, PrimitiveType superType) {
        if (superType == null) {
            type.getGeneralizations().clear();
        } else if (type.getGeneralization(superType) == null) {
            type.getGeneralizations().clear();
            type.createGeneralization(superType);
        }
    }

    public String getMinValue(PrimitiveType type) {
        Integer minValue = PokaYokeTypeUtil.getMinValue(type);
        return minValue == null ? null : minValue.toString();
    }

    public void setMinValue(PrimitiveType type, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            Constraint constraint = PokaYokeTypeUtil.getMinConstraint(type, false);
            if (constraint != null) {
                EcoreUtil.delete(constraint, true);
            }
            return;
        }
        try {
            LiteralInteger specification = UMLFactory.eINSTANCE.createLiteralInteger();
            specification.setValue(Integer.parseInt(newValue));
            PokaYokeTypeUtil.getMinConstraint(type, true).setSpecification(specification);
        } catch (NumberFormatException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, getClass(),
                    "Failed to parse integer value: " + e.getLocalizedMessage(), e));
        }
    }

    public String getMaxValue(PrimitiveType type) {
        Integer maxValue = PokaYokeTypeUtil.getMaxValue(type);
        return maxValue == null ? null : maxValue.toString();
    }

    public void setMaxValue(PrimitiveType type, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            Constraint constraint = PokaYokeTypeUtil.getMaxConstraint(type, false);
            if (constraint != null) {
                EcoreUtil.delete(constraint, true);
            }
            return;
        }
        try {
            LiteralInteger specification = UMLFactory.eINSTANCE.createLiteralInteger();
            specification.setValue(Integer.parseInt(newValue));
            PokaYokeTypeUtil.getMaxConstraint(type, true).setSpecification(specification);
        } catch (NumberFormatException e) {
            Activator.getDefault().getLog().log(new Status(IStatus.ERROR, getClass(),
                    "Failed to parse integer value: " + e.getLocalizedMessage(), e));
        }
    }
}
