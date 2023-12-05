//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

/** Plants referencing requirements warnings option. */
public class PlantsRefReqsWarnOption extends BooleanOption {
    /** Constructor for the {@link PlantsRefReqsWarnOption} class. */
    public PlantsRefReqsWarnOption() {
        super(
                // name
                "Plants referencing requirements warnings",

                // description
                "Whether to warn for plants that reference requirement state (BOOL=yes) or don't warn (BOOL=no). "
                        + "[DEFAULT=yes]",

                // cmdShort
                null,

                // cmdLong
                "plant-ref-req-warn",

                // cmdValue
                "BOOL",

                // defaultValue
                true,

                // showInDialog
                true,

                // optDialogDescr
                "Whether to warn for plants that reference requirement state.",

                // optDialogCheckboxText
                "Warn for plants that reference requirement state");
    }

    /**
     * Are warnings for plants referencing requirements enabled?
     *
     * @return {@code true} if warnings for plants referencing requirements are enabled, {@code false} otherwise.
     */
    public static boolean isEnabled() {
        return Options.get(PlantsRefReqsWarnOption.class);
    }
}
