////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.common;

import java.util.Collection;

/** Helper class for manipulating expressions. */
public class ExprHelper {
    private ExprHelper() {
    }

    public static boolean isNullOrTriviallyTrue(String s) {
        return s == null || s.equals("true");
    }

    /**
     * Gives the conjunction of all given strings as a single expression.
     *
     * @param exprs The strings to conjoin.
     * @return The conjunction of all given strings as a single expression.
     */
    public static String conjoinExprs(Collection<String> exprs) {
        return exprs.stream().filter(e -> e != null)
                .reduce((left, right) -> String.format("(%s) and (%s)", left, right)).orElse("true");
    }
}
