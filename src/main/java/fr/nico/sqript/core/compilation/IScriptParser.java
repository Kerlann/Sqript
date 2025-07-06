package fr.nico.sqript.core.compilation;

/**
 * Interface pour les parsers de scripts personnalisés
 */
public interface IScriptParser {
    /**
     * Parse un token de script
     *
     * @param token   le token à parser
     * @param context le contexte de compilation
     * @return le résultat du parsing ou null si non reconnu
     */
    Object parse(ScriptToken token, ScriptCompilationContext context) throws ScriptException;

    /**
     * Priorité du parser (plus haut = plus prioritaire)
     */
    int getPriority();

    /**
     * Nom du parser
     */
    String getName();
} 




