
package com.github.tno.pokayoke.transform.petrify2uml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import fr.lip6.move.pnml.ptnet.PetriNet;

public class FileHelper {
    private FileHelper() {
    }

    public static List<String> readFile(String sourcePath) throws IOException {
        return new LinkedList<>(Files.readAllLines(Paths.get(sourcePath)));
    }

    public static void writePetriNet(PetriNet petriNet, String outputPath) throws IOException {
        try (FileOutputStream output = new FileOutputStream(outputPath); FileChannel channel = output.getChannel()) {
            petriNet.toPNML(channel);
        }
    }

    public static void storeModel(Model model, String pathName) throws IOException {
        // Initialize a UML resource set to store the model.
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);

        // Store the model.
        URI uri = URI.createFileURI(pathName);
        Resource resource = resourceSet.createResource(uri);
        resource.getContents().add(model);
        resource.save(Collections.EMPTY_MAP);
    }
}
