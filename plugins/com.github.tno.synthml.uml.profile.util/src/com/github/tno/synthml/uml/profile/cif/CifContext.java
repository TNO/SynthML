/**
 *
 */

package com.github.tno.synthml.uml.profile.cif;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
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

import com.github.tno.synthml.uml.profile.util.PokaYokeTypeUtil;

/** Symbol table of the UML model, with all its declared and referenceable named elements from the scope for which it is created. */
public interface CifContext {
    /**
     * Creates a context containing all declared/referenceable elements found in the global scope of {@code model}.
     *
     * @param element An {@link Element} contained in the model for which the context is created.
     * @return A {@code CifContext} containing all declared/referenceable elements in the global scope.
     */
    public static CifGlobalContext createGlobal(Element element) {
        return new CifGlobalContext(element);
    }

    /**
     * Creates a context containing all declared/referenceable elements from the local scope and the global scope.
     *
     * @param element An {@link Element} contained in the model for which the context is created.
     * @return A {@link CifContext} containing all declared/referenceable elements from the local scope and the global
     *     scope.
     */
    public static CifScopedContext createScoped(Element element) {
        return createScoped(element, new CifGlobalContext(element));
    }

    public static CifScopedContext createScoped(Element element, CifGlobalContext context) {
        return new CifScopedContext(element, context);
    }

    /**
     * Gives the UML model whose context to consider.
     *
     * @return The UML model whose context to consider.
     */
    Model getModel();

    Collection<NamedElement> getDeclaredElements();

    NamedElement getReferenceableElement(String name);

    Map<String, List<NamedElement>> getReferenceableElementsInclDuplicates();

    default List<Class> getAllClasses(Predicate<Class> predicate) {
        return getDeclaredElements().stream().filter(e -> e instanceof Class c && predicate.test(c))
                .map(Class.class::cast).toList();
    }

    default List<Activity> getAllActivities() {
        return getDeclaredElements().stream().filter(Activity.class::isInstance).map(Activity.class::cast).toList();
    }

    default List<Activity> getAllAbstractActivities() {
        return getDeclaredElements().stream().filter(e -> e instanceof Activity a && a.isAbstract())
                .map(Activity.class::cast).toList();
    }

    default List<DataType> getAllCompositeDataTypes() {
        return getDeclaredElements().stream()
                .filter(e -> e instanceof DataType d && PokaYokeTypeUtil.isCompositeDataType(d))
                .map(DataType.class::cast).toList();
    }

    default List<Activity> getAllConcreteActivities() {
        return getDeclaredElements().stream().filter(e -> e instanceof Activity a && !a.isAbstract())
                .map(Activity.class::cast).toList();
    }

    default List<Property> getAllDeclaredProperties() {
        return getDeclaredElements().stream().filter(e -> e instanceof Property).map(Property.class::cast).toList();
    }

    default List<ControlFlow> getAllControlFlows() {
        return getAllActivities().stream().map(Activity::getOwnedElements).flatMap(Collection::stream)
                .filter(ControlFlow.class::isInstance).map(ControlFlow.class::cast).toList();
    }

    default boolean isEnumeration(String name) {
        return getReferenceableElement(name) instanceof Enumeration;
    }

    default Enumeration getEnumeration(String name) {
        if (getReferenceableElement(name) instanceof Enumeration enumeration) {
            return enumeration;
        }
        return null;
    }

    default List<Enumeration> getAllEnumerations() {
        return getDeclaredElements().stream().filter(Enumeration.class::isInstance).map(Enumeration.class::cast)
                .toList();
    }

    default boolean isEnumerationLiteral(String name) {
        return getReferenceableElement(name) instanceof EnumerationLiteral;
    }

    default EnumerationLiteral getEnumerationLiteral(String name) {
        if (getReferenceableElement(name) instanceof EnumerationLiteral literal) {
            return literal;
        }
        return null;
    }

    default List<EnumerationLiteral> getAllEnumerationLiterals() {
        return getDeclaredElements().stream().filter(EnumerationLiteral.class::isInstance)
                .map(EnumerationLiteral.class::cast).toList();
    }

    default List<OpaqueBehavior> getAllOpaqueBehaviors() {
        return getDeclaredElements().stream().filter(OpaqueBehavior.class::isInstance).map(OpaqueBehavior.class::cast)
                .toList();
    }

    /**
     * Checks if the element is present in the context and represents a variable, i.e. a {@link Property} or a
     * {@link NamedTemplateParameter}.
     *
     * @param name The name of the declared entity.
     * @return {@code true} if the element is present in the context and represents a variable, else {@code false}.
     */
    default boolean isVariable(String name) {
        return getVariable(name) != null;
    }

    /**
     * Finds the variable in the context, i.e. a {@link Property} or a {@link NamedTemplateParameter}.
     *
     * @param name The name of the declared entity.
     * @return {@link Property} or {@link NamedTemplateParameter} if the variable is present in the context, else
     *     {@code null}.
     */
    default NamedElement getVariable(String name) {
        NamedElement element = getReferenceableElement(name);

        if (element instanceof Property || element instanceof NamedTemplateParameter) {
            return element;
        }
        return null;
    }

    /**
     * Checks if the element is present in the context and represents an assignable variable, i.e. a {@link Property}.
     *
     * @param name The name of the declared entity.
     * @return {@code true} if the element is present and assignable in the context, and represents a variable, else
     *     {@code false}.
     */
    default boolean isAssignableVariable(String name) {
        return getAssignableVariable(name) != null;
    }

    /**
     * Finds the assignable variable in the context, i.e. a {@link Property}.
     *
     * @param name The name of the declared entity.
     * @return {@link Property} if the variable is present in the context, else {@code null}.
     */
    default Property getAssignableVariable(String name) {
        NamedElement element = getReferenceableElement(name);

        if (element instanceof Property property) {
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
    default boolean isDeclaredElement(Element element) {
        return getDeclaredElements().contains(element);
    }

    default boolean hasOpaqueBehaviors() {
        return getDeclaredElements().stream().anyMatch(OpaqueBehavior.class::isInstance);
    }

    default OpaqueBehavior getOpaqueBehavior(String name) {
        if (getReferenceableElement(name) instanceof OpaqueBehavior behavior) {
            return behavior;
        }
        return null;
    }

    default boolean hasConstraints(Predicate<Constraint> predicate) {
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

    default boolean hasAbstractActivities() {
        return getDeclaredElements().stream().anyMatch(e -> e instanceof Activity a && a.isAbstract());
    }

    default boolean hasParameterizedActivities() {
        return getDeclaredElements().stream()
                .anyMatch(e -> e instanceof Activity a && !a.getOwnedParameters().isEmpty());
    }
}
