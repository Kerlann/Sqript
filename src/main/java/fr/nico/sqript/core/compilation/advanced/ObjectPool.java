package fr.nico.sqript.core.compilation.advanced;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pool d'objets avancé pour réduire les allocations et la pression GC.
 * Supporte plusieurs types d'objets avec stratégies de recyclage optimisées.
 *
 * @author Nico-
 * @version 2.0
 * @since Phase 2 - Performance
 */
public class ObjectPool<T> {

    private final ThreadLocal<Queue<T>> localPool;
    private final Queue<WeakReference<T>> globalPool;
    private final ObjectFactory<T> factory;
    private final ObjectResetter<T> resetter;
    private final ReentrantLock lock;

    private final int maxLocalSize;
    private final int maxGlobalSize;

    // Métriques
    private volatile long totalCreated = 0;
    private volatile long totalReused = 0;
    private volatile long totalReturned = 0;

    public ObjectPool(ObjectFactory<T> factory, ObjectResetter<T> resetter) {
        this(factory, resetter, 8, 32);
    }

    public ObjectPool(ObjectFactory<T> factory, ObjectResetter<T> resetter,
                      int maxLocalSize, int maxGlobalSize) {
        this.factory = factory;
        this.resetter = resetter;
        this.maxLocalSize = maxLocalSize;
        this.maxGlobalSize = maxGlobalSize;
        this.lock = new ReentrantLock();

        this.localPool = ThreadLocal.withInitial(() -> new ArrayDeque<>(maxLocalSize));
        this.globalPool = new ArrayDeque<>(maxGlobalSize);
    }

    /**
     * Emprunte un objet du pool ou en crée un nouveau.
     */
    public T borrow() {
        // Essayer d'abord le pool local (lock-free)
        Queue<T> local = localPool.get();
        T object = local.poll();

        if (object != null) {
            totalReused++;
            return object;
        }

        // Essayer le pool global (avec lock)
        lock.lock();
        try {
            Iterator<WeakReference<T>> it = globalPool.iterator();
            while (it.hasNext()) {
                WeakReference<T> ref = it.next();
                object = ref.get();
                if (object != null && resetter.isValid(object)) {
                    it.remove();
                    resetter.reset(object);
                    totalReused++;
                    return object;
                } else {
                    // Référence morte, on la supprime
                    it.remove();
                }
            }
        } finally {
            lock.unlock();
        }

        // Créer un nouvel objet
        object = factory.create();
        totalCreated++;
        return object;
    }

    /**
     * Retourne un objet au pool pour réutilisation future.
     */
    public void returnObject(T object) {
        if (object == null || !resetter.isValid(object)) {
            return;
        }

        totalReturned++;
        resetter.reset(object);

        // Essayer d'abord le pool local
        Queue<T> local = localPool.get();
        if (local.size() < maxLocalSize) {
            local.offer(object);
            return;
        }

        // Sinon utiliser le pool global
        lock.lock();
        try {
            if (globalPool.size() < maxGlobalSize) {
                globalPool.offer(new WeakReference<>(object));
            }
            // Si les deux pools sont pleins, on laisse l'objet être GC
        } finally {
            lock.unlock();
        }
    }

    /**
     * Nettoie le pool et libère les ressources.
     */
    public void clear() {
        // Nettoyer les pools locaux de tous les threads
        // Note: On ne peut pas facilement accéder aux pools locaux des autres threads
        localPool.get().clear();

        // Nettoyer le pool global
        lock.lock();
        try {
            globalPool.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retourne les statistiques d'utilisation du pool.
     */
    public PoolStats getStats() {
        lock.lock();
        try {
            int localSize = localPool.get().size();
            int globalSize = globalPool.size();

            return new PoolStats(
                    totalCreated,
                    totalReused,
                    totalReturned,
                    localSize,
                    globalSize,
                    calculateReuseRate()
            );
        } finally {
            lock.unlock();
        }
    }

    private double calculateReuseRate() {
        long total = totalCreated + totalReused;
        return total > 0 ? (double) totalReused / total : 0.0;
    }

    /**
     * Interface pour créer de nouveaux objets.
     */
    public interface ObjectFactory<T> {
        T create();
    }

    /**
     * Interface pour nettoyer/réinitialiser les objets avant réutilisation.
     */
    public interface ObjectResetter<T> {
        void reset(T object);

        default boolean isValid(T object) {
            return true;
        }
    }

    /**
     * Statistiques du pool d'objets.
     */
    public static class PoolStats {
        public final long totalCreated;
        public final long totalReused;
        public final long totalReturned;
        public final int localPoolSize;
        public final int globalPoolSize;
        public final double reuseRate;

        public PoolStats(long totalCreated, long totalReused, long totalReturned,
                         int localPoolSize, int globalPoolSize, double reuseRate) {
            this.totalCreated = totalCreated;
            this.totalReused = totalReused;
            this.totalReturned = totalReturned;
            this.localPoolSize = localPoolSize;
            this.globalPoolSize = globalPoolSize;
            this.reuseRate = reuseRate;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{created=%d, reused=%d, returned=%d, " +
                            "localSize=%d, globalSize=%d, reuseRate=%.1f%%}",
                    totalCreated, totalReused, totalReturned,
                    localPoolSize, globalPoolSize, reuseRate * 100);
        }
    }
}





