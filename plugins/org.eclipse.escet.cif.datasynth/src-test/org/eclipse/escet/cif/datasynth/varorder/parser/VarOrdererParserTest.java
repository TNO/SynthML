//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.datasynth.varorder.parser;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newInputVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newIntType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;
import static org.eclipse.escet.common.java.Lists.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.eclipse.escet.cif.datasynth.options.BddAdvancedVariableOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddDcshVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddForceVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddHyperEdgeAlgoOption;
import org.eclipse.escet.cif.datasynth.options.BddHyperEdgeAlgoOption.BddHyperEdgeAlgo;
import org.eclipse.escet.cif.datasynth.options.BddSlidingWindowSizeOption;
import org.eclipse.escet.cif.datasynth.options.BddSlidingWindowVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddVariableOrderOption;
import org.eclipse.escet.cif.datasynth.spec.SynthesisInputVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.orderers.VarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererInstance;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.InputVariable;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.exceptions.InvalidOptionException;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.setext.runtime.DebugMode;
import org.eclipse.escet.setext.runtime.exceptions.SyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link VarOrdererParser} tests. */
public class VarOrdererParserTest {
    @BeforeEach
    @SuppressWarnings("javadoc")
    public void before() {
        AppEnv.registerSimple();
        Options.set(BddVariableOrderOption.class, BddVariableOrderOption.DEFAULT_VALUE);
        Options.set(BddDcshVarOrderOption.class, BddDcshVarOrderOption.DEFAULT_VALUE);
        Options.set(BddForceVarOrderOption.class, BddForceVarOrderOption.DEFAULT_VALUE);
        Options.set(BddSlidingWindowVarOrderOption.class, BddSlidingWindowVarOrderOption.DEFAULT_VALUE);
        Options.set(BddSlidingWindowSizeOption.class, BddSlidingWindowSizeOption.DEFAULT_VALUE);
        Options.set(BddHyperEdgeAlgoOption.class, BddHyperEdgeAlgoOption.DEFAULT_VALUE);
        Options.set(BddAdvancedVariableOrderOption.class, BddAdvancedVariableOrderOption.DEFAULT_VALUE);
    }

