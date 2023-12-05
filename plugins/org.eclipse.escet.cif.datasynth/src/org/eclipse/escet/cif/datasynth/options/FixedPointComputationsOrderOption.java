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

package org.eclipse.escet.cif.datasynth.options;

import static org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption.FixedPointComputation.CTRL;
import static org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption.FixedPointComputation.NONBLOCK;
import static org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption.FixedPointComputation.REACH;
import static org.eclipse.escet.common.java.Sets.set;

import java.util.List;

import org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption.FixedPointComputationsOrder;
import org.eclipse.escet.common.app.framework.options.EnumOption;
import org.eclipse.escet.common.app.framework.options.Options;
import org.eclipse.escet.common.java.Assert;

/** Option to specify the order of the fixed-point computations. */
public class FixedPointComputationsOrderOption extends EnumOption<FixedPointComputationsOrder> {
    /** Constructor for the {@link FixedPointComputationsOrderOption} class. */
    public FixedPointComputationsOrderOption() {
        super(
                // name
                "Fixed-point computations order",

                // description
                "Specify the order in which the fixed-point computations are to be performed during synthesis. "
                        + "Three fixed-point computations are performed (depending on other settings), "
                        + "to compute the non-blocking states, controllable states and reachable states. "
                        + "Specify \"nonblock\", \"ctrl\", and \"reach\", in the desired order, joined by dashes. "
                        + "For instance, specify \"ctrl-reach-nonblock\" or \"reach-ctrl-nonblock\". "
                        + "[DEFAULT=nonblock-ctrl-reach]",

                // cmdShort
                null,

                // cmdLong
                "fixed-point-order",

                // cmdValue
                "ORDER",

                // defaultValue
                FixedPointComputationsOrder.NONBLOCK_CTRL_REACH,

                // showInDialog
                true,

                // optDialogDescr
                "Specify the order in which the fixed-point computations are to be performed during synthesis. "
                        + "Three fixed-point computations are performed (depending on other settings), "
                        + "to compute the non-blocking states, controllable states and reachable states.");
    }

    @Override
    protected String getDialogText(FixedPointComputationsOrder order) {
        switch (order) {
            case NONBLOCK_CTRL_REACH:
                return "Non-blocking -> controllable -> reachable";
            case NONBLOCK_REACH_CTRL:
                return "Non-blocking -> reachable -> controllable";
            case CTRL_NONBLOCK_REACH:
                return "Controllable -> non-blocking -> reachable";
            case CTRL_REACH_NONBLOCK:
                return "Controllable -> reachable -> non-blocking";
            case REACH_NONBLOCK_CTRL:
                return "Reachable -> non-blocking -> controllable";
            case REACH_CTRL_NONBLOCK:
                return "Reachable -> controllable -> non-blocking";
        }
        throw new RuntimeException("Unknown order: " + order);
    }

    /**
     * Returns the value of the {@link FixedPointComputationsOrderOption} option.
     *
     * @return The value of the {@link FixedPointComputationsOrderOption} option.
     */
    public static FixedPointComputationsOrder getOrder() {
        return Options.get(FixedPointComputationsOrderOption.class);
    }

    /** Fixed-point computations order. */
    public static enum FixedPointComputationsOrder {
        /** Non-blocking -> controllable -> reachable. */
        NONBLOCK_CTRL_REACH(NONBLOCK, CTRL, REACH),

        /** Non-blocking -> reachable -> controllable. */
        NONBLOCK_REACH_CTRL(NONBLOCK, REACH, CTRL),

        /** Controllable -> non-blocking -> reachable. */
        CTRL_NONBLOCK_REACH(CTRL, NONBLOCK, REACH),

        /** Controllable -> reachable -> non-blocking. */
        CTRL_REACH_NONBLOCK(CTRL, REACH, NONBLOCK),

        /** Reachable -> non-blocking -> controllable. */
        REACH_NONBLOCK_CTRL(REACH, NONBLOCK, CTRL),

        /** Reachable -> controllable -> non-blocking. */
        REACH_CTRL_NONBLOCK(REACH, CTRL, NONBLOCK);

        /** The computations to perform, in the order to perform them. */
        public final List<FixedPointComputation> computations;

        /**
         * Constructor of the {@link FixedPointComputationsOrder} enum.
         *
         * @param computations The computations to perform, in the order to perform them.
         */
        private FixedPointComputationsOrder(FixedPointComputation... computations) {
            Assert.areEqual(FixedPointComputation.values().length, set(computations).size());
            this.computations = List.of(computations);
        }
    }

    /** Fixed-point computation. */
    public static enum FixedPointComputation {
        /** Compute non-blocking states. */
        NONBLOCK,

        /** Compute controllable states. */
        CTRL,

        /** Compute reachable states. */
        REACH,
    }
}
