
package com.github.tno.pokayoke.transform.petrify2uml.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.petrify2uml.Petrify2PNMLTranslator;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

/**
 * Regression tests.
 */
class Petrify2PNMLRegression extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "out";

    public static final String OUTPUT_FILE_EXTENSION = "pnml";

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        return RegressionTest.provideArguments(INPUT_FILE_EXTENSION, OUTPUT_FILE_EXTENSION);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws IOException {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void testAct(Path inputPath, Path outputPath) throws IOException {
        Petrify2PNMLTranslator.transformFile(inputPath.toString(), outputPath.toString());
    }
}
