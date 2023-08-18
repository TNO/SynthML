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

package org.eclipse.escet.cif.parser.ast.types;

import org.eclipse.escet.common.java.TextPosition;

/** Distribution type. */
public class ADistType extends ACifType {
    /** The sample type of the distribution type. */
    public final ACifType sampleType;

    /**
     * Constructor for the {@link ADistType} class.
     *
     * @param sampleType The sample type of the distribution type.
     * @param position Position information.
     */
    public ADistType(ACifType sampleType, TextPosition position) {
        super(position);
        this.sampleType = sampleType;
    }
}
