package fr.nico.sqript.core.compilation.advanced;

import fr.nico.sqript.core.compilation.LRUCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Cache intelligent multi-niveaux avec stratégies adaptatives.
 * Optimise automatiquement les performances selon les patterns d'usage.
 *
 * @author Nico-
 * @version 2.0
 * @since Phase 2 - Performance
 */
public class SmartCache<K, V> {

    private static final Logger log = LogManager.getLogger(SmartCache.class);

    // Cache L1: Hot data (très fréquent, accès ultra-rapide)
    private final LRUCache<K, V> hotCache;

    // Cache L2: Warm data (fréquent, accès rapide)
    private final LRUCache<K, V> warmCache;

    // Cache L3: Cold data (peu fréquent, accès normal)
    private final LRUCache<K, V> coldCache;

    // Métadonnées d'accès pour l'algorithme adaptatif
    private final ConcurrentHashMap<K, AccessMetadata> accessMap;

    // Statistiques globales
    private final AtomicLong totalHits = new AtomicLong(0);
    private final AtomicLong totalMisses = new AtomicLong(0);
    private final AtomicLong hotHits = new AtomicLong(0);
    private final AtomicLong warmHits = new AtomicLong(0);
    private final AtomicLong coldHits = new AtomicLong(0);
    private final ReentrantReadWriteLock adaptationLock = new ReentrantReadWriteLock();
    // Configuration adaptative
    private volatile double hotThreshold = 0.8;    // Seuil pour promotion vers hot
    private volatile double warmThreshold = 0.5;   // Seuil pour promotion vers warm
    private final long adaptationInterval = 1000; // Intervalle d'adaptation en ms
    private volatile long lastAdaptation = System.currentTimeMillis();

    public SmartCache(int hotSize, int warmSize, int coldSize) {
        this.hotCache = new LRUCache<>(hotSize);
        this.warmCache = new LRUCache<>(warmSize);
        this.coldCache = new LRUCache<>(coldSize);
        this.accessMap = new ConcurrentHashMap<>();

        log.debug("SmartCache initialized: hot={}, warm={}, cold={}",
                hotSize, warmSize, coldSize);
    }

    /**
     * Récupère une valeur du cache avec optimisation adaptative.
     */
    public V get(K key) {
        if (key == null) return null;

        // Enregistrer l'accès pour l'algorithme adaptatif
        AccessMetadata metadata = accessMap.computeIfAbsent(key, k -> new AccessMetadata());
        metadata.recordAccess();

        // Recherche dans les caches par ordre de performance
        V value = hotCache.get(key);
        if (value != null) {
            hotHits.incrementAndGet();
            totalHits.incrementAndGet();
            return value;
        }

        value = warmCache.get(key);
        if (value != null) {
            warmHits.incrementAndGet();
            totalHits.incrementAndGet();

            // Promouvoir vers hot si nécessaire
            if (shouldPromoteToHot(metadata)) {
                promoteToHot(key, value, metadata);
            }
            return value;
        }

        value = coldCache.get(key);
        if (value != null) {
            coldHits.incrementAndGet();
            totalHits.incrementAndGet();

            // Promouvoir vers warm si nécessaire
            if (shouldPromoteToWarm(metadata)) {
                promoteToWarm(key, value, metadata);
            }
            return value;
        }

        totalMisses.incrementAndGet();

        // Adaptation périodique des seuils
        considerAdaptation();

        return null;
    }

    /**
     * Stocke une valeur dans le cache avec placement intelligent.
     */
    public V put(K key, V value) {
        if (key == null || value == null) return null;

        AccessMetadata metadata = accessMap.computeIfAbsent(key, k -> new AccessMetadata());

        // Placement intelligent basé sur les métadonnées
        CacheLevel targetLevel = determineOptimalLevel(metadata);

        V oldValue = null;
        switch (targetLevel) {
            case HOT:
                oldValue = hotCache.put(key, value);
                metadata.currentLevel = CacheLevel.HOT;
                break;
            case WARM:
                oldValue = warmCache.put(key, value);
                metadata.currentLevel = CacheLevel.WARM;
                break;
            case COLD:
                oldValue = coldCache.put(key, value);
                metadata.currentLevel = CacheLevel.COLD;
                break;
        }

        return oldValue;
    }

    /**
     * Calcule une valeur et la met en cache si elle n'existe pas.
     */
    public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
        V value = get(key);
        if (value != null) {
            return value;
        }

        value = mappingFunction.apply(key);
        if (value != null) {
            put(key, value);
        }

