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

package org.eclipse.escet.cif.datasynth.varorder.parser;

import static org.eclipse.escet.common.java.Lists.first;
import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Lists.listc;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.datasynth.options.BddAdvancedVariableOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddDcshVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddForceVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddHyperEdgeAlgoOption;
import org.eclipse.escet.cif.datasynth.options.BddSlidingWindowSizeOption;
import org.eclipse.escet.cif.datasynth.options.BddSlidingWindowVarOrderOption;
import org.eclipse.escet.cif.datasynth.options.BddVariableOrderOption;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.graph.algos.PseudoPeripheralNodeFinderKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.RelationsKind;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrdererEffect;
import org.eclipse.escet.cif.datasynth.varorder.metrics.VarOrderMetricKind;
import org.eclipse.escet.cif.datasynth.varorder.orderers.ChoiceVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.CustomVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.DcshVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.ForceVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.ModelVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.RandomVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.ReverseVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.SequentialVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.SlidingWindowVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.SloanVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.SortedVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.VarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.orderers.WeightedCuthillMcKeeVarOrderer;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererInstance;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererListOrderersArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererMultiInstance;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererNumberArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererOrdererArg;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererSingleInstance;
import org.eclipse.escet.cif.datasynth.varorder.parser.ast.VarOrdererStringArg;
import org.eclipse.escet.common.app.framework.exceptions.InvalidOptionException;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.escet.common.typechecker.SemanticException;
import org.eclipse.escet.common.typechecker.TypeChecker;
import org.eclipse.escet.setext.runtime.Token;

/** Variable orderer type checker. */
public class VarOrdererTypeChecker extends TypeChecker<List<VarOrdererInstance>, VarOrderer> {
    /** The synthesis variables to order. */
    private final List<SynthesisVariable> variables;

    /**
     * Constructor for the {@link VarOrdererTypeChecker} class.
     *
     * @param variables The synthesis variables to order.
     */
    public VarOrdererTypeChecker(List<SynthesisVariable> variables) {
        this.variables = variables;
    }

    @Override
    protected VarOrderer transRoot(List<VarOrdererInstance> astInstances) {
        // Make sure basic and advanced options are not mixed.
        checkBasicAndAdvancedOptionsMix();

        // Process the advanced option.
        List<VarOrderer> orderers = checkVarOrderers(astInstances);
        VarOrderer orderer = (orderers.size() == 1) ? first(orderers) : new SequentialVarOrderer(orderers);
        return orderer;
    }

    /**
     * Type check variable orderers.
     *
     * @param astInstances The variable orderer instance AST objects.
     * @return The variable orderers (at least one).
     */
    private List<VarOrderer> checkVarOrderers(List<VarOrdererInstance> astInstances) {
        Assert.check(!astInstances.isEmpty());
        List<VarOrderer> orderers = listc(astInstances.size());
        for (VarOrdererInstance astOrderer: astInstances) {
            orderers.add(checkVarOrderer(astOrderer));
        }
        return orderers;
    }

