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

import static org.eclipse.escet.cif.datasynth.bdd.BddToCif.bddToCifPred;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.doout;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.out;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.List;

import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDFactory.GCStats;

/** BDD utility methods. */
public class BddUtils {
    /** Constructor for the {@link BddUtils} class. */
    private BddUtils() {
        // Static class.
    }

    /**
     * Returns the minimum number of bits need to represent the given decimal value in a binary representation.
     *
     * @param value The decimal value. Must be non-negative.
     * @return The minimum number of bits.
     */
    public static int getMinimumBits(int value) {
        int count = 0;
        while (value > 0) {
            count++;
            value = value >> 1;
        }
        return count;
    }

    /**
     * Returns the domain predicate of the given variable. That is, a predicate that is 'true' if and only if the
     * variable has a valid value.
     *
     * <p>
     * The result of this method includes only the values of the CIF domain of the variable, not the BDD domain of the
     * variable. For a variable 'x' of type 'int[1..2]', two BDD variables are created, which can represent values of
     * type 'int[0..3]'. Values '0' and '3' are not valid values of variable 'x'. This method returns 'x = 1 or x = 2'.
     * </p>
     *
     * @param variable The synthesis variable.
     * @param newDomain Whether to return a predicate for the pre/old domain of the variable ({@code false}) or the
     *     post/new domain of the variable ({@code true}).
     * @param factory The BDD factory to use.
     * @return The possible values of the variable being assigned.
     */
    public static BDD getVarDomain(SynthesisVariable variable, boolean newDomain, BDDFactory factory) {
        // Get minimum and maximum value of the domain.
        int min = variable.lower;
        int max = variable.upper;

        // Add possible values to the result.
        BDDDomain domain = newDomain ? variable.domainNew : variable.domain;
        BDD rslt = factory.zero();
        for (int i = min; i <= max; i++) {
            rslt = rslt.orWith(domain.ithVar(i));
        }
        return rslt;
    }

    /**
     * Converts a BDD to a textual representation that closely resembles CIF ASCII syntax.
     *
     * @param bdd The BDD.
     * @param aut The synthesis automaton.
     * @return The textual representation of the BDD.
     */
    public static String bddToStr(BDD bdd, SynthesisAutomaton aut) {
        // If one of the specific maximum counts is exceeded, don't actually
        // convert the BDD to a CNF/DNF predicate, for performance reasons.
        if (aut.debugMaxNodes != null || aut.debugMaxPaths != null) {
            // Get node count and true path count.
            int nc = bdd.nodeCount();
            double tpc = bdd.pathCount();

            boolean skip = (aut.debugMaxNodes != null && nc > aut.debugMaxNodes)
                    || (aut.debugMaxPaths != null && tpc > aut.debugMaxPaths);
            if (skip) {
                return fmt("<bdd %,dn %,.0fp>", nc, tpc);
            }
        }

        // Convert BDD to CNF/DNF predicate.
        Expression pred = bddToCifPred(bdd, aut);
        return CifTextUtils.exprToStr(pred);
    }

    /**
     * If requested, register BDD factory callbacks that print some statistics.
     *
     * @param factory The BDD factory for which to register the callbacks.
     * @param doGcStats Whether to output BDD GC statistics.
     * @param doResizeStats Whether to output BDD resize statistics.
     * @param doContinuousPerformanceStats Whether to output continuous BDD performance statistics.
     * @param continuousOpMisses The list into which to collect continuous operation misses samples.
     * @param continuousUsedBddNodes The list into which to collect continuous used BDD nodes statistics samples.
     */
    public static void registerBddCallbacks(BDDFactory factory, boolean doGcStats, boolean doResizeStats,
            boolean doContinuousPerformanceStats, List<Long> continuousOpMisses, List<Integer> continuousUsedBddNodes)
    {
        // Register BDD garbage collection callback.
        if (doGcStats && doout()) {
            factory.registerGcStatsCallback(BddUtils::bddGcStatsCallback);
        }

        // Register BDD internal node array resize callback.
        if (doResizeStats && doout()) {
            factory.registerResizeStatsCallback(BddUtils::bddResizeStatsCallback);
        }

        // Register continuous BDD performance statistics callback.
        if (doContinuousPerformanceStats) {
            factory.registerContinuousStatsCallback((n, o) -> {
                continuousOpMisses.add(o);
                continuousUsedBddNodes.add(n);
            });
        }
    }

    /**
     * Callback invoked when the BDD library performs garbage collection on its internal data structures. Prints
     * statistics.
     *
     * @param stats The garbage collection statistics.
     * @param pre Whether the callback is invoked just before garbage collection ({@code true}) or just after it
     *     ({@code false}).
     */
    private static void bddGcStatsCallback(GCStats stats, boolean pre) {
        StringBuilder txt = new StringBuilder();
        txt.append("BDD ");
        txt.append(pre ? "pre " : "post");
        txt.append(" garbage collection: #");
        txt.append(fmt("%,d", stats.num + 1 - (pre ? 0 : 1)));
        txt.append(", ");
        txt.append(fmt("%,13d", stats.freenodes));
        txt.append(" of ");
        txt.append(fmt("%,13d", stats.nodes));
        txt.append(" nodes free");
        if (!pre) {
            txt.append(", ");
            txt.append(fmt("%,13d", stats.time));
            txt.append(" ms, ");
            txt.append(fmt("%,13d", stats.sumtime));
            txt.append(" ms total");
        }
        out(txt.toString());
    }

    /**
     * Callback invoked when the BDD library resizes its internal node array. Prints statistics.
     *
     * @param oldSize The old size of the internal node array.
     * @param newSize The new size of the internal node array.
     */
    private static void bddResizeStatsCallback(int oldSize, int newSize) {
        out("BDD node table resize: from %,13d nodes to %,13d nodes", oldSize, newSize);
    }
}
