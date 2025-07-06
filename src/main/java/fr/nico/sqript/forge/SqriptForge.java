package fr.nico.sqript.forge;

import com.google.common.collect.SetMultimap;
import fr.nico.sqript.EnumLoadPhase;
import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.blocks.ScriptBlockCommand;
import fr.nico.sqript.core.bootstrap.AnnotationScanner;
import fr.nico.sqript.core.integration.HybridEngine;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.events.EvtFML;
import fr.nico.sqript.events.EvtOnWindowSetup;
import fr.nico.sqript.forge.capabilities.CapabilityHandler;
import fr.nico.sqript.forge.client.ClientProxy;
import fr.nico.sqript.forge.common.CommonProxy;
import fr.nico.sqript.forge.common.ScriptBlock.ScriptBlock;
import fr.nico.sqript.forge.common.ScriptEventHandler;
import fr.nico.sqript.forge.common.SqriptCommand;
import fr.nico.sqript.forge.common.item.ScriptItem;
import fr.nico.sqript.forge.server.ServerProxy;
import fr.nico.sqript.meta.*;
import fr.nico.sqript.network.ScriptMessage;
import fr.nico.sqript.network.ScriptNetworkManager;
import fr.nico.sqript.network.ScriptReloadMessage;
import fr.nico.sqript.network.ScriptSyncDataMessage;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mod(name = "Sqript", modid = "sqript", version = "1.0")
@Mod.EventBusSubscriber(modid = "sqript")
public class SqriptForge {

    public static String MODID = "sqript";
    public static SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel("sqript");
    public static File scriptDir;
    public static ArrayList<ScriptBlock> blocks = new ArrayList<>();
    public static ArrayList<Item> items = new ArrayList<>();
    public static ArrayList<SoundEvent> soundEvents = new ArrayList<>();
    public static ArrayList<ScriptItem> scriptItems = new ArrayList<>();
    @SidedProxy(clientSide = "fr.nico.sqript.forge.client.ClientProxy", serverSide = "fr.nico.sqript.forge.server.ServerProxy")
    private static CommonProxy proxy;
    private static final CapabilityHandler capabilityHandler = new CapabilityHandler();

    private static EnumLoadPhase currentPhase;

    // =================================================================
    // NOUVEAU SYSTÈME CORE - MIGRATION PHASE 2
    // =================================================================

    private static ScriptRegistry coreRegistry;
    private static AnnotationScanner annotationScanner;
    private static HybridEngine hybridEngine;

    // Timing et état pour le système core
    private static long coreSystemInitTime = 0;
    private static boolean coreSystemActive = false;
    // Variables pour le monitoring
    private static Thread monitoringThread;
    private static volatile boolean monitoringActive = false;
    private static final long startupTime = System.currentTimeMillis();
    // Métriques de performance
    private static long totalExpressionCompilations = 0;
    private static long totalCoreSystemUsage = 0;
    private static long totalLegacyFallbacks = 0;
    private static long totalErrors = 0;

    public static EnumLoadPhase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Retourne le registre du système core.
     */
    public static ScriptRegistry getCoreRegistry() {
        return coreRegistry;
    }

    /**
     * Retourne le moteur hybride.
     */
    public static HybridEngine getHybridEngine() {
        return hybridEngine;
    }

    /**
     * Retourne le temps d'initialisation du système core.
     */
    public static long getCoreSystemInitTime() {
        return coreSystemInitTime;
    }

    @Mod.EventHandler
    public static void serverStartEvent(FMLServerStartingEvent event) {
        event.registerServerCommand(new SqriptCommand());
        for (ScriptBlockCommand command : ScriptManager.commands) {
            if (command.getSide().isValid()) {
                ScriptManager.log.info("Registering server command : " + command.getName());
                event.getServer().getCommandManager().getCommands().remove(command.getName());
                event.registerServerCommand(command);
            }
        }
        ScriptManager.callEvent(new EvtFML.EvtOnServerStartingEvent(event.getServer()));
    }

