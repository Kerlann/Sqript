package fr.nico.sqript.types.primitive;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.meta.Primitive;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.structures.ScriptOperator;
import fr.nico.sqript.types.interfaces.ISerialisable;
import net.minecraft.nbt.NBTTagCompound;

@Primitive(name = "string",
        parsableAs = {},
        pattern = ScriptRegistry.CAPTURE_BETWEEN_QUOTES
)
public class TypeString extends PrimitiveType<String> implements ISerialisable {

    static {

        ScriptManager.registerBinaryOperation(ScriptOperator.ADD, TypeString.class, ScriptElement.class, TypeString.class,
                (a, b) -> new TypeString((a == null ? "null" : a.getObject()) + (b == null ? "null" : b.toString())), 1);

        ScriptManager.registerBinaryOperation(ScriptOperator.ADD, ScriptElement.class, TypeString.class, TypeString.class,
                (a, b) -> new TypeString((a == null ? "null" : a.toString()) + (b == null ? "null" : b.getObject())), 1);

        ScriptManager.registerBinaryOperation(ScriptOperator.ADD, ScriptElement.class, ScriptElement.class, TypeString.class,
                (a, b) -> new TypeString((a == null ? "null" : a.toString()) + (b == null ? "null" : b.toString())), 0);
        ScriptManager.registerBinaryOperation(ScriptOperator.ADD, TypeString.class, Object.class, TypeString.class,
                (a, b) -> new TypeString((a == null ? "null" : a.getObject()) + (b == null ? "null" : b.toString())), 0);

        ScriptManager.registerBinaryOperation(ScriptOperator.MT, TypeString.class, TypeString.class, TypeString.class,
                (a, b) -> new TypeBoolean(((TypeString) a).getObject().compareTo(((TypeString) b).getObject()) > 0), 1);

        ScriptManager.registerBinaryOperation(ScriptOperator.MTE, TypeString.class, TypeString.class, TypeString.class,
                (a, b) -> new TypeBoolean(((TypeString) a).getObject().compareTo(((TypeString) b).getObject()) >= 0), 1);

        ScriptManager.registerBinaryOperation(ScriptOperator.LT, TypeString.class, TypeString.class, TypeString.class,
                (a, b) -> new TypeBoolean(((TypeString) a).getObject().compareTo(((TypeString) b).getObject()) < 0), 1);

        ScriptManager.registerBinaryOperation(ScriptOperator.LTE, TypeString.class, TypeString.class, TypeString.class,
                (a, b) -> new TypeBoolean(((TypeString) a).getObject().compareTo(((TypeString) b).getObject()) <= 0), 1);

    }

    public TypeString(String parameter) {
        super(parameter);
    }

    @Override
    public NBTTagCompound write(NBTTagCompound compound) {
        compound.setString("object", getObject());
        return compound;
    }

    @Override
    public void read(NBTTagCompound compound) {
        setObject(compound.getString("object"));
    }

}





