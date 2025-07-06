package fr.nico.sqript.core.compilation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitaires pour la manipulation des chaînes de caractères dans la compilation.
 * Optimisé pour les performances avec mise en cache des patterns regex.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public final class StringUtils {

    // Patterns regex mis en cache pour éviter la recompilation
    private static final Pattern PATTERN_CAPTURE_QUOTES = Pattern.compile("(?:\"((?:[^\"\\\\\\\\]|\\\\.)*)\")");
    private static final Pattern PATTERN_STRING_MARKER = Pattern.compile("@\\{S(\\d*)}");
    private static final Pattern PATTERN_REMOVED_STRING = Pattern.compile("\\{S(\\d*)}");

    // Pool de StringBuilder pour éviter les allocations répétées
    private static final ThreadLocal<StringBuilder> STRING_BUILDER_POOL =
            ThreadLocal.withInitial(() -> new StringBuilder(256));

    private StringUtils() {
        // Classe utilitaire - pas d'instanciation
    }

    /**
     * Extrait toutes les chaînes de caractères entre guillemets d'une expression.
     *
     * @param expression L'expression à analyser
     * @return Un tableau des chaînes extraites
     */
    public static String[] extractStrings(String expression) {
        if (expression == null || expression.isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        Matcher matcher = PATTERN_CAPTURE_QUOTES.matcher(expression);

        while (matcher.find()) {
            String extractedString = matcher.group(1);
            if (extractedString != null) {
                result.add(extractedString);
            }
        }

        return result.toArray(new String[0]);
    }

    /**
     * Remplace les marqueurs de chaînes par les chaînes originales.
     *
     * @param expression L'expression avec des marqueurs
     * @param strings    Les chaînes originales
     * @return L'expression avec les chaînes restaurées
     */
    public static String replaceStringMarkers(String expression, String[] strings) {
        if (expression == null || strings == null || strings.length == 0) {
            return expression;
        }

        String result = expression;
        for (int i = 0; i < strings.length; i++) {
            String marker = "@{S" + i + "}";
            String replacement = "\"" + Matcher.quoteReplacement(strings[i]) + "\"";
            result = result.replaceFirst(Pattern.quote(marker), replacement);
        }

        return result;
    }

    /**
     * Supprime les chaînes d'une expression et les remplace par des marqueurs.
     *
     * @param expression L'expression originale
     * @param strings    Les chaînes à remplacer
     * @return L'expression avec des marqueurs
     */
    public static String removeStrings(String expression, String[] strings) {
        if (expression == null || strings == null || strings.length == 0) {
            return expression;
        }

        String result = expression;
        for (int i = 0; i < strings.length; i++) {
            String quotedString = "\"" + strings[i] + "\"";
            String marker = "@{S" + i + "}";
            result = result.replaceFirst(Pattern.quote(quotedString), marker);
        }

        return result;
    }

    /**
     * Nettoie et optimise une chaîne en supprimant les espaces redondants.
     *
     * @param input La chaîne à nettoyer
     * @return La chaîne nettoyée
     */
    public static String cleanSpaces(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Utiliser le pool de StringBuilder pour éviter les allocations
        StringBuilder sb = STRING_BUILDER_POOL.get();
        sb.setLength(0);

        boolean lastWasSpace = false;
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaping) {
                sb.append(c);
                escaping = false;
                lastWasSpace = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                sb.append(c);
                lastWasSpace = false;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                sb.append(c);
                lastWasSpace = false;
                continue;
            }

            if (inString) {
                sb.append(c);
                lastWasSpace = false;
                continue;
            }

            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }

        return sb.toString().trim();
    }

    /**
     * Échappe les caractères spéciaux dans une chaîne pour une utilisation dans une regex.
     *
     * @param input La chaîne à échapper
     * @return La chaîne échappée
     */
    public static String escapeRegex(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        return Pattern.quote(input);
    }

    /**
     * Remplace la dernière occurrence d'un pattern dans une chaîne.
     *
     * @param text        Le texte à modifier
     * @param regex       Le pattern à rechercher
     * @param replacement Le texte de remplacement
     * @return Le texte modifié
     */
    public static String replaceLast(String text, String regex, String replacement) {
        if (text == null || regex == null || replacement == null) {
            return text;
        }

        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }

    /**
     * Vérifie si une chaîne est vide ou ne contient que des espaces.
     *
     * @param str La chaîne à vérifier
     * @return true si la chaîne est vide ou ne contient que des espaces
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Joint un tableau de chaînes avec un délimiteur.
     *
     * @param strings   Le tableau de chaînes
     * @param delimiter Le délimiteur
     * @return La chaîne jointe
     */
    public static String join(String[] strings, String delimiter) {
        if (strings == null || strings.length == 0) {
            return "";
        }

        if (strings.length == 1) {
            return strings[0];
        }

        StringBuilder sb = STRING_BUILDER_POOL.get();
        sb.setLength(0);

        for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(strings[i]);
        }

        return sb.toString();
    }

    /**
     * Capitalise la première lettre d'une chaîne.
     *
     * @param str La chaîne à capitaliser
     * @return La chaîne capitalisée
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (str.length() == 1) {
            return str.toUpperCase();
        }

        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Compte le nombre d'occurrences d'un caractère dans une chaîne.
     *
     * @param str La chaîne à analyser
     * @param ch  Le caractère à compter
     * @return Le nombre d'occurrences
     */
    public static int countOccurrences(String str, char ch) {
        if (str == null || str.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }

        return count;
    }

    /**
     * Divise une chaîne en mots en respectant les guillemets.
     *
     * @param input La chaîne à diviser
     * @return Un tableau des mots
     */
    public static String[] splitRespectingQuotes(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaping = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
                current.append(c);
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }

            if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result.toArray(new String[0]);
    }

    /**
     * Formatte une durée en nanosecondes en chaîne lisible.
     *
     * @param nanos La durée en nanosecondes
     * @return La chaîne formatée
     */
    public static String formatDuration(long nanos) {
        if (nanos < 1_000) {
            return nanos + "ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.2fμs", nanos / 1_000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2fms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2fs", nanos / 1_000_000_000.0);
        }
    }

    /**
     * Génère un hash rapide pour une chaîne (alternative rapide à hashCode).
     *
     * @param str La chaîne à hasher
     * @return Le hash calculé
     */
    public static int fastHash(String str) {
        if (str == null) {
            return 0;
        }

        int hash = 0;
        int length = str.length();

        for (int i = 0; i < length; i++) {
            hash = 31 * hash + str.charAt(i);
        }

        return hash;
    }
}





