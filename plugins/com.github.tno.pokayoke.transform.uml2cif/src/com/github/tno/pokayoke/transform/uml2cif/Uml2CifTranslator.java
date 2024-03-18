
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
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
import org.eclipse.escet.cif.metamodel.cif.expressions.EnumLiteralExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.EnumType;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.ValueSpecification;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

// TODO order of methods (scan file from top to bottom)

/** Translates annotated UML models to CIF specifications. */
public abstract class Uml2CifTranslator {
    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    protected final Map<Enumeration, EnumDecl> enumMap = new LinkedHashMap<>();

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    protected final Map<EnumerationLiteral, EnumLiteral> enumLiteralMap = new LinkedHashMap<>();

    /** The mapping from UML opaque behaviors to corresponding translated CIF events. */
    protected final Map<OpaqueBehavior, Event> eventMap = new LinkedHashMap<>();

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    protected final Map<Property, DiscVariable> variableMap = new LinkedHashMap<>();

    /**
     * Parses the given string as a CIF expression.
     *
     * @param expr The string to parse.
     * @return The parsed CIF expression.
     */
    public abstract Expression parseExpression(String expr);

    /**
     * Parses the given string as a CIF invariant.
     *
     * @param invariant The string to parse.
     * @return The parsed CIF invariant.
     */
    public abstract Invariant parseInvariant(String invariant);

    /**
     * Parses the given string as a CIF update.
     *
     * @param update The string to parse.
     * @return The parsed CIF update.
     */
    public abstract Update parseUpdate(String update);

    /**
     * Parses the given string as a list of CIF updates.
     *
     * @param updates The string to parse.
     * @return The list of CIF updates.
     */
    public List<Update> parseUpdates(String updates) {
        // TODO I think this could be done more conveniently
        return Arrays.stream(updates.split(",")).map(this::parseUpdate).toList();
    }

    /**
     * Translates an UML model to a CIF specification.
     *
     * @param model The UML model to translate.
     * @return The translated CIF specification.
     */
    public Specification translateModel(Model model) {
        Specification specification = CifConstructors.newSpecification();

        // Translate all UML enumerations to CIF enumerations.
        for (PackageableElement element: model.getPackagedElements()) {
            if (element instanceof Enumeration enumeration) {
                specification.getDeclarations().add(translateEnumeration(enumeration));
            }
        }

        // Translate all UML classes to CIF plants, and while doing so, collect their classifier behaviors.
        List<Behavior> classifierBehaviors = new LinkedList<>();

        for (PackageableElement element: model.getPackagedElements()) {
            if (element instanceof Class umlClass) {
                specification.getComponents().add(translateClass(umlClass));
                classifierBehaviors.add(umlClass.getClassifierBehavior());
            }
        }

        // Translate all postconditions of all collected classifier behaviors.
        for (Behavior behavior: classifierBehaviors) {
            for (Constraint postcondition: behavior.getPostconditions()) {
                Invariant invariant = translateConstraint(postcondition);
                String name = invariant.getName();
                Expression predicate = invariant.getPredicate();
                specification.getComponents().add(createPostconditionAutomaton(name, predicate));
            }
        }

        // Translate all interval constraints of all collected classifier behaviors.
        for (Behavior behavior: classifierBehaviors) {
            for (Constraint constraint: behavior.getOwnedRules()) {
                if (constraint instanceof IntervalConstraint intervalConstraint) {
                    specification.getComponents().addAll(translateIntervalConstraint(intervalConstraint));
                }
            }
        }

        return specification;
    }

    /**
     * Translates an UML enumeration to a CIF enumeration declaration.
     *
     * @param enumeration The UML enumeration to translate.
     * @return The translated CIF enumeration declaration.
     */
    public EnumDecl translateEnumeration(Enumeration enumeration) {
        EnumDecl cifEnumeration = CifConstructors.newEnumDecl(ImmutableList.of(),
                enumeration.getOwnedLiterals().stream().map(this::translateEnumerationLiteral).toList(),
                enumeration.getLabel(), null);
        enumMap.put(enumeration, cifEnumeration);
        return cifEnumeration;
    }

    /**
     * Translates an UML enumeration literal to a CIF enumeration literal.
     *
     * @param literal The UML enumeration literal to translate.
     * @return The translated CIF enumeration literal.
     */
    public EnumLiteral translateEnumerationLiteral(EnumerationLiteral literal) {
        EnumLiteral cifLiteral = CifConstructors.newEnumLiteral(literal.getLabel(), null);
        enumLiteralMap.put(literal, cifLiteral);
        return cifLiteral;
    }

