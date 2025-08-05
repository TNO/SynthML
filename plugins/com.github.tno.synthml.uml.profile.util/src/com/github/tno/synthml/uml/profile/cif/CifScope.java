/**
 *
 */

package com.github.tno.synthml.uml.profile.cif;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Classifier;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.RedefinableTemplateSignature;
import org.eclipse.uml2.uml.TemplateParameter;

/** Symbol table of a scope, containing the properties only available within this scope. */
public class CifScope {
    /**
     * Contains all declared named elements defined within the scope that are supported by our subset of UML. Note that
     * properties that are declared in composite data types may be referenced in different ways when they are
     * instantiated multiple times.
     */
    private final Set<NamedTemplateParameter> declaredElements;

    public CifScope(Element element) {
        declaredElements = getDeclaredTemplateParameters(element);
    }

    private CifScope() {
        declaredElements = Collections.EMPTY_SET;
    }

    @SuppressWarnings("restriction")
    private Set<NamedTemplateParameter> getDeclaredTemplateParameters(Element inputElement) {
        EObject current = inputElement;

        while (!(current instanceof Activity activity)) {
            if (current == null) {
                return Collections.EMPTY_SET;
            }

            current = current.eContainer();
        }

        Stream<TemplateParameter> templateParameters = activity.getOwnedElements().stream()
                .filter(RedefinableTemplateSignature.class::isInstance).map(RedefinableTemplateSignature.class::cast)
                .flatMap(signature -> signature.getOwnedParameters().stream());

        Set<NamedTemplateParameter> resultParameters = new HashSet<>();

        // create a corresponding UML Property with the same name and type.
        for (TemplateParameter parameter: templateParameters.collect(Collectors.toList())) {
            if (parameter instanceof ClassifierTemplateParameter classifierParam
                    && parameter.getDefault() instanceof NamedElement defaultNamedElement)
            {
                String paramName = defaultNamedElement.getName();
                Classifier constraint = classifierParam.getConstrainingClassifiers().get(0);

                NamedTemplateParameter newParameter = new NamedTemplateParameter();
                newParameter.setName(paramName);
                newParameter.setConstrainingClassifier(constraint);

                resultParameters.add(newParameter);
            }
        }

        return resultParameters;
    }

    public Set<NamedTemplateParameter> getDeclaredTemplateParameters() {
        return declaredElements;
    }

    public static CifScope global() {
        return new CifScope();
    }
}
