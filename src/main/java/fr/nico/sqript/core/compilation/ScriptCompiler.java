package fr.nico.sqript.core.compilation;

import fr.nico.sqript.core.ScriptComponent;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.expressions.ExprCompiledExpression;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.LoopDefinition;
import fr.nico.sqript.structures.IScript;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Gestionnaire centralisé de la compilation des scripts.
 * Coordonne les différentes phases de compilation et optimise les performances.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class ScriptCompiler implements ScriptComponent {

    private static final Logger log = LogManager.getLogger(ScriptCompiler.class);

    private final ScriptRegistry registry;
    private final CompilationCache cache;
    private final TokenProcessor tokenProcessor;
    private final ExpressionBuilder expressionBuilder;
    private final PatternMatcher patternMatcher;
    private final SyntaxValidator syntaxValidator;

    // Pool de threads pour la compilation asynchrone
    private ExecutorService compilationExecutor;
    private volatile boolean active = false;

    public ScriptCompiler(ScriptRegistry registry) {
        this.registry = registry;
        this.cache = new CompilationCache();
        this.tokenProcessor = new TokenProcessor(cache);
        this.expressionBuilder = new ExpressionBuilder(registry, cache);
        this.patternMatcher = new PatternMatcher(cache);
        this.syntaxValidator = new SyntaxValidator();
    }

    @Override
    public void initialize() throws ScriptComponentException {
        log.info("Initializing ScriptCompiler...");
        try {
            cache.initialize();
            tokenProcessor.initialize();
            expressionBuilder.initialize();
            syntaxValidator.initialize();
            log.info("ScriptCompiler initialized successfully");
        } catch (Exception e) {
            throw new ScriptComponentException("Failed to initialize ScriptCompiler", e);
        }
    }

    @Override
    public void start() throws ScriptComponentException {
        if (active) {
            log.warn("ScriptCompiler is already active");
            return;
        }

        // Initialiser le pool de threads
        int threadCount = Runtime.getRuntime().availableProcessors();
        compilationExecutor = Executors.newFixedThreadPool(threadCount,
                r -> new Thread(r, "ScriptCompiler-" + System.currentTimeMillis()));

        active = true;
        log.info("ScriptCompiler started with {} threads", threadCount);
    }

    @Override
    public void stop() throws ScriptComponentException {
        if (!active) {
            log.warn("ScriptCompiler is already stopped");
            return;
        }

        active = false;

        // Arrêter le pool de threads proprement
        if (compilationExecutor != null) {
            compilationExecutor.shutdown();
            try {
                if (!compilationExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Compilation executor did not terminate gracefully, forcing shutdown");
                    compilationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for compilation executor to terminate");
                Thread.currentThread().interrupt();
            }
        }

        log.info("ScriptCompiler stopped");
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getComponentName() {
        return "ScriptCompiler";
    }

    /**
     * Compile un token de script en expression.
     * Version synchrone avec cache.
     */
    public CompilationResult<ScriptExpression> compileExpression(ScriptToken token,
                                                                 ScriptCompilationContext context,
                                                                 Class<?>... requiredTypes) {
        checkActive();

        long startTime = System.nanoTime();
        String cacheKey = generateCacheKey(token, context, requiredTypes);

        try {
            // Vérifier le cache d'abord
            Node cachedNode = cache.getExpression(cacheKey);
            if (cachedNode != null) {
                cache.recordCacheHit();
                return CompilationResult.success(createExpressionFromNode(cachedNode),
                        System.nanoTime() - startTime, true);
            }

            // Validation syntaxique
            ValidationResult validation = syntaxValidator.validate(token);
            if (!validation.isValid()) {
                return CompilationResult.failure(new ScriptException.ScriptSyntaxException(
                        token, "Syntax validation failed: " + validation.getError()));
            }

            // Compilation
            Node node = expressionBuilder.buildExpression(token, context, requiredTypes);
            if (node == null) {
                return CompilationResult.failure(new ScriptException.ScriptUnknownExpressionException(token));
            }

            // Mise en cache
            cache.putExpression(cacheKey, node);

            ScriptExpression expression = createExpressionFromNode(node);
            return CompilationResult.success(expression, System.nanoTime() - startTime, false);

        } catch (Exception e) {
            log.error("Compilation failed for token: {}", token.getText(), e);
            return CompilationResult.failure(e);
        }
    }

    /**
     * Compile un token de script en expression de manière asynchrone.
     */
    public CompletableFuture<CompilationResult<ScriptExpression>> compileExpressionAsync(
            ScriptToken token, ScriptCompilationContext context, Class<?>... requiredTypes) {
        checkActive();

        return CompletableFuture.supplyAsync(() ->
                compileExpression(token, context, requiredTypes), compilationExecutor);
    }

    /**
     * Compile une ligne de script en IScript.
     */
    public CompilationResult<IScript> compileLine(ScriptToken line, ScriptCompilationContext context) {
        checkActive();

        long startTime = System.nanoTime();

        try {
            // Validation syntaxique de base
            ValidationResult validation = syntaxValidator.validate(line);
            if (!validation.isValid()) {
                return CompilationResult.failure(new ScriptException.ScriptSyntaxException(
                        line, "Syntax validation failed: " + validation.getError()));
            }

            // Essayer de compiler comme une boucle
            IScript script = compileLoop(line, context);
            if (script != null) {
                return CompilationResult.success(script, System.nanoTime() - startTime, false);
            }

            // Essayer de compiler comme une action
            script = compileAction(line, context);
            if (script != null) {
                return CompilationResult.success(script, System.nanoTime() - startTime, false);
            }

            // Essayer les parsers personnalisés
            for (IScriptParser parser : registry.getCustomParsers()) {
                Object result = parser.parse(line, context);
                if (result instanceof IScript) {
                    script = (IScript) result;
                    if (script != null) {
                        return CompilationResult.success(script, System.nanoTime() - startTime, false);
                    }
                }
            }

            return CompilationResult.failure(new ScriptException.ScriptUnknownTokenException(line));

        } catch (Exception e) {
            log.error("Line compilation failed for: {}", line.getText(), e);
            return CompilationResult.failure(e);
        }
    }

    /**
     * Compile une chaîne avec des expressions intégrées (ex: "Hello %player%").
     */
    public CompilationResult<Node> compileString(ScriptToken line, ScriptCompilationContext context) {
        checkActive();

        long startTime = System.nanoTime();
        String cacheKey = "string:" + line.getText() + ":" + context.hashCode();

        try {
            // Vérifier le cache
            Node cachedNode = cache.getExpression(cacheKey);
            if (cachedNode != null) {
                cache.recordCacheHit();
                return CompilationResult.success(cachedNode, System.nanoTime() - startTime, true);
            }

            Node node = expressionBuilder.buildStringExpression(line, context);
            if (node == null) {
                return CompilationResult.failure(new ScriptException.ScriptUnknownExpressionException(line));
            }

            cache.putExpression(cacheKey, node);
            return CompilationResult.success(node, System.nanoTime() - startTime, false);

        } catch (Exception e) {
            log.error("String compilation failed for: {}", line.getText(), e);
            return CompilationResult.failure(e);
        }
    }

    /**
     * Compile un nom de variable avec des expressions intégrées.
     */
    public CompilationResult<ScriptExpression> compileVariableName(ScriptToken line,
                                                                   ScriptCompilationContext context) {
        checkActive();

        long startTime = System.nanoTime();

        try {
            ScriptExpression expression = expressionBuilder.buildVariableName(line, context);
            if (expression == null) {
                return CompilationResult.failure(new ScriptException.ScriptUnknownTokenException(line));
            }

            return CompilationResult.success(expression, System.nanoTime() - startTime, false);

        } catch (Exception e) {
            log.error("Variable name compilation failed for: {}", line.getText(), e);
            return CompilationResult.failure(e);
        }
    }

    /**
     * Précompile et met en cache les patterns fréquemment utilisés.
     */
    public void precompileCommonPatterns() {
        log.info("Precompiling common patterns...");

        // Précompiler les patterns d'actions
        registry.getActions().parallelStream().forEach(actionDef -> {
            try {
                patternMatcher.precompilePattern(actionDef);
            } catch (Exception e) {
                log.warn("Failed to precompile pattern for action: {}",
                        actionDef.getActionClass().getSimpleName(), e);
            }
        });

        // Précompiler les patterns d'expressions
        registry.getExpressions().values().parallelStream().forEach(exprDef -> {
            try {
                patternMatcher.precompilePattern(exprDef);
            } catch (Exception e) {
                log.warn("Failed to precompile pattern for expression: {}",
                        exprDef.getExpressionClass().getSimpleName(), e);
            }
        });

        log.info("Common patterns precompiled successfully");
    }

    /**
     * Nettoie le cache et optimise la mémoire.
     */
    public void optimizeCache() {
        cache.optimize();
    }

    /**
     * Retourne les statistiques de compilation.
     */
    public CompilationStats getStats() {
        return new CompilationStats(
                cache.getStats(),
                tokenProcessor.getProcessedCount(),
                expressionBuilder.getBuiltCount()
        );
    }

    public PatternMatcher getPatternMatcher() {
        return patternMatcher;
    }

    // === MÉTHODES PRIVÉES ===

    private void checkActive() {
        if (!active) {
            throw new IllegalStateException("ScriptCompiler is not active");
        }
    }

    private String generateCacheKey(ScriptToken token, ScriptCompilationContext context, Class<?>... types) {
        StringBuilder key = new StringBuilder(token.getText());
        key.append(":").append(context.hashCode());
        for (Class<?> type : types) {
            key.append(":").append(type.getSimpleName());
        }
        return key.toString();
    }

    private ScriptExpression createExpressionFromNode(Node node) {
        if (node instanceof NodeExpression) {
            return ((NodeExpression) node).getExpression();
        } else {
            return new ExprCompiledExpression(node);
        }
    }

    private IScript compileLoop(ScriptToken line, ScriptCompilationContext context) throws Exception {
        for (LoopDefinition loopDef : registry.getLoops()) {
            if (patternMatcher.matches(loopDef, line.getText().trim())) {
                return expressionBuilder.buildLoop(loopDef, line, context);
            }
        }
        return null;
    }

    private IScript compileAction(ScriptToken line, ScriptCompilationContext context) throws Exception {
        return expressionBuilder.buildAction(line, context, registry.getActions());
    }

    /**
     * Classe pour les statistiques de compilation.
     */
    public static class CompilationStats {
        public final CompilationCache.CacheStats cacheStats;
        public final long tokensProcessed;
        public final long expressionsBuilt;

        public CompilationStats(CompilationCache.CacheStats cacheStats,
                                long tokensProcessed, long expressionsBuilt) {
            this.cacheStats = cacheStats;
            this.tokensProcessed = tokensProcessed;
            this.expressionsBuilt = expressionsBuilt;
        }

        @Override
        public String toString() {
            return String.format("CompilationStats{cache=%s, tokensProcessed=%d, expressionsBuilt=%d}",
                    cacheStats, tokensProcessed, expressionsBuilt);
        }
    }
}





