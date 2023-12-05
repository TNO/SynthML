
package com.github.tno.pokayoke.transform.uml2gal;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.ValueSpecification;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.common.FlattenUMLActivity;
import com.github.tno.pokayoke.transform.common.UMLValidatorSwitch;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;

import fr.lip6.move.gal.Assignment;
import fr.lip6.move.gal.BooleanExpression;
import fr.lip6.move.gal.ConstParameter;
import fr.lip6.move.gal.Constant;
import fr.lip6.move.gal.GALTypeDeclaration;
import fr.lip6.move.gal.IntExpression;
import fr.lip6.move.gal.ParamRef;
import fr.lip6.move.gal.Parameter;
import fr.lip6.move.gal.Specification;
import fr.lip6.move.gal.Statement;
import fr.lip6.move.gal.Transition;
import fr.lip6.move.gal.Variable;

/** Translates annotated UML models to GAL specifications. */
public abstract class Uml2GalTranslator {
    protected static final int BOOL_FALSE = 0;

    protected static final int BOOL_TRUE = 1;

    protected GalSpecificationBuilder specificationBuilder;

    protected GalTypeDeclarationBuilder typeBuilder;

    private Variable initVariable;

    private final Map<ActivityEdge, Variable> edgeMapping = new LinkedHashMap<>();

    private final Map<Variable, Element> variableTracing = new LinkedHashMap<>();

    private final Map<Transition, Element> transitionTracing = new LinkedHashMap<>();

    /**
     * Translates the given expression to a GAL Boolean expression.
     *
     * @param expr The expression to translate.
     * @return The translated GAL Boolean expression.
     */
    protected abstract BooleanExpression translateBoolExpr(String expr);

    /**
     * Translates the given expression to a GAL integer expression.
     *
     * @param expr The expression to translate.
     * @return The translated GAL integer expression.
     */
    protected abstract IntExpression translateIntExpr(String expr);

