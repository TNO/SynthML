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

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAlgVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAlgVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAlphabet;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAssignmentFuncStatement;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newAutomaton;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newConstant;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newConstantExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariable;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdge;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEdgeEvent;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEventExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newField;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newFieldExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newFunctionCallExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newFunctionExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newFunctionParameter;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newGroup;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newIfExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newIntType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newInternalFunction;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newListExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newListType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocation;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newProjectionExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newReturnFuncStatement;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newSpecification;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newTupleExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newTupleType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newTypeDecl;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newTypeRef;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newWhileFuncStatement;
import static org.eclipse.escet.common.app.framework.output.OutputProvider.warn;
import static org.eclipse.escet.common.emf.EMFHelper.deepclone;
import static org.eclipse.escet.common.java.Lists.first;
import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Maps.map;
import static org.eclipse.escet.common.java.Maps.mapc;
import static org.eclipse.escet.common.java.Sets.setc;
import static org.eclipse.escet.common.java.Strings.fmt;
import static org.eclipse.escet.common.java.Strings.str;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.escet.cif.cif2cif.CifToCifPreconditionException;
import org.eclipse.escet.cif.cif2cif.RemoveRequirements;
import org.eclipse.escet.cif.common.CifScopeUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifTypeUtils;
import org.eclipse.escet.cif.common.CifValidationUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.datasynth.bdd.BddToCif;
import org.eclipse.escet.cif.datasynth.conversion.CifToSynthesisConverter.UnsupportedPredicateException;
import org.eclipse.escet.cif.datasynth.options.BddOutputNamePrefixOption;
import org.eclipse.escet.cif.datasynth.options.BddOutputOption;
import org.eclipse.escet.cif.datasynth.options.BddOutputOption.BddOutputMode;
import org.eclipse.escet.cif.datasynth.options.BddSimplify;
import org.eclipse.escet.cif.datasynth.options.BddSimplifyOption;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Alphabet;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Constant;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.declarations.TypeDecl;
import org.eclipse.escet.cif.metamodel.cif.expressions.AlgVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.ConstantExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.FieldExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.FunctionCallExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.FunctionExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IfExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.ListExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.ProjectionExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.TupleExpression;
import org.eclipse.escet.cif.metamodel.cif.functions.AssignmentFuncStatement;
import org.eclipse.escet.cif.metamodel.cif.functions.FunctionParameter;
import org.eclipse.escet.cif.metamodel.cif.functions.InternalFunction;
import org.eclipse.escet.cif.metamodel.cif.functions.ReturnFuncStatement;
import org.eclipse.escet.cif.metamodel.cif.functions.WhileFuncStatement;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.Field;
import org.eclipse.escet.cif.metamodel.cif.types.ListType;
import org.eclipse.escet.cif.metamodel.cif.types.TupleType;
import org.eclipse.escet.common.app.framework.exceptions.InvalidOptionException;
import org.eclipse.escet.common.app.framework.output.OutputProvider;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;

import com.github.javabdd.BDD;

/** Converter to convert synthesis result back to CIF. */
public class SynthesisToCifConverter {
    /** The synthesis result, or {@code null} if not available. */
    private SynthesisAutomaton synthAut;

    /** The input CIF specification, or {@code null} if not available. May be modified in-place. */
    private Specification spec;

    /** The new CIF supervisor automaton, or {@code null} if not available. */
    private Automaton supervisor;

    /** The BDD output mode, or {@code null} if not available. */
    private BddOutputMode outputMode;

    /** The prefix to use for BDD related names in the output, or {@code null} if not available. */
    private String bddNamePrefix;

    /**
     * Mapping from BDD nodes to their array indices, or {@code null} if not available. The zero/one nodes are not
     * included.
     */
    private Map<BDD, Integer> bddNodeMap;

    /**
     * Mapping from BDD variable indices to the indices of the algebraic variables created to represent them. Is
     * {@code null} if not available. Only 'old' variable indices are included; 'new' variable indices are omitted.
     */
    private Map<Integer, Integer> bddVarIdxMap;

    /** BDD nodes type, or {@code null} if not available. */
    private ListType bddNodesType;

    /** BDD nodes constant, or {@code null} if not available. */
    private Constant bddNodesConst;

    /** BDD values variable, or {@code null} if not available. */
    private AlgVariable bddValuesVar;

    /** BDD evaluation function, or {@code null} if not available. */
    private InternalFunction bddEvalFunc;

