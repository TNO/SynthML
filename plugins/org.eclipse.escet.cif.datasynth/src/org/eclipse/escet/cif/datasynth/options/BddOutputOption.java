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

package org.eclipse.escet.cif.datasynth.options;

import org.eclipse.escet.cif.datasynth.options.BddOutputOption.BddOutputMode;
import org.eclipse.escet.common.app.framework.options.EnumOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** BDD output mode option. */
public class BddOutputOption extends EnumOption<BddOutputMode> {
    /** Constructor for the {@link BddOutputOption} class. */
    public BddOutputOption() {
        super("BDD output mode", "Indicates how to convert BDDs to CIF for the output of synthesis. "
                + "Specify \"normal\" (default) to convert each BDD to a CIF predicate in conjunctive or disjunctive "
                + "normal form (CNF/DNF) notation, or \"nodes\" to represent the internal BDD nodes directly in CIF.",
                't', "bdd-output", "OUTMODE", BddOutputMode.NORMAL, true,
                "Indicates how to convert BDDs to CIF for the output of synthesis.");
    }

    @Override
    protected String getDialogText(BddOutputMode value) {
        switch (value) {
            case NORMAL:
                return "Convert each BDD to a CIF predicate in conjunctive or disjunctive normal form (CNF/DNF) "
                        + "notation";
            case NODES:
                return "Represent the internal BDD nodes directly in CIF";
        }
        throw new RuntimeException("Unknown mode: " + value);
    }

    /**
     * Returns the BDD output mode.
     *
     * @return The BDD output mode.
     */
    public static BddOutputMode getMode() {
        return Options.get(BddOutputOption.class);
    }

    /** BDD output mode. */
    public static enum BddOutputMode {
        /** Use conjunctive or disjunctive normal form. */
        NORMAL,

        /** Represent the internal BDD nodes directly in CI. */
        NODES;
    }
}
