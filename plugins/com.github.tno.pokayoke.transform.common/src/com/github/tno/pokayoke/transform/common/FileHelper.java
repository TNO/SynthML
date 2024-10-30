
package com.github.tno.pokayoke.transform.common;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

public class FileHelper {
    public static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private FileHelper() {
        // Empty for utility classes
    }

    /**
     * Loads a UML model.
     *
     * @param pathName The path name of the UML model.
     * @return The loaded model.
     */
    public static Model loadModel(String pathName) {
        // Load the UML model.
        URI fileURI = URI.createFileURI(pathName);
        Resource res = createModelResourceSet().getResource(fileURI, true);
        return (Model)EcoreUtil.getObjectByType(res.getContents(), UMLPackage.Literals.MODEL);
    }

    /**
     * Stores the given model to the specified path.
     *
     * @param model The model to store.
     * @param pathName The path to store the input model.
     * @throws IOException Thrown in case the model could not be saved.
     */
    public static void storeModel(Model model, String pathName) throws IOException {
        storeModel(model, URI.createFileURI(pathName));
    }

    public static void storeModel(Model model, URI uri) throws IOException {
        // Build the resource to store.
        Resource resource = createModelResourceSet().createResource(uri);
        resource.getContents().add(model);

        // Also add the UML profiles information to the resource
        List<EObject> stereotypeApplications = model.allOwnedElements().stream()
                .flatMap(e -> e.getStereotypeApplications().stream()).collect(Collectors.toList());
        resource.getContents().addAll(stereotypeApplications);

        // Store the model.
        EMFHelper.normalizeXmiIds((XMLResource)resource);
        resource.save(Collections.EMPTY_MAP);
    }

    public static ResourceSet createModelResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);
        return resourceSet;
    }

    public static final URI asURI(IFile file) {
        return URI.createPlatformResourceURI(file.getFullPath().toString(), true);
    }
}
