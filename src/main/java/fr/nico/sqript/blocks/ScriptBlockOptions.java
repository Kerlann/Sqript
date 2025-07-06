package fr.nico.sqript.blocks;

import fr.nico.sqript.core.compilation.CompilationResult;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Block;
import fr.nico.sqript.meta.Feature;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Block(
        feature = @Feature(name = "Options",
                description = "Define options in your script to give it more flexibility and configuration.",
                examples = "options:\n" +
                        "    my_value: 5",
                regex = "^options:\\s*")
)
public class ScriptBlockOptions extends ScriptBlock {

    // Cache de compilation pour éviter de recompiler les mêmes options
    private static final java.util.Map<String, ScriptExpression> optionCache = new java.util.concurrent.ConcurrentHashMap<>();
    // Contexte de compilation réutilisable pour les options
    private static final ScriptCompilationContext OPTIONS_CONTEXT = new ScriptCompilationContext();

    public ScriptBlockOptions(ScriptToken head) throws ScriptException.ScriptSyntaxException {
    }

    @Override
    public void init(ScriptLineBlock scriptLineBlock) throws Exception {
        Pattern p = Pattern.compile("^\\s+(.*?)\\s*:\\s*(.*)$");
        for (ScriptToken s : scriptLineBlock.getContent()) {
            Matcher m = p.matcher(s.getText());
            if (m.find()) {
                String optionName = m.group(1);
                if (!optionName.matches("[a-zA-Z0-9_]*")) {
                    throw new ScriptException.ScriptSyntaxException(s, "The option's key must only contain letters, numbers and underscores");
                }
                String optionValue = m.group(2);

                // Vérifier le cache d'abord
                String cacheKey = optionName + ":" + optionValue;
                ScriptExpression value = optionCache.get(cacheKey);

                if (value == null) {
                    try {
                        // Utiliser le contexte de compilation réutilisable
                        CompilationResult<fr.nico.sqript.expressions.ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(s.with(optionValue), OPTIONS_CONTEXT);
                        if (result != null && result.isSuccess()) {
                            value = result.getResult();
                            // Mettre en cache le résultat
                            optionCache.put(cacheKey, value);
                        } else {
                            String errorMsg = result != null && result.getError() != null ? result.getError().getMessage() : "Unknown compilation error";
                            throw new fr.nico.sqript.core.compilation.ScriptException(s.with(optionValue), "Erreur de compilation d'expression dans ScriptBlockOptions: " + errorMsg);
                        }
                    } catch (Exception e) {
                        throw new fr.nico.sqript.core.compilation.ScriptException(s.with(optionValue), "Erreur de compilation d'expression dans ScriptBlockOptions : " + e.getMessage());
                    }
                }

                getScriptInstance().getOptions().put(optionName, value);
            }
        }
    }
}





