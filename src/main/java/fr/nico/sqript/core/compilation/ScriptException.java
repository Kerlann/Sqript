package fr.nico.sqript.core.compilation;

import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.structures.ScriptOperator;
import fr.nico.sqript.types.ScriptType;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception principale pour les erreurs de compilation de scripts.
 * Cette classe remplace l'ancienne ScriptException du package compiling.
 */
public class ScriptException extends Exception {

    protected ScriptToken line;
    protected String message;

    public ScriptException(ScriptToken line) {
        this.line = line;
        this.message = "";
    }

    public ScriptException(ScriptToken line, String message) {
        this.line = line;
        this.message = message;
    }

    public ScriptToken getLine() {
        return line;
    }

    @Override
    public String getMessage() {
        return line + " " + message;
    }

    /**
     * Exception contenant une liste d'exceptions
     */
    public static class ScriptExceptionList extends ScriptException {

        public List<Throwable> exceptionList = new ArrayList<>();

        public ScriptExceptionList() {
            super(null);
        }

        @Override
        public String getMessage() {
            StringBuilder r = new StringBuilder();
            for (Throwable exception : exceptionList) {
                if (exception instanceof ScriptException) {
                    r.append(exception.getMessage()).append("\n");
                } else {
                    r.append("Fatal exception : ").append(exception.getMessage());
                    exception.printStackTrace();
                }
            }
            return r.toString();
        }
    }

    /**
     * Exception encapsulant une autre exception
     */
    public static class ScriptWrappedException extends ScriptException {

        private final Exception wrapped;

        public ScriptWrappedException(ScriptToken line, Exception e) {
            super(line);
            wrapped = e;
        }

        public Exception getWrapped() {
            return wrapped;
        }

        @Override
        public String getMessage() {
            return line + " " + wrapped;
        }
    }

    /**
     * Exception pour les types non sauvegardables
     */
    public static class ScriptTypeNotSaveableException extends ScriptException {

        private final Class type;
        private final String key;

        public ScriptTypeNotSaveableException(String key, Class type) {
            super(null);
            this.type = type;
            this.key = key;
        }

        @Override
        public String getMessage() {
            return key + "'s type is not savable : " + type.getSimpleName();
        }
    }

    /**
     * Exception pour les interfaces non implémentées
     */
    public static class ScriptInterfaceNotImplementedException extends ScriptException {

        private final Class interfaceClass;
        private final Class given;

        public ScriptInterfaceNotImplementedException(ScriptToken line, Class interfaceClass, Class given) {
            super(line);
            this.interfaceClass = interfaceClass;
            this.given = given;
        }

        @Override
        public String getMessage() {
            return line.getText() + " of type " + given.getSimpleName() + " does not implement the features of " + interfaceClass.getSimpleName() + ". Thus this expression cannot be applied on it.";
        }
    }

    /**
     * Exception pour les expressions vides
     */
    public static class ScriptEmptyExpressionException extends ScriptException {

