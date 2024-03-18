
package com.github.tno.pokayoke.transform.uml2cif;

import java.nio.file.Path;

import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.cif.metamodel.cif.Specification;

public class CifFileHelper {
    private CifFileHelper() {
    }

    public static Specification loadCifSpec(Path sourcePath) {
        CifReader reader = new CifReader();
        reader.suppressWarnings = true;
        reader.init(sourcePath.toString(), sourcePath.toAbsolutePath().toString(), false);
        return reader.read();
    }

    public static void writeCifSpec(Specification specification, String targetPath) {
        CifWriter.writeCifSpec(specification, targetPath, targetPath);
    }

    public static void main(String[] args) {
        Specification spec = loadCifSpec(Path.of("assets/System.cif"));
        Specification asd = spec;
    }
}
