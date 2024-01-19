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

package org.eclipse.escet.cif.parser.ast.iodecls.print;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.Assert;

/** The 'when' part of a {@link APrint}. */
public class APrintWhen extends ACifObject {
    /** The 'pre' condition of the 'when', or {@code null} if not available. */
    public final AExpression pre;

    /** The 'post' condition of the 'when', or {@code null} if not available. */
    public final AExpression post;

    /**
     * Constructor for the {@link APrintWhen} class.
     *
     * @param pre The 'pre' condition of the 'when', or {@code null} if not available.
     * @param post The 'post' condition of the 'when', or {@code null} if not available.
     */
    public APrintWhen(AExpression pre, AExpression post) {
        super(null);
        this.pre = pre;
        this.post = post;
        Assert.check(pre != null || post != null);
    }
}
