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

package org.eclipse.escet.cif.datasynth.spec;

import static org.eclipse.escet.cif.datasynth.bdd.BddUtils.bddToStr;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.app.framework.AppEnv;
import org.eclipse.escet.common.app.framework.AppEnvData;
import org.eclipse.escet.common.java.Strings;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDFactory;
import com.github.javabdd.BDDPairing;
import com.github.javabdd.BDDVarSet;

/** Data-based synthesis algorithm linearized automaton. */
public class SynthesisAutomaton {
    /** The application context to use. */
    public final AppEnvData env = AppEnv.getData();

    /** The BDD factory to use. */
    public BDDFactory factory;

    /**
     * The maximum number of BDD nodes for which to convert a BDD to a readable CNF/DNF representation for the debug
     * output, or {@code null} for no maximum.
     */
    public Integer debugMaxNodes;

    /**
     * The maximum number of BDD true paths for which to convert a BDD to a readable CNF/DNF representation for the
     * debug output, or {@code null} for no maximum.
     */
    public Double debugMaxPaths;

    /** The alphabet of the automaton. */
    public Set<Event> alphabet;

    /** The controllable subset of the {@link #alphabet} of the automaton. */
    public Set<Event> controllables;

    /** The temporary events created for the input variables. */
    public Set<Event> inputVarEvents;

    /**
     * Information on the variables of the specification, including location pointer variables. This is the order used
     * whenever variables are ordered.
     */
    public SynthesisVariable[] variables;

    /** The synthesis edges. */
    public List<SynthesisEdge> edges;

    /**
     * The synthesis edges, ordered for backward reachability computations. Contains all edges from {@link #edges} at
     * least once.
     */
    public List<SynthesisEdge> orderedEdgesBackward;

    /**
     * The synthesis edges, ordered for forward reachability computations. Contains all edges from {@link #edges} at
     * least once.
     */
    public List<SynthesisEdge> orderedEdgesForward;

    /** Mapping from events to their synthesis edges. */
    public Map<Event, List<SynthesisEdge>> eventEdges;

    /** The events that are disabled before synthesis. */
    public Set<Event> disabledEvents;

    /**
     * Per synthesis edge in {@link #orderedEdgesBackward}, its backward edge dependencies set for the workset
     * algorithm. This field is {@code null} until it is computed.
     */
    public List<BitSet> worksetDependenciesBackward;

    /**
     * Per synthesis edge in {@link #orderedEdgesForward}, its forward edge dependencies set for the workset algorithm.
     * This field is {@code null} until it is computed.
     */
    public List<BitSet> worksetDependenciesForward;

    /**
     * Initialization predicates for each of the synthesis variables. Predicates are obtained from the initial values as
     * specified with the declarations of the discrete variables. For synthesis variables that don't represent a
     * discrete variable, the predicate is {@code null}. Is {@code null} if not yet or no longer available.
     */
    public List<BDD> initialsVars;

    /** Initialization predicates from the components. Is {@code null} if not yet or no longer available. */
    public List<BDD> initialsComps;

    /**
     * Initialization predicates from the locations of the automata, per automaton. Is {@code null} if not yet or no
     * longer available.
     */
    public List<BDD> initialsLocs;

    /**
     * Initialization predicate for the discrete variables. Conjunction of {@link #initialsVars}. Is {@code null} if not
     * yet or no longer available.
     */
    public BDD initialVars;

    /**
     * Initialization predicate for the components. Conjunction of {@link #initialsComps}. Is {@code null} if not yet or
     * no longer available.
     */
    public BDD initialComps;

    /**
     * Initialization predicate for the locations of the automata. Conjunction of {@link #initialsLocs}. Is {@code null}
     * if not yet or no longer available.
     */
    public BDD initialLocs;

    /**
     * Initialization predicate of the uncontrolled system. Conjunction of {@link #initialVars}, {@link #initialComps}
     * and {@link #initialLocs}. Is {@code null} if not yet or no longer available.
     */
    public BDD initialUnctrl;

    /**
     * Combined initialization and state plant invariant predicates of the uncontrolled system. Conjunction of
     * {@link #initialUnctrl} and {@link #plantInv}. Is {@code null} if not yet or no longer available.
     */
    public BDD initialPlantInv;

    /**
     * Combined initialization and state invariant predicates of the uncontrolled system. Conjunction of
     * {@link #initialPlantInv} and {@link #reqInv}. Is {@code null} if not yet or no longer available.
     */
    public BDD initialInv;

    /** Initialization predicate of the controlled system. Is {@code null} if not yet or no longer available. */
    public BDD initialCtrl;

