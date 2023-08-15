package nl.esi.pokayoke.transform.annotateduml;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.uml2.uml.AcceptEventAction;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.ForkNode;
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

/**
 * Helper class for creating various kinds of activities.
 */
public class ActivityHelper {
	private ActivityHelper() {
	}

	/**
	 * Creates an activity that waits until {@code guard} becomes {@code true} and
	 * then executes {@code effect}. The evaluation (attempt) of {@code guard} and
	 * execution of {@code effect} happen together atomically.
	 * 
	 * @param guard  A Python expression returning a boolean.
	 * @param effect A Python program.
	 * @return The created activity that executes atomically.
	 */
	public static Activity createAtomicActivity(String guard, String effect, Signal acquire) {
		// Define a new activity that encodes the 'guard' and 'effect'.
		Activity activity = FileHelper.factory.createActivity();

		// Define the initial node.
		InitialNode initNode = FileHelper.factory.createInitialNode();
		initNode.setActivity(activity);

		// Define the outer merge node.
		MergeNode outerMergeNode = FileHelper.factory.createMergeNode();
		outerMergeNode.setActivity(activity);

		// Define the control flow from 'initNode' to 'outerMergeNode'.
		ControlFlow initToOuterMergeFlow = FileHelper.factory.createControlFlow();
		initToOuterMergeFlow.setActivity(activity);
		initToOuterMergeFlow.setSource(initNode);
		initToOuterMergeFlow.setTarget(outerMergeNode);

		// Define the acquire signal send action.
		SendSignalAction sendAcquireNode = FileHelper.factory.createSendSignalAction();
		sendAcquireNode.setActivity(activity);
		sendAcquireNode.setSignal(acquire);
		InputPin sendAcquireInput = FileHelper.factory.createInputPin();
		sendAcquireInput.setName("requester");
		sendAcquireInput.setType(FileHelper.loadPrimitiveType("String"));
		sendAcquireNode.getArguments().add(sendAcquireInput);

		// Define the requester value specification node.
		UUID activityRequesterId = UUID.randomUUID();
		ValueSpecificationAction requesterValueNode = FileHelper.factory.createValueSpecificationAction();
		requesterValueNode.setActivity(activity);
		OutputPin requesterValueOutput = FileHelper.factory.createOutputPin();
		requesterValueOutput.setName("requester");
		requesterValueOutput.setType(FileHelper.loadPrimitiveType("String"));
		requesterValueNode.setResult(requesterValueOutput);
		LiteralString requesterValueLiteral = FileHelper.factory.createLiteralString();
		requesterValueLiteral.setValue(activityRequesterId.toString());
		requesterValueNode.setValue(requesterValueLiteral);

		// Define the control flow from 'outerMergeNode' to 'requesterValueNode'.
		ControlFlow mergeToRequesterFlow = FileHelper.factory.createControlFlow();
		mergeToRequesterFlow.setActivity(activity);
		mergeToRequesterFlow.setSource(outerMergeNode);
		mergeToRequesterFlow.setTarget(requesterValueNode);

		// Define the object flow from 'requesterValueNode' to 'sendAcquireNode'.
		ObjectFlow requesterObjectFlow = FileHelper.factory.createObjectFlow();
		requesterObjectFlow.setActivity(activity);
		requesterObjectFlow.setSource(requesterValueOutput);
		requesterObjectFlow.setTarget(sendAcquireInput);

		// Define the inner merge node.
		MergeNode innerMergeNode = FileHelper.factory.createMergeNode();
		innerMergeNode.setActivity(activity);

		// Define the control flow from 'sendAcquireNode' to 'innerMergeNode'.
		ControlFlow acquireToInnerMergeFlow = FileHelper.factory.createControlFlow();
		acquireToInnerMergeFlow.setActivity(activity);
		acquireToInnerMergeFlow.setSource(sendAcquireNode);
		acquireToInnerMergeFlow.setTarget(innerMergeNode);

		// Define the node that checks whether the lock is granted.
		OpaqueAction checkActiveNode = FileHelper.factory.createOpaqueAction();
		checkActiveNode.setActivity(activity);
		checkActiveNode.getBodies().add("active == '" + activityRequesterId.toString() + "'");
		checkActiveNode.getLanguages().add("Python");
		OutputPin checkActiveOutput = checkActiveNode.createOutputValue("isActive",
				FileHelper.loadPrimitiveType("Boolean"));

		// Define the control flow from 'innerMergeNode' to 'checkActiveNode'.
		ControlFlow innerMergeToCheckActiveFlow = FileHelper.factory.createControlFlow();
		innerMergeToCheckActiveFlow.setActivity(activity);
		innerMergeToCheckActiveFlow.setSource(innerMergeNode);
		innerMergeToCheckActiveFlow.setTarget(checkActiveNode);

		// Define the inner decision node.
		DecisionNode innerDecisionNode = FileHelper.factory.createDecisionNode();
		innerDecisionNode.setActivity(activity);

		// Define the control flow from 'checkActiveNode' to 'innerDecisionNode'.
		ControlFlow checkActiveToInnerDecisionFlow = FileHelper.factory.createControlFlow();
		checkActiveToInnerDecisionFlow.setActivity(activity);
		checkActiveToInnerDecisionFlow.setSource(checkActiveNode);
		checkActiveToInnerDecisionFlow.setTarget(innerDecisionNode);

		// Define the object flow from 'checkActiveNode' to 'innerDecisionNode'.
		ObjectFlow checkActiveToInnerDecisionObjFlow = FileHelper.factory.createObjectFlow();
		checkActiveToInnerDecisionObjFlow.setActivity(activity);
		checkActiveToInnerDecisionObjFlow.setSource(checkActiveOutput);
		checkActiveToInnerDecisionObjFlow.setTarget(innerDecisionNode);

		// Define the opaque action of 'activity' that encodes the guard and effect.
		OpaqueAction guardAndEffectNode = FileHelper.factory.createOpaqueAction();
		guardAndEffectNode.setActivity(activity);
		guardAndEffectNode.getLanguages().add("Python");
		String guardAndEffectBody = String.format("guard = %s\n%s\nactive = ''\nisSuccessful = guard", guard, effect);
		guardAndEffectNode.getBodies().add(guardAndEffectBody);
		OutputPin guardAndEffectOutput = guardAndEffectNode.createOutputValue("isSuccessful",
				FileHelper.loadPrimitiveType("Boolean"));

		// Define the control flow from 'innerDecisionNode' to 'guardAndEffectNode'.
		ControlFlow innerDecisionToGuardAndEffectFlow = FileHelper.factory.createControlFlow();
		innerDecisionToGuardAndEffectFlow.setActivity(activity);
		innerDecisionToGuardAndEffectFlow.setSource(innerDecisionNode);
		innerDecisionToGuardAndEffectFlow.setTarget(guardAndEffectNode);
		OpaqueExpression innerDecisionToGuardAndEffectGuard = FileHelper.factory.createOpaqueExpression();
		innerDecisionToGuardAndEffectGuard.getBodies().add("isActive");
		innerDecisionToGuardAndEffectGuard.getLanguages().add("Python");
		innerDecisionToGuardAndEffectFlow.setGuard(innerDecisionToGuardAndEffectGuard);

		// Define the control flow from 'innerDecisionNode' to 'innerMergeNode'.
		ControlFlow innerDecisionToInnerMergeFlow = FileHelper.factory.createControlFlow();
		innerDecisionToInnerMergeFlow.setActivity(activity);
		innerDecisionToInnerMergeFlow.setSource(innerDecisionNode);
		innerDecisionToInnerMergeFlow.setTarget(innerMergeNode);
		OpaqueExpression innerDecisionToInnerMergeGuard = FileHelper.factory.createOpaqueExpression();
		innerDecisionToInnerMergeGuard.getBodies().add("else");
		innerDecisionToInnerMergeGuard.getLanguages().add("Python");
		innerDecisionToInnerMergeFlow.setGuard(innerDecisionToInnerMergeGuard);

		// Define the outer decision node.
		DecisionNode outerDecisionNode = FileHelper.factory.createDecisionNode();
		outerDecisionNode.setActivity(activity);

		// Define the control flow from 'guardAndEffectNode' to 'outerDecisionNode'.
		ControlFlow guardAndEffectToOuterDecisionFlow = FileHelper.factory.createControlFlow();
		guardAndEffectToOuterDecisionFlow.setActivity(activity);
		guardAndEffectToOuterDecisionFlow.setSource(guardAndEffectNode);
		guardAndEffectToOuterDecisionFlow.setTarget(outerDecisionNode);

		// Define the object flow from 'guardAndEffectNode' to 'outerDecisionNode'.
		ObjectFlow guardAndEffectToOuterDecisionObjFlow = FileHelper.factory.createObjectFlow();
		guardAndEffectToOuterDecisionObjFlow.setActivity(activity);
		guardAndEffectToOuterDecisionObjFlow.setSource(guardAndEffectOutput);
		guardAndEffectToOuterDecisionObjFlow.setTarget(outerDecisionNode);

		// Define the final node.
		FinalNode finalNode = FileHelper.factory.createActivityFinalNode();
		finalNode.setActivity(activity);

		// Define the control flow from 'outerDecisionNode' to 'finalNode'.
		ControlFlow outerDecisionToFinalFlow = FileHelper.factory.createControlFlow();
		outerDecisionToFinalFlow.setActivity(activity);
		outerDecisionToFinalFlow.setSource(outerDecisionNode);
		outerDecisionToFinalFlow.setTarget(finalNode);
		OpaqueExpression outerDecisionToFinalGuard = FileHelper.factory.createOpaqueExpression();
		outerDecisionToFinalGuard.getBodies().add(guardAndEffectOutput.getName());
		outerDecisionToFinalGuard.getLanguages().add("Python");
		outerDecisionToFinalFlow.setGuard(outerDecisionToFinalGuard);

		// Define the control flow from 'outerDecisionNode' to 'outerMergeNode'.
		ControlFlow outerDecisionToOuterMergeFlow = FileHelper.factory.createControlFlow();
		outerDecisionToOuterMergeFlow.setActivity(activity);
		outerDecisionToOuterMergeFlow.setSource(outerDecisionNode);
		outerDecisionToOuterMergeFlow.setTarget(outerMergeNode);
		OpaqueExpression outerDecisionToOuterMergeGuard = FileHelper.factory.createOpaqueExpression();
		outerDecisionToOuterMergeGuard.getBodies().add("else");
		outerDecisionToOuterMergeGuard.getLanguages().add("Python");
		outerDecisionToOuterMergeFlow.setGuard(outerDecisionToOuterMergeGuard);

		return activity;
	}

