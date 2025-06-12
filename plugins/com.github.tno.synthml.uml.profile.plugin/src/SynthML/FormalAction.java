/**
 */
package SynthML;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.uml2.uml.CallBehaviorAction;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Formal Action</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link SynthML.FormalAction#getTemplateArguments <em>Template Arguments</em>}</li>
 *   <li>{@link SynthML.FormalAction#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}</li>
 * </ul>
 *
 * @see SynthML.SynthMLPackage#getFormalAction()
 * @model
 * @generated
 */
public interface FormalAction extends EObject {
	/**
	 * Returns the value of the '<em><b>Template Arguments</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Template Arguments</em>' attribute list.
	 * @see SynthML.SynthMLPackage#getFormalAction_TemplateArguments()
	 * @model unique="false" dataType="org.eclipse.uml2.types.String" ordered="false"
	 * @generated
	 */
	EList<String> getTemplateArguments();

	/**
	 * Returns the value of the '<em><b>Base Call Behavior Action</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Base Call Behavior Action</em>' reference.
	 * @see #setBase_CallBehaviorAction(CallBehaviorAction)
	 * @see SynthML.SynthMLPackage#getFormalAction_Base_CallBehaviorAction()
	 * @model required="true" ordered="false"
	 * @generated
	 */
	CallBehaviorAction getBase_CallBehaviorAction();

	/**
	 * Sets the value of the '{@link SynthML.FormalAction#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Base Call Behavior Action</em>' reference.
	 * @see #getBase_CallBehaviorAction()
	 * @generated
	 */
	void setBase_CallBehaviorAction(CallBehaviorAction value);

} // FormalAction
