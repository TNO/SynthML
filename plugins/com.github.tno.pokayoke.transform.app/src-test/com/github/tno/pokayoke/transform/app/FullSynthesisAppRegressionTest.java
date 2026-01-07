////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.app;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.escet.common.java.Exceptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.tno.pokayoke.transform.tests.common.RegressionTest;

/**
 * Regression tests.
 */
class FullSynthesisAppRegressionTest extends RegressionTest {
    public static final String INPUT_FILE_EXTENSION = "uml";

    public static final String REGRESSIONTESTS_NAME = "regressiontests";

    public static Stream<? extends Arguments> provideArguments() throws Exception {
        return RegressionTest.provideArguments(INPUT_FILE_EXTENSION, REGRESSIONTESTS_NAME);
    }

    @Override
    @ParameterizedTest
    @MethodSource("provideArguments")
    public void regressionTest(Path inputPath, Path expectedPath, Path outputPath, String message) throws Exception {
        super.regressionTest(inputPath, expectedPath, outputPath, message);
    }

    @Override
    protected void actTest(Path inputPath, Path outputPath) throws IOException, CoreException {
        try {
            FullSynthesisApp.performFullSynthesis(inputPath, outputPath, new ArrayList<>());
        } catch (Throwable e) {
            Path exceptionPath = outputPath.resolve("exception.txt");
            try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(exceptionPath.toFile()));
                 Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8))
            {
                writer.write(Exceptions.exToStr(e));
            }
        }
    }
}
