package fr.nico.sqript.meta;

import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.structures.TransformedPattern;

public class TypeDefinition {

    private final Class<? extends ScriptElement<?>> cls;
    public String name;
    public String[] description;
    public String[] example;
    public TransformedPattern transformedPattern;

    public TypeDefinition(String name, String[] description, String[] example, Class<? extends ScriptElement<?>> cls) {
        this.name = name;
        this.example = example;
        this.description = description;
        this.cls = cls;
    }

    public Class<? extends ScriptElement<?>> getTypeClass() {
        return cls;
    }

    public boolean matchedPattern(String line) {
        if (transformedPattern != null) {
            return transformedPattern.getPattern().matcher(line).matches();
        }
        return false;
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

    public String[] getDescription() {
        return description;
    }

    public void setDescription(String[] description) {
        this.description = description;
    }

}





