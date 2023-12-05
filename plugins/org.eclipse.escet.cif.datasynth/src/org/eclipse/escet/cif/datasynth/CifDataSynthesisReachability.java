//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
//////////////////////////////////////////////////////////////////////////////

package org.eclipse.escet.cif.datasynth;

import static org.eclipse.escet.cif.datasynth.bdd.BddUtils.bddToStr;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.dbg;
import static org.eclipse.escet.common.java.BitSets.copy;
import static org.eclipse.escet.common.java.Pair.pair;
import static org.eclipse.escet.common.java.Sets.list2set;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.eclipse.escet.cif.datasynth.options.EdgeOrderDuplicateEventsOption;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderDuplicateEventsOption.EdgeOrderDuplicateEventAllowance;
import org.eclipse.escet.cif.datasynth.options.EdgeWorksetAlgoOption;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.datasynth.spec.SynthesisEdge;
import org.eclipse.escet.cif.datasynth.workset.pruners.MaxCardinalityEdgePruner;
import org.eclipse.escet.cif.datasynth.workset.pruners.RewardBasedEdgePruner;
import org.eclipse.escet.cif.datasynth.workset.pruners.SequentialEdgePruner;
import org.eclipse.escet.cif.datasynth.workset.selectors.EdgeSelector;
import org.eclipse.escet.cif.datasynth.workset.selectors.FirstEdgeSelector;
import org.eclipse.escet.cif.datasynth.workset.selectors.PruningEdgeSelector;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.BitSets;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.escet.common.java.Strings;

import com.github.javabdd.BDD;

/** CIF data-based synthesis reachability computations. */
public class CifDataSynthesisReachability {
    /** The synthesis automaton. */
    private final SynthesisAutomaton aut;

    /** The 1-based round number of the main synthesis algorithm, for debug output. */
    private final int round;

    /** The name of the predicate on which to apply the reachability, for debug output. Must be in lower case. */
    private final String predName;

    /**
     * The name of the initial value of the predicate on which to apply the reachability, for debug output. Must be in
     * lower case.
     */
    private final String initName;

    /**
     * The name of the restriction predicate, for debug output. Must be in lower case. May be {@code null} if no
     * restriction predicate will be used.
     */
    private final String restrictionName;

    /**
     * The predicate that indicates the upper bound on the reached states. That is, during reachability no states may be
     * reached outside these states. May be {@code null} to not impose a restriction, which is semantically equivalent
     * to providing 'true'.
     */
    private final BDD restriction;

    /** Whether the given predicate represents bad states ({@code true}) or good states ({@code false}). */
    private final boolean bad;

    /** Whether to apply forward reachability ({@code true}) or backward reachability ({@code false}). */
    private final boolean forward;

    /** Whether to include edges with controllable events in the reachability. */
    private final boolean ctrl;

    /** Whether to include edges with uncontrollable events in the reachability. */
    private final boolean unctrl;

    /** Whether debug output is enabled. */
    private final boolean dbgEnabled;

    /**
     * Constructor for the {@link CifDataSynthesisReachability} class.
     *
     * @param aut The synthesis automaton.
     * @param round The 1-based round number of the main synthesis algorithm, for debug output.
     * @param predName The name of the given predicate, for debug output. Must be in lower case.
     * @param initName The name of the initial value of the given predicate, for debug output. Must be in lower case.
     * @param restrictionName The name of the restriction predicate, for debug output. Must be in lower case. May be
     *     {@code null} if no restriction predicate will be used.
     * @param restriction The predicate that indicates the upper bound on the reached states. That is, during
     *     reachability no states may be reached outside these states. May be {@code null} to not impose a restriction,
     *     which is semantically equivalent to providing 'true'.
     * @param bad Whether the given predicate represents bad states ({@code true}) or good states ({@code false}).
     * @param forward Whether to apply forward reachability ({@code true}) or backward reachability ({@code false}).
     * @param ctrl Whether to include edges with controllable events in the reachability.
     * @param unctrl Whether to include edges with uncontrollable events in the reachability.
     * @param dbgEnabled Whether debug output is enabled.
     */
    public CifDataSynthesisReachability(SynthesisAutomaton aut, int round, String predName, String initName,
            String restrictionName, BDD restriction, boolean bad, boolean forward, boolean ctrl, boolean unctrl,
            boolean dbgEnabled)
    {
        Assert.areEqual(restrictionName == null, restriction == null);
        this.aut = aut;
        this.round = round;
        this.predName = predName;
        this.initName = initName;
        this.restrictionName = restrictionName;
        this.restriction = restriction;
        this.bad = bad;
        this.forward = forward;
        this.ctrl = ctrl;
        this.unctrl = unctrl;
        this.dbgEnabled = dbgEnabled;
    }

