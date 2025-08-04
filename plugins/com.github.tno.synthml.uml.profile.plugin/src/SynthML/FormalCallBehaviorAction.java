/**
 */
package SynthML;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.uml2.uml.CallBehaviorAction;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Formal Call Behavior Action</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link SynthML.FormalCallBehaviorAction#getActivityArguments <em>Activity Arguments</em>}</li>
 *   <li>{@link SynthML.FormalCallBehaviorAction#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}</li>
 * </ul>
 *
 * @see SynthML.SynthMLPackage#getFormalCallBehaviorAction()
 * @model
 * @generated
 */
public interface FormalCallBehaviorAction extends EObject {
	/**
	 * Returns the value of the '<em><b>Activity Arguments</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Activity Arguments</em>' attribute list.
	 * @see SynthML.SynthMLPackage#getFormalCallBehaviorAction_ActivityArguments()
	 * @model unique="false" dataType="org.eclipse.uml2.types.String"
	 * @generated
	 */
	EList<String> getActivityArguments();

	/**
	 * Returns the value of the '<em><b>Base Call Behavior Action</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Base Call Behavior Action</em>' reference.
	 * @see #setBase_CallBehaviorAction(CallBehaviorAction)
	 * @see SynthML.SynthMLPackage#getFormalCallBehaviorAction_Base_CallBehaviorAction()
	 * @model required="true" ordered="false"
	 * @generated
	 */
	CallBehaviorAction getBase_CallBehaviorAction();

	/**
	 * Sets the value of the '{@link SynthML.FormalCallBehaviorAction#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Base Call Behavior Action</em>' reference.
	 * @see #getBase_CallBehaviorAction()
	 * @generated
	 */
	void setBase_CallBehaviorAction(CallBehaviorAction value);

} // FormalCallBehaviorAction
