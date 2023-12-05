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

import org.eclipse.escet.common.app.framework.exceptions.InvalidOptionException;
import org.eclipse.escet.common.app.framework.options.StringOption;

/** Edge order option. */
public class EdgeOrderOption extends StringOption {
    /** Message to indicate the option is unsupported. */
    private static final String UNSUPPORTED_MESSAGE = "This option is no longer supported. "
            + "It will be removed in a future version of the tool. "
            + "Use the 'Edge order for backward reachability' and "
            + "'Edge order for forward reachability' options instead.";

    /** Constructor for the {@link EdgeOrderOption} class. */
    public EdgeOrderOption() {
        super(
                // name
                "Edge order",

                // description
                "The edge ordering. " + UNSUPPORTED_MESSAGE,

                // cmdShort
                'e',

                // cmdLong
                "edge-order",

                // cmdValue
                "EDGEORDER",

                // defaultValue
                "model",

                // emptyAsNull
                false,

                // showInDialog
                false,

                // optDialogDescr
                null,

                // optDialogLabelText
                null);
    }

    @Override
    public String parseValue(String optName, String value) {
        throw new InvalidOptionException("The edge ordering option is used. " + UNSUPPORTED_MESSAGE);
    }
}
