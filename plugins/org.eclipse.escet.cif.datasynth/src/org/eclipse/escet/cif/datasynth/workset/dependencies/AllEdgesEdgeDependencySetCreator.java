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

package org.eclipse.escet.cif.datasynth.workset.dependencies;

import static org.eclipse.escet.common.java.Lists.listc;

import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.common.java.BitSets;

/**
 * An edge dependency set creator that for each edge has all edges as dependencies. It is trivially correct, but has the
 * worst possible performance.
 */
public class AllEdgesEdgeDependencySetCreator implements EdgeDependencySetCreator {
    @Override
    public void createAndStore(SynthesisAutomaton synthAut, boolean forwardEnabled) {
        // Backward.
        synthAut.worksetDependenciesBackward = listc(synthAut.orderedEdgesBackward.size());
        for (int i = 0; i < synthAut.orderedEdgesBackward.size(); i++) {
            synthAut.worksetDependenciesBackward.add(BitSets.ones(synthAut.orderedEdgesBackward.size()));
        }

        // Forward.
        if (forwardEnabled) {
            synthAut.worksetDependenciesForward = listc(synthAut.orderedEdgesForward.size());
            for (int i = 0; i < synthAut.orderedEdgesForward.size(); i++) {
                synthAut.worksetDependenciesForward.add(BitSets.ones(synthAut.orderedEdgesForward.size()));
            }
        }
    }
}