    /**
     * Translates an UML interval constraint to a list of CIF (requirement) automata.
     *
     * @param constraint The UML interval constraint to translate.
     * @return The translated list of CIF automata.
     */
    public List<Automaton> translateIntervalConstraint(IntervalConstraint constraint) {
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

    /**
     * Translates an UML class to a CIF automaton.
     *
     * @param umlClass The UML class to translate.
     * @return The translated CIF automaton.
     */
    public Automaton translateClass(Class umlClass) {
        Automaton automaton = CifConstructors.newAutomaton();
        automaton.setKind(SupKind.PLANT);
        automaton.setName(umlClass.getLabel());

        // Define discrete CIF variable declarations for every UML class property.
        for (Property property: umlClass.getOwnedAttributes()) {
            automaton.getDeclarations().add(translateProperty(property));
        }

        // Define CIF event declarations for every opaque UML class behavior.
        for (Behavior behavior: umlClass.getOwnedBehaviors()) {
            if (behavior instanceof OpaqueBehavior opaqueBehavior) {
                automaton.getDeclarations().add(translateOpaqueBehavior(opaqueBehavior));
            }
        }

        // Create the single location within the plant automaton.
        Location location = CifConstructors.newLocation();
        location.getInitials().add(createBoolExpression(true));
        location.getMarkeds().add(createBoolExpression(true));
        automaton.getLocations().add(location);

        // Create CIF edges for every opaque UML class behavior.
        for (Behavior behavior: umlClass.getOwnedBehaviors()) {
            if (behavior instanceof OpaqueBehavior opaqueBehavior) {
                List<String> bodies = opaqueBehavior.getBodies();

                // Obtain all update clauses for the current opaque behavior.
                // If there are multiple update clauses, the opaque behavior is nondeterministic.
                List<List<Update>> updateClauses = bodies.stream().skip(1).map(this::parseUpdates)
                        .collect(Collectors.toCollection(LinkedList::new));

                // Ensure there is at least one update clause.
                if (updateClauses.isEmpty()) {
                    updateClauses.add(ImmutableList.of());
                }

                // Create a CIF edge for every update clause.
                for (List<Update> updates: updateClauses) {
                    EventExpression eventExpr = CifConstructors.newEventExpression();
                    eventExpr.setEvent(eventMap.get(opaqueBehavior));
                    eventExpr.setType(CifConstructors.newBoolType());
                    EdgeEvent edgeEvent = CifConstructors.newEdgeEvent();
                    edgeEvent.setEvent(eventExpr);
                    Edge edge = CifConstructors.newEdge();
                    edge.getEvents().add(edgeEvent);
                    edge.getGuards().addAll(bodies.stream().limit(1).map(this::parseExpression).toList());
                    edge.getUpdates().addAll(updates);
                    location.getEdges().add(edge);
                }
            }
        }

        // Translate all class constraints as CIF invariants.
        automaton.getInvariants().addAll(umlClass.getOwnedRules().stream().map(this::translateConstraint).toList());

        return automaton;
    }

    /**
     * Translates an UML opaque behavior to a CIF event.
     *
     * @param behavior The UML opaque behavior to translate.
     * @return The translated CIF event.
     */
    public Event translateOpaqueBehavior(OpaqueBehavior behavior) {
        String behaviorName = behavior.getLabel();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(behaviorName), "Expected a non-empty behavior name.");

        // Construct a CIF event depending on whether the UML behavior is deterministic or not.
        Event event = CifConstructors.newEvent();
        eventMap.put(behavior, event);

        if (behavior.getBodies().size() > 2) {
            event.setControllable(false);
            event.setName("u_" + behaviorName);
        } else {
            event.setControllable(true);
            event.setName("c_" + behaviorName);
        }

        return event;
    }

    /**
     * Translates an UML class property to a CIF discrete variable.
     *
     * @param property The UML class property to translate.
     *
     * @return The translated CIF discrete variable.
     */
    public DiscVariable translateProperty(Property property) {
        DiscVariable variable = CifConstructors.newDiscVariable();
        variable.setName(property.getLabel());
        variable.setType(translateType(property.getType()));
        variableMap.put(property, variable);

        // Translate the default property value, if set.
        ValueSpecification defaultValue = property.getDefaultValue();
        if (defaultValue != null) {
            variable.setValue(CifConstructors.newVariableValue(null,
                    ImmutableList.of(translateValueSpecification(defaultValue))));
        }

        return variable;
    }