    /**
     * Initialization predicate of the to use for the output. Computed as a result of synthesis. Is {@code null} if not
     * yet available, or if no additional initialization predicate is to be added to the output.
     */
    public BDD initialOutput;

    /** Marker predicates from the components. Is {@code null} if not yet or no longer available. */
    public List<BDD> markedsComps;

    /**
     * Marker predicates from the locations of the automata, combined per automaton. Is {@code null} if not yet or no
     * longer available.
     */
    public List<BDD> markedsLocs;

    /**
     * Marker predicate for the components. Conjunction of {@link #markedsComps}. Is {@code null} if not yet or no
     * longer available.
     */
    public BDD markedComps;

    /**
     * Marker predicate for the locations of the automata. Conjunction of {@link #markedsLocs}. Is {@code null} if not
     * yet or no longer available.
     */
    public BDD markedLocs;

    /**
     * Marker predicate for the uncontrolled system. Conjunction of {@link #markedComps} and {@link #markedLocs}. Is
     * {@code null} if not yet available.
     */
    public BDD marked;

    /**
     * Combined marking and state plant invariant predicates for the uncontrolled system. Conjunction of {@link #marked}
     * and {@link #plantInv}. Is {@code null} if not yet available.
     */
    public BDD markedPlantInv;

    /**
     * Combined marking and state invariant predicates of the uncontrolled system. Conjunction of
     * {@link #markedPlantInv} and {@link #reqInv}. Is {@code null} if not yet or no longer available.
     */
    public BDD markedInv;

    /** State plant invariants (predicates) from the components. Is {@code null} if not yet or no longer available. */
    public List<BDD> plantInvsComps;

    /**
     * State plant invariants (predicates) from the locations of the automata. Unlike initialization and marker
     * predicates, these are not combined per automaton, but instead individual state plant invariants (predicates) are
     * kept. Is {@code null} if not yet or no longer available.
     */
    public List<BDD> plantInvsLocs;

    /**
     * State plant invariant (predicate) for the components. Conjunction of {@link #plantInvsComps}. Is {@code null} if
     * not yet or no longer available.
     */
    public BDD plantInvComps;

    /**
     * State plant invariant (predicate) for the locations of the automata. Conjunction of {@link #plantInvsLocs}. Is
     * {@code null} if not yet or no longer available.
     */
    public BDD plantInvLocs;

    /**
     * State plant invariant (predicate) for the system. Conjunction of {@link #plantInvComps} and
     * {@link #plantInvLocs}. Is {@code null} if not yet or no longer available.
     */
    public BDD plantInv;

    /**
     * State requirement invariants (predicates) from the components. Is {@code null} if not yet or no longer available.
     */
    public List<BDD> reqInvsComps;

    /**
     * State requirement invariants (predicates) from the locations of the automata. Unlike initialization and marker
     * predicates, these are not combined per automaton, but instead individual state requirement invariants
     * (predicates) are kept. Is {@code null} if not yet or no longer available.
     */
    public List<BDD> reqInvsLocs;

    /**
     * State requirement invariant (predicate) for the components. Conjunction of {@link #reqInvsComps}. Is {@code null}
     * if not yet or no longer available.
     */
    public BDD reqInvComps;

    /**
     * State requirement invariant (predicate) for the locations of the automata. Conjunction of {@link #reqInvsLocs}.
     * Is {@code null} if not yet or no longer available.
     */
    public BDD reqInvLocs;

    /**
     * State requirement invariant (predicate) for the system. Conjunction of {@link #reqInvComps} and
     * {@link #reqInvLocs}. Is {@code null} if not yet or no longer available.
     */
    public BDD reqInv;

    /**
     * Mapping from controllable events to their corresponding state/event exclusion requirements, derived from
     * requirement automata. Per event, the state/event requirements are combined, using conjunctions. The state/event
     * requirement predicates indicate necessary global conditions for the event to be enabled/allowed. That is, the
     * predicates can be seen as additional global guards. The predicates originate only from the requirement automata,
     * not from the state/event exclusion requirement invariants of the input specification. Is {@code null} if not yet
     * or no longer available.
     */
    public Map<Event, BDD> stateEvtExclsReqAuts;

    /**
     * Mapping from controllable events to their corresponding state/event exclusion requirements, derived from
     * state/event exclusion requirement invariants from the input specification. Per event, the state/event
     * requirements are combined, using conjunctions. The state/event requirement predicates indicate necessary global
     * conditions for the event to be enabled/allowed. That is, the predicates can be seen as additional global guards.
     * The predicates originate only from the state/event exclusion requirement invariants of the input specification,
     * not from the requirement automata. Is {@code null} if not yet or no longer available.
     */
    public Map<Event, BDD> stateEvtExclsReqInvs;

