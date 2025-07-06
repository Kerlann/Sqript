package fr.nico.sqript.core.compilation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Validateur syntaxique rapide pour les tokens de script.
 * Effectue des validations de base avant la compilation complète.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class SyntaxValidator {

    private static final Logger log = LogManager.getLogger(SyntaxValidator.class);

    // Statistiques
    private final AtomicLong validationCount = new AtomicLong(0);
    private final AtomicLong validationErrors = new AtomicLong(0);

    // État
    private volatile boolean initialized = false;

    /**
     * Initialise le validateur syntaxique.
     */
    public void initialize() {
        if (initialized) {
            log.warn("SyntaxValidator is already initialized");
            return;
        }

        initialized = true;
        log.info("SyntaxValidator initialized successfully");
    }

    /**
     * Valide la syntaxe d'un token de script.
     *
     * @param token Le token à valider
     * @return Le résultat de la validation
     */
    public ValidationResult validate(ScriptToken token) {
        checkInitialized();

        if (token == null) {
            return ValidationResult.invalid("Token is null");
        }

        validationCount.incrementAndGet();

        String text = token.getText();
        if (text == null) {
            validationErrors.incrementAndGet();
            return ValidationResult.invalid("Token text is null");
        }

        text = text.trim();
        if (text.isEmpty()) {
            validationErrors.incrementAndGet();
            return ValidationResult.invalid("Token is empty", 0, ValidationResult.ValidationError.Type.EMPTY_EXPRESSION);
        }

        // Validation des délimiteurs
        ValidationResult delimiterResult = validateDelimiters(text);
        if (!delimiterResult.isValid()) {
            validationErrors.incrementAndGet();
            return delimiterResult;
        }

        // Validation des guillemets
        ValidationResult quotesResult = validateQuotes(text);
        if (!quotesResult.isValid()) {
            validationErrors.incrementAndGet();
            return quotesResult;
        }

        // Validation des caractères
        ValidationResult charactersResult = validateCharacters(text);
        if (!charactersResult.isValid()) {
            validationErrors.incrementAndGet();
            return charactersResult;
        }

        // Validation de la structure de base
        ValidationResult structureResult = validateBasicStructure(text);
        if (!structureResult.isValid()) {
            validationErrors.incrementAndGet();
            return structureResult;
        }

        return ValidationResult.valid();
    }

    /**
     * Valide l'équilibre des délimiteurs (parenthèses, crochets, accolades).
     */
    public ValidationResult validateDelimiters(String text) {
        if (text == null || text.isEmpty()) {
            return ValidationResult.valid();
        }

        DelimiterTracker tracker = new DelimiterTracker();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (tracker.isEscaping()) {
                tracker.clearEscaping();
                continue;
            }

            if (c == '\\') {
                tracker.setEscaping();
                continue;
            }

            if (c == '"') {
                tracker.toggleStringMode();
                continue;
            }

            if (tracker.isInString()) {
                continue; // Ignorer les délimiteurs dans les chaînes
            }

            switch (c) {
                case '(':
                    tracker.openParenthesis();
                    break;
                case ')':
                    if (!tracker.closeParenthesis()) {
                        return ValidationResult.invalid(
                                ValidationResult.ValidationError.UNBALANCED_PARENTHESES,
                                i,
                                ValidationResult.ValidationError.Type.PARENTHESES
                        );
                    }
                    break;
                case '[':
                    tracker.openBracket();
                    break;
                case ']':
                    if (!tracker.closeBracket()) {
                        return ValidationResult.invalid(
                                ValidationResult.ValidationError.UNBALANCED_BRACKETS,
                                i,
                                ValidationResult.ValidationError.Type.BRACKETS
                        );
                    }
                    break;
                case '{':
                    tracker.openBrace();
                    break;
                case '}':
                    if (!tracker.closeBrace()) {
                        return ValidationResult.invalid(
                                ValidationResult.ValidationError.UNBALANCED_BRACES,
                                i,
                                ValidationResult.ValidationError.Type.BRACES
                        );
                    }
                    break;
            }
        }

        // Vérifier l'équilibre final
        if (tracker.hasOpenParentheses()) {
            return ValidationResult.invalid(
                    ValidationResult.ValidationError.UNBALANCED_PARENTHESES,
                    -1,
                    ValidationResult.ValidationError.Type.PARENTHESES
            );
        }

        if (tracker.hasOpenBrackets()) {
            return ValidationResult.invalid(
                    ValidationResult.ValidationError.UNBALANCED_BRACKETS,
                    -1,
                    ValidationResult.ValidationError.Type.BRACKETS
            );
        }

        if (tracker.hasOpenBraces()) {
            return ValidationResult.invalid(
                    ValidationResult.ValidationError.UNBALANCED_BRACES,
                    -1,
                    ValidationResult.ValidationError.Type.BRACES
            );
        }

        return ValidationResult.valid();
    }

    /**
     * Valide les guillemets (qu'ils soient bien fermés).
     */
    public ValidationResult validateQuotes(String text) {
        if (text == null || text.isEmpty()) {
            return ValidationResult.valid();
        }

        boolean inString = false;
        boolean escaping = false;
        int lastQuotePosition = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                lastQuotePosition = i;
            }
        }

        if (inString) {
            return ValidationResult.invalid(
                    ValidationResult.ValidationError.UNCLOSED_QUOTES,
                    lastQuotePosition,
                    ValidationResult.ValidationError.Type.QUOTES
            );
        }

        return ValidationResult.valid();
    }

    /**
     * Valide les caractères (pas de caractères de contrôle interdits).
     */
    public ValidationResult validateCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return ValidationResult.valid();
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Vérifier les caractères de contrôle interdits
            if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') {
                return ValidationResult.invalid(
                        ValidationResult.ValidationError.INVALID_CHARACTER + ": " + (int) c,
                        i,
                        ValidationResult.ValidationError.Type.INVALID_CHARACTER
                );
            }

            // Vérifier les caractères Unicode problématiques
            if (Character.getType(c) == Character.UNASSIGNED) {
                return ValidationResult.invalid(
                        ValidationResult.ValidationError.INVALID_CHARACTER + ": unassigned Unicode",
                        i,
                        ValidationResult.ValidationError.Type.INVALID_CHARACTER
                );
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Valide la structure de base (pas d'opérateurs isolés, etc.).
     */
    public ValidationResult validateBasicStructure(String text) {
        if (text == null || text.isEmpty()) {
            return ValidationResult.valid();
        }

        // Vérifier qu'il n'y a pas d'opérateurs en début/fin sans contexte
        String trimmed = text.trim();

        // Opérateurs qui ne peuvent pas être en début
        if (trimmed.startsWith("&&") || trimmed.startsWith("||") ||
                trimmed.startsWith("==") || trimmed.startsWith("!=")) {
            return ValidationResult.invalid(
                    "Opérateur en début d'expression",
                    0,
                    ValidationResult.ValidationError.Type.MISSING_OPERATOR
            );
        }

        // Opérateurs qui ne peuvent pas être en fin  
        if (trimmed.endsWith("&&") || trimmed.endsWith("||") ||
                trimmed.endsWith("==") || trimmed.endsWith("!=") ||
                trimmed.endsWith("+") || trimmed.endsWith("-") ||
                trimmed.endsWith("*") || trimmed.endsWith("/")) {
            return ValidationResult.invalid(
                    "Opérateur en fin d'expression",
                    trimmed.length() - 1,
                    ValidationResult.ValidationError.Type.MISSING_OPERATOR
            );
        }

        return ValidationResult.valid();
    }

    /**
     * Validation rapide pour les cas simples (optimisation).
     */
    public boolean isValidFast(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // Validation rapide des délimiteurs seulement
        int parentheses = 0;
        int brackets = 0;
        int braces = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
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

                if (parentheses < 0 || brackets < 0 || braces < 0) {
                    return false;
                }
            }
        }

        return !inString && parentheses == 0 && brackets == 0 && braces == 0;
    }

    /**
     * Retourne les statistiques de validation.
     */
    public ValidationStats getStats() {
        long total = validationCount.get();
        long errors = validationErrors.get();
        double errorRate = total > 0 ? (double) errors / total : 0.0;

        return new ValidationStats(total, errors, errorRate);
    }

    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SyntaxValidator is not initialized");
        }
    }

    /**
     * Classe utilitaire pour suivre l'état des délimiteurs.
     */
    private static class DelimiterTracker {
        private int parentheses = 0;
        private int brackets = 0;
        private int braces = 0;
        private boolean inString = false;
        private boolean escaping = false;

        public boolean isEscaping() {
            return escaping;
        }

        public void setEscaping() {
            escaping = true;
        }

        public void clearEscaping() {
            escaping = false;
        }

        public boolean isInString() {
            return inString;
        }

        public void toggleStringMode() {
            inString = !inString;
        }

        public void openParenthesis() {
            parentheses++;
        }

        public boolean closeParenthesis() {
            return --parentheses >= 0;
        }

        public boolean hasOpenParentheses() {
            return parentheses > 0;
        }

        public void openBracket() {
            brackets++;
        }

        public boolean closeBracket() {
            return --brackets >= 0;
        }

        public boolean hasOpenBrackets() {
            return brackets > 0;
        }

        public void openBrace() {
            braces++;
        }

        public boolean closeBrace() {
            return --braces >= 0;
        }

        public boolean hasOpenBraces() {
            return braces > 0;
        }
    }

    /**
     * Statistiques de validation.
     */
    public static class ValidationStats {
        public final long totalValidations;
        public final long validationErrors;
        public final double errorRate;

        public ValidationStats(long totalValidations, long validationErrors, double errorRate) {
            this.totalValidations = totalValidations;
            this.validationErrors = validationErrors;
            this.errorRate = errorRate;
        }

        @Override
        public String toString() {
            return String.format("ValidationStats{total=%d, errors=%d, errorRate=%.2f%%}",
                    totalValidations, validationErrors, errorRate * 100);
        }
    }
}





