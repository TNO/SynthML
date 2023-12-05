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

package org.eclipse.escet.cif.datasynth.varorder.orderers;

import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.helper.RelationsKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.RepresentationKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetric;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetricKind;
import org.eclipse.escet.common.java.BitSets;

/**
 * FORCE variable ordering heuristic.
 *
 * <p>
 * Based on the paper: Fadi A. Aloul, Igor L. Markov, Karem A. Sakallah, "FORCE: A Fast and Easy-To-Implement
 * Variable-Ordering Heuristic", GLSVLSI '03 Proceedings of the 13th ACM Great Lakes symposium on VLSI, pages 116-119,
 * 2003, doi:<a href="https://doi.org/10.1145/764808.764839">10.1145/764808.764839</a>.
 * </p>
 */
public class ForceVarOrderer extends VarOrderer {
    /** The kind of metric to use to pick the best order. */
    private final VarOrderMetricKind metricKind;

    /** The kind of relations to use to compute metric values. */
    private final RelationsKind relationsKind;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link ForceVarOrderer} class.
     *
     * @param metricKind The kind of metric to use to pick the best order.
     * @param relationsKind The kind of relations to use to compute metric values.
     * @param effect The effect of applying the variable orderer.
     */
    public ForceVarOrderer(VarOrderMetricKind metricKind, RelationsKind relationsKind, VarOrdererEffect effect) {
        this.metricKind = metricKind;
        this.relationsKind = relationsKind;
        this.effect = effect;
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Get variable count and hyper-edges.
        int varCnt = inputData.helper.size();
        List<BitSet> hyperEdges = inputData.helper.getHyperEdges(relationsKind);

        // Determine maximum number of iterations.
        int maxIter = (int)Math.ceil(Math.log(varCnt));
        maxIter *= 10;

        // Debug output before applying the algorithm.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying FORCE algorithm:");
            inputData.helper.dbg(dbgLevel + 1, "Metric: %s", enumValueToParserArg(metricKind));
            inputData.helper.dbg(dbgLevel + 1, "Relations: %s", enumValueToParserArg(relationsKind));
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
            inputData.helper.dbgRepresentation(dbgLevel + 1, RepresentationKind.HYPER_EDGES, relationsKind);
            inputData.helper.dbg(dbgLevel + 1, "Maximum number of iterations: %,d", maxIter);
            inputData.helper.dbg();
        }

        // Skip algorithm if no hyper-edges.
        if (hyperEdges.isEmpty()) {
            if (dbgEnabled) {
                inputData.helper.dbg(dbgLevel + 1, "Skipping algorithm: no hyper-edges.");
            }
            return inputData;
        }

        // Create 'locations' storage: per variable/vertex (in their original order), its location, i.e. l[v] in the
        // paper.
        double[] locations = new double[varCnt];

        // Create 'idxLocPairs' storage: pairs of variable indices (from their original order) and locations.
        List<IdxLocPair> idxLocPairs = listc(varCnt);
        for (int i = 0; i < varCnt; i++) {
            idxLocPairs.add(new IdxLocPair());
        }

        // Crate 'cogs' storage: the center of gravity for each hyper-edge.
        double[] cogs = new double[hyperEdges.size()];

        // Initialize 'edgeCounts': per variable/vertex, the number of hyper-edges of which it is a part.
        int[] edgeCounts = new int[varCnt];
        for (BitSet edge: hyperEdges) {
            for (int i: BitSets.iterateTrueBits(edge)) {
                edgeCounts[i]++;
            }
        }

        // Initialize variable indices: for each variable (in their original order), its new 0-based index.
        int[] curIndices; // Current indices computed by the algorithm.
        int[] bestIndices; // Best indices computed by the algorithm.
        curIndices = inputData.helper.getNewIndicesForVarOrder(inputData.varOrder.getOrderedVars());
        bestIndices = curIndices.clone();

        // Initialize metric values.
        VarOrderMetric metric = metricKind.create();
        double curMetricValue = metric.computeForNewIndices(curIndices, hyperEdges);
        double bestMetricValue = curMetricValue;
        if (dbgEnabled) {
            inputData.helper.dbgMetricsForNewIndices(dbgLevel + 1, curIndices, "before", relationsKind);
        }

        // Perform iterations of the algorithm.
        for (int curIter = 0; curIter < maxIter; curIter++) {
            // Compute center of gravity for each edge.
            for (int i = 0; i < hyperEdges.size(); i++) {
                BitSet edge = hyperEdges.get(i);
                double cog = 0;
                for (int j: BitSets.iterateTrueBits(edge)) {
                    cog += curIndices[j];
                }
                cogs[i] = cog / edge.cardinality();
            }

            // Compute (new) locations.
            Arrays.fill(locations, 0.0);
            for (int i = 0; i < hyperEdges.size(); i++) {
                BitSet edge = hyperEdges.get(i);
                for (int j: BitSets.iterateTrueBits(edge)) {
                    locations[j] += cogs[i];
                }
            }
            for (int i = 0; i < varCnt; i++) {
                locations[i] /= edgeCounts[i];
            }

            // Determine a new order, and update the current indices to reflect that order.
            for (int i = 0; i < varCnt; i++) {
                IdxLocPair pair = idxLocPairs.get(i);
                pair.idx = i;
                pair.location = locations[i];
            }
            Collections.sort(idxLocPairs);
            for (int i = 0; i < varCnt; i++) {
                curIndices[idxLocPairs.get(i).idx] = i;
            }

            // Get new metric value.
            double newMetricValue = metric.computeForNewIndices(curIndices, hyperEdges);
            if (dbgEnabled) {
                inputData.helper.dbgMetricsForNewIndices(dbgLevel + 1, curIndices, fmt("iteration %,d", curIter + 1),
                        relationsKind);
            }

            // Stop when metric value stops changing. We could stop as soon as it stops decreasing. However, we may end
            // up in a local optimum. By continuing, and allowing increases, we can get out of the local optimum, and
            // try to find a better local or global optimum. We could potentially get stuck in an oscillation. However,
            // we have a maximum on the number of iterations, so it always terminates. We may spend more iterations
            // than needed, but the FORCE algorithm is fast, so it is not expected to be an issue.
            if (newMetricValue == curMetricValue) {
                break;
            }

            // Update best order, if new order is better than the current best order (has lower metric value).
            if (newMetricValue < bestMetricValue) {
                System.arraycopy(curIndices, 0, bestIndices, 0, varCnt);
                bestMetricValue = newMetricValue;
            }

            // Prepare for next iteration.
            curMetricValue = newMetricValue;
        }

        // Debug output after applying the algorithm.
        if (dbgEnabled) {
            inputData.helper.dbgMetricsForNewIndices(dbgLevel + 1, bestIndices, "after", relationsKind);
        }

        // Return the best order.
        return new VarOrdererData(inputData,
                VarOrder.createFromOrderedVars(inputData.helper.reorderForNewIndices(bestIndices)), effect);
    }

    /**
     * A pair of a variable index (from the original variable order) and a location. Can be compared, first in ascending
     * order based on location, and secondly in ascending order based on variable index.
     */
    private static class IdxLocPair implements Comparable<IdxLocPair> {
        /** The variable index. */
        public int idx;

        /** The location. */
        public double location;

        @Override
        public int compareTo(IdxLocPair other) {
            // First compare location.
            int rslt = Double.compare(this.location, other.location);
            if (rslt != 0) {
                return rslt;
            }

            // Locations the same, so compare variable indices.
            return Integer.compare(this.idx, other.idx);
        }

        @Override
        public String toString() {
            return fmt("(%s, %s)", idx, location);
        }
    }

    @Override
    public String toString() {
        return fmt("force(metric=%s, relations=%s, effect=%s)", enumValueToParserArg(metricKind),
                enumValueToParserArg(relationsKind), enumValueToParserArg(effect));
    }
}
