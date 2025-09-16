
package com.github.tno.synthml.uml.profile.cif;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;

/**
 * Caches {@link CifContext} instances to avoid redundant recreation. The class maintains a global context and scoped
 * contexts per {@link Activity}.
 */
public class CifContextManager {
    /** Cache mapping {@link Activity} instances to their corresponding scoped {@link CifContext}. */
    private final Map<Activity, CifContext> contextCache;

    /** Cached global {@link CifContext}. */
    private CifContext globalContext;

    /**
     * Initializes the context manager with a global context for the model that contains the given element.
     *
     * @param element The {@link Element} of the model for which to create the global context.
     */
    public CifContextManager(Element element) {
        contextCache = new HashMap<>();
        globalContext = new CifContext(element, CifScope.global());
    }

    public CifContext getScopedContext(Element element) {
        Activity activity = getActivity(element);

        if (activity == null) {
            return globalContext;
        } else {
            return contextCache.computeIfAbsent(activity, CifContextManager::createScoped);
        }
    }

    public void refresh() {
        contextCache.clear();
        Model model = globalContext.getModel();
        globalContext = new CifContext(model, CifScope.global());
    }

    public CifContext getGlobalContext() {
        return globalContext;
    }

    /**
     * Creates a context containing all declared/referenceable elements from the local scope and the global scope.
     *
     * @param element An {@link Element} contained in the model for which the context is created.
     * @return A {@link CifContext} containing all declared/referenceable elements from the local scope and the global
     *     scope.
     */
    private static CifContext createScoped(Element element) {
        return new CifContext(element, new CifScope(element));
    }

    private Activity getActivity(Element element) {
        EObject current = element;
        while (current != null) {
            if (current instanceof Activity activity) {
                if (CifScope.getClassifierTemplateParameters(activity).isEmpty()) {
                    return null;
                }

                return activity;
            }

            current = current.eContainer();
        }

        return null;
    }
}
