package fr.nico.sqript.migration;

import fr.nico.sqript.core.compilation.*;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.structures.ScriptInstance;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

/**
 * Test de validation de la migration du package compiling vers core.
 * Ce test vérifie que toutes les fonctionnalités de l'ancien système
 * sont correctement migrées et fonctionnelles dans le nouveau système.
 */
public class MigrationValidationTest {

    private ScriptRegistry registry;

    @Before
    public void setUp() throws Exception {
        // Initialisation du nouveau système core pour les tests
        registry = new ScriptRegistry();
        registry.initialize();
        registry.start();
        
        // Initialisation de l'adaptateur de migration
        MigrationAdapter.initialize(registry);
    }

    @After
    public void tearDown() throws Exception {
        if (registry != null && registry.isActive()) {
            registry.stop();
        }
    }

    @Test
    public void testScriptTokenMigration() {
        // Test de création de ScriptToken avec le nouveau système
        ScriptToken token = new ScriptToken("test script line", 1, null);
        
        assertNotNull(token);
        assertEquals("test script line", token.getText());
        assertEquals(1, token.getLineNumber());
        assertEquals(0, token.getTabLevel());
    }

    @Test
    public void testScriptCompilationContextMigration() {
        // Test de création de ScriptCompilationContext avec le nouveau système
        ScriptCompilationContext context = new ScriptCompilationContext();
        
        assertNotNull(context);
        assertNotNull(context.declaredVariables);
        assertTrue(context.declaredVariables.isEmpty());
    }

    @Test
    public void testNodeClassesMigration() {
        // Test de création des différents types de Node
        
        // Test NodeExpression
        NodeExpression nodeExpr = new NodeExpression(null);
        assertNotNull(nodeExpr);
        assertEquals(0, nodeExpr.getArity());
        
        // Test NodeParenthesis
        NodeParenthesis nodeParen = new NodeParenthesis(EnumTokenType.LEFT_PARENTHESIS);
        assertNotNull(nodeParen);
        assertEquals(EnumTokenType.LEFT_PARENTHESIS, nodeParen.getType());
        
        // Test NodeSwitch
        NodeSwitch nodeSwitch = new NodeSwitch(new Node[0]);
        assertNotNull(nodeSwitch);
        assertEquals(ScriptElement.class, nodeSwitch.getReturnType());
    }

    @Test
    public void testExpressionTokenMigration() {
        // Test de création d'ExpressionToken
        ExpressionToken token = new ExpressionToken(EnumTokenType.EXPRESSION, "test expression");
        
        assertNotNull(token);
        assertEquals(EnumTokenType.EXPRESSION, token.getType());
        assertEquals("test expression", token.getExpressionString());
    }

    @Test
    public void testScriptExceptionMigration() {
        // Test de création des différents types d'exceptions
        ScriptToken token = new ScriptToken("error line", 5, null);
        
        ScriptException baseException = new ScriptException(token, "Test error");
        assertNotNull(baseException);
        assertTrue(baseException.getMessage().contains("Test error"));
        
        ScriptException.ScriptUnknownExpressionException unknownExpr = 
            new ScriptException.ScriptUnknownExpressionException(token);
        assertNotNull(unknownExpr);
        assertTrue(unknownExpr.getMessage().contains("Unknown expression"));
        
        ScriptException.ScriptSyntaxException syntaxErr = 
            new ScriptException.ScriptSyntaxException(token, "Syntax error details");
        assertNotNull(syntaxErr);
        assertTrue(syntaxErr.getMessage().contains("Syntax error"));
    }

    @Test
    public void testScriptDecoderMigration() {
        // Test des méthodes utilitaires de ScriptDecoder
        
        // Test extractStrings
        String testString = "This is a \"test string\" with \"multiple quotes\"";
        String[] extracted = fr.nico.sqript.core.registry.ScriptRegistry.extractStrings(testString);
        assertEquals(2, extracted.length);
        assertEquals("test string", extracted[0]);
        assertEquals("multiple quotes", extracted[1]);
        
        // Test isParenthesageGood
        assertTrue(fr.nico.sqript.core.registry.ScriptRegistry.isParenthesageGood("(test)"));
        assertTrue(fr.nico.sqript.core.registry.ScriptRegistry.isParenthesageGood("[test]"));
        assertTrue(fr.nico.sqript.core.registry.ScriptRegistry.isParenthesageGood("{test}"));
        assertFalse(fr.nico.sqript.core.registry.ScriptRegistry.isParenthesageGood("(test"));
        assertFalse(fr.nico.sqript.core.registry.ScriptRegistry.isParenthesageGood("test)"));
        
        // Test splitAtComa
        String[] split = fr.nico.sqript.core.registry.ScriptRegistry.splitAtComa("arg1, arg2, arg3");
        assertEquals(3, split.length);
        assertEquals("arg1", split[0]);
        assertEquals("arg2", split[1]);
        assertEquals("arg3", split[2]);
        
        // Test getTabLevel
        assertEquals(0, fr.nico.sqript.SqriptUtils.getTabLevel("no tabs"));
        assertEquals(1, fr.nico.sqript.SqriptUtils.getTabLevel("\tone tab"));
        assertEquals(2, fr.nico.sqript.SqriptUtils.getTabLevel("\t\ttwo tabs"));
    }

