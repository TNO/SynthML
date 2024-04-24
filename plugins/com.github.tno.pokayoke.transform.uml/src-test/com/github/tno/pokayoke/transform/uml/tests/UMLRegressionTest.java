
package com.github.tno.pokayoke.transform.uml.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;
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
class UMLRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "uml";

    public static final String OUTPUT_FILE_EXTENSION = "umltst";

    @BeforeAll
    public static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(OUTPUT_FILE_EXTENSION, new UMLTestFactory());
    }

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        return RegressionTest.provideArguments(INPUT_FILE_EXTENSION);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException, CoreException {
        final String inputPathString = inputPath.toString();
        final Model model = FileHelper.loadModel(inputPathString);
        new UMLTransformer(model).transformModel();
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Path umlOutputFilePath = outputPath.resolve(filePrefix + "." + OUTPUT_FILE_EXTENSION);
        FileHelper.storeModel(model, umlOutputFilePath.toString());
    }
}
