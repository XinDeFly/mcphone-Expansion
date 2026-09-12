package cn.blockforge.generated.generatedmod.network;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.MarketServerActions;
import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class Network {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GeneratedMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private Network() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, MarketActionPacket.class,
                MarketActionPacket::encode, MarketActionPacket::decode, Network::handleAction);
        CHANNEL.registerMessage(1, MarketSyncPacket.class,
                MarketSyncPacket::encode, MarketSyncPacket::decode, Network::handleSync);
        CHANNEL.registerMessage(2, TowerSyncPacket.class,
                TowerSyncPacket::encode, TowerSyncPacket::decode, Network::handleTowerSync);
        CHANNEL.registerMessage(3, TowerActionPacket.class,
                TowerActionPacket::encode, TowerActionPacket::decode, Network::handleTowerAction);
    }

    private static void handleTowerSync(TowerSyncPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> TowerSyncPacket.handle(packet, ctxSupplier.get()));
        ctxSupplier.get().setPacketHandled(true);
    }

    private static void handleTowerAction(TowerActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                cn.blockforge.generated.generatedmod.AutoTradeManager.handleAction(player, packet);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleAction(MarketActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                MarketServerActions.handle(player, packet);
            }
        });
        ctx.setPacketHandled(true);
    }

    private static void handleSync(MarketSyncPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.containerMenu instanceof MarketMenu menu) {
                menu.setSnapshot(packet.snapshot);
                AssetQuote quote = packet.snapshot.toQuote();
                if (quote != null) {
                    menu.putQuote(packet.snapshot.asset(), packet.snapshot.futures(), quote);
                }
            }
        });
        ctx.setPacketHandled(true);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(MarketActionPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }
}
