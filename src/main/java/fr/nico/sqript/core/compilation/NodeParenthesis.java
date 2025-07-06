package fr.nico.sqript.core.compilation;

import fr.nico.sqript.structures.ScriptElement;

/**
 * Nœud représentant des parenthèses dans l'arbre syntaxique abstrait.
 * Cette classe remplace l'ancienne NodeParenthesis du package compiling.
 */
public class NodeParenthesis extends Node {

    private EnumTokenType type;

    public NodeParenthesis(EnumTokenType type) {
        this.type = type;
    }

    public EnumTokenType getType() {
        return type;
    }

    public void setType(EnumTokenType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type.toString() + super.toString();
    }

    @Override
    public Class<? extends ScriptElement> getReturnType() {
        return null;
    }
} 




