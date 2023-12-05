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

/** BDD FORCE variable ordering option. */
public class BddForceVarOrderOption extends BooleanOption {
    /** The default value of the {@link BddForceVarOrderOption} option. */
    public static final boolean DEFAULT_VALUE = true;

    /** Constructor for the {@link BddForceVarOrderOption} class. */
    public BddForceVarOrderOption() {
        super(
                // name
                "BDD FORCE variable ordering algorithm",

                // description
                "Whether to apply the FORCE variable ordering algorithm to improve the initial variable ordering "
                        + "(BOOL=yes), or not apply it (BOOL=no). [DEFAULT=yes]",

                // cmdShort
                null,

                // cmdLong
                "force-order",

                // cmdValue
                "BOOL",

                // defaultValue
                DEFAULT_VALUE,

                // showInDialog
                true,

                // optDialogDescr
                "Whether to apply the FORCE variable ordering algorithm to improve the initial variable ordering.",

                // optDialogLabelText
                "Apply FORCE variable ordering algorithm");
    }

    /**
     * Should the FORCE variable ordering algorithm be applied?
     *
     * @return {@code true} to apply the algorithm, {@code false} otherwise.
     */
    public static boolean isEnabled() {
        return Options.get(BddForceVarOrderOption.class);
    }

    /**
     * Returns whether the {@link BddForceVarOrderOption} option is configured with its default value.
     *
     * @return {@code true} if the option is configured with its default value, {@code false} otherwise.
     */
    public static boolean isDefault() {
        return isEnabled() == DEFAULT_VALUE;
    }
}
