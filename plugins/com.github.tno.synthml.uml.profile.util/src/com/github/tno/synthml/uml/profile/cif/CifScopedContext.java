
package com.github.tno.synthml.uml.profile.cif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ParameterableElement;
import org.eclipse.uml2.uml.TemplateSignature;
import org.eclipse.uml2.uml.Type;

/** Symbol table of a scope, containing the named elements available within this scope. */
public class CifScopedContext implements CifContext {
    /**
     * Contains all template parameters declared within the activity that owns the element given in the constructor.
     */
    private final List<NamedTemplateParameter> declaredTemplateParameters;

    private final Map<String, NamedTemplateParameter> referenceableTemplateParameters;

    private final CifContext parent;

    /**
     * Constructs a context containing all declared/referenceable elements from the local scope and the global scope.
     *
     * @param element An {@link Element} contained in the activity for which the context is created.
     * @param parent A {@link CifGlobalContext} that is the parent of this scoped context.
     */
    @SuppressWarnings("restriction")
    protected CifScopedContext(Element element, CifGlobalContext parent) {
        declaredTemplateParameters = getDeclaredTemplateParameters(element);
        referenceableTemplateParameters = declaredTemplateParameters.stream()
                .collect(Collectors.toMap(NamedTemplateParameter::getName, Function.identity()));
        this.parent = parent;
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

    @Override
    public Model getModel() {
        return parent.getModel();
    }

    @Override
    public NamedElement getReferenceableElement(String name) {
        NamedElement templateParameter = referenceableTemplateParameters.get(name);
        return (templateParameter != null) ? templateParameter : parent.getReferenceableElement(name);
    }

    @SuppressWarnings("restriction")
    @Override
    public Map<String, List<NamedElement>> getReferenceableElementsInclDuplicates() {
        Map<String, List<NamedElement>> referenceableElementsInclDuplicates = new LinkedHashMap<>();

        for (Entry<String, List<NamedElement>> e: parent.getReferenceableElementsInclDuplicates().entrySet()) {
            referenceableElementsInclDuplicates.computeIfAbsent(e.getKey(), k -> new LinkedList<>())
                    .addAll(e.getValue());
        }

        for (NamedTemplateParameter parameter: declaredTemplateParameters) {
            referenceableElementsInclDuplicates.computeIfAbsent(parameter.getName(), k -> new LinkedList<>())
                    .add(parameter);
        }

        return Collections.unmodifiableMap(referenceableElementsInclDuplicates);
    }

    @Override
    public Collection<NamedElement> getDeclaredElements() {
        return parent.getDeclaredElements();
    }
}
