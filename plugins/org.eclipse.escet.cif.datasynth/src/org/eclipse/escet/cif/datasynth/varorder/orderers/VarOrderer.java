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

package org.eclipse.escet.cif.datasynth.varorder.orderers;

import java.util.Locale;

import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;

/** Variable orderer. May produce a fixed variable order, apply an algorithm, etc. */
public abstract class VarOrderer {
    /**
     * Order synthesis variables.
     *
     * <p>
     * In general, there are no guarantees that the new order is always a 'better' order, though some algorithms may
     * offer such guarantees. Some heuristic algorithms may in certain cases even produce 'worse' orders.
     * </p>
     *
     * @param inputData The variable order data to be used as input for the orderer.
     * @param dbgEnabled Whether debug output is enabled.
     * @param dbgLevel The debug indentation level.
     * @return The variable order data produced as output by the orderer.
     */
    public abstract VarOrdererData order(VarOrdererData inputData, boolean dbgEnabled, int dbgLevel);

    /**
     * Returns the textual option syntax for the given enumeration constant value.
     *
     * @param <T> The type of the enumeration.
     * @param value The enumeration constant value.
     * @return The textual option syntax.
     */
    protected <T extends Enum<T>> String enumValueToParserArg(T value) {
        return value.name().toLowerCase(Locale.US).replace("_", "-");
    }
}
