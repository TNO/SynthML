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

package org.eclipse.escet.cif.datasynth.options;

/** Potential BDD predicate simplifications that can be performed. */
public enum BddSimplify {
    /** Supervisor guards of controllable events wrt their plant guards. */
    GUARDS_PLANTS,

    /**
     * Supervisor guards of controllable events wrt state/event exclusion requirement invariants derived from the
     * requirement automata.
     */
    GUARDS_REQ_AUTS,

    /**
     * Supervisor guards of controllable events wrt state/event exclusion plant invariants from the input specification.
     */
    GUARDS_SE_EXCL_PLANT_INVS,

    /**
     * Supervisor guards of controllable events wrt state/event exclusion requirement invariants from the input
     * specification.
     */
    GUARDS_SE_EXCL_REQ_INVS,

    /** Supervisor guards of controllable events wrt state plant invariants from the input specification. */
    GUARDS_STATE_PLANT_INVS,

    /** Supervisor guards of controllable events wrt state requirement invariants from the input specification. */
    GUARDS_STATE_REQ_INVS,

    /** Supervisor guards of controllable events wrt controlled behavior as computed by synthesis. */
    GUARDS_CTRL_BEH,

    /**
     * Initialization predicate of the controlled system wrt the initialization predicate of the uncontrolled system.
     */
    INITIAL_UNCTRL,

    /** Initialization predicate of the controlled system wrt the state plant invariants. */
    INITIAL_STATE_PLANT_INVS;
}
