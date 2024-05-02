
package com.github.tno.pokayoke.transform.tests.common;

import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility for comparing directories based on file contents.
 */
public class FileCompare {
    private FileCompare() {
    }

    /**
     * Check equality of two directories, after applying a filter. Files are matched based on paths.
     *
     * @param expectedPath Path to directory containing expected files.
     * @param actualPath Path to directory containing actual output files.
     * @param message The message to be used when the assertion fails.
     * @throws IOException when loading one of the files to compare fails.
     */
    public static void checkDirectoriesEqual(Path expectedPath, Path actualPath, String message) throws IOException {
        assertTrue(Files.isDirectory(expectedPath),
                "Path expectedPath " + expectedPath.toString() + " does not refer to a folder.");
        assertTrue(Files.isDirectory(actualPath),
                "Path actualPath " + actualPath.toString() + " does not refer to a folder.");

        List<Path> expectedPaths = Files.walk(expectedPath).filter(Files::isRegularFile).toList();
        List<Path> actualPaths = Files.walk(actualPath).filter(Files::isRegularFile).toList();

        checkFileListsEqual(expectedPaths, actualPaths, message);
    }

    /**
     * Check whether two given lists of files are equal content-wise. Files are matched based on paths.
     *
     * @param expectedPaths List of paths of expected output files.
     * @param actualPaths List of paths of actual output files.
     * @param message The message to be used when the assertion fails.
     * @throws IOException when loading one of the files to compare fails.
     */
    private static void checkFileListsEqual(List<Path> expectedPaths, List<Path> actualPaths, String message)
            throws IOException
    {
        List<String> expectedFileNames = expectedPaths.stream().map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

        List<String> actualFileNames = actualPaths.stream().map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

        Collections.sort(expectedFileNames);
        Collections.sort(actualFileNames);

        // Compare if the expected and actual file names are the same.
        assertLinesMatch(expectedFileNames, actualFileNames);

        // Compare if the content of the files are the same.
        for (Path actualItemPath: actualPaths) {
            String actualFileName = actualItemPath.getFileName().toString();
            Path expectedFilePath = actualItemPath.getParent().resolve(actualFileName);
            PathAssertions.assertContentsMatch(expectedFilePath, actualItemPath, message);
        }
    }
}
