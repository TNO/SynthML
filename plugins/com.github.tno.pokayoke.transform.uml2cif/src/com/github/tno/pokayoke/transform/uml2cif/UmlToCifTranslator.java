
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

//TODO JavaDoc
public class UmlToCifTranslator {
    // TODO JavaDoc
    private final ExtendedCifContext context;

    // TODO JavaDoc
    private final CifAnnotationTranslator translator;

    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    private final Map<Enumeration, EnumDecl> enumMap = new LinkedHashMap<>();

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    private final Map<EnumerationLiteral, EnumLiteral> enumLiteralMap = new LinkedHashMap<>();

    /** The mapping from UML opaque behaviors to corresponding translated CIF events. */
    private final Map<OpaqueBehavior, Event> eventMap = new LinkedHashMap<>();

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    private final Map<Property, DiscVariable> variableMap = new LinkedHashMap<>();

    // TODO JavaDoc
    public UmlToCifTranslator(Model model) {
        this.context = new ExtendedCifContext(model);
        this.translator = new CifAnnotationTranslator(context, enumMap, enumLiteralMap, eventMap, variableMap);
    }

    // TODO JavaDoc
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

        // Translate all classes and their classifier behaviors.
        for (Class umlClass: context.getAllClasses()) {
            if (umlClass instanceof Behavior) {
                continue;
            }

            // Translate the current class.
            cifSpec.getComponents().add(translate(umlClass));

            // Translate all postconditions of the classifier behavior of the current class.
            for (Constraint umlPostcondition: umlClass.getClassifierBehavior().getPostconditions()) {
                Expression cifPredicate = translator.translate(CifParserHelper.parseExpression(umlPostcondition));
                cifSpec.getComponents().add(createPostconditionAutomaton(umlPostcondition.getName(), cifPredicate));
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

    // TODO JavaDoc
    private Automaton translate(Class umlClass) {
        // Create a CIF plant for the UML class.
        Automaton cifPlant = CifConstructors.newAutomaton();
        cifPlant.setKind(SupKind.PLANT);
        cifPlant.setName(umlClass.getName());

        // Translate all class properties.
        for (Property umlProperty: umlClass.getOwnedAttributes()) {
            // Make a CIF discrete variable for the current class property.
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

        // Create the single location within the CIF flower automaton plant.
        Location cifLocation = CifConstructors.newLocation();
        cifLocation.getInitials().add(createBoolExpression(true));
        cifLocation.getMarkeds().add(createBoolExpression(true));
        cifPlant.getLocations().add(cifLocation);

        // Translate all opaque behaviors in the current class.
        for (Behavior umlBehavior: umlClass.getOwnedBehaviors()) {
            if (umlBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                List<String> bodies = umlOpaqueBehavior.getBodies();

                // Translate all opaque behavior guards and update clauses.
                // If there are multiple update clauses, the opaque behavior is nondeterministic.
                List<Expression> guards = bodies.stream().limit(1)
                        .map(e -> CifParserHelper.parseExpression(e, umlBehavior)).map(translator::translate).toList();
                List<List<Update>> updateClauses = bodies.stream().skip(1)
                        .map(u -> CifParserHelper.parseUpdates(u, umlBehavior)).map(translator::translate).toList();

                // Ensure there is at least one update clause.
                if (updateClauses.isEmpty()) {
                    updateClauses.add(ImmutableList.of());
                }

                // Make a CIF event declaration the current opaque behavior.
                Event cifEvent = CifConstructors.newEvent();
                boolean isControllable = bodies.size() <= 2;
                cifEvent.setControllable(isControllable);
                cifEvent.setName((isControllable ? "c_" : "u_") + umlOpaqueBehavior.getName());
                cifPlant.getDeclarations().add(cifEvent);
                eventMap.put(umlOpaqueBehavior, cifEvent);

                // Create CIF edges for the current opaque behavior -- one for every update clause.
                for (List<Update> updates: updateClauses) {
                    EventExpression eventExpr = CifConstructors.newEventExpression();
                    eventExpr.setEvent(eventMap.get(umlOpaqueBehavior));
                    eventExpr.setType(CifConstructors.newBoolType());
                    EdgeEvent edgeEvent = CifConstructors.newEdgeEvent();
                    edgeEvent.setEvent(eventExpr);
                    Edge edge = CifConstructors.newEdge();
                    edge.getEvents().add(edgeEvent);
                    edge.getGuards().addAll(EcoreUtil.copyAll(guards));
                    edge.getUpdates().addAll(updates);
                    cifLocation.getEdges().add(edge);
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
     * Translates an UML interval constraint to a list of CIF (requirement) automata.
     *
     * @param constraint The UML interval constraint to translate.
     * @return The translated list of CIF automata.
     */
    private List<Automaton> translate(IntervalConstraint constraint) {
        ValueSpecification constraintValue = constraint.getSpecification();

        if (constraintValue instanceof Interval interval) {
            int min = 0;
            Integer max = null;

            if (interval.getMin() instanceof LiteralInteger literal) {
                min = literal.getValue();
            }

            if (interval.getMax() instanceof LiteralInteger literal) {
                max = literal.getValue();
            }

            List<Automaton> automata = new ArrayList<>();

            for (Element element: constraint.getConstrainedElements()) {
                if (element instanceof OpaqueBehavior behavior) {
                    String name = behavior.getName() + "__" + constraint.getName();
                    automata.add(createIntervalAutomaton(name, eventMap.get(behavior), min, max));
                } else {
                    throw new RuntimeException("Unsupported element: " + element);
                }
            }

            return automata;
        } else {
            throw new RuntimeException("Unsupported value specification: " + constraintValue);
        }
    }

    // TODO JavaDoc
    private Automaton createIntervalAutomaton(String name, Event event, int min, Integer max) {
        Preconditions.checkArgument(0 <= min, "Expected the min value to be at least 0.");

        if (max != null) {
            Preconditions.checkArgument(min <= max, "Expected the max value to be at least the min value.");
        }

        // TODO use variable names that are consistent with rest of file

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

    // TODO JavaDoc
    private Automaton createPostconditionAutomaton(String name, Expression predicate) {
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
