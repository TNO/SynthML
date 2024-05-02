
package com.github.tno.pokayoke.transform.tests.common;

import static com.github.tno.pokayoke.transform.tests.common.PathAssertions.assertDirectoryExists;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
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
 * tear down is not executed, the actual output folder will remain available on disk. This is beneficial since
 * <ul>
 * <li>The actual output folder is useful for diagnosis, such as examining the difference with the expected output
 * folder.</li>
 * <li>The actual output folder is useful for (automatically) changing the expected output folder in case of intended
 * changes.</li>
 * <li>The next test run is not affected since it will just overwrite the actual output folder.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Verification whether the expected output folder exists is performed after the actual output folder is created. This
 * simplifies the creation of regression tests.
 * </p>
 */
public abstract class RegressionTest {
    /**
     * Executes the different steps in the regression test.
     *
     * @param inputPath Path to the input file.
     * @param expectedPath Path to the expected output folder.
     * @param outputPath Path for the actual output folder.
     * @param message Message in case of a failing assertion.
     * @throws Exception Thrown when one of operations fails.
     */
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        setUpTest(inputPath);
        actTest(inputPath, outputPath);
        verifyTest(expectedPath, outputPath, message);
        tearDownTest(outputPath);
    }

    /**
     * Set up test.
     *
     * @param inputPath Path to the input file.
     */
    private void setUpTest(Path inputPath) {
        PathAssertions.assertFileExists(inputPath, "Input for regression test must exist.");
    }

    /**
     * Act test.
     *
     * @param inputPath Path to the input file.
     * @param outputPath Path for the actual output folder.
     * @throws Exception Thrown when one of operations fails.
     */
    protected abstract void actTest(Path inputPath, Path outputPath) throws Exception;

    /**
     * Verify test.
     *
     * @param expectedPath Path to the expected output folder.
     * @param outputPath Path to the actual output folder.
     * @param message Message in case of a failing assertion.
     * @throws IOException Thrown when one of the files can't be read.
     */
    protected void verifyTest(Path expectedPath, Path outputPath, String message) throws IOException {
        FileCompare.checkDirectoriesEqual(expectedPath, outputPath, path -> true, message);
    }

    /**
     * Tear down test.
     *
     * @param outputPath Path to the actual output folder.
     * @throws IOException Thrown when actual output folder can't be deleted.
     */
    protected void tearDownTest(Path outputPath) throws IOException {
        FileUtils.deleteDirectory(new File(outputPath.toString()));
    }

    /**
     * Provide arguments for regression tests based on input extension.
     *
     * @param inputExtension Extension of the input file.
     * @return Stream of arguments for regression tests.
     */
    public Stream<? extends Arguments> provideArguments(String inputExtension) {
        return provideArguments("input." + inputExtension, "expected", "actual");
    }

    /**
     * Provide arguments for regression tests with the given input file name and the expected and actual output folder
     * names.
     *
     * @param inputFile Name of input file.
     * @param expectedFolder Name of expected output folder.
     * @param actualFolder Name of actual output folder.
     * @return Stream of arguments for regression tests.
     */
    private Stream<? extends Arguments> provideArguments(final String inputFile, final String expectedFolder,
            final String actualFolder)
    {
        final String testResourcesName = "resources-test";
        final Path testResourcesPath = Path.of(testResourcesName);
        assertDirectoryExists(testResourcesPath, "The '" + testResourcesName + "' directory doesn't exist.");

        String regressiontestsName = getRegressionTestsName();
        final Path regressiontestsPath = testResourcesPath.resolve(regressiontestsName);
        assertDirectoryExists(regressiontestsPath, "The '" + regressiontestsName
                + "' directory doesn't exist within the '" + testResourcesName + "' directory.");

        return provideArguments(regressiontestsPath, inputFile, expectedFolder, actualFolder);
    }

    protected String getRegressionTestsName() {
        return "regressiontests";
    }

    /**
     * Provide arguments for regression tests in the indicated regression test directory with the given input file name
     * and the expected and actual output folder names.
     *
     * @param regressiontestsPath Directory containing the regression tests.
     * @param inputFile Name of input file.
     * @param expectedFolder Name of expected output folder.
     * @param actualFolder Name of actual output folder.
     * @return Stream of arguments for regression tests.
     */
    private static Stream<? extends Arguments> provideArguments(final Path regressiontestsPath, final String inputFile,
            final String expectedFolder, final String actualFolder)
    {
        final String regressiontestsPathString = regressiontestsPath.toString();

        final List<Arguments> returnValue = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(regressiontestsPath,
                Files::isDirectory))
        {
            for (Path subDirectory: directoryStream) {
                returnValue.add(Arguments.of(subDirectory.resolve(inputFile), subDirectory.resolve(expectedFolder),
                        subDirectory.resolve(actualFolder), subDirectory.toString()));
            }
        } catch (IOException e) {
            throw new RuntimeException("I/O error while adding subdirectories of the regressiontests directory '"
                    + regressiontestsPathString + "'", e);
        }

        assertTrue(!returnValue.isEmpty(), "No regression tests are contained in the regressiontests directory '"
                + regressiontestsPathString + "'.");

        return returnValue.stream();
    }
}
