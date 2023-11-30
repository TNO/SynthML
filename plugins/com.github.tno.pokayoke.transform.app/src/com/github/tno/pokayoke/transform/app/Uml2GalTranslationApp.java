
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;

import com.github.tno.pokayoke.transform.uml2gal.Uml2GalTranslationHelper;

/** Application that translates UML models to GAL specifications. */
public class Uml2GalTranslationApp {
    private Uml2GalTranslationApp() {
    }

    public static void translateUml2Gal(Path inputPath, Path outputFolderPath) throws IOException, JSONException {
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