    /**
     * Mapping from events to their corresponding state/event exclusion requirements. Does not map internal events that
     * are not in the original specification, e.g. for input variables. Per event, the separate state/event requirements
     * are collected. The state/event requirement predicates indicate necessary global conditions for the event to be
     * enabled/allowed. That is, the predicates can be seen as additional global guards. The predicates originate not
     * only from the state/event exclusion requirement invariants, but also from requirement automata. Is {@code null}
     * if not yet or no longer available.
     */
    public Map<Event, List<BDD>> stateEvtExclReqLists;

    /**
     * Mapping from events to their corresponding state/event exclusion requirements. Does not map internal events that
     * are not in the original specification, e.g. for input variables. Per event, the state/event requirements are
     * combined, using conjunctions, with respect to {@link #stateEvtExclReqLists}. The state/event requirement
     * predicates indicate necessary global conditions for the event to be enabled/allowed. That is, the predicates can
     * be seen as additional global guards. The predicates originate not only from the state/event exclusion requirement
     * invariants, but also from requirement automata. Is {@code null} if not yet or no longer available.
     */
    public Map<Event, BDD> stateEvtExclReqs;

    /**
     * Mapping from events to their corresponding state/event exclusion plants. Does not map internal events that are
     * not in the original specification, e.g. for input variables. Per event, the separate state/event plant invariants
     * are collected. The state/event plant predicates indicate necessary global conditions for the event to be enabled.
     * That is, the predicates can be seen as additional global guards. Is {@code null} if not yet or no longer
     * available.
     */
    public Map<Event, List<BDD>> stateEvtExclPlantLists;

    /**
     * Mapping from events to their corresponding state/event exclusion plants. Does not map internal events that are
     * not in the original specification, e.g. for input variables. Per event, the state/event plants are combined,
     * using conjunctions, with respect to {@link #stateEvtExclPlantLists}. The state/event plant predicates indicate
     * necessary global conditions for the event to be enabled. That is, the predicates can be seen as additional global
     * guards. Is {@code null} if no yet or no longer available.
     */
    public Map<Event, BDD> stateEvtExclPlants;

    /**
     * BDD pairing for every old variable 'x' to its corresponding new variable 'x+'. Is {@code null} if not available.
     */
    public BDDPairing oldToNewVarsPairing;

    /**
     * BDD pairing for every new variable 'x+' to its corresponding old variable 'x'. Is {@code null} if not available.
     */
    public BDDPairing newToOldVarsPairing;

    /** The BDD variable set containing all old variables, i.e. '{x, y, z, ...}'. Is {@code null} if not available. */
    public BDDVarSet varSetOld;

    /**
     * The BDD variable set containing all new variables, i.e. '{x+, y+, z+, ...}'. Is {@code null} if not available.
     */
    public BDDVarSet varSetNew;

    /**
     * Mapping from the controllable events in the {@link #alphabet} to the guards to use as output of synthesis, when
     * constructing the output CIF model. Is {@code null} until computed as part of synthesis.
     */
    public Map<Event, BDD> outputGuards;

    /** Controlled-behavior predicate of the system. Computed and used during synthesis. Also a result of synthesis. */
    public BDD ctrlBeh;

    @Override
    public String toString() {
        return toString(0, "State: ", true);
    }

    /**
     * Returns a textual representation of the synthesis automaton, including the edges.
     *
     * @param indent The indentation level.
     * @return The textual representation.
     */
    public String toString(int indent) {
        return toString(indent, "State: ", true);
    }

    /**
     * Returns a textual representation of the synthesis automaton.
     *
     * @param indent The indentation level.
     * @param inclEdges Whether to include the edges.
     * @return The textual representation.
     */
    public String toString(int indent, boolean inclEdges) {
        return toString(indent, "State: ", inclEdges);
    }

    /**
     * Returns a textual representation of the synthesis automaton.
     *
     * @param indent The indentation level.
     * @param prefix The prefix to use, e.g. {@code "State: "} or {@code ""}.
     * @param inclEdges Whether to include the edges.
     * @return The textual representation.
     */
    public String toString(int indent, String prefix, boolean inclEdges) {
        StringBuilder txt = new StringBuilder();
        txt.append(Strings.duplicate(" ", 2 * indent));
        txt.append(prefix);
        String cbTxt = (ctrlBeh == null) ? "?" : bddToStr(ctrlBeh, this);
        txt.append(fmt("(controlled-behavior: %s)", cbTxt));
        if (inclEdges) {
            for (SynthesisEdge edge: edges) {
                txt.append("\n");
                txt.append(edge.toString(indent + 1, "Edge: "));
            }
        }
        return txt.toString();
    }
}