    /**
     * Type check variable orderer.
     *
     * @param astInstance The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkVarOrderer(VarOrdererInstance astInstance) {
        // Handle multiple instances.
        if (astInstance instanceof VarOrdererMultiInstance multiInstance) {
            List<VarOrderer> orderers = checkVarOrderers(multiInstance.instances);
            VarOrderer orderer = (orderers.size() == 1) ? first(orderers) : new SequentialVarOrderer(orderers);
            return orderer;
        }

        // Handle single instance.
        Assert.check(astInstance instanceof VarOrdererSingleInstance);
        VarOrdererSingleInstance astOrderer = (VarOrdererSingleInstance)astInstance;
        String name = astOrderer.name.text;
        switch (name) {
            // Use basic variable ordering options.
            case "basic":
                return checkBasicOrderer(astOrderer);

            // Basic orderers.
            case "model":
                return checkModelOrderer(astOrderer);

            case "sorted":
                return checkSortedOrderer(astOrderer);

            case "random":
                return checkRandomOrderer(astOrderer);

            case "custom":
                return checkCustomOrderer(astOrderer);

            // Variable orderer algorithms.
            case "dcsh":
                return checkDcshVarOrderer(astOrderer);

            case "force":
                return checkForceVarOrderer(astOrderer);

            case "slidwin":
                return checkSlidWinVarOrderer(astOrderer);

            case "sloan":
                return checkSloanVarOrderer(astOrderer);

            case "weighted-cm":
                return checkWeightedCmVarOrderer(astOrderer);

            // Composite variable orderers.
            case "or":
                return checkChoiceVarOrderer(astOrderer);

            case "reverse":
                return checkReverseVarOrderer(astOrderer);

            // Unknown.
            default:
                addError(fmt("Unknown variable orderer \"%s\".", name), astOrderer.name.position);
                throw new SemanticException();
        }
    }

    /**
     * Type check a basic-ordering variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkBasicOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        if (!astOrderer.arguments.isEmpty()) {
            reportUnsupportedArgumentName(name, first(astOrderer.arguments));
            throw new SemanticException();
        }
        return getBasicConfiguredOrderer();
    }

    /**
     * Check whether basic options and advanced options for configuring BDD variable ordering are mixed.
     *
     * @throws InvalidOptionException If the options are mixed.
     */
    private void checkBasicAndAdvancedOptionsMix() {
        boolean basicDefault = //
                BddVariableOrderOption.isDefault() && //
                        BddDcshVarOrderOption.isDefault() && //
                        BddForceVarOrderOption.isDefault() && //
                        BddSlidingWindowVarOrderOption.isDefault() && //
                        BddSlidingWindowSizeOption.isDefault() && //
                        BddHyperEdgeAlgoOption.isDefault();
        boolean advancedDefault = BddAdvancedVariableOrderOption.isDefault();

        if (!basicDefault && !advancedDefault) {
            throw new InvalidOptionException(
                    "The BDD variable ordering is configured through basic and advanced options, "
                            + "which is not supported. Use only basic or only advanced options.");
        }
    }

    /**
     * Get the variable orderer configured via the basic (non-advanced) options.
     *
     * @return The variable orderer.
     */
    private VarOrderer getBasicConfiguredOrderer() {
        VarOrderer initialOrderer = getBasicConfiguredInitialOrderer();
        List<VarOrderer> orderers = list(initialOrderer);
        if (BddDcshVarOrderOption.isEnabled()) {
            orderers.add(new DcshVarOrderer(PseudoPeripheralNodeFinderKind.GEORGE_LIU, VarOrderMetricKind.WES,
                    getBasicConfiguredRelationsKind("dcsh"), VarOrdererEffect.VAR_ORDER));
        }
        if (BddForceVarOrderOption.isEnabled()) {
            orderers.add(new ForceVarOrderer(VarOrderMetricKind.TOTAL_SPAN, getBasicConfiguredRelationsKind("force"),
                    VarOrdererEffect.VAR_ORDER));
        }
        if (BddSlidingWindowVarOrderOption.isEnabled()) {
            int maxLen = BddSlidingWindowSizeOption.getMaxLen();
            orderers.add(new SlidingWindowVarOrderer(maxLen, VarOrderMetricKind.TOTAL_SPAN,
                    getBasicConfiguredRelationsKind("slidwin"), VarOrdererEffect.VAR_ORDER));
        }
        return (orderers.size() == 1) ? first(orderers) : new SequentialVarOrderer(orderers);
    }

    /**
     * Get the initial variable orderer configured via the basic (non-advanced) option.
     *
     * @return The initial variable orderer.
     */
    private VarOrderer getBasicConfiguredInitialOrderer() {
        String orderTxt = BddVariableOrderOption.getOrder().trim();
        String orderTxtLower = orderTxt.toLowerCase(Locale.US);
        if (orderTxtLower.equals("model")) {
            return new ModelVarOrderer(VarOrdererEffect.BOTH);
        } else if (orderTxtLower.equals("reverse-model")) {
            return new SequentialVarOrderer(list(new ModelVarOrderer(VarOrdererEffect.VAR_ORDER),
                    new ReverseVarOrderer(getBasicConfiguredRelationsKind("reverse"), VarOrdererEffect.BOTH)));
        } else if (orderTxtLower.equals("sorted")) {
            return new SortedVarOrderer(VarOrdererEffect.BOTH);
        } else if (orderTxtLower.equals("reverse-sorted")) {
            return new SequentialVarOrderer(list(new SortedVarOrderer(VarOrdererEffect.VAR_ORDER),
                    new ReverseVarOrderer(getBasicConfiguredRelationsKind("reverse"), VarOrdererEffect.BOTH)));
        } else if (orderTxtLower.equals("random")) {
            return new RandomVarOrderer(null, VarOrdererEffect.BOTH);
        } else if (orderTxtLower.startsWith("random:")) {
            int idx = orderTxt.indexOf(":");
            String seedTxt = orderTxt.substring(idx + 1).trim();
            long seed;
            try {
                seed = Long.parseUnsignedLong(seedTxt);
            } catch (NumberFormatException ex) {
                String msg = fmt("Invalid BDD random variable order seed number: \"%s\".", orderTxt);
                throw new InvalidOptionException(msg, ex);
            }
            return new RandomVarOrderer(seed, VarOrdererEffect.BOTH);
        } else {
            Pair<VarOrder, String> customVarOrderOrError = CustomVarOrderParser.parse(orderTxt, variables);
            if (customVarOrderOrError.right != null) {
                throw new InvalidOptionException("Invalid BDD variable random order: " + customVarOrderOrError.right);
            }
            return new CustomVarOrderer(customVarOrderOrError.left, VarOrdererEffect.BOTH);
        }
    }

