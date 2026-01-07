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
import org.eclipse.uml2.uml.MergeNode;

import com.google.common.base.Preconditions;

/**
 * Functionality for finding and rewriting redundant decision-merge patterns in UML activities.
 * <p>
 * A <i>redundant decision-merge pattern</i> is a pattern where the activity branches and after that directly merges.
 * This pattern consists of a decision node and a merge node, such that all control flows from the decision node go
 * directly to the merge node, with nothing in between. Every such pattern can be rewritten by removing the decision
 * node and possibly the merge node (depending on whether it has other incoming control flows), and redirecting the
 * outgoing control flow of the decision node to the proper target node.
 * </p>
 */
public class RedundantDecisionMergePattern {
    private final DecisionNode decisionNode;

    private final MergeNode mergeNode;

    private RedundantDecisionMergePattern(DecisionNode decisionNode, MergeNode mergeNode) {
        this.decisionNode = decisionNode;
        this.mergeNode = mergeNode;
    }

    /**
     * Finds and rewrites all redundant decision-merge patterns in the given activity.
     *
     * @param activity The input activity, which is modified in-place.
     * @return {@code true} if the input activity has been rewritten, {@code false} otherwise.
     */
    public static boolean findAndRewriteAll(Activity activity) {
        List<RedundantDecisionMergePattern> patterns = findAll(activity);
        patterns.forEach(RedundantDecisionMergePattern::rewrite);
        return patterns.size() > 0;
    }

    /**
     * Finds all redundant decision-merge patterns in the given activity.
     *
     * @param activity The input activity.
     * @return All redundant decision-merge patterns in the given activity.
     */
    public static List<RedundantDecisionMergePattern> findAll(Activity activity) {
        return activity.getNodes().stream().flatMap(node -> findAny(node).stream()).toList();
    }

    /**
     * Tries finding a redundant decision-merge pattern that starts from the given activity node.
     *
     * @param node The input activity node.
     * @return Some redundant decision-merge pattern in case one was found, or an empty result otherwise.
     */
    private static Optional<RedundantDecisionMergePattern> findAny(ActivityNode node) {
        // If the given node is not a decision node, then it does not start a redundant decision-merge pattern.
        if (node instanceof DecisionNode decisionNode) {
            // Find the set of all target nodes that are reachable from the decision node by a control flow.
            Set<ActivityNode> targetNodes = decisionNode.getOutgoings().stream().map(ActivityEdge::getTarget)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // If there is exactly one target node which is a merge node, then we have found a pattern.
            if (targetNodes.size() == 1 && targetNodes.iterator().next() instanceof MergeNode mergeNode) {
                return Optional.of(new RedundantDecisionMergePattern(decisionNode, mergeNode));
            }
        }

        return Optional.empty();
    }

    /** Rewrites this redundant decision-merge pattern. */
    public void rewrite() {
        Preconditions.checkArgument(decisionNode.getIncomings().size() == 1,
                String.format("Expected decision nodes to have only one incoming control flow, but found '%d'.",
                        decisionNode.getIncomings().size()));
        Preconditions.checkArgument(mergeNode.getOutgoings().size() == 1,
                String.format("Expected merge nodes to have only one outgoing control flow, but found '%d'.",
                        mergeNode.getOutgoings().size()));

        // Update the target of the single incoming control flow into the decision node, to be the merge node in case
        // it has other incoming control flows (since the merge node will not be deleted), or otherwise to be the target
        // of the single outgoing control flow out of the merge node (since the merge node will be deleted).
        boolean keepMergeNode = mergeNode.getIncomings().stream()
                .anyMatch(controlFlow -> !controlFlow.getSource().equals(decisionNode));

        if (keepMergeNode) {
            decisionNode.getIncomings().get(0).setTarget(mergeNode);
        } else {
            decisionNode.getIncomings().get(0).setTarget(mergeNode.getOutgoings().get(0).getTarget());
        }

        // Delete the decision node and possibly the merge node, and all control flows between the deleted nodes.
        List.copyOf(decisionNode.getOutgoings()).forEach(ActivityEdge::destroy);
        decisionNode.destroy();

        if (!keepMergeNode) {
            List.copyOf(mergeNode.getOutgoings()).forEach(ActivityEdge::destroy);
            mergeNode.destroy();
        }
    }
}
