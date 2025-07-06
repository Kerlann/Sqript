package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;
import net.minecraft.util.DamageSource;

@Type(name = "damage_source",
        parsableAs = {})
public class TypeDamageSource extends ScriptType<DamageSource> {
    public TypeDamageSource(DamageSource damageSource) {
        super(damageSource);
    }

    @Override
    public String toString() {
        return this.getObject().getDamageType();
    }

    @Override
    public boolean equals(Object o) {
        return o.toString().equalsIgnoreCase(getObject().getDamageType());
    }

}





