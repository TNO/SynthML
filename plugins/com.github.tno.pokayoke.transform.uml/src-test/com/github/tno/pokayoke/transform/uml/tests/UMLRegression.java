
package com.github.tno.pokayoke.transform.uml.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.tests.common.RegressionTest;
import com.github.tno.pokayoke.transform.uml.UMLTransformer;

/**
 * Regression tests.
 */
class UMLRegression extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "uml";

    public static final String OUTPUT_FILE_EXTENSION = "umltst";

    @BeforeAll
    public static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(OUTPUT_FILE_EXTENSION, new UMLTestFactory());
    }

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
        final String inputPathString = inputPath.toString();
        final Model model = FileHelper.loadModel(inputPathString);
        new UMLTransformer(model, inputPathString).transformModel();
        FileHelper.storeModel(model, outputPath.toString());
    }
}
