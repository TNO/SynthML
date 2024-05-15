
package com.github.tno.pokayoke.uml.profile.util;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.resource.UMLResource;

import com.github.tno.pokayoke.transform.common.FileHelper;

public enum UmlPrimitiveType {
    BOOLEAN("Boolean"), INTEGER("Integer"), STRING("String"), REAL("Real"), UNLIMITED_NATURAL("UnlimitedNatural");

    private static final URI UML_PRIMITIVE_TYPES_LIBRARY_URI = URI
            .createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI);

    private final String umlName;

    private UmlPrimitiveType(String umlName) {
        this.umlName = umlName;
    }

    public String getUmlName() {
        return umlName;
    }

    public PrimitiveType load() {
        return load(FileHelper.createModelResourceSet());
    }

    /**
     * Load the primitive type using the resource set of the {@code context}, such that the type can be used for
     * comparison.
     *
     * @param context The context to load the primitive, should be contained by a {@link ResourceSet}.
     * @return The loaded primitive type.
     */
    public PrimitiveType load(EObject context) {
        Resource resource = context.eResource();
        if (resource == null) {
            throw new IllegalArgumentException("Expected element to be contained by a resource set.");
        }
        ResourceSet resourceSet = resource.getResourceSet();
        if (resourceSet == null) {
            throw new IllegalArgumentException("Expected element to be contained by a resource set.");
        }
        return load(resourceSet);
    }

    public PrimitiveType load(ResourceSet resourceSet) {
        return (PrimitiveType)resourceSet.getEObject(UML_PRIMITIVE_TYPES_LIBRARY_URI.appendFragment(umlName), true);
    }
}
