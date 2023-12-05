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

package org.eclipse.escet.cif.datasynth.varorder.helper;

/**
 * The kind of representations of the relations between synthesis variables, derived from the CIF specification, that
 * variable ordering algorithms may operate on.
 */
public enum RepresentationKind {
    /** Graph representation. */
    GRAPH,

    /** Hyper-edges representation. */
    HYPER_EDGES,
}
