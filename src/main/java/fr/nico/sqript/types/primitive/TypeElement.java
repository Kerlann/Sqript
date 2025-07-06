package fr.nico.sqript.types.primitive;

import fr.nico.sqript.meta.Primitive;
import fr.nico.sqript.structures.ScriptElement;

@Primitive(name = "element",
        parsableAs = {},
        pattern = "(\\S+)"
)
public class TypeElement extends PrimitiveType<ScriptElement> {

    public TypeElement(ScriptElement element) {
        super(element);
    }

    public TypeElement(String value) {
        super(null); // À implémenter selon les besoins
    }

    @Override
    public String toString() {
        return getObject() != null ? getObject().toString() : "null";
    }
}