    /**
     * Translates the given update to a GAL assignment.
     *
     * @param update The update to translate.
     * @return The translated GAL assignment.
     */
    protected abstract Assignment translateAssignment(String update);

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
     */
    public Specification translate(Model model) {
        // Validate and flatten the model.
        new UMLValidatorSwitch().doSwitch(model);
        new FlattenUMLActivity(model).transform();

        // Create GAL specification builders for translating the UML model, and reset from any previous translation.
        specificationBuilder = new GalSpecificationBuilder();
        typeBuilder = new GalTypeDeclarationBuilder();
        initVariable = null;
        edgeMapping.clear();
        variableTracing.clear();
        transitionTracing.clear();

        // Translate all supported primitive UML types, currently only Booleans (enumerations are translated later).
        specificationBuilder.addTypedef(FileHelper.loadPrimitiveType("Boolean").getName(), BOOL_FALSE, BOOL_TRUE);

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
        // variable type. Any other transitions can only be taken when the init variable is set to 'BOOL_TRUE'.
        initVariable = typeBuilder.addVariable("__init", BOOL_FALSE);

        // The initialization transition can only be taken when initialization has not already happened, and updates the
        // initialization variable to indicate to all transitions that initialization has happened.
        initTransitionBuilder.addEqualityGuard(initVariable, BOOL_FALSE);
        initTransitionBuilder.addAssignment(initVariable, BOOL_TRUE);

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
        ValueSpecification defaultValue = property.getDefaultValue();

        Variable variable;
        if (defaultValue != null) {
            variable = typeBuilder.addVariable(name, translateValueSpecificationToInt(defaultValue));
        } else {
            variable = typeBuilder.addVariable(name, BOOL_FALSE);
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
                    edge.getSource() instanceof InitialNode ? BOOL_TRUE : BOOL_FALSE);
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
            } else if (node instanceof OpaqueAction actionNode) {
                translateOpaqueActionNode(actionNode);
            } else if (node instanceof DecisionNode decisionNode) {
                translateDecisionNode(decisionNode);
            } else if (node instanceof MergeNode mergeNode) {
                translateMergeNode(mergeNode);
            }
        }
    }

    private void translateFinalForkOrJoinNode(ActivityNode node) {
        typeBuilder.addTransition(translateActivityNode(node, node.getIncomings(), node.getOutgoings(),
                ImmutableList.of(), ImmutableList.of()));
    }

    private void translateOpaqueActionNode(OpaqueAction node) {
        // Translate the guards and effects of the given action, and include them in the GAL transition.
        List<BooleanExpression> guards = node.getBodies().stream().limit(1).map(this::translateBoolExpr).toList();
        List<Assignment> effects = node.getBodies().stream().skip(1).map(this::translateAssignment).toList();

        typeBuilder
                .addTransition(translateActivityNode(node, node.getIncomings(), node.getOutgoings(), guards, effects));
    }

    private void translateDecisionNode(DecisionNode node) {
        // Define a GAL transition for every outgoing edge, so that only one of these edges can be taken.
        for (ActivityEdge outgoingEdge: node.getOutgoings()) {
            // Translate the edge guard and include it in the GAL transition.
            BooleanExpression guard = translateValueSpecificationToBoolean(outgoingEdge.getGuard());

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
        transitionBuilder.addEqualityGuard(initVariable, BOOL_TRUE);

        // Add the specified guards and effects to the transition.
        transitionBuilder.addGuards(guards);
        transitionBuilder.addActions(effects);

        // Define a guard for every incoming edge to consider, to check if it is enabled, as well as an assignment to
        // make it disabled after having taken the transition.
        for (ActivityEdge incomingEdge: incomingEdgesToConsider) {
            Variable variable = edgeMapping.get(incomingEdge);
            transitionBuilder.addEqualityGuard(variable, BOOL_TRUE);
            transitionBuilder.addAssignment(variable, BOOL_FALSE);
        }

        // Define a guard for every outgoing edge to consider, to check if it is disabled, as well as an assignment to
        // make it enabled after having taken the transition.
        for (ActivityEdge outgoingEdge: outgoingEdgesToConsider) {
            Variable variable = edgeMapping.get(outgoingEdge);
            transitionBuilder.addEqualityGuard(variable, BOOL_FALSE);
            transitionBuilder.addAssignment(variable, BOOL_TRUE);
        }

        // Build and return the transition.
        Transition transition = transitionBuilder.build();
        transitionTracing.put(transition, node);
        return transition;
    }

    private BooleanExpression translateValueSpecificationToBoolean(ValueSpecification specification) {
        if (specification instanceof LiteralBoolean literal) {
            return translateLiteralBooleanToBoolean(literal);
        } else if (specification instanceof OpaqueExpression expr) {
            return translateOpaqueExpressionToBoolean(expr);
        } else {
            throw new RuntimeException("Unsupported value specification: " + specification);
        }
    }

    private BooleanExpression translateLiteralBooleanToBoolean(LiteralBoolean literal) {
        return literal.isValue() ? Uml2GalTranslationHelper.FACTORY.createTrue()
                : Uml2GalTranslationHelper.FACTORY.createFalse();
    }

    private BooleanExpression translateOpaqueExpressionToBoolean(OpaqueExpression expr) {
        return Uml2GalTranslationHelper.combineAsAnd(expr.getBodies().stream().map(this::translateBoolExpr).toList());
    }

    private IntExpression translateValueSpecificationToInt(ValueSpecification specification) {
        if (specification instanceof LiteralBoolean literal) {
            return translateLiteralBooleanToInt(literal);
        } else if (specification instanceof LiteralInteger literal) {
            return translateLiteralIntegerToInt(literal);
        } else if (specification instanceof InstanceValue value) {
            return translateInstanceValueToInt(value);
        } else if (specification instanceof OpaqueExpression expr) {
            return translateOpaqueExpressionToInt(expr);
        } else {
            throw new RuntimeException("Unsupported value specification: " + specification);
        }
    }

    private Constant translateLiteralBooleanToInt(LiteralBoolean literal) {
        Constant constant = Uml2GalTranslationHelper.FACTORY.createConstant();
        constant.setValue(literal.isValue() ? BOOL_TRUE : BOOL_FALSE);
        return constant;
    }

    private Constant translateLiteralIntegerToInt(LiteralInteger literal) {
        Constant constant = Uml2GalTranslationHelper.FACTORY.createConstant();
        constant.setValue(literal.getValue());
        return constant;
    }

    private IntExpression translateOpaqueExpressionToInt(OpaqueExpression expr) {
        Preconditions.checkArgument(expr.getBodies().size() == 1, "Expected a single opaque expression body.");
        return translateIntExpr(expr.getBodies().get(0));
    }

    private IntExpression translateInstanceValueToInt(InstanceValue value) {
        return translateInstanceSpecificationToInt(value.getInstance());
    }

    private IntExpression translateInstanceSpecificationToInt(InstanceSpecification specification) {
        if (specification instanceof EnumerationLiteral literal) {
            return translateEnumerationLiteralToInt(literal);
        } else {
            throw new RuntimeException("Unsupported instance specification: " + specification);
        }
    }

    private ParamRef translateEnumerationLiteralToInt(EnumerationLiteral literal) {
        ConstParameter param = specificationBuilder.getParam(literal.getName());
        Preconditions.checkNotNull(param, "Expected the enumeration literal to have a corresponding parameter.");
        ParamRef paramRef = Uml2GalTranslationHelper.FACTORY.createParamRef();
        paramRef.setRefParam(param);
        return paramRef;
    }
}
