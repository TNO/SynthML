
package com.github.tno.pokayoke.transform.uml2cameo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Range;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.ABinaryExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ABoolExpression;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.cif.parser.ast.expressions.ANameExpression;
import org.eclipse.escet.common.java.Lists;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ClassifierTemplateParameter;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.InstanceValue;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.RedefinableElement;
import org.eclipse.uml2.uml.RedefinableTemplateSignature;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.TemplateParameter;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.common.ValidationHelper;
import com.github.tno.pokayoke.transform.flatten.CompositeDataTypeFlattener;
import com.github.tno.synthml.uml.profile.cif.CifContext;
import com.github.tno.synthml.uml.profile.cif.CifParameterCollector;
import com.github.tno.synthml.uml.profile.cif.CifParserHelper;
import com.github.tno.synthml.uml.profile.cif.NamedTemplateParameter;
import com.github.tno.synthml.uml.profile.util.PokaYokeTypeUtil;
import com.github.tno.synthml.uml.profile.util.PokaYokeUmlProfileUtil;
import com.github.tno.synthml.uml.profile.util.UMLActivityUtils;
import com.github.tno.synthml.uml.profile.util.UmlPrimitiveType;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * Transforms UML models that are annotated with guards, effects, preconditions, etc., to UML models that can be
 * simulated using Cameo. The annotation language is assumed to be CIF.
 */
public class UMLToCameoTransformer {
    /** The prefix to use template variable assignments, which is 'temp__'. */
    public static final String PARAM_PREFIX = "temp__";

    /** Name for the lock class. */
    private static final String LOCK_CLASS_NAME = "Lock";

    private final Model model;

    private final CifToPythonTranslator translator;

    private final Map<String, Range<Integer>> propertyBounds;

    public UMLToCameoTransformer(Model model) {
        this.model = model;
        this.translator = new CifToPythonTranslator();
        this.propertyBounds = new LinkedHashMap<>();
    }

    public static void main(String[] args) throws IOException, CoreException {
        if (args.length == 2) {
            transformFile(Paths.get(args[0]), Paths.get(args[1]));
        } else {
            throw new IOException("Exactly two arguments expected: a source path and a target path.");
        }
    }

    public static void transformFile(Path sourcePath, Path targetPath) throws IOException, CoreException {
        String filePrefix = FilenameUtils.removeExtension(sourcePath.getFileName().toString());
        Path umlOutputFilePath = targetPath.resolve(filePrefix + ".uml");
        Model model = FileHelper.loadModel(sourcePath.toString());
        CompositeDataTypeFlattener.flattenCompositeDataTypes(model);
        new UMLToCameoTransformer(model).transformModel();
        FileHelper.storeModel(model, umlOutputFilePath.toString());
    }

