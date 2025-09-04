
package com.github.tno.synthml.uml.profile.cif;

import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.internal.impl.NamedElementImpl;

/**
 * Encapsulates template information normally stored in the object tree of a {@link ClassifierTemplateParameter}.
 * Implements {@link NamedElement} to provide unified access through {@link CifScopedContext}.
 */
@SuppressWarnings("restriction")
public class NamedTemplateParameter extends NamedElementImpl {
    /** The type of the parameter. */
    protected DataType type;

    protected NamedTemplateParameter() {
        super();
    }

    /**
     * Returns the type of the parameter.
     *
     * @return The type of the parameter.
     */
    public DataType getType() {
        return type;
    }

    /**
     * Sets the type of the parameter.
     *
     * @param type The type.
     */
    public void setType(DataType type) {
        this.type = type;
    }
}
