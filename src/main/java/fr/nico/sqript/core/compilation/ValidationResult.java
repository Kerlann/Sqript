package fr.nico.sqript.core.compilation;

/**
 * Résultat d'une validation syntaxique.
 * Encapsule le succès/échec et les détails de l'erreur.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class ValidationResult {

    private final boolean valid;
    private final String error;
    private final int errorPosition;
    private final ValidationError.Type errorType;

    private ValidationResult(boolean valid, String error, int errorPosition, ValidationError.Type errorType) {
        this.valid = valid;
        this.error = error;
        this.errorPosition = errorPosition;
        this.errorType = errorType;
    }

    /**
     * Crée un résultat de validation réussi.
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null, -1, null);
    }

    /**
     * Crée un résultat de validation échoué.
     */
    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, error, -1, ValidationError.Type.SYNTAX);
    }

    /**
     * Crée un résultat de validation échoué avec position.
     */
    public static ValidationResult invalid(String error, int position) {
        return new ValidationResult(false, error, position, ValidationError.Type.SYNTAX);
    }

    /**
     * Crée un résultat de validation échoué avec type d'erreur.
     */
    public static ValidationResult invalid(String error, int position, ValidationError.Type type) {
        return new ValidationResult(false, error, position, type);
    }

    /**
     * Indique si la validation a réussi.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Retourne le message d'erreur (null si validation réussie).
     */
    public String getError() {
        return error;
    }

    /**
     * Retourne la position de l'erreur (-1 si pas de position ou validation réussie).
     */
    public int getErrorPosition() {
        return errorPosition;
    }

    /**
     * Retourne le type d'erreur (null si validation réussie).
     */
    public ValidationError.Type getErrorType() {
        return errorType;
    }

    /**
     * Combine ce résultat avec un autre.
     * Retourne le premier résultat invalide ou valid() si tous sont valides.
     */
    public ValidationResult combine(ValidationResult other) {
        if (!this.valid) {
            return this;
        }
        return other;
    }

    /**
     * Transforme ce résultat en CompilationResult.
     */
    public <T> CompilationResult<T> toCompilationResult() {
        if (valid) {
            return CompilationResult.success(null, 0, false);
        } else {
            return CompilationResult.failure(new ValidationException(this));
        }
    }

    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        } else {
            return String.format("ValidationResult{valid=false, error='%s', position=%d, type=%s}",
                    error, errorPosition, errorType);
        }
    }

    /**
     * Exception encapsulant un résultat de validation invalide.
     */
    public static class ValidationException extends RuntimeException {
        private final ValidationResult result;

        public ValidationException(ValidationResult result) {
            super(result.getError());
            this.result = result;
        }

        public ValidationResult getValidationResult() {
            return result;
        }
    }

    /**
     * Classe utilitaire pour les erreurs de validation.
     */
    public static class ValidationError {
        public static final String UNBALANCED_PARENTHESES = "Parenthèses non équilibrées";
        public static final String UNCLOSED_QUOTES = "Guillemets non fermés";
        public static final String UNBALANCED_BRACKETS = "Crochets non équilibrés";
        public static final String UNBALANCED_BRACES = "Accolades non équilibrées";
        public static final String EMPTY_EXPRESSION = "Expression vide";
        public static final String INVALID_CHARACTER = "Caractère invalide";
        public static final String MISSING_OPERATOR = "Opérateur manquant";
        public static final String INVALID_PATTERN = "Pattern invalide";
        public enum Type {
            SYNTAX,           // Erreur de syntaxe générale
            PARENTHESES,      // Parenthèses non équilibrées
            QUOTES,           // Guillemets non fermés
            BRACKETS,         // Crochets non équilibrés
            BRACES,           // Accolades non équilibrées
            EMPTY_EXPRESSION, // Expression vide
            INVALID_CHARACTER,// Caractère invalide
            MISSING_OPERATOR, // Opérateur manquant
            INVALID_PATTERN   // Pattern invalide
        }
    }
}





