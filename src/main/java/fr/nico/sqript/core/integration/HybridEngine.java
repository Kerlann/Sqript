package fr.nico.sqript.core.integration;

import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.core.ScriptComponent;
import fr.nico.sqript.core.compilation.*;
import fr.nico.sqript.core.compilation.advanced.PredictiveParser;
import fr.nico.sqript.core.compilation.advanced.SmartCache;
import fr.nico.sqript.core.metrics.PerformanceMonitor;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.structures.ScriptContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Moteur de compilation moderne pour Sqript utilisant uniquement le nouveau système optimisé.
 * Remplace complètement l'ancien système avec des performances améliorées.
 */
public class HybridEngine implements ScriptComponent {

    private static final Logger log = LogManager.getLogger(HybridEngine.class);

    private final ScriptRegistry registry;
    private final ScriptCompiler compiler;
    private final SmartCache<String, Node> cache;
    private final PredictiveParser predictiveParser;
    private final PerformanceMonitor performanceMonitor;

    private volatile boolean active = false;

    public HybridEngine() {
        this.registry = new ScriptRegistry();
        this.compiler = new ScriptCompiler(registry);
        this.cache = new SmartCache<>(500, 1500, 3000);
        this.predictiveParser = new PredictiveParser(registry);
        this.performanceMonitor = PerformanceMonitor.getInstance();

        log.info("Modern Sqript Engine created with advanced optimization components");
    }

    /**
     * Constructeur pour intégration avec SqriptForge.
     * Utilise un ScriptRegistry existant.
     */
    public HybridEngine(ScriptRegistry registry) {
        this.registry = registry;
        this.compiler = new ScriptCompiler(registry);
        this.cache = new SmartCache<>(500, 1500, 3000);
        this.predictiveParser = new PredictiveParser(registry);
        this.performanceMonitor = PerformanceMonitor.getInstance();

        log.info("Modern Sqript Engine created with existing registry for Forge integration");
    }

    @Override
    public void initialize() throws ScriptComponentException {
        log.info("Initializing Modern Sqript Engine...");

        try {
            registry.initialize();
            compiler.initialize();

            log.info("Modern Sqript Engine initialized successfully");

        } catch (Exception e) {
            throw new ScriptComponentException("Failed to initialize Modern Sqript Engine", e);
        }
    }

    @Override
    public void start() throws ScriptComponentException {
        if (active) {
            log.warn("Modern Sqript Engine is already active");
            return;
        }

        registry.start();
        compiler.start();
        compiler.precompileCommonPatterns();

        active = true;
        log.info("Modern Sqript Engine started successfully");
    }

