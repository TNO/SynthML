////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.petrify2uml.patterns;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.UMLFactory;

import com.google.common.base.Preconditions;

/**
 * Functionality for finding and rewriting redundant decision-fork-merge patterns in UML activities.
 * <p>
 * A <i>redundant decision-fork-merge pattern</i> is a pattern where, after doing some decision node, the activity
 * branches into <i>N</i> different paths, which all fork and all merge again into the same <i>N</i> merge nodes. This
 * pattern is redundant, since the decision node and all <i>N</i> fork and merge nodes can be replaced by a single fork
 * node.
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
     * @return {@code true} if the input activity has been rewritten, {@code false} otherwise.
     */
    public static boolean findAndRewriteAll(Activity activity) {
        List<RedundantDecisionForkMergePattern> patterns = findAll(activity);
        patterns.forEach(RedundantDecisionForkMergePattern::rewrite);
        return patterns.size() > 0;
    }

    /**
     * Finds all redundant decision-fork-merge patterns in the given activity.
     *
     * @param activity The input activity.
     * @return All redundant decision-fork-merge patterns in the given activity.
     */
    public static List<RedundantDecisionForkMergePattern> findAll(Activity activity) {
        return activity.getNodes().stream().flatMap(node -> findAny(node).stream()).toList();
    }

    /**
     * Tries finding a redundant decision-fork-merge pattern that starts from the given activity node.
     *
     * @param node The input activity node.
     * @return Some redundant decision-fork-merge pattern in case one was found, or an empty result otherwise.
     */
    private static Optional<RedundantDecisionForkMergePattern> findAny(ActivityNode node) {
        // If the given node is not a decision node, then it does not start a redundant decision-fork-merge pattern.
        if (node instanceof DecisionNode decisionNode) {
            int nrOfOutgoings = decisionNode.getOutgoings().size();

            // Find the set of all target nodes of the decision node, and check whether these are all fork nodes.
            Set<ForkNode> forkNodes = decisionNode.getOutgoings().stream().map(ActivityEdge::getTarget)
                    .filter(ForkNode.class::isInstance).map(ForkNode.class::cast)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (forkNodes.size() == nrOfOutgoings) {
                // Iterate over every fork node and: (1) find the set of all target nodes, (2) check whether the number
                // of target nodes is the same as the number of fork nodes, (3) check whether all target nodes are merge
                // nodes, (4) check whether all fork nodes that we previously iterated over target the same merge nodes.
                Set<MergeNode> mergeNodes = new LinkedHashSet<>();

                for (ForkNode forkNode: forkNodes) {
                    Set<MergeNode> localMergeNodes = forkNode.getOutgoings().stream().map(ActivityEdge::getTarget)
                            .filter(MergeNode.class::isInstance).map(MergeNode.class::cast)
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
