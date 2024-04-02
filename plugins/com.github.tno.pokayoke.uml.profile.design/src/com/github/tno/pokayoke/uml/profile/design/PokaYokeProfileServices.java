
package com.github.tno.pokayoke.uml.profile.design;

import static com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil.GUARD_EFFECTS_ACTION_STEREOTYPE;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralNull;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Strings;

import PokaYoke.GuardEffectsAction;

/**
 * The services class used by VSM.
 */
public class PokaYokeProfileServices {
    /**
     * Returns <code>true</code> if {@link GuardEffectsAction} stereotype is applied on {@link Action action}.
     *
     * @param action the action to interrogate.
     * @return <code>true</code> if {@link GuardEffectsAction} stereotype is applied on action.
     */
    public static boolean isGuardEffectsAction(Action action) {
        return PokaYokeUmlProfileUtil.isGuardEffectsAction(action);
    }

    /**
     * Returns the {@link GuardEffectsAction#getGuard()} property value if <code>action</code> is stereotype,
     * <code>null</code> otherwise.
     *
     * @param action the action to interrogate.
     * @return the {@link GuardEffectsAction#getGuard()} property value if <code>action</code> is stereotype,
     *     <code>null</code> otherwise.
     */
    public static String getGuard(Action action) {
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
     * @param action the action to set the property on.
     * @param newValue the new property value.
     */
    public static void setGuard(Action action, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            String effects = getEffects(action);
            if (Strings.isNullOrEmpty(effects)) {
                PokaYokeUmlProfileUtil.unapplyStereotype(action, GUARD_EFFECTS_ACTION_STEREOTYPE);
                return;
            }
        }
        PokaYokeUmlProfileUtil.setGuard(action, newValue);
    }

    /**
     * Returns the {@link GuardEffectsAction#getEffects()} property value if <code>action</code> is stereotype,
     * <code>null</code> otherwise.
     *
     * @param action the action to interrogate.
     * @return the {@link GuardEffectsAction#getEffects()} property value if <code>action</code> is stereotype,
     *     <code>null</code> otherwise.
     */
    public static String getEffects(Action action) {
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
     * @param action the action to set the property on.
     * @param newValue the new property value.
     */
    public static void setEffects(Action action, String newValue) {
        if (Strings.isNullOrEmpty(newValue)) {
            String effects = getEffects(action);
            if (Strings.isNullOrEmpty(effects)) {
                PokaYokeUmlProfileUtil.unapplyStereotype(action, GUARD_EFFECTS_ACTION_STEREOTYPE);
                return;
            }
        }
        PokaYokeUmlProfileUtil.setEffects(action, newValue);
    }

    /**
     * Returns the {@link ControlFlow#getGuard()} value as a {@link ValueSpecification#stringValue() String}.
     *
     * @param controlFlow the control-flow to interrogate.
     * @return the {@link ControlFlow#getGuard()} value as a {@link ValueSpecification#stringValue() String}.
     */
    public static String getGuard(ControlFlow controlFlow) {
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
     * @param controlFlow the control-flow to set the guard value on.
     * @param newValue the new guard value.
     */
    public static void setGuard(ControlFlow controlFlow, String newValue) {
        if (newValue == null || newValue.isEmpty()) {
            if (controlFlow.getGuard() != null) {
                controlFlow.setGuard(UMLFactory.eINSTANCE.createLiteralNull());
            }
            return;
        } else if ("true".equals(newValue) || "false".equals(newValue)) {
            // Supports resetting the value to the default 'true'.
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
        ((OpaqueExpression)guard).getBodies().clear();
        ((OpaqueExpression)guard).getBodies().add(newValue);
    }
}
