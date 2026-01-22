/**
 *
 */

package com.github.tno.pokayoke.transform.activitysynthesis;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.escet.cif.bdd.conversion.CifToBddConverter;
import org.eclipse.escet.cif.bdd.conversion.bitvectors.BddBitVector;
import org.eclipse.escet.cif.bdd.spec.CifBddDiscVariable;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.bdd.spec.CifBddVariable;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.datasynth.settings.CifDataSynthesisSettings;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
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
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

/** Restricts the possible initial values of CIF variables according to the activity's preconditions. */
public class InitialValuesRestricter {
    private InitialValuesRestricter() {
    }

    public static void restrict(Specification specification, UmlToCifTranslator translator, Path specPath) {
        // Get placeholder synthesis settings, that we will not really use, for the CIF/BDD converter.
        CifDataSynthesisSettings settings = new CifDataSynthesisSettings();

        // Convert the CIF specification to a CIF/BDD specification.
        CifToBddConverter converter = new CifToBddConverter("Initial predicate");
        converter.preprocess(specification, specPath.toAbsolutePath().toString(), settings.getWarnOutput(),
                settings.getDoPlantsRefReqsWarn(), Termination.NEVER);
        BDDFactory factory = CifToBddConverter.createFactory(settings, new ArrayList<>(), new ArrayList<>());
        CifBddSpec cifBddSpec = converter.convert(specification, settings, factory);

        // Find the CIF variables which can take only a restricted set of initial values given the activity's
        // preconditions.
        Map<DiscVariable, List<Expression>> varsToInitialValues = findReducedInitialValueVariables(cifBddSpec,
                translator, converter, cifBddSpec.initialPlantInv.id());

        // Get CIF plant.
        List<Automaton> cifPlants = specification.getComponents().stream()
                .filter(c -> c instanceof Automaton automaton && automaton.getKind().equals(SupKind.PLANT))
                .map(Automaton.class::cast).toList();
        Verify.verify(cifPlants.size() == 1, "Found more than one plant automaton.");
        Automaton cifPlant = cifPlants.get(0);

        // Set a new default value for all variables found.
        for (Entry<DiscVariable, List<Expression>> entry: varsToInitialValues.entrySet()) {
            DiscVariable cifVariable = entry.getKey();
            List<Expression> values = entry.getValue();

            // Remove old CIF variable from the plant, update the CIF variable with the new default values, and add it
            // back to the plant.
            cifPlant.getDeclarations().remove(cifVariable);
            cifVariable.setValue(CifConstructors.newVariableValue(null, ImmutableList.copyOf(values)));
            cifPlant.getDeclarations().add(cifVariable);
        }
    }

    private static Map<DiscVariable, List<Expression>> findReducedInitialValueVariables(CifBddSpec cifBddSpec,
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

                // Conjunct with initial predicate.
                BDD presentInInitialPred = initialPlantInv.and(varEqualsValue);

                // If the conjunction is not 'false', store the CIF variable.
                if (!presentInInitialPred.isZero()) {
                    varsToValues.computeIfAbsent(cifVariable, k -> new ArrayList<>()).add(value);
                }
            }

            // Remove variables that can take any value.
            if (varsToValues.getOrDefault(cifVariable, new ArrayList<>()).size() == allPossibleValues.size()) {
                varsToValues.remove(cifVariable);
            }
        }

        return varsToValues;
    }

    private static BDD createVarEqualValueBDD(CifBddSpec cifBddSpec, CifToBddConverter converter,
            CifBddDiscVariable var, Expression value)
    {
        // This method is inspired by CifToBddConverter.convertInit.

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

            // The CIF type checker ensures that the type of the value is contained in the type of the
            // discrete variable, and thus always fits in the bit vector representation. There are thus
            // no runtime errors.

            // The representations of the variable and value bit vectors can be different. For instance,
            // a signed variable may be assigned an non-negative (unsigned) value. And for an unsigned
            // variable, the value may be computed in such a way that we get a signed bit vector.
            // Therefore, we ensure the representations are compatible.
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
