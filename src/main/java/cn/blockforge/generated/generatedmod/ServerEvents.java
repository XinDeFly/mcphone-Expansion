package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.data.MarketData;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerEvents {
    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            if (level.getGameTime() % 20L == 0L) {
                AutoTradeManager.tick(level);
            }
            MarketData data = MarketData.get(level);
            if (data.onTick(level)) {
                Map<UUID, List<String>> messages = data.consumeDailyMessages();
                for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                    List<String> ownMessages = messages.get(player.getUUID());
                    if (ownMessages != null) {
                        for (String message : ownMessages) {
                            player.sendSystemMessage(Component.literal(message));
                        }
                    }
                    if (player.containerMenu instanceof MarketMenu) {
                        MarketServerActions.sendSnapshot(player, data, level, "市场行情已更新");
                    }
                }
            }
        }
    }
}
