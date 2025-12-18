////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023-2025 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available under the terms of the
// Eclipse Public License v2.0 which accompanies this distribution, and is available at
// https://spdx.org/licenses/EPL-2.0.html
//
// SPDX-License-Identifier: EPL-2.0
////////////////////////////////////////////////////////////////////////////////////////

package com.github.tno.synthml.uml.profile.validation;

import static org.eclipse.lsat.common.queries.QueryableIterable.from;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.ast.AInvariant;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AElifUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AIfUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AIntExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.common.java.Lists;
import org.eclipse.escet.setext.runtime.exceptions.CustomSyntaxException;
import org.eclipse.lsat.common.queries.QueryableIterable;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ControlNode;
import org.eclipse.uml2.uml.DataType;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.FlowFinalNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.Interval;
import org.eclipse.uml2.uml.IntervalConstraint;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.PrimitiveType;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.Stereotype;
import org.eclipse.uml2.uml.TemplateParameter;
import org.eclipse.uml2.uml.TemplateSignature;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.espilce.periksa.validation.Check;
import org.espilce.periksa.validation.ContextAwareDeclarativeValidator;

import com.github.tno.pokayoke.transform.common.NameHelper;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.CifContextManager;
import com.github.tno.synthml.uml.profile.cif.CifParserHelper;
import com.github.tno.synthml.uml.profile.cif.CifScopedContext;
import com.github.tno.synthml.uml.profile.cif.CifTypeChecker;
import com.github.tno.synthml.uml.profile.cif.NamedTemplateParameter;
import com.github.tno.synthml.uml.profile.cif.TypeException;
import com.github.tno.synthml.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import SynthML.FormalCallBehaviorAction;
import SynthML.FormalElement;
import SynthML.SynthMLPackage;

