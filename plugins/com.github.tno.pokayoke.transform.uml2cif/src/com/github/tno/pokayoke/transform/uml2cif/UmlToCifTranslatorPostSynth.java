
package com.github.tno.pokayoke.transform.uml2cif;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.escet.cif.common.CifValueUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.SupKind;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.AlgVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumLiteral;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.BinaryOperator;
import org.eclipse.escet.cif.metamodel.cif.expressions.DiscVariableExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.UnaryOperator;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;

import com.github.tno.pokayoke.transform.common.ValidationHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

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

    /** The list containing the token configuration related to the initial node. */
    private List<AlgVariable> initialNodeConfig = new ArrayList<>();

    /** The list containing the token configuration related to the final node. */
    private List<AlgVariable> finalNodeConfig = new ArrayList<>();

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
     * Translates the UML post-synthesis specifications to a CIF specification. Does not translate opaque behaviors of
     * the underlying activity.
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

        // Create the CIF plant for the UML activity to translate. Keeping the same activity name is needed for the
        // language equivalence check.
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

    /**
     * Translates all concrete UML activities that are in context to CIF variables, and CIF events with their
     * corresponding CIF edges.
     *
     * @return The translated CIF variables, and CIF events with their corresponding CIF edges.
     */
    @Override
    protected Pair<Set<DiscVariable>, BiMap<Event, Edge>> translateActivities() {
        Set<DiscVariable> newVariables = new LinkedHashSet<>();
        BiMap<Event, Edge> newEventEdges = HashBiMap.create();

        // Translate all concrete activities that are in context.
        for (Activity activity: context.getAllConcreteActivities()) {
            if (activity.equals(this.activity)) {
                Pair<Set<DiscVariable>, BiMap<Event, Edge>> result = translatePostSynthChainConcreteActivity(activity);
                newVariables.addAll(result.left);
                newEventEdges.putAll(result.right);
            } else {
                Pair<Set<DiscVariable>, BiMap<Event, Edge>> result = translateConcreteActivity(activity);
                newVariables.addAll(result.left);
                newEventEdges.putAll(result.right);
            }
        }

        return Pair.pair(newVariables, newEventEdges);
    }

    /**
     * Translates the post-synthesis-chain concrete UML activity to CIF variables, and CIF events with their
     * corresponding CIF edges. The initial and final node are not translated, since we place the first token after the
     * initial node, and the last token before the final node. Their guards are added to the preconditions and
     * postconditions, respectively.
     *
     * @param activity The concrete UML activity to translate.
     * @return The translated CIF variables, and CIF events with their corresponding CIF edges.
     */
    protected Pair<Set<DiscVariable>, BiMap<Event, Edge>> translatePostSynthChainConcreteActivity(Activity activity) {
        Preconditions.checkArgument(!activity.isAbstract(), "Expected a concrete activity.");

        // Translate all activity control flows.
        Set<DiscVariable> newVariables = new LinkedHashSet<>(activity.getEdges().size());
        for (ActivityEdge controlFlow: activity.getEdges()) {
            DiscVariable cifControlFlowVar = translateActivityControlFlow(controlFlow);
            if (controlFlow.getSource() instanceof InitialNode) {
                cifControlFlowVar.setValue(CifConstructors.newVariableValue(null, List.of(CifValueUtils.makeTrue())));
            }
            newVariables.add(cifControlFlowVar);
        }

        // Translate all activity nodes but the initial and final node. Computes the configurations for the initial and
        // final nodes.
        BiMap<Event, Edge> newEventEdges = HashBiMap.create(activity.getNodes().size());
        for (ActivityNode node: activity.getNodes()) {
            if (node instanceof InitialNode initialNode) {
                createInitialNodeConfiguration(initialNode);
            } else if (node instanceof FinalNode finalNode) {
                createFinalNodeConfiguration(finalNode);
            } else {
                newEventEdges.putAll(translateActivityNode(node));
            }
        }

        return Pair.pair(newVariables, newEventEdges);
    }

    /**
     * Translates the UML activity preconditions to a CIF algebraic variable. Adds the extra initial configuration
     * conditions.
     *
     * @return A pair consisting of auxiliary CIF algebraic variables that encode parts of the precondition, together
     *     with the CIF algebraic variable that encodes the entire precondition.
     */
    @Override
    protected Pair<List<AlgVariable>, AlgVariable> translatePreconditions() {
        List<AlgVariable> preconditionVars = translatePrePostconditions(activity.getPreconditions());

        // Add the initial node configuration.
        preconditionVars.addAll(initialNodeConfig);

        // Combine the activity preconditions with initial node preconditions.
        AlgVariable preconditionVar = combinePrePostconditionVariables(preconditionVars, PRECONDITION_PREFIX);
        return Pair.pair(preconditionVars, preconditionVar);
    }

    /**
     * Translates the UML activity postconditions to a CIF algebraic variable. Extra postconditions are added expressing
     * that no non-atomic and atomic non-deterministic actions may be active, and that all occurrence constraints must
     * be satisfied. Differently from the parent method, there must be a token on the incoming edge of the final node,
     * and nowhere else.
     *
     * @param cifNonAtomicVars The internal CIF variables created for non-atomic actions.
     * @param cifAtomicityVar The internal CIF variable created for atomic non-deterministic actions. Is {@code null} if
     *     no atomic non-deterministic actions are present.
     * @return A pair consisting of auxiliary CIF algebraic variables that encode parts of the postcondition, together
     *     with the CIF algebraic variable that encodes the entire postcondition.
     */
    @Override
    protected Pair<List<AlgVariable>, AlgVariable> translatePostconditions(List<DiscVariable> cifNonAtomicVars,
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
        // the control flow must not hold a token. Exclude the token on the final node incoming edge.
        for (var entry: controlFlowMap.entrySet()) {
            if (!(entry.getKey().getTarget() instanceof FinalNode)) {
                DiscVariable cifControlFlowVar = entry.getValue();

                // First define the postcondition expression.
                UnaryExpression cifExtraPostcondition = CifConstructors.newUnaryExpression();
                cifExtraPostcondition.setChild(CifConstructors.newDiscVariableExpression(null,
                        CifConstructors.newBoolType(), cifControlFlowVar));
                cifExtraPostcondition.setOperator(UnaryOperator.INVERSE);
                cifExtraPostcondition.setType(CifConstructors.newBoolType());

                // Then define an extra CIF algebraic variable for the extra postcondition.
                AlgVariable cifAlgVar = CifConstructors.newAlgVariable(null,
                        POSTCONDITION_PREFIX + cifControlFlowVar.getName(), null, CifConstructors.newBoolType(),
                        cifExtraPostcondition);
                postconditionVars.add(cifAlgVar);
            }
        }

        // Add the final node configuration.
        postconditionVars.addAll(finalNodeConfig);

        // Combine all defined postcondition variables to a single algebraic postcondition variable, whose value is the
        // conjunction of all these defined postcondition variables (which are all Boolean typed).
        AlgVariable postconditionVar = combinePrePostconditionVariables(postconditionVars, POSTCONDITION_PREFIX);

        return Pair.pair(postconditionVars, postconditionVar);
    }

    /**
     * Create the token configuration for the initial node of the activity: the token is placed on the control flow
     * whose source is the initial node. Add also its incoming guards, if present. Assumes the initial node has a unique
     * outgoing control flow, as per the validation model requirement.
     *
     * @param node The UML activity initial node.
     */
    protected void createInitialNodeConfiguration(InitialNode node) {
        // Create a new algebraic variable out of the discrete one, for later use in the preconditions.
        Verify.verify(node.getOutgoings().size() == 1, "Expected unique outgoing control flow from initial node.");
        ActivityEdge outgoing = node.getOutgoings().get(0);
        DiscVariable outgoingVariable = controlFlowMap.get(outgoing);
        DiscVariableExpression tokenOnControlflowExpr = CifConstructors.newDiscVariableExpression(null,
                EcoreUtil.copy(outgoingVariable.getType()), outgoingVariable);
        AlgVariable tokenOnOutgoing = CifConstructors.newAlgVariable();
        tokenOnOutgoing.setName("token_on_first_controlflow");
        tokenOnOutgoing.setType(CifConstructors.newBoolType());
        tokenOnOutgoing.setValue(tokenOnControlflowExpr);
        initialNodeConfig.add(tokenOnOutgoing);

        // If the control flow has an incoming guard, add it to the list of extra preconditions.
        if (PokaYokeUmlProfileUtil.getIncomingGuard((ControlFlow)outgoing) != null) {
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable();
            cifAlgVar.setName(node.getName());
            cifAlgVar.setType(CifConstructors.newBoolType());
            cifAlgVar.setValue(translator.translate(CifParserHelper.parseIncomingGuard((ControlFlow)outgoing)));
            initialNodeConfig.add(cifAlgVar);
        }
    }

    /**
     * Create the token configuration for the final node of the activity: the token is placed on the control flow whose
     * target is the final node. Add also its outgoing guards, if present. Assumes the final node has a unique incoming
     * control flow, as per the validation model requirement.
     *
     * @param node The UML activity final node.
     */
    protected void createFinalNodeConfiguration(FinalNode node) {
        // Create a new algebraic variable out of the discrete one, for later use in the postconditions.
        ActivityEdge incoming = node.getIncomings().get(0);
        DiscVariable incomingVariable = controlFlowMap.get(incoming);
        DiscVariableExpression tokenOnControlflowExpr = CifConstructors.newDiscVariableExpression(null,
                EcoreUtil.copy(incomingVariable.getType()), incomingVariable);
        AlgVariable tokenOnOutgoing = CifConstructors.newAlgVariable();
        tokenOnOutgoing.setName("token_on_last_controlflow");
        tokenOnOutgoing.setType(CifConstructors.newBoolType());
        tokenOnOutgoing.setValue(tokenOnControlflowExpr);
        finalNodeConfig.add(tokenOnOutgoing);

        // If the control flow has an incoming guard, add it to the list of extra postconditions.
        if (PokaYokeUmlProfileUtil.getOutgoingGuard((ControlFlow)incoming) != null) {
            AlgVariable cifAlgVar = CifConstructors.newAlgVariable();
            cifAlgVar.setName(node.getName());
            cifAlgVar.setType(CifConstructors.newBoolType());
            cifAlgVar.setValue(translator.translate(CifParserHelper.parseIncomingGuard((ControlFlow)incoming)));
            finalNodeConfig.add(cifAlgVar);
        }
    }
}
