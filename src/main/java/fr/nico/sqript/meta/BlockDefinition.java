package fr.nico.sqript.meta;

import fr.nico.sqript.blocks.ScriptBlock;

import java.util.regex.Pattern;

public class BlockDefinition {

    Feature feature;
    Feature[] fields;
    boolean reloadable;
    Pattern regex;
    private final Class<? extends ScriptBlock> cls;

    public BlockDefinition(Class<? extends ScriptBlock> cls, Feature feature, Feature[] fields, boolean reloadable) {
        this.cls = cls;
        this.feature = feature;
        this.fields = fields;
        this.regex = Pattern.compile(feature.regex());
        this.reloadable = reloadable;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public Class<? extends ScriptBlock> getBlockClass() {
        return cls;
    }

    public Pattern getRegex() {
        return regex;
    }


    public Feature getFeature() {
        return feature;
    }

    public Class<? extends ScriptBlock> getCls() {
        return cls;
    }


    public Feature[] getFields() {
        return fields;
    }

    /**
     * Vérifie si le texte correspond au pattern de ce bloc
     *
     * @param text Le texte à vérifier
     * @return 0 si le pattern correspond, -1 sinon
     */
    public int getMatchedPatternIndex(String text) {
        if (text == null || regex == null) {
            return -1;
        }
        return regex.matcher(text).matches() ? 0 : -1;
    }
}





