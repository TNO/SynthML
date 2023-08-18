//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2010, 2023 Contributors to the Eclipse Foundation
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

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** Event reference on an edge. */
public class AEdgeEvent extends ACifObject {
    /** The direction of the data of the event. */
    public final Direction direction;

    /** The reference to the event (may be 'tau'). */
    public final AExpression eventRef;

    /** The value of the send, or {@code null} if not available. */
    public final AExpression value;

    /**
     * Constructor for the {@link AEdgeEvent} class.
     *
     * @param direction The direction of the data of the event.
     * @param eventRef The reference to the event.
     * @param value The value of the send, or {@code null} if not available.
     * @param position Position information.
     */
    public AEdgeEvent(Direction direction, AExpression eventRef, AExpression value, TextPosition position) {
        super(position);
        this.direction = direction;
        this.eventRef = eventRef;
        this.value = value;
    }

    /** Direction for events on edges, related to the data of the event. */
    public static enum Direction {
        /** Synchronize only. */
        NONE,

        /** Send data. */
        SEND,

        /** Receive data. */
        RECEIVE;
    }
}
