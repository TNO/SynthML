/**
 */
package SynthML.impl;

import SynthML.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class SynthMLFactoryImpl extends EFactoryImpl implements SynthMLFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static SynthMLFactory init() {
		try {
			SynthMLFactory theSynthMLFactory = (SynthMLFactory)EPackage.Registry.INSTANCE.getEFactory(SynthMLPackage.eNS_URI);
			if (theSynthMLFactory != null) {
				return theSynthMLFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new SynthMLFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SynthMLFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case SynthMLPackage.FORMAL_ELEMENT: return createFormalElement();
			case SynthMLPackage.FORMAL_CONTROL_FLOW: return createFormalControlFlow();
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION: return createFormalCallBehaviorAction();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public FormalElement createFormalElement() {
		FormalElementImpl formalElement = new FormalElementImpl();
		return formalElement;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public FormalControlFlow createFormalControlFlow() {
		FormalControlFlowImpl formalControlFlow = new FormalControlFlowImpl();
		return formalControlFlow;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public FormalCallBehaviorAction createFormalCallBehaviorAction() {
		FormalCallBehaviorActionImpl formalCallBehaviorAction = new FormalCallBehaviorActionImpl();
		return formalCallBehaviorAction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SynthMLPackage getSynthMLPackage() {
		return (SynthMLPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static SynthMLPackage getPackage() {
		return SynthMLPackage.eINSTANCE;
	}

} //SynthMLFactoryImpl
