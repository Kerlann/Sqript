package fr.nico.sqript.types;

import fr.nico.sqript.blocks.ScriptBlockFunction;
import fr.nico.sqript.meta.Type;

@Type(name = "function",
        parsableAs = {})
public class TypeFunction extends ScriptType<ScriptBlockFunction> {

    public TypeFunction(ScriptBlockFunction function) {
        super(function);
    }

    @Override
    public String toString() {
        return this.getObject().name;
    }


}





