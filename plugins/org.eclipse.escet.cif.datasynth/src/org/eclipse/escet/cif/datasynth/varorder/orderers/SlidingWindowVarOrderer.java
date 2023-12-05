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

import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.cif.datasynth.varorder.helper.RelationsKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.RepresentationKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetric;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetricKind;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.PermuteUtils;

/** Sliding window algorithm variable ordering heuristic. */
public class SlidingWindowVarOrderer extends VarOrderer {
    /** The maximum length of the window. Is in the range [1..12]. */
    private final int maxLen;

    /** The kind of metric to use to pick the best order. */
    private final VarOrderMetricKind metricKind;

    /** The kind of relations to use to compute metric values. */
    private final RelationsKind relationsKind;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link SlidingWindowVarOrderer} class.
     *
     * @param maxLen The maximum length of the window. Must be in the range [1..12].
     * @param metricKind The kind of metric to use to pick the best order.
     * @param relationsKind The kind of relations to use to compute metric values.
     * @param effect The effect of applying the variable orderer.
     */
    public SlidingWindowVarOrderer(int maxLen, VarOrderMetricKind metricKind, RelationsKind relationsKind,
            VarOrdererEffect effect)
    {
        this.maxLen = maxLen;
        this.metricKind = metricKind;
        this.relationsKind = relationsKind;
        this.effect = effect;
        Assert.check(maxLen >= 1);
        Assert.check(maxLen <= 12);
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Get variable count.
        int varCnt = inputData.helper.size();

        // Determine window length.
        int length = Math.min(maxLen, varCnt);

        // Debug output before applying the algorithm.
        if (dbgEnabled) {
            inputData.helper.dbg(dbgLevel, "Applying sliding window algorithm:");
            inputData.helper.dbg(dbgLevel + 1, "Size: %d", maxLen);
            inputData.helper.dbg(dbgLevel + 1, "Metric: %s", enumValueToParserArg(metricKind));
            inputData.helper.dbg(dbgLevel + 1, "Relations: %s", enumValueToParserArg(relationsKind));
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
            inputData.helper.dbgRepresentation(dbgLevel + 1, RepresentationKind.HYPER_EDGES, relationsKind);
            inputData.helper.dbg(dbgLevel + 1, "Window length: %,d", length);
            inputData.helper.dbg();
        }

        // Skip algorithm if no hyper-edges.
        List<BitSet> hyperEdges = inputData.helper.getHyperEdges(relationsKind);
        if (hyperEdges.isEmpty()) {
            if (dbgEnabled) {
                inputData.helper.dbg(dbgLevel + 1, "Skipping algorithm: no hyper-edges.");
            }
            return inputData;
        }

        // Initialize current indices and metric value.
        VarOrderMetric metric = metricKind.create();
        int[] curIndices = inputData.helper.getNewIndicesForVarOrder(inputData.varOrder.getOrderedVars());
        double curMetricValue = metric.computeForNewIndices(curIndices, hyperEdges);
        if (dbgEnabled) {
            inputData.helper.dbgMetricsForNewIndices(dbgLevel + 1, curIndices, "before", relationsKind);
        }

        // Process all windows.
        int[] window = new int[length];
        int permCnt = PermuteUtils.factorial(window.length);
        int[][] windowPerms = new int[permCnt][window.length];
        for (int offset = 0; offset <= (varCnt - length); offset++) {
            // Fill window and all permutations.
            System.arraycopy(curIndices, offset, window, 0, length);
            PermuteUtils.permute(window, windowPerms);

            // Compute metric value for each order.
            int bestIdx = -1;
            int[] windowIndices = curIndices.clone();
            for (int i = 0; i < windowPerms.length; i++) {
                int[] windowPerm = windowPerms[i];
                System.arraycopy(windowPerm, 0, windowIndices, offset, length);
                double windowMetricValue = metric.computeForNewIndices(windowIndices, hyperEdges);
                if (windowMetricValue < curMetricValue) { // Check for better order (with lower metric value).
                    curMetricValue = windowMetricValue;
                    bestIdx = i;
                }
            }

            // Update order if improved by this window (has lower metric value).
            if (bestIdx >= 0) {
                System.arraycopy(windowPerms[bestIdx], 0, curIndices, offset, length);

                if (dbgEnabled) {
                    inputData.helper.dbgMetricsForNewIndices(dbgLevel + 1, curIndices,
                            fmt("window %d..%d", offset, offset + length - 1), relationsKind);
                }
            }
        }

        // Debug output after applying the algorithm.
        if (dbgEnabled) {
            inputData.helper.dbgMetricsForNewIndices(dbgLevel + 1, curIndices, "after", relationsKind);
        }

        // Return the resulting order.
        return new VarOrdererData(inputData,
                VarOrder.createFromOrderedVars(inputData.helper.reorderForNewIndices(curIndices)), effect);
    }

    @Override
    public String toString() {
        return fmt("slidwin(size=%d, metric=%s, relations=%s, effect=%s)", maxLen, enumValueToParserArg(metricKind),
                enumValueToParserArg(relationsKind), enumValueToParserArg(effect));
    }
}