    /**
     * Converts a synthesis result back to CIF. The original CIF specification is extended with an external supervisor,
     * to obtain the controlled system.
     *
     * @param synthAut The synthesis result.
     * @param spec The input CIF specification. Is modified in-place.
     * @param supName The name of the supervisor automaton.
     * @param supNamespace The namespace of the supervisor, or {@code null} for the empty namespace.
     * @return The output CIF specification, i.e. the modified input CIF specification.
     * @throws UnsupportedPredicateException
     */
    public Specification convert(SynthesisAutomaton synthAut, Specification spec, String supName, String supNamespace)
            throws UnsupportedPredicateException
    {
        // Initialization.
        this.synthAut = synthAut;
        this.spec = spec;
        this.supervisor = null;
        this.outputMode = BddOutputOption.getMode();
        this.bddNamePrefix = BddOutputNamePrefixOption.getPrefix();
        this.bddNodeMap = null;
        this.bddVarIdxMap = null;
        this.bddNodesConst = null;
        this.bddValuesVar = null;
        this.bddEvalFunc = null;

        // Remove temporary events created for input variables.
        for (Event event: synthAut.inputVarEvents) {
            EMFHelper.removeFromParentContainment(event);
            synthAut.alphabet.remove(event);
        }

        // Relabel requirement automata from input model to supervisors.
        relabelRequirementAutomata(spec);

        // Remove requirements.
        //
        // Note that requirement automata have already been relabeled as
        // supervisors, and are thus not removed.
        //
        // Whether it is allowed to remove the requirements depends on the BDD
        // predicate simplification option.
        try {
            // If we simplify against something, the 'something' needs to
            // remain to ensure we don't loose that restriction.
            EnumSet<BddSimplify> simplifications = BddSimplifyOption.getSimplifications();
            RemoveRequirements remover = new RemoveRequirements();
            remover.removeReqAuts = true;
            remover.removeStateEvtExclReqInvs = !simplifications.contains(BddSimplify.GUARDS_SE_EXCL_REQ_INVS);
            remover.removeStateReqInvs = !simplifications.contains(BddSimplify.GUARDS_STATE_REQ_INVS);
            remover.transform(spec);
        } catch (CifToCifPreconditionException ex) {
            // Unexpected, as we do not have requirement automata, and it is impossible to refer to requirement
            // invariants.
            throw new RuntimeException("Unexpected error.", ex);
        }

        // Relabel the remaining requirement invariants from the input model to supervisors.
        relabelRequirementInvariants(spec);

        // Construct new supervisor automaton.
        supervisor = createSupervisorAutomaton(supName);

        // Add the alphabet to the automaton. Only add controllable events, as
        // they may be restricted by the supervisor.
        Alphabet alphabet = newAlphabet();
        for (Event event: synthAut.alphabet) {
            if (!event.getControllable()) {
                continue;
            }

            EventExpression eventRef = newEventExpression(event, null, newBoolType());
            alphabet.getEvents().add(eventRef);
        }
        supervisor.setAlphabet(alphabet);

        // Add single nameless location, that is both initial and marked.
        Location cifLoc = newLocation();
        cifLoc.getInitials().add(CifValueUtils.makeTrue());
        cifLoc.getMarkeds().add(CifValueUtils.makeTrue());
        supervisor.getLocations().add(cifLoc);

        // Get controllable events for which we have to add self loops.
        Set<Event> controllables = setc(alphabet.getEvents().size());
        for (Event event: synthAut.alphabet) {
            if (event.getControllable()) {
                controllables.add(event);
            }
        }

        // Prepare for conversion of BDDs to CIF. No operations on BDDs are
        // allowed after this (this includes freeing BDDs), as it may lead to
        // re-allocation, breaking BDD equality/hashing and thus our internal
        // node mapping.
        prepareBddToCif();
//
        // --------------------------------Hacking code-------------------------------------------

        // Below are the names of two choice actions from choice-in-parallel example.
        String action1Name = "c_Drink_Coffee";
        String action2Name = "c_Drink_Tea";

        // Below are the names of two choice actions from feasibility study example.
//        String action1Name = "c_ALR_recenter_wafer";
//        String action2Name = "c_ALR_rotate_towards_LL1T";

        // Step 1: get synthesized conditions in BDD.
        Event action1 = controllables.stream().filter(e -> e.getName().equals(action1Name)).toList().get(0);
        Event action2 = controllables.stream().filter(e -> e.getName().equals(action2Name)).toList().get(0);

        BDD action1SynthesizedCondition = synthAut.outputGuards.get(action1);
        BDD action2SynthesizedCondition = synthAut.outputGuards.get(action2);
        Expression action1SynthesizedConditionExpr = convertPred(action1SynthesizedCondition);
        Expression action2SynthesizedConditionExpr = convertPred(action2SynthesizedCondition);

        // Print the synthesized conditions expressions.
        OutputProvider.out("Synthesized condition expression for " + action1Name);
        OutputProvider.out(CifTextUtils.exprToStr(action1SynthesizedConditionExpr));
        OutputProvider.out("Synthesized condition expression for " + action2Name);
        OutputProvider.out(CifTextUtils.exprToStr(action2SynthesizedConditionExpr));

        // Step 2: get state annotation in BDD.
        // Copy the annotation from the generated state space. Double quotations have been replaced with single
        // quotations.

        // Below are the annotations for choice-in-parallel example.
        String loc4 = "@state(constraint = 'busy', Drink_Coffee_happens_once = 'NotYetOccurred', Drink_Tea_happens_once = 'NotYetOccurred', Post = 'NotSatisfied', Spec = 'Home', Spec.Coffee_or_Tea = 'Unknown', Spec.Has_Drank_Coffee_or_Tea = false, Spec.Is_Computer_On = true, Spec.Is_Computer_Ready = false, Spec.Is_Ready_to_Work = false, Start_Working_happens_once = 'NotYetOccurred', Starting_Computer_happens_once = 'NotYetOccurred', sup = '*', Turn_Computer_On_happens_once = 'Occurred')";
        String loc5 = "@state(constraint = 'busy', Drink_Coffee_happens_once = 'NotYetOccurred', Drink_Tea_happens_once = 'NotYetOccurred', Post = 'NotSatisfied', Spec = 'Home', Spec.Coffee_or_Tea = 'Unknown', Spec.Has_Drank_Coffee_or_Tea = false, Spec.Is_Computer_On = true, Spec.Is_Computer_Ready = true, Spec.Is_Ready_to_Work = false, Start_Working_happens_once = 'NotYetOccurred', Starting_Computer_happens_once = 'Occurred', sup = '*', Turn_Computer_On_happens_once = 'Occurred')";
        String loc6 = "@state(constraint = 'idle', Drink_Coffee_happens_once = 'NotYetOccurred', Drink_Tea_happens_once = 'NotYetOccurred', Post = 'NotSatisfied', Spec = 'Home', Spec.Coffee_or_Tea = 'Coffee', Spec.Has_Drank_Coffee_or_Tea = false, Spec.Is_Computer_On = true, Spec.Is_Computer_Ready = false, Spec.Is_Ready_to_Work = false, Start_Working_happens_once = 'NotYetOccurred', Starting_Computer_happens_once = 'NotYetOccurred', sup = '*', Turn_Computer_On_happens_once = 'Occurred')";
        String loc7 = "@state(constraint = 'idle', Drink_Coffee_happens_once = 'NotYetOccurred', Drink_Tea_happens_once = 'NotYetOccurred', Post = 'NotSatisfied', Spec = 'Home', Spec.Coffee_or_Tea = 'Tea', Spec.Has_Drank_Coffee_or_Tea = false, Spec.Is_Computer_On = true, Spec.Is_Computer_Ready = false, Spec.Is_Ready_to_Work = false, Start_Working_happens_once = 'NotYetOccurred', Starting_Computer_happens_once = 'NotYetOccurred', sup = '*', Turn_Computer_On_happens_once = 'Occurred')";
        String loc8 = "@state(constraint = 'idle', Drink_Coffee_happens_once = 'NotYetOccurred', Drink_Tea_happens_once = 'NotYetOccurred', Post = 'NotSatisfied', Spec = 'Home', Spec.Coffee_or_Tea = 'Coffee', Spec.Has_Drank_Coffee_or_Tea = false, Spec.Is_Computer_On = true, Spec.Is_Computer_Ready = true, Spec.Is_Ready_to_Work = false, Start_Working_happens_once = 'NotYetOccurred', Starting_Computer_happens_once = 'Occurred', sup = '*', Turn_Computer_On_happens_once = 'Occurred')";
        String loc9 = "@state(constraint = 'idle', Drink_Coffee_happens_once = 'NotYetOccurred', Drink_Tea_happens_once = 'NotYetOccurred', Post = 'NotSatisfied', Spec = 'Home', Spec.Coffee_or_Tea = 'Tea', Spec.Has_Drank_Coffee_or_Tea = false, Spec.Is_Computer_On = true, Spec.Is_Computer_Ready = true, Spec.Is_Ready_to_Work = false, Start_Working_happens_once = 'NotYetOccurred', Starting_Computer_happens_once = 'Occurred', sup = '*', Turn_Computer_On_happens_once = 'Occurred')";
        String[] inputStates = new String[] {loc4, loc5, loc6, loc7, loc8, loc9};

        // Below are the annotations for feasibility study example.
//        String loc16 = "@state(ALR_claim_happens_once = 'Occurred', ALR_move_to_LL1T_happens_once = 'Occurred', ALR_release_happens_once = 'NotYetOccurred', ALR_rotate_towards_APA_happens_once = 'NotYetOccurred', ALR_rotate_towards_LL1T_happens_twice = 'OccurredOnce', ALR_unclamp_wafer_happens_once = 'Occurred', constraint = 'idle', LL1_claim_happens_once = 'Occurred', LL1_close_atmospheric_gate_happens_once = 'NotYetOccurred', LL1_open_atmospheric_gate_happens_once = 'Occurred', LL1_pumpdown_happens_once = 'NotYetOccurred', LL1_release_happens_once = 'NotYetOccurred', LL1_vent_happens_once = 'Occurred', Post = 'NotSatisfied', Spec = 'Home', Spec.ALR_claimed = true, Spec.ALR_has_wafer = false, Spec.ALR_position = 'AtLL1T', Spec.LL1_claimed = true, Spec.LL1_is_atmospheric = true, Spec.LL1_is_atmospheric_gate_open = true, Spec.LL1T_has_wafer = true, Spec.LL1T_is_wafer_centered = 'True', sup = '*')";
//        String loc17 = "@state(ALR_claim_happens_once = 'Occurred', ALR_move_to_LL1T_happens_once = 'Occurred', ALR_release_happens_once = 'NotYetOccurred', ALR_rotate_towards_APA_happens_once = 'NotYetOccurred', ALR_rotate_towards_LL1T_happens_twice = 'OccurredOnce', ALR_unclamp_wafer_happens_once = 'Occurred', constraint = 'idle', LL1_claim_happens_once = 'Occurred', LL1_close_atmospheric_gate_happens_once = 'NotYetOccurred', LL1_open_atmospheric_gate_happens_once = 'Occurred', LL1_pumpdown_happens_once = 'NotYetOccurred', LL1_release_happens_once = 'NotYetOccurred', LL1_vent_happens_once = 'Occurred', Post = 'NotSatisfied', Spec = 'Home', Spec.ALR_claimed = true, Spec.ALR_has_wafer = false, Spec.ALR_position = 'AtLL1T', Spec.LL1_claimed = true, Spec.LL1_is_atmospheric = true, Spec.LL1_is_atmospheric_gate_open = true, Spec.LL1T_has_wafer = true, Spec.LL1T_is_wafer_centered = 'False', sup = '*')";
//        String[] inputStates = new String[] {loc16, loc17};

        // Get the disjunction of BDDs generated from each input state;
        BDD disjunction = CIFExpressionHelper.getDisjunctionBDDOfStates(inputStates, synthAut);

        // Print the corresponding expression.
        Expression disjunctionExpression = convertPred(disjunction);
        OutputProvider.out("Expression for disjunction of expressions:");
        OutputProvider.out(CifTextUtils.exprToStr(disjunctionExpression));

        // Step 3: get action guards in BDDs.
        Automaton specComp = (Automaton)spec.getComponents().stream()
                .filter(component -> component.getName().equals("Spec")).toList().get(0);
        Location location = specComp.getLocations().stream().filter(loc -> loc.getName().equals("Home")).toList()
                .get(0);

        Expression action1ActionGuardExpr = CifValueUtils
                .createConjunction(CIFExpressionHelper.getActionGuard(action1Name, location.getEdges()));
        Expression action2ActionGuardExpr = CifValueUtils
                .createConjunction(CIFExpressionHelper.getActionGuard(action2Name, location.getEdges()));

        OutputProvider.out("Action guards for " + action1Name);
        OutputProvider.out(CifTextUtils.exprToStr(action1ActionGuardExpr));
        OutputProvider.out("Action guards for " + action2Name);
        OutputProvider.out(CifTextUtils.exprToStr(action2ActionGuardExpr));

        BDD action1ActionGuardBDD = CifToSynthesisConverter.convertPred(action1ActionGuardExpr, false, synthAut);
        BDD action2ActionGuardBDD = CifToSynthesisConverter.convertPred(action2ActionGuardExpr, false, synthAut);

        // Step 4: Simplify BDDs and concert BDDs into expression.
        BDD action1SimplifiedStateOnly = action1SynthesizedCondition.simplify(disjunction);
        BDD action2SimplifiedStateOnly = action2SynthesizedCondition.simplify(disjunction);
        Expression action1EdgeGuardExpr = convertPred(action1SimplifiedStateOnly);
        Expression action2EdgeGuardExpr = convertPred(action2SimplifiedStateOnly);

        OutputProvider.out("Print the result of the simplication against only the state info: ");
        OutputProvider.out(CifTextUtils.exprToStr(action1EdgeGuardExpr));
        OutputProvider.out(CifTextUtils.exprToStr(action2EdgeGuardExpr));

        BDD action1SimplifiedStateGuard = action1SynthesizedCondition.simplify(action1ActionGuardBDD)
                .simplify(disjunction);
        BDD action2SimplifiedStateGuard = action2SynthesizedCondition.simplify(action2ActionGuardBDD)
                .simplify(disjunction);
        action1EdgeGuardExpr = convertPred(action1SimplifiedStateGuard);
        action2EdgeGuardExpr = convertPred(action2SimplifiedStateGuard);

        OutputProvider.out("Print the result of the simplication against both the guard and the state info: ");
        OutputProvider.out(CifTextUtils.exprToStr(action1EdgeGuardExpr));
        OutputProvider.out(CifTextUtils.exprToStr(action2EdgeGuardExpr));

        // --------------------------------Hacking code-------------------------------------------

        // Add edges for controllable events.
        List<Edge> edges = listc(controllables.size());
        for (Entry<Event, BDD> entry: synthAut.outputGuards.entrySet()) {
            Event event = entry.getKey();
            BDD guard = entry.getValue();

            // Convert the guard to CIF.
            Expression cifGuard = convertPred(guard);

            // Add self loop.
            edges.add(createSelfLoop(event, list(cifGuard)));
        }

        // Sort edges by event, in ascending alphabetical order of the names of
        // the events.
        Collections.sort(edges, new EdgeSorter());
        cifLoc.getEdges().addAll(edges);

        // Add initialization predicate, if any.
        if (synthAut.initialOutput != null) {
            Expression initialPred = convertPred(synthAut.initialOutput);
            supervisor.getInitials().add(initialPred);
        }

        // After all predicates have been converted, finalize BDD conversion.
        finalizeBddToCif();

        // Add namespace, if requested.
        if (supNamespace != null) {
            spec = addNamespace(supNamespace);
            this.spec = spec;
        }

        // Return the modified input CIF specification as output.
        return spec;
    }

