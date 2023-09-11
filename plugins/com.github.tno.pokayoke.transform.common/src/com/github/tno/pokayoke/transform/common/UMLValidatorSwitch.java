
package com.github.tno.pokayoke.transform.common;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, EnumerationLiteral> enumLiterals = new LinkedHashMap<>();

    @Override
    public Object caseModel(Model model) {
        checkValidityOf(model.getName());

        // Visit all packaged elements and check (local) uniqueness of their names.
        Set<String> names = new LinkedHashSet<>();

        for (PackageableElement element: model.getPackagedElements()) {
            Object visistedElement = doSwitch(element);
            Verify.verifyNotNull(visistedElement, "Unsupported packageable element: " + element);
            String elementName = element.getName();
            Verify.verify(!names.contains(elementName), "Duplicate name: " + elementName);
            names.add(elementName);
        }

        return model;
    }

    @Override
    public Object caseClass(Class classElement) {
        checkValidityOf(classElement.getName());

        Preconditions.checkArgument(classElement.getNestedClassifiers().isEmpty(),
                "Expected classes to not contain any nested classifiers.");
        Preconditions.checkNotNull(classElement.getClassifierBehavior(),
                "Expected classes to have a classifier behavior.");
        Preconditions.checkArgument(classElement.getOwnedBehaviors().contains(classElement.getClassifierBehavior()),
                "Expected classes to own their classifier behavior.");

        // Visit all class properties and check (local) uniqueness of their names.
        Set<String> names = new LinkedHashSet<>();

        for (Property property: classElement.getOwnedAttributes()) {
            Object visitedProperty = doSwitch(property);
            Verify.verifyNotNull(visitedProperty, "Unsupported class property: " + property);
            String propertyName = property.getName();
            Verify.verify(!names.contains(propertyName), "Duplicate name: " + propertyName);
            names.add(propertyName);
        }

        // Visit all class behaviors and check (local) uniqueness of their names.
        for (Behavior behavior: classElement.getOwnedBehaviors()) {
            Object visitedBehavior = doSwitch(behavior);
            Verify.verifyNotNull(visitedBehavior, "Unsupported class behavior: " + behavior);
            String behaviorName = behavior.getName();
            Verify.verify(!names.contains(behaviorName), "Duplicate name: " + behaviorName);
            names.add(behaviorName);
        }

        return classElement;
    }

    @Override
    public Object caseProperty(Property property) {
        checkValidityOf(property.getName());

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
        checkValidityOf(enumeration.getName());

        Preconditions.checkArgument(enumeration.eContainer() instanceof Model,
                "Expected enumerations to be declared in models.");
        Preconditions.checkArgument(enumeration.eContainer().eContainer() == null,
                "Expected enumerations to be declared on the outer-most level.");
        Preconditions.checkArgument(!enumeration.getOwnedLiterals().isEmpty(),
                "Expected enumerations to have at least one literal.");

        // Visit all enumeration literals.
        for (EnumerationLiteral literal: enumeration.getOwnedLiterals()) {
            Object visitedLiteral = doSwitch(literal);
            Verify.verifyNotNull(visitedLiteral, "Unsupported enumeration literal: " + literal);
        }

        return enumeration;
    }

    @Override
    public Object caseEnumerationLiteral(EnumerationLiteral literal) {
        String literalName = literal.getName();

        checkValidityOf(literalName);

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
        Preconditions.checkArgument(expression.getBodies().size() == 1,
                "Expected opaque expressions to have exactly one expression body.");
        return expression;
    }

    @Override
    public Object caseActivity(Activity activity) {
        checkValidityOf(activity.getName());

        Preconditions.checkArgument(activity.getMembers().isEmpty(), "Expected activities to not have any members.");
        Preconditions.checkArgument(activity.getClassifierBehavior() == null,
                "Expected activities to not have a classifier behavior.");
        Preconditions.checkArgument(activity.getNodes().stream().filter(n -> n instanceof InitialNode).count() == 1,
                "Expected activities to have exactly one initial node.");

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
        String nodeName = node.getName();
        if (nodeName != null) {
            checkValidityOf(nodeName);
        }

        return node;
    }

    @Override
    public Object caseFinalNode(FinalNode node) {
        String nodeName = node.getName();
        if (nodeName != null) {
            checkValidityOf(nodeName);
        }

        return node;
    }

    @Override
    public Object caseForkNode(ForkNode node) {
        String nodeName = node.getName();
        if (nodeName != null) {
            checkValidityOf(nodeName);
        }

        return node;
    }

    @Override
    public Object caseJoinNode(JoinNode node) {
        String nodeName = node.getName();
        if (nodeName != null) {
            checkValidityOf(nodeName);
        }

        return node;
    }

    @Override
    public Object caseDecisionNode(DecisionNode node) {
        String nodeName = node.getName();
        if (nodeName != null) {
            checkValidityOf(nodeName);
        }

        return node;
    }

    @Override
    public Object caseMergeNode(MergeNode node) {
        String nodeName = node.getName();
        if (nodeName != null) {
            checkValidityOf(nodeName);
        }

        return node;
    }

    @Override
    public Object caseCallBehaviorAction(CallBehaviorAction action) {
        Preconditions.checkNotNull(action.getBehavior(),
                "Expected the called behavior of call behavior actions to be non-null.");
        return action;
    }

    @Override
    public Object caseOpaqueAction(OpaqueAction action) {
        checkValidityOf(action.getName());
        return action;
    }

    @Override
    public Object caseControlFlow(ControlFlow edge) {
        Preconditions.checkNotNull(edge.getSource(), "Expected a non-null source node.");
        Preconditions.checkNotNull(edge.getTarget(), "Expected a non-null target node.");

        String edgeName = edge.getName();
        if (edgeName != null) {
            checkValidityOf(edgeName);
        }

        return edge;
    }

    protected void checkValidityOf(String name) {
        Preconditions.checkNotNull(name, "Expected a non-null name.");
        Preconditions.checkArgument(!name.contains("__"), "Expected names to not contain '__'.");
    }
}
