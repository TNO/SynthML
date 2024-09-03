/**
 */
package PokaYoke;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.uml2.uml.RedefinableElement;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Formal Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link PokaYoke.FormalElement#getGuard <em>Guard</em>}</li>
 *   <li>{@link PokaYoke.FormalElement#getEffects <em>Effects</em>}</li>
 *   <li>{@link PokaYoke.FormalElement#getBase_RedefinableElement <em>Base Redefinable Element</em>}</li>
 *   <li>{@link PokaYoke.FormalElement#isAtomic <em>Atomic</em>}</li>
 * </ul>
 *
 * @see PokaYoke.PokaYokePackage#getFormalElement()
 * @model
 * @generated
 */
public interface FormalElement extends EObject {
	/**
	 * Returns the value of the '<em><b>Guard</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Guard</em>' attribute.
	 * @see #setGuard(String)
	 * @see PokaYoke.PokaYokePackage#getFormalElement_Guard()
	 * @model dataType="org.eclipse.uml2.types.String" ordered="false"
	 * @generated
	 */
	String getGuard();

	/**
	 * Sets the value of the '{@link PokaYoke.FormalElement#getGuard <em>Guard</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Guard</em>' attribute.
	 * @see #getGuard()
	 * @generated
	 */
	void setGuard(String value);

	/**
	 * Returns the value of the '<em><b>Effects</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Effects</em>' attribute.
	 * @see #setEffects(String)
	 * @see PokaYoke.PokaYokePackage#getFormalElement_Effects()
	 * @model unique="false" dataType="org.eclipse.uml2.types.String" ordered="false"
	 * @generated
	 */
	String getEffects();

	/**
	 * Sets the value of the '{@link PokaYoke.FormalElement#getEffects <em>Effects</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Effects</em>' attribute.
	 * @see #getEffects()
	 * @generated
	 */
	void setEffects(String value);

	/**
	 * Returns the value of the '<em><b>Base Redefinable Element</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Base Redefinable Element</em>' reference.
	 * @see #setBase_RedefinableElement(RedefinableElement)
	 * @see PokaYoke.PokaYokePackage#getFormalElement_Base_RedefinableElement()
	 * @model required="true" ordered="false"
	 * @generated
	 */
	RedefinableElement getBase_RedefinableElement();

	/**
	 * Sets the value of the '{@link PokaYoke.FormalElement#getBase_RedefinableElement <em>Base Redefinable Element</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Base Redefinable Element</em>' reference.
	 * @see #getBase_RedefinableElement()
	 * @generated
	 */
	void setBase_RedefinableElement(RedefinableElement value);

	/**
	 * Returns the value of the '<em><b>Atomic</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Atomic</em>' attribute.
	 * @see #setAtomic(boolean)
	 * @see PokaYoke.PokaYokePackage#getFormalElement_Atomic()
	 * @model default="false" dataType="org.eclipse.uml2.types.Boolean" ordered="false"
	 * @generated
	 */
	boolean isAtomic();

	/**
	 * Sets the value of the '{@link PokaYoke.FormalElement#isAtomic <em>Atomic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Atomic</em>' attribute.
	 * @see #isAtomic()
	 * @generated
	 */
	void setAtomic(boolean value);

} // FormalElement