    /**
     * Get the hyper-edges relations kind configured via the basic (non-advanced) option.
     *
     * @param ordererName The variable orderer name.
     * @return The relations kind.
     */
    private RelationsKind getBasicConfiguredRelationsKind(String ordererName) {
        switch (BddHyperEdgeAlgoOption.getAlgo()) {
            case LEGACY:
                return RelationsKind.LEGACY;
            case LINEARIZED:
                return RelationsKind.LINEARIZED;
            case DEFAULT: {
                boolean useLinearized = ordererName.equals("force") || ordererName.equals("slidwin");
                return useLinearized ? RelationsKind.LINEARIZED : RelationsKind.LEGACY;
            }
        }
        throw new RuntimeException("Unexpected option value: " + BddHyperEdgeAlgoOption.getAlgo());
    }

    /**
     * Type check a model-order variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkModelOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (effect == null) {
            effect = VarOrdererEffect.BOTH;
        }
        return new ModelVarOrderer(effect);
    }

    /**
     * Type check a sorted-order variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkSortedOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (effect == null) {
            effect = VarOrdererEffect.BOTH;
        }
        return new SortedVarOrderer(effect);
    }

    /**
     * Type check a random-order variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkRandomOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        Long seed = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "seed":
                    checkDuplicateArg(name, arg, seed);
                    seed = checkLongArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (effect == null) {
            effect = VarOrdererEffect.BOTH;
        }
        return new RandomVarOrderer(seed, effect);
    }

    /**
     * Type check a custom-order variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkCustomOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        VarOrder order = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "order":
                    checkDuplicateArg(name, arg, order);

                    if (!(arg instanceof VarOrdererStringArg)) {
                        reportUnsupportedArgumentValue(name, arg, "the value must be a string.");
                        throw new SemanticException();
                    }

                    Pair<VarOrder, String> customVarOrderOrError = CustomVarOrderParser
                            .parse(((VarOrdererStringArg)arg).text, variables);
                    if (customVarOrderOrError.right != null) {
                        reportUnsupportedArgumentValue(name, arg, customVarOrderOrError.right);
                        throw new SemanticException();
                    }

                    order = customVarOrderOrError.left;
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (order == null) {
            reportMissingArgument(astOrderer.name, "order");
            throw new SemanticException();
        }
        if (effect == null) {
            effect = VarOrdererEffect.BOTH;
        }
        return new CustomVarOrderer(order, effect);
    }

    /**
     * Type check a DCSH variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkDcshVarOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        PseudoPeripheralNodeFinderKind nodeFinder = null;
        VarOrderMetricKind metric = null;
        RelationsKind relations = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "node-finder":
                    checkDuplicateArg(name, arg, nodeFinder);
                    nodeFinder = checkEnumArg(name, arg, PseudoPeripheralNodeFinderKind.class,
                            "a node finder algorithm");
                    break;
                case "metric":
                    checkDuplicateArg(name, arg, metric);
                    metric = checkEnumArg(name, arg, VarOrderMetricKind.class, "a metric");
                    break;
                case "relations":
                    checkDuplicateArg(name, arg, relations);
                    relations = checkRelationsKindArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (nodeFinder == null) {
            nodeFinder = PseudoPeripheralNodeFinderKind.GEORGE_LIU;
        }
        if (metric == null) {
            metric = VarOrderMetricKind.WES;
        }
        if (relations == null) {
            relations = getBasicConfiguredRelationsKind(name);
        }
        if (effect == null) {
            effect = VarOrdererEffect.VAR_ORDER;
        }
        return new DcshVarOrderer(nodeFinder, metric, relations, effect);
    }

    /**
     * Type check a FORCE variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkForceVarOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        VarOrderMetricKind metric = null;
        RelationsKind relations = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "metric":
                    checkDuplicateArg(name, arg, metric);
                    metric = checkEnumArg(name, arg, VarOrderMetricKind.class, "a metric");
                    break;
                case "relations":
                    checkDuplicateArg(name, arg, relations);
                    relations = checkRelationsKindArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (metric == null) {
            metric = VarOrderMetricKind.TOTAL_SPAN;
        }
        if (relations == null) {
            relations = getBasicConfiguredRelationsKind(name);
        }
        if (effect == null) {
            effect = VarOrdererEffect.VAR_ORDER;
        }
        return new ForceVarOrderer(metric, relations, effect);
    }

    /**
     * Type check a sliding window variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkSlidWinVarOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        Integer size = null;
        VarOrderMetricKind metric = null;
        RelationsKind relations = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "size":
                    checkDuplicateArg(name, arg, size);
                    size = checkIntArg(name, arg);
                    if (size < 1 || size > 12) {
                        reportUnsupportedArgumentValue(name, arg, "the value must be in the range [1..12].");
                        throw new SemanticException();
                    }
                    break;
                case "metric":
                    checkDuplicateArg(name, arg, metric);
                    metric = checkEnumArg(name, arg, VarOrderMetricKind.class, "a metric");
                    break;
                case "relations":
                    checkDuplicateArg(name, arg, relations);
                    relations = checkRelationsKindArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (size == null) {
            size = BddSlidingWindowSizeOption.getMaxLen();
        }
        if (metric == null) {
            metric = VarOrderMetricKind.TOTAL_SPAN;
        }
        if (relations == null) {
            relations = getBasicConfiguredRelationsKind(name);
        }
        if (effect == null) {
            effect = VarOrdererEffect.VAR_ORDER;
        }
        return new SlidingWindowVarOrderer(size, metric, relations, effect);
    }

    /**
     * Type check a Sloan variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkSloanVarOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        RelationsKind relations = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "relations":
                    checkDuplicateArg(name, arg, relations);
                    relations = checkRelationsKindArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (relations == null) {
            relations = getBasicConfiguredRelationsKind(name);
        }
        if (effect == null) {
            effect = VarOrdererEffect.VAR_ORDER;
        }
        return new SloanVarOrderer(relations, effect);
    }

    /**
     * Type check a Weighted Cuthill-McKee variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkWeightedCmVarOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        PseudoPeripheralNodeFinderKind nodeFinder = null;
        RelationsKind relations = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "node-finder":
                    checkDuplicateArg(name, arg, nodeFinder);
                    nodeFinder = checkEnumArg(name, arg, PseudoPeripheralNodeFinderKind.class,
                            "a node finder algorithm");
                    break;
                case "relations":
                    checkDuplicateArg(name, arg, relations);
                    relations = checkRelationsKindArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (nodeFinder == null) {
            nodeFinder = PseudoPeripheralNodeFinderKind.GEORGE_LIU;
        }
        if (relations == null) {
            relations = getBasicConfiguredRelationsKind(name);
        }
        if (effect == null) {
            effect = VarOrdererEffect.VAR_ORDER;
        }
        return new WeightedCuthillMcKeeVarOrderer(nodeFinder, relations, effect);
    }

    /**
     * Type check a choice variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkChoiceVarOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        List<VarOrderer> choices = null;
        VarOrderMetricKind metric = null;
        RelationsKind relations = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "choices":
                    checkDuplicateArg(name, arg, choices);
                    if (!(arg instanceof VarOrdererListOrderersArg)) {
                        reportUnsupportedArgumentValue(name, arg, "the value must be a list of variable orderers.");
                        throw new SemanticException();
                    }
                    choices = checkVarOrderers(((VarOrdererListOrderersArg)arg).value);
                    if (choices.size() < 2) {
                        reportUnsupportedArgumentValue(name, arg,
                                "the value must be a list with at least two variable orderers.");
                        throw new SemanticException();
                    }
                    break;
                case "metric":
                    checkDuplicateArg(name, arg, metric);
                    metric = checkEnumArg(name, arg, VarOrderMetricKind.class, "a metric");
                    break;
                case "relations":
                    checkDuplicateArg(name, arg, relations);
                    relations = checkRelationsKindArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (choices == null) {
            reportMissingArgument(astOrderer.name, "choices");
            throw new SemanticException();
        }
        if (metric == null) {
            metric = VarOrderMetricKind.WES;
        }
        if (relations == null) {
            relations = getBasicConfiguredRelationsKind(name);
        }
        if (effect == null) {
            effect = VarOrdererEffect.VAR_ORDER;
        }
        return new ChoiceVarOrderer(choices, metric, relations, effect);
    }

    /**
     * Type check a reverse variable orderer.
     *
     * @param astOrderer The variable orderer instance AST object.
     * @return The variable orderer.
     */
    private VarOrderer checkReverseVarOrderer(VarOrdererSingleInstance astOrderer) {
        String name = astOrderer.name.text;
        RelationsKind relations = null;
        VarOrdererEffect effect = null;
        for (VarOrdererArg arg: astOrderer.arguments) {
            switch (arg.name.text) {
                case "relations":
                    checkDuplicateArg(name, arg, relations);
                    relations = checkRelationsKindArg(name, arg);
                    break;
                case "effect":
                    checkDuplicateArg(name, arg, effect);
                    effect = checkEnumArg(name, arg, VarOrdererEffect.class, "a variable orderer effect");
                    break;
                default:
                    reportUnsupportedArgumentName(name, arg);
                    throw new SemanticException();
            }
        }
        if (relations == null) {
            relations = getBasicConfiguredRelationsKind(name);
        }
        if (effect == null) {
            effect = VarOrdererEffect.VAR_ORDER;
        }
        return new ReverseVarOrderer(relations, effect);
    }

