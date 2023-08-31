
package com.github.tno.pokayoke.transform.common;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Enumeration;
import org.eclipse.uml2.uml.ForkNode;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralBoolean;
import org.eclipse.uml2.uml.LiteralInteger;
import org.eclipse.uml2.uml.MergeNode;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Property;

/** Utils that process UML activity diagrams. */
public class UMLActivityUtils {
    private UMLActivityUtils() {
    }

    /**
     * Removes irrelevant and redundant information from the given activity, like edge weights or redundant edge guards.
     *
     * @param activity The activity to clean up.
     */
    public static void removeIrrelevantInformation(Activity activity) {
        // Remove any default weights from all edges.
        for (ActivityEdge edge: activity.getEdges()) {
            if (edge.getWeight() instanceof LiteralInteger literal && literal.getValue() == 0) {
                edge.setWeight(null);
            }
        }

        // Remove any default guards from all edges not coming out of decision nodes.
        for (ActivityEdge edge: activity.getEdges()) {
            if (!(edge.getSource() instanceof DecisionNode) && edge.getGuard() instanceof LiteralBoolean literal
                    && literal.isValue())
            {
                edge.setGuard(null);
            }
        }
    }

    public static void setNameForEdges(Activity activity) {
        for (ActivityEdge edge: activity.getEdges()) {
            String source = edge.getSource().getName();
            String target = edge.getTarget().getName();
            edge.setName("edge__from__" + source + "__to__" + target);
        }
    }

    public static void setNameForNodes(Activity activity) {
        int f = 0, m = 0, d = 0, j = 0;
        for (ActivityNode node: activity.getNodes()) {
            if (node instanceof ForkNode) {
                node.setName("ForkNode" + String.valueOf(f));
                f++;
            }

            if (node instanceof MergeNode) {
                node.setName("MergeNode" + String.valueOf(m));
                m++;
            }

            if (node instanceof DecisionNode) {
                node.setName("DecisionNode" + String.valueOf(d));
                d++;
            }
            if (node instanceof JoinNode) {
                node.setName("JoinNode" + String.valueOf(j));
                j++;
            }

            if (node instanceof InitialNode) {
                node.setName("InitialNode");
            }

            if (node instanceof ActivityFinalNode) {
                node.setName("ActivityFinalNode");
            }
        }
    }

    public static void setAbsoluteNameForNestedElements(CallBehaviorAction action, Activity activity) {
        if (action != null) {
            for (ActivityNode node: activity.getNodes()) {
                node.setName(action.getName() + "__" + node.getName());
            }
        }
    }

    public static void setNameForBehaviorAction(CallBehaviorAction action) {
        if (action != null) {
            String parentName = action.getActivity().getName();
            String childName = action.getBehavior().getName();
            action.setName(parentName + "__" + childName);
        }
    }

    public static void renameActivity(Activity activity) {
        for (ActivityEdge edge: activity.getEdges()) {
            setNameWithID(edge);
        }
        for (ActivityNode node: activity.getNodes()) {
            setNameWithID(node);
        }
    }

    public static void renamePropertyAndEnumeration(Model model) {
        setNameWithID(model);

        for (NamedElement member: model.getMembers()) {
            if (member instanceof Enumeration enumeration) {
                setNameWithID(enumeration);


            } else if (member instanceof Class contextClass) {
                for (Property property: contextClass.getAllAttributes()) {
                    if (property.getName() != null) {
                        setNameWithID(property);
                    }
                }
            }
        }
    }

    private static void setNameWithID(NamedElement ob) {
        if (ob.getName() != null) {
            ob.setName(ob.getName() + "__" + ob.eResource().getURIFragment(ob));
        } else {
            ob.setName(ob.eResource().getURIFragment(ob));
        }
    }
}
