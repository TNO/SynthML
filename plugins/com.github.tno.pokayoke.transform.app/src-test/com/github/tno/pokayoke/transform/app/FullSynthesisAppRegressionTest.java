
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

/**
 * Regression tests.
 */
class FullSynthesisAppRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "uml";

    public static final String REGRESSIONTESTS_NAME = "regressiontests";

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
    protected void actTest(Path inputPath, Path outputPath) throws IOException, CoreException {
        FullSynthesisApp.performFullSynthesis(inputPath, outputPath, new ArrayList<>());
    }
}
