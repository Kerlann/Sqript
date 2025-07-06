package fr.nico.sqript.structures;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformedPattern {

    Pattern pattern;

    int marksCount; //Number of (¦n) marks in the expression
    int argsCount; //Number of {type} arguments in the expression


    /**
     * Whether this pattern can match any expression. In this case it will be checked only if types are *strictly* matching.
     */
    boolean greedy;
    /**
     * The parameters definitions of each defined parameters in this pattern. Ex: a {item|block} to {player} will have a definition of [[item,block], [player]]
     */
    ScriptParameterDefinition[][] parameterDefinitions = new ScriptParameterDefinition[0][0];
    /**
     * The return type of this pattern.
     */
    private Class<? extends ScriptElement> returnType;

    public TransformedPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public TransformedPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public TransformedPattern(Pattern pattern, int marksCount, int argsCount) {
        this.pattern = pattern;
        this.marksCount = marksCount;
        this.argsCount = argsCount;
    }

    public TransformedPattern(String pattern, int marksCount, int argsCount, ScriptParameterDefinition[][] parameterDefinitions) {
        this.pattern = Pattern.compile(pattern);
        this.marksCount = marksCount;
        this.argsCount = argsCount;
        this.parameterDefinitions = parameterDefinitions;
    }

    public int getArgsCount() {
        return argsCount;
    }

    public Class[] getValidTypes(int parameterIndex) {
        parameterIndex = Math.min(parameterIndex, parameterDefinitions.length - 1);
        Class[] validTypes = new Class[parameterDefinitions[parameterIndex].length];
        for (int i = 0; i < parameterDefinitions[parameterIndex].length; i++) {
            validTypes[i] = parameterDefinitions[parameterIndex][i].getTypeClass();
        }
        return validTypes;
    }

    public boolean isGreedy() {
        return greedy;
    }

    public void setGreedy(boolean greedy) {
        this.greedy = greedy;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public ScriptParameterDefinition[][] getTypes() {
        return parameterDefinitions;
    }

    /**
     * Returns a binary sequence on which each nth bit will be 1 if the nth mark is validated
     *
     * @param line The line to check marks
     * @return
     */
    public int getAllMarks(String line) {
        //System.out.println("Getting all marks for : "+line);
        //System.out.println("Pattern is : "+getPattern().pattern());
        int r = 0;
        Matcher m = getPattern().matcher(line);
        m.find();
        //System.out.println("Mark count is : "+marksCount);

        for (int i = 1; i <= marksCount; i++) {
            try {
                //System.out.println("Checking for mark : "+i);
                if (m.group("m" + i) != null) {
                    //System.out.println("m"+i+" group is not null");
                    r = r | (1 << i);
                    //System.out.println(Integer.toBinaryString(r));
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
        //System.out.println("Marks are : "+ Integer.toBinaryString(r));
        return r;
    }

    /**
     * Extracts the arguments of a String using this pattern.
     *
     * @param match
     * @return
     */
    public String[] getAllArguments(String match) {
        String[] r = new String[getArgsCount()];
        Matcher m = pattern.matcher(match);
        //System.out.println("Getting all arguments for : "+match+" as "+pattern.pattern());
        if (m.find()) {
            //System.out.println("Found");
            for (int i = 0; i < argsCount; i++) {
                String s = null;
                try {
                    s = m.group("a" + i);
                    //System.out.println("Adding : "+s);
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
                r[i] = s;
            }
        }
        return r;
    }

    /**
     * Vérifie si le pattern correspond au texte donné.
     */
    public boolean matches(String text) {
        if (text == null || pattern == null) {
            return false;
        }
        
        try {
            boolean result = pattern.matcher(text).matches();
            if (result) {
                fr.nico.sqript.ScriptManager.log.debug("Pattern '{}' matches text '{}'", pattern.pattern(), text);
            }
            return result;
        } catch (Exception e) {
            fr.nico.sqript.ScriptManager.log.warn("Error matching pattern '{}' against text '{}': {}", 
                pattern.pattern(), text, e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si le pattern trouve une correspondance dans le texte donné.
     */
    public boolean find(String text) {
        return pattern.matcher(text).find();
    }

    public Class<? extends ScriptElement> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<? extends ScriptElement> type) {
        this.returnType = type;
    }

    @Override
    public String toString() {
        return "TransformedPattern{" +
                "pattern=" + pattern +
                ", returnType=" + returnType +
                ", parameterDefinitions=" + Arrays.deepToString(parameterDefinitions) +
                '}';
    }
}





