////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.InvKind;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.track.SynthesisChainTracking;
import com.github.tno.pokayoke.transform.track.UmlToCifTranslationPurpose;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.CifParserHelper;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

/**
 * Base functionality for transforming models (e.g., Petri nets or activities) to CIF, in the context of Poka Yoke UML
 * specifications.
 */
public abstract class ModelToCifTranslator {
    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    protected final BiMap<Enumeration, EnumDecl> enumMap = HashBiMap.create();

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    protected final BiMap<EnumerationLiteral, EnumLiteral> enumLiteralMap = HashBiMap.create();

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    protected final BiMap<Property, DiscVariable> variableMap = HashBiMap.create();

    /** The context for querying the input UML model. */
    protected final CifContext context;

    /** The translator for UML annotations (guards, updates, invariants, etc.). */
    protected final UmlAnnotationsToCif translator;

    /** The purpose for which UML is translated to CIF. */
    protected final UmlToCifTranslationPurpose translationPurpose;

    /**
     * The tracker that indicates how results from intermediate steps of the activity synthesis chain relate to the
     * input UML.
     */
    protected final SynthesisChainTracking synthesisTracker;

    /** A list of warnings to notify the user of, which is modified in-place. */
    protected final List<String> warnings;

    /**
     * Constructs a new {@link ModelToCifTranslator}.
     *
     * @param context The context for querying the input UML model.
     * @param tracker The tracker that indicates how results from intermediate steps of the activity synthesis chain
     *     relate to the input UML.
     * @param purpose The translation purpose.
     * @param warnings Any warnings to notify the user of, which is modified in-place.
     */
    public ModelToCifTranslator(CifContext context, SynthesisChainTracking tracker, UmlToCifTranslationPurpose purpose,
            List<String> warnings)
    {
        this.context = context;
        this.translationPurpose = purpose;
        this.synthesisTracker = tracker;
        this.translator = new UmlAnnotationsToCif(context, enumMap, enumLiteralMap, variableMap, tracker,
                translationPurpose);
        this.warnings = warnings;
    }

    /**
     * Gives the mapping from UML enumerations to corresponding translated CIF enumeration declarations.
     *
     * @return The mapping from UML enumerations to corresponding translated CIF enumeration declarations.
     */
    public BiMap<Enumeration, EnumDecl> getEnumerationMap() {
        return ImmutableBiMap.copyOf(enumMap);
    }

    /**
     * Gives the mapping from UML enumeration literals to corresponding translated CIF enumeration literals.
     *
     * @return The mapping from UML enumeration literals to corresponding translated CIF enumeration literals.
     */
    public BiMap<EnumerationLiteral, EnumLiteral> getEnumerationLiteralMap() {
        return ImmutableBiMap.copyOf(enumLiteralMap);
    }

    /**
     * Gives the mapping from UML properties to corresponding translated CIF discrete variables.
     *
     * @return The mapping from UML properties to corresponding translated CIF discrete variables.
     */
    public BiMap<Property, DiscVariable> getPropertyMap() {
        return ImmutableBiMap.copyOf(variableMap);
    }

    /**
     * Translates all UML enumerations that are in context to CIF enumeration declarations. This method should be called
     * at most once.
     *
     * @return The translated CIF enumeration declarations.
     */
    protected List<EnumDecl> translateEnumerations() {
        // Translates all UML enumerations that are in context to CIF enumeration declarations.
        List<Enumeration> umlEnums = context.getAllEnumerations();
        List<EnumDecl> cifEnums = new ArrayList<>(umlEnums.size());

        for (Enumeration umlEnum: umlEnums) {
            EnumDecl cifEnum = CifConstructors.newEnumDecl(null, null, umlEnum.getName(), null);
            cifEnums.add(cifEnum);
            enumMap.put(umlEnum, cifEnum);
        }

        // Translates all UML enumeration literals that are in context to CIF enumeration literals.
        for (EnumerationLiteral umlLiteral: context.getAllEnumerationLiterals()) {
            EnumLiteral cifLiteral = CifConstructors.newEnumLiteral(null, umlLiteral.getName(), null);
            enumMap.get(umlLiteral.getEnumeration()).getLiterals().add(cifLiteral);
            enumLiteralMap.put(umlLiteral, cifLiteral);
        }

        return cifEnums;
    }

    /**
     * Translates all UML properties that are in context to CIF discrete variables. This method should be called at most
     * once.
     *
     * @return The translated CIF discrete variables.
     */
    protected List<DiscVariable> translateProperties() {
        List<Property> umlProperties = context.getAllDeclaredProperties();
        List<DiscVariable> cifVariables = new ArrayList<>(umlProperties.size());

        for (Property umlProperty: umlProperties) {
            DiscVariable cifVariable = translateProperty(umlProperty);
            cifVariables.add(cifVariable);
            variableMap.put(umlProperty, cifVariable);
        }

        return cifVariables;
    }

