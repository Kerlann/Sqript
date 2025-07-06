package fr.nico.sqript.core.metrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collecteur de métriques avancé pour le monitoring des performances.
 * Collecte et agrège les métriques de performance en temps réel.
 *
 * @author Nico-
 * @version 2.0
 * @since Phase 2 - Monitoring
 */
public class PerformanceMonitor {

    private static final Logger log = LogManager.getLogger(PerformanceMonitor.class);

    // Instance singleton
    private static volatile PerformanceMonitor instance;

    // Métriques de compilation
    private final LongAdder totalCompilations = new LongAdder();
    private final LongAdder successfulCompilations = new LongAdder();
    private final LongAdder failedCompilations = new LongAdder();
    private final LongAdder totalCompilationTime = new LongAdder(); // en nanoseconds

    // Métriques de cache
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();

    // Métriques de parsing
    private final LongAdder totalParsingOperations = new LongAdder();
    private final LongAdder fastPathHits = new LongAdder();
    private final LongAdder slowPathHits = new LongAdder();

    // Métriques par type d'expression
    private final ConcurrentHashMap<String, ExpressionMetrics> expressionMetrics = new ConcurrentHashMap<>();

    // Métriques de pool d'objets
    private final LongAdder objectsCreated = new LongAdder();
    private final LongAdder objectsReused = new LongAdder();

