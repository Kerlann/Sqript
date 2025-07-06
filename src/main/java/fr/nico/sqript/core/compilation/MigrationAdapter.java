package fr.nico.sqript.core.compilation;

import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.structures.ScriptElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Adaptateur pour la migration du package compiling vers le système core.
 * Cette classe fournit des méthodes de compatibilité pour permettre aux
 * actions et expressions existantes de fonctionner avec le nouveau système.
 */
public class MigrationAdapter {

    private static final Map<String, Class> TYPE_MAPPING = new HashMap<>();
    private static ScriptRegistry registry;

    static {
        // Initialisation des mappings de types
        initializeTypeMappings();
    }

    /**
     * Initialise l'adaptateur avec le registre du nouveau système
     */
    public static void initialize(ScriptRegistry scriptRegistry) {
        registry = scriptRegistry;
        // ScriptDecoder supprimé - fonctionnalités migrées vers ScriptRegistry
    }

    /**
     * Adapte l'ancien ScriptDecoder.parse vers le nouveau système
     */
    public static ScriptExpression parseExpression(ScriptToken token, ScriptCompilationContext context) throws ScriptException {
        try {
            CompilationResult<ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(token, context);
            if (result != null && result.isSuccess()) {
                return result.getResult();
            } else {
                throw new ScriptException(token, "Erreur de compilation d'expression dans MigrationAdapter");
            }
        } catch (Exception e) {
            throw new ScriptException(token, "Erreur de compilation d'expression dans MigrationAdapter : " + e.getMessage());
        }
    }

    /**
     * Adapte l'ancien ScriptDecoder.parse avec types requis vers le nouveau système
     */
    public static ScriptExpression parseExpression(ScriptToken token, ScriptCompilationContext context, Class[] requiredTypes) throws ScriptException {
        try {
            CompilationResult<ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(token, context, requiredTypes);
            if (result != null && result.isSuccess()) {
                return result.getResult();
            } else {
                throw new ScriptException(token, "Erreur de compilation d'expression dans MigrationAdapter");
            }
        } catch (Exception e) {
            throw new ScriptException(token, "Erreur de compilation d'expression dans MigrationAdapter : " + e.getMessage());
        }
    }

    /**
     * Adapte l'ancien ScriptDecoder.parseExpressionTree vers le nouveau système
     */
    public static Node parseExpressionTree(ScriptToken token, ScriptCompilationContext context, Class[] requiredTypes) throws ScriptException {
        return registry.parseExpressionTree(token, context, requiredTypes);
    }

    /**
     * Adapte l'ancien ScriptDecoder.splitAtComa vers le nouveau système
     */
    public static String[] splitAtComa(String expression) {
        return fr.nico.sqript.core.registry.ScriptRegistry.splitAtComa(expression);
    }

    /**
     * Adapte l'ancien ScriptDecoder.extractStrings vers le nouveau système
     */
    public static String[] extractStrings(String parameter) {
        return fr.nico.sqript.core.registry.ScriptRegistry.extractStrings(parameter);
    }

    /**
     * Adapte l'ancien ScriptDecoder.replaceStrings vers le nouveau système
     */
    public static String replaceStrings(String parameter, String[] strings) {
        return fr.nico.sqript.core.registry.ScriptRegistry.replaceStrings(parameter, strings);
    }

    /**
     * Adapte l'ancien ScriptDecoder.removeStrings vers le nouveau système
     */
    public static String removeStrings(String parameter, String[] strings) {
        return fr.nico.sqript.core.registry.ScriptRegistry.removeStrings(parameter, strings);
    }

    /**
     * Adapte l'ancien ScriptDecoder.isParenthesageGood vers le nouveau système
     */
    public static boolean isParenthesageGood(String line) {
        return fr.nico.sqript.core.registry.ScriptRegistry.isParenthesageGood(line);
    }

    /**
     * Adapte l'ancien ScriptDecoder.isOperator vers le nouveau système
     */
    public static boolean isOperator(String operator) {
        return registry.findOperator(operator) != null;
    }

    /**
     * Adapte l'ancien ScriptDecoder.isAlgebraic vers le nouveau système
     */
    public static boolean isAlgebraic(String expression) {
        return fr.nico.sqript.core.registry.ScriptRegistry.isAlgebraic(expression);
    }

    /**
     * Adapte l'ancien ScriptDecoder.containsOperator vers le nouveau système
     */
    public static boolean containsOperator(String expression) {
        return ScriptRegistry.containsOperator(expression);
    }

