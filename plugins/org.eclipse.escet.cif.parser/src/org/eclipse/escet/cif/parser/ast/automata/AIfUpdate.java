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

import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.TextPosition;

/** If update. */
public class AIfUpdate extends AUpdate {
    /** The guards of the 'if' update. */
    public final List<AExpression> guards;

    /** The 'then' updates of the 'if' update. */
    public final List<AUpdate> thens;

    /** The 'elif' updates of the 'if' update. */
    public final List<AElifUpdate> elifs;

    /** The 'else' updates of the 'if' update. */
    public final List<AUpdate> elses;

    /**
     * Constructor for the {@link AIfUpdate} class.
     *
     * @param guards The guards of the 'if' update.
     * @param thens The 'then' updates of the 'if' update.
     * @param elifs The 'elif' updates of the 'if' update.
     * @param elses The 'else' updates of the 'if' update.
     * @param position Position information.
     */
    public AIfUpdate(List<AExpression> guards, List<AUpdate> thens, List<AElifUpdate> elifs, List<AUpdate> elses,
            TextPosition position)
    {
        super(position);
        this.guards = guards;
        this.thens = thens;
        this.elifs = elifs;
        this.elses = elses;
    }
}
