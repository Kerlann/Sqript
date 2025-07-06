package fr.nico.sqript.structures;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.blocks.ScriptBlock;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.SimpleRegex;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.types.ScriptType;

import java.util.Objects;
import java.util.regex.Pattern;

public class ScriptTypeAccessor {

    //Permet d'accéder à une variable d'un event

    public ScriptType element;
    public String key;
    /***
     * Very important, allows Sqript to recognize a variable efficiently with this hash code.
     */
    public int hash;
    Class<? extends ScriptBlock> blockType;
    Class returnType;
    int lineCounter;
    private Pattern pattern;

    public ScriptTypeAccessor() {
    }

    public ScriptTypeAccessor(ScriptType element, String match) {
        this.element = element;
        try {
            if (element != null)
                this.returnType = element.getClass();
            this.pattern = Pattern.compile(SimpleRegex.simplePatternToRegex(match, true).replaceAll("\\{", "\\\\{").replaceAll("}", "\\\\}"));
            this.key = match;
        } catch (ScriptException.ScriptPatternError scriptPatternError) {
            ScriptManager.log.error("Error trying to generate an accessor : " + pattern + " in " + blockType.getSimpleName());
            scriptPatternError.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("Setting with match : "+match+" giving : "+match.hashCode());
        this.hash = match.hashCode();
    }

    public ScriptTypeAccessor(ScriptType element, int hash) {
        this.element = element;
        this.hash = hash;
    }

    public ScriptTypeAccessor(ScriptType element, String match, int varHash) {
        this.element = element;
        try {
            this.pattern = ScriptRegistry.transformPattern(match).pattern;
        } catch (Exception ignored) {
        }
        this.key = match;
        this.hash = varHash;
    }

    @Override
    public String toString() {
        return pattern + ":" + (element == null ? "null" : element.toString());
    }

    @Override
    public boolean equals(Object o) {
        return o.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, blockType, lineCounter);
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public ScriptType getElement() {
        return element;
    }

    public void setElement(ScriptType element) {
        this.element = element;
        this.returnType = element.getType();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public Class getReturnType() {
        return returnType;
    }

    public void setReturnType(Class returnType) {
        this.returnType = returnType;
    }
}





