package fr.nico.sqript.types;

import fr.nico.sqript.forge.gui.Label;
import fr.nico.sqript.meta.Type;

@Type(name = "label",
        parsableAs = {})
public class TypeLabel extends ScriptType<Label> {

    public TypeLabel(Label label) {
        super(label);
    }

    @Override
    public String toString() {
        return "Label " + getObject().getDisplayText();
    }


}





