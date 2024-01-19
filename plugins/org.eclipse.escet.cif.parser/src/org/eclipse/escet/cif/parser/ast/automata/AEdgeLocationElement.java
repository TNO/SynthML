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

package org.eclipse.escet.cif.parser.ast.automata;

import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.common.java.TextPosition;

/** Edge location element. */
public class AEdgeLocationElement extends ALocationElement {
    /** The core edge of the edge location element. */
    public final ACoreEdge coreEdge;

    /** The target location of the edge location element, or {@code null} for self-loops. */
    public final AIdentifier target;

    /**
     * Constructor for the {@link AEdgeLocationElement} class.
     *
     * @param coreEdge The core edge of the edge location element.
     * @param target The target location of the edge location element, or {@code null} for self-loops.
     * @param position Position information.
     */
    public AEdgeLocationElement(ACoreEdge coreEdge, AIdentifier target, TextPosition position) {
        super(position);
        this.coreEdge = coreEdge;
        this.target = target;
    }
}
