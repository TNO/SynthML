
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
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifTextUtils;
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
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ControlNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.ValueSpecification;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.common.IDHelper;
import com.github.tno.pokayoke.transform.common.ValidationHelper;
import com.github.tno.pokayoke.transform.flatten.FlattenUMLActivity;
import com.github.tno.pokayoke.transform.track.SynthesisUmlElementTracking;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.CifParserHelper;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

/** Translates UML synthesis specifications to CIF specifications. */
public class UmlToCifTranslator extends ModelToCifTranslator {
    /** The name of the atomicity variable used in translated CIF specifications. */
    public static final String ATOMICITY_VARIABLE_NAME = "__activeAction";

    /** The prefix of a variable used in translated CIF specifications indicating that an action is active. */
    public static final String NONATOMIC_PREFIX = "__nonAtomicActive";

    /** The prefix of a CIF variable that encodes (part of) the activity precondition. */
    public static final String PRECONDITION_PREFIX = "__precondition";

    /** The prefix of a CIF event that has been created for a concrete activity node. */
    public static final String NODE_PREFIX = "__node";

    /** The prefix of a CIF variable indicating that a translated UML control flow holds a token. */
    public static final String CONTROLFLOW_PREFIX = "__controlflow";

    /** The suffix of a UML element name that encodes the start of a non-atomic action. */
    public static final String START_ACTION_SUFFIX = "_start";

    /** The suffix of a UML element name that encodes the end of a non-atomic action. */
    public static final String END_ACTION_SUFFIX = "_end";

    /** The input UML activity to translate. */
    private final Activity activity;

    /** The translated precondition CIF variable. */
    private AlgVariable preconditionVariable;

    /**
     * The translated postcondition CIF variable, for each postcondition purpose for which a postcondition CIF variable
     * has been created so far.
     */
    private Map<PostConditionKind, AlgVariable> postconditionVariables = new LinkedHashMap<>();

    /** The purpose for which UML is translated to CIF. */
    private final TranslationPurpose translationPurpose;

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

    /**
     * The one-to-many mapping from normalized names (see {@link #normalizeName}) of UML elements to their corresponding
     * CIF events.
     */
    private final Map<String, List<Event>> normalizedNameToEvents = new LinkedHashMap<>();

    /**
     * The internal events of the generated CIF specification, i.e. events that are not observable from the UML model
     * point-of-view.
     */
    private final Set<Event> internalEvents = new LinkedHashSet<>();

    /** The mapping between pairs of incoming/outgoing edges of 'or'-type nodes and their corresponding start events. */
    private final BiMap<Pair<ActivityEdge, ActivityEdge>, Event> activityOrNodeMapping = HashBiMap.create();

    /**
     * The tracker that stores UML elements and related CIF events, the Petri net transitions, and finally the generated
     * UML elements in the synthesized activity.
     */
    private SynthesisUmlElementTracking synthesisUmlElementsTracker;

    public static enum TranslationPurpose {
        SYNTHESIS, GUARD_COMPUTATION, LANGUAGE_EQUIVALENCE;
    }

    public UmlToCifTranslator(Activity activity, TranslationPurpose purpose,
            SynthesisUmlElementTracking synthesisUmlElementsTracker)
    {
        super(new CifContext(activity.getModel()));
        this.activity = activity;
        this.translationPurpose = purpose;
        this.synthesisUmlElementsTracker = synthesisUmlElementsTracker;
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
     * Returns the one-to-many mapping from normalized names (see {@link #normalizeName}) of UML elements to their
     * corresponding CIF events.
     *
     * @return The mapping.
     */
    public Map<String, List<Event>> getNormalizedNameToEventsMap() {
        return normalizedNameToEvents;
    }

    /**
     * Returns the internal events of the generated CIF specification, i.e. events that are not observable from the UML
     * model point-of-view.
     *
     * @return The set of internal events.
     */
    public Set<Event> getInternalEvents() {
        return internalEvents;
    }

    /**
     * Returns the set containing the non-escaped absolute names of the CIF discrete variables.
     *
     * @return The set of CIF discrete variables non-escaped absolute names.
     */
    public Set<String> getVariableNames() {
        return variableMap.values().stream().map(p -> CifTextUtils.getAbsName(p, false)).collect(Collectors.toSet());
    }

    /**
     * Gives the CIF precondition of the translated activity.
     *
     * @return The CIF precondition of the translated activity.
     */
    public Expression getTranslatedPrecondition() {
        Verify.verifyNotNull(preconditionVariable, "Expected a translated precondition CIF variable.");
        return CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), preconditionVariable);
    }

    /**
     * Gives the CIF postcondition of the translated activity, for the given postcondition kind.
     *
     * @param kind The postcondition kind for which to return the postcondition expression.
     * @return The CIF postcondition of the translated activity, for the given postcondition kind.
     */
    public Expression getTranslatedPostcondition(PostConditionKind kind) {
        AlgVariable algVar = postconditionVariables.get(kind);
        Verify.verifyNotNull(algVar, "Expected a translated postcondition CIF variable for kind " + kind + ".");
        return CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), algVar);
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
     * Gives the one-to-one mapping from UML activity edges to their corresponding translated CIF discrete variables.
     *
     * @return The one-to-one mapping from UML activity edges to their corresponding translated CIF discrete variables.
     */
    public BiMap<ActivityEdge, DiscVariable> getControlFlowMap() {
        return ImmutableBiMap.copyOf(controlFlowMap);
    }

    /**
     * Returns the mapping between pairs of incoming/outgoing edges of 'or'-type nodes and their corresponding start
     * events.
     *
     * @return The mapping.
     */
    public BiMap<Pair<ActivityEdge, ActivityEdge>, Event> getActivityOrNodeMapping() {
        return ImmutableBiMap.copyOf(activityOrNodeMapping);
    }

