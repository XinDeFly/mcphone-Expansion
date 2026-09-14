package cn.blockforge.generated.generatedmod.data;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class MarketData extends SavedData {
    public static final int HISTORY_DAYS = 15;
    public static final int FUTURES_EXPIRY_DAYS = 3;
    public static final double FUTURES_MARGIN_RATE = 0.10;

    private final Map<UUID, Long> wallets = new HashMap<>();
    private final Map<UUID, Map<String, StockHolding>> stocks = new HashMap<>();
    private final Map<UUID, Map<String, FuturesPosition>> futures = new HashMap<>();
    private final Map<UUID, NonNullList<ItemStack>> storages = new HashMap<>();
    private final Series stockSeries = new Series();
    private final Series futuresSeries = new Series();
    private final Map<UUID, List<String>> dailyMessages = new HashMap<>();
    private long lastDay = -1L;
    private long lastSeenTick = Long.MIN_VALUE;

    private static final class Series {
        final Map<String, Double> prices = new HashMap<>();
        final Map<String, Double> changes = new HashMap<>();
        final Map<String, double[]> history = new HashMap<>();
        final Map<String, Long> lastDays = new HashMap<>();
        final Map<String, Integer> trendStreak = new HashMap<>();
        final Map<String, Integer> limitDirection = new HashMap<>();
        final Map<String, Integer> limitDays = new HashMap<>();
        final Map<String, Boolean> rejudgeLimit = new HashMap<>();
        final Map<String, Integer> limitCount = new HashMap<>();
    }

    private MarketData() {
    }

    public static MarketData load(CompoundTag tag) {
        MarketData data = new MarketData();
        data.lastDay = tag.getLong("LastDay");

        // 货币单位迁移：单位版本 2 起金额以「分」存储；旧存档（整元）一次性 ×100。
        boolean centsUnit = tag.getInt("MoneyUnit") >= 2;
        CompoundTag walletTag = tag.getCompound("Wallets");
        for (String key : walletTag.getAllKeys()) {
            try {
                long raw = walletTag.getLong(key);
                data.wallets.put(UUID.fromString(key), centsUnit ? raw : raw * 100L);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries.
            }
        }
        if (!centsUnit && !walletTag.getAllKeys().isEmpty()) {
            data.setDirty();
        }

        boolean hasStockSection = tag.contains("StockPrices", Tag.TAG_COMPOUND);
        loadSeries(data.stockSeries, tag, hasStockSection ? "Stock" : "");
        loadSeries(data.futuresSeries, tag, "Futures");
        // 持仓载入（修复：save 会写入 StockHoldings / FuturesPositions，此前 load 未读回，
        // 导致每次世界重载后股票持仓与期货仓位被静默清空）。
        data.loadHoldings(tag);
        if (!tag.contains("StockHoldings", Tag.TAG_COMPOUND)) {
            // 仅当不存在新版持仓区时才执行旧版（Holdings 列表）迁移，避免旧数据反复回填。
            data.migrateLegacyHoldings(tag);
        }

        CompoundTag storageTag = tag.getCompound("PlayerStorages");
        for (String key : storageTag.getAllKeys()) {
            try {
                NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(storageTag.getCompound(key), items);
                data.storages.put(UUID.fromString(key), items);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries.
            }
        }
        return data;
    }

    /** 每位玩家 27 格独立存储（手机现货页使用，持久化到存档）。 */
    public Container storage(UUID id) {
        NonNullList<ItemStack> items = storages.computeIfAbsent(id, k -> {
            NonNullList<ItemStack> list = NonNullList.withSize(27, ItemStack.EMPTY);
            setDirty();
            return list;
        });
        return new PlayerStorage(items, this::setDirty);
    }

    public static final class PlayerStorage implements Container {
        private final NonNullList<ItemStack> items;
        private final Runnable dirty;

        public PlayerStorage(NonNullList<ItemStack> items, Runnable dirty) {
            this.items = items;
            this.dirty = dirty;
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            return items.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return items.get(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
            if (!stack.isEmpty()) {
                dirty.run();
            }
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = items.set(slot, ItemStack.EMPTY);
            if (!stack.isEmpty()) {
                dirty.run();
            }
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            items.set(slot, stack);
            dirty.run();
        }

        @Override
        public void setChanged() {
            dirty.run();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            items.clear();
            dirty.run();
        }
    }

    private static void loadSeries(Series series, CompoundTag tag, String prefix) {
        CompoundTag priceTag = tag.getCompound(prefix + "Prices");
        CompoundTag changeTag = tag.getCompound(prefix + "Changes");
        for (String key : priceTag.getAllKeys()) {
            series.prices.put(key, priceTag.getDouble(key));
            series.changes.put(key, changeTag.getDouble(key));
        }
        CompoundTag historyTag = tag.getCompound(prefix + "History");
        for (String key : historyTag.getAllKeys()) {
            ListTag list = historyTag.getList(key, Tag.TAG_DOUBLE);
            if (list.size() == HISTORY_DAYS) {
                double[] values = new double[HISTORY_DAYS];
                for (int i = 0; i < HISTORY_DAYS; i++) {
                    values[i] = list.getDouble(i);
                }
                series.history.put(key, values);
            }
        }
        CompoundTag daysTag = tag.getCompound(prefix + "AssetDays");
        for (String key : daysTag.getAllKeys()) {
            series.lastDays.put(key, daysTag.getLong(key));
        }
        CompoundTag trendTag = tag.getCompound(prefix + "TrendStreak");
        for (String key : trendTag.getAllKeys()) {
            series.trendStreak.put(key, trendTag.getInt(key));
        }
        CompoundTag dirTag = tag.getCompound(prefix + "LimitDirection");
        for (String key : dirTag.getAllKeys()) {
            series.limitDirection.put(key, dirTag.getInt(key));
        }
        CompoundTag daysLeftTag = tag.getCompound(prefix + "LimitDays");
        for (String key : daysLeftTag.getAllKeys()) {
            series.limitDays.put(key, daysLeftTag.getInt(key));
        }
        CompoundTag rejudgeTag = tag.getCompound(prefix + "RejudgeLimit");
        for (String key : rejudgeTag.getAllKeys()) {
            series.rejudgeLimit.put(key, rejudgeTag.getBoolean(key));
        }
        CompoundTag countTag = tag.getCompound(prefix + "LimitCount");
        for (String key : countTag.getAllKeys()) {
            series.limitCount.put(key, countTag.getInt(key));
        }
    }

    /**
     * 载入股票持仓与期货仓位（与 {@link #save} 写入的字段一一对应）。
     *
     * <p>字段：股票 {@code StockHoldings[uuid][asset] = {Total, Locked}}；
     * 期货 {@code FuturesPositions[uuid][asset] = {Qty, Entry, Mark, Margin, Expiry}}。</p>
     */
    private void loadHoldings(CompoundTag tag) {
        CompoundTag stockTag = tag.getCompound("StockHoldings");
        for (String key : stockTag.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            CompoundTag assets = stockTag.getCompound(key);
            Map<String, StockHolding> map = new HashMap<>();
            for (String asset : assets.getAllKeys()) {
                CompoundTag value = assets.getCompound(asset);
                int total = value.getInt("Total");
                int locked = Math.max(0, value.getInt("Locked"));
                if (total > 0) {
                    map.put(asset, new StockHolding(total, Math.min(locked, total)));
                }
            }
            if (!map.isEmpty()) {
                stocks.put(id, map);
            }
        }

        CompoundTag futuresTag = tag.getCompound("FuturesPositions");
        for (String key : futuresTag.getAllKeys()) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            CompoundTag assets = futuresTag.getCompound(key);
            Map<String, FuturesPosition> map = new HashMap<>();
            for (String asset : assets.getAllKeys()) {
                CompoundTag value = assets.getCompound(asset);
                int qty = value.getInt("Qty");
                if (qty != 0) {
                    map.put(asset, new FuturesPosition(
                            qty,
                            value.getDouble("Entry"),
                            value.getDouble("Mark"),
                            value.getDouble("Margin"),
                            value.getLong("Expiry")));
                }
            }
            if (!map.isEmpty()) {
                futures.put(id, map);
            }
        }
    }

    private void migrateLegacyHoldings(CompoundTag tag) {
        ListTag users = tag.getList("Holdings", Tag.TAG_COMPOUND);
        for (Tag raw : users) {
            CompoundTag user = (CompoundTag) raw;
            UUID id = user.getUUID("Id");
            CompoundTag assets = user.getCompound("Assets");
            for (String key : assets.getAllKeys()) {
                int value = assets.getInt(key);
                if (value <= 0) {
                    continue;
                }
                if (key.startsWith("stock:")) {
                    String asset = key.substring("stock:".length());
                    stocks.computeIfAbsent(id, k -> new HashMap<>())
                            .putIfAbsent(asset, new StockHolding(value, 0));
                } else if (key.startsWith("future:")) {
                    String asset = key.substring("future:".length());
                    double entry = price(asset, true);
                    futures.computeIfAbsent(id, k -> new HashMap<>()).putIfAbsent(asset,
                            new FuturesPosition(value, entry, entry,
                                    entry * value * FUTURES_MARGIN_RATE, lastDay + FUTURES_EXPIRY_DAYS));
                }
            }
        }
        if (users.size() > 0) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("LastDay", lastDay);

        CompoundTag walletTag = new CompoundTag();
        wallets.forEach((id, amount) -> walletTag.putLong(id.toString(), amount));
        tag.put("Wallets", walletTag);
        tag.putInt("MoneyUnit", 2); // 2 = 金额以「分」存储

        saveSeries(stockSeries, tag, "Stock");
        saveSeries(futuresSeries, tag, "Futures");

        CompoundTag stockTag = new CompoundTag();
        stocks.forEach((id, map) -> {
            CompoundTag assets = new CompoundTag();
            map.forEach((asset, holding) -> {
                CompoundTag value = new CompoundTag();
                value.putInt("Total", holding.total);
                value.putInt("Locked", holding.locked);
                assets.put(asset, value);
            });
            stockTag.put(id.toString(), assets);
        });
        tag.put("StockHoldings", stockTag);

        CompoundTag futuresTag = new CompoundTag();
        futures.forEach((id, map) -> {
            CompoundTag assets = new CompoundTag();
            map.forEach((asset, position) -> {
                CompoundTag value = new CompoundTag();
                value.putInt("Qty", position.qty);
                value.putDouble("Entry", position.avgEntry);
                value.putDouble("Mark", position.markPrice);
                value.putDouble("Margin", position.margin);
                value.putLong("Expiry", position.expiryDay);
                assets.put(asset, value);
            });
            futuresTag.put(id.toString(), assets);
        });
        tag.put("FuturesPositions", futuresTag);

        CompoundTag storageTag = new CompoundTag();
        storages.forEach((id, items) -> {
            CompoundTag entry = new CompoundTag();
            ContainerHelper.saveAllItems(entry, items);
            storageTag.put(id.toString(), entry);
        });
        tag.put("PlayerStorages", storageTag);
        return tag;
    }

    private static void saveSeries(Series series, CompoundTag tag, String prefix) {
        CompoundTag priceTag = new CompoundTag();
        CompoundTag changeTag = new CompoundTag();
        series.prices.forEach(priceTag::putDouble);
        series.changes.forEach(changeTag::putDouble);
        tag.put(prefix + "Prices", priceTag);
        tag.put(prefix + "Changes", changeTag);

        CompoundTag historyTag = new CompoundTag();
        series.history.forEach((asset, values) -> {
            ListTag list = new ListTag();
            for (double value : values) {
                list.add(DoubleTag.valueOf(value));
            }
            historyTag.put(asset, list);
        });
        tag.put(prefix + "History", historyTag);

        CompoundTag daysTag = new CompoundTag();
        series.lastDays.forEach(daysTag::putLong);
        tag.put(prefix + "AssetDays", daysTag);

        CompoundTag trendTag = new CompoundTag();
        series.trendStreak.forEach(trendTag::putInt);
        tag.put(prefix + "TrendStreak", trendTag);

        CompoundTag dirTag = new CompoundTag();
        series.limitDirection.forEach(dirTag::putInt);
        tag.put(prefix + "LimitDirection", dirTag);

        CompoundTag daysLeftTag = new CompoundTag();
        series.limitDays.forEach(daysLeftTag::putInt);
        tag.put(prefix + "LimitDays", daysLeftTag);

        CompoundTag rejudgeTag = new CompoundTag();
        series.rejudgeLimit.forEach(rejudgeTag::putBoolean);
        tag.put(prefix + "RejudgeLimit", rejudgeTag);

        CompoundTag countTag = new CompoundTag();
        series.limitCount.forEach(countTag::putInt);
        tag.put(prefix + "LimitCount", countTag);
    }

    public static MarketData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MarketData::load, MarketData::new, "generated_mod_market");
    }

    public boolean onTick(ServerLevel level) {
        long now = level.getDayTime();
        boolean jumped = lastSeenTick != Long.MIN_VALUE && now != lastSeenTick + 1;
        lastSeenTick = now;
        boolean dayChanged = updateDaily(level);
        return jumped || dayChanged;
    }

    public Map<UUID, List<String>> consumeDailyMessages() {
        Map<UUID, List<String>> copy = new HashMap<>(dailyMessages);
        dailyMessages.clear();
        return copy;
    }

    public static String assetName(String asset) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(asset));
        return item == null ? asset : new ItemStack(item).getHoverName().getString();
    }

    /** 钱包余额，单位：<b>分</b>（显示时用 {@code Money.format} 折算为两位小数）。 */
    public long balance(UUID id) {
        return wallets.getOrDefault(id, 0L);
    }

    public void setBalance(UUID id, long value) {
        wallets.put(id, Math.max(0, value));
        setDirty();
    }

    public boolean withdraw(UUID id, long value) {
        if (value < 0 || balance(id) < value) {
            return false;
        }
        setBalance(id, balance(id) - value);
        return true;
    }

    public void deposit(UUID id, long value) {
        if (value > 0) {
            setBalance(id, balance(id) + value);
        }
    }

    public void ensureActivated(ServerLevel level, String asset, boolean futuresBoard) {
        Series series = futuresBoard ? futuresSeries : stockSeries;
        long currentDay = level.getDayTime() / 24000L;
        Double existing = series.prices.get(asset);
        if (existing == null) {
            double base = basePrice(asset);
            series.prices.put(asset, base);
            series.changes.put(asset, 0.0);
            double[] values = new double[HISTORY_DAYS];
            values[HISTORY_DAYS - 1] = base;
            Random random = new Random(((long) asset.hashCode()) * 31L ^ Double.doubleToLongBits(base));
            double swing = volatility(asset);
            double value = base;
            for (int i = HISTORY_DAYS - 2; i >= 0; i--) {
                double daily = (random.nextDouble() * 2.0 - 1.0) * swing;
                double step = 1.0 + daily;
                double denom = Math.max(0.25, step); // 防止 1+daily ≤ 0 导致除零/符号翻转
                value = futuresBoard ? value / denom : Math.max(1.0, value / denom);
                values[i] = value;
            }
            series.history.put(asset, values);
            series.lastDays.put(asset, currentDay);
            setDirty();
            return;
        }
        long last = series.lastDays.getOrDefault(asset, currentDay);
        if (last < currentDay) {
            for (long day = last + 1; day <= currentDay; day++) {
                applyDaily(series, asset, day, futuresBoard);
            }
            series.lastDays.put(asset, currentDay);
            setDirty();
        } else if (last > currentDay) {
            series.lastDays.put(asset, currentDay);
            setDirty();
        }
    }

    private void applyDaily(Series series, String asset, long day, boolean futuresBoard) {
        double current = series.prices.getOrDefault(asset, 100.0);
        Random random = new Random(((long) asset.hashCode()) * 31L ^ day);
        double baseCap = volatility(asset);

        boolean eventActive = series.limitDays.getOrDefault(asset, 0) > 0
                || series.rejudgeLimit.getOrDefault(asset, false);
        if (eventActive) {
            int direction = series.limitDirection.getOrDefault(asset, 1);
            boolean rejudge = series.rejudgeLimit.getOrDefault(asset, false);
            if (rejudge) {
                int count = series.limitCount.getOrDefault(asset, 1);
                double roll = random.nextDouble();
                boolean maintainLimit = count < 3 && roll < 0.40;
                if (maintainLimit) {
                    double next = nextPrice(current, direction * baseCap, futuresBoard);
                    double actual = next / current - 1.0;
                    series.changes.put(asset, actual);
                    series.prices.put(asset, next);
                    shiftHistory(series, asset, next);
                    series.limitCount.put(asset, count + 1);
                    updateStreaks(series, asset, actual);
                    return;
                }
                boolean reverse = count >= 3 ? random.nextBoolean() : roll >= 0.70;
                int moveDir = reverse ? -direction : direction;
                double magnitude = baseCap * (0.4 + random.nextDouble() * 0.5);
                double desired = moveDir * magnitude;
                double next = nextPrice(current, desired, futuresBoard);
                double actual = next / current - 1.0;
                series.changes.put(asset, actual);
                series.prices.put(asset, next);
                shiftHistory(series, asset, next);
                series.limitDays.remove(asset);
                series.limitCount.remove(asset);
                series.limitDirection.remove(asset);
                series.rejudgeLimit.remove(asset);
                updateStreaks(series, asset, actual);
                return;
            }
            int remaining = series.limitDays.getOrDefault(asset, 0);
            double next = nextPrice(current, direction * baseCap, futuresBoard);
            double actual = next / current - 1.0;
            series.changes.put(asset, actual);
            series.prices.put(asset, next);
            shiftHistory(series, asset, next);
            series.limitDays.put(asset, remaining - 1);
            if (remaining - 1 <= 0) {
                series.limitDirection.remove(asset);
                series.rejudgeLimit.remove(asset);
                series.limitCount.remove(asset);
            }
            updateStreaks(series, asset, actual);
            return;
        }

        double cap = baseCap * (0.5 + random.nextDouble() * 0.5);
        double desired = (random.nextDouble() * 2.0 - 1.0) * cap;
        int trend = series.trendStreak.getOrDefault(asset, 0);
        boolean rejudgeEvent = false;

        boolean crashed = false;
        if (trend >= 2) {
            double chance = Math.min(0.55, 0.05 + (trend - 2) * 0.125);
            if (random.nextDouble() < chance) {
                double factor = futuresBoard ? 0.5 + random.nextDouble() * 1.0 : 0.6 + random.nextDouble() * 0.35;
                desired = -factor * baseCap;
                crashed = true;
            }
        }

        if (!crashed) {
            boolean reversal = (trend > 0 && desired < 0) || (trend < 0 && desired > 0);
            if (trend >= 6 || trend <= -4) {
                double limitChance = 0.35 + random.nextDouble() * 0.35;
                if (random.nextDouble() < limitChance) {
                    desired = (trend >= 6 ? 1 : -1) * baseCap;
                } else if (trend >= 6) {
                    desired = -Math.abs(desired);
                } else {
                    desired = Math.abs(desired);
                }
            } else if (reversal) {
                double limitChance = 0.15 + random.nextDouble() * 0.30;
                if (random.nextDouble() < limitChance) {
                    desired = (trend > 0 ? 1 : -1) * baseCap;
                    rejudgeEvent = true;
                }
            }
        }

        double next = nextPrice(current, desired, futuresBoard);
        double actual = next / current - 1.0;
        series.changes.put(asset, actual);
        series.prices.put(asset, next);
        shiftHistory(series, asset, next);

        if (actual >= baseCap - 1e-9 || actual <= -baseCap + 1e-9) {
            int direction = actual >= 0 ? 1 : -1;
            series.limitDirection.put(asset, direction);
            if (rejudgeEvent) {
                series.limitDays.put(asset, 0);
                series.limitCount.put(asset, 1);
                series.rejudgeLimit.put(asset, true);
            } else {
                series.limitDays.put(asset, random.nextInt(3));
                series.limitCount.put(asset, 0);
                series.rejudgeLimit.put(asset, false);
            }
        } else {
            series.limitDays.remove(asset);
            series.limitCount.remove(asset);
            series.limitDirection.remove(asset);
            series.rejudgeLimit.remove(asset);
        }
        updateStreaks(series, asset, actual);
    }

    /**
     * 计算下一日价格。
     *
     * <p><b>股票 / 现货</b>保留 <b>$1 地板价</b>（实物商品不会一文不值）；
     * <b>期货</b>不设地板价 —— 高位波动（110% / 150%）下的暴跌可把价格打到 0 以下，
     * 与现实期货市场的"负价格"行情一致，做多方会因此赔掉保证金以外的资金。</p>
     */
    private static double nextPrice(double current, double desired, boolean futuresBoard) {
        double next = current * (1.0 + desired);
        return futuresBoard ? next : Math.max(1.0, next);
    }
    private static void shiftHistory(Series series, String asset, double next) {
        double[] values = series.history.get(asset);
        if (values == null || values.length != HISTORY_DAYS) {
            values = new double[HISTORY_DAYS];
            values[HISTORY_DAYS - 1] = next;
            series.history.put(asset, values);
        } else {
            System.arraycopy(values, 1, values, 0, HISTORY_DAYS - 1);
            values[HISTORY_DAYS - 1] = next;
        }
    }

    private static void updateStreaks(Series series, String asset, double actual) {
        int trend = series.trendStreak.getOrDefault(asset, 0);
        if (actual > 0) {
            series.trendStreak.put(asset, trend > 0 ? trend + 1 : 1);
        } else if (actual < 0) {
            series.trendStreak.put(asset, trend < 0 ? trend - 1 : -1);
        } else {
            series.trendStreak.put(asset, 0);
        }
    }

    /**
     * 物品基准价：按 <b>7 级稀有度</b>的价格区间取值（等级来源见
     * {@link cn.blockforge.generated.generatedmod.api.rarity.RaritySources}）。
     *
     * <p>1 普通 30–100 ｜ 2 稀有 101–300 ｜ 3 罕见 301–1000 ｜ 4 史诗 1001–3000
     * ｜ 5 传说 3001–10000 ｜ 6 神话 10001–30000 ｜ 7 唯一 30001–100000。</p>
     */
    private static double basePrice(String asset) {
        cn.blockforge.generated.generatedmod.api.rarity.RarityTier tier =
                cn.blockforge.generated.generatedmod.api.rarity.RaritySources.tier(asset);
        int hash = Math.abs(asset.hashCode());
        int span = Math.max(1, tier.priceMax() - tier.priceMin() + 1);
        return tier.priceMin() + hash % span;
    }

    /** 日波动上限（涨跌停幅度）：随稀有度等级递增（10% → 100%）。 */
    private static double volatility(String asset) {
        return cn.blockforge.generated.generatedmod.api.rarity.RaritySources.tier(asset).volatility();
    }

    public double price(String asset, boolean futuresBoard) {
        Series series = futuresBoard ? futuresSeries : stockSeries;
        return series.prices.getOrDefault(asset, 100.0);
    }

    public double change(String asset, boolean futuresBoard) {
        Series series = futuresBoard ? futuresSeries : stockSeries;
        return series.changes.getOrDefault(asset, 0.0);
    }

    public double[] history(String asset, boolean futuresBoard) {
        Series series = futuresBoard ? futuresSeries : stockSeries;
        double[] values = series.history.get(asset);
        return values == null ? new double[0] : values.clone();
    }

    public int stockTotal(UUID id, String asset) {
        StockHolding holding = stocks.getOrDefault(id, Map.of()).get(asset);
        return holding == null ? 0 : holding.total;
    }

    public int stockLocked(UUID id, String asset) {
        StockHolding holding = stocks.getOrDefault(id, Map.of()).get(asset);
        return holding == null ? 0 : holding.locked;
    }

    public void addStock(UUID id, String asset, int delta, boolean locked) {
        Map<String, StockHolding> map = stocks.computeIfAbsent(id, k -> new HashMap<>());
        StockHolding holding = map.computeIfAbsent(asset, k -> new StockHolding(0, 0));
        holding.total += delta;
        if (locked && delta > 0) {
            holding.locked += delta;
        }
        if (holding.total <= 0) {
            map.remove(asset);
        }
        if (map.isEmpty()) {
            stocks.remove(id);
        }
        setDirty();
    }

    public FuturesPosition futuresPosition(UUID id, String asset) {
        return futures.getOrDefault(id, Map.of()).get(asset);
    }

    public void openFutures(UUID id, String asset, int signedDelta, double price, double margin, long expiryDay) {
        Map<String, FuturesPosition> map = futures.computeIfAbsent(id, k -> new HashMap<>());
        FuturesPosition position = map.get(asset);
        if (position == null) {
            position = new FuturesPosition(0, price, price, 0.0, expiryDay);
            map.put(asset, position);
        }
        int old = Math.abs(position.qty);
        position.avgEntry = (position.avgEntry * old + price * Math.abs(signedDelta)) / (old + Math.abs(signedDelta));
        position.qty += signedDelta;
        position.margin += margin;
        position.markPrice = price;
        if (old == 0) {
            position.expiryDay = expiryDay;
        }
        setDirty();
    }

    public void closeFutures(UUID id, String asset, int closeQty, double marginBack) {
        Map<String, FuturesPosition> map = futures.get(id);
        if (map == null) {
            return;
        }
        FuturesPosition position = map.get(asset);
        if (position == null) {
            return;
        }
        position.qty += position.qty > 0 ? -closeQty : closeQty;
        position.margin = Math.max(0.0, position.margin - marginBack);
        if (position.qty == 0) {
            map.remove(asset);
        }
        if (map.isEmpty()) {
            futures.remove(id);
        }
        setDirty();
    }

    public boolean updateDaily(ServerLevel level) {
        long day = level.getDayTime() / 24000L;
        if (day == lastDay) {
            return false;
        }
        if (day < lastDay) {
            lastDay = day;
            setDirty();
            return true;
        }
        lastDay = day;

        for (Map.Entry<UUID, Map<String, StockHolding>> entry : stocks.entrySet()) {
            UUID id = entry.getKey();
            for (Map.Entry<String, StockHolding> asset : entry.getValue().entrySet()) {
                StockHolding holding = asset.getValue();
                if (holding.total <= 0) {
                    continue;
                }
                holding.locked = 0;
                ensureActivated(level, asset.getKey(), false);
                double price = price(asset.getKey(), false);
                double rate = 0.001 + level.random.nextDouble() * 0.003;
                long dividend = Math.max(1L, cn.blockforge.generated.generatedmod.api.economy.Money.fromDollars(price * holding.total * rate));
                wallets.put(id, balance(id) + dividend);
                message(id, "股票分红到账：" + assetName(asset.getKey()) + " ×" + holding.total + "，股息 $" + dividend);
            }
        }

        for (Map.Entry<UUID, Map<String, FuturesPosition>> entry : futures.entrySet()) {
            UUID id = entry.getKey();
            Iterator<Map.Entry<String, FuturesPosition>> iterator = entry.getValue().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, FuturesPosition> asset = iterator.next();
                FuturesPosition position = asset.getValue();
                ensureActivated(level, asset.getKey(), true);
                double price = price(asset.getKey(), true);
                long wallet = balance(id);
                if (position.expiryDay <= day) {
                    long pnl = cn.blockforge.generated.generatedmod.api.economy.Money.fromDollars((price - position.avgEntry) * position.qty);
                    wallet = Math.max(0L, wallet + pnl + Math.round(position.margin));
                    message(id, "期货到期交割：" + assetName(asset.getKey()) + " ×" + Math.abs(position.qty) + "，盈亏 $" + pnl);
                    iterator.remove();
                } else {
                    long delta = cn.blockforge.generated.generatedmod.api.economy.Money.fromDollars((price - position.markPrice) * position.qty);
                    if (wallet + delta < 0L) {
                        wallet = 0L;
                        message(id, "期货被强制平仓：" + assetName(asset.getKey()) + "，资金不足");
                        iterator.remove();
                    } else {
                        wallet += delta;
                        position.markPrice = price;
                    }
                }
                wallets.put(id, wallet);
            }
        }
        setDirty();
        cn.blockforge.generated.generatedmod.AutoTradeManager.onDailyUpdate(level);
        return true;
    }

    private void message(UUID id, String text) {
        dailyMessages.computeIfAbsent(id, k -> new ArrayList<>()).add(text);
    }
}