    @AfterEach
    @SuppressWarnings("javadoc")
    public void after() {
        AppEnv.unregisterApplication();
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testScannerInvalid() {
        testInvalid("%", "Scanning failed for character \"%\" (Unicode U+25) at line 1, column 1.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testWhitespace() {
        testValid("  model \t \n ( ) ", "model(effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testBasicValid() {
        testValid("basic",
                "sorted(effect=both) -> "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order) -> "
                        + "force(metric=total-span, relations=linearized, effect=var-order) -> "
                        + "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testBasicInvalid() {
        testInvalid("basic(a=1)",
                "Semantic error at line 1, column 7: The \"basic\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testModelValid() {
        testValid("model", "model(effect=both)");
        testValid("model()", "model(effect=both)");

        testValid("model(effect=var-order)", "model(effect=var-order)");
        testValid("model(effect=representations)", "model(effect=representations)");
        testValid("model(effect=both)", "model(effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testModelInvalid() {
        testInvalid("model(effect=1)",
                "Semantic error at line 1, column 7: "
                        + "The \"model\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("model(effect=both, effect=both)",
                "Semantic error at line 1, column 20: The \"model\" orderer has a duplicate \"effect\" argument.");

        testInvalid("model(a=1)",
                "Semantic error at line 1, column 7: The \"model\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSortedValid() {
        testValid("sorted", "sorted(effect=both)");
        testValid("sorted()", "sorted(effect=both)");

        testValid("sorted(effect=var-order)", "sorted(effect=var-order)");
        testValid("sorted(effect=representations)", "sorted(effect=representations)");
        testValid("sorted(effect=both)", "sorted(effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSortedInvalid() {
        testInvalid("sorted(effect=1)",
                "Semantic error at line 1, column 8: "
                        + "The \"sorted\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("sorted(effect=both, effect=both)",
                "Semantic error at line 1, column 21: The \"sorted\" orderer has a duplicate \"effect\" argument.");

        testInvalid("sorted(a=1)",
                "Semantic error at line 1, column 8: The \"sorted\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testRandomValid() {
        testValid("random", "random(effect=both)");
        testValid("random()", "random(effect=both)");

        testValid("random(seed=5)", "random(seed=5, effect=both)");
        testValid("random(seed=0)", "random(seed=0, effect=both)");
        testValid("random(seed=-1)", "random(seed=-1, effect=both)");

        testValid("random(effect=var-order)", "random(effect=var-order)");
        testValid("random(effect=representations)", "random(effect=representations)");
        testValid("random(effect=both)", "random(effect=both)");

        testValid("random(seed=1, effect=var-order)", "random(seed=1, effect=var-order)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testRandomInvalid() {
        testInvalid("random(seed=x)",
                "Semantic error at line 1, column 8: The \"random\" orderer has an unsupported value for the \"seed\" "
                        + "argument: the value must be a number.");
        testInvalid("random(seed=9223372036854775808)",
                "Semantic error at line 1, column 8: The \"random\" orderer has an unsupported value for the \"seed\" "
                        + "argument: the value is out of range.");
        testInvalid("random(seed=a())",
                "Semantic error at line 1, column 8: The \"random\" orderer has an unsupported value for the \"seed\" "
                        + "argument: the value must be a number.");
        testInvalid("random(seed=1, seed=2)",
                "Semantic error at line 1, column 16: The \"random\" orderer has a duplicate \"seed\" argument.");

        testInvalid("random(effect=1)",
                "Semantic error at line 1, column 8: "
                        + "The \"random\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("random(effect=both, effect=both)",
                "Semantic error at line 1, column 21: The \"random\" orderer has a duplicate \"effect\" argument.");

        testInvalid("random(a=1)",
                "Semantic error at line 1, column 8: The \"random\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testCustomValid() {
        Specification spec = newSpecification();
        InputVariable va = newInputVariable(null, "a", null, newIntType(0, null, 1));
        InputVariable vb = newInputVariable(null, "b", null, newIntType(0, null, 1));
        InputVariable vc = newInputVariable(null, "c", null, newIntType(0, null, 1));
        spec.getDeclarations().add(va);
        spec.getDeclarations().add(vb);
        spec.getDeclarations().add(vc);
        SynthesisVariable a = new SynthesisInputVariable(va, newIntType(0, null, 1), 2, 0, 1);
        SynthesisVariable b = new SynthesisInputVariable(vb, newIntType(0, null, 1), 2, 0, 1);
        SynthesisVariable c = new SynthesisInputVariable(vc, newIntType(0, null, 1), 2, 0, 1);
        List<SynthesisVariable> vars = list(a, b, c);

        testValid("custom(order=\"a,b,c\")", vars, "custom(effect=both, order=\"a,b,c\")");
        testValid("custom(order=\"c,a,b\")", vars, "custom(effect=both, order=\"c,a,b\")");
        testValid("custom(order=\"c,b,a\")", vars, "custom(effect=both, order=\"c,b,a\")");

        testValid("custom(order=\"a;b,c\")", vars, "custom(effect=both, order=\"a;b,c\")");
        testValid("custom(order=\"c,a;b\")", vars, "custom(effect=both, order=\"c,a;b\")");
        testValid("custom(order=\"c;b;a\")", vars, "custom(effect=both, order=\"c;b;a\")");

        testValid("custom(effect=var-order, order=\"a,b,c\")", vars, "custom(effect=var-order, order=\"a,b,c\")");
        testValid("custom(effect=representations, order=\"a,b,c\")", vars,
                "custom(effect=representations, order=\"a,b,c\")");
        testValid("custom(effect=both, order=\"a,b,c\")", vars, "custom(effect=both, order=\"a,b,c\")");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testCustomInvalid() {
        testInvalid("custom", "Semantic error at line 1, column 1: "
                + "The \"custom\" orderer is missing its mandatory \"order\" argument.");
        testInvalid("custom()", "Semantic error at line 1, column 1: "
                + "The \"custom\" orderer is missing its mandatory \"order\" argument.");
        testInvalid("custom(order=\"\", order=\"\")",
                "Semantic error at line 1, column 18: The \"custom\" orderer has a duplicate \"order\" argument.");
        testInvalid("custom(order=1)", "Semantic error at line 1, column 8: The \"custom\" orderer has "
                + "an unsupported value for the \"order\" argument: the value must be a string.");
        testInvalid("custom(order=\"does_not_exist\")", "Semantic error at line 1, column 8: The \"custom\" orderer "
                + "has an unsupported value for the \"order\" argument: can't find a match for \"does_not_exist\". "
                + "There is no supported variable or automaton (with two or more locations) in the specification that "
                + "matches the given name pattern.");

        Specification spec = newSpecification();
        InputVariable va = newInputVariable(null, "a", null, newIntType(0, null, 1));
        InputVariable vb = newInputVariable(null, "b", null, newIntType(0, null, 1));
        InputVariable vc = newInputVariable(null, "c", null, newIntType(0, null, 1));
        spec.getDeclarations().add(va);
        spec.getDeclarations().add(vb);
        spec.getDeclarations().add(vc);
        SynthesisVariable a = new SynthesisInputVariable(va, newIntType(0, null, 1), 2, 0, 1);
        SynthesisVariable b = new SynthesisInputVariable(vb, newIntType(0, null, 1), 2, 0, 1);
        SynthesisVariable c = new SynthesisInputVariable(vc, newIntType(0, null, 1), 2, 0, 1);
        List<SynthesisVariable> vars = list(a, b, c);

        testInvalid("custom(order=\"a,a,b,c\")", vars, "Semantic error at line 1, column 8: The \"custom\" orderer "
                + "has an unsupported value for the \"order\" argument: \"a\" is included more than once.");
        testInvalid("custom(order=\"a,b\")", vars, "Semantic error at line 1, column 8: The \"custom\" orderer has an "
                + "unsupported value for the \"order\" argument: the following are missing from the specified order: "
                + "\"c\".");

        testInvalid("custom(a=1)",
                "Semantic error at line 1, column 8: The \"custom\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testUnknownOrderer() {
        testInvalid("unknown", "Semantic error at line 1, column 1: Unknown variable orderer \"unknown\".");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testEnumValid() {
        testValid("sloan(relations=linearized)", "sloan(relations=linearized, effect=var-order)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testEnumsInvalid() {
        testInvalid("dcsh(node-finder=1)", "Semantic error at line 1, column 6: The \"dcsh\" orderer has "
                + "an unsupported value for the \"node-finder\" argument: the value must be a node finder algorithm.");
        testInvalid("dcsh(node-finder=x)", "Semantic error at line 1, column 6: The \"dcsh\" orderer has "
                + "an unsupported value for the \"node-finder\" argument: the value must be a node finder algorithm.");
        testInvalid("dcsh(node-finder=x())", "Semantic error at line 1, column 6: The \"dcsh\" orderer has "
                + "an unsupported value for the \"node-finder\" argument: the value must be a node finder algorithm.");
        testInvalid("dcsh(node-finder=x(a=1))", "Semantic error at line 1, column 6: "
                + "The \"dcsh\" orderer has an unsupported value for the \"node-finder\" argument: the value must be a "
                + "node finder algorithm.");
        testInvalid("dcsh(node-finder=(model -> dcsh))", "Semantic error at line 1, column 6: "
                + "The \"dcsh\" orderer has an unsupported value for the \"node-finder\" argument: the value must be a "
                + "node finder algorithm.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testDcshValid() {
        testValid("dcsh", "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)");
        testValid("dcsh()", "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)");

        testValid("dcsh(node-finder=george-liu)",
                "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)");
        testValid("dcsh(node-finder=sloan)", "dcsh(node-finder=sloan, metric=wes, relations=legacy, effect=var-order)");

        testValid("dcsh(metric=total-span)",
                "dcsh(node-finder=george-liu, metric=total-span, relations=legacy, effect=var-order)");
        testValid("dcsh(metric=wes)", "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)");

        testValid("dcsh(relations=legacy)",
                "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)");
        testValid("dcsh(relations=linearized)",
                "dcsh(node-finder=george-liu, metric=wes, relations=linearized, effect=var-order)");
        testValid("dcsh(relations=configured)",
                "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)");

        testValid("dcsh(effect=var-order)",
                "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)");
        testValid("dcsh(effect=representations)",
                "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=representations)");
        testValid("dcsh(effect=both)", "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=both)");

        testValid("dcsh(node-finder=sloan, metric=total-span, relations=linearized, effect=both)",
                "dcsh(node-finder=sloan, metric=total-span, relations=linearized, effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testDcshInvalid() {
        testInvalid("dcsh(node-finder=george-liu, node-finder=sloan)",
                "Semantic error at line 1, column 30: The \"dcsh\" orderer has a duplicate \"node-finder\" argument.");
        testInvalid("dcsh(node-finder=1)",
                "Semantic error at line 1, column 6: "
                        + "The \"dcsh\" orderer has an unsupported value for the \"node-finder\" argument: "
                        + "the value must be a node finder algorithm.");

        testInvalid("dcsh(metric=total-span, metric=wes)",
                "Semantic error at line 1, column 25: The \"dcsh\" orderer has a duplicate \"metric\" argument.");
        testInvalid("dcsh(metric=1)",
                "Semantic error at line 1, column 6: "
                        + "The \"dcsh\" orderer has an unsupported value for the \"metric\" argument: "
                        + "the value must be a metric.");

        testInvalid("dcsh(relations=legacy, relations=linearized)",
                "Semantic error at line 1, column 24: The \"dcsh\" orderer has a duplicate \"relations\" argument.");
        testInvalid("dcsh(relations=1)",
                "Semantic error at line 1, column 6: "
                        + "The \"dcsh\" orderer has an unsupported value for the \"relations\" argument: "
                        + "the value must be a kind of relations.");

        testInvalid("dcsh(effect=1)",
                "Semantic error at line 1, column 6: "
                        + "The \"dcsh\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("dcsh(effect=both, effect=both)",
                "Semantic error at line 1, column 19: The \"dcsh\" orderer has a duplicate \"effect\" argument.");

        testInvalid("dcsh(a=1)",
                "Semantic error at line 1, column 6: The \"dcsh\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testForceValid() {
        testValid("force", "force(metric=total-span, relations=linearized, effect=var-order)");
        testValid("force()", "force(metric=total-span, relations=linearized, effect=var-order)");

        testValid("force(metric=total-span)", "force(metric=total-span, relations=linearized, effect=var-order)");
        testValid("force(metric=wes)", "force(metric=wes, relations=linearized, effect=var-order)");

        testValid("force(relations=legacy)", "force(metric=total-span, relations=legacy, effect=var-order)");
        testValid("force(relations=linearized)", "force(metric=total-span, relations=linearized, effect=var-order)");
        testValid("force(relations=configured)", "force(metric=total-span, relations=linearized, effect=var-order)");

        testValid("force(effect=var-order)", "force(metric=total-span, relations=linearized, effect=var-order)");
        testValid("force(effect=representations)",
                "force(metric=total-span, relations=linearized, effect=representations)");
        testValid("force(effect=both)", "force(metric=total-span, relations=linearized, effect=both)");

        testValid("force(metric=wes, relations=legacy, effect=both)",
                "force(metric=wes, relations=legacy, effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testForceInvalid() {
        testInvalid("force(metric=total-span, metric=wes)",
                "Semantic error at line 1, column 26: The \"force\" orderer has a duplicate \"metric\" argument.");
        testInvalid("force(metric=1)",
                "Semantic error at line 1, column 7: "
                        + "The \"force\" orderer has an unsupported value for the \"metric\" argument: "
                        + "the value must be a metric.");

        testInvalid("force(relations=legacy, relations=linearized)",
                "Semantic error at line 1, column 25: The \"force\" orderer has a duplicate \"relations\" argument.");
        testInvalid("force(relations=1)",
                "Semantic error at line 1, column 7: "
                        + "The \"force\" orderer has an unsupported value for the \"relations\" argument: "
                        + "the value must be a kind of relations.");

        testInvalid("force(effect=1)",
                "Semantic error at line 1, column 7: "
                        + "The \"force\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("force(effect=both, effect=both)",
                "Semantic error at line 1, column 20: The \"force\" orderer has a duplicate \"effect\" argument.");

        testInvalid("force(a=1)",
                "Semantic error at line 1, column 7: The \"force\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSlidWinValid() {
        testValid("slidwin", "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
        testValid("slidwin()", "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");

        testValid("slidwin(size=1)", "slidwin(size=1, metric=total-span, relations=linearized, effect=var-order)");
        testValid("slidwin(size=12)", "slidwin(size=12, metric=total-span, relations=linearized, effect=var-order)");

        testValid("slidwin(metric=total-span)",
                "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
        testValid("slidwin(metric=wes)", "slidwin(size=4, metric=wes, relations=linearized, effect=var-order)");

        testValid("slidwin(relations=legacy)",
                "slidwin(size=4, metric=total-span, relations=legacy, effect=var-order)");
        testValid("slidwin(relations=linearized)",
                "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
        testValid("slidwin(relations=configured)",
                "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");

        testValid("slidwin(effect=var-order)",
                "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
        testValid("slidwin(effect=representations)",
                "slidwin(size=4, metric=total-span, relations=linearized, effect=representations)");
        testValid("slidwin(effect=both)", "slidwin(size=4, metric=total-span, relations=linearized, effect=both)");

        testValid("slidwin(size=5, metric=wes, relations=legacy, effect=both)",
                "slidwin(size=5, metric=wes, relations=legacy, effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSlidWinInvalid() {
        testInvalid("slidwin(size=-1)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"size\" argument: "
                        + "the value must be in the range [1..12].");
        testInvalid("slidwin(size=0)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"size\" argument: "
                        + "the value must be in the range [1..12].");
        testInvalid("slidwin(size=13)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"size\" argument: "
                        + "the value must be in the range [1..12].");
        testInvalid("slidwin(size=x)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"size\" argument: "
                        + "the value must be a number.");
        testInvalid("slidwin(size=2147483648)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"size\" argument: "
                        + "the value is out of range.");
        testInvalid("slidwin(size=a())",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"size\" argument: "
                        + "the value must be a number.");
        testInvalid("slidwin(size=1, size=2)",
                "Semantic error at line 1, column 17: The \"slidwin\" orderer has a duplicate \"size\" argument.");

        testInvalid("slidwin(metric=total-span, metric=wes)",
                "Semantic error at line 1, column 28: The \"slidwin\" orderer has a duplicate \"metric\" argument.");
        testInvalid("slidwin(metric=1)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"metric\" argument: "
                        + "the value must be a metric.");

        testInvalid("slidwin(relations=legacy, relations=linearized)",
                "Semantic error at line 1, column 27: The \"slidwin\" orderer has a duplicate \"relations\" argument.");
        testInvalid("slidwin(relations=1)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"relations\" argument: "
                        + "the value must be a kind of relations.");

        testInvalid("slidwin(effect=1)",
                "Semantic error at line 1, column 9: "
                        + "The \"slidwin\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("slidwin(effect=both, effect=both)",
                "Semantic error at line 1, column 22: The \"slidwin\" orderer has a duplicate \"effect\" argument.");

        testInvalid("slidwin(a=1)",
                "Semantic error at line 1, column 9: The \"slidwin\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSloanValid() {
        testValid("sloan", "sloan(relations=legacy, effect=var-order)");
        testValid("sloan()", "sloan(relations=legacy, effect=var-order)");

        testValid("sloan(relations=legacy)", "sloan(relations=legacy, effect=var-order)");
        testValid("sloan(relations=linearized)", "sloan(relations=linearized, effect=var-order)");
        testValid("sloan(relations=configured)", "sloan(relations=legacy, effect=var-order)");

        testValid("sloan(effect=var-order)", "sloan(relations=legacy, effect=var-order)");
        testValid("sloan(effect=representations)", "sloan(relations=legacy, effect=representations)");
        testValid("sloan(effect=both)", "sloan(relations=legacy, effect=both)");

        testValid("sloan(relations=linearized, effect=both)", "sloan(relations=linearized, effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testSloanInvalid() {
        testInvalid("sloan(relations=legacy, relations=linearized)",
                "Semantic error at line 1, column 25: The \"sloan\" orderer has a duplicate \"relations\" argument.");
        testInvalid("sloan(relations=1)",
                "Semantic error at line 1, column 7: "
                        + "The \"sloan\" orderer has an unsupported value for the \"relations\" argument: "
                        + "the value must be a kind of relations.");

        testInvalid("sloan(effect=1)",
                "Semantic error at line 1, column 7: "
                        + "The \"sloan\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("sloan(effect=both, effect=both)",
                "Semantic error at line 1, column 20: The \"sloan\" orderer has a duplicate \"effect\" argument.");

        testInvalid("sloan(a=1)",
                "Semantic error at line 1, column 7: The \"sloan\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testWeightedCmValid() {
        testValid("weighted-cm", "weighted-cm(node-finder=george-liu, relations=legacy, effect=var-order)");
        testValid("weighted-cm()", "weighted-cm(node-finder=george-liu, relations=legacy, effect=var-order)");

        testValid("weighted-cm(node-finder=george-liu)",
                "weighted-cm(node-finder=george-liu, relations=legacy, effect=var-order)");
        testValid("weighted-cm(node-finder=sloan)",
                "weighted-cm(node-finder=sloan, relations=legacy, effect=var-order)");

        testValid("weighted-cm(relations=legacy)",
                "weighted-cm(node-finder=george-liu, relations=legacy, effect=var-order)");
        testValid("weighted-cm(relations=linearized)",
                "weighted-cm(node-finder=george-liu, relations=linearized, effect=var-order)");
        testValid("weighted-cm(relations=configured)",
                "weighted-cm(node-finder=george-liu, relations=legacy, effect=var-order)");

        testValid("weighted-cm(effect=var-order)",
                "weighted-cm(node-finder=george-liu, relations=legacy, effect=var-order)");
        testValid("weighted-cm(effect=representations)",
                "weighted-cm(node-finder=george-liu, relations=legacy, effect=representations)");
        testValid("weighted-cm(effect=both)", "weighted-cm(node-finder=george-liu, relations=legacy, effect=both)");

        testValid("weighted-cm(node-finder=sloan, relations=linearized, effect=both)",
                "weighted-cm(node-finder=sloan, relations=linearized, effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testWeightedCmInvalid() {
        testInvalid("weighted-cm(node-finder=george-liu, node-finder=sloan)", "Semantic error at line 1, column 37: "
                + "The \"weighted-cm\" orderer has a duplicate \"node-finder\" argument.");
        testInvalid("weighted-cm(node-finder=1)",
                "Semantic error at line 1, column 13: "
                        + "The \"weighted-cm\" orderer has an unsupported value for the \"node-finder\" argument: "
                        + "the value must be a node finder algorithm.");

        testInvalid("weighted-cm(relations=legacy, relations=linearized)", "Semantic error at line 1, column 31: "
                + "The \"weighted-cm\" orderer has a duplicate \"relations\" argument.");
        testInvalid("weighted-cm(relations=1)",
                "Semantic error at line 1, column 13: "
                        + "The \"weighted-cm\" orderer has an unsupported value for the \"relations\" argument: "
                        + "the value must be a kind of relations.");

        testInvalid("weighted-cm(effect=1)",
                "Semantic error at line 1, column 13: "
                        + "The \"weighted-cm\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("weighted-cm(effect=both, effect=both)", "Semantic error at line 1, column 26: "
                + "The \"weighted-cm\" orderer has a duplicate \"effect\" argument.");

        testInvalid("weighted-cm(a=1)", "Semantic error at line 1, column 13: "
                + "The \"weighted-cm\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testChoiceValid() {
        testValid("or(choices=[force, dcsh])",
                "or(metric=wes, relations=legacy, effect=var-order, "
                        + "choices=[force(metric=total-span, relations=linearized, effect=var-order), "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)])");
        testValid("or(choices=[(force -> dcsh), (dcsh -> force)])",
                "or(metric=wes, relations=legacy, effect=var-order, "
                        + "choices=[force(metric=total-span, relations=linearized, effect=var-order) -> "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order), "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order) -> "
                        + "force(metric=total-span, relations=linearized, effect=var-order)])");

        testValid("or(choices=[sloan, sloan], metric=total-span)",
                "or(metric=total-span, relations=legacy, effect=var-order, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");
        testValid("or(choices=[sloan, sloan], metric=wes)",
                "or(metric=wes, relations=legacy, effect=var-order, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");

        testValid("or(choices=[sloan, sloan], relations=legacy)",
                "or(metric=wes, relations=legacy, effect=var-order, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");
        testValid("or(choices=[sloan, sloan], relations=linearized)",
                "or(metric=wes, relations=linearized, effect=var-order, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");
        testValid("or(choices=[sloan, sloan], relations=configured)",
                "or(metric=wes, relations=legacy, effect=var-order, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");

        testValid("or(choices=[sloan, sloan], effect=var-order)",
                "or(metric=wes, relations=legacy, effect=var-order, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");
        testValid("or(choices=[sloan, sloan], effect=representations)",
                "or(metric=wes, relations=legacy, effect=representations, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");
        testValid("or(choices=[sloan, sloan], effect=both)",
                "or(metric=wes, relations=legacy, effect=both, "
                        + "choices=[sloan(relations=legacy, effect=var-order), "
                        + "sloan(relations=legacy, effect=var-order)])");

        testValid("or(choices=[force, dcsh], metric=total-span, relations=linearized, effect=representations)",
                "or(metric=total-span, relations=linearized, effect=representations, "
                        + "choices=[force(metric=total-span, relations=linearized, effect=var-order), "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order)])");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testChoiceInvalid() {
        testInvalid("or", "Semantic error at line 1, column 1: "
                + "The \"or\" orderer is missing its mandatory \"choices\" argument.");
        testInvalid("or()", "Semantic error at line 1, column 1: "
                + "The \"or\" orderer is missing its mandatory \"choices\" argument.");

        testInvalid("or(choices=[force])",
                "Semantic error at line 1, column 4: "
                        + "The \"or\" orderer has an unsupported value for the \"choices\" argument: "
                        + "the value must be a list with at least two variable orderers.");
        testInvalid("or(choices=1)",
                "Semantic error at line 1, column 4: "
                        + "The \"or\" orderer has an unsupported value for the \"choices\" argument: "
                        + "the value must be a list of variable orderers.");
        testInvalid("or(choices=[force, dcsh], choices=[force, dcsh])",
                "Semantic error at line 1, column 27: The \"or\" orderer has a duplicate \"choices\" argument.");

        testInvalid("or(metric=total-span, metric=wes)",
                "Semantic error at line 1, column 23: The \"or\" orderer has a duplicate \"metric\" argument.");
        testInvalid("or(metric=1)",
                "Semantic error at line 1, column 4: "
                        + "The \"or\" orderer has an unsupported value for the \"metric\" argument: "
                        + "the value must be a metric.");

        testInvalid("or(relations=legacy, relations=linearized)",
                "Semantic error at line 1, column 22: The \"or\" orderer has a duplicate \"relations\" argument.");
        testInvalid("or(relations=1)",
                "Semantic error at line 1, column 4: "
                        + "The \"or\" orderer has an unsupported value for the \"relations\" argument: "
                        + "the value must be a kind of relations.");

        testInvalid("or(effect=1)",
                "Semantic error at line 1, column 4: "
                        + "The \"or\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("or(effect=both, effect=both)",
                "Semantic error at line 1, column 17: " + "The \"or\" orderer has a duplicate \"effect\" argument.");

        testInvalid("or(a=1)",
                "Semantic error at line 1, column 4: The \"or\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testReverseValid() {
        testValid("reverse", "reverse(relations=legacy, effect=var-order)");
        testValid("reverse()", "reverse(relations=legacy, effect=var-order)");

        testValid("reverse(relations=legacy)", "reverse(relations=legacy, effect=var-order)");
        testValid("reverse(relations=linearized)", "reverse(relations=linearized, effect=var-order)");
        testValid("reverse(relations=configured)", "reverse(relations=legacy, effect=var-order)");

        testValid("reverse(effect=var-order)", "reverse(relations=legacy, effect=var-order)");
        testValid("reverse(effect=representations)", "reverse(relations=legacy, effect=representations)");
        testValid("reverse(effect=both)", "reverse(relations=legacy, effect=both)");

        testValid("reverse(relations=linearized, effect=both)", "reverse(relations=linearized, effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testReverseInvalid() {
        testInvalid("reverse(relations=legacy, relations=linearized)",
                "Semantic error at line 1, column 27: The \"reverse\" orderer has a duplicate \"relations\" argument.");
        testInvalid("reverse(relations=1)",
                "Semantic error at line 1, column 9: "
                        + "The \"reverse\" orderer has an unsupported value for the \"relations\" argument: "
                        + "the value must be a kind of relations.");

        testInvalid("reverse(effect=1)",
                "Semantic error at line 1, column 9: "
                        + "The \"reverse\" orderer has an unsupported value for the \"effect\" argument: "
                        + "the value must be a variable orderer effect.");
        testInvalid("reverse(effect=both, effect=both)", "Semantic error at line 1, column 22: "
                + "The \"reverse\" orderer has a duplicate \"effect\" argument.");

        testInvalid("reverse(a=1)",
                "Semantic error at line 1, column 9: The \"reverse\" orderer does not support the \"a\" argument.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testExtraComma() {
        testValid("random(seed=5,)", "random(seed=5, effect=both)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testParentheses() {
        testValid("sorted -> dcsh -> force -> slidwin",
                "sorted(effect=both) -> "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order) -> "
                        + "force(metric=total-span, relations=linearized, effect=var-order) -> "
                        + "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
        testValid("(sorted -> dcsh -> force -> slidwin)",
                "sorted(effect=both) -> "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order) -> "
                        + "force(metric=total-span, relations=linearized, effect=var-order) -> "
                        + "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
        testValid("sorted -> (dcsh -> force) -> slidwin",
                "sorted(effect=both) -> "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order) -> "
                        + "force(metric=total-span, relations=linearized, effect=var-order) -> "
                        + "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
        testValid("(sorted) -> (dcsh) -> (force) -> (slidwin)",
                "sorted(effect=both) -> "
                        + "dcsh(node-finder=george-liu, metric=wes, relations=legacy, effect=var-order) -> "
                        + "force(metric=total-span, relations=linearized, effect=var-order) -> "
                        + "slidwin(size=4, metric=total-span, relations=linearized, effect=var-order)");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testMixBasicAdvancedOptionsInitialOrder() {
        Options.set(BddVariableOrderOption.class, "random");
        Options.set(BddAdvancedVariableOrderOption.class, "random");
        testInvalid("random", "The BDD variable ordering is configured through basic and advanced options, "
                + "which is not supported. Use only basic or only advanced options.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testMixBasicAdvancedOptionsDcsh() {
        Options.set(BddDcshVarOrderOption.class, false);
        Options.set(BddAdvancedVariableOrderOption.class, "random");
        testInvalid("random", "The BDD variable ordering is configured through basic and advanced options, "
                + "which is not supported. Use only basic or only advanced options.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testMixBasicAdvancedOptionsForce() {
        Options.set(BddForceVarOrderOption.class, false);
        Options.set(BddAdvancedVariableOrderOption.class, "random");
        testInvalid("random", "The BDD variable ordering is configured through basic and advanced options, "
                + "which is not supported. Use only basic or only advanced options.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testMixBasicAdvancedOptionsSlidWin() {
        Options.set(BddSlidingWindowVarOrderOption.class, false);
        Options.set(BddAdvancedVariableOrderOption.class, "random");
        testInvalid("random", "The BDD variable ordering is configured through basic and advanced options, "
                + "which is not supported. Use only basic or only advanced options.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testMixBasicAdvancedOptionsSlidWinSize() {
        Options.set(BddSlidingWindowSizeOption.class, 2);
        Options.set(BddAdvancedVariableOrderOption.class, "random");
        testInvalid("random", "The BDD variable ordering is configured through basic and advanced options, "
                + "which is not supported. Use only basic or only advanced options.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testMixBasicAdvancedOptionsRelations() {
        Options.set(BddHyperEdgeAlgoOption.class, BddHyperEdgeAlgo.LINEARIZED);
        Options.set(BddAdvancedVariableOrderOption.class, "random");
        testInvalid("random", "The BDD variable ordering is configured through basic and advanced options, "
                + "which is not supported. Use only basic or only advanced options.");
    }

    @Test
    @SuppressWarnings("javadoc")
    public void testMixBasicAdvancedOptionsMultiple() {
        Options.set(BddSlidingWindowSizeOption.class, 2);
        Options.set(BddHyperEdgeAlgoOption.class, BddHyperEdgeAlgo.LINEARIZED);
        Options.set(BddAdvancedVariableOrderOption.class, "random");
        testInvalid("random", "The BDD variable ordering is configured through basic and advanced options, "
                + "which is not supported. Use only basic or only advanced options.");
    }

    /**
     * Test a valid orderer, without variables given to the type checker.
     *
     * @param ordererTxt The orderer to test.
     * @param expectedOrderer The expected textual representation of the orderer.
     */
    private void testValid(String ordererTxt, String expectedOrderer) {
        testValid(ordererTxt, Collections.emptyList(), expectedOrderer);
    }

    /**
     * Test a valid orderer.
     *
     * @param ordererTxt The orderer to test.
     * @param variables The synthesis variables.
     * @param expectedOrderer The expected textual representation of the orderer.
     */
    private void testValid(String ordererTxt, List<SynthesisVariable> variables, String expectedOrderer) {
        // Parse.
        VarOrdererParser parser = new VarOrdererParser();
        List<VarOrdererInstance> parseResult = parser.parseString(ordererTxt, "/dummy", null, DebugMode.NONE);

        // Type check.
        VarOrdererTypeChecker tchecker = new VarOrdererTypeChecker(variables);
        VarOrderer orderer = tchecker.typeCheck(parseResult);
        assertFalse(tchecker.hasWarning(), "Type check warnings found.");
        if (tchecker.hasError()) {
            assertEquals(tchecker.getProblems().size(), 1, "Expected one type check problem.");
            assertTrue(false, tchecker.getProblems().get(0).toString());
        }
        assertTrue(orderer != null, "Type checker produced no result.");
        assertEquals(expectedOrderer, orderer.toString());
    }

    /**
     * Test an invalid orderer, without variables given to the type checker.
     *
     * @param ordererTxt The orderer to test.
     * @param expectedMsg The error message.
     */
    private void testInvalid(String ordererTxt, String expectedMsg) {
        testInvalid(ordererTxt, Collections.emptyList(), expectedMsg);
    }

    /**
     * Test an invalid orderer.
     *
     * @param ordererTxt The orderer to test.
     * @param variables The synthesis variables.
     * @param expectedMsg The error message.
     */
    private void testInvalid(String ordererTxt, List<SynthesisVariable> variables, String expectedMsg) {
        // Parse.
        VarOrdererParser parser = new VarOrdererParser();
        List<VarOrdererInstance> parseResult;
        try {
            parseResult = parser.parseString(ordererTxt, "/dummy", null, DebugMode.NONE);
        } catch (SyntaxException e) {
            String actualMsg = e.getMessage();
            assertEquals(expectedMsg, actualMsg);
            return;
        }

        // Type check.
        try {
            VarOrdererTypeChecker tchecker = new VarOrdererTypeChecker(variables);
            VarOrderer orderer = tchecker.typeCheck(parseResult);
            assertEquals(orderer, null, "Type checker produced result.");
            assertFalse(tchecker.hasWarning(), "Type check warnings found.");
            assertTrue(tchecker.hasError(), "Type check no error found.");
            assertEquals(tchecker.getProblems().size(), 1, "Expected one type check problem.");
            assertEquals(expectedMsg, tchecker.getProblems().get(0).toString());
        } catch (InvalidOptionException e) {
            assertEquals(expectedMsg, e.getMessage());
        }
    }
}
