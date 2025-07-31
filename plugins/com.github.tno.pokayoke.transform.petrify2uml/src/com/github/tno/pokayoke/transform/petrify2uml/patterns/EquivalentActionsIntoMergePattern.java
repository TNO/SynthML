
package com.github.tno.pokayoke.transform.petrify2uml.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.UMLFactory;

import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;

/**
 * Functionality for finding and rewriting <i>equivalent actions into merge</i> patterns in UML activities.
 * <p>
 * An <i>equivalent actions into merge</i> pattern is a pattern consisting of a merge node, which merges equivalent
 * action nodes. Such a pattern is redundant since the merge node could be 'pushed upwards' so that only a single such
 * action node would be needed. Every such pattern can be rewritten by 'pushing upwards' the merge node and eliminating
 * the redundant action nodes.
 * </p>
 */
public class EquivalentActionsIntoMergePattern {
    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    private final List<Action> actionNodes;

    private final MergeNode mergeNode;

    private EquivalentActionsIntoMergePattern(List<Action> actionNodes, MergeNode mergeNode) {
        Preconditions.checkArgument(actionNodes.size() > 1,
                String.format("Expected at least two action nodes, but found '%d'.", actionNodes.size()));

        this.actionNodes = actionNodes;
        this.mergeNode = mergeNode;
    }

    /**
     * Finds and rewrites all <i>equivalent actions into merge</i> patterns in the given activity.
     *
     * @param activity The input activity, which is modified in-place.
     * @return {@code true} if the input activity has been rewritten, {@code false} otherwise.
     */
    public static boolean findAndRewriteAll(Activity activity) {
        List<EquivalentActionsIntoMergePattern> patterns = findAll(activity);
        patterns.forEach(EquivalentActionsIntoMergePattern::rewrite);
        return patterns.size() > 0;
    }

    /**
     * Finds all <i>equivalent actions into merge</i> patterns in the given activity.
     *
     * @param activity The input activity.
     * @return All <i>equivalent actions into merge</i> patterns in the given activity.
     */
    public static List<EquivalentActionsIntoMergePattern> findAll(Activity activity) {
        return activity.getNodes().stream().flatMap(node -> findAny(node).stream()).toList();
    }

    /**
     * Tries finding an <i>equivalent actions into merge</i> pattern that involves the given guard-free activity node.
     *
     * @param node The input activity node.
     * @return Some <i>equivalent actions into merge</i> pattern in case one was found, or an empty result otherwise.
     */
    private static Optional<EquivalentActionsIntoMergePattern> findAny(ActivityNode node) {
        if (node instanceof MergeNode mergeNode && mergeNode.getIncomings().size() > 1) {
            List<Action> incomingActions = new ArrayList<>(node.getIncomings().size());

            for (ActivityEdge controlFlow: node.getIncomings()) {
                if (controlFlow.getSource() instanceof Action action
                        && !PokaYokeUmlProfileUtil.isGuardedControlFlow((ControlFlow)controlFlow)
                        && (incomingActions.isEmpty()
                                || PokaYokeUmlProfileUtil.areEquivalent(action, incomingActions.get(0))))
                {
                    incomingActions.add(action);
                } else {
                    return Optional.empty();
                }
            }

            return Optional.of(new EquivalentActionsIntoMergePattern(incomingActions, mergeNode));
        } else {
            return Optional.empty();
        }
    }

    /** Rewrites this double merge pattern. */
    public void rewrite() {
        // Create the new merge node, that 'pushes up' the old merge node in the pattern.
        MergeNode newMergeNode = UML_FACTORY.createMergeNode();
        newMergeNode.setActivity(mergeNode.getActivity());
        newMergeNode.setName(mergeNode.getName());

        // Redirect the incoming control flow of the action nodes in this pattern to target the new merge node instead.
        for (Action action: actionNodes) {
            action.getIncomings().get(0).setTarget(newMergeNode);
        }

        // Remove all outgoing control flows of the action nodes in this pattern.
        for (Action action: actionNodes) {
            List.copyOf(action.getOutgoings()).forEach(ActivityEdge::destroy);
        }

        // Remove all action nodes in this pattern except for the first one in the action node list.
        Action actionNode = actionNodes.get(0);
        actionNodes.stream().skip(1).forEach(ActivityNode::destroy);

        // Add a control flow from the new merge node to the leftover action node in the pattern.
        ControlFlow controlFlow = UML_FACTORY.createControlFlow();
        controlFlow.setActivity(actionNode.getActivity());
        controlFlow.setSource(newMergeNode);
        controlFlow.setTarget(actionNode);

        // Redirect the control flow from the old merge node in the pattern to have the leftover action node as source.
        Preconditions.checkArgument(mergeNode.getOutgoings().size() == 1,
                String.format("Expected merge nodes to have exactly one outgoing control flow, but found '%d'.",
                        mergeNode.getOutgoings().size()));

        mergeNode.getOutgoings().get(0).setSource(actionNode);

        // Remove the old merge node in the pattern.
        mergeNode.destroy();
    }
}
