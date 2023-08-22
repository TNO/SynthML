
package com.github.tno.pokayoke.transform.uml;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.parser.CifExpressionParser;
import org.eclipse.escet.cif.parser.CifUpdateParser;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;

/**
 * Transforms UML models that are annotated with guards, effects, preconditions, etc., to valid and executable UML, in
 * the sense that all such annotations are translated to valid UML. The annotation language is assumed to be CIF.
 */
public class UMLTransformer {
    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final Model model;

    private final ModelTyping typing;

    private final CifToPythonTranslator translator;

    private final CifUpdateParser updateParser = new CifUpdateParser();

    public UMLTransformer(Model model) {
        this.model = model;
        this.typing = new ModelTyping(this.model);
        this.translator = new CifToPythonTranslator(this.typing);
    }

    public static void transformFile(String sourcePath, String targetPath) throws IOException {
        Model model = FileHelper.loadModel(sourcePath);
        new UMLTransformer(model).transformModel();
        FileHelper.storeModel(model, targetPath);
    }

    public void transformModel() {
        // 1. Define locking infrastructure.

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

        // Create the signal event for 'acquireSignal' to trigger on.
        SignalEvent acquireEvent = FileHelper.FACTORY.createSignalEvent();
        acquireEvent.setSignal(acquireSignal);
        acquireEvent.setVisibility(VisibilityKind.PUBLIC_LITERAL);
        model.getPackagedElements().add(acquireEvent);

        // Create the activity that handles lock acquisition.
        Activity lockHandlerActivity = ActivityHelper.createLockHanderActivity(acquireEvent);
        lockHandlerActivity.setName("lockhandler");
        lockClass.getOwnedBehaviors().add(lockHandlerActivity);

        // 2. Transform the 'Context' class.

        Class contextClass = (Class)model.getMember("Context");

        // Create the static property that indicates the current owner of the lock (if any).
        Property activeProperty = FileHelper.FACTORY.createProperty();
        activeProperty.setIsStatic(true);
        activeProperty.setName("active");
        activeProperty.setType(FileHelper.loadPrimitiveType("String"));
        LiteralString activePropertyDefaultValue = FileHelper.FACTORY.createLiteralString();
        activePropertyDefaultValue.setValue("");
        activeProperty.setDefaultValue(activePropertyDefaultValue);
        contextClass.getOwnedAttributes().add(activeProperty);

        // Transform all activity behaviors of 'contextClass'.
        for (Behavior behavior: new LinkedHashSet<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity) {
                transformActivity((Activity)behavior, acquireSignal);
            }
        }

        // 3. Transform the classifier behavior (i.e., main activity) of the 'Context' class.

        // Obtain the main activity.
        Activity mainActivity = (Activity)contextClass.getClassifierBehavior();

        if (!mainActivity.getName().equals("main")) {
            throw new RuntimeException("Expected a main activity diagram.");
        }

        // Obtain the single fork node that 'mainActivity' should have.
        List<ForkNode> forkNodes = mainActivity.getNodes().stream().filter(n -> n instanceof ForkNode)
                .map(n -> (ForkNode)n).collect(Collectors.toList());

        if (forkNodes.size() != 1) {
            throw new RuntimeException("Expected the 'main' activity diagram to have exactly one fork node.");
        }

        ForkNode forkNode = forkNodes.get(0);

        // Define the action that calls the lock handler.
        CallBehaviorAction lockHandlerNode = FileHelper.FACTORY.createCallBehaviorAction();
        lockHandlerNode.setActivity(mainActivity);
        lockHandlerNode.setBehavior(lockHandlerActivity);

        // Define the control flow from 'forkNode' to 'lockHandlerNode'.
        ControlFlow forkToLockHandlerFlow = FileHelper.FACTORY.createControlFlow();
        forkToLockHandlerFlow.setActivity(mainActivity);
        forkToLockHandlerFlow.setSource(forkNode);
        forkToLockHandlerFlow.setTarget(lockHandlerNode);
    }

    private void transformActivity(Activity activity, Signal acquireSignal) {
        ActivityHelper.removeIrrelevantInformation(activity);

        // Create a separate class in which all new behaviors for 'activity' are stored.
        Class activityClass = (Class)model.createPackagedElement(activity.getName(), UMLPackage.eINSTANCE.getClass_());

        // Transform all opaque action nodes of 'activity'.
        for (ActivityNode node: new LinkedHashSet<>(activity.getNodes())) {
            if (node instanceof OpaqueAction) {
                transformOpaqueAction(activityClass, activity, (OpaqueAction)node, acquireSignal);
            }
        }
    }

    private void transformOpaqueAction(Class activityClass, Activity activity, OpaqueAction action,
            Signal acquireSignal)
    {
        // Extract the guard of 'action', to be encoded later.
        // If 'action' has at least one body, then parse the first body, which is assumed to be its guard.
        String guard = "True";

        if (!action.getBodies().isEmpty()) {
            AExpression cifGuard = expressionParser.parseString(action.getBodies().get(0), "");
            guard = translator.translateExpression(cifGuard);
        }

        // Extract the effect (i.e., a list of updates) of 'action', to be encoded later.
        // If 'action' has at least two bodies, parse all bodies except the first one, which are assumed to be updates.
        String effect = "pass";

        if (action.getBodies().size() > 1) {
            effect = action.getBodies().stream().skip(1).map(update -> updateParser.parseString(update, ""))
                    .map(cifUpdate -> translator.translateUpdate(cifUpdate))
                    .map(pythonUpdate -> "if guard: " + pythonUpdate).collect(Collectors.joining("\n"));
        }

        // Define a new activity that encodes the behavior of 'action'.
        Activity actionActivity = ActivityHelper.createAtomicActivity(guard, effect, acquireSignal);
        actionActivity.setName(action.getName());
        activityClass.getOwnedBehaviors().add(actionActivity);

        // Define the call behavior action that replaces 'action' in 'activity'.
        CallBehaviorAction replacementActionNode = FileHelper.FACTORY.createCallBehaviorAction();
        replacementActionNode.setActivity(activity);
        replacementActionNode.setBehavior(actionActivity);
        replacementActionNode.setName(action.getName());

        // Relocate all incoming edges into 'action' to 'replacementAction'.
        for (ActivityEdge edge: new LinkedHashSet<>(action.getIncomings())) {
            edge.setTarget(replacementActionNode);
        }

        // Relocate all outgoing edges out of 'action' to 'replacementAction'.
        for (ActivityEdge edge: new LinkedHashSet<>(action.getOutgoings())) {
            edge.setSource(replacementActionNode);
        }

        // Remove the old 'action' that is now replaced.
        action.setActivity(null);
    }
}
