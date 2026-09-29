/**
 */
package SynthML;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.uml2.uml.Activity;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Formal Activity</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link SynthML.FormalActivity#getBase_Activity <em>Base Activity</em>}</li>
 *   <li>{@link SynthML.FormalActivity#isInterface <em>Interface</em>}</li>
 * </ul>
 *
 * @see SynthML.SynthMLPackage#getFormalActivity()
 * @model
 * @generated
 */
public interface FormalActivity extends EObject {
	/**
	 * Returns the value of the '<em><b>Base Activity</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Base Activity</em>' reference.
	 * @see #setBase_Activity(Activity)
	 * @see SynthML.SynthMLPackage#getFormalActivity_Base_Activity()
	 * @model required="true" ordered="false"
	 * @generated
	 */
	Activity getBase_Activity();

	/**
	 * Sets the value of the '{@link SynthML.FormalActivity#getBase_Activity <em>Base Activity</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Base Activity</em>' reference.
	 * @see #getBase_Activity()
	 * @generated
	 */
	void setBase_Activity(Activity value);

	/**
	 * Returns the value of the '<em><b>Interface</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Interface</em>' attribute.
	 * @see #setInterface(boolean)
	 * @see SynthML.SynthMLPackage#getFormalActivity_Interface()
	 * @model default="false" dataType="org.eclipse.uml2.types.Boolean" ordered="false"
	 * @generated
	 */
	boolean isInterface();

	/**
	 * Sets the value of the '{@link SynthML.FormalActivity#isInterface <em>Interface</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Interface</em>' attribute.
	 * @see #isInterface()
	 * @generated
	 */
	void setInterface(boolean value);

} // FormalActivity
