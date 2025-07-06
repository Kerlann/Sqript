package fr.nico.sqript.actions;

import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.ScriptType;

public class ActSimpleExpression extends ScriptAction {

    ScriptExpression exp;

    public ActSimpleExpression(ScriptExpression exp) {
        this.exp = exp;
    }

    @Override
    public void execute(ScriptContext context) throws ScriptException {
        try {
            if (exp == null) {
                fr.nico.sqript.ScriptManager.log.error("ActSimpleExpression.execute() called with null expression");
                return;
            }
            
            fr.nico.sqript.ScriptManager.log.debug("ActSimpleExpression executing: {}", exp.getClass().getSimpleName());
            ScriptType result = exp.get(context);
            fr.nico.sqript.ScriptManager.log.debug("ActSimpleExpression result: {}", result != null ? result.toString() : "null");
        } catch (Exception e) {
            fr.nico.sqript.ScriptManager.log.error("Error in ActSimpleExpression.execute(): {}", e.getMessage());
            if (e instanceof ScriptException) {
                throw (ScriptException) e;
            } else {
                throw new ScriptException(getLine(), "Error executing simple expression: " + e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        return "(" + exp + ")";
    }
}





