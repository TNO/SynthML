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

import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.app.framework.options.StringOption;

/** BDD initial variable order and domain interleaving option. */
public class BddVariableOrderOption extends StringOption {
    /** The default value of the {@link BddVariableOrderOption} option. */
    public static final String DEFAULT_VALUE = "sorted";

    /** Constructor for the {@link BddVariableOrderOption} class. */
    public BddVariableOrderOption() {
        super(
                // name
                "BDD initial variable ordering",

                // description
                "The BDD initial variable ordering and domain interleaving. Specify " +

                        "\"model\" for model ordering without interleaving, " +

                        "\"reverse-model\" for reverse model ordering without interleaving, " +

                        "\"sorted\" (default) for sorted order without interleaving, " +

                        "\"reverse-sorted\" for reverse sorted order without interleaving, " +

                        "\"random\" for random order without interleaving (with random seed), " +

                        "\"random:SEED\" for random order without interleaving (with \"SEED\" as seed, "
                        + "in range [0..2^64-1]), " +

                        "or specify a custom ordering. Custom orders consist of names of variables and automata. "
                        + "The \"*\" character can be used as wildcard in names, and indicates zero or more "
                        + "characters. Separate names with \",\" for interleaving or with \";\" for non-interleaving.",

                // cmdShort
                'r',

                // cmdLong
                "var-order",

                // cmdValue
                "ORDER",

                // defaultValue
                DEFAULT_VALUE,

                // emptyAsNull
                false,

                // showInDialog
                true,

                // optDialogDescr
                "The BDD initial variable ordering and domain interleaving. Specify " +

                        "\"model\" for model ordering without interleaving, " +

                        "\"reverse-model\" for reverse model ordering without interleaving, " +

                        "\"sorted\" for sorted order without interleaving, " +

                        "\"reverse-sorted\" for reverse sorted order without interleaving, " +

                        "\"random\" for random order without interleaving (with random seed), " +

                        "\"random:SEED\" for random order without interleaving (with \"SEED\" as seed, "
                        + "in range [0..2^64-1]), " +

                        "or specify a custom ordering. Custom orders consist of names of variables and automata. "
                        + "The \"*\" character can be used as wildcard in names, and indicates zero or more "
                        + "characters. Separate names with \",\" for interleaving or with \";\" for non-interleaving.",

                // optDialogLabelText
                "Initial order:");
    }

    /**
     * Returns the value of the {@link BddVariableOrderOption} option.
     *
     * @return The value of the {@link BddVariableOrderOption} option.
     */
    public static String getOrder() {
        return Options.get(BddVariableOrderOption.class);
    }

    /**
     * Returns whether the {@link BddVariableOrderOption} option is configured with its default value.
     *
     * @return {@code true} if the option is configured with its default value, {@code false} otherwise.
     */
    public static boolean isDefault() {
        return getOrder().equals(DEFAULT_VALUE);
    }
}
