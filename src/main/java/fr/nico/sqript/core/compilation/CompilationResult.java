package fr.nico.sqript.core.compilation;

/**
 * Résultat unifié pour toutes les opérations de compilation.
 * Encapsule le succès/échec, les métriques de performance et le cache.
 *
 * @param <T> Le type de résultat de la compilation
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class CompilationResult<T> {

    private final boolean success;
    private final T result;
    private final Throwable error;
    private final long compilationTimeNanos;
    private final boolean fromCache;

    private CompilationResult(boolean success, T result, Throwable error,
                              long compilationTimeNanos, boolean fromCache) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.compilationTimeNanos = compilationTimeNanos;
        this.fromCache = fromCache;
    }

    /**
     * Crée un résultat de compilation réussi.
     */
    public static <T> CompilationResult<T> success(T result, long compilationTimeNanos, boolean fromCache) {
        return new CompilationResult<>(true, result, null, compilationTimeNanos, fromCache);
    }

    /**
     * Crée un résultat de compilation échoué.
     */
    public static <T> CompilationResult<T> failure(Throwable error) {
        return new CompilationResult<>(false, null, error, 0, false);
    }

    /**
     * Indique si la compilation a réussi.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Retourne le résultat de la compilation (null si échec).
     */
    public T getResult() {
        return result;
    }

    /**
     * Retourne l'erreur de compilation (null si succès).
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Retourne le temps de compilation en nanosecondes.
     */
    public long getCompilationTimeNanos() {
        return compilationTimeNanos;
    }

    /**
     * Retourne le temps de compilation en millisecondes.
     */
    public double getCompilationTimeMillis() {
        return compilationTimeNanos / 1_000_000.0;
    }

    /**
     * Indique si le résultat provient du cache.
     */
    public boolean isFromCache() {
        return fromCache;
    }

    /**
     * Transforme ce résultat en appliquant une fonction si c'est un succès.
     */
    public <U> CompilationResult<U> map(Function<T, U> mapper) {
        if (success) {
            try {
                U mappedResult = mapper.apply(result);
                return success(mappedResult, compilationTimeNanos, fromCache);
            } catch (Exception e) {
                return failure(e);
            }
        } else {
            return failure(error);
        }
    }

    /**
     * Transforme ce résultat en appliquant une fonction qui retourne un CompilationResult.
     */
    public <U> CompilationResult<U> flatMap(Function<T, CompilationResult<U>> mapper) {
        if (success) {
            try {
                return mapper.apply(result);
            } catch (Exception e) {
                return failure(e);
            }
        } else {
            return failure(error);
        }
    }

    /**
     * Exécute une action si la compilation a réussi.
     */
    public CompilationResult<T> ifSuccess(Consumer<T> action) {
        if (success && result != null) {
            try {
                action.accept(result);
            } catch (Exception e) {
                // Log l'erreur mais ne change pas le résultat
                System.err.println("Error in ifSuccess callback: " + e.getMessage());
            }
        }
        return this;
    }

    /**
     * Exécute une action si la compilation a échoué.
     */
    public CompilationResult<T> ifFailure(Consumer<Throwable> action) {
        if (!success && error != null) {
            try {
                action.accept(error);
            } catch (Exception e) {
                // Log l'erreur mais ne change pas le résultat
                System.err.println("Error in ifFailure callback: " + e.getMessage());
            }
        }
        return this;
    }

    /**
     * Retourne le résultat ou une valeur par défaut en cas d'échec.
     */
    public T orElse(T defaultValue) {
        return success ? result : defaultValue;
    }

    /**
     * Retourne le résultat ou lance l'exception en cas d'échec.
     */
    public T orElseThrow() throws Exception {
        if (success) {
            return result;
        } else {
            if (error instanceof Exception) {
                throw (Exception) error;
            } else if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new RuntimeException("Compilation failed", error);
            }
        }
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("CompilationResult{success=true, result=%s, time=%.2fms, cached=%s}",
                    result, getCompilationTimeMillis(), fromCache);
        } else {
            return String.format("CompilationResult{success=false, error=%s}",
                    error != null ? error.getMessage() : "Unknown error");
        }
    }

    // Interfaces fonctionnelles pour éviter les dépendances externes
    @FunctionalInterface
    public interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t) throws Exception;
    }
}





