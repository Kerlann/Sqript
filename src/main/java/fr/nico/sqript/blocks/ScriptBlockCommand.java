package fr.nico.sqript.blocks;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.core.compilation.ScriptCompilationContext;
import fr.nico.sqript.core.compilation.ScriptException;
import fr.nico.sqript.core.compilation.ScriptToken;
import fr.nico.sqript.core.registry.ScriptRegistry;
import fr.nico.sqript.forge.SqriptForge;
import fr.nico.sqript.meta.Block;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.*;
import fr.nico.sqript.types.TypeConsole;
import fr.nico.sqript.types.TypePlayer;
import fr.nico.sqript.types.primitive.TypeNumber;
import fr.nico.sqript.types.primitive.TypeString;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Block(
        feature = @Feature(name = "Command",
                description = "Define a new command that can be executed and have some action.",
                examples = "command /randomplayer:\n" +
                        "    usage: /randomplayer\n" +
                        "    description: returns a random player\n" +
                        "    send a random element of all players to sender",
                regex = "^command /.*"),
        fields = {
                @Feature(name = "side"),
                @Feature(name = "description"),
                @Feature(name = "usage"),
                @Feature(name = "aliases"),
                @Feature(name = "permission")
        }
)
public class ScriptBlockCommand extends ScriptBlock implements ICommand {

