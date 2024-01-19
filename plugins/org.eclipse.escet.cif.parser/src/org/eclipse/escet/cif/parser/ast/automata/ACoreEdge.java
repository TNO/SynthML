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

import java.util.List;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** Edge. */
public class ACoreEdge extends ACifObject {
    /** The events of the edge. */
    public final List<AEdgeEvent> events;

    /** The guards of the edge. */
    public final List<AExpression> guards;

    /** The position information of the urgency of the edge, or {@code null} if the edge is not urgent. */
    public final TextPosition urgentPos;

    /** The updates of the edge. */
    public final List<AUpdate> updates;

    /**
     * Constructor for the {@link ACoreEdge} class.
     *
     * @param events The events of the edge.
     * @param guards The guards of the edge.
     * @param urgentPos The position information of the urgency of the edge, or {@code null} if the edge is not urgent.
     * @param updates The updates of the edge.
     */
    public ACoreEdge(List<AEdgeEvent> events, List<AExpression> guards, TextPosition urgentPos, List<AUpdate> updates) {
        super(null);
        this.events = events;
        this.guards = guards;
        this.urgentPos = urgentPos;
        this.updates = updates;
    }
}
