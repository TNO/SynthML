/**
 */
package SynthML.util;

import SynthML.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.Switch;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see SynthML.SynthMLPackage
 * @generated
 */
public class SynthMLSwitch<T> extends Switch<T> {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static SynthMLPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public SynthMLSwitch() {
		if (modelPackage == null) {
			modelPackage = SynthMLPackage.eINSTANCE;
		}
	}

	/**
	 * Checks whether this is a switch for the given package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param ePackage the package in question.
	 * @return whether this is a switch for the given package.
	 * @generated
	 */
	@Override
	protected boolean isSwitchFor(EPackage ePackage) {
		return ePackage == modelPackage;
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	@Override
	protected T doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case SynthMLPackage.FORMAL_ELEMENT: {
				FormalElement formalElement = (FormalElement)theEObject;
				T result = caseFormalElement(formalElement);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.FORMAL_CONTROL_FLOW: {
				FormalControlFlow formalControlFlow = (FormalControlFlow)theEObject;
				T result = caseFormalControlFlow(formalControlFlow);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.FORMAL_CALL_BEHAVIOR_ACTION: {
				FormalCallBehaviorAction formalCallBehaviorAction = (FormalCallBehaviorAction)theEObject;
				T result = caseFormalCallBehaviorAction(formalCallBehaviorAction);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.REQUIREMENT: {
				Requirement requirement = (Requirement)theEObject;
				T result = caseRequirement(requirement);
				if (result == null) result = caseFormalConstraint(requirement);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.FORMAL_CONSTRAINT: {
				FormalConstraint formalConstraint = (FormalConstraint)theEObject;
				T result = caseFormalConstraint(formalConstraint);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.SYNTHESIS_PRECONDITION: {
				SynthesisPrecondition synthesisPrecondition = (SynthesisPrecondition)theEObject;
				T result = caseSynthesisPrecondition(synthesisPrecondition);
				if (result == null) result = caseFormalConstraint(synthesisPrecondition);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.USAGE_PRECONDITION: {
				UsagePrecondition usagePrecondition = (UsagePrecondition)theEObject;
				T result = caseUsagePrecondition(usagePrecondition);
				if (result == null) result = caseFormalConstraint(usagePrecondition);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.POSTCONDITION: {
				Postcondition postcondition = (Postcondition)theEObject;
				T result = casePostcondition(postcondition);
				if (result == null) result = caseFormalConstraint(postcondition);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case SynthMLPackage.OCCURRENCE: {
				Occurrence occurrence = (Occurrence)theEObject;
				T result = caseOccurrence(occurrence);
				if (result == null) result = caseFormalConstraint(occurrence);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Formal Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Formal Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFormalElement(FormalElement object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Formal Control Flow</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Formal Control Flow</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFormalControlFlow(FormalControlFlow object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Formal Call Behavior Action</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Formal Call Behavior Action</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFormalCallBehaviorAction(FormalCallBehaviorAction object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Requirement</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Requirement</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseRequirement(Requirement object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Formal Constraint</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Formal Constraint</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseFormalConstraint(FormalConstraint object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Synthesis Precondition</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Synthesis Precondition</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseSynthesisPrecondition(SynthesisPrecondition object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Usage Precondition</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Usage Precondition</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseUsagePrecondition(UsagePrecondition object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Postcondition</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Postcondition</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T casePostcondition(Postcondition object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>Occurrence</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>Occurrence</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public T caseOccurrence(Occurrence object) {
		return null;
	}

	/**
	 * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	@Override
	public T defaultCase(EObject object) {
		return null;
	}

} //SynthMLSwitch
