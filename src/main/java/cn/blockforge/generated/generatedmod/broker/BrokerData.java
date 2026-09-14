package cn.blockforge.generated.generatedmod.broker;

import cn.blockforge.generated.generatedmod.api.economy.Money;
import cn.blockforge.generated.generatedmod.data.MarketData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;

import java.util.HashMap;
import java.util.Map;

/**
 * 可复用的做市商村民状态：<b>每个村民各自独立</b>的钱包、库存与盈亏记录。
 *
 * <p>全部数据保存在该村民实体的持久化 NBT 中（{@code generated_mod:broker}），
 * 因此<b>旧存档无需任何迁移</b>；村民死亡时数据随实体消失。</p>
 *
 * <p>金额单位沿用 {@link Money} 的「分」（long），保证与钱包、手续费一致的精度。</p>
 */
public final class BrokerData {

    /** NBT 中的根键名。 */
    public static final String TAG = "generated_mod:broker";
    /** 做市商初始钱包：$10,000（= 1,000,000 分）。 */
    public static final long INITIAL_WALLET = 10_000L * Money.CENTS_PER_UNIT;

    /** 钱包余额（分）。 */
    public long wallet;
    /** 当前库存（物品 ID → 数量；抽象数量，不是实体物品）。 */
    public final Map<String, Integer> stock = new HashMap<>();
    /** 累计成本（分，用于计算已实现盈亏）。 */
    public long cost;
    /** 当日盈亏（分）。 */
    public long pnlDay;
    /** 累计盈亏（分）。 */
    public long pnlTotal;
    /** 上次结算日。 */
    public long lastDay = -1L;
    /** 村民原始显示名（刷新钱包名字牌时使用，避免重复拼接）。 */
    public String baseName = "";
    /** 上次结算时的净资产（分，用于计算当日盈亏）。 */
    public long lastNetWorth = -1L;

    private BrokerData() {
    }

    /** 读取（或首次初始化）该村民的做市商数据。 */
    public static BrokerData of(Villager villager) {
        CompoundTag root = villager.getPersistentData();
        if (!root.contains(TAG, Tag.TAG_COMPOUND)) {
            BrokerData fresh = new BrokerData();
            fresh.wallet = INITIAL_WALLET;
            fresh.save(villager);
            return fresh;
        }
        BrokerData data = new BrokerData();
        CompoundTag tag = root.getCompound(TAG);
        data.wallet = tag.getLong("Wallet");
        data.cost = tag.getLong("Cost");
        data.pnlDay = tag.getLong("PnlDay");
        data.pnlTotal = tag.getLong("PnlTotal");
        data.lastDay = tag.contains("LastDay") ? tag.getLong("LastDay") : -1L;
        data.lastNetWorth = tag.contains("LastNetWorth") ? tag.getLong("LastNetWorth") : -1L;
        data.baseName = tag.getString("BaseName");
        CompoundTag stockTag = tag.getCompound("Stock");
        for (String key : stockTag.getAllKeys()) {
            int count = stockTag.getInt(key);
            if (count > 0) {
                data.stock.put(key, count);
            }
        }
        return data;
    }

    /** 是否已经初始化过数据。 */
    public static boolean isBroker(Villager villager) {
        return villager.getPersistentData().contains(TAG, Tag.TAG_COMPOUND);
    }

    /** 写回村民 NBT。 */
    public void save(Villager villager) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Wallet", this.wallet);
        tag.putLong("Cost", this.cost);
        tag.putLong("PnlDay", this.pnlDay);
        tag.putLong("PnlTotal", this.pnlTotal);
        tag.putLong("LastDay", this.lastDay);
        tag.putLong("LastNetWorth", this.lastNetWorth);
        tag.putString("BaseName", this.baseName == null ? "" : this.baseName);
        CompoundTag stockTag = new CompoundTag();
        this.stock.forEach((id, count) -> {
            if (count > 0) {
                stockTag.putInt(id, count);
            }
        });
        tag.put("Stock", stockTag);
        villager.getPersistentData().put(TAG, tag);
    }

    /** 库存数量。 */
    public int stockOf(String itemId) {
        return this.stock.getOrDefault(itemId, 0);
    }

    /** 增加/减少库存（不会低于 0）。 */
    public void addStock(String itemId, int delta) {
        int next = Math.max(0, stockOf(itemId) + delta);
        if (next == 0) {
            this.stock.remove(itemId);
        } else {
            this.stock.put(itemId, next);
        }
    }

    /** 库存总市值（分）：按当日行情中间价折算。 */
    public long stockValue(ServerLevel level) {
        long value = 0L;
        for (Map.Entry<String, Integer> entry : this.stock.entrySet()) {
            double price = MarketData.get(level).price(entry.getKey(), false);
            value += Money.fromDollars(price * entry.getValue());
        }
        return value;
    }

    /** 净资产（分）= 钱包 + 库存市值。 */
    public long netWorth(ServerLevel level) {
        return this.wallet + stockValue(level);
    }

    /**
     * 每日结算：记录当日盈亏、累计盈亏，并刷新显示名字（钱包余额显示在名字下方）。
     *
     * @return 当日盈亏（分）
     */
    public long settle(Villager villager, ServerLevel level, long day) {
        long netWorth = netWorth(level);
        if (this.lastNetWorth >= 0L) {
            this.pnlDay = netWorth - this.lastNetWorth;
            this.pnlTotal += this.pnlDay;
        } else {
            this.pnlDay = 0L;
        }
        this.lastNetWorth = netWorth;
        this.lastDay = day;
        this.save(villager);
        BrokerName.refresh(villager, this);
        return this.pnlDay;
    }

    /** 名字牌文本：村民原名 + 换行 + 钱包余额（原版名字渲染支持换行，显示为两行）。 */
    public Component displayName() {
        String base = this.baseName == null || this.baseName.isEmpty() ? "券商" : this.baseName;
        return Component.literal(base + "\n" + Money.format(this.wallet));
    }
}
