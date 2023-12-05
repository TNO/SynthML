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

package org.eclipse.escet.cif.datasynth;

import static org.eclipse.escet.cif.datasynth.bdd.BddUtils.bddToStr;
import static org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption.FixedPointComputation.CTRL;
import static org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption.FixedPointComputation.REACH;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.dbg;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.out;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.warn;
import static org.eclipse.escet.common.java.Lists.concat;
import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Maps.mapc;
import static org.eclipse.escet.common.java.Sets.setc;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.datasynth.bdd.BddUtils;
import org.eclipse.escet.cif.datasynth.options.BddSimplify;
import org.eclipse.escet.cif.datasynth.options.BddSimplifyOption;
import org.eclipse.escet.cif.datasynth.options.EdgeWorksetAlgoOption;
import org.eclipse.escet.cif.datasynth.options.EventWarnOption;
import org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption;
import org.eclipse.escet.cif.datasynth.options.FixedPointComputationsOrderOption.FixedPointComputation;
import org.eclipse.escet.cif.datasynth.options.ForwardReachOption;
import org.eclipse.escet.cif.datasynth.options.StateReqInvEnforceOption;
import org.eclipse.escet.cif.datasynth.options.StateReqInvEnforceOption.StateReqInvEnforceMode;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.datasynth.spec.SynthesisDiscVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisEdge;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.workset.dependencies.BddBasedEdgeDependencySetCreator;
import org.eclipse.escet.cif.datasynth.workset.dependencies.EdgeDependencySetCreator;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.app.framework.exceptions.InvalidInputException;
import org.eclipse.escet.common.box.GridBox;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.BitSets;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.escet.common.java.Stopwatch;
import org.eclipse.escet.common.java.Strings;

import com.github.javabdd.BDD;

/** CIF data-based supervisory controller synthesis. */
public class CifDataSynthesis {
    /** Constructor for the {@link CifDataSynthesis} class. */
    private CifDataSynthesis() {
        // Static class.
    }

    /**
     * Performs data-based supervisory controller synthesis.
     *
     * @param aut The automaton on which to perform synthesis. Is updated to represent the result of synthesis.
     * @param dbgEnabled Whether debug output is enabled.
     * @param doTiming Whether to collect timing statistics.
     * @param timing The timing statistics data. Is modified in-place.
     * @param doPrintCtrlSysStates Whether to print controlled system states statistics.
     */
    public static void synthesize(SynthesisAutomaton aut, boolean dbgEnabled, boolean doTiming,
            CifDataSynthesisTiming timing, boolean doPrintCtrlSysStates)
    {
        // Algorithm is based on the following paper: Lucien Ouedraogo, Ratnesh Kumar, Robi Malik, and Knut Åkesson:
        // Nonblocking and Safe Control of Discrete-Event Systems Modeled as Extended Finite Automata, IEEE Transactions
        // on Automation Science and Engineering, Volume 8, Issue 3, Pages 560-569, July 2011.

        // Configuration.
        boolean doForward = ForwardReachOption.isEnabled();

        // Pre synthesis.
        if (doTiming) {
            timing.preSynth.start();
        }
        try {
            // Check system, and print debug information.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            checkSystem(aut, dbgEnabled);

            // Apply state/event exclusion plant invariants.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            applyStateEvtExclPlants(aut, dbgEnabled);

            // Initialize applying edges.
            for (SynthesisEdge edge: aut.edges) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }
                edge.initApply(doForward);
            }

