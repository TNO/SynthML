/**
 */
package PokaYoke.impl;

import PokaYoke.FormalControlFlow;
import PokaYoke.FormalElement;
import PokaYoke.PokaYokeFactory;
import PokaYoke.PokaYokePackage;

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
public class PokaYokePackageImpl extends EPackageImpl implements PokaYokePackage {
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
	 * @see PokaYoke.PokaYokePackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private PokaYokePackageImpl() {
		super(eNS_URI, PokaYokeFactory.eINSTANCE);
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
	 * <p>This method is used to initialize {@link PokaYokePackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static PokaYokePackage init() {
		if (isInited) return (PokaYokePackage)EPackage.Registry.INSTANCE.getEPackage(PokaYokePackage.eNS_URI);

		// Obtain or create and register package
		Object registeredPokaYokePackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		PokaYokePackageImpl thePokaYokePackage = registeredPokaYokePackage instanceof PokaYokePackageImpl ? (PokaYokePackageImpl)registeredPokaYokePackage : new PokaYokePackageImpl();

		isInited = true;

		// Initialize simple dependencies
		TypesPackage.eINSTANCE.eClass();
		UMLPackage.eINSTANCE.eClass();
		EcorePackage.eINSTANCE.eClass();

		// Create package meta-data objects
		thePokaYokePackage.createPackageContents();

		// Initialize created meta-data
		thePokaYokePackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		thePokaYokePackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(PokaYokePackage.eNS_URI, thePokaYokePackage);
		return thePokaYokePackage;
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
	public PokaYokeFactory getPokaYokeFactory() {
		return (PokaYokeFactory)getEFactoryInstance();
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

		// Initialize classes, features, and operations; add parameters
		initEClass(formalElementEClass, FormalElement.class, "FormalElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFormalElement_Guard(), theTypesPackage.getString(), "guard", null, 0, 1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEAttribute(getFormalElement_Effects(), theTypesPackage.getString(), "effects", null, 0, -1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEReference(getFormalElement_Base_RedefinableElement(), theUMLPackage.getRedefinableElement(), null, "base_RedefinableElement", null, 1, 1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEAttribute(getFormalElement_Atomic(), theTypesPackage.getBoolean(), "atomic", "false", 0, 1, FormalElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);

		initEClass(formalControlFlowEClass, FormalControlFlow.class, "FormalControlFlow", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFormalControlFlow_OutgoingGuard(), theTypesPackage.getString(), "outgoingGuard", null, 0, 1, FormalControlFlow.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEReference(getFormalControlFlow_Base_ControlFlow(), theUMLPackage.getControlFlow(), null, "base_ControlFlow", null, 1, 1, FormalControlFlow.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);

		// Create resource
		createResource(eNS_URI);
	}

} //PokaYokePackageImpl
