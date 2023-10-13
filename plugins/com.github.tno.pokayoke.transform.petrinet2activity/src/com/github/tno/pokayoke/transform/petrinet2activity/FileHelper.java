
package com.github.tno.pokayoke.transform.petrinet2activity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileHelper {
    private FileHelper() {
    }

    public static List<String> readFile(String sourcePath) throws IOException {
        Files.readAllLines(Paths.get(sourcePath));
        return Files.readAllLines(Paths.get(sourcePath));
    }
}
