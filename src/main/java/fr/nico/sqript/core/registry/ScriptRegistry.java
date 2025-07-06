package fr.nico.sqript.core.registry;

import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.blocks.ScriptBlock;
import fr.nico.sqript.core.ScriptComponent;
import fr.nico.sqript.core.compilation.*;
import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.function.ScriptNativeFunction;
import fr.nico.sqript.meta.*;
import fr.nico.sqript.structures.*;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.primitive.PrimitiveType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registre centralisé pour toutes les définitions du moteur Sqript.
 * Remplace la logique d'enregistrement dispersée dans ScriptManager.
 * <p>
 * Thread-safe et optimisé pour les accès concurrents.
 * 
 * @author Nico-
 * @version 1.1
 * @since Phase 1 - Refactoring
 */
public class ScriptRegistry implements ScriptComponent {
    
    // Patterns de capture pour les différents types de données (migré depuis ScriptDecoder)
    public static final String CAPTURE_BETWEEN_QUOTES = "(?:\"((?:[^\"\\\\\\\\]|\\\\.)*)\")";
    public static final String CAPTURE_INTEGER = "(?:\\s*[0-9]+)";
    public static final String CAPTURE_BOOLEAN = "(?:([Tt]rue|[Ff]alse))";
    public static final String CAPTURE_NUMBER = "(?:\\s*[0-9ABCDEFabcdef]*(?:\\.[0-9]*+)?)";
    public static final String CAPTURE_EXPRESSION_LAZY = "(?:.+?)";
    public static final String CAPTURE_EXPRESSION_GREEDY = "(?:.+)";
    public static final String CAPTURE_EXPRESSION_OR_NONE = "(?:.*)";
    // Patterns compilés pour les opérations courantes (migré depuis ScriptDecoder)
    public static final Pattern PATTERN_FUNCTION = Pattern.compile("^\\s*(\\w*)\\((.*)\\)\\s*$");
    public static final Pattern PATTERN_REMOVED_STRING = Pattern.compile("\\{S(\\d*)}");
    public static final Pattern PATTERN_CAPTURE_QUOTES = Pattern.compile("(" + CAPTURE_BETWEEN_QUOTES + ")");
    public static final Pattern PATTERN_VARIABLE = Pattern.compile("^(?:\\$)?\\{(.*)}$");
    public static final Pattern PATTERN_COMPILED_STRING = Pattern.compile("@\\{S(\\d*)}");
    public static final Pattern PATTERN_PERCENT = Pattern.compile("(?<!~)%");
    public static final Pattern PATTERN_MARK = Pattern.compile("\\?<m(\\d*)>");
    public static final Pattern PATTERN_L = Pattern.compile("(?<!\\\\)(L\\{(\\d+)})");
    // Variables pour les opérateurs (migré depuis ScriptDecoder)
    public static final List<String> operators_list = Collections.synchronizedList(new ArrayList<>());
    public static final List<Pattern> operators_pattern = Collections.synchronizedList(new ArrayList<>());
    private static final Logger log = LogManager.getLogger(ScriptRegistry.class);
    // Types et primitives
    private static final Map<Class<? extends ScriptElement<?>>, TypeDefinition> types = new ConcurrentHashMap<>();
    private static final Map<Class<? extends PrimitiveType<?>>, TypeDefinition> primitives = new ConcurrentHashMap<>();
    // Opérateurs
    private static final List<ScriptOperator> operators = Collections.synchronizedList(new ArrayList<>());
    // Collections pour les différents éléments du registre
    private final List<EventDefinition> events = Collections.synchronizedList(new ArrayList<>());
    private final List<ActionDefinition> actions = Collections.synchronizedList(new ArrayList<>());
    private final List<BlockDefinition> blocks = Collections.synchronizedList(new ArrayList<>());
    private final Map<ExpressionIdentifier, ExpressionDefinition> expressions = new ConcurrentHashMap<>();
    private final List<LoopDefinition> loops = Collections.synchronizedList(new ArrayList<>());
    // Fonctions natives
    private final Map<Class<? extends ScriptNativeFunction>, NativeDefinition> nativeFunctions = new ConcurrentHashMap<>();
    private final Map<ScriptOperator, Map<Class, Map<Class, OperatorDefinition>>> binaryOperations = new ConcurrentHashMap<>();
    private final Map<ScriptOperator, Map<Class, OperatorDefinition>> unaryOperations = new ConcurrentHashMap<>();
    // Type parsers
    private final Map<Class, Map<Class, TypeParserDefinition<?, ?>>> typeParsers = new ConcurrentHashMap<>();
    // Parsers personnalisés
    private final List<IScriptParser> customParsers = Collections.synchronizedList(new ArrayList<>());
    // État du registre
    private volatile boolean active = false;

    /**
     * Vérifie si une expression contient des opérateurs
     */
    public static boolean containsOperator(String expression) {
        return operators.stream()
                .anyMatch(op -> expression.contains(op.symbol));
    }

    /**
     * Parse un type depuis une chaîne
     */
    public static Class<? extends ScriptElement> parseType(String type) {
        if (type == null) {
            return ScriptElement.class;
        }

        // Recherche dans les types enregistrés
        for (Map.Entry<Class<? extends ScriptElement<?>>, TypeDefinition> entry : types.entrySet()) {
            if (entry.getValue().name.equals(type)) {
                return entry.getKey();
            }
        }

        // Recherche dans les primitives
        for (Map.Entry<Class<? extends PrimitiveType<?>>, TypeDefinition> entry : primitives.entrySet()) {
            if (entry.getValue().name.equals(type)) {
                return entry.getKey();
            }
        }

        // Mappings pour les types de base manquants
        switch (type.toLowerCase()) {
            case "string":
                return fr.nico.sqript.types.primitive.TypeString.class;
            case "number":
                return fr.nico.sqript.types.primitive.TypeNumber.class;
            case "boolean":
                return fr.nico.sqript.types.primitive.TypeBoolean.class;
            case "array":
                return fr.nico.sqript.types.primitive.TypeArray.class;
            case "element":
                return fr.nico.sqript.types.primitive.TypeElement.class;
            case "entity":
                return fr.nico.sqript.types.primitive.TypeEntity.class;
            case "resource":
                return fr.nico.sqript.types.primitive.TypeResource.class;
            case "world":
                return fr.nico.sqript.types.TypeWorld.class;
            case "player":
                return fr.nico.sqript.types.TypePlayer.class;
            case "item":
                return fr.nico.sqript.types.TypeItemStack.class;
            case "itemtype":
                return fr.nico.sqript.types.TypeItem.class;
            case "block":
                return fr.nico.sqript.types.TypeBlock.class;
            case "file":
                return fr.nico.sqript.types.TypeFile.class;
            case "color":
                return fr.nico.sqript.types.TypeColor.class;
            case "function":
                return fr.nico.sqript.types.TypeFunction.class;
            case "axisalignedbb":
                return fr.nico.sqript.types.TypeAxisAlignedBB.class;
        }

        log.warn("Type '{}' not found in registry, using ScriptElement as fallback", type);
        return ScriptElement.class;
    }

