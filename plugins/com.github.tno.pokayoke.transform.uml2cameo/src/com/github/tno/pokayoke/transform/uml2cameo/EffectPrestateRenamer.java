
package com.github.tno.pokayoke.transform.uml2cameo;

import java.util.List;
import java.util.Map;

import org.eclipse.escet.cif.parser.ast.ACifObject;
import org.eclipse.escet.cif.parser.ast.automata.AAssignmentUpdate;
import org.eclipse.escet.cif.parser.ast.automata.AUpdate;
import org.eclipse.escet.cif.parser.ast.expressions.AExpression;

import com.github.tno.pokayoke.uml.profile.cif.CifContext;
import com.github.tno.pokayoke.uml.profile.cif.CifExpressionRenamer;

/**
 * Renames all variables on the right-hand sides of {@link AAssignmentUpdate assignment updates} by prefixing them with
 * 'pre__'.
 */
public class EffectPrestateRenamer extends CifExpressionRenamer {
    /** The prefix to use for renaming, which is 'pre__'. */
    public static final String PREFIX = "pre__";

    /**
     * Constructs a new {@link EffectPrestateRenamer} that renames by prefixing given variable names with 'pre__'.
     *
     * @param context The context to determine
     */
    public EffectPrestateRenamer(CifContext context) {
        super(name -> context.isVariable(name) ? PREFIX + name : name);
    }

    @Override
    protected ACifObject visit(AAssignmentUpdate update, Map<String, String> renaming) {
        AExpression value = (AExpression)visit(update.value, renaming);
        return new AAssignmentUpdate(update.addressable, value, update.position);
    }

    /**
     * Renames the given list of effects by prefixing all variables on the right-hand sides of assignments by 'pre__'.
     *
     * @param effects The effects to rename.
     * @param renaming The mapping from old to new names, which is maintained while renaming and is modified in-place.
     * @return The updated effects.
     */
    public List<List<AUpdate>> renameEffects(List<List<AUpdate>> effects, Map<String, String> renaming) {
        return effects.stream().map(effect -> renameEffect(effect, renaming)).toList();
    }

    /**
     * Renames the given effect by prefixing all variables on the right-hand sides of assignments by 'pre__'.
     *
     * @param effect The effect to rename.
     * @param renaming The mapping from old to new names, which is maintained while renaming and is modified in-place.
     * @return The updated effect.
     */
    private List<AUpdate> renameEffect(List<AUpdate> effect, Map<String, String> renaming) {
        return effect.stream().map(update -> renameUpdate(update, renaming)).toList();
    }

    /**
     * Renames the given update by prefixing all variables on the right-hand sides of assignments by 'pre__'.
     *
     * @param update The update to rename.
     * @param renaming The mapping from old to new names, which is maintained while renaming and is modified in-place.
     * @return The updated update.
     */
    private AUpdate renameUpdate(AUpdate update, Map<String, String> renaming) {
        return (AUpdate)visit(update, renaming);
    }
}
