////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;

import com.github.tno.pokayoke.transform.uml2cameo.UMLToCameoTransformer;

public class Uml2CameoTranslationApp {
    private Uml2CameoTranslationApp() {
    }

    public static void translateUml2Cameo(Path inputPath, Path outputFolderPath) throws IOException, CoreException {
        Files.createDirectories(outputFolderPath);

        // Determine the path of the output file.
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Path outputFilePath = outputFolderPath.resolve(filePrefix + ".uml");

        // Translate the UML model at the input path, and write the resulting fUML specification to the output path.
        UMLToCameoTransformer.transformFile(inputPath, outputFilePath);
    }
}
