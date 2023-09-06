
package com.github.tno.pokayoke.transform.uml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;

import com.github.tno.pokayoke.transform.common.FileHelper;
import com.github.tno.pokayoke.transform.common.UMLActivityUtils;
import com.github.tno.pokayoke.transform.common.UMLValidatorSwitch;

/**
 * Transforms UML models that are annotated with guards, effects, etc., to valid and executable UML, in the sense that
 * all such annotations are translated to valid UML. The annotation language is assumed to be CIF.
 */
public class UMLTransformer {
    private final Model model;

    private final String modelPath;

    private final ModelTyping typing;

    private final CifToPythonTranslator translator;

    public UMLTransformer(Model model, String modelPath) {
        this.model = model;
        this.modelPath = modelPath;
        this.typing = new ModelTyping(this.model);
        this.translator = new CifToPythonTranslator(this.typing);
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        new UMLTransformer(model, sourcePath).transform();
        FileHelper.storeModel(model, targetPath);
    }

    public void transform() {
        // Create the signal for acquiring the lock.
        Signal acquireSignal = FileHelper.FACTORY.createSignal();
        acquireSignal.setName("__acquire");
        Property acquireParameter = FileHelper.FACTORY.createProperty();
        acquireParameter.setName("requester");
        acquireParameter.setType(FileHelper.loadPrimitiveType("String"));
        acquireSignal.getOwnedAttributes().add(acquireParameter);

        // Create the signal event for the acquire signal to trigger on.
        SignalEvent acquireEvent = FileHelper.FACTORY.createSignalEvent();
        acquireEvent.setSignal(acquireSignal);
        acquireEvent.setVisibility(VisibilityKind.PUBLIC_LITERAL);

        // Create the activity that handles lock acquisition.
        Activity lockHandlerActivity = ActivityHelper.createLockHanderActivity(acquireEvent);
        lockHandlerActivity.setName("__lockhandler");

        // Transform the classes and activities within the model.
        new Transformer(acquireSignal, lockHandlerActivity).doSwitch(model);

        // Create a class for holding lock-related structure and behavior defined earlier.
        Class lockClass = (Class)model.createPackagedElement("__Lock", UMLPackage.eINSTANCE.getClass_());
        lockClass.getNestedClassifiers().add(acquireSignal);
        lockClass.getOwnedBehaviors().add(lockHandlerActivity);

        // Add the lock acquire event defined earlier to the model.
        model.getPackagedElements().add(acquireEvent);
    }

    class Transformer extends UMLValidatorSwitch {
        private final Signal acquireSignal;

        private final Activity lockHandlerActivity;

        private final CifUpdateParser updateParser = new CifUpdateParser();

        private final CifExpressionParser expressionParser = new CifExpressionParser();

        Transformer(Signal acquireSignal, Activity lockHandlerActivity) {
            this.acquireSignal = acquireSignal;
            this.lockHandlerActivity = lockHandlerActivity;
        }

        @Override
        public Object caseClass(Class classElement) {
            Object visitedClassElement = super.caseClass(classElement);

            // Create the static property that indicates the current owner of the lock (if any).
            Property activeProperty = FileHelper.FACTORY.createProperty();
            activeProperty.setIsStatic(true);
            activeProperty.setName("__active");
            activeProperty.setType(FileHelper.loadPrimitiveType("String"));
            LiteralString activePropertyDefaultValue = FileHelper.FACTORY.createLiteralString();
            activePropertyDefaultValue.setValue("");
            activeProperty.setDefaultValue(activePropertyDefaultValue);
            classElement.getOwnedAttributes().add(activeProperty);

            // Obtain the classifier behavior of the class to transform, by adding a call to the lock handler to it.
            Activity classifierBehavior = (Activity)classElement.getClassifierBehavior();

            // Obtain the single initial node of the classified behavior.
            InitialNode initialNode = classifierBehavior.getNodes().stream().filter(n -> n instanceof InitialNode)
                    .map(n -> (InitialNode)n).findAny().get();

            // Create a fork node to start the lock handler in parallel to the rest of the classified behavior.
            ForkNode forkNode = FileHelper.FACTORY.createForkNode();
            forkNode.setActivity(classifierBehavior);

            // Relocate all outgoing edges out of the initial node to go out of the fork node instead.
            for (ActivityEdge edge: new ArrayList<>(initialNode.getOutgoings())) {
                edge.setSource(forkNode);
            }

            // Add an edge between the initial node and the new fork node.
            ControlFlow initToForkFlow = FileHelper.FACTORY.createControlFlow();
            initToForkFlow.setActivity(classifierBehavior);
            initToForkFlow.setSource(initialNode);
            initToForkFlow.setTarget(forkNode);

            // Define the action that calls the lock handler.
            CallBehaviorAction lockHandlerNode = FileHelper.FACTORY.createCallBehaviorAction();
            lockHandlerNode.setActivity(classifierBehavior);
            lockHandlerNode.setBehavior(lockHandlerActivity);

            // Define the control flow from the new fork node to the node that calls the lock handler.
            ControlFlow forkToLockHandlerFlow = FileHelper.FACTORY.createControlFlow();
            forkToLockHandlerFlow.setActivity(classifierBehavior);
            forkToLockHandlerFlow.setSource(forkNode);
            forkToLockHandlerFlow.setTarget(lockHandlerNode);

            return visitedClassElement;
        }

        @Override
        public Object caseActivity(Activity activity) {
            UMLActivityUtils.removeIrrelevantInformation(activity);
            return super.caseActivity(activity);
        }

        @Override
        public Object caseOpaqueAction(OpaqueAction action) {
            Activity activity = action.getActivity();

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
            activity.getOwnedBehaviors().add(actionActivity);

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

            return doSwitch(replacementActionNode);
        }

        private AExpression parseExpression(String expression) {
            return expressionParser.parseString(expression, modelPath);
        }

        private AUpdate parseUpdate(String update) {
            return updateParser.parseString(update, modelPath);
        }
    }
}
