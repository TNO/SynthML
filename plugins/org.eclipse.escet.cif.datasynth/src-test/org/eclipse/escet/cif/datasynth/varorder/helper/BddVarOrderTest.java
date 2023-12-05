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

package org.eclipse.escet.cif.datasynth.varorder.helper;

import static org.eclipse.escet.common.java.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDFactory;

/** BDD variable order tests. */
public class BddVarOrderTest {
    @Test
    @SuppressWarnings("javadoc")
    public void testHighLowBit() {
        // Create factory with 3 variables: a < b < c.
        BDDFactory factory = BDDFactory.init("java", 100, 100);
        factory.setVarNum(3);

        // Get each variable.
        BDD a = factory.ithVar(0);
        BDD b = factory.ithVar(1);
        BDD c = factory.ithVar(2);

        // Each variable's 0-based index corresponds to its level.
        assertEquals(0, a.level());
        assertEquals(1, b.level());
        assertEquals(2, c.level());

        // Create '(!a and b and c) or (a and !b and !c)' BDD.
        BDD pred = a.not().and(b).and(c).or(a.and(b.not()).and(c.not()));

        // Check satisfying assignments.
        assertEquals("!a b c, a !b !c", bddToAllSatText(pred));

        // Reverse variable order.
        factory.setVarOrder(new int[] {2, 1, 0}); // New order: c < b < a.

        // Check reverse levels.
        assertEquals(2, a.level());
        assertEquals(1, b.level());
        assertEquals(0, c.level());

        // Check reverse satisfying assignments.
        assertEquals("!c !b a, c b !a", bddToAllSatText(pred));
    }

    /**
     * Get text representing all satisfying assignments to the given BDD.
     *
     * @param bdd The BDD.
     * @return The text.
     */
    private static String bddToAllSatText(BDD bdd) {
        List<String> sats = list();
        bddToAllSatTexts(bdd, "", sats);
        return sats.stream().map(String::trim).collect(Collectors.joining(", "));
    }

    /**
     * Get texts representing all satisfying assignments to the given BDD.
     *
     * @param bdd The BDD.
     * @param cursat The current satisfying assignment based on ancestor BDD nodes.
     * @param sats The satisfying assignments.
     */
    private static void bddToAllSatTexts(BDD bdd, String cursat, List<String> sats) {
        if (bdd.isOne()) {
            sats.add(cursat);
        } else if (bdd.isZero()) {
            // Ignore.
        } else {
            int var = bdd.getFactory().level2Var(bdd.level());
            char c = (char)('a' + var);
            bddToAllSatTexts(bdd.low(), cursat + " !" + c, sats);
            bddToAllSatTexts(bdd.high(), cursat + " " + c, sats);
        }
    }
}
