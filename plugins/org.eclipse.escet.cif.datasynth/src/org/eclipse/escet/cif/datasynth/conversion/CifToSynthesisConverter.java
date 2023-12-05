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

package org.eclipse.escet.cif.datasynth.conversion;

import static org.eclipse.escet.cif.common.CifTextUtils.getAbsName;
import static org.eclipse.escet.cif.common.CifTypeUtils.normalizeType;
import static org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.BI_CONDITIONAL;
import static org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.CONJUNCTION;
import static org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.DISJUNCTION;
import static org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.IMPLICATION;
import static org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator.INTEGER_DIVISION;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignment;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newInputVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newMonitors;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.dbg;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.warn;
import static org.eclipse.escet.common.emf.EMFHelper.deepclone;
import static org.eclipse.escet.common.java.Lists.concat;
import static org.eclipse.escet.common.java.Lists.copy;
import static org.eclipse.escet.common.java.Lists.filter;
import static org.eclipse.escet.common.java.Lists.first;
import static org.eclipse.escet.common.java.Lists.last;
import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Lists.reverse;
import static org.eclipse.escet.common.java.Lists.set2list;
import static org.eclipse.escet.common.java.Maps.map;
import static org.eclipse.escet.common.java.Maps.mapc;
import static org.eclipse.escet.common.java.Pair.pair;
import static org.eclipse.escet.common.java.Sets.copy;
import static org.eclipse.escet.common.java.Sets.difference;
import static org.eclipse.escet.common.java.Sets.list2set;
import static org.eclipse.escet.common.java.Sets.set;
import static org.eclipse.escet.common.java.Sets.setc;
import static org.eclipse.escet.common.java.Sets.sortedstrings;
import static org.eclipse.escet.common.java.Sets.union;
import static org.eclipse.escet.common.java.Strings.fmt;
import static org.eclipse.escet.common.java.Strings.str;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.escet.cif.cif2cif.LinearizeProduct;
import org.eclipse.escet.cif.cif2cif.LocationPointerManager;
import org.eclipse.escet.cif.common.CifEnumLiteral;
import org.eclipse.escet.cif.common.CifEquationUtils;
import org.eclipse.escet.cif.common.CifEvalException;
import org.eclipse.escet.cif.common.CifEvalUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.common.CifEventUtils.Alphabets;
import org.eclipse.escet.cif.common.CifGuardUtils;
import org.eclipse.escet.cif.common.CifGuardUtils.LocRefExprCreator;
import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifTypeUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.datasynth.bdd.BddUtils;
import org.eclipse.escet.cif.datasynth.bdd.CifBddBitVector;
import org.eclipse.escet.cif.datasynth.bdd.CifBddBitVectorAndCarry;
import org.eclipse.escet.cif.datasynth.options.BddAdvancedVariableOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddDebugMaxNodesOption;
import org.eclipse.escet.cif.datasynth.options.BddDebugMaxPathsOption;
import org.eclipse.escet.cif.datasynth.options.EdgeGranularityOption;
import org.eclipse.escet.cif.datasynth.options.EdgeGranularityOption.EdgeGranularity;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderBackwardOption;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderDuplicateEventsOption;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderDuplicateEventsOption.EdgeOrderDuplicateEventAllowance;
import org.eclipse.escet.cif.datasynth.options.EdgeOrderForwardOption;
import org.eclipse.escet.cif.datasynth.options.EdgeWorksetAlgoOption;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.datasynth.spec.SynthesisDiscVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisEdge;
import org.eclipse.escet.cif.datasynth.spec.SynthesisInputVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisLocPtrVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisTypedVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrderHelper;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererData;
import org.eclipse.escet.cif.datasynth.varorder.orderers.VarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.parser.VarOrdererParser;
import org.eclipse.escet.cif.datasynth.varorder.parser.VarOrdererTypeChecker;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererInstance;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.InvKind;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.IfUpdate;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Monitors;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.automata.impl.EdgeEventImpl;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Constant;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.declarations.InputVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.AlgVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.ConstantExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.ContVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.ElifExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IfExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.InputVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IntExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.ProjectionExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.SwitchCase;
import org.eclipse.escet.cif.metamodel.cif.expressions.SwitchExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.TauExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.TupleExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator;
import org.eclipse.escet.cif.metamodel.cif.types.BoolType;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.escet.cif.metamodel.cif.types.IntType;
import org.eclipse.escet.common.app.framework.exceptions.InvalidInputException;
import org.eclipse.escet.common.app.framework.exceptions.InvalidOptionException;
import org.eclipse.escet.common.app.framework.exceptions.UnsupportedException;
import org.eclipse.escet.common.box.GridBox;
import org.eclipse.escet.common.box.GridBox.GridBoxLayout;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.escet.setext.runtime.DebugMode;
import org.eclipse.escet.setext.runtime.exceptions.SyntaxException;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;
import com.github.javabdd.BDDFactory;

/** Converter to convert CIF specification to synthesis representation. */
public class CifToSynthesisConverter {
    /** Precondition violations found so far. */
    private final Set<String> problems = set();

    /**
     * Per requirement automaton, the monitors as specified in the original specification. They are replaced by monitors
     * that monitor the entire alphabet of the automaton, in order to be able to treat requirement automata as plants.
     * This mapping is used to restore the original monitors afterwards. The mapping is {@code null} if not yet or no
     * longer available.
     */
    private Map<Automaton, Monitors> originalMonitors;

    /**
     * Converts a CIF specification to a data-based synthesis representation, checking for precondition violations along
     * the way.
     *
     * @param spec The CIF specification to convert. Must not have any component definitions or instantiations.
     * @param factory The BDD factory to use.
     * @param dbgEnabled Whether debug output is enabled.
     * @return The data-based synthesis representation of the CIF specification.
     */
    public SynthesisAutomaton convert(Specification spec, BDDFactory factory, boolean dbgEnabled) {
        // Convert CIF specification and return the resulting synthesis automaton, but only if no precondition
        // violations.
        SynthesisAutomaton aut = convertSpec(spec, factory, dbgEnabled);
        if (problems.isEmpty()) {
            return aut;
        }

        // Precondition violations found.
        String msg = "Data-based supervisory controller synthesis failed due to unsatisfied preconditions:\n - "
                + String.join("\n - ", sortedstrings(problems));
        throw new UnsupportedException(msg);
    }