    /**
     * Performs forward or backward reachability until a fixed point is reached.
     *
     * @param pred The predicate to which to apply the reachability. This predicate is {@link BDD#free freed} by this
     *     method.
     * @return The fixed point result of the reachability computation, or {@code null} if the application is terminated.
     */
    public BDD performReachability(BDD pred) {
        // Print debug output.
        if (dbgEnabled) {
            dbg();
            dbg("Round %d: computing %s predicate.", round, predName);
            dbg("%s: %s [%s predicate]", Strings.makeInitialUppercase(predName), bddToStr(pred, aut), initName);
        }

        // Initialization.
        boolean changed = false;

        // Restrict predicate.
        if (restriction != null) {
            BDD restrictedPred = pred.and(restriction);
            if (aut.env.isTerminationRequested()) {
                return null;
            }

            if (pred.equals(restrictedPred)) {
                restrictedPred.free();
            } else {
                if (dbgEnabled) {
                    Assert.notNull(restrictionName);
                    dbg("%s: %s -> %s [restricted to %s predicate: %s]", Strings.makeInitialUppercase(predName),
                            bddToStr(pred, aut), bddToStr(restrictedPred, aut), restrictionName,
                            bddToStr(restriction, aut));
                }
                pred.free();
                pred = restrictedPred;
                changed = true;
            }
        }

        // Determine the edges to be applied.
        boolean useWorkSetAlgo = EdgeWorksetAlgoOption.isEnabled();
        List<SynthesisEdge> orderedEdges = forward ? aut.orderedEdgesForward : aut.orderedEdgesBackward;
        Predicate<SynthesisEdge> edgeShouldBeApplied = e -> (ctrl && e.event.getControllable())
                || (unctrl && !e.event.getControllable());
        List<SynthesisEdge> edgesToApply = orderedEdges.stream().filter(edgeShouldBeApplied).toList();
        BitSet edgesToApplyMask = useWorkSetAlgo ? IntStream.range(0, orderedEdges.size())
                .filter(i -> edgeShouldBeApplied.test(orderedEdges.get(i))).boxed().collect(BitSets.toBitSet()) : null;

        // Prepare edges for being applied.
        Collection<SynthesisEdge> edgesToPrepare = EdgeOrderDuplicateEventsOption
                .getAllowance() == EdgeOrderDuplicateEventAllowance.ALLOWED ? list2set(edgesToApply) : edgesToApply;
        for (SynthesisEdge edge: edgesToPrepare) {
            edge.preApply(forward, restriction);
        }
        if (aut.env.isTerminationRequested()) {
            return null;
        }

        // Apply edges until we get a fixed point.
        Pair<BDD, Boolean> reachabilityResult;
        if (useWorkSetAlgo) {
            reachabilityResult = performReachabilityWorkset(pred, orderedEdges, edgesToApplyMask);
        } else {
            reachabilityResult = performReachabilityFixedOrder(pred, edgesToApply);
        }
        if (reachabilityResult == null || aut.env.isTerminationRequested()) {
            return null;
        }
        pred = reachabilityResult.left;
        changed |= reachabilityResult.right;

        // Cleanup edges for being applied.
        for (SynthesisEdge edge: edgesToPrepare) {
            edge.postApply(forward);
        }

        // Fixed point reached. Inform the user.
        if (aut.env.isTerminationRequested()) {
            return null;
        }
        if (dbgEnabled && changed) {
            dbg("%s: %s [fixed point].", Strings.makeInitialUppercase(predName), bddToStr(pred, aut));
        }
        return pred;
    }