	/**
	 * Creates an activity that handles lock acquisition, by listening for
	 * {@code acquireEvent} signal events and updating the shared variable 'active'
	 * accordingly.
	 * 
	 * @param acquireEvent The acquire signal event to listen to.
	 * @return The created lock handling activity.
	 */
	public static Activity createLockHanderActivity(SignalEvent acquireEvent) {
		Signal acquireSignal = acquireEvent.getSignal();
		Property acquireParameter = acquireSignal.getOwnedAttributes().get(0);

		// Create the activity that handles lock acquisition.
		Activity activity = FileHelper.factory.createActivity();

		// Define the initial node.
		InitialNode initNode = FileHelper.factory.createInitialNode();
		initNode.setActivity(activity);

		// Define the outer merge node.
		MergeNode outerMergeNode = FileHelper.factory.createMergeNode();
		outerMergeNode.setActivity(activity);

		// Define the control flow between 'initNode' and 'outerMergeNode'.
		ControlFlow initToOuterMergeFlow = FileHelper.factory.createControlFlow();
		initToOuterMergeFlow.setActivity(activity);
		initToOuterMergeFlow.setSource(initNode);
		initToOuterMergeFlow.setTarget(outerMergeNode);

		// Define the node that accepts acquire signals.
		AcceptEventAction acceptAcquireNode = FileHelper.factory.createAcceptEventAction();
		acceptAcquireNode.setActivity(activity);
		Trigger acquireTrigger = FileHelper.factory.createTrigger();
		acquireTrigger.setEvent(acquireEvent);
		acceptAcquireNode.getTriggers().add(acquireTrigger);
		OutputPin acceptAcquireOutput = acceptAcquireNode.createResult("msg", acquireSignal);
		acceptAcquireOutput.setIsOrdered(true);
		acceptAcquireOutput.setIsUnique(false);

		// Define the control flow between 'outerMergeNode' and 'acceptAcquireNode'.
		ControlFlow mergeToAcceptAcquireFlow = FileHelper.factory.createControlFlow();
		mergeToAcceptAcquireFlow.setActivity(activity);
		mergeToAcceptAcquireFlow.setSource(outerMergeNode);
		mergeToAcceptAcquireFlow.setTarget(acceptAcquireNode);

		// Define the node that reads the argument from an accepted acquire signal.
		ReadStructuralFeatureAction readRequesterNode = FileHelper.factory.createReadStructuralFeatureAction();
		readRequesterNode.setActivity(activity);
		readRequesterNode.setStructuralFeature(acquireParameter);
		InputPin readRequesterInput = readRequesterNode.createObject(acceptAcquireOutput.getName(), acquireSignal);
		OutputPin readRequesterOutput = readRequesterNode.createResult(acquireParameter.getName(),
				FileHelper.loadPrimitiveType("String"));

		// Define the object flow between 'acceptAcquireNode' and 'readRequesterNode'.
		ObjectFlow acceptAcquireToReadFlow = FileHelper.factory.createObjectFlow();
		acceptAcquireToReadFlow.setActivity(activity);
		acceptAcquireToReadFlow.setSource(acceptAcquireOutput);
		acceptAcquireToReadFlow.setTarget(readRequesterInput);

		// Define the action that updates the static 'active' variable.
		OpaqueAction setActiveNode = FileHelper.factory.createOpaqueAction();
		setActiveNode.setActivity(activity);
		InputPin setActiveInput = setActiveNode.createInputValue(acquireParameter.getName(),
				FileHelper.loadPrimitiveType("String"));
		setActiveNode.getBodies().add("active = " + acquireParameter.getName());
		setActiveNode.getLanguages().add("Python");

		// Define the object flow between 'lockHandlerNode' and 'setActiveNode'.
		ObjectFlow readToSetActiveFlow = FileHelper.factory.createObjectFlow();
		readToSetActiveFlow.setActivity(activity);
		readToSetActiveFlow.setSource(readRequesterOutput);
		readToSetActiveFlow.setTarget(setActiveInput);

		// Define the inner merge node.
		MergeNode innerMergeNode = FileHelper.factory.createMergeNode();
		innerMergeNode.setActivity(activity);

		// Define the control flow between 'setActiveNode' and 'innerMergeNode'.
		ControlFlow setActiveToInnerMergeFlow = FileHelper.factory.createControlFlow();
		setActiveToInnerMergeFlow.setActivity(activity);
		setActiveToInnerMergeFlow.setSource(setActiveNode);
		setActiveToInnerMergeFlow.setTarget(innerMergeNode);

		// Define the action that checks whether the lock has been released.
		OpaqueAction checkReleasedNode = FileHelper.factory.createOpaqueAction();
		checkReleasedNode.setActivity(activity);
		checkReleasedNode.getBodies().add("active == ''");
		checkReleasedNode.getLanguages().add("Python");
		OutputPin checkReleasedOutput = checkReleasedNode.createOutputValue("isReleased",
				FileHelper.loadPrimitiveType("Boolean"));

		// Define the control flow from 'innerMergeNode' to 'checkReleasedNode'.
		ControlFlow innerMergeToCheckReleaseFlow = FileHelper.factory.createControlFlow();
		innerMergeToCheckReleaseFlow.setActivity(activity);
		innerMergeToCheckReleaseFlow.setSource(innerMergeNode);
		innerMergeToCheckReleaseFlow.setTarget(checkReleasedNode);

		// Define the decision node.
		DecisionNode decisionNode = FileHelper.factory.createDecisionNode();
		decisionNode.setActivity(activity);

		// Define the control flow between 'checkReleasedNode' and 'decisionNode'.
		ControlFlow checkReleasedToDecisionFlow = FileHelper.factory.createControlFlow();
		checkReleasedToDecisionFlow.setActivity(activity);
		checkReleasedToDecisionFlow.setSource(checkReleasedNode);
		checkReleasedToDecisionFlow.setTarget(decisionNode);

		// Define the object flow between 'checkReleasedNode' and 'decisionNode'.
		ObjectFlow checkReleasedToDecisionObjFlow = FileHelper.factory.createObjectFlow();
		checkReleasedToDecisionObjFlow.setActivity(activity);
		checkReleasedToDecisionObjFlow.setSource(checkReleasedOutput);
		checkReleasedToDecisionObjFlow.setTarget(decisionNode);

		// Define the control flow between 'decisionNode' and 'outerMergeNode'.
		ControlFlow decisionToOuterMergeFlow = FileHelper.factory.createControlFlow();
		decisionToOuterMergeFlow.setActivity(activity);
		decisionToOuterMergeFlow.setSource(decisionNode);
		decisionToOuterMergeFlow.setTarget(outerMergeNode);
		OpaqueExpression decisionToWaitGuard = FileHelper.factory.createOpaqueExpression();
		decisionToWaitGuard.getBodies().add(checkReleasedOutput.getName());
		decisionToWaitGuard.getLanguages().add("Python");
		decisionToOuterMergeFlow.setGuard(decisionToWaitGuard);

		// Define the control flow between 'decisionNode' and 'innerMergeNode'.
		ControlFlow decisionToInnerMergeFlow = FileHelper.factory.createControlFlow();
		decisionToInnerMergeFlow.setActivity(activity);
		decisionToInnerMergeFlow.setSource(decisionNode);
		decisionToInnerMergeFlow.setTarget(innerMergeNode);
		OpaqueExpression decisionToInnerMergeGuard = FileHelper.factory.createOpaqueExpression();
		decisionToInnerMergeGuard.getBodies().add("else");
		decisionToInnerMergeGuard.getLanguages().add("Python");
		decisionToInnerMergeFlow.setGuard(decisionToInnerMergeGuard);

		return activity;
	}

