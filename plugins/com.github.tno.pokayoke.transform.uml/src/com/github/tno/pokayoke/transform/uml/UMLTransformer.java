
package com.github.tno.pokayoke.transform.uml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.escet.setext.runtime.exceptions.ParseException;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.common.UMLActivityUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * Transforms UML models that are annotated with guards, effects, preconditions, etc., to valid and executable UML, in
 * the sense that all such annotations are translated to valid UML. The annotation language is assumed to be CIF.
 */
public class UMLTransformer {
    private final Model model;

    private final String modelPath;

    private final CifUpdateParser updateParser = new CifUpdateParser();

    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final ModelTyping typing;

    private final CifToPythonTranslator translator;

    public UMLTransformer(Model model, String modelPath) {
        this.model = model;
        this.modelPath = modelPath;
        this.typing = new ModelTyping(this.model);
        this.translator = new CifToPythonTranslator(this.typing);
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            transformFile(args[0], args[1]);
        } else {
            System.out.println("Two arguments expected: sourcePath and targetPath");
        }
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        new UMLTransformer(model, sourcePath).transformModel();
        FileHelper.storeModel(model, targetPath);
    }

    private List<Class> getNestedClassesOf(Model modelElement) {
        List<Class> returnValue = new ArrayList<>();
        for (PackageableElement element: modelElement.getPackagedElements()) {
            if (element instanceof Model childElement) {
                List<Class> childClasses = getNestedClassesOf(childElement);
                returnValue.addAll(childClasses);
            } else if (element instanceof Class classElement) {
                // element can be both Class and Activity
                if (!(element instanceof Activity)) {
                    returnValue.add(classElement);
                }
            }
        }
        return returnValue;
    }

    public void transformModel() {
        // 1. Check whether the model has the expected structure and obtain relevant information from it.

        Preconditions.checkArgument(model.getPackagedElement("Lock") == null,
                "Expected no packaged element named 'Lock' to already exist.");

        // Obtain the single class that should be defined within the model.
        List<Class> modelClasses = getNestedClassesOf(model);
        Preconditions.checkArgument(modelClasses.size() == 1,
                "Expected the model to contain exactly one class, got " + modelClasses.size());
        Class contextClass = modelClasses.get(0);

        // Make sure the class does not contain an attribute named 'active'.
        Preconditions.checkArgument(
                contextClass.getOwnedAttributes().stream().noneMatch(a -> a.getName().equals("active")),
                "Expected no attribute named 'active' to already exist in the single class of the model.");

        // Obtain the activity that the single class within the model should have, as classifier behavior.
        Preconditions.checkNotNull(contextClass.getClassifierBehavior(),
                "Expected the single class within the model to have a classifier behavior.");
        Preconditions.checkArgument(contextClass.getClassifierBehavior() instanceof Activity,
                "Expected the classifier behavior of the single class within the model to be an activity.");
        Activity mainActivity = (Activity)contextClass.getClassifierBehavior();

        // 2. Define locking infrastructure.

        // Create a class for holding lock-related structure and behavior.
        Class lockClass = (Class)model.createPackagedElement("Lock", UMLPackage.eINSTANCE.getClass_());

        // Create the signal for acquiring the lock.
        Signal acquireSignal = FileHelper.FACTORY.createSignal();
        acquireSignal.setName("acquire");
        Property acquireParameter = FileHelper.FACTORY.createProperty();
        acquireParameter.setName("requester");
        acquireParameter.setType(FileHelper.loadPrimitiveType("String"));
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
        activeProperty.setType(FileHelper.loadPrimitiveType("String"));
        LiteralString activePropertyDefaultValue = FileHelper.FACTORY.createLiteralString();
        activePropertyDefaultValue.setValue("");
        activeProperty.setDefaultValue(activePropertyDefaultValue);
        contextClass.getOwnedAttributes().add(activeProperty);

        // Transform all activity behaviors of the single class within the model.
        for (Behavior behavior: new ArrayList<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity) {
                transformActivity(activity, acquireSignal);
            }
        }

        // 4. Transform the classifier behavior (i.e., main activity) of the single class within the model.

        // Obtain the single initial node of the main activity.
        List<InitialNode> initialNodes = mainActivity.getNodes().stream().filter(n -> n instanceof InitialNode)
                .map(n -> (InitialNode)n).toList();
        Preconditions.checkArgument(initialNodes.size() == 1,
                "Expected the classified behavior of the class of the model to have exactly one initial node.");
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
    }

    private void transformActivity(Activity activity, Signal acquireSignal) {
        String activityName = activity.getName();

        Preconditions.checkArgument(model.getPackagedElement(activity.getName()) == null,
                String.format("Expected the '%s' class to not already exist.", activityName));

        UMLActivityUtils.removeIrrelevantInformation(activity);

        // Create a separate class in which all new behaviors for the activity are stored.
        Class activityClass = (Class)model.createPackagedElement(activityName, UMLPackage.eINSTANCE.getClass_());

        // Transform all opaque action nodes of the activity.
        for (ActivityNode node: new ArrayList<>(activity.getNodes())) {
            if (node instanceof OpaqueAction opaqueActionNode) {
                transformOpaqueAction(activityClass, activity, opaqueActionNode, acquireSignal);
            }
        }
    }

    private void transformOpaqueAction(Class activityClass, Activity activity, OpaqueAction action,
            Signal acquireSignal)
    {
        // Extract the guard of the action, if any, to be encoded later.
        // If the action has at least one body, then parse the first body, which is assumed to be its guard.
        List<String> guards = action.getBodies().stream().limit(1).map(b -> parseExpression(b))
                .map(b -> translator.translateExpression(b)).toList();

        // Extract the effects of the action, if any, to be encoded later.
        // Parse all bodies except the first one, all of which should be updates.
        List<String> effects = action.getBodies().stream().skip(1).map(b -> parseUpdate(b))
                .map(b -> translator.translateUpdate(b)).toList();

        // Define a new activity that encodes the behavior of the action.
        Activity actionActivity = ActivityHelper.createAtomicActivity(guards, effects, acquireSignal);
        actionActivity.setName(action.getName());
        Verify.verify(activityClass.getOwnedBehavior(action.getName()) == null,
                String.format("Expected the '%s' activity to not already exist.", action.getName()));
        activityClass.getOwnedBehaviors().add(actionActivity);

        // Define the call behavior action that replaces the action in the activity.
        CallBehaviorAction replacementActionNode = FileHelper.FACTORY.createCallBehaviorAction();
        replacementActionNode.setActivity(activity);
        replacementActionNode.setBehavior(actionActivity);
        replacementActionNode.setName(action.getName());

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
    }

    private AExpression parseExpression(String expression) {
        try {
            return expressionParser.parseString(expression, modelPath);
        } catch (ParseException pe) {
            System.err.println("Parsing of \"" + expression + "\" failed.");
            throw pe;
        }
    }

    private AUpdate parseUpdate(String update) {
        return updateParser.parseString(update, modelPath);
    }
}
