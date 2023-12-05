//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2021, 2023 Contributors to the Eclipse Foundation
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

import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.app.framework.options.StringOption;

/** Edge order for backward reachability option. */
public class EdgeOrderBackwardOption extends StringOption {
    /** Constructor for the {@link EdgeOrderBackwardOption} class. */
    public EdgeOrderBackwardOption() {
        super(
                // name
                "Edge order for backward reachability",

                // description
                "The edge ordering to use for backward reachability computations. Specify " +

                        "\"model\" (default) for linearized model ordering, with edges for input variables sorted as "
                        + "in the variable ordering, " +

                        "\"reverse-model\" for reverse linearized model ordering, with edges for input variables "
                        + "reverse sorted as in the variable ordering, " +

                        "\"sorted\" for sorted order, " +

                        "\"reverse-sorted\" for reverse sorted order, " +

                        "\"random\" for random order (with random seed), " +

                        "\"random:SEED\" for random order (with \"SEED\" as seed, in range [0..2^64-1]), " +

                        "or specify a custom ordering. Custom orders consist of names of events and input variables. "
                        + "The \"*\" character can be used as wildcard in names, and indicates zero or more "
                        + "characters. Separate names with \",\".",

                // cmdShort
                null,

                // cmdLong
                "backward-edge-order",

                // cmdValue
                "EDGEORDER",

                // defaultValue
                "model",

                // emptyAsNull
                false,

                // showInDialog
                true,

                // optDialogDescr
                "The edge ordering to use for backward reachability computations. Specify " +

                        "\"model\" for linearized model ordering, with edges for input variables sorted as in the "
                        + "variable ordering, " +

                        "\"reverse-model\" for reverse linearized model ordering, with edges for input variables "
                        + "reverse sorted as in the variable ordering, " +

                        "\"sorted\" for sorted order, " +

                        "\"reverse-sorted\" for reverse sorted order, " +

                        "\"random\" for random order (with random seed), " +

                        "\"random:SEED\" for random order (with \"SEED\" as seed, in range [0..2^64-1]), " +

                        "or specify a custom ordering. Custom orders consist of names of events and input variables. "
                        + "The \"*\" character can be used as wildcard in names, and indicates zero or more "
                        + "characters. Separate names with \",\".",

                // optDialogLabelText
                "Edge order:");
    }

    /**
     * Returns the value of the {@link EdgeOrderBackwardOption} option.
     *
     * @return The value of the {@link EdgeOrderBackwardOption} option.
     */
    public static String getOrder() {
        return Options.get(EdgeOrderBackwardOption.class);
    }
}
