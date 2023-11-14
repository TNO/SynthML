
package com.github.tno.pokayoke.transform.cif2petrify.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.cif2petrify.Cif2Petrify;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

/**
 * Regression tests.
 */
class CIF2PetrifyRegression extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "cif";

    public static final String OUTPUT_FILE_EXTENSION = "g";

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
    protected void actTest(Path inputPath, Path outputPath) throws IOException {
        Cif2Petrify.transformFile(inputPath.toString(), outputPath.toString());
    }
}