    /**
     * Performs forward or backward reachability until a fixed point is reached, by applying the edges using the workset
     * algorithm.
     *
     * @param pred The predicate to which to apply the reachability. This predicate should not be used after this
     *     method, as it may have been {@link BDD#free freed} by this method. Instead, continue with the predicate
     *     returned by this method as part of the return value.
     * @param edges The synthesis edges.
     * @param edgesMask The edges to apply, as a mask on the edges, indicating for each edge whether it should be
     *     applied.
     * @return The fixed point result of the reachability computation, together with an indication of whether the
     *     predicate was changed as a result of the reachability computation. Instead of a pair, {@code null} is
     *     returned if the application is terminated.
     */
    private Pair<BDD, Boolean> performReachabilityWorkset(BDD pred, List<SynthesisEdge> edges, BitSet edgesMask) {
        boolean changed = false;
        List<BitSet> dependencies = forward ? aut.worksetDependenciesForward : aut.worksetDependenciesBackward;
        EdgeSelector edgeSelector = new PruningEdgeSelector(
                new SequentialEdgePruner(new MaxCardinalityEdgePruner(dependencies),
                        new RewardBasedEdgePruner(edges.size(), 1, -1)),
                new FirstEdgeSelector());
        BitSet workset = copy(edgesMask);
        while (!workset.isEmpty()) {
            // Select the next edge to apply.
            int edgeIdx = edgeSelector.select(workset);
            SynthesisEdge edge = edges.get(edgeIdx);

            // Repeatedly apply the selected edge, until it no longer has an effect.
            boolean changedByEdge = false;
            while (true) {
                // Apply selected edge. Apply the runtime error predicates when applying backward.
                BDD updPred = pred.id();
                updPred = edge.apply(updPred, bad, forward, restriction, !forward);
                if (aut.env.isTerminationRequested()) {
                    return null;
                }

                // Extend reachable states.
                BDD newPred = pred.id().orWith(updPred);
                if (aut.env.isTerminationRequested()) {
                    return null;
                }

                // Detect no change (fixed point reached for the selected edge).
                if (pred.equals(newPred)) {
                    newPred.free();
                    break;
                }

                // The predicate changed: at least one new state was reached. Inform the user.
                if (dbgEnabled) {
                    String restrTxt;
                    if (restriction == null) {
                        restrTxt = "";
                    } else {
                        Assert.notNull(restrictionName);
                        restrTxt = fmt(", restricted to %s predicate: %s", restrictionName, bddToStr(restriction, aut));
                    }
                    dbg("%s: %s -> %s [%s reach with edge: %s%s]", Strings.makeInitialUppercase(predName),
                            bddToStr(pred, aut), bddToStr(newPred, aut), (forward ? "forward" : "backward"),
                            edge.toString(0, ""), restrTxt);
                }

                // Update the administration.
                pred.free();
                pred = newPred;
                changed = true;
                changedByEdge = true;
            }

            // Update the workset.
            if (changedByEdge) {
                // Add the dependent edges of the edge that was just applied, as these may have become enabled.
                // However, only add those dependent edges that are to be applied at all.
                BitSet dependents = copy(dependencies.get(edgeIdx));
                dependents.and(edgesMask);
                workset.or(dependents);
            }
            // A fixed point was reached for the selected edge, so it can be removed from the workset. Note that by
            // removing this edge after adding its dependencies, it does not matter whether an edge is a dependency of
            // itself or not.
            workset.clear(edgeIdx);

            // Update the edge selector, based on the effect of applying the selected edge.
            edgeSelector.update(edgeIdx, changedByEdge);
        }
        return pair(pred, changed);
    }

    /**
     * Performs forward or backward reachability until a fixed point is reached, by applying the edges in a fixed order.
     *
     * @param pred The predicate to which to apply the reachability. This predicate should not be used after this
     *     method, as it may have been {@link BDD#free freed} by this method. Instead, continue with the predicate
     *     returned by this method as part of the return value.
     * @param edges The synthesis edges to apply.
     * @return The fixed point result of the reachability computation, together with an indication of whether the
     *     predicate was changed as a result of the reachability computation. Instead of a pair, {@code null} is
     *     returned if the application is terminated.
     */
    private Pair<BDD, Boolean> performReachabilityFixedOrder(BDD pred, List<SynthesisEdge> edges) {
        boolean changed = false;
        int iter = 0;
        int remainingEdges = edges.size(); // Number of edges to apply without change to get the fixed point.
        while (remainingEdges > 0) {
            // Print iteration, for debugging.
            iter++;
            if (dbgEnabled) {
                dbg("%s reachability: iteration %d.", (forward ? "Forward" : "Backward"), iter);
            }

            // Push through all edges.
            for (SynthesisEdge edge: edges) {
                // Apply edge. Apply the runtime error predicates when applying backward.
                BDD updPred = pred.id();
                updPred = edge.apply(updPred, bad, forward, restriction, !forward);
                if (aut.env.isTerminationRequested()) {
                    return null;
                }

                // Extend reachable states.
                BDD newPred = pred.id().orWith(updPred);
                if (aut.env.isTerminationRequested()) {
                    return null;
                }

                // Detect change.
                if (pred.equals(newPred)) {
                    // No change.
                    newPred.free();
                    remainingEdges--;
                    if (remainingEdges == 0) {
                        break; // Fixed point reached.
                    }
                } else {
                    // The predicate changed: at least one new state was reached. Inform the user.
                    if (dbgEnabled) {
                        String restrTxt;
                        if (restriction == null) {
                            restrTxt = "";
                        } else {
                            Assert.notNull(restrictionName);
                            restrTxt = fmt(", restricted to %s predicate: %s", restrictionName,
                                    bddToStr(restriction, aut));
                        }
                        dbg("%s: %s -> %s [%s reach with edge: %s%s]", Strings.makeInitialUppercase(predName),
                                bddToStr(pred, aut), bddToStr(newPred, aut), (forward ? "forward" : "backward"),
                                edge.toString(0, ""), restrTxt);
                    }

                    // Update the administration.
                    pred.free();
                    pred = newPred;
                    changed = true;
                    remainingEdges = edges.size(); // Change found, reset the counter.
                }
            }
        }
        return pair(pred, changed);
    }
}
