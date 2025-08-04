/**
 */
package SynthML.impl;

import SynthML.FormalCallBehaviorAction;
import SynthML.SynthMLPackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeEList;

import org.eclipse.uml2.uml.CallBehaviorAction;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Formal Call Behavior Action</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link SynthML.impl.FormalCallBehaviorActionImpl#getActivityArguments <em>Activity Arguments</em>}</li>
 *   <li>{@link SynthML.impl.FormalCallBehaviorActionImpl#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}</li>
 * </ul>
 *
 * @generated
 */
public class FormalCallBehaviorActionImpl extends MinimalEObjectImpl.Container implements FormalCallBehaviorAction {
	/**
	 * The cached value of the '{@link #getActivityArguments() <em>Activity Arguments</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getActivityArguments()
	 * @generated
	 * @ordered
	 */
	protected EList<String> activityArguments;

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
	public EList<String> getActivityArguments() {
		if (activityArguments == null) {
			activityArguments = new EDataTypeEList<String>(String.class, this, SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ACTIVITY_ARGUMENTS);
		}
		return activityArguments;
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
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ACTIVITY_ARGUMENTS:
				return getActivityArguments();
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
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ACTIVITY_ARGUMENTS:
				getActivityArguments().clear();
				getActivityArguments().addAll((Collection<? extends String>)newValue);
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
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ACTIVITY_ARGUMENTS:
				getActivityArguments().clear();
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
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION__ACTIVITY_ARGUMENTS:
				return activityArguments != null && !activityArguments.isEmpty();
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
		result.append(" (activityArguments: ");
		result.append(activityArguments);
		result.append(')');
		return result.toString();
	}

} //FormalCallBehaviorActionImpl
