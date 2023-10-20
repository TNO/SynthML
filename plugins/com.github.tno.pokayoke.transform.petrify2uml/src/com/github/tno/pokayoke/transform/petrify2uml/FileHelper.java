
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import fr.lip6.move.pnml.ptnet.PetriNet;

public class FileHelper {
    private FileHelper() {
    }

    public static List<String> readFile(String sourcePath) throws IOException {
        return Files.readAllLines(Paths.get(sourcePath));
    }

    public static void writePetriNet(PetriNet petriNet, String outputPath) throws IOException {
        try (FileOutputStream output = new FileOutputStream(outputPath); FileChannel channel = output.getChannel()) {
            petriNet.toPNML(channel);
        }
    }
}
