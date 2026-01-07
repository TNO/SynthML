////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

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
     * Check equality of two directories. Files are matched based on paths.
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

        checkFileListsEqual(expectedPath, expectedPaths, actualPath, actualPaths, message);
    }

    /**
     * Check whether two given lists of files are equal content-wise. Files are matched based on paths.
     *
     * @param expectedFolderPath Path to root directory containing expected files.
     * @param expectedItemPaths List of paths of expected output files.
     * @param actualFolderPath Path to root directory containing actual output files.
     * @param actualItemPaths List of paths of actual output files.
     * @param message The message to be used when the assertion fails.
     * @throws IOException when loading one of the files to compare fails.
     */
    private static void checkFileListsEqual(Path expectedFolderPath, List<Path> expectedItemPaths,
            Path actualFolderPath, List<Path> actualItemPaths, String message) throws IOException
    {
        List<String> expectedItemStrings = expectedItemPaths.stream()
                .map(p -> expectedFolderPath.relativize(p).toString()).collect(Collectors.toList());
        List<String> actualItemStrings = actualItemPaths.stream().map(p -> actualFolderPath.relativize(p).toString())
                .collect(Collectors.toList());

        Collections.sort(expectedItemStrings);
        Collections.sort(actualItemStrings);

        assertLinesMatch(expectedItemStrings, actualItemStrings);

        for (String actualItemString: actualItemStrings) {
            Path expectedFilePath = expectedFolderPath.resolve(actualItemString);
            Path actualFilePath = actualFolderPath.resolve(actualItemString);
            PathAssertions.assertContentsMatch(expectedFilePath, actualFilePath, message);
        }
    }
}
