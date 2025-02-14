
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;

import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.google.common.collect.Sets;

/** Symbol table of the UML model, with all its declared and referenceable named elements. */
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
     * Contains all declared named elements of the model that are supported by our subset of UML. Note that properties
     * that are declared in composite data types may be referenced in different ways when they are instantiated multiple
     * times.
     */
    private final Set<NamedElement> declaredElements;

    /**
     * Per absolute name of a referenceable element, the element that is referenced.
     *
     * <p>
     * For elements that are not properties, the names are single identifiers. For properties, which are recursively
     * instantiated starting at the active class as a root, the names are absolute names.
     * </p>
     *
     * <p>
     * In case of duplicate names, entries are overwritten and thus only one referenced element with that absolute name
     * is stored.
     * </p>
     *
     * <p>
     * If the UML model has no active class, all declared named elements are considered referenceable elements based on
     * their single identifier names. If the UML model has multiple active classes, only the first active class is
     * considered.
     * </p>
     */
    private final Map<String, NamedElement> referenceableElements;

    /**
     * Per absolute name of a referenceable element, the elements that are referenced.
     *
     * <p>
     * For elements that are not properties, the names are single identifiers. For properties, which are recursively
     * instantiated starting at the active class as a root, the names are absolute names.
     * </p>
     *
     * <p>
     * All referenceable elements with the same name are stored in a list.
     * </p>
     *
     * <p>
     * If the UML model has no active class, all declared named elements are considered referenceable elements based on
     * their single identifier names. If the UML model has multiple active classes, only the first active class is
     * considered.
     * </p>
     */
    private final Map<String, List<NamedElement>> referenceableElementsInclDuplicates;

    /**
     * Finds all declared elements in the {@code model}.
     *
     * @param model The search root.
     * @return All declared elements in the {@code model}.
     */
    public static QueryableIterable<NamedElement> getDeclaredElements(Model model) {
        return QueryableIterable.from(model.eAllContents()).union(model).select(e -> CONTEXT_TYPES.contains(e.eClass()))
                .asType(NamedElement.class);
    }

    public CifContext(Element element) {
        Model model = element.getModel();

        // Collect declared elements as set.
        declaredElements = getDeclaredElements(model).asOrderedSet();

        // Find the active classes in the model.
        List<Class> activeClasses = model.getOwnedElements().stream()
                .filter(e -> e instanceof Class cls && cls.isActive()).map(cls -> (Class)cls).toList();

        // Collect the referenceable elements that have a single identifier as their name. Elements that may have
        // absolute names consisting of multiple identifiers collected separately later on.
        if (activeClasses.isEmpty()) {
            // No active class. The profile validator checks the number of classes. Here, consider all declared elements
            // as referenceable elements based on their single identifier names, to be able to still do some type
            // checking.
            referenceableElements = declaredElements.stream()
                    .collect(Collectors.toMap(NamedElement::getName, e -> e, (oldValue, newValue) -> newValue));
            referenceableElementsInclDuplicates = declaredElements.stream()
                    .collect(Collectors.groupingBy(NamedElement::getName));
        } else {
            // Get the active class and create the referenceable element maps. In case there are multiple active
            // classes, we ignore all but the first one. The profile validator checks the number of classes.
            Class activeClass = activeClasses.get(0);
            referenceableElements = new LinkedHashMap<>();
            referenceableElementsInclDuplicates = new LinkedHashMap<>();

            // Collect all referenceable elements that are always referred to by a single identifier.
            for (NamedElement declaredElement: declaredElements) {
                String elementName = declaredElement.getName();
                if (!(declaredElement instanceof Property property)) {
                    referenceableElements.put(elementName, declaredElement);
                    referenceableElementsInclDuplicates.computeIfAbsent(elementName, k -> new LinkedList<>())
                            .add(declaredElement);
                }
            }

            // Collect all referenceable elements that may be referred to by an absolute name consisting of multiple
            // identifiers.
            addProperties(activeClass.getOwnedAttributes(), null, new LinkedHashSet<>());
        }
    }

    /**
     * Add instantiated properties, recursively, to {@link #referenceableElements} and
     * {@link #referenceableElementsInclDuplicates}.
     *
     * @param properties The properties to add.
     * @param prefix The prefix of the properties, or {@code null} for root properties.
     * @param hierarchy The set tracking the composite data type hierarchy.
     */
    private void addProperties(Collection<Property> properties, String prefix, Set<DataType> hierarchy) {
        for (Property umlProperty: properties) {
            String name = ((prefix == null) ? "" : prefix + ".") + umlProperty.getName();

            // Add property.
            referenceableElements.put(name, umlProperty);
            referenceableElementsInclDuplicates.computeIfAbsent(name, k -> new LinkedList<>()).add(umlProperty);

            // Add descendant, if property has them.
            Type propertyType = umlProperty.getType();
            if (PokaYokeTypeUtil.isCompositeDataType(propertyType)) {
                // Stop the recursion if instantiation cycle found (the Poka Yoke validator guarantees that valid UML
                // models don't contain instantiation cycles).
                if (hierarchy.contains(propertyType)) {
                    return;
                } else {
                    hierarchy.add((DataType)propertyType);
                    addProperties(((DataType)propertyType).getOwnedAttributes(), name, hierarchy);
                    hierarchy.remove(propertyType);
                }
            }
        }
    }

    protected Collection<NamedElement> getDeclaredElements() {
        return Collections.unmodifiableCollection(declaredElements);
    }

    protected NamedElement getReferenceableElement(String name) {
        return referenceableElements.get(name);
    }

    public Map<String, List<NamedElement>> getReferenceableElementsInclDuplicates() {
        return Collections.unmodifiableMap(referenceableElementsInclDuplicates);
    }

    public List<Class> getAllClasses(Predicate<Class> predicate) {
        return getDeclaredElements().stream().filter(e -> e instanceof Class c && predicate.test(c))
                .map(Class.class::cast).toList();
    }

    public List<Activity> getAllActivities() {
        return getDeclaredElements().stream().filter(Activity.class::isInstance).map(Activity.class::cast).toList();
    }

    public List<Activity> getAllAbstractActivities() {
        return getDeclaredElements().stream().filter(e -> e instanceof Activity a && a.isAbstract())
                .map(Activity.class::cast).toList();
    }

    public List<Activity> getAllConcreteActivities() {
        return getDeclaredElements().stream().filter(e -> e instanceof Activity a && !a.isAbstract())
                .map(Activity.class::cast).toList();
    }

    public List<Property> getAllDeclaredProperties() {
        return getDeclaredElements().stream().filter(e -> e instanceof Property).map(Property.class::cast).toList();
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
        return getDeclaredElements().stream().filter(Enumeration.class::isInstance).map(Enumeration.class::cast)
                .toList();
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
        return getDeclaredElements().stream().filter(EnumerationLiteral.class::isInstance)
                .map(EnumerationLiteral.class::cast).toList();
    }

    public List<OpaqueBehavior> getAllOpaqueBehaviors() {
        return getDeclaredElements().stream().filter(OpaqueBehavior.class::isInstance).map(OpaqueBehavior.class::cast)
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
     * Checks whether the given element is declared in the UML model.
     *
     * @param element The element to check.
     * @return {@code true} if the element is declared in the UML model, {@code false} otherwise.
     */
    public boolean isDeclaredElement(Element element) {
        return getDeclaredElements().contains(element);
    }

    public boolean hasOpaqueBehaviors() {
        return getDeclaredElements().stream().anyMatch(OpaqueBehavior.class::isInstance);
    }

    public OpaqueBehavior getOpaqueBehavior(String name) {
        if (referenceableElements.get(name) instanceof OpaqueBehavior behavior) {
            return behavior;
        }
        return null;
    }

    public boolean hasConstraints(Predicate<Constraint> predicate) {
        return getDeclaredElements().stream().anyMatch(e -> e instanceof Constraint c && predicate.test(c));
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
        return getDeclaredElements().stream().anyMatch(e -> e instanceof Activity a && a.isAbstract());
    }
}