    /**
     * Translates the given UML property to a CIF discrete variable.
     *
     * @param umlProperty The UML property to translate.
     * @return The translated CIF discrete variable.
     */
    private DiscVariable translateProperty(Property umlProperty) {
        DiscVariable cifVariable = CifConstructors.newDiscVariable();
        cifVariable.setName(umlProperty.getName());
        cifVariable.setType(translator.translateType(umlProperty.getType()));

        // Determine the default value(s) of the CIF variable.
        if (PokaYokeUmlProfileUtil.hasDefaultValue(umlProperty)) {
            // Translate the UML default property value.
            ValueSpecification umlDefaultValue = umlProperty.getDefaultValue();
            Expression cifDefaultValueExpr = translator.translate(CifParserHelper.parseExpression(umlDefaultValue));
            cifVariable.setValue(CifConstructors.newVariableValue(null, ImmutableList.of(cifDefaultValueExpr)));
        } else {
            // Indicate that the CIF variable can have any value by default.
            cifVariable.setValue(CifConstructors.newVariableValue());
        }

        return cifVariable;
    }

    /**
     * Gives the guard corresponding to the given CIF event.
     *
     * @param event The CIF event, which must have been translated for some UML element in the input UML model.
     * @return The guard corresponding to the given CIF event.
     */
    public Expression getGuard(Event event) {
        RedefinableElement element = synthesisTracker.getStartEventMap(translationPurpose).get(event);
        Preconditions.checkNotNull(element,
                "Expected a CIF event that has been translated for some UML element in the input UML model.");
        return getGuard(element);
    }

    /**
     * Gives the guard of the given UML element.
     *
     * @param element The UML element, which must not be a {@link ControlFlow control flow}.
     * @return The guard of the given UML element.
     */
    public Expression getGuard(RedefinableElement element) {
        return translateExpression(CifParserHelper.parseGuard(element));
    }

    /**
     * Gives the incoming guard of the given UML activity edge.
     *
     * @param edge The UML activity edge.
     * @return The incoming guard of the given UML activity edge.
     */
    public Expression getIncomingGuard(ActivityEdge edge) {
        if (edge instanceof ControlFlow controlFlow) {
            return translateExpression(CifParserHelper.parseIncomingGuard(controlFlow));
        } else {
            throw new RuntimeException(String.format("Expected a control flow, but got '%s'.", edge));
        }
    }

    /**
     * Gives the outgoing guard of the given UML activity edge.
     *
     * @param edge The UML activity edge.
     * @return The outgoing guard of the given UML activity edge.
     */
    public Expression getOutgoingGuard(ActivityEdge edge) {
        if (edge instanceof ControlFlow controlFlow) {
            return translateExpression(CifParserHelper.parseOutgoingGuard(controlFlow));
        } else {
            throw new RuntimeException(String.format("Expected a control flow, but got '%s'.", edge));
        }
    }

    /**
     * Translates the given expression to a CIF expression.
     *
     * @param expression The expression to translate.
     * @return The translated CIF expression.
     */
    protected Expression translateExpression(String expression) {
        return translateExpression(CifParserHelper.parseExpression(expression, context.getModel()));
    }

    /**
     * Translates the given expression to a CIF expression.
     *
     * @param expression The expression to translate.
     * @return The translated CIF expression.
     */
    private Expression translateExpression(AExpression expression) {
        if (expression == null) {
            return CifValueUtils.makeTrue();
        } else {
            return translator.translate(expression);
        }
    }

    /**
     * Gives all effects of the given UML element. Every effect consists of a list of updates.
     *
     * @param action The UML element.
     * @return All effects of the given UML element.
     */
    protected List<List<Update>> getEffects(RedefinableElement action) {
        return CifParserHelper.parseEffects(action).stream().map(translator::translate).toList();
    }

    /**
     * Gives the CIF requirement invariants of the given UML constraint.
     *
     * @param constraint The input UML constraint.
     * @return The CIF requirement invariants.
     */
    protected List<Invariant> getInvariants(Constraint constraint) {
        List<Invariant> result = translateInvariant(CifParserHelper.parseInvariant(constraint));
        if (result.isEmpty()) {
            warnings.add(String.format(
                    "Constraint '%s' was not translated, since its constrained elements are not used in the synthesized activity.",
                    constraint.getName()));
        }
        return result;
    }

    /**
     * Translates the given invariant to a CIF invariant.
     *
     * @param invariant The invariant to translate.
     * @return The translated CIF invariant.
     */
    private List<Invariant> translateInvariant(AInvariant invariant) {
        return translator.translate(invariant);
    }

    /**
     * Gives the CIF requirement state invariants of the given UML constraint.
     *
     * @param constraint The input UML constraint.
     * @return The CIF requirement state invariants.
     */
    protected Expression getStateInvariant(Constraint constraint) {
        List<Invariant> invariants = getInvariants(constraint);

        // Make sure the constraint has been translated to a single CIF invariant.
        int nrOfInvariants = invariants.size();
        Verify.verify(nrOfInvariants == 1,
                String.format("Expected exactly one translated invariant, but got %d.", nrOfInvariants));

        // Make sure that this single CIF invariant is a state invariant.
        Invariant invariant = invariants.get(0);
        Preconditions.checkArgument(invariant.getInvKind() == InvKind.STATE,
                "Expected the translated invariant to be a state invariant.");

        return invariant.getPredicate();
    }
}
