
package com.github.tno.pokayoke.transform.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.CoreException;

import com.github.tno.pokayoke.transform.uml.UMLTransformer;

public class Uml2CameoTranslationApp {
    private Uml2CameoTranslationApp() {
    }

    public static void translateUml2Cameo(Path inputPath, Path outputFolderPath) throws IOException, CoreException {
        Files.createDirectories(outputFolderPath);

        // Determine the path of the output file.
        String filePrefix = FilenameUtils.removeExtension(inputPath.getFileName().toString());
        Path outputFilePath = outputFolderPath.resolve(filePrefix + ".uml");

        // Translate the UML model at the input path, and write the resulting fUML specification to the output path.
        UMLTransformer.transformFile(inputPath.toString(), outputFilePath.toString());
    }
}
