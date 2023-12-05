//////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
//////////////////////////////////////////////////////////////////////////////

package org.eclipse.escet.cif.datasynth.varorder.parser;

import static org.eclipse.escet.common.java.Lists.list;
import static org.eclipse.escet.common.java.Pair.pair;
import static org.eclipse.escet.common.java.Sets.difference;
import static org.eclipse.escet.common.java.Sets.list2set;
import static org.eclipse.escet.common.java.Sets.setc;
import static org.eclipse.escet.common.java.Strings.fmt;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.escet.cif.datasynth.spec.SynthesisVariable;
import org.eclipse.escet.cif.datasynth.varorder.helper.VarOrder;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.escet.common.java.Strings;

/** Parser for custom variable orders. */
public class CustomVarOrderParser {
    /** Constructor for the {@link CustomVarOrderParser} class. */
    private CustomVarOrderParser() {
        // Static class.
    }

    /**
     * Parse a custom variable order.
     *
     * @param orderTxt The text of the custom variable order.
     * @param variables The synthesis variables to be ordered.
     * @return The custom variable order, and an error message indicating why the given order is invalid. If the order
     *     is valid, the error message is {@code null}. If the order is invalid, the order is {@code null}.
     */
    public static Pair<VarOrder, String> parse(String orderTxt, List<SynthesisVariable> variables) {
        List<List<SynthesisVariable>> customVarOrder = list();
        for (String groupTxt: StringUtils.split(orderTxt, ";")) {
            // Skip empty.
            groupTxt = groupTxt.trim();
            if (groupTxt.isEmpty()) {
                continue;
            }

            // Process elements.
            List<SynthesisVariable> group = list();
            for (String elemTxt: StringUtils.split(groupTxt, ",")) {
                // Skip empty.
                elemTxt = elemTxt.trim();
                if (elemTxt.isEmpty()) {
                    continue;
                }

                // Create regular expression from filter.
                String regEx = elemTxt.replace(".", "\\.");
                regEx = regEx.replace("*", ".*");
                Pattern pattern = Pattern.compile("^" + regEx + "$");

                // Found actual element. Look up matching synthesis variables.
                List<SynthesisVariable> matches = variables.stream().filter(v -> pattern.matcher(v.rawName).matches())
                        .collect(Collectors.toList());

                // Need a least one match.
                if (matches.isEmpty()) {
                    String msg = fmt("can't find a match for \"%s\". There is no supported "
                            + "variable or automaton (with two or more locations) in the specification "
                            + "that matches the given name pattern.", elemTxt);
                    return pair(null, msg);
                }

                // Sort matches.
                Collections.sort(matches, (v, w) -> Strings.SORTER.compare(v.rawName, w.rawName));

                // Add the matched variables to the group.
                group.addAll(matches);
            }

            // Add the group to the custom variable order.
            customVarOrder.add(group);
        }

        // Check for duplicates.
        Set<SynthesisVariable> varsInOrder = setc(customVarOrder.size());
        for (List<SynthesisVariable> group: customVarOrder) {
            for (SynthesisVariable var: group) {
                boolean added = varsInOrder.add(var);
                if (!added) {
                    String msg = fmt("\"%s\" is included more than once.", var.name);
                    return pair(null, msg);
                }
            }
        }

        // Check completeness.
        Set<SynthesisVariable> missingVars = difference(list2set(variables), varsInOrder);
        if (!missingVars.isEmpty()) {
            String names = missingVars.stream().map(v -> "\"" + v.name + "\"").sorted(Strings.SORTER)
                    .collect(Collectors.joining(", "));
            String msg = fmt("the following are missing from the specified order: %s.", names);
            return pair(null, msg);
        }

        // Return the custom order.
        return pair(new VarOrder(customVarOrder), null);
    }
}
