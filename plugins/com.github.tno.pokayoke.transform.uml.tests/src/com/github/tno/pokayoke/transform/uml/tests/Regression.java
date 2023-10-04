
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
 * Regression tests. Separate into the test case and the discovery functionality
 */
class Regression {
    private static final String TESTEXTENSION = "umltst";
    private static final String INPUT_NAME = "input.uml";
    private static final String EXPECTED_NAME = "expected." + TESTEXTENSION;
    private static final String OUTPUT_NAME = "output." + TESTEXTENSION;

    @BeforeAll
    public static void setup() {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(TESTEXTENSION, new TestFactory());
    }

    private static Stream<Arguments> provideArgumentsForRegressionTests() {
        final String testdataName = "testdata";
        final Path testdataPath = Path.of(testdataName);
        assertTrue(Files.isDirectory(testdataPath), "testdata directory doesn't exist");

        final String regressiontestsName = "regressiontests";
        final Path regressiontestsPath = testdataPath.resolve(regressiontestsName);
        assertTrue(Files.isDirectory(regressiontestsPath),
                "regressiontests directory doesn't exist within testdata directory");

        List<Arguments> returnValue = new ArrayList<>();
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
        // Set up - includes checking of preconditions
        final String dirLongName = dirPath.toString();

        final Path inputPath = dirPath.resolve(INPUT_NAME);
        assertTrue(Files.exists(inputPath),
                "Input file '" + INPUT_NAME + "' unexpectedly absent in " + dirLongName);
        final String inputLongName = inputPath.toString();

        final Path expectedPath = dirPath.resolve(EXPECTED_NAME);
        assertTrue(Files.exists(expectedPath),
                "Expected file '" + EXPECTED_NAME + "' unexpectedly absent in " + dirLongName);

        final Path outputPath = dirPath.resolve(OUTPUT_NAME);
        final String outputLongName = outputPath.toString();

        // Act
        Model model = FileHelper.loadModel(inputLongName);
        new UMLTransformer(model, inputLongName).transformModel();
        FileHelper.storeModel(model, outputLongName);

        // Verify
        FileCompare.assertContentsMatch(expectedPath, outputPath, dirLongName);

        // Tear down
        Files.delete(outputPath);
    }
}
