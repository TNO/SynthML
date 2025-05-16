/**
 */
package SynthML.impl;

import SynthML.FormalElement;
import SynthML.SynthMLPackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeEList;

import org.eclipse.uml2.uml.RedefinableElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Formal Element</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link SynthML.impl.FormalElementImpl#getGuard <em>Guard</em>}</li>
 *   <li>{@link SynthML.impl.FormalElementImpl#getEffects <em>Effects</em>}</li>
 *   <li>{@link SynthML.impl.FormalElementImpl#getBase_RedefinableElement <em>Base Redefinable Element</em>}</li>
 *   <li>{@link SynthML.impl.FormalElementImpl#isAtomic <em>Atomic</em>}</li>
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
	 * The cached value of the '{@link #getEffects() <em>Effects</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEffects()
	 * @generated
	 * @ordered
	 */
	protected EList<String> effects;

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
		return SynthMLPackage.Literals.FORMAL_ELEMENT;
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
			eNotify(new ENotificationImpl(this, Notification.SET, SynthMLPackage.FORMAL_ELEMENT__GUARD, oldGuard, guard));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getEffects() {
		if (effects == null) {
			effects = new EDataTypeEList<String>(String.class, this, SynthMLPackage.FORMAL_ELEMENT__EFFECTS);
		}
		return effects;
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
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, SynthMLPackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT, oldBase_RedefinableElement, base_RedefinableElement));
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
			eNotify(new ENotificationImpl(this, Notification.SET, SynthMLPackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT, oldBase_RedefinableElement, base_RedefinableElement));
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
			eNotify(new ENotificationImpl(this, Notification.SET, SynthMLPackage.FORMAL_ELEMENT__ATOMIC, oldAtomic, atomic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case SynthMLPackage.FORMAL_ELEMENT__GUARD:
				return getGuard();
			case SynthMLPackage.FORMAL_ELEMENT__EFFECTS:
				return getEffects();
			case SynthMLPackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				if (resolve) return getBase_RedefinableElement();
				return basicGetBase_RedefinableElement();
			case SynthMLPackage.FORMAL_ELEMENT__ATOMIC:
				return isAtomic();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case SynthMLPackage.FORMAL_ELEMENT__GUARD:
				setGuard((String)newValue);
				return;
			case SynthMLPackage.FORMAL_ELEMENT__EFFECTS:
				getEffects().clear();
				getEffects().addAll((Collection<? extends String>)newValue);
				return;
			case SynthMLPackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				setBase_RedefinableElement((RedefinableElement)newValue);
				return;
			case SynthMLPackage.FORMAL_ELEMENT__ATOMIC:
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
			case SynthMLPackage.FORMAL_ELEMENT__GUARD:
				setGuard(GUARD_EDEFAULT);
				return;
			case SynthMLPackage.FORMAL_ELEMENT__EFFECTS:
				getEffects().clear();
				return;
			case SynthMLPackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				setBase_RedefinableElement((RedefinableElement)null);
				return;
			case SynthMLPackage.FORMAL_ELEMENT__ATOMIC:
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
			case SynthMLPackage.FORMAL_ELEMENT__GUARD:
				return GUARD_EDEFAULT == null ? guard != null : !GUARD_EDEFAULT.equals(guard);
			case SynthMLPackage.FORMAL_ELEMENT__EFFECTS:
				return effects != null && !effects.isEmpty();
			case SynthMLPackage.FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT:
				return base_RedefinableElement != null;
			case SynthMLPackage.FORMAL_ELEMENT__ATOMIC:
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
