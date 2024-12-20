
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.google.common.collect.Sets;

/** Collects basic typing information from a model that can be queried. */
public class CifContext {
    /**
     * All elements are {@link EClass#isSuperTypeOf(EClass) derived} from
     * {@link org.eclipse.uml2.uml.UMLPackage.Literals#NAMED_ELEMENT}.
     */
    private static final Set<EClass> CONTEXT_TYPES = Sets.newHashSet(UMLPackage.Literals.CLASS,
            UMLPackage.Literals.ENUMERATION, UMLPackage.Literals.ENUMERATION_LITERAL,
            UMLPackage.Literals.PRIMITIVE_TYPE, UMLPackage.Literals.PROPERTY, UMLPackage.Literals.OPAQUE_BEHAVIOR,
            UMLPackage.Literals.CONSTRAINT, UMLPackage.Literals.ACTIVITY, UMLPackage.Literals.DATA_TYPE);

    static {
        for (EClass contextType: CONTEXT_TYPES) {
            if (!UMLPackage.Literals.NAMED_ELEMENT.isSuperTypeOf(contextType)) {
                throw new IllegalArgumentException("Invalid context type: " + contextType.getName());
            }
        }
    }

    /**
     * Finds all contextual elements in the {@code model}.
     *
     * @param model The search root.
     * @return All contextual elements in the {@code model}.
     */
    public static QueryableIterable<NamedElement> queryContextElements(Model model) {
        return QueryableIterable.from(model.eAllContents()).union(model).select(e -> CONTEXT_TYPES.contains(e.eClass()))
                .asType(NamedElement.class);
    }

    /**
     * Finds all contextual elements in the {@code model} whose names should be globally unique.
     *
     * @param model The search root.
     * @return All found contextual elements.
     */
    public static QueryableIterable<NamedElement> queryUniqueNameElements(Model model) {
        return queryContextElements(model).select(element -> !(element instanceof Activity)
                && !(element instanceof Constraint constraint && isPrimitiveTypeConstraint(constraint)));
    }

    private final Map<String, NamedElement> contextElements;

    public CifContext(Element element) {
        Model model = element.getModel();
        // Do not check duplicates here, as that is the responsibility of model validation
        Map<String, NamedElement> namesAndElement = queryContextElements(model).toMap(NamedElement::getName);
        Map<String, NamedElement> newNamesAndElements = new LinkedHashMap<>();

        // Loop over all elements, find the leaf of the dependency tree, and add it to the new map.
        for (Entry<String, NamedElement> entry: namesAndElement.entrySet()) {
            String elementName = entry.getKey();
            NamedElement elementObject = entry.getValue();

            // If the element is a property and of type DataType, recursive call on its children.
            if (elementObject instanceof Property property && PokaYokeTypeUtil.isDataTypeOnlyType(property.getType())) {
                getChildPropertyName((DataType)property.getType(), elementName, newNamesAndElements);
            } else {
                NamedElement childElement = entry.getValue();
                newNamesAndElements.put(elementName, childElement);
            }
        }

        contextElements = newNamesAndElements;
    }

    private static void getChildPropertyName(DataType datatype, String name,
            Map<String, NamedElement> namesAndElements)
    {
        // Loop over all data type's attributes. If they are not a data type, add them to the map;
        // otherwise, recursively call on the children object. Note that this assumes that only Enum, integers and
        // booleans (i.e. the basic suppported types) can be a leaf within a property.
        for (Property umlProperty: datatype.getOwnedAttributes()) {
            String newName = name + "." + umlProperty.getName();
            NamedElement elementObject = umlProperty;

            if (PokaYokeTypeUtil.isDataTypeOnlyType(umlProperty.getType())) {
                // Recursive call.
                getChildPropertyName((DataType)umlProperty.getType(), newName, namesAndElements);
            } else {
                namesAndElements.put(newName, elementObject);
            }
        }
    }

