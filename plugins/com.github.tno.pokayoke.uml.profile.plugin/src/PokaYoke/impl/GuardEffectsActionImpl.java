/**
 */
package PokaYoke.impl;

import PokaYoke.GuardEffectsAction;
import PokaYoke.PokaYokePackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.OpaqueAction;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Guard Effects Action</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#getBase_OpaqueAction <em>Base Opaque Action</em>}</li>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#getGuard <em>Guard</em>}</li>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#getEffects <em>Effects</em>}</li>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}</li>
 * </ul>
 *
 * @generated
 */
public class GuardEffectsActionImpl extends MinimalEObjectImpl.Container implements GuardEffectsAction {
	/**
	 * The cached value of the '{@link #getBase_OpaqueAction() <em>Base Opaque Action</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBase_OpaqueAction()
	 * @generated
	 * @ordered
	 */
	protected OpaqueAction base_OpaqueAction;

	/**
	 * The default value of the '{@link #getGuard() <em>Guard</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGuard()
	 * @generated
	 * @ordered
	 */
	protected static final String GUARD_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getGuard() <em>Guard</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGuard()
	 * @generated
	 * @ordered
	 */
	protected String guard = GUARD_EDEFAULT;

	/**
	 * The default value of the '{@link #getEffects() <em>Effects</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEffects()
	 * @generated
	 * @ordered
	 */
	protected static final String EFFECTS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getEffects() <em>Effects</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEffects()
	 * @generated
	 * @ordered
	 */
	protected String effects = EFFECTS_EDEFAULT;

	/**
	 * The cached value of the '{@link #getBase_CallBehaviorAction() <em>Base Call Behavior Action</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBase_CallBehaviorAction()
	 * @generated
	 * @ordered
	 */
	protected CallBehaviorAction base_CallBehaviorAction;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected GuardEffectsActionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return PokaYokePackage.Literals.GUARD_EFFECTS_ACTION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public OpaqueAction getBase_OpaqueAction() {
		if (base_OpaqueAction != null && base_OpaqueAction.eIsProxy()) {
			InternalEObject oldBase_OpaqueAction = (InternalEObject)base_OpaqueAction;
			base_OpaqueAction = (OpaqueAction)eResolveProxy(oldBase_OpaqueAction);
			if (base_OpaqueAction != oldBase_OpaqueAction) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION, oldBase_OpaqueAction, base_OpaqueAction));
			}
		}
		return base_OpaqueAction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OpaqueAction basicGetBase_OpaqueAction() {
		return base_OpaqueAction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBase_OpaqueAction(OpaqueAction newBase_OpaqueAction) {
		OpaqueAction oldBase_OpaqueAction = base_OpaqueAction;
		base_OpaqueAction = newBase_OpaqueAction;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION, oldBase_OpaqueAction, base_OpaqueAction));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getGuard() {
		return guard;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGuard(String newGuard) {
		String oldGuard = guard;
		guard = newGuard;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD, oldGuard, guard));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getEffects() {
		return effects;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setEffects(String newEffects) {
		String oldEffects = effects;
		effects = newEffects;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS, oldEffects, effects));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public CallBehaviorAction getBase_CallBehaviorAction() {
		if (base_CallBehaviorAction != null && base_CallBehaviorAction.eIsProxy()) {
			InternalEObject oldBase_CallBehaviorAction = (InternalEObject)base_CallBehaviorAction;
			base_CallBehaviorAction = (CallBehaviorAction)eResolveProxy(oldBase_CallBehaviorAction);
			if (base_CallBehaviorAction != oldBase_CallBehaviorAction) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_CALL_BEHAVIOR_ACTION, oldBase_CallBehaviorAction, base_CallBehaviorAction));
			}
		}
		return base_CallBehaviorAction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CallBehaviorAction basicGetBase_CallBehaviorAction() {
		return base_CallBehaviorAction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBase_CallBehaviorAction(CallBehaviorAction newBase_CallBehaviorAction) {
		CallBehaviorAction oldBase_CallBehaviorAction = base_CallBehaviorAction;
		base_CallBehaviorAction = newBase_CallBehaviorAction;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_CALL_BEHAVIOR_ACTION, oldBase_CallBehaviorAction, base_CallBehaviorAction));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION:
				if (resolve) return getBase_OpaqueAction();
				return basicGetBase_OpaqueAction();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				return getGuard();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				return getEffects();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_CALL_BEHAVIOR_ACTION:
				if (resolve) return getBase_CallBehaviorAction();
				return basicGetBase_CallBehaviorAction();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION:
				setBase_OpaqueAction((OpaqueAction)newValue);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				setGuard((String)newValue);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				setEffects((String)newValue);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_CALL_BEHAVIOR_ACTION:
				setBase_CallBehaviorAction((CallBehaviorAction)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION:
				setBase_OpaqueAction((OpaqueAction)null);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				setGuard(GUARD_EDEFAULT);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				setEffects(EFFECTS_EDEFAULT);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_CALL_BEHAVIOR_ACTION:
				setBase_CallBehaviorAction((CallBehaviorAction)null);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION:
				return base_OpaqueAction != null;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				return GUARD_EDEFAULT == null ? guard != null : !GUARD_EDEFAULT.equals(guard);
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				return EFFECTS_EDEFAULT == null ? effects != null : !EFFECTS_EDEFAULT.equals(effects);
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_CALL_BEHAVIOR_ACTION:
				return base_CallBehaviorAction != null;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (guard: ");
		result.append(guard);
		result.append(", effects: ");
		result.append(effects);
		result.append(')');
		return result.toString();
	}

} //GuardEffectsActionImpl
