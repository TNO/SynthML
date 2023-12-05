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

import org.eclipse.escet.common.app.framework.options.BooleanOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** Edge workset algorithm option. */
public class EdgeWorksetAlgoOption extends BooleanOption {
    /** Constructor for the {@link EdgeWorksetAlgoOption} class. */
    public EdgeWorksetAlgoOption() {
        super(
                // name
                "Edge workset algorithm",

                // description
                "Whether to use the edge workset algorithm to dynamically choose the best edge to apply during "
                        + "reachability computations (BOOL=yes), or not (BOOL=no). [DEFAULT=no]",

                // cmdShort
                null,

                // cmdLong
                "edge-workset",

                // cmdValue
                "BOOL",

                // defaultValue
                false,

                // showInDialog
                true,

                // optDialogDescr
                "Whether to use the edge workset algorithm to dynamically choose the best edge to apply during "
                        + "reachability computations.",

                // optDialogLabelText
                "Use workset algorithm");
    }

    /**
     * Should the edge workset algorithm be used?
     *
     * @return {@code true} to use the algorithm, {@code false} otherwise.
     */
    public static boolean isEnabled() {
        return Options.get(EdgeWorksetAlgoOption.class);
    }
}
