
package com.github.tno.pokayoke.transform.petrify2uml.patterns;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.UMLFactory;

import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;

/**
 * Functionality for finding and rewriting redundant decision-fork-merge patterns in UML activities.
 * <p>
 * A <i>redundant decision-fork-merge pattern</i> is a pattern where, after doing some decision node, the activity
 * branches into <i>N</i> different paths, which all fork and all merge again into the same <i>N</i> merge nodes, and
 * all control flows in between have trivial guards. Further, The decision and merge node must not correspond to the
 * translation of a concrete activity initial and final nodes, respectively. This pattern is redundant, since the
 * decision node and all <i>N</i> fork and merge nodes can be replaced by a single fork node.
 * </p>
 */
public class RedundantDecisionForkMergePattern {
    private static final UMLFactory UML_FACTORY = UMLFactory.eINSTANCE;

    private final DecisionNode decisionNode;

    private final Set<ForkNode> forkNodes;

    private final Set<MergeNode> mergeNodes;

    private RedundantDecisionForkMergePattern(DecisionNode decisionNode, Set<ForkNode> forkNodes,
            Set<MergeNode> mergeNodes)
    {
        this.decisionNode = decisionNode;
        this.forkNodes = forkNodes;
        this.mergeNodes = mergeNodes;
    }

    /**
     * Finds and rewrites all redundant decision-fork-merge patterns in the given activity.
     *
     * @param activity The input activity, which is modified in-place.
     * @param tracker The synthesis chain tracker.
     * @return {@code true} if the input activity has been rewritten, {@code false} otherwise.
     */
    public static boolean findAndRewriteAll(Activity activity, SynthesisChainTracking tracker) {
        List<RedundantDecisionForkMergePattern> patterns = findAll(activity, tracker);
        patterns.forEach(RedundantDecisionForkMergePattern::rewrite);
        return patterns.size() > 0;
    }

    /**
     * Finds all redundant decision-fork-merge patterns in the given activity.
     *
     * @param activity The input activity.
     * @param tracker The synthesis chain tracker.
     * @return All redundant decision-fork-merge patterns in the given activity.
     */
    public static List<RedundantDecisionForkMergePattern> findAll(Activity activity, SynthesisChainTracking tracker) {
        return activity.getNodes().stream().flatMap(node -> findAny(node, tracker).stream()).toList();
    }

    /**
     * Tries finding a redundant decision-fork-merge pattern that starts from the given activity node.
     *
     * @param node The input activity node.
     * @param tracker The synthesis chain tracker.
     * @return Some redundant decision-fork-merge pattern in case one was found, or an empty result otherwise.
     */
    private static Optional<RedundantDecisionForkMergePattern> findAny(ActivityNode node,
            SynthesisChainTracking tracker)
    {
        // To start a redundant decision-fork-merge pattern, the node must be a decision node that does not correspond
        // to an initial node of a called concrete activity.
        if (node instanceof DecisionNode decisionNode
                && !(tracker.getOriginalUmlElement(decisionNode) instanceof InitialNode))
        {
            int nrOfOutgoings = decisionNode.getOutgoings().size();

            // Find the set of all target nodes of the decision node, and check whether these are all fork nodes. The
            // control flow should also have no guard.
            Set<ForkNode> forkNodes = decisionNode.getOutgoings().stream()
                    .filter(e -> !PokaYokeUmlProfileUtil.isGuardedControlFlow((ControlFlow)e))
                    .map(ActivityEdge::getTarget).filter(ForkNode.class::isInstance).map(ForkNode.class::cast)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (forkNodes.size() == nrOfOutgoings) {
                // Iterate over every fork node and:
                // (1) find the set of all target merge nodes connected by a control flow without guards, and that do
                // not correspond to the translation of an activity final node of a called concrete activity,
                // (2) check whether the number of target nodes is the same as the number of fork nodes,
                // (3) check whether all target nodes are merge nodes,
                // (4) check whether all fork nodes that we previously iterated over target the same merge nodes.
                Set<MergeNode> mergeNodes = new LinkedHashSet<>();

                for (ForkNode forkNode: forkNodes) {
                    Set<MergeNode> localMergeNodes = forkNode.getOutgoings().stream()
                            .filter(e -> !PokaYokeUmlProfileUtil.isGuardedControlFlow((ControlFlow)e))
                            .map(ActivityEdge::getTarget).filter(MergeNode.class::isInstance).map(MergeNode.class::cast)
                            .filter(m -> !(tracker.getOriginalUmlElement(m) instanceof ActivityFinalNode))
                            .collect(Collectors.toCollection(LinkedHashSet::new));

                    if (forkNode.getOutgoings().size() != localMergeNodes.size()
                            || localMergeNodes.size() != nrOfOutgoings)
                    {
                        return Optional.empty();
                    }

                    if (mergeNodes.isEmpty()) {
                        mergeNodes.addAll(localMergeNodes);
                    } else if (!mergeNodes.equals(localMergeNodes)) {
                        return Optional.empty();
                    }
                }

                return Optional.of(new RedundantDecisionForkMergePattern(decisionNode, forkNodes, mergeNodes));
            }
        }

        return Optional.empty();
    }

    /** Rewrites this redundant decision-fork-merge pattern. */
    public void rewrite() {
        Preconditions.checkArgument(decisionNode.getIncomings().size() == 1,
                String.format("Expected decision nodes to have only one incoming control flow, but found '%d'.",
                        decisionNode.getIncomings().size()));

        // Create a new fork node that replaces the existing decision node, fork nodes, and merge nodes.
        ForkNode newForkNode = UML_FACTORY.createForkNode();
        newForkNode.setActivity(decisionNode.getActivity());

        if (decisionNode.getName() != null) {
            newForkNode.setName(decisionNode.getName().replace("Decision__", "Fork__"));
        }

        // Update the target of the single transition into the decision node, to be the new fork node instead.
        decisionNode.getIncomings().get(0).setTarget(newForkNode);

        // Update the source of the outgoing control flows of all merge nodes, to be the new fork node instead.
        for (MergeNode mergeNode: mergeNodes) {
            Preconditions.checkArgument(mergeNode.getOutgoings().size() == 1,
                    String.format("Expected merge nodes to have only one outgoing control flow, but found '%d'.",
                            mergeNode.getOutgoings().size()));

            mergeNode.getOutgoings().get(0).setSource(newForkNode);
        }

        // Delete the decision node and all control flows attached to it.
        List.copyOf(decisionNode.getOutgoings()).forEach(ActivityEdge::destroy);
        decisionNode.destroy();

        // Delete all fork nodes and all control flows that are attached to it.
        for (ForkNode forkNode: forkNodes) {
            List.copyOf(forkNode.getOutgoings()).forEach(ActivityEdge::destroy);
            forkNode.destroy();
        }

        // Delete all merge nodes and all control flows that are attached to it.
        for (MergeNode mergeNode: mergeNodes) {
            List.copyOf(mergeNode.getOutgoings()).forEach(ActivityEdge::destroy);
            mergeNode.destroy();
        }
    }
}
