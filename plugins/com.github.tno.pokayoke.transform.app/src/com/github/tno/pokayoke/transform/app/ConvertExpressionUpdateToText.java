
package com.github.tno.pokayoke.transform.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.EnumDecl;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.uml2.uml.OpaqueAction;

/** Convert CIF expressions and updates into CIF expression texts. */
public class ConvertExpressionUpdateToText {
    private Map<DiscVariable, EObject> discVariableToParent = new LinkedHashMap<>();

    private Map<EnumDecl, EObject> enumDeclToParent = new LinkedHashMap<>();

    /**
     * Convert CIF expressions into a string.
     *
     * @param cifSpec The CIF specification.
     * @param expressions A list of CIF expressions.
     * @return A string converted from the CIF expressions.
     */
    public String convertExpressions(Specification cifSpec, List<Expression> expressions) {
        // Check that the guard expressions do not contain location expressions and input variable expressions as they
        // are not expected in choice guards.
        CheckGuard checkGuard = new CheckGuard();
        expressions.stream().forEach(checkGuard::check);

        // Move the declarations to the root of the CIF specification.
        moveDeclarations(cifSpec);

        String string = CifTextUtils.exprsToStr(expressions);

        // Move the declarations back to their original scopes. This may change the order of the declarations.
        revertDeclarationsMove();

        return string;
    }

    private void moveDeclarations(Specification cifSpec) {
        List<Declaration> declarations = CifCollectUtils.collectDeclarations(cifSpec, new ArrayList<>());

        declarations.stream().filter(DiscVariable.class::isInstance).map(DiscVariable.class::cast)
                .forEach(v -> discVariableToParent.put(v, v.eContainer()));

        declarations.stream().filter(EnumDecl.class::isInstance).map(EnumDecl.class::cast)
                .forEach(e -> enumDeclToParent.put(e, e.eContainer()));

        cifSpec.getDeclarations().addAll(discVariableToParent.keySet());
        cifSpec.getDeclarations().addAll(enumDeclToParent.keySet());
    }

    private void revertDeclarationsMove() {
        discVariableToParent.entrySet()
                .forEach(e -> ((ComplexComponent)e.getValue()).getDeclarations().add(e.getKey()));
    }

    /**
     * Convert CIF updates into a string.
     *
     * @param cifSpec The CIF specification.
     * @param updates A list of CIF updates.
     * @return A string converted from the CIF updates.
     */
    public String convertUpdates(Specification cifSpec, List<Update> updates) {
        // Move the declarations to the root of the CIF specification.
        moveDeclarations(cifSpec);

        String string = CifTextUtils.updatesToStr(updates);

        // Move the declarations back to their original scopes. This may change the order of the declarations.
        revertDeclarationsMove();

        return string;
    }
}
