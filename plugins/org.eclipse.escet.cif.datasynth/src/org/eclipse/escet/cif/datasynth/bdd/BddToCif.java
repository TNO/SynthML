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

package org.eclipse.escet.cif.datasynth.bdd;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newInputVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocationExpression;
import static org.eclipse.escet.common.emf.EMFHelper.deepclone;
import static org.eclipse.escet.common.java.Lists.last;
import static org.eclipse.escet.common.java.Lists.list;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.datasynth.spec.SynthesisAutomaton;
import org.eclipse.escet.cif.datasynth.spec.SynthesisDiscVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisInputVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisLocPtrVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisTypedVariable;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EnumLiteralExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.InputVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.IntExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.cif.metamodel.cif.types.BoolType;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.escet.cif.metamodel.cif.types.IntType;
import org.eclipse.escet.common.java.Assert;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDDomain;

/** BDD to CIF conversion functionality. */
public class BddToCif {
    /** Constructor for the {@link BddToCif} class. */
    private BddToCif() {
        // Static class.
    }

    /**
     * Converts a BDD to a CIF predicate.
     *
     * @param bdd The BDD.
     * @param aut The synthesis automaton.
     * @return The CIF predicate.
     */
    public static Expression bddToCifPred(BDD bdd, SynthesisAutomaton aut) {
        // Convert to both DNF and CNF.
        Expression predDnf = bddToCifPred(bdd, aut, true);
        Expression predCnf = bddToCifPred(bdd, aut, false);

        // Return smallest one.
        int sizeDnf = exprNodeSize(predDnf);
        int sizeCnf = exprNodeSize(predCnf);
        return (sizeCnf <= sizeDnf) ? predCnf : predDnf;
    }

    /**
     * Converts a BDD to a CIF predicate.
     *
     * @param bdd The BDD.
     * @param aut The synthesis automaton.
     * @param dnf Whether to create a Disjunctive Normal Form (DNF, {@code true}) or Conjunctive Normal Form (CNF,
     *     {@code false}) predicate.
     * @return The CIF predicate.
     */
    private static Expression bddToCifPred(BDD bdd, SynthesisAutomaton aut, boolean dnf) {
        // Special case for 'true' and 'false'.
        if (bdd.isZero()) {
            return CifValueUtils.makeFalse();
        }
        if (bdd.isOne()) {
            return CifValueUtils.makeTrue();
        }

        // Generic case. Use DNF (or CNF) like algorithm to explore all paths
        // to 'true' (or 'false').

        // Initialize valuation to don't cares (-1).
        byte[] valuation = new byte[aut.factory.varNum()];
        Arrays.fill(valuation, (byte)-1);

        // Get predicates for paths in the BDD to either 'true' or 'false'.
        List<Expression> paths = list();
        bddToCifPred(bdd, aut, valuation, paths, dnf);

        // Combine path predicates.
        Expression rslt = dnf ? CifValueUtils.createDisjunction(paths, true)
                : CifValueUtils.createConjunction(paths, true);

        // Return the DNF-like CIF predicate.
        return rslt;
    }

    /**
     * Converts a BDD to a CIF predicate.
     *
     * @param bdd The BDD.
     * @param aut The synthesis automaton.
     * @param valuation The valuation of the internal BDD variables, with {@code -1} for don't care, {@code 0} for
     *     'false', and {@code 1} for 'true'. It represents that the first variable can('t) have a certain value
     *     <i>and</i> the second variable can('t) have a certain value <i>and</i> etc.
     * @param paths The CIF predicates for the paths in the BDD to 'true' (DNF) or 'false' (CNF). Is extended in-place.
     * @param dnf Whether to create a Disjunctive Normal Form (DNF, {@code true}) or Conjunctive Normal Form (CNF,
     *     {@code false}) predicate.
     */
    private static void bddToCifPred(BDD bdd, SynthesisAutomaton aut, byte[] valuation, List<Expression> paths,
            boolean dnf)
    {
        if (bdd.isZero() && dnf) {
            // Paths to 'false' are ignored in DNF.
            return;
        } else if (bdd.isOne() && !dnf) {
            // Paths to 'true' are ignored in CNF.
            return;
        } else if (bdd.isZero() || bdd.isOne()) {
            // Found a new path for the result.

            // Initialize the parts of this path.
            List<Expression> parts = list();

            // Process all variables/domains.
            for (SynthesisVariable var: aut.variables) {
                // Get valuation for the domain of this variable.
                BDDDomain domain = var.domain;
                byte[] domainValuation = new byte[domain.varNum()];
                int[] varIdxs = domain.vars();
                for (int i = 0; i < varIdxs.length; i++) {
                    int varIdx = varIdxs[i];
                    domainValuation[i] = valuation[varIdx];
                }

                // Convert valuation for the variable to a CIF predicate. The
                // resulting CIF predicate is a part of the path.
                Expression pred = valuationToCif(var, domainValuation, dnf);
                parts.add(pred);
            }

            // Add the CIF predicate for the path.
            Expression path = dnf ? CifValueUtils.createConjunction(parts, true)
                    : CifValueUtils.createDisjunction(parts, true);
            paths.add(path);
        } else {
            // Intermediate BDD node. Get index of the variable of the node.
            int varIdx = bdd.var();

            // Try path via low edge.
            valuation[varIdx] = 0;
            BDD lowBdd = bdd.low();
            bddToCifPred(lowBdd, aut, valuation, paths, dnf);
            lowBdd.free();

            // Try path via high edge.
            valuation[varIdx] = 1;
            BDD highBdd = bdd.high();
            bddToCifPred(highBdd, aut, valuation, paths, dnf);
            highBdd.free();

            // Restore valuation.
            valuation[varIdx] = -1;
        }
    }

