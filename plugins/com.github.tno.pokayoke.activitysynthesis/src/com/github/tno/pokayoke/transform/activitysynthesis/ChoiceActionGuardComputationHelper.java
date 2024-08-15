
package com.github.tno.pokayoke.transform.activitysynthesis;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocationExpression;
import static org.eclipse.escet.common.emf.EMFHelper.deepclone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.escet.cif.bdd.spec.CifBddDiscVariable;
import org.eclipse.escet.cif.bdd.spec.CifBddLocPtrVariable;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.bdd.spec.CifBddVariable;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.annotations.AnnotationArgument;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.StringExpression;
import org.eclipse.escet.cif.metamodel.cif.types.BoolType;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.escet.cif.metamodel.cif.types.IntType;

import com.google.common.base.Preconditions;

public class ChoiceActionGuardComputationHelper {
    private ChoiceActionGuardComputationHelper() {
    }

    /**
     * Get locations from a CIF specification.
     *
     * @param spec The CIF specification that contains the locations.
     * @param locationNames The names of the locations.
     * @return The locations from the CIF specification.
     */
    public static List<Location> getLocations(Specification spec, Set<String> locationNames) {
        // Obtain the automaton from the CIF specification.
        List<Automaton> automata = CifCollectUtils.collectAutomata(spec, new ArrayList<>());
        Preconditions.checkArgument(automata.size() == 1,
                "Expected the CIF specification to contain exactly one automaton.");
        Automaton automaton = automata.get(0);

        // Collect the locations from the automaton.
        List<Location> matchedLocations = new ArrayList<>();
        for (String locationName: locationNames) {
            List<Location> locations = automaton.getLocations().stream()
                    .filter(location -> location.getName().equals(locationName)).toList();
            Preconditions.checkArgument(locations.size() == 1,
                    String.format("Expected that there is exactly one location named %s.", locationName));
            matchedLocations.add(locations.get(0));
        }
        return matchedLocations;
    }

    /**
     * Transforms a state annotation into a CIF expression.
     *
     * @param annotation The state annotation to transform.
     * @param cifBddSpec The CIF/BDD specification.
     * @return A CIF expression.
     */
    public static Expression stateAnnotationToCifPred(Annotation annotation, CifBddSpec cifBddSpec) {
        List<Expression> expressions = new ArrayList<>();
        List<CifBddVariable> bddVariables = Arrays.asList(cifBddSpec.variables);

        // Extract an expression from each annotation argument. These expressions are later converted into a single
        // expression capturing the whole annotation.
        for (AnnotationArgument argument: annotation.getArguments()) {
            String variableName = argument.getName();
            Expression expression = argument.getValue();

            // Get from the annotation argument an expression representing the value of the synthesis variable. Skip
            // annotation arguments that do not correspond to synthesis variables, such as for automata with only one
            // location.
            if (isSynthesisVariable(variableName, bddVariables)) {
                List<CifBddVariable> variables = bddVariables.stream()
                        .filter(variable -> variable.rawName.equals(variableName)).toList();
                Preconditions.checkArgument(variables.size() == 1,
                        String.format("Expected that there is exactly one BDD variable named %s.", variableName));
                CifBddVariable variable = variables.get(0);

                if (variable instanceof CifBddLocPtrVariable locVariable) {
                    LocationExpression locationExpression = newLocationExpression();
                    locationExpression.setType(newBoolType());
                    String locationName = ((StringExpression)expression).getValue();
                    List<Location> locations = locVariable.aut.getLocations().stream()
                            .filter(loc -> loc.getName().equals(locationName)).toList();
                    Preconditions.checkArgument(locations.size() == 1,
                            String.format("Expected that there is exactly one location named %s.", locationName));
                    Location location = locations.get(0);
                    locationExpression.setLocation(location);
                    expressions.add(locationExpression);
                } else if (variable instanceof CifBddDiscVariable discVariable) {
                    DiscVariableExpression variableExpression = newDiscVariableExpression();
                    variableExpression.setType(deepclone(discVariable.var.getType()));
                    variableExpression.setVariable(discVariable.var);

                    BinaryExpression binaryExpression = newBinaryExpression();
                    binaryExpression.setType(newBoolType());
                    binaryExpression.setLeft(variableExpression);
                    binaryExpression.setOperator(BinaryOperator.EQUAL);

                    CifType variableType = discVariable.type;

                    if (variableType instanceof EnumType enumType) {
                        String variableValue = ((StringExpression)expression).getValue();
                        List<EnumLiteral> enumLiterals = enumType.getEnum().getLiterals().stream()
                                .filter(literal -> literal.getName().equals(variableValue)).toList();
                        Preconditions.checkArgument(enumLiterals.size() == 1, String.format(
                                "Expected that there is exactly one enummeration literal named %s.", variableValue));

                        EnumLiteral enumliteral = enumLiterals.get(0);

                        binaryExpression.setRight(newEnumLiteralExpression(enumliteral, null, deepclone(variableType)));
                    } else if (variableType instanceof BoolType) {
                        binaryExpression.setRight(deepclone(expression));
                    } else if (variableType instanceof IntType) {
                        binaryExpression.setRight(deepclone(expression));
                    } else {
                        throw new RuntimeException(
                                String.format("Variable type %s is not supported in guard computation.",
                                        variableType.getClass().getName()));
                    }
                    expressions.add(binaryExpression);
                } else {
                    throw new RuntimeException(String.format("Variable %s is not supported in guard computation.",
                            variable.getClass().getName()));
                }
            }
        }

        BinaryExpression expression = (BinaryExpression)CifValueUtils.createConjunction(expressions, true);

        return expression;
    }

    private static boolean isSynthesisVariable(String variableName, List<CifBddVariable> bddVariables) {
        List<String> synthesisVariableNames = bddVariables.stream().map(variable -> variable.rawName).toList();
        return synthesisVariableNames.contains(variableName);
    }
}