    public boolean isDeclared(String name) {
        return contextElements.containsKey(name);
    }

    protected NamedElement getElement(String name) {
        return contextElements.get(name);
    }

    protected Collection<NamedElement> getAllElements() {
        return Collections.unmodifiableCollection(contextElements.values());
    }

    public List<Class> getAllClasses(Predicate<Class> predicate) {
        return getAllElements().stream().filter(e -> e instanceof Class c && predicate.test(c)).map(Class.class::cast)
                .toList();
    }

    public List<Activity> getAllActivities() {
        return getAllElements().stream().filter(Activity.class::isInstance).map(Activity.class::cast).toList();
    }

    public List<Activity> getAllAbstractActivities() {
        return getAllElements().stream().filter(e -> e instanceof Activity a && a.isAbstract())
                .map(Activity.class::cast).toList();
    }

    public List<DataType> getAllDataTypes(Predicate<DataType> predicate) {
        return getAllElements().stream().filter(e -> e instanceof DataType d && predicate.test(d))
                .map(DataType.class::cast).toList();
    }

    public boolean isEnumeration(String name) {
        return contextElements.get(name) instanceof Enumeration;
    }

    public Enumeration getEnumeration(String name) {
        if (contextElements.get(name) instanceof Enumeration enumeration) {
            return enumeration;
        }
        return null;
    }

    public List<Enumeration> getAllEnumerations() {
        return getAllElements().stream().filter(Enumeration.class::isInstance).map(Enumeration.class::cast).toList();
    }

    public boolean isEnumerationLiteral(String name) {
        return contextElements.get(name) instanceof EnumerationLiteral;
    }

    public EnumerationLiteral getEnumerationLiteral(String name) {
        if (contextElements.get(name) instanceof EnumerationLiteral literal) {
            return literal;
        }
        return null;
    }

    public List<EnumerationLiteral> getAllEnumerationLiterals() {
        return getAllElements().stream().filter(EnumerationLiteral.class::isInstance)
                .map(EnumerationLiteral.class::cast).toList();
    }

    public boolean isVariable(String name) {
        return contextElements.get(name) instanceof Property;
    }

    public Property getVariable(String name) {
        if (contextElements.get(name) instanceof Property property) {
            return property;
        }
        return null;
    }

    /**
     * Checks whether the context contains the given element.
     *
     * @param element The input element.
     * @return {@code true} if the given element is in this context, {@code false} otherwise.
     */
    public boolean hasElement(Element element) {
        return getAllElements().contains(element);
    }

    public boolean hasOpaqueBehaviors() {
        return getAllElements().stream().anyMatch(OpaqueBehavior.class::isInstance);
    }

    public OpaqueBehavior getOpaqueBehavior(String name) {
        if (contextElements.get(name) instanceof OpaqueBehavior behavior) {
            return behavior;
        }
        return null;
    }

    public boolean hasConstraints(Predicate<Constraint> predicate) {
        return getAllElements().stream().anyMatch(e -> e instanceof Constraint c && predicate.test(c));
    }

    public static boolean isActivityPrePostconditionConstraint(Constraint constraint) {
        return constraint.getContext() instanceof Activity a
                && (a.getPreconditions().contains(constraint) || a.getPostconditions().contains(constraint));
    }

    public static boolean isClassConstraint(Constraint constraint) {
        return constraint.getContext() instanceof Class clazz && !(clazz instanceof Behavior);
    }

    public static boolean isOccurrenceConstraint(Constraint constraint) {
        return constraint.getContext() instanceof Activity && constraint instanceof IntervalConstraint;
    }

    public static boolean isPrimitiveTypeConstraint(Constraint constraint) {
        return constraint.getContext() instanceof PrimitiveType;
    }

    public boolean hasAbstractActivities() {
        return getAllElements().stream().anyMatch(e -> e instanceof Activity a && a.isAbstract());
    }
}
