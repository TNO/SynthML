
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/** Translates UML models with requirements and constraints to CIF specifications. */
public class UmlToCifTranslator {
    /** The context that allows querying the input model. */
    private final CifContext context;

    /** The translator for CIF annotations. */
    private final CifAnnotationTranslator translator;

    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    private final Map<Enumeration, EnumDecl> enumMap = new LinkedHashMap<>();

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    private final Map<EnumerationLiteral, EnumLiteral> enumLiteralMap = new LinkedHashMap<>();

    /** The mapping from UML opaque behaviors to corresponding translated CIF start events. */
    private final Map<OpaqueBehavior, Event> eventMap = new LinkedHashMap<>();

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    private final Map<Property, DiscVariable> variableMap = new LinkedHashMap<>();

    public UmlToCifTranslator(Model model) {
        this.context = new CifContext(model);
        this.translator = new CifAnnotationTranslator(context, enumMap, enumLiteralMap, eventMap, variableMap);
    }

    /**
     * Translates the UML model to a CIF specification.
     *
     * @return The translated CIF specification.
     */
    public Specification translate() {
        Specification cifSpec = CifConstructors.newSpecification();

        // Translate all enumerations.
        for (Enumeration umlEnum: context.getAllEnumerations()) {
            EnumDecl cifEnum = CifConstructors.newEnumDecl(null, null, umlEnum.getName(), null);
            cifSpec.getDeclarations().add(cifEnum);
            enumMap.put(umlEnum, cifEnum);
        }

        // Translate all enumeration literals.
        for (EnumerationLiteral umlLiteral: context.getAllEnumerationLiterals()) {
            EnumLiteral cifLiteral = CifConstructors.newEnumLiteral(umlLiteral.getName(), null);
            enumMap.get(umlLiteral.getEnumeration()).getLiterals().add(cifLiteral);
            enumLiteralMap.put(umlLiteral, cifLiteral);
        }

        // Translate all classes and their classifier behavior.
        for (Class umlClass: context.getAllClasses()) {
            if (umlClass instanceof Behavior) {
                continue;
            }

            // Translate the current class.
            cifSpec.getComponents().add(translate(umlClass));

            // Translate all postconditions of the classifier behavior of the current class.
            for (Constraint umlPostcondition: umlClass.getClassifierBehavior().getPostconditions()) {
                AInvariant cifInvariant = CifParserHelper.parseInvariant(umlPostcondition);
                Preconditions.checkArgument(cifInvariant.invKind == null && cifInvariant.events == null,
                        "Expected a state invariant.");
                Expression cifPredicate = translator.translate(cifInvariant.predicate);
                cifSpec.getComponents().add(createPostconditionRequirement(umlPostcondition.getName(), cifPredicate));
            }

            // Translate all interval constraints of the classifier behavior of the current class.
            for (Constraint umlConstraint: umlClass.getClassifierBehavior().getOwnedRules()) {
                if (umlConstraint instanceof IntervalConstraint umlIntervalConstraint) {
                    cifSpec.getComponents().addAll(translate(umlIntervalConstraint));
                }
            }
        }

        return cifSpec;
    }

