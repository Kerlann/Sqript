package fr.nico.sqript.structures;

@FunctionalInterface
public interface ITypeParser<T extends ScriptElement<?>, U> {

    U parse(T type);

}





