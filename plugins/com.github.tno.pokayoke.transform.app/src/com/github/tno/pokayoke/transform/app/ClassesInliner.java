
package com.github.tno.pokayoke.transform.app;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Property;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.util.PokaYokeTypeUtil;
import com.google.common.base.Preconditions;

/** Application that performs full synthesis. */
public class ClassesInliner {
    private ClassesInliner() {
    }

    public static void inlineClasses(Activity activity) {
        Model model = activity.getModel();

        CifContext context = new CifContext(model);
        Class activeClass = getSingleActiveClass(context);

        Map<String, Pair<String, Property>> flattenedNamesMap = new LinkedHashMap<>();

        // Find all properties of the main class that are instances of a data class.
        for (Property umlProperty: activeClass.getOwnedAttributes()) {
            if (!PokaYokeTypeUtil.isSupportedType(umlProperty.getType())) {
                flattenedNamesMap = getLeafPrimitiveType((Class)umlProperty.getType(), umlProperty.getName(),
                        umlProperty.getName(), flattenedNamesMap);
            }
        }
    }

    public static Map<String, Pair<String, Property>> getLeafPrimitiveType(Class clazz, String name, String position,
            Map<String, Pair<String, Property>> flattenedNamesMap)
    {
        // Loop over a class' attributes. If they are boolean, Enum, Integer, add them to the main class; otherwise,
        // recursively call on the children object.
        for (Property umlProperty: clazz.getOwnedAttributes()) {
            String newNameString = name + "_" + umlProperty.getName();
            String newPosiString = position + "." + umlProperty.getName();

            if (PokaYokeTypeUtil.isSupportedType(umlProperty.getType())) {
                flattenedNamesMap = addFlattenedNameToMap(umlProperty, newNameString, newPosiString, flattenedNamesMap);
            } else {
                // Add the intermediate product of these. For example, if we have robot1.arm.position, we should
                // add also robot1.arm as a property, together with robot1.arm.position.
                flattenedNamesMap = addFlattenedNameToMap(umlProperty, newNameString, newPosiString, flattenedNamesMap);
                getLeafPrimitiveType((Class)umlProperty.getType(), newNameString, newPosiString, flattenedNamesMap);
            }
        }

        return flattenedNamesMap;
    }

    public static Map<String, Pair<String, Property>> addFlattenedNameToMap(Property umlProperty, String name,
            String position, Map<String, Pair<String, Property>> flattenedNamesMap)
    {
        Pair<String, Property> positionAndObject = Pair.of(position, umlProperty);

        if (flattenedNamesMap.get(name) == null) {
            // Add the item to the map.
            flattenedNamesMap.put(name, positionAndObject);
        } else {
            // Find how many entries keys start with name and add a number at the end.
            Set<String> allKeys = flattenedNamesMap.keySet();
            int count = 0;
            for (String k: allKeys) {
                if (k.startsWith(name)) {
                    count += 1;
                }
            }
            flattenedNamesMap.put(name + String.valueOf(count), positionAndObject);
        }

        return flattenedNamesMap;
    }

    public static List<Class> getAllPassiveClasses(CifContext context) {
        List<Class> umlClasses = context.getAllClasses(c -> (!(c instanceof Behavior) && (!c.isActive())));
        return umlClasses;
    }

    public static Class getSingleActiveClass(CifContext context) {
        List<Class> umlClasses = context.getAllClasses(c -> (!(c instanceof Behavior) && (c.isActive())));
        Preconditions.checkArgument(umlClasses.size() == 1,
                "Expected exactly one active class, but got " + umlClasses.size());
        return umlClasses.get(0);
    }
}
