
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.InvKind;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator;
import org.eclipse.escet.cif.metamodel.cif.types.CifType;
import org.eclipse.escet.cif.metamodel.cif.types.IntType;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.common.BiMapUtils;
import com.github.tno.pokayoke.transform.common.ValidationHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

/** Translates UML synthesis specifications to CIF specifications. */
public class UmlToCifTranslator {
    /** The name of the atomicity variable used in translated CIF specifications. */
    public static final String ATOMICITY_VARIABLE_NAME = "__activeAction";

    /** The prefix of a variable used in translated CIF specifications indicating that an action is active. */
    public static final String NONATOMIC_PREFIX = "__nonAtomicActive";

    /** The suffix of an atomic action outcome. */
    public static final String ATOMIC_OUTCOME_SUFFIX = "__result_";

    /** The suffix of a non-atomic action outcome. */
    public static final String NONATOMIC_OUTCOME_SUFFIX = "__na_result_";

    /** The prefix of a CIF variable that encodes (part of) the activity precondition. */
    public static final String PRECONDITION_PREFIX = "__precondition";

    /** The prefix of a CIF variable that encodes (part of) the activity postcondition. */
    public static final String POSTCONDITION_PREFIX = "__postcondition";

    /** The input UML activity to translate. */
    private final Activity activity;

    /** The context that allows querying the UML model of the input UML activity to translate. */
    private final CifContext context;

    /** The translator for UML annotations (guards, updates, invariants, etc.). */
    private final UmlAnnotationsToCif translator;

    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    private final BiMap<Enumeration, EnumDecl> enumMap = HashBiMap.create();

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    private final BiMap<EnumerationLiteral, EnumLiteral> enumLiteralMap = HashBiMap.create();

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    private final BiMap<Property, DiscVariable> variableMap = HashBiMap.create();

    /** The mapping from UML opaque behaviors to corresponding translated CIF (controllable start) events. */
    private final BiMap<OpaqueBehavior, Event> eventMap = HashBiMap.create();

    /** The mapping from non-atomic CIF start events, to their corresponding CIF end events. */
    private final Map<Event, List<Event>> nonAtomicEventMap = new LinkedHashMap<>();

    /** The mapping from non-deterministic CIF start events, to their corresponding CIF end events. */
    private final Map<Event, List<Event>> nonDeterministicEventMap = new LinkedHashMap<>();

    /** The one-to-one mapping from CIF events to CIF edges. */
    private final BiMap<Event, Edge> eventEdgeMap = HashBiMap.create();

    /** The mapping from UML occurrence constraints to corresponding translated CIF requirement automata. */
    private final Map<IntervalConstraint, List<Automaton>> occurrenceConstraintMap = new LinkedHashMap<>();

    public UmlToCifTranslator(Activity activity) {
        this.activity = activity;
        this.context = new CifContext(activity.getModel());
        this.translator = new UmlAnnotationsToCif(context, enumMap, enumLiteralMap, variableMap, eventMap);
    }

    /**
     * Gives the UML activity to translate.
     *
     * @return The UML activity to translate.
     */
    public Activity getActivity() {
        return activity;
    }

    /**
     * Gives all CIF events related to non-atomic actions, as a mapping from non-atomic CIF start events to their
     * corresponding CIF end events.
     *
     * @return A mapping from all non-atomic start events to their corresponding end events.
     */
    public Map<Event, List<Event>> getNonAtomicEvents() {
        return Collections.unmodifiableMap(nonAtomicEventMap);
    }

    /**
     * Gives all CIF events related to non-deterministic actions, as a mapping from their CIF start events to their
     * corresponding CIF end events.
     *
     * @return A mapping from all non-deterministic start events to their corresponding end events.
     */
    public Map<Event, List<Event>> getNonDeterministicEvents() {
        return Collections.unmodifiableMap(nonDeterministicEventMap);
    }

    /**
     * Gives all CIF events related to atomic non-deterministic actions, as a mapping from their CIF start events to
     * their corresponding CIF end events.
     *
     * @return A mapping from all atomic non-deterministic start events to their corresponding end events.
     */
    public Map<Event, List<Event>> getAtomicNonDeterministicEvents() {
        Map<Event, List<Event>> result = new LinkedHashMap<>();

        for (var entry: nonDeterministicEventMap.entrySet()) {
            Event startEvent = entry.getKey();
            List<Event> endEvents = entry.getValue();

            if (!nonAtomicEventMap.containsKey(startEvent)) {
                result.put(startEvent, endEvents);
            }
        }

        return result;
    }

    /**
     * Gives a mapping from non-atomic/non-deterministic end events to their corresponding opaque behaviors and the
     * index of the corresponding effect of the end event.
     *
     * @return The mapping from non-atomic/non-deterministic end events to their corresponding opaque behaviors and the
     *     index of the corresponding effect of the end event.
     */
    public BiMap<Event, Pair<OpaqueBehavior, Integer>> getEndEventMap() {
        BiMap<Event, Pair<OpaqueBehavior, Integer>> result = HashBiMap.create();

        for (var entry: eventMap.entrySet()) {
            OpaqueBehavior action = entry.getKey();
            Event startEvent = entry.getValue();

            // Find all CIF end events for the current action.
            List<Event> endEvents = List.of();
            if (nonAtomicEventMap.containsKey(startEvent)) {
                endEvents = nonAtomicEventMap.get(startEvent);
            } else if (nonDeterministicEventMap.containsKey(startEvent)) {
                endEvents = nonDeterministicEventMap.get(startEvent);
            }

            // Add a map entry for every found end event.
            for (int i = 0; i < endEvents.size(); i++) {
                result.put(endEvents.get(i), Pair.pair(action, i));
            }
        }

        return result;
    }

