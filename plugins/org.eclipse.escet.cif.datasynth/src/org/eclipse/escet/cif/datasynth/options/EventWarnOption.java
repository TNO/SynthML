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

import org.eclipse.escet.common.app.framework.options.BooleanOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** Event warning option. */
public class EventWarnOption extends BooleanOption {
    /** Constructor for the {@link EventWarnOption} class. */
    public EventWarnOption() {
        super(
                // name
                "Event warning",

                // description
                "Whether to warn for events that are never enabled in the input specification or always disabled by "
                        + "the synthesized supervisor (BOOL=yes) or don't warn (BOOL=no). [DEFAULT=yes]",

                // cmdShort
                null,

                // cmdLong
                "event-warn",

                // cmdValue
                "BOOL",

                // defaultValue
                true,

                // showInDialog
                true,

                // optDialogDescr
                "Whether to warn for events that are never enabled in the input specification or always disabled by "
                        + "the synthesized supervisor.",

                // optDialogCheckboxText
                "Warn for never enabled events");
    }

    /**
     * Is warning for never enabled events enabled?
     *
     * @return {@code true} if warning for never enabled events is enabled, {@code false} otherwise.
     */
    public static boolean isEnabled() {
        return Options.get(EventWarnOption.class);
    }
}
