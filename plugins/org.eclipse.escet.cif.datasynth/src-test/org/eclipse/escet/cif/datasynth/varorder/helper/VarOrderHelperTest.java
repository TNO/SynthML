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

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAutomaton;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newInputVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newIntType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocation;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;
import static org.eclipse.escet.common.java.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.BitSet;
import java.util.List;

import org.eclipse.escet.cif.datasynth.options.BddHyperEdgeAlgoOption;
import org.eclipse.escet.cif.datasynth.options.BddHyperEdgeAlgoOption.BddHyperEdgeAlgo;
import org.eclipse.escet.cif.datasynth.spec.SynthesisDiscVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisInputVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisLocPtrVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.graph.Graph;
import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.InputVariable;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.box.CodeBox;
import org.eclipse.escet.common.box.MemoryCodeBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests for {@link VarOrderHelper}. */
public class VarOrderHelperTest {
    @SuppressWarnings("javadoc")
    @BeforeAll
    public static void beforeClass() {
        AppEnv.registerSimple();
        Options.set(BddHyperEdgeAlgoOption.class, BddHyperEdgeAlgo.LEGACY);
    }

    @SuppressWarnings("javadoc")
    @AfterAll
    public static void afterClass() {
        AppEnv.unregisterApplication();
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testReorder() {
        // Create CIF specification.
        Specification spec = newSpecification();

        InputVariable va = newInputVariable(null, "a", null, newIntType(0, null, 0));
        InputVariable vb = newInputVariable(null, "b", null, newIntType(0, null, 0));
        InputVariable vc = newInputVariable(null, "c", null, newIntType(0, null, 0));
        InputVariable vd = newInputVariable(null, "d", null, newIntType(0, null, 0));
        InputVariable ve = newInputVariable(null, "e", null, newIntType(0, null, 0));
        spec.getDeclarations().add(va);
        spec.getDeclarations().add(vb);
        spec.getDeclarations().add(vc);
        spec.getDeclarations().add(vd);
        spec.getDeclarations().add(ve);

        Automaton aut = newAutomaton();
        spec.getComponents().add(aut);
        aut.setName("f");
        Location loc1 = newLocation();
        Location loc2 = newLocation();
        aut.getLocations().add(loc1);
        aut.getLocations().add(loc2);
        loc1.setName("loc1");
        loc2.setName("loc2");
        DiscVariable vf = newDiscVariable(null, "f", null, null, null);

        // Create synthesis variables.
        SynthesisVariable a = new SynthesisInputVariable(va, newIntType(0, null, 0), 1, 0, 0);
        SynthesisVariable b = new SynthesisInputVariable(vb, newIntType(0, null, 0), 1, 0, 0);
        SynthesisVariable c = new SynthesisInputVariable(vc, newIntType(0, null, 0), 1, 0, 0);
        SynthesisVariable d = new SynthesisInputVariable(vd, newIntType(0, null, 0), 1, 0, 0);
        SynthesisVariable e = new SynthesisInputVariable(ve, newIntType(0, null, 0), 1, 0, 0);
        SynthesisVariable f = new SynthesisLocPtrVariable(aut, vf);
        List<SynthesisVariable> variables = list(a, b, c, d, e, f);

        // Reorder the variables.
        int[] newIndices = {0, 4, 1, 5, 2, 3}; // For each variable in 'variables', its new 0-based index.
        VarOrderHelper helper = new VarOrderHelper(spec, variables);
        List<SynthesisVariable> ordered = helper.reorderForNewIndices(newIndices);

        // Check the result. Invariant: ordered[newIndices[i]] == variables[i].
        assertSame(a, ordered.get(0));
        assertSame(c, ordered.get(1));
        assertSame(e, ordered.get(2));
        assertSame(f, ordered.get(3));
        assertSame(b, ordered.get(4));
        assertSame(d, ordered.get(5));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testRepresentationsAndMetrics() {
        // CIF specification.
        CodeBox box = new MemoryCodeBox();
        box.add("input int[0..2] a;");
        box.add("input int[0..2] b;");
        box.add("input int[0..2] d;");
        box.add("input int[0..2] e;");
        box.add("controllable c_e;");
        box.add("plant p:");
        box.add("  disc int[0..2] c;");
        box.add("  location:");
        box.add("    initial; marked;");
        box.add("    edge c_e when a = b or b = a;");
        box.add("    edge c_e when a = c;");
        box.add("    edge c_e do c := d;");
        box.add("    edge c_e do c := d;");
        box.add("    edge c_e do c := d;");
        box.add("end");
        box.add("invariant p.c != e;");

        // Read CIF specification.
        CifReader reader = new CifReader();
        reader.init("memory", "/memory", false);
        Specification spec = reader.read(box.toString());

        // Create synthesis variables.
        Automaton p = (Automaton)spec.getComponents().get(0);
        InputVariable va = (InputVariable)spec.getDeclarations().get(0);
        InputVariable vb = (InputVariable)spec.getDeclarations().get(1);
        InputVariable vd = (InputVariable)spec.getDeclarations().get(2);
        InputVariable ve = (InputVariable)spec.getDeclarations().get(3);
        DiscVariable vc = (DiscVariable)p.getDeclarations().get(0);
        SynthesisVariable a = new SynthesisInputVariable(va, newIntType(0, null, 2), 3, 0, 2);
        SynthesisVariable b = new SynthesisInputVariable(vb, newIntType(0, null, 2), 3, 0, 2);
        SynthesisVariable c = new SynthesisDiscVariable(vc, newIntType(0, null, 2), 3, 0, 2);
        SynthesisVariable d = new SynthesisInputVariable(vd, newIntType(0, null, 2), 3, 0, 2);
        SynthesisVariable e = new SynthesisInputVariable(ve, newIntType(0, null, 2), 3, 0, 2);
        List<SynthesisVariable> variables = list(a, b, c, d, e);

        // Create helper.
        VarOrderHelper helper = new VarOrderHelper(spec, variables);

        // Test hyper-edges: c/e (invariant), a/b (guard), b/a (guard), a/c (guard), c/d (update), c/d (update), c/d
        // (update), a/b/c/d (event c_e).
        List<BitSet> hyperEdges = helper.getHyperEdges(RelationsKind.LEGACY);
        assertEquals("[{2, 4}, {0, 1}, {0, 1}, {0, 2}, {2, 3}, {2, 3}, {2, 3}, {0, 1, 2, 3}]", hyperEdges.toString());

        // Test graph edges: (b, 3, a), (a, 2, c), (a, 1, d), (b, 1, c), (b, 1, d), (c, 4, d), (c, 1, e).
        Graph graph = helper.getGraph(RelationsKind.LEGACY);
        String expectedGraphText = String.join("\n", list( //
                ".321.", //
                "3.11.", //
                "21.41", //
                "114..", //
                "..1.."));
        assertEquals(expectedGraphText, graph.toString());

        // Test metrics for original order.
        // Total span = 2 + 1 + 1 + 2 + 1 + 1 + 1 + 3 = 12.
        // WES = 8/5*3/40 + 2/5*2/40 + 2/5*2/40 + 4/5*3/40 + 6/5*2/40 + 6/5*2/40 + 6/5*2/40 + 6/5*4/40 = 0.52.
        int[] defaultIndices = {0, 1, 2, 3, 4}; // For each variable in 'variables', its new 0-based index.
        assertEquals("Total span:   12 (total)   1.50 (avg/edge) / WES:   0.520000 (total)   0.065000 (avg/edge) [x]",
                helper.fmtMetrics(defaultIndices, "x", RelationsKind.LEGACY));

        // Test metrics for reverse order.
        // Total span: unchanged from original order.
        // WES = 4/5*3/40 + 8/5*2/40 + 8/5*2/40 + 8/5*3/40 + 4/5*2/40 + 4/5*2/40 + 4/5*2/40 + 8/5*4/40 = 0.52.
        int[] reverseIndices = {4, 3, 2, 1, 0}; // For each variable in 'variables', its new 0-based index.
        assertEquals("Total span:   12 (total)   1.50 (avg/edge) / WES:   0.620000 (total)   0.077500 (avg/edge) [x]",
                helper.fmtMetrics(reverseIndices, "x", RelationsKind.LEGACY));

        // Test metrics for a random order.
        // Total span: 2 + 4 + 4 + 1 + 1 + 1 + 1 + 4 = 18.
        // WES: 6/5*3/40 + 8/5*5/40 + 8/5*5/40 + 2/5*2/40 + 4/5*2/40 + 4/5*2/40 + 4/5*2/40 + 8/5*5/40 = 0.83.
        int[] randomIndices = {0, 4, 1, 2, 3}; // For each variable in 'variables', its new 0-based index.
        assertEquals("Total span:   18 (total)   2.25 (avg/edge) / WES:   0.830000 (total)   0.103750 (avg/edge) [x]",
                helper.fmtMetrics(randomIndices, "x", RelationsKind.LEGACY));
    }
}
