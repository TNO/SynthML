
package com.github.tno.pokayoke.transform.tests.common;

import static com.github.tno.pokayoke.transform.tests.common.PathAssertions.assertDirectoryExists;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Regression tests.
 *
 * <p>
 * Regression tests are discovered as sub-directories of a particular directory. A test will also fail whenever a
 * sub-directory of that particular directory does not contain a valid regression test.
 * </p>
 *
 * <p>
 * When verification within this test fails, this test will stop and, by design, tear down will NOT be executed. Since
 * tear down is not executed, the actual output file will remain available on disk. This is beneficial since
 * <ul>
 * <li>The actual output file is useful for diagnosis, such as examining the difference with the expected output
 * file.</li>
 * <li>The actual output file is useful for (automatically) changing the expected output file in case of intended
 * changes.</li>
 * <li>The next test run is not affected since it will just overwrite the actual output file.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Verification whether the expected output file exists is performed after the actual output file is created. This
 * simplifies the creation of regression tests.
 * </p>
 *
 */
public abstract class Regression {
    /**
     * Constructor.
     *
     * @param inputPath Path to the input file.
     * @param expectedPath Path to the expected output file.
     * @param outputPath Path for the actual output file.
     * @param message Message in case of a failing assertion.
     * @throws IOException Thrown when one of operations on the files fails.
     */
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws IOException {
        setUpTest();
        actTest(inputPath, outputPath);
        verifyTest(expectedPath, outputPath, message);
        tearDownTest(outputPath);
    }

    /**
     * Set up test.
     */
    private void setUpTest() {
        // nothing to do
    }

    /**
     * Act test.
     *
     * @param inputPath Path to the input file.
     * @param outputPath Path for the actual output file.
     * @throws IOException Thrown when one of operations on the files fails.
     */
    protected abstract void actTest(Path inputPath, Path outputPath) throws IOException;

    /**
     * Verify test.
     *
     * @param expectedPath Path to the expected output file.
     * @param outputPath Path to the actual output file.
     * @param message Message in case of a failing assertion.
     * @throws IOException Thrown when one of the files can't be read.
     */
    private void verifyTest(Path expectedPath, Path outputPath, String message) throws IOException {
        PathAssertions.assertContentsMatch(expectedPath, outputPath, message);
    }

    /**
     * Tear down test.
     *
     * @param outputPath Path to the actual output file.
     * @throws IOException Thrown when actual output file can't be deleted.
     */
    private void tearDownTest(Path outputPath) throws IOException {
        Files.delete(outputPath);
    }

    /**
     * Provide arguments for regression tests based on input and output extension.
     *
     * @param inputExtension Extension of the input file.
     * @param outputExtension Extension of the actual and expected output file.
     * @return Stream of arguments for regression tests.
     */
    public static Stream<? extends Arguments> provideArguments(String inputExtension, String outputExtension) {
        return provideArguments("input." + inputExtension, "expected." + outputExtension, "actual." + outputExtension);
    }

    /**
     * Provide arguments for regression tests with the given input file name and the expected and actual output file
     * names.
     *
     * @param inputFile Name of input file.
     * @param expectedFile Name of expected output file
     * @param actualFile Name of actual output file.
     * @return Stream of arguments for regression tests.
     */
    private static Stream<? extends Arguments> provideArguments(final String inputFile, final String expectedFile,
            final String actualFile)
    {
        final String testResourcesName = "resources-test";
        final Path testResourcesPath = Path.of(testResourcesName);
        assertDirectoryExists(testResourcesPath, "The '" + testResourcesName + "' directory doesn't exist.");

        final String regressiontestsName = "regressiontests";
        final Path regressiontestsPath = testResourcesPath.resolve(regressiontestsName);
        assertDirectoryExists(regressiontestsPath, "The '" + regressiontestsName
                + "' directory doesn't exist within the '" + testResourcesName + "' directory.");

        return provideArguments(regressiontestsPath, inputFile, expectedFile, actualFile);
    }

    /**
     * Provide arguments for regression tests in the directory regressiontestsPath with the given input file name and
     * the expected and actual output file names.
     *
     *
     * @param regressiontestsPath Directory containing the regression tests.
     * @param inputFile Name of input file.
     * @param expectedFile Name of expected output file
     * @param actualFile Name of actual output file.
     * @return Stream of arguments for regression tests.
     */
    private static Stream<? extends Arguments> provideArguments(final Path regressiontestsPath, final String inputFile,
            final String expectedFile, final String actualFile)
    {
        final String regressiontestsPathString = regressiontestsPath.toString();

        final List<Arguments> returnValue = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(regressiontestsPath,
                Files::isDirectory))
        {
            for (Path subDirectory: directoryStream) {
                returnValue.add(Arguments.of(subDirectory.resolve(inputFile), subDirectory.resolve(expectedFile),
                        subDirectory.resolve(actualFile), subDirectory.toString()));
            }
        } catch (IOException e) {
            fail("IOException while adding subdirectories of the regressiontests directory '"
                    + regressiontestsPathString + "': " + e.toString());
        }

        assertTrue(!returnValue.isEmpty(), "No regression tests are contained in the regressiontests directory '"
                + regressiontestsPathString + "'.");

        return returnValue.stream();
    }
}