    /**
     * Translates an UML type to a CIF type.
     *
     * @param type The UML type to translate.
     * @return The translated CIF type.
     */
    public CifType translateType(Type type) {
        if (type instanceof Enumeration enumeration) {
            return translateEnumerationType(enumeration);
        } else if (type instanceof PrimitiveType primitiveType) {
            return translatePrimitiveType(primitiveType);
        } else {
            throw new RuntimeException("Unsupported type: " + type);
        }
    }

    /**
     * Translates an UML enumeration to a CIF enumeration type.
     *
     * @param enumeration The UML enumeration to translate.
     * @return The translated CIF enumeration type.
     */
    public EnumType translateEnumerationType(Enumeration enumeration) {
        return CifConstructors.newEnumType(enumMap.get(enumeration), null);
    }

    /**
     * Translates an UML primitive type to a CIF type.
     *
     * @param type The UML primitive type to translate.
     * @return The translated CIF type.
     */
    public CifType translatePrimitiveType(PrimitiveType type) {
        if (type.getLabel().equals("Boolean")) {
            return CifConstructors.newBoolType();
        } else {
            throw new RuntimeException("Unsupported primitive type: " + type);
        }
    }

    /**
     * Translates an UML value specification to a CIF expression.
     *
     * @param value The UML value specification to translate.
     * @return The translated CIF expression.
     */
    public Expression translateValueSpecification(ValueSpecification value) {
        if (value instanceof InstanceValue instanceValue) {
            return translateInstanceValue(instanceValue);
        } else if (value instanceof LiteralBoolean literal) {
            return translateLiteralBoolean(literal);
        } else if (value instanceof OpaqueExpression expr) {
            return translateOpaqueExpression(expr);
        } else {
            throw new RuntimeException("Unsupported value specification: " + value);
        }
    }

    /**
     * Translates an UML instance value to a CIF enumeration literal expression.
     *
     * @param value The UML instance value to translate.
     * @return The translated CIF enumeration literal expression.
     */
    public EnumLiteralExpression translateInstanceValue(InstanceValue value) {
        return translateInstanceSpecification(value.getInstance());
    }

    /**
     * Translates an UML instance specification to a CIF enumeration literal expression.
     *
     * @param instance The UML instance specification to translate.
     * @return The translated CIF enumeration literal expression.
     */
    public EnumLiteralExpression translateInstanceSpecification(InstanceSpecification instance) {
        if (instance instanceof EnumerationLiteral literal) {
            return CifConstructors.newEnumLiteralExpression(enumLiteralMap.get(literal), null,
                    translateEnumerationType(literal.getEnumeration()));
        } else {
            throw new RuntimeException("Unsupported instance specification: " + instance);
        }
    }

    /**
     * Translates an UML literal Boolean to a CIF expression.
     *
     * @param literal The UML literal Boolean to translate.
     * @return The translated CIF expression.
     */
    public Expression translateLiteralBoolean(LiteralBoolean literal) {
        return createBoolExpression(literal.isValue());
    }

    /**
     * Translates an UML opaque expression to a CIF expression.
     *
     * @param expr The UML opaque expression to translate.
     * @return The translated CIF expression.
     */
    public Expression translateOpaqueExpression(OpaqueExpression expr) {
        Preconditions.checkArgument(expr.getBodies().size() == 1, "Expected exactly one opaque expression body.");
        return parseExpression(expr.getBodies().get(0));
    }

    /**
     * Translates an UML constraint to a CIF invariant.
     *
     * @param constraint The UML constraint to translate.
     * @return The translated CIF invariant.
     */
    public Invariant translateConstraint(Constraint constraint) {
        ValueSpecification constraintValue = constraint.getSpecification();

        if (constraintValue instanceof OpaqueExpression expr) {
            Preconditions.checkArgument(expr.getBodies().size() == 1, "Expected exactly one body.");
            Invariant invariant = parseInvariant(expr.getBodies().get(0));
            invariant.setName(constraint.getName());
            return invariant;
        } else {
            throw new RuntimeException("Unsupported value specification: " + constraintValue);
        }
    }

    /**
     * Creates a CIF Boolean expression with the indicated value.
     *
     * @param value The value of the CIF Boolean expression.
     * @return The created CIF Boolean expression.
     */
    protected BoolExpression createBoolExpression(boolean value) {
        return CifConstructors.newBoolExpression(null, CifConstructors.newBoolType(), value);
    }

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
}
