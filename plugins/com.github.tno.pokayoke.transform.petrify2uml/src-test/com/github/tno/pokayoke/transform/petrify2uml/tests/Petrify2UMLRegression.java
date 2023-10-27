
package com.github.tno.pokayoke.transform.petrify2uml.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.petrify2uml.Petrify2PNMLTranslator;
import com.github.tno.pokayoke.transform.tests.common.Regression;

/**
 * Regression tests.
 */
class Petrify2UMLRegression extends Regression {
    public static final String INPUTFILEEXTENSION = "out";

    public static final String OUTPUTFILEEXTENSION = "uml";

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        return Regression.provideArguments(INPUTFILEEXTENSION, OUTPUTFILEEXTENSION);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws IOException {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException {
        Petrify2PNMLTranslator.transformFile(inputPath.toString(), outputPath.toString());
    }
}
