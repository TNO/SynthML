package nl.esi.pokayoke.transform.uml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.LiteralString;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.OpaqueAction;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Signal;
import org.eclipse.uml2.uml.SignalEvent;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.VisibilityKind;

/**
 * Transforms UML models that are annotated with guards, updates, preconditions,
 * etc., to valid and executable UML, in the sense that all such annotations are
 * translated to valid UML. The annotation language is assumed to be Python.
 */
public class UMLTransformer {

	private Signal acquireSignal;

	private final Model model;

	private final Map<Activity, Activity> preconditions = new LinkedHashMap<>();

	public UMLTransformer(Model model) {
		this.model = model;
	}

	public static void transformFile(String sourcePath, String targetPath) throws IOException {
		Model model = FileHelper.loadModel(sourcePath);
		new UMLTransformer(model).transformModel();
		FileHelper.storeModel(model, targetPath);
	}

	public void transformModel() {
		// 1. Define locking infrastructure.

		// Create a class for holding lock-related structure and behavior.
		Class lockClass = (Class) model.createPackagedElement("Lock", UMLPackage.eINSTANCE.getClass_());

		// Create the signal for acquiring the lock.
		acquireSignal = FileHelper.factory.createSignal();
		acquireSignal.setName("acquire");
		Property acquireParameter = FileHelper.factory.createProperty();
		acquireParameter.setName("requester");
		acquireParameter.setType(FileHelper.loadPrimitiveType("String"));
		acquireSignal.getOwnedAttributes().add(acquireParameter);
		lockClass.getNestedClassifiers().add(acquireSignal);

		// Create the signal event for 'acquireSignal' to trigger on.
		SignalEvent acquireEvent = FileHelper.factory.createSignalEvent();
		acquireEvent.setSignal(acquireSignal);
		acquireEvent.setVisibility(VisibilityKind.PUBLIC_LITERAL);
		model.getPackagedElements().add(acquireEvent);

		// Create the activity that handles lock acquisition.
		Activity lockHandlerActivity = ActivityHelper.createLockHanderActivity(acquireEvent);
		lockHandlerActivity.setName("lockhandler");
		lockClass.getOwnedBehaviors().add(lockHandlerActivity);

		// 2. Transform the 'Context' class.

		Class contextClass = (Class) model.getMember("Context");

		// Create the static property containing the current owner of the lock.
		Property activeProperty = FileHelper.factory.createProperty();
		activeProperty.setIsStatic(true);
		activeProperty.setName("active");
		activeProperty.setType(FileHelper.loadPrimitiveType("String"));
		LiteralString activePropertyDefaultValue = FileHelper.factory.createLiteralString();
		activePropertyDefaultValue.setValue("");
		activeProperty.setDefaultValue(activePropertyDefaultValue);
		contextClass.getOwnedAttributes().add(activeProperty);

		// Transform all activity behaviors of 'contextClass'.
		for (Behavior behavior : new LinkedHashSet<>(contextClass.getOwnedBehaviors())) {
			if (behavior instanceof Activity) {
				transformActivity(contextClass, (Activity) behavior);
			}
		}

		// 3. Create a 'main' activity.

		// Get all activity behaviors of 'context'.
		List<Activity> activities = contextClass.getOwnedBehaviors().stream().filter(b -> b instanceof Activity)
				.map(b -> (Activity) b).collect(Collectors.toCollection(ArrayList::new));

		// Define a new activity that calls all 'activities' in parallel.
		Activity mainActivity = ActivityHelper.createMainActivity(activities, preconditions, lockHandlerActivity);
		mainActivity.setName("main");
		contextClass.getOwnedBehaviors().add(mainActivity);
		contextClass.setClassifierBehavior(mainActivity);
	}

	private void transformActivity(Class classMember, Activity activity) {
		ActivityHelper.removeIrrelevantInformation(activity);

		// Create a separate class in which all new behaviors for 'activity' are stored.
		Class activityClass = (Class) model.createPackagedElement(activity.getName(), UMLPackage.eINSTANCE.getClass_());

		// Define the activity that encodes the precondition of 'activity'.
		Activity preconditionActivity = ActivityHelper.createPreconditionActivityFor(activity);
		preconditionActivity.setName("precondition");
		activityClass.getOwnedBehaviors().add(preconditionActivity);
		preconditions.put(activity, preconditionActivity);

		// Transform all opaque action nodes of 'activity'.
		for (ActivityNode node : new LinkedHashSet<>(activity.getNodes())) {
			if (node instanceof OpaqueAction) {
				transformOpaqugeAction(activityClass, activity, (OpaqueAction) node);
			}
		}
	}

	private void transformOpaqugeAction(Class activityClass, Activity activity, OpaqueAction action) {
		// Extract the guard and effect of 'action', to be encoded later.
		String guard = null;
		String effect = null;

		if (action.getBodies().isEmpty()) {
			guard = "True";
			effect = "pass";
		} else if (action.getBodies().size() == 1) {
			guard = action.getBodies().get(0);
			effect = "pass";
		} else {
			guard = action.getBodies().get(0);
			effect = action.getBodies().stream().skip(1).map(b -> "if guard: " + b).collect(Collectors.joining("\n"));
		}

		// Define a new activity that encodes the behavior of 'action'.
		Activity actionActivity = ActivityHelper.createAtomicActivity(guard, effect, acquireSignal);
		actionActivity.setName(action.getName());
		activityClass.getOwnedBehaviors().add(actionActivity);

		// Define the call behavior action that replaces 'action' in 'activity'.
		CallBehaviorAction replacementActionNode = FileHelper.factory.createCallBehaviorAction();
		replacementActionNode.setActivity(activity);
		replacementActionNode.setBehavior(actionActivity);
		replacementActionNode.setName(action.getName());

		// Relocate all incoming edges into 'action' to 'replacementAction'.
		for (ActivityEdge edge : new LinkedHashSet<>(action.getIncomings())) {
			edge.setTarget(replacementActionNode);
		}

		// Relocate all outgoing edges out of 'action' to 'replacementAction'.
		for (ActivityEdge edge : new LinkedHashSet<>(action.getOutgoings())) {
			edge.setSource(replacementActionNode);
		}

		// Remove the old 'action' that is now replaced.
		action.setActivity(null);
	}
}
