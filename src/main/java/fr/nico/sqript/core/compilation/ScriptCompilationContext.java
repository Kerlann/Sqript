package fr.nico.sqript.core.compilation;

import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.structures.ScriptTypeAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contexte de compilation utilisé pour déterminer quels accesseurs peuvent être utilisés dans un contexte donné.
 * Cette classe remplace l'ancienne ScriptCompilationContext du package compiling.
 */
public class ScriptCompilationContext {

    /**
     * Variables déclarées accessibles dans ce contexte
     */
    public List<ScriptTypeAccessor> declaredVariables = new ArrayList<>();

    /**
     * Contexte parent (pour la hiérarchie des portées)
     */
    public ScriptCompilationContext parent;

    public ScriptCompilationContext(ScriptTypeAccessor... declaredVariables) {
        this.declaredVariables.addAll(Arrays.asList(declaredVariables));
    }

    public ScriptCompilationContext() {
    }

    public ScriptCompilationContext(ScriptCompilationContext parent) {
        this.parent = parent;
    }

    /**
     * Recherche un accesseur pour un paramètre donné
     *
     * @param parameter le paramètre à rechercher
     * @return l'accesseur correspondant ou null si non trouvé
     */
    public ScriptTypeAccessor getAccessorFor(String parameter) {
        for (ScriptTypeAccessor s : declaredVariables) {
            if (s.getPattern().matcher(parameter).matches()) {
                return s;
            }
        }
        if (parent != null)
            return parent.getAccessorFor(parameter);
        return null;
    }

    /**
     * Ajoute une variable avec son type de retour
     */
    public void add(String variable, Class<? extends ScriptElement> returnType) {
        ScriptTypeAccessor sa = new ScriptTypeAccessor(null, variable);
        sa.setReturnType(returnType);
        declaredVariables.add(sa);
    }

    /**
     * Ajoute une variable avec un hash spécifique
     */
    public void add(String variable, int hash) {
        ScriptTypeAccessor sa = new ScriptTypeAccessor(null, variable, hash);
        declaredVariables.add(sa);
    }

    /**
     * Debug : affiche les variables du contexte
     */
    public void debugVariables() {
        System.out.println(this);
        for (ScriptTypeAccessor s : declaredVariables) {
            System.out.println(s.getPattern() + " : " + s.getElement());
        }
    }

    @Override
    public String toString() {
        return "ScriptCompilationContext{" +
                "declaredVariables=" + declaredVariables +
                (parent == null ? "" : "[" + parent + "]") + "}";
    }

    /**
     * Ajoute un tableau de features au contexte
     */
    public void addArray(List<Feature> asList) {
        for (Feature s : asList) {
            add(s.pattern(), parseType(s.type()));
        }
    }

    /**
     * Parse un type depuis une chaîne de caractères
     */
    private Class<? extends ScriptElement> parseType(String type) {
        // TODO: Implémenter la logique de parsing des types
        // Pour l'instant, retourner ScriptElement par défaut
        return ScriptElement.class;
    }
} 




