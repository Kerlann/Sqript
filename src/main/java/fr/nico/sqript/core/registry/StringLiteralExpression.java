package fr.nico.sqript.core.registry;

import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.primitive.TypeString;

/**
 * Expression simple pour représenter une chaîne littérale
 */
public class StringLiteralExpression extends ScriptExpression {
    private final String value;

    public StringLiteralExpression(String value) {
        this.value = value;
    }

    @Override
    public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
        return new TypeString(value);
    }

    @Override
    public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
        return false; // Les chaînes littérales ne peuvent pas être modifiées
    }

    @Override
    public Class getReturnType() {
        return TypeString.class;
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