    /**
     * Gives a mapping from non-atomic/non-deterministic end event names to their corresponding opaque behaviors and the
     * index of the corresponding effect of the end event.
     *
     * @return The mapping from non-atomic/non-deterministic end event names to their corresponding opaque behaviors and
     *     the index of the corresponding effect of the end event.
     */
    public BiMap<String, Pair<OpaqueBehavior, Integer>> getEndEventNameMap() {
        BiMap<Event, Pair<OpaqueBehavior, Integer>> endEventMap = getEndEventMap();

        BiMap<String, Pair<OpaqueBehavior, Integer>> result = HashBiMap.create();

        for (Entry<Event, Pair<OpaqueBehavior, Integer>> entry: endEventMap.entrySet()) {
            result.put(entry.getKey().getName(), entry.getValue());
        }

        return result;
    }

    /**
     * Translates the UML synthesis specifications to a CIF specification.
     *
     * @return The translated CIF specification.
     * @throws CoreException In case the input UML model is invalid.
     */
    public Specification translate() throws CoreException {
        // Validate the UML input model.
        ValidationHelper.validateModel(activity.getModel());

        // Create the CIF specification to which the input UML model will be translated.
        Specification cifSpec = CifConstructors.newSpecification();
        cifSpec.setName("specification");

        // Translate all UML enumerations.
        List<EnumDecl> cifEnums = translateEnumerations();
        cifSpec.getDeclarations().addAll(cifEnums);

        // Translate all UML enumeration literals.
        translateEnumerationLiterals();

        // Create the CIF plant for the UML activity to translate.
        Automaton cifPlant = CifConstructors.newAutomaton();
        cifPlant.setKind(SupKind.PLANT);
        cifPlant.setName(activity.getContext().getName());
        cifSpec.getComponents().add(cifPlant);

        // Translate all UML properties.
        List<DiscVariable> cifPropertyVars = translateProperties();
        cifPlant.getDeclarations().addAll(cifPropertyVars);

        // Create the single location within the CIF plant, which is a flower automaton.
        Location cifLocation = CifConstructors.newLocation();
        cifLocation.getInitials().add(CifValueUtils.makeTrue());
        cifLocation.getMarkeds().add(CifValueUtils.makeTrue());
        cifPlant.getLocations().add(cifLocation);

        // Translate all UML opaque behaviors.
        BiMap<Event, Edge> cifEventEdges = translateOpaqueBehaviors();
        for (var entry: cifEventEdges.entrySet()) {
            cifSpec.getDeclarations().add(entry.getKey());
            cifLocation.getEdges().add(entry.getValue());
        }

        // Encode constraints to ensure that atomic non-deterministic actions are executed atomically.
        DiscVariable cifAtomicityVar = encodeAtomicNonDeterministicActionConstraints();
        if (cifAtomicityVar != null) {
            cifPlant.getDeclarations().add(cifAtomicityVar);
        }

        // Encode constraints to ensure that the start and end events of non-atomic actions are executed in order.
        List<DiscVariable> cifNonAtomicVars = encodeNonAtomicActionConstraints();
        cifPlant.getDeclarations().addAll(cifNonAtomicVars);

        // Translate all occurrence constraints of the input UML activity.
        List<Automaton> cifRequirementAutomata = translateOccurrenceConstraints();
        cifSpec.getComponents().addAll(cifRequirementAutomata);

        // Translate all preconditions of the input UML activity as an initial predicate in CIF.
        Pair<List<AlgVariable>, AlgVariable> preconditions = translatePreconditions();
        cifPlant.getDeclarations().addAll(preconditions.left);
        AlgVariable cifPreconditionVar = preconditions.right;
        cifPlant.getDeclarations().add(cifPreconditionVar);
        cifPlant.getInitials()
                .add(CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), cifPreconditionVar));

        // Translate all postconditions of the input UML activity as a marked predicate in CIF.
        Pair<List<AlgVariable>, AlgVariable> postconditions = translatePostconditions(cifNonAtomicVars,
                cifAtomicityVar);
        cifPlant.getDeclarations().addAll(postconditions.left);
        AlgVariable cifPostconditionVar = postconditions.right;
        cifPlant.getDeclarations().add(cifPostconditionVar);
        cifPlant.getMarkeds().add(
                CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), cifPostconditionVar));

        // Create extra requirements to ensure that, whenever the postcondition holds, no further steps can be taken.
        List<Invariant> cifDisableConstraints = createDisableEventsWhenDoneRequirements(cifPostconditionVar);
        cifSpec.getInvariants().addAll(cifDisableConstraints);

        // Translate all UML class constraints as CIF invariants.
        List<Invariant> cifRequirementInvariants = translateRequirements();
        cifPlant.getInvariants().addAll(cifRequirementInvariants);

        return cifSpec;
    }

    /**
     * Translates all UML enumerations that are in context to CIF enumeration declarations.
     *
     * @return The translated CIF enumeration declarations.
     */
    private List<EnumDecl> translateEnumerations() {
        List<Enumeration> umlEnums = context.getAllEnumerations();
        List<EnumDecl> cifEnums = new ArrayList<>(umlEnums.size());

        for (Enumeration umlEnum: umlEnums) {
            EnumDecl cifEnum = CifConstructors.newEnumDecl(null, null, umlEnum.getName(), null);
            cifEnums.add(cifEnum);
            enumMap.put(umlEnum, cifEnum);
        }

        return cifEnums;
    }

    /**
     * Translates all UML enumeration literals that are in context to CIF enumeration literals.
     *
     * @return The translated CIF enumeration literals.
     */
    private List<EnumLiteral> translateEnumerationLiterals() {
        List<EnumerationLiteral> umlLiterals = context.getAllEnumerationLiterals();
        List<EnumLiteral> cifLiterals = new ArrayList<>(umlLiterals.size());

        for (EnumerationLiteral umlLiteral: umlLiterals) {
            EnumLiteral cifLiteral = CifConstructors.newEnumLiteral(umlLiteral.getName(), null);
            cifLiterals.add(cifLiteral);
            enumMap.get(umlLiteral.getEnumeration()).getLiterals().add(cifLiteral);
            enumLiteralMap.put(umlLiteral, cifLiteral);
        }

        return cifLiterals;
    }

    /**
     * Translates all UML properties that are in context to CIF discrete variables.
     *
     * @return The translated CIF discrete variables.
     */
    private List<DiscVariable> translateProperties() {
        List<Property> umlProperties = context.getAllProperties();
        List<DiscVariable> cifVariables = new ArrayList<>(umlProperties.size());

        for (Property umlProperty: umlProperties) {
            cifVariables.add(translateProperty(umlProperty));
        }

        return cifVariables;
    }

    /**
     * Translates a given UML property to a CIF discrete variable.
     *
     * @param umlProperty The UML property to translate.
     * @return The translated CIF discrete variable.
     */
    private DiscVariable translateProperty(Property umlProperty) {
        DiscVariable cifVariable = CifConstructors.newDiscVariable();
        cifVariable.setName(umlProperty.getName());
        cifVariable.setType(translator.translateType(umlProperty.getType()));
        variableMap.put(umlProperty, cifVariable);

        // Determine the default value(s) of the CIF variable.
        if (PokaYokeUmlProfileUtil.hasDefaultValue(umlProperty)) {
            // Translate the UML default property value.
            ValueSpecification umlDefaultValue = umlProperty.getDefaultValue();
            Expression cifDefaultValueExpr = translator.translate(CifParserHelper.parseExpression(umlDefaultValue));
            cifVariable.setValue(CifConstructors.newVariableValue(null, ImmutableList.of(cifDefaultValueExpr)));
        } else {
            // Indicate that the CIF variable can have any value by default.
            cifVariable.setValue(CifConstructors.newVariableValue());
        }

        return cifVariable;
    }

    /**
     * Translates all UML opaque behaviors that are in context as actions, to CIF events and corresponding CIF edges.
     *
     * @return The translated CIF events with their corresponding CIF edges as a one-to-one mapping.
     */
    private BiMap<Event, Edge> translateOpaqueBehaviors() {
        BiMap<Event, Edge> eventEdges = HashBiMap.create();

        for (OpaqueBehavior umlOpaqueBehavior: context.getAllOpaqueBehaviors()) {
            eventEdges.putAll(translateAction(umlOpaqueBehavior));
        }

        return eventEdges;
    }

    /**
     * Translates a given UML opaque behavior as an action, to CIF events and corresponding CIF edges.
     * <p>
     * If the action to translate is an atomic deterministic action, then a single controllable CIF event is created for
     * starting and ending the action, together with a corresponding edge for that event. The guard of that edge is the
     * translated action guard, and the updates of that edge are the translated action effect.
     * </p>
     * <p>
     * If the action to translate is an non-atomic and/or non-deterministic action, then multiple CIF events are
     * created: one controllable event for starting the action, and uncontrollable events for each of the action
     * effects, for ending the action. At least one end event is always created, even if the action has no defined
     * effects. There is a corresponding edge for every created event. The start edge has the translated action guard as
     * its guard, and has no updates. Any end edge has 'true' as its guard, and the translated action effect as its
     * updates.
     * </p>
     *
     * @param umlAction The UML opaque behavior to translate as an action.
     * @return The translated CIF events with their corresponding CIF edges as a one-to-one mapping.
     */
    private BiMap<Event, Edge> translateAction(OpaqueBehavior umlAction) {
        BiMap<Event, Edge> newEventEdges = HashBiMap.create();

        // Obtain the guard and effects of the current action.
        Expression guard = getGuard(umlAction);
        List<List<Update>> effects = getEffects(umlAction);
        Verify.verify(!effects.isEmpty(), "Expected at least one effect, but found none.");

        // Create a CIF start event for the action that is represented by the current UML action.
        Event cifStartEvent = CifConstructors.newEvent();
        cifStartEvent.setControllable(true);
        cifStartEvent.setName(umlAction.getName());
        eventMap.put(umlAction, cifStartEvent);

        // Create a CIF edge for this start event.
        EventExpression cifEventExpr = CifConstructors.newEventExpression();
        cifEventExpr.setEvent(cifStartEvent);
        cifEventExpr.setType(CifConstructors.newBoolType());
        EdgeEvent cifEdgeEvent = CifConstructors.newEdgeEvent();
        cifEdgeEvent.setEvent(cifEventExpr);
        Edge cifStartEdge = CifConstructors.newEdge();
        cifStartEdge.getEvents().add(cifEdgeEvent);
        cifStartEdge.getGuards().add(guard);
        newEventEdges.put(cifStartEvent, cifStartEdge);

        // Create any CIF end events and corresponding end edges.
        boolean isAtomic = PokaYokeUmlProfileUtil.isAtomic(umlAction);
        boolean isDeterministic = PokaYokeUmlProfileUtil.isDeterministic(umlAction);

        if (isAtomic && isDeterministic) {
            // In case the action is both deterministic and atomic, then the start event also ends the action.
            // Add its effect as an edge update.
            cifStartEdge.getUpdates().addAll(effects.get(0));
        } else {
            // In all other cases, add uncontrollable events and edges to end the action.
            List<Event> cifEndEvents = new ArrayList<>(effects.size());

            // Make an uncontrollable event and corresponding edge for every effect (there is at least one).
            for (int i = 0; i < effects.size(); i++) {
                // Declare the CIF uncontrollable end event.
                Event cifEndEvent = CifConstructors.newEvent();
                cifEndEvent.setControllable(false);
                String outcomeSuffix = isAtomic ? UmlToCifTranslator.ATOMIC_OUTCOME_SUFFIX
                        : UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX;
                cifEndEvent.setName(umlAction.getName() + outcomeSuffix + (i + 1));
                cifEndEvents.add(cifEndEvent);

                // Make the CIF edge for the uncontrollable end event.
                EventExpression cifEndEventExpr = CifConstructors.newEventExpression();
                cifEndEventExpr.setEvent(cifEndEvent);
                cifEndEventExpr.setType(CifConstructors.newBoolType());
                EdgeEvent cifEdgeEndEvent = CifConstructors.newEdgeEvent();
                cifEdgeEndEvent.setEvent(cifEndEventExpr);
                Edge cifEndEdge = CifConstructors.newEdge();
                cifEndEdge.getEvents().add(cifEdgeEndEvent);
                cifEndEdge.getUpdates().addAll(effects.get(i));
                newEventEdges.put(cifEndEvent, cifEndEdge);
            }

            // Remember which start and end events belong together.
            if (!isAtomic) {
                nonAtomicEventMap.put(cifStartEvent, cifEndEvents);
            }
            if (!isDeterministic) {
                nonDeterministicEventMap.put(cifStartEvent, cifEndEvents);
            }
        }

        eventEdgeMap.putAll(newEventEdges);

        return newEventEdges;
    }

    /**
     * Gives the guard of the given element.
     *
     * @param element The element.
     * @return The guard of the given element.
     */
    public Expression getGuard(RedefinableElement element) {
        AExpression guard = CifParserHelper.parseGuard(element);
        if (guard == null) {
            return CifValueUtils.makeTrue();
        }
        return translator.translate(guard);
    }

    /**
     * Gives the original guard corresponding to the given CIF event, where original means: as specified in the UML
     * model (e.g., without atomicity variables and other auxiliary constructs that the transformation may have added).
     *
     * @param event The CIF event, which must have been translated for some opaque behavior in the input UML model.
     * @return The original guard corresponding to the given CIF event.
     */
    public Expression getGuard(Event event) {
        Map<Event, OpaqueBehavior> inverseEventMap = BiMapUtils.orderPreservingInverse(eventMap);
        Preconditions.checkArgument(inverseEventMap.containsKey(event),
                "Expected a CIF event that has been translated for some opaque behavior in the input UML model.");
        return getGuard(inverseEventMap.get(event));
    }

    /**
     * Gives all effects of the given UML action. Every effect consists of a list of updates.
     *
     * @param action The UML action.
     * @return All effects of the given UML action.
     */
    private List<List<Update>> getEffects(RedefinableElement action) {
        List<List<Update>> effects = CifParserHelper.parseEffects(action).stream().map(translator::translate).toList();

        if (effects.isEmpty()) {
            effects = List.of(List.of());
        }

        return effects;
    }

    /**
     * Encodes constraints to ensure that atomic non-deterministic actions are indeed atomically executed. The
     * constraints are encoded as extra edge guards and updates, which are expressed over an <i>atomicity variable</i>
     * that indicates whether an atomic non-deterministic action is being executed, and if so, which one. If there are
     * no atomic non-determinstic actions, then no atomicity variable and no extra edge guards and updates are created.
     *
     * @return The created atomicity variable, or {@code null} in case there were no atomic non-determinstic actions.
     */
    private DiscVariable encodeAtomicNonDeterministicActionConstraints() {
        DiscVariable cifAtomicityVar = null;

        // Find all the start and end events of atomic non-deterministic actions.
        Map<Event, List<Event>> atomicNonDeterministicEvents = getAtomicNonDeterministicEvents();

        Set<Event> atomicNonDeterministicStartEvents = new LinkedHashSet<>();
        Set<Event> atomicNonDeterministicEndEvents = new LinkedHashSet<>();

        for (Entry<Event, List<Event>> entry: atomicNonDeterministicEvents.entrySet()) {
            atomicNonDeterministicStartEvents.add(entry.getKey());
            atomicNonDeterministicEndEvents.addAll(entry.getValue());
        }

        // If there are atomic non-deterministic actions, then constraints must be added to the corresponding CIF edges.
        if (atomicNonDeterministicStartEvents.size() > 0) {
            // Declare a variable that indicates which atomic non-deterministic action is currently active. The value 0
            // then indicates that no non-deterministic action is currently active.
            cifAtomicityVar = CifConstructors.newDiscVariable();
            cifAtomicityVar.setName(ATOMICITY_VARIABLE_NAME);
            cifAtomicityVar.setType(CifConstructors.newIntType(0, null, atomicNonDeterministicStartEvents.size()));

            // Define a mapping from (start and end) events that are related to atomic non-deterministic actions, to the
            // index of the atomic non-deterministic action, starting with index 1. So all CIF events related to the
            // first such action get index 1, all events related to the second such action get index 2, etc.
            Map<Event, Integer> eventIndex = new LinkedHashMap<>();
            int index = 1;

            for (Event cifStartEvent: atomicNonDeterministicStartEvents) {
                eventIndex.put(cifStartEvent, index);

                for (Event cifEndEvent: nonDeterministicEventMap.get(cifStartEvent)) {
                    eventIndex.put(cifEndEvent, index);
                }

                index++;
            }

            // Add guards and updates to every edge to ensure that atomic actions are indeed atomically executed.
            for (Entry<Event, Edge> entry: eventEdgeMap.entrySet()) {
                Event cifEvent = entry.getKey();
                Edge cifEdge = entry.getValue();

                // Add guard '__activeAction = 0' for every event except the ends of atomic non-deterministic actions.
                if (!atomicNonDeterministicEndEvents.contains(cifEvent)) {
                    BinaryExpression cifGuard = CifConstructors.newBinaryExpression();
                    cifGuard.setLeft(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifGuard.setOperator(BinaryOperator.EQUAL);
                    cifGuard.setRight(CifValueUtils.makeInt(0));
                    cifGuard.setType(CifConstructors.newBoolType());
                    cifEdge.getGuards().add(cifGuard);
                }

                // Add update '__activeAction := i' for every start event of an atomic non-deterministic action.
                if (atomicNonDeterministicStartEvents.contains(cifEvent)) {
                    Assignment cifUpdate = CifConstructors.newAssignment();
                    cifUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifUpdate.setValue(CifValueUtils.makeInt(eventIndex.get(cifEvent)));
                    cifEdge.getUpdates().add(cifUpdate);
                }

                // Add guard '__activeAction = i' for every end event of an atomic non-deterministic action.
                if (atomicNonDeterministicEndEvents.contains(cifEvent)) {
                    BinaryExpression cifGuard = CifConstructors.newBinaryExpression();
                    cifGuard.setLeft(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifGuard.setOperator(BinaryOperator.EQUAL);
                    cifGuard.setRight(CifValueUtils.makeInt(eventIndex.get(cifEvent)));
                    cifGuard.setType(CifConstructors.newBoolType());
                    cifEdge.getGuards().add(cifGuard);
                }

                // Add update '__activeAction := 0' for every end event of an atomic non-deterministic action.
                if (atomicNonDeterministicEndEvents.contains(cifEvent)) {
                    Assignment cifUpdate = CifConstructors.newAssignment();
                    cifUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifUpdate.setValue(CifValueUtils.makeInt(0));
                    cifEdge.getUpdates().add(cifUpdate);
                }
            }
        }

        return cifAtomicityVar;
    }

    /**
     * Encodes constraints to ensure that the start and end events of non-atomic actions are executed in order. For
     * every non-atomic action, an <i>active variable</i> is created in CIF that indicates whether the non-atomic action
     * is active, i.e., is being executed. Then the constraints are encoded as extra guards and updates to the edges of
     * non-atomic actions, ensuring that start events can only be performed when the non-atomic action is inactive, and
     * the end events can only be performed when the action is active.
     *
     * @return The created active variables.
     */
    private List<DiscVariable> encodeNonAtomicActionConstraints() {
        // Add guards and updates to the edges of non-atomic actions to keep track of which such actions are active, and
        // to constrain their start and end events accordingly.
        List<DiscVariable> cifNonAtomicVars = new ArrayList<>(nonAtomicEventMap.size());

        for (Entry<Event, List<Event>> entry: nonAtomicEventMap.entrySet()) {
            Event cifStartEvent = entry.getKey();
            List<Event> cifEndEvents = entry.getValue();

            // Declare a Boolean variable that indicates whether the non-atomic action is currently active.
            // Value 'false' indicates inactive, and 'true' indicates active.
            DiscVariable cifNonAtomicVar = CifConstructors.newDiscVariable();
            cifNonAtomicVars.add(cifNonAtomicVar);
            cifNonAtomicVar.setName(NONATOMIC_PREFIX + "__" + cifStartEvent.getName());
            cifNonAtomicVar.setType(CifConstructors.newBoolType());

            // Add guard 'not __nonAtomicActive__{startEvent}' for the start event of this non-atomic action.
            UnaryExpression cifStartGuard = CifConstructors.newUnaryExpression();
            cifStartGuard.setChild(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(cifNonAtomicVar.getType()), cifNonAtomicVar));
            cifStartGuard.setOperator(UnaryOperator.INVERSE);
            cifStartGuard.setType(CifConstructors.newBoolType());
            eventEdgeMap.get(cifStartEvent).getGuards().add(cifStartGuard);

            // Add update '__nonAtomicActive__{startEvent} := true' for the start event of this non-atomic action.
            Assignment cifStartUpdate = CifConstructors.newAssignment();
            cifStartUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(cifNonAtomicVar.getType()), cifNonAtomicVar));
            cifStartUpdate.setValue(CifValueUtils.makeTrue());
            eventEdgeMap.get(cifStartEvent).getUpdates().add(cifStartUpdate);

            for (Event cifEndEvent: cifEndEvents) {
                // Add guard '__nonAtomicActive__{startEvent}' for every end event of this non-atomic action.
                DiscVariableExpression cifEndGuard = CifConstructors.newDiscVariableExpression(null,
                        EcoreUtil.copy(cifNonAtomicVar.getType()), cifNonAtomicVar);
                eventEdgeMap.get(cifEndEvent).getGuards().add(cifEndGuard);

                // Add update '__nonAtomicActive__{startEvent} := false' for every end event of this non-atomic action.
                Assignment cifEndUpdate = CifConstructors.newAssignment();
                cifEndUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                        EcoreUtil.copy(cifNonAtomicVar.getType()), cifNonAtomicVar));
                cifEndUpdate.setValue(CifValueUtils.makeFalse());
                eventEdgeMap.get(cifEndEvent).getUpdates().add(cifEndUpdate);
            }
        }

        return cifNonAtomicVars;
    }

    /**
     * Translates all occurrence constraints of the input UML activity, to CIF requirement automata.
     *
     * @return The translated CIF requirement automata.
     */
    private List<Automaton> translateOccurrenceConstraints() {
        List<Automaton> cifAutomata = new ArrayList<>();

        for (Constraint umlConstraint: activity.getOwnedRules()) {
            if (umlConstraint instanceof IntervalConstraint umlIntervalConstraint) {
                cifAutomata.addAll(translateOccurrenceConstraint(umlIntervalConstraint));
            }
        }

        return cifAutomata;
    }

    /**
     * Translates a given occurrence constraint to CIF requirement automata. This translation could result in multiple
     * automata in case the occurrence constraint constraints more than one UML element.
     *
     * @param umlConstraint The occurrence constraint to translate.
     * @return The translated CIF requirement automata.
     */
    private List<Automaton> translateOccurrenceConstraint(IntervalConstraint umlConstraint) {
        ValueSpecification umlConstraintValue = umlConstraint.getSpecification();

        if (umlConstraintValue instanceof Interval umlInterval) {
            Preconditions.checkArgument(umlInterval.getMin() instanceof LiteralInteger, "Invalid min specification.");
            Preconditions.checkArgument(umlInterval.getMax() instanceof LiteralInteger, "Invalid max specification.");

            int min = ((LiteralInteger)umlInterval.getMin()).getValue();
            int max = ((LiteralInteger)umlInterval.getMax()).getValue();

            List<Automaton> cifAutomata = new ArrayList<>();

            for (Element umlElement: umlConstraint.getConstrainedElements()) {
                if (umlElement instanceof OpaqueBehavior umlOpaqueBehavior) {
                    String name = umlConstraint.getName() + "__" + umlOpaqueBehavior.getName();
                    cifAutomata.add(createIntervalAutomaton(name, eventMap.get(umlOpaqueBehavior), min, max));
                }
            }

            occurrenceConstraintMap.put(umlConstraint, cifAutomata);

            return cifAutomata;
        } else {
            throw new RuntimeException("Unsupported value specification: " + umlConstraintValue);
        }
    }

    /**
     * Creates a CIF requirement automaton expressing that the number of occurrences of the given event must stay within
     * a specified interval.
     *
     * @param name The name of the CIF requirement automaton.
     * @param event The event to express the requirement over.
     * @param min The minimum number of event occurrences.
     * @param max The maximum number of event occurrences.
     * @return The CIF requirement automaton.
     */
    private Automaton createIntervalAutomaton(String name, Event event, int min, int max) {
        Preconditions.checkArgument(0 <= min, "Expected the min value to be at least 0.");
        Preconditions.checkArgument(min <= max, "Expected the max value to be at least the min value.");

        // Create the requirement automaton.
        Automaton automaton = CifConstructors.newAutomaton();
        automaton.setKind(SupKind.REQUIREMENT);
        automaton.setName(name);

        // Create the discrete variable that tracks the number of occurrences of the specified event. We ensure that the
        // upper bound of the range integer type is at least 1, since otherwise the type might become 'int[0..0]', which
        // would make the CIF specification invalid in combination with the CIF assignment on the edge update generated
        // below. The marked predicate of this automaton, together with the edge guard, ensure that the occurrence
        // constraint is properly encoded.
        DiscVariable variable = CifConstructors.newDiscVariable();
        CifType variableType = CifConstructors.newIntType(0, null, Math.max(max, 1));
        variable.setName("__occurrences");
        variable.setType(variableType);
        automaton.getDeclarations().add(variable);

        // Define the marked predicate for the automaton.
        Expression varExpr = CifConstructors.newDiscVariableExpression(null, EcoreUtil.copy(variableType), variable);
        BinaryExpression markedExpr = CifConstructors.newBinaryExpression();
        markedExpr.setLeft(varExpr);
        markedExpr.setOperator(BinaryOperator.GREATER_EQUAL);
        markedExpr.setRight(CifValueUtils.makeInt(min));
        markedExpr.setType(CifConstructors.newBoolType());
        automaton.getMarkeds().add(markedExpr);

        // Create the single location of the automaton.
        Location location = CifConstructors.newLocation();
        location.getInitials().add(CifValueUtils.makeTrue());
        location.getMarkeds().add(CifValueUtils.makeTrue());
        automaton.getLocations().add(location);

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
        edgeGuard.setRight(CifValueUtils.makeInt(max));
        edgeGuard.setType(CifConstructors.newBoolType());
        edge.getGuards().add(edgeGuard);

        // Define the edge update.
        Assignment update = CifConstructors.newAssignment();
        update.setAddressable(EcoreUtil.copy(varExpr));
        BinaryExpression updateExpr = CifConstructors.newBinaryExpression();
        updateExpr.setLeft(EcoreUtil.copy(varExpr));
        updateExpr.setOperator(BinaryOperator.ADDITION);
        Expression updateValue = CifValueUtils.makeInt(1);
        updateExpr.setRight(updateValue);
        updateExpr.setType(UmlAnnotationsToCif.typeForBinaryPlus((IntType)updateExpr.getLeft().getType(),
                (IntType)updateExpr.getRight().getType()));
        update.setValue(updateExpr);
        edge.getUpdates().add(update);

        return automaton;
    }

    /**
     * Translates the UML activity preconditions to a CIF algebraic variable.
     *
     * @return A pair consisting of auxiliary CIF algebraic variables that encode parts of the precondition, together
     *     with the CIF algebraic variable that encodes the entire precondition.
     */
    private Pair<List<AlgVariable>, AlgVariable> translatePreconditions() {
        List<AlgVariable> preconditionVars = translatePrePostconditions(activity.getPreconditions());
        AlgVariable preconditionVar = combinePrePostconditionVariables(preconditionVars, PRECONDITION_PREFIX);
        return Pair.pair(preconditionVars, preconditionVar);
    }

    /**
     * Translates a given collection of preconditions or postconditions to a set of CIF algebraic variables, one for
     * every pre/postcondition, whose values are the state invariant predicates of the corresponding pre/postcondition.
     *
     * @param umlConstraints The collection of UML pre/postconditions to translate.
     * @return The translated Boolean-typed CIF algebraic variables.
     */
    private List<AlgVariable> translatePrePostconditions(Collection<Constraint> umlConstraints) {
        // Define an algebraic CIF variable for every UML constraint, whose value is the state invariant predicate.
        List<AlgVariable> cifConstraintVars = new ArrayList<>(umlConstraints.size());

        for (Constraint umlConstraint: umlConstraints) {
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable();
            cifAlgVar.setName(umlConstraint.getName());
            cifAlgVar.setType(CifConstructors.newBoolType());
            cifAlgVar.setValue(translateStateInvariantConstraint(umlConstraint));
            cifConstraintVars.add(cifAlgVar);
        }

        return cifConstraintVars;
    }

    /**
     * Combines a set of Boolean-typed CIF algebraic pre/postcondition variables into a single Boolean-typed CIF
     * algebraic variable, whose value is the conjunction of all the given variables.
     *
     * @param cifAlgVars The collection of Boolean-typed CIF algebraic variables to combine.
     * @param varName The name of the CIF algebraic variable that is the result of the combination.
     * @return The combined CIF algebraic variable.
     */
    private AlgVariable combinePrePostconditionVariables(Collection<AlgVariable> cifAlgVars, String varName) {
        Expression cifCombinedExpr = CifValueUtils.createConjunction(cifAlgVars.stream().map(
                var -> (Expression)CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), var))
                .toList());

        AlgVariable cifAlgVar = CifConstructors.newAlgVariable();
        cifAlgVar.setName(varName);
        cifAlgVar.setType(CifConstructors.newBoolType());
        cifAlgVar.setValue(cifCombinedExpr);

        return cifAlgVar;
    }

    /**
     * Translates the UML activity postconditions to a CIF algebraic variable. Extra postconditions are added expressing
     * that no non-atomic and atomic non-deterministic actions may be active, and that all occurrence constraints must
     * be satisfied.
     *
     * @param cifNonAtomicVars The internal CIF variables created for non-atomic actions.
     * @param cifAtomicityVar The internal CIF variables created for atomic non-deterministic actions.
     * @return A pair consisting of auxiliary CIF algebraic variables that encode parts of the postcondition, together
     *     with the CIF algebraic variable that encodes the entire postcondition.
     */
    private Pair<List<AlgVariable>, AlgVariable> translatePostconditions(List<DiscVariable> cifNonAtomicVars,
            DiscVariable cifAtomicityVar)
    {
        List<AlgVariable> postconditionVars = translatePrePostconditions(activity.getPostconditions());

        // For every translated non-atomic action, define an extra postcondition that expresses that the non-atomic
        // action must not be active.
        for (DiscVariable cifNonAtomicVar: cifNonAtomicVars) {
            // First define the postcondition expression.
            UnaryExpression cifExtraPostcondition = CifConstructors.newUnaryExpression();
            cifExtraPostcondition.setChild(
                    CifConstructors.newDiscVariableExpression(null, CifConstructors.newBoolType(), cifNonAtomicVar));
            cifExtraPostcondition.setOperator(UnaryOperator.INVERSE);
            cifExtraPostcondition.setType(CifConstructors.newBoolType());

            // Then define an extra CIF algebraic variable for the extra postcondition.
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null,
                    POSTCONDITION_PREFIX + cifNonAtomicVar.getName(), null, CifConstructors.newBoolType(),
                    cifExtraPostcondition);
            postconditionVars.add(cifAlgVar);
        }

        // If the atomicity variable has been added, then define an extra postcondition that expresses that no
        // atomic non-deterministic action must be active.
        if (cifAtomicityVar != null) {
            // First define the postcondition expression.
            BinaryExpression cifExtraPostcondition = CifConstructors.newBinaryExpression();
            cifExtraPostcondition.setLeft(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
            cifExtraPostcondition.setOperator(BinaryOperator.EQUAL);
            cifExtraPostcondition.setRight(CifValueUtils.makeInt(0));
            cifExtraPostcondition.setType(CifConstructors.newBoolType());

            // Then define an extra CIF algebraic variable for this extra postcondition.
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null,
                    POSTCONDITION_PREFIX + cifAtomicityVar.getName(), null, CifConstructors.newBoolType(),
                    cifExtraPostcondition);
            postconditionVars.add(cifAlgVar);
        }

        // For every translated occurrence constraint, define an extra postcondition that expresses that the marked
        // predicate of the corresponding CIF requirement automata must hold.
        for (Entry<IntervalConstraint, List<Automaton>> entry: occurrenceConstraintMap.entrySet()) {
            for (Automaton cifRequirement: entry.getValue()) {
                // First define the postcondition expression.
                Expression cifExtraPostcondition = CifValueUtils
                        .createConjunction(List.copyOf(EcoreUtil.copyAll(cifRequirement.getMarkeds())));

                // Then define an extra CIF algebraic variable for this extra postcondition.
                AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null,
                        POSTCONDITION_PREFIX + "__" + cifRequirement.getName(), null, CifConstructors.newBoolType(),
                        cifExtraPostcondition);
                postconditionVars.add(cifAlgVar);
            }
        }

        AlgVariable postconditionVar = combinePrePostconditionVariables(postconditionVars, POSTCONDITION_PREFIX);
        return Pair.pair(postconditionVars, postconditionVar);
    }

    /**
     * Creates CIF state/event exclusion invariant requirements to disable all events whenever the activity
     * postcondition holds.
     *
     * @param cifPostconditionVar The CIF postcondition variable, which must be non-{@code null}.
     * @return The created CIF requirement invariants.
     */
    private List<Invariant> createDisableEventsWhenDoneRequirements(AlgVariable cifPostconditionVar) {
        Preconditions.checkNotNull(cifPostconditionVar, "Expected a non-null postcondition variable.");

        List<Invariant> cifInvariants = new ArrayList<>(eventEdgeMap.size());

        for (Event cifEvent: eventEdgeMap.keySet()) {
            Invariant cifInvariant = CifConstructors.newInvariant();
            cifInvariant.setEvent(CifConstructors.newEventExpression(cifEvent, null, CifConstructors.newBoolType()));
            cifInvariant.setInvKind(InvKind.EVENT_DISABLES);
            cifInvariant.setPredicate(
                    CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), cifPostconditionVar));
            cifInvariant.setSupKind(SupKind.REQUIREMENT);
            cifInvariants.add(cifInvariant);
        }

        return cifInvariants;
    }

    /**
     * Translates all UML class constraints that are in context to CIF requirement invariants.
     *
     * @return The translated CIF requirement invariants.
     */
    private List<Invariant> translateRequirements() {
        List<Invariant> cifInvariants = new ArrayList<>();

        for (Constraint umlConstraint: activity.getContext().getOwnedRules()) {
            cifInvariants.addAll(translateRequirement(umlConstraint));
        }

        return cifInvariants;
    }

    /**
     * Translates a given UML constraint to CIF requirement invariants.
     *
     * @param umlConstraint The UML constraint to translate.
     * @return The translated CIF requirement invariants.
     */
    private List<Invariant> translateRequirement(Constraint umlConstraint) {
        String constraintName = umlConstraint.getName();

        List<Invariant> cifInvariants = translator.translate(CifParserHelper.parseInvariant(umlConstraint));
        Verify.verify(!cifInvariants.isEmpty(), "Expected at least one translated invariant but got none.");

        // Determine the names of the translated CIF invariants, if any name is set.
        if (!Strings.isNullOrEmpty(constraintName)) {
            if (cifInvariants.size() == 1) {
                cifInvariants.get(0).setName(constraintName);
            } else {
                for (int i = 0; i < cifInvariants.size(); i++) {
                    cifInvariants.get(i).setName(constraintName + "__" + (i + 1));
                }
            }
        }

        return cifInvariants;
    }

    /**
     * Translates a UML constraint to a CIF expression, assuming that the UML constraint is a state invariant.
     *
     * @param umlConstraint The UML constraint to translate.
     * @return The translated CIF expression, which is the state invariant predicate.
     */
    public Expression translateStateInvariantConstraint(Constraint umlConstraint) {
        AInvariant cifInvariant = CifParserHelper.parseInvariant(umlConstraint);
        Preconditions.checkArgument(cifInvariant.invKind == null && cifInvariant.events == null,
                "Expected a state invariant.");
        return translator.translate(cifInvariant.predicate);
    }

    /**
     * Translates a collection of UML constraints to a single CIF expression, assuming that all given UML constraints
     * are state invariants.
     *
     * @param umlConstraints The UML constraints to translate.
     * @return The translated CIF expression, which is the conjunction of all state invariant predicates.
     */
    public Expression translateStateInvariantConstraints(Collection<Constraint> umlConstraints) {
        List<Expression> cifConstraints = umlConstraints.stream().map(this::translateStateInvariantConstraint).toList();
        return CifValueUtils.createConjunction(cifConstraints);
    }
}
