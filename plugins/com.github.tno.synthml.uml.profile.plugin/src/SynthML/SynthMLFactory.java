/**
 */
package SynthML;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see SynthML.SynthMLPackage
 * @generated
 */
public interface SynthMLFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	SynthMLFactory eINSTANCE = SynthML.impl.SynthMLFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Formal Element</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Formal Element</em>'.
	 * @generated
	 */
	FormalElement createFormalElement();

	/**
	 * Returns a new object of class '<em>Formal Control Flow</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Formal Control Flow</em>'.
	 * @generated
	 */
	FormalControlFlow createFormalControlFlow();

	/**
	 * Returns a new object of class '<em>Formal Call Behavior Action</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Formal Call Behavior Action</em>'.
	 * @generated
	 */
	FormalCallBehaviorAction createFormalCallBehaviorAction();

	/**
	 * Returns a new object of class '<em>Requirement</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Requirement</em>'.
	 * @generated
	 */
	Requirement createRequirement();

	/**
	 * Returns a new object of class '<em>Synthesis Precondition</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Synthesis Precondition</em>'.
	 * @generated
	 */
	SynthesisPrecondition createSynthesisPrecondition();

	/**
	 * Returns a new object of class '<em>Usage Precondition</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Usage Precondition</em>'.
	 * @generated
	 */
	UsagePrecondition createUsagePrecondition();

	/**
	 * Returns a new object of class '<em>Postcondition</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Postcondition</em>'.
	 * @generated
	 */
	Postcondition createPostcondition();

	/**
	 * Returns a new object of class '<em>Formal Activity</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Formal Activity</em>'.
	 * @generated
	 */
	FormalActivity createFormalActivity();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	SynthMLPackage getSynthMLPackage();

} //SynthMLFactory
