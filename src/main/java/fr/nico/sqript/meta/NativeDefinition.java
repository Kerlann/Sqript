package fr.nico.sqript.meta;

import fr.nico.sqript.function.ScriptNativeFunction;
import fr.nico.sqript.structures.TransformedPattern;

public class NativeDefinition {

    private final Class<? extends ScriptNativeFunction> cls;
    public TransformedPattern[] transformedPatterns;
    String name;
    String[] description;
    String[] patterns;
    String[] example;


    public NativeDefinition(String name, String[] description, String[] example, Class<? extends ScriptNativeFunction> cls) {
        this.name = name;
        this.example = example;
        this.description = description;
        this.cls = cls;
    }

    public NativeDefinition setTransformedPatterns(TransformedPattern... data) {
        this.transformedPatterns = data;
        return this;
    }

    public int getMatchedPatternIndex(String line) {
        for (int i = 0; i < transformedPatterns.length; i++) {
            //System.out.println("Checking if "+line+" matches "+transformedPatterns[i].regex);
            if (transformedPatterns[i].getPattern().matcher(line).matches()) return i;
        }
        return -1;
    }

    public Class<? extends ScriptNativeFunction> getNativeClass() {
        return cls;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getExample() {
        return example;
    }

    public void setExample(String[] example) {
        this.example = example;
    }

    public String[] getExamples() {
        return example;
    }

    public String[] getDescription() {
        return description;
    }

    public void setDescription(String[] description) {
        this.description = description;
    }

    public String[] getPatterns() {
        return patterns;
    }

    public void setPatterns(String... patterns) {
        this.patterns = patterns;
    }
}





