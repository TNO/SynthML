
package com.github.tno.pokayoke.transform.common;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.FinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.InstanceSpecification;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.util.UMLSwitch;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/** Visits UML elements and checks whether their structure is as expected. */
public class UMLValidatorSwitch extends UMLSwitch<Object> {
    private final Stack<Set<String>> elementNames = new Stack<>();

    private final Map<String, EnumerationLiteral> enumLiterals = new LinkedHashMap<>();

    @Override
    public Object caseModel(Model model) {
        checkNonNullNameOf(model);
        checkNamingConventions(model, true, true);

        // Visit all packaged elements and check (local) uniqueness of their names.
        elementNames.push(new LinkedHashSet<>());

        for (PackageableElement element: model.getPackagedElements()) {
            registerUniqueElementName(element.getName());
        }

        for (PackageableElement element: model.getPackagedElements()) {
            Object visistedElement = doSwitch(element);
            Verify.verifyNotNull(visistedElement, "Unsupported packageable element: " + element);
        }

        elementNames.pop();

        return model;
    }

    @Override
    public Object caseClass(Class classElement) {
        checkNonNullNameOf(classElement);
        checkNamingConventions(classElement, true, true);

        Preconditions.checkArgument(classElement.getNestedClassifiers().isEmpty(),
                String.format("Expected classes to not contain any nested classifiers. Violated by class '%s'.",
                        classElement.getName()));
        Preconditions.checkNotNull(classElement.getClassifierBehavior(), String.format(
                "Expected classes to have a classifier behavior. Violated by class '%s'.", classElement.getName()));
        Preconditions.checkArgument(classElement.getOwnedBehaviors().contains(classElement.getClassifierBehavior()),
                String.format("Expected classes to own their classifier behavior. Violated by class '%s'.",
                        classElement.getName()));

        // Visit all class properties and check (local) uniqueness of their names.
        elementNames.push(new LinkedHashSet<>());

        for (Property property: classElement.getOwnedAttributes()) {
            Object visitedProperty = doSwitch(property);
            Verify.verifyNotNull(visitedProperty, "Unsupported class property: " + property);
            registerUniqueElementName(property.getName());
        }

        // Visit all class behaviors and check (local) uniqueness of their names.
        for (Behavior behavior: classElement.getOwnedBehaviors()) {
            Object visitedBehavior = doSwitch(behavior);
            Verify.verifyNotNull(visitedBehavior, "Unsupported class behavior: " + behavior);
            registerUniqueElementName(behavior.getName());
        }

        elementNames.pop();

        return classElement;
    }

    @Override
    public Object caseProperty(Property property) {
        checkNonNullNameOf(property);
        checkNamingConventions(property, true, true);

        // Visit the property type.
        Type propertyType = property.getType();
        Object visitedType = doSwitch(propertyType);
        Verify.verifyNotNull(visitedType, "Unsupported property type: " + propertyType);

        // Visit the default property value if set.
        ValueSpecification defaultValue = property.getDefaultValue();

        if (defaultValue != null) {
            Object visitedDefaultValue = doSwitch(defaultValue);
            Verify.verifyNotNull(visitedDefaultValue, "Unsupported default property value: " + defaultValue);
        }

        return property;
    }

    @Override
    public Object casePrimitiveType(PrimitiveType primitiveType) {
        Preconditions.checkArgument(primitiveType.getName().equals("Boolean"),
                "Unsupported primitive type: " + primitiveType);
        return primitiveType;
    }

    @Override
    public Object caseEnumeration(Enumeration enumeration) {
        checkNonNullNameOf(enumeration);
        checkNamingConventions(enumeration, true, true);

        Preconditions.checkArgument(enumeration.eContainer() instanceof Model,
                String.format("Expected enumerations to be declared in models. Violated by enumeration '%s'.",
                        enumeration.getName()));
        Preconditions.checkArgument(enumeration.eContainer().eContainer() == null,
                String.format(
                        "Expected enumerations to be declared on the outer-most level. Violated by enumeration '%s'.",
                        enumeration.getName()));
        Preconditions.checkArgument(!enumeration.getOwnedLiterals().isEmpty(),
                String.format("Expected enumerations to have at least one literal. Violated by enumeration '%s'.",
                        enumeration.getName()));

        // Visit all enumeration literals.
        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            Object visitedLiteral = doSwitch(literal);
            Verify.verifyNotNull(visitedLiteral, "Unsupported enumeration literal: " + literal);
        }