    /**
     * Converts a valuation for a synthesis variable to a CIF predicate.
     *
     * @param var The synthesis variable.
     * @param valuation The valuation of the internal BDD variables, with {@code -1} for don't care, {@code 0} for
     *     'false', and {@code 1} for 'true'. It represents that the first variable can('t) have a certain value
     *     <i>and</i> the second variable can('t) have a certain value <i>and</i> etc.
     * @param dnf Whether to create a Disjunctive Normal Form (DNF, {@code true}) or Conjunctive Normal Form (CNF,
     *     {@code false}) predicate.
     * @return The CIF predicate.
     */
    private static Expression valuationToCif(SynthesisVariable var, byte[] valuation, boolean dnf) {
        // Get possible values of the variable, from the valuation.
        boolean[] values = new boolean[var.count];
        if (!dnf) {
            Arrays.fill(values, true);
        }
        valuationToValues(valuation, 0, 0, values, var.lower, var.upper, dnf);

        // Create CIF predicate for the possible values of a variable.
        return valuesToCif(var, values);
    }

    /**
     * Computes the values of a synthesis variable for which a valuation of that variable evaluates to 'true'.
     *
     * @param valuation The valuation of the internal BDD variables, with {@code -1} for don't care, {@code 0} for
     *     'false', and {@code 1} for 'true'. It represents that the first variable can('t) have a certain value
     *     <i>and</i> the second variable can('t) have a certain value <i>and</i> etc.
     * @param offset The 0-based offset into the valuation. First call should provide zero, and recursive calls will
     *     increment it.
     * @param value The current value, computed so far. First call should provide zero, and recursive calls will
     *     increase the value as needed. For DNF, it is a value for which the predicate is 'true'. For CNF, it is a
     *     value for which the predicate is 'false'.
     * @param values Per possible value of the variable, whether the variable can have that value, according to the
     *     valuation. First call should provide an array of the appropriate length for the variable, with all 'false'
     *     elements for DNF or all 'true' elements for CNF. Elements are present for the range {@code [min..max]}.
     *     Elements may be modified in-place.
     * @param min The minimum value of the variable.
     * @param max The maximum value of the variable.
     * @param dnf Whether to create a Disjunctive Normal Form (DNF, {@code true}) or Conjunctive Normal Form (CNF,
     *     {@code false}) predicate.
     */
    private static void valuationToValues(byte[] valuation, int offset, int value, boolean[] values, int min, int max,
            boolean dnf)
    {
        // If entire valuation processed, we have a complete value.
        if (offset == valuation.length) {
            if (value >= min && value <= max) {
                // For DNF, variable can have that value. For CNF, variable
                // can't have that value.
                values[value - min] = dnf ? true : false;
            }
            return;
        }

        // 'false' or don't care: don't increase the value.
        if (valuation[offset] == 0 || valuation[offset] == -1) {
            valuationToValues(valuation, offset + 1, value, values, min, max, dnf);
        }

        // 'true' or don't care: increase the value.
        if (valuation[offset] == 1 || valuation[offset] == -1) {
            value += 1 << offset;
            valuationToValues(valuation, offset + 1, value, values, min, max, dnf);
        }
    }

