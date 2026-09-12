package cn.blockforge.generated.generatedmod.network;

import cn.blockforge.generated.generatedmod.data.MarketData;
import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import net.minecraft.network.FriendlyByteBuf;

public final class MarketSyncPacket {
    public final MarketSnapshot snapshot;

    public MarketSyncPacket(MarketSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(MarketSyncPacket packet, FriendlyByteBuf buf) {
        MarketSnapshot s = packet.snapshot;
        buf.writeLong(s.balance());
        buf.writeInt(s.dollarItems());
        buf.writeLong(s.day());
        buf.writeLong(s.dayTime());
        buf.writeUtf(s.status());
        buf.writeUtf(s.asset());
        buf.writeBoolean(s.futures());
        buf.writeBoolean(s.hasQuote());
        if (s.hasQuote()) {
            buf.writeDouble(s.price());
            buf.writeDouble(s.change());
            double[] history = s.history();
            buf.writeVarInt(history.length);
            for (double value : history) {
                buf.writeDouble(value);
            }
            buf.writeInt(s.stockTotal());
            buf.writeInt(s.stockLocked());
            buf.writeInt(s.futureQty());
            buf.writeDouble(s.futureEntry());
            buf.writeDouble(s.futureMargin());
            buf.writeLong(s.futureExpiryDay());
        }
    }

    public static MarketSyncPacket decode(FriendlyByteBuf buf) {
        long balance = buf.readLong();
        int dollarItems = buf.readInt();
        long day = buf.readLong();
        long dayTime = buf.readLong();
        String status = buf.readUtf();
        String asset = buf.readUtf();
        boolean futures = buf.readBoolean();
        boolean hasQuote = buf.readBoolean();
        if (!hasQuote) {
            return new MarketSyncPacket(new MarketSnapshot(balance, dollarItems, day, dayTime, status));
        }
        double price = buf.readDouble();
        double change = buf.readDouble();
        int days = Math.min(buf.readVarInt(), MarketData.HISTORY_DAYS);
        double[] history = new double[days];
        for (int i = 0; i < days; i++) {
            history[i] = buf.readDouble();
        }
        int stockTotal = buf.readInt();
        int stockLocked = buf.readInt();
        int futureQty = buf.readInt();
        double futureEntry = buf.readDouble();
        double futureMargin = buf.readDouble();
        long futureExpiryDay = buf.readLong();
        return new MarketSyncPacket(new MarketSnapshot(balance, dollarItems, day, dayTime, status,
                asset, futures, price, change, history, stockTotal, stockLocked,
                futureQty, futureEntry, futureMargin, futureExpiryDay));
    }
}
