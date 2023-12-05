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

/** Pseudo-peripheral node finder algorithm kind. */
public enum PseudoPeripheralNodeFinderKind {
    /** Pseudo-peripheral node finder algorithm by George and Liu. */
    GEORGE_LIU,

    /** Pseudo-peripheral node pair finder algorithm by Sloan. */
    SLOAN;

    /**
     * Create an instance of the node finder algorithm for this node finder algorithm kind.
     *
     * @return The node finder algorithm.
     */
    public PseudoPeripheralNodeFinder create() {
        switch (this) {
            case GEORGE_LIU:
                return new GeorgeLiuPseudoPeripheralNodeFinder();
            case SLOAN:
                return new SloanPseudoPeripheralNodeFinder();
        }
        throw new RuntimeException("Unknown node finder algorithm: " + this);
    }
}