    /**
     * Returns the name of the created CIF plant automaton.
     *
     * @return The plant automaton name.
     */
    public String getPlantName() {
        return activity.getContext().getName();
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
        //
        // Ideally, we check this always, as the UML models resulting from synthesis should also be valid. Currently, we
        // do it only for the input to synthesis, as we generate some names during synthesis that are invalid. This is
        // to be improved in the future.
        if (translationPurpose == TranslationPurpose.SYNTHESIS) {
            ValidationHelper.validateModel(activity.getModel());
        }

        // Flatten UML activities and normalize IDs.
        if (translationPurpose == TranslationPurpose.SYNTHESIS) {
            FlattenUMLActivity flattener = new FlattenUMLActivity(activity.getModel());
            flattener.transform();
            FileHelper.normalizeIds(activity.getModel());
        }

        // Create the CIF specification to which the input UML model will be translated.
        Specification cifSpec = CifConstructors.newSpecification();
        cifSpec.setName("specification");

        // Translate all UML enumerations.
        List<EnumDecl> cifEnums = translateEnumerations();
        cifSpec.getDeclarations().addAll(cifEnums);

        // Create the CIF plant for the UML activity to translate.
        Automaton cifPlant = CifConstructors.newAutomaton();
        cifPlant.setKind(SupKind.PLANT);
        cifPlant.setName(getPlantName());
        cifSpec.getComponents().add(cifPlant);

        // Translate all UML properties.
        List<DiscVariable> cifPropertyVars = translateProperties();
        cifPlant.getDeclarations().addAll(cifPropertyVars);

        // Create the single location within the CIF plant, which will become a flower automaton.
        Location cifLocation = CifConstructors.newLocation();
        cifLocation.getInitials().add(CifValueUtils.makeTrue());
        cifLocation.getMarkeds().add(CifValueUtils.makeTrue());
        cifPlant.getLocations().add(cifLocation);

        // Translate all UML opaque behaviors. These are only translated for synthesis. For other purposes, the
        // already-synthesized activity is used, and call behaviors to opaque behaviors are inlined.
        if (translationPurpose == TranslationPurpose.SYNTHESIS) {
            BiMap<Event, Edge> cifEventEdges = translateOpaqueBehaviors();
            for (var entry: cifEventEdges.entrySet()) {
                cifSpec.getDeclarations().add(entry.getKey());
                cifLocation.getEdges().add(entry.getValue());
            }
        }

        // Translate all UML concrete activities, or only some of them, depending on the translation purpose.
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

        // Translate all occurrence constraints of the input UML activity. For the language equivalence check, the
        // constraints have already been included in the structure and guards, and we want to check that it was done
        // correctly, so we don't translate them.
        if (translationPurpose != TranslationPurpose.LANGUAGE_EQUIVALENCE) {
            List<Automaton> cifRequirementAutomata = translateOccurrenceConstraints();
            cifSpec.getComponents().addAll(cifRequirementAutomata);
        }

        // Translate all preconditions of the input UML activity as an initialization predicate in CIF, and also add
        // any additional required preconditions as needed.
        Pair<List<AlgVariable>, AlgVariable> preconditions = translatePreconditions();
        cifPlant.getDeclarations().addAll(preconditions.left);
        preconditionVariable = preconditions.right;
        cifPlant.getDeclarations().add(preconditionVariable);
        cifPlant.getInitials().add(getTranslatedPrecondition());

        // Translate all postconditions of the input UML activity.
        switch (translationPurpose) {
            case LANGUAGE_EQUIVALENCE:
            case SYNTHESIS: {
                // Translate postconditions once, to get a single algebraic variable that represents the postcondition.
                // It is used as marking predicate, and later also to disable events when the postcondition holds.
                Pair<List<AlgVariable>, AlgVariable> postconditions = translatePostconditions(cifNonAtomicVars,
                        cifAtomicityVar, PostConditionKind.SINGLE);
                cifPlant.getDeclarations().addAll(postconditions.left);
                postconditionVariables.put(PostConditionKind.SINGLE, postconditions.right);
                cifPlant.getDeclarations().add(postconditions.right);

                cifPlant.getMarkeds().add(getTranslatedPostcondition(PostConditionKind.SINGLE));
                break;
            }

            case GUARD_COMPUTATION: {
                // Translate postconditions twice, once to determine the postcondition without structure, and once to
                // determine the postcondition with structure. Both are later used for disable different events when
                // different postconditions hold. The postcondition with structure is used as marking predicate.
                Pair<List<AlgVariable>, AlgVariable> postconditionsWithoutStructure = translatePostconditions(
                        cifNonAtomicVars, cifAtomicityVar, PostConditionKind.WITHOUT_STRUCTURE);
                cifPlant.getDeclarations().addAll(postconditionsWithoutStructure.left);
                postconditionVariables.put(PostConditionKind.WITHOUT_STRUCTURE, postconditionsWithoutStructure.right);
                cifPlant.getDeclarations().add(postconditionsWithoutStructure.right);

                Pair<List<AlgVariable>, AlgVariable> postconditionsWithStructure = translatePostconditions(
                        cifNonAtomicVars, cifAtomicityVar, PostConditionKind.WITH_STRUCTURE);
                cifPlant.getDeclarations().addAll(postconditionsWithStructure.left);
                postconditionVariables.put(PostConditionKind.WITH_STRUCTURE, postconditionsWithStructure.right);
                cifPlant.getDeclarations().add(postconditionsWithStructure.right);

                cifPlant.getMarkeds().add(getTranslatedPostcondition(PostConditionKind.WITH_STRUCTURE));
                break;
            }

            default:
                throw new AssertionError("Unknown translation purpose: " + translationPurpose);
        }

        // Create extra requirements to ensure that, whenever the postcondition holds, no further steps can be taken.
        if (translationPurpose != TranslationPurpose.LANGUAGE_EQUIVALENCE) {
            List<Invariant> cifDisableConstraints = createDisableEventsWhenDoneRequirements();
            cifSpec.getInvariants().addAll(cifDisableConstraints);
        }

        // Translate all UML class constraints as CIF invariants. For the language equivalence check, the constraints
        // have already been included in the structure and guards, and we want to check that it was done correctly, so
        // we don't translate them again.
        if (translationPurpose != TranslationPurpose.LANGUAGE_EQUIVALENCE) {
            List<Invariant> cifRequirementInvariants = translateRequirements();
            cifPlant.getInvariants().addAll(cifRequirementInvariants);
        }

        // Return the final CIF specification.
        return cifSpec;
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
        // For guard computation, force all start events to be controllable, as the structure of the synthesized UML
        // activity is already fixed, and we just want to re-compute the guards as locally as possible.
        if (translationPurpose == TranslationPurpose.GUARD_COMPUTATION) {
            controllableStartEvent = true;
        }

        // Initialize mapping of new events to their edges.
        BiMap<Event, Edge> newEventEdges = HashBiMap.create();

        // Find the action to translate. If the UML element is a call behavior action that is not shadowed (i.e., has no
        // guards and effects), then the called action is translated, otherwise the UML element itself is translated.
        RedefinableElement umlAction;

        if (PokaYokeUmlProfileUtil.isFormalElement(umlElement)) {
            // Opaque behavior, opaque action, or shadowed call behavior, with our stereotype.
            umlAction = umlElement;
        } else if (umlElement instanceof CallBehaviorAction cbAction) {
            // Non-shadowed call behavior action. Translate the called behavior, inlining it.
            umlAction = cbAction.getBehavior();
        } else {
            // Other nodes.
            umlAction = umlElement;
        }

        Preconditions.checkArgument(!(umlAction instanceof Activity),
                "Expected call behavior nodes that call activities to first be flattened.");

        // Obtain the guard and effects of the current action.
        Expression guard = getGuard(umlAction);
        List<List<Update>> effects = getEffects(umlAction);

        // Ensure there is at least one effect.
        if (effects.isEmpty()) {
            effects = List.of(List.of());
        }

        // Check that a node with effects does not have incoming guards on its outgoing edges.
        if (effects.stream().flatMap(updates -> updates.stream()).findAny().isPresent()
                && (umlElement instanceof OpaqueAction || umlElement instanceof CallBehaviorAction))
        {
            ActivityNode node = (ActivityNode)umlElement;
            for (ActivityEdge outgoingEdge: node.getOutgoings()) {
                AExpression incomingGuard = CifParserHelper.parseIncomingGuard((ControlFlow)outgoingEdge);
                if (incomingGuard != null && !(incomingGuard instanceof ABoolExpression aBoolExpr && aBoolExpr.value)) {
                    throw new RuntimeException(String.format(
                            "Edge leaving node '%s' with effects has not-null/true incoming guard.", node.getName()));
                }
            }
        }

        // Create a CIF start event for the action.
        Event cifStartEvent = CifConstructors.newEvent();
        cifStartEvent.setControllable(controllableStartEvent);
        cifStartEvent.setName(name);
        startEventMap.put(cifStartEvent, umlElement);

        // Store the CIF event into the synthesis tracker.
        synthesisUmlElementsTracker.addCifEvent(cifStartEvent, umlElement);

        // Add the start event to the normalized name to event map.
        if (umlElement instanceof CallBehaviorAction || umlElement instanceof OpaqueAction
                || umlElement instanceof OpaqueBehavior)
        {
            // We normalize for synthesis and language equivalence check purposes, as the models generated for those
            // purposes these need to be compared by the language equivalence check. We don't do it for guard
            // computation, as there we then lose 'start' and 'end' post-fixes in names, which we need to detect
            // opaque actions that originated from split non-deterministic actions that haven't been merged back.
            // Later, we want to get rid of the name-based approach and have earlier steps produce the relevant
            // information, so that we know which actions represent starts/ends, but until then we keep basing this
            // on the names.
            if (translationPurpose != TranslationPurpose.GUARD_COMPUTATION) {
                // Add the event to the corresponding normalized name. More events may correspond to the same name.
                normalizedNameToEvents.computeIfAbsent(normalizeName(umlAction, ""), k -> new ArrayList<>())
                        .add(cifStartEvent);
            }
        } else {
            internalEvents.add(cifStartEvent);
        }

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
                String outcomeSuffix = isAtomic ? SynthesisUmlElementTracking.ATOMIC_OUTCOME_SUFFIX
                        : SynthesisUmlElementTracking.NONATOMIC_OUTCOME_SUFFIX;
                cifEndEvent.setName(name + outcomeSuffix + (i + 1));
                cifEndEvents.add(cifEndEvent);

                // Add the end event to the normalized names to event map.
                if (umlElement instanceof CallBehaviorAction || umlElement instanceof OpaqueAction
                        || umlElement instanceof OpaqueBehavior)
                {
                    // We normalize for synthesis and language equivalence check purposes, as the models generated for
                    // those purposes these need to be compared by the language equivalence check. We don't do it for
                    // guard computation, as there we then lose 'start' and 'end' post-fixes in names, which we need to
                    // detect opaque actions that originated from split non-deterministic actions that haven't been
                    // merged back. Later, we want to get rid of the name-based approach and have earlier steps produce
                    // the relevant information, so that we know which actions represent starts/ends, but until then we
                    // keep basing this on the names.
                    if (translationPurpose != TranslationPurpose.GUARD_COMPUTATION) {
                        normalizedNameToEvents
                                .computeIfAbsent(normalizeName(umlAction, outcomeSuffix + String.valueOf(i + 1)),
                                        k -> new ArrayList<>())
                                .add(cifEndEvent);
                    }
                } else {
                    internalEvents.add(cifEndEvent);
                }

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

                // Store the CIF event into the synthesis tracker.
                synthesisUmlElementsTracker.addCifEvent(cifEndEvent, new Pair<>(umlElement, i));
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
     * corresponding CIF edges. Depending on the translation purpose, only some of the concrete activities may be
     * translated.
     *
     * @return The translated CIF variables, and CIF events with their corresponding CIF edges.
     */
    private Pair<Set<DiscVariable>, BiMap<Event, Edge>> translateActivities() {
        Set<DiscVariable> newVariables = new LinkedHashSet<>();
        BiMap<Event, Edge> newEventEdges = HashBiMap.create();

        // Translate all concrete activities that are in context.
        for (Activity activity: context.getAllConcreteActivities()) {
            Pair<Set<DiscVariable>, BiMap<Event, Edge>> result = translateConcreteActivity(activity);
            newVariables.addAll(result.left);
            newEventEdges.putAll(result.right);
        }

        return Pair.pair(newVariables, newEventEdges);
    }

    /**
     * Translates a given concrete UML activity to CIF variables, and CIF events with their corresponding CIF edges.
     * Depending on the translation purpose, the activity may or may not be translated.
     *
     * @param activity The concrete UML activity to translate.
     * @return The translated CIF variables, and CIF events with their corresponding CIF edges.
     */
    private Pair<Set<DiscVariable>, BiMap<Event, Edge>> translateConcreteActivity(Activity activity) {
        // Sanity check.
        Preconditions.checkArgument(!activity.isAbstract(), "Expected a concrete activity.");

        // For the guard computation and language equivalence check, we are only concerned with the single activity
        // currently being synthesized. Therefore, we do not translate other activities than the one that is currently
        // being synthesized. The translation of other concrete activities would imply the translation of all its nodes,
        // e.g., call behavior actions to some opaque behavior, as well as opaque actions. These nodes would interleave
        // and interfere with the current activity nodes. For vertical scaling, this is still valid: once the
        // activity has been synthesized, it contains the flattened called activities. Only the synthesized activity
        // should be translated for guard computation and language equivalence check.
        if (translationPurpose != TranslationPurpose.SYNTHESIS && activity != this.activity) {
            return Pair.pair(new LinkedHashSet<>(), HashBiMap.create());
        }

        // Translate all activity control flows.
        Set<DiscVariable> newVariables = new LinkedHashSet<>(activity.getEdges().size());
        for (ActivityEdge controlFlow: activity.getEdges()) {
            // Translate the control flow.
            DiscVariable cifControlFlowVar = translateActivityControlFlow(controlFlow);
            newVariables.add(cifControlFlowVar);

            // For the activity being synthesized, place a token in the control flow that leaves the initial node.
            if (translationPurpose != TranslationPurpose.SYNTHESIS && controlFlow.getSource() instanceof InitialNode) {
                cifControlFlowVar.setValue(CifConstructors.newVariableValue(null, List.of(CifValueUtils.makeTrue())));
            }
        }

        // Translate all activity nodes.
        BiMap<Event, Edge> newEventEdges = HashBiMap.create(activity.getNodes().size());
        for (ActivityNode node: activity.getNodes()) {
            // For synthesis, we include the initial and final nodes of activities that may be called by the abstract
            // activity for which we're synthesizing the body. For other purposes, we've already excluded other
            // activities than the one being synthesized (see above), and now also exclude the initial and final nodes,
            // as the token configurations for these are considered while translating pre- and postconditions. That is,
            // for the other purposes, we start with a token on the control flow coming from the initial node, and thus
            // never 'execute' the initial node. Similarly, we end with a token on the control flow going into the final
            // node, and thus never 'execute' the final node.
            if (translationPurpose != TranslationPurpose.SYNTHESIS
                    && (node instanceof InitialNode || node instanceof FinalNode))
            {
                continue;
            }

            // Translate the activity node.
            newEventEdges.putAll(translateActivityNode(node));
        }

        // Return the new control flow token variables, and new events with their edges.
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
                // Sanity check. Translating a shadowed call behavior should occur only if translating for synthesis.
                Verify.verify(translationPurpose == TranslationPurpose.SYNTHESIS,
                        "Translating a shadowed call behavior is allowed only for synthesis translation purpose.");

                // The call behavior shadows the called behavior. We use the guards/effects of the call behavior node
                // and ignore the called behavior.
                newEventEdges = translateActivityAndNode(callNode, PokaYokeUmlProfileUtil.isAtomic(callNode), false);
            } else {
                // We translate the called behavior, inlining it. We do transform on the call node, to ensure that
                // each call gets a unique action.
                Behavior behavior = callNode.getBehavior();

                if (behavior instanceof Activity) {
                    // Sanity check. After the flattening there shouldn't be any call behaviors to activities.
                    throw new RuntimeException("Found a call behavior to an activity.");
                }

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
                        cifEdge.getGuards().add(getStateInvariant(precondition));
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
                        cifEdge.getGuards().add(getStateInvariant(postcondition));
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

        // Collect the CIF start and end events of the translated UML activity node.
        Event startEvent = translationResult.startEvent;
        List<Event> endEvents = translationResult.endEvents;

        // Collect the newly created CIF edges of the translated UML activity node.
        BiMap<Event, Edge> newEventEdges = translationResult.eventEdges;
        Edge startEdge = newEventEdges.get(startEvent);
        List<Edge> endEdges = endEvents.stream().map(newEventEdges::get).toList();

        // Add appropriate guards and updates to the newly created CIF edges.
        addExtraGuardsAndUpdatesForControlFlows(node.getIncomings(), node.getOutgoings(), startEdge, endEdges);

        return newEventEdges;
    }

    /**
     * Translates the given UML activity node as an 'or'-type node.
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

            // Collect the CIF start and end events of the translated UML activity node.
            Event startEvent = translationResult.startEvent;
            List<Event> endEvents = translationResult.endEvents;

            // Add control flow and start event to the mapping.
            activityOrNodeMapping.put(pair, startEvent);

            // Collect the newly created CIF edges of the translated UML activity node.
            BiMap<Event, Edge> newEventEdges = translationResult.eventEdges;
            Edge startEdge = newEventEdges.get(startEvent);
            List<Edge> endEdges = endEvents.stream().map(newEventEdges::get).toList();

            // Add appropriate guards and updates to the newly created CIF edges.
            addExtraGuardsAndUpdatesForControlFlows(Stream.ofNullable(incoming).toList(),
                    Stream.ofNullable(outgoing).toList(), startEdge, endEdges);

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
     * given incoming and outgoing UML control flows of a translated activity node. The guards will express that, to
     * start executing the activity node, every given incoming control flow must have a token, and none of the given
     * outgoing control flows must have a token. The updates will consume the token from every given incoming control
     * flow, and produce a token on every given outgoing control flow. Furthermore, the outgoing guards of the incoming
     * control flows and the incoming guards of the outgoing control flows are taken into account.
     *
     * @param incomingControlFlows The incoming UML control flows to consider. This list can be empty if no incoming
     *     control flows should be considered (e.g., for initial nodes).
     * @param outgoingControlFlows The outgoing UML control flows to consider. This list can be empty if no outgoing
     *     control flows should be considered (e.g., for final nodes).
     * @param startEdge The CIF edge that has been created to start executing the UML activity node.
     * @param endEdges The CIF edges that have been created to end executing the UML activity node. This list can be
     *     empty in case there are no implicit end edges (e.g., for nodes that have been translated as atomic
     *     deterministic actions).
     */
    private void addExtraGuardsAndUpdatesForControlFlows(List<ActivityEdge> incomingControlFlows,
            List<ActivityEdge> outgoingControlFlows, Edge startEdge, List<Edge> endEdges)
    {
        Preconditions.checkArgument(!endEdges.contains(startEdge),
                "Expected the given CIF start and edges to be disjoint.");

        // If there are no explicit end edges, then the start edge also ends node execution.
        if (endEdges.isEmpty()) {
            endEdges = List.of(startEdge);
        }

        // Add guards expressing that, to start executing the node, every incoming UML control flow must have a token,
        // and their outgoing guard must hold. Also add updates that consume the token from every incoming UML control
        // flow when starting node execution.
        for (ActivityEdge incoming: incomingControlFlows) {
            DiscVariable incomingVariable = controlFlowMap.get(incoming);

            // Add a guard expressing that the current incoming UML control flow must have a token.
            DiscVariableExpression guard = CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(incomingVariable.getType()), incomingVariable);
            startEdge.getGuards().add(guard);

            // Add a guard expressing that the outgoing guard of the current incoming UML control flow must hold.
            if (PokaYokeUmlProfileUtil.getOutgoingGuard(incoming) != null) {
                startEdge.getGuards().add(getOutgoingGuard(incoming));
            }

            // Add an update that consumes the token on the current incoming UML control flow.
            Assignment update = CifConstructors.newAssignment();
            update.setAddressable(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(incomingVariable.getType()), incomingVariable));
            update.setValue(CifValueUtils.makeFalse());
            startEdge.getUpdates().add(update);
        }