    /** Prepare for the conversion of BDDs to CIF. */
    private void prepareBddToCif() {
        switch (outputMode) {
            case NORMAL:
                // Nothing to prepare.
                return;

            case NODES: {
                // Make sure there are no declarations in the root of the
                // specification with the BDD name prefix.
                Set<String> names = CifScopeUtils.getSymbolNamesForScope(spec, null);
                for (String name: names) {
                    if (name.startsWith(bddNamePrefix)) {
                        String msg = fmt(
                                "Can't create BDD output using BDD output name prefix \"%s\", as a declaration "
                                        + "named \"%s\" already exists in the specification. Use the appropriate "
                                        + "option to specify a different name prefix.",
                                bddNamePrefix, name);
                        throw new InvalidOptionException(msg);
                    }
                }

                // Initialize node mapping.
                bddNodeMap = map();

                // Create node type.
                CifType nodeType0 = newIntType();
                CifType nodeType1 = newIntType();
                CifType nodeType2 = newIntType();
                Field nodeField0 = newField("var", null, nodeType0);
                Field nodeField1 = newField("low", null, nodeType1);
                Field nodeField2 = newField("high", null, nodeType2);
                List<Field> nodeFields = list(nodeField0, nodeField1, nodeField2);
                TupleType nodeType = newTupleType(nodeFields, null);

                TypeDecl nodeTypeDecl = newTypeDecl();
                nodeTypeDecl.setName(bddNamePrefix + "_node_type");
                nodeTypeDecl.setType(nodeType);
                spec.getDeclarations().add(nodeTypeDecl);

                // Create nodes type.
                bddNodesType = newListType();
                bddNodesType.setElementType(newTypeRef(null, nodeTypeDecl));

                TypeDecl nodesTypeDecl = newTypeDecl();
                nodesTypeDecl.setName(bddNamePrefix + "_nodes_type");
                nodesTypeDecl.setType(bddNodesType);
                spec.getDeclarations().add(nodesTypeDecl);

                // Create 'BDD nodes' constant.
                ListExpression nodesListExpr = newListExpression();
                nodesListExpr.setType(deepclone(bddNodesType));

                bddNodesConst = newConstant();
                bddNodesConst.setName(bddNamePrefix + "_nodes");
                bddNodesConst.setValue(nodesListExpr);
                bddNodesConst.setType(newTypeRef(null, nodesTypeDecl));
                spec.getDeclarations().add(bddNodesConst);

                // Get variables in sorted order.
                SynthesisVariable[] sortedVars = synthAut.variables.clone();
                Arrays.sort(sortedVars, (v, w) -> Strings.SORTER.compare(v.rawName, w.rawName));

                // Initialize BDD variable index mapping.
                int bddVarCnt = synthAut.factory.varNum();
                Assert.check(bddVarCnt % 2 == 0); // #old = #new, so total is even.
                bddVarIdxMap = mapc(bddVarCnt / 2);

                // Create 'BDD variable value' algebraic variables. Fill BDD
                // variable index mapping.
                List<AlgVariable> valueVars = listc(synthAut.factory.varNum());
                int cifVarIdx = 0;
                for (SynthesisVariable synthVar: sortedVars) {
                    int[] varIdxs = synthVar.domain.vars();
                    for (int i = 0; i < varIdxs.length; i++) {
                        AlgVariable var = newAlgVariable();
                        int bddVarIdx = varIdxs[i];
                        Assert.check(bddVarIdx % 2 == 0); // Is an old variable.
                        var.setName(bddNamePrefix + "_value" + str(cifVarIdx));
                        var.setType(newBoolType());
                        var.setValue(BddToCif.getBddVarPred(synthVar, i));
                        spec.getDeclarations().add(var);
                        valueVars.add(var);
                        bddVarIdxMap.put(bddVarIdx, cifVarIdx);
                        cifVarIdx++;
                    }
                }

                // Create 'BDD variable values' algebraic variable.
                ListType valuesType = newListType();
                bddVarCnt /= 2; // Skip new variables.
                valuesType.setLower(bddVarCnt);
                valuesType.setUpper(bddVarCnt);
                valuesType.setElementType(newBoolType());

                ListExpression valuesExpr = newListExpression();
                valuesExpr.setType(valuesType);
                for (AlgVariable valueVar: valueVars) {
                    AlgVariableExpression valueVarRef = newAlgVariableExpression();
                    valueVarRef.setVariable(valueVar);
                    valueVarRef.setType(newBoolType());
                    valuesExpr.getElements().add(valueVarRef);
                }

                bddValuesVar = newAlgVariable();
                bddValuesVar.setName(bddNamePrefix + "_values");
                bddValuesVar.setType(deepclone(valuesType));
                bddValuesVar.setValue(valuesExpr);
                spec.getDeclarations().add(bddValuesVar);

                // Create BDD evaluation function.
                bddEvalFunc = newInternalFunction();
                bddEvalFunc.setName(bddNamePrefix + "_eval");
                spec.getDeclarations().add(bddEvalFunc);

                bddEvalFunc.getReturnTypes().add(newBoolType());

                DiscVariable pvar1 = newDiscVariable();
                DiscVariable pvar2 = newDiscVariable();
                pvar1.setName("idx");
                pvar2.setName("values");
                pvar1.setType(newIntType());
                pvar2.setType(deepclone(valuesType));

                FunctionParameter param1 = newFunctionParameter(pvar1, null);
                FunctionParameter param2 = newFunctionParameter(pvar2, null);
                bddEvalFunc.getParameters().add(param1);
                bddEvalFunc.getParameters().add(param2);

                DiscVariable varNode = newDiscVariable();
                DiscVariable varVal = newDiscVariable();
                varNode.setName("node");
                varVal.setName("val");
                varNode.setType(newTypeRef(null, nodeTypeDecl));
                varVal.setType(newBoolType());
                bddEvalFunc.getVariables().add(varNode);
                bddEvalFunc.getVariables().add(varVal);

                DiscVariableExpression idxRef = newDiscVariableExpression();
                idxRef.setVariable(pvar1);
                idxRef.setType(deepclone(pvar1.getType()));

                BinaryExpression whileCond = newBinaryExpression();
                whileCond.setOperator(BinaryOperator.GREATER_EQUAL);
                whileCond.setLeft(idxRef);
                whileCond.setRight(CifValueUtils.makeInt(0));
                whileCond.setType(newBoolType());

                WhileFuncStatement whileStat = newWhileFuncStatement();
                bddEvalFunc.getStatements().add(whileStat);
                whileStat.getGuards().add(whileCond);

                DiscVariableExpression nodeRef = newDiscVariableExpression();
                nodeRef.setVariable(varNode);
                nodeRef.setType(deepclone(varNode.getType()));

                ConstantExpression nodesRef = newConstantExpression();
                nodesRef.setConstant(bddNodesConst);
                nodesRef.setType(deepclone(bddNodesConst.getType()));

                ProjectionExpression nodeIdxProj = newProjectionExpression();
                nodeIdxProj.setChild(nodesRef);
                nodeIdxProj.setIndex(deepclone(idxRef));
                nodeIdxProj.setType(newTypeRef(null, nodeTypeDecl));

                AssignmentFuncStatement asgn1 = newAssignmentFuncStatement();
                asgn1.setAddressable(nodeRef);
                asgn1.setValue(nodeIdxProj);
                whileStat.getStatements().add(asgn1);

                DiscVariableExpression valRef = newDiscVariableExpression();
                valRef.setVariable(varVal);
                valRef.setType(deepclone(varVal.getType()));

                FieldExpression varFieldRef = newFieldExpression();
                varFieldRef.setField(nodeField0);
                varFieldRef.setType(newIntType(0, null, 0));

                ProjectionExpression nodeVarProj = newProjectionExpression();
                nodeVarProj.setChild(deepclone(nodeRef));
                nodeVarProj.setIndex(varFieldRef);
                nodeVarProj.setType(deepclone(nodeType0));

                DiscVariableExpression valuesRef = newDiscVariableExpression();
                valuesRef.setVariable(pvar2);
                valuesRef.setType(deepclone(pvar2.getType()));

                ProjectionExpression valuesNodeVarProj = newProjectionExpression();
                valuesNodeVarProj.setChild(valuesRef);
                valuesNodeVarProj.setIndex(nodeVarProj);
                valuesNodeVarProj.setType(newBoolType());

                AssignmentFuncStatement asgn2 = newAssignmentFuncStatement();
                asgn2.setAddressable(valRef);
                asgn2.setValue(valuesNodeVarProj);
                whileStat.getStatements().add(asgn2);

                FieldExpression lowFieldRef = newFieldExpression();
                lowFieldRef.setField(nodeField1);
                lowFieldRef.setType(newIntType(1, null, 1));

                ProjectionExpression nodeLowProj = newProjectionExpression();
                nodeLowProj.setChild(deepclone(nodeRef));
                nodeLowProj.setIndex(lowFieldRef);
                nodeLowProj.setType(deepclone(nodeType1));

                FieldExpression highFieldRef = newFieldExpression();
                highFieldRef.setField(nodeField2);
                highFieldRef.setType(newIntType(2, null, 2));

                ProjectionExpression nodeHighProj = newProjectionExpression();
                nodeHighProj.setChild(deepclone(nodeRef));
                nodeHighProj.setIndex(highFieldRef);
                nodeHighProj.setType(deepclone(nodeType2));

                IfExpression ifExpr = newIfExpression();
                ifExpr.getGuards().add(deepclone(valRef));
                ifExpr.setThen(nodeHighProj);
                ifExpr.setElse(nodeLowProj);
                ifExpr.setType(newIntType());

                AssignmentFuncStatement asgn3 = newAssignmentFuncStatement();
                asgn3.setAddressable(deepclone(idxRef));
                asgn3.setValue(ifExpr);
                whileStat.getStatements().add(asgn3);

                BinaryExpression returnValue = newBinaryExpression();
                returnValue.setOperator(BinaryOperator.EQUAL);
                returnValue.setLeft(deepclone(idxRef));
                returnValue.setRight(CifValueUtils.makeInt(-1));
                returnValue.setType(newBoolType());

                ReturnFuncStatement returnStat = newReturnFuncStatement();
                bddEvalFunc.getStatements().add(returnStat);
                returnStat.getValues().add(returnValue);

                return;
            }
        }

        throw new RuntimeException("Unknown output mode: " + outputMode);
    }

