
package com.github.tno.pokayoke.transform.petrify2uml.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.petrify2uml.PetriNet2Activity;
import com.github.tno.pokayoke.transform.tests.common.Regression;

/** Regression test for the translation from Petrify output to Activity. */
class PetriNet2ActivityRegression extends Regression {
    public static final String INPUT_FILE_EXTENSION = "out";

    public static final String OUTPUT_FILE_EXTENSION = "uml";

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        return Regression.provideArguments(INPUT_FILE_EXTENSION, OUTPUT_FILE_EXTENSION);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws IOException {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException {
        PetriNet2Activity.transformFile(inputPath.toString(), outputPath.toString());
    }
}