    /**
     * Converts a CIF specification to a data-based synthesis representation, checking for precondition violations along
     * the way.
     *
     * @param spec The CIF specification to convert. Must not have any component definitions or instantiations.
     * @param factory The BDD factory to use.
     * @param dbgEnabled Whether debug output is enabled.
     * @return The data-based synthesis representation of the CIF specification.
     */
    private SynthesisAutomaton convertSpec(Specification spec, BDDFactory factory, boolean dbgEnabled) {
        // Initialize synthesis automaton.
        SynthesisAutomaton synthAut = new SynthesisAutomaton();
        synthAut.factory = factory;
        synthAut.debugMaxNodes = BddDebugMaxNodesOption.getMaximum();
        synthAut.debugMaxPaths = BddDebugMaxPathsOption.getMaximum();

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Collect and check events. May collect more than union of alphabets.
        List<Event> events = list();
        collectEvents(spec, events);
        for (Event event: events) {
            if (event.getControllable() == null) {
                String msg = fmt("Unsupported event \"%s\": event is not declared as controllable or uncontrollable.",
                        getAbsName(event));
                problems.add(msg);
            }
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Collect automata.
        List<Automaton> automata = list();
        collectAutomata(spec, automata);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Check automata, and partition into plants/requirements.
        List<Automaton> plants = list();
        List<Automaton> requirements = list();
        for (Automaton cifAut: automata) {
            switch (cifAut.getKind()) {
                case PLANT:
                    plants.add(cifAut);
                    break;
                case REQUIREMENT:
                    requirements.add(cifAut);
                    break;
                default: {
                    String msg = fmt("Unsupported automaton \"%s\": only plant and requirement automata are supported.",
                            getAbsName(cifAut));
                    problems.add(msg);
                }
            }
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Need at least one plant automaton.
        if (plants.isEmpty()) {
            String msg = "Unsupported specification: no plant automata found.";
            problems.add(msg);
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Update automata for partitioned ordering.
        automata = concat(plants, requirements);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Get plant and requirement alphabets.
        List<Alphabets> plantAlphabets = listc(plants.size());
        List<Alphabets> reqAlphabets = listc(requirements.size());
        Set<Event> plantAlphabet = set();
        Set<Event> reqAlphabet = set();
        for (Automaton plant: plants) {
            Alphabets alphabets = CifEventUtils.getAllAlphabets(plant, null);
            plantAlphabets.add(alphabets);
            plantAlphabet.addAll(alphabets.syncAlphabet);
            plantAlphabet.addAll(alphabets.sendAlphabet);
            plantAlphabet.addAll(alphabets.recvAlphabet);
        }
        for (Automaton req: requirements) {
            Alphabets alphabets = CifEventUtils.getAllAlphabets(req, null);
            reqAlphabets.add(alphabets);
            reqAlphabet.addAll(alphabets.syncAlphabet);
            reqAlphabet.addAll(alphabets.sendAlphabet);
            reqAlphabet.addAll(alphabets.recvAlphabet);
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Get alphabet for (un)controlled system. We allow events that are only in the alphabet of the requirements, by
        // considering them always enabled in the plant. Since the requirement automata are used as fully monitored
        // plants (full observers) during linearization, the combined linearized edges for such an event always allow
        // that event, in the uncontrolled system.
        synthAut.alphabet = union(plantAlphabet, reqAlphabet);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Check use of channels in requirements.
        for (int i = 0; i < requirements.size(); i++) {
            Set<Event> commEvents = union(reqAlphabets.get(i).sendAlphabet, reqAlphabets.get(i).recvAlphabet);
            if (commEvents.isEmpty()) {
                continue;
            }

            List<String> names = listc(commEvents.size());
            for (Event extra: commEvents) {
                names.add("\"" + getAbsName(extra) + "\"");
            }

            String msg = fmt("Unsupported %s: requirement uses channel%s: %s.",
                    CifTextUtils.getComponentText1(requirements.get(i)), (commEvents.size() == 1) ? "" : "s",
                    String.join(", ", names));
            problems.add(msg);
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Get controllable events subset of the alphabet.
        synthAut.controllables = set();
        for (Event event: synthAut.alphabet) {
            if (event.getControllable() == null) {
                continue;
            }
            if (event.getControllable()) {
                synthAut.controllables.add(event);
            }
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Collect variables, and automata for which we need to create location pointer variables, i.e. the automata
        // with more than one location.
        List<PositionObject> cifVarObjs = list();
        collectVariableObjects(spec, cifVarObjs);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Create location pointer manager.
        List<Automaton> lpAuts = filter(cifVarObjs, Automaton.class);
        CifDataSynthesisLocationPointerManager locPtrManager = new CifDataSynthesisLocationPointerManager(lpAuts);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Check and convert variables. Create location pointers.
        synthAut.variables = new SynthesisVariable[cifVarObjs.size()];
        for (int i = 0; i < synthAut.variables.length; i++) {
            PositionObject cifVarObj = cifVarObjs.get(i);
            if (cifVarObj instanceof Automaton) {
                Automaton lpAut = (Automaton)cifVarObj;
                DiscVariable lpVar = locPtrManager.getLocationPointer(lpAut);
                synthAut.variables[i] = createLpVar(lpAut, lpVar);
            } else {
                synthAut.variables[i] = convertTypedVar((Declaration)cifVarObj);
            }
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Order variables and create domains.
        orderVars(synthAut, spec, dbgEnabled);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        createVarDomains(synthAut);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Create auxiliary data for updates.
        createUpdateAuxiliaries(synthAut);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Check and convert initialization predicates.
        synthAut.initialsVars = listc(synthAut.variables.length);
        for (int i = 0; i < synthAut.variables.length; i++) {
            synthAut.initialsVars.add(null);
        }
        synthAut.initialsComps = list();
        synthAut.initialsLocs = list();
        synthAut.initialVars = synthAut.factory.one();
        synthAut.initialComps = synthAut.factory.one();
        synthAut.initialLocs = synthAut.factory.one();
        convertInit(spec, synthAut, locPtrManager);
        BDD initialCompsAndLocs = synthAut.initialComps.and(synthAut.initialLocs);
        synthAut.initialUnctrl = synthAut.initialVars.and(initialCompsAndLocs);
        initialCompsAndLocs.free();

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Check and convert marker predicates.
        synthAut.markedsComps = list();
        synthAut.markedsLocs = list();
        synthAut.markedComps = synthAut.factory.one();
        synthAut.markedLocs = synthAut.factory.one();
        convertMarked(spec, synthAut, locPtrManager);
        synthAut.marked = synthAut.markedComps.and(synthAut.markedLocs);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Initialize state plant invariants (predicates).
        synthAut.plantInvsComps = list();
        synthAut.plantInvsLocs = list();
        synthAut.plantInvComps = synthAut.factory.one();
        synthAut.plantInvLocs = synthAut.factory.one();

        // Initialize state requirement invariants (predicates).
        synthAut.reqInvsComps = list();
        synthAut.reqInvsLocs = list();
        synthAut.reqInvComps = synthAut.factory.one();
        synthAut.reqInvLocs = synthAut.factory.one();

        // Convert state invariants.
        convertStateInvs(spec, synthAut, locPtrManager);

        // Determine state plant invariant for the system, combination of the state plant invariant for the components
        // and the state plant invariant for the locations of the automata.
        synthAut.plantInv = synthAut.plantInvComps.and(synthAut.plantInvLocs);

        // Determine state requirement invariant for the system, combination of the state requirement invariant for the
        // components and the state requirement invariant for the locations of automata.
        synthAut.reqInv = synthAut.reqInvComps.and(synthAut.reqInvLocs);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Set combined predicate for initialization with state invariants.
        synthAut.initialPlantInv = synthAut.initialUnctrl.and(synthAut.plantInv);
        synthAut.initialInv = synthAut.initialPlantInv.and(synthAut.reqInv);

        // Set combined predicate for marking with state invariants.
        synthAut.markedPlantInv = synthAut.marked.and(synthAut.plantInv);
        synthAut.markedInv = synthAut.markedPlantInv.and(synthAut.reqInv);

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Initialize state/event plant exclusion conditions for events.
        synthAut.stateEvtExclPlants = mapc(synthAut.alphabet.size());
        synthAut.stateEvtExclPlantLists = mapc(synthAut.alphabet.size());
        for (Event event: synthAut.alphabet) {
            synthAut.stateEvtExclPlants.put(event, synthAut.factory.one());
            synthAut.stateEvtExclPlantLists.put(event, list());
        }

        // Initialize state/event requirement exclusion conditions for controllable events.
        synthAut.stateEvtExclsReqAuts = mapc(synthAut.controllables.size());
        synthAut.stateEvtExclsReqInvs = mapc(synthAut.controllables.size());
        for (Event event: synthAut.controllables) {
            synthAut.stateEvtExclsReqAuts.put(event, synthAut.factory.one());
            synthAut.stateEvtExclsReqInvs.put(event, synthAut.factory.one());
        }

        // Initialize state/event requirement exclusion conditions for events.
        synthAut.stateEvtExclReqs = mapc(synthAut.alphabet.size());
        synthAut.stateEvtExclReqLists = mapc(synthAut.alphabet.size());
        for (Event event: synthAut.alphabet) {
            synthAut.stateEvtExclReqs.put(event, synthAut.factory.one());
            synthAut.stateEvtExclReqLists.put(event, list());
        }

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Convert state/event exclusion invariants.
        convertStateEvtExclInvs(spec, synthAut, locPtrManager);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Preconvert requirement automata, to enable treating them as plants from here on.
        preconvertReqAuts(requirements, reqAlphabets, synthAut);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Convert plant and requirement automata.
        convertPlantReqAuts(plants, requirements, plantAlphabets, reqAlphabets, locPtrManager, synthAut);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Restore original monitors of the requirement automata.
        for (Entry<Automaton, Monitors> entry: originalMonitors.entrySet()) {
            entry.getKey().setMonitors(entry.getValue());
        }
        originalMonitors = null;

        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Add events and edges for input variables.
        addInputVariableEdges(synthAut);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Merge edges to the desired granularity.
        mergeEdges(synthAut);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Order the synthesis edges.
        orderEdges(synthAut);
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Check edge workset algorithm options.
        checkEdgeWorksetAlgorithmOptions();
        if (synthAut.env.isTerminationRequested()) {
            return synthAut;
        }

        // Return the conversion result, the synthesis automaton.
        return synthAut;
    }

    /**
     * Converts a CIF variable to a synthesis variable.
     *
     * @param var The CIF variable. Must be a {@link DiscVariable} or a {@link InputVariable}.
     * @return The synthesis variable, or {@code null} if conversion failed due to a precondition not being satisfied.
     */
    private SynthesisVariable convertTypedVar(Declaration var) {
        // Get normalized type of the variable.
        CifType type;
        if (var instanceof DiscVariable) {
            type = ((DiscVariable)var).getType();
        } else if (var instanceof InputVariable) {
            type = ((InputVariable)var).getType();
        } else {
            throw new RuntimeException("Unexpected variable: " + var);
        }
        type = normalizeType(type);

        // Get variable information, based on type.
        int count;
        int lower;
        int upper;
        if (type instanceof BoolType) {
            // Represent as 'int[0..1]', with '0' and '1' for 'false' and 'true', respectively.
            count = 2;
            lower = 0;
            upper = 1;
        } else if (type instanceof IntType) {
            // Check ranged integer type.
            IntType intType = (IntType)type;
            if (CifTypeUtils.isRangeless(intType)) {
                String msg = fmt(
                        "Unsupported variable \"%s\": variables with rangeless integer types are not supported.",
                        getAbsName(var));
                problems.add(msg);
                return null;
            }

            // Check range.
            lower = intType.getLower();
            upper = intType.getUpper();
            if (lower < 0) {
                String msg = fmt("Unsupported variable \"%s\": variables with integer type ranges that include "
                        + "negative values are not supported.", getAbsName(var));
                problems.add(msg);
                return null;
            }

            // Return synthesis variable.
            count = upper - lower + 1;
        } else if (type instanceof EnumType) {
            // Represent as 'int[0..n-1]' for an enumeration with 'n' literals.
            EnumDecl enumDecl = ((EnumType)type).getEnum();
            count = enumDecl.getLiterals().size();
            lower = 0;
            upper = count - 1;
        } else {
            // Unsupported.
            String msg = fmt("Unsupported variable \"%s\": variables of type \"%s\" are not supported.",
                    getAbsName(var), CifTextUtils.typeToStr(type));
            problems.add(msg);
            return null;
        }

        // Construct synthesis variable.
        if (var instanceof DiscVariable) {
            DiscVariable discVar = (DiscVariable)var;
            return new SynthesisDiscVariable(discVar, type, count, lower, upper);
        } else if (var instanceof InputVariable) {
            InputVariable inputVar = (InputVariable)var;
            return new SynthesisInputVariable(inputVar, type, count, lower, upper);
        } else {
            throw new RuntimeException("Unexpected variable: " + var);
        }
    }

    /**
     * Creates a synthesis location pointer variable for a CIF automaton.
     *
     * @param aut The CIF automaton.
     * @param var The dummy, internally-created location pointer variable for the CIF automaton. Does not have a type.
     * @return The synthesis variable.
     */
    private SynthesisVariable createLpVar(Automaton aut, DiscVariable var) {
        int count = aut.getLocations().size();
        Assert.check(count > 1);
        return new SynthesisLocPtrVariable(aut, var);
    }

    /**
     * Orders the synthesis variables. Also sets the {@link SynthesisVariable#group group} of each synthesis variable.
     *
     * @param synthAut The synthesis automaton.
     * @param spec The CIF specification.
     * @param dbgEnabled Whether debug output is enabled.
     */
    private void orderVars(SynthesisAutomaton synthAut, Specification spec, boolean dbgEnabled) {
        // Skip ordering, including option processing and debug output printing, if any variables failed to convert.
        if (Arrays.asList(synthAut.variables).contains(null)) {
            return;
        }

        // Configure variable orderer.
        String varOrderTxt = BddAdvancedVariableOrderOption.getOrder();
        List<VarOrdererInstance> parseResult;
        try {
            parseResult = new VarOrdererParser().parseString(varOrderTxt, "/in-memory.varorder", null, DebugMode.NONE);
        } catch (SyntaxException ex) {
            throw new InvalidOptionException("Invalid BDD variable ordering configuration.", ex);
        }

        VarOrdererTypeChecker typeChecker = new VarOrdererTypeChecker(Arrays.asList(synthAut.variables));
        VarOrderer varOrderer = typeChecker.typeCheck(parseResult);
        Assert.check(!typeChecker.hasWarning());
        if (varOrderer == null) {
            Assert.check(typeChecker.hasError());
            Assert.check(typeChecker.getProblems().size() == 1);
            InvalidOptionException ex = new InvalidOptionException(typeChecker.getProblems().get(0).toString());
            throw new InvalidOptionException("Invalid BDD variable ordering configuration.", ex);
        }

        // Skip ordering, including debug output printing, if no variables are present.
        int varCnt = synthAut.variables.length;
        if (varCnt == 0) {
            return;
        }

        // Initialize to model variable order without interleaving.
        for (int i = 0; i < synthAut.variables.length; i++) {
            synthAut.variables[i].group = i;
        }

        // Print variable debugging information, before ordering.
        if (dbgEnabled) {
            debugCifVars(synthAut);
        }

        // Only apply variable ordering if there are at least two variables (to order).
        if (synthAut.variables.length < 2) {
            if (dbgEnabled) {
                dbg();
                dbg("Skipping variable ordering: only one variable present.");
                dbg();
            }
            return;
        }

        // Create variable order helper, based on model order.
        List<SynthesisVariable> varsInModelOrder = Collections.unmodifiableList(Arrays.asList(synthAut.variables));
        VarOrderHelper helper = new VarOrderHelper(spec, varsInModelOrder);

        // Get current variable order, which is model order.
        VarOrder curOrder = VarOrder.createFromOrderedVars(varsInModelOrder);
        List<SynthesisVariable> varsInCurOrder = curOrder.getOrderedVars();

        // Create variable order data for input to first orderer.
        VarOrdererData data = new VarOrdererData(varsInModelOrder, curOrder, helper);

        // Get new variable order.
        if (dbgEnabled) {
            dbg();
            dbg("Applying variable ordering:");
        }
        VarOrdererData orderingResult = varOrderer.order(data, dbgEnabled, 1);
        VarOrder newOrder = orderingResult.varOrder;
        List<SynthesisVariable> varsInNewOrder = newOrder.getOrderedVars();

        // Check sanity of current and new variable orders.
        Assert.check(curOrder.getVarOrder().stream().allMatch(grp -> !grp.isEmpty())); // No empty groups.
        Assert.check(newOrder.getVarOrder().stream().allMatch(grp -> !grp.isEmpty())); // No empty groups.
        Assert.areEqual(varsInCurOrder.size(), list2set(varsInCurOrder).size()); // No duplicates.
        Assert.areEqual(varsInNewOrder.size(), list2set(varsInNewOrder).size()); // No duplicates.
        Assert.areEqual(varsInCurOrder.size(), varsInNewOrder.size()); // Same number of variables.
        Assert.areEqual(list2set(varsInCurOrder), list2set(varsInNewOrder)); // Same variables.

        // Update the variable order of the synthesis automaton.
        synthAut.variables = varsInNewOrder.toArray(n -> new SynthesisVariable[n]);
        for (int i = 0; i < newOrder.getVarOrder().size(); i++) {
            List<SynthesisVariable> group = newOrder.getVarOrder().get(i);
            for (SynthesisVariable var: group) {
                var.group = i;
            }
        }

        // If the new order differs from the current order, print updated variable debugging information.
        if (dbgEnabled) {
            boolean orderChanged = !curOrder.equals(newOrder);
            dbg();
            dbg("Variable order %schanged.", orderChanged ? "" : "un");
            if (orderChanged) {
                debugCifVars(synthAut);
            }
            dbg();
        }
    }

    /**
     * Prints CIF variable information. Should only be invoked if debug output is enabled.
     *
     * @param aut The synthesis automaton.
     */
    private static void debugCifVars(SynthesisAutomaton aut) {
        // Get variable counts.
        int cifVarCnt = aut.variables.length;

        // Initialize grid and header.
        GridBox grid = new GridBox(cifVarCnt + 4, 9, 0, 2);
        grid.set(0, 0, "Nr");
        grid.set(0, 1, "Kind");
        grid.set(0, 2, "Type");
        grid.set(0, 3, "Name");
        grid.set(0, 4, "Group");
        grid.set(0, 5, "BDD vars");
        grid.set(0, 6, "CIF values");
        grid.set(0, 7, "BDD values");
        grid.set(0, 8, "Values used");

        // Fill grid with variable information.
        Set<Integer> groups = set();
        int totalBddVarCnt = 0;
        int totalUsedCnt = 0;
        int totalReprCnt = 0;
        for (int i = 0; i < cifVarCnt; i++) {
            // Get synthesis variable.
            SynthesisVariable var = aut.variables[i];

            // Get type text.
            String typeTxt = var.getTypeText();
            if (typeTxt == null) {
                typeTxt = "n/a";
            }

            // Get some counts.
            int bddCnt = var.getBddVarCount();
            int usedCnt = var.count;
            int reprCnt = 1 << bddCnt;

            // Fill grid row.
            grid.set(i + 2, 0, str(i + 1));
            grid.set(i + 2, 1, var.getKindText());
            grid.set(i + 2, 2, typeTxt);
            grid.set(i + 2, 3, var.name);
            grid.set(i + 2, 4, str(var.group));
            grid.set(i + 2, 5, str(bddCnt) + " * 2");
            grid.set(i + 2, 6, str(usedCnt) + " * 2");
            grid.set(i + 2, 7, str(reprCnt) + " * 2");
            grid.set(i + 2, 8, getPercentageText(usedCnt, reprCnt));

            // Update totals.
            groups.add(var.group);
            totalBddVarCnt += bddCnt * 2;
            totalUsedCnt += usedCnt * 2;
            totalReprCnt += reprCnt * 2;
        }

        // Fill grid with totals.
        grid.set(cifVarCnt + 3, 0, "Total");
        grid.set(cifVarCnt + 3, 1, "");
        grid.set(cifVarCnt + 3, 2, "");
        grid.set(cifVarCnt + 3, 3, "");
        grid.set(cifVarCnt + 3, 4, str(groups.size()));
        grid.set(cifVarCnt + 3, 5, str(totalBddVarCnt));
        grid.set(cifVarCnt + 3, 6, str(totalUsedCnt));
        grid.set(cifVarCnt + 3, 7, str(totalReprCnt));
        grid.set(cifVarCnt + 3, 8, getPercentageText(totalUsedCnt, totalReprCnt));

        // Fill separation rows.
        GridBoxLayout layout = grid.computeLayout();
        for (int i = 0; i < layout.numCols; i++) {
            String bar = Strings.duplicate("-", layout.widths[i]);
            grid.set(1, i, bar);
            grid.set(cifVarCnt + 2, i, bar);
        }

        // Print the variable information, for debugging.
        dbg();
        dbg("CIF variables and location pointers:");
        for (String line: grid.getLines()) {
            dbg("  " + line);
        }
    }

    /**
     * Returns the percentage of a part with respect to a total, as end-user readable text.
     *
     * @param part The part value.
     * @param total The total value.
     * @return The percentage as integer number in the range [0..100], followed by a {@code "%"} sign, and preceded by a
     *     {@code "~"} if the integer percentage is rounded from a non-integer percentage. If the percentage can not be
     *     computed, e.g. because 'total' is zero, the result is {@code "n/a"}.
     */
    private static String getPercentageText(int part, int total) {
        double percentage = (100.0 * part) / total;
        if (Double.isNaN(percentage)) {
            return "n/a";
        }
        String txt = fmt("%.0f", percentage);
        if ((int)Math.floor(percentage) != percentage) {
            txt = "~" + txt;
        }
        txt += "%";
        return txt;
    }

    /**
     * Creates and sets BDD {@link SynthesisVariable#domain domains} for all the synthesis variables.
     *
     * @param synthAut The synthesis automaton.
     */
    private void createVarDomains(SynthesisAutomaton synthAut) {
        // Skip if no variables (due to earlier conversion error).
        int varCnt = synthAut.variables.length;
        if (varCnt == 0) {
            return;
        }

        // If not ordered (due to earlier conversion error), set dummy domains, but of the correct size to prevent
        // errors later on.
        boolean ordered = true;
        for (int i = 0; i < varCnt; i++) {
            SynthesisVariable var = synthAut.variables[i];
            if (var == null || var.group == -1) {
                ordered = false;
                break;
            }
        }

        if (!ordered) {
            for (int i = 0; i < varCnt; i++) {
                SynthesisVariable var = synthAut.variables[i];
                if (var == null) {
                    continue;
                }
                int size = var.getDomainSize();
                var.domain = synthAut.factory.extDomain(size);
                var.domainNew = synthAut.factory.extDomain(size);
                var.group = i;
            }
            return;
        }

        // Make sure the synthesis variable domain interleaving groups are ascending and continuous.
        int cur = 0;
        for (int i = 0; i < varCnt; i++) {
            int group = synthAut.variables[i].group;
            if (group == cur) {
                continue;
            }
            if (group == cur + 1) {
                cur = group;
                continue;
            }
            Assert.fail(fmt("Invalid cur/group: %d/%d.", cur, group));
        }

        // Count number of synthesis variables per group of interleaving domains.
        SynthesisVariable lastVar = synthAut.variables[varCnt - 1];
        int[] counts = new int[lastVar.group + 1];
        for (int i = 0; i < varCnt; i++) {
            counts[synthAut.variables[i].group]++;
        }

        // Create and set domains, per group of interleaving domains.
        int offset = 0;
        for (int grpIdx = 0; grpIdx < counts.length; grpIdx++) {
            // Get domain sizes, for variables in the group.
            int grpVarCnt = counts[grpIdx];
            int[] sizes = new int[grpVarCnt * 2];
            for (int varIdx = 0; varIdx < grpVarCnt; varIdx++) {
                int size = synthAut.variables[offset + varIdx].getDomainSize();
                sizes[(2 * varIdx) + 0] = size;
                sizes[(2 * varIdx) + 1] = size;
            }

            // Create domains.
            BDDDomain[] domains = synthAut.factory.extDomain(sizes);

            // Set domains.
            for (int varIdx = 0; varIdx < grpVarCnt; varIdx++) {
                synthAut.variables[offset + varIdx].domain = domains[(2 * varIdx) + 0];
                synthAut.variables[offset + varIdx].domainNew = domains[(2 * varIdx) + 1];
            }

            // Proceed with next group of interleaving domains.
            offset += grpVarCnt;
        }
    }

    /**
     * Create auxiliary data for updates.
     *
     * @param synthAut The synthesis automaton. Is modified in-place.
     * @see SynthesisAutomaton#oldToNewVarsPairing
     * @see SynthesisAutomaton#newToOldVarsPairing
     * @see SynthesisAutomaton#varSetOld
     * @see SynthesisAutomaton#varSetNew
     */
    private void createUpdateAuxiliaries(SynthesisAutomaton synthAut) {
        // Skip if earlier conversion failure.
        int domainCnt = synthAut.factory.numberOfDomains();
        int cifVarCnt = synthAut.variables.length;
        if (cifVarCnt * 2 != domainCnt) {
            return;
        }

        // oldToNewVarsPairing = 'x -> x+, y -> y+, z -> z+, ...'.
        // newToOldVarsPairing = 'x+ -> x, y+ -> y, z+ -> z, ...'.
        BDDDomain[] oldDomains = new BDDDomain[cifVarCnt];
        BDDDomain[] newDomains = new BDDDomain[cifVarCnt];
        for (int i = 0; i < cifVarCnt; i++) {
            oldDomains[i] = synthAut.variables[i].domain;
            newDomains[i] = synthAut.variables[i].domainNew;
        }
        synthAut.oldToNewVarsPairing = synthAut.factory.makePair();
        synthAut.newToOldVarsPairing = synthAut.factory.makePair();
        synthAut.oldToNewVarsPairing.set(oldDomains, newDomains);
        synthAut.newToOldVarsPairing.set(newDomains, oldDomains);

        if (synthAut.env.isTerminationRequested()) {
            return;
        }

        // varSetOld = {x, y, z, ...}
        // varSetNew = {x+, y+, z+, ...}
        int bddVarCnt = synthAut.factory.varNum();
        Assert.check(bddVarCnt % 2 == 0);
        int[] varIdxsOld = new int[bddVarCnt / 2];
        int[] varIdxsNew = new int[bddVarCnt / 2];
        int varIdx = 0;
        for (int i = 0; i < oldDomains.length; i++) {
            BDDDomain oldDomain = oldDomains[i];
            BDDDomain newDomain = newDomains[i];
            int[] domainVarIdxsOld = oldDomain.vars();
            int[] domainVarIdxsNew = newDomain.vars();
            System.arraycopy(domainVarIdxsOld, 0, varIdxsOld, varIdx, domainVarIdxsOld.length);
            System.arraycopy(domainVarIdxsNew, 0, varIdxsNew, varIdx, domainVarIdxsNew.length);
            varIdx += domainVarIdxsOld.length;
        }
        synthAut.varSetOld = synthAut.factory.makeSet(varIdxsOld);
        synthAut.varSetNew = synthAut.factory.makeSet(varIdxsNew);

        if (synthAut.env.isTerminationRequested()) {
            return;
        }
    }

    /**
     * Converts initialization predicates from the components, initialization predicates from the locations of automata,
     * and the initial values of the variables.
     *
     * @param comp The component for which to convert initialization, recursively.
     * @param synthAut The synthesis automaton to be updated with initialization information.
     * @param locPtrManager Location pointer manager.
     */
    private void convertInit(ComplexComponent comp, SynthesisAutomaton synthAut, LocationPointerManager locPtrManager) {
        // Initialization predicates of the component.
        for (Expression pred: comp.getInitials()) {
            // Convert.
            BDD initial;
            try {
                initial = convertPred(pred, true, synthAut);
            } catch (UnsupportedPredicateException ex) {
                if (ex.expr != null) {
                    String msg = fmt("Unsupported %s: unsupported part \"%s\" of initialization predicate \"%s\": %s",
                            CifTextUtils.getComponentText1(comp), CifTextUtils.exprToStr(ex.expr),
                            CifTextUtils.exprToStr(pred), ex.getMessage());
                    problems.add(msg);
                }
                continue;
            }

            // Store.
            synthAut.initialsComps.add(initial);
            synthAut.initialComps = synthAut.initialComps.andWith(initial.id());
        }

        // Initial values of discrete variables (automata only).
        if (comp instanceof Automaton) {
            for (Declaration cifDecl: comp.getDeclarations()) {
                // Skip all but discrete variables.
                if (!(cifDecl instanceof DiscVariable)) {
                    continue;
                }
                DiscVariable cifVar = (DiscVariable)cifDecl;

                // Get synthesis variable. Skip if earlier precondition violation.
                int varIdx = getDiscVarIdx(synthAut.variables, cifVar);
                if (varIdx == -1) {
                    continue;
                }
                SynthesisVariable synthVar = synthAut.variables[varIdx];
                Assert.check(synthVar instanceof SynthesisDiscVariable);
                SynthesisDiscVariable var = (SynthesisDiscVariable)synthVar;

                // Get initial value expressions. Use 'null' to indicate any value in the CIF domain.
                List<Expression> values;
                if (cifVar.getValue() == null) {
                    // Default initial value.
                    CifType type = cifVar.getType();
                    values = list(CifValueUtils.getDefaultValue(type, null));
                } else if (cifVar.getValue().getValues().isEmpty()) {
                    // Any value in its domain.
                    values = null;
                } else {
                    // One or more specific initialization values.
                    values = cifVar.getValue().getValues();
                }

                // Create initialization predicate for the discrete variable.
                BDD pred;
                if (values == null) {
                    // Any value in its domain.
                    pred = BddUtils.getVarDomain(var, false, synthAut.factory);
                } else {
                    // Specific values.
                    pred = synthAut.factory.zero();
                    for (Expression value: values) {
                        // Case distinction on types of values.
                        if (var.type instanceof BoolType) {
                            // Convert right hand side (value to assign).
                            BDD valueBdd;
                            try {
                                valueBdd = convertPred(value, true, synthAut);
                            } catch (UnsupportedPredicateException ex) {
                                // Add new problem, if not failed due to earlier problems.
                                if (ex.expr != null) {
                                    String msg = fmt(
                                            "Unsupported variable \"%s\": unsupported part \"%s\" of initial "
                                                    + "value \"%s\": %s",
                                            var.name, CifTextUtils.exprToStr(ex.expr), CifTextUtils.exprToStr(value),
                                            ex.getMessage());
                                    problems.add(msg);
                                }

                                // Set predicate to 'true' to prevent no initialization.
                                pred.free();
                                pred = synthAut.factory.one();

                                // Proceed with next value to check it as well.
                                continue;
                            }

                            // Create BDD for the left hand side (variable to get a new value).
                            Assert.check(var.domain.varNum() == 1);
                            int varVar = var.domain.vars()[0];
                            BDD varBdd = synthAut.factory.ithVar(varVar);

                            // Construct 'var = value' relation.
                            BDD relation = varBdd.biimpWith(valueBdd);

                            // Update initialization predicate for the variable.
                            pred = pred.orWith(relation);
                        } else {
                            // Convert value expression.
                            Supplier<String> partMsg = () -> fmt("initial value \"%s\" of variable \"%s\".",
                                    CifTextUtils.exprToStr(value), var.name);

                            CifBddBitVectorAndCarry valueRslt;
                            try {
                                valueRslt = convertExpr(value, true, synthAut, false, partMsg);
                            } catch (UnsupportedPredicateException ex) {
                                // Add new problem, if not failed due to earlier problems.
                                if (ex.expr != null) {
                                    String msg = fmt(
                                            "Unsupported variable \"%s\": unsupported part \"%s\" of initial "
                                                    + "value \"%s\": %s",
                                            var.name, CifTextUtils.exprToStr(ex.expr), CifTextUtils.exprToStr(value),
                                            ex.getMessage());
                                    problems.add(msg);
                                }

                                // Set predicate to 'true' to prevent no initialization.
                                pred.free();
                                pred = synthAut.factory.one();

                                // Proceed with next value to check it as well.
                                continue;
                            }
                            CifBddBitVector valueVec = valueRslt.vector;
                            Assert.check(valueRslt.carry.isZero());

                            // Create bit vector for the variable.
                            CifBddBitVector varVec = CifBddBitVector.createDomain(var.domain);

                            // Construct 'var = value' relation.
                            Assert.check(varVec.length() >= valueVec.length());
                            valueVec.resize(varVec.length());
                            BDD relation = varVec.equalTo(valueVec);
                            varVec.free();
                            valueVec.free();

                            // Update initialization predicate for the variable.
                            pred = pred.orWith(relation);
                        }
                    }
                }

                // Store initialization.
                synthAut.initialsVars.set(varIdx, pred);
                synthAut.initialVars = synthAut.initialVars.andWith(pred.id());
            }
        }

        // Initialization predicates of locations (automata only).
        if (comp instanceof Automaton) {
            // Get automaton.
            Automaton aut = (Automaton)comp;

            // Combine initialization predicates from the locations.
            BDD autInit = synthAut.factory.zero();
            for (Location loc: aut.getLocations()) {
                // Skip location without initialization predicates (implicit 'false').
                if (loc.getInitials().isEmpty()) {
                    continue;
                }

                // Convert initialization predicates of the location.
                BDD locInit = synthAut.factory.one();
                List<Expression> locInits = loc.getInitials();
                try {
                    locInit = convertPreds(locInits, true, synthAut);
                } catch (UnsupportedPredicateException ex) {
                    if (ex.expr != null) {
                        String msg = fmt(
                                "Unsupported %s: unsupported part \"%s\" of initialization predicate(s) \"%s\": %s",
                                CifTextUtils.getLocationText1(loc), CifTextUtils.exprToStr(ex.expr),
                                CifTextUtils.exprsToStr(locInits), ex.getMessage());
                        problems.add(msg);
                    }
                    continue;
                }

                // Add location predicate.
                BDD srcLocPred;
                try {
                    Expression srcLocRef = locPtrManager.createLocRef(loc);
                    srcLocPred = convertPred(srcLocRef, true, synthAut);
                } catch (UnsupportedPredicateException ex) {
                    if (ex.expr != null) {
                        // Internally created predicate shouldn't fail conversion.
                        throw new RuntimeException(ex);
                    }
                    continue;
                }

                locInit = locInit.and(srcLocPred);

                // Combine with initialization predicates of other locations.
                autInit = autInit.or(locInit);
            }

            // Store.
            synthAut.initialsLocs.add(autInit);
            synthAut.initialLocs = synthAut.initialLocs.andWith(autInit.id());
        }

        // Proceed recursively (groups only).
        if (comp instanceof Group) {
            for (Component child: ((Group)comp).getComponents()) {
                convertInit((ComplexComponent)child, synthAut, locPtrManager);
            }
        }
    }

    /**
     * Converts marker predicates from the components and marker predicates from the locations of automata.
     *
     * @param comp The component for which to convert marking, recursively.
     * @param synthAut The synthesis automaton to be updated with marking information.
     * @param locPtrManager Location pointer manager.
     */
    private void convertMarked(ComplexComponent comp, SynthesisAutomaton synthAut,
            LocationPointerManager locPtrManager)
    {
        // Marker predicates of the component.
        for (Expression pred: comp.getMarkeds()) {
            // Convert.
            BDD marked;
            try {
                marked = convertPred(pred, false, synthAut);
            } catch (UnsupportedPredicateException ex) {
                if (ex.expr != null) {
                    String msg = fmt("Unsupported %s: unsupported part \"%s\" of marker predicate \"%s\": %s",
                            CifTextUtils.getComponentText1(comp), CifTextUtils.exprToStr(ex.expr),
                            CifTextUtils.exprToStr(pred), ex.getMessage());
                    problems.add(msg);
                }
                continue;
            }

            // Store.
            synthAut.markedsComps.add(marked);
            synthAut.markedComps = synthAut.markedComps.andWith(marked.id());
        }

        // Marker predicates of locations (automata only).
        if (comp instanceof Automaton) {
            // Get automaton.
            Automaton aut = (Automaton)comp;

            // Combine marker predicates from the locations.
            BDD autMarked = synthAut.factory.zero();
            for (Location loc: aut.getLocations()) {
                // Skip location without marker predicates (implicit 'false').
                if (loc.getMarkeds().isEmpty()) {
                    continue;
                }

                // Convert marker predicates of the location.
                BDD locMarked = synthAut.factory.one();
                List<Expression> locMarkeds = loc.getMarkeds();
                try {
                    locMarked = convertPreds(locMarkeds, false, synthAut);
                } catch (UnsupportedPredicateException ex) {
                    if (ex.expr != null) {
                        String msg = fmt("Unsupported %s: unsupported part \"%s\" of marker predicate(s) \"%s\": %s",
                                CifTextUtils.getLocationText1(loc), CifTextUtils.exprToStr(ex.expr),
                                CifTextUtils.exprsToStr(locMarkeds), ex.getMessage());
                        problems.add(msg);
                    }
                    continue;
                }

                // Add location predicate.
                BDD srcLocPred;
                try {
                    Expression srcLocRef = locPtrManager.createLocRef(loc);
                    srcLocPred = convertPred(srcLocRef, false, synthAut);
                } catch (UnsupportedPredicateException ex) {
                    if (ex.expr != null) {
                        // Internally created predicate shouldn't fail conversion.
                        throw new RuntimeException(ex);
                    }
                    continue;
                }

                locMarked = locMarked.andWith(srcLocPred);

                // Combine with marker predicates of other locations.
                autMarked = autMarked.orWith(locMarked);
            }

            // Store.
            synthAut.markedsLocs.add(autMarked);
            synthAut.markedLocs = synthAut.markedLocs.andWith(autMarked.id());
        }

        // Proceed recursively (groups only).
        if (comp instanceof Group) {
            for (Component child: ((Group)comp).getComponents()) {
                convertMarked((ComplexComponent)child, synthAut, locPtrManager);
            }
        }
    }

    /**
     * Converts state invariants (predicates) from the components and the locations of automata.
     *
     * @param comp The component for which to convert state invariants (predicates), recursively.
     * @param synthAut The synthesis automaton to be updated with state invariants (predicates) information.
     * @param locPtrManager Location pointer manager.
     */
    private void convertStateInvs(ComplexComponent comp, SynthesisAutomaton synthAut,
            LocationPointerManager locPtrManager)
    {
        // State invariants (predicates) of the component.
        for (Invariant inv: comp.getInvariants()) {
            // Skip non-state invariants.
            if (inv.getInvKind() != InvKind.STATE) {
                continue;
            }

            // Check kind.
            if (inv.getSupKind() != SupKind.PLANT && inv.getSupKind() != SupKind.REQUIREMENT) {
                String msg = fmt(
                        "Unsupported %s: for state invariants, only plant and requirement invariants are supported.",
                        CifTextUtils.getComponentText1(comp));
                problems.add(msg);
                continue;
            }

            // Convert.
            Expression pred = inv.getPredicate();
            BDD invComp;
            try {
                invComp = convertPred(pred, false, synthAut);
            } catch (UnsupportedPredicateException ex) {
                if (ex.expr != null) {
                    String msg = fmt("Unsupported %s: unsupported part \"%s\" of state invariant \"%s\": %s",
                            CifTextUtils.getComponentText1(comp), CifTextUtils.exprToStr(ex.expr),
                            CifTextUtils.exprToStr(pred), ex.getMessage());
                    problems.add(msg);
                }
                continue;
            }

            // Store.
            switch (inv.getSupKind()) {
                case PLANT:
                    synthAut.plantInvsComps.add(invComp);
                    synthAut.plantInvComps = synthAut.plantInvComps.andWith(invComp.id());
                    break;
                case REQUIREMENT:
                    synthAut.reqInvsComps.add(invComp);
                    synthAut.reqInvComps = synthAut.reqInvComps.andWith(invComp.id());
                    break;
                default:
                    throw new RuntimeException("Unexpected kind: " + inv.getSupKind());
            }
        }

        // State invariants (predicates) of locations (automata only).
        if (comp instanceof Automaton) {
            // Get automaton.
            Automaton aut = (Automaton)comp;

            // Add state invariants (predicates) from the locations.
            for (Location loc: aut.getLocations()) {
                for (Invariant inv: loc.getInvariants()) {
                    // Skip non-state invariants.
                    if (inv.getInvKind() != InvKind.STATE) {
                        continue;
                    }

                    // Check kind.
                    if (inv.getSupKind() != SupKind.PLANT && inv.getSupKind() != SupKind.REQUIREMENT) {
                        String msg = fmt(
                                "Unsupported %s: for state invariants, only plant and requirement invariants are "
                                        + "supported.",
                                CifTextUtils.getLocationText1(loc));
                        problems.add(msg);
                        continue;
                    }

                    // Convert.
                    Expression pred = inv.getPredicate();
                    BDD invLoc;
                    try {
                        invLoc = convertPred(pred, false, synthAut);
                    } catch (UnsupportedPredicateException ex) {
                        if (ex.expr != null) {
                            String msg = fmt("Unsupported %s: unsupported part \"%s\" of state invariant \"%s\": %s",
                                    CifTextUtils.getLocationText1(loc), CifTextUtils.exprToStr(ex.expr),
                                    CifTextUtils.exprToStr(pred), ex.getMessage());
                            problems.add(msg);
                        }
                        continue;
                    }

                    // Add location predicate (srcLocPred => locInv).
                    BDD srcLocPred;
                    try {
                        Expression srcLocRef = locPtrManager.createLocRef(loc);
                        srcLocPred = convertPred(srcLocRef, false, synthAut);
                    } catch (UnsupportedPredicateException ex) {
                        if (ex.expr != null) {
                            // Internally created predicate shouldn't fail conversion.
                            throw new RuntimeException(ex);
                        }
                        continue;
                    }

                    invLoc = srcLocPred.not().orWith(invLoc);
                    srcLocPred.free();

                    // Store.
                    switch (inv.getSupKind()) {
                        case PLANT:
                            synthAut.plantInvsLocs.add(invLoc);
                            synthAut.plantInvLocs = synthAut.plantInvLocs.andWith(invLoc.id());
                            break;
                        case REQUIREMENT:
                            synthAut.reqInvsLocs.add(invLoc);
                            synthAut.reqInvLocs = synthAut.reqInvLocs.andWith(invLoc.id());
                            break;
                        default:
                            throw new RuntimeException("Unexpected kind: " + inv.getSupKind());
                    }
                }
            }
        }

        // Proceed recursively (groups only).
        if (comp instanceof Group) {
            for (Component child: ((Group)comp).getComponents()) {
                convertStateInvs((ComplexComponent)child, synthAut, locPtrManager);
            }
        }
    }

    /**
     * Converts state/event exclusion invariants (both plant and requirement) from the components and the locations of
     * automata.
     *
     * @param comp The component for which to convert state/event exclusion invariants, recursively.
     * @param synthAut The synthesis automaton to be updated with state/event exclusion invariants information.
     * @param locPtrManager Location pointer manager.
     */
    private void convertStateEvtExclInvs(ComplexComponent comp, SynthesisAutomaton synthAut,
            LocationPointerManager locPtrManager)
    {
        // State/event exclusion invariants of the component.
        for (Invariant inv: comp.getInvariants()) {
            // Skip state invariants.
            if (inv.getInvKind() == InvKind.STATE) {
                continue;
            }

            // Check kind.
            if (inv.getSupKind() != SupKind.PLANT && inv.getSupKind() != SupKind.REQUIREMENT) {
                String msg = fmt("Unsupported %s: for state/event exclusion invariants, only plant and requirement "
                        + "invariants are supported.", CifTextUtils.getComponentText1(comp));
                problems.add(msg);
                continue;
            }

            // Check that event is in the alphabet.
            Event event = ((EventExpression)inv.getEvent()).getEvent();
            if (!synthAut.alphabet.contains(event)) {
                String msg = fmt(
                        "State/event exclusion invariant \"%s\" of %s has no effect, as event \"%s\" is not "
                                + "in the alphabet of any automaton.",
                        CifTextUtils.invToStr(inv, false), CifTextUtils.getComponentText2(comp),
                        CifTextUtils.getAbsName(event));
                warn(msg);

                // Skip the rest as we won't use this invariant for synthesis.
                continue;
            }

            // Convert predicate.
            Expression pred = inv.getPredicate();
            BDD compInv;
            try {
                compInv = convertPred(pred, false, synthAut);
            } catch (UnsupportedPredicateException ex) {
                if (ex.expr != null) {
                    String msg = fmt("Unsupported %s: unsupported part \"%s\" of invariant \"%s\": %s",
                            CifTextUtils.getComponentText1(comp), CifTextUtils.exprToStr(ex.expr),
                            CifTextUtils.invToStr(inv, false), ex.getMessage());
                    problems.add(msg);
                }
                continue;
            }

            // Adapt predicate for the kind of invariant.
            if (inv.getInvKind() == InvKind.EVENT_DISABLES) {
                BDD compInvNot = compInv.not();
                compInv.free();
                compInv = compInvNot;
            }

            // Store copies of the BDD.
            switch (inv.getSupKind()) {
                case PLANT:
                    storeStateEvtExclInv(synthAut.stateEvtExclPlantLists, event, compInv.id());
                    conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclPlants, event, compInv.id());
                    break;
                case REQUIREMENT:
                    storeStateEvtExclInv(synthAut.stateEvtExclReqLists, event, compInv.id());
                    conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclReqs, event, compInv.id());
                    if (Boolean.TRUE.equals(event.getControllable())) {
                        conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclsReqInvs, event, compInv.id());
                    }
                    break;
                default:
                    throw new RuntimeException("Unexpected kind: " + inv.getSupKind());
            }

            // Free the original BDD.
            compInv.free();
        }

        // State/event exclusion requirement invariants of locations (automata only).
        if (comp instanceof Automaton) {
            // Get automaton.
            Automaton aut = (Automaton)comp;

            // Add state/event exclusion invariants from the locations.
            for (Location loc: aut.getLocations()) {
                for (Invariant inv: loc.getInvariants()) {
                    // Skip state invariants.
                    if (inv.getInvKind() == InvKind.STATE) {
                        continue;
                    }

                    // Check kind.
                    if (inv.getSupKind() != SupKind.PLANT && inv.getSupKind() != SupKind.REQUIREMENT) {
                        String msg = fmt("Unsupported %s: for state/event exclusion invariants, only plant and "
                                + "requirement invariants are supported.", CifTextUtils.getLocationText1(loc));
                        problems.add(msg);
                        continue;
                    }

                    // Check that event is in the alphabet.
                    Event event = ((EventExpression)inv.getEvent()).getEvent();
                    if (!synthAut.alphabet.contains(event)) {
                        String msg = fmt(
                                "State/event exclusion invariant \"%s\" of %s has no effect, as event \"%s\" is not in "
                                        + "the alphabet of any automaton.",
                                CifTextUtils.invToStr(inv, false), CifTextUtils.getLocationText2(loc),
                                CifTextUtils.getAbsName(event));
                        warn(msg);

                        // Skip the rest as we won't use this invariant for synthesis.
                        continue;
                    }

                    // Convert predicate.
                    Expression pred = inv.getPredicate();
                    BDD locInv;
                    try {
                        locInv = convertPred(pred, false, synthAut);
                    } catch (UnsupportedPredicateException ex) {
                        if (ex.expr != null) {
                            String msg = fmt("Unsupported %s: unsupported part \"%s\" of invariant \"%s\": %s",
                                    CifTextUtils.getLocationText1(loc), CifTextUtils.exprToStr(ex.expr),
                                    CifTextUtils.invToStr(inv, false), ex.getMessage());
                            problems.add(msg);
                        }
                        continue;
                    }

                    // Get location predicate (srcLocPred => locInv).
                    BDD srcLocPred;
                    try {
                        Expression srcLocRef = locPtrManager.createLocRef(loc);
                        srcLocPred = convertPred(srcLocRef, false, synthAut);
                    } catch (UnsupportedPredicateException ex) {
                        if (ex.expr != null) {
                            // Internally created predicate shouldn't fail conversion.
                            throw new RuntimeException(ex);
                        }
                        continue;
                    }

                    locInv = srcLocPred.not().orWith(locInv);
                    srcLocPred.free();

                    // Adapt predicate for the kind of invariant.
                    if (inv.getInvKind() == InvKind.EVENT_DISABLES) {
                        BDD locInvNot = locInv.not();
                        locInv.free();
                        locInv = locInvNot;
                    }

                    // Store copies of the BDD.
                    switch (inv.getSupKind()) {
                        case PLANT:
                            storeStateEvtExclInv(synthAut.stateEvtExclPlantLists, event, locInv.id());
                            conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclPlants, event, locInv.id());
                            break;
                        case REQUIREMENT:
                            storeStateEvtExclInv(synthAut.stateEvtExclReqLists, event, locInv.id());
                            conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclReqs, event, locInv.id());
                            if (Boolean.TRUE.equals(event.getControllable())) {
                                conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclsReqInvs, event, locInv.id());
                            }
                            break;
                        default:
                            throw new RuntimeException("Unexpected kind: " + inv.getSupKind());
                    }

                    // Free the original BDD.
                    locInv.free();
                }
            }
        }

        // Proceed recursively (groups only).
        if (comp instanceof Group) {
            for (Component child: ((Group)comp).getComponents()) {
                convertStateEvtExclInvs((ComplexComponent)child, synthAut, locPtrManager);
            }
        }
    }

    /**
     * Adds the given state/event exclusion invariant to the state/event exclusion invariants collected so far for the
     * supplied event. The invariants are retrieved from the supplied mapping.
     *
     * @param eventInvs Mapping from events to their corresponding invariants.
     * @param event The event to use as a key.
     * @param inv The invariant to add.
     */
    private void storeStateEvtExclInv(Map<Event, List<BDD>> eventInvs, Event event, BDD inv) {
        List<BDD> invs = eventInvs.get(event);
        invs.add(inv);
    }

    /**
     * Combines the given state/event exclusion invariant with the state/event exclusion invariants collected so far for
     * the supplied event. The invariants are combined, using conjunction. The invariants (as a BDD) are retrieved from
     * the supplied mapping.
     *
     * @param eventInvs Mapping from events to their corresponding invariants.
     * @param event The event to use as a key.
     * @param inv The invariant to combine.
     */
    private void conjunctAndStoreStateEvtExclInv(Map<Event, BDD> eventInvs, Event event, BDD inv) {
        BDD invs = eventInvs.get(event);
        invs = invs.andWith(inv);
        eventInvs.put(event, invs);
    }

    /**
     * Preconvert requirement automata, to enable treating them as monitor plants during synthesis.
     *
     * @param requirements The requirement automata.
     * @param alphabets Per requirement automaton, all the alphabets.
     * @param synthAut The synthesis automaton.
     */
    private void preconvertReqAuts(List<Automaton> requirements, List<Alphabets> alphabets,
            SynthesisAutomaton synthAut)
    {
        // Initialization.
        originalMonitors = mapc(requirements.size());

        // For synthesis, requirement automata are treated as plants that monitor the entire alphabet. They thus don't
        // restrict anything guard-wise. We add additional state/event exclusion requirements to restrict the behavior
        // to what the original requirement automaton allowed.
        for (int i = 0; i < requirements.size(); i++) {
            // Get requirement automaton and alphabets.
            Automaton requirement = requirements.get(i);
            Alphabets reqAlphabets = alphabets.get(i);

            // Add state/event exclusion requirements for non-monitored events. Problems have already been reported in
            // case of send/receive usage in the requirements.
            for (Event event: reqAlphabets.syncAlphabet) {
                // Skip events that are already monitored.
                if (reqAlphabets.moniAlphabet.contains(event)) {
                    continue;
                }

                // Get combined guard.
                Expression cifGuard = CifGuardUtils.mergeGuards(requirement, event, EdgeEventImpl.class,
                        LocRefExprCreator.DEFAULT);

                // Convert guard.
                BDD synthGuard;
                try {
                    synthGuard = convertPred(cifGuard, false, synthAut);
                } catch (UnsupportedPredicateException ex) {
                    if (ex.expr != null) {
                        String msg = fmt("Unsupported %s: unsupported part \"%s\" of combined guard \"%s\": %s",
                                CifTextUtils.getComponentText1(requirement), CifTextUtils.exprToStr(ex.expr),
                                CifTextUtils.exprToStr(cifGuard), ex.getMessage());
                        problems.add(msg);
                    }
                    continue;
                }

                // Add guard as state/event exclusion requirement for the event.
                storeStateEvtExclInv(synthAut.stateEvtExclReqLists, event, synthGuard.id());
                conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclReqs, event, synthGuard.id());

                if (Boolean.TRUE.equals(event.getControllable())) {
                    conjunctAndStoreStateEvtExclInv(synthAut.stateEvtExclsReqAuts, event, synthGuard.id());
                }

                synthGuard.free();
            }

            // Change requirement automaton to monitor all events. Skip this if the alphabet is empty, as we then get a
            // warning that we monitor an empty alphabet, when the input specification is converted to the output
            // specification, saved on disk, and used with other tools.
            if (reqAlphabets.syncAlphabet.isEmpty()) {
                // No alphabet, so shouldn't monitor anything. It may however already monitor the entire (empty)
                // alphabet. If so, just leave that as is.
            } else {
                // Store the original monitors, to be able to restore them later on.
                originalMonitors.put(requirement, requirement.getMonitors());

                // Monitor all events in the alphabet.
                requirement.setMonitors(newMonitors());
                reqAlphabets.moniAlphabet = copy(reqAlphabets.syncAlphabet);
            }
        }
    }