    /** Finalize the conversion of BDDs to CIF. */
    private void finalizeBddToCif() {
        switch (outputMode) {
            case NORMAL:
                // Nothing to finalize.
                return;

            case NODES:
                bddNodesType.setLower(bddNodeMap.size());
                bddNodesType.setUpper(bddNodeMap.size());
        }
    }

    /**
     * Converts a BDD/predicate to CIF. May modify {@link #spec}.
     *
     * @param bdd The BDD to convert.
     * @return The expression to use in the CIF model to represent the BDD.
     */
    private Expression convertPred(BDD bdd) {
        switch (outputMode) {
            case NORMAL:
                return BddToCif.bddToCifPred(bdd, synthAut);

            case NODES: {
                // Add to node map, and get index.
                int idx = bddToNodeMap(bdd);
                if (idx == -1) {
                    return CifValueUtils.makeTrue();
                }
                if (idx == -2) {
                    return CifValueUtils.makeFalse();
                }
                Assert.check(idx >= 0);

                // Return function call to evaluate the BDD.
                FunctionExpression funcRef = newFunctionExpression();
                funcRef.setFunction(bddEvalFunc);
                funcRef.setType(CifTypeUtils.makeFunctionType(bddEvalFunc, null));

                Expression arg0 = CifValueUtils.makeInt(idx);

                AlgVariableExpression arg1 = newAlgVariableExpression();
                arg1.setVariable(bddValuesVar);
                arg1.setType(deepclone(bddValuesVar.getType()));

                FunctionCallExpression callExpr = newFunctionCallExpression();
                callExpr.setFunction(funcRef);
                callExpr.getArguments().add(arg0);
                callExpr.getArguments().add(arg1);
                callExpr.setType(newBoolType());

                return callExpr;
            }
        }

        throw new RuntimeException("Unknown output mode: " + outputMode);
    }

