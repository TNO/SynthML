
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.Collection;
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
import org.eclipse.escet.cif.metamodel.cif.expressions.BoolExpression;
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
import com.google.common.collect.Sets;

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

    /** The UML model to translate. */
    private final Model model;

    /** The context that allows querying the input UML model. */
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

    public UmlToCifTranslator(Model model) {
        this.model = model;
        this.context = new CifContext(model);
        this.translator = new UmlAnnotationsToCif(context, enumMap, enumLiteralMap, variableMap, eventMap);
    }

    /**
     * Translates the UML synthesis specifications to a CIF specification.
     *
     * @return The translated CIF specification.
     * @throws CoreException In case the input UML model is invalid.
     */
    public Specification translate() throws CoreException {
        // Validate the UML input model.
        ValidationHelper.validateModel(model);

        // Create the CIF specification to which the input UML model will be translated.
        Specification cifSpec = CifConstructors.newSpecification();
        cifSpec.setName("specification");

        // Translate all UML enumerations.
        for (Enumeration umlEnum: context.getAllEnumerations()) {
            EnumDecl cifEnum = CifConstructors.newEnumDecl(null, null, umlEnum.getName(), null);
            cifSpec.getDeclarations().add(cifEnum);
            enumMap.put(umlEnum, cifEnum);
        }

        // Translate all UML enumeration literals.
        for (EnumerationLiteral umlLiteral: context.getAllEnumerationLiterals()) {
            EnumLiteral cifLiteral = CifConstructors.newEnumLiteral(umlLiteral.getName(), null);
            enumMap.get(umlLiteral.getEnumeration()).getLiterals().add(cifLiteral);
            enumLiteralMap.put(umlLiteral, cifLiteral);
        }

        // Find the single UML class to translate.
        Class umlClass = getSingleClass();

        // Translate the UML class.
        Automaton cifPlant = translateClass(umlClass, cifSpec);
        cifSpec.getComponents().add(cifPlant);

        // Translate all interval constraints of the classifier behavior of the UML class.
        for (Constraint umlConstraint: umlClass.getClassifierBehavior().getOwnedRules()) {
            if (umlConstraint instanceof IntervalConstraint umlIntervalConstraint) {
                List<Automaton> cifRequirements = translateIntervalConstraint(umlIntervalConstraint);
                cifSpec.getComponents().addAll(cifRequirements);
            }
        }

        return cifSpec;
    }

    /**
     * Gives the single UML class within the input UML model.
     *
     * @return The single UML class within the input UML model.
     */
    public Class getSingleClass() {
        List<Class> umlClasses = context.getAllClasses(c -> !(c instanceof Behavior));
        Preconditions.checkArgument(umlClasses.size() == 1, "Expected exactly one class, but got " + umlClasses.size());
        return umlClasses.get(0);
    }

    /**
     * Translates a UML class to a CIF plant automaton.
     *
     * @param umlClass The UML class to translate.
     * @param cifSpec The CIF specification of which the translated CIF plant will be part.
     * @return The translated CIF plant automaton.
     */
    private Automaton translateClass(Class umlClass, Specification cifSpec) {
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
        }

        // Create the single location within the CIF plant, which is a flower automaton.
        Location cifLocation = CifConstructors.newLocation();
        cifLocation.getInitials().add(createBoolExpression(true));
        cifLocation.getMarkeds().add(createBoolExpression(true));
        cifPlant.getLocations().add(cifLocation);

        // Translate all opaque behaviors as CIF event declarations and CIF edges. While doing so, maintain the
        // one-to-one relation between events and flower automaton edges in a mapping. Opaque behaviors that represent
        // non-deterministic or non-atomic actions are translated as multiple CIF start and end events. Therefore a
        // second mapping is maintained to keep track of which such start and end events belong together. We also
        // maintain sets to keep track of the start and end events of all non-atomic and non-deterministic actions, for
        // later use.
        Map<Event, Edge> eventEdgeMap = new LinkedHashMap<>();
        Map<Event, List<Event>> startEndEventMap = new LinkedHashMap<>();

        Set<Event> nonAtomicStartEvents = new LinkedHashSet<>();
        Set<Event> nonAtomicEndEvents = new LinkedHashSet<>();
        Set<Event> nonDeterministicStartEvents = new LinkedHashSet<>();
        Set<Event> nonDeterministicEndEvents = new LinkedHashSet<>();

        for (Behavior umlBehavior: umlClass.getOwnedBehaviors()) {
            if (umlBehavior instanceof OpaqueBehavior umlOpaqueBehavior) {
                // Obtain the guard and effects of the current action. Ensure that there is at least one effect.
                Expression guard = getGuard(umlOpaqueBehavior);
                List<List<Update>> effects = getEffects(umlOpaqueBehavior);

                if (effects.isEmpty()) {
                    effects = List.of(List.of());
                }

                // Create a CIF event for starting the action that is represented by the current opaque behavior.
                Event cifEvent = CifConstructors.newEvent();
                cifEvent.setControllable(true);
                cifEvent.setName(umlOpaqueBehavior.getName());
                cifSpec.getDeclarations().add(cifEvent);
                eventMap.put(umlOpaqueBehavior, cifEvent);

                // Create a CIF edge for this start event.
                EventExpression cifEventExpr = CifConstructors.newEventExpression();
                cifEventExpr.setEvent(cifEvent);
                cifEventExpr.setType(CifConstructors.newBoolType());
                EdgeEvent cifEdgeEvent = CifConstructors.newEdgeEvent();
                cifEdgeEvent.setEvent(cifEventExpr);
                Edge cifEdge = CifConstructors.newEdge();
                cifEdge.getEvents().add(cifEdgeEvent);
                cifEdge.getGuards().add(guard);
                cifLocation.getEdges().add(cifEdge);
                eventEdgeMap.put(cifEvent, cifEdge);

                // Determine whether the action is atomic and/or deterministic.
                boolean isAtomic = PokaYokeUmlProfileUtil.isAtomic(umlOpaqueBehavior);
                boolean isDeterministic = effects.size() == 1;

                if (isAtomic && isDeterministic) {
                    // In case the action is both deterministic and atomic, then the start event also ends the action.
                    // Add its effect as an edge update. (Remember that we ensured that there is at least one effect.)
                    cifEdge.getUpdates().addAll(effects.get(0));
                } else {
                    // In all other cases, add uncontrollable events and edges to end the action.
                    List<Event> cifEndEvents = new ArrayList<>();

                    // Make an uncontrollable event and corresponding edge for every effect (there is at least one).
                    for (int i = 0; i < effects.size(); i++) {
                        // Declare the CIF uncontrollable event.
                        Event cifEndEvent = CifConstructors.newEvent();
                        cifEndEvent.setControllable(false);
                        String outcomeSuffix = isAtomic ? UmlToCifTranslator.ATOMIC_OUTCOME_SUFFIX
                                : UmlToCifTranslator.NONATOMIC_OUTCOME_SUFFIX;
                        cifEndEvent.setName(umlOpaqueBehavior.getName() + outcomeSuffix + (i + 1));
                        cifSpec.getDeclarations().add(cifEndEvent);
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

                    if (!isAtomic) {
                        nonAtomicStartEvents.add(cifEvent);
                        nonAtomicEndEvents.addAll(cifEndEvents);
                    }
                    if (!isDeterministic) {
                        nonDeterministicStartEvents.add(cifEvent);
                        nonDeterministicEndEvents.addAll(cifEndEvents);
                    }
                }
            }
        }

        // In case atomic non-deterministic actions were encountered, encode the necessary atomicity constraints.
        DiscVariable cifAtomicityVar = null;

        Set<Event> nonDeterministicAtomicStartEvents = Sets.difference(nonDeterministicStartEvents,
                nonAtomicStartEvents);
        Set<Event> nonDeterministicAtomicEndEvents = Sets.difference(nonDeterministicEndEvents, nonAtomicEndEvents);

        if (!nonDeterministicAtomicStartEvents.isEmpty()) {
            // Declare a variable that indicates which atomic non-deterministic action is currently active.
            // The value 0 then indicates that no non-deterministic action is currently active.
            cifAtomicityVar = CifConstructors.newDiscVariable();
            cifAtomicityVar.setName(ATOMICITY_VARIABLE_NAME);
            cifAtomicityVar.setType(CifConstructors.newIntType(0, null, nonDeterministicAtomicStartEvents.size()));
            cifPlant.getDeclarations().add(cifAtomicityVar);

            // Define a mapping from (start and end) events that are related to atomic non-deterministic actions, to the
            // index of the atomic non-deterministic action, starting with index 1. So all CIF events related to the
            // first such action get index 1, all events related to the second such action get index 2, etc.
            Map<Event, Integer> eventIndex = new LinkedHashMap<>();
            int index = 1;

            for (Event cifStartEvent: nonDeterministicAtomicStartEvents) {
                eventIndex.put(cifStartEvent, index);

                for (Event cifEndEvent: startEndEventMap.get(cifStartEvent)) {
                    eventIndex.put(cifEndEvent, index);
                }

                index++;
            }

            // Add guards and updates to the edges to ensure that atomic actions are indeed atomically executed.
            for (Entry<Event, Edge> entry: eventEdgeMap.entrySet()) {
                Event cifEvent = entry.getKey();
                Edge cifEdge = entry.getValue();

                // Add guard '__activeAction = 0' for every start event, and every end event of a non-atomic action.
                if (cifEvent.getControllable() || nonAtomicEndEvents.contains(cifEvent)) {
                    BinaryExpression cifGuard = CifConstructors.newBinaryExpression();
                    cifGuard.setLeft(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifGuard.setOperator(BinaryOperator.EQUAL);
                    cifGuard.setRight(CifValueUtils.makeInt(0));
                    cifGuard.setType(CifConstructors.newBoolType());
                    cifEdge.getGuards().add(cifGuard);
                }

                // Add update '__activeAction := i' for every start event of an atomic non-deterministic action.
                if (nonDeterministicAtomicStartEvents.contains(cifEvent)) {
                    Assignment cifUpdate = CifConstructors.newAssignment();
                    cifUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifUpdate.setValue(CifValueUtils.makeInt(eventIndex.get(cifEvent)));
                    cifEdge.getUpdates().add(cifUpdate);
                }

                // Add guard '__activeAction = i' for every end event of an atomic non-deterministic action.
                if (nonDeterministicAtomicEndEvents.contains(cifEvent)) {
                    BinaryExpression cifGuard = CifConstructors.newBinaryExpression();
                    cifGuard.setLeft(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifGuard.setOperator(BinaryOperator.EQUAL);
                    cifGuard.setRight(CifValueUtils.makeInt(eventIndex.get(cifEvent)));
                    cifGuard.setType(CifConstructors.newBoolType());
                    cifEdge.getGuards().add(cifGuard);
                }

                // Add update '__activeAction := 0' for every end event of an atomic non-deterministic action.
                if (nonDeterministicAtomicEndEvents.contains(cifEvent)) {
                    Assignment cifUpdate = CifConstructors.newAssignment();
                    cifUpdate.setAddressable(CifConstructors.newDiscVariableExpression(null,
                            EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
                    cifUpdate.setValue(CifValueUtils.makeInt(0));
                    cifEdge.getUpdates().add(cifUpdate);
                }
            }
        }

        // Add guards and updates to the edges of non-atomic actions to keep track of which such actions are active, and
        // to constrain their start and end events accordingly.
        for (Event cifStartEvent: nonAtomicStartEvents) {
            List<Event> cifEndEvents = startEndEventMap.get(cifStartEvent);

            // Declare a Boolean variable that indicates whether the current non-atomic action is currently active.
            // Value 'false' indicates inactive, and 'true' indicates active.
            DiscVariable cifNonAtomicVar = CifConstructors.newDiscVariable();
            cifNonAtomicVar.setName(NONATOMIC_PREFIX + "__" + cifStartEvent.getName());
            cifNonAtomicVar.setType(CifConstructors.newBoolType());
            cifPlant.getDeclarations().add(cifNonAtomicVar);

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

        // Translate all preconditions of the classifier behavior of the UML class.
        Behavior umlClassifierBehavior = umlClass.getClassifierBehavior();

        Set<AlgVariable> cifPreconditionVars = translatePrePostconditions(umlClassifierBehavior.getPreconditions());

        if (!cifPreconditionVars.isEmpty()) {
            cifPlant.getDeclarations().addAll(cifPreconditionVars);
            AlgVariable cifPreconditionVar = combinePrePostconditionVariables(cifPreconditionVars, "__precondition");
            cifPlant.getDeclarations().add(cifPreconditionVar);
            Expression cifPrecondition = CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(),
                    cifPreconditionVar);
            cifPlant.getInitials().add(cifPrecondition);
        }

        // Translate all postconditions of the classifier behavior of the UML class.
        Set<AlgVariable> cifPostconditionVars = translatePrePostconditions(umlClassifierBehavior.getPostconditions());

        if (cifAtomicityVar != null) {
            // If the atomicity variable has been added, then define an extra postcondition that expresses that no
            // non-deterministic action must be active in order to be in a marked state.

            // First define the atomicity postcondition expression.
            BinaryExpression cifAtomicityPostcondition = CifConstructors.newBinaryExpression();
            cifAtomicityPostcondition.setLeft(CifConstructors.newDiscVariableExpression(null,
                    EcoreUtil.copy(cifAtomicityVar.getType()), cifAtomicityVar));
            cifAtomicityPostcondition.setOperator(BinaryOperator.EQUAL);
            cifAtomicityPostcondition.setRight(CifValueUtils.makeInt(0));
            cifAtomicityPostcondition.setType(CifConstructors.newBoolType());

            // Then define an extra CIF algebraic variable for this atomicity postcondition.
            AlgVariable cifAtomicityAlgVar = CifConstructors.newAlgVariable(null, "__postcondition_atomicity", null,
                    CifConstructors.newBoolType(), cifAtomicityPostcondition);
            cifPostconditionVars.add(cifAtomicityAlgVar);
        }

        AlgVariable cifPostconditionVar = null;

        if (!cifPostconditionVars.isEmpty()) {
            cifPlant.getDeclarations().addAll(cifPostconditionVars);
            cifPostconditionVar = combinePrePostconditionVariables(cifPostconditionVars, "__postcondition");
            cifPlant.getDeclarations().add(cifPostconditionVar);
            Expression cifPostcondition = CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(),
                    cifPostconditionVar);
            cifPlant.getMarkeds().add(cifPostcondition);
        }

        // Create CIF state/event exclusion invariants to disallow further steps from marked states.
        if (cifPostconditionVar != null) {
            for (Event cifEvent: eventEdgeMap.keySet()) {
                Invariant cifInvariant = CifConstructors.newInvariant();
                cifInvariant
                        .setEvent(CifConstructors.newEventExpression(cifEvent, null, CifConstructors.newBoolType()));
                cifInvariant.setInvKind(InvKind.EVENT_DISABLES);
                cifInvariant.setPredicate(CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(),
                        cifPostconditionVar));
                cifInvariant.setSupKind(SupKind.REQUIREMENT);
                cifSpec.getInvariants().add(cifInvariant);
            }
        }

        // Translate all UML class constraints as CIF invariants.
        for (Constraint umlConstraint: umlClass.getOwnedRules()) {
            String constraintName = umlConstraint.getName();

            List<Invariant> cifInvariants = translator.translate(CifParserHelper.parseInvariant(umlConstraint));
            Verify.verify(!cifInvariants.isEmpty(),
                    "Expected at least one translated invariant but got " + cifInvariants.size());

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

            cifPlant.getInvariants().addAll(cifInvariants);
        }

        return cifPlant;
    }

    /**
     * Gives the guard of the given behavior.
     *
     * @param behavior The opaque behavior.
     * @return The guard of the given opaque behavior.
     */
    private Expression getGuard(OpaqueBehavior behavior) {
        AExpression guard = CifParserHelper.parseGuard(behavior);
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
        Map<Event, OpaqueBehavior> inverseEventMap = eventMap.inverse();
        Preconditions.checkArgument(inverseEventMap.containsKey(event),
                "Expected a CIF event that has been translated for some opaque behavior in the input UML model.");
        return getGuard(inverseEventMap.get(event));
    }

    /**
     * Gives all effects of the given behavior. Every effect consists of a list of updates. If there are multiple
     * effects, then the given opaque behavior represents a non-deterministic action.
     *
     * @param behavior The opaque behavior.
     * @return All effects of the given opaque behavior.
     */
    private List<List<Update>> getEffects(OpaqueBehavior behavior) {
        return CifParserHelper.parseEffects(behavior).stream().map(translator::translate).toList();
    }

    /**
     * Translates a given collection of preconditions or postconditions to a set of CIF algebraic variables, one for
     * every pre/postcondition, whose values are the state invariant predicates of the corresponding pre/postcondition.
     *
     * @param umlConstraints The collection of UML pre/postconditions to translate.
     * @return The translated set of Boolean-typed CIF algebraic variables.
     */
    private Set<AlgVariable> translatePrePostconditions(Collection<Constraint> umlConstraints) {
        // Define an algebraic CIF variable for every UML constraint, whose value is the state invariant predicate.
        Set<AlgVariable> cifConstraintVars = new LinkedHashSet<>();

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
     * Translates a UML interval constraint to a list of CIF requirement automata. This translation could result in
     * multiple automata in case the interval constraint constraints more than one UML element.
     *
     * @param umlConstraint The UML interval constraint to translate.
     * @return The translated list of CIF automata.
     */
    private List<Automaton> translateIntervalConstraint(IntervalConstraint umlConstraint) {
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

        // Create the discrete variable that tracks the number of occurrences of the specified event.
        DiscVariable variable = CifConstructors.newDiscVariable();
        CifType variableType = CifConstructors.newIntType(0, null, max);
        variable.setName("__occurrences");
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
        markedExpr.setRight(CifValueUtils.makeInt(min));
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
     * Creates a CIF Boolean expression with the indicated value.
     *
     * @param value The value of the CIF Boolean expression.
     * @return The created CIF Boolean expression.
     */
    private static BoolExpression createBoolExpression(boolean value) {
        return CifConstructors.newBoolExpression(null, CifConstructors.newBoolType(), value);
    }
}
