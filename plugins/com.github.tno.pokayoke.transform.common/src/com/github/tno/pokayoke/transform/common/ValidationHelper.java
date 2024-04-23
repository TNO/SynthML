
package com.github.tno.pokayoke.transform.common;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.edit.providers.UMLItemProviderAdapterFactory;

public class ValidationHelper {
    public static final UMLFactory FACTORY = UMLFactory.eINSTANCE;

    private ValidationHelper() {
        // Empty for utility classes
    }

    /**
     * Validates an UML model.
     *
     * @param model The model to validate.
     * @throws CoreException Thrown when validation result is {@link Diagnostic#ERROR erroneous}.
     */
    public static void validateModel(Model model) throws CoreException {
        UMLItemProviderAdapterFactory adapterFactory = new UMLItemProviderAdapterFactory();
        try {
            Diagnostician diagnostician = new Diagnostician() {
                @Override
                public String getObjectLabel(EObject eObject) {
                    IItemLabelProvider labelProvider = (IItemLabelProvider)adapterFactory.adapt(eObject,
                            IItemLabelProvider.class);
                    return labelProvider == null ? super.getObjectLabel(eObject) : labelProvider.getText(eObject);
                }
            };
            Diagnostic validationResult = diagnostician.validate(model);
            if (validationResult.getSeverity() > Diagnostic.WARNING) {
                throw new CoreException(BasicDiagnostic.toIStatus(validationResult));
            }
        } finally {
            adapterFactory.dispose();
        }
    }
}
