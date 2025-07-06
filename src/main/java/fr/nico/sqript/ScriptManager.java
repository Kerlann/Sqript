package fr.nico.sqript;

import com.google.gson.*;
import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.blocks.*;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.events.EvtOnScriptLoad;
import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.forge.SqriptForge;
import fr.nico.sqript.forge.common.ScriptResourceLoader;
import fr.nico.sqript.function.ScriptNativeFunction;
import fr.nico.sqript.meta.*;
import fr.nico.sqript.network.ScriptNetworkManager;
import fr.nico.sqript.structures.*;
import fr.nico.sqript.types.primitive.PrimitiveType;
import fr.nico.sqript.types.primitive.TypeBoolean;
import fr.nico.sqript.types.primitive.TypeString;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Gestionnaire principal des scripts Sqript.
 * Migré vers le nouveau système ScriptRegistry pour une meilleure performance et maintenabilité.
 */
public class ScriptManager {

    public static final String version = "1.0";
    // Contexte global, les variables qu'il contient sont sauvegardées à l'arrêt et chargées au démarrage
    public static final ScriptContext GLOBAL_CONTEXT = new ScriptContext();
    public static final boolean FULL_DEBUG = true;
    public static File scriptDir;
    public static Logger log;
    // Stocke tous les scripts
    public static List<ScriptInstance> scripts = new ArrayList<>();
    // Commandes
    public static List<ScriptBlockCommand> commands = new ArrayList<>();

    // === ACCÈS AU REGISTRE MODERNE ===
    // Flag de rechargement
    public static boolean RELOADING = false;

    // === MÉTHODES D'ACCÈS AUX DÉFINITIONS (via ScriptRegistry) ===

    /**
     * Retourne le registre des scripts moderne.
     */
    public static ScriptRegistry getRegistry() {
        return SqriptForge.getCoreRegistry();
    }

    public static List<EventDefinition> getEvents() {
        return getRegistry() != null ? getRegistry().getEvents() : new ArrayList<>();
    }

    public static List<ActionDefinition> getActions() {
        return getRegistry() != null ? getRegistry().getActions() : new ArrayList<>();
    }

    public static List<BlockDefinition> getBlocks() {
        return getRegistry() != null ? getRegistry().getBlocks() : new ArrayList<>();
    }

    public static Map<ExpressionIdentifier, ExpressionDefinition> getExpressions() {
        return getRegistry() != null ? getRegistry().getExpressions() : new HashMap<>();
    }

    public static List<LoopDefinition> getLoops() {
        return getRegistry() != null ? getRegistry().getLoops() : new ArrayList<>();
    }

    public static Map<Class<? extends ScriptElement<?>>, TypeDefinition> getTypes() {
        return getRegistry() != null ? getRegistry().getTypes() : new HashMap<>();
    }

    public static Map<Class<? extends PrimitiveType<?>>, TypeDefinition> getPrimitives() {
        return getRegistry() != null ? getRegistry().getPrimitives() : new HashMap<>();
    }

    public static Map<Class<? extends ScriptNativeFunction>, NativeDefinition> getNativeFunctions() {
        return getRegistry() != null ? getRegistry().getNativeFunctions() : new HashMap<>();
    }

    // === MÉTHODES D'ENREGISTREMENT (déléguées au ScriptRegistry) ===

