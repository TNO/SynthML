
package com.github.tno.pokayoke.transform.common;

import java.util.List;

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
    public static String conjoinExprs(List<String> exprs) {
        return exprs.stream().filter(e -> e != null)
                .reduce((left, right) -> String.format("(%s) and (%s)", left, right)).orElse("true");
    }
}
