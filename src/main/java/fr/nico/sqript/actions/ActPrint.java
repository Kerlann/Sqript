package fr.nico.sqript.actions;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Action;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;

@Action(name = "Print Actions",
        features = @Feature(name = "Print", description = "Prints something in the console.", examples = "print \"Hello world !\"", pattern = "print {element}")
)
public class ActPrint extends ScriptAction {
    @Override
    public void execute(ScriptContext context) throws ScriptException {
        switch (getMatchedIndex()) {
            case 0:
                ScriptExpression firstParameter = getParameters().get(0);
                ScriptManager.log.info(getLine().getScriptInstance().getName() + " : " + firstParameter.get(context));
                break;
        }
    }
}
/*
public class ActPrint extends ScriptAction {
    @Override
    public void execute(ScriptContext context) throws ScriptException {
        if (getParameters() == null || getParameters().isEmpty()) {
            ScriptManager.log.error("ActPrint: No parameters found. Line: " + (getLine() != null ? getLine().getText() : "unknown"));
            return;
        }
        switch (getMatchedIndex()){
            case 0:
                ScriptExpression firstParameter = getParameters().get(0);
                if (firstParameter == null) {
                    ScriptManager.log.error("ActPrint: First parameter is null. Line: " + (getLine() != null ? getLine().getText() : "unknown"));
                    return;
                }

                try {
                    ScriptType result = firstParameter.get(context);
                    String output;
                    if (result != null && result.getObject() != null) {
                        output = result.getObject().toString();
                    } else {
                        output = "null";
                    }
                    
                    ScriptManager.log.info(getLine().getScriptInstance().getName() + " : " + output);
                } catch (Exception e) {
                    ScriptManager.log.error("ActPrint: Error evaluating expression. Line: " + (getLine() != null ? getLine().getText() : "unknown") + ", Error: " + e.getMessage(), e);
                }
                break;
                
            default:
                ScriptManager.log.error("ActPrint: Unknown matched index: " + getMatchedIndex() + ". Line: " + (getLine() != null ? getLine().getText() : "unknown"));
                break;
        }
    }
}
*/