    @Override
    public void stop() throws ScriptComponentException {
        if (!active) return;

        active = false;

        try {
            if (compiler != null) compiler.stop();
        } catch (Exception e) {
            log.warn("Error stopping compiler", e);
        }
        try {
            if (registry != null) registry.stop();
        } catch (Exception e) {
            log.warn("Error stopping registry", e);
        }

        log.info("Modern Sqript Engine stopped");
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getComponentName() {
        return "ModernSqriptEngine";
    }

    /**
     * Compile une expression en utilisant le système moderne optimisé.
     */
    public CompilationResult<ScriptExpression> compileExpression(ScriptToken token,
                                                                 ScriptCompilationContext context,
                                                                 Class<?>... requiredTypes) {
        // Toujours essayer de compiler, même si pas "actif" - nous voulons utiliser le système moderne
        long startTime = System.nanoTime();
        String cacheKey = buildCacheKey(token.getText(), context, requiredTypes);

        log.info("[DEBUG SQRIPT] HybridEngine.compileExpression : tentative de compilation de l'expression : '{}' | contexte : {}", token.getText(), context);

        // Vérifier le cache
        Node cachedNode = cache.get(cacheKey);
        if (cachedNode != null) {
            performanceMonitor.recordCacheHit("modern-engine");
            ScriptExpression expression = createExpressionFromNode(cachedNode);
            return CompilationResult.success(expression, System.nanoTime() - startTime, true);
        }

        performanceMonitor.recordCacheMiss("modern-engine");

        // Utiliser le système moderne
        CompilationResult<ScriptExpression> result = compileWithModernSystem(token, context, requiredTypes, startTime);

        if (result != null && result.isSuccess()) {
            // Mettre en cache le résultat
            if (result.getResult() != null) {
                Node node = convertExpressionToNode(result.getResult());
                if (node != null) {
                    cache.put(cacheKey, node);
                }
            }

            performanceMonitor.recordCompilationSuccess("modern-system", System.nanoTime() - startTime);
            return result;
        }

        // Échec de compilation avec le système moderne
        performanceMonitor.recordCompilationFailure("modern-system", System.nanoTime() - startTime, "no-match");
        
        // Essayer de parser directement avec le registre moderne
        try {
            log.info("[DEBUG SQRIPT] Tentative de parsing direct avec le registre pour : '{}'", token.getText());
            
            // Essayer de parser comme expression via le registre
            ScriptExpression registryResult = registry.parseExpression(token, context, requiredTypes);
            if (registryResult != null) {
                log.info("[DEBUG SQRIPT] Parsing registre réussi pour : '{}'", token.getText());
                return CompilationResult.success(registryResult, System.nanoTime() - startTime, false);
            }
            
            // Essayer de parser comme action
            log.info("[DEBUG SQRIPT] Fallback : tentative de parseAction pour la ligne : '{}'", token.getText());
            ScriptAction action = registry.parseAction(token, context);
            if (action != null) {
                log.info("[DEBUG SQRIPT] Fallback parseAction : action trouvée : {} pour la ligne : '{}'", action.getClass().getSimpleName(), token.getText());
                
                // Utiliser ActSimpleExpression qui existe déjà pour wrapper l'action
                // Mais ici on retourne une ScriptExpression qui exécute l'action et retourne le résultat
                ScriptExpression actionWrapper = new ActionWrapperExpression(action);
                return CompilationResult.success(actionWrapper, System.nanoTime() - startTime, false);
            } else {
                log.warn("[DEBUG SQRIPT] Fallback parseAction : aucune action trouvée pour la ligne : '{}'", token.getText());
            }
        } catch (Exception e) {
            log.error("[DEBUG SQRIPT] Exception lors du parsing avec le registre pour la ligne : '{}' : {}", token.getText(), e.getMessage());
        }
        return CompilationResult.failure(new ScriptException.ScriptUnknownExpressionException(token));
    }

    private CompilationResult<ScriptExpression> compileWithModernSystem(ScriptToken token,
                                                                        ScriptCompilationContext context,
                                                                        Class<?>[] requiredTypes,
                                                                        long startTime) {
        try {
            // Essayer d'abord le parser prédictif
            PredictiveParser.ParseResult parseResult = predictiveParser.parseExpression(token, context, requiredTypes);

            if (parseResult != null && parseResult.isSuccess()) {
                Object result = parseResult.getResult();
                if (result instanceof ScriptExpression) {
                    return CompilationResult.success((ScriptExpression) result,
                            System.nanoTime() - startTime, false);
                } else if (result instanceof Node) {
                    ScriptExpression expression = createExpressionFromNode((Node) result);
                    return CompilationResult.success(expression,
                            System.nanoTime() - startTime, false);
                }
            }

            // Fallback vers le compilateur standard
            return compiler.compileExpression(token, context, requiredTypes);

        } catch (Exception e) {
            log.debug("Modern system compilation failed for token: {}", token.getText(), e);
            return CompilationResult.failure(e);
        }
    }

    private String buildCacheKey(String text, ScriptCompilationContext context, Class<?>... types) {
        StringBuilder key = new StringBuilder(text);
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
            return new fr.nico.sqript.expressions.ExprCompiledExpression(node);
        }
    }

    private Node convertExpressionToNode(ScriptExpression expression) {
        return new NodeExpression(expression);
    }

    /**
     * Wrapper propre pour exécuter une action dans le contexte d'une expression.
     * Utilisé quand une ligne de code peut être interprétée comme une action mais doit être exécutée
     * dans un contexte d'expression (par exemple dans un bloc de commande).
     */
    private static class ActionWrapperExpression extends ScriptExpression {
        private final ScriptAction action;
        
        public ActionWrapperExpression(ScriptAction action) {
            this.action = action;
            this.setLine(action.getLine());
        }
        
        @Override
        public fr.nico.sqript.types.ScriptType get(ScriptContext context, fr.nico.sqript.types.ScriptType<?>[] parameters) throws ScriptException {
            try {
                // Exécuter l'action
                action.execute(context);
                
                // Pour les actions qui peuvent retourner quelque chose (comme les expressions),
                // on peut essayer de récupérer le résultat depuis le contexte
                // Sinon, on retourne un booléen indiquant le succès
                return new fr.nico.sqript.types.primitive.TypeBoolean(true);
            } catch (Exception e) {
                // Propager l'exception avec le contexte approprié
                if (e instanceof ScriptException) {
                    throw (ScriptException) e;
                } else {
                    throw new ScriptException(action.getLine(), "Erreur lors de l'exécution de l'action : " + e.getMessage());
                }
            }
        }
        
