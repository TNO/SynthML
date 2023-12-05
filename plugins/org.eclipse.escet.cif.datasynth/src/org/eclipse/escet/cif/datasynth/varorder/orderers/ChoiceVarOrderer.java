//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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
import java.util.stream.Collectors;

import org.eclipse.escet.cif.datasynth.varorder.helper.RelationsKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.RepresentationKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetric;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetricKind;
import org.eclipse.escet.common.java.Assert;

/** Variable orderer that applies multiple other orderers, and picks the best order. */
public class ChoiceVarOrderer extends VarOrderer {
    /** The name of the choice-based orderer, or {@code null} if no name is given. */
    private final String name;

    /** The orderers to apply. At least two orderers. */
    private final List<VarOrderer> choices;

    /** The kind of metric to use to pick the best order. */
    private final VarOrderMetricKind metricKind;

    /** The kind of relations to use to compute metric values. */
    private final RelationsKind relationsKind;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link ChoiceVarOrderer} class. Does not name the choice-based orderer.
     *
     * @param choices The orderers to apply. Must be at least two orderers.
     * @param metricKind The kind of metric to use to pick the best order.
     * @param relationsKind The kind of relations to use to compute metric values.
     * @param effect The effect of applying the variable orderer.
     */
    public ChoiceVarOrderer(List<VarOrderer> choices, VarOrderMetricKind metricKind, RelationsKind relationsKind,
            VarOrdererEffect effect)
    {
        this(null, choices, metricKind, relationsKind, effect);
    }

    /**
     * Constructor for the {@link ChoiceVarOrderer} class.
     *
     * @param name The name of the choice-based orderer.
     * @param choices The orderers to apply. Must be at least two orderers.
     * @param metricKind The kind of metric to use to pick the best order.
     * @param relationsKind The kind of relations to use to compute metric values.
     * @param effect The effect of applying the variable orderer.
     */
    public ChoiceVarOrderer(String name, List<VarOrderer> choices, VarOrderMetricKind metricKind,
            RelationsKind relationsKind, VarOrdererEffect effect)
    {
        this.name = name;
        this.choices = choices;
        this.metricKind = metricKind;
        this.relationsKind = relationsKind;
        this.effect = effect;
        Assert.check(choices.size() >= 2);
    }

    @Override
    public VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel) {
        // Debug output before applying the orderer.
        if (dbgEnabled) {
            if (name == null) {
                inputData.helper.dbg(dbgLevel, "Applying multiple orderers, and choosing the best result:");
            } else {
                inputData.helper.dbg(dbgLevel, "Applying %s:", name);
            }
            inputData.helper.dbg(dbgLevel + 1, "Metric: %s", enumValueToParserArg(metricKind));
            inputData.helper.dbg(dbgLevel + 1, "Relations: %s", enumValueToParserArg(relationsKind));
            inputData.helper.dbg(dbgLevel + 1, "Effect: %s", enumValueToParserArg(effect));
            inputData.helper.dbgRepresentation(dbgLevel + 1, RepresentationKind.HYPER_EDGES, relationsKind);
            inputData.helper.dbg();
        }

        // Skip orderer if no hyper-edges.
        List<BitSet> hyperEdges = inputData.helper.getHyperEdges(relationsKind);
        if (hyperEdges.isEmpty()) {
            if (dbgEnabled) {
                inputData.helper.dbg(dbgLevel + 1, "Skipping orderer%s: no hyper-edges.", (name == null) ? "s" : "");
            }
            return inputData;
        }

        // Initialize best result (the lower the metric value the better).
        VarOrdererData bestData = null;
        double bestMetric = Double.POSITIVE_INFINITY;

        // Apply each orderers.
        VarOrderMetric metric = metricKind.create();
        for (int i = 0; i < choices.size(); i++) {
            // Separate debug output of this orderer from that of the previous one.
            if (i > 0 && dbgEnabled) {
                inputData.helper.dbg();
            }

            // Apply orderers. Each orderer is independently applied to the input variable order.
            VarOrderer choice = choices.get(i);
            VarOrdererData choiceData = choice.order(inputData, dbgEnabled, dbgLevel + 1);

            // Update best result (with lowest metric value).
            double choiceMetric = metric.computeForVarOrder(inputData.helper, choiceData.varOrder.getOrderedVars(),
                    hyperEdges);
            if (choiceMetric < bestMetric) {
                bestData = choiceData;
                bestMetric = choiceMetric;

                if (dbgEnabled) {
                    inputData.helper.dbg();
                    inputData.helper.dbg(dbgLevel + 1, "Found new best variable order.");
                }
            }
        }

        // Use the best result.
        if (bestData == null) {
            throw new AssertionError();
        }
        return new VarOrdererData(inputData, bestData.varOrder, effect);
    }

    @Override
    public String toString() {
        return fmt("or(metric=%s, relations=%s, effect=%s, choices=[%s])", enumValueToParserArg(metricKind),
                enumValueToParserArg(relationsKind), enumValueToParserArg(effect),
                choices.stream().map(VarOrderer::toString).collect(Collectors.joining(", ")));
    }
}