public class PokaYokeProfileValidator extends ContextAwareDeclarativeValidator {
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][0-9a-zA-Z_]*$");

    private enum NamingConvention {
        /**
         * Optional, but when set it should not contain double underscores. Transformations will generate these names if
         * required.
         */
        OPTIONAL,

        /**
         * Name should be set and not contain double underscores.
         */
        MANDATORY,

        /**
         * Name should be set, should not contain double underscores and should match
         * {@link PokaYokeProfileValidator#IDENTIFIER_PATTERN}.
         */
        IDENTIFIER
    }

    private CifContextManager getContextManager(Element element) {
        Map<Object, Object> validationContext = getContext().getValidationContext();

        if (validationContext == null) {
            return new CifContextManager(element);
        }

        return (CifContextManager)validationContext.compute(CifContextManager.class, (__, v) -> {
            CifContextManager ctxManager = (CifContextManager)v;
            if (ctxManager == null || ctxManager.getGlobalContext().getModel() != element.getModel()) {
                return new CifContextManager(element);
            }

            return ctxManager;
        });
    }

    private CifContext getGlobalContext(Element element) {
        return getContextManager(element).getGlobalContext();
    }

    private CifScopedContext getScopedContext(Element element) {
        return getContextManager(element).getScopedContext(element);
    }

    /**
     * Reports an error if cycles are found in activities.
     *
     * @param action The action to check
     */
    @Check
    private void checkNoCyclesInActivities(CallBehaviorAction action) {
        if (hasCycle(action, Sets.newHashSet(action.getActivity()))) {
            error("Detected cycle in activities", null);
        }
    }

    private static boolean hasCycle(CallBehaviorAction action, Set<Activity> history) {
        if (action.getBehavior() instanceof Activity activity) {
            if (!history.add(activity)) {
                return true;
            }
            if (from(activity.getNodes()).objectsOfKind(CallBehaviorAction.class).exists(a -> hasCycle(a, history))) {
                return true;
            }
            history.remove(activity);
        }
        return false;
    }

    /**
     * Reports an error if instantiation cycles are found. An instantiation cycle is defined to be a circular dependency
     * between the properties of the data types that are defined within the given UML model. If the given data type is
     * part of some instantiation cycles, then only the shortest cycle will be reported. If there are multiple such
     * shortest cycles, then the first one that's encountered will be reported.
     *
     * @param dataType The data type to check.
     */
    @Check
    private void checkNoInstantiationCycles(DataType dataType) {
        // Only composite data types can be part of an instantiation cycle.
        if (PokaYokeTypeUtil.isCompositeDataType(dataType)) {
            List<String> shortestCycle = new LinkedList<>();
            shortestCycle = findShortestInstantiationCycle(dataType, new Stack<>(), shortestCycle);
            if (!shortestCycle.isEmpty()) {
                error("Found an instantiation cycle: " + String.join(" -> ", shortestCycle),
                        UMLPackage.Literals.ELEMENT__OWNED_ELEMENT);
            }
        }
    }

    private static List<String> findShortestInstantiationCycle(DataType dataType, Stack<DataType> hierarchy,
            List<String> shortestCycle)
    {
        hierarchy.push(dataType);
        for (Property property: dataType.getOwnedAttributes()) {
            Type propertyType = property.getType();
            if (PokaYokeTypeUtil.isCompositeDataType(propertyType)) {
                if (hierarchy.contains(propertyType)) {
                    // Get only the elements forming a cycle.
                    List<String> cycle = hierarchy.stream().dropWhile(type -> !type.equals(propertyType))
                            .map(NamedElement::getName).collect(Collectors.toList());
                    cycle.add(propertyType.getName());
                    // Substitute if current cycle is shorter than the shortest cycle that has been found so far.
                    if (shortestCycle.isEmpty() || cycle.size() < shortestCycle.size()) {
                        shortestCycle = cycle;
                    }
                } else {
                    shortestCycle = findShortestInstantiationCycle((DataType)propertyType, hierarchy, shortestCycle);
                }
            }
        }
        hierarchy.pop();
        return shortestCycle;
    }

    /**
     * Validates if the names of all {@link CifContext#getReferenceableElementsInclDuplicates unique name elements} are
     * unique within the {@code model}.
     *
     * @param model The model to validate.
     * @see CifContext
     */
    @Check
    private void checkGlobalUniqueNames(Model model) {
        if (!isPokaYokeUmlProfileApplied(model)) {
            return;
        }
        CifContext ctx = getGlobalContext(model);
        Map<String, List<NamedElement>> referenceableElementsInclDuplicates = ctx
                .getReferenceableElementsInclDuplicates();
        for (Map.Entry<String, List<NamedElement>> entry: referenceableElementsInclDuplicates.entrySet()) {
            // Skip primitive type constraints, that always have the same fixed name.
            if (entry.getValue().stream()
                    .allMatch(t -> t instanceof Constraint constr && CifContext.isPrimitiveTypeConstraint(constr)))
            {
                continue;
            }

            // Skip activity name check: we may have multiple activities with the same name.
            if (entry.getValue().stream().allMatch(t -> t instanceof Activity)) {
                continue;
            }

            // Null or empty strings are reported by #checkNamingConventions(NamedElement, boolean, boolean)
            if (!Strings.isNullOrEmpty(entry.getKey()) && entry.getValue().size() > 1) {
                for (NamedElement duplicate: entry.getValue()) {
                    error("Name should be unique within model: " + entry.getKey(), duplicate,
                            UMLPackage.Literals.NAMED_ELEMENT__NAME);
                }
            }
        }
    }

    @Check
    private void checkValidModel(Model model) {
        // Skip the next validation steps if the Poka Yoke UML Profile is not applied to the model and all its
        // ancestors.
        if (!isPokaYokeUmlProfileAppliedOnSelfOrAncestor(model)) {
            return;
        }
        checkNamingConventions(model, NamingConvention.MANDATORY);

        if (model.getOwner() != null) {
            error("Model is nested in another model.", UMLPackage.Literals.PACKAGE__NESTED_PACKAGE);
        }

        // Valid models should have one class.
        long count = model.getPackagedElements().stream().filter(pack -> pack instanceof Class).count();
        if (count != 1) {
            error(String.format("The model should contain one class, found %s.", count),
                    UMLPackage.Literals.PACKAGE__PACKAGED_ELEMENT);
        }
    }

    @Check
    private void checkValidClass(Class clazz) {
        if (!isPokaYokeUmlProfileApplied(clazz)) {
            return;
        }

        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(clazz, NamingConvention.IDENTIFIER);

        if (!clazz.getNestedClassifiers().isEmpty()) {
            error("Nested classifiers are not supported.", UMLPackage.Literals.CLASS__NESTED_CLASSIFIER);
        }

        if (clazz instanceof Behavior) {
            // Activities are also a Class in UML. Skip the next validations for all behaviors.
            return;
        }

        if (clazz.getOwner() instanceof TemplateParameter) {
            // This class is used to name a template parameter. Skip the next validations.
            return;
        }

        if (!clazz.isActive()) {
            error("Class must be active, not passive.", UMLPackage.Literals.CLASS__IS_ACTIVE);
        }

        if (clazz.getClassifierBehavior() == null) {
            error("Required classifier behavior not set.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
        } else {
            if (!(clazz.getClassifierBehavior() instanceof Activity activity)) {
                error("Classifier behavior must be an activity.",
                        UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
            } else if (!CifScopedContext.getClassifierTemplateParameters(activity).isEmpty()) {
                error("The classifier behavior activity must not have parameters.",
                        UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
            }

            if (!clazz.getOwnedBehaviors().contains(clazz.getClassifierBehavior())) {
                error("The classifier behavior of the class is not within the owned behaviors of the class.",
                        UMLPackage.Literals.BEHAVIORED_CLASSIFIER__OWNED_BEHAVIOR);
            }
        }

        // Classes must be defined directly within the model itself.
        if (!(clazz.getOwner() instanceof Model)) {
            error("Class is not defined at the top level of the UML model.",
                    UMLPackage.Literals.CLASSIFIER__INHERITED_MEMBER);
        }

        if (clazz.getOwnedRules().stream().anyMatch(IntervalConstraint.class::isInstance)) {
            error("Class contains interval constraints.", UMLPackage.Literals.NAMESPACE__MEMBER);
        }
    }

    @Check
    private void checkValidDataType(DataType dataType) {
        if (!isPokaYokeUmlProfileApplied(dataType)) {
            return;
        }

        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(dataType, NamingConvention.IDENTIFIER);

        if (!(dataType.getOwner() instanceof Model)) {
            error("Data type is not defined at the top level of the UML model.",
                    UMLPackage.Literals.CLASSIFIER__INHERITED_MEMBER);
        }

        // Skip the remaining checks for enumerations and primitive types, which are also data types.
        if (!PokaYokeTypeUtil.isCompositeDataType(dataType)) {
            return;
        }

        if (!dataType.getOwnedElements().stream().allMatch(Property.class::isInstance)) {
            error("Data type owns elements that are not properties.", UMLPackage.Literals.ELEMENT__OWNED_ELEMENT);
        }
    }

    @Check
    private void checkValidBehavior(Behavior behavior) {
        if (!isPokaYokeUmlProfileApplied(behavior)) {
            return;
        }

        // Check if behaviors are defined within an active class.
        if (!(behavior.getOwner() instanceof Class)) {
            error("Behavior is not defined within an active class.", UMLPackage.Literals.CLASS__IS_ACTIVE);
        }

        // Check if behaviors are defined as owned behavior.
        if (!((Class)behavior.getOwner()).getOwnedBehaviors().contains(behavior)) {
            error("Behavior is not defined as owned behaviors of a class.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__OWNED_BEHAVIOR);
        }
    }

    /**
     * Validates if the {@code property} is a single-valued, mandatory property, that the {@link Property#getType()
     * property type} is supported, and if the {@link Property#getDefaultValue() property default} is an instance of its
     * type.
     * <p>
     * This validation is only applied if the {@link SynthMLPackage Poka Yoke profile} is applied.
     * </p>
     *
     * @param property The property to validate.
     */
    @Check
    private void checkValidProperty(Property property) {
        if (!isPokaYokeUmlProfileApplied(property)) {
            return;
        }

        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(property, NamingConvention.IDENTIFIER);

        if (property.getUpper() != 1) {
            error("Property should be single-valued", UMLPackage.Literals.MULTIPLICITY_ELEMENT__UPPER);
        }
        if (property.getLower() != 1) {
            error("Property should be mandatory", UMLPackage.Literals.MULTIPLICITY_ELEMENT__LOWER);
        }

        Type propType = property.getType();
        if (propType == null) {
            error("Property type not set", UMLPackage.Literals.TYPED_ELEMENT__TYPE);
            return;
        } else if (!PokaYokeTypeUtil.isSupportedType(propType)) {
            error("Unsupported property type: " + propType.getName(), UMLPackage.Literals.TYPED_ELEMENT__TYPE);
            return;
        }

        try {
            AExpression propDefaultExpr = CifParserHelper.parseExpression(property.getDefaultValue());
            if (propDefaultExpr == null) {
                return;
            }
            new PropertyDefaultValueTypeChecker(getScopedContext(property)).checkAssignment(propType, propDefaultExpr);

            if (PokaYokeTypeUtil.isIntegerType(propType)) {
                // Default value is set and valid, thus can be parsed into an integer
                int propDefaultValue = Integer.parseInt(property.getDefault());
                Integer propMinValue = PokaYokeTypeUtil.getMinValue(propType);
                Integer propMaxValue = PokaYokeTypeUtil.getMaxValue(propType);
                // Only check if type range is valid, also see #checkValidPrimitiveType(PrimitiveType)
                if (propMinValue != null && propMaxValue != null && propMaxValue >= propMinValue
                        && (propDefaultValue < propMinValue || propDefaultValue > propMaxValue))
                {
                    throw new TypeException(String.format("value %d is not within range [%d .. %d]", propDefaultValue,
                            propMinValue, propMaxValue));
                }
            }
        } catch (RuntimeException e) {
            error("Invalid property default: " + e.getLocalizedMessage(), UMLPackage.Literals.PROPERTY__DEFAULT);
        }
    }

    @Check
    private void checkValidEnumeration(Enumeration enumeration) {
        if (!isPokaYokeUmlProfileApplied(enumeration)) {
            return;
        }

        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(enumeration, NamingConvention.IDENTIFIER);

        if (!(enumeration.eContainer() instanceof Model)) {
            error("Enumeration is not declared in the model.", null);
        } else if (enumeration.eContainer().eContainer() != null) {
            error("Enumeration is not declared on the outer-most level.", null);
        }
        if (enumeration.getOwnedLiterals().isEmpty()) {
            error("Enumeration does not own any literal.", null);
        } else {
            // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
            enumeration.getOwnedLiterals().forEach(l -> checkNamingConventions(l, NamingConvention.IDENTIFIER));
        }
    }

    @Check
    private void checkValidPrimitiveType(PrimitiveType primitiveType) {
        if (!isPokaYokeUmlProfileApplied(primitiveType)) {
            return;
        }

        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(primitiveType, NamingConvention.IDENTIFIER);

        if (!(primitiveType.eContainer() instanceof Model)) {
            error("Primitive type is not declared in the model.", null);
        } else if (primitiveType.eContainer().eContainer() != null) {
            error("Primitive type is not declared on the outer-most level.", null);
        }

        if (!PokaYokeTypeUtil.isIntegerType(primitiveType)) {
            error(PokaYokeTypeUtil.getLabel(primitiveType) + " should inherit from primitive Integer type.", null);
            return;
        }

        Constraint minConstraint = PokaYokeTypeUtil.getMinConstraint(primitiveType, false);
        if (minConstraint == null) {
            error("Minimum value constraint not set.", UMLPackage.Literals.NAMESPACE__OWNED_RULE);
            return;
        }
        Constraint maxConstraint = PokaYokeTypeUtil.getMaxConstraint(primitiveType, false);
        if (maxConstraint == null) {
            error("Maximum value constraint not set.", UMLPackage.Literals.NAMESPACE__OWNED_RULE);
            return;
        }

        if (minConstraint.getSpecification() instanceof LiteralInteger minValue
                && maxConstraint.getSpecification() instanceof LiteralInteger maxValue)
        {
            if (minValue.getValue() < 0) {
                error("Integer type range includes negative values.", minConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }
            if (minValue.getValue() > maxValue.getValue()) {
                error("Minimum value cannot be greater than maximum value.", minConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }
        } else {
            // Null values for getSpecification() are reported by default UML validator.
            if (minConstraint.getSpecification() != null) {
                error("Only literal integer is supported for minimum value constraint.", minConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }
            if (maxConstraint.getSpecification() != null) {
                error("Only literal integer is supported for maximum value constraint.", maxConstraint,
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }
        }
    }

    @Check
    private void checkValidOpaqueExpression(OpaqueExpression expression) {
        if (!isPokaYokeUmlProfileApplied(expression)) {
            return;
        }
        checkNamingConventions(expression, NamingConvention.OPTIONAL);

        if (expression.getBodies().size() != 1) {
            error(String.format("Opaque expression should have exactly one expression body, found %s.",
                    expression.getBodies().size()), UMLPackage.Literals.OPAQUE_EXPRESSION__BODY);
        }
    }

    @Check
    private void checkValidActivity(Activity activity) {
        if (!isPokaYokeUmlProfileApplied(activity)) {
            return;
        }
        checkNamingConventions(activity, NamingConvention.MANDATORY);

        if (activity.getClassifierBehavior() != null) {
            error("Activity owns a classifier behavior.",
                    UMLPackage.Literals.BEHAVIORED_CLASSIFIER__CLASSIFIER_BEHAVIOR);
        }

        // Check template parameters of the activity. Adding a check directly on 'TemplateSignature' fails
        // to report the error message to the Problems view.
        checkValidTemplateSignature(activity);

        Set<NamedElement> members = new LinkedHashSet<>(activity.getMembers());

        Set<Constraint> preAndPostconditions = new LinkedHashSet<>();
        preAndPostconditions.addAll(activity.getPreconditions());
        preAndPostconditions.addAll(activity.getPostconditions());

        Set<IntervalConstraint> intervalConstraints = activity.getOwnedRules().stream()
                .filter(IntervalConstraint.class::isInstance).map(IntervalConstraint.class::cast)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!members.equals(Sets.union(preAndPostconditions, intervalConstraints))) {
            error("Activity should contain only precondition, postcondition, and interval constraint members.",
                    UMLPackage.Literals.NAMESPACE__MEMBER);
        }

        if (activity.isAbstract()) {
            if (!activity.getNodes().isEmpty()) {
                error("Abstract activity has nodes.", UMLPackage.Literals.ACTIVITY__NODE);
            }

            if (!activity.getEdges().isEmpty()) {
                error("Abstract activity has edges.", UMLPackage.Literals.ACTIVITY__EDGE);
            }
        } else {
            QueryableIterable<InitialNode> initialNodes = from(activity.getNodes()).objectsOfKind(InitialNode.class);
            if (initialNodes.size() != 1) {
                error("Concrete activity should have exactly one initial node but got " + initialNodes.size(), activity,
                        null);
            }

            QueryableIterable<ActivityFinalNode> finalNodes = from(activity.getNodes())
                    .objectsOfKind(ActivityFinalNode.class);
            if (finalNodes.size() > 1) {
                error("Concrete activity should have at most one final node but got " + finalNodes.size(), activity,
                        null);
            }

            QueryableIterable<FlowFinalNode> flowFinalNodes = from(activity.getNodes())
                    .objectsOfKind(FlowFinalNode.class);
            if (!flowFinalNodes.isEmpty()) {
                error("Flow final nodes are not supported.", activity, null);
            }
        }
    }

    private void checkValidTemplateSignature(Activity activity) {
        List<ClassifierTemplateParameter> templateParameters = CifScopedContext
                .getClassifierTemplateParameters(activity);

        if (activity.isAbstract() && templateParameters.size() > 0) {
            error("Activity parameters are disallowed on abstract activities.", null);
            return; // Further checks do not provide the user with useful information.
        }

        TemplateSignature templateSignature = activity.getOwnedTemplateSignature();
        if (templateSignature != null
                && !templateSignature.getParameters().stream().allMatch(ClassifierTemplateParameter.class::isInstance))
        {
            error("Activity parameters must be of type 'ClassifierTemplateParameter'.", null);
        }

        if (templateParameters.stream().map(ClassifierTemplateParameter::getConstrainingClassifiers)
                .anyMatch(constrainingClassifiers -> constrainingClassifiers.size() != 1))
        {
            error("Activity parameters must have exactly one constraining classifier.", null);
        }

        if (!templateParameters.stream().map(ClassifierTemplateParameter::getDefault)
                .allMatch(NamedElement.class::isInstance))
        {
            error("Activity parameters must have a default of type 'NamedElement'.", null);
        }

        if (!templateParameters.stream().map(CifScopedContext::getClassifierTemplateParameterType)
                .allMatch(parameter -> parameter instanceof DataType && (PokaYokeTypeUtil.isPrimitiveType(parameter)
                        || PokaYokeTypeUtil.isEnumerationType(parameter))))
        {
            error("Activity parameters must be of primitive or enumeration type.", null);
        }

        List<String> parameterNames = templateParameters.stream().map(ClassifierTemplateParameter::getDefault)
                .map(CifScopedContext::getClassifierTemplateParameterName).toList();
        if (!parameterNames.stream().allMatch(new HashSet<>()::add)) {
            error("Activity parameters must have unique names.", null);
        }

        CifContext globalContext = getGlobalContext(activity);
        for (String parameterName: parameterNames) {
            // Check if the template parameter name matches a variable from an enclosing scope.
            // Currently this means that only properties declared in the global scope are found, so we can mention
            // 'property' specifically in the error message.
            if (globalContext.isVariable(parameterName)) {
                error(String.format("'%s' was already declared as a property.", parameterName), null);
            }
        }
    }

    @Check
    private void checkValidActivityEdge(ActivityEdge activityEdge) {
        if (!isPokaYokeUmlProfileApplied(activityEdge)) {
            return;
        }
        if (!(activityEdge instanceof ControlFlow)) {
            error("Unsupported activity edge type: " + activityEdge.eClass().getName(), null);
        } else {
            checkNamingConventions(activityEdge, NamingConvention.OPTIONAL);
        }
    }

    @Check
    private void checkValidActivityNode(ActivityNode node) {
        if (!isPokaYokeUmlProfileApplied(node)) {
            return;
        }
        if (!(node instanceof ControlNode || node instanceof CallBehaviorAction || node instanceof OpaqueAction)) {
            error("Unsupported activity node type: " + node.eClass().getName(), null);
            return;
        }

        if (node instanceof Action action && PokaYokeUmlProfileUtil.isFormalElement(action)) {
            checkNamingConventions(node, NamingConvention.MANDATORY);
        } else {
            checkNamingConventions(node, NamingConvention.OPTIONAL);
        }

        // Check that call behavior actions call either an opaque behavior or a concrete activity.
        if (node instanceof CallBehaviorAction cbAction) {
            if (!(cbAction.getBehavior() instanceof OpaqueBehavior
                    || (cbAction.getBehavior() instanceof Activity activity && !activity.isAbstract())))
            {
                error("Call behavior actions should call an opaque behavior or a concrete activity.", node,
                        UMLPackage.Literals.CALL_BEHAVIOR_ACTION__BEHAVIOR);
            }
        }

        // Check number of incoming and outgoing edges.
        if (node instanceof CallBehaviorAction || node instanceof OpaqueAction) {
            if (node.getIncomings().size() != 1) {
                error(String.format("Node of type '%s' should have exactly one incoming edge, but got %s.",
                        node.getClass().getSimpleName(), node.getIncomings().size()), node,
                        UMLPackage.Literals.ACTIVITY_NODE__INCOMING);
            }
            if (node.getOutgoings().size() != 1) {
                error(String.format("Node of type '%s' should have exactly one outgoing edge, but got %s.",
                        node.getClass().getSimpleName(), node.getOutgoings().size()), node,
                        UMLPackage.Literals.ACTIVITY_NODE__OUTGOING);
            }
        } else if (node instanceof DecisionNode || node instanceof ForkNode) {
            if (node.getIncomings().size() != 1) {
                error(String.format("Node of type '%s' should have exactly one incoming edge, but got %s.",
                        node.getClass().getSimpleName(), node.getIncomings().size()), node,
                        UMLPackage.Literals.ACTIVITY_NODE__INCOMING);
            }
            if (node.getOutgoings().size() == 0) {
                error(String.format("Node of type '%s' should have at least one outgoing edge.",
                        node.getClass().getSimpleName()), node, UMLPackage.Literals.ACTIVITY_NODE__OUTGOING);
            }
        } else if (node instanceof MergeNode || node instanceof JoinNode) {
            if (node.getIncomings().size() == 0) {
                error(String.format("Node of type '%s' should have at least one incoming edge.",
                        node.getClass().getSimpleName()), node, UMLPackage.Literals.ACTIVITY_NODE__INCOMING);
            }
            if (node.getOutgoings().size() != 1) {
                error(String.format("Node of type '%s' should have exactly one outgoing edge, but got %s.",
                        node.getClass().getSimpleName(), node.getOutgoings().size()), node,
                        UMLPackage.Literals.ACTIVITY_NODE__OUTGOING);
            }
        } else if (node instanceof InitialNode) {
            if (node.getIncomings().size() != 0) {
                error("Initial node should have no incoming edges.", node, UMLPackage.Literals.ACTIVITY_NODE__INCOMING);
            }
            if (node.getOutgoings().size() != 1) {
                error(String.format("Initial node should have exactly one outgoing edge, but got %s.",
                        node.getOutgoings().size()), node, UMLPackage.Literals.ACTIVITY_NODE__OUTGOING);
            }
        } else if (node instanceof ActivityFinalNode) {
            if (node.getIncomings().size() != 1) {
                error(String.format("Activity final node should have exactly one incoming edge, but got %s.",
                        node.getIncomings().size()), node, UMLPackage.Literals.ACTIVITY_NODE__INCOMING);
            }
            if (node.getOutgoings().size() != 0) {
                error("Activity final node should have no outgoing edges.", node,
                        UMLPackage.Literals.ACTIVITY_NODE__OUTGOING);
            }
        } else {
            error(String.format("Unsupported node type: '%s'", node.getClass().getSimpleName()), null);
        }
    }

    /**
     * Validates that the behavior that is called by {@code action} does not have guards and effects whenever
     * {@code action} itself has {@link PokaYokeUmlProfileUtil#isFormalElement(org.eclipse.uml2.uml.RedefinableElement)
     * guards and effects}.
     *
     * @param action The action to validate.
     */
    @Check
    private void checkShadowedFormalElement(CallBehaviorAction action) {
        if (!isPokaYokeUmlProfileApplied(action)) {
            return;
        }
        Behavior behavior = action.getBehavior();
        if (behavior instanceof Activity subActivity) {
            if (!PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
                // No shadowing
                return;
            }
            if (isGuardEffectsActivity(subActivity, new HashSet<>())) {
                warning("The guard and effects on this call behavior action overrides the guards and effects of its sub-activity",
                        null);
            }
        } else if (behavior instanceof OpaqueBehavior) {
            // Shadowing is not allowed in case of call opaque behavior actions, as such behaviors directly have guards
            // and effects.
            if (PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
                error("Cannot override the guards and effects of an opaque behavior in a call opaque behavior action.",
                        UMLPackage.Literals.CALL_BEHAVIOR_ACTION__BEHAVIOR);
            }
        } else {
            // Added this validation here, as we're already checking the behavior type.
            error("Found behavior that is not an activity or an opaque behavior.",
                    UMLPackage.Literals.CALL_BEHAVIOR_ACTION__BEHAVIOR);
        }
    }

    private static boolean isGuardEffectsActivity(Activity activity, Set<Activity> history) {
        boolean containsGuardEffectsActions = from(activity.getOwnedNodes()).objectsOfKind(Action.class)
                .exists(a -> PokaYokeUmlProfileUtil.isSetGuard(a) || PokaYokeUmlProfileUtil.isSetEffects(a));
        boolean containsGuardedControlFlows = from(activity.getOwnedElements()).objectsOfKind(ControlFlow.class)
                .exists(cf -> !Strings.isNullOrEmpty(PokaYokeUmlProfileUtil.getOutgoingGuard(cf))
                        || !Strings.isNullOrEmpty(PokaYokeUmlProfileUtil.getIncomingGuard(cf)));
        if (containsGuardEffectsActions || containsGuardedControlFlows) {
            return true;
        } else if (!history.add(activity)) {
            // Cope with cycles.
            return false;
        }

        return from(activity.getOwnedNodes()).objectsOfKind(CallBehaviorAction.class)
                .xcollectOne(CallBehaviorAction::getBehavior).exists(b -> b instanceof OpaqueBehavior
                        || b instanceof Activity a && isGuardEffectsActivity(a, history));
    }

    /**
     * Validates the {@link FormalElement#getGuard()} property or the incoming and outgoing guards of a control flow, if
     * set.
     *
     * @param element The element to validate.
     */
    @Check
    private void checkValidGuard(RedefinableElement element) {
        CifTypeChecker typeChecker = new CifTypeChecker(getScopedContext(element));
        if (element instanceof ControlFlow controlFlow) {
            try {
                AExpression incomingGuardExpr = CifParserHelper.parseIncomingGuard(controlFlow);
                if (incomingGuardExpr != null) {
                    typeChecker.checkBooleanAssignment(incomingGuardExpr);
                }
            } catch (RuntimeException e) {
                error("Invalid incoming guard: " + e.getLocalizedMessage(), UMLPackage.Literals.ACTIVITY_EDGE__GUARD);
            }

            try {
                AExpression outgoingGuardExpr = CifParserHelper.parseOutgoingGuard(controlFlow);
                if (outgoingGuardExpr != null) {
                    typeChecker.checkBooleanAssignment(outgoingGuardExpr);
                }
            } catch (RuntimeException e) {
                error("Invalid outgoing guard: " + e.getLocalizedMessage(), null);
            }
        } else {
            try {
                AExpression guardExpr = CifParserHelper.parseGuard(element);
                if (guardExpr != null) {
                    typeChecker.checkBooleanAssignment(guardExpr);
                }
            } catch (RuntimeException e) {
                error("Invalid guard: " + e.getLocalizedMessage(), null);
            }
        }
    }

    /**
     * Validates the {@link FormalElement#getEffects()} property if set.
     *
     * @param element The element to validate.
     */
    @Check
    private void checkValidEffects(RedefinableElement element) {
        List<String> effects = PokaYokeUmlProfileUtil.getEffects(element);
        for (int i = 0; i < effects.size(); i++) {
            try {
                checkValidUpdates(CifParserHelper.parseUpdates(effects.get(i), element), element);
            } catch (RuntimeException re) {
                String prefix = "Invalid effects: ";
                if (effects.size() > 1) {
                    prefix = String.format("Invalid effects (%d of %d): ", i + 1, effects.size());
                }
                error(prefix + re.getLocalizedMessage(), null);
            }
        }
    }

    /**
     * Validates the {@link FormalCallBehaviorAction#getArguments()} property if set.
     *
     * @param callAction The call action to validate.
     */
    @Check
    private void checkValidArguments(CallBehaviorAction callAction) {
        try {
            List<AAssignmentUpdate> assignments = CifParserHelper.parseArguments(callAction);

            Behavior behavior = callAction.getBehavior();
            if (!(behavior instanceof Activity calledActivity)) {
                if (!assignments.isEmpty()) {
                    String got = (behavior == null) ? "null" : behavior.getClass().getSimpleName();
                    error("A call behavior action with arguments must call an activity, got: " + got, null);
                }
                // No activity could be resolved, skip argument parsing of the behavior.
                return;
            }

            // Valid assignments are valid updates with restrictions.
            checkValidArguments(assignments, callAction, calledActivity);

            // Ensure that no parameter is assigned more than once.
            checkUniqueAddressables(Lists.cast(assignments), new LinkedHashSet<>());

            // Ensure that every parameter is assigned.
            if (assignments.size() != getScopedContext(calledActivity).getDeclaredTemplateParameters().size()) {
                error("Not all parameters of the called activity have been assigned.", null);
            }
        } catch (RuntimeException re) {
            String prefix = "Invalid parameter assignments: ";
            error(prefix + re.getLocalizedMessage(), null);
        }
    }

    private void checkValidArguments(List<AAssignmentUpdate> assignments, CallBehaviorAction callAction,
            Activity calledActivity)
    {
        CifScopedContext addressableContext = getScopedContext(calledActivity);
        CifContext valueContext = getScopedContext(callAction);

        List<NamedTemplateParameter> declaredTemplateParameters = addressableContext.getDeclaredTemplateParameters();

        for (AAssignmentUpdate assignment: assignments) {
            // Ensure the addressable part is a named expression referring to the name of a template parameter, and that
            // the addressable and value have the same type.
            if (!(assignment.addressable instanceof ANameExpression addressable)) {
                error("Invalid parameter assignment: Only single names are allowed as addressables.", null);
            } else if (addressable.derivative) {
                error("Invalid parameter assignment: Expected a non-derivative parameter name.", null);
            } else if (!isNameInNamedElements(addressable.name.name, declaredTemplateParameters)) {
                error("Invalid parameter assignment: Unknown activity parameter name (of the called activity): "
                        + addressable.name.name, null);
            } else {
                // Verify that the types match.
                CifTypeChecker checker = new CifTypeChecker(valueContext);

                checker.checkArgumentAssignment(addressable, addressableContext, assignment.value, valueContext,
                        assignment.position);
            }

            // Ensure that the value expression is supported.
            if (assignment.value instanceof ANameExpression nameExpr) {
                if (nameExpr.derivative) {
                    error("Expected a non-derivative assignment.", null);
                }

                String name = nameExpr.name.name;
                NamedElement element = valueContext.getReferenceableElement(name);
                if (!(element instanceof EnumerationLiteral || element instanceof NamedTemplateParameter)) {
                    error("Invalid parameter assignment: Expected a literal or a parameter of the calling activity, got: "
                            + name, null);
                }
            } else if (!(assignment.value instanceof ABoolExpression || assignment.value instanceof AIntExpression)) {
                error("Invalid parameter assignment: Expected a literal or a parameter of the calling activity", null);
            }
        }
    }

    private static boolean isNameInNamedElements(String name, List<? extends NamedElement> namedElements) {
        return namedElements.stream().anyMatch(p -> p.getName().equals(name));
    }

    private void checkValidUpdates(List<AUpdate> updates, Element element) {
        // Type check all updates.
        CifTypeChecker typeChecker = new CifTypeChecker(getScopedContext(element));

        for (AUpdate update: updates) {
            typeChecker.checkUpdate(update);
        }

        // Ensure that no variable is assigned more than once by the given list of updates.
        checkUniqueAddressables(updates, new LinkedHashSet<>());
    }

    /**
     * Checks that no variable is assigned more than once by the given list of {@code updates}, where {@code addrVars}
     * is the set of variables that are already known to be assigned.
     *
     * @param updates The updates to check.
     * @param addrVars The set of assigned variables, which is modified in-place.
     */
    private void checkUniqueAddressables(List<AUpdate> updates, Set<String> addrVars) {
        for (AUpdate update: updates) {
            checkUniqueAddressables(update, addrVars);
        }
    }

    /**
     * Checks that no variable is assigned more than once by the given {@code update}, where {@code addrVars} is the set
     * of variables that are already known to be assigned.
     *
     * @param update The update to check.
     * @param addrVars The set of assigned variables, which is modified in-place.
     */
    private void checkUniqueAddressables(AUpdate update, Set<String> addrVars) {
        if (update instanceof AAssignmentUpdate assignment) {
            ANameExpression varExpr = (ANameExpression)assignment.addressable;
            String varName = varExpr.name.name;
            boolean added = addrVars.add(varName);

            if (!added) {
                throw new CustomSyntaxException(String.format("Variable '%s' is updated multiple times.", varName),
                        varExpr.position);
            }
        } else if (update instanceof AIfUpdate ifUpdate) {
            Set<String> newAddrVars = new LinkedHashSet<>(addrVars);

            Set<String> addrVarsThens = new LinkedHashSet<>(addrVars);
            checkUniqueAddressables(ifUpdate.thens, addrVarsThens);
            newAddrVars.addAll(addrVarsThens);

            for (AElifUpdate elifUpdate: ifUpdate.elifs) {
                Set<String> addrVarsElifs = new LinkedHashSet<>(addrVars);
                checkUniqueAddressables(elifUpdate.thens, addrVarsElifs);
                newAddrVars.addAll(addrVarsElifs);
            }

            Set<String> addrVarsElses = new LinkedHashSet<>(addrVars);
            checkUniqueAddressables(ifUpdate.elses, addrVarsElses);
            newAddrVars.addAll(addrVarsElses);

            addrVars.addAll(newAddrVars);
        } else {
            error("Unsupported update: " + update, UMLPackage.Literals.TYPED_ELEMENT__TYPE);
        }
    }

    /**
     * Validates the name, guard and effects of the given opaque behavior.
     *
     * @param behavior The opaque behavior to validate.
     */
    @Check
    private void checkValidOpaqueBehavior(OpaqueBehavior behavior) {
        // Name uniqueness is checked by #checkGlobalUniqueNames(Model)
        checkNamingConventions(behavior, NamingConvention.IDENTIFIER);
    }

    @Check
    private void checkValidConstraint(Constraint constraint) {
        checkNamingConventions(constraint, NamingConvention.IDENTIFIER);

        if (CifContext.isActivityPrePostconditionConstraint(constraint)) {
            checkValidActivityPrePostconditionConstraint(constraint);
        } else if (CifContext.isClassConstraint(constraint)) {
            checkValidClassConstraint(constraint);
        } else if (CifContext.isOccurrenceConstraint(constraint)) {
            checkValidOccurrenceConstraint((IntervalConstraint)constraint);
        } else if (CifContext.isPrimitiveTypeConstraint(constraint)) {
            // The constraints for primitive types are validated in #checkValidPrimitiveType(PrimitiveType)
        } else {
            error("Unsupported constraint", UMLPackage.Literals.CONSTRAINT__CONTEXT);
        }
    }

    private void checkValidActivityPrePostconditionConstraint(Constraint constraint) {
        // Check that the constraint has the right stereotype applied.
        List<Stereotype> stereotypes = constraint.getAppliedStereotypes();
        if (stereotypes.size() != 1) {
            error(String.format("Constraint '%s' must have exactly one stereotype applied.", constraint.getName()),
                    UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return;
        }

        if (CifContext.isActivityPreconditionConstraint(constraint)
                && !(stereotypes.get(0).getName().equals(PokaYokeUmlProfileUtil.ST_SYNTHESIS_PRECONDITION)
                        || stereotypes.get(0).getName().equals(PokaYokeUmlProfileUtil.ST_USAGE_PRECONDITION)))
        {
            error(String.format("Constraint '%s' must have a precondition stereotype applied.", constraint.getName()),
                    UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return;
        } else if (CifContext.isActivityPostconditionConstraint(constraint)
                && !(stereotypes.get(0).getName().equals(PokaYokeUmlProfileUtil.ST_POSTCONDITION)))
        {
            error(String.format("Constraint '%s' must have a postcondition stereotype applied.", constraint.getName()),
                    UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return;
        }

        // Check if the constraint specification is supported.
        if (!isValidConstraintSpecificationForm(constraint)) {
            return;
        }

        try {
            AInvariant invariant = CifParserHelper.parseInvariant(constraint);
            new CifTypeChecker(getScopedContext(constraint)).checkInvariant(invariant);

            // Activity preconditions and postconditions are constraints and therefore parsed as invariants.
            // Make sure that they are state invariants.
            if (invariant.invKind != null || invariant.events != null) {
                error("All activity preconditions and postconditions should be state predicates.",
                        UMLPackage.Literals.NAMESPACE__MEMBER);
            }
        } catch (RuntimeException e) {
            error("Invalid invariant: " + e.getLocalizedMessage(), UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
        }
    }

    private void checkValidClassConstraint(Constraint constraint) {
        // Check that the constraint has the right stereotype applied.
        List<Stereotype> stereotypes = constraint.getAppliedStereotypes();

        if (stereotypes.size() != 1) {
            error(String.format("Constraint '%s' must have exactly one stereotype applied.", constraint.getName()),
                    UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return;
        }

        if (!stereotypes.get(0).getName().equals(PokaYokeUmlProfileUtil.ST_CLASS_REQUIREMENT)) {
            error(String.format("Constraint '%s' must have a requirement stereotype applied.", constraint.getName()),
                    UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return;
        }

        // Check if the constraint specification is supported.
        if (!isValidConstraintSpecificationForm(constraint)) {
            return;
        }

        try {
            new CifTypeChecker(getGlobalContext(constraint)).checkInvariant(CifParserHelper.parseInvariant(constraint));
        } catch (RuntimeException e) {
            error("Invalid invariant: " + e.getLocalizedMessage(), UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
        }
    }

    private boolean isValidConstraintSpecificationForm(Constraint constraint) {
        if (!(constraint.getSpecification() instanceof OpaqueExpression)) {
            error(String.format("Constraint '%s' must have an opaque expression as specification.",
                    constraint.getName()), UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return false;
        }

        if (((OpaqueExpression)constraint.getSpecification()).getBodies().size() != 1) {
            error(String.format("Constraint '%s' must have an opaque expression specification with exactly one body.",
                    constraint.getName()), UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return false;
        }

        if (!((OpaqueExpression)constraint.getSpecification()).getLanguages().equals(List.of("CIF"))) {
            error(String.format(
                    "Constraint '%s' must have an opaque expression specification with exactly one language that must be 'CIF'.",
                    constraint.getName()), UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return false;
        }

        if (((OpaqueExpression)constraint.getSpecification()).getBodies().get(0) == null
                || ((OpaqueExpression)constraint.getSpecification()).getBodies().get(0).isEmpty())
        {
            error(String.format(
                    "Constraint '%s' must have an opaque expression specification containing a valid expression.",
                    constraint.getName()), UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            return false;
        }

        return true;
    }

    private void checkValidOccurrenceConstraint(IntervalConstraint constraint) {
        if (constraint.getSpecification() instanceof Interval interval) {
            int min;
            int max;

            if (interval.getMin() instanceof LiteralInteger minLiteral) {
                min = minLiteral.getValue();
            } else {
                error("Invalid interval min value.", UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                return;
            }

            if (interval.getMax() instanceof LiteralInteger maxLiteral) {
                max = maxLiteral.getValue();
            } else {
                error("Invalid interval max value.", UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                return;
            }

            if (min < 0) {
                error("The interval min value is negative.", UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }

            if (min > max) {
                error("The interval min value exceeds the max value.", UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
            }

            if (constraint.getConstrainedElements().isEmpty()) {
                error("Interval constraint does not constrain any element.",
                        UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                return;
            }

            CifContext context = getGlobalContext(constraint);

            for (Element element: constraint.getConstrainedElements()) {
                if (element instanceof OpaqueBehavior || element instanceof Activity) {
                    if (!context.isDeclaredElement(element)) {
                        error("Constrained behavior is not in scope.", UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                    }
                } else {
                    error("Interval constraint constrains an element that is not an opaque behavior or an activity.",
                            UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
                }
            }
        } else {
            error("Interval constraint specification is not an interval.",
                    UMLPackage.Literals.CONSTRAINT__SPECIFICATION);
        }
    }

    private void checkNamingConventions(NamedElement element, NamingConvention convention) {
        String name = element.getName();
        if (Strings.isNullOrEmpty(name)) {
            if (convention != NamingConvention.OPTIONAL) {
                error("Required name not set.", element, UMLPackage.Literals.NAMED_ELEMENT__NAME);
            }
            return;
        }

        if (name.contains("__")) {
            error("Name cannot contain '__'", element, UMLPackage.Literals.NAMED_ELEMENT__NAME);
        }

        if (convention == NamingConvention.IDENTIFIER && !IDENTIFIER_PATTERN.matcher(name).matches()) {
            error("Element name should start with [a-zA-Z_] and then be followed by [0-9a-zA-Z_]*", element,
                    UMLPackage.Literals.NAMED_ELEMENT__NAME);
        }
    }

    public boolean isPokaYokeUmlProfileAppliedOnSelfOrAncestor(Element element) {
        if (isPokaYokeUmlProfileApplied(element)) {
            return true;
        }

        while (element.getOwner() != null) {
            if (isPokaYokeUmlProfileApplied(element.getOwner())) {
                return true;
            }
            element = element.getOwner();
        }
        return false;
    }

    private boolean isPokaYokeUmlProfileApplied(Element element) {
        return PokaYokeUmlProfileUtil.getAppliedProfile(element, PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE).isPresent();
    }

    /**
     * Checks if the names that are being used in the given UML model are not reserved keywords in CIF, GAL, or Petrify.
     *
     * @param model The UML model to check.
     */
    @Check
    private void checkReservedKeywords(Model model) {
        Collection<NamedElement> elements = getGlobalContext(model).getDeclaredElements();

        for (NamedElement element: elements) {
            // Primitive integer types are bounded between a min and a max value. These automatically generate
            // constraints named 'min' and 'max', which clash with the reserved keywords. Skip the check for these.
            if (element instanceof Constraint constraint && constraint.getContext() instanceof PrimitiveType) {
                continue;
            }
            if (NameHelper.isReservedKeyword(element.getName())) {
                error("Name matching keyword \"" + element.getName() + "\"", element,
                        UMLPackage.Literals.NAMED_ELEMENT__NAME);
            }
        }
    }
}
