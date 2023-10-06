
package com.github.tno.pokayoke.transform.uml;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.uml2.uml.AcceptEventAction;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;
import org.eclipse.uml2.uml.SendSignalAction;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.Trigger;
import org.eclipse.uml2.uml.ValueSpecificationAction;

import com.github.tno.pokayoke.transform.common.FileHelper;

/** Helper class for creating various kinds of activities. */

public class ActivityHelper {
    private ActivityHelper() {
    }

    /**
     * Creates an activity that waits until the specified guard becomes {@code true} and then executes the specified
     * effect. The evaluation of the guard and possible execution of the effect happen together atomically.
     *
     * @param guards A list of single-line Python boolean expressions.
     * @param effects A list of single-line Python programs.
     * @param acquire The signal for acquiring the lock.
     * @param callerId The identifier of the caller.
     * @return The created activity that executes atomically.
     */
    public static Activity createAtomicActivity(List<String> guards, List<String> effects, Signal acquire, String callerId) {
        // Combine all given guards into a single Python expression.
        String guard = "True";
        if (!guards.isEmpty()) {
            guard = guards.stream().map(e -> "(" + e + ")").collect(Collectors.joining(" and "));
        }

        // Combine all given effects into a single Python program.
        String effect = "pass";
        if (!effects.isEmpty()) {
            effect = "if guard:\n";
            effect += effects.stream().map(e -> "\t" + e).collect(Collectors.joining("\n"));
        }

        // Define a new activity that encodes the guard and effect.
        Activity activity = FileHelper.FACTORY.createActivity();

        // Define the initial node.
        InitialNode initNode = FileHelper.FACTORY.createInitialNode();
        initNode.setActivity(activity);

        // Define the outer merge node.
        MergeNode outerMergeNode = FileHelper.FACTORY.createMergeNode();
        outerMergeNode.setActivity(activity);

        // Define the control flow from the initial node to the outer merge node.
        ControlFlow initToOuterMergeFlow = FileHelper.FACTORY.createControlFlow();
        initToOuterMergeFlow.setActivity(activity);
        initToOuterMergeFlow.setSource(initNode);
        initToOuterMergeFlow.setTarget(outerMergeNode);

        // Define the node used to repeatedly check whether the guard holds, without locking, for improved performance.
        OpaqueAction checkGuardNode = FileHelper.FACTORY.createOpaqueAction();
        checkGuardNode.setActivity(activity);
        checkGuardNode.getBodies().add(guard);
        checkGuardNode.getLanguages().add("Python");
        OutputPin checkGuardOutput = checkGuardNode.createOutputValue("doesGuardHold",
                FileHelper.loadPrimitiveType("Boolean"));

        // Define the control flow between the outer merge node and the node that checks the guard.
        ControlFlow outerMergeToCheckGuardFlow = FileHelper.FACTORY.createControlFlow();
        outerMergeToCheckGuardFlow.setActivity(activity);
        outerMergeToCheckGuardFlow.setSource(outerMergeNode);
        outerMergeToCheckGuardFlow.setTarget(checkGuardNode);

        // Define the decision node that checks whether the guard holds.
        DecisionNode checkGuardDecisionNode = FileHelper.FACTORY.createDecisionNode();
        checkGuardDecisionNode.setActivity(activity);

        // Define the control flow from the node that checks the guards to the decision node that checks the guard.
        ControlFlow checkGuardToDecisionFlow = FileHelper.FACTORY.createControlFlow();
        checkGuardToDecisionFlow.setActivity(activity);
        checkGuardToDecisionFlow.setSource(checkGuardNode);
        checkGuardToDecisionFlow.setTarget(checkGuardDecisionNode);

        // Define the object flow between the node that checks the guards to the decision node that checks the guard.
        ObjectFlow checkGuardToDecisionObjFlow = FileHelper.FACTORY.createObjectFlow();
        checkGuardToDecisionObjFlow.setActivity(activity);
        checkGuardToDecisionObjFlow.setSource(checkGuardOutput);
        checkGuardToDecisionObjFlow.setTarget(checkGuardDecisionNode);

        // Define the control flow from the decision node that checks the guard to the outer merge node.
        ControlFlow checkGuardDecisionToOuterMergeFlow = FileHelper.FACTORY.createControlFlow();
        checkGuardDecisionToOuterMergeFlow.setActivity(activity);
        checkGuardDecisionToOuterMergeFlow.setSource(checkGuardDecisionNode);
        checkGuardDecisionToOuterMergeFlow.setTarget(outerMergeNode);
        OpaqueExpression decisionToMergeGuard = FileHelper.FACTORY.createOpaqueExpression();
        decisionToMergeGuard.getBodies().add("else");
        decisionToMergeGuard.getLanguages().add("Python");
        checkGuardDecisionToOuterMergeFlow.setGuard(decisionToMergeGuard);

        // Define the acquire signal send action.
        SendSignalAction sendAcquireNode = FileHelper.FACTORY.createSendSignalAction();
        sendAcquireNode.setActivity(activity);
        sendAcquireNode.setSignal(acquire);
        InputPin sendAcquireInput = FileHelper.FACTORY.createInputPin();
        sendAcquireInput.setName("requester");
        sendAcquireInput.setType(FileHelper.loadPrimitiveType("String"));
        sendAcquireNode.getArguments().add(sendAcquireInput);

        // Define the requester value specification node.
        ValueSpecificationAction requesterValueNode = FileHelper.FACTORY.createValueSpecificationAction();
        requesterValueNode.setActivity(activity);
        OutputPin requesterValueOutput = FileHelper.FACTORY.createOutputPin();
        requesterValueOutput.setName("requester");
        requesterValueOutput.setType(FileHelper.loadPrimitiveType("String"));
        requesterValueNode.setResult(requesterValueOutput);
        LiteralString requesterValueLiteral = FileHelper.FACTORY.createLiteralString();
        requesterValueLiteral.setValue(callerId);
        requesterValueNode.setValue(requesterValueLiteral);

        // Define the control flow from the decision node that checks the guard to the requester value node.
        ControlFlow checkGuardDecisionToRequesterValueFlow = FileHelper.FACTORY.createControlFlow();
        checkGuardDecisionToRequesterValueFlow.setActivity(activity);
        checkGuardDecisionToRequesterValueFlow.setSource(checkGuardDecisionNode);
        checkGuardDecisionToRequesterValueFlow.setTarget(requesterValueNode);
        OpaqueExpression decisionToFinalGuard = FileHelper.FACTORY.createOpaqueExpression();
        decisionToFinalGuard.getBodies().add(checkGuardOutput.getName());
        decisionToFinalGuard.getLanguages().add("Python");
        checkGuardDecisionToRequesterValueFlow.setGuard(decisionToFinalGuard);

        // Define the object flow from the requester value node to the node that sends the acquire signal.
        ObjectFlow requesterObjectFlow = FileHelper.FACTORY.createObjectFlow();
        requesterObjectFlow.setActivity(activity);
        requesterObjectFlow.setSource(requesterValueOutput);
        requesterObjectFlow.setTarget(sendAcquireInput);

        // Define the inner merge node.
        MergeNode innerMergeNode = FileHelper.FACTORY.createMergeNode();
        innerMergeNode.setActivity(activity);

        // Define the control flow from the node that sends the acquire signal to the inner merge node.
        ControlFlow acquireToInnerMergeFlow = FileHelper.FACTORY.createControlFlow();
        acquireToInnerMergeFlow.setActivity(activity);
        acquireToInnerMergeFlow.setSource(sendAcquireNode);
        acquireToInnerMergeFlow.setTarget(innerMergeNode);

        // Define the node that checks whether the lock is granted.
        OpaqueAction checkActiveNode = FileHelper.FACTORY.createOpaqueAction();
        checkActiveNode.setActivity(activity);
        checkActiveNode.getBodies().add("active == '" + callerId + "'");
        checkActiveNode.getLanguages().add("Python");
        OutputPin checkActiveOutput = checkActiveNode.createOutputValue("isActive",
                FileHelper.loadPrimitiveType("Boolean"));

        // Define the control flow from the inner merge node to the node that checks the active variable.
        ControlFlow innerMergeToCheckActiveFlow = FileHelper.FACTORY.createControlFlow();
        innerMergeToCheckActiveFlow.setActivity(activity);
        innerMergeToCheckActiveFlow.setSource(innerMergeNode);
        innerMergeToCheckActiveFlow.setTarget(checkActiveNode);

        // Define the inner decision node.
        DecisionNode innerDecisionNode = FileHelper.FACTORY.createDecisionNode();
        innerDecisionNode.setActivity(activity);

        // Define the control flow from the node that checks the active variable to the inner decision node.
        ControlFlow checkActiveToInnerDecisionFlow = FileHelper.FACTORY.createControlFlow();
        checkActiveToInnerDecisionFlow.setActivity(activity);
        checkActiveToInnerDecisionFlow.setSource(checkActiveNode);
        checkActiveToInnerDecisionFlow.setTarget(innerDecisionNode);

        // Define the object flow from the node that checks the active variable to the inner decision node.
        ObjectFlow checkActiveToInnerDecisionObjFlow = FileHelper.FACTORY.createObjectFlow();
        checkActiveToInnerDecisionObjFlow.setActivity(activity);
        checkActiveToInnerDecisionObjFlow.setSource(checkActiveOutput);
        checkActiveToInnerDecisionObjFlow.setTarget(innerDecisionNode);

        // Define the opaque action of the activity that encodes the guard and effect.
        OpaqueAction guardAndEffectNode = FileHelper.FACTORY.createOpaqueAction();
        guardAndEffectNode.setActivity(activity);
        guardAndEffectNode.getLanguages().add("Python");
        String guardAndEffectBody = String.format("guard = %s\n%s\nactive = ''\nisSuccessful = guard", guard, effect);
        guardAndEffectNode.getBodies().add(guardAndEffectBody);
        OutputPin guardAndEffectOutput = guardAndEffectNode.createOutputValue("isSuccessful",
                FileHelper.loadPrimitiveType("Boolean"));

        // Define the control flow from the inner decision node to the node that executes the guard and effect.
        ControlFlow innerDecisionToGuardAndEffectFlow = FileHelper.FACTORY.createControlFlow();
        innerDecisionToGuardAndEffectFlow.setActivity(activity);
        innerDecisionToGuardAndEffectFlow.setSource(innerDecisionNode);
        innerDecisionToGuardAndEffectFlow.setTarget(guardAndEffectNode);
        OpaqueExpression innerDecisionToGuardAndEffectGuard = FileHelper.FACTORY.createOpaqueExpression();
        innerDecisionToGuardAndEffectGuard.getBodies().add("isActive");
        innerDecisionToGuardAndEffectGuard.getLanguages().add("Python");
        innerDecisionToGuardAndEffectFlow.setGuard(innerDecisionToGuardAndEffectGuard);

        // Define the control flow from the inner decision node to the node that executes the guard and effect.
        ControlFlow innerDecisionToInnerMergeFlow = FileHelper.FACTORY.createControlFlow();
        innerDecisionToInnerMergeFlow.setActivity(activity);
        innerDecisionToInnerMergeFlow.setSource(innerDecisionNode);
        innerDecisionToInnerMergeFlow.setTarget(innerMergeNode);
        OpaqueExpression innerDecisionToInnerMergeGuard = FileHelper.FACTORY.createOpaqueExpression();
        innerDecisionToInnerMergeGuard.getBodies().add("else");
        innerDecisionToInnerMergeGuard.getLanguages().add("Python");
        innerDecisionToInnerMergeFlow.setGuard(innerDecisionToInnerMergeGuard);

        // Define the outer decision node.
        DecisionNode outerDecisionNode = FileHelper.FACTORY.createDecisionNode();
        outerDecisionNode.setActivity(activity);

        // Define the control flow from the node that executes the guard and effect to the outer decision node.
        ControlFlow guardAndEffectToOuterDecisionFlow = FileHelper.FACTORY.createControlFlow();
        guardAndEffectToOuterDecisionFlow.setActivity(activity);
        guardAndEffectToOuterDecisionFlow.setSource(guardAndEffectNode);
        guardAndEffectToOuterDecisionFlow.setTarget(outerDecisionNode);

        // Define the object flow from the node that executes the guard and effect to the outer decision node.
        ObjectFlow guardAndEffectToOuterDecisionObjFlow = FileHelper.FACTORY.createObjectFlow();
        guardAndEffectToOuterDecisionObjFlow.setActivity(activity);
        guardAndEffectToOuterDecisionObjFlow.setSource(guardAndEffectOutput);
        guardAndEffectToOuterDecisionObjFlow.setTarget(outerDecisionNode);

        // Define the final node.
        FinalNode finalNode = FileHelper.FACTORY.createActivityFinalNode();
        finalNode.setActivity(activity);

        // Define the control flow from the outer decision node to the final node.
        ControlFlow outerDecisionToFinalFlow = FileHelper.FACTORY.createControlFlow();
        outerDecisionToFinalFlow.setActivity(activity);
        outerDecisionToFinalFlow.setSource(outerDecisionNode);
        outerDecisionToFinalFlow.setTarget(finalNode);
        OpaqueExpression outerDecisionToFinalGuard = FileHelper.FACTORY.createOpaqueExpression();
        outerDecisionToFinalGuard.getBodies().add(guardAndEffectOutput.getName());
        outerDecisionToFinalGuard.getLanguages().add("Python");
        outerDecisionToFinalFlow.setGuard(outerDecisionToFinalGuard);

        // Define the control flow from the outer decision node to the outer merge node.
        ControlFlow outerDecisionToOuterMergeFlow = FileHelper.FACTORY.createControlFlow();
        outerDecisionToOuterMergeFlow.setActivity(activity);
        outerDecisionToOuterMergeFlow.setSource(outerDecisionNode);
        outerDecisionToOuterMergeFlow.setTarget(outerMergeNode);
        OpaqueExpression outerDecisionToOuterMergeGuard = FileHelper.FACTORY.createOpaqueExpression();
        outerDecisionToOuterMergeGuard.getBodies().add("else");
        outerDecisionToOuterMergeGuard.getLanguages().add("Python");
        outerDecisionToOuterMergeFlow.setGuard(outerDecisionToOuterMergeGuard);

        return activity;
    }

