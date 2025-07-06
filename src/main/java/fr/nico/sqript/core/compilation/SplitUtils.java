package fr.nico.sqript.core.compilation;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaires pour diviser et analyser les expressions de script.
 * Optimisé pour gérer correctement les parenthèses, crochets et guillemets.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public final class SplitUtils {

    private SplitUtils() {
        // Classe utilitaire - pas d'instanciation
    }

    /**
     * Divise une chaîne à chaque virgule en respectant les parenthèses et guillemets.
     * Exemple : "cos(5,4), max(3,4), 18" → ["cos(5,4)", "max(3,4)", "18"]
     *
     * @param expression La chaîne à diviser
     * @return Un tableau des parties divisées
     */
    public static String[] splitAtComma(String expression) {
        if (expression == null) {
            return new String[0];
        }

        expression = expression.trim();
        if (expression.isEmpty()) {
            return new String[0];
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        ParenthesesTracker tracker = new ParenthesesTracker();
        int charIndex = 0;

        while (charIndex < expression.length()) {
            char c = expression.charAt(charIndex);

            // Gérer les caractères d'échappement
            if (c == '\\' && charIndex + 1 < expression.length()) {
                current.append(c);
                current.append(expression.charAt(charIndex + 1));
                charIndex += 2;
                continue;
            }

            tracker.processChar(c);

            if (c == ',' && tracker.isAtTopLevel()) {
                // Virgule au niveau supérieur - diviser ici
                String part = current.toString().trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }

            charIndex++;
        }

        // Ajouter la dernière partie
        String lastPart = current.toString().trim();
        if (!lastPart.isEmpty()) {
            parts.add(lastPart);
        }

        return parts.toArray(new String[0]);
    }

    /**
     * Supprime les parenthèses extérieures inutiles d'une chaîne.
     * Exemple : "((expression))" → "expression"
     *
     * @param text La chaîne à traiter
     * @return La chaîne sans parenthèses extérieures inutiles
     */
    public static String trimParentheses(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = text.trim();

        while (text.length() >= 2 && text.charAt(0) == '(' && text.charAt(text.length() - 1) == ')') {
            // Vérifier si les parenthèses correspondent vraiment
            if (isCompletelyWrapped(text, '(', ')')) {
                text = text.substring(1, text.length() - 1).trim();
            } else {
                break;
            }
        }

        return text;
    }

    /**
     * Divise une chaîne au double-point en respectant les parenthèses.
     * Exemple : "player:name" → ["player", "name"]
     *
     * @param expression La chaîne à diviser
     * @return Un tableau des parties
     */
    public static String[] splitAtDoubleColon(String expression) {
        if (expression == null || expression.isEmpty()) {
            return new String[0];
        }

        // Trouver le dernier ':' qui n'est pas dans des parenthèses
        ParenthesesTracker tracker = new ParenthesesTracker();
        int lastColonIndex = -1;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            tracker.processChar(c);

            if (c == ':' && tracker.isAtTopLevel()) {
                lastColonIndex = i;
            }
        }

        if (lastColonIndex == -1) {
            return new String[]{expression};
        }

        String before = expression.substring(0, lastColonIndex).trim();
        String after = expression.substring(lastColonIndex + 1).trim();

        if (before.isEmpty()) {
            return new String[]{after};
        }

        return new String[]{before, after};
    }

    /**
     * Divise une expression en opérandes et opérateurs.
     *
     * @param expression L'expression à analyser
     * @param operators  Les opérateurs à rechercher
     * @return Un tableau alternant opérandes et opérateurs
     */
    public static String[] splitAtOperators(String expression, String... operators) {
        if (expression == null || expression.isEmpty() || operators == null || operators.length == 0) {
            return new String[]{expression};
        }

        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        ParenthesesTracker tracker = new ParenthesesTracker();

        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            tracker.processChar(c);

            if (tracker.isAtTopLevel()) {
                // Chercher un opérateur à cette position
                String foundOperator = findOperatorAt(expression, i, operators);
                if (foundOperator != null) {
                    // Ajouter l'opérande actuel
                    String operand = current.toString().trim();
                    if (!operand.isEmpty()) {
                        parts.add(operand);
                    }

                    // Ajouter l'opérateur
                    parts.add(foundOperator);

                    // Recommencer après l'opérateur
                    current.setLength(0);
                    i += foundOperator.length();
                    continue;
                }
            }

            current.append(c);
            i++;
        }

        // Ajouter la dernière partie
        String lastPart = current.toString().trim();
        if (!lastPart.isEmpty()) {
            parts.add(lastPart);
        }

        return parts.toArray(new String[0]);
    }

    /**
     * Vérifie si une chaîne est complètement entourée par des délimiteurs correspondants.
     *
     * @param text  La chaîne à vérifier
     * @param start Le délimiteur de début
     * @param end   Le délimiteur de fin
     * @return true si la chaîne est complètement entourée
     */
    public static boolean isCompletelyWrapped(String text, char start, char end) {
        if (text == null || text.length() < 2) {
            return false;
        }

        if (text.charAt(0) != start || text.charAt(text.length() - 1) != end) {
            return false;
        }

        // Vérifier que les délimiteurs correspondent
        int depth = 0;
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
                if (c == start) {
                    depth++;
                } else if (c == end) {
                    depth--;
                    // Si on atteint 0 avant la fin, ce n'est pas complètement enveloppé
                    if (depth == 0 && i < text.length() - 1) {
                        return false;
                    }
                }
            }
        }

        return depth == 0;
    }

    /**
     * Trouve un opérateur à une position donnée dans une chaîne.
     *
     * @param text      La chaîne à analyser
     * @param position  La position à vérifier
     * @param operators Les opérateurs à rechercher
     * @return L'opérateur trouvé ou null
     */
    private static String findOperatorAt(String text, int position, String... operators) {
        for (String operator : operators) {
            if (position + operator.length() <= text.length()) {
                String substring = text.substring(position, position + operator.length());
                if (substring.equals(operator)) {
                    return operator;
                }
            }
        }
        return null;
    }

    /**
     * Classe utilitaire pour suivre l'état des parenthèses, crochets et guillemets.
     */
    private static class ParenthesesTracker {
        private int parentheses = 0;
        private int brackets = 0;
        private int braces = 0;
        private boolean inString = false;
        private boolean escaping = false;

        public void processChar(char c) {
            if (escaping) {
                escaping = false;
                return;
            }

            if (c == '\\') {
                escaping = true;
                return;
            }

            if (c == '"') {
                inString = !inString;
                return;
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
            }
        }

        public boolean isAtTopLevel() {
            return !inString && parentheses == 0 && brackets == 0 && braces == 0;
        }

        public boolean isBalanced() {
            return !inString && parentheses == 0 && brackets == 0 && braces == 0;
        }

        public void reset() {
            parentheses = 0;
            brackets = 0;
            braces = 0;
            inString = false;
            escaping = false;
        }
    }
}