        // Add guards expressing that, to start executing the node, no outgoing UML control flow must have a token. Also
        // add guards expressing that, to end node execution, no outgoing UML control flow must have a token. If the
        // control flow has an incoming guard, add it as an extra guard to end node execution. And also add updates that
        // produce a token on every outgoing UML control flow when ending the execution of the node.
        for (ActivityEdge outgoing: outgoingControlFlows) {
            DiscVariable outgoingVariable = controlFlowMap.get(outgoing);

            // Add a guard expressing that, to start node execution, the current control flow must not have a token.
            UnaryExpression startGuard = CifConstructors.newUnaryExpression();
            startGuard.setChild(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(outgoingVariable.getType()), outgoingVariable));
            startGuard.setOperator(UnaryOperator.INVERSE);
            startGuard.setType(CifConstructors.newBoolType());
            startEdge.getGuards().add(startGuard);

            for (Edge endEdge: endEdges) {
                // Add a guard expressing that, to end node execution, the current control flow must not have a token.
                // Note that such a guard has already been added to the start edge, so we may skip that one.
                if (!endEdge.equals(startEdge)) {
                    UnaryExpression endGuard = CifConstructors.newUnaryExpression();
                    endGuard.setChild(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(outgoingVariable.getType()), outgoingVariable));
                    endGuard.setOperator(UnaryOperator.INVERSE);
                    endGuard.setType(CifConstructors.newBoolType());
                    endEdge.getGuards().add(endGuard);
                }

