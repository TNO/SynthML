
package com.github.tno.pokayoke.transform.uml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.OpaqueExpression;
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
    private Signal acquireSignal;

    private final CifExpressionParser expressionParser = new CifExpressionParser();

    private final Model model;

    private final ModelTyping modelTyping;

    private final Map<Activity, Activity> preconditions = new LinkedHashMap<>();

    private final CifToPythonTranslator translator;

    private final CifUpdateParser updateParser = new CifUpdateParser();

    public UMLTransformer(Model model) {
        this.model = model;
        this.modelTyping = new ModelTyping(this.model);
        this.translator = new CifToPythonTranslator(this.modelTyping);
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
        acquireSignal = FileHelper.FACTORY.createSignal();
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

        // Create the static property containing the current owner of the lock.
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
                transformActivity((Activity)behavior);
            }
        }

        // 3. Create a 'main' activity.

        // Get all activity behaviors of 'context'.
        List<Activity> activities = contextClass.getOwnedBehaviors().stream().filter(b -> b instanceof Activity)
                .map(b -> (Activity)b).collect(Collectors.toCollection(ArrayList::new));

        // Define a new activity that calls all 'activities' in parallel.
        Activity mainActivity = ActivityHelper.createMainActivity(activities, preconditions, lockHandlerActivity);
        mainActivity.setName("main");
        contextClass.getOwnedBehaviors().add(mainActivity);
        contextClass.setClassifierBehavior(mainActivity);
    }

    private void transformActivity(Activity activity) {
        ActivityHelper.removeIrrelevantInformation(activity);

        // Create a separate class in which all new behaviors for 'activity' are stored.
        Class activityClass = (Class)model.createPackagedElement(activity.getName(), UMLPackage.eINSTANCE.getClass_());

        // Define the activity that encodes the precondition of 'activity'.
        List<AExpression> parsedPreconditions = activity.getPreconditions().stream()
                .map(constraint -> constraint.getSpecification())
                .filter(specification -> specification instanceof OpaqueExpression)
                .map(specification -> (OpaqueExpression)specification)
                .flatMap(specification -> specification.getBodies().stream())
                .map(precondition -> expressionParser.parseString(precondition, "")).collect(Collectors.toList());

        String combinedPythonPrecondition = translator.translateExpressions(parsedPreconditions);
        Activity preconditionActivity = ActivityHelper.createAtomicActivity(combinedPythonPrecondition, "pass",
                acquireSignal);
        preconditionActivity.setName("precondition");
        activityClass.getOwnedBehaviors().add(preconditionActivity);
        preconditions.put(activity, preconditionActivity);

        // Transform all opaque action nodes of 'activity'.
        for (ActivityNode node: new LinkedHashSet<>(activity.getNodes())) {
            if (node instanceof OpaqueAction) {
                transformOpaqueAction(activityClass, activity, (OpaqueAction)node);
            }
        }
    }

    private void transformOpaqueAction(Class activityClass, Activity activity, OpaqueAction action) {
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
