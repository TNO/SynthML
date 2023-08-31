
package com.github.tno.pokayoke.transform.uml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;

import com.google.common.base.Verify;

/** Collects basic typing information from a model that can be queried. */
public class ModelTyping {
    private final Model model;

    private final Map<String, Enumeration> enums = new LinkedHashMap<>();

    private final Map<String, EnumerationLiteral> enumLiterals = new LinkedHashMap<>();

    private final Map<String, Property> variables = new LinkedHashMap<>();

    public ModelTyping(Model model) {
        this.model = model;
        populate();
    }

    public Enumeration getEnumeration(String name) {
        return enums.get(name);
    }

    public EnumerationLiteral getEnumerationLiteral(String name) {
        return enumLiterals.get(name);
    }

    public Property getVariable(String name) {
        return variables.get(name);
    }

    public boolean isDeclared(String name) {
        return isEnumeration(name) || isEnumerationLiteral(name) || isVariable(name);
    }

    public boolean isEnumeration(String name) {
        return enums.containsKey(name);
    }

    public boolean isEnumerationLiteral(String name) {
        return enumLiterals.containsKey(name);
    }

    public boolean isVariable(String name) {
        return variables.containsKey(name);
    }

    private void ensureUndeclared(String name) {
        Verify.verify(!isDeclared(name), String.format("Symbol %s already exists.", name));
    }

    private void populate() {
        populateFrom(model);
    }

    private void populateFrom(Model modelElement) {
        for (PackageableElement element: modelElement.getPackagedElements()) {
            if (element instanceof Model childElement) {
                populateFrom(childElement);
            } else if (element instanceof Class classElement) {
                populateFrom(classElement);
            } else if (element instanceof Enumeration enumElement) {
                populateFrom(enumElement);
            } else {
                throw new RuntimeException("Unsupported packaged element type: " + element);
            }
        }
    }

    private void populateFrom(Class classElement) {
        classElement.getOwnedAttributes().forEach(property -> populateFrom(property));
    }

    private void populateFrom(Enumeration enumeration) {
        String enumName = enumeration.getName();
        ensureUndeclared(enumName);
        enums.put(enumName, enumeration);
        enumeration.getOwnedLiterals().forEach(enumLiteral -> populateFrom(enumLiteral));
    }

    private void populateFrom(EnumerationLiteral enumLiteral) {
        String name = enumLiteral.getName();
        ensureUndeclared(name);
        enumLiterals.put(name, enumLiteral);
    }

    private void populateFrom(Property property) {
        String name = property.getName();
        ensureUndeclared(name);
        variables.put(name, property);
    }
}
