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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.github.tno.pokayoke.transform.uml2gal.Uml2GalTranslationHelper;

/** Application that translates UML models to GAL specifications. */
public class Uml2GalTranslationApp {
    private Uml2GalTranslationApp() {
    }

    public static void translateUml2Gal(Path inputPath, Path outputFolderPath)
            throws IOException, JSONException, CoreException
    {
        Files.createDirectories(outputFolderPath);

        // Determine the paths of the output GAL and JSON files.
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Path outputGalFilePath = outputFolderPath.resolve(filePrefix + ".gal");
        Path outputJsonFilePath = outputFolderPath.resolve(filePrefix + ".json");

        // Translate the UML model at the input path, and write the resulting GAL specification to the output path.
        Uml2GalTranslationHelper.translateCifAnnotatedModel(inputPath.toString(), outputGalFilePath.toString(),
                outputJsonFilePath.toString());
    }
}
