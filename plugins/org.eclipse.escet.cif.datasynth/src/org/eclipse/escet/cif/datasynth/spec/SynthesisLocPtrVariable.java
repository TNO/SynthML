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

package org.eclipse.escet.cif.datasynth.spec;

import static org.eclipse.escet.common.java.Strings.fmt;

import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.common.java.Assert;

/**
 * Information on a location pointer variable for an automaton, used for synthesis.
 *
 * <p>
 * Location pointer variables are only created for automata with at least two locations.
 * </p>
 */
public class SynthesisLocPtrVariable extends SynthesisVariable {
    /** The automaton for which this variable is a location pointer. */
    public final Automaton aut;

    /**
     * A dummy, internally-created CIF discrete variable that corresponds to this synthesis variable. Does not have a
     * data type.
     */
    public final DiscVariable var;

    /**
     * Constructor for the {@link SynthesisLocPtrVariable} class.
     *
     * @param aut The automaton for which this variable is a location pointer.
     * @param var A dummy, internally-created CIF discrete variable that corresponds to this synthesis variable. Does
     *     not have a data type.
     */
    public SynthesisLocPtrVariable(Automaton aut, DiscVariable var) {
        super(aut, aut.getLocations().size(), 0, aut.getLocations().size() - 1);

        this.aut = aut;
        this.var = var;

        Assert.check(var.getType() == null);
        Assert.check(aut.getLocations().size() > 1);
    }

    @Override
    public int getDomainSize() {
        // [0..n-1] for automaton with 'n' locations.
        Assert.check(count > 1);
        Assert.check(count == aut.getLocations().size());
        return count;
    }

    @Override
    public String getKindText() {
        return "location pointer";
    }

    @Override
    public String getTypeText() {
        return null;
    }

    @Override
    protected String toStringInternal() {
        return fmt("location pointer for automaton \"%s\"", name);
    }
}
