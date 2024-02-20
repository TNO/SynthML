/**
 */
package PokaYoke;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see PokaYoke.PokaYokeFactory
 * @model kind="package"
 * @generated
 */
public interface PokaYokePackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "PokaYoke";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "https://github.com/TNO/PokaYoke/0.0.1/";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "PokaYoke";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	PokaYokePackage eINSTANCE = PokaYoke.impl.PokaYokePackageImpl.init();

	/**
	 * The meta object id for the '{@link PokaYoke.impl.GuardEffectsActionImpl <em>Guard Effects Action</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see PokaYoke.impl.GuardEffectsActionImpl
	 * @see PokaYoke.impl.PokaYokePackageImpl#getGuardEffectsAction()
	 * @generated
	 */
	int GUARD_EFFECTS_ACTION = 0;

	/**
	 * The feature id for the '<em><b>Base Opaque Action</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION = 0;

	/**
	 * The feature id for the '<em><b>Guard</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GUARD_EFFECTS_ACTION__GUARD = 1;

	/**
	 * The feature id for the '<em><b>Effects</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GUARD_EFFECTS_ACTION__EFFECTS = 2;

	/**
	 * The number of structural features of the '<em>Guard Effects Action</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GUARD_EFFECTS_ACTION_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Guard Effects Action</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GUARD_EFFECTS_ACTION_OPERATION_COUNT = 0;


	/**
	 * Returns the meta object for class '{@link PokaYoke.GuardEffectsAction <em>Guard Effects Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Guard Effects Action</em>'.
	 * @see PokaYoke.GuardEffectsAction
	 * @generated
	 */
	EClass getGuardEffectsAction();

	/**
	 * Returns the meta object for the reference '{@link PokaYoke.GuardEffectsAction#getBase_OpaqueAction <em>Base Opaque Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Base Opaque Action</em>'.
	 * @see PokaYoke.GuardEffectsAction#getBase_OpaqueAction()
	 * @see #getGuardEffectsAction()
	 * @generated
	 */
	EReference getGuardEffectsAction_Base_OpaqueAction();

	/**
	 * Returns the meta object for the attribute '{@link PokaYoke.GuardEffectsAction#getGuard <em>Guard</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Guard</em>'.
	 * @see PokaYoke.GuardEffectsAction#getGuard()
	 * @see #getGuardEffectsAction()
	 * @generated
	 */
	EAttribute getGuardEffectsAction_Guard();

	/**
	 * Returns the meta object for the attribute '{@link PokaYoke.GuardEffectsAction#getEffects <em>Effects</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Effects</em>'.
	 * @see PokaYoke.GuardEffectsAction#getEffects()
	 * @see #getGuardEffectsAction()
	 * @generated
	 */
	EAttribute getGuardEffectsAction_Effects();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	PokaYokeFactory getPokaYokeFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link PokaYoke.impl.GuardEffectsActionImpl <em>Guard Effects Action</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see PokaYoke.impl.GuardEffectsActionImpl
		 * @see PokaYoke.impl.PokaYokePackageImpl#getGuardEffectsAction()
		 * @generated
		 */
		EClass GUARD_EFFECTS_ACTION = eINSTANCE.getGuardEffectsAction();

		/**
		 * The meta object literal for the '<em><b>Base Opaque Action</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference GUARD_EFFECTS_ACTION__BASE_OPAQUE_ACTION = eINSTANCE.getGuardEffectsAction_Base_OpaqueAction();

		/**
		 * The meta object literal for the '<em><b>Guard</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute GUARD_EFFECTS_ACTION__GUARD = eINSTANCE.getGuardEffectsAction_Guard();

		/**
		 * The meta object literal for the '<em><b>Effects</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute GUARD_EFFECTS_ACTION__EFFECTS = eINSTANCE.getGuardEffectsAction_Effects();

	}

} //PokaYokePackage
