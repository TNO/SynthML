////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.pokayoke.transform.common.ExprHelper;
import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

public class ConcreteActivityRestorer {
    /** The synthesized UML activity where to perform restoration for elements originating from concrete activities. */
    private final Activity activity;

    /**
     * The tracker that indicates how results from intermediate steps of the activity synthesis chain relate to the
     * input UML.
     */
    protected final SynthesisChainTracking tracker;

    public ConcreteActivityRestorer(Activity activity, SynthesisChainTracking tracker) {
        this.activity = activity;
        this.tracker = tracker;
    }

    /**
     * Restores the control flow guards from called concrete activities, and the decision (or merge) node patterns
     * deriving from decision (or merge) nodes located in called concrete activities.
     */
    public void restore() {
        restoreConcreteControlFlowGuards();
        restoreConcreteDecisionMergeNodes();
    }

    /** Restores the control flow guards from called concrete activities. */
    private void restoreConcreteControlFlowGuards() {
        for (ActivityNode node: activity.getNodes()) {
            // Track the original UML element related to the current node. If the original UML element is 'null', or if
            // the original UML is an opaque behavior, the node is newly created. Do not add guards to these nodes.
            RedefinableElement originalSourceElement = tracker.getOriginalUmlElement(node);
            if (originalSourceElement == null || originalSourceElement instanceof OpaqueBehavior) {
                continue;
            }
            String entryGuard = tracker.getEntryGuard(node);
            String exitGuard = tracker.getExitGuard(node);

            // Sanity check: the outgoing guard of the node's incoming edges and the incoming guard of the node's
            // outgoing edges must have no guards.
            Verify.verify(
                    node.getIncomings().stream().allMatch(
                            e -> ExprHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getOutgoingGuard(e))),
                    String.format("Node '%s' has non-'null' outgoing guards on its incoming edges.", node.getName()));
            Verify.verify(
                    node.getOutgoings().stream().allMatch(
                            e -> ExprHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(e))),
                    String.format("Node '%s' has non-'null' incoming guards on its outgoing edges.", node.getName()));

