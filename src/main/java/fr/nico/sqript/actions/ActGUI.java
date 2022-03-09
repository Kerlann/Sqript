package fr.nico.sqript.actions;

import fr.nico.sqript.compiling.ScriptException;
import fr.nico.sqript.meta.Action;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.ScriptContext;
import fr.nico.sqript.structures.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;
import java.net.URI;
import java.net.URL;

@Action(name = "GUI Actions",
        features = {@Feature(name = "Close GUI", description = "Close the current GUI.", examples = "close current gui", pattern = "close [the] [current] (GUI|gui)", side = Side.CLIENT),
                    @Feature(name = "Open settings GUI", description = "Opens the settings GUI.", examples = "open settings", pattern = "open [the] settings [(GUI|gui)]", side = Side.CLIENT),
                    @Feature(name = "Open single-player GUI", description = "Opens the world selection GUI.", examples = "open single-player gui", pattern = "open [the] (single-player|world selection) [(GUI|gui)]", side = Side.CLIENT),
                    @Feature(name = "Open multi-player GUI", description = "Opens the server selection GUI.", examples = "open multi-player gui", pattern = "open [the] (multi-player|server selection) [(GUI|gui)]", side = Side.CLIENT),
                    @Feature(name = "Open link", description = "Open the web links.", examples = "open web link \"www.google.com\"", pattern = "open web link {string}", side = Side.CLIENT),
                    @Feature(name = "Connect server", description = "connect a player to a server.", examples = "connect to \"mc.hypixel.net\"", pattern = "connect to {string} [with port {number}]", side = Side.CLIENT)
        }
)
public class ActGUI extends ScriptAction {

    @SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    @Override
    public void execute(ScriptContext context) throws ScriptException {
        switch (getMatchedIndex()){
            case 0:
                Minecraft.getMinecraft().displayGuiScreen(null);
                break;
            case 1:
                Minecraft.getMinecraft().displayGuiScreen(new GuiOptions(Minecraft.getMinecraft().currentScreen, Minecraft.getMinecraft().gameSettings));
                break;
            case 2:
                Minecraft.getMinecraft().displayGuiScreen(new GuiWorldSelection(Minecraft.getMinecraft().currentScreen));
                break;
            case 3:
                Minecraft.getMinecraft().displayGuiScreen(new GuiMultiplayer(Minecraft.getMinecraft().currentScreen));
                break;
            case 4:
                openWebLink((String) getParameter(0,context));
                break;
            case 5:
                String ip = (String) getParameter(1,context);
                int port = getParameterOrDefault(getParameter(2), 25565, context);
                Minecraft.getMinecraft().displayGuiScreen(new GuiConnecting(Minecraft.getMinecraft().currentScreen, Minecraft.getMinecraft(), ip, port));
                break;
        }
    }

    public void openWebLink(String url) {
        try {
            URI uri = new URL(url).toURI();

            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
