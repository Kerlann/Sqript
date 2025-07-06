package fr.nico.sqript.expressions;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.meta.Expression;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.types.ScriptType;


/**
 * Something that returns a ScriptElement, according to parameters
 * <br>Ex : inventory of player -> ExprItemContainer return TypeArray<TypeItem>
 * <br>Ex : item in hand of player -> ExprItem return TypeItem
 * <br>Ex : 5+9*9 -> ExprCompiledEvaluation return TypeNumber
 */
public abstract class ScriptExpression {

    int marks;
    ScriptToken line;

    private Class<? extends ScriptElement> returnType = ScriptElement.class;
    private int matchedIndex;

    /**
     * Default implementation of expressions returns null, because return type is get with patterns
     * <br>Only useful with special expressions which are not registered commonly
     *
     * @return The return type of this expression.
     */
    public Class<? extends ScriptElement> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<? extends ScriptElement> returnType) {
        this.returnType = returnType;
    }

    public ScriptType get(ScriptContext context) throws ScriptException {
        return this.get(context, new ScriptType[0]);
    }

    public <T extends ScriptType<?>> T get(ScriptContext context, Class<T> type) throws ScriptException {
        return ScriptManager.parse(get(context, new ScriptType[0]), type);
    }

    public boolean set(ScriptContext context, ScriptType to) throws ScriptException {
        return this.set(context, to, new ScriptType[0]);
    }

    public abstract ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException;

    public abstract boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException; //True if it can be set to another value, or false.

    public <T> T getParameterOrDefault(ScriptType<T> parameter, T defaultValue) {
        return parameter == null ? defaultValue : parameter.getObject();
    }


    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public int getMatchedIndex() {
        return matchedIndex;
    }

    public void setMatchedIndex(int matchedIndex) {
        this.matchedIndex = matchedIndex;
    }

    public String getMatchedName() {
        return this.getClass().getAnnotation(Expression.class).features()[getMatchedIndex()].name();
    }

    public ScriptToken getLine() {
        return line;
    }

    public void setLine(ScriptToken line) {
        this.line = line;
    }

    /**
     * Executed during interpretation time, the parsing check of this expression will be ignored if this method returns false. Use it as a way to restrain the possible interpretation of your pattern or to check additional conditions that regular expressions cannot.
     *
     * @param parameters The given parameters of this event instance.
     * @return True if the given configuration should come to a correct instance of this expression (for an example, check if resources give the good type of object).
     */
    public boolean validate(String[] parameters, ScriptToken line) {
        return true;
    }

    /**
     * Debug purposes.
     *
     * @return A visual description of this expression.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getMatchedIndex();
    }

}





