package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;

import java.awt.image.BufferedImage;

@Type(name = "image",
        parsableAs = {})
public class TypeImage extends ScriptType<BufferedImage> {

    public TypeImage(BufferedImage image) {
        super(image);
    }

    @Override
    public String toString() {
        return "Image sized " + getObject().getWidth() + "x" + getObject().getHeight();
    }


}





