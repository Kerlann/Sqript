package fr.nico.sqript.forge.common.item;

import net.minecraft.item.Item;

public class ScriptItem {

    //Wrapper for item classes

    public final String modid, customModel;
    public final Item item;

    public ScriptItem(String modid, String customModel, Item item) {
        this.modid = modid;
        this.customModel = customModel;
        this.item = item;
    }


    public String getModid() {
        return modid;
    }

    public String getCustomModel() {
        return customModel;
    }

    public Item getItem() {
        return item;
    }
}