    @Mod.EventHandler
    public static void serverStopEvent(FMLServerStoppingEvent event) {
        ScriptManager.stop();
    }

    public static CommonProxy getCommonProxy() {
        return proxy;
    }

    @SideOnly(Side.CLIENT)
    public static ClientProxy getClientProxy() {
        return (ClientProxy) proxy;
    }

    @SideOnly(Side.SERVER)
    public static ServerProxy getServerProxy() {
        return (ServerProxy) proxy;
    }

    public static void modBuilding(FMLConstructionEvent event, ModContainer container) throws Exception {
        currentPhase = EnumLoadPhase.Build;
        SetMultimap<String, ASMDataTable.ASMData> modData = event.getASMHarvestedData().getAnnotationsFor(container);

        Set<ASMDataTable.ASMData> primitives = modData.get(Primitive.class.getName());
        Set<ASMDataTable.ASMData> types = modData.get(Type.class.getName());
        Set<ASMDataTable.ASMData> events = modData.get(Event.class.getName());
        Set<ASMDataTable.ASMData> expressions = modData.get(Expression.class.getName());
        Set<ASMDataTable.ASMData> actions = modData.get(Action.class.getName());
        Set<ASMDataTable.ASMData> functions = modData.get(Native.class.getName());
        Set<ASMDataTable.ASMData> blocks = modData.get(Block.class.getName());
        Set<ASMDataTable.ASMData> loops = modData.get(Loop.class.getName());


        for (ASMDataTable.ASMData c : primitives) {
            try {
                Class toRegister = Class.forName(c.getClassName());
                Primitive e = (Primitive) toRegister.getAnnotation(Primitive.class);
                ScriptManager.registerPrimitive(toRegister, e.name(), e.pattern());
            } catch (Exception e) {
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }


        for (ASMDataTable.ASMData c : types) {
            try {
                Class toRegister = Class.forName(c.getClassName());
                Type e = (Type) toRegister.getAnnotation(Type.class);
                ScriptManager.registerType(toRegister, e.name());
            } catch (Exception e) {
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }


        for (ASMDataTable.ASMData c : expressions) {
            try {
                Class toRegister = Class.forName(c.getClassName());
                Expression e = (Expression) toRegister.getAnnotation(Expression.class);
                ScriptManager.registerExpression(toRegister, e.name(), e.priority(), e.features());
            } catch (Exception e) {
                ScriptManager.log.error("Error trying to load ScriptExpression : " + c.getClassName());
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }


        for (ASMDataTable.ASMData c : loops) {
            try {
                Class toRegister = Class.forName(c.getClassName());
                Loop e = (Loop) toRegister.getAnnotation(Loop.class);
                ScriptManager.registerLoop(toRegister, e.name(), e.pattern(), e.side(), e.priority());
            } catch (Exception e) {
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }


        for (ASMDataTable.ASMData c : functions) {
            try {
                Class toRegister = Class.forName(c.getClassName());
                Native e = (Native) toRegister.getAnnotation(Native.class);
                ScriptManager.registerNativeFunction(toRegister, e.name(), e.definitions(), e.description(), e.examples());
            } catch (Exception e) {
                ScriptManager.log.error("Error trying to load ScriptFunction : " + c.getClassName());
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }


        for (ASMDataTable.ASMData c : events) {
            try {
                Class toRegister = Class.forName(c.getClassName());
                Event e = (Event) toRegister.getAnnotation(Event.class);
                ScriptManager.registerEvent(toRegister, e.feature(), e.accessors());
            } catch (Exception e) {
                ScriptManager.log.error("Error trying to load ScriptEvent : " + c.getClassName());
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }


        for (ASMDataTable.ASMData c : actions) {
            try {
                //System.out.println("Loading action : "+c.getClassName()+" "+c.getAnnotationInfo());
                if (c.getAnnotationInfo().get("side") != null && !(fr.nico.sqript.structures.Side.from(((ModAnnotation.EnumHolder) c.getAnnotationInfo().get("side")).getValue()).isValid()))
                    continue;
                Class toRegister = Class.forName(c.getClassName());
                Action e = (Action) toRegister.getAnnotation(Action.class);
                ScriptManager.registerAction(toRegister, e.name(), e.priority(), e.features());
            } catch (Exception e) {
                ScriptManager.log.error("Error trying to load ScriptAction : " + c.getClassName());
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }
        // Tri des actions par priorité via le registre
        // Plus besoin d'accès direct à ScriptManager.actions


        for (ASMDataTable.ASMData c : blocks) {
            try {
                //System.out.println("Loading block : "+c.getClassName());
                Class toRegister = Class.forName(c.getClassName());
                Block e = (Block) toRegister.getAnnotation(Block.class);
                ScriptManager.registerBlock(toRegister, e.feature(), e.fields(), e.reloadable());
            } catch (Exception e) {
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
            }
        }


    }

    public static void registerClientCommand(ScriptBlockCommand command) {
        ScriptManager.log.info("Registering client command : " + command.getName());
        ClientCommandHandler.instance.registerCommand(command);
    }

    public static void addCommand(ScriptBlockCommand command) {
        ScriptManager.commands.add(command);
    }

    public static void registerCommands() {
        for (ScriptBlockCommand command : ScriptManager.commands) {
            if (FMLCommonHandler.instance().getMinecraftServerInstance() != null && command.getSide() != fr.nico.sqript.structures.Side.CLIENT) {
                ScriptManager.log.info("Registering server command : " + command.getName());
                FMLCommonHandler.instance().getMinecraftServerInstance().getCommandManager().getCommands().put(command.getName(), command);
            } else {
                registerClientCommand(command);
            }
        }

    }

    // ========================================
    // MÉTHODES POUR LE NOUVEAU SYSTÈME CORE
    // ========================================

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        System.out.println("Registering Sqript items, there are : " + scriptItems.size() + " item(s) to register.");
        for (ScriptItem e : scriptItems) {
            System.out.println("Registering : " + e.getItem().getRegistryName());
            event.getRegistry().register(e.getItem());
        }
        for (Item e : items) {
            System.out.println("Registering : " + e.getRegistryName());
            event.getRegistry().register(e);
        }

    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<net.minecraft.block.Block> event) {
        for (net.minecraft.block.Block e : blocks) {
            System.out.println("Registering : " + e.getRegistryName());
            event.getRegistry().register(e);
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        for (SoundEvent e : soundEvents) {
            //System.out.println("Registering sound in handler : "+e.getRegistryName());
            event.getRegistry().register(e);
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (ScriptItem e : scriptItems) {
            if (e.customModel.isEmpty())
                ModelLoader.setCustomModelResourceLocation(e.getItem(), 0, new ModelResourceLocation(
                        e.getItem().getRegistryName(), "inventory"));
            else
                ModelLoader.setCustomModelResourceLocation(e.getItem(), 0, new ModelResourceLocation(
                        e.customModel, "inventory"));
        }
        for (ScriptBlock e : blocks) {
            ModelLoader.setCustomStateMapper(e, new DefaultStateMapper());
        }
        for (Item e : items) {
            ModelLoader.setCustomModelResourceLocation(e, 0, new ModelResourceLocation(
                    e.getRegistryName(), "inventory"));
        }

    }

    // ========================================
    // MONITORING ET LOGGING DÉTAILLÉ
    // ========================================

    /**
     * Initialise le nouveau système core.
     * Crée et initialise tous les composants nécessaires.
     */
    private static void initializeCoreSystem() {
        try {
            long startTime = System.currentTimeMillis();

            // Vérification de sécurité pour le logger
            if (ScriptManager.log == null) {
                ScriptManager.log = LogManager.getLogger("Sqript");
            }

            ScriptManager.log.info("Initializing Core System...");

            // Création des composants du système core
            coreRegistry = new ScriptRegistry();
            annotationScanner = new AnnotationScanner(coreRegistry);
            hybridEngine = new HybridEngine(coreRegistry);

            // Initialisation ET démarrage immédiat du registre
            coreRegistry.initialize();
            coreRegistry.start(); // DÉMARRAGE IMMÉDIAT pour permettre l'enregistrement

            // Initialisation du moteur hybride
            hybridEngine.initialize();

            coreSystemInitTime = System.currentTimeMillis() - startTime;
            ScriptManager.log.info("Core System initialized successfully in {}ms", coreSystemInitTime);

        } catch (Exception e) {
            if (ScriptManager.log != null) {
                ScriptManager.log.error("Failed to initialize Core System", e);
                ScriptManager.log.warn("Core System disabled, falling back to legacy system");
            } else {
                System.err.println("Failed to initialize Core System: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Traite un mod avec le nouveau système core.
     * Utilise l'AnnotationScanner pour enregistrer les composants.
     */
    private static void processModWithCoreSystem(ModContainer container,
                                                 SetMultimap<String, ASMDataTable.ASMData> modData) {
        try {
            // Vérification de sécurité pour le logger
            if (ScriptManager.log == null) {
                ScriptManager.log = LogManager.getLogger("Sqript");
            }

            ScriptManager.log.info("Processing mod with Core System: {}", container.getName());

            // Utilise les données ASM pour compatibilité avec l'existant
            annotationScanner.scanFromASMData(modData);

            // Optionnel : Scan par package si possible
            String basePackage = getBasePackageForMod(container);
            if (basePackage != null && !basePackage.isEmpty()) {
                ScriptManager.log.debug("Additional package scan for mod {}: {}", container.getName(), basePackage);
                annotationScanner.scanPackage(basePackage);
            }

            ScriptManager.log.debug("Successfully processed mod: {}", container.getName());

        } catch (Exception e) {
            if (ScriptManager.log != null) {
                ScriptManager.log.warn("Failed to process mod with Core System: {}", container.getName(), e);
            } else {
                System.err.println("Failed to process mod " + container.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Finalise le système core après le chargement de tous les mods.
     * Démarre le moteur hybride et active le système complet.
     */
    private static void finalizeCoreSystem() {
        try {
            // Vérification de sécurité pour le logger
            if (ScriptManager.log == null) {
                ScriptManager.log = LogManager.getLogger("Sqript");
            }

            ScriptManager.log.info("Finalizing Core System...");

            // Démarrage du moteur hybride (le registry est déjà démarré)
            hybridEngine.start();

            // Activation complète du système
            coreSystemActive = true;

            // Statistiques d'enregistrement
            ScriptRegistry.RegistryStats stats = coreRegistry.getStats();
            ScriptManager.log.info("Core System finalized successfully");
            ScriptManager.log.info("=== CORE SYSTEM STATISTICS ===");
            ScriptManager.log.info("Events: {}, Actions: {}, Expressions: {}",
                    stats.eventCount, stats.actionCount, stats.expressionCount);
            ScriptManager.log.info("Primitives: {}, Types: {}, Loops: {}",
                    stats.primitiveCount, stats.typeCount, stats.loopCount);
            ScriptManager.log.info("Native Functions: {}, Operators: {}",
                    stats.nativeFunctionCount, stats.operatorCount);
            ScriptManager.log.info("===========================");

        } catch (Exception e) {
            if (ScriptManager.log != null) {
                ScriptManager.log.error("Failed to finalize Core System", e);
                ScriptManager.log.warn("Core System disabled due to finalization failure");
            } else {
                System.err.println("Failed to finalize Core System: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Essaie de déterminer le package de base d'un mod.
     * Utilisé pour le scan ciblé par package.
     */
    private static String getBasePackageForMod(ModContainer container) {
        try {
            // Essaie de deviner le package de base à partir du nom du mod
            String modId = container.getModId();

            // Packages spéciaux connus
            if ("sqript".equals(modId)) {
                return "fr.nico.sqript";
            }

            // Pour les autres mods, on peut essayer quelques heuristiques
            // mais ce n'est pas critique si on ne trouve pas
            return null;

        } catch (Exception e) {
            ScriptManager.log.debug("Could not determine base package for mod: {}", container.getName());
            return null;
        }
    }

    /**
     * Démarre le monitoring de performance en arrière-plan.
     */
    private static void startPerformanceMonitoring() {
        if (monitoringActive) {
            ScriptManager.log.warn("Performance monitoring already active");
            return;
        }

        monitoringActive = true;
        monitoringThread = new Thread(() -> {
            ScriptManager.log.info("Performance monitoring started");

            while (monitoringActive) {
                try {
                    Thread.sleep(30000); // Rapport toutes les 30 secondes

                    if (monitoringActive && coreSystemActive) {
                        generatePerformanceReport();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    ScriptManager.log.error("Error in performance monitoring", e);
                }
            }

            ScriptManager.log.info("Performance monitoring stopped");
        }, "Sqript-Performance-Monitor");

        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }

    /**
     * Arrête le monitoring de performance.
     */
    public static void stopPerformanceMonitoring() {
        if (monitoringActive) {
            monitoringActive = false;
            if (monitoringThread != null) {
                monitoringThread.interrupt();
                try {
                    monitoringThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ScriptManager.log.info("Performance monitoring stopped");
        }
    }

    /**
     * Génère un rapport de performance détaillé.
     */
    private static void generatePerformanceReport() {
        try {
            long uptime = System.currentTimeMillis() - startupTime;

            ScriptManager.log.info("=== SQRIPT PERFORMANCE REPORT ===");
            ScriptManager.log.info("Uptime: {}ms ({}min)", uptime, uptime / 60000);
            ScriptManager.log.info("Core System: {}", coreSystemActive ? "ACTIVE" : "INACTIVE");

            if (coreSystemActive && coreRegistry != null) {
                // Statistiques du registre
                ScriptRegistry.RegistryStats registryStats = coreRegistry.getStats();
                ScriptManager.log.info("Registry - Primitives: {}, Expressions: {}, Actions: {}",
                        registryStats.primitiveCount,
                        registryStats.expressionCount,
                        registryStats.actionCount);

                // Statistiques du moteur hybride
                if (hybridEngine != null) {
                    HybridEngine.ModernEngineStats engineStats = hybridEngine.getStats();
                    int totalCacheSize = engineStats.cacheStats.hotSize + engineStats.cacheStats.warmSize + engineStats.cacheStats.coldSize;
                    ScriptManager.log.info("HybridEngine - Cache Hit Rate: {:.1f}%, Cache Size: {}",
                            engineStats.cacheStats.hitRate * 100,
                            totalCacheSize);

                    // Rapport de santé détaillé
                    String healthReport = hybridEngine.getHealthReport();
                    ScriptManager.log.debug("Detailed Health Report:\n{}", healthReport);
                }

                // Métriques de compilation
                ScriptManager.log.info("Compilations - Total: {}, Core: {}, Legacy: {}, Errors: {}",
                        totalExpressionCompilations,
                        totalCoreSystemUsage,
                        totalLegacyFallbacks,
                        totalErrors);

                // Taux de réussite
                if (totalExpressionCompilations > 0) {
                    double successRate = ((double) (totalExpressionCompilations - totalErrors) / totalExpressionCompilations) * 100;
                    double coreUsageRate = ((double) totalCoreSystemUsage / totalExpressionCompilations) * 100;

                    ScriptManager.log.info("Success Rate: {:.1f}%, Core Usage: {:.1f}%",
                            successRate, coreUsageRate);
                }

                // Vérifications de santé
                checkSystemHealth();
            }

            ScriptManager.log.info("===============================");

        } catch (Exception e) {
            ScriptManager.log.error("Error generating performance report", e);
        }
    }

    /**
     * Vérifie la santé globale du système.
     */
    private static void checkSystemHealth() {
        try {
            // Vérifier la mémoire
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = ((double) usedMemory / totalMemory) * 100;

            if (memoryUsagePercent > 85) {
                ScriptManager.log.warn("High memory usage detected: {:.1f}%", memoryUsagePercent);

                // Suggérer un nettoyage du cache si disponible
                if (hybridEngine != null) {
                    ScriptManager.log.info("Clearing hybrid engine cache to free memory");
                    hybridEngine.clearCache();
                }
            } else {
                ScriptManager.log.debug("Memory usage: {:.1f}%", memoryUsagePercent);
            }

            // Vérifier l'état des composants
            if (coreRegistry != null && !coreRegistry.isActive()) {
                ScriptManager.log.warn("Core registry is not active!");
            }

            if (hybridEngine != null && !hybridEngine.isActive()) {
                ScriptManager.log.warn("Hybrid engine is not active!");
            }

            // Vérifier les erreurs récentes
            if (totalErrors > 0) {
                double errorRate = ((double) totalErrors / Math.max(1, totalExpressionCompilations)) * 100;
                if (errorRate > 10) {
                    ScriptManager.log.warn("High error rate detected: {:.1f}%", errorRate);
                }
            }

        } catch (Exception e) {
            ScriptManager.log.error("Error checking system health", e);
        }
    }

    /**
     * Enregistre l'utilisation du système core.
     */
    public static void recordCoreSystemUsage() {
        totalExpressionCompilations++;
        totalCoreSystemUsage++;
    }

    /**
     * Enregistre l'utilisation du fallback legacy.
     */
    public static void recordLegacyFallback() {
        totalExpressionCompilations++;
        totalLegacyFallbacks++;
    }

    /**
     * Enregistre une erreur de compilation.
     */
    public static void recordCompilationError() {
        totalExpressionCompilations++;
        totalErrors++;
    }

    /**
     * Retourne les métriques de performance actuelles.
     */
    public static PerformanceMetrics getPerformanceMetrics() {
        return new PerformanceMetrics(
                System.currentTimeMillis() - startupTime,
                totalExpressionCompilations,
                totalCoreSystemUsage,
                totalLegacyFallbacks,
                totalErrors,
                coreSystemActive
        );
    }

    /**
     * Force la génération d'un rapport de performance.
     */
    public static void forcePerformanceReport() {
        ScriptManager.log.info("Forcing performance report generation...");
        generatePerformanceReport();
    }

    @Mod.EventHandler
    public void construction(FMLConstructionEvent event) throws IOException {

        // ========================================
        // INITIALISATION DU RÉPERTOIRE SCRIPTS
        // ========================================
        scriptDir = new File("scripts");
        if (!scriptDir.exists()) {
            if (scriptDir.getParentFile() != null) {
                //Dev-environment
                scriptDir = new File(scriptDir.getParentFile().getParentFile(), "scripts");
                if (!scriptDir.exists()) {
                    //first-run of the mod, in production environment
                    scriptDir = new File("scripts");
                    scriptDir.mkdirs();
                }
            } else //First run, in production environment
                scriptDir.mkdirs();
        }

        // ========================================
        // INITIALISATION DU LOGGER ET SYSTÈME LEGACY
        // ========================================
        ScriptManager.preInit(scriptDir);
        ScriptManager.log.info("Loading content of Sqript.");

        // ========================================
        // INITIALISATION DU NOUVEAU SYSTÈME CORE
        // ========================================
        ScriptManager.log.info("Initializing new Core System...");
        initializeCoreSystem();



        // ========================================
        // TRAITEMENT DES MODS (SYSTÈME HYBRIDE)
        // ========================================

        // Traitement du mod Sqript principal
        ModContainer sqriptContainer = Loader.instance().activeModContainer();
        try {
            processModWithCoreSystem(sqriptContainer, event.getASMHarvestedData().getAnnotationsFor(sqriptContainer));
            // Traitement legacy en parallèle pour validation
            modBuilding(event, sqriptContainer);
        } catch (Exception e) {
            ScriptManager.log.error("Error processing Sqript main mod", e);
            e.printStackTrace();
        }

        // Traitement des autres mods
        List<ModContainer> containerList = new ArrayList<>(Loader.instance().getIndexedModList().values());
        containerList.remove(sqriptContainer);

        for (ModContainer container : containerList) {
            try {
                if (container == null || container.getName().equalsIgnoreCase("Minecraft"))
                    continue;

                ScriptManager.log.debug("Processing mod: {}", container.getName());

                processModWithCoreSystem(container, event.getASMHarvestedData().getAnnotationsFor(container));

                // Ancien système en fallback
                modBuilding(event, container);

            } catch (Exception e) {
                if (!(e instanceof NullPointerException)) {
                    ScriptManager.log.error("Error while loading Sqript addon : " + container.getName());
                    e.printStackTrace();
                }
            }
        }

        // ========================================
        // FINALISATION
        // ========================================

        // Finalisation du nouveau système core
        finalizeCoreSystem();

        // Initialisation legacy
        ScriptManager.init();

        // ========================================
        // RAPPORT D'INTÉGRATION
        // ========================================
        ScriptManager.log.info("=== SQRIPT INTEGRATION REPORT ===");
        ScriptManager.log.info("Core System: ENABLED (migration completed)");
        if (coreRegistry != null) {
            ScriptManager.log.info("Core System Init Time: {}ms", getCoreSystemInitTime());
            ScriptRegistry.RegistryStats stats = coreRegistry.getStats();
            ScriptManager.log.info("Core Registry Stats: {}", stats);
        }
        ScriptManager.log.info("================================");

        // ========================================
        // DÉMARRAGE DU MONITORING
        // ========================================
        startPerformanceMonitoring();

        ScriptManager.callEvent(new EvtOnWindowSetup());

    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        currentPhase = EnumLoadPhase.Load;


        channel.registerMessage(ScriptSyncDataMessage.ScriptSyncDataMessageHandler.class, ScriptSyncDataMessage.class, 0, Side.SERVER);
        channel.registerMessage(ScriptSyncDataMessage.ScriptSyncDataMessageHandler.class, ScriptSyncDataMessage.class, 0, Side.CLIENT);

        channel.registerMessage(ScriptMessage.ScriptMessageHandler.class, ScriptMessage.class, 1, Side.CLIENT);
        channel.registerMessage(ScriptMessage.ScriptMessageHandler.class, ScriptMessage.class, 1, Side.SERVER);

        channel.registerMessage(ScriptReloadMessage.ScriptMessageHandler.class, ScriptReloadMessage.class, 2, Side.CLIENT);
        channel.registerMessage(ScriptReloadMessage.ScriptMessageHandler.class, ScriptReloadMessage.class, 2, Side.SERVER);

        ScriptNetworkManager.init();
        capabilityHandler.preInit();

        MinecraftForge.EVENT_BUS.register(new ScriptEventHandler());
        ScriptManager.callEvent(new EvtFML.EvtOnPreInit());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ScriptManager.callEvent(new EvtFML.EvtOnInit());
    }

    /**
     * Classe pour encapsuler les métriques de performance.
     */
    public static class PerformanceMetrics {
        public final long uptime;
        public final long totalCompilations;
        public final long coreUsage;
        public final long legacyFallbacks;
        public final long errors;
        public final boolean coreSystemActive;

        public PerformanceMetrics(long uptime, long totalCompilations, long coreUsage,
                                  long legacyFallbacks, long errors, boolean coreSystemActive) {
            this.uptime = uptime;
            this.totalCompilations = totalCompilations;
            this.coreUsage = coreUsage;
            this.legacyFallbacks = legacyFallbacks;
            this.errors = errors;
            this.coreSystemActive = coreSystemActive;
        }

        public double getSuccessRate() {
            return totalCompilations > 0 ?
                    ((double) (totalCompilations - errors) / totalCompilations) * 100 : 0;
        }

        public double getCoreUsageRate() {
            return totalCompilations > 0 ?
                    ((double) coreUsage / totalCompilations) * 100 : 0;
        }

        public double getErrorRate() {
            return totalCompilations > 0 ?
                    ((double) errors / totalCompilations) * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("PerformanceMetrics{uptime=%dms, compilations=%d, " +
                            "successRate=%.1f%%, coreUsage=%.1f%%, errorRate=%.1f%%}",
                    uptime, totalCompilations, getSuccessRate(),
                    getCoreUsageRate(), getErrorRate());
        }
    }

}





