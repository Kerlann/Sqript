package fr.nico.sqript.actions;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.SqriptUtils;
import fr.nico.sqript.blocks.ScriptBlock;
import fr.nico.sqript.compiling.ScriptCompilationContext;
import fr.nico.sqript.compiling.ScriptDecoder;
import fr.nico.sqript.compiling.ScriptException;
import fr.nico.sqript.compiling.ScriptToken;
import fr.nico.sqript.meta.Action;
import fr.nico.sqript.meta.Feature;
import fr.nico.sqript.structures.*;
import fr.nico.sqript.types.TypePlayer;
import fr.nico.sqript.types.primitive.TypeString;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;

@Action(name = "Container Actions",
        features = {
                @Feature(name = "Open Container", description = "Opens the custom container", examples = "open chest with 1 row named \"Level up! Choose an attribute to upgrade!\" to player", pattern = "open chest with {number} row named {string} to {player}"),
                @Feature(name = "format slot", description = "set the custom container", examples = "format slot 0 of player with minecraft:stone named \"&6Test\"", pattern = "format slot {number} of {player} with [{+number}] {item} [named {string}] to run {string}"),
        }
)
public class ActContainer extends ScriptAction {

    @Override
    public void execute(ScriptContext context) throws ScriptException {
        switch (getMatchedIndex()) {
            case 0:
                double row = (double) getParameter(1,context);
                String name = (String) getParameter(2,context);
                EntityPlayer player = (EntityPlayer) getParameter(3,context);
                player.displayGUIChest(new InventoryBasic(name, false, (int) row * 9));
                break;
            case 1:
                row = (double) getParameter(1, context);
                player = (EntityPlayer) getParameter(2, context);
                int amount = getParameterOrDefault(getParameter(3),1d, context).intValue();
                ItemStack item = SqriptUtils.getItemWithParameter(getParameter(4).get(context), amount);
                String itemName = getParameterOrDefault(getParameter(5),"", context);
                String text = (String) getParameter(6, context);
                item.setStackDisplayName(itemName);
                player.openContainer.putStackInSlot((int) row, item);
                ScriptInstance from = ScriptManager.getScriptFromName("sqript");
                ScriptToken token =  new ScriptToken(text, 1, from);
                IScript script = null;
                try {
                    script = ScriptDecoder.parseLine(token, new ScriptCompilationContext());
                    script.setLine(token);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ScriptContext c = new ScriptContext(ScriptManager.GLOBAL_CONTEXT);
                c.put(new ScriptTypeAccessor(new TypePlayer((EntityPlayer) player), "(sender|player)","(sender|player|console|server)".hashCode()));
                ScriptClock k = new ScriptClock(c);
                k.current = script;
                try {
                    //System.out.println("Running the command");
                    k.start(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}