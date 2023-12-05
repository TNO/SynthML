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

package org.eclipse.escet.cif.datasynth.conversion;

import static org.eclipse.escet.cif.common.CifTextUtils.getAbsName;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignment;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newIntType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;
import static org.eclipse.escet.common.java.Maps.mapc;

import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.cif2cif.LocationPointerManager;
import org.eclipse.escet.cif.common.CifGuardUtils.LocRefExprCreator;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.types.IntType;
import org.eclipse.escet.common.java.Assert;

/** Location pointer manager for data-based synthesis. */
public class CifDataSynthesisLocationPointerManager implements LocationPointerManager {
    /** Mapping of automata to location pointer variables. */
    private final Map<Automaton, DiscVariable> autToLpMap;

    /** Mapping of location pointer variables to automata. */
    private final Map<DiscVariable, Automaton> lpToAutMap;

    /**
     * Constructor for the {@link CifDataSynthesisLocationPointerManager} class.
     *
     * @param automata The automata that need location pointers. They must each have more than one location.
     */
    public CifDataSynthesisLocationPointerManager(List<Automaton> automata) {
        // Ensure that the automata actual require location pointer variables.
        Assert.check(automata.stream().allMatch(aut -> aut.getLocations().size() > 1));

        // Create a dummy specification to hold the location pointer variables, for proper containment.
        Specification dummySpec = newSpecification();

        // Construct location pointer mappings.
        autToLpMap = mapc(automata.size());
        lpToAutMap = mapc(automata.size());
        for (Automaton automaton: automata) {
            // Create discrete variable. Set name for debugging only, even though absolute automata names are not valid
            // names for variables. No initial value or type is set.
            DiscVariable var = newDiscVariable();
            var.setName(getAbsName(automaton));

            // Store discrete variable.
            autToLpMap.put(automaton, var);
            lpToAutMap.put(var, automaton);

            // Add variable to dummy specification for proper containment. Note that technically, discrete variables
            // need to be contained in automata.
            dummySpec.getDeclarations().add(var);
        }
    }

    /**
     * Returns the location pointer variable for the given automaton, or {@code null} if no location pointer was created
     * for the automaton.
     *
     * @param automaton The automaton.
     * @return The location pointer variable, or {@code null}.
     */
    public DiscVariable getLocationPointer(Automaton automaton) {
        return autToLpMap.get(automaton);
    }

    /**
     * Returns the automaton for the given location pointer variable, or {@code null} if the discrete variable does not
     * represent a location pointer variable.
     *
     * @param locPtr The location pointer variable.
     * @return The automaton, or {@code null}
     */
    public Automaton getAutomaton(DiscVariable locPtr) {
        return lpToAutMap.get(locPtr);
    }

    @Override
    public Update createLocUpdate(Location loc) {
        // Get 0-based location index.
        Automaton aut = (Automaton)loc.eContainer();
        int locIdx = aut.getLocations().indexOf(loc);

        // Get variable.
        DiscVariable lp = autToLpMap.get(aut);
        Assert.notNull(lp);

        // Get integer type 'int[0..n-1] for 'n' locations.
        IntType type = newIntType();
        type.setLower(0);
        type.setUpper(aut.getLocations().size() - 1);

        // Create and return 'lp := locIdx' assignment.
        DiscVariableExpression lpRef = newDiscVariableExpression();
        lpRef.setVariable(lp);
        lpRef.setType(type);

        Assignment asgn = newAssignment();
        asgn.setAddressable(lpRef);
        asgn.setValue(CifValueUtils.makeInt(locIdx));

        return asgn;
    }

    @Override
    public Expression createLocRef(Location loc) {
        // Create CIF location reference expression, to be converted later.
        return LocRefExprCreator.DEFAULT.create(loc);
    }
}
