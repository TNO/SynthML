
package com.github.tno.pokayoke.transform.uml2cameo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Range;
import org.eclipse.uml2.uml.AcceptEventAction;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityParameterNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.InputPin;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.Parameter;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ReadStructuralFeatureAction;
import org.eclipse.uml2.uml.SendSignalAction;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.Trigger;
import org.eclipse.uml2.uml.UMLFactory;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.CifParserHelper;
import com.github.tno.synthml.uml.profile.util.UmlPrimitiveType;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/** Helper class for creating various kinds of activities. */
public class ActivityHelper {
    private ActivityHelper() {
        // Empty for utility classes
    }

    /**
     * Creates an activity that waits until the specified guard becomes {@code true} and then executes one of the
     * specified effects.
     *
     * @param name The name of the activity to create.
     * @param guard A single-line Python boolean expression.
     * @param effects The list of effects. Every effect must be a list of single-line Python programs.
     * @param propertyBounds The integer properties in the model with their bounds.
     * @param acquire The signal for acquiring the lock.
     * @param isAtomic Whether the activity to create should be atomic.
     * @param scopedProperties The properties that are defined in the scope of the calling activity
     * @return The created activity.
     */
    public static Activity createActivity(String name, String guard, List<List<String>> effects,
            Map<String, Range<Integer>> propertyBounds, Signal acquire, boolean isAtomic, Set<String> scopedProperties)
    {
        if (isAtomic) {
            return createAtomicActivity(name, guard, effects, propertyBounds, acquire, scopedProperties);
        } else {
            return createNonAtomicActivity(name, guard, effects, propertyBounds, acquire, scopedProperties);
        }
    }

