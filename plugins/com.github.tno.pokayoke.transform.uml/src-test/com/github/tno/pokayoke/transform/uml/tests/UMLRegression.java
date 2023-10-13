
package com.github.tno.pokayoke.transform.uml.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.tests.common.FileCompare;
import com.github.tno.pokayoke.transform.tests.common.RegressionArgumentsProvider;
import com.github.tno.pokayoke.transform.uml.UMLTransformer;

/**
 * Regression tests.
 */
class UMLRegression {
    /**
     * A test extension is used to enable the comparison of the UML files/resources.
     *
     * <p>
     * When verification within a test case fails, that test will stop and, by design, tear down will NOT be executed.
     * Since tear down is not executed, the actual output file will remain available in the corresponding sub-directory
     * on disk. This is beneficial since
     * <ul>
     * <li>The actual output file is useful for diagnosis, such as examining the difference with the expected output
     * file.</li>
     * <li>The actual output file is useful for (automatically) changing the expected output file in case of intended
     * changes.</li>
     * <li>The next test run is not affected since it will just overwrite the actual output file.</li>
     * </ul>
     * </p>
     */

    @BeforeAll
    public static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap()
                .put(UMLRegressionArgumentsProvider.OUTPUTFILEEXTENSION, new UMLTestFactory());
    }

    @ParameterizedTest
    @ArgumentsSource(UMLRegressionArgumentsProvider.class)
    void regressionTests(Path dirPath) throws IOException {
        // Set up.
        final RegressionArgumentsProvider argumentsProvider = new UMLRegressionArgumentsProvider();

        final Path inputPath = dirPath.resolve(argumentsProvider.getInputFileName());
        final Path expectedPath = dirPath.resolve(argumentsProvider.getExpectedOutputFileName());
        final Path outputPath = dirPath.resolve(argumentsProvider.getActualOutputFileName());

        final String inputPathString = inputPath.toString();

        // Act.
        final Model model = FileHelper.loadModel(inputPathString);
        new UMLTransformer(model, inputPathString).transformModel();
        FileHelper.storeModel(model, outputPath.toString());

        // Verify.
        FileCompare.assertContentsMatch(expectedPath, outputPath, dirPath.toString());

        // Tear down, which will only be executed when the test is successful.
        Files.delete(outputPath);
    }
}
