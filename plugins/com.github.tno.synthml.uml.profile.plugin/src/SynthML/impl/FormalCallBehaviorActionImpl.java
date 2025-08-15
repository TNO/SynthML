/**
 */
package SynthML.impl;

import SynthML.FormalCallBehaviorAction;
import SynthML.SynthMLPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.uml2.uml.CallBehaviorAction;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Formal Call Behavior Action</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link SynthML.impl.FormalCallBehaviorActionImpl#getArguments <em>Arguments</em>}</li>
 *   <li>{@link SynthML.impl.FormalCallBehaviorActionImpl#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}</li>
 * </ul>
 *
 * @generated
 */
public class FormalCallBehaviorActionImpl extends MinimalEObjectImpl.Container implements FormalCallBehaviorAction {
	/**
	 * The default value of the '{@link #getArguments() <em>Arguments</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getArguments()
	 * @generated
	 * @ordered
	 */
	protected static final String ARGUMENTS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getArguments() <em>Arguments</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getArguments()
	 * @generated
	 * @ordered
	 */
	protected String arguments = ARGUMENTS_EDEFAULT;

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
	protected FormalCallBehaviorActionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return SynthMLPackage.Literals.FORMAL_CALL_BEHAVIOR_ACTION;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getArguments() {
		return arguments;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setArguments(String newArguments) {
		String oldArguments = arguments;
		arguments = newArguments;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS, oldArguments, arguments));
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
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION, oldBase_CallBehaviorAction, base_CallBehaviorAction));
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
			eNotify(new ENotificationImpl(this, Notification.SET, SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION, oldBase_CallBehaviorAction, base_CallBehaviorAction));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS:
				return getArguments();
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION:
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
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS:
				setArguments((String)newValue);
				return;
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION:
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
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS:
				setArguments(ARGUMENTS_EDEFAULT);
				return;
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION:
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
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS:
				return ARGUMENTS_EDEFAULT == null ? arguments != null : !ARGUMENTS_EDEFAULT.equals(arguments);
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION:
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
		result.append(" (arguments: ");
		result.append(arguments);
		result.append(')');
		return result.toString();
	}

} //FormalCallBehaviorActionImpl
