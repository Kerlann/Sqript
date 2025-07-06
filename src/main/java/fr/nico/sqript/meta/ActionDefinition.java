package fr.nico.sqript.meta;

import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.core.compilation.CompilationCache;
import fr.nico.sqript.core.compilation.PatternMatcher;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.structures.TransformedPattern;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;

public class ActionDefinition {

    // Initialisation correcte du cache et du matcher
    private static final CompilationCache CACHE;
    private static final PatternMatcher PATTERN_MATCHER;
    static {
        CACHE = new CompilationCache(1000, 300000, 300000);
        CACHE.initialize();
        PATTERN_MATCHER = new PatternMatcher(CACHE);
    }

    final int priority;
    public TransformedPattern[] transformedPatterns;
    public Class<? extends ScriptAction> cls;
    String name;
    Feature[] features;

    public ActionDefinition(String name, Class<? extends ScriptAction> cls, int priority, @Nullable Feature... features) throws Exception {
        this.name = name;
        this.cls = cls;
        this.priority = priority;
        if (features != null) {
            this.features = features;
            this.transformedPatterns = new TransformedPattern[this.features.length];
            for (int i = 0; i < this.features.length; i++) {
                try {
                    this.transformedPatterns[i] = ScriptRegistry.transformPattern(this.features[i].pattern());
                    //System.out.println("Set transformed patterns for "+getActionClass()+" : "+ Arrays.toString(transformedPatterns));
                    //System.out.println("Regex for "+features[i]+" is : "+transformedPatterns[i].getPattern().pattern());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }

    }

    public int getPriority() {
        return priority;
    }

    //[0] : index
    //[1] : marks
    public int[] getMatchedPatternIndexAndMarks(String line) {
        for (int i = 0; i < transformedPatterns.length; i++) {
            //System.out.println("Checking if "+line+" matches "+transformedPatterns[i].getPattern().pattern());
            // Utiliser PatternMatcher avec cache au lieu de regex directe
            if (PATTERN_MATCHER.matches(transformedPatterns[i], line)) {
                return new int[]{i, transformedPatterns[i].getAllMarks(line)};
            }
        }
        return new int[0];
    }

    public Class<? extends ScriptAction> getActionClass() {
        return cls;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Feature[] getFeatures() {
        return features;
    }

    public String toString() {
        return name;
    }

    /**
     * Vérifie si un pattern de cette action correspond au texte donné
     */
    public boolean hasMatchingPattern(String line) {
        if (transformedPatterns == null) {
            return false;
        }
        
        // Utiliser PatternMatcher avec cache pour optimiser
        for (TransformedPattern pattern : transformedPatterns) {
            if (pattern != null && PATTERN_MATCHER.matches(pattern, line)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Tente de parser une action avec cette définition
     */
    public ScriptAction tryParse(ScriptToken token, ScriptCompilationContext context) throws Exception {
        // Implémentation simplifiée - utilise le constructeur par défaut
        try {
            Constructor<? extends ScriptAction> constructor = cls.getConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}





