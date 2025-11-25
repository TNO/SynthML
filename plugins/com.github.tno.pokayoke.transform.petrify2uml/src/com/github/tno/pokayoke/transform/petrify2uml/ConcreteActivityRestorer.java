
package com.github.tno.pokayoke.transform.petrify2uml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.pokayoke.transform.common.NameHelper;
import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;

public class ConcreteActivityRestorer {
    /** The synthesized UML activity where to add control flow guards coming from any called concrete activity. */
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
     * Restores the control flow guards from called concrete activities, and the decision (merge) node patterns deriving
     * from decision (merge) nodes located in called concrete activities..
     */
    public void restore() {
        restoreConcreteControlFlowGuards();
        restoreConcreteDecisionMergeNodes();
    }

    /** Restores the control flow guards from called concrete activities. */
    private void restoreConcreteControlFlowGuards() {
        for (ActivityNode node: activity.getNodes()) {
            // Track the original UML element related to the current node. If the original UML element is 'null', or if
            // the original UML element belongs to the activity (e.g., the current node is a call behavior and the
            // original UML element is an opaque behavior), the node is newly created. Do not add guards to these nodes.
            RedefinableElement originalSourceElement = tracker.getOriginalUmlElement(node);
            if (originalSourceElement == null || tracker.belongsToSynthesizedActivity(originalSourceElement)) {
                continue;
            }
            String entryGuard = tracker.getEntryGuard(node);
            String exitGuard = tracker.getExitGuard(node);

            // Sanity check: the node's incoming and outgoing edges must have no guards.
            Verify.verify(
                    node.getOutgoings().stream().allMatch(
                            e -> NameHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getIncomingGuard(e))),
                    String.format("Node '%s' has non-'null' incoming guards on its outgoing edges.", node.getName()));
            Verify.verify(
                    node.getIncomings().stream().allMatch(
                            e -> NameHelper.isNullOrTriviallyTrue(PokaYokeUmlProfileUtil.getOutgoingGuard(e))),
                    String.format("Node '%s' has non-'null' outgoing guards on its incoming edges.", node.getName()));

            // Add the entry and exit guards to its incoming and outgoing edges, respectively.
            node.getIncomings().stream().forEach(e -> PokaYokeUmlProfileUtil.setOutgoingGuard(e, entryGuard));
            node.getOutgoings().stream().forEach(e -> PokaYokeUmlProfileUtil.setIncomingGuard(e, exitGuard));
        }
    }

    /**
     * Restore the patterns of decision (and merge) nodes when: 1) All nodes can be traced back to the same decision
     * (merge) node in a called concrete activity; 2) Each node has exactly one outgoing (incoming) edge; 3) That edge's
     * source (target) is the new decision (merge) node created as a translation of a Petri net place with multiple
     * outgoing (incoming) arcs.
     */
    private void restoreConcreteDecisionMergeNodes() {
        for (Entry<ActivityNode, Set<ActivityNode>> pattern: tracker.getDecisionOrMergePatternNodes().entrySet()) {
            ActivityNode newNode = pattern.getKey();
            if (newNode instanceof DecisionNode decisionNode) {
                restoreConcreteDecisionNodePattern(decisionNode, pattern.getValue());
            } else if (newNode instanceof MergeNode mergeNode) {
                restoreConcreteMergeNodePattern(mergeNode, pattern.getValue());
            } else {
                throw new RuntimeException(
                        String.format("Node '%s' must be either a decision or a merge node.", newNode.getName()));
            }
        }
    }

    /**
     * Analyze the children of a newly created decision node, and look for the ones who are the translation of a single
     * decision node located in a called concrete activity. These children decision nodes can be united into a single
     * decision node when: 1) All nodes can be traced back to the same decision node in a called concrete activity; 2)
     * Each node has exactly one incoming edge; 3) That edge comes from the new decision node created as a translation
     * of a Petri net place with multiple outgoing arcs. In practice, keep the first decision node, redirect the
     * outgoing edge of the other decision nodes to start from it, then delete the other decision nodes and their
     * outgoing edge.
     *
     * @param decisionNode The decision node created as the translation of a Petri net place.
     * @param childrenNodes The children nodes of the decision node.
     */
    private void restoreConcreteDecisionNodePattern(DecisionNode decisionNode, Set<ActivityNode> childrenNodes) {
        // Find the children node who refer to the same original decision node and group them by their original UML
        // element.
        Map<DecisionNode, List<DecisionNode>> originalDecisionNodeToPatternNodes = new LinkedHashMap<>();
        for (ActivityNode child: childrenNodes) {
            if (tracker.getOriginalUmlElement(child) instanceof DecisionNode originalDecisionNode) {
                originalDecisionNodeToPatternNodes.computeIfAbsent(originalDecisionNode, k -> new ArrayList<>())
                        .add((DecisionNode)child);
            }
        }

        // Sanity check: there must be only one original decision node to which all pattern nodes refer to.
        Verify.verify(originalDecisionNodeToPatternNodes.size() == 1,
                "Found more than one original decision node while restoring a concrete cativity decision node pattern.");

        // Get the original decision node and the decision nodes derived from it.
        ActivityNode originalDecisionNode = originalDecisionNodeToPatternNodes.keySet().iterator().next();
        List<DecisionNode> patternNodes = originalDecisionNodeToPatternNodes.values().iterator().next();

        // If all pattern decision nodes have only one incoming edge and the edge is coming from the newly created
        // decision node, we can unify them.
        boolean unifyable = patternNodes.stream().allMatch(
                m -> m.getIncomings().size() == 1 && m.getIncomings().get(0).getSource().equals(decisionNode));

        if (!unifyable) {
            return;
        }

        // Sanity check: all the pattern decision nodes have the entry guard equal to the entry guard of the
        // original decision node (from which they are derived).
        String originalEntryGuard = PokaYokeUmlProfileUtil.getOutgoingGuard(originalDecisionNode.getIncomings().get(0));
        boolean equalEntryGuard = patternNodes.stream().allMatch(m -> java.util.Objects.equals(
                // Correctly handle 'null' values.
                PokaYokeUmlProfileUtil.getOutgoingGuard(m.getIncomings().get(0)), originalEntryGuard));
        Verify.verify(equalEntryGuard,
                String.format("The derived decision nodes from node '%s' have a different entry guard.",
                        originalDecisionNode.getName()));

        // Sanity check: the number of pattern decision nodes must be equal to the number of outgoing edges of the
        // original decision node.
        Verify.verify(originalDecisionNode.getOutgoings().size() == patternNodes.size(), String.format(
                "The concrete decision node '%s' has %s outgoing edges, but found %s corresponding pattern decision nodes.",
                originalDecisionNode.getName(), String.valueOf(originalDecisionNode.getOutgoings().size()),
                String.valueOf(patternNodes.size())));

        // Redirect all outgoing control flows to start from the first pattern decision node in the list. Mark the
        // other pattern decision nodes and their incoming edges to be deleted.
        List<ActivityNode> nodesToDelete = patternNodes.subList(1, patternNodes.size()).stream()
                .map(ActivityNode.class::cast).toList();
        List<ActivityEdge> edgesToDelete = nodesToDelete.stream().map(n -> n.getIncomings().get(0)).toList();
        for (ActivityNode node: new ArrayList<>(nodesToDelete)) { // Avoid concurrent duplication error.
            for (ActivityEdge edge: new ArrayList<>(node.getOutgoings())) {
                edge.setSource(patternNodes.get(0));
            }
        }

        // Update the tracker and destroy the other decision nodes and edges.
        tracker.removeNodes(nodesToDelete);

        for (ActivityEdge edge: new ArrayList<>(edgesToDelete)) {
            edge.destroy();
        }

        for (ActivityNode node: new ArrayList<>(nodesToDelete)) {
            node.destroy();
        }
    }

    /**
     * Analyze the parents of a newly created merge node, and look for the ones who are the translation of a single
     * merge node located in a called concrete activity. These children merge nodes can be united into a single merge
     * node when: 1) All nodes can be traced back to the same merge node in a called concrete activity; 2) Each node has
     * exactly one outgoing edge; 3) That edge is directed to the new merge node created as a translation of a Petri net
     * place with multiple incoming arcs. In practice, keep the first merge node, redirect the incoming edge of the
     * other merge nodes to it, then delete the other merge nodes and their incoming edge.
     *
     * @param mergeNode The merge node created as the translation of a Petri net place.
     * @param parentNodes The parent nodes of the merge node.
     */
    private void restoreConcreteMergeNodePattern(MergeNode mergeNode, Set<ActivityNode> parentNodes) {
        // Find the parent nodes who refer to the same original merge node and group them by their original UML element.
        Map<MergeNode, List<MergeNode>> originalMergeNodeToPatternNodes = new LinkedHashMap<>();
        for (ActivityNode parent: parentNodes) {
            if (tracker.getOriginalUmlElement(parent) instanceof MergeNode originalMergeNode) {
                originalMergeNodeToPatternNodes.computeIfAbsent(originalMergeNode, k -> new ArrayList<>())
                        .add((MergeNode)parent);
            }
        }

        // Sanity check: there must be only one original merge node to which all pattern nodes refer to.
        Verify.verify(originalMergeNodeToPatternNodes.size() == 1,
                "Found more than one original decision node while restoring a concrete cativity decision node pattern.");

        // Get the original merge node and the merge nodes derived from it.
        ActivityNode originalMergeNode = originalMergeNodeToPatternNodes.keySet().iterator().next();
        List<MergeNode> patternNodes = originalMergeNodeToPatternNodes.values().iterator().next();

        // If all pattern merge nodes have only one outgoing edge and the edge is directed to the newly created merge
        // node, we can unify them.
        boolean unifyable = patternNodes.stream()
                .allMatch(m -> m.getOutgoings().size() == 1 && m.getOutgoings().get(0).getTarget().equals(mergeNode));

        if (!unifyable) {
            return;
        }

        // Sanity check: all the pattern merge nodes have the exit guard equal to the exit guard of the original
        // merge node (from which they are derived).
        String originalExitGuard = PokaYokeUmlProfileUtil.getIncomingGuard(originalMergeNode.getOutgoings().get(0));
        boolean equalExitGuard = patternNodes.stream().allMatch(m -> java.util.Objects.equals(
                // Correctly handle 'null' values.
                PokaYokeUmlProfileUtil.getIncomingGuard(m.getOutgoings().get(0)), originalExitGuard));
        Verify.verify(equalExitGuard, String.format(
                "The derived merge nodes from node '%s' have a different exit guard.", originalMergeNode.getName()));

        // Sanity check: the number of pattern merge nodes must be equal to the number of incoming edges of the
        // original merge node.
        Verify.verify(originalMergeNode.getIncomings().size() == patternNodes.size(), String.format(
                "The original merge node '%s' has %s incoming edges, but found %s corresponding pattern merge nodes.",
                originalMergeNode.getName(), String.valueOf(originalMergeNode.getOutgoings().size()),
                String.valueOf(patternNodes.size())));

        // Redirect all incoming control flows to reach the first pattern merge node in the list. Mark the other
        // pattern merge nodes and their outgoing edge to be deleted.
        List<ActivityNode> nodesToDelete = patternNodes.subList(1, patternNodes.size()).stream()
                .map(ActivityNode.class::cast).toList();
        List<ActivityEdge> edgesToDelete = nodesToDelete.stream().map(n -> n.getOutgoings().get(0)).toList();
        for (ActivityNode node: new ArrayList<>(nodesToDelete)) { // Avoid concurrent duplication error.
            for (ActivityEdge edge: new ArrayList<>(node.getIncomings())) {
                edge.setTarget(patternNodes.get(0));
            }
        }

        // Update the tracker and destroy the other merge nodes and edges.
        tracker.removeNodes(nodesToDelete);

        for (ActivityEdge edge: new ArrayList<>(edgesToDelete)) {
            edge.destroy();
        }

        for (ActivityNode node: new ArrayList<>(nodesToDelete)) {
            node.destroy();
        }
    }
}
