
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import fr.lip6.move.pnml.ptnet.Page;

public class FileHelper {
    private FileHelper() {
    }

    public static List<String> readFile(String sourcePath) throws IOException {
        return Files.readAllLines(Paths.get(sourcePath));
    }

    public static void writePNMLFile(Page page, String outputPath) throws IOException {
        File file = new File(outputPath);
        try (FileOutputStream output = new FileOutputStream(file)) {
            FileChannel channel = output.getChannel();
            page.getContainerPetriNet().getContainerPetriNetDoc().toPNML(channel);
            channel.close();
        }
    }
}