    /**
     * Translates a UML class to a CIF plant automaton.
     *
     * @param umlClass The UML class to translate.
     * @return The translated CIF plant automaton.
     */
    private Automaton translate(Class umlClass) {
        // Create a CIF plant for the UML class.
        Automaton cifPlant = CifConstructors.newAutomaton();
        cifPlant.setKind(SupKind.PLANT);
        cifPlant.setName(umlClass.getName());

        // Translate all UML class properties to CIF discrete variables.
        for (Property umlProperty: umlClass.getOwnedAttributes()) {
            DiscVariable cifVariable = CifConstructors.newDiscVariable();
            cifVariable.setName(umlProperty.getName());
            cifVariable.setType(translator.translateType(umlProperty.getType()));
            cifPlant.getDeclarations().add(cifVariable);
            variableMap.put(umlProperty, cifVariable);

            // Translate the default property value, if set.
            ValueSpecification umlDefaultValue = umlProperty.getDefaultValue();
            if (umlDefaultValue != null) {
                Expression cifDefaultValueExpr = translator.translate(CifParserHelper.parseExpression(umlDefaultValue));
                cifVariable.setValue(CifConstructors.newVariableValue(null, ImmutableList.of(cifDefaultValueExpr)));
            }
        }

        // Create the single location within the CIF plant, which is a flower automaton.
        Location cifLocation = CifConstructors.newLocation();
        cifLocation.getInitials().add(createBoolExpression(true));
        cifLocation.getMarkeds().add(createBoolExpression(true));
        cifPlant.getLocations().add(cifLocation);

        // Translate all opaque behaviors as CIF event declarations and CIF edges.
        // While doing so, maintain the one-to-one relation between events and edges in a mapping.
        // Opaque behaviors that represent nondeterministic actions are translated as multiple CIF start and end events.
        // A second mapping is maintained to keep track of which such start and end events belong together.
        Map<Event, Edge> eventEdgeMap = new LinkedHashMap<>();
        Map<Event, List<Event>> startEndEventMap = new LinkedHashMap<>();

        for (Behavior umlBehavior: umlClass.getOwnedBehaviors()) {
            if (umlBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                List<Expression> guards = getGuards(umlOpaqueBehavior);
                List<List<Update>> effects = getEffects(umlOpaqueBehavior);

                // Create a CIF event for starting the action that is represented by the current opaque behavior.
                // In case the action is deterministic, this event also ends the action.
                Event cifEvent = CifConstructors.newEvent();
                cifEvent.setControllable(true);
                cifEvent.setName(umlOpaqueBehavior.getName());
                cifPlant.getDeclarations().add(cifEvent);
                eventMap.put(umlOpaqueBehavior, cifEvent);

                // Create a CIF edge for this start event.
                EventExpression cifEventExpr = CifConstructors.newEventExpression();
                cifEventExpr.setEvent(cifEvent);
                cifEventExpr.setType(CifConstructors.newBoolType());
                EdgeEvent cifEdgeEvent = CifConstructors.newEdgeEvent();
                cifEdgeEvent.setEvent(cifEventExpr);
                Edge cifEdge = CifConstructors.newEdge();
                cifEdge.getEvents().add(cifEdgeEvent);
                cifEdge.getGuards().addAll(guards);

                if (effects.size() == 1) {
                    cifEdge.getUpdates().addAll(effects.get(0));
                }

                cifLocation.getEdges().add(cifEdge);
                eventEdgeMap.put(cifEvent, cifEdge);

                // In case of a nondeterministic action, also make uncontrollable events and edges to end the action.
                if (effects.size() > 1) {
                    List<Event> cifEndEvents = new ArrayList<>();

                    // Make an uncontrollable event and corresponding edge for every effect.
                    for (int i = 0; i < effects.size(); i++) {
                        // Declare the CIF uncontrollable event.
                        Event cifEndEvent = CifConstructors.newEvent();
                        cifEndEvent.setControllable(false);
                        cifEndEvent.setName(umlOpaqueBehavior.getName() + "_result_" + i);
                        cifPlant.getDeclarations().add(cifEndEvent);
                        cifEndEvents.add(cifEndEvent);

                        // Make the CIF edge for the uncontrollable event.
                        EventExpression cifEndEventExpr = CifConstructors.newEventExpression();
                        cifEndEventExpr.setEvent(cifEndEvent);
                        cifEndEventExpr.setType(CifConstructors.newBoolType());
                        EdgeEvent cifEdgeEndEvent = CifConstructors.newEdgeEvent();
                        cifEdgeEndEvent.setEvent(cifEndEventExpr);
                        Edge cifEndEdge = CifConstructors.newEdge();
                        cifEndEdge.getEvents().add(cifEdgeEndEvent);
                        cifEndEdge.getUpdates().addAll(effects.get(i));
                        cifLocation.getEdges().add(cifEndEdge);
                        eventEdgeMap.put(cifEndEvent, cifEndEdge);
                    }

                    // Remember which start and end events belong together.
                    startEndEventMap.put(cifEvent, cifEndEvents);
                }
            }
        }

        // In case nondeterministic actions were encountered, encode necessary atomicity constraints.
        if (!startEndEventMap.isEmpty()) {
            // Declare a variable that indicates which nondeterministic action is currently active / being executed.
            // The value 0 then indicates that no nondeterministic action is currently active.
            DiscVariable cifAtomicityVar = CifConstructors.newDiscVariable();
            cifAtomicityVar.setName("__activeAction");
            cifAtomicityVar.setType(CifConstructors.newIntType(0, null, startEndEventMap.size()));
            cifPlant.getDeclarations().add(cifAtomicityVar);

            // Define a mapping from events that are related to nondeterministic actions, to the index of the
            // nondeterministic action, starting with index 1. So all CIF events related to the first nondeterministic
            // action get index 1, all events related to the second such action get index 2, etc.
            Map<Event, Integer> eventIndex = new LinkedHashMap<>();
            int index = 1;

            for (Entry<Event, List<Event>> entry: startEndEventMap.entrySet()) {
                Event cifStartEvent = entry.getKey();
                eventIndex.put(cifStartEvent, index);

                for (Event cifEndEvent: entry.getValue()) {
                    eventIndex.put(cifEndEvent, index);
                }

                index++;
            }

            // Add guards and updates to every edge to ensure that actions are atomically executed.
            for (Entry<Event, Edge> entry: eventEdgeMap.entrySet()) {
                Event cifEvent = entry.getKey();
                Edge cifEdge = entry.getValue();

                // The current event, which has been created for the current edge, can be one of three things:
                // 1. The start and end event of a deterministic action.
                // 2. The start event of a nondeterministic action.
                // 3. The end event of a nondeterministic action.

                // Do a case distinction, and add appropriate CIF edge guards and updates for the case that applies.
                if (cifEvent.getControllable()) {
                    // Case 1 or 2 applies. Either way, add a guard expressing that no other action must be active.
                    BinaryExpression cifGuard = CifConstructors.newBinaryExpression();
                    cifGuard.setLeft(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifGuard.setOperator(BinaryOperator.EQUAL);
                    cifGuard.setRight(
                            CifConstructors.newIntExpression(null, EcoreUtil.copy(cifAtomicityVar.getType()), 0));
                    cifGuard.setType(CifConstructors.newBoolType());
                    cifEdge.getGuards().add(cifGuard);

                    // If case 1 applies, no further work is needed. Otherwise, the atomicity variable must be updated.
                    if (startEndEventMap.containsKey(cifEvent)) {
                        // Case 2 applies. Add an update to indicate that the corresponding action is now active.
                        Assignment cifUpdate = CifConstructors.newAssignment();
                        cifUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                                EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                        cifUpdate.setValue(CifConstructors.newIntExpression(null,
                                EcoreUtil.copy(cifAtomicityVar.getType()), eventIndex.get(cifEvent)));
                        cifEdge.getUpdates().add(cifUpdate);
                    }
                } else {
                    // Case 3 applies. Add a guard expressing that the corresponding action must have started.
                    BinaryExpression cifGuard = CifConstructors.newBinaryExpression();
                    cifGuard.setLeft(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifGuard.setOperator(BinaryOperator.EQUAL);
                    cifGuard.setRight(CifConstructors.newIntExpression(null, EcoreUtil.copy(cifAtomicityVar.getType()),
                            eventIndex.get(cifEvent)));
                    cifGuard.setType(CifConstructors.newBoolType());
                    cifEdge.getGuards().add(cifGuard);

                    // Add an update to indicate that the corresponding action has completed.
                    Assignment cifUpdate = CifConstructors.newAssignment();
                    cifUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifUpdate.setValue(
                            CifConstructors.newIntExpression(null, EcoreUtil.copy(cifAtomicityVar.getType()), 0));
                    cifEdge.getUpdates().add(cifUpdate);
                }
            }
        }

        // Translate all UML class constraints as CIF invariants.
        for (Constraint umlConstraint: umlClass.getOwnedRules()) {
            cifPlant.getInvariants().addAll(translator.translate(CifParserHelper.parseInvariant(umlConstraint)));
        }

        return cifPlant;
    }

    /**
     * Gives all guards of the given behavior.
     *
     * @param behavior The opaque behavior.
     * @return All guards of the given opaque behavior.
     */
    private List<Expression> getGuards(OpaqueBehavior behavior) {
        return behavior.getBodies().stream().limit(1).map(e -> CifParserHelper.parseExpression(e, behavior))
                .map(translator::translate).toList();
    }

    /**
     * Gives all effects of the given behavior. Every effect consists of a list of updates. If there are multiple
     * effects, then the given opaque behavior represents a nondeterministic action.
     *
     * @param behavior The opaque behavior.
     * @return All effects of the given opaque behavior.
     */
    private List<List<Update>> getEffects(OpaqueBehavior behavior) {
        return behavior.getBodies().stream().skip(1).map(u -> CifParserHelper.parseUpdates(u, behavior))
                .map(translator::translate).toList();
    }

    /**
     * Translates an UML interval constraint to a list of CIF requirement automata. This translation could result in
     * multiple automata in case the interval constraint constraints more than one UML element.
     *
     * @param umlConstraint The UML interval constraint to translate.
     * @return The translated list of CIF automata.
     */
    private List<Automaton> translate(IntervalConstraint umlConstraint) {
        ValueSpecification umlConstraintValue = umlConstraint.getSpecification();

        if (umlConstraintValue instanceof Interval umlInterval) {
            int min = 0;
            Integer max = null;

            if (umlInterval.getMin() instanceof LiteralInteger umlLiteral) {
                min = umlLiteral.getValue();
            }

            if (umlInterval.getMax() instanceof LiteralInteger umlLiteral) {
                max = umlLiteral.getValue();
            }

            List<Automaton> cifAutomata = new ArrayList<>();

            for (Element umlElement: umlConstraint.getConstrainedElements()) {
                if (umlElement instanceof OpaqueBehavior umlOpaqueBehavior) {
                    String name = umlOpaqueBehavior.getName() + "__" + umlConstraint.getName();
                    cifAutomata.add(createIntervalAutomaton(name, eventMap.get(umlOpaqueBehavior), min, max));
                } else {
                    throw new RuntimeException("Unsupported element: " + umlElement);
                }
            }

            return cifAutomata;
        } else {
            throw new RuntimeException("Unsupported value specification: " + umlConstraintValue);
        }
    }

    /**
     * Creates a CIF requirement automaton expressing that the amount of occurrences of the given event must stay within
     * a specified interval.
     *
     * @param name The name of the CIF requirement automaton.
     * @param event The event to express the requirement over.
     * @param min The minimum number of event occurrences.
     * @param max The maximum number of event occurrences. Can be {@code null} to indicate that there is no maximum.
     * @return The CIF requirement automaton.
     */
    private Automaton createIntervalAutomaton(String name, Event event, int min, Integer max) {
        Preconditions.checkArgument(0 <= min, "Expected the min value to be at least 0.");

        if (max != null) {
            Preconditions.checkArgument(min <= max, "Expected the max value to be at least the min value.");
        }

        // Create the requirement automaton.
        Automaton automaton = CifConstructors.newAutomaton();
        automaton.setKind(SupKind.REQUIREMENT);
        automaton.setName(name);

        // Create the discrete variable that tracks the number of occurrences of the specified event.
        DiscVariable variable = CifConstructors.newDiscVariable();
        CifType variableType = CifConstructors.newIntType(0, null, max);
        variable.setName("occurrences");
        variable.setType(variableType);
        automaton.getDeclarations().add(variable);

        // Create the single location of the automaton.
        Location location = CifConstructors.newLocation();
        location.getInitials().add(createBoolExpression(true));
        automaton.getLocations().add(location);

        // Define the marked predicate for the single location.
        Expression varExpr = CifConstructors.newDiscVariableExpression(null, EcoreUtil.copy(variableType), variable);
        BinaryExpression markedExpr = CifConstructors.newBinaryExpression();
        markedExpr.setLeft(varExpr);
        markedExpr.setOperator(BinaryOperator.GREATER_EQUAL);
        markedExpr.setRight(CifConstructors.newIntExpression(null, EcoreUtil.copy(variableType), min));
        markedExpr.setType(CifConstructors.newBoolType());
        location.getMarkeds().add(markedExpr);

        // Create the single edge in the automaton.
        EdgeEvent edgeEvent = CifConstructors.newEdgeEvent();
        edgeEvent.setEvent(CifConstructors.newEventExpression(event, null, CifConstructors.newBoolType()));
        Edge edge = CifConstructors.newEdge();
        edge.getEvents().add(edgeEvent);
        location.getEdges().add(edge);

        // Define the edge guard.
        BinaryExpression edgeGuard = CifConstructors.newBinaryExpression();
        edgeGuard.setLeft(EcoreUtil.copy(varExpr));
        edgeGuard.setOperator(BinaryOperator.LESS_THAN);
        edgeGuard.setRight(CifConstructors.newIntExpression(null, EcoreUtil.copy(variableType), max));
        edge.getGuards().add(edgeGuard);

        // Define the edge update.
        Assignment update = CifConstructors.newAssignment();
        update.setAddressable(EcoreUtil.copy(varExpr));
        BinaryExpression updateExpr = CifConstructors.newBinaryExpression();
        updateExpr.setLeft(EcoreUtil.copy(varExpr));
        updateExpr.setOperator(BinaryOperator.ADDITION);
        updateExpr.setRight(CifConstructors.newIntExpression(null, EcoreUtil.copy(variableType), 1));
        update.setValue(updateExpr);
        edge.getUpdates().add(update);

        return automaton;
    }

    /**
     * Creates a CIF requirement automaton that expresses the activity postcondition.
     *
     * @param name The name of the CIF requirement automaton.
     * @param predicate The predicate describing the activity postcondition.
     * @return The CIF requirement automaton.
     */
    private Automaton createPostconditionRequirement(String name, Expression predicate) {
        // Create the requirement automaton.
        Automaton automaton = CifConstructors.newAutomaton();
        automaton.setKind(SupKind.REQUIREMENT);
        automaton.setName(name);

        // Define the event that indicates that the postcondition is satisfied.
        Event event = CifConstructors.newEvent();
        event.setControllable(true);
        event.setName("c_satisfied");
        automaton.getDeclarations().add(event);

        // Define the two locations of the automaton.
        Location notSatisfied = CifConstructors.newLocation();
        notSatisfied.getInitials().add(createBoolExpression(true));
        notSatisfied.setName("NotSatisfied");
        automaton.getLocations().add(notSatisfied);
        Location satisfied = CifConstructors.newLocation();
        satisfied.setName("Satisfied");
        satisfied.getMarkeds().add(createBoolExpression(true));
        automaton.getLocations().add(satisfied);

        // Define the edge between the two locations.
        EventExpression eventExpr = CifConstructors.newEventExpression(event, null, CifConstructors.newBoolType());
        Edge edge = CifConstructors.newEdge();
        edge.getEvents().add(CifConstructors.newEdgeEvent(eventExpr, null));
        edge.getGuards().add(predicate);
        edge.setTarget(satisfied);
        edge.setUrgent(false);
        notSatisfied.getEdges().add(edge);

        return automaton;
    }

    /**
     * Creates a CIF Boolean expression with the indicated value.
     *
     * @param value The value of the CIF Boolean expression.
     * @return The created CIF Boolean expression.
     */
    private static BoolExpression createBoolExpression(boolean value) {
        return CifConstructors.newBoolExpression(null, CifConstructors.newBoolType(), value);
    }
}
