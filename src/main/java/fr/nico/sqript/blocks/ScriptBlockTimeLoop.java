package fr.nico.sqript.blocks;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.ScriptTimer;
import fr.nico.sqript.core.compilation.CompilationResult;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Block;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;

@Block(
        feature = @Feature(name = "Time loop",
                description = "Define a piece of code that will be run periodically every time a specific amount of time is reached.",
                examples = "every 1 minute:\n" +
                        "    ...",
                regex = "every .*")
)
public class ScriptBlockTimeLoop extends ScriptBlock {

    // Cache de compilation pour éviter de recompiler la même expression de temps
    private static final java.util.Map<String, Long> delayCache = new java.util.concurrent.ConcurrentHashMap<>();
    // Contexte de compilation réutilisable pour les expressions de temps
    private static final ScriptCompilationContext TIME_CONTEXT = new ScriptCompilationContext();
    private final ScriptToken head;
    public long delay;

    public ScriptBlockTimeLoop(ScriptToken head) {
        this.head = head;
    }

    @Override
    public void displayTree(int i) {
        String tab = "";
        for (int j = 0; j < i; j++) tab += "|    ";
        ScriptManager.log.info(tab + "ScriptBlockTimeLoop : " + delay + " ms :");
        super.displayTree(i + 1);
    }

    public long getDelay(ScriptToken line) throws Exception {
        String originalText = line.getText();

        // Vérifier le cache d'abord
        Long cachedDelay = delayCache.get(originalText);
        if (cachedDelay != null) {
            return cachedDelay;
        }

        line.setText(line.getText().replaceAll("every\\s+", "").replaceAll(":", ""));
        if (line.getText().endsWith("server")) {
            line.setText(line.getText().replaceAll("server", ""));
        }

        ScriptExpression expr = null;
        try {
            // Utiliser le contexte de compilation réutilisable au lieu d'en créer un nouveau
            CompilationResult<ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(line, TIME_CONTEXT);
            if (result != null && result.isSuccess()) {
                expr = result.getResult();
            } else {
                String errorMsg = result != null && result.getError() != null ? result.getError().getMessage() : "Unknown compilation error";
                throw new fr.nico.sqript.core.compilation.ScriptException(line, "Erreur de compilation d'expression dans ScriptBlockTimeLoop: " + errorMsg);
            }
        } catch (Exception e) {
            throw new fr.nico.sqript.core.compilation.ScriptException(line, "Erreur de compilation d'expression dans ScriptBlockTimeLoop : " + e.getMessage());
        }

        long delay = (((Long) expr.get(new ScriptContext()).getObject()));

        // Mettre en cache le résultat
        delayCache.put(originalText, delay);

        return delay;
    }

    @Override
    public void init(ScriptLineBlock scriptLineBlock) throws Exception {
        this.delay = getDelay(head);
        setRoot(scriptLineBlock.compile());
        //System.out.println("Putting in loop with delay : "+delay);
        ScriptTimer.loopIScript(this, delay);
        getScriptInstance().registerBlock(this);
    }

}




