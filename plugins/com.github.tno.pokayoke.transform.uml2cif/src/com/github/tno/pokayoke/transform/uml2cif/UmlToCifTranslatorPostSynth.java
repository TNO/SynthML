
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Assignment;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.pokayoke.transform.common.ValidationHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Verify;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

/** Translates UML synthesis specifications to CIF specifications. */
public class UmlToCifTranslatorPostSynth extends UmlToCifTranslator {
    public static final String INITIAL_NODE_CALLED = "__initial_node_called";

    /** The input UML activity to translate. */
    private Activity activity;

    /** The context that allows querying the UML model of the input UML activity to translate. */
    private final CifContext context;

    /** The translator for UML annotations (guards, updates, invariants, etc.). */
    protected UmlAnnotationsToCif translator;

    /** The translated precondition CIF variable. */
    private AlgVariable preconditionVariable;

    /** The translated postcondition CIF variable. */
    private AlgVariable postconditionVariable;

    /** The mapping from UML enumerations to corresponding translated CIF enumeration declarations. */
    private final BiMap<Enumeration, EnumDecl> enumMap = HashBiMap.create();

    /** The mapping from UML enumeration literals to corresponding translated CIF enumeration literals. */
    private final BiMap<EnumerationLiteral, EnumLiteral> enumLiteralMap = HashBiMap.create();

    /** The mapping from UML properties to corresponding translated CIF discrete variables. */
    private final BiMap<Property, DiscVariable> variableMap = HashBiMap.create();

    /** The mapping from translated CIF start events to their corresponding UML elements for which they were created. */
    private final Map<Event, RedefinableElement> startEventMap = new LinkedHashMap<>();

    private DiscVariable initialNodeCalled = null;

    public UmlToCifTranslatorPostSynth(Activity activity) {
        super(activity);
        this.activity = activity;
        this.context = new CifContext(activity.getModel());
        this.translator = new UmlAnnotationsToCif(context, enumMap, enumLiteralMap, variableMap, startEventMap);
    }

    /**
     * Gives the CIF precondition of the translated activity.
     *
     * @return The CIF precondition of the translated activity.
     */
    @Override
    public Expression getTranslatedPrecondition() {
        Verify.verifyNotNull(preconditionVariable, "Expected a translated precondition CIF variable.");
        return CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), preconditionVariable);
    }

    /**
     * Gives the CIF postcondition of the translated activity.
     *
     * @return The CIF postcondition of the translated activity.
     */
    @Override
    public Expression getTranslatedPostcondition() {
        Verify.verifyNotNull(postconditionVariable, "Expected a translated postcondition CIF variable.");
        return CifConstructors.newAlgVariableExpression(null, CifConstructors.newBoolType(), postconditionVariable);
    }

    /**
     * Translates the UML post-synthesis specifications to a CIF specification. Does not translate opaque behaviors and
     * occurrence constraints of the underlying activity.
     *
     * @return The translated CIF specification.
     * @throws CoreException In case the input UML model is invalid.
     */
    @Override
    public Specification translate() throws CoreException {
        // Validate the UML input model.
        ValidationHelper.validateModel(activity.getModel());

        // Create the CIF specification to which the input UML model will be translated.
        Specification cifSpec = CifConstructors.newSpecification();
        cifSpec.setName("specificationPostSynthesis");

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

        // Add an called initial node variable.
        DiscVariable cifInitialNodeCalledVariable = createCalledInitialNode();
        cifPlant.getDeclarations().add(cifInitialNodeCalledVariable);

        // Create the single location within the CIF plant, which will become a flower automaton.
        Location cifLocation = CifConstructors.newLocation();
        cifLocation.getInitials().add(CifValueUtils.makeTrue());
        cifLocation.getMarkeds().add(CifValueUtils.makeTrue());
        cifPlant.getLocations().add(cifLocation);

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

        // Translate all preconditions of the input UML activity as an initial predicate in CIF.
        Pair<List<AlgVariable>, AlgVariable> preconditions = translatePreconditions();
        cifPlant.getDeclarations().addAll(preconditions.left);
        preconditionVariable = preconditions.right;
        cifPlant.getDeclarations().add(preconditionVariable);
        cifPlant.getInitials().add(getTranslatedPrecondition());

        // Translate all postconditions of the input UML activity as a marked predicate in CIF.
        Pair<List<AlgVariable>, AlgVariable> postconditions = translatePostconditions(cifNonAtomicVars,
                cifAtomicityVar);
        cifPlant.getDeclarations().addAll(postconditions.left);
        postconditionVariable = postconditions.right;
        cifPlant.getDeclarations().add(postconditionVariable);
        cifPlant.getMarkeds().add(getTranslatedPostcondition());

        return cifSpec;
    }

    private DiscVariable createCalledInitialNode() {
        // Create a Boolean CIF variable for the initial node unique call, with default value false.
        DiscVariable cifVariable = CifConstructors.newDiscVariable();
        cifVariable.setName(INITIAL_NODE_CALLED);
        cifVariable.setType(CifConstructors.newBoolType());
        cifVariable.setValue(CifConstructors.newVariableValue(null, ImmutableList.of(CifValueUtils.makeFalse())));
        initialNodeCalled = cifVariable;
        return cifVariable;
    }

    /**
     * Translates a UML activity node to CIF events and corresponding CIF edges.
     *
     * @param node The UML activity node to translate.
     * @return The translated CIF events and corresponding CIF edges as a one-to-one mapping.
     */
    @Override
    protected BiMap<Event, Edge> translateActivityNode(ActivityNode node) {
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
                // Add extra condition for initial node.
                addInitialNodeExtraGuard(cifEdge);
            }
        }

        // If the UML activity node is final, then add the activity postconditions as extra guards for performing the
        // translated CIF start events for the final node.
        if (node instanceof FinalNode)

        {
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

    private void addInitialNodeExtraGuard(Edge edge) {
        // Add a guard expressing that, to start node execution, the initial node should not have been called earlier.
        UnaryExpression startGuard = CifConstructors.newUnaryExpression();
        startGuard.setChild(CifConstructors.newDiscVariableExpression(null, EcoreUtil.copy(initialNodeCalled.getType()),
                initialNodeCalled));
        startGuard.setOperator(UnaryOperator.INVERSE);
        startGuard.setType(CifConstructors.newBoolType());
        edge.getGuards().add(startGuard);

        // Add an update that set the initialization variable to true.
        Assignment update = CifConstructors.newAssignment();
        update.setAddressable(CifConstructors.newDiscVariableExpression(null,
                EcoreUtil.copy(initialNodeCalled.getType()), initialNodeCalled));
        update.setValue(CifValueUtils.makeTrue());
        edge.getUpdates().add(update);
    }
}