    /**
     * Adapte l'ancien ScriptDecoder.trim vers le nouveau système
     */
    public static String trim(String string) {
        return fr.nico.sqript.core.registry.ScriptRegistry.trim(string);
    }

    /**
     * Adapte l'ancien ScriptDecoder.checkIfVariable vers le nouveau système
     */
    public static boolean checkIfVariable(ScriptToken token) {
        return fr.nico.sqript.core.registry.ScriptRegistry.checkIfVariable(token);
    }

    /**
     * Adapte l'ancien ScriptDecoder.parseType vers le nouveau système
     */
    public static Class<? extends ScriptElement> parseType(String type) {
        return ScriptRegistry.parseType(type);
    }

    /**
     * Adapte l'ancien ScriptDecoder.getTabLevel vers le nouveau système
     */
    public static int getTabLevel(String line) {
        return fr.nico.sqript.SqriptUtils.getTabLevel(line);
    }

    /**
     * Adapte l'ancien ScriptDecoder.getNameOfType vers le nouveau système
     */
    public static String getNameOfType(Class type) {
        return fr.nico.sqript.SqriptUtils.getNameOfType(type);
    }

    /**
     * Adapte l'ancien ScriptDecoder.replaceLast vers le nouveau système
     */
    public static String replaceLast(String text, String regex, String replacement) {
        return fr.nico.sqript.SqriptUtils.replaceLast(text, regex, replacement);
    }

    /**
     * Adapte l'ancien ScriptDecoder.getUncommentedPart vers le nouveau système
     */
    public static String getUncommentedPart(String text) {
        return fr.nico.sqript.SqriptUtils.getUncommentedPart(text);
    }


    /**
     * Adapte l'ancien SimpleRegex.simplePatternToRegex vers le nouveau système
     */
    public static String simplePatternToRegex(String simplePattern, boolean canBeOptional) throws Exception {
        return SimpleRegex.simplePatternToRegex(simplePattern, canBeOptional);
    }

    /**
     * Créé un nouveau ScriptToken pour compatibilité
     */
    public static ScriptToken createToken(String text, int lineNumber, fr.nico.sqript.structures.ScriptInstance instance) {
        return new ScriptToken(text, lineNumber, instance);
    }

    /**
     * Créé un nouveau ScriptCompilationContext pour compatibilité
     */
    public static ScriptCompilationContext createContext() {
        return new ScriptCompilationContext();
    }

    /**
     * Créé un nouveau ScriptCompilationContext avec parent pour compatibilité
     */
    public static ScriptCompilationContext createContext(ScriptCompilationContext parent) {
        return new ScriptCompilationContext(parent);
    }

    /**
     * Initialise les mappings de types pour la migration
     */
    private static void initializeTypeMappings() {
        TYPE_MAPPING.put("string", String.class);
        TYPE_MAPPING.put("number", Number.class);
        TYPE_MAPPING.put("boolean", Boolean.class);
        TYPE_MAPPING.put("object", Object.class);
        // Ajouter d'autres mappings selon les besoins
    }

    /**
     * Mappe un nom de type vers la classe correspondante
     */
    public static Class mapTypeByName(String typeName) {
        return TYPE_MAPPING.getOrDefault(typeName.toLowerCase(), Object.class);
    }

    /**
     * Vérifie si l'adaptateur est correctement initialisé
     */
    public static boolean isInitialized() {
        return registry != null;
    }

    /**
     * Obtient le registre associé à cet adaptateur
     */
    public static ScriptRegistry getRegistry() {
        return registry;
    }

    /**
     * Méthode utilitaire pour créer des exceptions de compilation
     */
    public static ScriptException createException(ScriptToken token, String message) {
        return new ScriptException(token, message);
    }

    /**
     * Méthode utilitaire pour créer des exceptions d'expression inconnue
     */
    public static ScriptException.ScriptUnknownExpressionException createUnknownExpressionException(ScriptToken token) {
        return new ScriptException.ScriptUnknownExpressionException(token);
    }

    /**
     * Méthode utilitaire pour créer des exceptions d'action inconnue
     */
    public static ScriptException.ScriptUnknownActionException createUnknownActionException(ScriptToken token) {
        return new ScriptException.ScriptUnknownActionException(token);
    }

    /**
     * Adapte l'ancien ScriptDecoder.splitAtDoubleDot vers le nouveau système
     */
    public static String[] splitAtDoubleDot(String s) {
        return fr.nico.sqript.core.registry.ScriptRegistry.splitAtDoubleDot(s);
    }
} 




