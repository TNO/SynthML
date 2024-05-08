
package com.github.tno.pokayoke.transform.petrify2uml.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.petrify2uml.PetrifyOutput2PNMLTranslator;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

/** Regression test for the translation from Petrify output to PNML. */
class PetrifyOutput2PNMLRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "out";

    public static final String REGRESSIONTESTS_NAME = "regressiontests-petrify2pnml";

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        return RegressionTest.provideArguments(INPUT_FILE_EXTENSION, REGRESSIONTESTS_NAME);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException {
        PetrifyOutput2PNMLTranslator.transformFile(inputPath, outputPath);
    }
}
