package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;
import net.minecraft.world.World;

@Type(name = "world",
        parsableAs = {})
public class TypeWorld extends ScriptType<World> {

    public TypeWorld(World world) {
        super(world);
    }

    @Override
    public String toString() {
        return this.getObject().getWorldInfo().getWorldName();
    }

}





