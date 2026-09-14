package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.command.RarityExportCommand;
import cn.blockforge.generated.generatedmod.data.MarketData;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
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

    /**
     * 服务器启动后：若已安装 Rarity Core，主动向其批量请求全部物品的稀有度并写入缓存
     * （未安装则不做任何事，自动使用内置快照 + 本地 4 级分类）。
     */
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        int prefetched = cn.blockforge.generated.generatedmod.api.rarity.RaritySources.prefetch();
        if (prefetched > 0) {
            org.slf4j.LoggerFactory.getLogger("MCphoneMarketExpansion")
                    .info("已从 Rarity Core 预取 {} 条物品稀有度", prefetched);
        }
    }

    /** 注册命令：/mcme rarity info | export（导出稀有度快照）。 */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        RarityExportCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            if (level.getGameTime() % 20L == 0L) {
                AutoTradeManager.tick(level);
            }
            MarketData data = MarketData.get(level);
            if (data.onTick(level)) {
                // 跨天：先给做市商村民做每日结算（记录当日盈亏并刷新名字牌上的钱包余额）
                cn.blockforge.generated.generatedmod.broker.BrokerSpawn.settleAll(level, level.getDayTime() / 24000L);
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
