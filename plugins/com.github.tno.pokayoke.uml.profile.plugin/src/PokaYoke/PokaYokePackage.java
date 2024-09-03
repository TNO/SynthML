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
	 * The meta object id for the '{@link PokaYoke.impl.FormalElementImpl <em>Formal Element</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see PokaYoke.impl.FormalElementImpl
	 * @see PokaYoke.impl.PokaYokePackageImpl#getFormalElement()
	 * @generated
	 */
	int FORMAL_ELEMENT = 0;

	/**
	 * The feature id for the '<em><b>Guard</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ELEMENT__GUARD = 0;

	/**
	 * The feature id for the '<em><b>Effects</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ELEMENT__EFFECTS = 1;

	/**
	 * The feature id for the '<em><b>Base Redefinable Element</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT = 2;

	/**
	 * The feature id for the '<em><b>Atomic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ELEMENT__ATOMIC = 3;

	/**
	 * The number of structural features of the '<em>Formal Element</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ELEMENT_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Formal Element</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ELEMENT_OPERATION_COUNT = 0;


	/**
	 * Returns the meta object for class '{@link PokaYoke.FormalElement <em>Formal Element</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Formal Element</em>'.
	 * @see PokaYoke.FormalElement
	 * @generated
	 */
	EClass getFormalElement();

	/**
	 * Returns the meta object for the attribute '{@link PokaYoke.FormalElement#getGuard <em>Guard</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Guard</em>'.
	 * @see PokaYoke.FormalElement#getGuard()
	 * @see #getFormalElement()
	 * @generated
	 */
	EAttribute getFormalElement_Guard();

	/**
	 * Returns the meta object for the attribute '{@link PokaYoke.FormalElement#getEffects <em>Effects</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Effects</em>'.
	 * @see PokaYoke.FormalElement#getEffects()
	 * @see #getFormalElement()
	 * @generated
	 */
	EAttribute getFormalElement_Effects();

	/**
	 * Returns the meta object for the reference '{@link PokaYoke.FormalElement#getBase_RedefinableElement <em>Base Redefinable Element</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Base Redefinable Element</em>'.
	 * @see PokaYoke.FormalElement#getBase_RedefinableElement()
	 * @see #getFormalElement()
	 * @generated
	 */
	EReference getFormalElement_Base_RedefinableElement();

	/**
	 * Returns the meta object for the attribute '{@link PokaYoke.FormalElement#isAtomic <em>Atomic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Atomic</em>'.
	 * @see PokaYoke.FormalElement#isAtomic()
	 * @see #getFormalElement()
	 * @generated
	 */
	EAttribute getFormalElement_Atomic();

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
		 * The meta object literal for the '{@link PokaYoke.impl.FormalElementImpl <em>Formal Element</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see PokaYoke.impl.FormalElementImpl
		 * @see PokaYoke.impl.PokaYokePackageImpl#getFormalElement()
		 * @generated
		 */
		EClass FORMAL_ELEMENT = eINSTANCE.getFormalElement();

		/**
		 * The meta object literal for the '<em><b>Guard</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FORMAL_ELEMENT__GUARD = eINSTANCE.getFormalElement_Guard();

		/**
		 * The meta object literal for the '<em><b>Effects</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FORMAL_ELEMENT__EFFECTS = eINSTANCE.getFormalElement_Effects();

		/**
		 * The meta object literal for the '<em><b>Base Redefinable Element</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT = eINSTANCE.getFormalElement_Base_RedefinableElement();

		/**
		 * The meta object literal for the '<em><b>Atomic</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FORMAL_ELEMENT__ATOMIC = eINSTANCE.getFormalElement_Atomic();

	}

} //PokaYokePackage
