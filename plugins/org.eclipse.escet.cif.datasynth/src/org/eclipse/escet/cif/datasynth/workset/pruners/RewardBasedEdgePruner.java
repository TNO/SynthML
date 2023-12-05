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

package org.eclipse.escet.cif.datasynth.workset.pruners;

import java.util.BitSet;

import org.eclipse.escet.common.java.BitSets;

/** Edge pruner that employs rewards to prune the edges. */
public class RewardBasedEdgePruner extends EdgePruner {
    /** Per edge, the current cumulative reward. */
    private final int[] rewards;

    /** The reward in case the edge had an effect. */
    private final int effectReward;

    /** The reward in case the edge did not have an effect. */
    private final int noEffectReward;

    /**
     * Constructor for the {@link RewardBasedEdgePruner} class.
     *
     * @param edgeCnt The number of edges.
     * @param effectReward The reward in case the edge had an effect.
     * @param noEffectReward The reward in case the edge did not have an effect.
     */
    public RewardBasedEdgePruner(int edgeCnt, int effectReward, int noEffectReward) {
        this.rewards = new int[edgeCnt];
        this.effectReward = effectReward;
        this.noEffectReward = noEffectReward;
    }

    @Override
    protected BitSet pruneInternal(BitSet workset) {
        // Keep only the edges with the maximum cumulative reward.
        int max = Integer.MIN_VALUE;
        for (int i: BitSets.iterateTrueBits(workset)) {
            int reward = rewards[i];
            if (reward < max) {
                workset.clear(i); // Not the maximum.
            } else if (reward > max) {
                max = reward;
                workset.clear(0, i); // New maximum, so all previous bits are not the maximum (anymore).
            }
        }
        return workset;
    }

    @Override
    public void update(int edgeIdx, boolean hadAnEffect) {
        // Compute the new cumulative reward of the edge.
        long curReward = rewards[edgeIdx];
        curReward += hadAnEffect ? effectReward : noEffectReward;

        // Saturate the reward to prevent wrap-around at overflow.
        if (curReward > Integer.MAX_VALUE) {
            curReward = Integer.MAX_VALUE;
        } else if (curReward < Integer.MIN_VALUE) {
            curReward = Integer.MIN_VALUE;
        }

        // Store the new reward.
        rewards[edgeIdx] = (int)curReward;
    }
}
