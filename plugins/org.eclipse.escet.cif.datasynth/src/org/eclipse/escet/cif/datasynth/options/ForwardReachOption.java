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

/** Forward reachability option. */
public class ForwardReachOption extends BooleanOption {
    /** Constructor for the {@link ForwardReachOption} class. */
    public ForwardReachOption() {
        super(
                // name
                "Forward reachability",

                // description
                "Whether to perform forward reachability during synthesis (BOOL=yes) or omit it (BOOL=no). "
                        + "[DEFAULT=no]",

                // cmdShort
                null,

                // cmdLong
                "forward-reach",

                // cmdValue
                "BOOL",

                // defaultValue
                false,

                // showInDialog
                true,

                // optDialogDescr
                "Perform forward reachability during synthesis or omit it.",

                // optDialogCheckboxText
                "Perform forward reachability");
    }

    /**
     * Is forward reachability enabled?
     *
     * @return {@code true} if forward reachability is enabled, {@code false} otherwise.
     */
    public static boolean isEnabled() {
        return Options.get(ForwardReachOption.class);
    }
}
