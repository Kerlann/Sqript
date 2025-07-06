package fr.nico.sqript.types.interfaces;

import fr.nico.sqript.types.ScriptType;

public interface IIndexedCollection {

    boolean contains(ScriptType<?> t);

    ScriptType<?> get(int index);

    IIndexedCollection sort(int mode);

    IIndexedCollection copy();

    int size();

}





