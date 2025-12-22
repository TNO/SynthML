/**
 */
package SynthML.impl;

import SynthML.FormalCallBehaviorAction;
import SynthML.FormalConstraint;
import SynthML.FormalControlFlow;
import SynthML.FormalElement;
import SynthML.Occurrence;
import SynthML.Postcondition;
import SynthML.Requirement;
import SynthML.SynthMLFactory;
import SynthML.SynthMLPackage;
import SynthML.SynthesisPrecondition;
import SynthML.UsagePrecondition;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;

import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.eclipse.uml2.types.TypesPackage;

import org.eclipse.uml2.uml.UMLPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class SynthMLPackageImpl extends EPackageImpl implements SynthMLPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass formalElementEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass formalControlFlowEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass formalCallBehaviorActionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass requirementEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass formalConstraintEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass synthesisPreconditionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass usagePreconditionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass postconditionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass occurrenceEClass = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see SynthML.SynthMLPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private SynthMLPackageImpl() {
		super(eNS_URI, SynthMLFactory.eINSTANCE);
	}
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 *
	 * <p>This method is used to initialize {@link SynthMLPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static SynthMLPackage init() {
		if (isInited) return (SynthMLPackage)EPackage.Registry.INSTANCE.getEPackage(SynthMLPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredSynthMLPackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		SynthMLPackageImpl theSynthMLPackage = registeredSynthMLPackage instanceof SynthMLPackageImpl ? (SynthMLPackageImpl)registeredSynthMLPackage : new SynthMLPackageImpl();

		isInited = true;

		// Initialize simple dependencies
		TypesPackage.eINSTANCE.eClass();
		UMLPackage.eINSTANCE.eClass();
		EcorePackage.eINSTANCE.eClass();

		// Create package meta-data objects
		theSynthMLPackage.createPackageContents();

		// Initialize created meta-data
		theSynthMLPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theSynthMLPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(SynthMLPackage.eNS_URI, theSynthMLPackage);
		return theSynthMLPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFormalElement() {
		return formalElementEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFormalElement_Guard() {
		return (EAttribute)formalElementEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFormalElement_Effects() {
		return (EAttribute)formalElementEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFormalElement_Base_RedefinableElement() {
		return (EReference)formalElementEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFormalElement_Atomic() {
		return (EAttribute)formalElementEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFormalControlFlow() {
		return formalControlFlowEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFormalControlFlow_OutgoingGuard() {
		return (EAttribute)formalControlFlowEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFormalControlFlow_Base_ControlFlow() {
		return (EReference)formalControlFlowEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFormalCallBehaviorAction() {
		return formalCallBehaviorActionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFormalCallBehaviorAction_Arguments() {
		return (EAttribute)formalCallBehaviorActionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFormalCallBehaviorAction_Base_CallBehaviorAction() {
		return (EReference)formalCallBehaviorActionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getRequirement() {
		return requirementEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFormalConstraint() {
		return formalConstraintEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFormalConstraint_Base_Constraint() {
		return (EReference)formalConstraintEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getSynthesisPrecondition() {
		return synthesisPreconditionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getUsagePrecondition() {
		return usagePreconditionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getPostcondition() {
		return postconditionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getOccurrence() {
		return occurrenceEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SynthMLFactory getSynthMLFactory() {
		return (SynthMLFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		formalElementEClass = createEClass(FORMAL_ELEMENT);
		createEAttribute(formalElementEClass, FORMAL_ELEMENT__GUARD);
		createEAttribute(formalElementEClass, FORMAL_ELEMENT__EFFECTS);
		createEReference(formalElementEClass, FORMAL_ELEMENT__BASE_REDEFINABLE_ELEMENT);
		createEAttribute(formalElementEClass, FORMAL_ELEMENT__ATOMIC);

		formalControlFlowEClass = createEClass(FORMAL_CONTROL_FLOW);
		createEAttribute(formalControlFlowEClass, FORMAL_CONTROL_FLOW__OUTGOING_GUARD);
		createEReference(formalControlFlowEClass, FORMAL_CONTROL_FLOW__BASE_CONTROL_FLOW);

		formalCallBehaviorActionEClass = createEClass(FORMAL_CALL_BEHAVIOR_ACTION);
		createEAttribute(formalCallBehaviorActionEClass, FORMAL_CALL_BEHAVIOR_ACTION__ARGUMENTS);
		createEReference(formalCallBehaviorActionEClass, FORMAL_CALL_BEHAVIOR_ACTION__BASE_CALL_BEHAVIOR_ACTION);

		requirementEClass = createEClass(REQUIREMENT);

		formalConstraintEClass = createEClass(FORMAL_CONSTRAINT);
		createEReference(formalConstraintEClass, FORMAL_CONSTRAINT__BASE_CONSTRAINT);

		synthesisPreconditionEClass = createEClass(SYNTHESIS_PRECONDITION);

		usagePreconditionEClass = createEClass(USAGE_PRECONDITION);

		postconditionEClass = createEClass(POSTCONDITION);

		occurrenceEClass = createEClass(OCCURRENCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Obtain other dependent packages
		TypesPackage theTypesPackage = (TypesPackage)EPackage.Registry.INSTANCE.getEPackage(TypesPackage.eNS_URI);
		UMLPackage theUMLPackage = (UMLPackage)EPackage.Registry.INSTANCE.getEPackage(UMLPackage.eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		requirementEClass.getESuperTypes().add(this.getFormalConstraint());
		synthesisPreconditionEClass.getESuperTypes().add(this.getFormalConstraint());
		usagePreconditionEClass.getESuperTypes().add(this.getFormalConstraint());
		postconditionEClass.getESuperTypes().add(this.getFormalConstraint());
		occurrenceEClass.getESuperTypes().add(this.getFormalConstraint());

		// Initialize classes, features, and operations; add parameters
		initEClass(formalElementEClass, FormalElement.class, "FormalElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFormalElement_Guard(), theTypesPackage.getString(), "guard", null, 0, 1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEAttribute(getFormalElement_Effects(), theTypesPackage.getString(), "effects", null, 0, -1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEReference(getFormalElement_Base_RedefinableElement(), theUMLPackage.getRedefinableElement(), null, "base_RedefinableElement", null, 1, 1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEAttribute(getFormalElement_Atomic(), theTypesPackage.getBoolean(), "atomic", "false", 0, 1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);

		initEClass(formalControlFlowEClass, FormalControlFlow.class, "FormalControlFlow", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFormalControlFlow_OutgoingGuard(), theTypesPackage.getString(), "outgoingGuard", null, 0, 1, FormalControlFlow.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEReference(getFormalControlFlow_Base_ControlFlow(), theUMLPackage.getControlFlow(), null, "base_ControlFlow", null, 1, 1, FormalControlFlow.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);

		initEClass(formalCallBehaviorActionEClass, FormalCallBehaviorAction.class, "FormalCallBehaviorAction", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFormalCallBehaviorAction_Arguments(), theTypesPackage.getString(), "arguments", null, 0, 1, FormalCallBehaviorAction.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEReference(getFormalCallBehaviorAction_Base_CallBehaviorAction(), theUMLPackage.getCallBehaviorAction(), null, "base_CallBehaviorAction", null, 1, 1, FormalCallBehaviorAction.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);

		initEClass(requirementEClass, Requirement.class, "Requirement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(formalConstraintEClass, FormalConstraint.class, "FormalConstraint", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEReference(getFormalConstraint_Base_Constraint(), theUMLPackage.getConstraint(), null, "base_Constraint", null, 1, 1, FormalConstraint.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);

		initEClass(synthesisPreconditionEClass, SynthesisPrecondition.class, "SynthesisPrecondition", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(usagePreconditionEClass, UsagePrecondition.class, "UsagePrecondition", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(postconditionEClass, Postcondition.class, "Postcondition", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		initEClass(occurrenceEClass, Occurrence.class, "Occurrence", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

		// Create resource
		createResource(eNS_URI);
	}

} //SynthMLPackageImpl
