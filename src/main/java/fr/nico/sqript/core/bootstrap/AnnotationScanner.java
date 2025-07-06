package fr.nico.sqript.core.bootstrap;

import com.google.common.collect.SetMultimap;
import fr.nico.sqript.actions.ScriptAction;
import fr.nico.sqript.blocks.ScriptBlock;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.function.ScriptNativeFunction;
import fr.nico.sqript.meta.*;
import fr.nico.sqript.structures.ScriptElement;
import fr.nico.sqript.structures.ScriptLoop;
import fr.nico.sqript.types.ScriptType;
import fr.nico.sqript.types.primitive.PrimitiveType;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scanner automatique des annotations pour enregistrer les composants Sqript.
 */
public class AnnotationScanner {

    private static final Logger log = LogManager.getLogger(AnnotationScanner.class);

    private final ScriptRegistry registry;

    public AnnotationScanner(ScriptRegistry registry) {
        this.registry = registry;
    }

    /**
     * Scanne et enregistre automatiquement tous les composants annotés.
     */
    public void scanAndRegisterAll() {
        log.info("Starting automatic component registration...");

        try {
            // IMPORTANT: Les primitives doivent être enregistrées EN PREMIER
            // car elles sont utilisées par les expressions, actions, etc.
            scanAndRegisterPrimitives("fr.nico.sqript.types.primitive");

            // Ensuite les types de base
            scanAndRegisterTypes("fr.nico.sqript.types");

            // Puis les expressions et actions
            scanAndRegisterExpressions("fr.nico.sqript.expressions");
            scanAndRegisterActions("fr.nico.sqript.actions");

            // Les fonctions natives
            scanAndRegisterNativeFunctions("fr.nico.sqript.function");

            // Les blocs et événements
            scanAndRegisterBlocks("fr.nico.sqript.blocks");
            scanAndRegisterEvents("fr.nico.sqript.events");

            // Enfin les boucles
            scanAndRegisterLoops("fr.nico.sqript.structures");

            log.info("Automatic component registration completed");

        } catch (Exception e) {
            log.error("Failed to scan and register components", e);
        }
    }

