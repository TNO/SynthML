//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

import org.eclipse.escet.cif.datasynth.options.EdgeGranularityOption.EdgeGranularity;
import org.eclipse.escet.common.app.framework.options.EnumOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** Option to specify the granularity of edges. */
public class EdgeGranularityOption extends EnumOption<EdgeGranularity> {
    /** Constructor for the {@link EdgeGranularityOption} class. */
    public EdgeGranularityOption() {
        super(
                // name
                "Edge granularity",

                // description
                "Specify the granularity of edges to use during synthesis."
                        + "Specify \"per-edge\" to allow each event to have multiple edges, "
                        + "or \"per-event\" (default) to merge for each event the edges into a single edge.",

                // cmdShort
                null,

                // cmdLong
                "edge-granularity",

                // cmdValue
                "GRAN",

                // defaultValue
                EdgeGranularity.PER_EVENT,

                // showInDialog
                true,

                // optDialogDescr
                "Specify the granularity of edges to use during synthesis.");
    }

    @Override
    protected String getDialogText(EdgeGranularity granularity) {
        switch (granularity) {
            case PER_EDGE:
                return "Per edge (allow each event to have multiple edges)";
            case PER_EVENT:
                return "Per event (merge for each event the edges into a single edge)";
        }
        throw new RuntimeException("Unknown granularity: " + granularity);
    }

    /**
     * Returns the value of the {@link EdgeGranularityOption} option.
     *
     * @return The value of the {@link EdgeGranularityOption} option.
     */
    public static EdgeGranularity getGranularity() {
        return Options.get(EdgeGranularityOption.class);
    }

    /** Edge granularity. */
    public static enum EdgeGranularity {
        /** Allow each event to have multiple edges. */
        PER_EDGE,

        /** Merge for each event the edges into a single edge. */
        PER_EVENT;
    }
}
