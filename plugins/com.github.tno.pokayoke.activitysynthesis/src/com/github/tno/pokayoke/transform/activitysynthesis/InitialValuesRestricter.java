////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2026 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.pokayoke.transform.activitysynthesis;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.bitvectors.BddBitVector;
import org.eclipse.escet.cif.bdd.settings.CifBddSettings;
import org.eclipse.escet.cif.bdd.spec.CifBddDiscVariable;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.bdd.spec.CifBddVariable;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.types.BoolType;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.escet.common.java.Termination;

import com.github.javabdd.BDD;
import com.github.javabdd.BDDFactory;
import com.github.tno.pokayoke.transform.uml2cif.UmlToCifTranslator;

/** Restricts the possible initial values of CIF variables according to the activity's preconditions. */
public class InitialValuesRestricter {
    private InitialValuesRestricter() {
    }

    /**
     * Restricts the initial values of CIF discrete variable declarations corresponding to UML properties, based on
     * values they can have according to activity preconditions.
     *
     * @param specification The CIF specification to update.
     * @param translator The UML-to-CIF translator used to determine variable restrictions.
     * @param specPath The path of the CIF specification.
     */
    public static void restrict(Specification specification, UmlToCifTranslator translator, Path specPath) {
        // Get placeholder synthesis settings for the CIF/BDD converter.
        CifBddSettings settings = new CifBddSettings();

        // Convert the CIF specification to a CIF/BDD specification.
        CifToBddConverter converter = new CifToBddConverter("Initial value restricter");
        converter.preprocess(specification, specPath.toAbsolutePath().toString(), settings.getWarnOutput(),
                settings.getDoPlantsRefReqsWarn(), Termination.NEVER);
        BDDFactory factory = CifToBddConverter.createFactory(settings, new ArrayList<>(), new ArrayList<>());
        CifBddSpec cifBddSpec = converter.convert(specification, settings, factory);

        // Find the CIF variables which can take only a restricted set of initial values given the activity's
        // preconditions.
        Map<DiscVariable, List<Expression>> varsToInitialValues = findRestrictedInitialValueVariables(cifBddSpec,
                translator, converter, cifBddSpec.initialPlantInv);

        // Restrict the possible initial values of the to-be-restricted variables.
        for (Entry<DiscVariable, List<Expression>> entry: varsToInitialValues.entrySet()) {
            DiscVariable cifVariable = entry.getKey();
            List<Expression> values = entry.getValue();
            cifVariable.setValue(CifConstructors.newVariableValue(null, values));
        }
    }

    /**
     * Finds the CIF discrete variables corresponding to UML properties, whose initial values are restricted by the
     * activity's precondition, and returns the map from those variables to their admissible initial values. The map
     * includes only the variables whose initial values are to be modified.
     *
     * @param cifBddSpec The CIF/BDD specification.
     * @param translator The UML-to-CIF translator.
     * @param converter The CIF/BDD converter.
     * @param initialPlantInv Combined initialization and state plant invariant predicates of the model. The method does
     *     not free the BDD.
     * @return The map from variables to the admissible initial values.
     */
    private static Map<DiscVariable, List<Expression>> findRestrictedInitialValueVariables(CifBddSpec cifBddSpec,
            UmlToCifTranslator translator, CifToBddConverter converter, BDD initialPlantInv)
    {
        Map<DiscVariable, List<Expression>> varsToValues = new LinkedHashMap<>();
        for (DiscVariable cifVariable: translator.getPropertyMap().values()) {
            List<Expression> allPossibleValues = CifValueUtils.getPossibleValues(cifVariable.getType());

            for (Expression value: allPossibleValues) {
                // Get CIF/BDD variable.
                int varIdx = CifToBddConverter.getDiscVarIdx(cifBddSpec.variables, cifVariable);
                Assert.check(varIdx >= 0);
                CifBddVariable cifBddVar = cifBddSpec.variables[varIdx];
                Assert.check(cifBddVar instanceof CifBddDiscVariable);
                CifBddDiscVariable var = (CifBddDiscVariable)cifBddVar;

                // Create 'var = value' BDD.
                BDD varEqualsValue = createVarEqualValueBDD(cifBddSpec, converter, var, value);

                // Conjunct 'var = value' predicate with initial predicate to see if variable can have this value in any
                // initial state.
                BDD canHaveValueInInitialState = initialPlantInv.and(varEqualsValue);

                // If the conjunction is not 'false', the variable can have the value in the initial state, so we store
                // this value for the CIF variable.
                if (!canHaveValueInInitialState.isZero()) {
                    varsToValues.computeIfAbsent(cifVariable, k -> new ArrayList<>()).add(value);
                }

                // Free the BDDs.
                varEqualsValue.free();
                canHaveValueInInitialState.free();
            }

            // Handle corner cases: the variable has no possible values or it can take all possible values.
            if (varsToValues.get(cifVariable) == null) {
                // If the CIF variable can take no value at all, give it the default value. This value will not be
                // considered anyway, but helps with later processing during the state space generation.
                varsToValues.computeIfAbsent(cifVariable, k -> new ArrayList<>())
                        .add(CifValueUtils.getDefaultValue(cifVariable.getType(), null));
            } else if (varsToValues.get(cifVariable).size() == allPossibleValues.size()) {
                // Remove variables that can take any value.
                varsToValues.remove(cifVariable);
            }
        }

        return varsToValues;
    }

    private static BDD createVarEqualValueBDD(CifBddSpec cifBddSpec, CifToBddConverter converter,
            CifBddDiscVariable var, Expression value)
    {
        // This method is inspired by `CifToBddConverter.convertInit`.

        BDD pred = cifBddSpec.factory.zero();

        // Case distinction on types of values.
        if (var.type instanceof BoolType) {
            // Convert right hand side (value to assign).
            BDD valueBdd = converter.convertPred(value, true, cifBddSpec, null);

            // Create BDD for the left hand side (variable to get a new value).
            Assert.check(var.domain.getVarCount() == 1);
            int varVar = var.domain.getVarIndices()[0];
            BDD varBdd = cifBddSpec.factory.ithVar(varVar);

            // Construct 'var = value' relation.
            BDD relation = varBdd.biimpWith(valueBdd);

            // Update initialization predicate for the variable.
            pred = pred.orWith(relation);
        } else {
            // Get bit vectors for the variable and its initial value.
            BddBitVector<?, ?> varVector = var.createBitVector(false);
            BddBitVector<?, ?> valueVector = converter.convertExpr(value, true, cifBddSpec, null);

            // The representations of the variable and value bit vectors can be different. For instance,
            // a signed variable may be initialized to a non-negative (unsigned) value. Therefore, we ensure the
            // representations are compatible.
            Pair<BddBitVector<?, ?>, BddBitVector<?, ?>> vectors = BddBitVector.ensureCompatible(varVector,
                    valueVector);
            varVector = vectors.left;
            valueVector = vectors.right;

            // Resize the variable and value vectors to have the same length, such that an equality
            // operation can be performed.
            BddBitVector.ensureSameLength(varVector, valueVector);

            // Construct 'var = value' relation.
            BDD relation = varVector.equalToAny(valueVector);

            // Cleanup.
            varVector.free();
            valueVector.free();

            // Update initialization predicate for the variable.
            pred = pred.orWith(relation);
        }

        return pred;
    }
}
