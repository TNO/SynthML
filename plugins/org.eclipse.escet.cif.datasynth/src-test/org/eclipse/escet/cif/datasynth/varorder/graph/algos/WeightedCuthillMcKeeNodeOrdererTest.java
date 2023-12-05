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

package org.eclipse.escet.cif.datasynth.varorder.graph.algos;

import static org.eclipse.escet.common.java.Lists.list;

/** Tests for {@link WeightedCuthillMcKeeNodeOrderer}. */
public class WeightedCuthillMcKeeNodeOrdererTest extends NodeOrdererTest {
    @Override
    protected NodeOrderer createNodeOrderer() {
        return new WeightedCuthillMcKeeNodeOrderer(new GeorgeLiuPseudoPeripheralNodeFinder());
    }

    @Override
    protected String getSimpleExpectedGraph() {
        return String.join("\n", list( //
                ".4....", //
                "4.82..", //
                ".8..7.", //
                ".2...3", //
                "..7..5", //
                "...35."));
    }

    @Override
    protected String getPaperSloan89ExpectedGraph() {
        return String.join("\n", list( //
                ". 1 . . .  .  . .", //
                "1 . 3 2 .  .  . .", //
                ". 3 . 4 8  7  6 .", //
                ". 2 4 . .  .  5 .", //
                ". . 8 . .  .  9 11", //
                ". . 7 . .  .  . 10", //
                ". . 6 5 9  .  . .", //
                ". . . . 11 10 . ."));
    }
}
