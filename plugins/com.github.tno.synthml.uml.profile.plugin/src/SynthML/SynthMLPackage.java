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
	 * The meta object id for the '{@link SynthML.impl.FormalCallBehaviorActionImpl <em>Formal Call Behavior Action</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.FormalCallBehaviorActionImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getFormalCallBehaviorAction()
	 * @generated
	 */
	int FORMAL_CALL_BEHAVIOR_ACTION = 2;

	/**
	 * The feature id for the '<em><b>Arguments</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS = 0;

	/**
	 * The feature id for the '<em><b>Base Call Behavior Action</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION = 1;

	/**
	 * The number of structural features of the '<em>Formal Call Behavior Action</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CALL_BEHAVIOR_ACTION_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Formal Call Behavior Action</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CALL_BEHAVIOR_ACTION_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link SynthML.impl.FormalConstraintImpl <em>Formal Constraint</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.FormalConstraintImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getFormalConstraint()
	 * @generated
	 */
	int FORMAL_CONSTRAINT = 4;

	/**
	 * The feature id for the '<em><b>Base Constraint</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CONSTRAINT__BASE_CONSTRAINT = 0;

	/**
	 * The number of structural features of the '<em>Formal Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CONSTRAINT_FEATURE_COUNT = 1;

	/**
	 * The number of operations of the '<em>Formal Constraint</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FORMAL_CONSTRAINT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link SynthML.impl.RequirementImpl <em>Requirement</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.RequirementImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getRequirement()
	 * @generated
	 */
	int REQUIREMENT = 3;

	/**
	 * The feature id for the '<em><b>Base Constraint</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUIREMENT__BASE_CONSTRAINT = FORMAL_CONSTRAINT__BASE_CONSTRAINT;

	/**
	 * The number of structural features of the '<em>Requirement</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUIREMENT_FEATURE_COUNT = FORMAL_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Requirement</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REQUIREMENT_OPERATION_COUNT = FORMAL_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link SynthML.impl.SynthesisPreconditionImpl <em>Synthesis Precondition</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.SynthesisPreconditionImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getSynthesisPrecondition()
	 * @generated
	 */
	int SYNTHESIS_PRECONDITION = 5;

	/**
	 * The feature id for the '<em><b>Base Constraint</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYNTHESIS_PRECONDITION__BASE_CONSTRAINT = FORMAL_CONSTRAINT__BASE_CONSTRAINT;

	/**
	 * The number of structural features of the '<em>Synthesis Precondition</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYNTHESIS_PRECONDITION_FEATURE_COUNT = FORMAL_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Synthesis Precondition</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYNTHESIS_PRECONDITION_OPERATION_COUNT = FORMAL_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link SynthML.impl.UsagePreconditionImpl <em>Usage Precondition</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.UsagePreconditionImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getUsagePrecondition()
	 * @generated
	 */
	int USAGE_PRECONDITION = 6;

	/**
	 * The feature id for the '<em><b>Base Constraint</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int USAGE_PRECONDITION__BASE_CONSTRAINT = FORMAL_CONSTRAINT__BASE_CONSTRAINT;

	/**
	 * The number of structural features of the '<em>Usage Precondition</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int USAGE_PRECONDITION_FEATURE_COUNT = FORMAL_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Usage Precondition</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int USAGE_PRECONDITION_OPERATION_COUNT = FORMAL_CONSTRAINT_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link SynthML.impl.PostconditionImpl <em>Postcondition</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see SynthML.impl.PostconditionImpl
	 * @see SynthML.impl.SynthMLPackageImpl#getPostcondition()
	 * @generated
	 */
	int POSTCONDITION = 7;

	/**
	 * The feature id for the '<em><b>Base Constraint</b></em>' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POSTCONDITION__BASE_CONSTRAINT = FORMAL_CONSTRAINT__BASE_CONSTRAINT;

	/**
	 * The number of structural features of the '<em>Postcondition</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POSTCONDITION_FEATURE_COUNT = FORMAL_CONSTRAINT_FEATURE_COUNT + 0;

	/**
	 * The number of operations of the '<em>Postcondition</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POSTCONDITION_OPERATION_COUNT = FORMAL_CONSTRAINT_OPERATION_COUNT + 0;


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
	 * Returns the meta object for class '{@link SynthML.FormalCallBehaviorAction <em>Formal Call Behavior Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Formal Call Behavior Action</em>'.
	 * @see SynthML.FormalCallBehaviorAction
	 * @generated
	 */
	EClass getFormalCallBehaviorAction();

	/**
	 * Returns the meta object for the attribute '{@link SynthML.FormalCallBehaviorAction#getArguments <em>Arguments</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Arguments</em>'.
	 * @see SynthML.FormalCallBehaviorAction#getArguments()
	 * @see #getFormalCallBehaviorAction()
	 * @generated
	 */
	EAttribute getFormalCallBehaviorAction_Arguments();

	/**
	 * Returns the meta object for the reference '{@link SynthML.FormalCallBehaviorAction#getBase_CallBehaviorAction <em>Base Call Behavior Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Base Call Behavior Action</em>'.
	 * @see SynthML.FormalCallBehaviorAction#getBase_CallBehaviorAction()
	 * @see #getFormalCallBehaviorAction()
	 * @generated
	 */
	EReference getFormalCallBehaviorAction_Base_CallBehaviorAction();

	/**
	 * Returns the meta object for class '{@link SynthML.Requirement <em>Requirement</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Requirement</em>'.
	 * @see SynthML.Requirement
	 * @generated
	 */
	EClass getRequirement();

	/**
	 * Returns the meta object for class '{@link SynthML.FormalConstraint <em>Formal Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Formal Constraint</em>'.
	 * @see SynthML.FormalConstraint
	 * @generated
	 */
	EClass getFormalConstraint();

	/**
	 * Returns the meta object for the reference '{@link SynthML.FormalConstraint#getBase_Constraint <em>Base Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference '<em>Base Constraint</em>'.
	 * @see SynthML.FormalConstraint#getBase_Constraint()
	 * @see #getFormalConstraint()
	 * @generated
	 */
	EReference getFormalConstraint_Base_Constraint();

	/**
	 * Returns the meta object for class '{@link SynthML.SynthesisPrecondition <em>Synthesis Precondition</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Synthesis Precondition</em>'.
	 * @see SynthML.SynthesisPrecondition
	 * @generated
	 */
	EClass getSynthesisPrecondition();

	/**
	 * Returns the meta object for class '{@link SynthML.UsagePrecondition <em>Usage Precondition</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Usage Precondition</em>'.
	 * @see SynthML.UsagePrecondition
	 * @generated
	 */
	EClass getUsagePrecondition();

	/**
	 * Returns the meta object for class '{@link SynthML.Postcondition <em>Postcondition</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Postcondition</em>'.
	 * @see SynthML.Postcondition
	 * @generated
	 */
	EClass getPostcondition();

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
		 * The meta object literal for the '{@link SynthML.impl.FormalCallBehaviorActionImpl <em>Formal Call Behavior Action</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.FormalCallBehaviorActionImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getFormalCallBehaviorAction()
		 * @generated
		 */
		EClass FORMAL_CALL_BEHAVIOR_ACTION = eINSTANCE.getFormalCallBehaviorAction();

		/**
		 * The meta object literal for the '<em><b>Arguments</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS = eINSTANCE.getFormalCallBehaviorAction_Arguments();

		/**
		 * The meta object literal for the '<em><b>Base Call Behavior Action</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION = eINSTANCE.getFormalCallBehaviorAction_Base_CallBehaviorAction();

		/**
		 * The meta object literal for the '{@link SynthML.impl.RequirementImpl <em>Requirement</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.RequirementImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getRequirement()
		 * @generated
		 */
		EClass REQUIREMENT = eINSTANCE.getRequirement();

		/**
		 * The meta object literal for the '{@link SynthML.impl.FormalConstraintImpl <em>Formal Constraint</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.FormalConstraintImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getFormalConstraint()
		 * @generated
		 */
		EClass FORMAL_CONSTRAINT = eINSTANCE.getFormalConstraint();

		/**
		 * The meta object literal for the '<em><b>Base Constraint</b></em>' reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference FORMAL_CONSTRAINT__BASE_CONSTRAINT = eINSTANCE.getFormalConstraint_Base_Constraint();

		/**
		 * The meta object literal for the '{@link SynthML.impl.SynthesisPreconditionImpl <em>Synthesis Precondition</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.SynthesisPreconditionImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getSynthesisPrecondition()
		 * @generated
		 */
		EClass SYNTHESIS_PRECONDITION = eINSTANCE.getSynthesisPrecondition();

		/**
		 * The meta object literal for the '{@link SynthML.impl.UsagePreconditionImpl <em>Usage Precondition</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.UsagePreconditionImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getUsagePrecondition()
		 * @generated
		 */
		EClass USAGE_PRECONDITION = eINSTANCE.getUsagePrecondition();

		/**
		 * The meta object literal for the '{@link SynthML.impl.PostconditionImpl <em>Postcondition</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see SynthML.impl.PostconditionImpl
		 * @see SynthML.impl.SynthMLPackageImpl#getPostcondition()
		 * @generated
		 */
		EClass POSTCONDITION = eINSTANCE.getPostcondition();

	}

} //SynthMLPackage
