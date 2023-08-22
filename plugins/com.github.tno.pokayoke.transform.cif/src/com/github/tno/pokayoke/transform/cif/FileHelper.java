/**
 *
 */
package com.github.tno.pokayoke.transform.cif;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

/**
 *
 */
public class FileHelper {
    static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private FileHelper() {
    }

    /**
     * Loads an UML model.
     *
     * @param pathName The path name of the UML model.
     * @return The loaded model.
     */
    public static Model loadModel(String pathName) {
        // Initialize an UML resource set to load the UML model.
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);

        // Load the UML model.
        URI fileURI = URI.createFileURI(pathName);
        Resource res = resourceSet.getResource(fileURI, true);
        Model umlModel = (Model)res.getContents().get(0);

        return umlModel;
    }

    /**
     * Stores {@code model} to {@code pathName}.
     *
     * @param model The model to store.
     * @param pathName The path to store {@code model}.
     * @throws IOException Thrown in case the model could not be saved.
     */
    public static void storeModel(Model model, String pathName) throws IOException {
        // Initialize an UML resource set to store the model.
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);


        // Store the model.
        URI uri = URI.createFileURI(pathName);
        Resource resource = resourceSet.createResource(uri);
        resource.getContents().add(model);
        resource.save(Collections.EMPTY_MAP);
    }

}