        @Override
        public boolean set(ScriptContext context, fr.nico.sqript.types.ScriptType to, fr.nico.sqript.types.ScriptType<?>[] parameters) throws ScriptException {
            // Les actions ne sont généralement pas assignables
            return false;
        }
        
        @Override
        public Class getReturnType() {
            // Les actions wrapper retournent un booléen par défaut
            return fr.nico.sqript.types.primitive.TypeBoolean.class;
        }
        
        @Override
        public String toString() {
            return "ActionWrapper{" + action.getClass().getSimpleName() + "}";
        }
    }

    // ========================================
    // MÉTHODES POUR INTÉGRATION FORGE
    // ========================================

    /**
     * Compile une expression simple pour compatibilité.
     */
    public CompilationResult<ScriptExpression> compileExpression(ScriptToken token, ScriptCompilationContext context) {
        return compileExpression(token, context, new Class<?>[0]);
    }

    /**
     * Vérifie la santé du système et optimise les performances.
     */
    public void checkSystemHealth() {
        PerformanceMonitor.PerformanceReport report = performanceMonitor.generateReport();

        // Vérifier les performances du cache
        SmartCache.SmartCacheStats cacheStats = cache.getStats();
        int totalCacheSize = cacheStats.hotSize + cacheStats.warmSize + cacheStats.coldSize;

        if (cacheStats.hitRate < 0.5 && totalCacheSize > 1000) {
            log.info("Cache hit rate low ({}%), clearing cache to improve performance",
                    cacheStats.hitRate * 100);
            cache.clear();
        }

        // Log des métriques importantes
        log.debug("System Health Check - Success Rate: {}%, Cache: {}% hit rate, {} entries",
                report.successRate * 100,
                cacheStats.hitRate * 100,
                totalCacheSize);
    }

    /**
     * Obtient le registre utilisé par le moteur.
     */
    public ScriptRegistry getRegistry() {
        return registry;
    }

    /**
     * Force un nettoyage du cache pour libérer la mémoire.
     */
    public void clearCache() {
        cache.clear();
        log.info("Modern Sqript Engine cache cleared");
    }

    /**
     * Retourne un rapport de santé complet du système.
     */
    public String getHealthReport() {
        PerformanceMonitor.PerformanceReport report = performanceMonitor.generateReport();
        SmartCache.SmartCacheStats cacheStats = cache.getStats();
        int totalCacheSize = cacheStats.hotSize + cacheStats.warmSize + cacheStats.coldSize;

        return String.format(
                "Modern Sqript Engine Health Report:\n" +
                        "- Status: %s\n" +
                        "- Compilations: %d total, %d successful (%.1f%%)\n" +
                        "- Cache: %.1f%% hit rate, %d entries\n" +
                        "- Average Response Time: %.2f ms",
                active ? "ACTIVE" : "INACTIVE",
                report.totalCompilations, report.successfulCompilations,
                report.successRate * 100,
                cacheStats.hitRate * 100, totalCacheSize,
                report.averageResponseTime
        );
    }

    /**
     * Retourne les statistiques du moteur moderne.
     */
    public ModernEngineStats getStats() {
        return new ModernEngineStats(
                cache.getStats(),
                predictiveParser.getStats(),
                compiler.getStats(),
                performanceMonitor.generateReport()
        );
    }

    public static class ModernEngineStats {
        public final SmartCache.SmartCacheStats cacheStats;
        public final PredictiveParser.PredictiveParserStats parserStats;
        public final ScriptCompiler.CompilationStats compilerStats;
        public final PerformanceMonitor.PerformanceReport performanceReport;

        public ModernEngineStats(SmartCache.SmartCacheStats cacheStats,
                                 PredictiveParser.PredictiveParserStats parserStats,
                                 ScriptCompiler.CompilationStats compilerStats,
                                 PerformanceMonitor.PerformanceReport performanceReport) {
            this.cacheStats = cacheStats;
            this.parserStats = parserStats;
            this.compilerStats = compilerStats;
            this.performanceReport = performanceReport;
        }

        @Override
        public String toString() {
            return String.format("ModernEngineStats{cache=%.1f%%, parser=%s, compiler=%s}",
                    cacheStats.hitRate * 100, parserStats, compilerStats);
        }
    }

    public ScriptCompiler getCompiler() {
        return compiler;
    }
}







