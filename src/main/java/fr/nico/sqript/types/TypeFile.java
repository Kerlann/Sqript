package fr.nico.sqript.types;

import fr.nico.sqript.meta.Type;

import java.io.File;

@Type(name = "file",
        parsableAs = {})
public class TypeFile extends ScriptType<File> {

    public TypeFile(File file) {
        super(file);
    }

    @Override
    public String toString() {
        return this.getObject().getName();
    }


}





