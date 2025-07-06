package fr.nico.sqript.blocks;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.SqriptUtils;
import fr.nico.sqript.core.compilation.CompilationResult;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Block;
import fr.nico.sqript.meta.Event;
import fr.nico.sqript.meta.EventDefinition;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.structures.Side;
import fr.nico.sqript.structures.TransformedPattern;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.TypeNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Block(
        feature = @Feature(name = "Event",
                description = "Run code when a specific event is triggered.",
                examples = "on script load:\n" +
                        "    print \"Hello world !\"",
                regex = "^(on|when) .*"),
        fields = {@Feature(name = "side")}
)
public class ScriptBlockEvent extends ScriptBlock {

    // Cache de compilation pour éviter de recompiler les mêmes arguments d'événements
    private static final java.util.Map<String, ScriptExpression> eventArgCache = new java.util.concurrent.ConcurrentHashMap<>();
    // Contexte de compilation réutilisable pour les arguments d'événements
    private static final ScriptCompilationContext EVENT_ARG_CONTEXT = new ScriptCompilationContext();
    public Class<? extends ScriptEvent> eventType;
    public ScriptType[] parameters;
    public int marks;
    //By default, events will launch on server
    public Side side;

    public ScriptBlockEvent(ScriptToken head) throws ScriptException {
        super(head);
        getHead().setText(head.getText().trim().replaceFirst("(^|\\s+)(on|when)\\s+", "")); //Extracting the event parameters
        getHead().setText(head.getText().substring(0, head.getText().length() - 1)); //Removing the last ":"
    }

    @Override
    public void init(ScriptLineBlock block) throws Exception {
        this.eventType = parseEvent(getHead());
        if (eventType == null)
            throw new ScriptException.ScriptUnknownEventException(getHead());
        this.side = eventType.getAnnotation(Event.class).feature().side();
        //System.out.println("Initialising : "+eventType+ " sided : "+side);
        if (!side.isValid())
            return;

        super.init(block);
    }

    @Override
    public void displayTree(int i) {
        ScriptManager.dispScriptTree(getRoot(), i);
    }

    public Class<? extends ScriptEvent> parseEvent(ScriptToken line) throws ScriptException {
        //System.out.println("Parsing event for : "+line);
        line = line.with(line.getText().trim());
        for (EventDefinition eventDefinition : ScriptManager.getEvents()) {
            //System.out.println("Checking for : "+eventDefinition.getEventClass());
            int matchedPatternIndex = -1;
            if ((matchedPatternIndex = eventDefinition.getMatchedPatternIndex(line.getText())) != -1) {
                //Parsing the arguments
                TransformedPattern transformedPattern = eventDefinition.getTransformedPatterns()[matchedPatternIndex];
                String[] arguments = transformedPattern.getAllArguments(line.getText());
                //System.out.println("Parsing event :"+eventDefinition.eventClass.getSimpleName()+" "+arguments.length+" "+Arrays.toString(arguments));
                ScriptType[] parameters = new ScriptType[arguments.length];
                int marks = eventDefinition.getTransformedPatterns()[matchedPatternIndex].getAllMarks(line.getText());
                for (int i = 0; i < arguments.length; i++) {
                    try {
                        if (arguments[i] != null) {
                            ScriptExpression expr = null;

                            // Vérifier le cache d'abord
                            String cacheKey = arguments[i] + ":" + Arrays.toString(transformedPattern.getValidTypes(i));
                            expr = eventArgCache.get(cacheKey);

                            if (expr == null) {
                                // Utiliser le contexte de compilation réutilisable
                                CompilationResult<ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(line.with(arguments[i]), EVENT_ARG_CONTEXT, transformedPattern.getValidTypes(i));
                                if (result != null && result.isSuccess()) {
                                    expr = result.getResult();
                                    // Mettre en cache le résultat
                                    eventArgCache.put(cacheKey, expr);
                                } else {
                                    String errorMsg = result != null && result.getError() != null ? result.getError().getMessage() : "Unknown compilation error";
                                    throw new fr.nico.sqript.core.compilation.ScriptException(line.with(arguments[i]), "Erreur de compilation d'expression dans ScriptBlockEvent: " + errorMsg);
                                }
                            }

                            parameters[i] = expr.get(ScriptContext.fromGlobal());
                        } else parameters[i] = new TypeNull();
                    } catch (Exception e) {
                        throw new fr.nico.sqript.core.compilation.ScriptException(line.with(arguments[i]), "Erreur de compilation d'expression dans ScriptBlockEvent : " + e.getMessage());
                    }
                }
                try {
                    ScriptEvent event = SqriptUtils.rawInstantiation(ScriptEvent.class, eventDefinition.getEventClass());
                    if (event.validate(parameters, marks)) {
                        this.marks = marks;
                        this.parameters = parameters;
                        //System.out.println("Validated");
                        return eventDefinition.getEventClass();
                    } else {
                        //System.out.println("Not validated");
                    }
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                         InvocationTargetException ignored) {
                }
            }
        }
        return null;
    }

    @Override
    protected void load() throws Exception {
        if (fieldDefined("side"))
            side = Side.from(getSubBlock("side").getRawContent());
        if (side != null && !side.isValid()) {
            return;
        }

        ScriptCompilationContext group = new ScriptCompilationContext();
        group.addArray(Arrays.asList(eventType.getAnnotation(Event.class).accessors()));

        setRoot(getMainField().compile(group));
        getScriptInstance().registerBlock(this);

    }

    public ScriptType[] getParameters() {
        return parameters;
    }

    public Class<? extends ScriptEvent> getEventType() {
        return eventType;
    }

    public int getMarks() {
        return marks;
    }

}




