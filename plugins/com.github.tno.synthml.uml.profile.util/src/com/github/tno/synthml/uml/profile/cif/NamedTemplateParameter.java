
package com.github.tno.synthml.uml.profile.cif;

import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.internal.impl.NamedElementImpl;

/**
 * Dummy template parameter used to encapsulate the information normally stored in the object tree of a
 * {@link ClassifierTemplateParameter} for easier use during translation. The implementation implements
 * {@link NamedElement} to unify access through {@link CifScope}
 */
@SuppressWarnings("restriction")
public class NamedTemplateParameter extends NamedElementImpl {
    /**
     * The cached type classifier for the template parameter.
     */
    protected Classifier constrainingClassifier;

    protected NamedTemplateParameter() {
        super();
    }

    /**
     * Returns the type of the constraining classifier.
     *
     * @return The type of the constraining classifier.
     */
    public Classifier getType() {
        return constrainingClassifier;
    }

    /**
     * Sets the constraining classifier.
     *
     * @param newConstrainingClassifier The new constraining classifier.
     */
    public void setConstrainingClassifier(Classifier newConstrainingClassifier) {
        constrainingClassifier = newConstrainingClassifier;
    }
}
