
package com.github.tno.synthml.uml.profile.cif;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;

import com.github.tno.synthml.uml.profile.util.PokaYokeTypeUtil;
import com.google.common.collect.Sets;

/** Symbol table of the UML model, with all declared and named elements referencable from any scope. */
public class CifGlobalContext implements CifContext {
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

    /** The UML model whose context to consider. */
    private final Model model;

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
     * @param model An {@link Element} contained in the model for which the context is created..
     * @return All declared elements in the {@code model}.
     */
    public static QueryableIterable<NamedElement> getDeclaredElements(Model model) {
        return QueryableIterable.from(model.eAllContents()).union(model).select(e -> CONTEXT_TYPES.contains(e.eClass()))
                .asType(NamedElement.class);
    }

    CifGlobalContext(Element element) {
        this.model = element.getModel();

        // Collect declared elements as set.
        declaredElements = getDeclaredElements(model).asOrderedSet();

        // Find the active classes in the model.
        List<Class> activeClasses = model.getOwnedElements().stream()
                .filter(e -> e instanceof Class cls && cls.isActive()).map(cls -> (Class)cls).toList();

        // Collect the referenceable elements, as well as any duplicates.
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
                if (!(declaredElement instanceof Property || declaredElement.eClass() == UMLPackage.Literals.CLASS)) {
                    referenceableElements.put(elementName, declaredElement);
                    referenceableElementsInclDuplicates.computeIfAbsent(elementName, k -> new LinkedList<>())
                            .add(declaredElement);
                }
            }

            // Collect all referenceable elements that may be referred to by an absolute name consisting of multiple
            // identifiers.
            addProperties(activeClass.getOwnedAttributes(), null, new LinkedHashSet<>());

            // Collect all template parameters that may be referenced within the context of this activity.
            addNamedTemplateParameters(scope.getDeclaredTemplateParameters());
        }
    }

    /**
     * Gives the UML model whose context to consider.
     *
     * @return The UML model whose context to consider.
     */
    @Override
    public Model getModel() {
        return model;
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

            // Add descendants, if property has them.
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

    /**
     * Add template parameters to {@link #referenceableElements} and {@link #referenceableElementsInclDuplicates}.
     *
     * @param parameters The template parameters to add.
     */
    private void addNamedTemplateParameters(Collection<NamedTemplateParameter> parameters) {
        for (NamedElement parameter: parameters) {
            String name = parameter.getName();

            // Add parameter.
            referenceableElements.put(name, parameter);
            referenceableElementsInclDuplicates.computeIfAbsent(name, k -> new LinkedList<>()).add(parameter);
        }
    }

    @Override
    public Collection<NamedElement> getDeclaredElements() {
        return Collections.unmodifiableCollection(declaredElements);
    }

    @Override
    public NamedElement getReferenceableElement(String name) {
        return referenceableElements.get(name);
    }

    @Override
    public Map<String, List<NamedElement>> getReferenceableElementsInclDuplicates() {
        return Collections.unmodifiableMap(referenceableElementsInclDuplicates);
    }
}
