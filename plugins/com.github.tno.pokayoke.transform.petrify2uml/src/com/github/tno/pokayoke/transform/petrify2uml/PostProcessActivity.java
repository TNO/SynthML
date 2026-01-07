////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.List;
import java.util.Set;

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

import com.github.tno.pokayoke.transform.common.ExprHelper;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.DoubleMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.EquivalentActionsIntoMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionForkMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionMergePattern;
import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.pokayoke.transform.track.SynthesisChainTracking.ActionKind;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

public class PostProcessActivity {
    private PostProcessActivity() {
    }

    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    /**
     * Remove the opaque actions from the activity.
     *
     * @param activity The activity from which to remove the actions.
     * @param actionsToRemove The opaque actions to remove.
     */
    public static void removeOpaqueActions(Activity activity, Set<OpaqueAction> actionsToRemove) {
        for (OpaqueAction action: actionsToRemove) {
            List<ActivityEdge> incomingEdges = action.getIncomings();
            Preconditions.checkArgument(incomingEdges.size() == 1,
                    "Expected that an opaque action has exactly one incoming edge.");
            ActivityEdge incomingEdge = incomingEdges.get(0);

            List<ActivityEdge> outgoingEdges = action.getOutgoings();
            Preconditions.checkArgument(outgoingEdges.size() == 1,
                    "Expected that an opaque action has exactly one outgoing edge.");
            ActivityEdge outgoingEdge = outgoingEdges.get(0);

            Verify.verify(ExprHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(incomingEdge)),
                    "Expected no incoming guard for incoming edge to opaque action to remove.");
            Verify.verify(ExprHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getOutgoingGuard(incomingEdge)),
                    "Expected no outgoing guard for incoming edge to opaque action to remove.");
            Verify.verify(ExprHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(outgoingEdge)),
                    "Expected no incoming guard for outgoing edge from opaque action to remove.");
            Verify.verify(ExprHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getOutgoingGuard(outgoingEdge)),
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
     * @param tracker The synthesis chain tracker.
     * @param warnings Any warnings to notify the user of, which is modified in-place.
     */
    public static void finalizeOpaqueActions(Activity activity, SynthesisChainTracking tracker, List<String> warnings) {
        for (ActivityNode node: List.copyOf(activity.getNodes())) {
            if (node instanceof OpaqueAction action) {
                if (tracker.isTemporaryPetrificationAction(action)) {
                    // If the action is temporary, skip the current action.
                    continue;
                }

                // Get the kind of action, and finalize the opaque action accordingly.
                ActionKind actionKind = tracker.getActionKind(action);
                RedefinableElement umlElement = tracker.getUmlElement(action);

                switch (actionKind) {
                    case COMPLETE_OPAQUE_BEHAVIOR -> {
                        // The action represents a complete opaque behavior. Transform it to a call behavior.
                        CallBehaviorAction callAction = UML_FACTORY.createCallBehaviorAction();
                        callAction.setBehavior((OpaqueBehavior)umlElement);
                        callAction.setActivity(activity);
                        callAction.setName(action.getName());

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(callAction, action);

                        // Redirect the incoming/outgoing control flow edges, and destroy the original action.
                        Verify.verify(action.getIncomings().size() == 1 && action.getOutgoings().size() == 1,
                                "Opaque actions must have a single incoming and outgoing edge.");
                        action.getIncomings().get(0).setTarget(callAction);
                        action.getOutgoings().get(0).setSource(callAction);
                        action.destroy();

                        break;
                    }
                    case START_OPAQUE_BEHAVIOR -> {
                        // The action is the start of a non-merged non-atomic opaque behavior. Add its guards to the
                        // opaque action. Set the atomicity property to 'true'.
                        action.setName(umlElement.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start action '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }
                    case COMPLETE_CALL_BEHAVIOR -> {
                        // The opaque action represents a call behavior action that calls either an atomic opaque
                        // behavior or a rewritten non-atomic opaque behavior. Transform it to a call
                        // behavior.
                        CallBehaviorAction callAction = UML_FACTORY.createCallBehaviorAction();
                        callAction.setBehavior(((CallBehaviorAction)umlElement).getBehavior());
                        callAction.setActivity(activity);
                        callAction.setName(action.getName());

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(callAction, action);

                        // Redirect the incoming/outgoing control flow edges, and destroy the original action.
                        action.getIncomings().get(0).setTarget(callAction);
                        action.getOutgoings().get(0).setSource(callAction);
                        action.destroy();

                        break;
                    }
                    case START_CALL_BEHAVIOR -> {
                        // The opaque action represents the start of a non-rewritten call behavior that calls a
                        // non-atomic opaque behavior. Add the guards of the called opaque behavior to the opaque
                        // action. Set the atomicity property to 'true'.
                        action.setName(action.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        PokaYokeUmlProfileUtil.setGuard(action,
                                PokaYokeUmlProfileUtil.getGuard(((CallBehaviorAction)umlElement).getBehavior()));

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }
                    case COMPLETE_OPAQUE_ACTION -> {
                        // Atomic or rewritten opaque action. Add the original UML element's guard and effects to the
                        // current action. Set the atomicity property based on the UML element's one.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        PokaYokeUmlProfileUtil.setEffects(action, PokaYokeUmlProfileUtil.getEffects(umlElement));
                        PokaYokeUmlProfileUtil.setAtomic(action, PokaYokeUmlProfileUtil.isAtomic(umlElement));
                        action.setName(action.getName());

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(action, action);

                        break;
                    }
                    case START_OPAQUE_ACTION -> {
                        // Non-atomic non-rewritten opaque action: this represents just the start of an opaque action.
                        // Add the original UML element's guard. Set the atomicity property to 'true'.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        action.setName(action.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }
                    case COMPLETE_SHADOW -> {
                        // Atomic or rewritten shadowed call behavior. Add the original UML element's guard and effects
                        // to the current action. Set the atomicity property based on the UML element's one.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        PokaYokeUmlProfileUtil.setEffects(action, PokaYokeUmlProfileUtil.getEffects(umlElement));
                        PokaYokeUmlProfileUtil.setAtomic(action, PokaYokeUmlProfileUtil.isAtomic(umlElement));
                        action.setName(action.getName());

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(action, action);

                        break;
                    }
                    case START_SHADOW -> {
                        // Non-atomic non-rewritten shadowed call behavior: this represents just the start of a shadowed
                        // call. Add the original UML element's guard. Set the atomicity property to 'true'.
                        PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(umlElement));
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        action.setName(action.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic start action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                                umlElement.getName(), action.getName()));

                        break;
                    }
                    case END_OPAQUE_BEHAVIOR, END_CALL_BEHAVIOR, END_OPAQUE_ACTION, END_SHADOW -> {
                        // Sanity check.
                        Verify.verify(action.getName().contains(UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX),
                                "End of non-atomic action name does not contain the non-atomic outcome suffix.");

                        // The action is the end of a non-merged non-atomic UML element. Rename the current action,
                        // set the atomicity property to 'true', set its guard to 'true', and retain the original
                        // relevant effect.
                        int effectIdx = tracker.getEffectIdx(action);
                        action.setName(action.getName().replace(UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX,
                                UmlToCifTranslator.END_ACTION_SUFFIX));
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        PokaYokeUmlProfileUtil.setGuard(action, "true");

                        // If the element is a non-shadowed call behavior, get the called behavior's effects. Otherwise,
                        // get the current UML element effects.
                        String effect;
                        if (umlElement instanceof CallBehaviorAction cbAction
                                && !PokaYokeUmlProfileUtil.isFormalElement(cbAction))
                        {
                            effect = PokaYokeUmlProfileUtil.getEffects(cbAction.getBehavior()).get(effectIdx);
                        } else {
                            effect = PokaYokeUmlProfileUtil.getEffects(umlElement).get(effectIdx);
                        }
                        PokaYokeUmlProfileUtil.setEffects(action, List.of(effect));

                        // Store the finalized UML element in the synthesis chain tracker.
                        tracker.addFinalizedUmlElement(action, action);

                        // Add a warning that the non-atomic end action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit end action '%s'.",
                                umlElement.getName(), action.getName()));

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
}
