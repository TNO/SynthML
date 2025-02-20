
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralNull;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.activitysynthesis.NonAtomicPatternRewriter;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.DoubleMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.EquivalentActionsIntoMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionForkMergePattern;
import com.github.tno.pokayoke.transform.petrify2uml.patterns.RedundantDecisionMergePattern;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.github.tno.pokayoke.uml.profile.util.UmlPrimitiveType;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.PetriNet;

public class PostProcessActivity {
    private PostProcessActivity() {
    }

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

            ActivityNode source = incomingEdge.getSource();
            ActivityNode target = outgoingEdge.getTarget();

            // Add a new control flow from source to target.
            ControlFlow newEdge = PNML2UMLTranslator.createControlFlow(activity, source, target);
            newEdge.setGuard(combineGuards(incomingEdge.getGuard(), outgoingEdge.getGuard()));

            // Destroy the action and its incoming and outgoing edges.
            incomingEdge.destroy();
            outgoingEdge.destroy();
            action.destroy();
        }
        return numberOfActions;
    }

    /**
     * Combines two given edge guards into a single edge guard.
     *
     * @param left The first edge guard to combine.
     * @param right The second edge guard to combine.
     * @return The combined edge guard.
     */
    private static ValueSpecification combineGuards(ValueSpecification left, ValueSpecification right) {
        if (left == null || left instanceof LiteralNull) {
            return right;
        }
        if (right == null || right instanceof LiteralNull) {
            return left;
        }
        if (left instanceof LiteralBoolean leftBool && right instanceof LiteralBoolean rightBool) {
            LiteralBoolean result = UMLFactory.eINSTANCE.createLiteralBoolean();
            result.setType(UmlPrimitiveType.BOOLEAN.load(left));
            result.setValue(leftBool.isValue() && rightBool.isValue());
            return result;
        }
        if (left instanceof OpaqueExpression leftExpr && right instanceof OpaqueExpression rightExpr) {
            OpaqueExpression result = UMLFactory.eINSTANCE.createOpaqueExpression();
            result.setType(UmlPrimitiveType.BOOLEAN.load(left));

            // Combine languages.
            Preconditions.checkArgument(leftExpr.getLanguages().stream().allMatch(l -> l.equals("CIF")),
                    "Expected to combine only CIF expressions.");
            Preconditions.checkArgument(rightExpr.getLanguages().stream().allMatch(l -> l.equals("CIF")),
                    "Expected to combine only CIF expressions.");
            result.getLanguages().add("CIF");

            // Combine bodies.
            List<String> bodies = new ArrayList<>();
            bodies.addAll(leftExpr.getBodies());
            bodies.addAll(rightExpr.getBodies());
            Optional<String> body = bodies.stream().reduce((l, r) -> String.format("(%s) and (%s)", l, r));
            body.ifPresent(b -> result.getBodies().add(b));

            return result;
        }

        throw new RuntimeException("Could not combine " + left + " with " + right);
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
        // Iterate over all nodes in the activity that start or end a non-atomic action, but haven't yet been rewritten.
        for (ActivityNode node: List.copyOf(activity.getNodes())) {
            if (node instanceof Action action && !rewrittenActions.contains(action)) {
                // Check whether the current action is a call opaque behavior action to start a non-atomic action.
                if (action instanceof CallBehaviorAction cbAction
                        && cbAction.getBehavior() instanceof OpaqueBehavior behavior
                        && !PokaYokeUmlProfileUtil.isAtomic(behavior))
                {
                    // If so, we replace the action by an opaque action that keeps the guard of the original action.
                    OpaqueAction replacementAction = UMLFactory.eINSTANCE.createOpaqueAction();
                    replacementAction.setActivity(activity);
                    replacementAction.setName(behavior.getName() + "_start");
                    PokaYokeUmlProfileUtil.setAtomic(replacementAction, true);
                    PokaYokeUmlProfileUtil.setGuard(replacementAction, PokaYokeUmlProfileUtil.getGuard(behavior));

                    // Redirect all incoming/outgoing control flow edges, and destroy the original action.
                    for (ActivityEdge edge: List.copyOf(action.getIncomings())) {
                        edge.setTarget(replacementAction);
                    }

                    for (ActivityEdge edge: List.copyOf(action.getOutgoings())) {
                        edge.setSource(replacementAction);
                    }

                    action.destroy();

                    // Add a warning that the current non-atomic start action has not been fully merged.
                    warnings.add(String.format(
                            "Non-atomic action '%s' was not fully reduced, leading to an explicit start event '%s'.",
                            behavior.getName(), replacementAction.getName()));
                }

                // Check whether the current action is an opaque action that ends a non-atomic action.
                if (action instanceof OpaqueAction && !rewrittenActions.contains(action)) {
                    String actionName = action.getName();

                    if (actionName.contains(nonAtomicOutcomeSuffix)) {
                        // Find the UML element for the non-atomic action, and the index to the relevant effect.
                        Pair<RedefinableElement, Integer> actionAndEffectIndex = endEventMap.get(actionName);
                        Verify.verifyNotNull(actionAndEffectIndex, String.format(
                                "Expected the CIF end event '%s' to map to a non-atomic UML element.", actionName));

                        // Determine the UML element that has the guard and effects of the current action.
                        RedefinableElement actionElement = actionAndEffectIndex.left;
                        if (actionElement instanceof CallBehaviorAction cbAction) {
                            actionElement = cbAction.getBehavior();
                        }

                        // Rename the current action, set its guard to 'true', and retain the original relevant effect.
                        action.setName(actionName.replace(nonAtomicOutcomeSuffix, "_end"));
                        PokaYokeUmlProfileUtil.setAtomic(action, true);
                        PokaYokeUmlProfileUtil.setGuard(action, "true");
                        String effect = PokaYokeUmlProfileUtil.getEffects(actionElement)
                                .get(actionAndEffectIndex.right);
                        PokaYokeUmlProfileUtil.setEffects(action, List.of(effect));

                        // Add a warning that the current non-atomic end action has not been fully merged.
                        warnings.add(String.format(
                                "Non-atomic action '%s' was not fully reduced, leading to an explicit end event '%s'.",
                                actionElement.getName(), action.getName()));
                    }
                }
            }
        }
    }
}
