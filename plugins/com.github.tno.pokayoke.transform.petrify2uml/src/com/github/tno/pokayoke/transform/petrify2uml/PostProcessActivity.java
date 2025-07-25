
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.List;
import java.util.Set;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.UMLFactory;

import com.github.tno.pokayoke.transform.activitysynthesis.CifSourceSinkLocationTransformer;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.DoubleMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.EquivalentActionsIntoMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionForkMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionMergePattern;
import com.github.tno.pokayoke.transform.track.SynthesisUmlElementTracking;
import com.github.tno.pokayoke.transform.track.SynthesisUmlElementTracking.ActionKind;
import com.github.tno.pokayoke.transform.track.UmlElementInfo;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

public class PostProcessActivity {
    private PostProcessActivity() {
    }

    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    /**
     * Remove the internal actions that were added in CIF specification and petrification.
     *
     * @param activity The activity in which actions to be removed.
     * @param synthesisTracker The tracker containing the original UML element and information about the synthesis chain
     *     manipulations for each action.
     */
    public static void removeInternalActions(Activity activity, SynthesisUmlElementTracking synthesisTracker) {
        Set<String> internalActionNames = synthesisTracker.getInternalActionNames();

        // Remove all internal opaque actions that were found.
        for (String internalActionName: internalActionNames) {
            removeOpaqueActions(internalActionName, activity);
        }
    }

