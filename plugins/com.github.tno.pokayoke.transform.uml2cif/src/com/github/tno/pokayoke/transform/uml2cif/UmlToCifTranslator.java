
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/** Translates UML models with requirements and constraints to CIF specifications. */
public class UmlToCifTranslator {
    /** The context that allows querying the input model. */
    private final ExtendedCifContext context;

    /** The translator for CIF annotations. */
    private final CifAnnotationTranslator translator;

    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    private final Map<Enumeration, EnumDecl> enumMap = new LinkedHashMap<>();

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    private final Map<EnumerationLiteral, EnumLiteral> enumLiteralMap = new LinkedHashMap<>();

    /** The mapping from UML opaque behaviors to corresponding translated CIF events. */
    private final Map<OpaqueBehavior, Event> eventMap = new LinkedHashMap<>();

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    private final Map<Property, DiscVariable> variableMap = new LinkedHashMap<>();

    /** A mapping from the start events of nondeterministic actions to their corresponding uncontrollable end events. */
    private final Map<Event, Set<Event>> startEndEventMap = new LinkedHashMap<>();

    public UmlToCifTranslator(Model model) {
        this.context = new ExtendedCifContext(model);
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

            // Determine whether nondeterministic actions were encountered while translating the current UML class.
            // If so, an extra CIF plant must be generated to ensure the atomicity of those actions.
            List<Event> cifClassEvents = umlClass.getOwnedBehaviors().stream().filter(OpaqueBehavior.class::isInstance)
                    .map(OpaqueBehavior.class::cast).map(eventMap::get).toList();

            if (cifClassEvents.stream().anyMatch(startEndEventMap::containsKey)) {
                cifSpec.getComponents().add(createAtomicityPlant(umlClass.getName(), cifClassEvents));
            }

            // Translate all postconditions of the classifier behavior of the current class.
            for (Constraint umlPostcondition: umlClass.getClassifierBehavior().getPostconditions()) {
                Expression cifPredicate = translator.translate(CifParserHelper.parseExpression(umlPostcondition));
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

        // Translate all opaque behaviors in the current class.
        for (Behavior umlBehavior: umlClass.getOwnedBehaviors()) {
            if (umlBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                List<String> bodies = umlOpaqueBehavior.getBodies();

                // Translate all opaque behavior guards and effects. Every effect consists of a list of updates.
                // If there are multiple effects, then the opaque behavior represents a nondeterministic action.
                List<Expression> guards = bodies.stream().limit(1)
                        .map(e -> CifParserHelper.parseExpression(e, umlBehavior)).map(translator::translate).toList();
                List<List<Update>> effects = bodies.stream().skip(1)
                        .map(u -> CifParserHelper.parseUpdates(u, umlBehavior)).map(translator::translate).toList();

                // Make a CIF event declaration the current UML opaque behavior.
                Event cifEvent = CifConstructors.newEvent();
                cifEvent.setControllable(true);
                cifEvent.setName(umlOpaqueBehavior.getName());
                cifPlant.getDeclarations().add(cifEvent);
                eventMap.put(umlOpaqueBehavior, cifEvent);

                // Create a CIF edge for the event.
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

                // In case of a nondeterministic action, also make uncontrollable events and edges to end the action.
                if (effects.size() > 1) {
                    Set<Event> cifEndEvents = new LinkedHashSet<>();

                    // Make an uncontrollable event and corresponding edge for every effect.
                    for (int i = 0; i < effects.size(); i++) {
                        // Declare the CIF uncontrollable event.
                        Event cifEndEvent = CifConstructors.newEvent();
                        cifEndEvent.setControllable(false);
                        cifEndEvent.setName(umlOpaqueBehavior.getName() + "__result_" + i);
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
                    }

                    // Remember which start and end events belong together.
                    startEndEventMap.put(cifEvent, cifEndEvents);
                }
            }
        }

        // Translate all UML class constraints as CIF invariants.
        for (Constraint umlConstraint: umlClass.getOwnedRules()) {
            cifPlant.getInvariants().addAll(translator.translate(CifParserHelper.parseInvariant(umlConstraint)));
        }

        return cifPlant;
    }

    // TODO relocate
    private Automaton createAtomicityPlant(String umlClassName, List<Event> events) {
        // Create the CIF plant.
        Automaton plant = CifConstructors.newAutomaton();
        plant.setKind(SupKind.PLANT);
        plant.setName(umlClassName + "__atomicity");

        // Create the idle location.
        Location idleLocation = CifConstructors.newLocation();
        idleLocation.setName("Idle");
        idleLocation.getInitials().add(createBoolExpression(true));
        idleLocation.getMarkeds().add(createBoolExpression(true));
        plant.getLocations().add(idleLocation);

        // For every event define an edge in the idle location, and make 'busy locations' for nondeterministic actions.
        for (Event event: events) {
            Location busyLocation = null;

            // Check whether the current event has end events. If not, create a busy location for it.
            Set<Event> endEvents = startEndEventMap.get(event);

            if (endEvents != null) {
                busyLocation = CifConstructors.newLocation();
                busyLocation.setName("Busy__" + event.getName());
                plant.getLocations().add(busyLocation);

                // Define the edges from this busy location back to the idle location.
                for (Event endEvent: endEvents) {
                    EventExpression endEventExpr = CifConstructors.newEventExpression();
                    endEventExpr.setEvent(endEvent);
                    endEventExpr.setType(CifConstructors.newBoolType());
                    EdgeEvent endEdgeEvent = CifConstructors.newEdgeEvent();
                    endEdgeEvent.setEvent(endEventExpr);
                    Edge endEdge = CifConstructors.newEdge();
                    endEdge.getEvents().add(endEdgeEvent);
                    endEdge.setTarget(idleLocation);
                    busyLocation.getEdges().add(endEdge);
                }
            }

            // Define an edge for the current event in the idle location.
            EventExpression eventExpr = CifConstructors.newEventExpression();
            eventExpr.setEvent(event);
            eventExpr.setType(CifConstructors.newBoolType());
            EdgeEvent edgeEvent = CifConstructors.newEdgeEvent();
            edgeEvent.setEvent(eventExpr);
            Edge edge = CifConstructors.newEdge();
            edge.getEvents().add(edgeEvent);
            edge.setTarget(busyLocation);
            idleLocation.getEdges().add(edge);
        }

        return plant;
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
