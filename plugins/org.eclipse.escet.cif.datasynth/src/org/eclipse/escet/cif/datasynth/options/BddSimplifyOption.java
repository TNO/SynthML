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

import java.util.EnumSet;

import org.eclipse.escet.common.app.framework.options.EnumSetOption;
import org.eclipse.escet.common.app.framework.options.Options;

/** BDD predicate simplify option. */
public class BddSimplifyOption extends EnumSetOption<BddSimplify> {
    /** Constructor for the {@link BddSimplifyOption} class. */
    public BddSimplifyOption() {
        super(
                // name
                "BDD predicate simplify",

                // description
                "Specify comma separated names of the desired BDD predicate simplifications to perform. Specify " +

                        "\"guards-plants\" to simplify supervisor guards wrt plant guards, " +

                        "\"guards-req-auts\" to simplify supervisor guards wrt state/event exclusion requirement "
                        + "invariants derived from the requirement automata, " +

                        "\"guard-se-excl-plant-invs\" to simplify supervisor guards wrt state/event exclusion "
                        + "plant invariants from the input specification, " +

                        "\"guards-se-excl-req-invs\" to simplify supervisor guards wrt state/event exclusion "
                        + "requirement invariants from the input specification, " +

                        "\"guards-state-plant-invs\" to simplify supervisor guards wrt state plant invariants from the "
                        + "input specification, " +

                        "\"guards-state-req-invs\" to simplify supervisor guards wrt state requirement invariants "
                        + "from the input specification, " +

                        "\"guards-ctrl-beh\" to simplify supervisor guards wrt controlled behavior, " +

                        "\"initial-unctrl\" to simplify the initialization predicate of the controlled system wrt "
                        + "the initialization predicate of the uncontrolled system, " +

                        "and/or " +

                        "\"initial-state-plant-invs\" to simplify the initialization predicate of the controlled system wrt "
                        + "the state plant invariants. " +

                        "Prefix a name with \"+\" to add it on top of the defaults, or with \\\"-\\\" to remove it "
                        + "from the defaults. " +

                        "By default, all simplifications are enabled.",

                // cmdShort
                null,

                // cmdLong
                "bdd-simplify",

                // cmdValue
                "SIMPLIFICATION",

                // defaultValue
                EnumSet.allOf(BddSimplify.class),

                // showInDialog
                true,

                // optDialogDescr
                "The desired BDD predicate simplifications to perform.",

                // enumClass
                BddSimplify.class);
    }

    @Override
    protected String getDialogText(BddSimplify simplification) {
        switch (simplification) {
            case GUARDS_PLANTS:
                return "Supervisor guards wrt their plant guards.";

            case GUARDS_REQ_AUTS:
                return "Supervisor guards wrt state/event exclusion requirement invariants derived from the "
                        + "requirement automata.";

            case GUARDS_SE_EXCL_PLANT_INVS:
                return "Supervisor guards wrt state/event exclusion plant invariants from the input specification.";

            case GUARDS_SE_EXCL_REQ_INVS:
                return "Supervisor guards wrt state/event exclusion requirement invariants from the input "
                        + "specification.";

            case GUARDS_STATE_PLANT_INVS:
                return "Supervisor guards wrt state plant invariants from the input specification.";

            case GUARDS_STATE_REQ_INVS:
                return "Supervisor guards wrt state requirement invariants from the input specification.";

            case GUARDS_CTRL_BEH:
                return "Supervisor guards wrt controlled behavior.";

            case INITIAL_UNCTRL:
                return "Initialization predicate of the controlled system wrt the initialization predicate of the "
                        + "uncontrolled system.";

            case INITIAL_STATE_PLANT_INVS:
                return "Initialization predicate of the controlled system wrt the state plant invariants.";
        }
        throw new RuntimeException("Unknown simplification: " + simplification);
    }

    /**
     * Returns the BDD predicate simplifications to perform.
     *
     * @return The BDD predicate simplifications to perform.
     */
    public static EnumSet<BddSimplify> getSimplifications() {
        return Options.get(BddSimplifyOption.class);
    }
}
