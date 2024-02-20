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
	public String getGuard() {
		return guard;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public String getEffects() {
		return effects;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION:
				if (resolve) return getBase_OpaqueAction();
				return basicGetBase_OpaqueAction();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				return getGuard();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				return getEffects();
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
