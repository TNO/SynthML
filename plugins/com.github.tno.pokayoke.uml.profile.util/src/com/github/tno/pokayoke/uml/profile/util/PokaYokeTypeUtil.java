
package com.github.tno.pokayoke.uml.profile.util;

import java.util.List;
import java.util.Objects;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import com.google.common.base.Preconditions;

/**
 * Type supported types for CIF annotated UML models, currently supporting:
 * <ul>
 * <li>Boolean</li>
 * <li>Integer</li>
 * <li>Enumeration</li>
 * </ul>
 */
public class PokaYokeTypeUtil {
    private static final URI UML_PRIMITIVE_TYPES_LIBRARY_URI = URI
            .createURI(UMLResource.UML_PRIMITIVE_TYPES_LIBRARY_URI);

    public static final String PRIMITIVE_TYPE_BOOLEAN = "Boolean";

    public static final String PRIMITIVE_TYPE_INTEGER = "Integer";

    public static final String PRIMITIVE_TYPE_STRING = "String";

    private PokaYokeTypeUtil() {
        // Empty for utility classes
    }

    /**
     * Returns all Poka Yoke supported types for {@code context}.
     * <p>
     * The next types are supported: {@link Enumeration enumerations in the model},
     * {@link #loadPrimitiveType(String, Element) primitive Boolean} and {@link #loadPrimitiveType(String, Element)
     * primitive Integer}.
     * </p>
     *
     * @param context The context for which the supported types are queried.
     * @return All Poka Yoke supported types for {@code context}.
     * @see #isSupportedType(Type)
     */
    public static List<Type> getSupportedTypes(Element context) {
        return QueryableIterable.from(context.getModel().getOwnedTypes()).objectsOfKind(Enumeration.class)
                .asType(Type.class).union(loadPrimitiveType(PRIMITIVE_TYPE_BOOLEAN, context),
                        loadPrimitiveType(PRIMITIVE_TYPE_INTEGER, context))
                .asList();
    }

    public static boolean isSupportedType(Type type) {
        return isEnumerationType(type) || isBooleanType(type) || isIntegerType(type);
    }

    public static boolean isEnumerationType(Type type) {
        return type instanceof Enumeration;
    }

    public static boolean isBooleanType(Type type) {
        return PRIMITIVE_TYPE_BOOLEAN.equals(type.getName()) && isUmlPrimitiveType(type);
    }

    public static boolean isIntegerType(Type type) {
        return PRIMITIVE_TYPE_INTEGER.equals(type.getName()) && isUmlPrimitiveType(type);
    }

    private static boolean isUmlPrimitiveType(Type type) {
        Resource resource = type.eResource();
        return resource != null && Objects.equals(UML_PRIMITIVE_TYPES_LIBRARY_URI, resource.getURI());
    }

    /**
     * Loads a {@link UMLResource#UML_PRIMITIVE_TYPES_LIBRARY_URI primitive UML type}, e.g., "Boolean" or "String".
     *
     * @param name The name of the primitive type to load.
     * @param context The context to load the primitive types, such that they can be used for comparison
     * @return The loaded primitive type.
     */
    public static PrimitiveType loadPrimitiveType(String name, Element context) {
        // Use the resource set of the model to load the primitive types, such that they can be used for comparison
        Resource resource = context.eResource();
        ResourceSet resourceSet = resource == null ? null : resource.getResourceSet();
        Preconditions.checkNotNull(resourceSet, "Expected element to be contained by a resource set.");
        resource = resourceSet.getResource(UML_PRIMITIVE_TYPES_LIBRARY_URI, true);
        Package primitivesPackage = (Package)EcoreUtil.getObjectByType(resource.getContents(),
                UMLPackage.Literals.PACKAGE);
        return (PrimitiveType)primitivesPackage.getOwnedType(name);
    }

    /**
     * Returns the {@link Type#getLabel(boolean) label} of {@code type}.
     *
     * @param type The type.
     * @return The label of the type or 'null' if type is {@code null}.
     */
    public static String getLabel(Type type) {
        return type == null ? "null" : type.getLabel(true);
    }
}
