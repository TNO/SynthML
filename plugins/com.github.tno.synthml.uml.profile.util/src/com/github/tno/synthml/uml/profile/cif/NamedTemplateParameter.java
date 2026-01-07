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

    @Override
    public String getName() {
        // Override only to suppress discouraged access warnings when calling this method.
        return super.getName();
    }

    @Override
    public void setName(String name) {
        // Override only to suppress discouraged access warnings when calling this method.
        super.setName(name);
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
