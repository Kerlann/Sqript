package fr.nico.sqript.core.compilation;

import fr.nico.sqript.structures.ScriptElement;

/**
 * Représente un type de retour pour les opérations de compilation.
 * Cette classe remplace l'ancienne ReturnType du package compiling.
 */
public class ReturnType {

    private Class<? extends ScriptElement> type;

    public ReturnType() {
    }

    public ReturnType(Class<? extends ScriptElement> type) {
        this.type = type;
    }

    public Class<? extends ScriptElement> getType() {
        return type;
    }

    public void setType(Class<? extends ScriptElement> type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ReturnType{" +
                "type=" + type +
                '}';
    }
} 