        return enumeration;
    }

    @Override
    public Object caseEnumerationLiteral(EnumerationLiteral literal) {
        checkNonNullNameOf(literal);
        checkNamingConventions(literal, true, true);

        String literalName = literal.getName();

        // Ensure that the literal uses a unique name.
        EnumerationLiteral existingLiteral = enumLiterals.put(literalName, literal);
        if (existingLiteral != null) {
            Verify.verify(existingLiteral.equals(literal), "Duplicate enum literal: " + literalName);
        }

        return literal;
    }

    @Override
    public Object caseInstanceValue(InstanceValue instanceValue) {
        // Visit the instance specification.
        InstanceSpecification instanceSpecification = instanceValue.getInstance();
        Object visitedInstanceSpecification = doSwitch(instanceSpecification);
        Verify.verifyNotNull(visitedInstanceSpecification,
                "Unsupported instance specification: " + instanceSpecification);

        return instanceValue;
    }

    @Override
    public Object caseLiteralBoolean(LiteralBoolean literal) {
        return literal;
    }

    @Override
    public Object caseOpaqueExpression(OpaqueExpression expression) {
        checkNamingConventions(expression, true, false);
        Preconditions.checkArgument(expression.getBodies().size() == 1,
                "Expected opaque expressions to have exactly one expression body.");
        return expression;
    }

    @Override
    public Object caseActivity(Activity activity) {
        checkNonNullNameOf(activity);
        checkNamingConventions(activity, true, true);

        Preconditions.checkArgument(activity.getMembers().isEmpty(), String
                .format("Expected activities to not have any members. Violated by activity '%s'.", activity.getName()));
        Preconditions.checkArgument(activity.getClassifierBehavior() == null,
                String.format("Expected activities to not have a classifier behavior. Violated by activity '%s'.",
                        activity.getName()));
        Preconditions.checkArgument(activity.getNodes().stream().filter(n -> n instanceof InitialNode).count() == 1,
                String.format("Expected activities to have exactly one initial node. Violated by activity '%s'.",
                        activity.getName()));

        // Visit all activity nodes.
        for (ActivityNode node: activity.getNodes()) {
            Object visitedNode = doSwitch(node);
            Verify.verifyNotNull(visitedNode, "Unsupported activity node: " + node);
        }

        // Visit all activity edges.
        for (ActivityEdge edge: activity.getEdges()) {
            Object visitedEdge = doSwitch(edge);
            Verify.verifyNotNull(visitedEdge, "Unsupported activity edge: " + edge);
        }

        return activity;
    }

    @Override
    public Object caseInitialNode(InitialNode node) {
        checkNamingConventions(node, true, true);
        return node;
    }

    @Override
    public Object caseFinalNode(FinalNode node) {
        checkNamingConventions(node, true, true);
        return node;
    }

    @Override
    public Object caseForkNode(ForkNode node) {
        checkNamingConventions(node, true, true);
        return node;
    }

    @Override
    public Object caseJoinNode(JoinNode node) {
        checkNamingConventions(node, true, true);
        return node;
    }

    @Override
    public Object caseDecisionNode(DecisionNode node) {
        checkNamingConventions(node, true, true);
        return node;
    }

    @Override
    public Object caseMergeNode(MergeNode node) {
        checkNamingConventions(node, true, true);
        return node;
    }

    @Override
    public Object caseCallBehaviorAction(CallBehaviorAction action) {
        checkNamingConventions(action, true, true);
        Preconditions.checkNotNull(action.getBehavior(), String.format(
                "Expected the called behavior of call behavior actions to be non-null. Violated by action '%s'.",
                action.getName()));
        Preconditions.checkArgument(action.getBehavior() instanceof Activity,
                String.format(
                        "Expected the behavior of any call behavior action to be an activity. Violated by action '%s'.",
                        action.getName()));
        return action;
    }

    @Override
    public Object caseOpaqueAction(OpaqueAction action) {
        checkNamingConventions(action, true, true);
        return action;
    }

    @Override
    public Object caseControlFlow(ControlFlow edge) {
        checkNamingConventions(edge, true, false);
        Preconditions.checkNotNull(edge.getSource(),
                String.format("Expected a non-null source node. Violated by edge '%s'.", edge.getName()));
        Preconditions.checkNotNull(edge.getTarget(),
                String.format("Expected a non-null target node. Violated by edge '%s'.", edge.getName()));

        ValueSpecification guard = edge.getGuard();
        if (guard != null) {
            doSwitch(guard);
        }

        return edge;
    }

    protected void checkNonNullNameOf(NamedElement element) {
        Preconditions.checkNotNull(element.getName(),
                String.format("Expected the given %s to have a non-null name.", element.eClass().getName()));
    }

    protected void checkNamingConventions(NamedElement element, boolean checkDoubleUnderscore,
            boolean checkProperIdentifierName)
    {
        String name = element.getName();

        if (name != null && !name.isEmpty()) {
            if (checkDoubleUnderscore) {
                Preconditions.checkArgument(!name.contains("__"),
                        String.format("Expected the name of the given %s to not contain '__', but got '%s'.",
                                element.eClass().getName(), name));
            }
            if (checkProperIdentifierName) {
                String pattern = "^[a-zA-Z_][0-9a-zA-Z_]*$";
                Pattern regex = Pattern.compile(pattern);
                Matcher matcher = regex.matcher(name);

                Preconditions.checkArgument(matcher.matches(), String.format(
                        "Expected the name of the given %s to start with [a-zA-Z_] and then be followed by [0-9a-zA-Z_]*, but got '%s'.",
                        element.eClass().getName(), name));
            }
        }
    }

    private void registerUniqueElementName(String name) {
        Verify.verify(elementNames.stream().noneMatch(names -> names.contains(name)), "Duplicate name: " + name);
        elementNames.peek().add(name);
    }
}
