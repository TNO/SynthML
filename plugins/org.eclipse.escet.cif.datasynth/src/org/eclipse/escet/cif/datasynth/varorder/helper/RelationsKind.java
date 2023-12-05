//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
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

import org.eclipse.escet.cif.datasynth.varorder.hyperedges.LegacyHyperEdgeCreator;
import org.eclipse.escet.cif.datasynth.varorder.hyperedges.LinearizedHyperEdgeCreator;

/** The kind of relations from the CIF specification to use. */
public enum RelationsKind {
    /** Use {@link LegacyHyperEdgeCreator legacy} relations. */
    LEGACY,

    /** Use {@link LinearizedHyperEdgeCreator linear} relations. */
    LINEARIZED;
}
