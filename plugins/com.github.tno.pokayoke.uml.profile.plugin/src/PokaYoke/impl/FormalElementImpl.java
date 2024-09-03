/**
 */
package PokaYoke.impl;

import PokaYoke.FormalElement;
import PokaYoke.PokaYokePackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.uml2.uml.RedefinableElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Formal Element</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link PokaYoke.impl.FormalElementImpl#getGuard <em>Guard</em>}</li>
 *   <li>{@link PokaYoke.impl.FormalElementImpl#getEffects <em>Effects</em>}</li>
 *   <li>{@link PokaYoke.impl.FormalElementImpl#getBase_RedefinableElement <em>Base Redefinable Element</em>}</li>
 *   <li>{@link PokaYoke.impl.FormalElementImpl#isAtomic <em>Atomic</em>}</li>
 * </ul>
 *
 * @generated
 */
public class FormalElementImpl extends MinimalEObjectImpl.Container implements FormalElement {
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
	 * The cached value of the '{@link #getBase_RedefinableElement() <em>Base Redefinable Element</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBase_RedefinableElement()
	 * @generated
	 * @ordered
	 */
	protected RedefinableElement base_RedefinableElement;

	/**
	 * The default value of the '{@link #isAtomic() <em>Atomic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAtomic()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ATOMIC_EDEFAULT = false;

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
	protected FormalElementImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return PokaYokePackage.Literals.FORMAL_ELEMENT;
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
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.FORMAL_ELEMENT__GUARD, oldGuard, guard));
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
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.FORMAL_ELEMENT__EFFECTS, oldEffects, effects));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public RedefinableElement getBase_RedefinableElement() {
		if (base_RedefinableElement != null && base_RedefinableElement.eIsProxy()) {
			InternalEObject oldBase_RedefinableElement = (InternalEObject)base_RedefinableElement;
			base_RedefinableElement = (RedefinableElement)eResolveProxy(oldBase_RedefinableElement);
			if (base_RedefinableElement != oldBase_RedefinableElement) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, PokaYokePackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT, oldBase_RedefinableElement, base_RedefinableElement));
			}
		}
		return base_RedefinableElement;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RedefinableElement basicGetBase_RedefinableElement() {
		return base_RedefinableElement;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBase_RedefinableElement(RedefinableElement newBase_RedefinableElement) {
		RedefinableElement oldBase_RedefinableElement = base_RedefinableElement;
		base_RedefinableElement = newBase_RedefinableElement;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT, oldBase_RedefinableElement, base_RedefinableElement));
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
			eNotify(new ENotificationImpl(this, Notification.SET, PokaYokePackage.FORMAL_ELEMENT__ATOMIC, oldAtomic, atomic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case PokaYokePackage.FORMAL_ELEMENT__GUARD:
				return getGuard();
			case PokaYokePackage.FORMAL_ELEMENT__EFFECTS:
				return getEffects();
			case PokaYokePackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				if (resolve) return getBase_RedefinableElement();
				return basicGetBase_RedefinableElement();
			case PokaYokePackage.FORMAL_ELEMENT__ATOMIC:
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
			case PokaYokePackage.FORMAL_ELEMENT__GUARD:
				setGuard((String)newValue);
				return;
			case PokaYokePackage.FORMAL_ELEMENT__EFFECTS:
				setEffects((String)newValue);
				return;
			case PokaYokePackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				setBase_RedefinableElement((RedefinableElement)newValue);
				return;
			case PokaYokePackage.FORMAL_ELEMENT__ATOMIC:
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
			case PokaYokePackage.FORMAL_ELEMENT__GUARD:
				setGuard(GUARD_EDEFAULT);
				return;
			case PokaYokePackage.FORMAL_ELEMENT__EFFECTS:
				setEffects(EFFECTS_EDEFAULT);
				return;
			case PokaYokePackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				setBase_RedefinableElement((RedefinableElement)null);
				return;
			case PokaYokePackage.FORMAL_ELEMENT__ATOMIC:
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
			case PokaYokePackage.FORMAL_ELEMENT__GUARD:
				return GUARD_EDEFAULT == null ? guard != null : !GUARD_EDEFAULT.equals(guard);
			case PokaYokePackage.FORMAL_ELEMENT__EFFECTS:
				return EFFECTS_EDEFAULT == null ? effects != null : !EFFECTS_EDEFAULT.equals(effects);
			case PokaYokePackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				return base_RedefinableElement != null;
			case PokaYokePackage.FORMAL_ELEMENT__ATOMIC:
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

} //FormalElementImpl
