package fr.nico.sqript.core.compilation;

import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.structures.ScriptElement;

/**
 * Nœud représentant une expression dans l'arbre syntaxique abstrait.
 * Cette classe remplace l'ancienne NodeExpression du package compiling.
 */
public class NodeExpression extends Node {

    private ScriptExpression expression;

    public NodeExpression(Node[] children, ScriptExpression expression) {
        super(children);
        this.expression = expression;
    }

    public NodeExpression(ScriptExpression expression) {
        this.expression = expression;
    }

    public ScriptExpression getExpression() {
        return expression;
    }

    public void setExpression(ScriptExpression expression) {
        this.expression = expression;
    }

    public int getArity() {
        if (getChildren() != null)
            return getChildren().length;
        else
            return 0;
    }

    @Override
    public String toString() {
        return expression.toString() + super.toString();
    }

    @Override
    public Class<? extends ScriptElement> getReturnType() {
        return expression.getReturnType();
    }

    public boolean childrenAreNull() {
        if (getChildren() == null) return true;
        for (Node n : getChildren())
            if (n != null) return false;
        return true;
    }
} 