        return value;
    }

    /**
     * Supprime une entrée de tous les niveaux de cache.
     */
    public V remove(K key) {
        if (key == null) return null;

        accessMap.remove(key);

        V value = hotCache.remove(key);
        if (value == null) {
            value = warmCache.remove(key);
        }
        if (value == null) {
            value = coldCache.remove(key);
        }

        return value;
    }

    /**
     * Nettoie tous les caches.
     */
    public void clear() {
        hotCache.clear();
        warmCache.clear();
        coldCache.clear();
        accessMap.clear();
    }

    /**
     * Optimise la distribution des données entre les caches.
     */
    public void optimize() {
        adaptationLock.writeLock().lock();
        try {
            log.debug("Optimizing cache distribution...");

            // Analyser les patterns d'accès
            long totalAccesses = totalHits.get() + totalMisses.get();
            if (totalAccesses < 100) return; // Pas assez de données

            double hotHitRate = (double) hotHits.get() / totalAccesses;
            double warmHitRate = (double) warmHits.get() / totalAccesses;

            // Ajuster les seuils selon les performances
            if (hotHitRate < 0.3) {
                hotThreshold = Math.max(0.6, hotThreshold - 0.1);
            } else if (hotHitRate > 0.6) {
                hotThreshold = Math.min(0.9, hotThreshold + 0.05);
            }

            if (warmHitRate < 0.2) {
                warmThreshold = Math.max(0.3, warmThreshold - 0.1);
            } else if (warmHitRate > 0.4) {
                warmThreshold = Math.min(0.7, warmThreshold + 0.05);
            }

            log.debug("Cache thresholds adapted: hot={}, warm={}", hotThreshold, warmThreshold);

        } finally {
            adaptationLock.writeLock().unlock();
        }
    }

    /**
     * Retourne les statistiques détaillées du cache.
     */
    public SmartCacheStats getStats() {
        long hits = totalHits.get();
        long misses = totalMisses.get();
        long total = hits + misses;

        return new SmartCacheStats(
                hits, misses, total > 0 ? (double) hits / total : 0.0,
                hotHits.get(), warmHits.get(), coldHits.get(),
                hotCache.size(), warmCache.size(), coldCache.size(),
                hotCache.getCapacity(), warmCache.getCapacity(), coldCache.getCapacity(),
                hotThreshold, warmThreshold
        );
    }

    private boolean shouldPromoteToHot(AccessMetadata metadata) {
        return metadata.frequency > hotThreshold &&
                metadata.currentLevel != CacheLevel.HOT;
    }

    private boolean shouldPromoteToWarm(AccessMetadata metadata) {
        return metadata.frequency > warmThreshold &&
                metadata.currentLevel == CacheLevel.COLD;
    }

    // === MÉTHODES PRIVÉES ===

    private void promoteToHot(K key, V value, AccessMetadata metadata) {
        // Supprimer des caches inférieurs
        warmCache.remove(key);
        coldCache.remove(key);

        // Ajouter au cache hot
        hotCache.put(key, value);
        metadata.currentLevel = CacheLevel.HOT;

        log.trace("Promoted {} to HOT cache", key);
    }

    private void promoteToWarm(K key, V value, AccessMetadata metadata) {
        // Supprimer du cache cold
        coldCache.remove(key);

        // Ajouter au cache warm
        warmCache.put(key, value);
        metadata.currentLevel = CacheLevel.WARM;

        log.trace("Promoted {} to WARM cache", key);
    }

    private CacheLevel determineOptimalLevel(AccessMetadata metadata) {
        if (metadata.frequency > hotThreshold) {
            return CacheLevel.HOT;
        } else if (metadata.frequency > warmThreshold) {
            return CacheLevel.WARM;
        } else {
            return CacheLevel.COLD;
        }
    }

    private void considerAdaptation() {
        long now = System.currentTimeMillis();
        if (now - lastAdaptation > adaptationInterval) {
            if (adaptationLock.writeLock().tryLock()) {
                try {
                    if (now - lastAdaptation > adaptationInterval) {
                        optimize();
                        lastAdaptation = now;
                    }
                } finally {
                    adaptationLock.writeLock().unlock();
                }
            }
        }
    }

    private enum CacheLevel {
        HOT, WARM, COLD
    }

    /**
     * Métadonnées d'accès pour chaque clé.
     */
    private static class AccessMetadata {
        volatile long accessCount = 1;
        volatile long lastAccess = System.currentTimeMillis();
        volatile double frequency = 0.0;
        volatile CacheLevel currentLevel = CacheLevel.COLD;

        void recordAccess() {
            accessCount++;
            long now = System.currentTimeMillis();
            long timeDelta = now - lastAccess;
            lastAccess = now;

            // Calcul de fréquence avec décroissance temporelle
            if (timeDelta > 0) {
                frequency = (frequency * 0.9) + (1000.0 / timeDelta * 0.1);
            }
        }
    }

    /**
     * Statistiques détaillées du SmartCache.
     */
    public static class SmartCacheStats {
        public final long hits;
        public final long misses;
        public final double hitRate;
        public final long hotHits;
        public final long warmHits;
        public final long coldHits;
        public final int hotSize;
        public final int warmSize;
        public final int coldSize;
        public final int hotCapacity;
        public final int warmCapacity;
        public final int coldCapacity;
        public final double hotThreshold;
        public final double warmThreshold;

        public SmartCacheStats(long hits, long misses, double hitRate,
                               long hotHits, long warmHits, long coldHits,
                               int hotSize, int warmSize, int coldSize,
                               int hotCapacity, int warmCapacity, int coldCapacity,
                               double hotThreshold, double warmThreshold) {
            this.hits = hits;
            this.misses = misses;
            this.hitRate = hitRate;
            this.hotHits = hotHits;
            this.warmHits = warmHits;
            this.coldHits = coldHits;
            this.hotSize = hotSize;
            this.warmSize = warmSize;
            this.coldSize = coldSize;
            this.hotCapacity = hotCapacity;
            this.warmCapacity = warmCapacity;
            this.coldCapacity = coldCapacity;
            this.hotThreshold = hotThreshold;
            this.warmThreshold = warmThreshold;
        }

        @Override
        public String toString() {
            return String.format("SmartCacheStats{hitRate=%.1f%%, hits=%d(H:%d/W:%d/C:%d), " +
                            "misses=%d, sizes=%d/%d/%d, capacities=%d/%d/%d, " +
                            "thresholds=%.2f/%.2f}",
                    hitRate * 100, hits, hotHits, warmHits, coldHits, misses,
                    hotSize, warmSize, coldSize,
                    hotCapacity, warmCapacity, coldCapacity,
                    hotThreshold, warmThreshold);
        }
    }
}





