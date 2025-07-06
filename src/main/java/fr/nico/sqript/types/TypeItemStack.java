package fr.nico.sqript.types;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.meta.Type;
import fr.nico.sqript.types.primitive.TypeResource;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

@Type(name = "item",
        parsableAs = {})
public class TypeItemStack extends ScriptType<ItemStack> {

    static {
        ScriptManager.registerTypeParser(TypeResource.class, TypeItemStack.class, r -> {
                    if (ForgeRegistries.ITEMS.getValue(r.getObject()) == null)
                        System.err.println("Item not defined : " + r.getObject());
                    return new TypeItemStack(new ItemStack(ForgeRegistries.ITEMS.getValue(r.getObject())));
                }
                , 0);
    }

    public TypeItemStack(ItemStack itemStack) {
        super(itemStack);
    }

    @Override
    public String toString() {
        return this.getObject().getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        //System.out.println("Comparing "+this+" with "+((ScriptType)(o)).getObject());
        if (o instanceof TypeItemStack) {
            return ItemStack.areItemStacksEqual(((TypeItemStack) (o)).getObject(), getObject());
        } else if (o instanceof TypeResource) {
            return ((TypeResource) (o)).getObject().equals(getObject().getItem().getRegistryName());
        } else if (o instanceof TypeItem) {
            return this.getObject().getItem() == ((TypeItem) o).getObject();
        }
        return false;
    }

}





