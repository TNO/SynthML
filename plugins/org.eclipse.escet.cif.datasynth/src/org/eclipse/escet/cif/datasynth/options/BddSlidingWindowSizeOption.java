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

/** BDD sliding window size option. */
public class BddSlidingWindowSizeOption extends IntegerOption {
    /** The default value of the {@link BddSlidingWindowSizeOption} option. */
    public static final int DEFAULT_VALUE = 4;

    /** Constructor for the {@link BddSlidingWindowSizeOption} class. */
    public BddSlidingWindowSizeOption() {
        super(
                // name
                "BDD sliding window size",

                // description
                "The maximum length of the window to use for the BDD sliding window variable ordering algorithm. "
                        + "Must be an integer number in the range [1 .. 12]. [DEFAULT=4]",

                // cmdShort
                null,

                // cmdLong
                "sliding-window-size",

                // cmdValue
                "SIZE",

                // defaultValue
                DEFAULT_VALUE,

                // minimumValue
                1,

                // maximumValue
                12,

                // pageIncrementValue
                1,

                // showInDialog
                true,

                // optDialogDescr
                "The maximum length of the window to use for the BDD sliding window variable ordering algorithm.",

                // optDialogLabelText
                "Maximum size:");
    }

    /**
     * Returns the maximum length of the window to use for the BDD sliding window variable ordering algorithm.
     *
     * @return The maximum length of the window.
     */
    public static int getMaxLen() {
        return Options.get(BddSlidingWindowSizeOption.class);
    }

    /**
     * Returns whether the {@link BddSlidingWindowSizeOption} option is configured with its default value.
     *
     * @return {@code true} if the option is configured with its default value, {@code false} otherwise.
     */
    public static boolean isDefault() {
        return getMaxLen() == DEFAULT_VALUE;
    }
}
