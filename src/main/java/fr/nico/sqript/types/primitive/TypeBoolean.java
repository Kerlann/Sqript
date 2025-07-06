package fr.nico.sqript.types.primitive;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.meta.Primitive;
import fr.nico.sqript.structures.ScriptOperator;
import fr.nico.sqript.types.interfaces.ISerialisable;
import net.minecraft.nbt.NBTTagCompound;

@Primitive(name = "boolean",
        parsableAs = TypeString.class,
        pattern = "(" + ScriptRegistry.CAPTURE_BOOLEAN + ")")
public class TypeBoolean extends PrimitiveType<Boolean> implements ISerialisable {

    static {
        ScriptManager.registerBinaryOperation(ScriptOperator.AND, TypeBoolean.class, TypeBoolean.class, TypeBoolean.class,
                (a, b) -> new TypeBoolean(((TypeBoolean) a).getObject() && ((TypeBoolean) b).getObject()));

        ScriptManager.registerBinaryOperation(ScriptOperator.OR, TypeBoolean.class, TypeBoolean.class, TypeBoolean.class,
                (a, b) -> new TypeBoolean(((TypeBoolean) a).getObject() || ((TypeBoolean) b).getObject()));

        ScriptManager.registerUnaryOperation(ScriptOperator.NOT, TypeBoolean.class, TypeBoolean.class,
                (a, b) -> new TypeBoolean(!((TypeBoolean) a).getObject()));
    }


    public TypeBoolean(Boolean object) {
        super(object);
    }

    public TypeBoolean(String match) {
        this(Boolean.valueOf(match));
    }

    public static TypeBoolean FALSE() {
        return new TypeBoolean(false);
    }

    public static TypeBoolean TRUE() {
        return new TypeBoolean(true);
    }

    @Override
    public NBTTagCompound write(NBTTagCompound compound) {
        compound.setBoolean("value", getObject());
        return compound;
    }

    @Override
    public void read(NBTTagCompound compound) {
        setObject(compound.getBoolean("value"));
    }
}





