package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;
import fr.nico.sqript.network.ScriptMessage;

@Type(name = "message",
        parsableAs = {})
public class TypeMessagePrototype extends ScriptType<ScriptMessage> {

    public TypeMessagePrototype(ScriptMessage message) {
        super(message);
    }

    @Override
    public String toString() {
        return this.getObject().getMessage_id();
    }


}





