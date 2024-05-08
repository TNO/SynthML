
package com.github.tno.pokayoke.transform.common;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

public class FileHelper {
    public static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private FileHelper() {
    }

    /**
     * Loads a UML model.
     *
     * @param pathName The path name of the UML model.
     * @return The loaded model.
     */
    public static Model loadModel(String pathName) {
        // Initialize a UML resource set to load the UML model.
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);

        // Load the UML model.
        URI fileURI = URI.createFileURI(pathName);
        Resource res = resourceSet.getResource(fileURI, true);
        Model umlModel = (Model)res.getContents().get(0);

        return umlModel;
    }

    /**
     * Loads a UML package.
     *
     * @param uri The URI of the UML model.
     * @return The loaded package.
     */
    public static Package loadPackage(URI uri) {
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource resource = resourceSet.getResource(uri, true);
        return (Package)EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.PACKAGE);
    }

    /**
     * Loads a primitive UML type, e.g., "Boolean" or "String".
     *
     * @param name Then name of the primitive type to load.
     * @return The loaded primitive type.
     */
    public static PrimitiveType loadPrimitiveType(String name) {
        Package umlLibrary = loadPackage(URI.createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI));
        return (PrimitiveType)umlLibrary.getOwnedType(name);
    }

    /**
     * Stores the given model to the specified path.
     *
     * @param model The model to store.
     * @param pathName The path to store the input model.
     * @throws IOException Thrown in case the model could not be saved.
     */
    public static void storeModel(Model model, String pathName) throws IOException {
        // Initialize a UML resource set to store the model.
        ResourceSet resourceSet = new ResourceSetImpl();
        UMLResourcesUtil.init(resourceSet);

        // Store the model.
        URI uri = URI.createFileURI(pathName);
        Resource resource = resourceSet.createResource(uri);
        resource.getContents().add(model);
        EMFHelper.normalizeXmiIds((XMLResource)resource);

        // Also add the UML profiles information to the resource
        List<EObject> stereotypeApplications = model.allOwnedElements().stream()
                .flatMap(e -> e.getStereotypeApplications().stream()).collect(Collectors.toList());
        resource.getContents().addAll(stereotypeApplications);
        resource.save(Collections.EMPTY_MAP);
    }
}