    private void scanAndRegisterExpressions(String packageName) {
        log.info("Scanning expressions in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (ScriptExpression.class.isAssignableFrom(clazz)) {
                Expression annotation = clazz.getAnnotation(Expression.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ScriptExpression> expressionClass = (Class<? extends ScriptExpression>) clazz;

                        registry.registerExpression(
                                expressionClass,
                                annotation.name(),
                                annotation.priority(),
                                annotation.features()
                        );

                        registered++;
                        log.debug("Registered expression: {} ({})", annotation.name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.warn("Failed to register expression: {}", clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} expressions from package {}", registered, packageName);
    }

    private void scanAndRegisterActions(String packageName) {
        log.info("Scanning actions in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (ScriptAction.class.isAssignableFrom(clazz)) {
                Action annotation = clazz.getAnnotation(Action.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ScriptAction> actionClass = (Class<? extends ScriptAction>) clazz;

                        registry.registerAction(
                                actionClass,
                                annotation.name(),
                                annotation.priority(),
                                annotation.features()
                        );

                        registered++;
                        log.debug("Registered action: {} ({})", annotation.name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.warn("Failed to register action: {}", clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} actions from package {}", registered, packageName);
    }

    private void scanAndRegisterBlocks(String packageName) {
        log.info("Scanning blocks in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (ScriptBlock.class.isAssignableFrom(clazz)) {
                Block annotation = clazz.getAnnotation(Block.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ScriptBlock> blockClass = (Class<? extends ScriptBlock>) clazz;

                        registry.registerBlock(
                                blockClass,
                                annotation.feature(),
                                annotation.fields(),
                                annotation.reloadable()
                        );

                        registered++;
                        log.debug("Registered block: {} ({})", annotation.feature().name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.warn("Failed to register block: {}", clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} blocks from package {}", registered, packageName);
    }

    private void scanAndRegisterEvents(String packageName) {
        log.info("Scanning events in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (ScriptEvent.class.isAssignableFrom(clazz)) {
                Event annotation = clazz.getAnnotation(Event.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ScriptEvent> eventClass = (Class<? extends ScriptEvent>) clazz;

                        registry.registerEvent(
                                eventClass,
                                annotation.feature(),
                                annotation.accessors()
                        );

                        registered++;
                        log.debug("Registered event: {} ({})", annotation.feature().name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.warn("Failed to register event: {}", clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} events from package {}", registered, packageName);
    }

    private void scanAndRegisterPrimitives(String packageName) {
        log.info("Scanning primitives in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (PrimitiveType.class.isAssignableFrom(clazz)) {
                Primitive annotation = clazz.getAnnotation(Primitive.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends PrimitiveType<?>> primitiveClass = (Class<? extends PrimitiveType<?>>) clazz;

                        // Utilise les patterns de l'annotation
                        String[] patterns = new String[]{annotation.pattern()};

                        registry.registerPrimitive(
                                primitiveClass,
                                annotation.name(),
                                patterns
                        );

                        registered++;
                        log.debug("Registered primitive: {} ({})", annotation.name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.error("Failed to register primitive: {} ({})", annotation.name(), clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} primitives from package {}", registered, packageName);
    }

    private void scanAndRegisterLoops(String packageName) {
        log.info("Scanning loops in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (ScriptLoop.class.isAssignableFrom(clazz)) {
                Loop annotation = clazz.getAnnotation(Loop.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ScriptLoop> loopClass = (Class<? extends ScriptLoop>) clazz;

                        registry.registerLoop(
                                loopClass,
                                annotation.name(),
                                annotation.pattern(),
                                annotation.side(),
                                annotation.priority()
                        );

                        registered++;
                        log.debug("Registered loop: {} ({})", annotation.name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.warn("Failed to register loop: {}", clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} loops from package {}", registered, packageName);
    }

    private void scanAndRegisterTypes(String packageName) {
        log.info("Scanning types in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (ScriptType.class.isAssignableFrom(clazz)) {
                Type annotation = clazz.getAnnotation(Type.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ScriptElement<?>> typeClass = (Class<? extends ScriptElement<?>>) clazz;

                        registry.registerType(
                                typeClass,
                                annotation.name()
                        );

                        registered++;
                        log.debug("Registered type: {} ({})", annotation.name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.warn("Failed to register type: {}", clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} types from package {}", registered, packageName);
    }

    private void scanAndRegisterNativeFunctions(String packageName) {
        log.info("Scanning native functions in package: {}", packageName);

        Set<Class<?>> classes = getClassesInPackage(packageName);
        int registered = 0;

        for (Class<?> clazz : classes) {
            if (ScriptNativeFunction.class.isAssignableFrom(clazz)) {
                Native annotation = clazz.getAnnotation(Native.class);
                if (annotation != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ScriptNativeFunction> functionClass = (Class<? extends ScriptNativeFunction>) clazz;

                        registry.registerNativeFunction(
                                functionClass,
                                annotation.name(),
                                annotation.definitions(),
                                annotation.description(),
                                annotation.examples()
                        );

                        registered++;
                        log.debug("Registered native function: {} ({})", annotation.name(), clazz.getSimpleName());

                    } catch (Exception e) {
                        log.warn("Failed to register native function: {}", clazz.getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Registered {} native functions from package {}", registered, packageName);
    }

    /**
     * Obtient toutes les classes dans un package donné.
     */
    private Set<Class<?>> getClassesInPackage(String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        String packagePath = packageName.replace('.', '/');

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packagePath);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if (resource.getProtocol().equals("file")) {
                    File directory = new File(resource.getFile());
                    if (directory.exists()) {
                        classes.addAll(getClassesFromDirectory(directory, packageName));
                    }
                } else if (resource.getProtocol().equals("jar")) {
                    classes.addAll(getClassesFromJar(resource, packagePath));
                }
            }

        } catch (Exception e) {
            log.warn("Failed to scan package: {}", packageName, e);
        }

        return classes;
    }

    private Set<Class<?>> getClassesFromDirectory(File directory, String packageName) {
        Set<Class<?>> classes = new HashSet<>();

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(getClassesFromDirectory(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        log.debug("Could not load class: {}", className);
                    }
                }
            }
        }

        return classes;
    }

    private Set<Class<?>> getClassesFromJar(URL jarUrl, String packagePath) {
        Set<Class<?>> classes = new HashSet<>();

        try {
            String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
            JarFile jarFile = new JarFile(jarPath);

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        log.debug("Could not load class: {}", className);
                    }
                }
            }

            jarFile.close();

        } catch (IOException e) {
            log.warn("Failed to read JAR file: {}", jarUrl, e);
        }

        return classes;
    }

    // ========================================
    // MÉTHODES POUR COMPATIBILITÉ FORGE
    // ========================================

    /**
     * Scanne un package spécifique pour un mod donné.
     * Utilisé pour scanner les annotations d'un mod spécifique.
     */
    public void scanPackage(String packageName) {
        log.info("Scanning package: {} for mod-specific components", packageName);

        try {
            // Scan ciblé pour ce package
            scanAndRegisterPrimitives(packageName);
            scanAndRegisterTypes(packageName);
            scanAndRegisterExpressions(packageName);
            scanAndRegisterActions(packageName);
            scanAndRegisterNativeFunctions(packageName);
            scanAndRegisterBlocks(packageName);
            scanAndRegisterEvents(packageName);
            scanAndRegisterLoops(packageName);

            log.info("Successfully scanned package: {}", packageName);
        } catch (Exception e) {
            log.error("Failed to scan package: {}", packageName, e);
        }
    }

    /**
     * Scanne à partir des données ASM de Forge pour compatibilité.
     * Utilisé pour l'intégration avec le système existant de SqriptForge.
     */
    public void scanFromASMData(SetMultimap<String, ASMDataTable.ASMData> modData) {
        if (modData == null || modData.isEmpty()) {
            log.warn("No ASM data provided for scanning");
            return;
        }

        log.info("Scanning from ASM data (Forge compatibility mode)");
        int totalRegistered = 0;

        try {
            // Traitement des primitives (EN PREMIER - important pour l'ordre)
            Set<ASMDataTable.ASMData> primitives = modData.get(Primitive.class.getName());
            if (primitives != null) {
                for (ASMDataTable.ASMData data : primitives) {
                    try {
                        processASMPrimitive(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process primitive from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            // Traitement des types
            Set<ASMDataTable.ASMData> types = modData.get(Type.class.getName());
            if (types != null) {
                for (ASMDataTable.ASMData data : types) {
                    try {
                        processASMType(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process type from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            // Traitement des expressions
            Set<ASMDataTable.ASMData> expressions = modData.get(Expression.class.getName());
            if (expressions != null) {
                for (ASMDataTable.ASMData data : expressions) {
                    try {
                        processASMExpression(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process expression from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            // Traitement des actions (avec vérification de côté)
            Set<ASMDataTable.ASMData> actions = modData.get(Action.class.getName());
            if (actions != null) {
                for (ASMDataTable.ASMData data : actions) {
                    try {
                        // Vérification de côté (side) comme dans modBuilding
                        if (data.getAnnotationInfo().get("side") != null) {
                            ModAnnotation.EnumHolder sideHolder = (ModAnnotation.EnumHolder) data.getAnnotationInfo().get("side");
                            fr.nico.sqript.structures.Side side = fr.nico.sqript.structures.Side.from(sideHolder.getValue());
                            if (!side.isValid()) {
                                log.debug("Skipping action {} due to invalid side: {}", data.getClassName(), side);
                                continue;
                            }
                        }

                        processASMAction(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process action from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            // Traitement des fonctions natives
            Set<ASMDataTable.ASMData> functions = modData.get(Native.class.getName());
            if (functions != null) {
                for (ASMDataTable.ASMData data : functions) {
                    try {
                        processASMNativeFunction(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process native function from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            // Traitement des blocs
            Set<ASMDataTable.ASMData> blocks = modData.get(Block.class.getName());
            if (blocks != null) {
                for (ASMDataTable.ASMData data : blocks) {
                    try {
                        processASMBlock(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process block from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            // Traitement des événements
            Set<ASMDataTable.ASMData> events = modData.get(Event.class.getName());
            if (events != null) {
                for (ASMDataTable.ASMData data : events) {
                    try {
                        processASMEvent(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process event from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            // Traitement des boucles
            Set<ASMDataTable.ASMData> loops = modData.get(Loop.class.getName());
            if (loops != null) {
                for (ASMDataTable.ASMData data : loops) {
                    try {
                        processASMLoop(data);
                        totalRegistered++;
                    } catch (Exception e) {
                        log.warn("Failed to process loop from ASM: {}", data.getClassName(), e);
                    }
                }
            }

            log.info("Successfully processed {} components from ASM data", totalRegistered);

        } catch (Exception e) {
            log.error("Failed to scan from ASM data", e);
        }
    }

    /**
     * Traite une primitive depuis les données ASM.
     */
    private void processASMPrimitive(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (PrimitiveType.class.isAssignableFrom(clazz)) {
            Primitive annotation = clazz.getAnnotation(Primitive.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends PrimitiveType<?>> primitiveClass = (Class<? extends PrimitiveType<?>>) clazz;

                String[] patterns = new String[]{annotation.pattern()};

                registry.registerPrimitive(
                        primitiveClass,
                        annotation.name(),
                        patterns
                );

                log.debug("Registered primitive from ASM: {} ({})", annotation.name(), clazz.getSimpleName());
            }
        }
    }

    /**
     * Traite un type depuis les données ASM.
     */
    private void processASMType(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (ScriptType.class.isAssignableFrom(clazz)) {
            Type annotation = clazz.getAnnotation(Type.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ScriptElement<?>> typeClass = (Class<? extends ScriptElement<?>>) clazz;

                registry.registerType(
                        typeClass,
                        annotation.name()
                );

                log.debug("Registered type from ASM: {} ({})", annotation.name(), clazz.getSimpleName());
            }
        }
    }

    /**
     * Traite une fonction native depuis les données ASM.
     */
    private void processASMNativeFunction(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (ScriptNativeFunction.class.isAssignableFrom(clazz)) {
            Native annotation = clazz.getAnnotation(Native.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ScriptNativeFunction> functionClass = (Class<? extends ScriptNativeFunction>) clazz;

                registry.registerNativeFunction(
                        functionClass,
                        annotation.name(),
                        annotation.definitions(),
                        annotation.description(),
                        annotation.examples()
                );

                log.debug("Registered native function from ASM: {} ({})", annotation.name(), clazz.getSimpleName());
            }
        }
    }

    /**
     * Traite une expression depuis les données ASM.
     */
    private void processASMExpression(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (ScriptExpression.class.isAssignableFrom(clazz)) {
            Expression annotation = clazz.getAnnotation(Expression.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ScriptExpression> expressionClass = (Class<? extends ScriptExpression>) clazz;

                registry.registerExpression(
                        expressionClass,
                        annotation.name(),
                        annotation.priority(),
                        annotation.features()
                );

                log.debug("Registered expression from ASM: {} ({})", annotation.name(), clazz.getSimpleName());
            }
        }
    }

    /**
     * Traite une action depuis les données ASM.
     */
    private void processASMAction(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (ScriptAction.class.isAssignableFrom(clazz)) {
            Action annotation = clazz.getAnnotation(Action.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ScriptAction> actionClass = (Class<? extends ScriptAction>) clazz;

                registry.registerAction(
                        actionClass,
                        annotation.name(),
                        annotation.priority(),
                        annotation.features()
                );

                log.debug("Registered action from ASM: {} ({})", annotation.name(), clazz.getSimpleName());
            }
        }
    }

    /**
     * Traite un bloc depuis les données ASM.
     */
    private void processASMBlock(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (ScriptBlock.class.isAssignableFrom(clazz)) {
            Block annotation = clazz.getAnnotation(Block.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ScriptBlock> blockClass = (Class<? extends ScriptBlock>) clazz;

                registry.registerBlock(
                        blockClass,
                        annotation.feature(),
                        annotation.fields(),
                        annotation.reloadable()
                );

                log.debug("Registered block from ASM: {} ({})", annotation.feature().name(), clazz.getSimpleName());
            }
        }
    }

    /**
     * Traite un événement depuis les données ASM.
     */
    private void processASMEvent(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (ScriptEvent.class.isAssignableFrom(clazz)) {
            Event annotation = clazz.getAnnotation(Event.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ScriptEvent> eventClass = (Class<? extends ScriptEvent>) clazz;

                registry.registerEvent(
                        eventClass,
                        annotation.feature(),
                        annotation.accessors()
                );

                log.debug("Registered event from ASM: {} ({})", annotation.feature().name(), clazz.getSimpleName());
            }
        }
    }

    /**
     * Traite une boucle depuis les données ASM.
     */
    private void processASMLoop(ASMDataTable.ASMData data) throws Exception {
        Class<?> clazz = Class.forName(data.getClassName());

        if (ScriptLoop.class.isAssignableFrom(clazz)) {
            Loop annotation = clazz.getAnnotation(Loop.class);
            if (annotation != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ScriptLoop> loopClass = (Class<? extends ScriptLoop>) clazz;

                registry.registerLoop(
                        loopClass,
                        annotation.name(),
                        annotation.pattern(),
                        annotation.side(),
                        annotation.priority()
                );

                log.debug("Registered loop from ASM: {} ({})", annotation.name(), clazz.getSimpleName());
            }
        }
    }
}





