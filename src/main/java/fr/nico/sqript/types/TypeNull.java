package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;

@Type(name = "null",
        parsableAs = {})
public class TypeNull extends ScriptType<Object> {

    public TypeNull() {
        super(null);
    }

    @Override
    public String toString() {
        return "undefined";
    }


}





