
package com.github.tno.pokayoke.transform.cif2petrify;

import java.nio.file.Path;

import org.eclipse.escet.cif.io.CifReader;
import org.eclipse.escet.cif.metamodel.cif.Specification;

public class CIFFileHelper {
    /**
     * Read CIF {@link Specification} from given {@link Path}.
     *
     * @param path {@link Path} of input file.
     * @return {@link Specification} read from input file.
     */
    public static Specification loadCIFSpec(Path path) {
        CifReader reader = new CifReader();
        reader.suppressWarnings = true;
        reader.init(path.toString(), path.toAbsolutePath().toString(), false);
        return reader.read();
    }
}
