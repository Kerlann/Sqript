package fr.nico.sqript.core.compilation;

import fr.nico.sqript.meta.ActionDefinition;
import fr.nico.sqript.meta.ExpressionDefinition;
import fr.nico.sqript.meta.LoopDefinition;
import fr.nico.sqript.structures.TransformedPattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matcher optimisé pour les patterns de script.
 * Utilise un cache intelligent et des optimisations de performance.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class PatternMatcher {

    private static final Logger log = LogManager.getLogger(PatternMatcher.class);

    private final CompilationCache cache;

    // Cache spécialisé pour les patterns précompilés
    private final ConcurrentHashMap<String, CompiledPattern> patternCache = new ConcurrentHashMap<>();

    // Statistiques
    private final AtomicLong matchAttempts = new AtomicLong(0);
    private final AtomicLong matchHits = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);

    public PatternMatcher(CompilationCache cache) {
        this.cache = cache;
    }


    /**
     * Vérifie si une chaîne correspond à un pattern d'action.
     *
     * @param actionDef La définition d'action
     * @param input     La chaîne à tester
     * @return true si correspondance trouvée
     */
    public boolean matches(ActionDefinition actionDef, String input) {
        if (actionDef == null || input == null) {
            return false;
        }

        matchAttempts.incrementAndGet();

        TransformedPattern[] patterns = actionDef.transformedPatterns;
        if (patterns == null) {
            return false;
        }

        for (TransformedPattern transformedPattern : patterns) {
            if (matches(transformedPattern, input)) {
                matchHits.incrementAndGet();
                return true;
            }
        }

        return false;
    }

    /**
     * Vérifie si une chaîne correspond à un pattern d'expression.
     *
     * @param exprDef La définition d'expression
     * @param input   La chaîne à tester
     * @return true si correspondance trouvée
     */
    public boolean matches(ExpressionDefinition exprDef, String input) {

        if (exprDef == null || input == null) {
            return false;
        }

        matchAttempts.incrementAndGet();

        TransformedPattern[] patterns = exprDef.transformedPatterns;
        if (patterns == null) {
            return false;
        }

        for (TransformedPattern transformedPattern : patterns) {
            if (matches(transformedPattern, input)) {
                matchHits.incrementAndGet();
                return true;
            }
        }

        return false;
    }

    /**
     * Vérifie si une chaîne correspond à un pattern de boucle.
     *
     * @param loopDef La définition de boucle
     * @param input   La chaîne à tester
     * @return true si correspondance trouvée
     */
    public boolean matches(LoopDefinition loopDef, String input) {

        if (loopDef == null || input == null) {
            return false;
        }

        matchAttempts.incrementAndGet();

        if (loopDef.matches(input)) {
            matchHits.incrementAndGet();
            return true;
        }

        return false;
    }

    /**
     * Vérifie si une chaîne correspond à un TransformedPattern.
     *
     * @param transformedPattern Le pattern transformé
     * @param input              La chaîne à tester
     * @return true si correspondance trouvée
     */
    public boolean matches(TransformedPattern transformedPattern, String input) {

        if (transformedPattern == null || input == null) {
            return false;
        }

        String patternString = transformedPattern.getPattern().pattern();
        return matchesPattern(patternString, input);
    }

    /**
     * Vérifie si une chaîne correspond à un pattern string.
     *
     * @param patternString Le pattern en tant que chaîne
     * @param input         La chaîne à tester
     * @return true si correspondance trouvée
     */
    public boolean matchesPattern(String patternString, String input) {

        if (patternString == null || input == null) {
            return false;
        }

        CompiledPattern compiledPattern = getOrCompilePattern(patternString);
        return compiledPattern.matches(input);
    }

    /**
     * Retourne un Matcher pour un pattern et une chaîne.
     *
     * @param transformedPattern Le pattern transformé
     * @param input              La chaîne à analyser
     * @return Le Matcher configuré
     */
    public Matcher getMatcher(TransformedPattern transformedPattern, String input) {

        if (transformedPattern == null || input == null) {
            return null;
        }

        Pattern pattern = transformedPattern.getPattern();
        return pattern.matcher(input);
    }

    /**
     * Retourne un Matcher pour un pattern string et une chaîne.
     *
     * @param patternString Le pattern en tant que chaîne
     * @param input         La chaîne à analyser
     * @return Le Matcher configuré
     */
    public Matcher getMatcher(String patternString, String input) {

        if (patternString == null || input == null) {
            return null;
        }

        CompiledPattern compiledPattern = getOrCompilePattern(patternString);
        return compiledPattern.createMatcher(input);
    }

    /**
     * Trouve le premier pattern correspondant parmi plusieurs.
     *
     * @param patterns Les patterns à tester
     * @param input    La chaîne à tester
     * @return L'index du premier pattern correspondant, ou -1
     */
    public int findFirstMatch(TransformedPattern[] patterns, String input) {

        if (patterns == null || input == null) {
            return -1;
        }

        for (int i = 0; i < patterns.length; i++) {
            if (matches(patterns[i], input)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Précompile un pattern pour optimiser les futures utilisations.
     *
     * @param actionDef La définition d'action à précompiler
     */
    public void precompilePattern(ActionDefinition actionDef) {

        if (actionDef == null) {
            return;
        }

        TransformedPattern[] patterns = actionDef.transformedPatterns;
        if (patterns != null) {
            for (TransformedPattern pattern : patterns) {
                precompilePattern(pattern);
            }
        }
    }

    /**
     * Précompile un pattern pour optimiser les futures utilisations.
     *
     * @param exprDef La définition d'expression à précompiler
     */
    public void precompilePattern(ExpressionDefinition exprDef) {

        if (exprDef == null) {
            return;
        }

        TransformedPattern[] patterns = exprDef.transformedPatterns;
        if (patterns != null) {
            for (TransformedPattern pattern : patterns) {
                precompilePattern(pattern);
            }
        }
    }

    /**
     * Précompile un TransformedPattern.
     *
     * @param transformedPattern Le pattern à précompiler
     */
    public void precompilePattern(TransformedPattern transformedPattern) {

        if (transformedPattern == null) {
            return;
        }

        String patternString = transformedPattern.getPattern().pattern();
        getOrCompilePattern(patternString); // Force la compilation et mise en cache
    }

    /**
     * Nettoie le cache des patterns pour libérer la mémoire.
     */
    public void clearCache() {

        patternCache.clear();
        log.info("Pattern cache cleared");
    }

    /**
     * Optimise le cache en supprimant les patterns peu utilisés.
     */
    public void optimizeCache() {

        int sizeBefore = patternCache.size();

        // Supprimer les patterns avec peu d'utilisations
        patternCache.entrySet().removeIf(entry -> {
            CompiledPattern pattern = entry.getValue();
            return pattern.getUsageCount() < 5 &&
                    System.currentTimeMillis() - pattern.getCreationTime() > 300_000; // 5 minutes
        });

        int sizeAfter = patternCache.size();
        log.info("Pattern cache optimized: {} → {} patterns", sizeBefore, sizeAfter);
    }

    /**
     * Retourne les statistiques du matcher.
     */
    public MatcherStats getStats() {
        long attempts = matchAttempts.get();
        long hits = matchHits.get();
        long cacheHitsCount = cacheHits.get();

        double hitRate = attempts > 0 ? (double) hits / attempts : 0.0;
        double cacheHitRate = attempts > 0 ? (double) cacheHitsCount / attempts : 0.0;

        return new MatcherStats(
                attempts, hits, hitRate,
                cacheHitsCount, cacheHitRate,
                patternCache.size()
        );
    }

    // === MÉTHODES PRIVÉES ===


    private CompiledPattern getOrCompilePattern(String patternString) {
        CompiledPattern compiled = patternCache.get(patternString);

        if (compiled != null) {
            cacheHits.incrementAndGet();
            compiled.incrementUsage();
            return compiled;
        }

        // Vérifier dans le cache global
        Pattern cachedPattern = cache.getPattern(patternString);
        if (cachedPattern != null) {
            compiled = new CompiledPattern(cachedPattern);
            patternCache.put(patternString, compiled);
            return compiled;
        }

        // Compiler le pattern
        try {
            Pattern pattern = Pattern.compile(patternString);
            cache.putPattern(patternString, pattern);
            compiled = new CompiledPattern(pattern);
            patternCache.put(patternString, compiled);
            return compiled;
        } catch (Exception e) {
            log.warn("Failed to compile pattern: {}", patternString, e);
            // Retourner un pattern qui ne match jamais
            return new CompiledPattern(Pattern.compile("(?!)"));
        }
    }

    /**
     * Wrapper pour un pattern compilé avec métriques.
     */
    private static class CompiledPattern {
        private final Pattern pattern;
        private final long creationTime;
        private final AtomicLong usageCount = new AtomicLong(0);

        public CompiledPattern(Pattern pattern) {
            this.pattern = pattern;
            this.creationTime = System.currentTimeMillis();
        }

        public boolean matches(String input) {
            usageCount.incrementAndGet();
            return pattern.matcher(input).matches();
        }

        public Matcher createMatcher(String input) {
            usageCount.incrementAndGet();
            return pattern.matcher(input);
        }

        public void incrementUsage() {
            usageCount.incrementAndGet();
        }

        public long getUsageCount() {
            return usageCount.get();
        }

        public long getCreationTime() {
            return creationTime;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

    /**
     * Statistiques du matcher de patterns.
     */
    public static class MatcherStats {
        public final long matchAttempts;
        public final long matchHits;
        public final double hitRate;
        public final long cacheHits;
        public final double cacheHitRate;
        public final int cachedPatterns;

        public MatcherStats(long matchAttempts, long matchHits, double hitRate,
                            long cacheHits, double cacheHitRate, int cachedPatterns) {
            this.matchAttempts = matchAttempts;
            this.matchHits = matchHits;
            this.hitRate = hitRate;
            this.cacheHits = cacheHits;
            this.cacheHitRate = cacheHitRate;
            this.cachedPatterns = cachedPatterns;
        }

        @Override
        public String toString() {
            return String.format("MatcherStats{attempts=%d, hits=%d, hitRate=%.2f%%, " +
                            "cacheHits=%d, cacheHitRate=%.2f%%, cachedPatterns=%d}",
                    matchAttempts, matchHits, hitRate * 100,
                    cacheHits, cacheHitRate * 100, cachedPatterns);
        }
    }
}





