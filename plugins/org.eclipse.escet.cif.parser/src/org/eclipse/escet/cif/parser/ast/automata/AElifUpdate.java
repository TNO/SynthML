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

/** An 'elif' update part of 'if' update. */
public class AElifUpdate extends ACifObject {
    /** The guards of the 'elif' update. */
    public final List<AExpression> guards;

    /** The 'then' updates of the 'elif' update. */
    public final List<AUpdate> thens;

    /**
     * Constructor for the {@link AElifUpdate} class.
     *
     * @param guards The guards of the 'elif' update.
     * @param thens The 'then' updates of the 'elif' update.
     * @param position Position information.
     */
    public AElifUpdate(List<AExpression> guards, List<AUpdate> thens, TextPosition position) {
        super(position);
        this.guards = guards;
        this.thens = thens;
    }
}