    /**
     * Converts the plant and requirement automata, to a single linearized synthesis automaton.
     *
     * @param plants The plant automata of the specification.
     * @param requirements The requirement automata.
     * @param plantAlphabets Per plant automaton, all the alphabets.
     * @param reqAlphabets Per requirement automaton, all the alphabets.
     * @param locPtrManager Location pointer manager.
     * @param synthAut The synthesis automaton to be updated.
     */
    private void convertPlantReqAuts(List<Automaton> plants, List<Automaton> requirements,
            List<Alphabets> plantAlphabets, List<Alphabets> reqAlphabets,
            CifDataSynthesisLocationPointerManager locPtrManager, SynthesisAutomaton synthAut)
    {
        // Combine information about plants and requirements.
        List<Automaton> automata = concat(plants, requirements);
        List<Alphabets> alphabets = concat(plantAlphabets, reqAlphabets);

        // Check no 'tau' edges.
        boolean tauOk = checkNoTauEdges(automata);

        // Add linearized edges.
        if (tauOk) {
            // Linearize edges for all events in the alphabet.
            // Must match a similar call to linearize edges in `LinearizedHyperEdgeCreator'.
            List<Edge> cifEdges = list();
            LinearizeProduct.linearizeEdges(automata, alphabets, set2list(synthAut.alphabet), locPtrManager, false,
                    true, cifEdges);

            // Create and add synthesis edges.
            synthAut.edges = listc(cifEdges.size());
            synthAut.eventEdges = mapc(synthAut.alphabet.size());
            for (Edge cifEdge: cifEdges) {
                // Check for termination.
                if (synthAut.env.isTerminationRequested()) {
                    break;
                }

                // Create edge.
                SynthesisEdge synthEdge = new SynthesisEdge(synthAut);
                synthEdge.edges = list(cifEdge);

                // Set event.
                Assert.check(cifEdge.getEvents().size() == 1);
                EdgeEvent edgeEvent = first(cifEdge.getEvents());
                Event event = CifEventUtils.getEventFromEdgeEvent(edgeEvent);
                synthEdge.event = event;

                // Add edge.
                synthAut.edges.add(synthEdge);
                List<SynthesisEdge> synthEdges = synthAut.eventEdges.get(event);
                if (synthEdges == null) {
                    synthEdges = list();
                    synthAut.eventEdges.put(event, synthEdges);
                }
                synthEdges.add(synthEdge);

                // Convert and set guards.
                BDD guard;
                try {
                    guard = convertPreds(cifEdge.getGuards(), false, synthAut);
                } catch (UnsupportedPredicateException ex) {
                    if (ex.expr != null) {
                        String msg = fmt("Unsupported linearized guard: unsupported part \"%s\" of guard(s) \"%s\": %s",
                                CifTextUtils.exprToStr(ex.expr), CifTextUtils.exprsToStr(cifEdge.getGuards()),
                                ex.getMessage());
                        problems.add(msg);
                    }

                    // Set dummy guard to allow continuing. Use 'false' to avoid non-determinism check to give false
                    // positives.
                    guard = synthAut.factory.zero();
                }

                synthEdge.guard = guard;
                synthEdge.origGuard = guard.id();

                // Convert and set assignments.
                List<Update> updates = cifEdge.getUpdates();
                convertUpdates(updates, synthEdge, locPtrManager, synthAut);
            }

            if (synthAut.env.isTerminationRequested()) {
                return;
            }

            // Check for non-determinism of controllable events.
            checkNonDeterminism(synthAut.edges);
        }
    }