    public static List<ScriptOperator> getOperators() {
        return getRegistry() != null ? getRegistry().getOperators() : new ArrayList<>();
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerBinaryOperation()
     */
    @Deprecated
    public static void registerBinaryOperation(ScriptOperator o, Class a, Class b, Class<? extends ScriptElement<?>> returnType, IOperation operation) {
        registerBinaryOperation(o, a, b, returnType, operation, 0);
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerBinaryOperation()
     */
    @Deprecated
    public static void registerBinaryOperation(ScriptOperator o, Class a, Class b, Class<? extends ScriptElement<?>> returnType, IOperation operation, int priority) {
        if (getRegistry() != null) {
            getRegistry().registerBinaryOperation(o, a, b, returnType, operation, priority);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerTypeParser()
     */
    @Deprecated
    public static <T extends ScriptElement<?>, U extends ScriptElement<?>> void registerTypeParser(Class<T> from, Class<U> to, ITypeParser<T, U> parser) {
        registerTypeParser(from, to, parser, 0);
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerTypeParser()
     */
    @Deprecated
    public static <T extends ScriptElement<?>, U extends ScriptElement<?>> void registerTypeParser(Class<T> from, Class<U> to, ITypeParser<T, U> parser, int priority) {
        if (getRegistry() != null) {
            getRegistry().registerTypeParser(from, to, parser, priority);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerUnaryOperation()
     */
    @Deprecated
    public static void registerUnaryOperation(ScriptOperator o, Class<? extends ScriptElement<?>> a, Class<? extends ScriptElement<?>> returnType, IOperation operation) {
        if (getRegistry() != null) {
            getRegistry().registerUnaryOperation(o, a, returnType, operation);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerExpression()
     */
    @Deprecated
    public static void registerExpression(Class<? extends ScriptExpression> expressionClass, String name, int priority, Feature... features) throws Exception {
        if (getRegistry() != null) {
            getRegistry().registerExpression(expressionClass, name, priority, features);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerNativeFunction()
     */
    @Deprecated
    public static void registerNativeFunction(Class<? extends ScriptNativeFunction> func, String name, String[] definitions, String[] description, String[] example) {
        if (getRegistry() != null) {
            getRegistry().registerNativeFunction(func, name, definitions, description, example);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerType()
     */
    @Deprecated
    public static void registerType(Class<? extends ScriptElement<?>> type, String name) throws Exception {
        if (getRegistry() != null) {
            getRegistry().registerType(type, name);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerPrimitive()
     */
    @Deprecated
    public static void registerPrimitive(Class<? extends PrimitiveType<?>> type, String name, String... patterns) {
        if (getRegistry() != null) {
            getRegistry().registerPrimitive(type, name, patterns);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerLoop()
     */
    @Deprecated
    public static void registerLoop(Class<? extends ScriptEvent> cls, String name, String pattern, Side side, int priority) {
        if (getRegistry() != null) {
            // Vérification que la classe hérite bien de ScriptLoop
            if (ScriptLoop.class.isAssignableFrom(cls)) {
                // Cast sécurisé avec vérification de type
                Class<? extends ScriptLoop> loopClass = cls.asSubclass(ScriptLoop.class);
                getRegistry().registerLoop(loopClass, name, pattern, side, priority);
            } else {
                log.warn("Cannot register loop: {} does not extend ScriptLoop", cls.getSimpleName());
            }
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerEvent()
     */
    @Deprecated
    public static void registerEvent(Class<? extends ScriptEvent> cls, Feature feature, Feature[] accesors) throws Exception {
        if (getRegistry() != null) {
            getRegistry().registerEvent(cls, feature, accesors);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerAction()
     */
    @Deprecated
    public static void registerAction(Class<? extends ScriptAction> cls, String name, int priority, Feature... features) throws Exception {
        if (getRegistry() != null) {
            getRegistry().registerAction(cls, name, priority, features);
        }
    }

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerBlock()
     */
    @Deprecated
    public static void registerBlock(Class<? extends ScriptBlock> cls, Feature feature, Feature[] fields, boolean reloadable) {
        if (getRegistry() != null) {
            getRegistry().registerBlock(cls, feature, fields, reloadable);
        }
    }

    // === MÉTHODES D'ACCÈS AUX PARSERS (via ScriptRegistry) ===

    /**
     * @deprecated Utiliser directement SqriptForge.getCoreRegistry().registerOperator()
     */
    @Deprecated
    public static void registerOperator(ScriptOperator operator) {
        if (getRegistry() != null) {
            getRegistry().registerOperator(operator);
        }
    }

    public static <T extends ScriptElement<?>, U extends ScriptElement<?>> TypeParserDefinition<T, U> getParser(T from, Class<U> to) {
        if (getRegistry() != null) {
            return getRegistry().findTypeParser((Class<T>) from.getClass(), to);
        }
        return null;
    }

    public static boolean isParsable(Class from, Class toType) {
        if (getRegistry() != null) {
            return getRegistry().findTypeParser(from, toType) != null;
        }
        return false;
    }

    public static <T extends ScriptElement<?>, U extends ScriptElement<?>> U parseOrDefault(T from, Class<U> toType, U defaultValue) {
        if (from == null)
            return defaultValue;
        U result = parse(from, toType);
        if (result != null)
            return result;
        else
            return defaultValue;
    }

    // === MÉTHODES D'ACCÈS AUX OPÉRATIONS (via ScriptRegistry) ===

    public static <T extends ScriptElement<?>, U extends ScriptElement<?>> U parse(T from, Class<U> toType) {
        if (from.getClass() == toType || from.getClass().isAssignableFrom(toType))
            return (U) from;

        if (getRegistry() != null) {
            TypeParserDefinition<T, U> typeParserDefinition = getRegistry().findTypeParser((Class<T>) from.getClass(), toType);
            if (typeParserDefinition != null)
                return typeParserDefinition.getParser().parse(from);
        }

        if (toType == TypeString.class)
            return (U) new TypeString(from.toString());
        else
            return null;
    }

    public static OperatorDefinition getBinaryOperation(Class<? extends ScriptElement> a, Class<? extends ScriptElement> b, ScriptOperator o) {
        if (getRegistry() != null) {
            return getRegistry().findBinaryOperation(a, b, o);
        }
        return null;
    }

    // === MÉTHODES D'ACCÈS AUX DÉFINITIONS ===

    public static OperatorDefinition getUnaryOperation(Class a, ScriptOperator o) {
        if (getRegistry() != null) {
            return getRegistry().findUnaryOperation(a, o);
        }
        return null;
    }

    public static ScriptInstance getScriptFromName(String name) {
        for (ScriptInstance i : scripts) {
            if (i.getName().equals(name)) return i;
        }
        return null;
    }

    public static ActionDefinition getDefinitionFromAction(Class<? extends ScriptAction> cls) {
        if (getRegistry() != null) {
            return getRegistry().findActionDefinition(cls);
        }
        return null;
    }

    // === CONFIGURATION ===

    public static ExpressionDefinition getDefinitionFromExpression(Class<? extends ScriptExpression> cls) {
        if (getRegistry() != null) {
            return getRegistry().findExpressionDefinition(cls);
        }
        return null;
    }

    // === INITIALISATION ===

    public static void preInit(File scriptsFolder) {
        log = LogManager.getLogger("Sqript");
        log.info("Sqript" + " version " + ScriptManager.version + " is running, by Nico-");
        scriptDir = scriptsFolder;
    }

    public static void init() {
        try {
            ScriptDataManager.load();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialiser ScriptDecoder avec le registre
        // ScriptDecoder supprimé - fonctionnalités migrées vers ScriptRegistry et SqriptUtils

        // Initialisation des opérateurs intégrés (via le registre)
        initOperators();
        buildOperators();
        // Initialisation des patterns d'opérateurs migrée vers ScriptRegistry

        try {
            ScriptManager.log.info("Loading scripts.");
            loadScripts(scriptDir);
        } catch (Throwable e) {
            ScriptManager.log.error(e.getMessage());
        }

        SqriptForge.registerCommands();
    }

    public static void stop() {
        try {
            ScriptDataManager.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === OPÉRATEURS INTÉGRÉS ===

    private static void initOperators() {
        if (getRegistry() != null) {
            getRegistry().registerOperator(ScriptOperator.PLUS_UNARY);
            getRegistry().registerOperator(ScriptOperator.MINUS_UNARY);
            getRegistry().registerOperator(ScriptOperator.MULTIPLY);
            getRegistry().registerOperator(ScriptOperator.MOD);
            getRegistry().registerOperator(ScriptOperator.QUOTIENT);
            getRegistry().registerOperator(ScriptOperator.DIVIDE);
            getRegistry().registerOperator(ScriptOperator.ADD);
            getRegistry().registerOperator(ScriptOperator.SUBTRACT);
            getRegistry().registerOperator(ScriptOperator.MTE);
            getRegistry().registerOperator(ScriptOperator.LTE);
            getRegistry().registerOperator(ScriptOperator.LT);
            getRegistry().registerOperator(ScriptOperator.MT);
            getRegistry().registerOperator(ScriptOperator.NOT_EQUAL);
            getRegistry().registerOperator(ScriptOperator.EQUAL);
            getRegistry().registerOperator(ScriptOperator.NOT);
            getRegistry().registerOperator(ScriptOperator.AND);
            getRegistry().registerOperator(ScriptOperator.EXP);
            getRegistry().registerOperator(ScriptOperator.OR);
            getRegistry().registerOperator(ScriptOperator.FACTORIAL);
        }
    }

    private static void buildOperators() {
        if (getRegistry() != null) {
            for (ScriptOperator s : getRegistry().getOperators()) {
                if (s.word && !s.unary) {
                    ScriptRegistry.operators_list.add("(\\)\\s+|^)" + Pattern.quote(s.symbol) + "(\\s+\\()");
                } else if (s.word) {
                    ScriptRegistry.operators_list.add("(\\s+|^)" + Pattern.quote(s.symbol) + "(\\s+\\()");
                } else {
                    ScriptRegistry.operators_list.add(Pattern.quote(s.symbol));
                }
                ScriptRegistry.operators_pattern.add(Pattern.compile(ScriptRegistry.operators_list.get(ScriptRegistry.operators_list.size() - 1)));
            }
        }
    }

    // === GESTION DES ERREURS ===

    public static void handleError(ScriptToken line, Throwable throwable) {
        ScriptManager.log.error("Error while loading " + line.getScriptInstance().getName() + " : ");
        if (throwable instanceof ScriptException) {
            for (String s : throwable.getMessage().split("\n"))
                ScriptManager.log.error(s);
        }
        if (ScriptManager.FULL_DEBUG)
            throwable.printStackTrace();
    }

    // === CHARGEMENT DES SCRIPTS ===

    private static void loadFolder(File folder) throws Throwable {
        ScriptException.ScriptExceptionList list = new ScriptException.ScriptExceptionList();
        if (FMLCommonHandler.instance().getSide() == net.minecraftforge.fml.relauncher.Side.CLIENT)
            ScriptManager.loadResources();
        for (File f : folder.listFiles()) {
            if (f.isDirectory()) {
                try {
                    loadFolder(f);
                } catch (Exception e) {
                    if (e instanceof ScriptException.ScriptExceptionList) {
                        list.exceptionList.addAll(((ScriptException.ScriptExceptionList) (e)).exceptionList);
                    } else {
                        throw e;
                    }
                }
                if (f.getName().equalsIgnoreCase("sounds")) {
                    for (File file : f.listFiles()) {
                        log.info("Registering sound : " + folder.getName() + ":" + file.getName().split("\\.")[0]);
                        addSoundToJsonFile(new File(folder, "sounds.json"), folder.getName(), file.getName().split("\\.")[0]);
                        SoundEvent event = new SoundEvent(new ResourceLocation(folder.getName() + ":" + file.getName().split("\\.")[0]));
                        event.setRegistryName(folder.getName() + ":" + file.getName().split("\\.")[0]);
                        SqriptForge.soundEvents.add(event);
                    }
                }
            } else if (f.toPath().toString().endsWith(".sq")) {
                log.info("Found script file: " + f.getName());
                try {
                    // Chargeur simple - remplace l'ancien ScriptLoader
                    String name = f.getName().replace(".sq", "");
                    log.info("Creating ScriptInstance for: " + name);
                    ScriptInstance instance = new ScriptInstance(name, f);
                    log.info("Calling script load event for: " + name);
                    instance.callEvent(new ScriptContext(GLOBAL_CONTEXT), new EvtOnScriptLoad(f));
                    scripts.add(instance);
                    log.info("Successfully loaded script: " + name);
                } catch (Exception e) {
                    log.error("Failed to load script: " + f.getName() + " - " + e.getMessage());
                    if (e instanceof ScriptException.ScriptExceptionList) {
                        list.exceptionList.addAll(((ScriptException.ScriptExceptionList) (e)).exceptionList);
                    } else {
                        // Créer une exception de script et l'ajouter à la liste au lieu de l'interrompre
                        ScriptException scriptException = new ScriptException.ScriptSyntaxException(
                                new ScriptToken("", 0, null),
                                "Error loading script " + f.getName() + ": " + e.getMessage()
                        );
                        list.exceptionList.add(scriptException);
                    }
                }
            }
        }
        if (!list.exceptionList.isEmpty()) {
            throw list;
        }
    }

    private static void addSoundToJsonFile(File file, String domain, String name) throws IOException {
        if (!file.exists())
            file.createNewFile();

        //Parsing file and adding new json object
        JsonParser parser = new JsonParser();
        JsonObject obj = new JsonObject();

        try {
            obj = parser.parse(Files.newBufferedReader(file.toPath())).getAsJsonObject();
        } catch (Exception ignored) {
        }

        JsonObject newSound = new JsonObject();
        newSound.add("category", new JsonPrimitive("master"));
        newSound.add("subtitle", new JsonPrimitive("subtitle." + name));
        JsonArray sounds = new JsonArray();
        sounds.add(domain + ":" + name);
        newSound.add("sounds", sounds);
        obj.add(name, newSound);

        //Creating as instance of gson
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        //Writing the new content to the file
        new PrintWriter(file).close();
        PrintWriter printWriter = new PrintWriter(file);
        printWriter.println(gson.toJson(obj));
        printWriter.close();
    }

    public static void loadScripts(File mainFolder) throws Throwable {
        ScriptException.ScriptExceptionList elist = new ScriptException.ScriptExceptionList();
        try {
            loadFolder(mainFolder);
        } catch (ScriptException.ScriptExceptionList e) {
            elist = e;
        }

        if (FMLCommonHandler.instance().getSide() == net.minecraftforge.fml.relauncher.Side.CLIENT)
            loadResources();

        log.info("All scripts are loaded");
        if (!elist.exceptionList.isEmpty())
            throw elist;

        if (FULL_DEBUG) {
            try {
                for (ScriptInstance instance : scripts) {
                    List<ScriptBlock> list = instance.getBlocksOfClass(ScriptBlockEvent.class);
                    list.addAll(instance.getBlocksOfClass(ScriptBlockCommand.class));
                    list.addAll(instance.getBlocksOfClass(ScriptBlockPacket.class));
                    list.addAll(instance.getBlocksOfClass(ScriptBlockTimeLoop.class));
                    list.addAll(instance.getBlocksOfClass(ScriptBlockFunction.class));
                    for (ScriptBlock s : list) {
                        dispScriptTree(s, 0);
                    }
                    log.info("");
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void loadResources() throws IllegalAccessException {
        ScriptResourceLoader resourceLoader = new ScriptResourceLoader();
        Field defaultResourcePacksField = ObfuscationReflectionHelper.findField(Minecraft.getMinecraft().getClass(), "field_110449_ao");
        defaultResourcePacksField.setAccessible(true);
        ((ArrayList) defaultResourcePacksField.get(Minecraft.getMinecraft())).add(resourceLoader);
    }

    // === GESTION DES ÉVÉNEMENTS ===

    public static ScriptContext callEventAndGetContext(ScriptEvent event) throws ScriptException {
        ScriptContext context = new ScriptContext(GLOBAL_CONTEXT);
        context.setReturnValue(new ScriptTypeAccessor(TypeBoolean.FALSE(), ""));
        for (ScriptInstance instance : scripts) {
            context = instance.callEventAndGetContext(context, event);
        }
        return context;
    }

    public static boolean callEvent(ScriptEvent event) {
        Optional<EventDefinition> optional = getEvents().stream().filter(a -> {
            return a.eventClass == event.getClass();
        }).findFirst();
        EventDefinition eventDefinition = null;
        if (optional.isPresent())
            eventDefinition = optional.get();
        boolean result = false;
        if (eventDefinition != null && eventDefinition.getFeature().side().isValid()) {
            ScriptContext context = new ScriptContext(GLOBAL_CONTEXT);
            try {
                for (ScriptInstance script : scripts) {
                    if (script.callEvent(context, event)) {
                        result = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    // === GESTION DES COMMANDES ===

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public static void clearClientCommands() {
        for (ScriptBlockCommand command : commands) {
            System.out.println("Removing command : " + command.getName());
            ClientCommandHandler.instance.getCommands().remove(command.getName());
        }
    }

    public static void clearServerCommands() {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
            for (ScriptBlockCommand command : commands) {
                FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager().getCommands().remove(command.getName());
            }
        }
    }

    // === RECHARGEMENT ===

    public static void reload() throws Throwable {
        RELOADING = true;

        scripts.clear();
        ScriptNetworkManager.clear();

        if (FMLCommonHandler.instance().getSide() == net.minecraftforge.fml.relauncher.Side.CLIENT)
            clearClientCommands();
        clearServerCommands();
        commands.clear();

        ScriptTimer.reload();
        ScriptException.ScriptExceptionList elist = new ScriptException.ScriptExceptionList();
        try {
            loadScripts(scriptDir);
        } catch (ScriptException.ScriptExceptionList e) {
            elist = e;
        }

        SqriptForge.registerCommands();

        RELOADING = false;

        if (!elist.exceptionList.isEmpty())
            throw elist;
    }

    /**
     * Affiche l'arbre de script pour debug
     * Remplace l'ancienne méthode ScriptLoader.dispScriptTree
     */
    public static void dispScriptTree(IScript script, int indent) {
        if (script == null) return;

        String tab = "";
        for (int j = 0; j < indent; j++) tab += "|    ";

        if (script instanceof ScriptBlock) {
            ScriptBlock block = (ScriptBlock) script;
            log.info(tab + block.getClass().getSimpleName() + ": " + block.getHead());
            if (block.getRoot() != null) {
                dispScriptTree(block.getRoot(), indent + 1);
            }
        } else {
            log.info(tab + script.getClass().getSimpleName() + ": " + script);
        }
    }
}