	/**
	 * Creates a "main" activity that continuously call all activities from
	 * {@code activities} in parallel, and also puts a call to {@code lockHandler}
	 * in parallel.
	 * 
	 * @param activities    The activities to continuously call in parallel.
	 * @param preconditions The preconditions of all activities in
	 *                      {@code activities}.
	 * @param lockHandler   The activity that encodes the lock handler.
	 * @return The created precondition activity.
	 */
	public static Activity createMainActivity(List<Activity> activities, Map<Activity, Activity> preconditions,
			Activity lockHandler) {
		// Create the new activity that puts 'activities' in parallel.
		Activity newActivity = FileHelper.factory.createActivity();

		// Define the initial node.
		InitialNode initNode = FileHelper.factory.createInitialNode();
		initNode.setActivity(newActivity);

		// Define the fork node.
		ForkNode forkNode = FileHelper.factory.createForkNode();
		forkNode.setActivity(newActivity);

		// Define the control flow from 'initNode' to 'forkNode'.
		ControlFlow initToFork = FileHelper.factory.createControlFlow();
		initToFork.setActivity(newActivity);
		initToFork.setSource(initNode);
		initToFork.setTarget(forkNode);

		// Define the action that calls the lock handler.
		CallBehaviorAction lockHandlerNode = FileHelper.factory.createCallBehaviorAction();
		lockHandlerNode.setActivity(newActivity);
		lockHandlerNode.setBehavior(lockHandler);

		// Define the control flow from 'forkNode' to 'lockHandlerNode'.
		ControlFlow forkToLockHandlerFlow = FileHelper.factory.createControlFlow();
		forkToLockHandlerFlow.setActivity(newActivity);
		forkToLockHandlerFlow.setSource(forkNode);
		forkToLockHandlerFlow.setTarget(lockHandlerNode);

		// Define a forked behavior call for every activity in 'activities'.
		for (Activity activity : activities) {
			// Define the merge node.
			MergeNode mergeNode = FileHelper.factory.createMergeNode();
			mergeNode.setActivity(newActivity);

			// Define the control flow from 'forkNode' to 'mergeNode'.
			ControlFlow forkToMergeFlow = FileHelper.factory.createControlFlow();
			forkToMergeFlow.setActivity(newActivity);
			forkToMergeFlow.setSource(forkNode);
			forkToMergeFlow.setTarget(mergeNode);

			// Define the action that calls the 'preconditionActivity'.
			CallBehaviorAction preconditionCallNode = FileHelper.factory.createCallBehaviorAction();
			preconditionCallNode.setActivity(newActivity);
			preconditionCallNode.setBehavior(preconditions.get(activity));

			// Define the control flow from 'mergeNode' to 'preconditionCallNode'.
			ControlFlow mergeToPreFlow = FileHelper.factory.createControlFlow();
			mergeToPreFlow.setActivity(newActivity);
			mergeToPreFlow.setSource(mergeNode);
			mergeToPreFlow.setTarget(preconditionCallNode);

			// Define the call behavior action that calls 'activity'.
			CallBehaviorAction activityCallNode = FileHelper.factory.createCallBehaviorAction();
			activityCallNode.setActivity(newActivity);
			activityCallNode.setBehavior(activity);
			activityCallNode.setName(activity.getName());

			// Define the control flow from 'preconditionCallNode' to 'activityCallNode'.
			ControlFlow preToActivityCallFlow = FileHelper.factory.createControlFlow();
			preToActivityCallFlow.setActivity(newActivity);
			preToActivityCallFlow.setSource(preconditionCallNode);
			preToActivityCallFlow.setTarget(activityCallNode);

			// Define the control flow from 'activityCallNode' to 'mergeNode'.
			ControlFlow activityCallToMergeEdge = FileHelper.factory.createControlFlow();
			activityCallToMergeEdge.setActivity(newActivity);
			activityCallToMergeEdge.setSource(activityCallNode);
			activityCallToMergeEdge.setTarget(mergeNode);
		}

		return newActivity;

	}

