package fr.nico.sqript.expressions;

import fr.nico.sqript.meta.Expression;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.TypeArray;
import fr.nico.sqript.types.primitive.TypeBoolean;
import fr.nico.sqript.types.primitive.TypeNumber;
import fr.nico.sqript.types.primitive.TypeString;

import java.util.ArrayList;
import java.util.Arrays;

@Expression(name = "Strings Expressions",
        features = {
                @Feature(name = "Length of a string", description = "Returns the number of characters in a string.", examples = "length of \"Test\"", pattern = "length of {string}", type = "number"),
                @Feature(name = "Substring of a string", description = "Returns a substring from a string.", examples = "substring of \"Test string\" from 1 to 4 #Returns \"est\"", pattern = "substring of {string} from {number} to {number}", type = "string"),
                @Feature(name = "Split of a string", description = "Returns an array of a split string.", examples = "\"Hello world\" split at each \" \" #Returns [\"Hello\",\"world\"]", pattern = "{string} split at each {string}", type = "array"),
                @Feature(name = "Character of a string", description = "Returns the character at a specific position of a string.", examples = "character at position 2 of \"Hello world\" #Returns \"l\"", pattern = "character at [position] {number} of {string}", type = "array"),
                @Feature(name = "Contains of a string", description = "It returns true if sequence of char values are found in this string otherwise returns false", examples = " \"Test1\" contains \"1\"", pattern = "{string} contains {string}", type = "boolean"),
                @Feature(name = "Replace of a string", description = "Returns a string in which all or part of the matches to a template are replaced by a", examples = "replace all {string} with {string} in {string}", pattern = "replace all {string} with {string} in {string}", type = "string"),

        }
)
public class ExprStrings extends ScriptExpression {

    @Override
    public ScriptType get(ScriptContext context, ScriptType[] parameters) {
        switch(getMatchedIndex()){
            case 0:
                TypeString string = (TypeString) parameters[0];
                return new TypeNumber(string.getObject().length());
            case 1:
                string = (TypeString) parameters[0];
                TypeNumber start = (TypeNumber) parameters[1];
                TypeNumber end = (TypeNumber) parameters[2];
                return new TypeString(string.getObject().substring(start.getObject().intValue(),end.getObject().intValue()));
            case 2:
                string = (TypeString) parameters[0];
                TypeString split = (TypeString) parameters[1];
                ArrayList splits = new ArrayList();
                for(String s : string.getObject().split(split.getObject())){
                    splits.add(new TypeString(s));
                }
                return new TypeArray(splits);
            case 3:
                TypeNumber index = (TypeNumber) parameters[0];
                string = (TypeString) parameters[1];
                return new TypeString(String.valueOf(string.getObject().charAt(index.getObject().intValue())));
            case 4:
                string = (TypeString) parameters[0];
                TypeString string2 = (TypeString) parameters[1];
                return new TypeBoolean(string.getObject().contains(string2.getObject()));
            case 5:
                string = (TypeString) parameters[0];
                string2 = (TypeString) parameters[1];
                TypeString string3 = (TypeString) parameters[2];
                return new TypeString(string3.getObject().replaceAll(string.getObject(), string2.getObject()));
        }
        return null;
    }

    @Override
    public boolean set(ScriptContext context, ScriptType to, ScriptType[] parameters) {
        return false;
    }
}
