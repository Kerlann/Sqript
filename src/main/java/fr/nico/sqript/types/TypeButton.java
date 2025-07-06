package fr.nico.sqript.types;

import fr.nico.sqript.forge.gui.Button;
import fr.nico.sqript.meta.Type;

@Type(name = "button",
        parsableAs = {})
public class TypeButton extends ScriptType<Button> {

    public TypeButton(Button button) {
        super(button);
    }

    @Override
    public String toString() {
        return "container";
    }


}





