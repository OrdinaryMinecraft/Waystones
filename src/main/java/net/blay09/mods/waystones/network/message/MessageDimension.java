package net.blay09.mods.waystones.network.message;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MessageDimension implements IMessage {

    private int dim;

    public MessageDimension() {
    }

    public MessageDimension(int dim) {
        this.dim = dim;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dim = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dim);
    }

    public int getDim() {
        return dim;
    }
}

