
package com.github.tno.pokayoke.transform.cif;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;


/**
 * Transform flattened UML model into CIF model.
 */
public class CIFTransformer {
    private final Model model;

    public CIFTransformer(Model model) {
        this.model = model;
    }

    public void transformAction(CallBehaviorAction action) {
    }

    public void transformFork(ForkNode fork) {
    }

    public void transformJoint(JoinNode action) {
    }

    public void transformMerge(MergeNode action) {
    }

    public void transformDecision(DecisionNode action) {
    }

    public void transformActivityDiagram(Activity activity, Automaton aut) {

        for (ActivityNode node: new LinkedHashSet<>(activity.getNodes())) {
            if (node instanceof CallBehaviorAction) {


            }

//            if (node instanceof ForkNode) {
//
//            }
//
//            if (node instanceof MergeNode) {
//
//            }
//
//            if (node instanceof DecisionNode) {
//
//            }
//
//            if (node instanceof JoinNode) {
//
//            }

        }
    }

    public void initializeAutomaton() {


    }

    public void transformModel() {

        Automaton aut = CIFHelper.initializeAutomaton(model);
        // Extract activities.
        Class contextClass = (Class)model.getMember("Context");
        // Transform all activity behaviors of 'contextClass'.
        for (Behavior behavior: new LinkedHashSet<>(contextClass.getOwnedBehaviors())) {
            if (behavior instanceof Activity activity && activity.getName().equals("main")) {
                transformActivityDiagram(activity,aut);
            }
        }
    }
}
