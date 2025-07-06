package fr.nico.sqript.function;


import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.meta.Native;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.types.ScriptType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ScriptNativeFunction {

    int matchedDefinition;
    Class returnType;
    int nbParameters;
    String funcName;
    String[] parametersTypeName;
    Class[] parametersType;
    public ScriptNativeFunction(int matchedDefinition) {
        //System.out.println("Instanciating a function : "+this.getClass().getSimpleName());
        this.matchedDefinition = matchedDefinition;
        //System.out.println("Matched defintion is : "+matchedDefinition);
        Native fun = this.getClass().getAnnotation(Native.class);
        Pattern p = Pattern.compile("(^[\\w]+)");
        Matcher m = p.matcher(fun.definitions()[matchedDefinition]);
        if (m.find()) {
            String funcname = m.group(1);
            //System.out.println("Funcname is : "+funcname);
            nbParameters = fun.definitions()[matchedDefinition].split(",").length;
            parametersTypeName = fun.definitions()[matchedDefinition].split(":")[0].split(",");
            for (int i = 0; i < parametersTypeName.length; i++) {
                parametersTypeName[i] = parametersTypeName[i].replace("(", "").replace(")", "").replace(funcname, "");
            }
            //System.out.println("Parameters type are : " + Arrays.toString(parametersTypeName));

            String typeName = fun.definitions()[matchedDefinition].split(":")[1];
            //System.out.println("TypeName is : " + typeName);

            parametersType = new Class[parametersTypeName.length];
            for (int i = 0; i < parametersType.length; i++) {
                parametersType[i] = ScriptRegistry.parseType(parametersTypeName[i]);
                //System.out.println("Parameters "+i+"th is a "+parametersType[i].getSimpleName());
            }
            returnType = ScriptRegistry.parseType(typeName);
            //System.out.println("Return type is : "+returnType);
        }
    }

    public String getFuncName() {
        return funcName;
    }

    public int getNbParameters() {
        return nbParameters;
    }

    public String[] getParametersTypeString() {
        return parametersTypeName;
    }

    public Class[] getParametersType() {
        return parametersType;
    }

    public abstract ScriptType get(ScriptContext context, ScriptType... parameters);

    public Class<? extends ScriptElement> getReturnType() {
        return returnType;
    }
}





