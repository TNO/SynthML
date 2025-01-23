
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
import java.util.stream.Collectors;

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
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.common.IDHelper;
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

    /** The prefix of a CIF event that has been created for a concrete activity node. */
    public static final String NODE_PREFIX = "__node";

    /** The prefix of a CIF variable indicating that a translated UML control flow holds a token. */
    public static final String CONTROLFLOW_PREFIX = "__controlflow";

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

    /** The mapping from translated CIF start events to their corresponding UML elements for which they were created. */
    private final Map<Event, RedefinableElement> startEventMap = new LinkedHashMap<>();

    /** The one-to-one mapping from UML activity edges to their corresponding translated CIF discrete variables. */
    private final BiMap<ActivityEdge, DiscVariable> controlFlowMap = HashBiMap.create();

    /** The mapping from CIF start events of non-atomic actions, to their corresponding CIF end events. */
    private final Map<Event, List<Event>> nonAtomicEventMap = new LinkedHashMap<>();

    /** The mapping from CIF start events of non-deterministic actions, to their corresponding CIF end events. */
    private final Map<Event, List<Event>> nonDeterministicEventMap = new LinkedHashMap<>();

    /** The one-to-one mapping from CIF events to CIF edges. */
    private final BiMap<Event, Edge> eventEdgeMap = HashBiMap.create();

    /** The mapping from UML occurrence constraints to corresponding translated CIF requirement automata. */
    private final Map<IntervalConstraint, List<Automaton>> occurrenceConstraintMap = new LinkedHashMap<>();

    public UmlToCifTranslator(Activity activity) {
        this.activity = activity;
        this.context = new CifContext(activity.getModel());
        this.translator = new UmlAnnotationsToCif(context, enumMap, enumLiteralMap, variableMap, startEventMap);
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
     * Gives a mapping from non-atomic/non-deterministic CIF end events to their corresponding UML elements for which
     * they were created, as well as the index of the corresponding effect of the end event.
     *
     * @return The mapping from non-atomic/non-deterministic CIF end events to their corresponding UML elements and the
     *     index of the corresponding effect of the end event.
     */
    public Map<Event, Pair<RedefinableElement, Integer>> getEndEventMap() {
        Map<Event, Pair<RedefinableElement, Integer>> result = new LinkedHashMap<>();

        for (var entry: startEventMap.entrySet()) {
            Event startEvent = entry.getKey();
            RedefinableElement action = entry.getValue();

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
     * Gives a mapping from non-atomic/non-deterministic CIF end event names to their corresponding UML elements for
     * which they were created, as well as the index of the corresponding effect of the end event.
     *
     * @return The mapping from non-atomic/non-deterministic CIF end event names to their corresponding UML elements and
     *     the index of the corresponding effect of the end event.
     */
    public Map<String, Pair<RedefinableElement, Integer>> getEndEventNameMap() {
        Map<Event, Pair<RedefinableElement, Integer>> endEventMap = getEndEventMap();

        Map<String, Pair<RedefinableElement, Integer>> result = new LinkedHashMap<>();

        for (var entry: endEventMap.entrySet()) {
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

        // Create the single location within the CIF plant, which will become a flower automaton.
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

        // Translate all UML concrete activities.
        Pair<Set<DiscVariable>, BiMap<Event, Edge>> translatedActivities = translateActivities();

        cifPlant.getDeclarations().addAll(translatedActivities.left);

        for (var entry: translatedActivities.right.entrySet()) {
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

        for (OpaqueBehavior umlBehavior: context.getAllOpaqueBehaviors()) {
            ActionTranslationResult translationResult = translateAsAction(umlBehavior, umlBehavior.getName(),
                    PokaYokeUmlProfileUtil.isAtomic(umlBehavior), true);
            eventEdges.putAll(translationResult.eventEdges);
        }

        return eventEdges;
    }

    /**
     * Translates a given UML element as an action, to CIF events and corresponding CIF edges.
     * <p>
     * If the UML element is translated as an atomic deterministic action, then a single CIF event is created for
     * starting and ending the action, together with a corresponding edge for that event. The guard of that edge is the
     * translated action guard, and the updates of that edge are the translated action effect.
     * </p>
     * <p>
     * If the UML element is translated as an non-atomic and/or non-deterministic action, then multiple CIF events are
     * created: one event for starting the action, and uncontrollable events for each of the action effects, for ending
     * the action. At least one end event is always created, even if the action has no defined effects. There is a
     * corresponding edge for every created event. The start edge has the translated action guard as its guard, and has
     * no updates. Any end edge has 'true' as its guard, and the translated action effect as its updates.
     * </p>
     * <p>
     * If the UML element is a {@link PokaYokeUmlProfileUtil#isFormalElement(RedefinableElement) formal element} (e.g.,
     * an opaque behavior, an opaque action node, or a shadowed call behavior node), then its guard and effects are used
     * for translating the action. If the UML element is a call behavior node that calls an opaque behavior, then the
     * guard and effects of that opaque behavior are used. Otherwise, the UML element doesn't have guards nor effects,
     * thus the translated action will have 'true' as its guard, and will have no effects. For example, this holds for
     * {@link ControlNode control nodes}, e.g., initial, final, fork, join, decision, and merge nodes.
     * </p>
     *
     * @param umlElement The UML element to translate as an action.
     * @param name The name of the action to create.
     * @param isAtomic Whether the UML element should be translated as an atomic action.
     * @param controllableStartEvent Whether the created CIF start event should be controllable.
     * @return An action translation result.
     */
    private ActionTranslationResult translateAsAction(RedefinableElement umlElement, String name, boolean isAtomic,
            boolean controllableStartEvent)
    {
        BiMap<Event, Edge> newEventEdges = HashBiMap.create();

        // Find the action to translate. If the UML element is a call behavior action that is not shadowed (i.e., has no
        // guards and effects), then the called action is translated, otherwise the UML element itself is translated.
        RedefinableElement umlAction;

        if (PokaYokeUmlProfileUtil.isFormalElement(umlElement)) {
            umlAction = umlElement;
        } else if (umlElement instanceof CallBehaviorAction cbAction) {
            umlAction = cbAction.getBehavior();
        } else {
            umlAction = umlElement;
        }

        Preconditions.checkArgument(!(umlAction instanceof Activity), "Expected not to find an activity.");

        // Obtain the guard and effects of the current action.
        Expression guard = getGuard(umlAction);
        List<List<Update>> effects = getEffects(umlAction);
        Verify.verify(!effects.isEmpty(), "Expected at least one effect, but found none.");

        // Create a CIF start event for the action.
        Event cifStartEvent = CifConstructors.newEvent();
        cifStartEvent.setControllable(controllableStartEvent);
        cifStartEvent.setName(name);
        startEventMap.put(cifStartEvent, umlElement);

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
        boolean isDeterministic = PokaYokeUmlProfileUtil.isDeterministic(umlAction);

        List<Event> cifEndEvents = new ArrayList<>(effects.size());

        if (isAtomic && isDeterministic) {
            // In case the action is both deterministic and atomic, then the start event also ends the action.
            // Add its effect as an edge update.
            cifStartEdge.getUpdates().addAll(effects.get(0));
        } else {
            // In all other cases, add uncontrollable events and edges to end the action. Make an uncontrollable event
            // and corresponding edge for every effect (there is at least one).
            for (int i = 0; i < effects.size(); i++) {
                // Declare the CIF uncontrollable end event.
                Event cifEndEvent = CifConstructors.newEvent();
                cifEndEvent.setControllable(false);
                String outcomeSuffix = isAtomic ? UmlToCifTranslator.ATOMIC_OUTCOME_SUFFIX
                        : UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX;
                cifEndEvent.setName(name + outcomeSuffix + (i + 1));
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

        return new ActionTranslationResult(cifStartEvent, cifEndEvents, newEventEdges);
    }

    /**
     * The result of translating a UML element as an action.
     *
     * @param startEvent The CIF start event that has been created for the action.
     * @param endEvents The CIF end events that have been created for the action. If the translated action was both
     *     atomic and deterministic, then this list is empty, since then the created start event also ends the action.
     * @param eventEdges The translated CIF events with their corresponding CIF edges as a one-to-one mapping.
     */
    private record ActionTranslationResult(Event startEvent, List<Event> endEvents, BiMap<Event, Edge> eventEdges) {
    }

    /**
     * Translates all concrete UML activities that are in context to CIF variables, and CIF events with their
     * corresponding CIF edges.
     *
     * @return The translated CIF variables, and CIF events with their corresponding CIF edges.
     */
    private Pair<Set<DiscVariable>, BiMap<Event, Edge>> translateActivities() {
        Set<DiscVariable> newVariables = new LinkedHashSet<>();
        BiMap<Event, Edge> newEventEdges = HashBiMap.create();

        // Translate all concrete activities that are in context.
        for (Activity activity: context.getAllConcreteActivities()) {
            Pair<Set<DiscVariable>, BiMap<Event, Edge>> result = translateActivity(activity);
            newVariables.addAll(result.left);
            newEventEdges.putAll(result.right);
        }

        return Pair.pair(newVariables, newEventEdges);
    }

    /**
     * Translates a given UML activity to CIF variables, and CIF events with their corresponding CIF edges.
     *
     * @param activity The UML activity to translate.
     * @return The translated CIF variables, and CIF events with their corresponding CIF edges.
     */
    private Pair<Set<DiscVariable>, BiMap<Event, Edge>> translateActivity(Activity activity) {
        Preconditions.checkArgument(!activity.isAbstract(), "Expected a concrete activity.");

        // Translate all activity control flows.
        Set<DiscVariable> newVariables = new LinkedHashSet<>(activity.getEdges().size());
        for (ActivityEdge controlFlow: activity.getEdges()) {
            newVariables.add(translateActivityControlFlow(controlFlow));
        }

        // Translate all activity nodes.
        BiMap<Event, Edge> newEventEdges = HashBiMap.create(activity.getNodes().size());
        for (ActivityNode node: activity.getNodes()) {
            newEventEdges.putAll(translateActivityNode(node));
        }

        return Pair.pair(newVariables, newEventEdges);
    }

    /**
     * Translates a UML control flow to a CIF variable.
     *
     * @param controlFlow The UML control flow to translate.
     * @return The translated CIF variable.
     */
    private DiscVariable translateActivityControlFlow(ActivityEdge controlFlow) {
        // Create a Boolean CIF variable for the UML control flow.
        DiscVariable cifVariable = CifConstructors.newDiscVariable();
        cifVariable.setName(String.format("%s__%s", CONTROLFLOW_PREFIX, IDHelper.getID(controlFlow)));
        cifVariable.setType(CifConstructors.newBoolType());
        controlFlowMap.put(controlFlow, cifVariable);

        return cifVariable;
    }

    /**
     * Translates a UML activity node to CIF events and corresponding CIF edges.
     *
     * @param node The UML activity node to translate.
     * @return The translated CIF events and corresponding CIF edges as a one-to-one mapping.
     */
    private BiMap<Event, Edge> translateActivityNode(ActivityNode node) {
        // Translate the given activity node as either an AND or OR type node, depending on its type. The CIF start
        // events that will be created for this node should only be controllable in case the node is an initial node.
        BiMap<Event, Edge> newEventEdges;

        if (node instanceof InitialNode) {
            newEventEdges = translateActivityOrNode(node, true, true);
        } else if (node instanceof FinalNode || node instanceof DecisionNode || node instanceof MergeNode) {
            newEventEdges = translateActivityOrNode(node, true, false);
        } else if (node instanceof ForkNode || node instanceof JoinNode) {
            newEventEdges = translateActivityAndNode(node, true, false);
        } else if (node instanceof CallBehaviorAction callNode) {
            if (PokaYokeUmlProfileUtil.isFormalElement(callNode)) {
                newEventEdges = translateActivityAndNode(callNode, PokaYokeUmlProfileUtil.isAtomic(callNode), false);
            } else {
                Behavior behavior = callNode.getBehavior();
                newEventEdges = translateActivityAndNode(callNode, PokaYokeUmlProfileUtil.isAtomic(behavior), false);
            }
        } else if (node instanceof OpaqueAction) {
            newEventEdges = translateActivityAndNode(node, PokaYokeUmlProfileUtil.isAtomic(node), false);
        } else {
            throw new RuntimeException("Unsupported activity node: " + node);
        }

        // If the UML activity node is initial, then add the activity preconditions as extra guards for performing the
        // translated CIF start events for the initial node.
        if (node instanceof InitialNode) {
            for (Entry<Event, Edge> entry: newEventEdges.entrySet()) {
                Event cifEvent = entry.getKey();
                Edge cifEdge = entry.getValue();

                // If the current CIF event is a start event, then add all preconditions to its edge as extra guards.
                if (startEventMap.containsKey(cifEvent)) {
                    for (Constraint precondition: node.getActivity().getPreconditions()) {
                        cifEdge.getGuards().add(translateStateInvariantConstraint(precondition));
                    }
                }
            }
        }

        // If the UML activity node is final, then add the activity postconditions as extra guards for performing the
        // translated CIF start events for the final node.
        if (node instanceof FinalNode) {
            for (Entry<Event, Edge> entry: newEventEdges.entrySet()) {
                Event cifEvent = entry.getKey();
                Edge cifEdge = entry.getValue();

                // If the current CIF event is a start event, then add all postconditions to its edge as extra guards.
                if (startEventMap.containsKey(cifEvent)) {
                    for (Constraint postcondition: node.getActivity().getPostconditions()) {
                        cifEdge.getGuards().add(translateStateInvariantConstraint(postcondition));
                    }
                }
            }
        }

        return newEventEdges;
    }

    /**
     * Translates the given UML activity node as an AND-type node.
     *
     * @param node The UML activity node to translate.
     * @param isAtomic Whether the UML activity node should be translated as an atomic action.
     * @param controllableStartEvents Whether to translate the CIF start events as controllable events.
     * @return The translated CIF events and corresponding CIF edges as a one-to-one mapping.
     */
    private BiMap<Event, Edge> translateActivityAndNode(ActivityNode node, boolean isAtomic,
            boolean controllableStartEvents)
    {
        // Translate the UML activity node as an action.
        String actionName = getActionNameForActivityNode(node);
        ActionTranslationResult translationResult = translateAsAction(node, actionName, isAtomic,
                controllableStartEvents);

        // Collect the CIF start events, end events, and newly created edges of the translated UML activity node.
        Event startEvent = translationResult.startEvent;
        List<Event> endEvents = translationResult.endEvents;
        BiMap<Event, Edge> newEventEdges = translationResult.eventEdges;

        // If no explicit end events were created during the translation, then the start event also ends the action.
        if (endEvents.isEmpty()) {
            endEvents.add(startEvent);
        }

        // For every incoming UML control flow, add appropriate guards and updates to the newly created CIF edges.
        for (ActivityEdge incoming: node.getIncomings()) {
            Edge startEdge = newEventEdges.get(startEvent);
            addGuardsAndUpdatesToIncomingControlFlow(incoming, startEdge);
        }

        // For every outgoing UML control flow, add appropriate guards and updates to the newly created CIF edges.
        for (ActivityEdge outgoing: node.getOutgoings()) {
            List<Edge> endEdges = endEvents.stream().map(newEventEdges::get).toList();
            addGuardsAndUpdatesToOutgoingControlFlow(outgoing, endEdges);
        }

        return newEventEdges;
    }

    /**
     * Translates the given UML activity node as an OR-type node.
     *
     * @param node The UML activity node to translate.
     * @param isAtomic Whether the UML activity node should be translated as an atomic action.
     * @param controllableStartEvents Whether to translate the CIF start events as controllable events.
     * @return The translated CIF events and corresponding CIF edges as a one-to-one mapping.
     */
    private BiMap<Event, Edge> translateActivityOrNode(ActivityNode node, boolean isAtomic,
            boolean controllableStartEvents)
    {
        // Find all combinations of incoming and outgoing UML control flows to translate CIF events/edges for, as pairs.
        // If the activity node has no incoming control flows, then collect all outgoing control flows instead, to
        // translate. And likewise, if there are no outgoing control flows, collect all incoming control flows instead.
        Set<Pair<ActivityEdge, ActivityEdge>> controlFlowPairs = new LinkedHashSet<>();

        if (node.getOutgoings().isEmpty()) {
            for (ActivityEdge incoming: node.getIncomings()) {
                controlFlowPairs.add(Pair.pair(incoming, null));
            }
        } else if (node.getIncomings().isEmpty()) {
            for (ActivityEdge outgoing: node.getOutgoings()) {
                controlFlowPairs.add(Pair.pair(null, outgoing));
            }
        } else {
            for (ActivityEdge incoming: node.getIncomings()) {
                for (ActivityEdge outgoing: node.getOutgoings()) {
                    controlFlowPairs.add(Pair.pair(incoming, outgoing));
                }
            }
        }

        // If there is at most one control flow pair, then we can translate the node as an AND-type node (which is
        // slightly simpler and gives slightly nicer event names), since that's semantically equivalent to translating
        // it as an OR-type node.
        if (controlFlowPairs.size() <= 1) {
            return translateActivityAndNode(node, isAtomic, controllableStartEvents);
        }

        // For every collected pair of control flows, translate the UML activity node.
        BiMap<Event, Edge> result = HashBiMap.create();
        int count = 0;

        for (Pair<ActivityEdge, ActivityEdge> pair: controlFlowPairs) {
            ActivityEdge incoming = pair.left;
            ActivityEdge outgoing = pair.right;

            // Translate the UML activity node for the current control flow pair, as an action.
            String actionName = String.format("%s__%d", getActionNameForActivityNode(node), count);
            ActionTranslationResult translationResult = translateAsAction(node, actionName, isAtomic,
                    controllableStartEvents);
            count++;

            // Collect the CIF start events, end events, and newly created edges of the translated UML activity node.
            Event startEvent = translationResult.startEvent;
            List<Event> endEvents = translationResult.endEvents;
            BiMap<Event, Edge> newEventEdges = translationResult.eventEdges;

            // If no explicit end events were created during the translation, then the start event also ends the action.
            if (endEvents.isEmpty()) {
                endEvents.add(startEvent);
            }

            // Add appropriate guards and updates to the possibly newly created CIF edge for the incoming control flow.
            if (incoming != null) {
                Edge startEdge = newEventEdges.get(startEvent);
                addGuardsAndUpdatesToIncomingControlFlow(incoming, startEdge);
            }

            // Add appropriate guards and updates to the possibly newly created CIF edge for the outgoing control flow.
            if (outgoing != null) {
                List<Edge> endEdges = endEvents.stream().map(newEventEdges::get).toList();
                addGuardsAndUpdatesToOutgoingControlFlow(outgoing, endEdges);
            }

            result.putAll(newEventEdges);
        }

        return result;
    }

    /**
     * Determines a name for the given activity node, for translating it to an action.
     *
     * @param node The activity node.
     * @return The action name of the given activity node.
     */
    private static String getActionNameForActivityNode(ActivityNode node) {
        return String.format("%s__%s__%s", NODE_PREFIX, node.eClass().getName(), IDHelper.getID(node));
    }

    /**
     * Helper method for translating UML activity nodes. This method adds appropriate CIF guards and updates for the
     * given incoming UML control flow of a translated activity node: the control flow must have a token, and after
     * performing (one of the translated CIF start events of) the node, the token will be removed.
     *
     * @param controlFlow The incoming UML control flow.
     * @param startEdge The CIF edge that has been created to start executing the UML activity node.
     */
    private void addGuardsAndUpdatesToIncomingControlFlow(ActivityEdge controlFlow, Edge startEdge) {
        DiscVariable incomingVariable = controlFlowMap.get(controlFlow);

        // Add a guard expressing that, to start executing the node, the UML control flow must have a token.
        DiscVariableExpression incomingGuard = CifConstructors.newDiscVariableExpression(null,
                EcoreUtil.copy(incomingVariable.getType()), incomingVariable);
        startEdge.getGuards().add(incomingGuard);

        // Add an update that removes the token from the UML control flow when starting to execute the node.
        Assignment incomingUpdate = CifConstructors.newAssignment();
        incomingUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                EcoreUtil.copy(incomingVariable.getType()), incomingVariable));
        incomingUpdate.setValue(CifValueUtils.makeFalse());
        startEdge.getUpdates().add(incomingUpdate);
    }

    /**
     * Helper method for translating UML activity nodes. This method adds appropriate CIF guards and updates for the
     * given outgoing UML control flow of a translated activity node: the control flow must not have a token, and after
     * performing (one of the translated CIF end events of) the node, it will receive a token. Moreover, if the outgoing
     * control flow has a guard, then this guard is added as an extra guard for performing the end event.
     *
     * @param controlFlow The outgoing UML control flow.
     * @param endEdges The CIF edges that have been created to end executing the UML activity node.
     */
    private void addGuardsAndUpdatesToOutgoingControlFlow(ActivityEdge controlFlow, List<Edge> endEdges) {
        DiscVariable outgoingVariable = controlFlowMap.get(controlFlow);

        for (Edge endEdge: endEdges) {
            // Add a guard expressing that, to end executing the node, the UML control flow must not have a token.
            UnaryExpression outgoingGuard = CifConstructors.newUnaryExpression();
            outgoingGuard.setChild(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(outgoingVariable.getType()), outgoingVariable));
            outgoingGuard.setOperator(UnaryOperator.INVERSE);
            outgoingGuard.setType(CifConstructors.newBoolType());
            endEdge.getGuards().add(outgoingGuard);

            // Add an update that adds a token to the UML control flow when ending the execution of the node.
            Assignment outgoingUpdate = CifConstructors.newAssignment();
            outgoingUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(outgoingVariable.getType()), outgoingVariable));
            outgoingUpdate.setValue(CifValueUtils.makeTrue());
            endEdge.getUpdates().add(outgoingUpdate);

            // If the UML control flow has a guard, then add it as an extra guard for ending the node execution.
            // Moreover, in that case, we require that the UML activity node has no defined effects, which is
            // needed to adhere to the execution semantics of activities. In practice, the UML activity node
            // is likely a UML decision node and thus have no effects.
            if (controlFlow.getGuard() != null) {
                Verify.verify(!PokaYokeUmlProfileUtil.isSetEffects(controlFlow.getSource()),
                        "Expected the source nodes of guarded outgoing control flows to have no defined effects.");
                endEdge.getGuards().add(translator.translate(CifParserHelper.parseExpression(controlFlow.getGuard())));
            }
        }
    }

    /**
     * Gives the guard of the given UML element.
     *
     * @param element The UML element.
     * @return The guard of the given UML element.
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
     * @param event The CIF event, which must have been translated for some UML element in the input UML model.
     * @return The original guard corresponding to the given CIF event.
     */
    public Expression getGuard(Event event) {
        RedefinableElement element = startEventMap.get(event);
        Preconditions.checkNotNull(element,
                "Expected a CIF event that has been translated for some UML element in the input UML model.");
        return getGuard(element);
    }

    /**
     * Gives all effects of the given UML element. Every effect consists of a list of updates.
     *
     * @param action The UML element.
     * @return All effects of the given UML element. There is always at least one effect.
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
     * no atomic non-deterministic actions, then no atomicity variable and no extra edge guards and updates are created.
     *
     * @return The created atomicity variable, or {@code null} in case there are no atomic non-deterministic actions.
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
        if (!atomicNonDeterministicStartEvents.isEmpty()) {
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
                if (umlElement instanceof Activity umlActivity) {
                    if (!umlActivity.isAbstract()) {
                        Set<InitialNode> initialNodes = umlActivity.getNodes().stream()
                                .filter(InitialNode.class::isInstance).map(InitialNode.class::cast)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        List<Event> cifStartEvents = startEventMap.entrySet().stream()
                                .filter(entry -> initialNodes.contains(entry.getValue())).map(Entry::getKey).toList();

                        String name = umlConstraint.getName() + "__" + umlActivity.getName();
                        cifAutomata.add(createIntervalAutomaton(name, cifStartEvents, min, max));
                    }
                } else if (umlElement instanceof OpaqueBehavior umlOpaqueBehavior) {
                    List<Event> cifStartEvents = startEventMap.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(umlOpaqueBehavior)).map(Entry::getKey).toList();

                    String name = umlConstraint.getName() + "__" + umlOpaqueBehavior.getName();
                    cifAutomata.add(createIntervalAutomaton(name, cifStartEvents, min, max));
                } else {
                    throw new RuntimeException("Unsupported element: " + umlElement);
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
     * @param events The events to express the requirement over.
     * @param min The minimum number of event occurrences.
     * @param max The maximum number of event occurrences.
     * @return The CIF requirement automaton.
     */
    private Automaton createIntervalAutomaton(String name, List<Event> events, int min, int max) {
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

        // Create the edges in the automaton.
        for (Event event: events) {
            // Create an edge for the current event in the automaton.
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
        }

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
     * that no non-atomic and atomic non-deterministic actions may be active, that all occurrence constraints must be
     * satisfied, and that no control flow of any translated concrete activity holds a token.
     *
     * @param cifNonAtomicVars The internal CIF variables created for non-atomic actions.
     * @param cifAtomicityVar The internal CIF variable created for atomic non-deterministic actions. Is {@code null} if
     *     no atomic non-deterministic actions are present.
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

        // For every control flow of a translated concrete activity, define an extra postcondition that expresses that
        // the control flow must not hold a token.
        for (var entry: controlFlowMap.entrySet()) {
            DiscVariable cifControlFlowVar = entry.getValue();

            // First define the postcondition expression.
            UnaryExpression cifExtraPostcondition = CifConstructors.newUnaryExpression();
            cifExtraPostcondition.setChild(
                    CifConstructors.newDiscVariableExpression(null, CifConstructors.newBoolType(), cifControlFlowVar));
            cifExtraPostcondition.setOperator(UnaryOperator.INVERSE);
            cifExtraPostcondition.setType(CifConstructors.newBoolType());

            // Then define an extra CIF algebraic variable for the extra postcondition.
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null,
                    POSTCONDITION_PREFIX + cifControlFlowVar.getName(), null, CifConstructors.newBoolType(),
                    cifExtraPostcondition);
            postconditionVars.add(cifAlgVar);
        }

        // Combine all defined postcondition variables to a single algebraic postcondition variable, whose value is the
        // conjunction of all these defined postcondition variables (which are all Boolean typed).
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
