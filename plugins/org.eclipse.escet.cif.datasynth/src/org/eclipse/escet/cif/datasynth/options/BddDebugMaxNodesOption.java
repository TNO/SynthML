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

import org.eclipse.escet.common.app.framework.options.IntegerOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** BDD predicates debug output maximum number of nodes option. */
public class BddDebugMaxNodesOption extends IntegerOption {
    /** Constructor for the {@link BddDebugMaxNodesOption} class. */
    public BddDebugMaxNodesOption() {
        super(
                // name
                "BDD debug max nodes",

                // description
                "The maximum number of BDD nodes for which to convert a BDD to a readable CNF/DNF representation "
                        + "for the debug output. Value must be in the range [0 .. 2^31-1]. Specify \"inf\" to "
                        + "not set a maximum. [DEFAULT=10]",

                // cmdShort
                null,

                // cmdLong
                "bdd-dbg-maxnodes",

                // cmdValue
                "MAX",

                // defaultValue
                10,

                // minimumValue
                0,

                // maximumValue
                Integer.MAX_VALUE,

                // pageIncrementValue
                1,

                // showInDialog
                true,

                // optDialogDescr
                "The maximum number of BDD nodes for which to convert a BDD to a readable CNF/DNF representation "
                        + "for the debug output.",

                // optDialogLabelText
                "Node count:",

                // hasSpecialValue
                true,

                // defaultNormalValue
                10,

                // specialValueSyntax
                "inf",

                // optDialogSpecialText
                "Infinite maximum (no maximum)",

                // optDialogNormalText
                "Finite maximum");
    }

    /**
     * Returns the maximum number of BDD nodes for which to convert a BDD to a readable CNF/DNF representation for the
     * debug output, or {@code null} for no maximum.
     *
     * @return The maximum or {@code null}.
     */
    public static Integer getMaximum() {
        return Options.get(BddDebugMaxNodesOption.class);
    }
}