    /**
     * Check a value of an argument for being a duplicate.
     *
     * @param name The name of the variable orderer.
     * @param arg The argument.
     * @param curValue The current value of the argument. If not {@code null}, this argument provides a second value.
     */
    private void checkDuplicateArg(String name, VarOrdererArg arg, Object curValue) {
        if (curValue != null) {
            reportDuplicateArgument(name, arg);
            throw new SemanticException();
        }
    }

    /**
     * Check a value of an int-typed argument.
     *
     * @param name The name of the variable orderer.
     * @param arg The argument.
     * @return The value of the argument.
     */
    private int checkIntArg(String name, VarOrdererArg arg) {
        // Check for right kind of value.
        if (!(arg instanceof VarOrdererNumberArg)) {
            reportUnsupportedArgumentValue(name, arg, "the value must be a number.");
            throw new SemanticException();
        }

        // Parse the value.
        int value;
        try {
            value = Integer.parseInt(((VarOrdererNumberArg)arg).value.text);
        } catch (NumberFormatException e) {
            reportUnsupportedArgumentValue(name, arg, "the value is out of range.");
            throw new SemanticException();
        }

        // Return the value.
        return value;
    }

    /**
     * Check a value of a long-typed argument.
     *
     * @param name The name of the variable orderer.
     * @param arg The argument.
     * @return The value of the argument.
     */
    private long checkLongArg(String name, VarOrdererArg arg) {
        // Check for right kind of value.
        if (!(arg instanceof VarOrdererNumberArg)) {
            reportUnsupportedArgumentValue(name, arg, "the value must be a number.");
            throw new SemanticException();
        }

        // Parse the value.
        long value;
        try {
            value = Long.parseLong(((VarOrdererNumberArg)arg).value.text);
        } catch (NumberFormatException e) {
            reportUnsupportedArgumentValue(name, arg, "the value is out of range.");
            throw new SemanticException();
        }

        // Return the value.
        return value;
    }

