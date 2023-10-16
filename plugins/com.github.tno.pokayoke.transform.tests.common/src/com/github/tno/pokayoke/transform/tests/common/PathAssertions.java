
package com.github.tno.pokayoke.transform.tests.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility functions for file comparison.
 */
public class PathAssertions {
    private PathAssertions() {
    }

    /**
     * Assert that the path refers to an existing directory.
     *
     * @param file The path of the file.
     * @param message The message to be used when the assertion fails.
     */
    public static void assertDirectoryExists(Path file, String message) {
        assertTrue(Files.exists(file), message + ": file '" + file.toString() + "' does not exist.");
        assertTrue(Files.isDirectory(file),
                message + ": path '" + file.toString() + "' does not refer to a directory.");
    }

    /**
     * Assert that the path refers to an existing file.
     *
     * @param file The path of the file.
     * @param message The message to be used when the assertion fails.
     */
    public static void assertFileExists(Path file, String message) {
        assertTrue(Files.exists(file), message + ": file '" + file.toString() + "' does not exist.");
        assertFalse(Files.isDirectory(file),
                message + ": path '" + file.toString() + "' does not refer to a file.");
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
        assertFileExists(expectedFile, message + " expected file");
        assertFileExists(actualFile, message + " actual file");

        final List<String> expectedContents = Files.readAllLines(expectedFile);
        final List<String> actualContents = Files.readAllLines(actualFile);

        assertLinesMatch(expectedContents, actualContents,
                message + ": '" + actualFile.toString() + "' does not match '" + expectedFile.toString() + "'");
    }
}
