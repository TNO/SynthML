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

package org.eclipse.escet.cif.datasynth.bdd;

import static org.eclipse.escet.common.java.Strings.fmt;

import com.github.javabdd.BDD;

/** A {@link CifBddBitVector} and a carry {@link BDD}. */
public class CifBddBitVectorAndCarry {
    /** The BDD bit vector. */
    public final CifBddBitVector vector;

    /** The carry bit as BDD. */
    public final BDD carry;

    /**
     * Constructor for the {@link CifBddBitVectorAndCarry} class.
     *
     * @param vector The BDD bit vector.
     * @param carry The carry bit as BDD.
     */
    public CifBddBitVectorAndCarry(CifBddBitVector vector, BDD carry) {
        this.vector = vector;
        this.carry = carry;
    }

    @Override
    public String toString() {
        return fmt("(%s, %s)", vector, carry);
    }
}
