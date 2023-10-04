
package com.github.tno.pokayoke.transform.uml.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility functions for file comparison.
 */
public class FileCompare {
    private FileCompare() {
    }

    /**
     * Assert that the contents of the files match.
     *
     * @param expectedFile The path of the expected file.
     * @param actualFile The path of the actual file.
     * @param message The message to be used when the assertion fails.
     * @throws IOException Thrown when one of the files can't be read.
     */
    public static void assertContentsMatch(Path expectedFile, Path actualFile, String message) throws IOException {
        assertFalse(Files.isDirectory(expectedFile),
                message + ": expectedFile " + expectedFile.toString() + " is not a file but a directory");
        assertFalse(Files.isDirectory(actualFile),
                message + ": actualFile " + actualFile.toString() + " is not a file but a directory");

        final List<String> expectedContents = Files.readAllLines(expectedFile);
        final List<String> actualContents = Files.readAllLines(actualFile);

        assertLinesMatch(expectedContents, actualContents,
                message + ": " + actualFile.toString() + " does not match " + expectedFile.toString());
    }
}
