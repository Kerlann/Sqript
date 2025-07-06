package fr.nico.sqript.core.compilation;

import fr.nico.sqript.structures.ScriptElement;

/**
 * Nœud représentant un switch dans l'arbre syntaxique abstrait.
 * Cette classe remplace l'ancienne NodeSwitch du package compiling.
 */
public class NodeSwitch extends Node {

    public NodeSwitch(Node[] children) {
        super(children);
    }

    @Override
    public Class<? extends ScriptElement> getReturnType() {
        return ScriptElement.class;
    }

    @Override
    public String toString() {
        return "switch:(" + super.toString() + ")";
    }
} 




