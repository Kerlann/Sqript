package fr.nico.sqript.structures;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.blocks.ScriptBlock;
import fr.nico.sqript.blocks.ScriptBlockEvent;
import fr.nico.sqript.blocks.ScriptFunctionalBlock;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.events.EvtBlock;
import fr.nico.sqript.events.ScriptEvent;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.BlockDefinition;
import fr.nico.sqript.types.primitive.TypeBoolean;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScriptInstance {

    private final Map<String, ScriptExpression> options = new HashMap<>();

    private final List<ScriptBlock> blocks = new ArrayList<>();
    private final File scriptFile;
    private final String name;

    public ScriptInstance(String name, File scriptFile) throws Exception {
        this.name = name;
        this.scriptFile = scriptFile;

        // Parser le contenu du fichier
        parseScriptFile();
    }

    /**
     * Parse le contenu du fichier de script et crée les blocs correspondants.
     */
    private void parseScriptFile() throws Exception {
        if (!scriptFile.exists()) {
            throw new ScriptException.ScriptSyntaxException(new ScriptToken("", 0, this), "Script file does not exist: " + scriptFile.getPath());
        }

        ScriptManager.log.info("Loading script: " + name);

        try {
            // Lire le contenu du fichier
            List<String> lines = Files.readAllLines(scriptFile.toPath());

            // Créer les tokens
            List<ScriptToken> tokens = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    tokens.add(new ScriptToken(line, i + 1, this));
                }
            }

            // Nouveau système : parser les tokens en blocs (indentation)
            if (!tokens.isEmpty()) {
                List<List<ScriptToken>> blocks = parseBlocksFromTokens(tokens);
                
                // Pour chaque bloc, détecter dynamiquement le type de bloc et l'instancier proprement
                for (List<ScriptToken> block : blocks) {
                    if (block.isEmpty()) continue;
                    ScriptToken headerToken = block.get(0);
                    
                    ScriptManager.log.debug("Processing block starting with: '{}' with {} total tokens", headerToken.getText(), block.size());
                    
                    // Debug: afficher tous les tokens du bloc
                    for (int i = 0; i < block.size(); i++) {
                        ScriptManager.log.debug("  Token {}: '{}'", i, block.get(i).getText());
                    }
                    
                    // 1. Détection du type de bloc via le ScriptRegistry
                    BlockDefinition matchedBlockDef = null;
                    for (BlockDefinition def : ScriptManager.getBlocks()) {
                        if (def.getMatchedPatternIndex(headerToken.getText()) != -1) {
                            matchedBlockDef = def;
                            break;
                        }
                    }
                    if (matchedBlockDef == null) {
                        ScriptManager.log.warn("Aucun type de bloc reconnu pour la ligne {} : {}", headerToken.getLineNumber(), headerToken.getText());
                        continue;
                    }

                    // 2. Instanciation dynamique du bloc
                    ScriptBlock scriptBlock;
                    try {
                        scriptBlock = matchedBlockDef.getBlockClass().getConstructor(ScriptToken.class).newInstance(headerToken);
                    } catch (Exception e) {
                        ScriptManager.log.error("Erreur d'instanciation du bloc {} à la ligne {} : {}", matchedBlockDef.getBlockClass().getSimpleName(), headerToken.getLineNumber(), e.getMessage());
                        continue;
                    }

                    // 3. Création du ScriptLineBlock avec tous les tokens du bloc
                    ScriptBlock.ScriptLineBlock lineBlock = new ScriptBlock.ScriptLineBlock("", block);

                    // 4. Initialisation complète du bloc (groupFields, compilation, enregistrement, etc.)
                    try {
                        scriptBlock.init(lineBlock);
                    } catch (Exception e) {
                        ScriptManager.log.error("Erreur d'initialisation du bloc {} à la ligne {} : {}", matchedBlockDef.getBlockClass().getSimpleName(), headerToken.getLineNumber(), e.getMessage());
                        if (ScriptManager.FULL_DEBUG) {
                            e.printStackTrace();
                        }
                        continue;
                    }

                    // 5. Ajout à la liste des blocs du script (si pas déjà fait dans init/load)
                    if (!this.blocks.contains(scriptBlock)) {
                        this.blocks.add(scriptBlock);
                    }
                }
            }

        } catch (Exception e) {
            ScriptManager.log.error("Error loading script " + name + ": " + e.getMessage());
            if (e instanceof ScriptException) {
                throw e;
            } else {
                throw new ScriptException.ScriptSyntaxException(new ScriptToken("", 0, this), "Failed to parse script: " + e.getMessage());
            }
        }
    }
    
    /**
     * Parse une liste de tokens en blocs en respectant l'indentation.
     * Chaque bloc commence par une ligne de niveau 0 et contient toutes les lignes indentées qui suivent.
     */
    private List<List<ScriptToken>> parseBlocksFromTokens(List<ScriptToken> tokens) {
        List<List<ScriptToken>> blocks = new ArrayList<>();
        List<ScriptToken> currentBlock = new ArrayList<>();
        
        for (ScriptToken token : tokens) {
            int tabLevel = fr.nico.sqript.SqriptUtils.getTabLevel(token.getText());
            
            ScriptManager.log.debug("Processing token '{}' with tabLevel {}", token.getText(), tabLevel);
            
            if (tabLevel == 0) {
                // Nouvelle ligne de niveau 0 = nouveau bloc
                if (!currentBlock.isEmpty()) {
                    ScriptManager.log.debug("Closing previous block with {} tokens", currentBlock.size());
                    blocks.add(new ArrayList<>(currentBlock));
                    currentBlock.clear();
                }
                ScriptManager.log.debug("Starting new block with header: '{}'", token.getText());
                currentBlock.add(token);
            } else {
                // Ligne indentée = appartient au bloc actuel
                if (!currentBlock.isEmpty()) {
                    ScriptManager.log.debug("Adding indented line to current block: '{}'", token.getText());
                    currentBlock.add(token);
                } else {
                    // Ligne indentée sans bloc parent - erreur de syntaxe
                    ScriptManager.log.warn("Ligne indentée sans bloc parent à la ligne {} : {}", token.getLineNumber(), token.getText());
                }
            }
        }
        
        // Ajouter le dernier bloc
        if (!currentBlock.isEmpty()) {
            blocks.add(currentBlock);
        }
        
        // Log pour debug
        ScriptManager.log.debug("Parsed {} blocks from {} tokens", blocks.size(), tokens.size());
        for (int i = 0; i < blocks.size(); i++) {
            List<ScriptToken> block = blocks.get(i);
            ScriptManager.log.debug("  Block {}: '{}' with {} tokens", i, block.get(0).getText(), block.size());
            for (int j = 1; j < block.size(); j++) {
                ScriptManager.log.debug("    - '{}'", block.get(j).getText());
            }
        }
        
        return blocks;
    }

    public File getScriptFile() {
        return scriptFile;
    }


    public Map<String, ScriptExpression> getOptions() {
        return options;
    }

    public boolean callEvent(ScriptContext context, ScriptEvent event) {

        try {
            context.setReturnValue(new ScriptTypeAccessor(TypeBoolean.FALSE(), ""));
            ScriptContext returnContext = callEventAndGetContext(context, event);
            return (boolean) returnContext.getReturnValue().element.getObject();
        } catch (Exception e) {
            ScriptManager.log.error("Error while calling event : " + event.getClass().getSimpleName());
            e.printStackTrace();
            if (e instanceof ScriptException.ScriptWrappedException) {
                Throwable ex = ((ScriptException.ScriptWrappedException) (e)).getWrapped();
                ScriptManager.log.error(((ScriptException.ScriptWrappedException) (e)).getLine() + " : " + ex.getMessage());
                e.printStackTrace();
            } else if (e instanceof ScriptException) {
                if (ScriptManager.FULL_DEBUG) e.printStackTrace();
                for (String s : e.getMessage().split("\n"))
                    ScriptManager.log.error(s);
            }
        }
        return false;
    }

    public List<ScriptBlock> getBlocksOfClass(Class<? extends ScriptBlock> type) {
        return blocks.stream().filter(a -> a.getClass().isAssignableFrom(type)).collect(Collectors.toList());
    }

    public ScriptFunctionalBlock getFunction(String name) {
        //System.out.println("Getting function for : "+name);
        for (ScriptBlock f : blocks) {
            if (f instanceof ScriptFunctionalBlock) {
                //System.out.println("- "+((ScriptFunctionalBlock) f).name +  " " +f.getHead()+" "+f);
                ScriptFunctionalBlock function = (ScriptFunctionalBlock) f;
                //System.out.println("Comparing "+name+" "+((ScriptFunctionalBlock) f).name+" "+function.name.equals(name));
                if (function.name.equals(name)) {
                    //System.out.println("Returning "+function);
                    return function;
                }
            }
        }
        return null;
    }

    public ScriptContext callEventAndGetContext(ScriptContext context, ScriptEvent event) throws ScriptException {
        //long t1 = //System.currentTimeMillis();
        //System.out.println("Calling event and get context");

        for (ScriptBlock b : getBlocksOfClass(ScriptBlockEvent.class)) {
            ScriptBlockEvent scriptBlockEvent = (ScriptBlockEvent) b;
            if (event instanceof EvtBlock.EvtOnBlockBreak) {
                //System.out.println(scriptBlockEvent.eventType+" "+event.getClass());
            }
            if (scriptBlockEvent.eventType == event.getClass() && event.check(scriptBlockEvent.getParameters(), scriptBlockEvent.getMarks()) && scriptBlockEvent.side.isEffectivelyValid()) {
                //System.out.println("Calling event : "+event.getClass().getSimpleName()+" with accessors "+ Arrays.toString(event.getAccessors()));
                ScriptClock clock = new ScriptClock(context);
                context.wrap(event.getAccessors());
                clock.start(scriptBlockEvent);
                //System.out.println("Finished ! It took : " + (System.currentTimeMillis()) + " ms");
            }
        }
        return context;
    }

    public String getName() {
        return name;
    }


    public void registerBlock(ScriptBlock block) {
        //System.out.println("Adding block : "+block.getClass());
        blocks.add(block);
    }

    public List<ScriptBlock> getBlocks() {
        return blocks;
    }

}





