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

/** Tests for {@link SloanNodeOrderer}. */
public class SloanNodeOrdererTest extends NodeOrdererTest {
    @Override
    protected NodeOrderer createNodeOrderer() {
        return new SloanNodeOrderer();
    }

    @Override
    protected String getSimpleExpectedGraph() {
        return String.join("\n", list( //
                ".4....", //
                "4.2.8.", //
                ".2.3..", //
                "..3..5", //
                ".8...7", //
                "...57."));
    }

    @Override
    protected String getPaperSloan89ExpectedGraph() {
        return String.join("\n", list( //
                ". 1 . . . .  .  .", //
                "1 . 2 . 3 .  .  .", //
                ". 2 . 5 4 .  .  .", //
                ". . 5 . 6 9  .  .", //
                ". 3 4 6 . 8  7  .", //
                ". . . 9 8 .  .  11", //
                ". . . . 7 .  .  10", //
                ". . . . . 11 10 ."));
    }
}