    /**
     * Remove opaque actions from activity.
     *
     * @param actionName The name of the opaque actions to remove.
     * @param activity The activity from which to remove the actions.
     * @return Number of actions that were removed.
     */
    public static int removeOpaqueActions(String actionName, Activity activity) {
        List<ActivityNode> nodes = activity.getNodes().stream().filter(node -> node.getName() != null)
                .filter(node -> node.getName().equals(actionName)).toList();
        List<OpaqueAction> actions = nodes.stream().filter(OpaqueAction.class::isInstance).map(OpaqueAction.class::cast)
                .toList();
        int numberOfActions = actions.size();

        for (OpaqueAction action: actions) {
            List<ActivityEdge> incomingEdges = action.getIncomings();
            Preconditions.checkArgument(incomingEdges.size() == 1,
                    "Expected that an opaque action has exactly one incoming edge.");
            ActivityEdge incomingEdge = incomingEdges.get(0);

            List<ActivityEdge> outgoingEdges = action.getOutgoings();
            Preconditions.checkArgument(outgoingEdges.size() == 1,
                    "Expected that an opaque action has exactly one ougoing edge.");
            ActivityEdge outgoingEdge = outgoingEdges.get(0);

            Verify.verify(isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(incomingEdge)),
                    "Expected no incoming guard for incoming edge to opaque action to remove.");
            Verify.verify(isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getOutgoingGuard(incomingEdge)),
                    "Expected no outgoing guard for incoming edge to opaque action to remove.");
            Verify.verify(isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(outgoingEdge)),
                    "Expected no incoming guard for outgoing edge from opaque action to remove.");
            Verify.verify(isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getOutgoingGuard(outgoingEdge)),
                    "Expected no outgoing guard for outgoing edge from opaque action to remove.");

            ActivityNode source = incomingEdge.getSource();
            ActivityNode target = outgoingEdge.getTarget();

            // Add a new control flow from source to target.
            PNML2UMLTranslator.createControlFlow(activity, source, target);

            // Destroy the action and its incoming and outgoing edges.
            incomingEdge.destroy();
            outgoingEdge.destroy();
            action.destroy();
        }
        return numberOfActions;
    }

    private static boolean isNullOrTriviallyTrue(String s) {
        return s == null || s.equals("true");
    }

    /**
     * Remove the names of edges, and nodes that are not call behavior actions or opaque actions.
     *
     * @param activity The activity from which to remove names of nodes and edges.
     */
    public static void removeNodesEdgesNames(Activity activity) {
        activity.getEdges().stream().forEach(edge -> edge.unsetName());
        List<ActivityNode> nodes = activity.getNodes().stream()
                .filter(node -> !(node instanceof OpaqueAction) && !(node instanceof CallBehaviorAction)).toList();
        nodes.stream().forEach(node -> node.unsetName());
    }

    /**
     * Simplifies the given activity.
     *
     * @param activity The activity to simplify, which is modified in-place.
     */
    public static void simplify(Activity activity) {
        while (true) {
            boolean changed = false;

            changed |= RedundantDecisionMergePattern.findAndRewriteAll(activity);
            changed |= RedundantDecisionForkMergePattern.findAndRewriteAll(activity);
            changed |= EquivalentActionsIntoMergePattern.findAndRewriteAll(activity);
            changed |= DoubleMergePattern.findAndRewriteAll(activity);

            if (!changed) {
                break;
            }
        }
    }

    /**
     * Finalize all opaque actions in the given activity. They may for instance become call behavior actions, or get
     * extra guards/effects.
     *
     * @param activity The activity for which to finalize the opaque actions.
     * @param synthesisTracker The tracker containing the original UML element and information about the synthesis chain
     *     manipulations for each action.
     * @param warnings Any warnings to notify the user of, which is modified in-place.
     */
    public static void finalizeOpaqueActions(Activity activity, SynthesisUmlElementTracking synthesisTracker,
            List<String> warnings)
    {
        // Iterate over all nodes in the activity that start or end a non-atomic action, but haven't yet been rewritten.
        for (ActivityNode node: List.copyOf(activity.getNodes())) {
            if (node instanceof Action action) {
                if (!(action instanceof OpaqueAction)) {
                    throw new RuntimeException(
                            "Expected only opaque actions, found: " + action.getClass().getSimpleName());
                }

                // Distinguish between start/end actions, atomic/non-atomic actions, opaque behaviors/actions, and
                // rewrite them accordingly. Note that the activity does *not* contain the ends of atomic
                // non-deterministic actions at this point, since they have been filtered out previously.

                if (isInternalAction(action)) {
                    // If the action is internal, skip the current action.
                    continue;
                }

                // Get the kind of action, and finalize the opaque action accordingly.
                ActionKind actionKind = synthesisTracker.getActionKind(action);
                RedefinableElement umlElement = synthesisTracker.getUmlElementInfo(action).getUmlElement();

                switch (actionKind) {
                    case COMPLETE_OPAQUE_BEHAVIOR -> {
                        // The action represents an atomic opaque behavior, or the start of a rewritten non-atomic
                        // action. Transform it to a call behavior.
                        CallBehaviorAction callAction = UML_FACTORY.createCallBehaviorAction();
                        callAction.setBehavior((OpaqueBehavior)umlElement);
                        callAction.setActivity(activity);
                        callAction.setName(action.getName());

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(callAction, action);

                        // Redirect the incoming/outgoing control flow edges, and destroy the original action.
                        action.getIncomings().get(0).setTarget(callAction);
                        action.getOutgoings().get(0).setSource(callAction);
                        action.destroy();

                        break;
                    }
                    case START_OPAQUE_BEHAVIOR -> {
                        // The action is the start of a non-rewritten non-atomic opaque behavior. Add its guards to the
                        // opaque action.
                        action.setName(action.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }
                    case COMPLETE_CALL_BEHAVIOR -> {
                        // The action represents an atomic call behavior, or the start of a rewritten non-atomic
                        // call behavior. Transform it to a call behavior.
                        CallBehaviorAction callAction = UML_FACTORY.createCallBehaviorAction();
                        callAction.setBehavior(((CallBehaviorAction)umlElement).getBehavior());
                        callAction.setActivity(activity);
                        callAction.setName(action.getName());

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(callAction, action);

                        // Redirect the incoming/outgoing control flow edges, and destroy the original action.
                        action.getIncomings().get(0).setTarget(callAction);
                        action.getOutgoings().get(0).setSource(callAction);
                        action.destroy();

                        break;
                    }
                    case START_CALL_BEHAVIOR -> {
                        // The action is the start of a non-rewritten non-atomic call behavior. Add the guards of the
                        // called behavior to the opaque action.
                        action.setName(action.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }
                    case COMPLETE_OPAQUE_ACTION -> {
                        // Atomic or rewritten opaque actions. Add the original UML element's guard and effects to the
                        // current action.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        PokaYokeUmlProfileUtil.setEffects(action, PokaYokeUmlProfileUtil.getEffects(umlElement));
                        PokaYokeUmlProfileUtil.setAtomic(action, PokaYokeUmlProfileUtil.isAtomic(umlElement));
                        action.setName(action.getName());

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(action, action);

                        break;
                    }
                    case START_OPAQUE_ACTION -> {
                        // Non-atomic non-rewritten opaque action: this represents just the start of an opaque action.
                        // Add the original UML element's guard.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        action.setName(action.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }
                    case COMPLETE_SHADOW -> {
                        // Atomic or rewritten shadowed call behaviors. Add the original UML element's guard and effects
                        // to the current action.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        PokaYokeUmlProfileUtil.setEffects(action, PokaYokeUmlProfileUtil.getEffects(umlElement));
                        PokaYokeUmlProfileUtil.setAtomic(action, PokaYokeUmlProfileUtil.isAtomic(umlElement));
                        action.setName(action.getName());

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(action, action);

                        break;
                    }
                    case START_SHADOW -> {
                        // Non-atomic non-rewritten shadowed call behavior: this represents just the start of a shadowed
                        // call. Add the original UML element's guard.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        action.setName(action.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }

                    case END_OPAQUE_BEHAVIOR, END_OPAQUE_ACTION, END_SHADOW, END_CALL_BEHAVIOR -> {
                        // Sanity check.
                        Verify.verify(action.getName().contains(SynthesisUmlElementTracking.NONATOMIC_OUTCOME_SUFFIX),
                                "End of non-atomic action name does not contain the non-atomic outcome suffix.");

                        // The action is the end of a non-rewritten non-atomic action. Add its effects to the opaque
                        // action.

                        // Find the UML element for the non-atomic action, and the index to the relevant effect.
                        UmlElementInfo umlElementInfo = synthesisTracker.getUmlElementInfo(action);
                        RedefinableElement actionElement = umlElementInfo.getUmlElement();
                        int effectIdx = umlElementInfo.getEffectIdx();

                        // Rename the current action, set its guard to 'true', and retain the original relevant effect.
                        action.setName(action.getName().replace(SynthesisUmlElementTracking.NONATOMIC_OUTCOME_SUFFIX,
                                UmlToCifTranslator.END_ACTION_SUFFIX));
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        PokaYokeUmlProfileUtil.setGuard(action, "true");

                        // If the element is a non-shadowed call behavior, get the called behavior's effects.
                        String effect;
                        if (actionElement instanceof CallBehaviorAction cbAction
                                && !PokaYokeUmlProfileUtil.isFormalElement(actionElement))
                        {
                            effect = PokaYokeUmlProfileUtil.getEffects(cbAction.getBehavior()).get(effectIdx);
                        } else {
                            effect = PokaYokeUmlProfileUtil.getEffects(actionElement).get(effectIdx);
                        }
                        PokaYokeUmlProfileUtil.setEffects(action, List.of(effect));

                        // Store the new UML element in the synthesis transformation tracker.
                        synthesisTracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic end action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit end event '%s'.",
                                actionElement.getName(), action.getName()));

                        break;
                    }

                    default -> {
                        throw new RuntimeException("Found unexpected action kind: " + actionKind);
                    }
                }
            } else if (!(node instanceof DecisionNode || node instanceof MergeNode || node instanceof JoinNode
                    || node instanceof ForkNode || node instanceof InitialNode || node instanceof ActivityFinalNode))
            {
                throw new RuntimeException("Found unexpected node type: " + node.getClass().getSimpleName());
            }
        }
    }

    private static boolean isInternalAction(Action action) {
        return action.getName().equals(CifSourceSinkLocationTransformer.START_EVENT_NAME)
                || action.getName().equals(CifSourceSinkLocationTransformer.END_EVENT_NAME);
    }
}
