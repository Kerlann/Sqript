package fr.nico.sqript.expressions;


import fr.nico.sqript.meta.Expression;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.TypeDate;

@Expression(name = "Date Expressions",
        features = {
                @Feature(name = "Year", description = "Returns the given amount of years.", examples = "1 year and 5 months", pattern = "{number} year[s] [[and] {date}]", type = "date"),
                @Feature(name = "Month", description = "Returns the given amount of months.", examples = "1 month and 5 weeks", pattern = "{number} month[s] [[and] {date}]", type = "date"),
                @Feature(name = "Day", description = "Returns the given amount of days.", examples = "1 day and 5 hours", pattern = "{number} day[s] [[and] {date}]", type = "date"),
                @Feature(name = "Hour", description = "Returns the given amount of hours.", examples = "1 hour and 5 minutes", pattern = "{number} hour[s] [[and] {date}]", type = "date"),
                @Feature(name = "Minute", description = "Returns the given amount of minutes.", examples = "1 minute and 5 seconds", pattern = "{number} minute[s] [[and] {date}]", type = "date"),
                @Feature(name = "Second", description = "Returns the given amount of seconds.", examples = "1 second and 5 ticks", pattern = "{number} second[s] [[and] {date}]", type = "date"),
                @Feature(name = "Tick", description = "Returns the given amount of ticks.", examples = "1 tick", pattern = "{number} tick[s] [[and] {date}]", type = "date"),
                @Feature(name = "Now", description = "Returns the current date.", examples = "now", pattern = "now", type = "date")
        })
public class ExprDate extends ScriptExpression {


    @Override
    public ScriptType get(ScriptContext context, ScriptType[] parameters) {
        //System.out.println("Date parameters :" + Arrays.toString(parameters));
        
        // Protection contre l'ArrayIndexOutOfBoundsException
        if (parameters == null) {
            fr.nico.sqript.ScriptManager.log.error("ExprDate.get() called with null parameters array");
            return new TypeDate(System.currentTimeMillis()); // Fallback sur "now"
        }
        
        int matchedIndex = getMatchedIndex();
        
        // Cas spécial pour "now" qui ne nécessite pas de paramètres
        if (matchedIndex == 7) {
            return new TypeDate(System.currentTimeMillis());
        }
        
        // Vérifier que nous avons au moins un paramètre pour les autres cas
        if (parameters.length == 0) {
            fr.nico.sqript.ScriptManager.log.error("ExprDate.get() called with empty parameters array for matchedIndex {}", matchedIndex);
            return new TypeDate(System.currentTimeMillis()); // Fallback sur "now"
        }
        
        // Vérifier que le premier paramètre n'est pas null
        if (parameters[0] == null) {
            fr.nico.sqript.ScriptManager.log.error("ExprDate.get() called with null first parameter for matchedIndex {}", matchedIndex);
            return new TypeDate(System.currentTimeMillis()); // Fallback sur "now"
        }

        switch (matchedIndex) {
            case 0:
                int amount = ((Double) parameters[0].getObject()).intValue();
                return new TypeDate(amount * 20L * 60 * 60 * 24 * 7 * 365);
            case 1:
                amount = ((Double) parameters[0].getObject()).intValue();
                return new TypeDate((long) amount * 20 * 60 * 60 * 24 * 7 * 30);
            case 2:
                amount = ((Double) parameters[0].getObject()).intValue();
                return new TypeDate((long) amount * 20 * 60 * 60 * 24 * 7);
            case 3:
                amount = ((Double) parameters[0].getObject()).intValue();
                return new TypeDate((long) amount * 20 * 60 * 60 * 24);
            case 4:
                amount = ((Double) parameters[0].getObject()).intValue();
                return new TypeDate((long) amount * 20 * 60 * 60);
            case 5:
                amount = ((Double) parameters[0].getObject()).intValue();
                return new TypeDate((long) amount * 20 * 60);
            case 6:
                amount = ((Double) parameters[0].getObject()).intValue();
                return new TypeDate((long) amount * 20);
            case 7:
                return new TypeDate(System.currentTimeMillis());
            default:
                fr.nico.sqript.ScriptManager.log.error("ExprDate.get() called with unexpected matchedIndex {}", matchedIndex);
                return new TypeDate(System.currentTimeMillis()); // Fallback sur "now"
        }
    }

    @Override
    public boolean set(ScriptContext context, ScriptType to, ScriptType[] parameters) {
        return false;
    }
}