    /**
     * Adds the given BDD to the {@link #bddNodeMap} and {@link #bddNodesConst}.
     *
     * @param bdd The BDD node.
     * @return The 0-based index into the node array (index in the value of {@link #bddNodesConst} and the value that
     *     the BDD maps to in {@link #bddNodeMap}), or {@code -1} for the one/true node, or {@code -2} for the
     *     zero/false node.
     */
    private int bddToNodeMap(BDD bdd) {
        // Special case for true/false.
        if (bdd.isOne()) {
            return -1;
        }
        if (bdd.isZero()) {
            return -2;
        }

        // Convert a node only once.
        Integer idx = bddNodeMap.get(bdd);
        if (idx != null) {
            return idx;
        }

        // New node. Add to the mapping.
        idx = bddNodeMap.size();
        bddNodeMap.put(bdd, idx);

        // Add empty tuple expression to the list of nodes, to be filled later.
        ListExpression nodesExpr = (ListExpression)bddNodesConst.getValue();
        TupleExpression tupleExpr = newTupleExpression();
        nodesExpr.getElements().add(tupleExpr);

        // Recursively convert low/high edges of the BDD node.
        int lowIdx = bddToNodeMap(bdd.low());
        int highIdx = bddToNodeMap(bdd.high());

        // Fill the tuple expression.
        int bddVarIdx = bdd.var();
        Assert.check(bddVarIdx % 2 == 0); // Is an old variable.
        Integer cifVarIdx = bddVarIdxMap.get(bddVarIdx);
        Assert.notNull(cifVarIdx);
        tupleExpr.getFields().add(CifValueUtils.makeInt(cifVarIdx));
        tupleExpr.getFields().add(CifValueUtils.makeInt(lowIdx));
        tupleExpr.getFields().add(CifValueUtils.makeInt(highIdx));

        CifType fieldType0 = tupleExpr.getFields().get(0).getType();
        CifType fieldType1 = tupleExpr.getFields().get(1).getType();
        CifType fieldType2 = tupleExpr.getFields().get(2).getType();
        List<CifType> fieldTypes = list(fieldType0, fieldType1, fieldType2);
        CifType tupleType = CifTypeUtils.makeTupleType(fieldTypes, null);
        tupleExpr.setType(tupleType);

        // Return the index.
        return idx;
    }

