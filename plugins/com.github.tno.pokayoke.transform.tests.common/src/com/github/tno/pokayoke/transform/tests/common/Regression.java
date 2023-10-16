
package com.github.tno.pokayoke.transform.tests.common;

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
 */
public abstract class Regression {
    /**
     * The regression test.
     *
     * <p>
     * When verification within this test fails, this test will stop and, by design, tear down will NOT be executed.
     * Since tear down is not executed, the actual output file will remain available on disk. This is beneficial since
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

    private static boolean isRegressionTestDirectory(Path path, String inputFile) {
        if (!Files.isDirectory(path)) {
            return false;
        }

        final Path inputPath = path.resolve(inputFile);
        return Files.exists(inputPath);
    }

    public static Stream<? extends Arguments> provideArguments(String inputExtension, String outputExtension)
            throws Exception
    {
        final String inputFile = "input." + inputExtension;
        final String expectedFile = "expected." + outputExtension;
        final String outputFile = "output." + outputExtension;

        final String testResourcesName = "resources-test";
        final Path testResourcesPath = Path.of(testResourcesName);
        assertTrue(Files.isDirectory(testResourcesPath), "The '" + testResourcesName + "' directory doesn't exist.");

        final String regressiontestsName = "regressiontests";
        final Path regressiontestsPath = testResourcesPath.resolve(regressiontestsName);
        assertTrue(Files.isDirectory(regressiontestsPath), "The '" + regressiontestsName
                + "' directory doesn't exist within the '" + testResourcesName + "' directory.");
        final String regressiontestsPathString = regressiontestsPath.toString();

        final List<Arguments> returnValue = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(regressiontestsPath,
                p -> isRegressionTestDirectory(p, inputFile)))
        {
            for (Path subDirectory: directoryStream) {
                returnValue.add(Arguments.of(subDirectory.resolve(inputFile), subDirectory.resolve(expectedFile),
                        subDirectory.resolve(outputFile), subDirectory.toString()));
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
