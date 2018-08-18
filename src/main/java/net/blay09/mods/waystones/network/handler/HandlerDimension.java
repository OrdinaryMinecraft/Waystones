package net.blay09.mods.waystones.network.handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.network.message.MessageDimension;
import net.minecraftforge.common.DimensionManager;

public class HandlerDimension implements IMessageHandler<MessageDimension, IMessage> {
    @Override
    public IMessage onMessage(final MessageDimension message, MessageContext ctx) {
        Waystones.proxy.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                System.out.println("WAYSTONES DEBUG: Cheching provider for dim " + message.getDim());
                if (!DimensionManager.isDimensionRegistered(message.getDim())) {
                    System.out.println("WAYSTONES DEBUG: Provider not exist, creating");
                    DimensionManager.registerDimension(message.getDim(), 0);
                } else {
                    System.out.println("WAYSTONES DEBUG: Provider exist");
                }
            }
        });
        return null;
    }
}