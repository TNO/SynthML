/**
 */
package SynthML;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.uml2.uml.ControlFlow;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Formal Control Flow</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link SynthML.FormalControlFlow#getOutgoingGuard <em>Outgoing Guard</em>}</li>
 *   <li>{@link SynthML.FormalControlFlow#getBase_ControlFlow <em>Base Control Flow</em>}</li>
 * </ul>
 *
 * @see SynthML.SynthMLPackage#getFormalControlFlow()
 * @model
 * @generated
 */
public interface FormalControlFlow extends EObject {
	/**
	 * Returns the value of the '<em><b>Outgoing Guard</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Outgoing Guard</em>' attribute.
	 * @see #setOutgoingGuard(String)
	 * @see SynthML.SynthMLPackage#getFormalControlFlow_OutgoingGuard()
	 * @model dataType="org.eclipse.uml2.types.String" ordered="false"
	 * @generated
	 */
	String getOutgoingGuard();

	/**
	 * Sets the value of the '{@link SynthML.FormalControlFlow#getOutgoingGuard <em>Outgoing Guard</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Outgoing Guard</em>' attribute.
	 * @see #getOutgoingGuard()
	 * @generated
	 */
	void setOutgoingGuard(String value);

	/**
	 * Returns the value of the '<em><b>Base Control Flow</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Base Control Flow</em>' reference.
	 * @see #setBase_ControlFlow(ControlFlow)
	 * @see SynthML.SynthMLPackage#getFormalControlFlow_Base_ControlFlow()
	 * @model required="true" ordered="false"
	 * @generated
	 */
	ControlFlow getBase_ControlFlow();

	/**
	 * Sets the value of the '{@link SynthML.FormalControlFlow#getBase_ControlFlow <em>Base Control Flow</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Base Control Flow</em>' reference.
	 * @see #getBase_ControlFlow()
	 * @generated
	 */
	void setBase_ControlFlow(ControlFlow value);

} // FormalControlFlow
