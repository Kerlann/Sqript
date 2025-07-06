package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;
import net.minecraft.server.MinecraftServer;

@Type(name = "console",
        parsableAs = {})
public class TypeConsole extends ScriptType<MinecraftServer> {

    public TypeConsole(MinecraftServer sender) {
        super(sender);
    }

    @Override
    public String toString() {
        return this.getObject().getName();
    }


}





