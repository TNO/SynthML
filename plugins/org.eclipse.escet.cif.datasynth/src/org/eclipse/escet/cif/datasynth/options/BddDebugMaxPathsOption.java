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

import org.eclipse.escet.common.app.framework.options.DoubleOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** BDD predicates debug output maximum number of true paths option. */
public class BddDebugMaxPathsOption extends DoubleOption {
    /** Constructor for the {@link BddDebugMaxPathsOption} class. */
    public BddDebugMaxPathsOption() {
        super(
                // name
                "BDD debug max paths",

                // description
                "The maximum number of BDD true paths for which to convert a BDD to a readable CNF/DNF representation "
                        + "for the debug output. Value must be in the range [0 .. 1.7e308]. Specify \"inf\" to not "
                        + "set a maximum. [DEFAULT=10]",

                // cmdShort
                null,

                // cmdLong
                "bdd-dbg-maxpaths",

                // cmdValue
                "MAX",

                // defaultValue
                10.0,

                // minimumValue
                0.0,

                // maximumValue
                Double.MAX_VALUE,

                // showInDialog
                true,

                // optDialogDescr
                "The maximum number of BDD true paths for which to convert a BDD to a readable CNF/DNF representation "
                        + "for the debug output.",

                // optDialogLabelText
                "Path count:",

                // hasSpecialValue
                true,

                // defaultNormalValue
                10.0,

                // specialValueSyntax
                "inf",

                // optDialogSpecialText
                "Infinite maximum (no maximum)",

                // optDialogNormalText
                "Finite maximum");
    }

    /**
     * Returns the maximum number of BDD true paths for which to convert a BDD to a readable CNF/DNF representation for
     * the debug output, or {@code null} for no maximum.
     *
     * @return The maximum or {@code null}.
     */
    public static Double getMaximum() {
        return Options.get(BddDebugMaxPathsOption.class);
    }
}
