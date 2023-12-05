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

/** BDD library operation cache size option. */
public class BddOpCacheSizeOption extends IntegerOption {
    /** Constructor for the {@link BddOpCacheSizeOption} class. */
    public BddOpCacheSizeOption() {
        super(
                // name
                "BDD library operation cache size",

                // description
                "The fixed size of the operation cache of the BDD library. "
                        + "Value must be in the range [2 .. 2^31-1]. Specify \"off\" to disable a fixed cache size. "
                        + "If enabled, this option takes priority over the BDD library operation cache ratio option. "
                        + "[DEFAULT=off]",

                // cmdShort
                null,

                // cmdLong
                "bdd-cache-size",

                // cmdValue
                "SIZE",

                // defaultValue
                null,

                // minimumValue
                2,

                // maximumValue
                Integer.MAX_VALUE,

                // pageIncrementValue
                1000,

                // showInDialog
                true,

                // optDialogDescr
                "The fixed size of the operation cache of the BDD library.",

                // optDialogLabelText
                "Cache size:",

                // hasSpecialValue
                true,

                // defaultNormalValue
                1000,

                // specialValueSyntax
                "off",

                // optDialogSpecialText
                "Fixed cache size disabled",

                // optDialogNormalText
                "Fixed cache size enabled");
    }

    /**
     * Returns the size of operation cache of the BDD library, or {@code null} if disabled.
     *
     * @return The size of operation cache of the BDD library, or {@code null}.
     */
    public static Integer getCacheSize() {
        return Options.get(BddOpCacheSizeOption.class);
    }
}
