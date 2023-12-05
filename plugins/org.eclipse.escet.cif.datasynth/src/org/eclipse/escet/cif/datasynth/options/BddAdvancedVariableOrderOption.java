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

/** BDD advanced variable order and domain interleaving option. */
public class BddAdvancedVariableOrderOption extends StringOption {
    /** The default value of the {@link BddAdvancedVariableOrderOption} option. */
    public static final String DEFAULT_VALUE = "basic";

    /** Constructor for the {@link BddAdvancedVariableOrderOption} class. */
    public BddAdvancedVariableOrderOption() {
        super(
                // name
                "BDD advanced variable ordering",

                // description
                "Specify the BDD variable ordering and domain interleaving. "
                        + "See the documentation for the syntax to use.",

                // cmdShort
                null,

                // cmdLong
                "adv-var-order",

                // cmdValue
                "ORDER",

                // defaultValue
                DEFAULT_VALUE,

                // emptyAsNull
                false,

                // showInDialog
                true,

                // optDialogDescr
                "Specify the BDD variable ordering and domain interleaving. "
                        + "See the documentation for the syntax to use.",

                // optDialogLabelText
                "Order:");
    }

    /**
     * Returns the value of the {@link BddAdvancedVariableOrderOption} option.
     *
     * @return The value of the {@link BddAdvancedVariableOrderOption} option.
     */
    public static String getOrder() {
        return Options.get(BddAdvancedVariableOrderOption.class);
    }

    /**
     * Returns whether the {@link BddAdvancedVariableOrderOption} option is configured with its default value.
     *
     * @return {@code true} if the option is configured with its default value, {@code false} otherwise.
     */
    public static boolean isDefault() {
        return getOrder().equals(DEFAULT_VALUE);
    }
}
