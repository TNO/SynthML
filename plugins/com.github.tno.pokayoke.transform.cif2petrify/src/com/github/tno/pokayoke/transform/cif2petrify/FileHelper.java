
package com.github.tno.pokayoke.transform.cif2petrify;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.metamodel.cif.Specification;

public class FileHelper {
    private FileHelper() {
    }

    public static Specification loadCIFSpec(String sourcePath) {
        Path path = Paths.get(sourcePath);
        CifReader reader = new CifReader();
        reader.suppressWarnings = true;
        reader.init(path.toString(), path.toAbsolutePath().toString(), false);
        return reader.read();
    }

    public static void storePetrifySpec(String output, String targetPath) throws IOException {
        FileWriter file = new FileWriter(targetPath);
        BufferedWriter buffer = new BufferedWriter(file);
        buffer.write(output);
        buffer.flush();
        buffer.close();
    }
}
