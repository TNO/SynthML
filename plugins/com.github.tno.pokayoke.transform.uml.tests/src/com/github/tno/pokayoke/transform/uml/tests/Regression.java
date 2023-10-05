
package com.github.tno.pokayoke.transform.uml.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.uml.UMLTransformer;

/**
 * Regression tests.
 */
class Regression {
    /**
     * The regression tests consists out of test discovery functionality and a general test case. Furthermore, a test
     * extension is used to enable the comparison of the UML files/resources.
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

    /**
     * The test extension of the UML files/resources on disk.
     */
    private static final String TESTEXTENSION = "umltst";

    /**
     * The filename of the input of the regression test.
     */
    private static final String INPUT_FILENAME = "input.uml";

    /**
     * The filename of the expected output of the regression test.
     */
    private static final String EXPECTED_FILENAME = "expected." + TESTEXTENSION;

    /**
     * The filename of the actual output of the regression test.
     */
    private static final String OUTPUT_FILENAME = "output." + TESTEXTENSION;

    @BeforeAll
    public static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(TESTEXTENSION, new TestFactory());
    }

    private static Stream<Arguments> provideArgumentsForRegressionTests() {
        final String testdataName = "testdata";
        final Path testdataPath = Path.of(testdataName);
        assertTrue(Files.isDirectory(testdataPath), "The 'testdata' directory doesn't exist.");

        final String regressiontestsName = "regressiontests";
        final Path regressiontestsPath = testdataPath.resolve(regressiontestsName);
        assertTrue(Files.isDirectory(regressiontestsPath),
                "regressiontests directory doesn't exist within testdata directory");

        final List<Arguments> returnValue = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(regressiontestsPath,
                Files::isDirectory))
        {
            for (Path subDirectory: directoryStream) {
                returnValue.add(Arguments.of(subDirectory));
            }
        } catch (IOException e) {
            fail("IOException while adding subdirectories of the regressiontests directory" + e.toString());
        }
        return returnValue.stream();
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForRegressionTests")
    void regressionTests(Path dirPath) throws IOException {
        // Set up - includes checking of preconditions.
        final String dirLongName = dirPath.toString();

        final Path inputPath = dirPath.resolve(INPUT_FILENAME);
        assertTrue(Files.exists(inputPath),
                "Input file '" + INPUT_FILENAME + "' unexpectedly absent in " + dirLongName + ".");
        final String inputLongName = inputPath.toString();

        final Path expectedPath = dirPath.resolve(EXPECTED_FILENAME);
        assertTrue(Files.exists(expectedPath),
                "Expected file '" + EXPECTED_FILENAME + "' unexpectedly absent in " + dirLongName + ".");

        final Path outputPath = dirPath.resolve(OUTPUT_FILENAME);
        final String outputLongName = outputPath.toString();

        // Act.
        final Model model = FileHelper.loadModel(inputLongName);
        new UMLTransformer(model, inputLongName).transformModel();
        FileHelper.storeModel(model, outputLongName);

        // Verify.
        FileCompare.assertContentsMatch(expectedPath, outputPath, dirLongName);

        // Tear down - only executed when the test is successful.
        Files.delete(outputPath);
    }
}
