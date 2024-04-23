
package com.github.tno.pokayoke.transform.uml2gal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.tno.pokayoke.transform.common.FlattenUMLActivity;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

import fr.lip6.move.gal.Assignment;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.GALTypeDeclaration;
import fr.lip6.move.gal.Parameter;
import fr.lip6.move.gal.Specification;
import fr.lip6.move.gal.Statement;
import fr.lip6.move.gal.Transition;
import fr.lip6.move.gal.Variable;

/** Translates annotated UML models to GAL specifications. */
public class Uml2GalTranslator {
    protected GalSpecificationBuilder specificationBuilder;

    protected GalTypeDeclarationBuilder typeBuilder;

    protected CifToGalExpressionTranslator expressionTranslator;

    private Variable initVariable;

    private final Map<ActivityEdge, Variable> edgeMapping = new LinkedHashMap<>();

    private final Map<Variable, Element> variableTracing = new LinkedHashMap<>();

    private final Map<Transition, Element> transitionTracing = new LinkedHashMap<>();

    /**
     * Gives tracing information of how the translated GAL specification relates to the input UML model.
     *
     * @return Tracing infromation, as JSON.
     * @throws JSONException In case generating the tracing JSON failed.
     */
    public JSONObject getTracingAsJson() throws JSONException {
        // Helper function to convert UML comments to JSON arrays.
        Function<Element, JSONArray> convertComments = element -> new JSONArray(
                element.getOwnedComments().stream().map(Comment::getBody).toList());

        // Convert the tracing mappings to JSON objects.
        JSONObject variableTracingJson = new JSONObject(variableTracing.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getName(), e -> convertComments.apply(e.getValue()))));
        JSONObject transitionTracingJson = new JSONObject(transitionTracing.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getName(), e -> convertComments.apply(e.getValue()))));

        // Construct and return the root JSON object.
        JSONObject root = new JSONObject();
        root.put("variables", variableTracingJson);
        root.put("transitions", transitionTracingJson);

        return root;
    }

    /**
     * Translates the given UML model to a GAL specification.
     *
     * @param model The model to translate.
     * @return The translated specification.
     * @throws CoreException Thrown when {@code model} cannot be transformed.
     */
    public Specification translate(Model model) throws CoreException {
        // Validate and flatten the model.
        new FlattenUMLActivity(model).transform();

        // Create GAL specification builders for translating the UML model, and reset from any previous translation.
        specificationBuilder = new GalSpecificationBuilder();
        typeBuilder = new GalTypeDeclarationBuilder();
        initVariable = null;
        edgeMapping.clear();
        variableTracing.clear();
        transitionTracing.clear();

        CifContext cifContext = new CifContext(model);
        expressionTranslator = new CifToGalExpressionTranslator(cifContext, specificationBuilder, typeBuilder);

        // Translate all supported primitive UML types, currently only Booleans (enumerations are translated later).
        specificationBuilder.addTypedef(cifContext.getBooleanType().getName(),
                Uml2GalTranslationHelper.toIntExpression(false), Uml2GalTranslationHelper.toIntExpression(true));

        // Translate the given model by visiting and translating all its elements.
        translateModel(model);

        // Return the translated GAL specification.
        return specificationBuilder.build();
    }

    private void translateModel(Model model) {
        // Visit and translate all enumerations.
        for (PackageableElement element: model.getPackagedElements()) {
            if (element instanceof Enumeration enumeration) {
                // Translate UML enumerations to typedef declarations in GAL with an appropriate range.
                specificationBuilder.addTypedef(enumeration.getName(), 0, enumeration.getOwnedLiterals().size() - 1);

                // Visit and translate all enumeration literals.
                for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
                    // Translate UML enumeration literals to constant specification parameters in GAL.
                    specificationBuilder.addParam(literal.getName(),
                            literal.getEnumeration().getOwnedLiterals().indexOf(literal));
                }
            }
        }

        // Visit nested models and non-activity classes directly within the model.
        for (PackageableElement element: model.getPackagedElements()) {
            if (element instanceof Model nestedModel) {
                translateModel(nestedModel);
            } else if (element instanceof Class classElement && !(element instanceof Activity)) {
                translateClass(classElement);
            }
        }
    }

    private void translateClass(Class classElement) {
        Preconditions.checkArgument(specificationBuilder.getMain() == null,
                "Expected only a single class to be present in the UML model.");
        Preconditions.checkArgument(!(classElement instanceof Activity),
                "Expected the given class to not be an activity.");

        // Define the name of the GAL type to translate the given UML class to.
        typeBuilder.setName(classElement.getQualifiedName().replace("::", "__"));

        // Visit and translate all class properties.
        for (Property property: classElement.getOwnedAttributes()) {
            translateProperty(property);
        }

        // Next we build an initialization transition.
        GalTransitionBuilder initTransitionBuilder = new GalTransitionBuilder();
        initTransitionBuilder.setName("__initialize");

        // Declare the variable that indicates whether the initialization transition for the given class has been taken.
        // The initialization transition itself is constructed later below, and will initialize all variables that do
        // not have a fixed default value, by assigning an arbitrary value to them from the domain indicated by the
        // variable type. Any other transitions can only be taken when the init variable is set to 'true'.
        initVariable = typeBuilder.addVariable("__init", Uml2GalTranslationHelper.toIntExpression(false));

        // The initialization transition can only be taken when initialization has not already happened, and updates the
        // initialization variable to indicate to all transitions that initialization has happened.
        initTransitionBuilder.addEqualityGuard(initVariable, Uml2GalTranslationHelper.toIntExpression(false));
        initTransitionBuilder.addAssignment(initVariable, Uml2GalTranslationHelper.toIntExpression(true));

        // For every class property without default value, define a parameter that ranges over the type domain, as well
        // as an assignment to assign this parameter to the corresponding variable, making its value arbitrary.
        for (Property property: classElement.getOwnedAttributes()) {
            if (property.getDefaultValue() == null) {
                String name = property.getName();
                Parameter parameter = initTransitionBuilder.addParam(name,
                        specificationBuilder.getTypedef(property.getType().getName()));
                initTransitionBuilder.addAssignment(typeBuilder.getVariable(name), parameter);
            }
        }

        // Build the initialization transition and add it to the GAL type declaration.
        typeBuilder.addTransition(initTransitionBuilder.build());

        // Visit and translate the classifier behavior activity of this class.
        translateActivity((Activity)classElement.getClassifierBehavior());

        // Build the GAL type declaration for the given class and add it to the GAL specification.
        GALTypeDeclaration typeDeclaration = typeBuilder.build();
        specificationBuilder.addType(typeDeclaration);
        specificationBuilder.setMain(typeDeclaration);
    }

    private void translateProperty(Property property) {
        String name = property.getName();

        // Make sure the property type has already been translated to GAL.
        String typeName = property.getType().getName();
        Preconditions.checkNotNull(specificationBuilder.getTypedef(typeName), "Undeclared type: " + typeName);

        // In GAL all variable declarations need to have a default value. If the current property has a default value
        // defined, we translate it as part of the variable declaration. Otherwise, we give the variable a (temporary)
        // default value of 'BOOL_FALSE'. The initialization transition will then fix the latter case, by allowing any
        // variable that should not have a default value to have an arbitrary value instead of 'BOOL_FALSE'.
        AExpression defaultValue = CifParserHelper.parseExpression(property.getDefaultValue());

        Variable variable;
        if (defaultValue != null) {
            variable = typeBuilder.addVariable(name, expressionTranslator.translateIntExpr(defaultValue));
        } else {
            variable = typeBuilder.addVariable(name, Uml2GalTranslationHelper.toIntExpression(false));
        }

        // Make sure the created variable can be traced back to the property.
        variableTracing.put(variable, property);
    }

    private void translateActivity(Activity activity) {
        // Visit and translate all activity edges, by creating GAL variables for them.
        for (ActivityEdge edge: activity.getEdges()) {
            // Make sure that edges are only handled once.
            Preconditions.checkArgument(!edgeMapping.containsKey(edge), "Duplicate edge: " + edge);

            // Translate the edge as a GAL variable.
            Variable variable = typeBuilder.addVariable(String.format("__edge__%s", edgeMapping.size()),
                    edge.getSource() instanceof InitialNode ? Uml2GalTranslationHelper.toIntExpression(true)
                            : Uml2GalTranslationHelper.toIntExpression(false));
            edgeMapping.put(edge, variable);

            // Make sure the created variable can be traced back to the edge.
            variableTracing.put(variable, edge);
        }

        // Visit and translate all activity nodes, according to their type (initial nodes are already accounted for).
        for (ActivityNode node: activity.getNodes()) {
            if (node instanceof FinalNode) {
                translateFinalForkOrJoinNode(node);
            } else if (node instanceof ForkNode) {
                translateFinalForkOrJoinNode(node);
            } else if (node instanceof JoinNode) {
                translateFinalForkOrJoinNode(node);
            } else if (node instanceof Action actionNode) {
                translateActionNode(actionNode);
            } else if (node instanceof DecisionNode decisionNode) {
                translateDecisionNode(decisionNode);
            } else if (node instanceof MergeNode mergeNode) {
                translateMergeNode(mergeNode);
            } else if (!(node instanceof InitialNode)) {
                throw new RuntimeException("Unsupported activity node: " + node);
            }
        }
    }

    private void translateFinalForkOrJoinNode(ActivityNode node) {
        typeBuilder.addTransition(translateActivityNode(node, node.getIncomings(), node.getOutgoings(),
                ImmutableList.of(), ImmutableList.of()));
    }

    private void translateActionNode(Action node) {
        // Translate the guards and effects of the given action, and include them in the GAL transition.
        BooleanExpression guard = expressionTranslator.translateBoolExpr(CifParserHelper.parseGuard(node));
        List<Assignment> effects = expressionTranslator.translateAssignments(CifParserHelper.parseEffects(node));

        typeBuilder.addTransition(translateActivityNode(node, node.getIncomings(), node.getOutgoings(),
                ImmutableList.of(guard), effects));
    }

    private void translateDecisionNode(DecisionNode node) {
        // Define a GAL transition for every outgoing edge, so that only one of these edges can be taken.
        for (ActivityEdge outgoingEdge: node.getOutgoings()) {
            // Translate the edge guard and include it in the GAL transition.
            AExpression guardExpr = CifParserHelper.parseExpression(outgoingEdge.getGuard());
            BooleanExpression guard = expressionTranslator.translateBoolExpr(guardExpr);

            typeBuilder.addTransition(translateActivityNode(node, node.getIncomings(), ImmutableList.of(outgoingEdge),
                    ImmutableList.of(guard), ImmutableList.of()));
        }
    }

    private void translateMergeNode(MergeNode node) {
        // Define a GAL transition for every incoming edge, since only one of these edges needs to be enabled.
        for (ActivityEdge incomingEdge: node.getIncomings()) {
            typeBuilder.addTransition(translateActivityNode(node, ImmutableList.of(incomingEdge), node.getOutgoings(),
                    ImmutableList.of(), ImmutableList.of()));
        }
    }

    /**
     * Translates the given UML activity node to a GAL transition.
     *
     * @param node The node to translate.
     * @param incomingEdgesToConsider The subset of incoming edges of the node to consider.
     * @param outgoingEdgesToConsider The subset of outgoing edges of the node to consider.
     * @param guards The guards to add to the translated transition.
     * @param effects The effects to add to the translated transition.
     * @return The translated transition.
     */
    private Transition translateActivityNode(ActivityNode node, Collection<ActivityEdge> incomingEdgesToConsider,
            Collection<ActivityEdge> outgoingEdgesToConsider, Collection<? extends BooleanExpression> guards,
            Collection<? extends Statement> effects)
    {
        Preconditions.checkArgument(node.getIncomings().containsAll(incomingEdgesToConsider),
                "Expected all incoming edges to consider to be actual incoming edges of the given node.");
        Preconditions.checkArgument(node.getOutgoings().containsAll(outgoingEdgesToConsider),
                "Expected all outgoing edges to consider to be actual outgoing edges of the given node.");

        // Define a builder for the GAL transition to translate the given node to.
        GalTransitionBuilder transitionBuilder = new GalTransitionBuilder();

        // Determine the transition name based on the name of the current node.
        String nodeName = node.eClass().getName();
        Verify.verifyNotNull(nodeName, "Expected the type of the given node to have a name.");

        if (!Strings.isNullOrEmpty(node.getName())) {
            nodeName += "_" + node.getName().replace(" ", "_");
        }

        transitionBuilder.setName(String.format("__%s__%s", nodeName, typeBuilder.getTransitionCount()));

        // Define a guard that ensures that the initialization transition has already been taken.
        transitionBuilder.addEqualityGuard(initVariable, Uml2GalTranslationHelper.toIntExpression(true));

        // Add the specified guards and effects to the transition.
        transitionBuilder.addGuards(guards);
        transitionBuilder.addActions(effects);

        // Define a guard for every incoming edge to consider, to check if it is enabled, as well as an assignment to
        // make it disabled after having taken the transition.
        for (ActivityEdge incomingEdge: incomingEdgesToConsider) {
            Variable variable = edgeMapping.get(incomingEdge);
            transitionBuilder.addEqualityGuard(variable, Uml2GalTranslationHelper.toIntExpression(true));
            transitionBuilder.addAssignment(variable, Uml2GalTranslationHelper.toIntExpression(false));
        }

        // Define a guard for every outgoing edge to consider, to check if it is disabled, as well as an assignment to
        // make it enabled after having taken the transition.
        for (ActivityEdge outgoingEdge: outgoingEdgesToConsider) {
            Variable variable = edgeMapping.get(outgoingEdge);
            transitionBuilder.addEqualityGuard(variable, Uml2GalTranslationHelper.toIntExpression(false));
            transitionBuilder.addAssignment(variable, Uml2GalTranslationHelper.toIntExpression(true));
        }

        // Build and return the transition.
        Transition transition = transitionBuilder.build();
        transitionTracing.put(transition, node);
        return transition;
    }
}
