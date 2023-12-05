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

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Strings.fmt;

import org.eclipse.escet.cif.datasynth.varorder.graph.algos.PseudoPeripheralNodeFinderKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.RelationsKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetricKind;

/**
 * DSM-based Cuthill-McKee/Sloan variable ordering Heuristic (DCSH).
 *
 * <p>
 * This algorithm is based on: Sam Lousberg, Sander Thuijsman and Michel Reniers, "DSM-based variable ordering heuristic
 * for reduced computational effort of symbolic supervisor synthesis", IFAC-PapersOnLine, volume 53, issue 4, pages
 * 429-436, 2020, doi:<a href="https://doi.org/10.1016/j.ifacol.2021.04.058">10.1016/j.ifacol.2021.04.058</a>.
 * </p>
 */
public class DcshVarOrderer extends ChoiceVarOrderer {
    /** The kind of pseudo-peripheral node finder to use. */
    private final PseudoPeripheralNodeFinderKind nodeFinderKind;

    /** The kind of metric to use to pick the best order. */
    private final VarOrderMetricKind metricKind;

    /** The kind of relations to use to compute metric values. */
    private final RelationsKind relationsKind;

    /** The effect of applying the variable orderer. */
    private final VarOrdererEffect effect;

    /**
     * Constructor for the {@link DcshVarOrderer} class.
     *
     * @param nodeFinderKind The kind of pseudo-peripheral node finder to use.
     * @param metricKind The kind of metric to use to pick the best order.
     * @param relationsKind The kind of relations to use to compute metric values.
     * @param effect The effect of applying the variable orderer.
     */
    public DcshVarOrderer(PseudoPeripheralNodeFinderKind nodeFinderKind, VarOrderMetricKind metricKind,
            RelationsKind relationsKind, VarOrdererEffect effect)
    {
        super("DCSH algorithm", list(
                // First algorithm.
                new WeightedCuthillMcKeeVarOrderer(nodeFinderKind, relationsKind, VarOrdererEffect.VAR_ORDER),
                // Second algorithm.
                new SloanVarOrderer(relationsKind, VarOrdererEffect.VAR_ORDER),
                // Reverse first algorithm.
                new SequentialVarOrderer(list(
                        new WeightedCuthillMcKeeVarOrderer(nodeFinderKind, relationsKind, VarOrdererEffect.VAR_ORDER),
                        new ReverseVarOrderer(relationsKind, VarOrdererEffect.VAR_ORDER))),
                // Reverse second algorithm.
                new SequentialVarOrderer(list(new SloanVarOrderer(relationsKind, VarOrdererEffect.VAR_ORDER),
                        new ReverseVarOrderer(relationsKind, VarOrdererEffect.VAR_ORDER)))),
                // Other settings.
                metricKind, relationsKind, effect);

        this.nodeFinderKind = nodeFinderKind;
        this.metricKind = metricKind;
        this.relationsKind = relationsKind;
        this.effect = effect;
    }

    @Override
    public String toString() {
        return fmt("dcsh(node-finder=%s, metric=%s, relations=%s, effect=%s)", enumValueToParserArg(nodeFinderKind),
                enumValueToParserArg(metricKind), enumValueToParserArg(relationsKind), enumValueToParserArg(effect));
    }
}