    /**
     * Recursively relabel requirement automata to supervisors.
     *
     * @param component The component in which to recursively apply the relabeling.
     */
    private static void relabelRequirementAutomata(ComplexComponent component) {
        // Relabel requirement automata as supervisor.
        if (component instanceof Automaton) {
            Automaton aut = (Automaton)component;
            if (aut.getKind() == SupKind.REQUIREMENT) {
                aut.setKind(SupKind.SUPERVISOR);
            }
            return;
        }

        // Recursively relabel for groups.
        Group group = (Group)component;
        for (Component child: group.getComponents()) {
            relabelRequirementAutomata((ComplexComponent)child);
        }
    }

    /**
     * Recursively relabel requirement invariants to supervisors.
     *
     * @param component The component in which to recursively apply the relabeling.
     */
    private static void relabelRequirementInvariants(ComplexComponent component) {
        // Relabel requirement invariants in automata.
        if (component instanceof Automaton) {
            Automaton aut = (Automaton)component;
            relabelRequirementInvariants(aut.getInvariants());
            for (Location loc: aut.getLocations()) {
                relabelRequirementInvariants(loc.getInvariants());
            }
            return;
        }

        // Relabel invariants in the group.
        Group group = (Group)component;
        relabelRequirementInvariants(group.getInvariants());

        // Recursively relabel for groups.
        for (Component child: group.getComponents()) {
            relabelRequirementInvariants((ComplexComponent)child);
        }
    }