        public ScriptEmptyExpressionException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Empty expression at line : " + line;
        }
    }

    /**
     * Exception pour les boucles vides
     */
    public static class ScriptEmptyLoopException extends ScriptException {

        public ScriptEmptyLoopException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Empty loop at line : " + line;
        }
    }

    /**
     * Exception pour les erreurs de pattern
     */
    public static class ScriptPatternError extends Exception {

        private final String reason;

        public ScriptPatternError(String reason) {
            this.reason = reason;
        }

        @Override
        public String getMessage() {
            return reason;
        }
    }

    /**
     * Exception pour les arguments insuffisants
     */
    public static class ScriptNotEnoughArgumentException extends ScriptException {

        public ScriptNotEnoughArgumentException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Not enough arguments given to function: \n" + line;
        }
    }

    /**
     * Exception pour les événements inconnus
     */
    public static class ScriptUnknownEventException extends ScriptException {

        public ScriptUnknownEventException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Unknown trigger: \n" + line;
        }
    }

    /**
     * Exception pour les fonctions inconnues
     */
    public static class ScriptUnknownFunctionException extends ScriptException {

        public ScriptUnknownFunctionException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Not function defined as following in the given script instance: \n" + line;
        }
    }

    /**
     * Exception pour les instances inconnues
     */
    public static class ScriptUnknownInstanceException extends ScriptException {

        public ScriptUnknownInstanceException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Unknown script instance (no script file found with the given parameter): \n" + line;
        }
    }

    /**
     * Exception pour les actions inconnues
     */
    public static class ScriptUnknownActionException extends ScriptException {

        public ScriptUnknownActionException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Unknown action: \n" + line;
        }
    }

    /**
     * Exception pour les expressions inconnues
     */
    public static class ScriptUnknownExpressionException extends ScriptException {

        public ScriptUnknownExpressionException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Unknown expression: \n" + line;
        }
    }

    /**
     * Exception pour les types incompatibles
     */
    public static class ScriptTypeException extends ScriptException {

        private final Class[] wanted;
        private final Class given;

        public ScriptTypeException(ScriptToken line, Class[] wanted, Class given) {
            super(line);
            this.wanted = wanted;
            this.given = given;
        }

        @Override
        public String getMessage() {
            return "Type mismatch: expected " + java.util.Arrays.toString(wanted) + " but got " + given.getSimpleName() + " at line: " + line;
        }
    }

    /**
     * Exception pour les noms de variables invalides
     */
    public static class ScriptBadVariableNameException extends ScriptException {

        public ScriptBadVariableNameException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Bad variable name: \n" + line;
        }
    }

    /**
     * Exception pour les opérations non supportées
     */
    public static class ScriptNotOperableException extends ScriptException {

        public ScriptNotOperableException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Operation not supported: \n" + line;
        }
    }

    /**
     * Exception pour les tokens inattendus
     */
    public static class ScriptUnexpectedTokenException extends ScriptException {

        public ScriptUnexpectedTokenException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Unexpected token: \n" + line;
        }
    }

    /**
     * Exception pour les blocs inconnus
     */
    public static class ScriptUnknownBlockException extends ScriptException {

        public ScriptUnknownBlockException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Unknown block: \n" + line;
        }
    }

    /**
     * Exception pour les tokens inconnus
     */
    public static class ScriptUnknownTokenException extends ScriptException {

        public ScriptUnknownTokenException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Unknown token: \n" + line;
        }
    }

    /**
     * Exception pour les champs manquants
     */
    public static class ScriptMissingFieldException extends ScriptException {

        private final String requiredFieldName;
        private final String blockName;

        public ScriptMissingFieldException(ScriptToken line, String blockName, String requiredFieldName) {
            super(line);
            this.requiredFieldName = requiredFieldName;
            this.blockName = blockName;
        }

        @Override
        public String getMessage() {
            return "Missing required field '" + requiredFieldName + "' in block '" + blockName + "' at line: " + line;
        }
    }

    /**
     * Exception pour les tokens de fermeture manquants
     */
    public static class ScriptMissingClosingTokenException extends ScriptException {

        public ScriptMissingClosingTokenException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Missing closing token at line: " + line;
        }
    }

    /**
     * Exception pour les tokens manquants
     */
    public static class ScriptMissingTokenException extends ScriptException {

        public ScriptMissingTokenException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Missing token at line: " + line;
        }
    }

    /**
     * Exception pour les expressions non assignables
     */
    public static class ScriptNonSettableException extends ScriptException {

        private final ScriptExpression e;

        public ScriptNonSettableException(ScriptToken line, ScriptExpression e) {
            super(line);
            this.e = e;
        }

        @Override
        public String getMessage() {
            return "Expression cannot be set: " + e + " at line: " + line;
        }
    }

    /**
     * Exception pour les index hors limites
     */
    public static class ScriptIndexOutOfBoundsException extends ScriptException {

        public ScriptIndexOutOfBoundsException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Index out of bounds at line: " + line;
        }
    }

    /**
     * Exception pour les erreurs d'indentation
     */
    public static class ScriptIndentationErrorException extends ScriptException {

        public ScriptIndentationErrorException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Indentation error at line: " + line;
        }
    }

    /**
     * Exception pour les erreurs de syntaxe
     */
    public static class ScriptSyntaxException extends ScriptException {

        private final String detail;

        public ScriptSyntaxException(ScriptToken line, String detail) {
            super(line);
            this.detail = detail;
        }

        @Override
        public String getMessage() {
            return "Syntax error: " + detail + " at line: " + line;
        }
    }

    /**
     * Exception pour les références nulles
     */
    public static class ScriptNullReferenceException extends ScriptException {

        public ScriptNullReferenceException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Null reference at line: " + line;
        }
    }

    /**
     * Exception pour les références non définies
     */
    public static class ScriptUndefinedReferenceException extends ScriptException {

        public ScriptUndefinedReferenceException(ScriptToken line) {
            super(line);
        }

        @Override
        public String getMessage() {
            return "Undefined reference at line: " + line;
        }
    }

    /**
     * Exception pour les opérations non supportées
     */
    public static class ScriptOperationNotSupportedException extends ScriptException {

        private final ScriptOperator o;
        private final Class a;
        private final Class b;

        public ScriptOperationNotSupportedException(ScriptToken line, ScriptOperator o, Class<? extends ScriptType> b, Class<? extends ScriptType> a) {
            super(line);
            this.o = o;
            this.a = a;
            this.b = b;
        }

        @Override
        public String getMessage() {
            return "Operation " + o + " not supported between " + a.getSimpleName() + " and " + b.getSimpleName() + " at line: " + line;
        }
    }

    /**
     * Exception pour les erreurs de côté (client/serveur)
     */
    public static class ScriptBadSideException extends ScriptException {

        private final Side side;

        public ScriptBadSideException(ScriptToken line, Side side) {
            super(line);
            this.side = side;
        }

        @Override
        public String getMessage() {
            return "Bad side: " + side + " at line: " + line;
        }
    }
} 




