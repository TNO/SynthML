/**
 */
package PokaYoke;

import org.eclipse.emf.ecore.EObject;

import org.eclipse.uml2.uml.Action;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Guard Effects Action</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link PokaYoke.GuardEffectsAction#getGuard <em>Guard</em>}</li>
 *   <li>{@link PokaYoke.GuardEffectsAction#getEffects <em>Effects</em>}</li>
 *   <li>{@link PokaYoke.GuardEffectsAction#getBase_Action <em>Base Action</em>}</li>
 * </ul>
 *
 * @see PokaYoke.PokaYokePackage#getGuardEffectsAction()
 * @model
 * @generated
 */
public interface GuardEffectsAction extends EObject {
	/**
	 * Returns the value of the '<em><b>Guard</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Guard</em>' attribute.
	 * @see #setGuard(String)
	 * @see PokaYoke.PokaYokePackage#getGuardEffectsAction_Guard()
	 * @model dataType="org.eclipse.uml2.types.String" ordered="false"
	 * @generated
	 */
	String getGuard();

	/**
	 * Sets the value of the '{@link PokaYoke.GuardEffectsAction#getGuard <em>Guard</em>}' attribute.
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
	 * @see PokaYoke.PokaYokePackage#getGuardEffectsAction_Effects()
	 * @model unique="false" dataType="org.eclipse.uml2.types.String" ordered="false"
	 * @generated
	 */
	String getEffects();

	/**
	 * Sets the value of the '{@link PokaYoke.GuardEffectsAction#getEffects <em>Effects</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Effects</em>' attribute.
	 * @see #getEffects()
	 * @generated
	 */
	void setEffects(String value);

	/**
	 * Returns the value of the '<em><b>Base Action</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Base Action</em>' reference.
	 * @see #setBase_Action(Action)
	 * @see PokaYoke.PokaYokePackage#getGuardEffectsAction_Base_Action()
	 * @model required="true" ordered="false"
	 * @generated
	 */
	Action getBase_Action();

	/**
	 * Sets the value of the '{@link PokaYoke.GuardEffectsAction#getBase_Action <em>Base Action</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Base Action</em>' reference.
	 * @see #getBase_Action()
	 * @generated
	 */
	void setBase_Action(Action value);

} // GuardEffectsAction
