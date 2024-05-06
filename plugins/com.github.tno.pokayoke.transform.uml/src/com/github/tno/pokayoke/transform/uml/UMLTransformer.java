
package com.github.tno.pokayoke.transform.uml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.ObjectFlow;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.OutputPin;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.ValueSpecification;
import org.eclipse.uml2.uml.VisibilityKind;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.common.UMLActivityUtils;
import com.github.tno.pokayoke.transform.common.ValidationHelper;
import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifParserHelper;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeUmlProfileUtil;
import com.github.tno.pokayoke.uml.profile.util.UmlPrimitiveType;
import com.google.common.base.Preconditions;

/**
 * Transforms UML models that are annotated with guards, effects, preconditions, etc., to valid and executable UML, in
 * the sense that all such annotations are translated to valid UML. The annotation language is assumed to be CIF.
 */
public class UMLTransformer {
    /** Name for the lock class. */
    private static final String LOCK_CLASS_NAME = "Lock";

    private final Model model;

    private final CifToPythonTranslator translator;

    public UMLTransformer(Model model) {
        this.model = model;
        this.translator = new CifToPythonTranslator(new CifContext(this.model));
    }

    public static void main(String[] args) throws IOException, CoreException {
        if (args.length == 2) {
            transformFile(args[0], args[1]);
        } else {
            throw new IOException("Exactly two arguments expected: a source path and a target path.");
        }
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException, CoreException {
        Model model = FileHelper.loadModel(sourcePath);
        new UMLTransformer(model).transformModel();
        FileHelper.storeModel(model, targetPath);
    }

    public void transformModel() throws CoreException {
        // 1. Check whether the model has the expected structure and obtain relevant information from it.
        ValidationHelper.validateModel(model);

        Preconditions.checkArgument(model.getPackagedElement(LOCK_CLASS_NAME) == null,
                "Expected no packaged element named 'Lock' to already exist.");

        // Obtain the single class that should be defined within the model.
        List<Class> modelNestedClasses = getNestedNonActivityClassesOf(model);
        Preconditions.checkArgument(modelNestedClasses.size() == 1,
                "Expected the model to contain exactly one class, got " + modelNestedClasses.size());
        Class contextClass = modelNestedClasses.get(0);

        // Translate all default values of class properties that are literal strings, to become opaque actions.
        for (Property property: contextClass.getOwnedAttributes()) {
            AExpression cifExpression = CifParserHelper.parseExpression(property.getDefaultValue());
            if (cifExpression != null) {
                OpaqueExpression newDefaultValue = FileHelper.FACTORY.createOpaqueExpression();
                newDefaultValue.getLanguages().add("Python");
                String translatedLiteral = translator.translateExpression(cifExpression);
                newDefaultValue.getBodies().add(translatedLiteral);
                property.setDefaultValue(newDefaultValue);
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

        // Relocate all outgoing edges out of the initial node to go out of the fork node instead.
        for (ActivityEdge edge: new ArrayList<>(initialNode.getOutgoings())) {
            edge.setSource(forkNode);
        }

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
     * Get the nested activities of the model.
     *
     * @param model The model.
     * @return The nested activities of the provided model.
     */
    private List<Activity> getNestedActivitiesOf(Model model) {
        List<Activity> returnValue = new ArrayList<>();
        for (PackageableElement element: model.getPackagedElements()) {
            // Since an element can have multiple types, we don't use else if.

            if (element instanceof Model modelElement) {
                List<Activity> childActivities = getNestedActivitiesOf(modelElement);
                returnValue.addAll(childActivities);
            }

            if (element instanceof Activity activityElement) {
                returnValue.add(activityElement);
            }

            if (element instanceof Class cls) {
                // Skip class generated for lock.
                if (!cls.getName().equals(LOCK_CLASS_NAME)) {
                    for (Behavior behavior: cls.getOwnedBehaviors()) {
                        if (behavior instanceof Activity activity) {
                            returnValue.add(activity);
                        }
                    }
                }
            }
        }
        return returnValue;
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
    }

    private void transformCallBehaviorAction(Activity activity, CallBehaviorAction action, Signal acquireSignal) {
        if (PokaYokeUmlProfileUtil.isGuardEffectsAction(action)) {
            transformAction(activity, action, acquireSignal);
        }
    }

    private void transformOpaqueAction(Activity activity, OpaqueAction action, Signal acquireSignal) {
        transformAction(activity, action, acquireSignal);
    }

    private void transformAction(Activity activity, Action action, Signal acquireSignal) {
        // Extract the guard of the action, if any, to be encoded later.
        // If the action has at least one body, then parse the first body, which is assumed to be its guard.
        String guard = translator.translateExpression(CifParserHelper.parseGuard(action));

        // Extract the effects of the action, if any, to be encoded later.
        // Parse all bodies except the first one, all of which should be updates.
        List<String> effects = translator.translateUpdates(CifParserHelper.parseEffects(action));

        // Define a new activity that encodes the behavior of the action.
        Activity newActivity = ActivityHelper.createAtomicActivity(guard, effects, acquireSignal,
                action.getQualifiedName());
        String actionName = action.getName();
        newActivity.setName(actionName);

        // Define the call behavior action that replaces the action in the activity.
        CallBehaviorAction replacementActionNode = FileHelper.FACTORY.createCallBehaviorAction();
        replacementActionNode.setActivity(activity);
        replacementActionNode.setBehavior(newActivity);
        replacementActionNode.setName(actionName);

        // Relocate all incoming edges into the action to the replacement action.
        for (ActivityEdge edge: new ArrayList<>(action.getIncomings())) {
            edge.setTarget(replacementActionNode);
        }

        // Relocate all outgoing edges out of the action to the replacement action.
        for (ActivityEdge edge: new ArrayList<>(action.getOutgoings())) {
            edge.setSource(replacementActionNode);
        }

        // Remove the old action that is now replaced.
        action.destroy();
        activity.getOwnedBehaviors().add(newActivity);
    }

    private void transformDecisionNode(DecisionNode decisionNode) {
        // Define an action that evaluates the guards of all outgoing edges, and non-deterministically chooses one edge
        // whose guard holds.
        OpaqueAction decisionEvaluationNode = FileHelper.FACTORY.createOpaqueAction();
        decisionEvaluationNode.setActivity(decisionNode.getActivity());
        decisionEvaluationNode.getLanguages().add("Python");

        // Define the Python body program of the decision evaluation node.
        StringBuilder decisionEvaluationProgram = new StringBuilder();
        decisionEvaluationProgram.append("import random\n");
        decisionEvaluationProgram.append("branches = []\n");

        for (int i = 0; i < decisionNode.getOutgoings().size(); i++) {
            ActivityEdge edge = decisionNode.getOutgoings().get(i);
            ValueSpecification edgeGuard = edge.getGuard();
            String translatedGuard = translator.translateExpression(CifParserHelper.parseExpression(edgeGuard));
            decisionEvaluationProgram.append("if " + translatedGuard + ": branches.append(" + i + ")\n");
        }

        decisionEvaluationProgram.append("branch = random.choice(branches)\n");
        decisionEvaluationNode.getBodies().add(decisionEvaluationProgram.toString());

        // Redirect the incoming edges into the decision node to go into the new evaluation node instead.
        for (ActivityEdge incomingEdge: new ArrayList<>(decisionNode.getIncomings())) {
            incomingEdge.setTarget(decisionEvaluationNode);
        }

        // Define the control flow from the new evaluator node to the decision node.
        ControlFlow evaluationToDecisionFlow = FileHelper.FACTORY.createControlFlow();
        evaluationToDecisionFlow.setActivity(decisionNode.getActivity());
        evaluationToDecisionFlow.setSource(decisionEvaluationNode);
        evaluationToDecisionFlow.setTarget(decisionNode);

        // Define the object flow from the new evaluator node to the decision node.
        OutputPin evaluationOutput = decisionEvaluationNode.createOutputValue("branch",
                UmlPrimitiveType.INTEGER.load(decisionNode));
        ObjectFlow evaluationToDecisionObjFlow = FileHelper.FACTORY.createObjectFlow();
        evaluationToDecisionObjFlow.setActivity(decisionNode.getActivity());
        evaluationToDecisionObjFlow.setSource(evaluationOutput);
        evaluationToDecisionObjFlow.setTarget(decisionNode);

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
