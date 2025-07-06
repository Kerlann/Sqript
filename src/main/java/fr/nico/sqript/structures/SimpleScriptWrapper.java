package fr.nico.sqript.structures;

import fr.nico.sqript.core.compilation.ScriptException;

/**
 * Wrapper simple pour encapsuler une chaîne d'instructions de script.
 * Utilisé pour exécuter séquentiellement plusieurs instructions dans un bloc.
 */
public class SimpleScriptWrapper extends ScriptWrapper {

    private IScript wrappedScript;

    public SimpleScriptWrapper() {
        super();
    }

    @Override
    public IScript getWrapped() {
        if (wrappedScript != null) {
            return wrappedScript;
        }
        return super.getWrapped();
    }

    /**
     * Définit le script wrappé
     */
    public void setWrapped(IScript wrapped) {
        // Utiliser la méthode héritée de ScriptWrapper
        if (this instanceof ScriptWrapper) {
            // Accès direct au champ de la classe parent
            try {
                java.lang.reflect.Field wrappedField = ScriptWrapper.class.getDeclaredField("wrapped");
                wrappedField.setAccessible(true);
                wrappedField.set(this, wrapped);
            } catch (Exception e) {
                // Fallback : stocker dans une variable locale
                this.wrappedScript = wrapped;
            }
        }
    }

    @Override
    public IScript run(ScriptContext context) throws ScriptException {
        // Exécuter l'instruction wrappée
        if (getWrapped() != null) {
            return getWrapped();
        }
        return getNext(context);
    }

    @Override
    public void execute(ScriptContext context) throws ScriptException {
        // Cette méthode peut être appelée directement pour exécuter le wrapper
        if (getWrapped() != null) {
            getWrapped().execute(context);
        }
    }

    @Override
    public String toString() {
        return "SimpleScriptWrapper{wrapped=" + (getWrapped() != null ? getWrapped().getClass().getSimpleName() : "null") + "}";
    }
}
