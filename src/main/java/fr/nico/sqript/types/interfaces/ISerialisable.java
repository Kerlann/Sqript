package fr.nico.sqript.types.interfaces;

import fr.nico.sqript.core.compilation.ScriptException;
import net.minecraft.nbt.NBTTagCompound;

public interface ISerialisable {

    NBTTagCompound write(NBTTagCompound compound);

    void read(NBTTagCompound compound) throws ScriptException;

}





