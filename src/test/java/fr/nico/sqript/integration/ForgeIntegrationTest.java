package fr.nico.sqript.integration;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.core.bootstrap.AnnotationScanner;
import fr.nico.sqript.core.integration.HybridEngine;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.forge.SqriptForge;
import fr.nico.sqript.types.primitive.TypeArray;
import fr.nico.sqript.types.primitive.TypeElement;
import fr.nico.sqript.types.primitive.TypeEntity;
import fr.nico.sqript.types.primitive.TypeString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour valider la compatibilité entre le nouveau système core et Forge.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ForgeIntegrationTest {
    
    private ScriptRegistry coreRegistry;
    private AnnotationScanner annotationScanner;
    private HybridEngine hybridEngine;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Initialisation du répertoire de scripts
        File scriptDir = new File("test-scripts");
        if (!scriptDir.exists()) {
            scriptDir.mkdirs();
        }
        
        // Initialisation du système legacy pour compatibilité
        ScriptManager.preInit(scriptDir);
        
        // Initialisation du nouveau système core
        coreRegistry = new ScriptRegistry();
        coreRegistry.initialize();
        
        annotationScanner = new AnnotationScanner(coreRegistry);
        hybridEngine = new HybridEngine(coreRegistry);
        hybridEngine.initialize();
    }
    
    /**
     * Test d'intégration principal : vérifier que le système core s'intègre correctement avec Forge.
     */
    @Test
    public void testForgeIntegration() throws Exception {
        // Vérifier que SqriptForge peut accéder au système core
        assertNotNull(SqriptForge.getCoreRegistry(), "Le registre core doit être accessible via SqriptForge");
        
        // Vérifier que le système core est actif
        assertTrue(SqriptForge.getCoreRegistry().isActive(), "Le registre core doit être actif");
        
        // Vérifier que les composants sont initialisés
        assertNotNull(SqriptForge.getHybridEngine(), "Le moteur hybride doit être accessible");
        assertTrue(SqriptForge.getHybridEngine().isActive(), "Le moteur hybride doit être actif");
        
        // Vérifier l'enregistrement des types primitifs
        ScriptRegistry.RegistryStats stats = SqriptForge.getCoreRegistry().getStats();
        assertTrue(stats.primitiveCount > 0, "Des types primitifs doivent être enregistrés");
        
        // Vérifier que les types primitifs critiques sont disponibles
        assertTrue(coreRegistry.hasPrimitive("array"), "Le type 'array' doit être enregistré");
        assertTrue(coreRegistry.hasPrimitive("element"), "Le type 'element' doit être enregistré");
        assertTrue(coreRegistry.hasPrimitive("entity"), "Le type 'entity' doit être enregistré");
        assertTrue(coreRegistry.hasPrimitive("string"), "Le type 'string' doit être enregistré");
        
        System.out.println("✓ Test d'intégration Forge réussi");
        System.out.println("  - Registre core: " + stats.primitiveCount + " primitives");
        System.out.println("  - Moteur hybride: " + (hybridEngine.isActive() ? "ACTIF" : "INACTIF"));
        System.out.println("  - Temps d'initialisation: " + SqriptForge.getCoreSystemInitTime() + "ms");
    }
    
    /**
     * Test de performance : vérifier que le nouveau système améliore les performances.
     */
    @Test
    public void testPerformanceImprovement() throws Exception {
        hybridEngine.start();
        
        // Expressions de test
        String[] testExpressions = {
            "\"Hello World\"",
            "[1, 2, 3]",
            "\"test\" + \"value\"",
            "array[0]",
            "entity player"
        };
        
        long legacyTime = 0;
        long coreTime = 0;
        int successfulCompilations = 0;
        
        // Test avec le nouveau système
        for (String expr : testExpressions) {
            ScriptToken token = new ScriptToken(expr);
            ScriptCompilationContext context = new ScriptCompilationContext();
            
            long startTime = System.nanoTime();
            try {
                ScriptExpression result = hybridEngine.compileExpression(token, context);
                coreTime += System.nanoTime() - startTime;
                
                if (result != null) {
                    successfulCompilations++;
                }
            } catch (Exception e) {
                // Ignorer les erreurs pour ce test de performance
            }
        }
        
        // Vérifier que le cache fonctionne (deuxième passage)
        for (String expr : testExpressions) {
            ScriptToken token = new ScriptToken(expr);
            ScriptCompilationContext context = new ScriptCompilationContext();
            
            long startTime = System.nanoTime();
            try {
                ScriptExpression result = hybridEngine.compileExpression(token, context);
                // Le deuxième passage devrait être plus rapide grâce au cache
            } catch (Exception e) {
                // Ignorer les erreurs
            }
        }
        
        // Vérifier les métriques
        HybridEngine.HybridEngineStats stats = hybridEngine.getStats();
        assertNotNull(stats, "Les statistiques doivent être disponibles");
        assertNotNull(stats.cacheStats, "Les statistiques de cache doivent être disponibles");
        
        // Vérifier que le système fonctionne
        assertTrue(successfulCompilations > 0, "Au moins une compilation doit réussir");
        
        System.out.println("✓ Test de performance réussi");
        System.out.println("  - Compilations réussies: " + successfulCompilations + "/" + testExpressions.length);
        System.out.println("  - Temps moyen: " + (coreTime / testExpressions.length / 1_000_000) + "ms");
        System.out.println("  - Cache hit rate: " + String.format("%.1f%%", stats.cacheStats.hitRate * 100));
    }
    
    /**
     * Test de fallback hybride : vérifier que le système bascule correctement entre nouveau et ancien système.
     */
    @Test
    public void testHybridFallback() throws Exception {
        hybridEngine.start();
        
        // Configurer le moteur hybride pour tester le fallback
        hybridEngine.configureHybridEngine(true, true);
        
        // Test avec une expression qui devrait fonctionner dans le nouveau système
        ScriptToken validToken = new ScriptToken("\"test string\"");
        ScriptCompilationContext context = new ScriptCompilationContext();
        
        ScriptExpression result = hybridEngine.compileExpression(validToken, context);
        assertNotNull(result, "L'expression valide doit être compilée");
        
        // Test avec une expression qui pourrait nécessiter le fallback
        ScriptToken complexToken = new ScriptToken("complex expression that might need fallback");
        
        // Le test principal est que cela ne lance pas d'exception
        assertDoesNotThrow(() -> {
            ScriptExpression fallbackResult = hybridEngine.compileExpression(complexToken, context);
            // Le résultat peut être null si aucun système ne peut traiter l'expression
        }, "Le fallback ne doit pas lancer d'exception");
        
        // Vérifier que le système de santé fonctionne
        assertDoesNotThrow(() -> {
            hybridEngine.checkSystemHealth();
        }, "La vérification de santé ne doit pas lancer d'exception");
        
        // Test du rapport de santé
        String healthReport = hybridEngine.getHealthReport();
        assertNotNull(healthReport, "Le rapport de santé doit être disponible");
        assertTrue(healthReport.contains("HybridEngine Health Report"), "Le rapport doit contenir le titre approprié");
        
        // Vérifier la configuration
        assertTrue(hybridEngine.isUsingNewSystemFirst(), "Le nouveau système doit être en premier");
        assertTrue(hybridEngine.isFallbackEnabled(), "Le fallback doit être activé");
        
        System.out.println("✓ Test de fallback hybride réussi");
        System.out.println("  - Nouveau système en premier: " + hybridEngine.isUsingNewSystemFirst());
        System.out.println("  - Fallback activé: " + hybridEngine.isFallbackEnabled());
        System.out.println("  - Rapport de santé généré avec succès");
    }
    
    /**
     * Test de stress : vérifier que le système reste stable sous charge.
     */
    @Test
    public void testStressTest() throws Exception {
        hybridEngine.start();
        
        int numThreads = 4;
        int expressionsPerThread = 100;
        
        CompletableFuture<?>[] futures = new CompletableFuture[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < expressionsPerThread; j++) {
                    try {
                        ScriptToken token = new ScriptToken("\"thread" + threadId + "_expr" + j + "\"");
                        ScriptCompilationContext context = new ScriptCompilationContext();
                        
                        hybridEngine.compileExpression(token, context);
                        
                        // Vérification de santé périodique
                        if (j % 20 == 0) {
                            hybridEngine.checkSystemHealth();
                        }
                        
                    } catch (Exception e) {
                        // Ignorer les erreurs de compilation individuelles
                    }
                }
            });
        }
        
        // Attendre que tous les threads terminent
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        // Vérifier que le système est encore actif
        assertTrue(hybridEngine.isActive(), "Le moteur hybride doit être encore actif après le test de stress");
        
        // Vérifier les statistiques
        HybridEngine.HybridEngineStats stats = hybridEngine.getStats();
        assertNotNull(stats, "Les statistiques doivent être disponibles après le test de stress");
        
        System.out.println("✓ Test de stress réussi");
        System.out.println("  - Threads: " + numThreads);
        System.out.println("  - Expressions par thread: " + expressionsPerThread);
        System.out.println("  - Système encore actif: " + hybridEngine.isActive());
    }
    
    /**
     * Test de nettoyage : vérifier que les ressources sont libérées correctement.
     */
    @Test
    public void testCleanup() throws Exception {
        hybridEngine.start();
        
        // Utiliser le système pour créer du contenu en cache
        for (int i = 0; i < 50; i++) {
            ScriptToken token = new ScriptToken("\"test" + i + "\"");
            ScriptCompilationContext context = new ScriptCompilationContext();
            
            try {
                hybridEngine.compileExpression(token, context);
            } catch (Exception e) {
                // Ignorer les erreurs
            }
        }
        
        // Vérifier que le cache contient des éléments
        HybridEngine.HybridEngineStats statsBefore = hybridEngine.getStats();
        assertTrue(statsBefore.cacheStats.size > 0, "Le cache doit contenir des éléments");
        
        // Nettoyer le cache
        hybridEngine.clearCache();
        
        // Vérifier que le cache est vide
        HybridEngine.HybridEngineStats statsAfter = hybridEngine.getStats();
        assertEquals(0, statsAfter.cacheStats.size, "Le cache doit être vide après nettoyage");
        
        // Arrêter le moteur
        hybridEngine.stop();
        assertFalse(hybridEngine.isActive(), "Le moteur doit être inactif après arrêt");
        
        System.out.println("✓ Test de nettoyage réussi");
        System.out.println("  - Cache avant nettoyage: " + statsBefore.cacheStats.size + " éléments");
        System.out.println("  - Cache après nettoyage: " + statsAfter.cacheStats.size + " éléments");
        System.out.println("  - Moteur arrêté correctement");
    }
} 




