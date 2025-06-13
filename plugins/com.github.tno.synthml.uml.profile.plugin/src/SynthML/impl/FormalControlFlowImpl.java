/**
 */
package SynthML.impl;

import SynthML.FormalControlFlow;
import SynthML.SynthMLPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.uml2.uml.ControlFlow;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Formal Control Flow</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link SynthML.impl.FormalControlFlowImpl#getOutgoingGuard <em>Outgoing Guard</em>}</li>
 *   <li>{@link SynthML.impl.FormalControlFlowImpl#getBase_ControlFlow <em>Base Control Flow</em>}</li>
 * </ul>
 *
 * @generated
 */
public class FormalControlFlowImpl extends MinimalEObjectImpl.Container implements FormalControlFlow {
	/**
	 * The default value of the '{@link #getOutgoingGuard() <em>Outgoing Guard</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutgoingGuard()
	 * @generated
	 * @ordered
	 */
	protected static final String OUTGOING_GUARD_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getOutgoingGuard() <em>Outgoing Guard</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutgoingGuard()
	 * @generated
	 * @ordered
	 */
	protected String outgoingGuard = OUTGOING_GUARD_EDEFAULT;

	/**
	 * The cached value of the '{@link #getBase_ControlFlow() <em>Base Control Flow</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBase_ControlFlow()
	 * @generated
	 * @ordered
	 */
	protected ControlFlow base_ControlFlow;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected FormalControlFlowImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return SynthMLPackage.Literals.FORMAL_CONTROL_FLOW;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getOutgoingGuard() {
		return outgoingGuard;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOutgoingGuard(String newOutgoingGuard) {
		String oldOutgoingGuard = outgoingGuard;
		outgoingGuard = newOutgoingGuard;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SynthMLPackage.FORMAL_CONTROL_FLOW__OUTGOING_GUARD, oldOutgoingGuard, outgoingGuard));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public ControlFlow getBase_ControlFlow() {
		if (base_ControlFlow != null && base_ControlFlow.eIsProxy()) {
			InternalEObject oldBase_ControlFlow = (InternalEObject)base_ControlFlow;
			base_ControlFlow = (ControlFlow)eResolveProxy(oldBase_ControlFlow);
			if (base_ControlFlow != oldBase_ControlFlow) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, SynthMLPackage.FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW, oldBase_ControlFlow, base_ControlFlow));
			}
		}
		return base_ControlFlow;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ControlFlow basicGetBase_ControlFlow() {
		return base_ControlFlow;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBase_ControlFlow(ControlFlow newBase_ControlFlow) {
		ControlFlow oldBase_ControlFlow = base_ControlFlow;
		base_ControlFlow = newBase_ControlFlow;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, SynthMLPackage.FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW, oldBase_ControlFlow, base_ControlFlow));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case SynthMLPackage.FORMAL_CONTROL_FLOW__OUTGOING_GUARD:
				return getOutgoingGuard();
			case SynthMLPackage.FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW:
				if (resolve) return getBase_ControlFlow();
				return basicGetBase_ControlFlow();
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
			case SynthMLPackage.FORMAL_CONTROL_FLOW__OUTGOING_GUARD:
				setOutgoingGuard((String)newValue);
				return;
			case SynthMLPackage.FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW:
				setBase_ControlFlow((ControlFlow)newValue);
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
			case SynthMLPackage.FORMAL_CONTROL_FLOW__OUTGOING_GUARD:
				setOutgoingGuard(OUTGOING_GUARD_EDEFAULT);
				return;
			case SynthMLPackage.FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW:
				setBase_ControlFlow((ControlFlow)null);
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
			case SynthMLPackage.FORMAL_CONTROL_FLOW__OUTGOING_GUARD:
				return OUTGOING_GUARD_EDEFAULT == null ? outgoingGuard != null : !OUTGOING_GUARD_EDEFAULT.equals(outgoingGuard);
			case SynthMLPackage.FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW:
				return base_ControlFlow != null;
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
		result.append(" (outgoingGuard: ");
		result.append(outgoingGuard);
		result.append(')');
		return result.toString();
	}

} //FormalControlFlowImpl