    /**
     * Checks the given automata, to make sure they don't have any 'tau' edges. Any 'tau' edges are reported as
     * {@link #problems}.
     *
     * @param automata The automata to check.
     * @return Whether the automata have no 'tau' edges ({@code true}) or do have 'tau' edges ({@code false}).
     */
    private boolean checkNoTauEdges(List<Automaton> automata) {
        boolean tauOk = true;
        for (Automaton aut: automata) {
            for (Location loc: aut.getLocations()) {
                for (Edge edge: loc.getEdges()) {
                    if (edge.getEvents().isEmpty()) {
                        String msg = fmt("Unsupported %s: edges without events (implicitly event \"tau\") "
                                + "are not supported.", CifTextUtils.getLocationText1(loc));
                        problems.add(msg);
                        tauOk = false;
                    }

                    for (EdgeEvent edgeEvent: edge.getEvents()) {
                        Expression eventRef = edgeEvent.getEvent();
                        if (eventRef instanceof TauExpression) {
                            String msg = fmt("Unsupported %s: edges with \"tau\" events are not supported.",
                                    CifTextUtils.getLocationText1(loc));
                            problems.add(msg);
                            tauOk = false;
                        }
                    }
                }
            }
        }
        return tauOk;
    }

    /**
     * Check synthesis edges to make sure there is no non-determinism for controllable events. Non-determinism by means
     * of multiple outgoing edges for the same event, with overlapping guards, is not supported. An external supervisor
     * can't force the correct edge to be taken, if only the updates (includes location pointer variable assignment for
     * target location) are different. For uncontrollable events non-determinism is not a problem, as the supervisor
     * won't restrict edges for uncontrollable events.
     *
     * @param edges The synthesis edges (self loops). May include edges for both controllable and uncontrollable events.
     */
    private void checkNonDeterminism(List<SynthesisEdge> edges) {
        // Initialize conflict information.
        Map<Event, BDD> eventGuards = map();
        Set<Event> conflicts = setc(0);

        // Check edges for conflicts (non-determinism).
        for (SynthesisEdge edge: edges) {
            // Skip uncontrollable events. Also skip events without controllability (is already previously reported).
            Event evt = edge.event;
            Boolean controllable = evt.getControllable();
            if (controllable == null || !controllable) {
                continue;
            }

            // Skip events already found to have conflicting edges.
            if (conflicts.contains(evt)) {
                continue;
            }

            // Get guards so far and new guard of the edge.
            BDD curGuard = eventGuards.get(evt);
            BDD newGuard = edge.guard;

            // Check for overlap.
            if (curGuard == null) {
                // First edge for this event.
                eventGuards.put(evt, newGuard.id());
            } else {
                BDD overlap = curGuard.and(newGuard);
                if (overlap.isZero()) {
                    // No overlap, update guard so far.
                    eventGuards.put(evt, curGuard.orWith(newGuard.id()));
                } else {
                    // Overlap. Conflict found.
                    conflicts.add(evt);
                }
                overlap.free();
            }
        }

        // Clean up.
        for (BDD guard: eventGuards.values()) {
            guard.free();
        }

        // Report conflicts.
        for (Event conflict: conflicts) {
            // Get edges for the event.
            List<SynthesisEdge> eventEdges = list();
            for (SynthesisEdge edge: edges) {
                if (edge.event != conflict) {
                    continue;
                }
                eventEdges.add(edge);
            }

            // Partition guards into non-overlapping groups.
            List<List<SynthesisEdge>> groups = groupOnGuardOverlap(eventEdges);

            // Get guard texts per group with overlap.
            List<String> guardsTxts = list();
            for (List<SynthesisEdge> group: groups) {
                // Only overlap in the group if at least two edges.
                if (group.size() < 2) {
                    continue;
                }

                // Add guards text for this group.
                List<String> guardTxts = list();
                for (SynthesisEdge edge: group) {
                    Assert.check(edge.edges.size() == 1); // No synthesis edges have been merged yet.
                    List<Expression> guards = first(edge.edges).getGuards();
                    String guardsTxt;
                    if (guards.isEmpty()) {
                        guardsTxt = "true";
                    } else {
                        guardsTxt = CifTextUtils.exprsToStr(guards);
                    }
                    guardTxts.add("\"" + guardsTxt + "\"");
                }
                Assert.check(!guardTxts.isEmpty());
                guardsTxts.add(String.join(", ", guardTxts));
            }
            Assert.check(!guardsTxts.isEmpty());

            // Get groups text.
            String groupsTxt;
            if (guardsTxts.size() == 1) {
                groupsTxt = " " + guardsTxts.get(0) + ".";
            } else {
                for (int i = 0; i < guardsTxts.size(); i++) {
                    String txt = guardsTxts.get(i);
                    txt = fmt("\n    Group %d: %s", i + 1, txt);
                    guardsTxts.set(i, txt);
                }
                groupsTxt = String.join("", guardsTxts);
            }

            // Report conflict.
            String msg = fmt("Unsupported linearized edges: non-determinism detected for edges of controllable "
                    + "event \"%s\" with overlapping guards:%s", getAbsName(conflict), groupsTxt);
            problems.add(msg);
        }
    }

