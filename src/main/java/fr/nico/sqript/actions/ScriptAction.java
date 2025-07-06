package fr.nico.sqript.actions;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.compilation.CompilationResult;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Action;
import fr.nico.sqript.meta.ActionDefinition;
import fr.nico.sqript.structures.IScript;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.TypeNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ScriptAction extends IScript {

    private List<ScriptExpression> parameters;
    private int matchedIndex;
    private int marks;

    public ScriptAction() {
    }

    public List<ScriptExpression> getParameters() {
        return parameters;
    }

    public void setParameters(List<ScriptExpression> parameters) {
        this.parameters = parameters;
    }

    public ScriptExpression getParameter(int index) {
        return getParameters().get(index - 1);
    }

    public Object getParameter(int index, ScriptContext context) throws ScriptException {
        return getParameters().get(index - 1).get(context).getObject();
    }

    public <U, T extends ScriptElement<U>> U getParsed(int index, ScriptContext context, Class<T> to) throws ScriptException {
        return ScriptManager.parse(getParameters().get(index - 1).get(context), to).getObject();
    }

    public <U, T extends ScriptElement<U>> U getParsed(int index, ScriptContext context, Class<T> to, U defaultValue) throws ScriptException {
        ScriptType<?> type = getParameters().get(index - 1).get(context);
        return (type == null || (type instanceof TypeNull)) ? defaultValue : ScriptManager.parse(type, to).getObject();
    }

    public <T> T getParameterOrDefault(ScriptExpression parameter, T defaultValue, ScriptContext context) throws ScriptException {
        ScriptType result;
        return (parameter == null || ((result = parameter.get(context)) instanceof TypeNull)) ? defaultValue : (T) result.getObject();
    }

    public <V, T extends ScriptElement<?>, U extends ScriptElement<V>> V getParameterOrDefault(int index, V defaultValue, ScriptContext context, Class<U> typeClass) throws ScriptException {
        ScriptType result;
        ScriptExpression parameter = getParameter(index);
        T value = (parameter == null || ((result = parameter.get(context)) instanceof TypeNull)) ? null : (T) result;
        if (value == null)
            return defaultValue;
        else
            return ScriptManager.parse(value, typeClass).getObject();
    }

    public int getParametersSize() {
        return parameters.size();
    }

    public int getMatchedIndex() {
        return matchedIndex;
    }

    public void setMatchedIndex(int matchedIndex) {
        this.matchedIndex = matchedIndex;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public boolean getMarkValue(int mark) {
        return marks >> mark == 1;
    }

    public String getMatchedName() {
        return this.getClass().getAnnotation(Action.class).features()[getMatchedIndex()].name();
    }

    @Override
    public void execute(ScriptContext context) throws ScriptException {

    }

    public void execute(ScriptContext context, ScriptType[] parameters) throws ScriptException {
    }

    public ScriptType[] evaluate(ScriptContext context) throws ScriptException {
        ScriptType[] result = new ScriptType[getParameters().size()];
        int parameterIndex = 0;
        for (ScriptExpression expression : getParameters()) {
            parameterIndex++;
            result[parameterIndex] = (expression.get(context));
        }
        return result;
    }

    public void build(ScriptToken line, ScriptCompilationContext compileGroup, List<String> parameters, int matchedIndex, int marks) throws Exception {
        List<ScriptExpression> expressions = new ArrayList<>(parameters.size());
        //System.out.println("Building action for line : "+line+", parameters are :"+ Arrays.toString(parameters.toArray(new String[0])));
        //System.out.println("Marks are : "+Integer.toBinaryString(marks));
        String[] strings = fr.nico.sqript.core.compilation.MigrationAdapter.extractStrings(line.getText());
        //System.out.println("for line : "+line+" marks are : "+Integer.toBinaryString(marks));
        ActionDefinition actionDefinition = ScriptManager.getDefinitionFromAction(this.getClass());

        for (int i = 0; i < parameters.size(); i++) {
            ScriptManager.log.debug("Processing parameter {}: '{}'", i, parameters.get(i));
            String parameter = parameters.get(i);
            if (parameter == null) {
                expressions.add(null);
                continue;
            }

            // Utiliser uniquement le nouveau système moderne (HybridEngine)
            ScriptExpression e = null;
            try {
                CompilationResult<fr.nico.sqript.expressions.ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(line.with(parameter), compileGroup, ScriptElement.class);
                if (result != null && result.isSuccess()) {
                    e = result.getResult();
                    ScriptManager.log.debug("Successfully parsed parameter {} as: {}", i, e.getClass().getSimpleName());
                } else {
                    String errorMsg = result != null && result.getError() != null ? result.getError().getMessage() : "Unknown compilation error";
                    throw new fr.nico.sqript.core.compilation.ScriptException(line.with(parameter), "Erreur de compilation d'expression dans ScriptAction: " + errorMsg);
                }
            } catch (Exception ex2) {
                throw new fr.nico.sqript.core.compilation.ScriptException(line.with(parameter), "Erreur de compilation d'expression dans ScriptAction : " + ex2.getMessage());
            }

            if (e != null) {
                expressions.add(e);
            } else {
                ScriptManager.log.error("Failed to parse parameter {}: '{}'", i, parameter);
                throw new ScriptException.ScriptUnknownExpressionException(line.with(parameter).withString(strings));
            }
        }
        setParameters(expressions);
        setMatchedIndex(matchedIndex);
        //System.out.println("Built : "+expressions);
        //System.out.println("Settign line to "+line);
        setLine(line);
        setMarks(marks);
    }

    @Override
    public String toString() {
        return "";
    }
}





