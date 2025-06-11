/**
 */
package SynthML;

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
 * @see SynthML.SynthMLFactory
 * @model kind="package"
 * @generated
 */
public interface SynthMLPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "SynthML";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "https://github.com/TNO/SynthML/";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "SynthML";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	SynthMLPackage eINSTANCE = SynthML.impl.SynthMLPackageImpl.init();

	/**
	 * The meta object id for the '{@link SynthML.impl.FormalElementImpl <em>Formal Element</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.FormalElementImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getFormalElement()
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
	 * The feature id for the '<em><b>Effects</b></em>' attribute list.
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
	 * The meta object id for the '{@link SynthML.impl.FormalControlFlowImpl <em>Formal Control Flow</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.FormalControlFlowImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getFormalControlFlow()
	 * @generated
	 */
	int FORMAL_CONTROL_FLOW = 1;

	/**
	 * The feature id for the '<em><b>Outgoing Guard</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CONTROL_FLOW__OUTGOING_GUARD = 0;

	/**
	 * The feature id for the '<em><b>Base Control Flow</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW = 1;

	/**
	 * The number of structural features of the '<em>Formal Control Flow</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CONTROL_FLOW_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Formal Control Flow</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CONTROL_FLOW_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link SynthML.impl.FormalActionImpl <em>Formal Action</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.FormalActionImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getFormalAction()
	 * @generated
	 */
	int FORMAL_ACTION = 2;

	/**
	 * The feature id for the '<em><b>Template Parameters</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ACTION__TEMPLATE_PARAMETERS = 0;

	/**
	 * The feature id for the '<em><b>Base Call Behavior Action</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ACTION__BASE_CALL_BEHAVIOR_ACTION = 1;

	/**
	 * The number of structural features of the '<em>Formal Action</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ACTION_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Formal Action</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_ACTION_OPERATION_COUNT = 0;


	/**
	 * Returns the meta object for class '{@link SynthML.FormalElement <em>Formal Element</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Formal Element</em>'.
	 * @see SynthML.FormalElement
	 * @generated
	 */
	EClass getFormalElement();

	/**
	 * Returns the meta object for the attribute '{@link SynthML.FormalElement#getGuard <em>Guard</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Guard</em>'.
	 * @see SynthML.FormalElement#getGuard()
	 * @see #getFormalElement()
	 * @generated
	 */
	EAttribute getFormalElement_Guard();

	/**
	 * Returns the meta object for the attribute list '{@link SynthML.FormalElement#getEffects <em>Effects</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Effects</em>'.
	 * @see SynthML.FormalElement#getEffects()
	 * @see #getFormalElement()
	 * @generated
	 */
	EAttribute getFormalElement_Effects();

	/**
	 * Returns the meta object for the reference '{@link SynthML.FormalElement#getBase_RedefinableElement <em>Base Redefinable Element</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Base Redefinable Element</em>'.
	 * @see SynthML.FormalElement#getBase_RedefinableElement()
	 * @see #getFormalElement()
	 * @generated
	 */
	EReference getFormalElement_Base_RedefinableElement();

	/**
	 * Returns the meta object for the attribute '{@link SynthML.FormalElement#isAtomic <em>Atomic</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Atomic</em>'.
	 * @see SynthML.FormalElement#isAtomic()
	 * @see #getFormalElement()
	 * @generated
	 */
	EAttribute getFormalElement_Atomic();

	/**
	 * Returns the meta object for class '{@link SynthML.FormalControlFlow <em>Formal Control Flow</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Formal Control Flow</em>'.
	 * @see SynthML.FormalControlFlow
	 * @generated
	 */
	EClass getFormalControlFlow();

	/**
	 * Returns the meta object for the attribute '{@link SynthML.FormalControlFlow#getOutgoingGuard <em>Outgoing Guard</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Outgoing Guard</em>'.
	 * @see SynthML.FormalControlFlow#getOutgoingGuard()
	 * @see #getFormalControlFlow()
	 * @generated
	 */
	EAttribute getFormalControlFlow_OutgoingGuard();

	/**
	 * Returns the meta object for the reference '{@link SynthML.FormalControlFlow#getBase_ControlFlow <em>Base Control Flow</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Base Control Flow</em>'.
	 * @see SynthML.FormalControlFlow#getBase_ControlFlow()
	 * @see #getFormalControlFlow()
	 * @generated
	 */
	EReference getFormalControlFlow_Base_ControlFlow();

	/**
	 * Returns the meta object for class '{@link SynthML.FormalAction <em>Formal Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Formal Action</em>'.
	 * @see SynthML.FormalAction
	 * @generated
	 */
	EClass getFormalAction();

	/**
	 * Returns the meta object for the attribute list '{@link SynthML.FormalAction#getTemplateParameters <em>Template Parameters</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Template Parameters</em>'.
	 * @see SynthML.FormalAction#getTemplateParameters()
	 * @see #getFormalAction()
	 * @generated
	 */
	EAttribute getFormalAction_TemplateParameters();

	/**
	 * Returns the meta object for the reference '{@link SynthML.FormalAction#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Base Call Behavior Action</em>'.
	 * @see SynthML.FormalAction#getBase_CallBehaviorAction()
	 * @see #getFormalAction()
	 * @generated
	 */
	EReference getFormalAction_Base_CallBehaviorAction();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	SynthMLFactory getSynthMLFactory();

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
		 * The meta object literal for the '{@link SynthML.impl.FormalElementImpl <em>Formal Element</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.FormalElementImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getFormalElement()
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
		 * The meta object literal for the '<em><b>Effects</b></em>' attribute list feature.
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

		/**
		 * The meta object literal for the '{@link SynthML.impl.FormalControlFlowImpl <em>Formal Control Flow</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.FormalControlFlowImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getFormalControlFlow()
		 * @generated
		 */
		EClass FORMAL_CONTROL_FLOW = eINSTANCE.getFormalControlFlow();

		/**
		 * The meta object literal for the '<em><b>Outgoing Guard</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FORMAL_CONTROL_FLOW__OUTGOING_GUARD = eINSTANCE.getFormalControlFlow_OutgoingGuard();

		/**
		 * The meta object literal for the '<em><b>Base Control Flow</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW = eINSTANCE.getFormalControlFlow_Base_ControlFlow();

		/**
		 * The meta object literal for the '{@link SynthML.impl.FormalActionImpl <em>Formal Action</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.FormalActionImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getFormalAction()
		 * @generated
		 */
		EClass FORMAL_ACTION = eINSTANCE.getFormalAction();

		/**
		 * The meta object literal for the '<em><b>Template Parameters</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FORMAL_ACTION__TEMPLATE_PARAMETERS = eINSTANCE.getFormalAction_TemplateParameters();

		/**
		 * The meta object literal for the '<em><b>Base Call Behavior Action</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FORMAL_ACTION__BASE_CALL_BEHAVIOR_ACTION = eINSTANCE.getFormalAction_Base_CallBehaviorAction();

	}

} //SynthMLPackage
