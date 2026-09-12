package cn.blockforge.generated.mod3ce985ee;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 村民交易注册：图书管理员（1 级）以 3 个金锭兑换 1 条内存条（机箱存储元件）。
 */
@Mod.EventBusSubscriber(modid = "generated_mod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillagerTradeEvents {
    private VillagerTradeEvents() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.LIBRARIAN) {
            return;
        }
        List<VillagerTrades.ItemListing> levelOne = event.getTrades().get(1);
        if (levelOne != null) {
            levelOne.add(MemoryStickTrade.INSTANCE);
        }
    }

    /** 图书管理员：3 金锭 → 1 内存条（每次 12 次交易、5 经验，供 64 交易品质加成最优）。 */
    public enum MemoryStickTrade implements VillagerTrades.ItemListing {
        INSTANCE;

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return new MerchantOffer(
                    new ItemStack(Items.GOLD_INGOT, 3),
                    new ItemStack(GeneratedMod.MEMORY_STICK.get()),
                    12, 5, 0.05F);
        }
    }
}
