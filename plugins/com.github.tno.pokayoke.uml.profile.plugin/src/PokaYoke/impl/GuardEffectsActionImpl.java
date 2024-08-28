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

import org.eclipse.uml2.uml.Action;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Guard Effects Action</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#getGuard <em>Guard</em>}</li>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#getEffects <em>Effects</em>}</li>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#getBase_Action <em>Base Action</em>}</li>
 *   <li>{@link PokaYoke.impl.GuardEffectsActionImpl#isAtomic <em>Atomic</em>}</li>
 * </ul>
 *
 * @generated
 */
public class GuardEffectsActionImpl extends MinimalEObjectImpl.Container implements GuardEffectsAction {
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
	 * The cached value of the '{@link #getBase_Action() <em>Base Action</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBase_Action()
	 * @generated
	 * @ordered
	 */
	protected Action base_Action;

	/**
	 * The default value of the '{@link #isAtomic() <em>Atomic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAtomic()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ATOMIC_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isAtomic() <em>Atomic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAtomic()
	 * @generated
	 * @ordered
	 */
	protected boolean atomic = ATOMIC_EDEFAULT;

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
	public Action getBase_Action() {
		if (base_Action != null && base_Action.eIsProxy()) {
			InternalEObject oldBase_Action = (InternalEObject)base_Action;
			base_Action = (Action)eResolveProxy(oldBase_Action);
			if (base_Action != oldBase_Action) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_ACTION, oldBase_Action, base_Action));
			}
		}
		return base_Action;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Action basicGetBase_Action() {
		return base_Action;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBase_Action(Action newBase_Action) {
		Action oldBase_Action = base_Action;
		base_Action = newBase_Action;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_ACTION, oldBase_Action, base_Action));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAtomic() {
		return atomic;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAtomic(boolean newAtomic) {
		boolean oldAtomic = atomic;
		atomic = newAtomic;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.GUARD_EFFECTS_ACTION__ATOMIC, oldAtomic, atomic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				return getGuard();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				return getEffects();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_ACTION:
				if (resolve) return getBase_Action();
				return basicGetBase_Action();
			case PokaYokePackage.GUARD_EFFECTS_ACTION__ATOMIC:
				return isAtomic();
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
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				setGuard((String)newValue);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				setEffects((String)newValue);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_ACTION:
				setBase_Action((Action)newValue);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__ATOMIC:
				setAtomic((Boolean)newValue);
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
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				setGuard(GUARD_EDEFAULT);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				setEffects(EFFECTS_EDEFAULT);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_ACTION:
				setBase_Action((Action)null);
				return;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__ATOMIC:
				setAtomic(ATOMIC_EDEFAULT);
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
			case PokaYokePackage.GUARD_EFFECTS_ACTION__GUARD:
				return GUARD_EDEFAULT == null ? guard != null : !GUARD_EDEFAULT.equals(guard);
			case PokaYokePackage.GUARD_EFFECTS_ACTION__EFFECTS:
				return EFFECTS_EDEFAULT == null ? effects != null : !EFFECTS_EDEFAULT.equals(effects);
			case PokaYokePackage.GUARD_EFFECTS_ACTION__BASE_ACTION:
				return base_Action != null;
			case PokaYokePackage.GUARD_EFFECTS_ACTION__ATOMIC:
				return atomic != ATOMIC_EDEFAULT;
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
		result.append(", atomic: ");
		result.append(atomic);
		result.append(')');
		return result.toString();
	}

} //GuardEffectsActionImpl
