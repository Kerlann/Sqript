package fr.nico.sqript.types.primitive;

import fr.nico.sqript.meta.Primitive;
import fr.nico.sqript.types.interfaces.ILocatable;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

@Primitive(name = "entity",
        parsableAs = {},
        pattern = "entity\\s*(\\w+)"
)
public class TypeEntity extends PrimitiveType<Entity> implements ILocatable {

    public TypeEntity(Entity entity) {
        super(entity);
    }

    public TypeEntity(String entityName) {
        super(null); // À implémenter selon les besoins
    }

    @Override
    public String toString() {
        return this.getObject().getName();
    }

    @Override
    public Vec3d getVector() {
        return new Vec3d(getObject().posX, getObject().posY, getObject().posZ);
    }
}





