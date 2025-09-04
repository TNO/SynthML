
package com.github.tno.synthml.uml.profile.cif;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ParameterableElement;
import org.eclipse.uml2.uml.TemplateSignature;
import org.eclipse.uml2.uml.Type;

/** Symbol table of a scope, containing the named elements only available within this scope. */
public class CifScopedContext {
    /**
     * Contains all declared named elements defined within the scope that are supported by our subset of UML. Note that
     * properties that are declared in composite data types may be referenced in different ways when they are
     * instantiated multiple times.
     */
    private final List<NamedTemplateParameter> declaredTemplateParameters;

    public CifScopedContext(Element element) {
        declaredTemplateParameters = getDeclaredTemplateParameters(element);
    }

    private CifScopedContext() {
        declaredTemplateParameters = Collections.EMPTY_LIST;
    }

    @SuppressWarnings("restriction")
    private List<NamedTemplateParameter> getDeclaredTemplateParameters(Element inputElement) {
        EObject current = inputElement;

        while (!(current instanceof Activity activity)) {
            if (current == null) {
                return Collections.EMPTY_LIST;
            }

            current = current.eContainer();
        }

        List<ClassifierTemplateParameter> templateParameters = getClassifierTemplateParameters(activity);

        List<NamedTemplateParameter> resultParameters = new ArrayList<>();

        // Create a corresponding named template parameter with the same name and type.
        for (ClassifierTemplateParameter classifierParameter: templateParameters) {
            if (!(classifierParameter.getDefault() instanceof NamedElement)) {
                continue;
            }

            String paramName = getClassifierTemplateParameterName(classifierParameter.getDefault());
            Type parameterType = getClassifierTemplateParameterType(classifierParameter);

            NamedTemplateParameter newParameter = new NamedTemplateParameter();
            newParameter.setName(paramName);

            if (parameterType instanceof DataType parameterDataType) {
                newParameter.setType(parameterDataType);
                resultParameters.add(newParameter);
            }
        }

        return resultParameters;
    }

    public List<NamedTemplateParameter> getDeclaredTemplateParameters() {
        return Collections.unmodifiableList(declaredTemplateParameters);
    }

    public static CifScope global() {
        return new CifScope();
    }

    public static Type getClassifierTemplateParameterType(ClassifierTemplateParameter classifierParameter) {
        return classifierParameter.getConstrainingClassifiers().stream().findFirst().orElse(null);
    }

    public static String getClassifierTemplateParameterName(ParameterableElement classifierParameter) {
        return ((NamedElement)classifierParameter).getName();
    }

    public static List<ClassifierTemplateParameter> getClassifierTemplateParameters(Activity activity) {
        TemplateSignature templateSignature = activity.getOwnedTemplateSignature();

        if (templateSignature == null) {
            return Collections.EMPTY_LIST;
        }

        return getClassifierTemplateParameters(templateSignature);
    }

    public static List<ClassifierTemplateParameter>
            getClassifierTemplateParameters(TemplateSignature templateSignature)
    {
        return templateSignature.getOwnedParameters().stream().filter(ClassifierTemplateParameter.class::isInstance)
                .map(ClassifierTemplateParameter.class::cast).toList();
    }
}