    /**
     * Relabel requirement invariants to supervisor.
     *
     * @param invs The invariants to relabel.
     */
    private static void relabelRequirementInvariants(List<Invariant> invs) {
        for (Invariant inv: invs) {
            if (inv.getSupKind() == SupKind.REQUIREMENT) {
                inv.setSupKind(SupKind.SUPERVISOR);
            }
        }
    }

    /**
     * Creates a supervisor automaton with the given name, in the root of the specification.
     *
     * @param supName The name of the supervisor automaton.
     * @return The newly created supervisor automaton.
     */
    private Automaton createSupervisorAutomaton(String supName) {
        // Create automaton.
        Automaton aut = newAutomaton();
        aut.setKind(SupKind.SUPERVISOR);

        // Give new supervisor automaton a unique name.
        String name = supName;
        Set<String> curNames;
        curNames = CifScopeUtils.getSymbolNamesForScope(spec, null);
        if (curNames.contains(supName)) {
            name = CifScopeUtils.getUniqueName(name, curNames, Collections.emptySet());
            warn("Supervisor automaton is named \"%s\" instead of \"%s\" to avoid a naming conflict.", name, supName);
        }
        aut.setName(name);

        // Add automaton to the specification.
        spec.getComponents().add(aut);
        return aut;
    }

    /**
     * Creates a self loop edge.
     *
     * @param event The event on the edge.
     * @param guards The guards of the edge.
     * @return The newly created self loop edge.
     */
    private static Edge createSelfLoop(Event event, List<Expression> guards) {
        EventExpression eventRef = newEventExpression();
        eventRef.setEvent(event);
        eventRef.setType(newBoolType());

        EdgeEvent edgeEvent = newEdgeEvent();
        edgeEvent.setEvent(eventRef);

        Edge edge = newEdge();
        edge.getGuards().addAll(guards);
        edge.getEvents().add(edgeEvent);

        return edge;
    }

