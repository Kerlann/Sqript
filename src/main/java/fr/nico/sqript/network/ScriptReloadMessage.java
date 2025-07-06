package fr.nico.sqript.network;

import fr.nico.sqript.ScriptManager;
import fr.nico.sqript.SqriptUtils;
import fr.nico.sqript.core.compilation.ScriptException;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class ScriptReloadMessage implements IMessage {

    public ScriptReloadMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class ScriptMessageHandler implements IMessageHandler<ScriptReloadMessage, ScriptReloadMessage> {

        @SideOnly(Side.CLIENT)
        @Override
        public ScriptReloadMessage onMessage(ScriptReloadMessage message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                long t = System.currentTimeMillis();
                EntityPlayer player = Minecraft.getMinecraft().player;
                SqriptUtils.sendMessage("Reloading all scripts on client side.", player);
                try {
                    ScriptManager.reload();
                } catch (Throwable e) {
                    if (e instanceof ScriptException.ScriptExceptionList) {
                        SqriptUtils.sendError("\247cError while reloading the scripts on client side: ", player);
                        for (Throwable ex : ((ScriptException.ScriptExceptionList) e).exceptionList) {
                            SqriptUtils.sendError("\247c" + ex.getLocalizedMessage() + " (\2478" + ex.getStackTrace()[0] + "\247c)", player);
                            ex.printStackTrace();
                        }
                    } else {
                        SqriptUtils.sendError("\247cError while reloading scripts on client side, see stacktrace for more information.", player);
                        SqriptUtils.sendError("\247c" + e.getLocalizedMessage(), player);
                        e.printStackTrace();
                    }
                }
                SqriptUtils.sendMessage("Done in " + ((System.currentTimeMillis() - t) / 1000d) + " seconds", player);
            }
            return null;
        }

    }
}