    /**
     * Creates an activity that handles lock acquisition, by listening for the specified acquire signal event and
     * updating the shared variable 'active' accordingly, in a loop.
     *
     * @param acquireEvent The acquire signal event to listen to.
     * @return The created lock handling activity.
     */
    public static Activity createLockHanderActivity(SignalEvent acquireEvent) {
        Signal acquireSignal = acquireEvent.getSignal();
        Property acquireParameter = acquireSignal.getOwnedAttributes().get(0);

        // Create the activity that handles lock acquisition.
        Activity activity = FileHelper.FACTORY.createActivity();

        // Define the initial node.
        InitialNode initNode = FileHelper.FACTORY.createInitialNode();
        initNode.setActivity(activity);

        // Define the outer merge node.
        MergeNode outerMergeNode = FileHelper.FACTORY.createMergeNode();
        outerMergeNode.setActivity(activity);

        // Define the control flow between the initial node and the outer merge node.
        ControlFlow initToOuterMergeFlow = FileHelper.FACTORY.createControlFlow();
        initToOuterMergeFlow.setActivity(activity);
        initToOuterMergeFlow.setSource(initNode);
        initToOuterMergeFlow.setTarget(outerMergeNode);

        // Define the node that accepts acquire signals.
        AcceptEventAction acceptAcquireNode = FileHelper.FACTORY.createAcceptEventAction();
        acceptAcquireNode.setActivity(activity);
        Trigger acquireTrigger = FileHelper.FACTORY.createTrigger();
        acquireTrigger.setEvent(acquireEvent);
        acceptAcquireNode.getTriggers().add(acquireTrigger);
        OutputPin acceptAcquireOutput = acceptAcquireNode.createResult("msg", acquireSignal);
        acceptAcquireOutput.setIsOrdered(true);
        acceptAcquireOutput.setIsUnique(false);

        // Define the control flow between the outer merge node and the node that accepts acquire signals.
        ControlFlow mergeToAcceptAcquireFlow = FileHelper.FACTORY.createControlFlow();
        mergeToAcceptAcquireFlow.setActivity(activity);
        mergeToAcceptAcquireFlow.setSource(outerMergeNode);
        mergeToAcceptAcquireFlow.setTarget(acceptAcquireNode);

        // Define the node that reads the argument from an accepted acquire signal.
        ReadStructuralFeatureAction readRequesterNode = FileHelper.FACTORY.createReadStructuralFeatureAction();
        readRequesterNode.setActivity(activity);
        readRequesterNode.setStructuralFeature(acquireParameter);
        InputPin readRequesterInput = readRequesterNode.createObject(acceptAcquireOutput.getName(), acquireSignal);
        OutputPin readRequesterOutput = readRequesterNode.createResult(acquireParameter.getName(),
                FileHelper.loadPrimitiveType("String"));

        // Define the object flow between the node that accepts acquire signals and the node that reads the requester
        // variable.
        ObjectFlow acceptAcquireToReadFlow = FileHelper.FACTORY.createObjectFlow();
        acceptAcquireToReadFlow.setActivity(activity);
        acceptAcquireToReadFlow.setSource(acceptAcquireOutput);
        acceptAcquireToReadFlow.setTarget(readRequesterInput);

        // Define the action that updates the static active variable.
        OpaqueAction setActiveNode = FileHelper.FACTORY.createOpaqueAction();
        setActiveNode.setActivity(activity);
        InputPin setActiveInput = setActiveNode.createInputValue(acquireParameter.getName(),
                FileHelper.loadPrimitiveType("String"));
        setActiveNode.getBodies().add("active = " + acquireParameter.getName());
        setActiveNode.getLanguages().add("Python");

        // Define the object flow between the lock handler node and the node that sets the active variable.
        ObjectFlow readToSetActiveFlow = FileHelper.FACTORY.createObjectFlow();
        readToSetActiveFlow.setActivity(activity);
        readToSetActiveFlow.setSource(readRequesterOutput);
        readToSetActiveFlow.setTarget(setActiveInput);

        // Define the inner merge node.
        MergeNode innerMergeNode = FileHelper.FACTORY.createMergeNode();
        innerMergeNode.setActivity(activity);

        // Define the control flow between the node that sets the active variable and the inner merge node.
        ControlFlow setActiveToInnerMergeFlow = FileHelper.FACTORY.createControlFlow();
        setActiveToInnerMergeFlow.setActivity(activity);
        setActiveToInnerMergeFlow.setSource(setActiveNode);
        setActiveToInnerMergeFlow.setTarget(innerMergeNode);

        // Define the action that checks whether the lock has been released.
        OpaqueAction checkReleasedNode = FileHelper.FACTORY.createOpaqueAction();
        checkReleasedNode.setActivity(activity);
        checkReleasedNode.getBodies().add("active == ''");
        checkReleasedNode.getLanguages().add("Python");
        OutputPin checkReleasedOutput = checkReleasedNode.createOutputValue("isReleased",
                FileHelper.loadPrimitiveType("Boolean"));

        // Define the control flow from the inner merge node to the node that checks whether the lock has been released.
        ControlFlow innerMergeToCheckReleaseFlow = FileHelper.FACTORY.createControlFlow();
        innerMergeToCheckReleaseFlow.setActivity(activity);
        innerMergeToCheckReleaseFlow.setSource(innerMergeNode);
        innerMergeToCheckReleaseFlow.setTarget(checkReleasedNode);

        // Define the decision node.
        DecisionNode decisionNode = FileHelper.FACTORY.createDecisionNode();
        decisionNode.setActivity(activity);

        // Define the control flow between the node that checks whether the lock has been released and the decision
        // node.
        ControlFlow checkReleasedToDecisionFlow = FileHelper.FACTORY.createControlFlow();
        checkReleasedToDecisionFlow.setActivity(activity);
        checkReleasedToDecisionFlow.setSource(checkReleasedNode);
        checkReleasedToDecisionFlow.setTarget(decisionNode);

        // Define the object flow between the node that checks whether the lock has been released and the decision node.
        ObjectFlow checkReleasedToDecisionObjFlow = FileHelper.FACTORY.createObjectFlow();
        checkReleasedToDecisionObjFlow.setActivity(activity);
        checkReleasedToDecisionObjFlow.setSource(checkReleasedOutput);
        checkReleasedToDecisionObjFlow.setTarget(decisionNode);

        // Define the control flow between the decision node and the outer merge node.
        ControlFlow decisionToOuterMergeFlow = FileHelper.FACTORY.createControlFlow();
        decisionToOuterMergeFlow.setActivity(activity);
        decisionToOuterMergeFlow.setSource(decisionNode);
        decisionToOuterMergeFlow.setTarget(outerMergeNode);
        OpaqueExpression decisionToWaitGuard = FileHelper.FACTORY.createOpaqueExpression();
        decisionToWaitGuard.getBodies().add(checkReleasedOutput.getName());
        decisionToWaitGuard.getLanguages().add("Python");
        decisionToOuterMergeFlow.setGuard(decisionToWaitGuard);

        // Define the control flow between the decision node and the inner merge node.
        ControlFlow decisionToInnerMergeFlow = FileHelper.FACTORY.createControlFlow();
        decisionToInnerMergeFlow.setActivity(activity);
        decisionToInnerMergeFlow.setSource(decisionNode);
        decisionToInnerMergeFlow.setTarget(innerMergeNode);
        OpaqueExpression decisionToInnerMergeGuard = FileHelper.FACTORY.createOpaqueExpression();
        decisionToInnerMergeGuard.getBodies().add("else");
        decisionToInnerMergeGuard.getLanguages().add("Python");
        decisionToInnerMergeFlow.setGuard(decisionToInnerMergeGuard);

        return activity;
    }
}
