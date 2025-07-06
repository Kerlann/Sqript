package fr.nico.sqript.meta;

import fr.nico.sqript.core.compilation.CompilationCache;
import fr.nico.sqript.core.compilation.PatternMatcher;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.structures.TransformedPattern;

public class EventDefinition {

    // Initialisation correcte du cache et du matcher
    private static final CompilationCache CACHE;
    private static final PatternMatcher PATTERN_MATCHER;
    static {
        CACHE = new CompilationCache(1000, 300000, 300000);
        CACHE.initialize();
        PATTERN_MATCHER = new PatternMatcher(CACHE);
    }

    public Class<? extends ScriptEvent> eventClass;
    private TransformedPattern[] transformedPatterns;
    private final Feature feature;
    private Feature[] accessors;

    public EventDefinition(Class<? extends ScriptEvent> eventClass, Feature feature, Feature[] accessors) throws Exception {
        this.feature = feature;
        this.accessors = accessors;
        this.eventClass = eventClass;
        if (feature != null) {
            try {
                this.transformedPatterns = new TransformedPattern[]{ScriptRegistry.transformPattern(feature.pattern())};
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }

    }

    public Feature getFeature() {
        return feature;
    }

    public Feature[] getAccessors() {
        return accessors;
    }

    public EventDefinition setAccessors(Feature... accessors) {
        this.accessors = accessors;
        this.transformedPatterns = new TransformedPattern[accessors.length];
        for (int i = 0; i < this.accessors.length; i++) {
            try {
                this.transformedPatterns[i] = ScriptRegistry.transformPattern(accessors[i].pattern());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this;
    }

    public TransformedPattern[] getTransformedPatterns() {
        return transformedPatterns;
    }

    public int getMatchedPatternIndex(String line) {
        for (int i = 0; i < transformedPatterns.length; i++) {
            //System.out.println("Checking if "+line+" matches "+transformedPatterns[i].getPattern().pattern());
            // Utiliser PatternMatcher avec cache au lieu de regex directe
            if (PATTERN_MATCHER.matches(transformedPatterns[i], line))
                return i;
        }
        return -1;
    }

    public Class<? extends ScriptEvent> getEventClass() {
        return eventClass;
    }

}





