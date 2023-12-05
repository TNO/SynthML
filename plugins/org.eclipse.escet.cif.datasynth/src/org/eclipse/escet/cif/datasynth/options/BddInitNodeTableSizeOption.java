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

/** BDD library initial node table size option. */
public class BddInitNodeTableSizeOption extends IntegerOption {
    /** Constructor for the {@link BddInitNodeTableSizeOption} class. */
    public BddInitNodeTableSizeOption() {
        super(
                // name
                "BDD library initial node table size",

                // description
                "The initial size of the node table of the BDD library. Value must be in the range [1 .. 2^31-1]. "
                        + "[DEFAULT=100000]",

                // cmdShort
                null,

                // cmdLong
                "bdd-table",

                // cmdValue
                "SIZE",

                // defaultValue
                100000,

                // minimumValue
                1,

                // maximumValue
                Integer.MAX_VALUE,

                // pageIncrementValue
                10000,

                // showInDialog
                true,

                // optDialogDescr
                "The initial size of the node table of the BDD library.",

                // optDialogLabelText
                "Initial size:");
    }

    /**
     * Returns the initial size of the node table of the BDD library.
     *
     * @return The initial size of the node table of the BDD library.
     */
    public static Integer getInitialSize() {
        return Options.get(BddInitNodeTableSizeOption.class);
    }
}
