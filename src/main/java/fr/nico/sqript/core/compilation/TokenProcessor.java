package fr.nico.sqript.core.compilation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Processeur optimisé pour les tokens de script.
 * Gère le parsing, la validation et la transformation des tokens.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class TokenProcessor {

    private static final Logger log = LogManager.getLogger(TokenProcessor.class);

    private final CompilationCache cache;
    private final AtomicLong processedCount = new AtomicLong(0);
    private volatile boolean initialized = false;

    public TokenProcessor(CompilationCache cache) {
        this.cache = cache;
    }

    /**
     * Initialise le processeur de tokens.
     */
    public void initialize() {
        if (initialized) {
            log.warn("TokenProcessor is already initialized");
            return;
        }

        initialized = true;
        log.info("TokenProcessor initialized successfully");
    }

    /**
     * Traite un token en supprimant les espaces inutiles et en normalisant le contenu.
     */
    public ScriptToken processToken(ScriptToken token) {
        checkInitialized();

        if (token == null || token.getText() == null) {
            return token;
        }

        processedCount.incrementAndGet();

        String text = token.getText();

        // Supprimer les espaces en début et fin
        text = text.trim();

        // Normaliser les espaces multiples
        text = text.replaceAll("\\s+", " ");

        // Retourner un nouveau token avec le texte normalisé
        return token.with(text);
    }

    /**
     * Divise une chaîne à chaque virgule en respectant les parenthèses.
     */
    public String[] splitAtComma(String expression) {
        checkInitialized();

        if (expression == null || expression.trim().isEmpty()) {
            return new String[0];
        }

        return SplitUtils.splitAtComma(expression.trim());
    }

    /**
     * Supprime les parenthèses extérieures inutiles d'une chaîne.
     */
    public String trimParentheses(String text) {
        checkInitialized();

        if (text == null) {
            return null;
        }

        return SplitUtils.trimParentheses(text);
    }

    /**
     * Vérifie si le parenthésage d'une ligne est correct.
     */
    public boolean isParenthesesBalanced(String line) {
        checkInitialized();

        if (line == null) {
            return true;
        }

        int parentheses = 0;
        int brackets = 0;
        int braces = 0;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            switch (c) {
                case '(':
                    parentheses++;
                    break;
                case ')':
                    parentheses--;
                    break;
                case '[':
                    brackets++;
                    break;
                case ']':
                    brackets--;
                    break;
                case '{':
                    braces++;
                    break;
                case '}':
                    braces--;
                    break;
            }

            // Vérifier qu'on ne devient jamais négatif
            if (parentheses < 0 || brackets < 0 || braces < 0) {
                return false;
            }
        }

        return parentheses == 0 && brackets == 0 && braces == 0;
    }

    /**
     * Extrait les chaînes de caractères d'une expression.
     */
    public String[] extractStrings(String expression) {
        checkInitialized();

        return StringUtils.extractStrings(expression);
    }

    /**
     * Remplace les marqueurs de chaînes par les chaînes originales.
     */
    public String replaceStringMarkers(String expression, String[] strings) {
        checkInitialized();

        return StringUtils.replaceStringMarkers(expression, strings);
    }

    /**
     * Supprime les chaînes d'une expression et les remplace par des marqueurs.
     */
    public String removeStrings(String expression, String[] strings) {
        checkInitialized();

        return StringUtils.removeStrings(expression, strings);
    }

    /**
     * Calcule le niveau d'indentation d'une ligne.
     */
    public int getIndentationLevel(String line) {
        checkInitialized();

        if (line == null) {
            return 0;
        }

        int indentation = 0;
        int charIndex = 0;

        while (charIndex < line.length()) {
            char c = line.charAt(charIndex);
            if (c == '\t') {
                indentation += 4;
            } else if (c == ' ') {
                indentation++;
            } else {
                break;
            }
            charIndex++;
        }

        return indentation / 4; // Convertir en niveau d'indentation
    }

    /**
     * Supprime les commentaires d'une ligne.
     */
    public String removeComments(String line) {
        checkInitialized();

        if (line == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escaping) {
                result.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                result.append(c);
                continue;
            }

            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }

            if (c == '#' && !inString) {
                // Début d'un commentaire, on s'arrête ici
                break;
            }

            result.append(c);
        }

        return result.toString();
    }

    /**
     * Vérifie si un token représente une variable.
     */
    public boolean isVariable(ScriptToken token) {
        checkInitialized();

        if (token == null || token.getText() == null) {
            return false;
        }

        String text = token.getText().trim();

        // Vérifie le format ${...} ou {....}
        if (!text.startsWith("{") || !text.endsWith("}")) {
            return false;
        }

        // Vérifie qu'il n'y a que des caractères valides entre les accolades
        String content = text.substring(1, text.length() - 1);
        return !content.trim().isEmpty();
    }

    /**
     * Retourne le nombre de tokens traités.
     */
    public long getProcessedCount() {
        return processedCount.get();
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("TokenProcessor is not initialized");
        }
    }
}