    // Métriques de temps de réponse
    private final ConcurrentHashMap<String, ResponseTimeMetrics> responseTimeMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "PerformanceMonitor-Reporter"));
    // Configuration et état
    private volatile boolean enabled = true;
    private final long reportingInterval = 30; // secondes

    private PerformanceMonitor() {
        startPeriodicReporting();
    }

    public static PerformanceMonitor getInstance() {
        if (instance == null) {
            synchronized (PerformanceMonitor.class) {
                if (instance == null) {
                    instance = new PerformanceMonitor();
                }
            }
        }
        return instance;
    }

    // === MÉTHODES D'ENREGISTREMENT DES MÉTRIQUES ===

    /**
     * Enregistre une compilation réussie.
     */
    public void recordCompilationSuccess(String expressionType, long durationNanos) {
        if (!enabled) return;

        totalCompilations.increment();
        successfulCompilations.increment();
        totalCompilationTime.add(durationNanos);

        recordExpressionMetric(expressionType, durationNanos, true);
        recordResponseTime("compilation", durationNanos);
    }

    /**
     * Enregistre une compilation échouée.
     */
    public void recordCompilationFailure(String expressionType, long durationNanos, String errorType) {
        if (!enabled) return;

        totalCompilations.increment();
        failedCompilations.increment();
        totalCompilationTime.add(durationNanos);

        recordExpressionMetric(expressionType, durationNanos, false);
        recordResponseTime("compilation", durationNanos);
    }

    /**
     * Enregistre un hit de cache.
     */
    public void recordCacheHit(String cacheType) {
        if (!enabled) return;
        cacheHits.increment();
        recordResponseTime("cache." + cacheType, 0); // Cache hits sont instantanés
    }

    /**
     * Enregistre un miss de cache.
     */
    public void recordCacheMiss(String cacheType) {
        if (!enabled) return;
        cacheMisses.increment();
    }

    /**
     * Enregistre une opération de parsing.
     */
    public void recordParsingOperation(boolean fastPath, long durationNanos) {
        if (!enabled) return;

        totalParsingOperations.increment();
        if (fastPath) {
            fastPathHits.increment();
        } else {
            slowPathHits.increment();
        }

        recordResponseTime("parsing", durationNanos);
    }

    /**
     * Enregistre la création d'un objet depuis un pool.
     */
    public void recordObjectCreation() {
        if (!enabled) return;
        objectsCreated.increment();
    }

    /**
     * Enregistre la réutilisation d'un objet depuis un pool.
     */
    public void recordObjectReuse() {
        if (!enabled) return;
        objectsReused.increment();
    }

    // === MÉTHODES D'ACCÈS AUX MÉTRIQUES ===

    /**
     * Retourne un rapport complet des métriques.
     */
    public PerformanceReport generateReport() {
        return new PerformanceReport(
                // Métriques de compilation
                totalCompilations.sum(),
                successfulCompilations.sum(),
                failedCompilations.sum(),
                calculateAverageCompilationTime(),
                calculateSuccessRate(),

                // Métriques de cache
                cacheHits.sum(),
                cacheMisses.sum(),
                calculateCacheHitRate(),

                // Métriques de parsing
                totalParsingOperations.sum(),
                fastPathHits.sum(),
                slowPathHits.sum(),
                calculateFastPathRate(),

                // Métriques de pool d'objets
                objectsCreated.sum(),
                objectsReused.sum(),
                calculateObjectReuseRate(),

                // Métriques par expression
                new ConcurrentHashMap<>(expressionMetrics),

                // Métriques de temps de réponse
                new ConcurrentHashMap<>(responseTimeMetrics)
        );
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Performance monitoring {}", enabled ? "enabled" : "disabled");
    }

    public void reset() {
        totalCompilations.reset();
        successfulCompilations.reset();
        failedCompilations.reset();
        totalCompilationTime.reset();
        cacheHits.reset();
        cacheMisses.reset();
        totalParsingOperations.reset();
        fastPathHits.reset();
        slowPathHits.reset();
        objectsCreated.reset();
        objectsReused.reset();
        expressionMetrics.clear();
        responseTimeMetrics.clear();

        log.info("Performance metrics reset");
    }

    public void shutdown() {
        enabled = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Performance monitor shut down");
    }

    // === MÉTHODES PRIVÉES ===

    private void recordExpressionMetric(String expressionType, long durationNanos, boolean success) {
        expressionMetrics.computeIfAbsent(expressionType, k -> new ExpressionMetrics())
                .record(durationNanos, success);
    }

    private void recordResponseTime(String operation, long durationNanos) {
        responseTimeMetrics.computeIfAbsent(operation, k -> new ResponseTimeMetrics())
                .record(durationNanos);
    }

    private double calculateAverageCompilationTime() {
        long total = totalCompilations.sum();
        return total > 0 ? (double) totalCompilationTime.sum() / total / 1_000_000.0 : 0.0; // en ms
    }

    private double calculateSuccessRate() {
        long total = totalCompilations.sum();
        return total > 0 ? (double) successfulCompilations.sum() / total : 0.0;
    }

    private double calculateCacheHitRate() {
        long totalCacheRequests = cacheHits.sum() + cacheMisses.sum();
        return totalCacheRequests > 0 ? (double) cacheHits.sum() / totalCacheRequests : 0.0;
    }

    private double calculateFastPathRate() {
        long total = totalParsingOperations.sum();
        return total > 0 ? (double) fastPathHits.sum() / total : 0.0;
    }

    private double calculateObjectReuseRate() {
        long totalObjects = objectsCreated.sum() + objectsReused.sum();
        return totalObjects > 0 ? (double) objectsReused.sum() / totalObjects : 0.0;
    }

    private void startPeriodicReporting() {
        scheduler.scheduleAtFixedRate(() -> {
            if (enabled) {
                try {
                    PerformanceReport report = generateReport();
                    logPerformanceReport(report);
                } catch (Exception e) {
                    log.error("Error generating performance report", e);
                }
            }
        }, reportingInterval, reportingInterval, TimeUnit.SECONDS);
    }

    private void logPerformanceReport(PerformanceReport report) {
        if (report.totalCompilations == 0) {
            return; // Pas de données à reporter
        }

        log.info("=== Performance Report ===");
        log.info("Compilations: {} total, {:.1f}% success, {:.2f}ms avg",
                report.totalCompilations, report.successRate * 100, report.averageCompilationTime);
        log.info("Cache: {:.1f}% hit rate ({} hits, {} misses)",
                report.cacheHitRate * 100, report.cacheHits, report.cacheMisses);
        log.info("Parsing: {:.1f}% fast path ({} total operations)",
                report.fastPathRate * 100, report.totalParsingOperations);
        log.info("Object Pool: {:.1f}% reuse rate ({} created, {} reused)",
                report.objectReuseRate * 100, report.objectsCreated, report.objectsReused);
    }

    // === CLASSES INTERNES ===

    public static class ExpressionMetrics {
        private final LongAdder totalCalls = new LongAdder();
        private final LongAdder successfulCalls = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private volatile double averageTime = 0.0;
        private volatile double successRate = 0.0;

        void record(long durationNanos, boolean success) {
            totalCalls.increment();
            totalTime.add(durationNanos);
            if (success) {
                successfulCalls.increment();
            }

            long calls = totalCalls.sum();
            averageTime = calls > 0 ? (double) totalTime.sum() / calls / 1_000_000.0 : 0.0;
            successRate = calls > 0 ? (double) successfulCalls.sum() / calls : 0.0;
        }

        public long getTotalCalls() {
            return totalCalls.sum();
        }

        public double getAverageTime() {
            return averageTime;
        }

        public double getSuccessRate() {
            return successRate;
        }
    }

    public static class ResponseTimeMetrics {
        private final LongAdder totalOperations = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private volatile double averageTime = 0.0;

        void record(long durationNanos) {
            totalOperations.increment();
            totalTime.add(durationNanos);

            long ops = totalOperations.sum();
            averageTime = ops > 0 ? (double) totalTime.sum() / ops / 1_000_000.0 : 0.0;
        }

        public long getTotalOperations() {
            return totalOperations.sum();
        }

        public double getAverageTime() {
            return averageTime;
        }
    }

    /**
     * Rapport de performance consolidé
     */
    public static class PerformanceReport {
        // Champs publics pour compatibilité
        public final long totalCompilations;
        public final long successfulCompilations;
        public final long failedCompilations;
        public final double averageCompilationTime;
        public final double successRate;

        public final long cacheHits;
        public final long cacheMisses;
        public final double cacheHitRate;

        public final long totalParsingOperations;
        public final long fastPathHits;
        public final long slowPathHits;
        public final double fastPathRate;

        public final long objectsCreated;
        public final long objectsReused;
        public final double objectReuseRate;

        public final Map<String, ExpressionMetrics> expressionMetrics;
        public final Map<String, ResponseTimeMetrics> responseTimeMetrics;

        public final long timestamp;
        public final double averageResponseTime;
        // Nouveaux champs pour le système hybride
        private final Map<String, AtomicLong> successCounts = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> failureCounts = new ConcurrentHashMap<>();
        private final Map<String, AtomicReference<Double>> averageLatencies = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> totalLatencies = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
        private final long reportGenerationTime;
        private final double totalOperationsPerSecond;

        public PerformanceReport(long reportGenerationTime, double totalOperationsPerSecond, double averageResponseTime) {
            this.reportGenerationTime = reportGenerationTime;
            this.totalOperationsPerSecond = totalOperationsPerSecond;
            this.averageResponseTime = averageResponseTime;

            // Valeurs par défaut pour la compatibilité
            this.totalCompilations = 0;
            this.successfulCompilations = 0;
            this.failedCompilations = 0;
            this.averageCompilationTime = 0.0;
            this.successRate = 0.0;
            this.cacheHits = 0;
            this.cacheMisses = 0;
            this.cacheHitRate = 0.0;
            this.totalParsingOperations = 0;
            this.fastPathHits = 0;
            this.slowPathHits = 0;
            this.fastPathRate = 0.0;
            this.objectsCreated = 0;
            this.objectsReused = 0;
            this.objectReuseRate = 0.0;
            this.expressionMetrics = new ConcurrentHashMap<>();
            this.responseTimeMetrics = new ConcurrentHashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        // Constructeur complet pour compatibilité avec generateReport
        public PerformanceReport(long totalCompilations, long successfulCompilations, long failedCompilations,
                                 double averageCompilationTime, double successRate,
                                 long cacheHits, long cacheMisses, double cacheHitRate,
                                 long totalParsingOperations, long fastPathHits, long slowPathHits, double fastPathRate,
                                 long objectsCreated, long objectsReused, double objectReuseRate,
                                 Map<String, ExpressionMetrics> expressionMetrics,
                                 Map<String, ResponseTimeMetrics> responseTimeMetrics) {
            this.totalCompilations = totalCompilations;
            this.successfulCompilations = successfulCompilations;
            this.failedCompilations = failedCompilations;
            this.averageCompilationTime = averageCompilationTime;
            this.successRate = successRate;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.cacheHitRate = cacheHitRate;
            this.totalParsingOperations = totalParsingOperations;
            this.fastPathHits = fastPathHits;
            this.slowPathHits = slowPathHits;
            this.fastPathRate = fastPathRate;
            this.objectsCreated = objectsCreated;
            this.objectsReused = objectsReused;
            this.objectReuseRate = objectReuseRate;
            this.expressionMetrics = expressionMetrics;
            this.responseTimeMetrics = responseTimeMetrics;
            this.timestamp = System.currentTimeMillis();

            // Valeurs par défaut pour le système hybride
            this.reportGenerationTime = System.currentTimeMillis();
            this.totalOperationsPerSecond = totalCompilations / 30.0;
            this.averageResponseTime = responseTimeMetrics.containsKey("compilation") ?
                    responseTimeMetrics.get("compilation").getAverageTime() : 0.0;
        }

        /**
         * Obtient le nombre de succès pour une opération donnée
         */
        public long getSuccessCount(String operationType) {
            return successCounts.computeIfAbsent(operationType, k -> new AtomicLong(0)).get();
        }

        /**
         * Obtient le nombre d'échecs pour une opération donnée
         */
        public long getFailureCount(String operationType) {
            return failureCounts.computeIfAbsent(operationType, k -> new AtomicLong(0)).get();
        }

        /**
         * Enregistre un succès pour une opération donnée
         */
        public void recordSuccess(String operationType) {
            successCounts.computeIfAbsent(operationType, k -> new AtomicLong(0)).incrementAndGet();
        }

        /**
         * Enregistre un échec pour une opération donnée
         */
        public void recordFailure(String operationType) {
            failureCounts.computeIfAbsent(operationType, k -> new AtomicLong(0)).incrementAndGet();
        }

        public long getReportGenerationTime() {
            return reportGenerationTime;
        }

        public double getTotalOperationsPerSecond() {
            return totalOperationsPerSecond;
        }

        public double getAverageResponseTime() {
            return averageResponseTime;
        }
    }

    /**
     * Statistiques du cache intelligent
     */
    public static class SmartCacheStats {
        public final double hitRate;
        public final long size;
        public final long hits;
        public final long misses;
        public final long evictions;

        public SmartCacheStats(double hitRate, long size, long hits, long misses, long evictions) {
            this.hitRate = hitRate;
            this.size = size;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
        }
    }

    /**
     * Statistiques des performances du moteur
     */
    public static class EngineStats {
        public final SmartCacheStats cacheStats;
        public final int executionThreads;
        public final int cacheSize;
        public final long totalOperations;
        public final double averageLatency;

        public EngineStats(SmartCacheStats cacheStats, int executionThreads, int cacheSize, long totalOperations, double averageLatency) {
            this.cacheStats = cacheStats;
            this.executionThreads = executionThreads;
            this.cacheSize = cacheSize;
            this.totalOperations = totalOperations;
            this.averageLatency = averageLatency;
        }
    }
}