    /**
     * Converts values of a variable to a CIF predicate that evaluates to 'true' if and only if the variable has one of
     * those values.
     *
     * @param var The synthesis variable..
     * @param possibles Per possible value of the variable, whether the variable can have that value. Elements are
     *     present for the range {@code [min..max]}.
     * @return The CIF predicate.
     */
    private static Expression valuesToCif(SynthesisVariable var, boolean[] possibles) {
        // Count possible and impossible values.
        int possibleCnt = 0;
        int impossibleCnt = 0;
        for (boolean possible: possibles) {
            if (possible) {
                possibleCnt++;
            }
            if (!possible) {
                impossibleCnt++;
            }
        }

        // Case for 'true' (variable can have any value).
        if (possibleCnt == possibles.length) {
            return CifValueUtils.makeTrue();
        }

        // Case for 'false' (variable can't have any value).
        if (impossibleCnt == possibles.length) {
            return CifValueUtils.makeFalse();
        }

        // Case for single possible value. Create 'var = value' predicate.
        if (possibleCnt == 1) {
            int trueIdx = ArrayUtils.indexOf(possibles, true);
            return createVarPred(var, trueIdx, BinaryOperator.EQUAL, true);
        }

        // Case for single impossible value. Create 'var != value' predicate.
        if (impossibleCnt == 1) {
            int falseIdx = ArrayUtils.indexOf(possibles, false);
            return createVarPred(var, falseIdx, BinaryOperator.UNEQUAL, true);
        }

        // Create 'small' representation.
        CifType type = (var instanceof SynthesisTypedVariable) ? ((SynthesisTypedVariable)var).type : null;

        if (type instanceof IntType) {
            // Split bits into consecutive ranges.
            boolean[] bits = possibles; // Values of the bits.
            List<Boolean> values = list(); // Values of the ranges.
            List<Integer> indices = list(); // Range start 0-based bit indices.
            List<Integer> counts = list(); // Size of range.
            for (int i = 0; i < bits.length; i++) {
                if (!values.isEmpty() && last(values).equals(bits[i])) {
                    counts.set(counts.size() - 1, last(counts) + 1);
                } else {
                    values.add(bits[i]);
                    indices.add(i);
                    counts.add(1);
                }
            }

            // Compute score for '0' bits and '1' bits. Single bit in range
            // adds score 1, more bits in range adds score 2. For '0' bits,
            // start with score 1, for final inverse.
            int score0 = 1;
            int score1 = 0;
            for (int i = 0; i < values.size(); i++) {
                int count = counts.get(i);
                if (values.get(i)) {
                    score1 += (count == 1) ? 1 : 2;
                } else {
                    score0 += (count == 1) ? 1 : 2;
                }
            }

            // Create predicates for the cheapest variant.
            boolean chosenValue = (score1 <= score0) ? true : false;
            List<Expression> rslts = list();
            for (int i = 0; i < values.size(); i++) {
                if (!values.get(i).equals(chosenValue)) {
                    continue;
                }

                if (counts.get(i) == 1) {
                    // v = n
                    rslts.add(createVarPred(var, indices.get(i), BinaryOperator.EQUAL, true));
                } else if (counts.get(i) == 2) {
                    // v = n or v = n + 1
                    rslts.add(createVarPred(var, indices.get(i), BinaryOperator.EQUAL, true));
                    rslts.add(createVarPred(var, indices.get(i) + 1, BinaryOperator.EQUAL, true));
                } else {
                    // n <= v and v <= m
                    Expression p1;
                    Expression p2;
                    p1 = createVarPred(var, indices.get(i), BinaryOperator.LESS_EQUAL, false);
                    p2 = createVarPred(var, indices.get(i) + counts.get(i) - 1, BinaryOperator.LESS_EQUAL, true);
                    rslts.add(CifValueUtils.createConjunction(list(p1, p2)));
                }
            }
            Expression rslt = CifValueUtils.createDisjunction(rslts);
            if (!chosenValue) {
                rslt = CifValueUtils.makeInverse(rslt);
            }
            return rslt;
        } else if (type == null || type instanceof EnumType) {
            // Count '0' and '1' bits.
            boolean[] bits = possibles;
            int count0 = 0;
            int count1 = 0;
            for (int i = 0; i < bits.length; i++) {
                if (bits[i]) {
                    count1++;
                } else {
                    count0++;
                }
            }

            // Create predicates for the cheapest variant. That is:
            // - 'x = v1 or x = v2 or ... or x = vn' for '1' bits.
            // - 'x != v1 and x != vn and ... and x != vn' for '0' bits.
            boolean chosenValue = (count1 <= count0) ? true : false;
            BinaryOperator op = chosenValue ? BinaryOperator.EQUAL : BinaryOperator.UNEQUAL;
            List<Expression> rslts = list();
            for (int i = 0; i < bits.length; i++) {
                if (!bits[i] == chosenValue) {
                    continue;
                }

                // v op lit
                rslts.add(createVarPred(var, i, op, true));
            }
            return chosenValue ? CifValueUtils.createDisjunction(rslts) : CifValueUtils.createConjunction(rslts);
        } else {
            throw new RuntimeException("Unexpected var type: " + type);
        }
    }

