
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.List;

import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;

public class ExtendedCifContext extends CifContext {
    public ExtendedCifContext(Element element) {
        super(element);
    }

    public List<Class> getAllClasses() {
        return getAllElements().stream().filter(Class.class::isInstance).map(Class.class::cast).toList();
    }

    public List<Enumeration> getAllEnumerations() {
        return getAllElements().stream().filter(Enumeration.class::isInstance).map(Enumeration.class::cast).toList();
    }

    public List<EnumerationLiteral> getAllEnumerationLiterals() {
        return getAllElements().stream().filter(EnumerationLiteral.class::isInstance)
                .map(EnumerationLiteral.class::cast).toList();
    }
}
