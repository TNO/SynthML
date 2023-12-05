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

package org.eclipse.escet.cif.datasynth.varorder.graph.algos;

/** Tests for {@link GeorgeLiuPseudoPeripheralNodeFinder}. */
public class GeorgeLiuPseudoPeripheralNodeFinderTest extends PseudoPeripheralNodeFinderTest {
    @Override
    protected PseudoPeripheralNodeFinder createPseudoPeripheralNodeFinder() {
        return new GeorgeLiuPseudoPeripheralNodeFinder();
    }
}
