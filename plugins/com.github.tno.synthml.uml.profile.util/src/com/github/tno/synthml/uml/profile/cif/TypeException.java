////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.synthml.uml.profile.cif;

import org.eclipse.escet.common.java.TextPosition;

public class TypeException extends RuntimeException {
    private static final long serialVersionUID = 6860730254242254224L;

    private final TextPosition position;

    /**
     * Constructs a new runtime exception with the specified detail {@code message}.
     *
     * @param message The detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *     method.
     */
    public TypeException(String message) {
        this(message, null);
    }

    /**
     * Constructs a new runtime exception with the specified detail {@code message} and {@code position}.
     *
     * @param message The detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *     method.
     * @param position The text position of the problem. The position is saved for later retrieval by the
     *     {@link #getMessage()} method.
     */
    public TypeException(String message, TextPosition position) {
        super(message);
        this.position = position;
    }

    /**
     * Returns the position.
     *
     * @return The position
     */
    public TextPosition getPosition() {
        return position;
    }

    @Override
    public String getMessage() {
        if (position == null) {
            return super.getMessage();
        }
        return String.format("Type error at line %d, column %d: %s", position.startLine, position.startColumn,
                super.getMessage());
    }
}
