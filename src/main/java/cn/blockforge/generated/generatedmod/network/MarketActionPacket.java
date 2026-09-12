package cn.blockforge.generated.generatedmod.network;

import net.minecraft.network.FriendlyByteBuf;

public final class MarketActionPacket {
    public static final byte REQUEST_SYNC = 0;
    public static final byte BUY_STOCK = 1;
    public static final byte SELL_STOCK = 2;
    public static final byte BUY_FUTURE = 3;
    public static final byte SELL_FUTURE = 4;
    public static final byte BUY_SPOT = 5;
    public static final byte SELL_SPOT = 6;
    public static final byte OPEN_SPOT = 7;
    public static final byte REQUEST_QUOTE = 8;
    public static final byte OPEN_HUB = 9;
    public static final byte REQUEST_FUTURES_QUOTE = 10;
    public static final byte OPEN_PHONE_HUB = 11;
    public static final byte OPEN_TOWER_STORAGE = 12;
    public static final byte OPEN_MONITOR = 13;

    public final byte action;
    public final String asset;
    public final long amount;

    public MarketActionPacket(byte action, String asset, long amount) {
        this.action = action;
        this.asset = asset == null ? "" : asset;
        this.amount = amount;
    }

    public static void encode(MarketActionPacket packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.action);
        buf.writeUtf(packet.asset);
        buf.writeLong(packet.amount);
    }

    public static MarketActionPacket decode(FriendlyByteBuf buf) {
        return new MarketActionPacket(buf.readByte(), buf.readUtf(), buf.readLong());
    }
}
