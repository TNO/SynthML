/**
 */
package PokaYoke;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see PokaYoke.PokaYokePackage
 * @generated
 */
public interface PokaYokeFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	PokaYokeFactory eINSTANCE = PokaYoke.impl.PokaYokeFactoryImpl.init();

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
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	PokaYokePackage getPokaYokePackage();

} //PokaYokeFactory
