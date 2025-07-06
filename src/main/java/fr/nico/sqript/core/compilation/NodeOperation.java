package fr.nico.sqript.core.compilation;

import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.structures.OperatorDefinition;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.structures.ScriptOperator;
import fr.nico.sqript.types.primitive.TypeBoolean;

/**
 * Nœud représentant une opération dans l'arbre syntaxique abstrait.
 * Cette classe remplace l'ancienne NodeOperation du package compiling.
 */
public class NodeOperation extends Node {

    private ScriptOperator operator;
    private ScriptRegistry registry; // Référence au nouveau système de registre

    public NodeOperation(Node[] children, ScriptOperator operator) {
        super(children);
        this.operator = operator;
    }

    public NodeOperation(ScriptOperator operator) {
        this.operator = operator;
    }

    public NodeOperation(Node[] children, ScriptOperator operator, ScriptRegistry registry) {
        super(children);
        this.operator = operator;
        this.registry = registry;
    }

    public ScriptOperator getOperator() {
        return operator;
    }

    public void setOperator(ScriptOperator operator) {
        this.operator = operator;
    }

    public void setRegistry(ScriptRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String toString() {
        return operator.toString() + super.toString();
    }

    @Override
    public Class<? extends ScriptElement> getReturnType() {
        if (getChildren() == null || getChildren().length == 0) {
            return ScriptElement.class;
        }

        if (getChildren().length == 1) {
            if (operator == ScriptOperator.NOT)
                return TypeBoolean.class;

            if (registry != null) {
                OperatorDefinition operatorDefinition = registry.findUnaryOperation(getChildren()[0].getReturnType(), operator);
                if (operatorDefinition != null)
                    return operatorDefinition.getReturnType();
            }
            return ScriptElement.class;

        } else if (getChildren().length == 2) {
            if (getChildren()[1] == null || getChildren()[0] == null)
                return null;

            if (registry != null) {
                // Recherche d'opération binaire
                OperatorDefinition binaryOp1 = registry.findBinaryOperation(
                        getChildren()[0].getReturnType(),
                        getChildren()[1].getReturnType(),
                        operator
                );
                if (binaryOp1 != null)
                    return binaryOp1.getReturnType();

                // Recherche inversée
                OperatorDefinition binaryOp2 = registry.findBinaryOperation(
                        getChildren()[1].getReturnType(),
                        getChildren()[0].getReturnType(),
                        operator
                );
                if (binaryOp2 != null)
                    return binaryOp2.getReturnType();
            }

            // Opérateurs de comparaison retournent toujours boolean
            if (operator == ScriptOperator.MTE || operator == ScriptOperator.MT ||
                    operator == ScriptOperator.LTE || operator == ScriptOperator.LT ||
                    operator == ScriptOperator.OR || operator == ScriptOperator.AND ||
                    operator == ScriptOperator.NOT || operator == ScriptOperator.EQUAL ||
                    operator == ScriptOperator.NOT_EQUAL) {
                return TypeBoolean.class;
            }
        }
        return null;
    }
} 