    /**
     * Découpe une expression sur les +, en respectant les guillemets et accolades
     */
    private static List<String> splitByPlusRespectingQuotesAndBraces(String expression) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '"' && (i == 0 || expression.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (!inString) {
                if (c == '{') depth++;
                if (c == '}') depth--;
                if (c == '+' && depth == 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(c);
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    /**
     * Vérifie si les parenthèses sont correctement équilibrées
     */
    public static boolean isParenthesageGood(String line) {
        if (line == null) return true;
        int p = 0, b = 0, c = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '(') p++;
            else if (ch == '[') b++;
            else if (ch == '{') c++;
            else if (ch == ')') p--;
            else if (ch == ']') b--;
            else if (ch == '}') c--;
        }
        return p == 0 && b == 0 && c == 0;
    }

    /**
     * Vérifie si une expression est algébrique
     */
    public static boolean isAlgebraic(String expression) {
        return expression.contains("(") || expression.contains(")") || containsOperator(expression);
    }

    // === MÉTHODES D'ENREGISTREMENT ===

    /**
     * Extrait les chaînes entre guillemets
     */
    public static String[] extractStrings(String parameter) {
        List<String> result = new ArrayList<>();
        Matcher m = PATTERN_CAPTURE_QUOTES.matcher(parameter);
        while (m.find()) {
            String f = m.group(2);
            result.add(f);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Remplace les marqueurs de chaînes par les chaînes réelles
     */
    public static String replaceStrings(String parameter, String[] strings) {
        int i = 0;
        for (String s : strings) {
            parameter = parameter.replaceFirst("@\\{S" + i + "}", "\"" + Matcher.quoteReplacement(s) + "\"");
            i++;
        }
        return parameter;
    }

    /**
     * Remplace les chaînes par des marqueurs
     */
    public static String removeStrings(String parameter, String[] strings) {
        int i = 0;
        for (String s : strings) {
            parameter = parameter.replaceFirst(Pattern.quote("\"" + s + "\""), "@{S" + i + "}");
            i++;
        }
        return parameter;
    }

    /**
     * Divise une chaîne aux virgules
     */
    public static String[] splitAtComa(String expressionString) {
        if (expressionString == null || expressionString.trim().isEmpty()) {
            return new String[0];
        }

        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inString = false;

        for (int i = 0; i < expressionString.length(); i++) {
            char c = expressionString.charAt(i);

            if (c == '"' && (i == 0 || expressionString.charAt(i - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '(' || c == '[' || c == '{') {
                    depth++;
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result.toArray(new String[0]);
    }

    /**
     * Nettoie une chaîne en retirant les espaces inutiles
     */
    public static String trim(String string) {
        if (string == null) return null;
        return string.trim();
    }

    /**
     * Vérifie si un token est une variable
     */
    public static boolean checkIfVariable(ScriptToken token) {
        return PATTERN_VARIABLE.matcher(token.getText()).matches();
    }

    /**
     * Calcule le niveau d'indentation d'une ligne
     */
    public static int getTabLevel(String line) {
        if (line == null) return 0;
        int tabs = 0;
        for (char c : line.toCharArray()) {
            if (c == '\t') tabs++;
            else if (c == ' ') tabs += 0.25; // 4 espaces = 1 tab
            else break;
        }
        return tabs;
    }

    /**
     * Obtient le nom d'un type
     */
    public static String getNameOfType(Class type) {
        if (type == null) return "null";
        return type.getSimpleName();
    }

    /**
     * Remplace la dernière occurrence d'un pattern
     */
    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceAll("(?s)" + regex + "(?!.*?" + regex + ")", replacement);
    }

    /**
     * Nettoie les commentaires d'une ligne
     */
    public static String getUncommentedPart(String text) {
        if (text == null) return null;
        int commentIndex = text.indexOf('#');
        if (commentIndex != -1) {
            return text.substring(0, commentIndex);
        }
        return text;
    }

    /**
     * Vérifie si un type est valide parmi les types acceptés
     */
    public static boolean isTypeValid(Class type, Class[] validTypes) {
        if (type == null || validTypes == null || validTypes.length == 0) {
            return true; // Pas de restriction
        }

        for (Class validType : validTypes) {
            if (validType != null && validType.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Divise une chaîne aux doubles deux-points
     */
    public static String[] splitAtDoubleDot(String input) {
        if (input == null) return new String[0];
        return input.split("::");
    }

    /**
     * Transforme un pattern Sqript en pattern regex
     */
    public static TransformedPattern transformPattern(String pattern) throws Exception {
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        // Analyse le pattern pour compter les arguments et les marques
        int argsCount = 0;
        int marksCount = 0;

        String processedPattern = pattern;
        List<ScriptParameterDefinition[]> parameterDefinitions = new ArrayList<>();

        // ÉTAPE 1: Traiter les éléments optionnels Sqript [text] récursivement
        processedPattern = processOptionalElements(processedPattern);

        // ÉTAPE 2: Échapper les caractères spéciaux regex
        processedPattern = processedPattern.replaceAll("\\.", "\\\\.");
        processedPattern = processedPattern.replaceAll("~", "\\\\~");

        // ÉTAPE 3: Traiter les groupes de marques (1;...) (2;...) etc.
        Pattern markGroupPattern = Pattern.compile("\\((\\d+);([^)]+)\\)");
        Matcher markGroupMatcher = markGroupPattern.matcher(processedPattern);
        StringBuffer markBuffer = new StringBuffer();

        while (markGroupMatcher.find()) {
            String markNumber = markGroupMatcher.group(1);
            String markContent = markGroupMatcher.group(2);

            String replacement = "(?<m" + markNumber + ">" + markContent + ")";
            markGroupMatcher.appendReplacement(markBuffer, replacement);

            int markNum = Integer.parseInt(markNumber);
            marksCount = Math.max(marksCount, markNum + 1);
        }
        markGroupMatcher.appendTail(markBuffer);
        processedPattern = markBuffer.toString();

        // ÉTAPE 4: Traiter les arguments {type} et {+type} et {type*}
        Pattern argPattern = Pattern.compile("\\{(\\+?\\w+(?:\\|\\w+)*)([*]?)\\}");
        Matcher argMatcher = argPattern.matcher(processedPattern);
        StringBuffer argBuffer = new StringBuffer();

        while (argMatcher.find()) {
            String typeSpec = argMatcher.group(1);
            String modifier = argMatcher.group(2);

            boolean isPlus = typeSpec.startsWith("+");
            if (isPlus) {
                typeSpec = typeSpec.substring(1);
            }

            String[] types = typeSpec.split("\\|");
            Class<? extends ScriptElement> primaryType = null;

            for (String typeName : types) {
                Class<? extends ScriptElement> typeClass = parseType(typeName);
                if (typeClass != null && typeClass != ScriptElement.class) {
                    primaryType = typeClass;
                    break;
                }
            }

            if (primaryType == null) {
                primaryType = ScriptElement.class;
            }

            String replacement = "(?<a" + argsCount + ">.+)";
            argMatcher.appendReplacement(argBuffer, replacement);

            parameterDefinitions.add(new ScriptParameterDefinition[]{
                    new ScriptParameterDefinition((Class<? extends ScriptElement<?>>) primaryType, false)
            });

            argsCount++;
        }
        argMatcher.appendTail(argBuffer);
        processedPattern = argBuffer.toString();

        // ÉTAPE 5: Finaliser le pattern
        processedPattern = processedPattern.replaceAll("\\s+", "\\\\s+");

        ScriptParameterDefinition[][] paramArray = parameterDefinitions.toArray(new ScriptParameterDefinition[0][]);

        return new TransformedPattern(processedPattern, marksCount, argsCount, paramArray);
    }

    // === MÉTHODES D'ACCÈS (GETTERS) ===

    /**
     * Traite les éléments optionnels [text] de manière récursive
     */
    private static String processOptionalElements(String pattern) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < pattern.length()) {
            char c = pattern.charAt(i);

            if (c == '[') {
                int depth = 1;
                int end = i + 1;

                while (end < pattern.length() && depth > 0) {
                    if (pattern.charAt(end) == '[') {
                        depth++;
                    } else if (pattern.charAt(end) == ']') {
                        depth--;
                    }
                    end++;
                }

                if (depth == 0) {
                    String optionalContent = pattern.substring(i + 1, end - 1);

                    int spaceStart = end;
                    while (spaceStart < pattern.length() && Character.isWhitespace(pattern.charAt(spaceStart))) {
                        spaceStart++;
                    }

                    String processedContent = processOptionalElements(optionalContent);

                    if (spaceStart > end) {
                        String spaces = pattern.substring(end, spaceStart);
                        result.append("(?:").append(processedContent).append(spaces).append(")?");
                        i = spaceStart;
                    } else {
                        result.append("(?:").append(processedContent).append(")?");
                        i = end;
                    }
                } else {
                    result.append("\\[");
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }

        return result.toString();
    }

    @Override
    public void initialize() throws ScriptComponentException {
        log.info("Initializing ScriptRegistry...");
        initializeBuiltInOperators();
        log.info("ScriptRegistry initialized successfully");
        log.info("Total expressions registered: {}", expressions.size());
        log.info("Total actions registered: {}", actions.size());
    }
    
    @Override
    public void start() throws ScriptComponentException {
        if (active) {
            log.warn("ScriptRegistry is already active");
            return;
        }
        active = true;
        log.info("ScriptRegistry started");
    }
    
    @Override
    public void stop() throws ScriptComponentException {
        if (!active) {
            log.warn("ScriptRegistry is already stopped");
            return;
        }
        active = false;
        log.info("ScriptRegistry stopped");
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public String getComponentName() {
        return "ScriptRegistry";
    }

    /**
     * Enregistre une nouvelle expression dans le registre.
     */
    public void registerExpression(Class<? extends ScriptExpression> expressionClass, 
                                   String name, int priority, Feature... features) {
        ExpressionIdentifier id = new ExpressionIdentifier(priority, expressionClass);
        try {
            ExpressionDefinition definition = new ExpressionDefinition(name, expressionClass, priority, features);
            expressions.put(id, definition);
            log.info("Registered expression: {} ({}) with {} features", name, expressionClass.getSimpleName(), features.length);
            
            // Log des patterns pour debug
            for (int i = 0; i < features.length; i++) {
                Feature feature = features[i];
                log.debug("  Feature {}: name='{}', pattern='{}'", i, feature.name(), feature.pattern());
            }
        } catch (Exception e) {
            log.error("Failed to register expression: {} ({})", name, expressionClass.getSimpleName(), e);
        }
    }
    
    /**
     * Enregistre une nouvelle action dans le registre.
     */
    public void registerAction(Class<? extends ScriptAction> actionClass, 
                               String name, int priority, Feature... features) {
        try {
            ActionDefinition definition = new ActionDefinition(name, actionClass, priority, features);
            actions.add(definition);
            actions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            log.debug("Registered action: {} ({})", name, actionClass.getSimpleName());
        } catch (Exception e) {
            log.error("Failed to register action: {} ({})", name, actionClass.getSimpleName(), e);
        }
    }
    
    /**
     * Enregistre un nouveau bloc dans le registre.
     */
    public void registerBlock(Class<? extends ScriptBlock> blockClass, Feature feature, Feature[] fields, boolean reloadable) {
        BlockDefinition definition = new BlockDefinition(blockClass, feature, fields, reloadable);
        blocks.add(definition);
        log.debug("Registered block: {} ({})", feature.name(), blockClass.getSimpleName());
    }
    
    /**
     * Enregistre un nouvel événement dans le registre.
     */
    public void registerEvent(Class<? extends ScriptEvent> eventClass, 
                              Feature feature, Feature[] accessors) {
        try {
            EventDefinition definition = new EventDefinition(eventClass, feature, accessors);
            events.add(definition);
            log.debug("Registered event: {} ({})", feature.name(), eventClass.getSimpleName());
        } catch (Exception e) {
            log.error("Failed to register event: {} ({})", feature.name(), eventClass.getSimpleName(), e);
        }
    }
    
    /**
     * Enregistre une nouvelle boucle dans le registre.
     */
    public void registerLoop(Class<? extends ScriptLoop> loopClass, 
                             String name, String pattern, Side side, int priority) {
        LoopDefinition definition = new LoopDefinition(pattern, loopClass, side, name, priority);
        loops.add(definition);
        loops.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        log.debug("Registered loop: {} ({})", name, loopClass.getSimpleName());
    }
    
    /**
     * Enregistre une fonction native dans le registre.
     */
    public void registerNativeFunction(Class<? extends ScriptNativeFunction> functionClass,
                                       String name, String[] definitions, 
                                       String[] description, String[] examples) {
        NativeDefinition definition = new NativeDefinition(name, description, examples, functionClass);
        // TODO: Traitement des patterns transformés
        nativeFunctions.put(functionClass, definition);
        log.debug("Registered native function: {} ({})", name, functionClass.getSimpleName());
    }
    
    /**
     * Enregistre un nouveau type dans le registre.
     */
    public void registerType(Class<? extends ScriptElement<?>> typeClass, String name) {
        TypeDefinition definition = new TypeDefinition(name, new String[0], new String[]{""}, typeClass);
        types.put(typeClass, definition);
        log.debug("Registered type: {} ({})", name, typeClass.getSimpleName());
    }
    
    /**
     * Enregistre un nouveau type primitif dans le registre.
     */
    public void registerPrimitive(Class<? extends PrimitiveType<?>> primitiveClass, 
                                  String name, String... patterns) {
        TypeDefinition definition = new TypeDefinition(name, new String[0], new String[]{""}, primitiveClass);
        if (patterns.length > 0) {
            definition.transformedPattern = new TransformedPattern(patterns[0], 0, 0, 
                new ScriptParameterDefinition[][]{{new ScriptParameterDefinition(primitiveClass, false)}});
        }
        primitives.put(primitiveClass, definition);
        log.debug("Registered primitive: {} ({})", name, primitiveClass.getSimpleName());
    }
    
    /**
     * Enregistre un nouvel opérateur dans le registre.
     */
    public void registerOperator(ScriptOperator operator) {
        // Vérification de l'unicité
        for (ScriptOperator existing : operators) {
            if (existing.symbol.equals(operator.symbol) && existing.unary == operator.unary) {
                log.error("Cannot register operator {} - symbol '{}' already exists", 
                         operator, operator.symbol);
                return;
            }
        }
        operators.add(operator);
        log.debug("Registered operator: {}", operator.symbol);
    }

    // === MÉTHODES UTILITAIRES ===
    
    /**
     * Enregistre une opération binaire.
     */
    public void registerBinaryOperation(ScriptOperator operator, Class typeA, Class typeB,
                                        Class<? extends ScriptElement<?>> returnType, 
                                        IOperation operation, int priority) {
        binaryOperations.computeIfAbsent(operator, k -> new ConcurrentHashMap<>())
                       .computeIfAbsent(typeA, k -> new ConcurrentHashMap<>())
                       .put(typeB, new OperatorDefinition(operation, returnType, priority));
    }

    // === MÉTHODES POUR LE SYSTÈME DE COMPILATION ===
    
    /**
     * Enregistre une opération unaire.
     */
    public void registerUnaryOperation(ScriptOperator operator, Class<? extends ScriptElement<?>> type,
                                       Class<? extends ScriptElement<?>> returnType, IOperation operation) {
        unaryOperations.computeIfAbsent(operator, k -> new ConcurrentHashMap<>())
                      .put(type, new OperatorDefinition(operation, returnType, 0));
    }
    
    /**
     * Enregistre un parser de type.
     */
    public <T extends ScriptElement<?>, U extends ScriptElement<?>> void registerTypeParser(
            Class<T> fromType, Class<U> toType, ITypeParser<T, U> parser, int priority) {
        typeParsers.computeIfAbsent(fromType, k -> new ConcurrentHashMap<>())
                  .put(toType, new TypeParserDefinition<>(priority, parser, fromType, toType));
    }
    
    /**
     * Enregistre un parser personnalisé.
     */
    public void registerCustomParser(IScriptParser parser) {
        customParsers.add(parser);
        log.debug("Registered custom parser: {}", parser.getClass().getSimpleName());
    }
    
    public List<EventDefinition> getEvents() {
        return new ArrayList<>(events);
    }

    public List<ActionDefinition> getActions() {
        return new ArrayList<>(actions);
    }

    public List<BlockDefinition> getBlocks() {
        return new ArrayList<>(blocks);
    }

    public Map<ExpressionIdentifier, ExpressionDefinition> getExpressions() {
        return new HashMap<>(expressions);
    }

    public List<LoopDefinition> getLoops() {
        return new ArrayList<>(loops);
    }

    public Map<Class<? extends ScriptElement<?>>, TypeDefinition> getTypes() {
        return new HashMap<>(types);
    }

    public Map<Class<? extends PrimitiveType<?>>, TypeDefinition> getPrimitives() {
        return new HashMap<>(primitives);
    }

    public Map<Class<? extends ScriptNativeFunction>, NativeDefinition> getNativeFunctions() {
        return new HashMap<>(nativeFunctions);
    }

    public List<ScriptOperator> getOperators() {
        return new ArrayList<>(operators);
    }

    public List<IScriptParser> getCustomParsers() {
        return new ArrayList<>(customParsers);
    }
    
    /**
     * Recherche une définition d'action par classe.
     */
    public ActionDefinition findActionDefinition(Class<? extends ScriptAction> actionClass) {
        return actions.stream()
                     .filter(def -> def.getActionClass().equals(actionClass))
                     .findFirst()
                     .orElse(null);
    }
    
    /**
     * Recherche une définition d'expression par classe.
     */
    public ExpressionDefinition findExpressionDefinition(Class<? extends ScriptExpression> expressionClass) {
        return expressions.get(new ExpressionIdentifier(0, expressionClass));
    }
    
    /**
     * Recherche une opération binaire.
     */
    public OperatorDefinition findBinaryOperation(Class<? extends ScriptElement> typeA, 
                                                  Class<? extends ScriptElement> typeB, 
                                                  ScriptOperator operator) {
        if (operator == null) {
            operator = fr.nico.sqript.structures.ScriptOperator.ADD;
        }
        Map<Class, Map<Class, OperatorDefinition>> operatorMap = binaryOperations.get(operator);
        if (operatorMap == null) {
            log.error("Aucun opérateur enregistré pour " + operator + " !");
            return null;
        }
        // LOG DEBUG
        log.debug("Recherche opérateur " + operator + " pour types : " + typeA + " + " + typeB);
        
        Map<Class, OperatorDefinition> typeMap = operatorMap.get(typeA);
        if (typeMap == null) {
            // Fallback vers ScriptElement.class
            typeMap = operatorMap.get(ScriptElement.class);
            if (typeMap == null) {
                log.error("Aucun mapping pour typeA=" + typeA + " ni fallback ScriptElement.class");
                return null;
            }
        }
        if (typeMap == null) {
            log.error("typeMap null pour typeA=" + typeA);
            return null;
        }
        OperatorDefinition result = typeMap.get(typeB);
        if (result == null) {
            // Fallback vers ScriptElement.class
            result = typeMap.get(ScriptElement.class);
        }
        if (result == null) {
            log.error("Aucun opérateur trouvé pour " + typeA + " + " + typeB + " (" + operator + ")");
        }
        return result;
    }
    
    /**
     * Recherche une opération unaire.
     */
    public OperatorDefinition findUnaryOperation(Class type, ScriptOperator operator) {
        Map<Class, OperatorDefinition> operatorMap = unaryOperations.get(operator);
        return operatorMap != null ? operatorMap.get(type) : null;
    }
    
    /**
     * Recherche un parser de type.
     */
    public <T extends ScriptElement<?>, U extends ScriptElement<?>> TypeParserDefinition<T, U> findTypeParser(
            Class<T> fromType, Class<U> toType) {
        Map<Class, TypeParserDefinition<?, ?>> fromMap = typeParsers.get(fromType);
        if (fromMap == null) return null;
        return (TypeParserDefinition<T, U>) fromMap.get(toType);
    }
    
    // === MÉTHODES UTILITAIRES (migrées depuis ScriptDecoder) ===

    /**
     * Initialise les opérateurs intégrés.
     */
    private void initializeBuiltInOperators() {
        // Liste des types primitifs et ScriptElement
        Class[] concatTypes = new Class[] {
            fr.nico.sqript.structures.ScriptElement.class,
            fr.nico.sqript.types.primitive.TypeString.class,
            fr.nico.sqript.types.primitive.TypeNumber.class,
            fr.nico.sqript.types.primitive.TypeBoolean.class,
            fr.nico.sqript.types.primitive.TypeArray.class,
            fr.nico.sqript.types.primitive.TypeEntity.class,
            fr.nico.sqript.types.primitive.TypeElement.class,
            fr.nico.sqript.types.primitive.TypeResource.class
        };
        // Enregistrement exhaustif pour la concaténation
        for (Class<?> a : concatTypes) {
            for (Class<?> b : concatTypes) {
                registerBinaryOperation(
                    fr.nico.sqript.structures.ScriptOperator.ADD,
                    a,
                    b,
                    fr.nico.sqript.types.primitive.TypeString.class,
                    (o1, o2) -> new fr.nico.sqript.types.primitive.TypeString(
                        (o1 == null ? "null" : o1.getObject().toString()) +
                        (o2 == null ? "null" : o2.getObject().toString())
                    ),
                    0
                );
            }
        }
        log.debug("Built-in operators initialized (generic string concatenation registered for all primitive types)");
    }
    
    /**
     * Recherche un opérateur par son symbole
     */
    public ScriptOperator findOperator(String symbol) {
        return operators.stream()
                .filter(op -> op.symbol.equals(symbol))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Retourne la liste des parseurs personnalisés
     */
    public List<IScriptParser> getScriptParsers() {
        return new ArrayList<>(customParsers);
    }
    
    /**
     * Vérifie si un token est un mot-clé réservé
     */
    private static boolean isKeyword(String text) {
        switch (text.toLowerCase()) {
            case "now":
            case "player":
            case "true":
            case "false":
            case "sender":
            case "console":
            case "server":
            case "null":
            case "function":
            case "for":
            case "if":
            case "else":
            case "while":
            case "loop":
            case "in":
            case "and":
            case "or":
            case "not":
            case "all":
            case "players":
            case "entities":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Parse une expression en utilisant le nouveau système
     */
    public ScriptExpression parseExpression(ScriptToken token, ScriptCompilationContext context, Class[] requiredTypes) throws ScriptException {
        String text = token.getText().trim();
        
        log.debug("Parsing expression: '{}'", text);


        /*

        // Si aucune expression complexe ne matche, essayer les expressions simples

        // Vérification pour les chaînes littérales (avec ou sans interpolation)
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2 && !text.contains(" + ")) {
            // Chaîne littérale - vérifier s'il y a interpolation
            String content = text.substring(1, text.length() - 1);
            if (content.contains("%")) {
                // Chaîne avec interpolation
                return new InterpolatedStringExpression(content, context);
            } else {
                // Chaîne littérale simple
                return new StringLiteralExpression(content);
            }
        }

        // Vérification pour les variables {nom}
        if (text.startsWith("{") && text.endsWith("}") && text.length() >= 3) {
            // Variable - créer une expression de variable
            String varName = text.substring(1, text.length() - 1);
            return new VariableExpression(varName);
        }
        // === NOUVEAU : variables déclarées sans accolades ===
        if (context != null && context.getAccessorFor(text) != null) {
            return new VariableExpression(text);
        }

        // Identifiants de ressources Minecraft (namespace:path)
        if (text.matches("[a-z0-9_.-]+:[a-z0-9_./]+")) {
            return new ResourceIdentifierExpression(text);
        }

        // Variables simples (identifiants simples comme 'message', 'p', etc.)
        if (text.matches("[a-zA-Z_][a-zA-Z0-9_]*") && !isKeyword(text)) {
            return new VariableExpression(text);
        }

         */

        // Vérifier d'abord contre toutes les expressions enregistrées
        for (ExpressionDefinition def : expressions.values()) {
            try {
                // Vérifier chaque feature de l'expression
                for (int i = 0; i < def.getFeatures().length; i++) {
                    Feature feature = def.getFeatures()[i];
                    if (feature.pattern() != null && !feature.pattern().isEmpty()) {
                        try {
                            // Transformer le pattern Sqript en regex
                            TransformedPattern transformedPattern = transformPattern(feature.pattern());
                            
                            if (transformedPattern.matches(text)) {
                                log.debug("Found matching pattern '{}' for text '{}' in expression {}", 
                                    feature.pattern(), text, def.getExpressionClass().getSimpleName());
                                
                                // Créer une instance de l'expression
                                ScriptExpression expr = def.getExpressionClass().newInstance();
                                if (expr != null) {
                                    // Configurer l'expression avec l'index correspondant
                                    expr.setMatchedIndex(i);
                                    expr.setLine(token);
                                    
                                    // Extraire les paramètres si nécessaire
                                    String[] parameters = transformedPattern.getAllArguments(text);
                                    if (parameters != null && parameters.length > 0) {
                                        log.debug("Extracted parameters: {}", java.util.Arrays.toString(parameters));
                                        // Les paramètres seront gérés par l'expression elle-même lors de l'exécution
                                        // via le système de compilation existant
                                    }
                                    
                                    return expr;
                                }
                            }
                        } catch (Exception e) {
                            log.trace("Pattern '{}' failed to match text '{}': {}", feature.pattern(), text, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.trace("Failed to create expression {}: {}", def.getExpressionClass().getSimpleName(), e.getMessage());
            }
        }
        
        // Si c'est une expression complexe avec interpolation ou opérateurs, utiliser le système legacy
        // Recherche dans les expressions enregistrées
        /*
        for (ExpressionDefinition def : expressions.values()) {
            try {
                // Implémentation temporaire - vérifier si l'expression peut être créée
                ScriptExpression expr = def.getExpressionClass().newInstance();
                if (expr != null) {
                    return expr;
                }
            } catch (Exception e) {
                // Continue avec l'expression suivante
            }
        }
        */
        // Si aucune expression n'est trouvée, créer une expression de chaîne par défaut
        log.warn("No expression found for token '{}', treating as string literal", text);
        return new StringLiteralExpression(text);
    }
    
    /**
     * Parse un arbre d'expression
     */
    public Node parseExpressionTree(ScriptToken token, ScriptCompilationContext context, Class[] requiredTypes) throws ScriptException {
        String text = token.getText().trim();
        
        // Cas simples : chaînes littérales (avec ou sans interpolation)
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2 && !text.contains("+")) {
            String content = text.substring(1, text.length() - 1);
            if (content.contains("%")) {
                // Chaîne avec interpolation
                return new NodeExpression(new InterpolatedStringExpression(content, context));
            } else {
                // Chaîne littérale simple
                return new NodeExpression(new StringLiteralExpression(content));
            }
        }
        
        // Cas simples : variables {nom}
        if (text.startsWith("{") && text.endsWith("}") && text.length() >= 3) {
            String varName = text.substring(1, text.length() - 1);
            return new NodeExpression(new VariableExpression(varName));
        }
        // === NOUVEAU : variables déclarées sans accolades ===
        if (context != null && context.getAccessorFor(text) != null) {
            return new NodeExpression(new VariableExpression(text));
        }
        
        // Cas simples : nombres
        try {
            if (text.matches("\\d+")) {
                int value = Integer.parseInt(text);
                return new NodeExpression(new NumberLiteralExpression(value));
            }
            if (text.matches("\\d+\\.\\d+")) {
                double value = Double.parseDouble(text);
                return new NodeExpression(new NumberLiteralExpression(value));
            }
        } catch (NumberFormatException e) {
            // Pas un nombre, continuer
        }
        
        // Identifiants de ressources Minecraft (namespace:path)
        if (text.matches("[a-z0-9_.-]+:[a-z0-9_./]+")) {
            return new NodeExpression(new ResourceIdentifierExpression(text));
        }
        
        // Variables simples (identifiants simples comme 'message', 'p', etc.)
        if (text.matches("[a-zA-Z_][a-zA-Z0-9_]*") && !isKeyword(text)) {
            return new NodeExpression(new VariableExpression(text));
        }
        
        // Mots-clés spéciaux
        switch (text.toLowerCase()) {
            case "now":
                return new NodeExpression(new NowExpression());
            case "player":
                return new NodeExpression(new PlayerExpression());
            case "true":
                return new NodeExpression(new BooleanLiteralExpression(true));
            case "false":
                return new NodeExpression(new BooleanLiteralExpression(false));
        }
        
        // Arguments de commande
        if (text.startsWith("arg ") && text.length() > 4) {
            try {
                String argNumStr = text.substring(4).trim();
                int argNum = Integer.parseInt(argNumStr);
                return new NodeExpression(new ArgumentExpression(argNum));
            } catch (NumberFormatException e) {
                // Pas un argument valide
            }
        }
        
        // Expressions complexes avec opérateurs
        if (containsOperator(text)) {
            // Gestion de la concaténation (opérateur +)
            List<String> parts = splitByPlusRespectingQuotesAndBraces(text);
            if (parts.size() > 1) {
                List<Node> children = new ArrayList<>();
                for (String part : parts) {
                    Node child = parseExpressionTree(new ScriptToken(part.trim()), context, requiredTypes);
                    log.debug("Résultat du parsing de la partie '{}' : {}", part, child);
                    if (child == null)
                        throw new fr.nico.sqript.core.compilation.ScriptException(token, "Erreur de compilation dans la concaténation : " + part);
                    children.add(child);
                }
                // Utilise ScriptOperator.ADD pour la concaténation
                return new NodeOperation(children.toArray(new Node[0]), fr.nico.sqript.structures.ScriptOperator.ADD);
            }
            // Si d'autres opérateurs, legacy pour l'instant
            throw new fr.nico.sqript.core.compilation.ScriptException(token, "Expression inconnue : " + text);
        }
        
        // Expressions de type "numbers in range of {variable}"
        if (text.matches(".+\\s+in\\s+.+")) {
            throw new fr.nico.sqript.core.compilation.ScriptException(token, "Expression inconnue : " + text);
        }
        
        // Fallback : expression legacy
        throw new fr.nico.sqript.core.compilation.ScriptException(token, "Expression inconnue : " + text);
    }
    
    /**
     * Parse une action
     */
    public ScriptAction parseAction(ScriptToken token, ScriptCompilationContext context) throws Exception {
        String lineText = token.getText().trim();
        
        // Nettoyer d'abord - enlever l'indentation excessive
        if (lineText.startsWith("    ")) {
            lineText = lineText.substring(4); // Enlever 4 espaces
        }
        
        log.info("[DEBUG SQRIPT] parseAction pour la ligne : '{}'", lineText);
        
        for (ActionDefinition def : actions) {
            try {
                log.debug("[DEBUG SQRIPT] Test de l'action : {}", def.getName());
                
                // Vérifier si un des patterns de l'action correspond
                int[] matchResult = def.getMatchedPatternIndexAndMarks(lineText);
                if (matchResult.length > 0) {
                    int matchedIndex = matchResult[0];
                    int marks = matchResult.length > 1 ? matchResult[1] : 0;
                    
                    log.info("[DEBUG SQRIPT] Pattern matche '{}' pour l'action {} à l'index {}", lineText, def.getName(), matchedIndex);
                    
                    // Créer une instance de l'action
                    ScriptAction action = def.getActionClass().newInstance();
                    if (action != null) {
                        log.info("[DEBUG SQRIPT] Action créée avec succès : {}", action.getClass().getSimpleName());
                        
                        // Extraire les paramètres avec le pattern matché
                        String[] parameters = def.transformedPatterns[matchedIndex].getAllArguments(lineText);
                        List<String> parameterList = new ArrayList<>();
                        if (parameters != null) {
                            log.debug("[DEBUG SQRIPT] {} paramètres extraits : {}", parameters.length, Arrays.toString(parameters));
                            parameterList.addAll(Arrays.asList(parameters));
                        }
                        
                        // Compiler l'action avec les paramètres extraits
                        action.build(token, context, parameterList, matchedIndex, marks);
                        
                        return action;
                    }
                } else {
                    log.trace("[DEBUG SQRIPT] Aucun pattern ne matche '{}' pour l'action {}", lineText, def.getName());
                }
            } catch (Exception e) {
                log.debug("[DEBUG SQRIPT] Erreur lors du test de l'action '{}' avec la ligne '{}' : {}", def.getName(), lineText, e.getMessage());
            }
        }
        
        log.warn("[DEBUG SQRIPT] Aucune action ne matche la ligne '{}'", lineText);
        return null;
    }
    
    /**
     * Parse une boucle
     */
    public ScriptWrapper parseLoop(ScriptToken token, ScriptCompilationContext context) throws Exception {
        String lineText = token.getText().trim();
        
        for (LoopDefinition def : loops) {
            try {
                // Vérifier si le pattern correspond au texte
                if (def.matches(lineText)) {
                    log.debug("Pattern '{}' matches token '{}' for loop {}", def.getPattern().pattern(), lineText, def.getName());
                    
                    // Créer une instance de la boucle
                    ScriptLoop loop = def.getLoopClass().newInstance();
                    if (loop != null) {
                        // Compiler la boucle avec le token
                        loop.build(token, context);
                        
                        // Retourner dans un wrapper
                        SimpleScriptWrapper wrapper = new SimpleScriptWrapper();
                        wrapper.setWrapped(loop);
                        return wrapper;
                    }
                } else {
                    log.trace("Pattern '{}' does not match token '{}'", def.getPattern().pattern(), lineText);
                }
            } catch (Exception e) {
                log.debug("Error trying to parse loop '{}' with token '{}': {}", def.getName(), lineText, e.getMessage());
                // Continue avec la boucle suivante
            }
        }
        return null;
    }
    
    /**
     * Retourne des statistiques sur le registre.
     */
    public RegistryStats getStats() {
        return new RegistryStats(
            events.size(),
            actions.size(), 
            blocks.size(),
            expressions.size(),
            loops.size(),
            types.size(),
            primitives.size(),
            nativeFunctions.size(),
            operators.size(),
            customParsers.size()
        );
    }
    
    /**
     * Expression simple pour chaîne littérale
     */
    public static class StringLiteralExpression extends ScriptExpression {
        private final String value;
        
        public StringLiteralExpression(String value) {
            this.value = value;
            log.debug("Created StringLiteralExpression with value: '{}'", value);
        }
        
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            fr.nico.sqript.types.primitive.TypeString result = new fr.nico.sqript.types.primitive.TypeString(value);
            return result;
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false; // Les chaînes littérales ne peuvent pas être modifiées
        }
        
        @Override
        public Class getReturnType() {
            return fr.nico.sqript.types.primitive.TypeString.class;
        }
        
        @Override
        public String toString() {
            return "StringLiteralExpression{value='" + value + "'}";
        }
    }
    
    /**
     * Expression simple pour variable
     */
    public static class VariableExpression extends ScriptExpression {
        private final String varName;
        
        public VariableExpression(String varName) {
            this.varName = varName;
        }
        
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            // Utiliser le même système que ExprReference
            int varHash = context.getHash(varName, false);
            ScriptType<?> result = context.getVariable(varHash);
            if (result == null) {
                // Fallback: essayer avec l'accessor directement
                ScriptTypeAccessor accessor = context.getAccessor(varName);
                if (accessor != null) {
                    result = accessor.element;
                }
            }
            return result;
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            // Utiliser le même système que ExprReference pour setter
            int varHash = context.getHash(varName, false);
            ScriptTypeAccessor typeAccessor = context.getAccessor(varHash);
            if (typeAccessor != null) {
                typeAccessor.setElement(to);
            } else {
                // Créer un nouvel accessor
                typeAccessor = new ScriptTypeAccessor(to, varHash);
                typeAccessor.setKey(varName);
                context.put(typeAccessor);
            }
            return true;
        }
        
        @Override
        public Class getReturnType() {
            return ScriptType.class;
        }
        
        @Override
        public String toString() {
            return "VariableExpression{varName='" + varName + "'}";
        }
    }
    
    /**
     * Expression simple pour nombre
     */
    public static class NumberLiteralExpression extends ScriptExpression {
        private final Number value;
        
        public NumberLiteralExpression(Number value) {
            this.value = value;
        }
        
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            if (value instanceof Integer) {
                return new fr.nico.sqript.types.primitive.TypeNumber((Integer) value);
            } else if (value instanceof Double) {
                return new fr.nico.sqript.types.primitive.TypeNumber((Double) value);
            } else if (value instanceof Float) {
                return new fr.nico.sqript.types.primitive.TypeNumber((Float) value);
            } else {
                return new fr.nico.sqript.types.primitive.TypeNumber(value.doubleValue());
            }
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false; // Les nombres littéraux ne peuvent pas être modifiés
        }
        
        @Override
        public Class getReturnType() {
            return fr.nico.sqript.types.primitive.TypeNumber.class;
        }
        
        @Override
        public String toString() {
            return "NumberLiteralExpression{value=" + value + "}";
        }
    }
    
    /**
     * Expression simple pour booléen
     */
    public static class BooleanLiteralExpression extends ScriptExpression {
        private final boolean value;
        
        public BooleanLiteralExpression(boolean value) {
            this.value = value;
        }
        
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            return new fr.nico.sqript.types.primitive.TypeBoolean(value);
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false; // Les booléens littéraux ne peuvent pas être modifiés
        }
        
        @Override
        public Class getReturnType() {
            return fr.nico.sqript.types.primitive.TypeBoolean.class;
        }
        
        @Override
        public String toString() {
            return "BooleanLiteralExpression{value=" + value + "}";
        }
    }
    
    /**
     * Expression pour "now"
     */
    public static class NowExpression extends ScriptExpression {
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            return new fr.nico.sqript.types.primitive.TypeNumber(System.currentTimeMillis());
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false;
        }
        
        @Override
        public Class getReturnType() {
            return fr.nico.sqript.types.primitive.TypeNumber.class;
        }
        
        @Override
        public String toString() {
            return "NowExpression{}";
        }
    }
    
    /**
     * Expression pour "player"
     */
    public static class PlayerExpression extends ScriptExpression {
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            // Récupérer le joueur depuis le contexte
            ScriptTypeAccessor playerAccessor = context.getAccessor("player");
            if (playerAccessor != null) {
                return playerAccessor.element;
            }
            return null; // Retourner null au lieu de lancer une exception
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false;
        }
        
        @Override
        public Class getReturnType() {
            return fr.nico.sqript.types.TypePlayer.class;
        }
        
        @Override
        public String toString() {
            return "PlayerExpression{}";
        }
    }
    
    /**
     * Expression pour les arguments de commande
     */
    public static class ArgumentExpression extends ScriptExpression {
        private final int argNumber;
        
        public ArgumentExpression(int argNumber) {
            this.argNumber = argNumber;
        }
        
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            ScriptTypeAccessor argAccessor = context.getAccessor("arg " + argNumber);
            if (argAccessor != null) {
                return argAccessor.element;
            }
            return null; // Retourner null au lieu de lancer une exception
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false;
        }
        
        @Override
        public Class getReturnType() {
            return ScriptType.class;
        }
        
        @Override
        public String toString() {
            return "ArgumentExpression{argNumber=" + argNumber + "}";
        }
    }
    
    /**
     * Expression pour les identifiants de ressources Minecraft (namespace:path)
     */
    public static class ResourceIdentifierExpression extends ScriptExpression {
        private final String identifier;
        
        public ResourceIdentifierExpression(String identifier) {
            this.identifier = identifier;
        }
        
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            // Retourner l'identifiant comme une ressource Minecraft
            return new fr.nico.sqript.types.primitive.TypeResource(identifier);
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false; // Les identifiants de ressources ne peuvent pas être modifiés
        }
        
        @Override
        public Class getReturnType() {
            return fr.nico.sqript.types.primitive.TypeResource.class;
        }
        
        @Override
        public String toString() {
            return "ResourceIdentifierExpression{identifier='" + identifier + "'}";
        }
    }
    
    /**
     * Expression pour les chaînes avec interpolation de variables
     */
    public static class InterpolatedStringExpression extends ScriptExpression {
        private final String template;
        
        public InterpolatedStringExpression(String template, ScriptCompilationContext context) {
            this.template = template;
            log.debug("Created InterpolatedStringExpression with template: '{}'", template);
        }
        
        @Override
        public ScriptType get(ScriptContext context, ScriptType<?>[] parameters) throws ScriptException {
            String result = template;
            
            // Pattern pour capturer %{variable}% et %variable%
            Pattern pattern = Pattern.compile("%\\{([^}]+)\\}%|%([^%\\s]+)%");
            Matcher matcher = pattern.matcher(template);
            
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String varName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                log.debug("Interpolating variable: '{}'", varName);
                
                try {
                    // Récupérer la variable en utilisant le système de hash comme ExprReference
                    int varHash = context.getHash(varName, false);
                    ScriptType<?> variable = context.getVariable(varHash);
                    if (variable != null) {
                        String value = variable.getObject().toString();
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                        log.debug("Variable '{}' resolved to: '{}'", varName, value);
                    } else {
                        // Essayer les accesseurs du contexte
                        ScriptTypeAccessor accessor = context.getAccessor(varName);
                        if (accessor != null && accessor.element != null) {
                            String value = accessor.element.getObject().toString();
                            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                            log.debug("Variable '{}' resolved via accessor to: '{}'", varName, value);
                        } else {
                            // Variable non trouvée, la laisser telle quelle
                            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                            log.warn("Variable '{}' not found in context", varName);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error interpolating variable '{}': {}", varName, e.getMessage());
                    // En cas d'erreur, laisser la variable telle quelle
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                }
            }
            matcher.appendTail(sb);
            
            result = sb.toString();
            log.debug("Interpolation result: '{}'", result);
            return new fr.nico.sqript.types.primitive.TypeString(result);
        }
        
        @Override
        public boolean set(ScriptContext context, ScriptType to, ScriptType<?>[] parameters) throws ScriptException {
            return false; // Les chaînes interpolées ne peuvent pas être modifiées
        }
        
        @Override
        public Class getReturnType() {
            return fr.nico.sqript.types.primitive.TypeString.class;
        }
        
        @Override
        public String toString() {
            return "InterpolatedStringExpression{template='" + template + "'}";
        }
    }
    
    /**
     * Classe utilitaire pour les statistiques du registre.
     */
    public static class RegistryStats {
        public final int eventCount;
        public final int actionCount;
        public final int blockCount;
        public final int expressionCount;
        public final int loopCount;
        public final int typeCount;
        public final int primitiveCount;
        public final int nativeFunctionCount;
        public final int operatorCount;
        public final int customParserCount;
        
        public RegistryStats(int eventCount, int actionCount, int blockCount, 
                           int expressionCount, int loopCount, int typeCount,
                           int primitiveCount, int nativeFunctionCount, 
                           int operatorCount, int customParserCount) {
            this.eventCount = eventCount;
            this.actionCount = actionCount;
            this.blockCount = blockCount;
            this.expressionCount = expressionCount;
            this.loopCount = loopCount;
            this.typeCount = typeCount;
            this.primitiveCount = primitiveCount;
            this.nativeFunctionCount = nativeFunctionCount;
            this.operatorCount = operatorCount;
            this.customParserCount = customParserCount;
        }
        
        @Override
        public String toString() {
            return String.format("RegistryStats{events=%d, actions=%d, blocks=%d, expressions=%d, " +
                               "loops=%d, types=%d, primitives=%d, natives=%d, operators=%d, parsers=%d}",
                               eventCount, actionCount, blockCount, expressionCount, loopCount,
                               typeCount, primitiveCount, nativeFunctionCount, operatorCount, customParserCount);
        }
    }
}