                // Add an update that produces a token on the current control flow.
                Assignment update = CifConstructors.newAssignment();
                update.setAddressable(CifConstructors.newDiscVariableExpression(null,
                        EcoreUtil.copy(outgoingVariable.getType()), outgoingVariable));
                update.setValue(CifValueUtils.makeTrue());
                endEdge.getUpdates().add(update);

                // If the current control flow has an incoming guard, then add it as an extra guard for ending node
                // execution. Moreover, in that case, we require that the UML activity node has been translated as an
                // atomic deterministic action and it has no defined effects, which is needed to adhere to the execution
                // semantics of activities. In practice, the UML activity node is likely a UML decision node and thus
                // is atomic, deterministic, and has no effects. There are some validation checks just to be sure.
                if (PokaYokeUmlProfileUtil.getIncomingGuard(outgoing) != null) {
                    Verify.verify(endEdge.equals(startEdge),
                            "Expected the activity node to have been translated as an atomic deterministic action.");
                    Verify.verify(!PokaYokeUmlProfileUtil.isSetEffects(outgoing.getSource()),
                            "Expected the source nodes of guarded outgoing control flows to have no defined effects.");

                    endEdge.getGuards().add(getIncomingGuard(outgoing));
                }
            }
        }
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
                        // The constraints are considered at a local level: this activity *directly* calls the
                        // constrained activity. We do not include the initial nodes of the activities recursively, i.e.
                        // this activity can call an activity that calls a constrained activity, and will *not* add to
                        // the occurrence constraint count.
                        Set<InitialNode> initialNodes = umlActivity.getNodes().stream()
                                .filter(InitialNode.class::isInstance).map(InitialNode.class::cast)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                        List<Event> cifStartEvents = startEventMap.entrySet().stream()
                                .filter(entry -> initialNodes.contains(entry.getValue())).map(Entry::getKey).toList();

                        String name = String.format("%s__%s__%s__%s", umlConstraint.getName(),
                                IDHelper.getID(umlConstraint), umlActivity.getName(), IDHelper.getID(umlActivity));

                        cifAutomata.add(createIntervalAutomaton(name, cifStartEvents, min, max));
                    }
                } else if (umlElement instanceof OpaqueBehavior umlOpaqueBehavior) {
                    // The constraints are considered at a local level: this activity *directly* calls the
                    // constrained behavior. We do not include the call nodes of the opaque behavior recursively, i.e.
                    // this activity can call an activity that calls the constrained behavior, and will *not* add to the
                    // occurrence constraint count.
                    List<Event> cifStartEvents;
                    if (translationPurpose == TranslationPurpose.SYNTHESIS) {
                        // For synthesis, we directly translate the opaque behaviors, so we simply look up their
                        // associated start events.
                        cifStartEvents = startEventMap.entrySet().stream()
                                .filter(entry -> entry.getValue().equals(umlOpaqueBehavior)).map(Entry::getKey)
                                .toList();

                        // Sanity check: we must have found at least one start event.
                        Verify.verify(!cifStartEvents.isEmpty(), "Found no CIF start events for: " + umlOpaqueBehavior);
                    } else if (translationPurpose == TranslationPurpose.GUARD_COMPUTATION) {
                        // For guard computation, we don't directly translate the opaque behaviors. Instead, we inline
                        // call behaviors to such opaque behaviors. Furthermore, some non-atomic opaque behaviors may
                        // have separate start and end opaque actions that could not be merged back into call behaviors
                        // to the original opaque behaviors. The occurrence constraints still refer to the original
                        // opaque behaviors, and for both cases we have to find the relevant CIF events.
                        cifStartEvents = startEventMap.entrySet().stream().filter(entry ->
                        // Include all CIF events for call behavior actions that call the constrained opaque behavior.
                        // This includes all atomic opaque behaviors, as well as merged-back non-atomic ones.
                        (entry.getValue() instanceof CallBehaviorAction cbAction
                                && cbAction.getBehavior().equals(umlOpaqueBehavior))
                                // Include all CIF events for opaque actions that represent start actions of
                                // non-deterministic opaque behaviors. These are the ones that couldn't be merged back
                                // to call behavior actions that call the constrained opaque behavior.
                                || (entry.getValue() instanceof OpaqueAction oAction
                                        && oAction.getName().equals(umlOpaqueBehavior.getName() + START_ACTION_SUFFIX)))
                                .map(Entry::getKey).toList();

                        // We don't check whether we found at least one start event. In case of unused actions, these
                        // won't have been translated, and we thus don't get any start events. That is OK, if we don't
                        // have to do the action. If we must do the action, we have to have at least one event, to track
                        // the occurrences and enforce the constraint.
                        if (min > 0) {
                            Verify.verify(!cifStartEvents.isEmpty(),
                                    "Found no CIF start events for mandatory opaque behavior: " + umlOpaqueBehavior);
                        }
                    } else {
                        throw new AssertionError("Unexpected translation purpose: " + translationPurpose);
                    }

                    // Create interval automaton for the occurrence constraint.
                    String name = String.format("%s__%s__%s__%s", umlConstraint.getName(),
                            IDHelper.getID(umlConstraint), umlOpaqueBehavior.getName(),
                            IDHelper.getID(umlOpaqueBehavior));

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
     * Translates the user-specified UML activity preconditions and any other required preconditions to CIF algebraic
     * variables, combining them into a single algebraic precondition variable.
     *
     * @return A pair consisting of auxiliary CIF algebraic variables that encode parts of the precondition, together
     *     with the CIF algebraic variable that encodes the entire precondition.
     */
    private Pair<List<AlgVariable>, AlgVariable> translatePreconditions() {
        // Translate the user-specified activity preconditions.
        List<AlgVariable> preconditionVars = translateUserSpecifiedPrePostconditions(activity.getPreconditions());

        // Add the synthesized activity's initial node configuration.
        if (translationPurpose != TranslationPurpose.SYNTHESIS) {
            for (ActivityNode node: activity.getNodes()) {
                if (node instanceof InitialNode initialNode) {
                    preconditionVars.addAll(createInitialNodeConfiguration(initialNode));
                }
            }
        }

        // Combine the user-specified and additional preconditions.
        AlgVariable preconditionVar = combinePrePostconditionVariables(preconditionVars, PRECONDITION_PREFIX);
        return Pair.pair(preconditionVars, preconditionVar);
    }

    /**
     * Translates a given collection of user-specified preconditions or postconditions to a set of CIF algebraic
     * variables, one for each user-specified pre/postcondition, whose values are the state invariant predicates of the
     * corresponding user-specified pre/postcondition.
     *
     * @param umlConstraints The collection of user-specified UML pre/postconditions to translate.
     * @return The translated Boolean-typed CIF algebraic variables.
     */
    private List<AlgVariable> translateUserSpecifiedPrePostconditions(Collection<Constraint> umlConstraints) {
        // Define an algebraic CIF variable for every UML constraint, whose value is the state invariant predicate.
        List<AlgVariable> cifConstraintVars = new ArrayList<>(umlConstraints.size());

        for (Constraint umlConstraint: umlConstraints) {
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable();
            cifAlgVar.setName(umlConstraint.getName());
            cifAlgVar.setType(CifConstructors.newBoolType());
            cifAlgVar.setValue(getStateInvariant(umlConstraint));
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
     * Translates the user-specified UML activity postconditions and/or any other required postconditions to CIF
     * algebraic variables, combining them into a single algebraic postcondition variable.
     *
     * @param cifNonAtomicVars The internal CIF variables created for non-atomic actions.
     * @param cifAtomicityVar The internal CIF variable created for atomic non-deterministic actions. Is {@code null} if
     *     no atomic non-deterministic actions are present.
     * @param kind The kind of postcondition to translate.
     * @return A pair consisting of auxiliary CIF algebraic variables that encode parts of the postcondition, together
     *     with the CIF algebraic variable that encodes the entire postcondition.
     */
    private Pair<List<AlgVariable>, AlgVariable> translatePostconditions(List<DiscVariable> cifNonAtomicVars,
            DiscVariable cifAtomicityVar, PostConditionKind kind)
    {
        // Initialize the list of postcondition variables for the partial conditions.
        List<AlgVariable> postconditionVars = new ArrayList<>();

        // For guard computation, we have two postconditions. For the 'with structure' postcondition, include the
        // 'without structure' postcondition.
        if (translationPurpose == TranslationPurpose.GUARD_COMPUTATION && kind == PostConditionKind.WITH_STRUCTURE) {
            Expression condition = getTranslatedPostcondition(PostConditionKind.WITHOUT_STRUCTURE);
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null, kind.prefix + "__without_structure", null,
                    CifConstructors.newBoolType(), condition);
            postconditionVars.add(cifAlgVar);
        }

        // Translate the user-specified activity postconditions. For the language equivalence check, these conditions
        // have already been included in the structure and guards, and we want to check that it was done correctly, so
        // we don't translate them again. For guard computation, these are not part of the structure postconditions.
        if (translationPurpose != TranslationPurpose.LANGUAGE_EQUIVALENCE && kind != PostConditionKind.WITH_STRUCTURE) {
            postconditionVars.addAll(translateUserSpecifiedPrePostconditions(activity.getPostconditions()));
        }

        // For every translated non-atomic action, define an extra postcondition that expresses that the non-atomic
        // action must not be active. For the language equivalence check, these conditions have already been included in
        // the structure and guards, and we want to check that it was done correctly, so we don't translate them again.
        // For guard computation, these are not part of the structure postconditions.
        if (translationPurpose != TranslationPurpose.LANGUAGE_EQUIVALENCE && kind != PostConditionKind.WITH_STRUCTURE) {
            for (DiscVariable cifNonAtomicVar: cifNonAtomicVars) {
                // First define the postcondition expression.
                UnaryExpression cifExtraPostcondition = CifConstructors.newUnaryExpression();
                cifExtraPostcondition.setChild(CifConstructors.newDiscVariableExpression(null,
                        CifConstructors.newBoolType(), cifNonAtomicVar));
                cifExtraPostcondition.setOperator(UnaryOperator.INVERSE);
                cifExtraPostcondition.setType(CifConstructors.newBoolType());

                // Then define an extra CIF algebraic variable for the extra postcondition.
                AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null, kind.prefix + cifNonAtomicVar.getName(),
                        null, CifConstructors.newBoolType(), cifExtraPostcondition);
                postconditionVars.add(cifAlgVar);
            }
        }

        // If the atomicity variable has been added, then define an extra postcondition that expresses that no
        // atomic non-deterministic action must be active. For the language equivalence check, these conditions have
        // already been included in the structure and guards, and we want to check that it was done correctly, so we
        // don't translate them again. For guard computation, these are not part of the structure postconditions.
        if (translationPurpose != TranslationPurpose.LANGUAGE_EQUIVALENCE && kind != PostConditionKind.WITH_STRUCTURE
                && cifAtomicityVar != null)
        {
            // First define the postcondition expression.
            BinaryExpression cifExtraPostcondition = CifConstructors.newBinaryExpression();
            cifExtraPostcondition.setLeft(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
            cifExtraPostcondition.setOperator(BinaryOperator.EQUAL);
            cifExtraPostcondition.setRight(CifValueUtils.makeInt(0));
            cifExtraPostcondition.setType(CifConstructors.newBoolType());

            // Then define an extra CIF algebraic variable for this extra postcondition.
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null, kind.prefix + cifAtomicityVar.getName(), null,
                    CifConstructors.newBoolType(), cifExtraPostcondition);
            postconditionVars.add(cifAlgVar);
        }

        // For every translated occurrence constraint, define an extra postcondition that expresses that the marked
        // predicate of the corresponding CIF requirement automata must hold. For the language equivalence check, these
        // conditions have already been included in the structure and guards, and we want to check that it was done
        // correctly, so we don't translate them again. For guard computation, these are not part of the structure
        // postconditions.
        if (translationPurpose == TranslationPurpose.LANGUAGE_EQUIVALENCE) {
            Verify.verify(occurrenceConstraintMap.isEmpty());
        } else if (kind != PostConditionKind.WITH_STRUCTURE) {
            for (Entry<IntervalConstraint, List<Automaton>> entry: occurrenceConstraintMap.entrySet()) {
                for (Automaton cifRequirement: entry.getValue()) {
                    // First define the postcondition expression.
                    Expression cifExtraPostcondition = CifValueUtils
                            .createConjunction(List.copyOf(EcoreUtil.copyAll(cifRequirement.getMarkeds())));

                    // Then define an extra CIF algebraic variable for this extra postcondition.
                    AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null,
                            kind.prefix + "__" + cifRequirement.getName(), null, CifConstructors.newBoolType(),
                            cifExtraPostcondition);
                    postconditionVars.add(cifAlgVar);
                }
            }
        }

        // Define extra postconditions for control flows of translated concrete activities that must not hold a token.
        // For the language equivalence check, these conditions have already been included in the structure and guards,
        // and we want to check that it was done correctly, so we don't translate them again. For guard computation,
        // these are part of the structure postconditions.
        if (translationPurpose != TranslationPurpose.LANGUAGE_EQUIVALENCE
                && kind != PostConditionKind.WITHOUT_STRUCTURE)
        {
            for (var entry: controlFlowMap.entrySet()) {
                // For synthesis, we don't want any tokens on control flows of called activities. For guard computation,
                // we also want no tokens on control flows, except for the incoming control flow into the final node.
                // That last case is handled later in this method, so that particular control flow is excluded here.
                boolean isIncomingToFinalNode = entry.getKey().getTarget() instanceof FinalNode;
                if (translationPurpose == TranslationPurpose.GUARD_COMPUTATION && isIncomingToFinalNode) {
                    continue;
                }

                // Get the control flow variable for the control flow that must not have a token.
                DiscVariable cifControlFlowVar = entry.getValue();

                // Define the postcondition expression.
                UnaryExpression cifExtraPostcondition = CifConstructors.newUnaryExpression();
                cifExtraPostcondition.setChild(CifConstructors.newDiscVariableExpression(null,
                        CifConstructors.newBoolType(), cifControlFlowVar));
                cifExtraPostcondition.setOperator(UnaryOperator.INVERSE);
                cifExtraPostcondition.setType(CifConstructors.newBoolType());

                // Define a CIF algebraic variable for the postcondition.
                AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null, kind.prefix + cifControlFlowVar.getName(),
                        null, CifConstructors.newBoolType(), cifExtraPostcondition);
                postconditionVars.add(cifAlgVar);
            }
        }

        // If we already have a synthesized activity, add the postcondition that a token must be placed at the 'final'
        // control flow leading to the final node. For guard computation, this is part of the structure postcondition.
        if (translationPurpose != TranslationPurpose.SYNTHESIS && kind != PostConditionKind.WITHOUT_STRUCTURE) {
            for (ActivityNode node: activity.getNodes()) {
                if (node instanceof FinalNode finalNode) {
                    postconditionVars.addAll(createFinalNodeConfiguration(finalNode));
                }
            }
        }

        // Combine the user-specified and/or additional postconditions.
        AlgVariable postconditionVar = combinePrePostconditionVariables(postconditionVars, kind.prefix);
        return Pair.pair(postconditionVars, postconditionVar);
    }

    /**
     * Creates CIF state/event exclusion invariant requirements to disable all events whenever the relevant activity
     * postcondition holds.
     *
     * @return The created CIF requirement invariants.
     */
    private List<Invariant> createDisableEventsWhenDoneRequirements() {
        List<Invariant> cifInvariants = new ArrayList<>(eventEdgeMap.size());

        for (Event cifEvent: eventEdgeMap.keySet()) {
            // Determine which postcondition to use.
            PostConditionKind kind = switch (translationPurpose) {
                case GUARD_COMPUTATION -> {
                    if (internalEvents.contains(cifEvent)) {
                        // We must allow internal actions after the user-defined postconditions etc hold, to ensure
                        // that the token can still pass through merge/join/etc nodes and the token can still reach
                        // the incoming control flow to the final place.
                        yield PostConditionKind.WITH_STRUCTURE;
                    } else if (startEventMap.containsKey(cifEvent)
                            && startEventMap.get(cifEvent) instanceof CallBehaviorAction)
                    {
                        // As soon as the user-defined postconditions etc hold, we should no longer allow starting any
                        // of the actions that the user defined. This case handles events that start such actions
                        // through a call behavior action.
                        yield PostConditionKind.WITHOUT_STRUCTURE;
                    } else if (startEventMap.containsKey(cifEvent)
                            && startEventMap.get(cifEvent) instanceof OpaqueAction oAction
                            && oAction.getName().endsWith(START_ACTION_SUFFIX))
                    {
                        // As soon as the user-defined postconditions etc hold, we should no longer allow starting any
                        // of the actions that the user defined. This case handles events that start such actions
                        // through an opaque action, which only applies to non-atomic actions that couldn't be merged
                        // back to a call behavior to the original opaque behavior.
                        yield PostConditionKind.WITHOUT_STRUCTURE;
                    } else {
                        // We must allow finishing non-atomic/non-deterministic actions.
                        if (startEventMap.containsKey(cifEvent)) {
                            // End of a non-deterministic opaque behavior that couldn't be merged back to a call
                            // behavior to the original opaque behavior, but instead is left as an opaque action.
                            RedefinableElement umlElem = startEventMap.get(cifEvent);
                            Verify.verify(umlElem instanceof OpaqueAction, cifEvent.getName());
                            Verify.verify(umlElem.getName().contains(END_ACTION_SUFFIX), cifEvent.getName());
                        } else {
                            // End event of a call behavior to a non-atomic/non-deterministic opaque behavior.
                            boolean isNonAtomicEnd = nonAtomicEventMap.values().stream()
                                    .anyMatch(events -> events.contains(cifEvent));
                            boolean isNonDeterministicEnd = nonDeterministicEventMap.values().stream()
                                    .anyMatch(events -> events.contains(cifEvent));
                            Verify.verify(isNonAtomicEnd || isNonDeterministicEnd, cifEvent.getName());
                        }
                        yield PostConditionKind.WITH_STRUCTURE;
                    }
                }

                // If there is only one postcondition, there is nothing to choose.
                case LANGUAGE_EQUIVALENCE, SYNTHESIS -> PostConditionKind.SINGLE;
            };

            // Get the associated postcondition algebraic variable.
            AlgVariable cifPostconditionVar = postconditionVariables.get(kind);
            Preconditions.checkNotNull(cifPostconditionVar, "Expected a non-null postcondition variable.");

            // Add the requirement.
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

        List<Invariant> cifInvariants = getInvariants(umlConstraint);

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
     * For a UML element, returns a normalized name that is consistent pre and post synthesis. It is used to map a CIF
     * event to the underlying UML element: e.g., if the element is a call behavior action, it refers to the called
     * element.
     *
     * @param umlElement The UML element.
     * @param postfix A string that is added to the normalized name; it can be empty.
     * @return The normalized name of the UML element.
     */
    private String normalizeName(NamedElement umlElement, String postfix) {
        // Get to the called behavior.
        if (umlElement instanceof CallBehaviorAction cbAction) {
            umlElement = cbAction.getBehavior();
        }

        // If name contains the post-synthesis identifier for the start or end of a non-atomic action, replace it with
        // its original non-atomic action identifier.
        String elementName = umlElement.getName();
        if (elementName.endsWith(START_ACTION_SUFFIX)) {
            umlElement.setName(elementName.substring(0, elementName.length() - START_ACTION_SUFFIX.length()));
        } else if (elementName.contains(END_ACTION_SUFFIX)) {
            umlElement.setName(elementName.substring(0, elementName.lastIndexOf(END_ACTION_SUFFIX))
                    + SynthesisUmlElementTracking.NONATOMIC_OUTCOME_SUFFIX
                    + elementName.substring(elementName.lastIndexOf(END_ACTION_SUFFIX) + END_ACTION_SUFFIX.length()));
        }

        return "UML_element__" + umlElement.getName() + postfix;
    }

    /**
     * Create the token configuration for the initial node of the activity: the token is placed on the control flow
     * whose source is the initial node. Add also its incoming guards, if present.
     *
     * @param node The UML activity initial node.
     * @return The list of CIF algebraic variables representing the initial node token configuration.
     */
    private List<AlgVariable> createInitialNodeConfiguration(InitialNode node) {
        List<AlgVariable> initialNodeConfig = new ArrayList<>();

        // Create a new algebraic variable out of the discrete one, for later use in the preconditions.
        Verify.verify(node.getOutgoings().size() == 1, "Expected unique outgoing control flow from initial node.");
        ActivityEdge outgoing = node.getOutgoings().get(0);
        DiscVariable outgoingVariable = controlFlowMap.get(outgoing);
        DiscVariableExpression tokenOnControlflowExpr = CifConstructors.newDiscVariableExpression(null,
                EcoreUtil.copy(outgoingVariable.getType()), outgoingVariable);
        AlgVariable tokenOnOutgoing = CifConstructors.newAlgVariable();
        tokenOnOutgoing.setName("__token_on_first_controlflow");
        tokenOnOutgoing.setType(CifConstructors.newBoolType());
        tokenOnOutgoing.setValue(tokenOnControlflowExpr);
        initialNodeConfig.add(tokenOnOutgoing);

        // If the control flow has a nontrivial incoming guard, add it to the list of extra preconditions.
        AExpression incomingGuard = CifParserHelper.parseIncomingGuard((ControlFlow)outgoing);
        if (incomingGuard != null && !(incomingGuard instanceof ABoolExpression aBoolExpr && aBoolExpr.value)) {
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable();
            cifAlgVar.setName("__initial_node_condition");
            cifAlgVar.setType(CifConstructors.newBoolType());
            cifAlgVar.setValue(translator.translate(incomingGuard));
            initialNodeConfig.add(cifAlgVar);
        }

        return initialNodeConfig;
    }

    /**
     * Create the token configuration for the final node of the activity: the token is placed on the control flow whose
     * target is the final node. Add also its outgoing guards, if present.
     *
     * @param node The UML activity final node.
     * @return The list of CIF algebraic variables representing the final node token configuration.
     */
    private List<AlgVariable> createFinalNodeConfiguration(FinalNode node) {
        List<AlgVariable> finalNodeConfig = new ArrayList<>();

        // Create a new algebraic variable out of the discrete one, for later use in the postconditions.
        Verify.verify(node.getIncomings().size() == 1, "Expected unique incoming control flow to final node.");
        ActivityEdge incoming = node.getIncomings().get(0);
        DiscVariable incomingVariable = controlFlowMap.get(incoming);
        DiscVariableExpression tokenOnControlflowExpr = CifConstructors.newDiscVariableExpression(null,
                EcoreUtil.copy(incomingVariable.getType()), incomingVariable);
        AlgVariable tokenOnIncoming = CifConstructors.newAlgVariable();
        tokenOnIncoming.setName("__token_on_last_controlflow");
        tokenOnIncoming.setType(CifConstructors.newBoolType());
        tokenOnIncoming.setValue(tokenOnControlflowExpr);
        finalNodeConfig.add(tokenOnIncoming);

        // If the control flow has a nontrivial outgoing guard, add it to the list of extra postconditions.
        AExpression outgoingGuard = CifParserHelper.parseOutgoingGuard((ControlFlow)incoming);
        if (outgoingGuard != null && !(outgoingGuard instanceof ABoolExpression aBoolExpr && aBoolExpr.value)) {
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable();
            cifAlgVar.setName("__final_node_condition");
            cifAlgVar.setType(CifConstructors.newBoolType());
            cifAlgVar.setValue(translator.translate(outgoingGuard));
            finalNodeConfig.add(cifAlgVar);
        }

        return finalNodeConfig;
    }

    /** Postcondition kind. */
    public static enum PostConditionKind {
        /**
         * For {@link TranslationPurpose#SYNTHESIS} and {@link TranslationPurpose#LANGUAGE_EQUIVALENCE}, there is only
         * one single kind of postcondition.
         */
        SINGLE("__postcondition"),

        /**
         * For {@link TranslationPurpose#GUARD_COMPUTATION}, the postcondition without activity structure token
         * constraints.
         */
        WITHOUT_STRUCTURE("__postcondition_without_structure"),

        /**
         * For {@link TranslationPurpose#GUARD_COMPUTATION}, the postcondition with activity structure token
         * constraints.
         */
        WITH_STRUCTURE("__postcondition_with_structure");

        /** The prefix of the CIF variable that encodes (part of) the postcondition. */
        protected final String prefix; // POSTCONDITION_PREFIX = "";

        private PostConditionKind(String prefix) {
            this.prefix = prefix;
        }
    }
}
