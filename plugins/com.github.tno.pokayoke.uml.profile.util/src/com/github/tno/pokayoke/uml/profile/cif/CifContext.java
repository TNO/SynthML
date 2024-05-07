
package com.github.tno.pokayoke.uml.profile.cif;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.UMLPackage;

import com.google.common.collect.Sets;

/** Collects basic typing information from a model that can be queried. */
public class CifContext {
    /**
     * All elements are {@link EClass#isSuperTypeOf(EClass) derived} from
     * {@link org.eclipse.uml2.uml.UMLPackage.Literals#NAMED_ELEMENT}.
     */
    private static final Set<EClass> CONTEXT_TYPES = Sets.newHashSet(UMLPackage.Literals.PACKAGE,
            UMLPackage.Literals.ENUMERATION, UMLPackage.Literals.ENUMERATION_LITERAL, UMLPackage.Literals.CLASS,
            UMLPackage.Literals.PROPERTY, UMLPackage.Literals.ACTIVITY, UMLPackage.Literals.OPAQUE_BEHAVIOR);

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

    private final Map<String, NamedElement> contextElements;

    public CifContext(Element element) {
        Model model = element.getModel();
        // Do not check duplicates here, as that is the responsibility of model validation
        contextElements = queryContextElements(model).toMap(NamedElement::getName);
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

    public boolean isEnumeration(String name) {
        return contextElements.get(name) instanceof Enumeration;
    }

    public Enumeration getEnumeration(String name) {
        if (contextElements.get(name) instanceof Enumeration enumeration) {
            return enumeration;
        }
        return null;
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

    public boolean isVariable(String name) {
        return contextElements.get(name) instanceof Property;
    }

    public Property getVariable(String name) {
        if (contextElements.get(name) instanceof Property property) {
            return property;
        }
        return null;
    }

    public boolean hasOpaqueBehaviors() {
        return getAllElements().stream().anyMatch(OpaqueBehavior.class::isInstance);
    }
}
