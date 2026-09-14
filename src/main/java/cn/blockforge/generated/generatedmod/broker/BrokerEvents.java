package cn.blockforge.generated.generatedmod.broker;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.MarketServerActions;
import cn.blockforge.generated.generatedmod.api.economy.Money;
import cn.blockforge.generated.generatedmod.api.rarity.RaritySources;
import cn.blockforge.generated.generatedmod.api.rarity.RarityTier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 做市商村民的运行时事件入口。
 *
 * <p>当前阶段（①）：生成判定 + 右键查看报价概览（专用交易界面在阶段②实现）。</p>
 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BrokerEvents {

    private BrokerEvents() {
    }

    /** 村民加入世界 → 按规则（小概率、村庄规模、每村上限）转化为做市商。 */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        BrokerSpawn.onEntityJoin(event);
    }

    /**
     * 右键做市商村民：屏蔽原版交易界面，打开专用做市商交易界面（双列 bid/ask 报价）。
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || !BrokerSpawn.isBroker(villager)) {
            return;
        }
        event.setCanceled(true);
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MarketServerActions.openBroker(player, villager);
    }
}
