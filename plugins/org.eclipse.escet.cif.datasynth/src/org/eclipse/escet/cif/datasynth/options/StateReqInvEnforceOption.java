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

import org.eclipse.escet.cif.datasynth.options.StateReqInvEnforceOption.StateReqInvEnforceMode;
import org.eclipse.escet.common.app.framework.options.EnumOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** Option to specify how state requirement invariants are enforced. */
public class StateReqInvEnforceOption extends EnumOption<StateReqInvEnforceMode> {
    /** Constructor for the {@link StateReqInvEnforceOption} class. */
    public StateReqInvEnforceOption() {
        super(
                // name
                "State requirement invariant enforcement",

                // description
                "Specify how state requirement invariants are enforced during synthesis. "
                        + "Specify \"all-ctrl-beh\" (default) to enforce all of them via the controlled behavior, "
                        + "or \"per-edge\" to decide per edge how to enforce them, enforcing them via edge guards for "
                        + "edges with controllable events, and enforcing them via the controlled behavior for edges "
                        + "with uncontrollable events.",

                // cmdShort
                null,

                // cmdLong
                "state-req-invs",

                // cmdValue
                "MODE",

                // defaultValue
                StateReqInvEnforceMode.ALL_CTRL_BEH,

                // showInDialog
                true,

                // optDialogDescr
                "Specify how state requirement invariants are enforced during synthesis. "
                        + "Either enforce all of them via the controlled behavior, or decide per edge how to enforce "
                        + "them, enforcing them via edge guards for edges with controllable events, and enforcing "
                        + "them via the controlled behavior for edges with uncontrollable events.");
    }

    @Override
    protected String getDialogText(StateReqInvEnforceMode mode) {
        switch (mode) {
            case ALL_CTRL_BEH:
                return "All via controlled behavior";
            case PER_EDGE:
                return "Decide per edge";
        }
        throw new RuntimeException("Unknown mode: " + mode);
    }

    /**
     * Returns the value of the {@link StateReqInvEnforceOption} option.
     *
     * @return The value of the {@link StateReqInvEnforceOption} option.
     */
    public static StateReqInvEnforceMode getMode() {
        return Options.get(StateReqInvEnforceOption.class);
    }

    /** The way that state requirement invariants are enforced. */
    public static enum StateReqInvEnforceMode {
        /** Enforce all of them via the controlled behavior. */
        ALL_CTRL_BEH,

        /**
         * Decide per edge how to enforce them, enforcing them via edge guards for edges with controllable events, and
         * enforcing them via the controlled behavior for edges with uncontrollable events.
         */
        PER_EDGE;
    }
}
