
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
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
import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.DoubleMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.EquivalentActionsIntoMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionForkMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionMergePattern;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.PetriNet;

public class PostProcessActivity {
    private PostProcessActivity() {
    }

    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    /**
     * Remove the internal actions that were added in CIF specification and petrification.
     *
     * @param activity The activity in which actions to be removed.
     */
    public static void removeInternalActions(Activity activity) {
        // Find all names of internal actions in the activity, which are the opaque actions whose name contain '__'.
        Set<String> internalActionNames = activity.getNodes().stream()
                .filter(node -> node instanceof OpaqueAction && node.getName().contains("__"))
                .map(node -> node.getName()).collect(Collectors.toCollection(LinkedHashSet::new));

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

            Verify.verify(PokaYokeUmlProfileUtil.getIncomingGuard(incomingEdge) == null,
                    "Expected no incoming guard for incoming edge to opaque action to remove.");
            Verify.verify(PokaYokeUmlProfileUtil.getIncomingGuard(incomingEdge) == null,
                    "Expected no outgoing guard for incoming edge to opaque action to remove.");
            Verify.verify(PokaYokeUmlProfileUtil.getIncomingGuard(incomingEdge) == null,
                    "Expected no incoming guard for outgoing edge from opaque action to remove.");
            Verify.verify(PokaYokeUmlProfileUtil.getIncomingGuard(incomingEdge) == null,
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
     * Rewrites all actions in the given activity which start or end a non-atomic action, that have not been rewritten
     * as {@link NonAtomicPatternRewriter#findAndRewritePatterns(PetriNet) non-atomic patterns}.
     *
     * @param activity The activity to rewrite.
     * @param rewrittenActions All actions that have already been rewritten (on Petri Net level).
     * @param endEventMap The mapping from non-atomic/non-deterministic CIF end event names to their corresponding UML
     *     elements and the index of the corresponding effect of the end event.
     * @param nonAtomicOutcomeSuffix The name suffix that was used to indicate a non-atomic action outcome.
     * @param warnings Any warnings to notify the user of, which is modified in-place.
     */
    public static void rewriteLeftoverNonAtomicActions(Activity activity, Set<Action> rewrittenActions,
            Map<String, Pair<RedefinableElement, Integer>> endEventMap, String nonAtomicOutcomeSuffix,
            List<String> warnings)
    {
        CifContext context = new CifContext(activity.getModel());

        // Iterate over all nodes in the activity that start or end a non-atomic action, but haven't yet been rewritten.
        for (ActivityNode node: List.copyOf(activity.getNodes())) {
            if (node instanceof Action action) {
                if (!(action instanceof OpaqueAction)) {
                    throw new RuntimeException(
                            "Expected only opaque actions, found: " + action.getClass().getSimpleName());
                }

                // Rewrite/adapt the opaque action, if needed.
                String actionName = action.getName();
                Behavior behavior = context.getOpaqueBehavior(actionName);
                if (behavior != null
                        && (PokaYokeUmlProfileUtil.isAtomic(behavior) || rewrittenActions.contains(action)))
                {
                    // Atomic opaque behavior, or start of a rewritten non-atomic action. Transform it to a call
                    // behavior.
                    CallBehaviorAction callAction = UML_FACTORY.createCallBehaviorAction();
                    callAction.setBehavior(behavior);
                    callAction.setActivity(activity);
                    callAction.setName(action.getName());

                    // Redirect the incoming/outgoing control flow edges, and destroy the original action.
                    action.getIncomings().get(0).setTarget(callAction);
                    action.getOutgoings().get(0).setSource(callAction);
                    action.destroy();
                } else if (behavior != null) {
                    // The action is the start of a non-rewritten non-atomic opaque behavior. Add its guards to the
                    // opaque action.
                    action.setName(behavior.getName() + UmlToCifTranslator.START_ACTION_SUFFIX);
                    PokaYokeUmlProfileUtil.setAtomic(action, true);
                    PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(behavior));

                    // Add a warning that the non-atomic start action has not been fully merged.
                    warnings.add(String.format(
                            "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                            behavior.getName(), actionName));
                } else if (!isInternalAction(action)) {
                    // Sanity check.
                    Verify.verify(!rewrittenActions.contains(action),
                            "The end of a non-atomic action is contained in the set of rewritten actions.");
                    Verify.verify(actionName.contains(nonAtomicOutcomeSuffix),
                            "End of non-atomic action name does not contain the non-atomic outcome suffix.");

                    // The action is the end of a non-rewritten non-atomic action. Add its effects to the opaque action.

                    // Find the UML element for the non-atomic action, and the index to the relevant effect.
                    Pair<RedefinableElement, Integer> actionAndEffectIndex = endEventMap.get(actionName);
                    Verify.verifyNotNull(actionAndEffectIndex, String
                            .format("Expected the CIF end event '%s' to map to a non-atomic UML element.", actionName));

                    // Determine the UML opaque behavior that has the guard and effects of the current action.
                    RedefinableElement actionElement = actionAndEffectIndex.left;
                    if (actionElement instanceof CallBehaviorAction cbAction) {
                        actionElement = cbAction.getBehavior();
                    }
                    Verify.verify(actionElement instanceof OpaqueBehavior,
                            "Expected an opaque behavior, found: " + actionElement.getClass().getSimpleName());

                    // Rename the current action, set its guard to 'true', and retain the original relevant effect.
                    action.setName(actionName.replace(nonAtomicOutcomeSuffix, UmlToCifTranslator.END_ACTION_SUFFIX));
                    PokaYokeUmlProfileUtil.setAtomic(action, true);
                    PokaYokeUmlProfileUtil.setGuard(action, "true");
                    String effect = PokaYokeUmlProfileUtil.getEffects(actionElement).get(actionAndEffectIndex.right);
                    PokaYokeUmlProfileUtil.setEffects(action, List.of(effect));

                    // Add a warning that the non-atomic end action has not been fully merged.
                    warnings.add(String.format(
                            "Non-atomic action '%s' was not fully reduced, leading to an explicit end event '%s'.",
                            actionElement.getName(), action.getName()));
                }
            } else if (!(node instanceof DecisionNode || node instanceof MergeNode || node instanceof JoinNode
                    || node instanceof ForkNode || node instanceof InitialNode || node instanceof ActivityFinalNode))
            {
                throw new RuntimeException("Found unexpected node type: " + node.getClass().getSimpleName());
            }
        }
    }

    private static boolean isInternalAction(Action action) {
        return action.getName().contains(CifSourceSinkLocationTransformer.START_EVENT_NAME)
                || action.getName().contains(CifSourceSinkLocationTransformer.END_EVENT_NAME);
    }
}
