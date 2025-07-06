package fr.nico.sqript.blocks;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.compilation.CompilationResult;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.expressions.ScriptExpression;
import fr.nico.sqript.meta.Block;
import fr.nico.sqript.structures.IScript;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.structures.ScriptInstance;
import fr.nico.sqript.structures.Side;
import fr.nico.sqript.types.ScriptType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ScriptBlock extends IScript {

    private ScriptLineBlock mainField;
    private IScript root;
    private ScriptToken head;
    private ScriptInstance scriptInstance;
    private Side side = Side.BOTH;

    public ScriptBlock() {
    }

    /**
     * Be careful, no error must be thrown during the call of constructor.
     *
     * @param head The very first line of the block (e.g : "on jump:")
     * @throws ScriptException.ScriptSyntaxException When the head of the block doesn't match the required pattern
     */
    public ScriptBlock(ScriptToken head) throws ScriptException.ScriptSyntaxException {
        this.head = head;
        scriptInstance = head.getScriptInstance();
    }

    public ScriptToken getHead() {
        return head;
    }

    public ScriptLineBlock getMainField() {
        return mainField;
    }

    public void setMainField(ScriptLineBlock mainField) {
        this.mainField = mainField;
    }

    public ScriptInstance getScriptInstance() {
        return scriptInstance;
    }

    public void setScriptInstance(ScriptInstance scriptInstance) {
        this.scriptInstance = scriptInstance;
    }

    public ScriptLineBlock getSubBlock(String label) {
        for (ScriptLineBlock f : mainField.getSubBlocks()) {
            //System.out.println("Looping on block : "+f.getLabel());
            if (f.getLabel().equalsIgnoreCase(label))
                return f;
        }
        return null;
    }

    public boolean fieldDefined(String label) {
        return getSubBlock(label) != null;
    }

    /**
     * Should not be overridden, except for very special behaviors.
     *
     * @param block The full ScriptLineBlock of the block
     * @throws Exception
     */
    public void init(ScriptLineBlock block) throws Exception {
        if (block == null) {
            throw new ScriptException.ScriptSyntaxException(getHead(), "ScriptLineBlock is null");
        }
        if (block.getContent() == null) {
            throw new ScriptException.ScriptSyntaxException(getHead(), "ScriptLineBlock content is null");
        }
        groupFields(block.content);
        load();
    }

    public String shiftIndentation(String str, int lvl) {
        for (int i = 0; i < lvl; i++) {
            if (str.charAt(0) == '\t') {
                str = str.substring(1);
            }
        }
        return str;
    }

    /**
     * Generates the sub-blocks within this block
     *
     * @param block The complete block to work with
     * @throws ScriptException.ScriptIndentationErrorException When a line is indented while it shouldn't
     * @throws ScriptException.ScriptMissingTokenException     When a sub-block is empty
     */
    protected void groupFields(List<ScriptToken> block) throws ScriptException.ScriptIndentationErrorException, ScriptException.ScriptMissingTokenException {
        try {
            ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Starting with " + (block != null ? block.size() : "null") + " tokens");

            Pattern getLabel = Pattern.compile("\\s*([\\w ]*)(?: )?:(.*)");
            String currentLabel = "";
            ScriptToken next;
            setMainField(new ScriptLineBlock(currentLabel, new ArrayList<>()));

            ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Available fields for " + this.getClass().getSimpleName() + ": " +
                    Arrays.asList(this.getClass().getAnnotation(Block.class).fields()).stream().map(a -> a.name()).collect(Collectors.toList()));

            while (!block.isEmpty()) {
                next = block.remove(0);
                ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Processing token: '" + next.getText() + "'");

                //System.out.println(next);
                //Indentation error
                if (fr.nico.sqript.SqriptUtils.getTabLevel(next.getText()) > 1) {
                    ScriptManager.log.error("DEBUG ScriptBlock.groupFields(): Indentation error on: '" + next.getText() + "'");
                    throw new ScriptException.ScriptIndentationErrorException(next);
                }

                //System.out.println("this fields : "+Arrays.asList(this.getClass().getAnnotation(Block.class).fields()));
                Matcher m = getLabel.matcher(next.getText());
                if (m.find() && Arrays.asList(this.getClass().getAnnotation(Block.class).fields()).stream().map(a -> a.name()).collect(Collectors.toList()).contains(currentLabel = m.group(1))) {
                    ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Found field label: '" + currentLabel + "'");

                    //Found the description of a sub-block

                    List<ScriptToken> content = new ArrayList<>();

                    if (m.groupCount() < 2 || !m.group(2).isEmpty())
                        content.add(next.with(m.group(2)));

                    while (!block.isEmpty() && fr.nico.sqript.SqriptUtils.getTabLevel(block.get(0).getText()) > 1) {
                        next = block.remove(0);
                        next = next.with(shiftIndentation(next.getText(), 2));
                        content.add(next);
                    }

                    if (content.size() == 0) {
                        ScriptManager.log.error("DEBUG ScriptBlock.groupFields(): Empty field content for: '" + currentLabel + "'");
                        throw new ScriptException.ScriptMissingTokenException(next);
                    } else {
                        ScriptLineBlock field = new ScriptLineBlock(currentLabel, content);
                        getMainField().addSubBlock(field);
                        ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Added field: '" + currentLabel + "' with " + content.size() + " tokens");
                    }
                } else {
                    ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Not a field label, adding to main content: '" + next.getText() + "'");

                    // Correction : ignorer la première ligne (en-tête du bloc)
                    List<ScriptToken> content = new ArrayList<>();
                    // On saute la première ligne (l'en-tête)
                    if (!block.isEmpty()) {
                        next = block.remove(0);
                    }
                    // On ajoute le reste des lignes (actions/expressions)
                    while (!block.isEmpty()) {
                        next = block.remove(0);
                        String line = shiftIndentation(next.getText(), 1).trim();
                        // On ignore les lignes vides ou qui ressemblent à une déclaration de bloc
                        if (!line.isEmpty() && !line.matches("^(command |function |event |on |for |while ).*")) {
                            content.add(next.with(line));
                            ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Added to main content: '" + line + "'");
                        }
                    }
                    getMainField().setContent(content);
                    ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Set main field content with " + content.size() + " tokens");
                    return;
                }

            }
            ScriptManager.log.info("DEBUG ScriptBlock.groupFields(): Finished grouping fields");
        } catch (Exception e) {
            ScriptManager.log.error("DEBUG ScriptBlock.groupFields(): Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    protected void load() throws Exception {
    }

    public IScript getRoot() {
        return root;
    }

    public void setRoot(IScript root) {
        this.root = root;
    }

    @Override
    public IScript run(ScriptContext context) {
        return getRoot();
    }

    @Override
    public void execute(ScriptContext context) {
    }

    @Override
    public IScript getNext(ScriptContext context) {
        return null;
    }

    @Override
    public IScript getParent() {
        return null;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public void displayTree(int i) {
        ScriptManager.dispScriptTree(getRoot(), i);
    }

    public static class ScriptLineBlock {

        private final List<ScriptLineBlock> subBlocks = new ArrayList<>();
        private List<ScriptToken> content;
        private String label;

        public ScriptLineBlock(String label, List<ScriptToken> block) {
            setLabel(label);
            setContent(block);
        }

        public List<ScriptLineBlock> getSubBlocks() {
            return subBlocks;
        }

        public void addSubBlock(ScriptLineBlock subBlock) {
            this.subBlocks.add(subBlock);
        }

        public List<ScriptToken> getContent() {
            return content;
        }

        public void setContent(List<ScriptToken> content) {
            this.content = content;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getString() throws Exception {
            return (String) evaluate().getObject();
        }

        public Float getFloat() throws Exception {
            return (Float) evaluate().getObject();
        }

        public Double getDouble() throws Exception {
            return (Double) evaluate().getObject();
        }

        public Integer getInteger() throws Exception {
            return (Integer) evaluate().getObject();
        }

        public ScriptType evaluate(ScriptCompilationContext group, ScriptContext context) throws Exception {
            ScriptExpression expr = null;
            try {
                CompilationResult<fr.nico.sqript.expressions.ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(content.get(0), group);
                if (result != null && result.isSuccess()) {
                    expr = result.getResult();
                } else {
                    throw new fr.nico.sqript.core.compilation.ScriptException(content.get(0), "Erreur de compilation d'expression dans ScriptBlock");
                }
            } catch (Exception e) {
                throw new fr.nico.sqript.core.compilation.ScriptException(content.get(0), "Erreur de compilation d'expression dans ScriptBlock : " + e.getMessage());
            }
            return expr.get(context);
        }

        public String getRawContent() {
            return content.get(0).getText().split("#")[0].trim();
        }

        public ScriptType evaluate() throws Exception {
            return evaluate(new ScriptCompilationContext(), new ScriptContext());
        }


        public IScript compile() throws Exception {
            return compile(new ScriptCompilationContext());
        }

        public IScript compile(ScriptCompilationContext compileGroup) throws Exception {
            //System.out.println("Compiling content : "+content);
            try {
                fr.nico.sqript.structures.SimpleScriptWrapper wrapper = new fr.nico.sqript.structures.SimpleScriptWrapper();
                fr.nico.sqript.structures.IScript firstScript = null;
                fr.nico.sqript.structures.IScript previousScript = null;

                for (fr.nico.sqript.core.compilation.ScriptToken token : content) {
                    CompilationResult<fr.nico.sqript.expressions.ScriptExpression> result = fr.nico.sqript.forge.SqriptForge.getHybridEngine().compileExpression(token, compileGroup);
                    if (result != null && result.isSuccess()) {
                        // Créer une action simple qui exécute l'expression compilée
                        fr.nico.sqript.actions.ActSimpleExpression action = new fr.nico.sqript.actions.ActSimpleExpression(result.getResult());

                        if (firstScript == null) {
                            firstScript = action;
                            previousScript = action;
                        } else {
                            previousScript.setNext(action);
                            previousScript = action;
                        }
                    } else {
                        // Optionnel : logguer un warning
                        fr.nico.sqript.ScriptManager.log.warn("Échec de compilation d'une instruction de bloc : " + token.getText());
                    }
                }

                if (firstScript != null) {
                    wrapper.setWrapped(firstScript);
                    return wrapper;
                } else {
                    return null;
                }
            } catch (Exception e) {
                if (e instanceof ScriptException.ScriptBadSideException)
                    return null;
                else throw e;
            }
        }
    }
}





