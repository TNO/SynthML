
package com.github.tno.pokayoke.transform.cif2petrify;

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
}