    /**
     * Check a value of a enum-typed argument.
     *
     * @param <T> The enum type.
     * @param name The name of the variable orderer.
     * @param arg The argument.
     * @param enumClass The class of the enum.
     * @param valueDescription A description of the value.
     * @return The value of the argument.
     */
    private <T extends Enum<T>> T checkEnumArg(String name, VarOrdererArg arg, Class<T> enumClass,
            String valueDescription)
    {
        // Check for right kind of value.
        if (!(arg instanceof VarOrdererOrdererArg)) {
            reportUnsupportedArgumentValue(name, arg, fmt("the value must be %s.", valueDescription));
            throw new SemanticException();
        }
        VarOrdererInstance orderer = ((VarOrdererOrdererArg)arg).value;

        // Check for single.
        if (orderer instanceof VarOrdererMultiInstance) {
            reportUnsupportedArgumentValue(name, arg, fmt("the value must be %s.", valueDescription));
            throw new SemanticException();
        }
        VarOrdererSingleInstance single = (VarOrdererSingleInstance)orderer;

        // Check for no arguments.
        if (single.hasArgs) {
            reportUnsupportedArgumentValue(name, arg, fmt("the value must be %s.", valueDescription));
            throw new SemanticException();
        }

        // Parse the value.
        String constantName = single.name.text.replace("-", "_").toUpperCase(Locale.US);
        T[] values = enumClass.getEnumConstants();
        List<T> matches = Arrays.stream(values).filter(v -> v.name().equals(constantName)).collect(Collectors.toList());
        Assert.check(matches.size() <= 2);
        if (matches.size() == 1) {
            return first(matches);
        }

        // No matching enum constant found.
        reportUnsupportedArgumentValue(name, arg, fmt("the value must be %s.", valueDescription));
        throw new SemanticException();
    }

