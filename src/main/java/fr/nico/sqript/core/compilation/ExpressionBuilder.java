package fr.nico.sqript.core.compilation;

import fr.nico.sqript.core.compilation.advanced.PredictiveParser;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.ActionDefinition;
import fr.nico.sqript.meta.LoopDefinition;
import fr.nico.sqript.structures.IScript;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Constructeur optimisé d'expressions de script.
 * Gère la construction d'expressions, de chaînes et de variables.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class ExpressionBuilder {

    private static final Logger log = LogManager.getLogger(ExpressionBuilder.class);

    private final ScriptRegistry registry;
    private final CompilationCache cache;
    private final PredictiveParser predictiveParser;

    // Statistiques
    private final AtomicLong expressionsBuilt = new AtomicLong(0);

    // État
    private volatile boolean initialized = false;

    public ExpressionBuilder(ScriptRegistry registry, CompilationCache cache) {
        this.registry = registry;
        this.cache = cache;
        this.predictiveParser = new PredictiveParser(registry);
    }

    /**
     * Initialise le constructeur d'expressions.
     */
    public void initialize() {
        if (initialized) {
            log.warn("ExpressionBuilder is already initialized");
            return;
        }

        initialized = true;
        log.info("ExpressionBuilder initialized successfully");
    }

    /**
     * Construit une expression à partir d'un token.
     *
     * @param token         Le token à parser
     * @param context       Le contexte de compilation
     * @param requiredTypes Les types requis
     * @return Le nœud d'expression construit
     */
    public Node buildExpression(ScriptToken token, ScriptCompilationContext context, Class<?>... requiredTypes) {
        checkInitialized();

        if (token == null || token.getText() == null) {
            return null;
        }

        expressionsBuilt.incrementAndGet();

        String cacheKey = token.getText() + ":" + context.hashCode();
        Node cachedNode = cache.getExpression(cacheKey);
        if (cachedNode != null) {
            return cachedNode;
        }

        PredictiveParser.ParseResult result = predictiveParser.parseExpression(token, context, requiredTypes);
        if (result != null && result.isSuccess() && result.getResult() instanceof Node) {
            Node node = (Node) result.getResult();
            cache.putExpression(cacheKey, node);
            return node;
        }
        return null;
    }

    /**
     * Construit une expression de chaîne avec interpolation.
     *
     * @param token   Le token contenant la chaîne
     * @param context Le contexte de compilation
     * @return Le nœud d'expression de chaîne
     */
    public Node buildStringExpression(ScriptToken token, ScriptCompilationContext context) {
        checkInitialized();

        if (token == null || token.getText() == null) {
            return null;
        }

        String cacheKey = "string:" + token.getText() + ":" + context.hashCode();
        Node cachedNode = cache.getExpression(cacheKey);
        if (cachedNode != null) {
            return cachedNode;
        }

        PredictiveParser.ParseResult result = predictiveParser.parseExpression(token, context, fr.nico.sqript.types.primitive.TypeString.class);
        if (result != null && result.isSuccess() && result.getResult() instanceof Node) {
            Node node = (Node) result.getResult();
            cache.putExpression(cacheKey, node);
            return node;
        }
        return null;
    }

    /**
     * Construit une expression de nom de variable.
     *
     * @param token   Le token contenant le nom de variable
     * @param context Le contexte de compilation
     * @return L'expression de variable
     */
    public ScriptExpression buildVariableName(ScriptToken token, ScriptCompilationContext context) {
        checkInitialized();

        if (token == null || token.getText() == null) {
            return null;
        }

        PredictiveParser.ParseResult result = predictiveParser.parseExpression(token, context, fr.nico.sqript.types.primitive.TypeString.class);
        if (result != null && result.isSuccess() && result.getResult() instanceof ScriptExpression) {
            return (ScriptExpression) result.getResult();
        }
        return null;
    }

    /**
     * Construit une boucle à partir d'une définition.
     *
     * @param loopDef La définition de boucle
     * @param token   Le token de la boucle
     * @param context Le contexte de compilation
     * @return Le script de boucle
     */
    public IScript buildLoop(LoopDefinition loopDef, ScriptToken token, ScriptCompilationContext context) {
        checkInitialized();
        if (loopDef == null || token == null) {
            return null;
        }
        PatternMatcher matcher = new PatternMatcher(cache);
        if (matcher.matches(loopDef, token.getText().trim())) {
            try {
                return loopDef.tryParse(token, context);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Construit une action à partir d'un token.
     *
     * @param token   Le token de l'action
     * @param context Le contexte de compilation
     * @param actions Les définitions d'actions disponibles
     * @return Le script d'action
     */
    public IScript buildAction(ScriptToken token, ScriptCompilationContext context, List<ActionDefinition> actions) {
        checkInitialized();
        if (token == null || actions == null) {
            return null;
        }
        PatternMatcher matcher = new PatternMatcher(cache);
        for (ActionDefinition actionDef : actions) {
            if (matcher.matches(actionDef, token.getText().trim())) {
                try {
                    return actionDef.tryParse(token, context);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Retourne le nombre d'expressions construites.
     */
    public long getBuiltCount() {
        return expressionsBuilt.get();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ExpressionBuilder is not initialized");
        }
    }
}