    /**
     * Group the edges with overlapping guards. That is, partition the edges into groups with non-overlapping guards.
     *
     * @param edges The edges.
     * @return The groups of edges.
     */
    private static List<List<SynthesisEdge>> groupOnGuardOverlap(List<SynthesisEdge> edges) {
        // Initialize to one edge per group.
        List<List<SynthesisEdge>> groups = listc(edges.size());
        for (int i = 0; i < edges.size(); i++) {
            groups.add(list(edges.get(i)));
        }

        // Merge groups with overlapping guards. For each group, we merge with all overlapping groups that come after
        // it.
        for (int i = 0; i < groups.size(); i++) {
            // All groups start with exactly one edge, so get that guard.
            Assert.check(groups.get(i).size() == 1);
            BDD curGuard = groups.get(i).get(0).guard.id();

            // Process all groups that come after the current one.
            boolean changed = true;
            while (changed) {
                changed = false;

                for (int j = i + 1; j < groups.size(); j++) {
                    // All groups start with exactly one edge, so get that guard.
                    Assert.check(groups.get(j).size() == 1);
                    BDD newGuard = groups.get(j).get(0).guard;

                    // If disjoint (no overlap), groups don't need to be merged.
                    BDD overlapPred = curGuard.and(newGuard);
                    boolean disjoint = overlapPred.isZero();
                    overlapPred.free();
                    if (disjoint) {
                        continue;
                    }

                    // Overlap detected. Merge groups.
                    changed = true;
                    groups.get(i).add(groups.get(j).get(0));
                    groups.remove(j);

                    curGuard = curGuard.andWith(newGuard.id());
                }
            }

            // Cleanup.
            curGuard.free();
        }

        // Return the disjoint groups.
        return groups;
    }

    /**
     * Converts CIF updates to synthesis updates.
     *
     * @param updates The CIF updates.
     * @param synthEdge The synthesis edge in which to store the synthesis updates. Is modified in-place.
     * @param locPtrManager Location pointer manager.
     * @param aut The synthesis automaton.
     */
    private void convertUpdates(List<Update> updates, SynthesisEdge synthEdge,
            CifDataSynthesisLocationPointerManager locPtrManager, SynthesisAutomaton aut)
    {
        // Initialization.
        List<Assignment> assignments = listc(updates.size());
        boolean[] assigned = new boolean[aut.variables.length];

        // Convert separate updates, and merge to form the update relation and runtime error predicate.
        BDD relation = aut.factory.one();
        BDD error = aut.factory.zero();
        for (Update update: updates) {
            Pair<BDD, BDD> rslt = convertUpdate(update, assignments, assigned, locPtrManager, aut);
            if (aut.env.isTerminationRequested()) {
                return;
            }

            if (rslt != null) {
                BDD updateRelation = rslt.left;
                relation = relation.andWith(updateRelation);

                BDD updateError = rslt.right;
                error = error.orWith(updateError);
            }

            if (aut.env.isTerminationRequested()) {
                return;
            }
        }

        // Add relations to assure variables not being assigned don't change, i.e. won't jump arbitrarily.
        //
        // We go through the variables in the reverse order of the variable order. This generally ensures the best
        // performance, as new updates are added 'on top' of existing updates, allowing reuse of the existing BDDs,
        // rather than needing to recreate them.
        for (int i = assigned.length - 1; i >= 0; i--) {
            // If assigned, skip variable.
            if (assigned[i]) {
                continue;
            }

            // If conversion of variable failed, skip it.
            SynthesisVariable var = aut.variables[i];
            if (var == null) {
                continue;
            }

            // Unassigned, add 'x = x+' predicate.
            CifBddBitVector vectorOld = CifBddBitVector.createDomain(var.domain);
            CifBddBitVector vectorNew = CifBddBitVector.createDomain(var.domainNew);
            BDD unchangedRelation = vectorOld.equalTo(vectorNew);
            relation = relation.andWith(unchangedRelation);
            vectorOld.free();
            vectorNew.free();
        }

        // Store data for the updates.
        synthEdge.assignments = list(assignments);
        synthEdge.update = relation;
        synthEdge.error = error;
    }

    /**
     * Converts a CIF update to an update predicate and runtime error predicate.
     *
     * @param update The CIF update to convert.
     * @param assignments The assignments converted so far. Is modified in-place.
     * @param assigned Per synthesis variable, whether it has been assigned on this edge. Is modified in-place.
     * @param locPtrManager Location pointer manager.
     * @param aut The synthesis automaton.
     * @return The update relation and runtime error predicate. May be {@code null} if the update can't be converted due
     *     to a precondition violation.
     */
    private Pair<BDD, BDD> convertUpdate(Update update, List<Assignment> assignments, boolean[] assigned,
            CifDataSynthesisLocationPointerManager locPtrManager, SynthesisAutomaton aut)
    {
        // Make sure it is not a conditional update ('if' update).
        if (update instanceof IfUpdate) {
            String msg = "Unsupported update: conditional updates ('if' updates) are not supported.";
            problems.add(msg);
            return null;
        }
        Assignment asgn = (Assignment)update;

        // Store assignment.
        assignments.add(asgn);

        // Make sure a discrete variable is assigned.
        Expression addr = asgn.getAddressable();
        if (addr instanceof TupleExpression) {
            String msg = "Unsupported update: multi-assignments are not supported.";
            problems.add(msg);
            return null;
        } else if (addr instanceof ProjectionExpression) {
            String msg = "Unsupported update: partial variable assignments are not supported.";
            problems.add(msg);
            return null;
        } else if (addr instanceof ContVariableExpression) {
            String msg = "Unsupported update: assignments to continuous variables are not supported.";
            problems.add(msg);
            return null;
        }

        // Get assigned variable.
        DiscVariable cifVar = ((DiscVariableExpression)addr).getVariable();

        // Special case for location pointer variable assignments created during linearization. Note that location
        // pointers are only created for automata with more than one location, and updates are only created for non self
        // loop edges. Since automata with one location have only self loops, automata for which location pointer
        // updates are created also have location pointer variables.
        Automaton cifAut = locPtrManager.getAutomaton(cifVar);
        if (cifAut != null) {
            // Get synthesis variable.
            int varIdx = getLpVarIdx(aut.variables, cifAut);
            if (varIdx == -1) {
                return null;
            }
            SynthesisVariable var = aut.variables[varIdx];
            Assert.check(var instanceof SynthesisLocPtrVariable);

            // Mark variable as assigned.
            Assert.check(!assigned[varIdx]);
            assigned[varIdx] = true;

            // Get 0-based location index, which is also the bit index.
            Assert.check(asgn.getValue() instanceof IntExpression);
            int locIdx = ((IntExpression)asgn.getValue()).getValue();

            // Create and return assignment relation 'lp+ = locIdx'. The location always fits within the domain of the
            // location pointer variable, so there is no error predicate.
            CifBddBitVector varVector = CifBddBitVector.createDomain(var.domainNew);
            CifBddBitVector locVector = CifBddBitVector.createInt(aut.factory, locIdx);
            Assert.check(locVector.length() <= varVector.length());
            locVector.resize(varVector.length());
            BDD relation = varVector.equalTo(locVector);
            varVector.free();
            locVector.free();
            return pair(relation, aut.factory.zero());
        }

        // Normal case: assignment originating from original CIF model.
        int varIdx = getTypedVarIdx(aut.variables, cifVar);
        if (varIdx == -1) {
            return null;
        }
        SynthesisVariable synthVar = aut.variables[varIdx];
        Assert.check(synthVar instanceof SynthesisTypedVariable);
        SynthesisTypedVariable var = (SynthesisTypedVariable)synthVar;

        // Mark variable as assigned.
        Assert.check(!assigned[varIdx]);
        assigned[varIdx] = true;

        // Case distinction on types of values.
        if (var.type instanceof BoolType) {
            // Convert right hand side (value to assign).
            Expression rhsExpr = asgn.getValue();
            BDD rhsBdd;
            try {
                rhsBdd = convertPred(rhsExpr, false, aut);
            } catch (UnsupportedPredicateException ex) {
                // Add new problem, if not failed due to earlier problems.
                if (ex.expr != null) {
                    String msg = fmt("Unsupported assignment: unsupported part \"%s\" of assignment \"%s := %s\": %s",
                            CifTextUtils.exprToStr(ex.expr), CifTextUtils.exprToStr(addr),
                            CifTextUtils.exprToStr(rhsExpr), ex.getMessage());
                    problems.add(msg);
                }

                // Return identity values.
                return pair(aut.factory.one(), aut.factory.zero());
            }

            // Create BDD for the left hand side (variable to get a new value).
            Assert.check(var.domainNew.varNum() == 1);
            int lhsVar = var.domainNew.vars()[0];
            BDD lhsBdd = aut.factory.ithVar(lhsVar);

            // Construct 'lhs+ = rhs' relation.
            BDD relation = lhsBdd.biimpWith(rhsBdd);

            // Return the full relation.
            return pair(relation, aut.factory.zero());
        } else {
            // Convert right hand side (value to assign).
            Expression rhsExpr = asgn.getValue();
            Supplier<String> partMsg = () -> fmt("assignment \"%s := %s\"", CifTextUtils.exprToStr(addr),
                    CifTextUtils.exprToStr(rhsExpr));

            CifBddBitVectorAndCarry rhsRslt;
            try {
                rhsRslt = convertExpr(rhsExpr, false, aut, true, partMsg);
            } catch (UnsupportedPredicateException ex) {
                // Add new problem, if not failed due to earlier problems.
                if (ex.expr != null) {
                    String msg = fmt("Unsupported assignment: unsupported part \"%s\" of assignment \"%s := %s\": %s",
                            CifTextUtils.exprToStr(ex.expr), CifTextUtils.exprToStr(addr),
                            CifTextUtils.exprToStr(rhsExpr), ex.getMessage());
                    problems.add(msg);
                }

                // Return identity values.
                return pair(aut.factory.one(), aut.factory.zero());
            }
            CifBddBitVector rhsVec = rhsRslt.vector;

            // The runtime error predicate resulting from the right hand side is used to initialize the runtime error
            // predicate of the assignment.
            BDD error = rhsRslt.carry;

            // Create bit vector for the left hand side (variable to get a new value).
            CifBddBitVector lhsVec = CifBddBitVector.createDomain(var.domainNew);

            // Construct 'lhs+ = rhs' relation. By resizing both vectors to be equal size, a smaller rhs (in number of
            // bits) is properly handled, as 'false' bits are added for the part of the rhs that is missing with respect
            // to the lhs.
            int lhsLen = lhsVec.length();
            int len = Math.max(lhsVec.length(), rhsVec.length());
            lhsVec.resize(len);
            rhsVec.resize(len);
            BDD relation = lhsVec.equalTo(rhsVec);
            lhsVec.free();

            // Prevent out of bounds assignment. We only need to prevent values that can be represented by the rhs, that
            // can not be represented by the lhs. The values that the rhs can have that the lhs can't have, but that can
            // be represented by the lhs, are already prevented elsewhere by means of the range invariants.
            for (int i = lhsLen; i < len; i++) {
                error = error.orWith(rhsVec.getBit(i).id());
            }
            rhsVec.free();

            // Return the full relation.
            return pair(relation, error);
        }
    }

