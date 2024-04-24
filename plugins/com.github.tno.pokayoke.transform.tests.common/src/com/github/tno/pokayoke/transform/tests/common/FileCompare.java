
package com.github.tno.pokayoke.transform.tests.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
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
     * @param filter Predicate used to filter files.
     * @throws IOException when loading one of the files to compare fails.
     */
    public static void checkDirectoriesEqual(Path expectedPath, Path actualPath, Predicate<Path> filter)
            throws IOException
    {
        assertTrue(Files.isDirectory(expectedPath),
                "Path expectedPath " + expectedPath.toString() + " does not refer to a folder.");
        assertTrue(Files.isDirectory(actualPath),
                "Path actualPath " + actualPath.toString() + " does not refer to a folder.");

        List<Path> actualItemPaths = Files.walk(actualPath).filter(filter::test).toList();
        List<Path> expectedItemPaths = Files.walk(expectedPath).filter(filter::test).toList();

        checkFileListsEqual(expectedPath, expectedItemPaths, actualPath, actualItemPaths);
    }

    /**
     * Check whether two given lists of files are equal content-wise. Files are matched based on paths.
     *
     * @param expectedPath Path to root directory containing expected files.
     * @param expectedItemPaths List of paths of expected files.
     * @param actualPath Path to root directory containing actual output files.
     * @param actualItemPaths List of paths of output files.
     * @throws IOException when loading one of the files to compare fails.
     */
    public static void checkFileListsEqual(Path expectedPath, List<Path> expectedItemPaths, Path actualPath,
            List<Path> actualItemPaths) throws IOException
    {
        List<String> actualItemStrings = actualItemPaths.stream().map(p -> actualPath.relativize(p).toString())
                .collect(Collectors.toList());
        List<String> expectedItemStrings = expectedItemPaths.stream().map(p -> expectedPath.relativize(p).toString())
                .collect(Collectors.toList());

        Collections.sort(actualItemStrings);
        Collections.sort(expectedItemStrings);

        assertLinesMatch(expectedItemStrings, actualItemStrings);

        for (String actualItemString: actualItemStrings) {
            checkFilesEqual(actualPath.resolve(actualItemString), expectedPath.resolve(actualItemString));
        }
    }

    /**
     * Check equality of two paths, where directories are always considered equal and files are considered equal if
     * their textual contents are equal.
     *
     * @param actualPath Path to first file to compare.
     * @param expectedPath Path to second file to compare.
     * @throws IOException when loading one of the files to compare fails.
     */
    public static void checkFilesEqual(Path actualPath, Path expectedPath) throws IOException {
        assertEquals(Files.isDirectory(actualPath), Files.isDirectory(expectedPath),
                actualPath.toString() + " and " + expectedPath.toString() + " cannot be compared.");
        if (!Files.isDirectory(actualPath)) {
            assertEquals(expectedPath.getFileName(), actualPath.getFileName(),
                    expectedPath.toString() + " and " + actualPath.toString() + " do not have the same file name");
            List<String> actualContents = Files.readAllLines(actualPath);
            List<String> expectedContents = Files.readAllLines(expectedPath);

            assertLinesMatch(expectedContents, actualContents,
                    actualPath.toString() + " does not match " + expectedPath.toString());
        }
    }
}