            // Add the entry and exit guards to its incoming and outgoing edges, respectively.
            node.getIncomings().stream().forEach(e -> PokaYokeUmlProfileUtil.setOutgoingGuard(e, entryGuard));
            node.getOutgoings().stream().forEach(e -> PokaYokeUmlProfileUtil.setIncomingGuard(e, exitGuard));
        }
    }

    private void restoreConcreteDecisionMergeNodes() {
        // Initialize the lists of updated nodes and the nodes and edges to delete.
        List<ActivityNode> updatedNodes = new ArrayList<>();
        List<RedefinableElement> elementsToDelete = new ArrayList<>();

        // Restore the decision node patterns.
        for (Entry<DecisionNode, Set<ActivityNode>> pattern: tracker.getDecisionChildNodes().entrySet()) {
            Pair<Set<ActivityNode>, Set<RedefinableElement>> updatedAndToDelete = restoreConcreteDecisionNodePatterns(
                    pattern.getKey(), pattern.getValue());
            updatedNodes.addAll(updatedAndToDelete.left);
            elementsToDelete.addAll(updatedAndToDelete.right);
        }

        // Restore the merge node patterns.
        for (Entry<MergeNode, Set<ActivityNode>> pattern: tracker.getMergeParentNodes().entrySet()) {
            Pair<Set<ActivityNode>, Set<RedefinableElement>> updatedAndToDelete = restoreConcreteMergeNodePatterns(
                    pattern.getKey(), pattern.getValue());
            updatedNodes.addAll(updatedAndToDelete.left);
            elementsToDelete.addAll(updatedAndToDelete.right);
        }

        // Sanity check: exactly one edge per node to remove.
        Verify.verify(
                elementsToDelete.stream().filter(e -> e instanceof ActivityNode).toList().size() == elementsToDelete
                        .stream().filter(e -> e instanceof ActivityEdge).toList().size(),
                "Expected the same number of nodes and edges to remove after restoring decision/merge patterns.");

        // Update the tracker and destroy the other decision and merge nodes and their edges.
        tracker.updateConcreteDecisionMergeNodesAndEdges(updatedNodes, elementsToDelete.stream()
                .filter(e -> e instanceof ActivityNode).map(ActivityNode.class::cast).toList());
        elementsToDelete.forEach(e -> e.destroy());
    }

    /**
     * Analyze the children of a newly created decision node, and look for the ones that are the translation of a single
     * decision node located in a called concrete activity. These child decision nodes can be united into a single
     * decision node when:
     * <ol>
     * <li>All decision nodes can be traced back to the same decision node in a called concrete activity;</li>
     * <li>Each node has exactly one incoming edge;</li>
     * <li>That edge comes from the new decision node created as a translation of a Petri net place with multiple
     * outgoing arcs.</li>
     * </ol>
     * In practice, keep the first decision node, redirect the outgoing edge of the other decision nodes to start from
     * it.
     *
     * @param decisionNode The decision node created as the translation of a Petri net place.
     * @param childNodes The child nodes of the decision node.
     * @return A pair containing the updated decision nodes and a set containing the other decision nodes and their
     *     incoming edges to remove.
     */
    private Pair<Set<ActivityNode>, Set<RedefinableElement>>
            restoreConcreteDecisionNodePatterns(DecisionNode decisionNode, Set<ActivityNode> childNodes)
    {
        // Find the child nodes that refer to the same original decision node and group them by their original UML
        // element.
        Map<DecisionNode, List<DecisionNode>> originalDecisionNodeToPatternNodes = new LinkedHashMap<>();
        for (ActivityNode child: childNodes) {
            if (tracker.getOriginalUmlElement(child) instanceof DecisionNode originalDecisionNode) {
                originalDecisionNodeToPatternNodes.computeIfAbsent(originalDecisionNode, k -> new ArrayList<>())
                        .add((DecisionNode)child);
            }
        }

        Set<ActivityNode> updatedDecisionNodes = new LinkedHashSet<>();
        Set<RedefinableElement> incomingEdgesAndNodesToDelete = new LinkedHashSet<>();
        for (Entry<DecisionNode, List<DecisionNode>> decisionNodeToPatternNodes: originalDecisionNodeToPatternNodes
                .entrySet())
        {
            // Get the original decision node and the decision nodes derived from it.
            ActivityNode originalDecisionNode = decisionNodeToPatternNodes.getKey();
            List<DecisionNode> patternNodes = decisionNodeToPatternNodes.getValue();

            // We can unify the decision nodes if the pattern is complete and structurally unifiable. The pattern is
            // complete when all the child decision nodes of the original decision node are present. The pattern is
            // structurally unifiable if all pattern decision nodes have only one incoming edge, the edge has a trivial
            // incoming guard, and it is coming from the newly created decision node.
            boolean completePattern = originalDecisionNode.getOutgoings().size() == patternNodes.size();
            boolean unifiable = patternNodes.stream().allMatch(m -> m.getIncomings().size() == 1
                    && m.getIncomings().get(0).getSource().equals(decisionNode) && ExprHelper
                            .isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(m.getIncomings().get(0))));

            if (!completePattern || !unifiable) {
                continue;
            }

            // Sanity check: all the pattern decision nodes have the entry guard equal to the entry guard of the
            // original decision node (from which they are derived).
            String originalEntryGuard = PokaYokeUmlProfileUtil
                    .getOutgoingGuard(originalDecisionNode.getIncomings().get(0));
            boolean equalEntryGuard = patternNodes.stream().allMatch(m -> Objects
                    .equals(PokaYokeUmlProfileUtil.getOutgoingGuard(m.getIncomings().get(0)), originalEntryGuard));
            Verify.verify(equalEntryGuard,
                    String.format("The decision nodes derived from node '%s' have a different entry guard.",
                            originalDecisionNode.getName()));

            // Redirect all outgoing control flows to start from the first pattern decision node in the list. Mark the
            // other pattern decision nodes and their incoming edges to be deleted.
            List<ActivityNode> nodesToDelete = patternNodes.subList(1, patternNodes.size()).stream()
                    .map(ActivityNode.class::cast).toList();
            List<ActivityEdge> edgesToDelete = nodesToDelete.stream().map(n -> n.getIncomings().get(0)).toList();
            for (ActivityNode node: nodesToDelete) {
                for (ActivityEdge edge: new ArrayList<>(node.getOutgoings())) {
                    edge.setSource(patternNodes.get(0));
                }
            }

            updatedDecisionNodes.add(patternNodes.get(0));
            incomingEdgesAndNodesToDelete.addAll(nodesToDelete);
            incomingEdgesAndNodesToDelete.addAll(edgesToDelete);
        }

        return new Pair<>(updatedDecisionNodes, incomingEdgesAndNodesToDelete);
    }

    /**
     * Analyze the parents of a newly created merge node, and look for the ones that are the translation of a single
     * merge node located in a called concrete activity. These parent merge nodes can be united into a single merge node
     * when:
     * <ol>
     * <li>All merge nodes can be traced back to the same merge node in a called concrete activity;</li>
     * <li>Each node has exactly one outgoing edge;</li>
     * <li>That edge is directed to the new merge node created as a translation of a Petri net place with multiple
     * incoming arcs.</li>
     * </ol>
     * In practice, keep the first merge node, redirect the incoming edge of the other merge nodes to it.
     *
     * @param mergeNode The merge node created as the translation of a Petri net place.
     * @param parentNodes The parent nodes of the merge node.
     * @return A pair containing the updated merge nodes and a set containing the other merge nodes and their outgoing
     *     edges to remove.
     */
    private Pair<Set<ActivityNode>, Set<RedefinableElement>> restoreConcreteMergeNodePatterns(MergeNode mergeNode,
            Set<ActivityNode> parentNodes)
    {
        // Find the parent nodes that refer to the same original merge node and group them by their original UML
        // element.
        Map<MergeNode, List<MergeNode>> originalMergeNodeToPatternNodes = new LinkedHashMap<>();
        for (ActivityNode parent: parentNodes) {
            if (tracker.getOriginalUmlElement(parent) instanceof MergeNode originalMergeNode) {
                originalMergeNodeToPatternNodes.computeIfAbsent(originalMergeNode, k -> new ArrayList<>())
                        .add((MergeNode)parent);
            }
        }

        Set<ActivityNode> updatedMergeNodes = new LinkedHashSet<>();
        Set<RedefinableElement> outgoingEdgesAndNodesToDelete = new LinkedHashSet<>();
        for (Entry<MergeNode, List<MergeNode>> mergeNodeToPatternNodes: originalMergeNodeToPatternNodes.entrySet()) {
            // Get the original merge node and the merge nodes derived from it.
            ActivityNode originalMergeNode = mergeNodeToPatternNodes.getKey();
            List<MergeNode> patternNodes = mergeNodeToPatternNodes.getValue();

            // We can unify the merge nodes if the pattern is complete and structurally unifiable. The pattern is
            // complete when all the parent merge nodes of the original merge node are present. The pattern is
            // structurally unifiable if all pattern merge nodes have only one outgoing edge, the edge has a trivial
            // outgoing guard, and it is directed to the newly created merge node.
            boolean completePattern = originalMergeNode.getIncomings().size() == patternNodes.size();
            boolean unifiable = patternNodes.stream()
                    .allMatch(m -> m.getOutgoings().size() == 1 && m.getOutgoings().get(0).getTarget().equals(mergeNode)
                            && ExprHelper.isNullOrTriviallyTrue(
                                    PokaYokeUmlProfileUtil.getOutgoingGuard(m.getOutgoings().get(0))));

            if (!completePattern || !unifiable) {
                continue;
            }

            // Sanity check: all the pattern merge nodes have the exit guard equal to the exit guard of the original
            // merge node (from which they are derived).
            String originalExitGuard = PokaYokeUmlProfileUtil.getIncomingGuard(originalMergeNode.getOutgoings().get(0));
            boolean equalExitGuard = patternNodes.stream().allMatch(m -> java.util.Objects
                    .equals(PokaYokeUmlProfileUtil.getIncomingGuard(m.getOutgoings().get(0)), originalExitGuard));
            Verify.verify(equalExitGuard,
                    String.format("The merge nodes derived from node '%s' have a different exit guard.",
                            originalMergeNode.getName()));

            // Redirect all incoming control flows to reach the first pattern merge node in the list. Mark the other
            // pattern merge nodes and their outgoing edge to be deleted.
            List<ActivityNode> nodesToDelete = patternNodes.subList(1, patternNodes.size()).stream()
                    .map(ActivityNode.class::cast).toList();
            List<ActivityEdge> edgesToDelete = nodesToDelete.stream().map(n -> n.getOutgoings().get(0)).toList();
            for (ActivityNode node: nodesToDelete) {
                for (ActivityEdge edge: new ArrayList<>(node.getIncomings())) {
                    edge.setTarget(patternNodes.get(0));
                }
            }

            updatedMergeNodes.add(patternNodes.get(0));
            outgoingEdgesAndNodesToDelete.addAll(nodesToDelete);
            outgoingEdgesAndNodesToDelete.addAll(edgesToDelete);
        }

        return new Pair<>(updatedMergeNodes, outgoingEdgesAndNodesToDelete);
    }
}