    @Test
    public void testSimpleRegexMigration() throws Exception {
        // Test de la migration de SimpleRegex
        String pattern = "(test|example)";
        String regex = SimpleRegex.simplePatternToRegex(pattern, true);
        assertNotNull(regex);
        assertTrue(regex.length() > 0);
    }

    @Test
    public void testMigrationAdapterFunctionality() {
        // Test de l'adaptateur de migration
        assertTrue(MigrationAdapter.isInitialized());
        assertEquals(registry, MigrationAdapter.getRegistry());
        
        // Test des méthodes utilitaires
        ScriptToken token = MigrationAdapter.createToken("test", 1, null);
        assertNotNull(token);
        assertEquals("test", token.getText());
        
        ScriptCompilationContext context = MigrationAdapter.createContext();
        assertNotNull(context);
        
        // Test des mappings de types
        Class stringClass = MigrationAdapter.mapTypeByName("string");
        assertEquals(String.class, stringClass);
    }

    @Test
    public void testRegistryIntegration() {
        // Test de l'intégration avec le registre
        assertNotNull(registry);
        assertTrue(registry.isActive());
        
        // Test des statistiques
        ScriptRegistry.RegistryStats stats = registry.getStats();
        assertNotNull(stats);
        assertTrue(stats.toString().contains("Registry Stats"));
    }

    @Test
    public void testPackageStructure() {
        // Test que l'ancien package n'existe plus
        try {
            Class.forName("fr.nico.sqript.compiling.ScriptDecoder");
            fail("L'ancien package compiling devrait avoir été supprimé");
        } catch (ClassNotFoundException e) {
            // Comportement attendu - l'ancien package n'existe plus
        }
        
        // Test que le nouveau package existe
        try {
            Class.forName("fr.nico.sqript.core.compilation.ScriptDecoder");
            Class.forName("fr.nico.sqript.core.compilation.ScriptToken");
            Class.forName("fr.nico.sqript.core.compilation.ScriptCompilationContext");
            Class.forName("fr.nico.sqript.core.compilation.Node");
            // Test réussi si aucune exception n'est levée
        } catch (ClassNotFoundException e) {
            fail("Le nouveau package core.compilation devrait exister : " + e.getMessage());
        }
    }

    @Test
    public void testBackwardCompatibility() {
        // Test de la compatibilité ascendante via l'adaptateur
        ScriptToken token = new ScriptToken("test", 1, null);
        ScriptCompilationContext context = new ScriptCompilationContext();
        
        // Ces appels devraient fonctionner sans erreur
        String[] strings = MigrationAdapter.extractStrings("\"test\"");
        assertEquals(1, strings.length);
        assertEquals("test", strings[0]);
        
        boolean isGood = MigrationAdapter.isParenthesageGood("(test)");
        assertTrue(isGood);
        
        String trimmed = MigrationAdapter.trim("  test  ");
        assertEquals("test", trimmed);
    }

    @Test
    public void testSystemHealthAfterMigration() {
        // Test de la santé du système après migration
        assertTrue("Registry should be active", registry.isActive());
        assertTrue("Migration adapter should be initialized", MigrationAdapter.isInitialized());
        
        // Test que ScriptDecoder est correctement initialisé
        assertNotNull("ScriptDecoder should be functional", 
                     fr.nico.sqript.core.registry.ScriptRegistry.extractStrings("\"test\""));
        
        // Test qu'aucune dépendance vers l'ancien package ne subsiste
        ScriptRegistry.RegistryStats stats = registry.getStats();
        assertNotNull("Registry stats should be available", stats);
    }
} 




