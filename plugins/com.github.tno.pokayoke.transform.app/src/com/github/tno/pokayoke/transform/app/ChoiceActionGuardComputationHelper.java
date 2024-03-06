
package com.github.tno.pokayoke.transform.app;

import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBinaryExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newBoolType;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newDiscVariableExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newEnumLiteralExpression;
import static org.eclipse.escet.cif.metamodel.java.CifConstructors.newLocationExpression;
import static org.eclipse.escet.common.emf.EMFHelper.deepclone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.escet.cif.bdd.spec.CifBddDiscVariable;
import org.eclipse.escet.cif.bdd.spec.CifBddEdge;
import org.eclipse.escet.cif.bdd.spec.CifBddLocPtrVariable;
import org.eclipse.escet.cif.bdd.spec.CifBddSpec;
import org.eclipse.escet.cif.bdd.spec.CifBddVariable;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.annotations.Annotation;
import org.eclipse.escet.cif.metamodel.cif.annotations.AnnotationArgument;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.LocationExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.StringExpression;
import org.eclipse.escet.cif.metamodel.cif.types.BoolType;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;

import com.github.javabdd.BDD;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import fr.lip6.move.pnml.ptnet.Page;
import fr.lip6.move.pnml.ptnet.PetriNet;
import fr.lip6.move.pnml.ptnet.Place;
import fr.lip6.move.pnml.ptnet.Transition;

public class ChoiceActionGuardComputationHelper {
    private ChoiceActionGuardComputationHelper() {
    }

    /**
     * Collect guards of CIF events from a CIF BDD specification.
     *
     * @param cifBddSpec The CIF specification.
     * @return A map from CIF events to their guards in BDDs.
     */
    public static Map<Event, BDD> collectEventGuards(CifBddSpec cifBddSpec) {
        Map<Event, BDD> guards = new HashMap<>();
        for (Entry<Event, List<CifBddEdge>> entry: cifBddSpec.eventEdges.entrySet()) {
            List<CifBddEdge> cifBDDEdges = entry.getValue();
            Preconditions.checkArgument(cifBDDEdges.size() == 1,
                    "Expected that each event has exactly one CIF BDD edge.");
            BDD bdd = cifBDDEdges.get(0).guard.id();
            guards.put(entry.getKey(), bdd);
        }
        return guards;
    }

    /**
     * Get CIF events that corresponds to the transitions from all the choice places in the Petri net.
     *
     * @param petriNet The Petri net.
     * @param allEvents Events from the CIF specification corresponding to the Petri net.
     * @return A map from choice place in Petri net to choice events in CIF specification.
     */
    public static Map<Place, List<Event>> getChoiceEventsPerChoicePlace(PetriNet petriNet, Set<Event> allEvents) {
        List<Page> pnPages = petriNet.getPages();
        Map<Place, List<Event>> place2Event = new LinkedHashMap<>();
        Preconditions.checkArgument(pnPages.size() == 1, "Expected the Petri Net to have exactly one Petri Net page.");
        Page pnPage = pnPages.get(0);

        List<Place> places = pnPage.getObjects().stream().filter(Place.class::isInstance).map(Place.class::cast)
                .toList();
        List<Place> choicePlaces = places.stream().filter(place -> place.getOutArcs().size() > 1).toList();

        for (Place choicePlace: choicePlaces) {
            List<String> eventNames = choicePlace.getOutArcs().stream().map(arc -> arc.getTarget().getName().getText())
                    .toList();

            List<Event> choiceEvents = new ArrayList<>();
            for (String eventName: eventNames) {
                List<Event> events = allEvents.stream().filter(event -> event.getName().equals(eventName)).toList();
                Preconditions.checkArgument(events.size() == 1,
                        String.format("Expected that there is exactly one event named %s.", eventName));
                choiceEvents.add(events.get(0));
            }

            Verify.verify(!place2Event.values().contains(choiceEvents),
                    "Expected that the choice events are unique to a choice place.");
            place2Event.put(choicePlace, choiceEvents);
        }

        return place2Event;
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
                "Expected the CIF specification to include exactly one automaton.");
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
     * @param cifBddSpec The CIF specification.
     * @return A CIF expression.
     */
    public static Expression stateAnnoToCifPred(Annotation annotation, CifBddSpec cifBddSpec) {
        List<Expression> expressions = new ArrayList<>();
        List<CifBddVariable> bddVariables = Arrays.asList(cifBddSpec.variables);

        // Extract an expression from each argument.
        for (AnnotationArgument argument: annotation.getArguments()) {
            String variableName = argument.getName();
            Expression expression = argument.getValue();

            // Extract the expression from the argument that contains synthesis variable.
            if (isSynthesisVariable(variableName, bddVariables)) {
                List<CifBddVariable> variables = bddVariables.stream()
                        .filter(variable -> variable.rawName.equals(variableName)).toList();
                Preconditions.checkArgument(variables.size() == 1,
                        String.format("Expected that there is exactly one BDD variable named %s", variableName));
                CifBddVariable variable = variables.get(0);

                if (variable instanceof CifBddLocPtrVariable locVariable) {
                    LocationExpression locationExpression = newLocationExpression();
                    locationExpression.setType(newBoolType());
                    String locationName = ((StringExpression)expression).getValue();
                    List<Location> locations = locVariable.aut.getLocations().stream()
                            .filter(loc -> loc.getName().equals(locationName)).toList();
                    Preconditions.checkArgument(locations.size() == 1,
                            String.format("Expected that there is exactly one location named %s", locationName));
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
                                .filter(literal -> CifTextUtils.getAbsName(literal, false).equals(variableValue))
                                .toList();
                        Preconditions.checkArgument(enumLiterals.size() == 1, String.format(
                                "Expected that there is exactly one enummeration literal named %s.", variableValue));

                        EnumLiteral enumliteral = enumLiterals.get(0);

                        binaryExpression.setRight(newEnumLiteralExpression(enumliteral, null, deepclone(variableType)));
                    } else if (variableType instanceof BoolType) {
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

    /**
     * Get the transition that corresponds to a CIF event from a choice place.
     *
     * @param place The choice place.
     * @param event The CIF event.
     * @return The corresponding transition.
     */
    public static Transition getTransition(Place place, Event event) {
        List<Transition> targetTransitions = place.getOutArcs().stream().map(arc -> arc.getTarget())
                .map(Transition.class::cast).toList();
        List<Transition> transitions = targetTransitions.stream()
                .filter(transition -> transition.getName().getText().equals(event.getName())).toList();
        Preconditions.checkArgument(transitions.size() == 1,
                String.format("Expected that there is exactly one transition named %s from the choice %s.",
                        event.getName(), place.getName()));

        return transitions.get(0);
    }
}
