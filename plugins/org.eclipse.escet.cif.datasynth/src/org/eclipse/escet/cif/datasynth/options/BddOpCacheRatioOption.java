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

/** BDD library operation cache ratio option. */
public class BddOpCacheRatioOption extends DoubleOption {
    /** Constructor for the {@link BddOpCacheRatioOption} class. */
    public BddOpCacheRatioOption() {
        super(
                // name
                "BDD library operation ratio size",

                // description
                "The ratio of the size of the operation cache of the BDD library to the size of the node table of the "
                        + "BDD library. Value must be in the range [0.01 .. 1000]. The default is 1. "
                        + "This option has no effect if the BDD library operation cache size option is enabled.",

                // cmdShort
                null,

                // cmdLong
                "bdd-cache-ratio",

                // cmdValue
                "RATIO",

                // defaultValue
                1.0,

                // minimumValue
                0.01,

                // maximumValue
                1000.0,

                // showInDialog
                true,

                // optDialogDescr
                "The ratio of the size of the operation cache of the BDD library to the size of the node table of "
                        + "the BDD library. This option has no effect if the BDD library operation cache size option "
                        + "is enabled.",

                // optDialogLabelText
                "Cache ratio:");
    }

    /**
     * Returns the ratio of the size of the operation cache of the BDD library to the size of the node table of the BDD
     * library. Value should not be used if the BDD library operation size option is enabled.
     *
     * @return The ratio of the size of the operation cache of the BDD library to the size of the node table of the BDD
     *     library.
     */
    public static double getCacheRatio() {
        return Options.get(BddOpCacheRatioOption.class);
    }
}
