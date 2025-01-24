
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

    // Contains queryContextElements as a set.
    private final Set<NamedElement> declaredElements;

    // All declared elements that are not properties, together with all recursive property instantiations from the
    // active class as a root, including intermediate ones. Name duplicates are overwritten.
    private final Map<String, NamedElement> referenceableElements;

    // All declared elements that are not properties, together with all recursive property instantiations from the
    // active class as a root, including intermediate ones. Name duplicates are stored in a list.
    private final Map<String, List<NamedElement>> referenceableElementsInclDuplicates;

    /**
     * Returns all contextual elements of the UML model. If there is no active class, returns only the elements defined
     * only at the outer most level of the UML model. Otherwise, adds also all the leaves of composite data types.
     *
     * @param element The UML element.
     */
    public CifContext(Element element) {
        Model model = element.getModel();

        // Collect declared elements as set.
        declaredElements = queryContextElements(model).asSet();

        // Find the the active classes in the model.
        List<Element> activeClasses = model.getOwnedElements().stream()
                .filter(e -> e instanceof Class d && d.isActive()).toList();

        // If there are no active classes, simply returns the model elements. ProfileValidator checks the number of
        // classes.
        if (activeClasses.isEmpty()) {
            referenceableElements = queryContextElements(model).toMap(NamedElement::getName);
            referenceableElementsInclDuplicates = CifContext.queryContextElements(model).groupBy(NamedElement::getName);
        } else {
            // Get the active class and create the referenceable element maps.
            Class activeClass = (Class)activeClasses.get(0);
            referenceableElements = new LinkedHashMap<>();
            referenceableElementsInclDuplicates = new LinkedHashMap<>();

            // Get context elements, including the ones with duplicate names.
            Map<String, List<NamedElement>> contextElements = CifContext.queryContextElements(model)
                    .groupBy(NamedElement::getName);

            // Loop over every element and add it to the context if it is not a property. Properties are considered
            // in the following, starting from the active class.
            for (Entry<String, List<NamedElement>> entry: contextElements.entrySet()) {
                String elementName = entry.getKey();
                List<NamedElement> elementsWithSameName = entry.getValue();
                for (NamedElement umlElement: elementsWithSameName) {
                    if (!(umlElement instanceof Property property)) {
                        referenceableElements.put(elementName, umlElement);
                        referenceableElementsInclDuplicates.computeIfAbsent(elementName, k -> new LinkedList<>())
                                .add(umlElement);
                    }
                }
            }

            // For all properties of the active class, loop over their children (with and without duplicate names).
            for (Property property: activeClass.getOwnedAttributes()) {
                // Add the current intermediate property, with and without duplicate names.
                referenceableElements.put(property.getName(), property);
                referenceableElementsInclDuplicates.computeIfAbsent(property.getName(), k -> new LinkedList<>())
                        .add(property);

                // Recursive call on the children.
                addNestedProperties((DataType)property.getType(), property.getName(), referenceableElements,
                        referenceableElementsInclDuplicates);
            }
        }
    }

    private static void addNestedProperties(DataType datatype, String name, Map<String, NamedElement> namesAndElements,
            Map<String, List<NamedElement>> namesAndElementsWithDuplicates)
    {
        // Loop over all data type's attributes. If they are not a composite data type, add them to the map; otherwise,
        // recursively call on the children objects.
        for (Property umlProperty: datatype.getOwnedAttributes()) {
            String newName = name + "." + umlProperty.getName();

            // Add the current intermediate property.
            namesAndElements.put(newName, umlProperty);
            namesAndElementsWithDuplicates.computeIfAbsent(newName, k -> new LinkedList<>()).add(umlProperty);

            if (PokaYokeTypeUtil.isCompositeDataType(umlProperty.getType())) {
                // Recursive call on its children.
                addNestedProperties((DataType)umlProperty.getType(), newName, namesAndElements,
                        namesAndElementsWithDuplicates);
            }
        }
    }

    public boolean isDeclared(NamedElement element) {
        return declaredElements.contains(element);
    }

    protected NamedElement getElement(String name) {
        return referenceableElements.get(name);
    }

    protected Collection<NamedElement> getAllElements() {
        return Collections.unmodifiableCollection(referenceableElements.values());
    }

    public Map<String, List<NamedElement>> getAllElementsInclDuplicateNames() {
        return Collections.unmodifiableMap(referenceableElementsInclDuplicates);
    }

    public Map<String, NamedElement> getContextMap() {
        return Collections.unmodifiableMap(referenceableElements);
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

    public List<Property> getAllProperties() {
        return getAllElements().stream().filter(e -> e instanceof Property).map(Property.class::cast).toList();
    }

    public boolean isEnumeration(String name) {
        return referenceableElements.get(name) instanceof Enumeration;
    }

    public Enumeration getEnumeration(String name) {
        if (referenceableElements.get(name) instanceof Enumeration enumeration) {
            return enumeration;
        }
        return null;
    }

    public List<Enumeration> getAllEnumerations() {
        return getAllElements().stream().filter(Enumeration.class::isInstance).map(Enumeration.class::cast).toList();
    }

    public boolean isEnumerationLiteral(String name) {
        return referenceableElements.get(name) instanceof EnumerationLiteral;
    }

    public EnumerationLiteral getEnumerationLiteral(String name) {
        if (referenceableElements.get(name) instanceof EnumerationLiteral literal) {
            return literal;
        }
        return null;
    }

    public List<EnumerationLiteral> getAllEnumerationLiterals() {
        return getAllElements().stream().filter(EnumerationLiteral.class::isInstance)
                .map(EnumerationLiteral.class::cast).toList();
    }

    public List<OpaqueBehavior> getAllOpaqueBehaviors() {
        return getAllElements().stream().filter(OpaqueBehavior.class::isInstance).map(OpaqueBehavior.class::cast)
                .toList();
    }

    public boolean isVariable(String name) {
        return referenceableElements.get(name) instanceof Property;
    }

    public Property getVariable(String name) {
        if (referenceableElements.get(name) instanceof Property property) {
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
        if (referenceableElements.get(name) instanceof OpaqueBehavior behavior) {
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
