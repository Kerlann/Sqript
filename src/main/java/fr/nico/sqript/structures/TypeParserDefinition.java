package fr.nico.sqript.structures;

public class TypeParserDefinition<T extends ScriptElement<?>, U extends ScriptElement<?>> {

    private final int priority;
    private final Class from;
    private final Class to;
    private ITypeParser<T, U> parser;

    public TypeParserDefinition(int priority, ITypeParser<T, U> parser, Class from, Class to) {
        this.priority = priority;
        this.parser = parser;
        this.from = from;
        this.to = to;
    }

    public int getPriority() {
        return priority;
    }

    public ITypeParser<T, U> getParser() {
        return parser;
    }

    public void setParser(ITypeParser parser) {
        this.parser = parser;
    }

    public Class getFrom() {
        return from;
    }

    public Class getTo() {
        return to;
    }
}





