package fr.nico.sqript.core.compilation;

import fr.nico.sqript.structures.ScriptInstance;

import java.util.Objects;

/**
 * Représente une ligne complète ou une partie d'une ligne d'un script.
 * Cette classe remplace l'ancienne ScriptToken du package compiling.
 */
public class ScriptToken {

    /**
     * Le numéro de ligne où se trouve ce token.
     */
    private final int lineNumber;
    /**
     * L'instance de script contenant ce ScriptToken.
     */
    private final ScriptInstance scriptInstance;
    /**
     * Le texte brut représentant ce token
     */
    private String text;
    /**
     * Le niveau d'indentation initial de ce token.
     */
    private int tabLevel;

    public ScriptToken(String text, int number, ScriptInstance scriptInstance) {
        this.text = text;
        this.tabLevel = getTabLevel(text);
        this.lineNumber = number;
        this.scriptInstance = scriptInstance;
    }

    public ScriptToken(String text) {
        this.text = text;
        this.tabLevel = getTabLevel(text);
        this.lineNumber = 0;
        this.scriptInstance = null;
    }

    /**
     * Calcule le niveau d'indentation d'une ligne
     */
    private static int getTabLevel(String line) {
        if (line == null) return 0;
        int tabs = 0;
        for (char c : line.toCharArray()) {
            if (c == '\t') tabs++;
            else if (c == ' ') tabs += 0.25; // 4 espaces = 1 tab
            else break;
        }
        return tabs;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public ScriptInstance getScriptInstance() {
        return scriptInstance;
    }

    public int getTabLevel() {
        return tabLevel;
    }

    public void setTabLevel(int tabLevel) {
        this.tabLevel = tabLevel;
    }

    @Override
    public String toString() {
        if (scriptInstance != null && scriptInstance.getScriptFile() != null) {
            return "[" + scriptInstance.getScriptFile().getName() + " at line n°" + (lineNumber + 1) + "] \"" + text.trim() + "\"";
        } else {
            return "[example at line n°" + (lineNumber + 1) + "] \"" + text.trim() + "\"";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptToken that = (ScriptToken) o;
        return lineNumber == that.lineNumber && Objects.equals(text, that.text) && Objects.equals(scriptInstance, that.scriptInstance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, lineNumber, scriptInstance);
    }

    @Override
    public Object clone() {
        return new ScriptToken(text, lineNumber, scriptInstance);
    }

    public ScriptToken with(String parameter) {
        ScriptToken token = new ScriptToken(parameter, lineNumber, scriptInstance);
        token.setTabLevel(this.getTabLevel());
        return token;
    }

    public ScriptToken withString(String[] strings) {
        text = StringUtils.replaceStringMarkers(text, strings);
        return this;
    }

    public void trim() {
        setText(text.trim());
    }
} 