            // Apply state plant invariants if there are any.
            if (!aut.plantInv.isOne()) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }
                applyStatePlantInvs(aut, dbgEnabled);
            }

            // Initialize controlled behavior.
            if (aut.env.isTerminationRequested()) {
                return;
            }

            aut.ctrlBeh = aut.factory.one();
            aut.initialCtrl = aut.initialPlantInv.id();

            if (dbgEnabled) {
                dbg();
                dbg("Initialized controlled-behavior predicate: %s.", bddToStr(aut.ctrlBeh, aut));
                dbg("Initialized controlled-initialization predicate: %s.", bddToStr(aut.initialCtrl, aut));
            }

            // Apply requirements.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            applyStateReqInvs(aut, dbgEnabled);

            if (aut.env.isTerminationRequested()) {
                return;
            }
            applyVarRanges(aut, dbgEnabled);

            if (aut.env.isTerminationRequested()) {
                return;
            }
            applyStateEvtExclReqs(aut, dbgEnabled);

            // Re-initialize applying edges after applying the state plant invariants, state requirement invariants
            // (depending on options), and state/event exclusion requirement invariants.
            for (SynthesisEdge edge: aut.edges) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }
                edge.reinitApply(doForward);
            }

            // Check edges.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            if (EventWarnOption.isEnabled()) {
                checkInputEdges(aut);
            }

            // Prepare workset algorithm, if enabled.
            if (EdgeWorksetAlgoOption.isEnabled()) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }
                prepareWorksetAlgorithm(aut, dbgEnabled);
            }
        } finally {
            if (doTiming) {
                timing.preSynth.stop();
            }
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }

        // Perform actual synthesis, using fixed point calculations.
        if (doTiming) {
            timing.main.start();
        }
        try {
            if (aut.env.isTerminationRequested()) {
                return;
            }
            synthesizeFixedPoints(aut, doForward, dbgEnabled, doTiming, timing);
            aut.marked.free();
            aut.marked = null;
        } finally {
            if (doTiming) {
                timing.main.stop();
            }
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }

        // Post synthesis.
        if (doTiming) {
            timing.postSynth.start();
        }
        try {
            // Determine controlled system guards.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            determineCtrlSysGuards(aut, dbgEnabled);

            // Done with actual synthesis. May no longer apply edges from here on.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            for (SynthesisEdge edge: aut.edges) {
                edge.cleanupApply();
            }

            // Result of synthesis is in the automaton.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            if (dbgEnabled) {
                dbg();
                dbg("Final synthesis result:");
                dbg(aut.toString(1));
            }

            // Determine controlled system initialization predicate.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            determineCtrlSysInit(aut);

            // Check whether an initial state is present, or the supervisor is empty.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            boolean emptySup = !checkInitStatePresent(aut);

            // Statistics: number of states in controlled system.
            if (doPrintCtrlSysStates) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }
                printNumberStates(aut, emptySup, doForward, dbgEnabled);
            }

            // Determine the output of synthesis (1/2).
            if (aut.env.isTerminationRequested()) {
                return;
            }
            determineOutputInitial(aut, dbgEnabled);

            // Fail if supervisor is empty.
            if (emptySup) {
                throw new InvalidInputException("Empty supervisor.");
            }

            // Determine the guards for the controllable events.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            Map<Event, BDD> ctrlGuards = determineGuards(aut, aut.controllables, false);

            // Check edges.
            if (EventWarnOption.isEnabled()) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }
                checkOutputEdges(aut, ctrlGuards);
            }

            // Determine the output of synthesis (2/2).
            if (aut.env.isTerminationRequested()) {
                return;
            }
            determineOutputGuards(aut, ctrlGuards, dbgEnabled);

            // Separate debug output from what is to come.
            if (dbgEnabled) {
                dbg();
            }
        } finally {
            if (doTiming) {
                timing.postSynth.stop();
            }
        }
    }

    /**
     * Checks the system for problems with initialization, marking, and state invariants. Also prints related debug
     * information.
     *
     * @param aut The automaton on which to perform synthesis.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void checkSystem(SynthesisAutomaton aut, boolean dbgEnabled) {
        // Debug state plant invariants (predicates) of the components.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg();
            for (BDD pred: aut.plantInvsComps) {
                dbg("Invariant (component state plant invariant): %s", bddToStr(pred, aut));
            }
            dbg("Invariant (components state plant inv):      %s", bddToStr(aut.plantInvComps, aut));
        }

        // Debug state plant invariants (predicates) of the locations of the automata.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            for (BDD pred: aut.plantInvsLocs) {
                dbg("Invariant (location state plant invariant):  %s", bddToStr(pred, aut));
            }
            dbg("Invariant (locations state plant invariant): %s", bddToStr(aut.plantInvLocs, aut));
        }

        // Debug state plant invariant (predicate) of the system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Invariant (system state plant invariant):    %s", bddToStr(aut.plantInv, aut));
        }

        // Warn if no state in system, due to state plant invariants.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (aut.plantInv.isZero()) {
            warn("The uncontrolled system has no states (taking into account only the state plant invariants).");
        }

        // Debug state requirement invariants (predicates) of the components.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg();
            for (BDD pred: aut.reqInvsComps) {
                dbg("Invariant (component state req invariant):   %s", bddToStr(pred, aut));
            }
            dbg("Invariant (components state req invariant):  %s", bddToStr(aut.reqInvComps, aut));
        }

        // Debug state requirement invariants (predicates) of the locations of the automata.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            for (BDD pred: aut.reqInvsLocs) {
                dbg("Invariant (location state req invariant):    %s", bddToStr(pred, aut));
            }
            dbg("Invariant (locations state req invariant):   %s", bddToStr(aut.reqInvLocs, aut));
        }

        // Debug state requirement invariant (predicate) of the system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Invariant (system state req invariant):      %s", bddToStr(aut.reqInv, aut));
        }

        // Warn if no state in system, due to state requirement invariants.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (aut.reqInv.isZero()) {
            warn("The controlled system has no states (taking into account only the state requirement invariants).");
        }

        // Debug initialization predicates of the discrete variables.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg();
            for (int i = 0; i < aut.variables.length; i++) {
                SynthesisVariable var = aut.variables[i];
                if (!(var instanceof SynthesisDiscVariable)) {
                    continue;
                }

                String nr = String.valueOf(i);
                dbg("Initial   (discrete variable %s):%s%s", nr, Strings.spaces(14 - nr.length()),
                        bddToStr(aut.initialsVars.get(i), aut));
            }

            dbg("Initial   (discrete variables):              %s", bddToStr(aut.initialVars, aut));
        }

        // Debug initialization predicates of the components.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            for (BDD pred: aut.initialsComps) {
                dbg("Initial   (component init predicate):        %s", bddToStr(pred, aut));
            }
            dbg("Initial   (components init predicate):       %s", bddToStr(aut.initialComps, aut));
        }

        // Debug initialization predicates of the locations of the automata.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            for (BDD pred: aut.initialsLocs) {
                dbg("Initial   (aut/locs init predicate):         %s", bddToStr(pred, aut));
            }
            dbg("Initial   (auts/locs init predicate):        %s", bddToStr(aut.initialLocs, aut));
        }

        // Debug initialization predicate of the uncontrolled system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Initial   (uncontrolled system):             %s", bddToStr(aut.initialUnctrl, aut));
        }

        // Debug combined initialization and state plant invariants of the system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Initial   (system, combined init/plant inv): %s", bddToStr(aut.initialPlantInv, aut));
        }

        // Debug combined initialization and state invariants of the system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Initial   (system, combined init/state inv): %s", bddToStr(aut.initialInv, aut));
        }

        // Warn if no initial state in uncontrolled system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (aut.initialUnctrl.isZero()) {
            warn("The uncontrolled system has no initial state (taking into account only initialization).");
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (!aut.initialUnctrl.isZero() && !aut.plantInv.isZero() && aut.initialPlantInv.isZero()) {
            warn("The uncontrolled system has no initial state (taking into account only initialization and state "
                    + "plant invariants).");
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (!aut.initialPlantInv.isZero() && !aut.initialUnctrl.isZero() && !aut.plantInv.isZero()
                && !aut.reqInv.isZero() && aut.initialInv.isZero())
        {
            warn("The controlled system has no initial state (taking into account both initialization and state "
                    + "invariants).");
        }

        // Debug marker predicates of the components.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg();
            for (BDD pred: aut.markedsComps) {
                dbg("Marked    (component marker predicate):      %s", bddToStr(pred, aut));
            }
            dbg("Marked    (components marker predicate):     %s", bddToStr(aut.markedComps, aut));
        }

        // Debug marker predicates of the locations of the automata.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            for (BDD pred: aut.markedsLocs) {
                dbg("Marked    (aut/locs marker predicate):       %s", bddToStr(pred, aut));
            }
            dbg("Marked    (auts/locs marker predicate):      %s", bddToStr(aut.markedLocs, aut));
        }

        // Debug marker predicate of the uncontrolled system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Marked    (uncontrolled system):             %s", bddToStr(aut.marked, aut));
        }

        // Debug combined marking and state plant invariants of the system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Marked    (system, combined mark/plant inv): %s", bddToStr(aut.markedPlantInv, aut));
        }

        // Debug combined marking and state invariants of the system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg("Marked    (system, combined mark/state inv): %s", bddToStr(aut.markedInv, aut));
        }

        // Warn if no marked state in uncontrolled system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (aut.marked.isZero()) {
            warn("The uncontrolled system has no marked state (taking into account only marking).");
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (!aut.marked.isZero() && !aut.plantInv.isZero() && aut.markedPlantInv.isZero()) {
            warn("The uncontrolled system has no marked state (taking into account only marking and state plant "
                    + "invariants).");
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (!aut.markedPlantInv.isZero() && !aut.marked.isZero() && !aut.plantInv.isZero() && !aut.reqInv.isZero()
                && aut.markedInv.isZero())
        {
            warn("The controlled system has no marked state (taking into account both marking and state invariants).");
        }

        // Debug state/event exclusion plants.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg();
            dbg("State/event exclusion plants:");
            if (aut.stateEvtExclPlantLists.values().stream().flatMap(x -> x.stream()).findAny().isEmpty()) {
                dbg("  None");
            }
            for (Entry<Event, List<BDD>> entry: aut.stateEvtExclPlantLists.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                dbg("  Event \"%s\" needs:", CifTextUtils.getAbsName(entry.getKey()));
                for (BDD pred: entry.getValue()) {
                    dbg("    %s", bddToStr(pred, aut));
                }
            }
        }

        // Debug state/event exclusion requirements.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg();
            dbg("State/event exclusion requirements:");
            if (aut.stateEvtExclReqLists.values().stream().flatMap(x -> x.stream()).findAny().isEmpty()) {
                dbg("  None");
            }
            for (Entry<Event, List<BDD>> entry: aut.stateEvtExclReqLists.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                dbg("  Event \"%s\" needs:", CifTextUtils.getAbsName(entry.getKey()));
                for (BDD pred: entry.getValue()) {
                    dbg("    %s", bddToStr(pred, aut));
                }
            }
        }

        // Debug automaton.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled) {
            dbg();
            if (aut.stateEvtExclPlantLists.values().stream().flatMap(x -> x.stream()).findAny().isEmpty()) {
                dbg("Uncontrolled system:");
            } else {
                dbg("Uncontrolled system (state/event exclusion plants not applied yet):");
            }
            dbg(aut.toString(1));
        }

        // Free no longer needed predicates.
        for (BDD bdd: aut.plantInvsComps) {
            bdd.free();
        }
        for (BDD bdd: aut.plantInvsLocs) {
            bdd.free();
        }
        aut.plantInvComps.free();
        aut.plantInvLocs.free();

        if (StateReqInvEnforceOption.getMode() == StateReqInvEnforceMode.ALL_CTRL_BEH) {
            for (BDD bdd: aut.reqInvsComps) {
                bdd.free();
            }
            for (BDD bdd: aut.reqInvsLocs) {
                bdd.free();
            }
        }
        aut.reqInvComps.free();
        aut.reqInvLocs.free();

        for (BDD bdd: aut.initialsVars) {
            if (bdd != null) {
                bdd.free();
            }
        }
        for (BDD bdd: aut.initialsComps) {
            bdd.free();
        }
        for (BDD bdd: aut.initialsLocs) {
            bdd.free();
        }
        aut.initialVars.free();
        aut.initialComps.free();
        aut.initialLocs.free();
        aut.initialInv.free();

        for (BDD bdd: aut.markedsComps) {
            bdd.free();
        }
        for (BDD bdd: aut.markedsLocs) {
            bdd.free();
        }
        aut.markedComps.free();
        aut.markedLocs.free();
        aut.markedPlantInv.free();
        aut.markedInv.free();

        for (List<BDD> preds: aut.stateEvtExclPlantLists.values()) {
            for (BDD pred: preds) {
                pred.free();
            }
        }

        for (List<BDD> preds: aut.stateEvtExclReqLists.values()) {
            for (BDD pred: preds) {
                pred.free();
            }
        }

        aut.plantInvsComps = null;
        aut.plantInvComps = null;
        aut.plantInvsLocs = null;
        aut.plantInvLocs = null;

        if (StateReqInvEnforceOption.getMode() == StateReqInvEnforceMode.ALL_CTRL_BEH) {
            aut.reqInvsComps = null;
            aut.reqInvsLocs = null;
        }
        aut.reqInvComps = null;
        aut.reqInvLocs = null;

        aut.initialsVars = null;
        aut.initialVars = null;
        aut.initialsComps = null;
        aut.initialComps = null;
        aut.initialsLocs = null;
        aut.initialLocs = null;
        aut.initialInv = null;

        aut.markedsComps = null;
        aut.markedComps = null;
        aut.markedsLocs = null;
        aut.markedLocs = null;
        aut.markedPlantInv = null;
        aut.markedInv = null;

        aut.stateEvtExclPlantLists = null;
        aut.stateEvtExclReqLists = null;
    }

    /**
     * Applies the state/event exclusion plant invariants, as preprocessing step for synthesis.
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void applyStateEvtExclPlants(SynthesisAutomaton aut, boolean dbgEnabled) {
        // Update guards to ensure that transitions not allowed by the state/event exclusion plant invariants, are
        // blocked.
        if (dbgEnabled) {
            dbg();
            dbg("Restricting behavior using state/event exclusion plants.");
        }

        boolean firstDbg = true;
        boolean guardChanged = false;
        for (SynthesisEdge edge: aut.edges) {
            // Get additional condition for the edge. Skip for internal events that are not in the original
            // specification and for trivially true conditions.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            BDD plant = aut.stateEvtExclPlants.get(edge.event);
            if (plant == null || plant.isOne() || edge.guard.isZero()) {
                continue;
            }

            // Enforce the additional condition by restricting the guard.
            BDD newGuard = edge.guard.and(plant);
            if (aut.env.isTerminationRequested()) {
                return;
            }

            if (edge.guard.equals(newGuard)) {
                newGuard.free();
            } else {
                if (dbgEnabled) {
                    if (firstDbg) {
                        firstDbg = false;
                        dbg();
                    }
                    dbg("Edge %s: guard: %s -> %s [plant: %s].", edge.toString(0, ""), bddToStr(edge.guard, aut),
                            bddToStr(newGuard, aut), bddToStr(plant, aut));
                }
                edge.guard.free();
                edge.guard = newGuard;
                guardChanged = true;
            }
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled && guardChanged) {
            dbg();
            dbg("Uncontrolled system:");
            dbg(aut.toString(1, guardChanged));
        }
    }

    /**
     * Applies the state plant invariants, as preprocessing step for synthesis.
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void applyStatePlantInvs(SynthesisAutomaton aut, boolean dbgEnabled) {
        if (dbgEnabled) {
            dbg();
            dbg("Restricting uncontrolled behavior using state plant invariants.");
        }

        boolean guardUpdated = false;
        for (SynthesisEdge edge: aut.edges) {
            if (aut.env.isTerminationRequested()) {
                return;
            }

            // The guards of the edge are restricted such that transitioning to a state that violates the plant
            // invariants is not possible. The update to the predicate is obtained by applying the edge's update
            // backward to the state plant invariant.
            BDD updPred = aut.plantInv.id();
            edge.preApply(false, null);
            updPred = edge.apply(updPred, // pred
                    false, // bad
                    false, // forward
                    null, // restriction
                    false); // don't apply error. The supervisor should restrict that.
            edge.postApply(false);

            if (aut.env.isTerminationRequested()) {
                return;
            }

            // Compute 'guard and plantInv => updPred'. If the backwards applied state plant invariant is already
            // implied by the current guard and state plant invariant in the source state, we don't need to strengthen
            // the guard. In that case, replace the predicate by 'true', to not add any restriction.
            BDD guardAndPlantInv = edge.guard.and(aut.plantInv);
            BDD implication = guardAndPlantInv.imp(updPred);
            boolean skip = implication.isOne();
            guardAndPlantInv.free();
            implication.free();
            if (skip) {
                updPred.free();
                updPred = aut.factory.one();
            }

            if (aut.env.isTerminationRequested()) {
                return;
            }

            // Store.
            BDD newGuard = edge.guard.id().andWith(updPred);
            if (aut.env.isTerminationRequested()) {
                return;
            }

            if (edge.guard.equals(newGuard)) {
                newGuard.free();
            } else {
                if (dbgEnabled) {
                    if (!guardUpdated) {
                        dbg();
                    }
                    dbg("Edge %s: guard: %s -> %s.", edge.toString(0, ""), bddToStr(edge.guard, aut),
                            bddToStr(newGuard, aut));
                }
                edge.guard.free();
                edge.guard = newGuard;
                guardUpdated = true;
            }
        }
    }

    /**
     * Applies the state requirement invariants, as preprocessing step for synthesis.
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void applyStateReqInvs(SynthesisAutomaton aut, boolean dbgEnabled) {
        // Apply the state requirement invariants.
        if (dbgEnabled) {
            dbg();
            dbg("Restricting behavior using state requirements.");
        }

        StateReqInvEnforceMode enforceMode = StateReqInvEnforceOption.getMode();
        switch (enforceMode) {
            case ALL_CTRL_BEH: {
                // Add the invariants to the controlled-behavior predicate. This ensures that a state is only in the
                // controlled system if the state requirement invariants hold.
                BDD newCtrlBeh = aut.ctrlBeh.and(aut.reqInv);
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                if (aut.ctrlBeh.equals(newCtrlBeh)) {
                    newCtrlBeh.free();
                } else {
                    if (dbgEnabled) {
                        dbg("Controlled behavior: %s -> %s [state requirements: %s].", bddToStr(aut.ctrlBeh, aut),
                                bddToStr(newCtrlBeh, aut), bddToStr(aut.reqInv, aut));
                    }
                    aut.ctrlBeh.free();
                    aut.ctrlBeh = newCtrlBeh;
                }
                break;
            }
            case PER_EDGE: {
                // Apply state requirement invariants, per edge.
                List<BDD> reqInvs = concat(aut.reqInvsComps, aut.reqInvsLocs);
                Function<SynthesisEdge, Stream<BDD>> reqsPerEdge = edge -> reqInvs.stream().map(reqInv -> {
                    // Apply the requirement invariant backwards through the edge, to obtain a requirement condition
                    // that must hold for the edge to be taken. This condition further restricts the edge to prevent
                    // transitioning to a state that violates the requirement invariant.
                    BDD updPred = reqInv.id();
                    edge.preApply(false, null);
                    updPred = edge.apply(updPred, // pred
                            false, // bad
                            false, // forward
                            null, // restriction
                            false); // don't apply error. The supervisor should restrict that.
                    edge.postApply(false);

                    // If trivial, we have the final predicate.
                    if (updPred.isOne()) {
                        return updPred;
                    }

                    // If termination is requested, we won't do any extra work on the predicate, as it won't be used.
                    if (aut.env.isTerminationRequested()) {
                        return updPred;
                    }

                    // Compute 'guard and reqInv => updPred'. If the backwards applied state requirement invariant is
                    // already implied by the current guard and state requirement invariant in the source state, we
                    // don't need to strengthen the guard. In that case, replace the predicate by 'true', to ensure it
                    // gets ignored when it is applied.
                    BDD guardAndReqInv = edge.guard.and(reqInv);
                    BDD implication = guardAndReqInv.imp(updPred);
                    boolean skip = implication.isOne();
                    guardAndReqInv.free();
                    implication.free();
                    if (skip) {
                        updPred.free();
                        updPred = aut.factory.one();
                    }

                    // Return the predicate to apply per edge.
                    return updPred;
                });
                applyReqsPerEdge(aut, reqsPerEdge, true, dbgEnabled, "state");

                // Restrict the initialization predicate of the controlled system, allowing only states that satisfy
                // the state requirement invariants.
                BDD newInitialCtrl = aut.initialCtrl.and(aut.reqInv);
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                if (aut.initialCtrl.equals(newInitialCtrl)) {
                    newInitialCtrl.free();
                } else {
                    if (dbgEnabled) {
                        dbg("Controlled initialization: %s -> %s [state requirements: %s].",
                                bddToStr(aut.initialCtrl, aut), bddToStr(newInitialCtrl, aut),
                                bddToStr(aut.reqInv, aut));
                    }
                    aut.initialCtrl.free();
                    aut.initialCtrl = newInitialCtrl;
                }

                // Cleanup.
                for (BDD bdd: aut.reqInvsComps) {
                    bdd.free();
                }
                for (BDD bdd: aut.reqInvsLocs) {
                    bdd.free();
                }
                aut.reqInvsComps = null;
                aut.reqInvsLocs = null;

                break;
            }
            default:
                throw new RuntimeException("Unknown mode: " + enforceMode);
        }
    }

    /**
     * Ensure that variables stay within their range, i.e. ensure no out of bounds values are ever used, as
     * preprocessing step for synthesis. This is done by enforcing that variables stay within their
     * {@link SynthesisVariable#domain domains}.
     *
     * <p>
     * As an example, consider a variable 'x' of type 'int[0..2]'. Two BDD variables are used to represent the integer
     * CIF variable. The two BDD variables can represent 2^2=4 values, i.e. they can represent values of type
     * 'int[0..3]'. Value '3' can be represented, but is not a valid value of the variable, and should thus not be used.
     * The predicate that represents the value range is {@code 0 <= x <= 2}. This predicate is added to the
     * controlled-behavior predicate.
     * </p>
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void applyVarRanges(SynthesisAutomaton aut, boolean dbgEnabled) {
        if (dbgEnabled) {
            dbg();
            dbg("Extending controlled-behavior predicate using variable ranges.");
        }

        boolean firstDbg = true;
        boolean changed = false;
        for (SynthesisVariable var: aut.variables) {
            if (aut.env.isTerminationRequested()) {
                return;
            }

            // Compute out of range predicate.
            BDD range = BddUtils.getVarDomain(var, false, aut.factory);
            if (aut.env.isTerminationRequested()) {
                return;
            }

            // Update controlled-behavior predicate.
            BDD newCtrlBeh = aut.ctrlBeh.and(range);
            if (aut.env.isTerminationRequested()) {
                return;
            }

            if (aut.ctrlBeh.equals(newCtrlBeh)) {
                newCtrlBeh.free();
                range.free();
            } else {
                if (dbgEnabled) {
                    if (firstDbg) {
                        firstDbg = false;
                        dbg();
                    }
                    dbg("Controlled behavior: %s -> %s [range: %s, variable: %s].", bddToStr(aut.ctrlBeh, aut),
                            bddToStr(newCtrlBeh, aut), bddToStr(range, aut), var.toString(0, ""));
                }
                range.free();
                aut.ctrlBeh.free();
                aut.ctrlBeh = newCtrlBeh;
                changed = true;
            }
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled && changed) {
            dbg();
            dbg("Extended controlled-behavior predicate using variable ranges: %s.", bddToStr(aut.ctrlBeh, aut));
        }
    }

    /**
     * Applies the state/event exclusion requirement invariants, as preprocessing step for synthesis.
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void applyStateEvtExclReqs(SynthesisAutomaton aut, boolean dbgEnabled) {
        // Update guards and controlled-behavior predicate, to ensure that transitions not allowed by the state/event
        // exclusion requirement invariants, are blocked.
        if (dbgEnabled) {
            dbg();
            dbg("Restricting behavior using state/event exclusion requirements.");
        }

        // Apply state/event exclusion requirement invariants, per edge.
        Function<SynthesisEdge, Stream<BDD>> reqsPerEdge = edge -> {
            BDD req = aut.stateEvtExclReqs.get(edge.event);
            return (req == null) ? Stream.empty() : Stream.of(req);
        };
        applyReqsPerEdge(aut, reqsPerEdge, false, dbgEnabled, "state/event exclusion");

        // Free no longer needed predicates.
        for (BDD bdd: aut.stateEvtExclReqs.values()) {
            bdd.free();
        }
        aut.stateEvtExclReqs = null;
    }

    /**
     * Apply requirements, per edge.
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param reqsPerEdge Function that gives per edge a stream of requirements to apply.
     * @param freeReqs Whether to free the requirements after they are applied ({@code true}) or not ({@code false}).
     * @param dbgEnabled Whether debug output is enabled.
     * @param dbgDescription Description of the kind of requirements that are applied.
     */
    private static void applyReqsPerEdge(SynthesisAutomaton aut, Function<SynthesisEdge, Stream<BDD>> reqsPerEdge,
            boolean freeReqs, boolean dbgEnabled, String dbgDescription)
    {
        boolean firstDbg = true;
        boolean changed = false;
        boolean guardChanged = false;
        for (SynthesisEdge edge: aut.edges) {
            // Get requirements for the edge.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            Stream<BDD> reqsStream = reqsPerEdge.apply(edge);
            Iterable<BDD> reqsIterable = () -> reqsStream.iterator();

            // Process each requirement.
            for (BDD req: reqsIterable) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                // Skip trivially true requirements.
                if (req.isOne()) {
                    continue;
                }

                // Enforce the additional condition.
                if (edge.event.getControllable()) {
                    // For controllable events, we can simply restrict the guard.
                    BDD newGuard = edge.guard.and(req);
                    if (aut.env.isTerminationRequested()) {
                        return;
                    }

                    if (edge.guard.equals(newGuard)) {
                        newGuard.free();
                    } else {
                        if (dbgEnabled) {
                            if (firstDbg) {
                                firstDbg = false;
                                dbg();
                            }
                            dbg("Edge %s: guard: %s -> %s [%s requirement: %s].", edge.toString(0, ""),
                                    bddToStr(edge.guard, aut), bddToStr(newGuard, aut), dbgDescription,
                                    bddToStr(req, aut));
                        }
                        edge.guard.free();
                        edge.guard = newGuard;
                        changed = true;
                        guardChanged = true;
                    }
                } else {
                    // For uncontrollable events, update the controlled-behavior predicate. If the guard of the edge
                    // holds (event enabled in the plant), and the requirement condition doesn't hold (event disabled
                    // by the requirement), the edge may not be taken.
                    //
                    // reqBad = guard && !req
                    // reqGood = !(guard && !req) = !guard || req = guard => req
                    //
                    // Only good states in controlled behavior. So restrict controlled behavior with 'reqGood'.
                    BDD reqGood = edge.guard.imp(req);
                    if (aut.env.isTerminationRequested()) {
                        return;
                    }

                    BDD newCtrlBeh = aut.ctrlBeh.id().andWith(reqGood);
                    if (aut.env.isTerminationRequested()) {
                        return;
                    }

                    if (aut.ctrlBeh.equals(newCtrlBeh)) {
                        newCtrlBeh.free();
                    } else {
                        if (dbgEnabled) {
                            if (firstDbg) {
                                firstDbg = false;
                                dbg();
                            }
                            dbg("Controlled behavior: %s -> %s [%s requirement: %s, edge: %s].",
                                    bddToStr(aut.ctrlBeh, aut), bddToStr(newCtrlBeh, aut), dbgDescription,
                                    bddToStr(req, aut), edge.toString(0, ""));
                        }
                        aut.ctrlBeh.free();
                        aut.ctrlBeh = newCtrlBeh;
                        changed = true;
                    }
                }

                // Free requirement predicate, if requested.
                if (freeReqs) {
                    req.free();
                }
            }
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (dbgEnabled && changed) {
            dbg();
            dbg("Restricted behavior using %s requirements:", dbgDescription);
            dbg(aut.toString(1, guardChanged));
        }
    }

    /**
     * Checks the system for problems with events that are never enabled in the input specification. Saves the disabled
     * events.
     *
     * @param aut The automaton on which to perform synthesis.
     */
    private static void checkInputEdges(SynthesisAutomaton aut) {
        aut.disabledEvents = setc(aut.alphabet.size());

        for (Event event: aut.alphabet) {
            if (aut.env.isTerminationRequested()) {
                return;
            }

            // Skip events for input variables as they have no edges.
            if (aut.inputVarEvents.contains(event)) {
                continue;
            }

            // Skip events that are in the alphabet, but never on an edge as these are globally disabled. Note, the type
            // checker reports these already.
            if (aut.eventEdges.get(event) == null) {
                aut.disabledEvents.add(event);
                continue;
            }

            // Check whether the combined state/event exclusion plants are 'false'.
            if (aut.stateEvtExclPlants.get(event).isZero()) {
                warn("Event \"%s\" is never enabled in the input specification, taking into account only state/event "
                        + "exclusion plants.", CifTextUtils.getAbsName(event));
                aut.disabledEvents.add(event);
                continue;
            }

            // Check whether the combined state/event exclusion requirements are 'false'.
            if (event.getControllable() && aut.stateEvtExclsReqInvs.get(event).isZero()) {
                warn("Event \"%s\" is never enabled in the input specification, taking into account only state/event "
                        + "exclusion requirements.", CifTextUtils.getAbsName(event));
                aut.disabledEvents.add(event);
                continue;
            }

            // Check whether the guards on edges of automata are all 'false'. There might be multiple edges for an
            // event.
            if (aut.eventEdges.get(event).stream().filter(edge -> !edge.origGuard.isZero()).count() == 0) {
                warn("Event \"%s\" is never enabled in the input specification, taking into account only automaton "
                        + "guards.", CifTextUtils.getAbsName(event));
                aut.disabledEvents.add(event);
                continue;
            }

            // Check whether the guards on edges of automata combined with invariants are all 'false'. There might be
            // multiple edges for an event. State/event exclusion plant invariants are included in the edge guards.
            // State/event exclusion requirement invariants are included in the edge guards for controllable events.
            // State plant invariants and state requirement invariants are sometimes included in the edge guard
            // (it may depend on options, and on whether the edge guard was strengthened). To simplify the implementation and make it
            // more consistent regardless, we always include the state invariants again.
            boolean alwaysDisabled = true;
            for (SynthesisEdge edge: aut.eventEdges.get(event)) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                BDD enabledExpression = edge.guard.and(aut.reqInv);
                enabledExpression = enabledExpression.andWith(aut.plantInv.id());
                if (!enabledExpression.isZero()) {
                    enabledExpression.free();
                    alwaysDisabled = false;
                    break;
                }
            }

            if (alwaysDisabled) {
                warn("Event \"%s\" is never enabled in the input specification, taking into account automaton guards "
                        + "and invariants.", CifTextUtils.getAbsName(event));
                aut.disabledEvents.add(event);
                continue;
            }
        }
    }

    /**
     * Prepare edge workset algorithm.
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void prepareWorksetAlgorithm(SynthesisAutomaton aut, boolean dbgEnabled) {
        // Compute the dependency sets for all edges, and store them in the synthesis automaton.
        boolean forwardEnabled = ForwardReachOption.isEnabled();
        EdgeDependencySetCreator creator = new BddBasedEdgeDependencySetCreator();
        creator.createAndStore(aut, forwardEnabled);

        // Print dependency sets as debug output.
        if (dbgEnabled) {
            int edgeCnt = aut.worksetDependenciesBackward.size();
            if (edgeCnt > 0) {
                if (forwardEnabled) {
                    dbg();
                    dbg("Edge workset algorithm forward dependencies:");
                    GridBox box = new GridBox(edgeCnt, 4, 0, 1);
                    for (int i = 0; i < edgeCnt; i++) {
                        BitSet bitset = aut.worksetDependenciesForward.get(i);
                        box.set(i, 0, " -");
                        box.set(i, 1, Integer.toString(i + 1) + ":");
                        box.set(i, 2, CifTextUtils.getAbsName(aut.orderedEdgesForward.get(i).event));
                        box.set(i, 3, BitSets.bitsetToStr(bitset, edgeCnt));
                    }
                    for (String line: box.getLines()) {
                        dbg(line);
                    }
                }

                dbg();
                dbg("Edge workset algorithm backward dependencies:");
                GridBox box = new GridBox(edgeCnt, 4, 0, 1);
                for (int i = 0; i < edgeCnt; i++) {
                    BitSet bitset = aut.worksetDependenciesBackward.get(i);
                    box.set(i, 0, " -");
                    box.set(i, 1, Integer.toString(i + 1) + ":");
                    box.set(i, 2, CifTextUtils.getAbsName(aut.orderedEdgesBackward.get(i).event));
                    box.set(i, 3, BitSets.bitsetToStr(bitset, edgeCnt));
                }
                for (String line: box.getLines()) {
                    dbg(line);
                }
            }
        }
    }

    /**
     * Performs the actual synthesis algorithm from the paper, calculating various fixed points.
     *
     * @param aut The automaton on which to perform synthesis. Is modified in-place.
     * @param doForward Whether to do forward reachability during synthesis.
     * @param dbgEnabled Whether debug output is enabled.
     * @param doTiming Whether to collect timing statistics.
     * @param timing The timing statistics data. Is modified in-place.
     */
    private static void synthesizeFixedPoints(SynthesisAutomaton aut, boolean doForward, boolean dbgEnabled,
            boolean doTiming, CifDataSynthesisTiming timing)
    {
        // We know that:
        // - Each round, we perform the same computations, in the same order.
        // - Each computation is a fixed-point reachability computation.
        // - All the computations take a controlled behavior and produce a potentially changed controlled behavior.
        // - All other data that is used is constant. For instance, marking and initialization predicates of the
        // original model are used, and they don't change during the fixed-point computations. The used guards and
        // update relations also don't change during the fixed-point computations, etc.
        //
        // To ensure a proper fixed-point result for synthesis:
        // - We need to perform all the computations at least once.
        // - We can stop as soon as the controlled behavior remains stable for all the computations.
        //
        // Therefore:
        // - We keep track of the number of consecutive computations that produced a stable result.
        // - If a computation does not change the controlled behavior, we can increment the number by one.
        // - If a computation changed the controlled behavior, we need to reconsider the other computations again.
        // However, the computation itself is a fixed-point computation, and thus won't change the controlled behavior
        // when applied again. Hence, that computation is the first computation after the change that produced a stable
        // result, and the number can be set to one.
        // - As soon as the number of consecutive computations that produced a stable result equals the total number of
        // computations to perform, we have produced a stable result for all the computations.
        //
        // Note that:
        // - We will perform each computation at least once, as we need that many increments to reach the termination
        // condition.
        // - We stop as soon as the controlled behavior is stable, regardless of whether we have completed the current
        // round of performing the computations, since there is no need to finish the round if the controlled behavior
        // is already stable.

        // Get the fixed-point reachability computations to perform, in the order to perform them.
        List<FixedPointComputation> computationsInOrder = FixedPointComputationsOrderOption.getOrder().computations;
        if (!doForward) {
            computationsInOrder = computationsInOrder.stream().filter(c -> c != REACH).toList();
        }
        int numberOfComputations = computationsInOrder.size();

        // Perform synthesis.
        int round = 0;
        int stableCount = 0;
        FIXED_POINT_LOOP:
        while (true) {
            // Next round.
            round++;
            if (aut.env.isTerminationRequested()) {
                return;
            }

            if (dbgEnabled) {
                dbg();
                dbg("Round %d: started.", round);
            }

            // Perform the fixed-point reachability computations of the round.
            for (FixedPointComputation fixedPointComputation: computationsInOrder) {
                // Get predicate from which to start the fixed-point reachability computation.
                BDD startPred = switch (fixedPointComputation) {
                    case NONBLOCK -> aut.marked.id();
                    case CTRL -> aut.ctrlBeh.not();
                    case REACH -> aut.initialCtrl.id();
                };
                if (fixedPointComputation == CTRL && aut.env.isTerminationRequested()) {
                    return;
                }

                // Configure fixed-point reachability computation.
                String predName; // Name of the predicate to compute.
                String initName; // Name of the initial value of the predicate.
                String restrictionName; // Name of the restriction predicate, if applicable.
                BDD restriction; // The restriction predicate, if applicable.
                boolean badStates; // Whether the predicate represents bad states (true) or good states (false).
                boolean applyForward; // Whether to apply forward reachability (true) or backward reachability (false).
                boolean inclCtrl; // Whether to include edges with controllable events in the reachability.
                final boolean inclUnctrl = true; // Always include edges with uncontrollable events in the reachability.
                switch (fixedPointComputation) {
                    case NONBLOCK:
                        predName = "backward controlled-behavior";
                        initName = "marker";
                        restrictionName = "current/previous controlled-behavior";
                        restriction = aut.ctrlBeh;
                        badStates = false;
                        applyForward = false;
                        inclCtrl = true;
                        break;
                    case CTRL:
                        predName = "backward uncontrolled bad-state";
                        initName = "current/previous controlled behavior";
                        restrictionName = null;
                        restriction = null;
                        badStates = true;
                        applyForward = false;
                        inclCtrl = false;
                        break;
                    case REACH:
                        predName = "forward controlled-behavior";
                        initName = "initialization";
                        restrictionName = "current/previous controlled-behavior";
                        restriction = aut.ctrlBeh;
                        badStates = false;
                        applyForward = true;
                        inclCtrl = true;
                        break;
                    default:
                        throw new RuntimeException("Unknown fixed-point computation: " + fixedPointComputation);
                }

                // Start timing the fixed-point reachability computation.
                if (doTiming) {
                    Stopwatch stopwatch = switch (fixedPointComputation) {
                        case NONBLOCK -> timing.mainBwMarked;
                        case CTRL -> timing.mainBwBadState;
                        case REACH -> timing.mainFwInit;
                    };
                    stopwatch.start();
                }

                // Perform the fixed-point reachability computation.
                BDD reachabilityResult;
                try {
                    CifDataSynthesisReachability reachability = new CifDataSynthesisReachability(aut, round, predName,
                            initName, restrictionName, restriction, badStates, applyForward, inclCtrl, inclUnctrl,
                            dbgEnabled);
                    reachabilityResult = reachability.performReachability(startPred);
                } finally {
                    // Stop timing the fixed-point reachability computation.
                    if (doTiming) {
                        Stopwatch stopwatch = switch (fixedPointComputation) {
                            case NONBLOCK -> timing.mainBwMarked;
                            case CTRL -> timing.mainBwBadState;
                            case REACH -> timing.mainFwInit;
                        };
                        stopwatch.stop();
                    }
                }

                if (aut.env.isTerminationRequested()) {
                    return;
                }

                // Get new controlled behavior.
                BDD newCtrlBeh;
                switch (fixedPointComputation) {
                    case NONBLOCK:
                    case REACH:
                        newCtrlBeh = reachabilityResult;
                        break;
                    case CTRL:
                        newCtrlBeh = reachabilityResult.not();
                        reachabilityResult.free();
                        if (aut.env.isTerminationRequested()) {
                            return;
                        }
                        break;
                    default:
                        throw new RuntimeException("Unknown fixed-point computation: " + fixedPointComputation);
                }

                // Detect change in controlled behavior.
                boolean unchanged = aut.ctrlBeh.equals(newCtrlBeh);
                boolean changed = !unchanged;
                if (unchanged) {
                    newCtrlBeh.free();
                    stableCount++;
                } else {
                    if (dbgEnabled) {
                        dbg("Controlled behavior: %s -> %s.", bddToStr(aut.ctrlBeh, aut), bddToStr(newCtrlBeh, aut));
                    }
                    aut.ctrlBeh.free();
                    aut.ctrlBeh = newCtrlBeh;
                    stableCount = 1;
                }

                // Detect a fixed point for all fixed-point computations (as far as they are not disabled by options):

                // 1) Check for empty controlled behavior.
                BDD ctrlStates = aut.ctrlBeh.and(aut.plantInv);
                boolean noCtrlStates = ctrlStates.isZero();
                ctrlStates.free();
                if (noCtrlStates) {
                    if (dbgEnabled) {
                        dbg();
                        dbg("Round %d: finished, all states are bad.", round);
                    }
                    break FIXED_POINT_LOOP;
                }
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                // 2) Check for controlled behavior being stable, after having performed all computations at least once.
                if (stableCount == numberOfComputations) {
                    if (dbgEnabled) {
                        dbg();
                        dbg("Round %d: finished, controlled behavior is stable.", round);
                    }
                    break FIXED_POINT_LOOP;
                }

                // 3) Check for no initial states left, if the controlled behavior changed. There is no need to check
                // this for forward reachability, as it starts from the initial states, and if there are no initial
                // states, then the controlled behavior is empty and a fixed point was detected above already.
                if (changed && fixedPointComputation != REACH) {
                    BDD init = aut.initialCtrl.and(aut.ctrlBeh);
                    boolean noInit = init.isZero();
                    init.free();
                    if (noInit) {
                        if (dbgEnabled) {
                            dbg();
                            dbg("Round %d: finished, no initialization possible.", round);
                        }
                        break FIXED_POINT_LOOP;
                    }
                    if (aut.env.isTerminationRequested()) {
                        return;
                    }
                }
            }

            // Finished round.
            if (dbgEnabled) {
                dbg();
                dbg("Round %d: finished, need another round.", round);
            }
        }
    }

    /**
     * Determines the guards of the controlled system, for each linearized edge.
     *
     * @param aut The synthesis result.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void determineCtrlSysGuards(SynthesisAutomaton aut, boolean dbgEnabled) {
        // Compute guards of edges with controllable events.
        if (dbgEnabled) {
            dbg();
            dbg("Computing controlled system guards.");
        }

        boolean guardUpdated = false;
        for (SynthesisEdge edge: aut.edges) {
            if (!edge.event.getControllable()) {
                continue;
            }
            if (aut.env.isTerminationRequested()) {
                return;
            }

            BDD updPred = aut.ctrlBeh.id();
            edge.preApply(false, null);
            updPred = edge.apply(updPred, false, false, null, true);
            edge.postApply(false);
            edge.cleanupApply();
            if (aut.env.isTerminationRequested()) {
                return;
            }

            BDD newGuard = edge.guard.id().andWith(updPred);
            if (aut.env.isTerminationRequested()) {
                return;
            }

            if (edge.guard.equals(newGuard)) {
                newGuard.free();
            } else {
                if (dbgEnabled) {
                    if (!guardUpdated) {
                        dbg();
                    }
                    dbg("Edge %s: guard: %s -> %s.", edge.toString(0, ""), bddToStr(edge.guard, aut),
                            bddToStr(newGuard, aut));
                }
                edge.guard.free();
                edge.guard = newGuard;
                guardUpdated = true;
            }
        }
    }

    /**
     * Determine the initialization predicate of the controlled system, updating the initialization predicate of the
     * controlled system as computed so far ({@link SynthesisAutomaton#initialCtrl}) with the controlled behavior.
     *
     * @param aut The synthesis result.
     */
    private static void determineCtrlSysInit(SynthesisAutomaton aut) {
        // Update initialization predicate for controlled system with the controlled behavior.
        aut.initialCtrl = aut.initialCtrl.andWith(aut.ctrlBeh.id());

        // Free no longer needed predicates.
        aut.initialPlantInv.free();
        aut.initialPlantInv = null;
    }

    /**
     * Check the synthesis result, to see whether an initial state is still present.
     *
     * @param aut The synthesis result.
     * @return Whether an initial state exists in the controlled system ({@code true}) or the supervisor is empty
     *     ({@code false}).
     */
    private static boolean checkInitStatePresent(SynthesisAutomaton aut) {
        // Check for empty supervisor (no initial state).
        boolean emptySup = aut.initialCtrl.isZero();
        return !emptySup;
    }

    /**
     * Prints the number of states in the controlled system, as normal output, separating it from the debug output.
     *
     * @param aut The synthesis automaton.
     * @param emptySup Whether the supervisor is empty.
     * @param doForward Whether to do forward reachability during synthesis.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void printNumberStates(SynthesisAutomaton aut, boolean emptySup, boolean doForward,
            boolean dbgEnabled)
    {
        // Get number of states in controlled system.
        double nr;
        if (emptySup) {
            nr = 0;
        } else if (aut.variables.length == 0) {
            Assert.check(aut.ctrlBeh.isZero() || aut.ctrlBeh.isOne());
            nr = aut.ctrlBeh.isOne() ? 1 : 0;
        } else {
            nr = aut.ctrlBeh.satCount(aut.varSetOld);
        }
        Assert.check(emptySup || nr > 0);

        // Print controlled system states statistics.
        boolean isExact = emptySup || doForward;
        if (dbgEnabled) {
            dbg();
        }
        out("Controlled system: %s %,.0f state%s.", (isExact ? "exactly" : "at most"), nr, nr == 1 ? "" : "s");
    }

    /**
     * Determines the synthesis output, related to initialization, that is to end up in the resulting CIF model.
     *
     * @param aut The synthesis result.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void determineOutputInitial(SynthesisAutomaton aut, boolean dbgEnabled) {
        // Print some debug output.
        if (dbgEnabled) {
            dbg();
            dbg("Initial (synthesis result):            %s", bddToStr(aut.ctrlBeh, aut));
            dbg("Initial (uncontrolled system):         %s", bddToStr(aut.initialUnctrl, aut));
            dbg("Initial (controlled system):           %s", bddToStr(aut.initialCtrl, aut));
        }
        if (aut.env.isTerminationRequested()) {
            return;
        }

        // What initialization was allowed in the uncontrolled system, but is no longer allowed in the controlled
        // system, as thus has been removed as allowed initialization? The inverse of that is what the supervisor adds
        // as additional initialization restriction on top of the uncontrolled system.
        BDD initialRemoved = aut.initialUnctrl.id().andWith(aut.initialCtrl.not());
        if (aut.env.isTerminationRequested()) {
            return;
        }

        BDD initialAdded = initialRemoved.not();
        if (aut.env.isTerminationRequested()) {
            return;
        }

        if (dbgEnabled) {
            dbg("Initial (removed by supervisor):       %s", bddToStr(initialRemoved, aut));
            dbg("Initial (added by supervisor):         %s", bddToStr(initialAdded, aut));
        }
        if (aut.env.isTerminationRequested()) {
            return;
        }

        // Determine initialization predicate. The initialization predicate of the controlled system is used, if it is
        // at all restricted with respect to the uncontrolled system.
        EnumSet<BddSimplify> simplifications = BddSimplifyOption.getSimplifications();
        List<String> assumptionTxts = list();

        if (!initialRemoved.isZero()) {
            aut.initialOutput = aut.initialCtrl.id();
            BDD assumption = aut.factory.one();

            // If requested, the controlled system initialization predicate is simplified under the assumption of the
            // uncontrolled system initialization predicate, to obtain the additional initialization restrictions
            // introduced by the controller with respect to the uncontrolled system initialization predicate.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            if (simplifications.contains(BddSimplify.INITIAL_UNCTRL)) {
                assumptionTxts.add("uncontrolled system initialization predicates");

                BDD extra = aut.initialUnctrl.id();
                assumption = assumption.andWith(extra);
            }

            // If requested, the controlled system initialization predicate is simplified under the assumption of the
            // state plant invariants, to obtain the additional initialization restrictions introduced by the
            // controller with respect to the state plant invariants.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            if (simplifications.contains(BddSimplify.INITIAL_STATE_PLANT_INVS)) {
                assumptionTxts.add("state plant invariants");

                BDD extra = aut.plantInv.id();
                assumption = assumption.andWith(extra);
            }

            // Perform simplification if there are assumptions.
            if (!assumptionTxts.isEmpty()) {
                if (aut.env.isTerminationRequested()) {
                    return;
                }
                String assumptionsTxt = combineAssumptionTexts(assumptionTxts);

                BDD newInitial = aut.initialOutput.simplify(assumption);
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                if (dbgEnabled && !aut.initialOutput.equals(newInitial)) {
                    dbg();
                    dbg("Simplification of controlled system initialization predicate under the assumption of the %s:",
                            assumptionsTxt);
                    dbg("  Initial: %s -> %s [assume %s].", bddToStr(aut.initialOutput, aut), bddToStr(newInitial, aut),
                            bddToStr(assumption, aut));
                }
                aut.initialOutput.free();
                aut.initialOutput = newInitial;
            }

            assumption.free();
        }

        if (aut.env.isTerminationRequested()) {
            return;
        }

        // Free no longer needed predicates.
        aut.initialCtrl.free();
        aut.initialUnctrl.free();
        initialRemoved.free();
        initialAdded.free();

        aut.initialCtrl = null;
        aut.initialUnctrl = null;
    }

    /**
     * Computes the global guards for the given events. This is done by combining the guards of all edges, per event.
     *
     * <p>
     * The guard BDDs on the edges are consumed.
     * </p>
     *
     * @param aut The automaton on which synthesis was performed.
     * @param events The events for which to compute the guards.
     * @param useOrigGuards Whether to use the original guard or the current guard on the synthesis edge.
     * @return The guards.
     */
    private static Map<Event, BDD> determineGuards(SynthesisAutomaton aut, Set<Event> events, boolean useOrigGuards) {
        Map<Event, BDD> guards = mapc(events.size());

        // Initialize guards to 'false'.
        for (Event event: events) {
            guards.put(event, aut.factory.zero());
        }

        // Compute linearized guards. This is done by combining the guards of all edges, per event.
        for (SynthesisEdge synthEdge: aut.edges) {
            // Skip the edges for other events.
            if (!events.contains(synthEdge.event)) {
                continue;
            }
            if (aut.env.isTerminationRequested()) {
                return null;
            }

            // Get current guard.
            BDD guard = guards.get(synthEdge.event);

            // Update guard. Frees the guard of the edge.
            guard = useOrigGuards ? guard.orWith(synthEdge.origGuard) : guard.orWith(synthEdge.guard);

            // Store updated guard.
            guards.put(synthEdge.event, guard);
        }

        return guards;
    }

    /**
     * Checks the system for problems with events that are disabled in the global controlled system.
     *
     * <p>
     * Does not report problems for events that were already disabled before synthesis.
     * </p>
     *
     * @param aut The automaton on which synthesis was performed.
     * @param ctrlGuards The guards in the controlled system for the controllable events to check.
     */
    private static void checkOutputEdges(SynthesisAutomaton aut, Map<Event, BDD> ctrlGuards) {
        // Determine the guards for the uncontrollable events.
        Set<Event> uncontrollables = Sets.difference(aut.alphabet, aut.controllables, aut.inputVarEvents);
        Map<Event, BDD> unctrlGuards = determineGuards(aut, uncontrollables, false);

        // Warn for controllable events never enabled in the controlled system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        warnEventsDisabled(aut, ctrlGuards);

        // Warn for uncontrollable events never enabled in the controlled system.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        warnEventsDisabled(aut, unctrlGuards);

        // Free no longer needed predicates.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        for (BDD bdd: unctrlGuards.values()) {
            bdd.free();
        }
    }

    /**
     * Warns for events that are disabled in the global controlled system.
     *
     * <p>
     * Does not report problems for events that were already disabled before synthesis.
     * </p>
     *
     * @param aut The automaton on which synthesis was performed.
     * @param guards The guards in the controlled system for the events.
     */
    private static void warnEventsDisabled(SynthesisAutomaton aut, Map<Event, BDD> guards) {
        // Calculate controlled state space.
        BDD ctrlBehPlantInv = aut.ctrlBeh.and(aut.plantInv);

        // Check all events.
        for (Event event: guards.keySet()) {
            if (aut.env.isTerminationRequested()) {
                return;
            }

            // Determine when the event is enabled in controlled statespace.
            BDD ctrlBehGuard = guards.get(event).and(ctrlBehPlantInv);

            // Warn for events that are never enabled.
            if (ctrlBehGuard.isZero() && !aut.disabledEvents.contains(event)) {
                warn("Event \"%s\" is disabled in the controlled system.", CifTextUtils.getAbsName(event));
                aut.disabledEvents.add(event);
                continue;
            }
            ctrlBehGuard.free();
        }
        ctrlBehPlantInv.free();
    }

    /**
     * Determines the synthesis output, related to guards of controllable events, that is to end up in the resulting CIF
     * model.
     *
     * <p>
     * It takes the controlled system guards of the controllable events. It then applies simplification as requested.
     * </p>
     *
     * @param aut The synthesis result.
     * @param ctrlGuards The guards in the controlled system for the controllable events.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private static void determineOutputGuards(SynthesisAutomaton aut, Map<Event, BDD> ctrlGuards, boolean dbgEnabled) {
        // Get simplifications to perform.
        EnumSet<BddSimplify> simplifications = BddSimplifyOption.getSimplifications();
        List<String> assumptionTxts = list();

        // Initialize assumptions to 'true', for all controllable events.
        Map<Event, BDD> assumptions = mapc(aut.controllables.size());
        for (Event controllable: aut.controllables) {
            assumptions.put(controllable, aut.factory.one());
        }

        // If requested, simplify output guards assuming the uncontrolled system guard. This results in the additional
        // restrictions introduced by the controller with respect to the plants (i.e. uncontrolled system), instead of
        // the full controlled system guard. Simplification is best effort.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (simplifications.contains(BddSimplify.GUARDS_PLANTS)) {
            assumptionTxts.add("plants");

            // Compute the global uncontrolled system guards, for all controllable events.
            Map<Event, BDD> unctrlGuards = determineGuards(aut, aut.controllables, true);

            // Add guards to the assumptions.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            for (Event controllable: aut.controllables) {
                BDD assumption = assumptions.get(controllable);
                BDD extra = unctrlGuards.get(controllable);
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                assumption = assumption.andWith(extra);
                assumptions.put(controllable, assumption);
            }
        }

        // If requested, simplify output guards assuming the state/event exclusion requirement invariants derived from
        // the requirement automata. This results in the additional restrictions introduced by the controller with
        // respect to those requirements, instead of the full controlled system guard. Simplification is best effort.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (simplifications.contains(BddSimplify.GUARDS_REQ_AUTS)) {
            assumptionTxts.add("requirement automata");

            for (Event controllable: aut.controllables) {
                BDD assumption = assumptions.get(controllable);
                BDD extra = aut.stateEvtExclsReqAuts.get(controllable);
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                assumption = assumption.andWith(extra);
                assumptions.put(controllable, assumption);
            }
        }
        aut.stateEvtExclsReqAuts = null;

        // If requested, simplify output guards assuming the state/event exclusion plant invariants from the input
        // specification. This results in the additional restrictions introduced by the controller with respect to these
        // plants, instead of the full controlled system guard. Simplification is best effort.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (simplifications.contains(BddSimplify.GUARDS_SE_EXCL_PLANT_INVS)) {
            assumptionTxts.add("state/event exclusion plant invariants");

            for (Event controllable: aut.controllables) {
                BDD assumption = assumptions.get(controllable);
                BDD extra = aut.stateEvtExclPlants.get(controllable);
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                assumption = assumption.andWith(extra);
                assumptions.put(controllable, assumption);
            }
        }
        aut.stateEvtExclPlants = null;

        // If requested, simplify output guards assuming the state/event exclusion requirement invariants from the input
        // specification. This results in the additional restrictions introduced by the controller with respect to those
        // requirements, instead of the full controlled system guard. Simplification is best effort.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (simplifications.contains(BddSimplify.GUARDS_SE_EXCL_REQ_INVS)) {
            assumptionTxts.add("state/event exclusion requirement invariants");

            for (Event controllable: aut.controllables) {
                BDD assumption = assumptions.get(controllable);
                BDD extra = aut.stateEvtExclsReqInvs.get(controllable);
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                assumption = assumption.andWith(extra);
                assumptions.put(controllable, assumption);
            }
        }
        aut.stateEvtExclsReqInvs = null;

        // If requested, simplify output guards assuming the state plant invariants from the input specification.
        // This results in the additional restrictions introduced by the controller with respect to those plants,
        // instead of the full controlled system guard. Simplification is best effort.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (simplifications.contains(BddSimplify.GUARDS_STATE_PLANT_INVS)) {
            assumptionTxts.add("state plant invariants");

            for (Event controllable: aut.controllables) {
                BDD assumption = assumptions.get(controllable);
                BDD extra = aut.plantInv.id();
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                assumption = assumption.andWith(extra);
                assumptions.put(controllable, assumption);
            }
        }
        aut.plantInv.free();
        aut.plantInv = null;

        // If requested, simplify output guards assuming the state requirement invariants from the input specification.
        // This results in the additional restrictions introduced by the controller with respect to those requirements,
        // instead of the full controlled system guard. Simplification is best effort.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (simplifications.contains(BddSimplify.GUARDS_STATE_REQ_INVS)) {
            assumptionTxts.add("state requirement invariants");

            for (Event controllable: aut.controllables) {
                BDD assumption = assumptions.get(controllable);
                BDD extra = aut.reqInv.id();
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                assumption = assumption.andWith(extra);
                assumptions.put(controllable, assumption);
            }
        }
        aut.reqInv.free();
        aut.reqInv = null;

        // If requested, simplify output guards assuming the controlled behavior as computed by synthesis.
        // Initialization is restricted to ensure the system starts within the controlled behavior. Each guard ensures
        // the system remains in the controlled behavior. We may assume before a transition, we are in the controlled
        // behavior. We can thus simplify guards using this assumption. Simplification is best effort.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        if (simplifications.contains(BddSimplify.GUARDS_CTRL_BEH)) {
            assumptionTxts.add("controlled behavior");

            for (Event controllable: aut.controllables) {
                BDD assumption = assumptions.get(controllable);
                BDD extra = aut.ctrlBeh.id();
                if (aut.env.isTerminationRequested()) {
                    return;
                }

                assumption = assumption.andWith(extra);
                assumptions.put(controllable, assumption);
            }
        }
        aut.ctrlBeh.free();
        aut.ctrlBeh = null;

        // Initialize output guards.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        aut.outputGuards = ctrlGuards;

        // If no assumptions, we are done.
        if (assumptionTxts.isEmpty()) {
            return;
        }

        // Perform the simplification using all the collected assumptions.
        if (aut.env.isTerminationRequested()) {
            return;
        }
        String assumptionsTxt = combineAssumptionTexts(assumptionTxts);
        simplifyOutputGuards(aut, dbgEnabled, assumptions, assumptionsTxt);
    }

    /**
     * Combines assumption texts. Separates entries with commas, and puts an 'and' before the last entry. Only uses
     * commas if more than two items are present.
     *
     * @param texts The texts.
     * @return The combined text.
     */
    private static String combineAssumptionTexts(List<String> texts) {
        if (texts.size() == 0) {
            return "";
        }
        if (texts.size() == 1) {
            return texts.get(0);
        }

        StringBuilder txt = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) {
                if (texts.size() > 2) {
                    txt.append(",");
                }
                txt.append(" ");
            }
            if (i == texts.size() - 1) {
                txt.append("and ");
            }
            txt.append(texts.get(i));
        }
        return txt.toString();
    }

    /**
     * Simplify the {@link SynthesisAutomaton#outputGuards output guards}.
     *
     * @param aut The synthesis result. Is modified in-place.
     * @param dbgEnabled Whether debug output is enabled.
     * @param assumptions Per controllable event, the assumption to use. All assumptions are {@link BDD#free freed}
     *     after use.
     * @param assumptionsTxt Text describing the assumptions that are used, for debugging output.
     */
    private static void simplifyOutputGuards(SynthesisAutomaton aut, boolean dbgEnabled, Map<Event, BDD> assumptions,
            String assumptionsTxt)
    {
        boolean dbgPrinted = false;
        for (Event controllable: aut.controllables) {
            if (aut.env.isTerminationRequested()) {
                return;
            }

            // Get current guard and assumption.
            BDD guard = aut.outputGuards.get(controllable);
            BDD assumption = assumptions.get(controllable);

            // Simplify.
            BDD newGuard;
            if (assumption.isZero() && guard.isZero()) {
                // Special case for events that are assumed to be never enabled, the supervisor does not restrict them.
                newGuard = aut.factory.one();
            } else {
                newGuard = guard.simplify(assumption);
            }
            if (aut.env.isTerminationRequested()) {
                return;
            }

            aut.outputGuards.put(controllable, newGuard);

            // If it had an effect, print some debug info.
            if (dbgEnabled && !guard.equals(newGuard)) {
                if (!dbgPrinted) {
                    dbgPrinted = true;
                    dbg();
                    dbg("Simplification of controlled system under the assumption of the %s:", assumptionsTxt);
                }
                dbg("  Event %s: guard: %s -> %s [assume %s].", CifTextUtils.getAbsName(controllable),
                        bddToStr(guard, aut), bddToStr(newGuard, aut), bddToStr(assumption, aut));
            }

            // Free no longer needed predicates.
            if (aut.env.isTerminationRequested()) {
                return;
            }
            assumption.free();
            guard.free();
        }
    }
}
