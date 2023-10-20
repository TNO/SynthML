
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.metamodel.cif.Specification;

public class FileHelper {
    private FileHelper() {
    }

    public static Specification loadCifSpec(Path sourcePath) {
        CifReader reader = new CifReader();
        reader.suppressWarnings = true;
        reader.init(sourcePath.toString(), sourcePath.toAbsolutePath().toString(), false);
        return reader.read();
    }

    public static void writeToFile(String body, Path targetPath) throws IOException {
        Files.writeString(targetPath, body);
    }
}
