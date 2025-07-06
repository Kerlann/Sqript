package fr.nico.sqript.types.interfaces;

public interface IComparable<T> {
    default int compare(T a, T b) {
        return 0;
    }
}





