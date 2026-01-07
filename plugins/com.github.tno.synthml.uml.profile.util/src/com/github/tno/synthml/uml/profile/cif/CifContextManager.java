////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

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
    /** Cached {@link CifGlobalContext}. */
    private CifGlobalContext globalContext;

    /** Cache mapping {@link Activity} instances to their corresponding {@link CifScopedContext}. */
    private final Map<Activity, CifScopedContext> scopedContexts;

    /**
     * Initializes the context manager with a global context for the model that contains the given element.
     *
     * @param element The {@link Element} of the model for which to create the global context.
     */
    public CifContextManager(Element element) {
        scopedContexts = new HashMap<>();
        globalContext = new CifGlobalContext(element);
    }

    public CifGlobalContext getGlobalContext() {
        return globalContext;
    }

    public CifScopedContext getScopedContext(Element element) {
        Activity activity = getActivity(element);
        return scopedContexts.computeIfAbsent(activity, this::createScoped);
    }

    public void refresh() {
        scopedContexts.clear();
        Model model = globalContext.getModel();
        globalContext = new CifGlobalContext(model);
    }

    /**
     * Creates a context containing all declared/referenceable elements from the local scope and the global scope.
     *
     * @param element An {@link Element} contained in the model for which the context is created.
     * @return A {@link CifContext} containing all declared/referenceable elements from the local scope and the global
     *     scope.
     */
    private CifScopedContext createScoped(Element element) {
        return new CifScopedContext(element, globalContext);
    }

    private Activity getActivity(Element element) {
        EObject current = element;
        while (current != null) {
            if (current instanceof Activity activity) {
                return activity;
            }

            current = current.eContainer();
        }

        return null;
    }
}
