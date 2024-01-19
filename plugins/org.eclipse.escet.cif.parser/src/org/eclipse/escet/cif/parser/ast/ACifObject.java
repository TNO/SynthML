//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2024 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
//////////////////////////////////////////////////////////////////////////////

package org.eclipse.escet.cif.parser.ast;

import static org.eclipse.escet.common.position.common.PositionUtils.toPosition;

import org.eclipse.escet.common.java.TextPosition;
import org.eclipse.escet.common.position.metamodel.position.Position;

/** Base class of all classes in the CIF parser AST. */
public abstract class ACifObject {
    /** Position information. */
    public final TextPosition position;

    /**
     * Constructor for the {@link ACifObject} class.
     *
     * @param position Position information, or {@code null} if not available.
     */
    public ACifObject(TextPosition position) {
        this.position = position;
    }

    /**
     * Construct a fresh {@link Position} from the stored {@link #position}.
     *
     * @return Fresh instance of the stored position.
     */
    public Position createPosition() {
        return toPosition(position);
    }
}
