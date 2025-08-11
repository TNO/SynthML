
package com.github.tno.synthml.uml.profile.cif;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.internal.impl.NamedElementImpl;

/**
 * Encapsulates template information normally stored in the object tree of a {@link ClassifierTemplateParameter}.
 * Implements {@link NamedElement} to provide unified access through {@link CifScope}.
 */
@SuppressWarnings("restriction")
public class NamedTemplateParameter extends NamedElementImpl {
    /**
     * The cached type classifier for the template parameter.
     */
    protected DataType constrainingClassifier;

    protected NamedTemplateParameter() {
        super();
    }

    /**
     * Returns the type of the constraining classifier.
     *
     * @return The type of the constraining classifier.
     */
    public DataType getType() {
        return constrainingClassifier;
    }

    /**
     * Sets the constraining classifier.
     *
     * @param newConstrainingClassifier The new constraining classifier.
     */
    public void setConstrainingClassifier(DataType newConstrainingClassifier) {
        constrainingClassifier = newConstrainingClassifier;
    }
}