    /**
     * Check a value of a relations kind argument.
     *
     * @param name The name of the variable orderer.
     * @param arg The argument.
     * @return The value of the argument.
     */
    private RelationsKind checkRelationsKindArg(String name, VarOrdererArg arg) {
        // Check for right kind of value.
        if (!(arg instanceof VarOrdererOrdererArg)) {
            reportUnsupportedArgumentValue(name, arg, "the value must be a kind of relations.");
            throw new SemanticException();
        }
        VarOrdererInstance orderer = ((VarOrdererOrdererArg)arg).value;

        // Check for single.
        if (orderer instanceof VarOrdererMultiInstance) {
            reportUnsupportedArgumentValue(name, arg, "the value must be a kind of relations.");
            throw new SemanticException();
        }
        VarOrdererSingleInstance single = (VarOrdererSingleInstance)orderer;

        // Check for no arguments.
        if (single.hasArgs) {
            reportUnsupportedArgumentValue(name, arg, "the value must be a kind of relations.");
            throw new SemanticException();
        }

        // Parse the value.
        String constantName = single.name.text.replace("-", "_").toUpperCase(Locale.US);
        RelationsKind[] values = RelationsKind.class.getEnumConstants();
        List<RelationsKind> matches = Arrays.stream(values).filter(v -> v.name().equals(constantName))
                .collect(Collectors.toList());
        Assert.check(matches.size() <= 2);
        if (matches.size() == 1) {
            return first(matches);
        }

        // Parse special value 'configured'.
        if (constantName.equals("CONFIGURED")) {
            return getBasicConfiguredRelationsKind(name);
        }

        // No matching enum constant found.
        reportUnsupportedArgumentValue(name, arg, "the value must be a kind of relations.");
        throw new SemanticException();
    }

    /**
     * Report an unsupported name of an variable orderer argument.
     *
     * @param name The name of the variable orderer.
     * @param arg The unsupported argument.
     */
    private void reportUnsupportedArgumentName(String name, VarOrdererArg arg) {
        addError(fmt("The \"%s\" orderer does not support the \"%s\" argument.", name, arg.name.text),
                arg.name.position);
    }

    /**
     * Report a duplicate variable orderer argument.
     *
     * @param name The name of the variable orderer.
     * @param arg The duplicate argument.
     */
    private void reportDuplicateArgument(String name, VarOrdererArg arg) {
        addError(fmt("The \"%s\" orderer has a duplicate \"%s\" argument.", name, arg.name.text), arg.name.position);
    }

    /**
     * Report a missing mandatory variable orderer argument.
     *
     * @param name The name of the variable orderer.
     * @param missingArgName The name of the missing argument.
     */
    private void reportMissingArgument(Token name, String missingArgName) {
        addError(fmt("The \"%s\" orderer is missing its mandatory \"%s\" argument.", name.text, missingArgName),
                name.position);
    }

    /**
     * Report an unsupported value of an variable orderer argument.
     *
     * @param name The name of the variable orderer.
     * @param arg The unsupported argument.
     * @param details A detail message describing why the value is not supported. Must end with a period.
     */
    private void reportUnsupportedArgumentValue(String name, VarOrdererArg arg, String details) {
        addError(fmt("The \"%s\" orderer has an unsupported value for the \"%s\" argument: %s", name, arg.name.text,
                details), arg.name.position);
    }
}
