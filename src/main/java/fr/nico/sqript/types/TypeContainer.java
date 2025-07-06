package fr.nico.sqript.types;

import fr.nico.sqript.forge.gui.Container;
import fr.nico.sqript.meta.Type;

@Type(name = "container",
        parsableAs = {})
public class TypeContainer extends ScriptType<Container> {

    public TypeContainer(Container container) {
        super(container);
    }

    @Override
    public String toString() {
        return "container";
    }


}