    /**
     * Adds and event and edge for each input variable.
     *
     * @param synthAut The synthesis automaton. Is modified in-place.
     */
    private void addInputVariableEdges(SynthesisAutomaton synthAut) {
        // Initialization.
        synthAut.inputVarEvents = set();

        // Add for each input variable.
        for (SynthesisVariable var: synthAut.variables) {
            // Handle only input variables.
            if (var == null) {
                continue;
            }
            if (!(var instanceof SynthesisInputVariable)) {
                continue;
            }
            SynthesisInputVariable synthInputVar = (SynthesisInputVariable)var;

            // Create uncontrollable event for the input variable.
            Event event = newEvent();
            event.setControllable(false);
            event.setName(synthInputVar.var.getName());

            // Add new event to the alphabet.
            synthAut.alphabet.add(event);

            // Add new event to the original specification, for proper absolute naming. Also store it in the synthesis
            // automaton, to allow for post synthesis removal of the temporary event.
            ComplexComponent comp = (ComplexComponent)synthInputVar.var.eContainer();
            comp.getDeclarations().add(event);
            synthAut.inputVarEvents.add(event);

            // Add edge that allows input variable to change to any other value.
            SynthesisEdge edge = new SynthesisEdge(synthAut);
            edge.edges = list((Edge)null);
            edge.event = event;
            edge.origGuard = synthAut.factory.one();
            edge.guard = synthAut.factory.one();
            edge.error = synthAut.factory.zero();
            synthAut.edges.add(edge);
            synthAut.eventEdges.put(event, list(edge));

            // Add CIF assignment to edge. Right hand side not filled, as it is not a 'normal' assignment. Also,
            // technically in CIF an input variable can not be assigned.
            InputVariableExpression addr = newInputVariableExpression();
            addr.setVariable(synthInputVar.var);
            Assignment asgn = newAssignment();
            asgn.setAddressable(addr);
            edge.assignments = list(list(asgn));

            // Add update relation.
            edge.update = synthAut.factory.one();
            for (SynthesisVariable updVar: synthAut.variables) {
                // If conversion of variable failed, skip it.
                if (updVar == null) {
                    continue;
                }

                // Get lhs and rhs.
                CifBddBitVector vectorOld = CifBddBitVector.createDomain(updVar.domain);
                CifBddBitVector vectorNew = CifBddBitVector.createDomain(updVar.domainNew);

                // Create update predicate for this variable, and add it.
                BDD varUpdate;
                if (updVar == var) {
                    // The input variable: add 'input != input+' to allow it to change to any other value. Also keep the
                    // new value in the domain.
                    BDD newInDomain = BddUtils.getVarDomain(updVar, true, synthAut.factory);
                    varUpdate = vectorOld.unequalTo(vectorNew);
                    varUpdate = varUpdate.andWith(newInDomain);
                } else {
                    // Any other variable: add 'var = var+'.
                    varUpdate = vectorOld.equalTo(vectorNew);
                }

                edge.update = edge.update.andWith(varUpdate);

                // Cleanup.
                vectorOld.free();
                vectorNew.free();
            }
        }
    }

    /**
     * Merges synthesis edges to the desired granularity.
     *
     * @param synthAut The synthesis automaton. Is modified in-place.
     */
    private void mergeEdges(SynthesisAutomaton synthAut) {
        // Skip in case of conversion failure.
        if (synthAut.eventEdges == null) {
            return;
        }

        // Merge the edges, if needed.
        EdgeGranularity granularity = EdgeGranularityOption.getGranularity();
        switch (granularity) {
            case PER_EDGE:
                // Nothing to do, as already at per-edge granularity.
                return;
            case PER_EVENT: {
                // Merge edges per event.
                int eventCount = synthAut.eventEdges.size();
                synthAut.edges = listc(eventCount);
                for (Entry<Event, List<SynthesisEdge>> entry: synthAut.eventEdges.entrySet()) {
                    SynthesisEdge mergedEdge = entry.getValue().stream().reduce(SynthesisEdge::mergeEdges).get();
                    synthAut.edges.add(mergedEdge);
                    entry.setValue(list(mergedEdge));
                }
                return;
            }
        }
        throw new RuntimeException("Unknown granularity: " + granularity);
    }

    /**
     * Orders the synthesis edges, for reachability computations.
     *
     * @param synthAut The synthesis automaton. Is modified in-place.
     */
    private void orderEdges(SynthesisAutomaton synthAut) {
        synthAut.orderedEdgesBackward = orderEdgesForDirection(synthAut.edges, EdgeOrderBackwardOption.getOrder(),
                false);
        synthAut.orderedEdgesForward = orderEdgesForDirection(synthAut.edges, EdgeOrderForwardOption.getOrder(), true);
    }

    /**
     * Orders the synthesis edges, for a single direction, i.e., for forward or backward reachability computations.
     *
     * @param edges The edges in linearized model order.
     * @param orderTxt The order as textual value from the option, for the given direction.
     * @param forForwardReachability Order for forward reachability ({@code true}) or backward reachability
     *     ({@code false}).
     * @return The ordered edges.
     */
    private static List<SynthesisEdge> orderEdgesForDirection(List<SynthesisEdge> edges, String orderTxt,
            boolean forForwardReachability)
    {
        if (orderTxt.toLowerCase(Locale.US).equals("model")) {
            // No reordering. Keep linearized model order.
            return edges;
        } else if (orderTxt.toLowerCase(Locale.US).equals("reverse-model")) {
            // Reorder to the reverse of the linearized model order.
            return reverse(edges);
        } else if (orderTxt.toLowerCase(Locale.US).equals("sorted")) {
            // Sort based on absolute event names.
            return edges.stream()
                    .sorted((v, w) -> Strings.SORTER.compare(getAbsName(v.event, false), getAbsName(w.event, false)))
                    .toList();
        } else if (orderTxt.toLowerCase(Locale.US).equals("reverse-sorted")) {
            // Sort based on absolute event names, and reverse.
            return reverse(edges.stream()
                    .sorted((v, w) -> Strings.SORTER.compare(getAbsName(v.event, false), getAbsName(w.event, false)))
                    .toList());
        } else if (orderTxt.toLowerCase(Locale.US).equals("random")
                || orderTxt.toLowerCase(Locale.US).startsWith("random:"))
        {
            // Get seed, if specified.
            Long seed = null;
            if (orderTxt.contains(":")) {
                int idx = orderTxt.indexOf(":");
                String seedTxt = orderTxt.substring(idx + 1);
                try {
                    seed = Long.parseUnsignedLong(seedTxt);
                } catch (NumberFormatException ex) {
                    String msg = fmt("Invalid random %s edge order seed number: \"%s\".",
                            forForwardReachability ? "forward" : "backward", orderTxt);
                    throw new InvalidOptionException(msg, ex);
                }
            }

            // Shuffle to random order.
            List<SynthesisEdge> orderedEdges = copy(edges);
            if (seed == null) {
                Collections.shuffle(orderedEdges);
            } else {
                Collections.shuffle(orderedEdges, new Random(seed));
            }
            return orderedEdges;
        } else {
            // Sort based on supplied custom event order.
            List<SynthesisEdge> orderedEdges = listc(edges.size());
            Set<SynthesisEdge> processedEdges = set();

            // Process elements.
            for (String elemTxt: StringUtils.split(orderTxt, ",")) {
                // Skip empty.
                elemTxt = elemTxt.trim();
                if (elemTxt.isEmpty()) {
                    continue;
                }

                // Create regular expression for matching.
                String regEx = elemTxt.replace(".", "\\.");
                regEx = regEx.replace("*", ".*");
                Pattern pattern = Pattern.compile("^" + regEx + "$");

                // Found actual element. Look up matching synthesis edges.
                List<SynthesisEdge> matches = list();
                for (SynthesisEdge edge: edges) {
                    String name = getAbsName(edge.event, false);
                    if (pattern.matcher(name).matches()) {
                        matches.add(edge);
                    }
                }

                // Need a least one match.
                if (matches.isEmpty()) {
                    String msg = fmt(
                            "Invalid custom %s edge order: can't find a match for \"%s\". There is no supported event "
                                    + "or input variable in the specification that matches the given name pattern.",
                            forForwardReachability ? "forward" : "backward", elemTxt);
                    throw new InvalidOptionException(msg);
                }

                // Sort matches.
                Collections.sort(matches,
                        (v, w) -> Strings.SORTER.compare(getAbsName(v.event, false), getAbsName(w.event, false)));

                // Check for duplicate events, if duplicates are disallowed.
                if (EdgeOrderDuplicateEventsOption.getAllowance() == EdgeOrderDuplicateEventAllowance.DISALLOWED) {
                    for (SynthesisEdge edge: matches) {
                        if (processedEdges.contains(edge)) {
                            String msg = fmt("Invalid custom %s edge order: event \"%s\" is included more than once. "
                                    + "If the duplicate event is intentional, enable allowing duplicate events "
                                    + "in the custom event order using the \"Edge order duplicate events\" option.",
                                    forForwardReachability ? "forward" : "backward", getAbsName(edge.event, false));
                            throw new InvalidOptionException(msg);
                        }
                    }
                }

                // Update for matched edges.
                processedEdges.addAll(matches);
                orderedEdges.addAll(matches);
            }

            // Check completeness.
            Set<SynthesisEdge> missingEdges = difference(edges, processedEdges);
            if (!missingEdges.isEmpty()) {
                Set<String> names = set();
                for (SynthesisEdge edge: missingEdges) {
                    names.add("\"" + getAbsName(edge.event, false) + "\"");
                }
                List<String> sortedNames = Sets.sortedgeneric(names, Strings.SORTER);
                String msg = fmt(
                        "Invalid custom %s edge order: "
                                + "the following event(s) are missing from the specified order: %s.",
                        forForwardReachability ? "forward" : "backward", String.join(", ", sortedNames));
                throw new InvalidOptionException(msg);
            }

            // Return the custom edge order.
            return orderedEdges;
        }
    }

    /** Check edge workset algorithm options. */
    private void checkEdgeWorksetAlgorithmOptions() {
        // Skip if workset algorithm is disabled.
        if (!EdgeWorksetAlgoOption.isEnabled()) {
            return;
        }

        // Edge workset algorithm requires per-event edge granularity, and no duplicate edges in the edge order.
        if (EdgeGranularityOption.getGranularity() != EdgeGranularity.PER_EVENT) {
            throw new InvalidOptionException(
                    "The edge workset algorithm can only be used with per-event edge granularity. "
                            + "Either disable the edge workset algorithm, or configure per-event edge granularity.");
        }
        if (EdgeOrderDuplicateEventsOption.getAllowance() == EdgeOrderDuplicateEventAllowance.ALLOWED) {
            throw new InvalidOptionException(
                    "The edge workset algorithm can not be used with duplicate events in the edge order. "
                            + "Either disable the edge workset algorithm, or disable duplicates for custom edge orders.");
        }
    }

    /**
     * Converts CIF predicates to a synthesis predicate, assuming conjunction between the CIF predicates.
     *
     * @param preds The CIF predicates.
     * @param initial Whether the predicates apply only to the initial state ({@code true}) or any state ({@code false},
     *     includes the initial state).
     * @param synthAut The synthesis automaton.
     * @return The synthesis predicate.
     * @throws UnsupportedPredicateException If one of the predicates is not supported.
     */
    private static BDD convertPreds(List<Expression> preds, boolean initial, SynthesisAutomaton synthAut)
            throws UnsupportedPredicateException
    {
        BDD rslt = synthAut.factory.one();
        for (Expression pred: preds) {
            rslt = rslt.andWith(convertPred(pred, initial, synthAut));
        }
        return rslt;
    }

