package fr.nico.sqript.types;

import fr.nico.sqript.forge.gui.Image;
import fr.nico.sqript.meta.Type;

@Type(name = "image widget",
        parsableAs = {})
public class TypeImageWidget extends ScriptType<Image> {

    public TypeImageWidget(Image image) {
        super(image);
    }

    @Override
    public String toString() {
        return "Image widget from " + getObject().imageName;
    }


}