	/**
	 * Creates an activity that waits until the precondition of {@code activity}
	 * becomes true.
	 * 
	 * @param activity The activity to create the precondition activity for.
	 * @return The created precondition activity.
	 */
	public static Activity createPreconditionActivityFor(Activity activity) {
		// Determine the precondition of 'activity'.
		String precondition = activity.getPreconditions().stream().map(c -> c.getSpecification())
				.filter(s -> s instanceof OpaqueExpression).map(s -> (OpaqueExpression) s)
				.flatMap(s -> s.getBodies().stream()).collect(Collectors.joining(" and "));

		if (precondition.isEmpty()) {
			precondition = "True";
		}

		// Create a waiting activity that waits on 'precondition'.
		return createWaitingActivity(precondition);
	}

	/**
	 * Creates an activity that waits until {@code guard} is true before it
	 * finalizes. The waiting mechanism is implemented by means of a busy loop
	 * (i.e., polling).
	 * 
	 * @param guard A Boolean Python expression.
	 * @return The created activity.
	 */
	public static Activity createWaitingActivity(String guard) {
		// Create the activity.
		Activity activity = FileHelper.factory.createActivity();

		// Define the initial node.
		InitialNode initNode = FileHelper.factory.createInitialNode();
		initNode.setActivity(activity);

		// Define the merge node.
		MergeNode mergeNode = FileHelper.factory.createMergeNode();
		mergeNode.setActivity(activity);

		// Define the control flow between 'initNode' and 'mergeNode'.
		ControlFlow initToMergeFlow = FileHelper.factory.createControlFlow();
		initToMergeFlow.setActivity(activity);
		initToMergeFlow.setSource(initNode);
		initToMergeFlow.setTarget(mergeNode);

		// Define the action that checks whether the guard holds.
		OpaqueAction checkGuardNode = FileHelper.factory.createOpaqueAction();
		checkGuardNode.setActivity(activity);
		checkGuardNode.getBodies().add(guard);
		checkGuardNode.getLanguages().add("Python");
		OutputPin checkGuardOutput = checkGuardNode.createOutputValue("doesGuardHold",
				FileHelper.loadPrimitiveType("Boolean"));

		// Define the control flow between 'mergeNode' and 'checkGuardNode'.
		ControlFlow mergeToCheckGuardFlow = FileHelper.factory.createControlFlow();
		mergeToCheckGuardFlow.setActivity(activity);
		mergeToCheckGuardFlow.setSource(mergeNode);
		mergeToCheckGuardFlow.setTarget(checkGuardNode);

		// Define the decision node.
		DecisionNode decisionNode = FileHelper.factory.createDecisionNode();
		decisionNode.setActivity(activity);

		// Define the control flow from 'checkGuardNode' to 'decisionNode'.
		ControlFlow checkGuardToDecisionFlow = FileHelper.factory.createControlFlow();
		checkGuardToDecisionFlow.setActivity(activity);
		checkGuardToDecisionFlow.setSource(checkGuardNode);
		checkGuardToDecisionFlow.setTarget(decisionNode);

		// Define the object flow between 'checkGuardNode' and 'decisionNode'.
		ObjectFlow checkGuardToDecisionObjFlow = FileHelper.factory.createObjectFlow();
		checkGuardToDecisionObjFlow.setActivity(activity);
		checkGuardToDecisionObjFlow.setSource(checkGuardOutput);
		checkGuardToDecisionObjFlow.setTarget(decisionNode);

		// Define the final node.
		FinalNode finalNode = FileHelper.factory.createActivityFinalNode();
		finalNode.setActivity(activity);

		// Define the control flow from 'decisionNode' to 'finalNode'.
		ControlFlow decisionToFinalFlow = FileHelper.factory.createControlFlow();
		decisionToFinalFlow.setActivity(activity);
		decisionToFinalFlow.setSource(decisionNode);
		decisionToFinalFlow.setTarget(finalNode);
		OpaqueExpression decisionToFinalGuard = FileHelper.factory.createOpaqueExpression();
		decisionToFinalGuard.getBodies().add(checkGuardOutput.getName());
		decisionToFinalGuard.getLanguages().add("Python");
		decisionToFinalFlow.setGuard(decisionToFinalGuard);

		// Define the control flow from 'decisionNode' to 'mergeNode'.
		ControlFlow decisionToMergeFlow = FileHelper.factory.createControlFlow();
		decisionToMergeFlow.setActivity(activity);
		decisionToMergeFlow.setSource(decisionNode);
		decisionToMergeFlow.setTarget(mergeNode);
		OpaqueExpression decisionToMergeGuard = FileHelper.factory.createOpaqueExpression();
		decisionToMergeGuard.getBodies().add("else");
		decisionToMergeGuard.getLanguages().add("Python");
		decisionToMergeFlow.setGuard(decisionToMergeGuard);

		return activity;
	}

	/**
	 * Removes irrelevant and redundant information from {@code activity}, like edge
	 * weights or redundant edge guards.
	 * 
	 * @param activity The activity to clean up.
	 */
	public static void removeIrrelevantInformation(Activity activity) {
		// Remove any weights from all edges.
		for (ActivityEdge edge : activity.getEdges()) {
			edge.setWeight(null);
		}

		// Remove the guards from all edges not coming out of decision nodes.
		for (ActivityEdge edge : activity.getEdges()) {
			if (!(edge.getSource() instanceof DecisionNode)) {
				edge.setGuard(null);
			}
		}
	}
}