    public void transformModel() throws CoreException {
        // 1. Check whether the model has the expected structure and obtain relevant information from it.
        ValidationHelper.validateModel(model);

        CifContext cifContext = CifContext.createGlobal(model);

        Preconditions.checkArgument(!cifContext.hasConstraints(c -> !CifContext.isPrimitiveTypeConstraint(c)),
                "Only type constraints are supported.");
        Preconditions.checkArgument(!cifContext.hasAbstractActivities(), "Abstract activities are unsupported.");

        Preconditions.checkArgument(model.getPackagedElement(LOCK_CLASS_NAME) == null,
                "Expected no packaged element named 'Lock' to already exist.");

        // Obtain the single class that should be defined within the model.
        List<Class> modelNestedClasses = getNestedNonActivityClassesOf(model);
        Preconditions.checkArgument(modelNestedClasses.size() == 1,
                "Expected the model to contain exactly one class, got " + modelNestedClasses.size());
        Class contextClass = modelNestedClasses.get(0);

        // Check that only outgoing edges from decision nodes have incoming guards, and only incoming edges to guarded
        // actions have outgoing guards.
        Activity contextActivity = (Activity)contextClass.getClassifierBehavior();
        for (Element activityElement: contextActivity.getOwnedElements()) {
            if (activityElement instanceof ControlFlow controlFlow) {
                AExpression incomingGuard = CifParserHelper.parseIncomingGuard(controlFlow);
                Preconditions.checkArgument(
                        incomingGuard == null || (incomingGuard instanceof ABoolExpression aBoolExpr && aBoolExpr.value)
                                || controlFlow.getSource() instanceof DecisionNode,
                        "Expected incoming guards only for edges that leave a decision node.");

                AExpression outgoingGuard = CifParserHelper.parseOutgoingGuard(controlFlow);
                Preconditions.checkArgument(
                        outgoingGuard == null || (outgoingGuard instanceof ABoolExpression aBoolExpr && aBoolExpr.value)
                                || controlFlow.getTarget() instanceof CallBehaviorAction
                                || controlFlow.getTarget() instanceof OpaqueAction,
                        "Expected outgoing guards only for edges that reach a call behavior or opaque action node.");
            }
        }

        // Collect integer bounds and set default values for all class properties
        propertyBounds.clear();
        for (Property property: contextClass.getOwnedAttributes()) {
            // Collect the bounds for integer properties, they will be validated later.
            Range<Integer> propertyRange = null;
            if (PokaYokeTypeUtil.isIntegerType(property.getType())) {
                propertyRange = Range.of(PokaYokeTypeUtil.getMinValue(property.getType()),
                        PokaYokeTypeUtil.getMaxValue(property.getType()));
                propertyBounds.put(property.getName(), propertyRange);
            }

            // Translate all default values of class properties to become Python expressions,
            // or set default values for simulation.
            AExpression cifExpression = CifParserHelper.parseExpression(property.getDefaultValue());
            if (cifExpression != null) {
                OpaqueExpression newDefaultValue = FileHelper.FACTORY.createOpaqueExpression();
                newDefaultValue.getLanguages().add("Python");
                String translatedLiteral = translator.translateExpression(cifExpression,
                        CifContext.createScoped(property));
                newDefaultValue.getBodies().add(translatedLiteral);
                property.setDefaultValue(newDefaultValue);
            } else if (propertyRange != null /* i.e. PokaYokeTypeUtil.isIntegerType(property.getType()) */) {
                // As default value, we choose the value that is closest to zero within its bounds.
                int propertyDefault = 0;
                if (propertyRange.getMaximum() < 0) {
                    propertyDefault = propertyRange.getMaximum();
                } else if (propertyRange.getMinimum() > 0) {
                    propertyDefault = propertyRange.getMinimum();
                }
                LiteralInteger value = FileHelper.FACTORY.createLiteralInteger();
                value.setValue(propertyDefault);
                property.setDefaultValue(value);
            } else if (PokaYokeTypeUtil.isBooleanType(property.getType())) {
                property.setDefaultValue(FileHelper.FACTORY.createLiteralBoolean());
            } else if (PokaYokeTypeUtil.isEnumerationType(property.getType())) {
                InstanceValue value = FileHelper.FACTORY.createInstanceValue();
                value.setInstance(((Enumeration)property.getType()).getOwnedLiterals().get(0));
                property.setDefaultValue(value);
            }
        }

        // Make sure the class does not contain an attribute named 'active'.
        Preconditions.checkArgument(
                contextClass.getOwnedAttributes().stream().noneMatch(a -> a.getName().equals("active")),
                "Expected no attribute named 'active' to already exist in the single class of the model.");

        // Obtain the activity that the single class within the model should have, as classifier behavior.
        Activity mainActivity = (Activity)contextClass.getClassifierBehavior();

        // 2. Define locking infrastructure.

        // Create a class for holding lock-related structure and behavior.
        Class lockClass = (Class)model.createPackagedElement(LOCK_CLASS_NAME, UMLPackage.eINSTANCE.getClass_());

        // Create the signal for acquiring the lock.
        Signal acquireSignal = FileHelper.FACTORY.createSignal();
        acquireSignal.setName("acquire");
        Property acquireParameter = FileHelper.FACTORY.createProperty();
        acquireParameter.setName("requester");
        acquireParameter.setType(UmlPrimitiveType.STRING.load(lockClass));
        acquireSignal.getOwnedAttributes().add(acquireParameter);
        lockClass.getNestedClassifiers().add(acquireSignal);

        // Create the signal event for the acquire signal to trigger on.
        SignalEvent acquireEvent = FileHelper.FACTORY.createSignalEvent();
        acquireEvent.setSignal(acquireSignal);
        acquireEvent.setVisibility(VisibilityKind.PUBLIC_LITERAL);
        model.getPackagedElements().add(acquireEvent);

        // Create the activity that handles lock acquisition.
        Activity lockHandlerActivity = ActivityHelper.createLockHanderActivity(acquireEvent);
        lockHandlerActivity.setName("lockhandler");
        lockClass.getOwnedBehaviors().add(lockHandlerActivity);

        // 3. Transform the single class within the model.

        // Create the static property that indicates the current owner of the lock (if any).
        Property activeProperty = FileHelper.FACTORY.createProperty();
        activeProperty.setIsStatic(true);
        activeProperty.setName("active");
        activeProperty.setType(UmlPrimitiveType.STRING.load(lockClass));
        LiteralString activePropertyDefaultValue = FileHelper.FACTORY.createLiteralString();
        activePropertyDefaultValue.setValue("");
        activeProperty.setDefaultValue(activePropertyDefaultValue);
        contextClass.getOwnedAttributes().add(activeProperty);

        // Transform all opaque behaviors within the model.
        for (OpaqueBehavior behavior: getNestedOpaqueBehaviorsOf(model)) {
            transformOpaqueBehavior(behavior, acquireSignal);
        }

        // Transform all activity behaviors within the model.
        for (Activity activity: getNestedActivitiesOf(model)) {
            transformActivity(activity, acquireSignal);
        }

        // 4. Transform the classifier behavior (i.e., main activity) of the single class within the model.

        // Obtain the single initial node of the main activity.
        List<InitialNode> initialNodes = mainActivity.getNodes().stream().filter(n -> n instanceof InitialNode)
                .map(n -> (InitialNode)n).toList();
        InitialNode initialNode = initialNodes.get(0);

        // Create a fork node to start the lock handler in parallel to the rest of the main activity.
        ForkNode forkNode = FileHelper.FACTORY.createForkNode();
        forkNode.setActivity(mainActivity);

        // Relocate the outgoing edge out of the initial node to go out of the fork node instead.
        initialNode.getOutgoings().get(0).setSource(forkNode);

        // Add an edge between the initial node and the new fork node.
        ControlFlow initToForkFlow = FileHelper.FACTORY.createControlFlow();
        initToForkFlow.setActivity(mainActivity);
        initToForkFlow.setSource(initialNode);
        initToForkFlow.setTarget(forkNode);

        // Define the action that calls the lock handler.
        CallBehaviorAction lockHandlerNode = FileHelper.FACTORY.createCallBehaviorAction();
        lockHandlerNode.setActivity(mainActivity);
        lockHandlerNode.setBehavior(lockHandlerActivity);

        // Define the control flow from the new fork node to the node that calls the lock handler.
        ControlFlow forkToLockHandlerFlow = FileHelper.FACTORY.createControlFlow();
        forkToLockHandlerFlow.setActivity(mainActivity);
        forkToLockHandlerFlow.setSource(forkNode);
        forkToLockHandlerFlow.setTarget(lockHandlerNode);

        // Remove the Poka Yoke UML profile as all its contents has been transformed
        Profile pokaYokeUmlProfile = model.getAppliedProfile(PokaYokeUmlProfileUtil.POKA_YOKE_PROFILE);
        if (pokaYokeUmlProfile != null) {
            model.unapplyProfile(pokaYokeUmlProfile);
        }
    }