    /**
     * Converts a CIF predicate to a synthesis predicate.
     *
     * @param pred The CIF predicate.
     * @param initial Whether the predicate applies only to the initial state ({@code true}) or any state
     *     ({@code false}, includes the initial state).
     * @param synthAut The synthesis automaton.
     * @return The synthesis predicate.
     * @throws UnsupportedPredicateException If the predicate is not supported.
     */
    private static BDD convertPred(Expression pred, boolean initial, SynthesisAutomaton synthAut)
            throws UnsupportedPredicateException
    {
        if (pred instanceof BoolExpression) {
            // Boolean literal.
            boolean value = ((BoolExpression)pred).isValue();
            return value ? synthAut.factory.one() : synthAut.factory.zero();
        } else if (pred instanceof DiscVariableExpression) {
            // Boolean variable reference.
            DiscVariable cifVar = ((DiscVariableExpression)pred).getVariable();
            Assert.check(normalizeType(cifVar.getType()) instanceof BoolType);
            int varIdx = getDiscVarIdx(synthAut.variables, cifVar);
            if (varIdx == -1) {
                throw new UnsupportedPredicateException();
            }

            // Create synthesis predicate for 'x' or 'x = true'.
            SynthesisVariable var = synthAut.variables[varIdx];
            return var.domain.ithVar(1);
        } else if (pred instanceof InputVariableExpression) {
            // Boolean variable reference.
            InputVariable cifVar = ((InputVariableExpression)pred).getVariable();
            Assert.check(normalizeType(cifVar.getType()) instanceof BoolType);
            int varIdx = getInputVarIdx(synthAut.variables, cifVar);
            if (varIdx == -1) {
                throw new UnsupportedPredicateException();
            }

            // Create synthesis predicate for 'x' or 'x = true'.
            SynthesisVariable var = synthAut.variables[varIdx];
            return var.domain.ithVar(1);
        } else if (pred instanceof AlgVariableExpression) {
            // Algebraic variable reference. Get the single defining value expression, representing the value of the
            // variable. It is in an 'if' expression if an equation is provided per location of an automaton with more
            // than one location.
            AlgVariable var = ((AlgVariableExpression)pred).getVariable();
            Assert.check(normalizeType(var.getType()) instanceof BoolType);
            Expression value = CifEquationUtils.getSingleValueForAlgVar(var);

            // Convert the defining value expression instead.
            return convertPred(value, initial, synthAut);
        } else if (pred instanceof LocationExpression) {
            // Location reference.
            Location loc = ((LocationExpression)pred).getLocation();
            Automaton aut = CifLocationUtils.getAutomaton(loc);
            int varIdx = getLpVarIdx(synthAut.variables, aut);
            if (varIdx == -1) {
                // Automata with only one location have no location pointer, but are always the active location. So,
                // referring to them is as using a 'true' predicate.
                if (aut.getLocations().size() == 1) {
                    return synthAut.factory.one();
                }

                // Unsupported automaton, probably due to wrong kind.
                throw new UnsupportedPredicateException();
            }
            Assert.check(varIdx >= 0);
            SynthesisVariable var = synthAut.variables[varIdx];

            // Create synthesis predicate for location pointer being equal to value that represents the location.
            int locIdx = aut.getLocations().indexOf(loc);
            Assert.check(locIdx >= 0);
            return var.domain.ithVar(locIdx);
        } else if (pred instanceof ConstantExpression) {
            // Boolean constant reference.
            Constant constant = ((ConstantExpression)pred).getConstant();
            Assert.check(normalizeType(constant.getType()) instanceof BoolType);

            // Evaluate the constant's value.
            Object valueObj;
            try {
                valueObj = CifEvalUtils.eval(constant.getValue(), initial);
            } catch (CifEvalException ex) {
                String msg = fmt("Failed to statically evaluate the value of constant \"%s\".", getAbsName(constant));
                throw new InvalidInputException(msg, ex);
            }

            return (boolean)valueObj ? synthAut.factory.one() : synthAut.factory.zero();
        } else if (pred instanceof UnaryExpression) {
            // Inverse unary expression.
            UnaryExpression upred = (UnaryExpression)pred;
            UnaryOperator op = upred.getOperator();
            if (op != UnaryOperator.INVERSE) {
                String msg = fmt("unary operator \"%s\" is not supported.", CifTextUtils.operatorToStr(op));
                throw new UnsupportedPredicateException(msg, upred);
            }

            // not x
            BDD child = convertPred(upred.getChild(), initial, synthAut);
            BDD rslt = child.not();
            child.free();
            return rslt;
        } else if (pred instanceof BinaryExpression) {
            // Various binary expressions.
            BinaryExpression bpred = (BinaryExpression)pred;
            BinaryOperator op = (((BinaryExpression)pred).getOperator());
            Expression lhs = bpred.getLeft();
            Expression rhs = bpred.getRight();

            // a and b
            if (op == CONJUNCTION) {
                CifType ltype = normalizeType(lhs.getType());
                CifType rtype = normalizeType(rhs.getType());
                if (!(ltype instanceof BoolType) || !(rtype instanceof BoolType)) {
                    String msg = fmt("binary operator \"%s\" on values of types \"%s\" and \"%s\" is not supported.",
                            CifTextUtils.operatorToStr(op), CifTextUtils.typeToStr(ltype),
                            CifTextUtils.typeToStr(rtype));
                    throw new UnsupportedPredicateException(msg, bpred);
                }

                BDD left = convertPred(lhs, initial, synthAut);
                BDD right = convertPred(rhs, initial, synthAut);
                return left.andWith(right);
            }

            // a or b
            if (op == DISJUNCTION) {
                CifType ltype = normalizeType(lhs.getType());
                CifType rtype = normalizeType(rhs.getType());
                if (!(ltype instanceof BoolType) || !(rtype instanceof BoolType)) {
                    String msg = fmt("binary operator \"%s\" on values of types \"%s\" and \"%s\" is not supported.",
                            CifTextUtils.operatorToStr(op), CifTextUtils.typeToStr(ltype),
                            CifTextUtils.typeToStr(rtype));
                    throw new UnsupportedPredicateException(msg, bpred);
                }

                BDD left = convertPred(lhs, initial, synthAut);
                BDD right = convertPred(rhs, initial, synthAut);
                return left.orWith(right);
            }

            // a => b
            if (op == IMPLICATION) {
                BDD left = convertPred(lhs, initial, synthAut);
                BDD right = convertPred(rhs, initial, synthAut);
                return left.impWith(right);
            }

            // a <=> b
            if (op == BI_CONDITIONAL) {
                BDD left = convertPred(lhs, initial, synthAut);
                BDD right = convertPred(rhs, initial, synthAut);
                return left.biimpWith(right);
            }

            // Check supported operator.
            switch (op) {
                case EQUAL:
                case GREATER_EQUAL:
                case GREATER_THAN:
                case LESS_EQUAL:
                case LESS_THAN:
                case UNEQUAL:
                    break;

                default: {
                    String msg = fmt("binary operator \"%s\" is not supported.", CifTextUtils.operatorToStr(op));
                    throw new UnsupportedPredicateException(msg, bpred);
                }
            }

            return convertCmpPred(lhs, rhs, op, pred, bpred, initial, synthAut);
        } else if (pred instanceof IfExpression) {
            // Condition expression with boolean result values.
            IfExpression ifPred = (IfExpression)pred;

            // Convert else.
            BDD rslt = convertPred(ifPred.getElse(), initial, synthAut);

            // Convert elifs/thens.
            for (int i = ifPred.getElifs().size() - 1; i >= 0; i--) {
                ElifExpression elifPred = ifPred.getElifs().get(i);
                BDD elifGuards = convertPreds(elifPred.getGuards(), initial, synthAut);
                BDD elifThen = convertPred(elifPred.getThen(), initial, synthAut);
                BDD elifRslt = elifGuards.ite(elifThen, rslt);
                elifGuards.free();
                elifThen.free();
                rslt.free();
                rslt = elifRslt;
            }

            // Convert if/then.
            BDD ifGuards = convertPreds(ifPred.getGuards(), initial, synthAut);
            BDD ifThen = convertPred(ifPred.getThen(), initial, synthAut);
            BDD elifRslt = ifGuards.ite(ifThen, rslt);
            ifGuards.free();
            ifThen.free();
            rslt.free();
            rslt = elifRslt;

            // Return converted conditional expression.
            return rslt;
        } else if (pred instanceof SwitchExpression) {
            // Switch expression with boolean result values.
            SwitchExpression switchPred = (SwitchExpression)pred;
            Expression value = switchPred.getValue();
            List<SwitchCase> cases = switchPred.getCases();

            // Convert else.
            BDD rslt = convertPred(last(cases).getValue(), initial, synthAut);

            // Convert cases.
            for (int i = cases.size() - 2; i >= 0; i--) {
                SwitchCase cse = cases.get(i);
                Expression caseGuardExpr = CifTypeUtils.isAutRefExpr(value) ? cse.getKey() : newBinaryExpression(
                        deepclone(value), BinaryOperator.EQUAL, null, deepclone(cse.getKey()), newBoolType());
                BDD caseGuard = convertPred(caseGuardExpr, initial, synthAut);
                BDD caseThen = convertPred(cse.getValue(), initial, synthAut);
                BDD caseRslt = caseGuard.ite(caseThen, rslt);
                caseGuard.free();
                caseThen.free();
                rslt.free();
                rslt = caseRslt;
            }

            return rslt;
        } else {
            // Others: unsupported.
            String msg = fmt("predicate is not supported.");
            throw new UnsupportedPredicateException(msg, pred);
        }
    }

    /**
     * Converts a CIF comparison predicate to a synthesis predicate.
     *
     * @param lhs The left hand side of the comparison predicate.
     * @param rhs The right hand side of the comparison predicate.
     * @param op The comparison operator ({@code =}, {@code !=}, {@code <}, {@code <=}, {@code >}, or {@code >=}).
     * @param pred The whole CIF predicate, of which the comparison predicate is a part.
     * @param bpred The binary expression of the comparison predicate. Essentially '{@code lhs op rhs}'.
     * @param initial Whether the predicate applies only to the initial state ({@code true}) or any state
     *     ({@code false}, includes the initial state).
     * @param synthAut The synthesis automaton.
     * @return The synthesis predicate.
     * @throws UnsupportedPredicateException If the predicate is not supported.
     */
    private static BDD convertCmpPred(Expression lhs, Expression rhs, BinaryOperator op, Expression pred,
            BinaryExpression bpred, boolean initial, SynthesisAutomaton synthAut) throws UnsupportedPredicateException
    {
        // Check lhs and rhs types.
        CifType ltype = normalizeType(lhs.getType());
        CifType rtype = normalizeType(rhs.getType());
        if (!(ltype instanceof BoolType && rtype instanceof BoolType)
                && !(ltype instanceof EnumType && rtype instanceof EnumType)
                && !(ltype instanceof IntType && rtype instanceof IntType))
        {
            String msg = fmt("binary operator \"%s\" on values of types \"%s\" and \"%s\" is not supported.",
                    CifTextUtils.operatorToStr(op), CifTextUtils.typeToStr(ltype), CifTextUtils.typeToStr(rtype));
            throw new UnsupportedPredicateException(msg, bpred);
        }

        // Special handling of boolean values.
        if (ltype instanceof BoolType && rtype instanceof BoolType) {
            BDD lbdd = convertPred(lhs, initial, synthAut);
            BDD rbdd = convertPred(rhs, initial, synthAut);
            switch (op) {
                case EQUAL:
                    return lbdd.biimpWith(rbdd);

                case UNEQUAL: {
                    BDD eq = lbdd.biimpWith(rbdd);
                    BDD rslt = eq.not();
                    eq.free();
                    return rslt;
                }

                default:
                    throw new RuntimeException("Unexpected op: " + op);
            }
        }

        // Convert lhs and rhs to bit vectors.
        Supplier<String> partMsg = () -> fmt("predicate \"%s\"", CifTextUtils.exprToStr(pred));
        CifBddBitVectorAndCarry lrslt = convertExpr(lhs, initial, synthAut, false, partMsg);
        CifBddBitVectorAndCarry rrslt = convertExpr(rhs, initial, synthAut, false, partMsg);
        Assert.check(lrslt.carry.isZero());
        Assert.check(rrslt.carry.isZero());
        CifBddBitVector lvec = lrslt.vector;
        CifBddBitVector rvec = rrslt.vector;

        // Make bit vectors equal size.
        int length = Math.max(lvec.length(), rvec.length());
        lvec.resize(length);
        rvec.resize(length);

        // Apply comparison operator.
        BDD rslt;
        switch (op) {
            case LESS_THAN:
                rslt = lvec.lessThan(rvec);
                break;
            case LESS_EQUAL:
                rslt = lvec.lessOrEqual(rvec);
                break;
            case GREATER_THAN:
                rslt = lvec.greaterThan(rvec);
                break;
            case GREATER_EQUAL:
                rslt = lvec.greaterOrEqual(rvec);
                break;
            case EQUAL:
                rslt = lvec.equalTo(rvec);
                break;
            case UNEQUAL:
                rslt = lvec.unequalTo(rvec);
                break;

            default:
                throw new RuntimeException("Unexpected op: " + op);
        }
        lvec.free();
        rvec.free();
        return rslt;
    }