    /**
     * Creates an activity that waits until the specified guard becomes {@code true} and then executes one of the
     * specified effects. The evaluation of the guard and possible execution of the effect happen together atomically.
     *
     * @param name The name of the activity to create.
     * @param guard A single-line Python boolean expression.
     * @param effects The list of effects. Every effect must be a list of single-line Python programs.
     * @param propertyBounds The integer properties in the model with their bounds.
     * @param acquire The signal for acquiring the lock.
     * @param scopedProperties The properties that are defined in the scope of the calling activity
     *
     * @return The created activity that executes atomically.
     */
    public static Activity createAtomicActivity(String name, String guard, List<List<String>> effects,
            Map<String, Range<Integer>> propertyBounds, Signal acquire, Set<String> scopedProperties)
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(guard),
                "Argument guard cannot be null nor an empty string.");

        // The Python code generator does not escape quotes when translating 'name'.
        // To avoid syntax errors in the generated Python code, we disallow the use of single quotes in 'name'.
        Preconditions.checkArgument(!name.contains("'"), "Argument name contains quote character ('): " + name);

        // Translate the given effects as a single Python program.
        String effectBody = translateEffects(effects);

        // Validate the property bounds at runtime.
        if (!effects.isEmpty()) {
            effectBody += "\n" + translatePropertyBounds(propertyBounds);
        }

        // Define a new activity that encodes the guard and effect.
        Activity activity = FileHelper.FACTORY.createActivity();
        activity.setName(name);

        // Add the template parameters
        for (String propertyName: scopedProperties) {
            addParameterToActivity(activity, propertyName);
        }

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
                UmlPrimitiveType.BOOLEAN.load(acquire));

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
        sendAcquireInput.setType(UmlPrimitiveType.STRING.load(acquire));
        sendAcquireNode.getArguments().add(sendAcquireInput);

        // Define the requester value specification node.
        OpaqueAction requesterValueNode = FileHelper.FACTORY.createOpaqueAction();
        requesterValueNode.setActivity(activity);
        requesterValueNode.getBodies().add("import uuid\r\n" + "requester = \'" + name + "_\' + str(uuid.uuid4())");
        requesterValueNode.getLanguages().add("Python");
        OutputPin requesterValueOutput = requesterValueNode.createOutputValue("requester",
                UmlPrimitiveType.STRING.load(acquire));

        // Define the control flow from the decision node that checks the guard to the requester value node.
        ControlFlow checkGuardDecisionToRequesterValueFlow = FileHelper.FACTORY.createControlFlow();
        checkGuardDecisionToRequesterValueFlow.setActivity(activity);
        checkGuardDecisionToRequesterValueFlow.setSource(checkGuardDecisionNode);
        checkGuardDecisionToRequesterValueFlow.setTarget(requesterValueNode);
        OpaqueExpression decisionToFinalGuard = FileHelper.FACTORY.createOpaqueExpression();
        decisionToFinalGuard.getBodies().add(checkGuardOutput.getName());
        decisionToFinalGuard.getLanguages().add("Python");
        checkGuardDecisionToRequesterValueFlow.setGuard(decisionToFinalGuard);

        // Define the fork node for duplicating the 'requester' output.
        ForkNode requestDuplicatorForkNode = FileHelper.FACTORY.createForkNode();
        requestDuplicatorForkNode.setActivity(activity);

        // Define the object flow from the requester value node to the duplicator fork node.
        ObjectFlow requesterToDuplicatorObjectFlow = FileHelper.FACTORY.createObjectFlow();
        requesterToDuplicatorObjectFlow.setActivity(activity);
        requesterToDuplicatorObjectFlow.setSource(requesterValueOutput);
        requesterToDuplicatorObjectFlow.setTarget(requestDuplicatorForkNode);

        // Define the object flow from the requester value node to the node that sends the acquire signal.
        ObjectFlow requestDuplicatorToSignalObjectFlow = FileHelper.FACTORY.createObjectFlow();
        requestDuplicatorToSignalObjectFlow.setActivity(activity);
        requestDuplicatorToSignalObjectFlow.setSource(requestDuplicatorForkNode);
        requestDuplicatorToSignalObjectFlow.setTarget(sendAcquireInput);

        // Define the inner merge node.
        MergeNode innerMergeNode = FileHelper.FACTORY.createMergeNode();
        innerMergeNode.setActivity(activity);

        // Define the object flow from the request signal duplicator node to the inner merge node.
        ObjectFlow requestDuplicatorToInnerMergeObjectFlow = FileHelper.FACTORY.createObjectFlow();
        requestDuplicatorToInnerMergeObjectFlow.setActivity(activity);
        requestDuplicatorToInnerMergeObjectFlow.setSource(requestDuplicatorForkNode);
        requestDuplicatorToInnerMergeObjectFlow.setTarget(innerMergeNode);

        // Define the node that unmarshals the request for the decision node.
        OpaqueAction unmarshalRequestNode = FileHelper.FACTORY.createOpaqueAction();
        unmarshalRequestNode.setActivity(activity);
        InputPin unmarshalRequestInput = unmarshalRequestNode.createInputValue("requester",
                UmlPrimitiveType.STRING.load(acquire));
        OutputPin unmarshalRequestOutput = unmarshalRequestNode.createOutputValue("requester",
                UmlPrimitiveType.STRING.load(acquire));

        // Define the object flow from the inner merge node to the node that unmarshals the request.
        ObjectFlow innerMergeToUnmarshalRequestObjectFlow = FileHelper.FACTORY.createObjectFlow();
        innerMergeToUnmarshalRequestObjectFlow.setActivity(activity);
        innerMergeToUnmarshalRequestObjectFlow.setSource(innerMergeNode);
        innerMergeToUnmarshalRequestObjectFlow.setTarget(unmarshalRequestInput);

        // Define the inner decision node.
        DecisionNode innerDecisionNode = FileHelper.FACTORY.createDecisionNode();
        innerDecisionNode.setActivity(activity);

        // Define the object flow from the node that unmarshals the request to the inner decision node that checks the
        // active variable. This is needed to add requester to the context of 'innerDecisionToGuardAndEffectGuard'.
        ObjectFlow unmarshalRequestToInnerObjectFlow = FileHelper.FACTORY.createObjectFlow();
        unmarshalRequestToInnerObjectFlow.setActivity(activity);
        unmarshalRequestToInnerObjectFlow.setSource(unmarshalRequestOutput);
        unmarshalRequestToInnerObjectFlow.setTarget(innerDecisionNode);

        // Define the opaque action of the activity that encodes the guard and effect.
        OpaqueAction guardAndEffectNode = FileHelper.FACTORY.createOpaqueAction();
        guardAndEffectNode.setActivity(activity);
        guardAndEffectNode.getLanguages().add("Python");
        String randomImport = effects.size() > 1 ? "import random\n" : "";
        String guardAndEffectBody = String.format("%sguard = %s\n%s\nactive = ''\nisSuccessful = guard", randomImport,
                guard, effectBody);
        guardAndEffectNode.getBodies().add(guardAndEffectBody);
        OutputPin guardAndEffectOutput = guardAndEffectNode.createOutputValue("isSuccessful",
                UmlPrimitiveType.BOOLEAN.load(acquire));

        // Define the control flow from the inner decision node to the node that executes the guard and effect.
        ControlFlow innerDecisionToGuardAndEffectFlow = FileHelper.FACTORY.createControlFlow();
        innerDecisionToGuardAndEffectFlow.setActivity(activity);
        innerDecisionToGuardAndEffectFlow.setSource(innerDecisionNode);
        innerDecisionToGuardAndEffectFlow.setTarget(guardAndEffectNode);
        OpaqueExpression innerDecisionToGuardAndEffectGuard = FileHelper.FACTORY.createOpaqueExpression();
        innerDecisionToGuardAndEffectGuard.getBodies().add("active == requester");
        innerDecisionToGuardAndEffectGuard.getLanguages().add("Python");
        innerDecisionToGuardAndEffectFlow.setGuard(innerDecisionToGuardAndEffectGuard);

        // Define the object flow from the inner decision node to the inner merge node.
        ObjectFlow innerDecisionToInnerObjectFlow = FileHelper.FACTORY.createObjectFlow();
        innerDecisionToInnerObjectFlow.setActivity(activity);
        innerDecisionToInnerObjectFlow.setSource(innerDecisionNode);
        innerDecisionToInnerObjectFlow.setTarget(innerMergeNode);
        OpaqueExpression innerDecisionToInnerMergeGuard = FileHelper.FACTORY.createOpaqueExpression();
        innerDecisionToInnerMergeGuard.getBodies().add("else");
        innerDecisionToInnerMergeGuard.getLanguages().add("Python");
        innerDecisionToInnerObjectFlow.setGuard(innerDecisionToInnerMergeGuard);

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
        ActivityFinalNode finalNode = FileHelper.FACTORY.createActivityFinalNode();
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
     * Adds a parameter to an activity.
     *
     * @param activity The activity to which the parameter is added.
     * @param name The name of the parameter to create.
     */
    public static void addParameterToActivity(Activity activity, String name) {
        Parameter inputParameter = UMLFactory.eINSTANCE.createParameter();
        inputParameter.setName(name);

        activity.getOwnedParameters().add(inputParameter);

        ActivityParameterNode parameterNode = UMLFactory.eINSTANCE.createActivityParameterNode();
        parameterNode.setName(name + "Node");
        parameterNode.setParameter(inputParameter);
        parameterNode.setActivity(activity);
    }

    /**
     * Creates an activity that waits until the specified guard becomes {@code true} and then executes one of the
     * specified effects. The evaluation of the guard and possible execution of the effect happen non-atomically, as two
     * separate steps (which themselves are atomic).
     *
     * @param name The name of the activity to create.
     * @param guard A single-line Python boolean expression.
     * @param effects The list of effects. Every effect must be a list of single-line Python programs.
     * @param propertyBounds The integer properties in the model with their bounds.
     * @param acquire The signal for acquiring the lock.
     * @param scopedProperties The properties that are defined in the scope of the calling activity
     * @return The created activity that executes non-atomically.
     */
    public static Activity createNonAtomicActivity(String name, String guard, List<List<String>> effects,
            Map<String, Range<Integer>> propertyBounds, Signal acquire, Set<String> scopedProperties)
    {
        // Split the non-atomic activity into two atomic parts: one to check the guard and one to perform the effects.
        Activity start = createAtomicActivity(name + "__start", guard, List.of(), propertyBounds, acquire,
                scopedProperties);
        Activity end = createAtomicActivity(name + "__end", "True", effects, propertyBounds, acquire, scopedProperties);

        // Create the activity that calls the start and end activities in sequence.
        Activity activity = FileHelper.FACTORY.createActivity();
        activity.getOwnedBehaviors().add(start);
        activity.getOwnedBehaviors().add(end);
        activity.setName(name);

        // Add the template parameters
        for (String propertyName: scopedProperties) {
            addParameterToActivity(activity, propertyName);
        }

        // Define the initial node.
        InitialNode initNode = FileHelper.FACTORY.createInitialNode();
        initNode.setActivity(activity);

        // Define the call behavior node that calls the start activity.
        CallBehaviorAction callStartNode = FileHelper.FACTORY.createCallBehaviorAction();
        callStartNode.setActivity(activity);
        callStartNode.setBehavior(start);
        callStartNode.setName(start.getName());

        // Define the control flow from the initial node to the node that calls the start activity.
        ControlFlow initToStartFlow = FileHelper.FACTORY.createControlFlow();
        initToStartFlow.setActivity(activity);
        initToStartFlow.setSource(initNode);
        initToStartFlow.setTarget(callStartNode);

        // Define the call behavior node that calls the end activity.
        CallBehaviorAction callEndNode = FileHelper.FACTORY.createCallBehaviorAction();
        callEndNode.setActivity(activity);
        callEndNode.setBehavior(end);
        callEndNode.setName(end.getName());

        // Define the control flow from the node that calls the start activity to the node that calls the end activity.
        ControlFlow startToEndFlow = FileHelper.FACTORY.createControlFlow();
        startToEndFlow.setActivity(activity);
        startToEndFlow.setSource(callStartNode);
        startToEndFlow.setTarget(callEndNode);

        // Define the final node.
        ActivityFinalNode finalNode = FileHelper.FACTORY.createActivityFinalNode();
        finalNode.setActivity(activity);

        // Define the control flow from the node that calls the end activity to the final node.
        ControlFlow endToFinalFlow = FileHelper.FACTORY.createControlFlow();
        endToFinalFlow.setActivity(activity);
        endToFinalFlow.setSource(callEndNode);
        endToFinalFlow.setTarget(finalNode);

        // Pass arguments to the newly created activities. Properties could be scoped further to the start/end node
        addTemplateArguments(callStartNode, scopedProperties, null);
        addTemplateArguments(callEndNode, scopedProperties, null);

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
                UmlPrimitiveType.STRING.load(acquireEvent));

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
                UmlPrimitiveType.STRING.load(acquireEvent));
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
                UmlPrimitiveType.BOOLEAN.load(acquireEvent));

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

    /**
     * Creates an activity that evaluates the incoming guards of the outgoing control flows of the given decision node,
     * and randomly picks one of the branches whose guard holds.
     *
     * @param decisionNode The decision node.
     * @param translator The translator for translating incoming guards to Python expressions.
     * @return The created activity.
     */
    public static Activity createDecisionEvaluationActivity(DecisionNode decisionNode,
            CifToPythonTranslator translator)
    {
        // Create the activity.
        Activity activity = FileHelper.FACTORY.createActivity();

        // Define the initial node.
        InitialNode initNode = FileHelper.FACTORY.createInitialNode();
        initNode.setActivity(activity);

        // Define the merge node.
        MergeNode mergeNode = FileHelper.FACTORY.createMergeNode();
        mergeNode.setActivity(activity);

        // Define the control flow between the initial node and the merge node.
        ControlFlow initToMergeFlow = FileHelper.FACTORY.createControlFlow();
        initToMergeFlow.setActivity(activity);
        initToMergeFlow.setSource(initNode);
        initToMergeFlow.setTarget(mergeNode);

        // Define an action that evaluates the guards of all outgoing edges, and non-deterministically chooses one edge
        // whose guard holds.
        OpaqueAction evalNode = FileHelper.FACTORY.createOpaqueAction();
        evalNode.setActivity(activity);
        evalNode.getLanguages().add("Python");

        // Define the Python body program of the decision evaluation node. This program will evaluate the incoming
        // guards of every outgoing control flow of the given decision node, and randomly selects one of these branches
        // whose guard holds. If none of the branches can be taken, then the branch -1 is returned instead. In that
        // case, the new activity will re-evaluate the branches in a loop until it finds some branch that can be taken.
        StringBuilder evalProgram = new StringBuilder();
        evalProgram.append("import random\n");
        evalProgram.append("branches = []\n");

        // Get the incoming guard of the outgoing edges.
        for (int i = 0; i < decisionNode.getOutgoings().size(); i++) {
            ControlFlow edge = (ControlFlow)decisionNode.getOutgoings().get(i);
            String translatedGuard = translator.translateExpression(CifParserHelper.parseIncomingGuard(edge),
                    CifContext.createScoped(decisionNode));
            evalProgram.append("if " + translatedGuard + ": branches.append(" + i + ")\n");
        }

        evalProgram.append("branch = random.choice(branches) if len(branches) > 0 else -1\n");
        evalNode.getBodies().add(evalProgram.toString());

        // Define the control flow between the merge node and the decision evaluation node.
        ControlFlow mergeToEvalFlow = FileHelper.FACTORY.createControlFlow();
        mergeToEvalFlow.setActivity(activity);
        mergeToEvalFlow.setSource(mergeNode);
        mergeToEvalFlow.setTarget(evalNode);

        // Define the inner decision node, which will loop back to the merge node if another evaluation round is needed.
        DecisionNode checkNode = FileHelper.FACTORY.createDecisionNode();
        checkNode.setActivity(activity);

        // Define the object flow from the decision evaluation node to the inner decision node.
        OutputPin evalOutput = evalNode.createOutputValue("branch", UmlPrimitiveType.INTEGER.load(decisionNode));
        ObjectFlow evalToCheckFlow = FileHelper.FACTORY.createObjectFlow();
        evalToCheckFlow.setActivity(activity);
        evalToCheckFlow.setSource(evalOutput);
        evalToCheckFlow.setTarget(checkNode);

        // Define the control flow from the inner decision node to the merge node.
        ControlFlow checkToMergeFlow = FileHelper.FACTORY.createControlFlow();
        checkToMergeFlow.setActivity(activity);
        checkToMergeFlow.setSource(checkNode);
        checkToMergeFlow.setTarget(mergeNode);

        OpaqueExpression checkToMergeGuard = FileHelper.FACTORY.createOpaqueExpression();
        checkToMergeGuard.getLanguages().add("Python");
        checkToMergeGuard.getBodies().add("branch == -1");
        checkToMergeFlow.setGuard(checkToMergeGuard);

        // Define the output parameter of the activity.
        Parameter outputParam = FileHelper.FACTORY.createParameter();
        outputParam.setDirection(ParameterDirectionKind.RETURN_LITERAL);
        outputParam.setName("branch");
        outputParam.setType(UmlPrimitiveType.INTEGER.load(decisionNode));
        activity.getOwnedParameters().add(outputParam);

        // Define the output parameter node of the activity.
        ActivityParameterNode outputParamNode = FileHelper.FACTORY.createActivityParameterNode();
        outputParamNode.setActivity(activity);
        outputParamNode.setParameter(outputParam);
        outputParamNode.setType(UmlPrimitiveType.INTEGER.load(decisionNode));

        // Define the object flow from the inner decision node to the output parameter node.
        ObjectFlow checkToOutputFlow = FileHelper.FACTORY.createObjectFlow();
        checkToOutputFlow.setActivity(activity);
        checkToOutputFlow.setSource(checkNode);
        checkToOutputFlow.setTarget(outputParamNode);

        OpaqueExpression checkToOutputGuard = FileHelper.FACTORY.createOpaqueExpression();
        checkToOutputGuard.getLanguages().add("Python");
        checkToOutputGuard.getBodies().add("else");
        checkToOutputFlow.setGuard(checkToOutputGuard);

        return activity;
    }

    /**
     * Translates the given given list of effects (which are themselves represented as lists of single-line Python
     * programs) to a Python program that performs these effects.
     * <p>
     * If {@code effects} is empty, then the translated Python program will do nothing. If {@code effects} contains
     * exactly one effect (i.e., the corresponding action is deterministic), then the translated Python program will
     * perform this effect under the condition that the action guard holds. If {@code effects} contains multiple effects
     * (i.e., the corresponding action is non-deterministic), then the translated Python program will randomly perform
     * one of the effects, under the condition that the action guard holds.
     * </p>
     *
     * @param effects The list of effects to translate. Every effect must be a list of single-line Python programs.
     * @return The translated Python program that performs the effect(s) as described above.
     */
    public static String translateEffects(List<List<String>> effects) {
        if (effects.isEmpty()) {
            return "pass";
        }

        // Translate the individual effects and ensure proper indentation.
        List<String> translatedEffects = effects.stream().map(effect -> CifToPythonTranslator
                .mergeAll(CifToPythonTranslator.increaseIndentation(effect), "\n").orElse("pass")).toList();

        if (translatedEffects.size() == 1) {
            // If there is only one effect, i.e., the corresponding action is deterministic, then return a Python
            // program that performs the effect under the condition that the action guard holds.
            return String.format("if guard:\n%s", translatedEffects.get(0));
        } else {
            // If there are multiple effects, i.e., the corresponding action is non-deterministic, then return a Python
            // program that generates a random number and performs one of the effects based on this generated number.

            // First we 'extend' the translated effects with randomization as described above.
            List<String> extendedEffects = new ArrayList<>(translatedEffects.size() + 1);
            extendedEffects.add(String.format("__branch = random.randint(0, %d)", translatedEffects.size() - 1));

            for (int i = 0; i < translatedEffects.size(); i++) {
                extendedEffects.add(String.format("if __branch == %d:\n%s", i, translatedEffects.get(i)));
            }

            // Then we return a Python program that randomly performs one of the effects if the action guard holds.
            return String.format("if guard:\n%s", CifToPythonTranslator
                    .mergeAll(CifToPythonTranslator.increaseIndentation(extendedEffects), "\n").get());
        }
    }

    private static void passTemplateArgument(CallBehaviorAction callAction, OpaqueAction assignmentAction,
            String argumentName)
    {
        // Create an output pin on the assignment action and an input pin on the call action
        OutputPin outputPin = assignmentAction.createOutputValue(UMLToCameoTransformer.PARAM_PREFIX + argumentName,
                null);
        InputPin inputPin = callAction.createArgument(argumentName, null);

        // Connect the output and input pins with an ObjectFlow
        ObjectFlow dataFlow = UMLFactory.eINSTANCE.createObjectFlow();
        dataFlow.setSource(outputPin);
        dataFlow.setTarget(inputPin);
        callAction.getActivity().getEdges().add(dataFlow);
    }

    public static void addTemplateArguments(CallBehaviorAction callAction, Set<String> passedArguments,
            String templateBody)
    {
        if (passedArguments.isEmpty()) {
            return;
        }

        if (templateBody == null) {
            // Add the temp__ prefix variables
            List<String> translatedAssignments = new ArrayList<>();
            for (String argument: passedArguments) {
                translatedAssignments.add(UMLToCameoTransformer.PARAM_PREFIX + argument + "=" + argument);
            }

            templateBody = CifToPythonTranslator.mergeAll(translatedAssignments, "\n").get();
        }

        Activity parentActivity = callAction.getActivity();

        OpaqueAction assignmentAction = FileHelper.FACTORY.createOpaqueAction();
        assignmentAction.setActivity(parentActivity);

        // Merge all translated assignments into a single Python code block
        assignmentAction.getBodies().add(templateBody);
        assignmentAction.getLanguages().add("Python");

        callAction.getIncomings().get(0).setTarget(assignmentAction);

        // For each assignment, create a data flow from the new action to the original call action
        for (String argumentName: passedArguments) {
            passTemplateArgument(callAction, assignmentAction, argumentName);
        }
    }

    /**
     * Translates the given mapping from integer properties to their integer bounds, to a Python program that checks
     * whether all these integer properties stay within their bounds. If not, then the Python program will print a
     * message.
     *
     * @param propertyBounds The property bounds to translate.
     * @return The translated Python program.
     */
    private static String translatePropertyBounds(Map<String, Range<Integer>> propertyBounds) {
        if (propertyBounds.isEmpty()) {
            return "pass";
        }

        String result = "if guard:";

        for (Map.Entry<String, Range<Integer>> entry: propertyBounds.entrySet()) {
            String property = entry.getKey();
            Range<Integer> bounds = entry.getValue();

            result += String.format("\n\tif not (%d <= %s <= %d):", bounds.getMinimum(), property, bounds.getMaximum());
            result += String.format("\n\t\tprint(\"Expected '%s' to stay between %d and %d, but got \" + str(%s))",
                    property, bounds.getMinimum(), bounds.getMaximum(), property);
        }

        return result;
    }
}
