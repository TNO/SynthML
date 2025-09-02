
package com.github.tno.pokayoke.transform.app;

//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023, 2025 Contributors to the Eclipse Foundation
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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.common.CifEvalException;
import org.eclipse.escet.cif.common.CifEvalUtils;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.common.java.Assert;

/** CIF state annotation wrapper class, for proper hashing and equality. */
public class StateAnnotationEqHashWrap {
    /** The wrapped annotation. */
    public final Annotation annotation;

    /** Per annotation argument name, the evaluated argument value. */
    private final Map<String, Object> argNamesToValues;

    /** Per unique value of unnamed arguments of the annotation, the number of unnamed arguments that have the value. */
    private final Map<Object, Long> unnamedArgsValues;

    /** The cached hash code of this wrapper. */
    private final int hashCode;

    /**
     * Constructor for the {@link StateAnnotationEqHashWrap}.
     *
     * @param annotation The wrapped annotation.
     */
    public StateAnnotationEqHashWrap(Annotation annotation) {
        Assert.areEqual("state", annotation.getName());

        this.annotation = annotation;
        this.argNamesToValues = annotation.getArguments().stream().filter(arg -> arg.getName() != null)
                .collect(Collectors.toMap(arg -> arg.getName(), arg -> evalAnnoArgValue(arg.getValue())));
        this.unnamedArgsValues = annotation.getArguments().stream().filter(arg -> arg.getName() == null)
                .map(arg -> evalAnnoArgValue(arg.getValue()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        this.hashCode = annotation.getName().hashCode() + argNamesToValues.hashCode();
    }

    /**
     * Evaluate an annotation argument value.
     *
     * @param value The argument value.
     * @return The evaluated argument value.
     */
    private static Object evalAnnoArgValue(Expression value) {
        try {
            return CifEvalUtils.eval(value, false);
        } catch (CifEvalException e) {
            // Type checker should have determined that it is safe to evaluate these literals.
            throw new RuntimeException("Failed to evaluate annotation argument value.", e);
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        // Check same object and same type of object.
        if (this == obj) {
            return true;
        }
        StateAnnotationEqHashWrap other = (StateAnnotationEqHashWrap)obj;

        // Check annotation name and arguments:
        // - We use a map of argument names as the order of the named arguments is not relevant.
        // - For unnamed arguments, we use a map as well, as there the order is also not relevant. The map does take
        // into account how many arguments have a specific value, since the number of times that an argument is
        // provided may be relevant.
        // - And we use evaluated argument values such that for instance values '{1, 2}' and '{2, 1}' are considered
        // equal.
        return this.annotation.getName().equals(other.annotation.getName())
                && this.argNamesToValues.equals(other.argNamesToValues)
                && this.unnamedArgsValues.equals(other.unnamedArgsValues);
    }
}
