package fr.nico.sqript.core.compilation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implémentation thread-safe d'un cache LRU (Least Recently Used).
 * Optimisé pour les performances avec une complexité O(1) pour get/put.
 *
 * @param <K> Type des clés
 * @param <V> Type des valeurs
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class LRUCache<K, V> {

    private final int maxSize;
    private final ConcurrentHashMap<K, Node<K, V>> cache;
    private final ReentrantLock lock = new ReentrantLock();
    // Sentinelles pour la liste doublement chaînée
    private final Node<K, V> head;
    private final Node<K, V> tail;
    // Statistiques
    private volatile int currentSize = 0;
    private volatile long accessCount = 0;
    private volatile long evictionCount = 0;
    public LRUCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive");
        }

        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>(maxSize);

        // Initialiser les sentinelles
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    /**
     * Récupère une valeur du cache.
     *
     * @param key La clé à rechercher
     * @return La valeur associée ou null si non trouvée
     */
    public V get(K key) {
        if (key == null) {
            return null;
        }

        Node<K, V> node = cache.get(key);
        if (node == null) {
            return null;
        }

        // Mettre à jour l'ordre d'accès
        lock.lock();
        try {
            moveToHead(node);
            node.accessTime = System.nanoTime();
            accessCount++;
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Met une valeur dans le cache.
     *
     * @param key   La clé
     * @param value La valeur
     * @return L'ancienne valeur associée à la clé, ou null
     */
    public V put(K key, V value) {
        if (key == null || value == null) {
            return null;
        }

        lock.lock();
        try {
            Node<K, V> existingNode = cache.get(key);

            if (existingNode != null) {
                // Mise à jour d'une valeur existante
                V oldValue = existingNode.value;
                existingNode.value = value;
                existingNode.accessTime = System.nanoTime();
                moveToHead(existingNode);
                return oldValue;
            }

            // Nouvelle entrée
            Node<K, V> newNode = new Node<>(key, value);
            cache.put(key, newNode);
            addToHead(newNode);
            currentSize++;

            // Vérifier si on dépasse la capacité
            if (currentSize > maxSize) {
                Node<K, V> tail = removeTail();
                cache.remove(tail.key);
                currentSize--;
                evictionCount++;
            }

            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Supprime une entrée du cache.
     *
     * @param key La clé à supprimer
     * @return La valeur supprimée ou null
     */
    public V remove(K key) {
        if (key == null) {
            return null;
        }

        lock.lock();
        try {
            Node<K, V> node = cache.remove(key);
            if (node != null) {
                removeNode(node);
                currentSize--;
                return node.value;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Vide complètement le cache.
     */
    public void clear() {
        lock.lock();
        try {
            cache.clear();
            head.next = tail;
            tail.prev = head;
            currentSize = 0;
            accessCount = 0;
            evictionCount = 0;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Vérifie si une clé existe dans le cache.
     */
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    /**
     * Retourne la taille actuelle du cache.
     */
    public int size() {
        return currentSize;
    }

    /**
     * Vérifie si le cache est vide.
     */
    public boolean isEmpty() {
        return currentSize == 0;
    }

    /**
     * Retourne la taille maximale du cache.
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Retourne la capacité du cache (alias pour getMaxSize).
     */
    public int getCapacity() {
        return maxSize;
    }

    /**
     * Retourne le nombre total d'accès au cache.
     */
    public long getAccessCount() {
        return accessCount;
    }

    /**
     * Retourne le nombre d'évictions du cache.
     */
    public long getEvictionCount() {
        return evictionCount;
    }

    /**
     * Retourne le taux d'occupation du cache (0.0 à 1.0).
     */
    public double getLoadFactor() {
        return (double) currentSize / maxSize;
    }

    /**
     * Optimise le cache en supprimant les entrées les plus anciennes
     * si le taux d'occupation dépasse le seuil spécifié.
     */
    public void optimizeIfNeeded(double threshold) {
        if (getLoadFactor() > threshold) {
            lock.lock();
            try {
                int targetSize = (int) (maxSize * 0.8); // Réduire à 80% de la capacité
                while (currentSize > targetSize) {
                    Node<K, V> tail = removeTail();
                    cache.remove(tail.key);
                    currentSize--;
                    evictionCount++;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Ajoute un nœud juste après la tête.
     */
    private void addToHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    // === MÉTHODES PRIVÉES POUR LA GESTION DE LA LISTE ===

    /**
     * Supprime un nœud de la liste.
     */
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    /**
     * Déplace un nœud vers la tête.
     */
    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }

    /**
     * Supprime le dernier nœud (le moins récemment utilisé).
     */
    private Node<K, V> removeTail() {
        Node<K, V> lastNode = tail.prev;
        removeNode(lastNode);
        return lastNode;
    }

    @Override
    public String toString() {
        return String.format("LRUCache{size=%d/%d, accesses=%d, evictions=%d, loadFactor=%.2f}",
                currentSize, maxSize, accessCount, evictionCount, getLoadFactor());
    }

    /**
     * Nœud de la liste doublement chaînée.
     */
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;
        long accessTime;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.accessTime = System.nanoTime();
        }
    }
}





