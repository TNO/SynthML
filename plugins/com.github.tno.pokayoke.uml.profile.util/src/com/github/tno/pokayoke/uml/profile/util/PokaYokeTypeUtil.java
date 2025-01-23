
package com.github.tno.pokayoke.uml.profile.util;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.lsat.common.util.IterableUtil;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * Type supported types for CIF annotated UML models, currently supporting:
 * <ul>
 * <li>Boolean</li>
 * <li>Integer</li>
 * <li>Enumeration</li>
 * </ul>
 */
public class PokaYokeTypeUtil {
    private PokaYokeTypeUtil() {
        // Empty for utility classes
    }

    /**
     * Returns all Poka Yoke supported types for {@code context}.
     * <p>
     * The next types are supported: {@link Enumeration enumerations in the model}, {@link UmlPrimitiveType#INTEGER
     * integers in the model}, {@link UmlPrimitiveType#BOOLEAN the primitive Boolean} and {@link DataType data types in
     * the model}.
     * </p>
     *
     * @param context The context for which the supported types are queried.
     * @return All Poka Yoke supported types for {@code context}.
     * @see #isSupportedType(Type)
     */
    public static List<Type> getSupportedTypes(Element context) {
        QueryableIterable<Type> supportedTypes = QueryableIterable.from(context.getModel().getOwnedTypes())
                .select(PokaYokeTypeUtil::isSupportedType).union(UmlPrimitiveType.BOOLEAN.load(context));

        return IterableUtil.sortedBy(supportedTypes, Type::getName);
    }

    public static boolean isSupportedType(Type type) {
        return isEnumerationType(type) || isBooleanType(type) || isIntegerType(type) || isCompositeDataType(type);
    }

    public static boolean isCompositeDataType(Type type) {
        // Check the type is only a data type; DataType is the super interface of Enumeration and PrimitiveType.
        return type instanceof DataType && !(isEnumerationType(type) || isPrimitiveType(type));
    }

    public static boolean isEnumerationType(Type type) {
        return type instanceof Enumeration;
    }

    public static boolean isPrimitiveType(Type type) {
        return type instanceof PrimitiveType;
    }

    public static boolean isBooleanType(Type type) {
        PrimitiveType primitiveBool = UmlPrimitiveType.BOOLEAN.load(type);
        return type != null && type.equals(primitiveBool);
    }

    public static boolean isIntegerType(Type type) {
        PrimitiveType primitiveInt = UmlPrimitiveType.INTEGER.load(type);
        return type != null && !type.equals(primitiveInt) && type.conformsTo(primitiveInt);
    }

    public static Integer getMinValue(Type type) {
        if (type instanceof PrimitiveType primitiveType) {
            Constraint constraint = getMinConstraint(primitiveType, false);
            if (constraint != null && constraint.getSpecification() instanceof LiteralInteger literalInteger) {
                return literalInteger.getValue();
            }
        }
        return null;
    }

    public static void setMinValue(PrimitiveType type, Integer newValue) {
        if (newValue == null) {
            Constraint constraint = getMinConstraint(type, false);
            if (constraint != null) {
                EcoreUtil.delete(constraint, true);
            }
            return;
        }
        LiteralInteger specification = UMLFactory.eINSTANCE.createLiteralInteger();
        specification.setValue(newValue);
        getMinConstraint(type, true).setSpecification(specification);
    }

    public static Constraint getMinConstraint(PrimitiveType primitiveType, boolean createOnDemand) {
        return primitiveType.getOwnedRule("min", false, UMLPackage.Literals.CONSTRAINT, createOnDemand);
    }

    public static Integer getMaxValue(Type type) {
        if (type instanceof PrimitiveType primitiveType) {
            Constraint constraint = getMaxConstraint(primitiveType, false);
            if (constraint != null && constraint.getSpecification() instanceof LiteralInteger literalInteger) {
                return literalInteger.getValue();
            }
        }
        return null;
    }

    public static void setMaxValue(PrimitiveType type, Integer newValue) {
        if (newValue == null) {
            Constraint constraint = getMaxConstraint(type, false);
            if (constraint != null) {
                EcoreUtil.delete(constraint, true);
            }
            return;
        }
        LiteralInteger specification = UMLFactory.eINSTANCE.createLiteralInteger();
        specification.setValue(newValue);
        getMaxConstraint(type, true).setSpecification(specification);
    }

    public static Constraint getMaxConstraint(PrimitiveType primitiveType, boolean createOnDemand) {
        return primitiveType.getOwnedRule("max", false, UMLPackage.Literals.CONSTRAINT, createOnDemand);
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

    /**
     * Collect all property names from the current property to the leaf property.
     *
     * @param parentProperty The parent UML property.
     * @param partialName The string containing the names unfolded so far.
     * @param childrenProperties The set containing all the unfolded properties of the parent, which will be modified
     *     in-place.
     */
    public static void collectPropertyNamesUntilLeaf(Property parentProperty, String partialName,
            Set<String> childrenProperties)
    {
        // If parent property is a composite data type, add its name to the string, and perform a recursive call on its
        // children until a leaf is reached.
        if (!PokaYokeTypeUtil.isCompositeDataType(parentProperty.getType())) {
            return;
        }
        for (Property property: ((DataType)parentProperty.getType()).getOwnedAttributes()) {
            String updatedPartialName = partialName + "." + property.getName();
            if (PokaYokeTypeUtil.isCompositeDataType(property.getType())) {
                collectPropertyNamesUntilLeaf(property, updatedPartialName, childrenProperties);
            } else {
                childrenProperties.add(updatedPartialName);
            }
        }
    }
}