    /**
     * Creates an 'x op n' CIF predicate for a single synthesis variable. Is optimized for boolean and location pointer
     * variables.
     *
     * @param var The synthesis variable 'x' for which to create the predicate.
     * @param bitIdx The 0-based bit index of the value 'n' of the variable. For a variable with integer range
     *     {@code [-2..5]}, the range for 'n' is {@code [0..7]}.
     * @param op The binary operator 'op' to use. Must be a comparison operator.
     * @param varLeft Whether to put the variable to the left of the operator ({@code true}) or to the right of the
     *     operator ({@code false}).
     * @return The newly created predicate.
     */
    private static Expression createVarPred(SynthesisVariable var, int bitIdx, BinaryOperator op, boolean varLeft) {
        // Optimize for location pointers.
        if (var instanceof SynthesisLocPtrVariable) {
            Assert.check(op == BinaryOperator.EQUAL || op == BinaryOperator.UNEQUAL);

            SynthesisLocPtrVariable lpVar = (SynthesisLocPtrVariable)var;
            Location loc = lpVar.aut.getLocations().get(bitIdx);
            LocationExpression locRef = newLocationExpression();
            locRef.setLocation(loc);
            locRef.setType(newBoolType());

            Expression rslt = locRef;
            if (op == BinaryOperator.UNEQUAL) {
                rslt = CifValueUtils.makeInverse(rslt);
            }
            return rslt;
        }

        // Not a location pointer. Get variable reference expression.
        SynthesisTypedVariable typedVar = (SynthesisTypedVariable)var;
        Expression varRef = createVarRef(typedVar);

        // Optimize for boolean variables.
        if (typedVar.type instanceof BoolType) {
            Assert.check(op == BinaryOperator.EQUAL || op == BinaryOperator.UNEQUAL);
            if (op == BinaryOperator.UNEQUAL) {
                bitIdx = 1 - bitIdx;
            }
            if (bitIdx == 1) {
                return varRef;
            }
            return CifValueUtils.makeInverse(varRef);
        }

        // Get value expression.
        Expression valueExpr;
        if (typedVar.type instanceof IntType) {
            int value = var.lower + bitIdx;
            valueExpr = CifValueUtils.makeInt(value);
        } else if (typedVar.type instanceof EnumType) {
            EnumDecl enumDecl = ((EnumType)typedVar.type).getEnum();
            EnumLiteral lit = enumDecl.getLiterals().get(bitIdx);
            EnumLiteralExpression litRef = newEnumLiteralExpression();
            litRef.setLiteral(lit);
            litRef.setType(deepclone(typedVar.type));
            valueExpr = litRef;
        } else {
            throw new RuntimeException("Unexpected var type: " + typedVar.type);
        }

        // Create and return variable predicate.
        BinaryExpression bin = newBinaryExpression();
        bin.setOperator(op);
        bin.setLeft(varLeft ? varRef : valueExpr);
        bin.setRight(varLeft ? valueExpr : varRef);
        bin.setType(newBoolType());
        return bin;
    }

    /**
     * Creates a CIF reference expression for a given synthesis variable.
     *
     * @param var The synthesis variable.
     * @return A CIF reference expression.
     */
    private static Expression createVarRef(SynthesisVariable var) {
        Assert.check(var instanceof SynthesisTypedVariable);

        if (var instanceof SynthesisDiscVariable) {
            SynthesisDiscVariable discVar = (SynthesisDiscVariable)var;
            DiscVariableExpression discVarRef = newDiscVariableExpression();
            discVarRef.setVariable(discVar.var);
            discVarRef.setType(deepclone(discVar.type));
            return discVarRef;
        } else if (var instanceof SynthesisInputVariable) {
            SynthesisInputVariable inputVar = (SynthesisInputVariable)var;
            InputVariableExpression inputVarRef = newInputVariableExpression();
            inputVarRef.setVariable(inputVar.var);
            inputVarRef.setType(deepclone(inputVar.type));
            return inputVarRef;
        } else {
            throw new RuntimeException("Unknown typed var: " + var);
        }
    }

