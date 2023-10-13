
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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

/**
 * Arguments provider for regression tests. The arguments are discovered in the sub-directory named 'regressiontests'
 * within the test resources directory.
 */
public abstract class RegressionArgumentsProvider implements ArgumentsProvider {
    /**
     * The extension of the input file for the regression test.
     *
     * @return The extension of the input file for the regression test.
     */
    protected abstract String getInputFileExtension();

    /**
     * The file name of the input file for the regression test.
     *
     * @return The file name of the input file for the regression test.
     */
    public String getInputFileName() {
        return "input." + getInputFileExtension();
    }

    /**
     * The extension of the actual and expected output file for the regression test.
     *
     * @return The extension of the actual and expected output file for the regression test.
     */
    protected abstract String getOutputFileExtension();

    /**
     * The file name of the expected output file for the regression test.
     *
     * @return The file name of the expected output file for the regression test.
     */
    public String getExpectedOutputFileName() {
        return "expected." + getOutputFileExtension();
    }

    /**
     * The file name of the actual output file for the regression test.
     *
     * @return The input file name of the regression test.
     */
    public String getActualOutputFileName() {
        return "output." + getOutputFileExtension();
    }

    private boolean isRegressionTestDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }

        final Path inputPath = path.resolve(getInputFileName());
        final Path expectedPath = path.resolve(getExpectedOutputFileName());
        return Files.exists(inputPath) && Files.exists(expectedPath);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        final String testDataName = "testData";
        final Path testResourcesPath = Path.of(testDataName);
        assertTrue(Files.isDirectory(testResourcesPath), "The '" + testDataName + "' directory doesn't exist.");

        final String regressiontestsName = "regressiontests";
        final Path regressiontestsPath = testResourcesPath.resolve(regressiontestsName);
        assertTrue(Files.isDirectory(regressiontestsPath), "The '" + regressiontestsName
                + "' directory doesn't exist within the '" + testDataName + "' directory.");
        final String regressiontestsPathString = regressiontestsPath.toString();

        final List<Arguments> returnValue = new ArrayList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(regressiontestsPath,
                p -> isRegressionTestDirectory(p)))
        {
            for (Path subDirectory: directoryStream) {
                returnValue.add(Arguments.of(subDirectory));
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
