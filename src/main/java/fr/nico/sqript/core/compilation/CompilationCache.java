package fr.nico.sqript.core.compilation;

import fr.nico.sqript.structures.TransformedPattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Cache intelligent pour optimiser les performances de compilation.
 * Utilise une stratégie LRU et des métriques de performance.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class CompilationCache {

    private static final Logger log = LogManager.getLogger(CompilationCache.class);

    // Configuration par défaut
    private static final int DEFAULT_MAX_SIZE = 1000;
    private static final int DEFAULT_PATTERN_CACHE_SIZE = 500;
    private static final int DEFAULT_CONSTRUCTOR_CACHE_SIZE = 200;

    // Caches LRU
    private final LRUCache<String, Node> expressionCache;
    private final LRUCache<String, Pattern> patternCache;
    private final LRUCache<Class<?>, Constructor<?>> constructorCache;
    private final LRUCache<String, TransformedPattern> transformedPatternCache;

    // Métriques
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);

    // État
    private volatile boolean initialized = false;

    public CompilationCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_PATTERN_CACHE_SIZE, DEFAULT_CONSTRUCTOR_CACHE_SIZE);
    }

    public CompilationCache(int expressionCacheSize, int patternCacheSize, int constructorCacheSize) {
        this.expressionCache = new LRUCache<>(expressionCacheSize);
        this.patternCache = new LRUCache<>(patternCacheSize);
        this.constructorCache = new LRUCache<>(constructorCacheSize);
        this.transformedPatternCache = new LRUCache<>(patternCacheSize);
    }

    /**
     * Initialise le cache.
     */
    public void initialize() {
        if (initialized) {
            log.warn("CompilationCache is already initialized");
            return;
        }

        log.info("Initializing CompilationCache with sizes: expressions={}, patterns={}, constructors={}",
                expressionCache.getMaxSize(), patternCache.getMaxSize(), constructorCache.getMaxSize());

        initialized = true;
        log.info("CompilationCache initialized successfully");
    }

    // === CACHE D'EXPRESSIONS ===

    /**
     * Récupère une expression compilée du cache.
     */
    public Node getExpression(String key) {
        checkInitialized();
        totalRequests.incrementAndGet();

        Node result = expressionCache.get(key);
        if (result != null) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        return result;
    }

    /**
     * Met une expression compilée en cache.
     */
    public void putExpression(String key, Node expression) {
        checkInitialized();
        if (key != null && expression != null) {
            expressionCache.put(key, expression);
        }
    }

    // === CACHE DE PATTERNS ===

    /**
     * Récupère un pattern compilé du cache.
     */
    public Pattern getPattern(String regex) {
        checkInitialized();
        totalRequests.incrementAndGet();

        Pattern result = patternCache.get(regex);
        if (result != null) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        return result;
    }

    /**
     * Met un pattern compilé en cache.
     */
    public void putPattern(String regex, Pattern pattern) {
        checkInitialized();
        if (regex != null && pattern != null) {
            patternCache.put(regex, pattern);
        }
    }

    /**
     * Compile et met en cache un pattern regex.
     */
    public Pattern compileAndCachePattern(String regex) {
        Pattern pattern = getPattern(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            putPattern(regex, pattern);
        }
        return pattern;
    }

    // === CACHE DE PATTERNS TRANSFORMÉS ===

    /**
     * Récupère un pattern transformé du cache.
     */
    public TransformedPattern getTransformedPattern(String key) {
        checkInitialized();
        totalRequests.incrementAndGet();

        TransformedPattern result = transformedPatternCache.get(key);
        if (result != null) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        return result;
    }

    /**
     * Met un pattern transformé en cache.
     */
    public void putTransformedPattern(String key, TransformedPattern pattern) {
        checkInitialized();
        if (key != null && pattern != null) {
            transformedPatternCache.put(key, pattern);
        }
    }

    // === CACHE DE CONSTRUCTEURS ===

    /**
     * Récupère un constructeur du cache.
     */
    public Constructor<?> getConstructor(Class<?> clazz) {
        checkInitialized();
        totalRequests.incrementAndGet();

        Constructor<?> result = constructorCache.get(clazz);
        if (result != null) {
            cacheHits.incrementAndGet();
        } else {
            cacheMisses.incrementAndGet();
        }
        return result;
    }

    /**
     * Met un constructeur en cache.
     */
    public void putConstructor(Class<?> clazz, Constructor<?> constructor) {
        checkInitialized();
        if (clazz != null && constructor != null) {
            constructorCache.put(clazz, constructor);
        }
    }

    /**
     * Trouve et met en cache le constructeur par défaut d'une classe.
     */
    public Constructor<?> findAndCacheDefaultConstructor(Class<?> clazz) {
        Constructor<?> constructor = getConstructor(clazz);
        if (constructor == null) {
            try {
                constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                putConstructor(clazz, constructor);
            } catch (NoSuchMethodException e) {
                log.warn("No default constructor found for class: {}", clazz.getSimpleName());
            }
        }
        return constructor;
    }

    // === GESTION DU CACHE ===

    /**
     * Enregistre un hit de cache (pour les métriques externes).
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
        totalRequests.incrementAndGet();
    }

    /**
     * Enregistre un miss de cache (pour les métriques externes).
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
        totalRequests.incrementAndGet();
    }

    /**
     * Vide tous les caches.
     */
    public void clear() {
        checkInitialized();
        expressionCache.clear();
        patternCache.clear();
        constructorCache.clear();
        transformedPatternCache.clear();

        // Reset des métriques
        cacheHits.set(0);
        cacheMisses.set(0);
        totalRequests.set(0);

        log.info("All caches cleared");
    }

    /**
     * Optimise les caches en supprimant les entrées les moins utilisées.
     */
    public void optimize() {
        checkInitialized();

        int beforeExpressions = expressionCache.size();
        int beforePatterns = patternCache.size();
        int beforeConstructors = constructorCache.size();
        int beforeTransformed = transformedPatternCache.size();

        // Les caches LRU s'optimisent automatiquement
        // Ici on peut ajouter une logique d'optimisation supplémentaire si nécessaire

        log.info("Cache optimization completed. Sizes: expressions={}→{}, patterns={}→{}, constructors={}→{}, transformed={}→{}",
                beforeExpressions, expressionCache.size(),
                beforePatterns, patternCache.size(),
                beforeConstructors, constructorCache.size(),
                beforeTransformed, transformedPatternCache.size());
    }

    /**
     * Retourne les statistiques du cache.
     */
    public CacheStats getStats() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = totalRequests.get();
        double hitRate = total > 0 ? (double) hits / total : 0.0;

        return new CacheStats(
                hits, misses, total, hitRate,
                expressionCache.size(), expressionCache.getMaxSize(),
                patternCache.size(), patternCache.getMaxSize(),
                constructorCache.size(), constructorCache.getMaxSize(),
                transformedPatternCache.size(), transformedPatternCache.getMaxSize()
        );
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("CompilationCache is not initialized");
        }
    }

    /**
     * Classe pour les statistiques du cache.
     */
    public static class CacheStats {
        public final long hits;
        public final long misses;
        public final long totalRequests;
        public final double hitRate;
        public final int expressionCacheSize;
        public final int expressionCacheMaxSize;
        public final int patternCacheSize;
        public final int patternCacheMaxSize;
        public final int constructorCacheSize;
        public final int constructorCacheMaxSize;
        public final int transformedPatternCacheSize;
        public final int transformedPatternCacheMaxSize;

        public CacheStats(long hits, long misses, long totalRequests, double hitRate,
                          int expressionCacheSize, int expressionCacheMaxSize,
                          int patternCacheSize, int patternCacheMaxSize,
                          int constructorCacheSize, int constructorCacheMaxSize,
                          int transformedPatternCacheSize, int transformedPatternCacheMaxSize) {
            this.hits = hits;
            this.misses = misses;
            this.totalRequests = totalRequests;
            this.hitRate = hitRate;
            this.expressionCacheSize = expressionCacheSize;
            this.expressionCacheMaxSize = expressionCacheMaxSize;
            this.patternCacheSize = patternCacheSize;
            this.patternCacheMaxSize = patternCacheMaxSize;
            this.constructorCacheSize = constructorCacheSize;
            this.constructorCacheMaxSize = constructorCacheMaxSize;
            this.transformedPatternCacheSize = transformedPatternCacheSize;
            this.transformedPatternCacheMaxSize = transformedPatternCacheMaxSize;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{hitRate=%.2f%%, hits=%d, misses=%d, total=%d, " +
                            "expressions=%d/%d, patterns=%d/%d, constructors=%d/%d, transformed=%d/%d}",
                    hitRate * 100, hits, misses, totalRequests,
                    expressionCacheSize, expressionCacheMaxSize,
                    patternCacheSize, patternCacheMaxSize,
                    constructorCacheSize, constructorCacheMaxSize,
                    transformedPatternCacheSize, transformedPatternCacheMaxSize);
        }
    }
}