    /**
     * Get all nested behaviors in the model that satisfy the given predicate.
     *
     * @param model The model.
     * @param filter The predicate for filtering behaviors to return.
     * @return The nested behaviors of the provided model that satisfy the given predicate.
     */
    private List<Behavior> getNestedBehaviorsOf(Model model, Predicate<Behavior> filter) {
        List<Behavior> returnValue = new ArrayList<>();

        for (PackageableElement element: model.getPackagedElements()) {
            // Since an element can have multiple types, we don't use else if.

            if (element instanceof Model modelElement) {
                List<Behavior> childBehaviors = getNestedBehaviorsOf(modelElement, filter);
                returnValue.addAll(childBehaviors);
            }

            if (element instanceof Behavior behavior && filter.test(behavior)) {
                returnValue.add(behavior);
            }

            if (element instanceof Class cls) {
                // Skip class generated for lock.
                if (!cls.getName().equals(LOCK_CLASS_NAME)) {
                    for (Behavior behavior: cls.getOwnedBehaviors()) {
                        if (filter.test(behavior)) {
                            returnValue.add(behavior);
                        }
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Get the nested opaque behaviors of the model.
     *
     * @param model The model.
     * @return The nested opaque behaviors of the provided model.
     */
    private List<OpaqueBehavior> getNestedOpaqueBehaviorsOf(Model model) {
        return getNestedBehaviorsOf(model, e -> e instanceof OpaqueBehavior).stream().map(OpaqueBehavior.class::cast)
                .toList();
    }

    /**
     * Translates the given opaque behavior.
     *
     * @param behavior The behavior to translate.
     * @param acquireSignal The signal for acquiring the lock.
     */
    private void transformOpaqueBehavior(OpaqueBehavior behavior, Signal acquireSignal) {
        Preconditions.checkArgument(behavior.getOwnedElements().isEmpty(),
                "Expected opaque behaviors to not have owned elements.");

        // Translate the guard and effects of the given behavior.
        Set<String> variables = getLocalVariableList(behavior);

        CifContext context = CifContext.createScoped(behavior);
        String guard = translateGuard(behavior, context);
        List<List<String>> effects = translateEffects(behavior, context);

        // Define a new activity that encodes the behavior of the action.
        Activity activity = ActivityHelper.createActivity(behavior.getName(), guard, effects, propertyBounds,
                acquireSignal, PokaYokeUmlProfileUtil.isAtomic(behavior), variables);

        // Store the created activity as the single owned behavior of the given opaque behavior.
        behavior.getOwnedBehaviors().add(activity);
    }

    @SuppressWarnings("restriction")
    private Set<String> getLocalVariableList(RedefinableElement behavior) {
        CifContext context = CifContext.createScoped(behavior);

        List<List<AUpdate>> parsedEffects = CifParserHelper.parseEffects(behavior);
        AExpression combinedGuard = getCombinedGuard(behavior);

        CifParameterCollector templateParameterCollector = new CifParameterCollector();
        Stream<NamedTemplateParameter> guardParameters = combinedGuard == null ? Stream.empty()
                : templateParameterCollector.flatten(combinedGuard, context);
        Stream<NamedTemplateParameter> effectParameters = parsedEffects.stream().flatMap(List::stream)
                .flatMap(expr -> templateParameterCollector.flatten(expr, context));
        return Stream.concat(guardParameters, effectParameters).map(p -> p.getName()).collect(Collectors.toSet());
    }

    private Set<String> getLocalVariableList(DecisionNode decisionNode) {
        return decisionNode.getOutgoings().stream().flatMap(edge -> getLocalVariableList(edge).stream())
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("restriction")
    private Set<String> getLocalVariableList(ActivityEdge edge) {
        if (!(edge instanceof ControlFlow controlFlow)) {
            return Collections.EMPTY_SET;
        }

        CifContext context = CifContext.createScoped(edge);
        AExpression incomingGuard = CifParserHelper.parseIncomingGuard(controlFlow);

        if (incomingGuard == null) {
            return Collections.EMPTY_SET;
        }

        CifParameterCollector templateParameterCollector = new CifParameterCollector();
        return templateParameterCollector.flatten(incomingGuard, context).map(NamedTemplateParameter::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Get the nested activities of the model.
     *
     * @param model The model.
     * @return The nested activities of the provided model.
     */
    private List<Activity> getNestedActivitiesOf(Model model) {
        return getNestedBehaviorsOf(model, e -> e instanceof Activity).stream().map(Activity.class::cast).toList();
    }

    /**
     * Get the nested non-activity classes of the model.
     *
     * @param model The model.
     * @return The nested non-activity classes of the provided model.
     */
    private List<Class> getNestedNonActivityClassesOf(Model model) {
        List<Class> returnValue = new ArrayList<>();
        for (PackageableElement element: model.getPackagedElements()) {
            if (element instanceof Model modelElement) {
                final List<Class> modelElementClasses = getNestedNonActivityClassesOf(modelElement);
                returnValue.addAll(modelElementClasses);
            } else if (element instanceof Class classElement && !(element instanceof Activity)) {
                returnValue.add(classElement);
            }
        }
        return returnValue;
    }

    private void transformActivity(Activity activity, Signal acquireSignal) {
        String activityName = activity.getName();

        Preconditions.checkArgument(model.getPackagedElement(activityName) == null,
                String.format("Expected the '%s' class to not already exist.", activityName));

        UMLActivityUtils.removeIrrelevantInformation(activity);

        // Transform all opaque action nodes and decision nodes of the activity.
        for (ActivityNode node: new ArrayList<>(activity.getNodes())) {
            if (node instanceof OpaqueAction opaqueActionNode) {
                transformOpaqueAction(activity, opaqueActionNode, acquireSignal);
            } else if (node instanceof CallBehaviorAction callBehaviorAction) {
                transformCallBehaviorAction(activity, callBehaviorAction, acquireSignal);
            } else if (node instanceof DecisionNode decisionNode) {
                transformDecisionNode(decisionNode);
            }
        }

        if (activity.getOwnedTemplateSignature() instanceof RedefinableTemplateSignature templateSignature) {
            transformTemplateSignature(activity, templateSignature);
        }
    }

    private void transformTemplateSignature(Activity activity, RedefinableTemplateSignature templateSignature) {
        for (TemplateParameter parameter: templateSignature.getParameters()) {
            if (parameter instanceof ClassifierTemplateParameter) {
                String name = ((NamedElement)parameter.getParameteredElement()).getName();

                ActivityHelper.addParameterToActivity(activity, name);
            }
        }
    }

    private void transformCallBehaviorActionArguments(CallBehaviorAction callAction) {
        List<AAssignmentUpdate> parsedAssignments = CifParserHelper.parseActivityArguments(callAction);
        if (parsedAssignments.isEmpty()) {
            return;
        }

        CifContext cifScope = CifContext.createScoped(callAction);

        List<String> translatedAssignments = new ArrayList<>();
        Set<String> arguments = new HashSet<>();
        for (AAssignmentUpdate assignment: parsedAssignments) {
            String adressable = ((ANameExpression)assignment.addressable).name.name;
            String value = translator.translateExpression(assignment.value, cifScope);

            translatedAssignments.add(PARAM_PREFIX + adressable + "=" + value);
            arguments.add(adressable);
        }

        String pythonBody = CifToPythonTranslator.mergeAll(translatedAssignments, "\n").get();

        ActivityHelper.passVariablesToCallBehaviorAction(callAction, new HashSet<>(arguments), pythonBody);
    }

    private void transformCallBehaviorAction(Activity activity, CallBehaviorAction action, Signal acquireSignal) {
        if (PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
            transformAction(activity, action, acquireSignal);
        } else if (action.getBehavior() instanceof OpaqueBehavior behavior) {
            // Check whether 'action' has an outgoing guard on the incoming edge.
            boolean incomingEdgeHasOutgoingGuard = PokaYokeUmlProfileUtil
                    .getOutgoingGuard(action.getIncomings().get(0)) != null;

            // If the action has an outgoing guard on the incoming edge, we must translate the outgoing guard in such a
            // way that the called behavior can only be performed if the outgoing guard holds. To do that, we can't
            // simply call the activity that's created for the called opaque behavior. Instead, we translate the
            // behavior as an opaque action, and consider the outgoing guard to be extra action guards. To do this
            // translation, we first shadow the call behavior node by lifting the guard, effects, and atomicity of the
            // called behavior to the call node, and then translate the call node as an action.
            if (incomingEdgeHasOutgoingGuard) {
                PokaYokeUmlProfileUtil.setGuard(action, PokaYokeUmlProfileUtil.getGuard(behavior));
                PokaYokeUmlProfileUtil.setEffects(action, PokaYokeUmlProfileUtil.getEffects(behavior));
                PokaYokeUmlProfileUtil.setAtomic(action, PokaYokeUmlProfileUtil.isAtomic(behavior));
                transformAction(activity, action, acquireSignal);
            } else {
                Verify.verify(behavior.getOwnedBehaviors().size() == 1,
                        "The opaque behavior owns more than one activity.");
                action.setBehavior(behavior.getOwnedBehaviors().get(0));
            }
        }

        transformCallBehaviorActionArguments(action);
    }

    private void transformOpaqueAction(Activity activity, OpaqueAction action, Signal acquireSignal) {
        transformAction(activity, action, acquireSignal);
    }

    private void transformAction(Activity activity, Action action, Signal acquireSignal) {
        Set<String> localVariables = getLocalVariableList(action);

        // Translate the guard and effects of the action.
        CifContext context = CifContext.createScoped(action);
        String guard = translateGuard(action, context);
        List<List<String>> effects = translateEffects(action, context);

        // Define a new activity that encodes the behavior of the action.
        String actionName = action.getName();
        Activity newActivity = ActivityHelper.createActivity(actionName, guard, effects, propertyBounds, acquireSignal,
                PokaYokeUmlProfileUtil.isAtomic(action), localVariables);

        // Define the call behavior action that replaces the action in the activity.
        CallBehaviorAction replacementActionNode = FileHelper.FACTORY.createCallBehaviorAction();
        replacementActionNode.setActivity(activity);
        replacementActionNode.setBehavior(newActivity);
        replacementActionNode.setName(actionName);

        // Relocate the incoming edge into the action to the replacement action.
        action.getIncomings().get(0).setTarget(replacementActionNode);

        // Relocate the outgoing edge out of the action to the replacement action.
        action.getOutgoings().get(0).setSource(replacementActionNode);

        // Remove the old action that is now replaced.
        action.destroy();
        activity.getOwnedBehaviors().add(newActivity);

        ActivityHelper.passVariablesToCallBehaviorAction(replacementActionNode, localVariables, null);
    }

    /**
     * Translates the guard of the specified element.
     *
     * @param element The element of which to translate the guard.
     * @param context The context in which the element is translated.
     * @return The translated guard.
     */
    private String translateGuard(RedefinableElement element, CifContext context) {
        AExpression combinedGuard = getCombinedGuard(element);
        return translator.translateExpression(combinedGuard, context);
    }

    private AExpression getCombinedGuard(RedefinableElement element) {
        // Get the outgoing guard of the incoming edge, if element is an activity node.
        AExpression extraGuard = null;
        if (element instanceof ActivityNode activityNode) {
            List<ActivityEdge> incomingEdges = activityNode.getIncomings();
            Verify.verify(incomingEdges.size() == 1);
            extraGuard = CifParserHelper.parseOutgoingGuard((ControlFlow)incomingEdges.get(0));
        }

        // Get the guard of the element. Conjunct with the extra guard if relevant.
        AExpression elementGuard = CifParserHelper.parseGuard(element);
        if (extraGuard != null && elementGuard != null) {
            extraGuard = new ABinaryExpression("and", extraGuard, elementGuard, null);
        } else {
            extraGuard = (extraGuard == null) ? elementGuard : extraGuard;
        }

        return extraGuard;
    }

    /**
     * Translates the effects of the specified element.
     *
     * @param element The element of which to translate the effects.
     * @param context The context in which the element is translated.
     * @return The translated effects.
     */
    private List<List<String>> translateEffects(RedefinableElement element, CifContext context) {
        // Parse all effects.
        List<List<AUpdate>> parsedEffects = CifParserHelper.parseEffects(element);

        // Rename all variables on the right-hand sides of assignments by prefixing them with 'pre__'.
        Map<String, String> renaming = new LinkedHashMap<>();
        List<List<AUpdate>> renamedEffects = new EffectPrestateRenamer(context).renameEffects(parsedEffects, renaming);

        // Translate all parsed and renamed effects.
        List<List<String>> translatedEffects = renamedEffects.stream()
                .map(effects -> translator.translateUpdates(effects, context)).toList();

        // From the renaming map, construct pre-state assignments of the form 'pre__X = X' with 'X' a renamed variable.
        List<String> prestateAssignments = renaming.entrySet().stream()
                .filter(entry -> entry.getValue().startsWith(EffectPrestateRenamer.PREFIX))
                .map(entry -> entry.getValue() + " = " + entry.getKey()).toList();

        // Add these pre-state assignments to the front of every translated effect, and return the result.
        return translatedEffects.stream().map(effects -> Lists.concat(prestateAssignments, effects)).toList();
    }

    private void transformDecisionNode(DecisionNode decisionNode) {
        // Create the activity that evaluates the incoming guards of the outgoing control flows of the decision node.
        Activity evalActivity = ActivityHelper.createDecisionEvaluationActivity(decisionNode, translator);
        decisionNode.getActivity().getOwnedBehaviors().add(evalActivity);
        evalActivity.setName("eval");

        Set<String> localVariables = getLocalVariableList(decisionNode);

        // Add template parameters to the newly created activity for local variables that are used in the program of the
        // decision node.
        for (String variableName: localVariables) {
            ActivityHelper.addParameterToActivity(evalActivity, variableName);
        }

        // Create the call behavior node that calls the activity we just created.
        CallBehaviorAction evalNode = FileHelper.FACTORY.createCallBehaviorAction();
        evalNode.setActivity(decisionNode.getActivity());
        evalNode.setBehavior(evalActivity);

        // Redirect the incoming edge into the decision node to go into the new evaluation node instead.
        decisionNode.getIncomings().get(0).setTarget(evalNode);

        // Pass local variables to the activity.
        ActivityHelper.passVariablesToCallBehaviorAction(evalNode, localVariables, null);

        // Define the control flow from the new evaluator node to the decision node.
        ControlFlow evalToDecisionFlow = FileHelper.FACTORY.createControlFlow();
        evalToDecisionFlow.setActivity(decisionNode.getActivity());
        evalToDecisionFlow.setSource(evalNode);
        evalToDecisionFlow.setTarget(decisionNode);

        // Define the object flow from the new evaluator node to the decision node.
        OutputPin evalOutput = evalNode.createResult("branch", UmlPrimitiveType.INTEGER.load(decisionNode));
        ObjectFlow evalToDecisionObjFlow = FileHelper.FACTORY.createObjectFlow();
        evalToDecisionObjFlow.setActivity(decisionNode.getActivity());
        evalToDecisionObjFlow.setSource(evalOutput);
        evalToDecisionObjFlow.setTarget(decisionNode);

        // Update the guards of all outgoing edges accordingly to the outcome of the decision evaluation node.
        for (int i = 0; i < decisionNode.getOutgoings().size(); i++) {
            ActivityEdge edge = decisionNode.getOutgoings().get(i);
            OpaqueExpression newGuard = FileHelper.FACTORY.createOpaqueExpression();
            newGuard.getLanguages().add("Python");
            newGuard.getBodies().add("branch == " + i);
            edge.setGuard(newGuard);
        }
    }
}
