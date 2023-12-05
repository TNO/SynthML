//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
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

package org.eclipse.escet.cif.datasynth.varorder.parser.ast;

import java.util.List;

import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.TextPosition;

/** Multiple variable orderer instances. */
public class VarOrdererMultiInstance extends VarOrdererInstance {
    /** The variable orderer instances. At least one. */
    public final List<VarOrdererInstance> instances;

    /**
     * Constructor for the {@link VarOrdererMultiInstance} class.
     *
     * @param position The position of the variable orderer instance(s).
     * @param instances The variable orderer instances. At least one.
     */
    public VarOrdererMultiInstance(TextPosition position, List<VarOrdererInstance> instances) {
        super(position);
        this.instances = instances;
        Assert.check(!instances.isEmpty());
    }
}
