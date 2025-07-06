package fr.nico.sqript.forge.common.item;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class ScriptItemArmor extends ItemArmor {

    public final String displayName;
    public final String textureName;

    public ScriptItemArmor(String displayName, String toolMaterial, EntityEquipmentSlot slot, String textureName) {
        super(ArmorMaterial.valueOf(toolMaterial.toUpperCase()), 0, slot);
        this.displayName = displayName;
        this.textureName = textureName;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return displayName;
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        return String.format("%s.png", textureName);
    }
}




