
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
     * Convert CIF expressions of guards into CIF expression texts.
     *
     * @param cifSpec The CIF specification.
     * @param actionToExpression The map from opaque actions to expressions.
     * @return A map from opaque actions to CIF expression texts.
     */
    public Map<OpaqueAction, String> convert(Specification cifSpec,
            Map<OpaqueAction, Expression> actionToExpression)
    {
        // Check that the guard expressions do not contain location expressions and input variable expressions as they
        // are not expected in choice guards.
        CheckGuard checkGuard = new CheckGuard();
        actionToExpression.values().stream().forEach(checkGuard::check);

        // Move the declarations to the root of the CIF specification.
        moveDeclarations(cifSpec);

        // Convert the expressions to texts.
        Map<OpaqueAction, String> choiceActionToGuardText = new LinkedHashMap<>();
        actionToExpression.forEach(
                (action, expression) -> choiceActionToGuardText.put(action, CifTextUtils.exprToStr(expression)));

        // Move the declarations back to their original scopes. This may change the order of the declarations.
        revertDeclarationsMove();

        return choiceActionToGuardText;
    }

    public void moveDeclarations(Specification cifSpec) {
        List<Declaration> declarations = CifCollectUtils.collectDeclarations(cifSpec, new ArrayList<>());

        declarations.stream().filter(DiscVariable.class::isInstance).map(DiscVariable.class::cast)
                .forEach(v -> discVariableToParent.put(v, v.eContainer()));

        declarations.stream().filter(EnumDecl.class::isInstance).map(EnumDecl.class::cast)
                .forEach(e -> enumDeclToParent.put(e, e.eContainer()));

        cifSpec.getDeclarations().addAll(discVariableToParent.keySet());
        cifSpec.getDeclarations().addAll(enumDeclToParent.keySet());
    }

    public void revertDeclarationsMove() {
        discVariableToParent.entrySet()
                .forEach(e -> ((ComplexComponent)e.getValue()).getDeclarations().add(e.getKey()));
    }
}