    private final String name;
    ScriptParameterDefinition[][] argumentsDefinitions;
    private fr.nico.sqript.structures.Side side = fr.nico.sqript.structures.Side.SERVER;
    private String[] aliases = new String[0];
    private String description;
    private String usage;
    private String permission;
    public ScriptBlockCommand(ScriptToken head) throws ScriptException.ScriptSyntaxException {
        super(head);

        try {
            ScriptManager.log.info("DEBUG ScriptBlockCommand: Starting constructor with head: '" + head.getText() + "'");

            //System.out.println("Loading block command:"+head);
            String rawText = head.getText();
            ScriptManager.log.info("DEBUG ScriptBlockCommand: Raw text: '" + rawText + "'");

            String afterCommandReplace = rawText.replaceFirst("command\\s+/", "");
            ScriptManager.log.info("DEBUG ScriptBlockCommand: After command replace: '" + afterCommandReplace + "'");

            String afterColonReplace = afterCommandReplace.replaceFirst(":$", "");
            ScriptManager.log.info("DEBUG ScriptBlockCommand: After colon replace: '" + afterColonReplace + "'");

            String[] splitResult = fr.nico.sqript.core.compilation.MigrationAdapter.splitAtDoubleDot(afterColonReplace);
            ScriptManager.log.info("DEBUG ScriptBlockCommand: Split result length: " + splitResult.length);
            if (splitResult.length > 0) {
                ScriptManager.log.info("DEBUG ScriptBlockCommand: Split result[0]: '" + splitResult[0] + "'");
            }

            final String def = splitResult[0];
            ScriptManager.log.info("DEBUG ScriptBlockCommand: Final def: '" + def + "'");

            Matcher m = Pattern.compile("<(.*?)>").matcher(def);
            final List<ScriptParameterDefinition[]> parameterDefinitions = new ArrayList<>();
            while (m.find()) {
                try {
                    //System.out.println("Adding argument : "+m.group(1));
                    if (m.group(1) != null) {
                        Collections.addAll(parameterDefinitions,
                                Arrays.stream(m.group(1).split("\\|"))
                                        .map(type -> ScriptRegistry.parseType(type))
                                        .map(type -> new ScriptParameterDefinition((Class<? extends ScriptElement<?>>) type))
                                        .toArray(ScriptParameterDefinition[]::new));
                    }
                    //System.out.println("Now : "+parameterDefinitions);
                } catch (Exception e) {
                    ScriptManager.log.error("DEBUG ScriptBlockCommand: Error in parameter parsing: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            this.name = def.split("<(.*?)>")[0].trim();
            ScriptManager.log.info("DEBUG ScriptBlockCommand: Command name: '" + this.name + "'");

            this.argumentsDefinitions = parameterDefinitions.toArray(new ScriptParameterDefinition[0][0]);
            ScriptManager.log.info("DEBUG ScriptBlockCommand: Constructor completed successfully");
        } catch (Exception e) {
            ScriptManager.log.error("DEBUG ScriptBlockCommand: Exception in constructor: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static List<String> getListOfStringsMatchingLastWord(String[] inputArgs, Collection<?> possibleCompletions) {
        if (inputArgs == null || inputArgs.length == 0) {
            return new ArrayList<>();
        }
        String s = inputArgs[inputArgs.length - 1];
        List<String> list = Lists.newArrayList();

        if (!possibleCompletions.isEmpty()) {
            for (String s1 : Iterables.transform(possibleCompletions, Functions.toStringFunction())) {
                if (doesStringStartWith(s, s1)) {
                    list.add(s1);
                }
            }

            if (list.isEmpty()) {
                for (Object object : possibleCompletions) {
                    if (object instanceof ResourceLocation && doesStringStartWith(s, ((ResourceLocation) object).getNamespace())) {
                        list.add(String.valueOf(object));
                    }
                }
            }
        }

        return list;
    }

    public static boolean doesStringStartWith(String original, String region) {
        return region.regionMatches(true, 0, original, 0, original.length());
    }

    @Override
    protected void load() throws Exception {
        try {
            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Starting load for command: " + this.name);

            if (fieldDefined("side")) {
                ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Setting side");
                this.setSide(fr.nico.sqript.structures.Side.from(getSubBlock("side").getRawContent()));
            }

            ScriptCompilationContext compileGroup = new ScriptCompilationContext();
            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Created compilation context");

            if (getMainField() != null) {
                // Log du contenu avant compilation
                ScriptManager.log.info("DEBUG: Lignes compilées pour la commande '" + this.name + "' :");
                for (ScriptToken token : getMainField().getContent()) {
                    ScriptManager.log.info("  > " + token.getText());
                }
                ScriptManager.log.debug("MainField content before compilation: {} tokens", getMainField().getContent().size());
                for (ScriptToken token : getMainField().getContent()) {
                    ScriptManager.log.debug("  - '{}'", token.getText());
                }
            }
            // Ajout des arguments dynamiques
            for (int j = 0; j < argumentsDefinitions.length; j++) {
                String argName = "arg[ument] " + (j + 1);
                compileGroup.add(argName, ScriptElement.class);
                ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Ajout de l'argument au contexte: " + argName);
            }
            // Ajout du sender
            compileGroup.add("sender", ScriptElement.class);
            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Ajout de 'sender' au contexte");
            compileGroup.add("player", ScriptElement.class);
            compileGroup.add("console", ScriptElement.class);
            compileGroup.add("server", ScriptElement.class);
            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Ajout de 'player', 'console', 'server' au contexte");

            // Log du contenu du contexte de compilation
            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Variables dans le contexte de compilation: " + compileGroup);

            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): About to compile main field");
            if (getMainField() == null) {
                ScriptManager.log.error("DEBUG ScriptBlockCommand.load(): MainField is null!");
                throw new RuntimeException("MainField is null");
            }

            // Log chaque ligne compilée
            for (fr.nico.sqript.core.compilation.ScriptToken token : getMainField().getContent()) {
                ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Tentative de compilation de la ligne: '" + token.getText() + "' avec contexte: " + compileGroup);
            }

            this.setRoot(getMainField().compile(compileGroup));
            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Main field compiled successfully, root: " + (getRoot() != null ? getRoot().getClass().getSimpleName() : "null"));

            if (fieldDefined("description")) {
                ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Setting description");
                this.setDescription(getSubBlock("description").getRawContent());
            }

            if (fieldDefined("usage")) {
                ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Setting usage");
                this.setUsage(getSubBlock("usage").getRawContent());
            }

            if (fieldDefined("aliases")) {
                ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Setting aliases");
                this.setAliases(getSubBlock("aliases").getContent().stream().map(ScriptToken::getText).toArray(String[]::new));
            }

            if (fieldDefined("permission")) {
                ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Setting permission");
                this.setPermission(getSubBlock("permission").getRawContent());
            }
            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Adding command to SqriptForge");
            SqriptForge.addCommand(this);

            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Registering block");
            getScriptInstance().registerBlock(this);

            ScriptManager.log.info("DEBUG ScriptBlockCommand.load(): Load completed successfully");
        } catch (Exception e) {
            ScriptManager.log.error("DEBUG ScriptBlockCommand.load(): Exception in load: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public fr.nico.sqript.structures.Side getSide() {
        return side;
    }

    public void setSide(fr.nico.sqript.structures.Side side) {
        this.side = side;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return (usage == null ? "/" + name : usage);
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList(aliases);
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    @SideOnly(Side.CLIENT)
    public void executeOnServer(String[] strings) {
        String args = "";
        for (String s : strings) args += s + " ";
        Minecraft.getMinecraft().getConnection().getNetworkManager().sendPacket(new CPacketChatMessage("/" + getName() + " " + args));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings) throws CommandException {
        //System.out.println("executing");

        if ((side != fr.nico.sqript.structures.Side.CLIENT && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)) {
            executeOnServer(strings);
            if (side == fr.nico.sqript.structures.Side.SERVER)
                return;
        }

        ScriptContext c = new ScriptContext(ScriptManager.GLOBAL_CONTEXT);

        //Adding arguments to the context
        //Arguments can only be numbers, strings or player.
        for (int i = 0; i < argumentsDefinitions.length && i < strings.length; i++) {
            //Todo : Dynamic command parameters type check
            Class p = argumentsDefinitions[i][0].getTypeClass();
            if (i == argumentsDefinitions.length - 1 && p == TypeString.class) {
                String r = "";
                for (int j = i; j < strings.length; j++) {
                    r += strings[j] + " ";
                }
                r = r.substring(0, r.length() - 1);
                c.put(new ScriptTypeAccessor(new TypeString(r), "arg[ument] " + (i + 1)));
            } else {
                if (p == TypeString.class) {
                    c.put(new ScriptTypeAccessor(new TypeString(strings[i]), "arg[ument] " + (i + 1)));
                } else if (p == TypeNumber.class) {
                    c.put(new ScriptTypeAccessor(new TypeNumber(strings[i]), "arg[ument] " + (i + 1)));
                } else if (p == TypePlayer.class) {
                    c.put(new ScriptTypeAccessor(new TypePlayer(FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(strings[i])), "arg[ument] " + (i + 1)));
                }
            }
        }
        //System.out.println("ICOMMANDSENDER NULL : "+(iCommandSender==null));
        if (iCommandSender instanceof EntityPlayer) {
            c.put(new ScriptTypeAccessor(new TypePlayer((EntityPlayer) iCommandSender), "(sender|player)", "(sender|player|console|server)".hashCode()));
        } else if (iCommandSender instanceof MinecraftServer) {
            c.put(new ScriptTypeAccessor(new TypeConsole((MinecraftServer) iCommandSender), "(sender|console|server)", "(sender|player|console|server)".hashCode()));
        }

        //Running the associated script
        ScriptClock k = new ScriptClock(c);
        try {
            //System.out.println("Running the command");
            k.start(this.getRoot());
        } catch (Exception e) {
            iCommandSender.sendMessage(new TextComponentString("\247cAn error occured while executing Sqript command : "));
            if (e instanceof ScriptException.ScriptWrappedException) {
                Exception wrapped = ((ScriptException.ScriptWrappedException) (e)).getWrapped();
                iCommandSender.sendMessage(new TextComponentString(((ScriptException.ScriptWrappedException) (e)).getLine() + " : " + wrapped).setStyle(new Style().setColor(TextFormatting.RED)));
                wrapped.printStackTrace();
            } else {
                iCommandSender.sendMessage(new TextComponentString("\247c" + e.getMessage()).setStyle(new Style().setColor(TextFormatting.RED)));
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer minecraftServer, ICommandSender iCommandSender) {
        if (getPermission() == null) return true;
        return MinecraftForge.EVENT_BUS.post(new checkPermission(minecraftServer, iCommandSender, getPermission()));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos blockPos) {
        int nArg = strings.length - 1;
        if (nArg >= 0 && nArg < argumentsDefinitions.length) {
            if (Arrays.stream(argumentsDefinitions[nArg]).anyMatch(s -> s.getTypeClass() == TypePlayer.class)) {
                return getListOfStringsMatchingLastWord(strings,
                        Arrays.asList(FMLCommonHandler.instance().getMinecraftServerInstance().getOnlinePlayerNames()));
            }
        }
        return new ArrayList<>();
    }

    @Override
    public boolean isUsernameIndex(String[] strings, int i) {
        return false;
    }

    public ScriptParameterDefinition[][] getArgumentsDefinitions() {
        return argumentsDefinitions;
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }

    @Cancelable
    public static class checkPermission extends Event {

        private final MinecraftServer minecraftServer;
        private final ICommandSender commandSender;
        private final String permission;

        public checkPermission(MinecraftServer minecraftServer, ICommandSender commandSender, String permission) {
            this.minecraftServer = minecraftServer;
            this.commandSender = commandSender;
            this.permission = permission;
        }

        public MinecraftServer getMinecraftServer() {
            return minecraftServer;
        }

        public ICommandSender getCommandSender() {
            return commandSender;
        }

        public String getPermission() {
            return permission;
        }
    }

}




