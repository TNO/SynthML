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

package org.eclipse.escet.cif.parser.ast;

import java.util.List;

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.tokens.AIdentifier;
import org.eclipse.escet.cif.parser.ast.tokens.AName;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.setext.runtime.Token;

/** Invariant. */
public class AInvariant extends ACifObject {
    /** The name, or {@code null} if not specified. */
    public final AIdentifier name;

    /** The predicate. */
    public final AExpression predicate;

    /** The invariant kind. Is either a state/event exclusion kind token, or {@code null} for state invariants. */
    public final Token invKind;

    /** The textual references to the events, or {@code null} for state invariants. */
    public final List<AName> events;

    /**
     * Constructor for the {@link AInvariant} class.
     *
     * @param name The name, or {@code null}.
     * @param predicate The predicate.
     * @param invKind The invariant kind. Is either a state/event exclusion kind token, or {@code null} for state
     *     invariants.
     * @param events The textual references to the events, or {@code null} for state invariants.
     */
    public AInvariant(AIdentifier name, AExpression predicate, Token invKind, List<AName> events) {
        super(null); // No position information.
        this.name = name;
        this.predicate = predicate;
        this.invKind = invKind;
        this.events = events;
        Assert.ifAndOnlyIf(invKind == null, events == null);
    }
}
