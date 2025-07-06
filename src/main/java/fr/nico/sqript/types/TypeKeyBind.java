package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;
import net.minecraft.client.settings.KeyBinding;

@Type(name = "key",
        parsableAs = {})
public class TypeKeyBind extends ScriptType<KeyBinding> {

    public TypeKeyBind(KeyBinding key) {
        super(key);
    }

    @Override
    public String toString() {
        return this.getObject().getDisplayName();
    }

}





