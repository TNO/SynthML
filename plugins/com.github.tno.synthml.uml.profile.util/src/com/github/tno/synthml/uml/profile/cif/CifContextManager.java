
package com.github.tno.synthml.uml.profile.cif;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Element;

/**
 * Caches {@link CifContext} instances to avoid redundant recreation.
 * The class maintains a global context and scoped contexts per {@link Activity}.
 */
public class CifContextManager {

    /** Cache mapping {@link Activity} instances to their corresponding scoped {@link CifContext}. */
    private Map<Activity, CifContext> contextCache;

    private CifContext globalContext;

    /**
     * Initializes the context manager with a global context derived from the given element.
     *
     * @param element The {@link Element} used to initialize the global context.
     */
    public CifContextManager(Element element) {
        contextCache = new HashMap<>();
        globalContext = CifContext.createGlobal(element);
    }

    public CifContext getScopedContext(Element element) {
        Activity activity = getActivity(element);

        if (activity == null) {
            return globalContext;
        } else {
            contextCache.computeIfAbsent(activity, CifContext::createScoped);
            return contextCache.get(activity);
        }
    }

    public CifContext getGlobalContext() {
        return globalContext;
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