    /**
     * Add a namespace around the resulting supervisor. With 'supervisor', we here mean the entire specification,
     * including plants and former requirements, both of which can be seen as observers for the added supervisor
     * automaton.
     *
     * @param namespace The (absolute) namespace name. Is not {@code null} and has already been
     *     {@link CifValidationUtils#isValidName}.
     * @return The new specification.
     */
    private Specification addNamespace(String namespace) {
        // Create new specification.
        Specification newSpec = newSpecification();

        // Collect the events from original specification.
        List<Event> events = list();
        CifToSynthesisConverter.collectEvents(spec, events);

        // Move events to new specification, in proper groups, to maintain
        // their original identity.
        for (Event event: events) {
            addEvent(newSpec, event);
        }

        // Create namespace.
        String[] namespaceParts = namespace.split("\\.");
        Group namespaceGroup;
        try {
            namespaceGroup = getGroup(newSpec, namespaceParts, 0);
        } catch (DuplicateNameException ex) {
            // The new specification only has groups and events. To a conflict
            // must be for an event.
            PositionObject obj = CifScopeUtils.getObject(ex.group, ex.name);
            Assert.check(obj instanceof Event);

            // Report error to user.
            String msg = fmt("Can't create supervisor namespace \"%s\": an event named \"%s\" already exists in %s.",
                    namespace, ex.name, CifTextUtils.getComponentText2(ex.group));
            throw new InvalidOptionException(msg);
        }

        // Make sure namespace is empty (no named declarations).
        Set<String> names = CifScopeUtils.getSymbolNamesForScope(namespaceGroup, null);
        if (!names.isEmpty()) {
            List<String> sortedNames = Sets.sortedstrings(names);
            for (int i = 0; i < sortedNames.size(); i++) {
                sortedNames.set(i, fmt("\"%s\"", sortedNames.get(i)));
            }
            String msg1 = fmt("Can't put supervisor in namespace \"%s\".", namespace);
            String msg2 = fmt("The namespace is not empty, as it contains the following declarations: %s.",
                    String.join(", ", sortedNames));
            Exception ex = new InvalidOptionException(msg2);
            throw new InvalidOptionException(msg1, ex);
        }

        // Move contents of the old specification to the new specification.
        namespaceGroup.getComponents().addAll(spec.getComponents());
        namespaceGroup.getDefinitions().addAll(spec.getDefinitions());
        namespaceGroup.getDeclarations().addAll(spec.getDeclarations());
        namespaceGroup.getInitials().addAll(spec.getInitials());
        namespaceGroup.getInvariants().addAll(spec.getInvariants());
        namespaceGroup.getMarkeds().addAll(spec.getMarkeds());
        namespaceGroup.getEquations().addAll(spec.getEquations());
        namespaceGroup.getIoDecls().addAll(spec.getIoDecls());

        // Return the new specification.
        return newSpec;
    }

    /**
     * Adds the given event to the given specification. The event is added in groups if necessary, to ensure the proper
     * identity (absolute name).
     *
     * @param spec The specification to which to add the event.
     * @param event The event to add.
     */
    private static void addEvent(Specification spec, Event event) {
        // Get absolute name of event.
        String[] names = CifTextUtils.getAbsName(event, false).split("\\.");

        // Get/create parent group.
        Group group;
        try {
            group = getGroup(spec, names, 1);
        } catch (DuplicateNameException ex) {
            // Can't occur: no naming conflicts in original specification.
            throw new RuntimeException("Can't occur.", ex);
        }

        // Add event. This moves it out of the original specification.
        group.getDeclarations().add(event);
    }

    /**
     * Obtains the group from the given specification that has the given names (excluding the last part, if applicable)
     * as its absolute name. If no such group exists, groups are created as necessary.
     *
     * @param spec The specification from which to obtain the groups. Is modified in-place if necessary.
     * @param names The absolute name of the group. Names must not be escaped for keywords.
     * @param cnt The number of parts of the absolute name to exclude/ignore. If zero, no parts are ignored, if one, the
     *     last part is ignored, if two, the last two parts are ignored, etc.
     * @return The obtained or created group.
     * @throws DuplicateNameException If creating a new group resulted in a duplicate name.
     */
    private static Group getGroup(Specification spec, String[] names, int cnt) throws DuplicateNameException {
        // Start at the specification.
        Group current = spec;

        // Process all but last part of the absolute name.
        NAMES:
        for (int i = 0; i < names.length - cnt; i++) {
            String name = names[i];

            // Get existing group.
            for (Component comp: current.getComponents()) {
                if (comp.getName().equals(name)) {
                    current = (Group)comp;
                    continue NAMES;
                }
            }

            // Check that creating a new group won't lead to duplicate names.
            Set<String> curNames;
            curNames = CifScopeUtils.getSymbolNamesForScope(current, null);
            if (curNames.contains(name)) {
                throw new DuplicateNameException(current, name);
            }

            // Create new group.
            Group group = newGroup();
            group.setName(name);
            current.getComponents().add(group);
            current = group;
        }
        return current;
    }

    /** Duplicate name within a group exception. */
    private static class DuplicateNameException extends Exception {
        /** The group that contains the duplicate name. */
        public final Group group;

        /** The duplicate name. */
        public final String name;

        /**
         * Constructor for the {@link DuplicateNameException} class.
         *
         * @param group The group that contains the duplicate name.
         * @param name The duplicate name.
         */
        public DuplicateNameException(Group group, String name) {
            this.group = group;
            this.name = name;
        }
    }

    /** Sorter to sort edges in ascending alphabetical order based on their absolute names. */
    private static class EdgeSorter implements Comparator<Edge> {
        @Override
        public int compare(Edge edge1, Edge edge2) {
            Expression eventRef1 = first(edge1.getEvents()).getEvent();
            Expression eventRef2 = first(edge2.getEvents()).getEvent();
            Event event1 = ((EventExpression)eventRef1).getEvent();
            Event event2 = ((EventExpression)eventRef2).getEvent();
            String name1 = CifTextUtils.getAbsName(event1, false);
            String name2 = CifTextUtils.getAbsName(event2, false);
            return Strings.SORTER.compare(name1, name2);
        }
    }
}
