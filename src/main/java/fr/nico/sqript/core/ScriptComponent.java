package fr.nico.sqript.core;

/**
 * Interface de base pour tous les composants du moteur Sqript.
 * Définit le cycle de vie standard des composants.
 *
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public interface ScriptComponent {

    /**
     * Initialise le composant avec les dépendances nécessaires.
     *
     * @throws ScriptComponentException si l'initialisation échoue
     */
    void initialize() throws ScriptComponentException;

    /**
     * Démarre le composant après initialisation.
     *
     * @throws ScriptComponentException si le démarrage échoue
     */
    void start() throws ScriptComponentException;

    /**
     * Arrête proprement le composant.
     *
     * @throws ScriptComponentException si l'arrêt échoue
     */
    void stop() throws ScriptComponentException;

    /**
     * Indique si le composant est actif.
     *
     * @return true si le composant est démarré et fonctionnel
     */
    boolean isActive();

    /**
     * Retourne le nom unique du composant.
     *
     * @return le nom du composant
     */
    String getComponentName();

    /**
     * Exception spécifique aux composants Sqript.
     */
    class ScriptComponentException extends Exception {
        public ScriptComponentException(String message) {
            super(message);
        }

        public ScriptComponentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}





