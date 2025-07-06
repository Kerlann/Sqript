package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;
import net.minecraft.util.math.AxisAlignedBB;

@Type(name = "axisalignedbb",
        parsableAs = {}
)
public class TypeAxisAlignedBB extends ScriptType<AxisAlignedBB> {

    public TypeAxisAlignedBB(AxisAlignedBB object) {
        super(object);
    }

    @Override
    public String toString() {
        return getObject().toString();
    }


}





