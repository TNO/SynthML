////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.tests.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Common assertions for path variables. Includes checking the existence of files and directories, and comparing their
 * contents.
 */
public class PathAssertions {
    private PathAssertions() {
    }

    /**
     * Assert that the path exists.
     *
     * @param path The path.
     * @param message The message to be used when the assertion fails.
     */
    public static void assertPathExists(Path path, String message) {
        assertTrue(Files.exists(path), message + ": path '" + path.toString() + "' does not exist.");
    }

    /**
     * Assert that the path refers to an existing directory.
     *
     * @param directory The path of the directory.
     * @param message The message to be used when the assertion fails.
     */
    public static void assertDirectoryExists(Path directory, String message) {
        assertPathExists(directory, message);
        assertTrue(Files.isDirectory(directory),
                message + ": path '" + directory.toString() + "' does not refer to a directory.");
    }

    /**
     * Assert that the path refers to an existing file.
     *
     * @param file The path of the file.
     * @param message The message to be used when the assertion fails.
     */
    public static void assertFileExists(Path file, String message) {
        assertPathExists(file, message);
        assertFalse(Files.isDirectory(file), message + ": path '" + file.toString() + "' does not refer to a file.");
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