    /**
     * Returns the size of the expression, as the number of CIF expression nodes.
     *
     * @param expr The CIF expression.
     * @return The size of the expression.
     */
    private static int exprNodeSize(Expression expr) {
        // Operators.
        if (expr instanceof BinaryExpression) {
            BinaryExpression bexpr = (BinaryExpression)expr;
            Expression left = bexpr.getLeft();
            Expression right = bexpr.getRight();
            return 1 + exprNodeSize(left) + exprNodeSize(right);
        } else if (expr instanceof UnaryExpression) {
            return 1 + exprNodeSize(((UnaryExpression)expr).getChild());
        }

        // Literals.
        if (expr instanceof BoolExpression) {
            return 1;
        } else if (expr instanceof IntExpression) {
            return 1;
        }

        // References.
        if (expr instanceof DiscVariableExpression) {
            return 1;
        } else if (expr instanceof InputVariableExpression) {
            return 1;
        } else if (expr instanceof EnumLiteralExpression) {
            return 1;
        } else if (expr instanceof LocationExpression) {
            return 1;
        }

        // Unexpected.
        throw new RuntimeException("Unexpected expr: " + expr);
    }

    /**
     * Creates a CIF predicate for a BDD variable.
     *
     * @param var The synthesis variable.
     * @param idx The 0-based index of the BDD variable into the domain of the synthesis variable, for which to create
     *     the CIF predicate.
     * @return The CIF predicate the BDD variable.
     */
    public static Expression getBddVarPred(SynthesisVariable var, int idx) {
        // Get variable type.
        CifType type = (var instanceof SynthesisTypedVariable) ? ((SynthesisTypedVariable)var).type : null;

        // Get node predicate.
        if (type == null) {
            // Locations matching the bit. Ideally we would cast the automaton
            // to the 0-based integer index of the active location and proceed
            // as we do for integer values, but such a cast is not (yet)
            // available in CIF.
            SynthesisLocPtrVariable synthLpVar = (SynthesisLocPtrVariable)var;
            List<Location> locs = synthLpVar.aut.getLocations();
            List<Expression> locRefs = list();
            int mask = 1 << idx;
            for (int i = 0; i < locs.size(); i++) {
                if ((i & mask) == 0) {
                    continue;
                }

                LocationExpression locRef = newLocationExpression();
                locRef.setLocation(locs.get(i));
                locRef.setType(newBoolType());
                locRefs.add(locRef);
            }
            return CifValueUtils.createDisjunction(locRefs);
        } else if (type instanceof BoolType) {
            // Boolean variable reference 'b'.
            Assert.check(idx == 0);
            return createVarRef(var);
        } else if (type instanceof IntType) {
            // Integer values matching the bit. Ideally we'd use bitwise
            // operations against the mask ((v & mask) > 0), but such
            // operations are not (yet) available in CIF. So, we use
            // (((v div mask) mod 2) > 0) instead.
            Expression varRef = createVarRef(var);

            BinaryExpression divExpr = newBinaryExpression();
            divExpr.setOperator(BinaryOperator.INTEGER_DIVISION);
            divExpr.setLeft(varRef);
            divExpr.setRight(CifValueUtils.makeInt(1 << idx));

            BinaryExpression modExpr = newBinaryExpression();
            modExpr.setOperator(BinaryOperator.MODULUS);
            modExpr.setLeft(divExpr);
            modExpr.setRight(CifValueUtils.makeInt(2));

            BinaryExpression gtExpr = newBinaryExpression();
            gtExpr.setOperator(BinaryOperator.GREATER_THAN);
            gtExpr.setLeft(modExpr);
            gtExpr.setRight(CifValueUtils.makeInt(0));

            return gtExpr;
        } else if (type instanceof EnumType) {
            // Enumeration literals matching the bit. Ideally we would cast the
            // enumeration value to the 0-based integer index of the literal
            // and proceed as we do for integer values, but such a cast is not
            // (yet) available in CIF.
            EnumDecl enumDecl = ((EnumType)type).getEnum();
            List<EnumLiteral> literals = enumDecl.getLiterals();
            List<Expression> valuePreds = list();
            int mask = 1 << idx;
            for (int i = 0; i < literals.size(); i++) {
                if ((i & mask) == 0) {
                    continue;
                }

                Expression varRef = createVarRef(var);

                EnumLiteralExpression litRef = newEnumLiteralExpression();
                litRef.setLiteral(literals.get(i));
                litRef.setType(newEnumType(enumDecl, null));

                BinaryExpression bexpr = newBinaryExpression();
                bexpr.setLeft(varRef);
                bexpr.setRight(litRef);
                bexpr.setOperator(BinaryOperator.EQUAL);
                bexpr.setType(newBoolType());

                valuePreds.add(bexpr);
            }
            return CifValueUtils.createDisjunction(valuePreds);
        } else {
            throw new RuntimeException("Unexpected type: " + type);
        }
    }
}