    /**
     * Converts a CIF expression to a BDD bit vector.
     *
     * @param expr The CIF expression. Has an integer or enumeration type.
     * @param initial Whether the predicate applies only to the initial state ({@code true}) or any state
     *     ({@code false}, includes the initial state).
     * @param synthAut The synthesis automaton.
     * @param allowSubtract Whether a subtraction is allowed ({@code true}) or not ({@code false}). It must only be
     *     allowed for top level expressions, if the caller can handle the potential unrepresentable results. For sub
     *     expressions, subtraction is never allowed.
     * @param partMsg A supplier of a part of an error message. In case the expression is invalid (e.g. static and can't
     *     be evaluated), the supplied error message part indicates of what larger thing the invalid expression is a
     *     part. The partial error message obtained from this supplier will be inserted for {@code "[supplied]"} in
     *     error messages of the form {@code "Failed to ... the ... part of [supplied]."}. The supplier is not used in
     *     case the expression is unsupported.
     * @return The BDD bit vector, and a carry indicating situations in which the expression results in an
     *     unrepresentable value. Currently, the only unrepresentable values are negative values resulting from a
     *     subtraction. If subtraction is not allowed, the carry is always {@link BDD#isZero() zero}.
     * @throws UnsupportedPredicateException If the predicate is not supported.
     * @throws InvalidInputException If a static part of the given expression can't be evaluated.
     */
    private static CifBddBitVectorAndCarry convertExpr(Expression expr, boolean initial, SynthesisAutomaton synthAut,
            boolean allowSubtract, Supplier<String> partMsg) throws UnsupportedPredicateException
    {
        // Variable references.
        if (expr instanceof DiscVariableExpression) {
            // Get variable.
            DiscVariable cifVar = ((DiscVariableExpression)expr).getVariable();
            int varIdx = getDiscVarIdx(synthAut.variables, cifVar);
            if (varIdx == -1) {
                throw new UnsupportedPredicateException();
            }
            SynthesisVariable var = synthAut.variables[varIdx];

            // Create bit vector for the domain of the variable.
            CifBddBitVector vector = CifBddBitVector.createDomain(var.domain);
            return new CifBddBitVectorAndCarry(vector, synthAut.factory.zero());
        } else if (expr instanceof InputVariableExpression) {
            // Get variable.
            InputVariable cifVar = ((InputVariableExpression)expr).getVariable();
            int varIdx = getInputVarIdx(synthAut.variables, cifVar);
            if (varIdx == -1) {
                throw new UnsupportedPredicateException();
            }
            SynthesisVariable var = synthAut.variables[varIdx];

            // Create bit vector for the domain of the variable.
            CifBddBitVector vector = CifBddBitVector.createDomain(var.domain);
            return new CifBddBitVectorAndCarry(vector, synthAut.factory.zero());
        } else if (expr instanceof AlgVariableExpression) {
            // Algebraic variable reference. Get the single defining value expression, representing the value of the
            // variable. It is in an 'if' expression if an equation is provided per location of an automaton with more
            // than one location.
            AlgVariable var = ((AlgVariableExpression)expr).getVariable();
            Expression value = CifEquationUtils.getSingleValueForAlgVar(var);

            // Convert the defining value expression instead.
            return convertExpr(value, initial, synthAut, allowSubtract, partMsg);
        }

        // Unary operators.
        if (expr instanceof UnaryExpression) {
            UnaryExpression uexpr = (UnaryExpression)expr;
            switch (uexpr.getOperator()) {
                case PLUS:
                    return convertExpr(uexpr.getChild(), initial, synthAut, false, partMsg);

                default:
                    break;
            }
        }

        // Binary operators.
        if (expr instanceof BinaryExpression) {
            BinaryExpression bexpr = (BinaryExpression)expr;
            Expression lhs = bexpr.getLeft();
            Expression rhs = bexpr.getRight();

            switch (bexpr.getOperator()) {
                case ADDITION: {
                    // Get lhs and rhs vectors.
                    CifBddBitVectorAndCarry lrslt = convertExpr(lhs, initial, synthAut, false, partMsg);
                    CifBddBitVectorAndCarry rrslt = convertExpr(rhs, initial, synthAut, false, partMsg);
                    Assert.check(lrslt.carry.isZero());
                    Assert.check(rrslt.carry.isZero());
                    CifBddBitVector lvec = lrslt.vector;
                    CifBddBitVector rvec = rrslt.vector;

                    // Calculate minimum needed vector length, taking into account the final carry bit.
                    int length = Math.max(lvec.length(), rvec.length()) + 1;

                    // Resize lhs and rhs vectors.
                    lvec.resize(length);
                    rvec.resize(length);

                    // Apply addition.
                    CifBddBitVectorAndCarry rslt = lvec.add(rvec);
                    Assert.check(rslt.carry.isZero());
                    lvec.free();
                    rvec.free();
                    return rslt;
                }

                case SUBTRACTION: {
                    // Handle subtraction only if allowed.
                    if (!allowSubtract) {
                        break;
                    }

                    // Get lhs and rhs vectors.
                    CifBddBitVectorAndCarry lrslt = convertExpr(lhs, initial, synthAut, false, partMsg);
                    CifBddBitVectorAndCarry rrslt = convertExpr(rhs, initial, synthAut, false, partMsg);
                    Assert.check(lrslt.carry.isZero());
                    Assert.check(rrslt.carry.isZero());
                    CifBddBitVector lvec = lrslt.vector;
                    CifBddBitVector rvec = rrslt.vector;

                    // Resize lhs and rhs vectors to be of equal length.
                    int length = Math.max(lvec.length(), rvec.length());
                    lvec.resize(length);
                    rvec.resize(length);

                    // Apply subtraction.
                    CifBddBitVectorAndCarry rslt = lvec.subtract(rvec);
                    lvec.free();
                    rvec.free();
                    return rslt;
                }

                case INTEGER_DIVISION:
                case MODULUS: {
                    // Convert lhs.
                    CifBddBitVectorAndCarry lrslt = convertExpr(lhs, initial, synthAut, false, partMsg);
                    Assert.check(lrslt.carry.isZero());
                    CifBddBitVector lvec = lrslt.vector;

                    // Evaluate rhs.
                    if (!CifValueUtils.hasSingleValue(rhs, initial, true)) {
                        String msg = "value is too complex to be statically evaluated.";
                        throw new UnsupportedPredicateException(msg, rhs);
                    }

                    Object rhsValueObj;
                    try {
                        rhsValueObj = CifEvalUtils.eval(rhs, initial);
                    } catch (CifEvalException ex) {
                        // It would be rather complex to provide more context in this error message.
                        String msg = fmt("Failed to statically evaluate the \"%s\" part of %s.",
                                CifTextUtils.exprToStr(rhs), partMsg.get());
                        throw new InvalidInputException(msg, ex);
                    }

                    // Check divisor (rhs value).
                    int divisor = (int)rhsValueObj;
                    if (divisor == 0) {
                        // Unsupported: always division by zero.
                        String msg = fmt("\"%s\" always results in division by zero.", CifTextUtils.exprToStr(expr));
                        throw new UnsupportedPredicateException(msg, expr);
                    } else if (divisor < 0) {
                        // Unsupported: can't represent negative integers.
                        String msg = fmt(
                                "\"%s\" performs division/modulus by a negative value, which is not supported.",
                                CifTextUtils.exprToStr(expr));
                        throw new UnsupportedPredicateException(msg, expr);
                    }

                    // Resize lhs vector if needed. The rhs needs to fit. For 'mod', the highest bit of the lhs needs to
                    // be 'false' as well.
                    boolean isDiv = bexpr.getOperator() == INTEGER_DIVISION;
                    int lhsLen = lvec.length();
                    if (!isDiv) {
                        lhsLen++;
                    }
                    int rhsLen = BddUtils.getMinimumBits(divisor);
                    int length = Math.max(lhsLen, rhsLen);
                    lvec.resize(length);

                    // Apply div/mod.
                    CifBddBitVector rslt = lvec.divmod(divisor, isDiv);
                    lvec.free();
                    return new CifBddBitVectorAndCarry(rslt, synthAut.factory.zero());
                }

                default:
                    break;
            }
        }

        // Conditional expression.
        if (expr instanceof IfExpression) {
            // Condition expression.
            IfExpression ifExpr = (IfExpression)expr;

            // Convert else.
            CifBddBitVectorAndCarry elseRslt = convertExpr(ifExpr.getElse(), initial, synthAut, false, partMsg);
            Assert.check(elseRslt.carry.isZero());
            CifBddBitVector rslt = elseRslt.vector;

            // Convert elifs/thens.
            for (int i = ifExpr.getElifs().size() - 1; i >= 0; i--) {
                ElifExpression elifExpr = ifExpr.getElifs().get(i);
                BDD elifGuards = convertPreds(elifExpr.getGuards(), initial, synthAut);
                CifBddBitVectorAndCarry elifThen = convertExpr(elifExpr.getThen(), initial, synthAut, false, partMsg);
                Assert.check(elifThen.carry.isZero());
                CifBddBitVector elifVector = elifThen.vector;
                int len = Math.max(rslt.length(), elifVector.length());
                rslt.resize(len);
                elifVector.resize(len);
                CifBddBitVector elifRslt = elifVector.ifThenElse(rslt, elifGuards);
                elifGuards.free();
                elifVector.free();
                rslt.free();
                rslt = elifRslt;
            }

            // Convert if/then.
            BDD ifGuards = convertPreds(ifExpr.getGuards(), initial, synthAut);
            CifBddBitVectorAndCarry ifThen = convertExpr(ifExpr.getThen(), initial, synthAut, false, partMsg);
            Assert.check(ifThen.carry.isZero());
            CifBddBitVector ifVector = ifThen.vector;
            int len = Math.max(rslt.length(), ifVector.length());
            rslt.resize(len);
            ifVector.resize(len);
            CifBddBitVector ifRslt = ifVector.ifThenElse(rslt, ifGuards);
            ifGuards.free();
            ifVector.free();
            rslt.free();
            rslt = ifRslt;

            // Return converted conditional expression.
            return new CifBddBitVectorAndCarry(rslt, synthAut.factory.zero());
        }

        // Switch expression.
        if (expr instanceof SwitchExpression) {
            SwitchExpression switchExpr = (SwitchExpression)expr;
            Expression value = switchExpr.getValue();
            List<SwitchCase> cases = switchExpr.getCases();

            // Convert else.
            CifBddBitVectorAndCarry elseRslt = convertExpr(last(cases).getValue(), initial, synthAut, false, partMsg);
            Assert.check(elseRslt.carry.isZero());
            CifBddBitVector rslt = elseRslt.vector;

            // Convert cases.
            for (int i = cases.size() - 2; i >= 0; i--) {
                SwitchCase cse = cases.get(i);
                Expression caseGuardExpr = CifTypeUtils.isAutRefExpr(value) ? cse.getKey() : newBinaryExpression(
                        deepclone(value), BinaryOperator.EQUAL, null, deepclone(cse.getKey()), newBoolType());
                BDD caseGuard = convertPred(caseGuardExpr, initial, synthAut);
                CifBddBitVectorAndCarry caseThen = convertExpr(cse.getValue(), initial, synthAut, false, partMsg);
                Assert.check(caseThen.carry.isZero());
                CifBddBitVector caseVector = caseThen.vector;
                int len = Math.max(rslt.length(), caseVector.length());
                rslt.resize(len);
                caseVector.resize(len);
                CifBddBitVector caseRslt = caseVector.ifThenElse(rslt, caseGuard);
                caseGuard.free();
                caseVector.free();
                rslt.free();
                rslt = caseRslt;
            }

            // Return converted switch expression.
            return new CifBddBitVectorAndCarry(rslt, synthAut.factory.zero());
        }

        // Static evaluable expression.
        if (!CifValueUtils.hasSingleValue(expr, initial, true)) {
            String msg = "value is too complex to be statically evaluated.";
            throw new UnsupportedPredicateException(msg, expr);
        }

        // Evaluate expression.
        Object valueObj;
        try {
            valueObj = CifEvalUtils.eval(expr, initial);
        } catch (CifEvalException ex) {
            // It would be rather complex to provide more context in this error message.
            String msg = fmt("Failed to statically evaluate the \"%s\" part of %s.", CifTextUtils.exprToStr(expr),
                    partMsg.get());
            throw new InvalidInputException(msg, ex);
        }

        // Create bit vector.
        if (valueObj instanceof Integer) {
            // Get integer value.
            int value = (Integer)valueObj;

            // Negative integer values not supported.
            if (value < 0) {
                String msg = fmt("value \"%d\" is unsupported, as it is negative.", value);
                throw new UnsupportedPredicateException(msg, expr);
            }

            // Create BDD bit vector for constant value.
            CifBddBitVector vector = CifBddBitVector.createInt(synthAut.factory, value);
            return new CifBddBitVectorAndCarry(vector, synthAut.factory.zero());
        } else {
            // Get enumeration declaration and literal.
            Assert.check(valueObj instanceof CifEnumLiteral);
            EnumLiteral lit = ((CifEnumLiteral)valueObj).literal;
            EnumDecl enumDecl = (EnumDecl)lit.eContainer();

            // Create bit vector.
            int litIdx = enumDecl.getLiterals().indexOf(lit);
            CifBddBitVector vector = CifBddBitVector.createInt(synthAut.factory, litIdx);
            return new CifBddBitVectorAndCarry(vector, synthAut.factory.zero());
        }
    }

    /**
     * Collect the events declared in the given component (recursively).
     *
     * @param comp The component.
     * @param events The events collected so far. Is modified in-place.
     */
    public static void collectEvents(ComplexComponent comp, List<Event> events) {
        // Collect locally.
        for (Declaration decl: comp.getDeclarations()) {
            if (decl instanceof Event) {
                events.add((Event)decl);
            }
        }

        // Collect recursively.
        if (comp instanceof Group) {
            for (Component child: ((Group)comp).getComponents()) {
                collectEvents((ComplexComponent)child, events);
            }
        }
    }

    /**
     * Collect the automata of the given component (recursively).
     *
     * @param comp The component.
     * @param automata The automata collected so far. Is modified in-place.
     */
    private static void collectAutomata(ComplexComponent comp, List<Automaton> automata) {
        if (comp instanceof Automaton) {
            // Add automaton.
            automata.add((Automaton)comp);
        } else {
            // Collect recursively.
            for (Component child: ((Group)comp).getComponents()) {
                collectAutomata((ComplexComponent)child, automata);
            }
        }
    }

    /**
     * Collect CIF objects for which synthesis variables need to be constructed.
     *
     * @param comp The component in which to collect (recursively).
     * @param objs The objects collected so far. Is modified in-place.
     */
    private static void collectVariableObjects(ComplexComponent comp, List<PositionObject> objs) {
        // Collect automaton locally.
        if (comp instanceof Automaton) {
            Automaton aut = (Automaton)comp;
            if (aut.getLocations().size() > 1) {
                objs.add(aut);
            }
        }

        // Collect variables locally.
        for (Declaration decl: comp.getDeclarations()) {
            if (decl instanceof DiscVariable) {
                objs.add(decl);
            }
            if (decl instanceof InputVariable) {
                objs.add(decl);
            }
        }

        // Collect recursively.
        if (comp instanceof Group) {
            for (Component child: ((Group)comp).getComponents()) {
                collectVariableObjects((ComplexComponent)child, objs);
            }
        }
    }

    /**
     * Returns the synthesis variable index of the given CIF discrete variable.
     *
     * @param vars The synthesis variables, in which to look for the given CIF discrete variable. May not be a
     *     dummy/internal location pointer variable created for an automaton with two or more locations.
     * @param var The CIF discrete variable to look for, and for which the index is to be returned.
     * @return The 0-based index of the synthesis variable, or {@code -1} if it is not found, due to it not being
     *     available, due to a precondition violation.
     */
    private static int getDiscVarIdx(SynthesisVariable[] vars, DiscVariable var) {
        // Make sure the given discrete variable is an actual discrete variable, and not a dummy one created for a
        // location pointer of an automaton.
        Assert.check(var.getType() != null);

        // Look up the discrete variable.
        return getTypedVarIdx(vars, var);
    }

    /**
     * Returns the synthesis variable index of the given CIF input variable.
     *
     * @param vars The synthesis variables, in which to look for the given CIF input variable.
     * @param var The CIF input variable to look for, and for which the index is to be returned.
     * @return The 0-based index of the synthesis variable, or {@code -1} if it is not found, due to it not being
     *     available, due to a precondition violation.
     */
    private static int getInputVarIdx(SynthesisVariable[] vars, InputVariable var) {
        return getTypedVarIdx(vars, var);
    }

    /**
     * Returns the synthesis variable index of the given CIF typed object, i.e. a discrete variable or an input
     * variable.
     *
     * @param vars The synthesis variables, in which to look for the given CIF typed object. May not be a dummy/internal
     *     location pointer variable created for an automaton with two or more locations.
     * @param var The CIF variable to look for, and for which the index is to be returned.
     * @return The 0-based index of the synthesis variable, or {@code -1} if it is not found, due to it not being
     *     available, due to a precondition violation.
     */
    private static int getTypedVarIdx(SynthesisVariable[] vars, Declaration var) {
        for (int i = 0; i < vars.length; i++) {
            SynthesisVariable synthVar = vars[i];
            if (synthVar == null) {
                continue;
            }
            if (!(synthVar instanceof SynthesisTypedVariable)) {
                continue;
            }
            SynthesisTypedVariable synthTypedVar = (SynthesisTypedVariable)synthVar;
            if (synthTypedVar.obj == var) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the synthesis variable index of the given CIF automaton, or rather the location pointer variable for that
     * automaton.
     *
     * @param vars The synthesis variables, in which to look for the given CIF automaton.
     * @param aut The CIF automaton to look for, and for which the index is to be returned.
     * @return The 0-based index of the synthesis variable, or {@code -1} if it is not found. If not found, no location
     *     pointer was created for the automaton, or the location pointer is not available, due to a precondition
     *     violation.
     */
    private static int getLpVarIdx(SynthesisVariable[] vars, Automaton aut) {
        for (int i = 0; i < vars.length; i++) {
            SynthesisVariable synthVar = vars[i];
            if (synthVar == null) {
                continue;
            }
            if (!(synthVar instanceof SynthesisLocPtrVariable)) {
                continue;
            }
            SynthesisLocPtrVariable synthLpVar = (SynthesisLocPtrVariable)synthVar;
            if (synthLpVar.aut == aut) {
                return i;
            }
        }
        return -1;
    }

    /** Exception to indicate an unsupported predicate. */
    private static class UnsupportedPredicateException extends Exception {
        /**
         * The (part of the) predicate that is not supported. May be {@code null} to indicate that the predicate is not
         * supported due to earlier precondition violations.
         */
        public final Expression expr;

        /**
         * Constructor for the {@link UnsupportedPredicateException} class, in case a predicate is unsupported due to
         * earlier precondition violations.
         */
        public UnsupportedPredicateException() {
            this(null, null);
        }

        /**
         * Constructor for the {@link UnsupportedPredicateException} class.
         *
         * @param msg A message describing the problem.
         * @param expr The (part of the) predicate that is not supported.
         */
        public UnsupportedPredicateException(String msg, Expression expr) {
            super(msg);
            this.expr = expr;
        }
    }
}
