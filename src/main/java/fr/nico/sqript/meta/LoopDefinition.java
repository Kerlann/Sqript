package fr.nico.sqript.meta;

import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.structures.IScript;
import fr.nico.sqript.structures.ScriptLoop;
import fr.nico.sqript.structures.ScriptWrapper;
import fr.nico.sqript.structures.Side;

import java.lang.reflect.Constructor;
import java.util.regex.Pattern;

public class LoopDefinition {

    private final Pattern pattern;
    private final Class<? extends ScriptLoop> cls;
    private final Side side;
    private final String name;
    private final int priority;

    public LoopDefinition(String pattern, Class cls, Side side, String name, int priority) {
        this.pattern = Pattern.compile(pattern);
        this.cls = cls;
        this.side = side;
        this.name = name;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getName() {
        return name;
    }

    public Side getSide() {
        return side;
    }

    public Class<? extends ScriptLoop> getLoopClass() {
        return cls;
    }

    public boolean matches(String line) {
        return pattern.matcher(line).matches();
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Tente de parser une boucle avec cette définition
     */
    public ScriptWrapper tryParse(ScriptToken token, ScriptCompilationContext context) throws Exception {
        // Implémentation simplifiée - utilise le constructeur par défaut
        try {
            Constructor<? extends ScriptLoop> constructor = cls.getConstructor();
            ScriptLoop loop = constructor.newInstance();
            return new SimpleScriptWrapper(loop);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Implémentation simple de ScriptWrapper pour les boucles
     */
    private static class SimpleScriptWrapper extends ScriptWrapper {
        public SimpleScriptWrapper(IScript wrapped) {
            super(wrapped);
        }
    }
}





