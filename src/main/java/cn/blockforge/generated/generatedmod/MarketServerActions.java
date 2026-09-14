package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.data.FuturesPosition;
import cn.blockforge.generated.generatedmod.data.MarketData;
import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import cn.blockforge.generated.generatedmod.data.StockHolding;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.MarketSyncPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import cn.blockforge.generated.generatedmod.api.economy.Money;
import cn.blockforge.generated.generatedmod.api.rarity.RaritySources;
import cn.blockforge.generated.generatedmod.api.rarity.RarityTier;

public final class MarketServerActions {
    public static final long MAX_AMOUNT = 9_999_999L;

    private MarketServerActions() {
    }

    public static void handle(ServerPlayer player, MarketActionPacket packet) {
        ServerLevel level = (ServerLevel) player.level();
        MarketData data = MarketData.get(level);
        String status;
        String quoteAsset = "";
        boolean quoteFutures = false;
        switch (packet.action) {
            case MarketActionPacket.REQUEST_SYNC -> status = "";
            case MarketActionPacket.REQUEST_QUOTE -> {
                status = "";
                if (isValidAsset(packet.asset)) {
                    data.ensureActivated(level, packet.asset, false);
                    quoteAsset = packet.asset;
                }
            }
            case MarketActionPacket.REQUEST_FUTURES_QUOTE -> {
                status = "";
                if (isValidAsset(packet.asset)) {
                    data.ensureActivated(level, packet.asset, true);
                    quoteAsset = packet.asset;
                    quoteFutures = true;
                }
            }
            case MarketActionPacket.BUY_STOCK -> {
                status = stockTrade(player, data, level, packet.asset, true, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
            }
            case MarketActionPacket.SELL_STOCK -> {
                status = stockTrade(player, data, level, packet.asset, false, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
            }
            case MarketActionPacket.BUY_FUTURE -> {
                status = futuresTrade(player, data, level, packet.asset, true, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
                quoteFutures = true;
            }
            case MarketActionPacket.SELL_FUTURE -> {
                status = futuresTrade(player, data, level, packet.asset, false, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
                quoteFutures = true;
            }
            case MarketActionPacket.BUY_SPOT -> {
                status = spotBuy(player, data, level, packet.asset, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
            }
            case MarketActionPacket.SELL_SPOT -> {
                status = spotSell(player, data, level, packet.asset, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
            }
            case MarketActionPacket.OPEN_SPOT -> status = openMenu(player, 2, "现货交易");
            case MarketActionPacket.BROKER_BUY -> {
                status = brokerTrade(player, data, level, true, packet.asset, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
            }
            case MarketActionPacket.BROKER_SELL -> {
                status = brokerTrade(player, data, level, false, packet.asset, packet.amount);
                quoteAsset = validOrEmpty(packet.asset);
            }
            case MarketActionPacket.OPEN_HUB -> status = openMenu(player, 0, "世界金融中心");
            case MarketActionPacket.OPEN_PHONE_HUB -> status = openPhoneHub(player);
            case MarketActionPacket.OPEN_TOWER_STORAGE -> status = openTowerStorage(player, packet.asset);
            case MarketActionPacket.OPEN_MONITOR -> status = openMonitor(player, packet.asset);
            default -> status = "未知操作";
        }
        if (!quoteAsset.isEmpty() && isValidAsset(quoteAsset)) {
            sendQuote(player, data, level, quoteAsset, quoteFutures, status);
        } else {
            sendSnapshot(player, data, level, status);
        }
    }

    public static void sendSnapshot(ServerPlayer player, MarketData data, ServerLevel level, String status) {
        Network.sendToPlayer(player, new MarketSyncPacket(walletSnapshot(player, data, level, status)));
    }

    private static void sendQuote(ServerPlayer player, MarketData data, ServerLevel level,
                                  String asset, boolean futuresBoard, String status) {
        data.ensureActivated(level, asset, futuresBoard);
        MarketSnapshot snapshot = new MarketSnapshot(
                data.balance(player.getUUID()),
                countDollars(player),
                level.getDayTime() / 24000L,
                level.getDayTime(),
                status,
                asset,
                futuresBoard,
                data.price(asset, futuresBoard),
                data.change(asset, futuresBoard),
                data.history(asset, futuresBoard),
                data.stockTotal(player.getUUID(), asset),
                data.stockLocked(player.getUUID(), asset),
                futureQty(data, player, asset),
                futureEntry(data, player, asset),
                futureMargin(data, player, asset),
                futureExpiry(data, player, asset)
        );
        Network.sendToPlayer(player, new MarketSyncPacket(snapshot));
    }

    private static MarketSnapshot walletSnapshot(ServerPlayer player, MarketData data, ServerLevel level, String status) {
        return new MarketSnapshot(data.balance(player.getUUID()), countDollars(player),
                level.getDayTime() / 24000L, level.getDayTime(), status);
    }

    private static int futureQty(MarketData data, ServerPlayer player, String asset) {
        FuturesPosition position = data.futuresPosition(player.getUUID(), asset);
        return position == null ? 0 : position.qty;
    }

    private static double futureEntry(MarketData data, ServerPlayer player, String asset) {
        FuturesPosition position = data.futuresPosition(player.getUUID(), asset);
        return position == null ? 0.0 : position.avgEntry;
    }

    private static double futureMargin(MarketData data, ServerPlayer player, String asset) {
        FuturesPosition position = data.futuresPosition(player.getUUID(), asset);
        return position == null ? 0.0 : position.margin;
    }

    private static long futureExpiry(MarketData data, ServerPlayer player, String asset) {
        FuturesPosition position = data.futuresPosition(player.getUUID(), asset);
        return position == null ? 0L : position.expiryDay;
    }

    private static boolean isValidAsset(String asset) {
        return asset != null && !asset.isEmpty()
                && BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse(asset));
    }

    private static String validOrEmpty(String asset) {
        return isValidAsset(asset) ? asset : "";
    }

    private static boolean validAmount(long amount) {
        return amount >= 1 && amount <= MAX_AMOUNT;
    }

    private static String stockTrade(ServerPlayer player, MarketData data, ServerLevel level,
                                     String asset, boolean buy, long amount) {
        if (!isValidAsset(asset)) {
            return "未知的交易标的";
        }
        if (!validAmount(amount)) {
            return "数量无效：请输入 1 到 " + MAX_AMOUNT + " 之间的数量";
        }
        data.ensureActivated(level, asset, false);
        double unitPrice = Math.max(0.01, data.price(asset, false));
        long total = Money.fromDollars(unitPrice * amount);
        String name = MarketData.assetName(asset);
        if (buy) {
            long commission = Math.max(1L, Money.rate(total, 0.0003));
            if (!data.withdraw(player.getUUID(), total + commission)) {
                return "余额不足：需要 " + Money.format(total + commission);
            }
            data.addStock(player.getUUID(), asset, (int) amount, true);
            return "买入股票成功（T+1，明日可卖）：" + name + " ×" + amount
                    + "，共 " + Money.format(total + commission) + "（含佣金 " + Money.format(commission) + "）";
        }
        int available = data.stockTotal(player.getUUID(), asset) - data.stockLocked(player.getUUID(), asset);
        if (available < amount) {
            return "T+1 限制：今日买入的股票明日才能卖出，当前可卖 " + available;
        }
        long stamp = Math.max(1L, Money.rate(total, 0.001));
        long net = total - stamp;
        data.addStock(player.getUUID(), asset, -(int) amount, false);
        data.deposit(player.getUUID(), net);
        return "卖出股票成功：" + name + " ×" + amount + "，到账 " + Money.format(net) + "（印花税 " + Money.format(stamp) + "）";
    }

    private static String futuresTrade(ServerPlayer player, MarketData data, ServerLevel level,
                                       String asset, boolean buyAction, long amount) {
        if (!isValidAsset(asset)) {
            return "未知的交易标的";
        }
        if (!validAmount(amount)) {
            return "数量无效：请输入 1 到 " + MAX_AMOUNT + " 之间的数量";
        }
        data.ensureActivated(level, asset, true);
        // 期货价格可为负（不做下限钳制），以支持"跌到负数"的实际行情
        double price = data.price(asset, true);
        long fee = Money.CENTS_PER_UNIT; // $1 固定手续费（= 100 分）
        long day = level.getDayTime() / 24000L;
        String name = MarketData.assetName(asset);
        FuturesPosition position = data.futuresPosition(player.getUUID(), asset);

        if (buyAction && position != null && position.qty < 0) {
            int closeable = -position.qty;
            if (amount > closeable) {
                return "最多可平空 " + closeable + " 手，请减少数量";
            }
            int close = (int) Math.min(amount, closeable);
            long pnl = Money.fromDollars((price - position.avgEntry) * (-close));
            double marginBack = position.margin * (close / (double) closeable);
            long delta = pnl + Money.fromDollars(marginBack) - fee;
            data.closeFutures(player.getUUID(), asset, close, marginBack);
            data.setBalance(player.getUUID(), Math.max(0L, data.balance(player.getUUID()) + delta));
            return "平空成功：" + name + " ×" + close + "，盈亏 " + Money.format(pnl - fee) + "，保证金已返还";
        }
        if (!buyAction && position != null && position.qty > 0) {
            int closeable = position.qty;
            if (amount > closeable) {
                return "最多可平多 " + closeable + " 手，请减少数量";
            }
            int close = (int) Math.min(amount, closeable);
            long pnl = Money.fromDollars((price - position.avgEntry) * close);
            double marginBack = position.margin * (close / (double) closeable);
            long delta = pnl + Money.fromDollars(marginBack) - fee;
            data.closeFutures(player.getUUID(), asset, close, marginBack);
            data.setBalance(player.getUUID(), Math.max(0L, data.balance(player.getUUID()) + delta));
            return "平多成功：" + name + " ×" + close + "，盈亏 " + Money.format(pnl - fee) + "，保证金已返还";
        }

        double notional = price * amount;
        long marginCents = Math.max(1L, Money.fromDollars(Math.abs(notional) * MarketData.FUTURES_MARGIN_RATE));
        if (!data.withdraw(player.getUUID(), marginCents + fee)) {
            return "保证金不足：需要 " + Money.format(marginCents + fee) + "（10% 保证金 + " + Money.format(fee) + " 手续费）";
        }
        int signed = buyAction ? (int) amount : -(int) amount;
        data.openFutures(player.getUUID(), asset, signed, price, Money.toDollars(marginCents), day + MarketData.FUTURES_EXPIRY_DAYS);
        String direction = buyAction ? "开多" : "开空";
        return direction + "成功：" + name + " ×" + amount + "，占用保证金 " + Money.format(marginCents)
                + "，合约到期日 第 " + (day + MarketData.FUTURES_EXPIRY_DAYS) + " 天";
    }

    private static String spotBuy(ServerPlayer player, MarketData data, ServerLevel level, String asset, long amount) {
        if (!isValidAsset(asset)) {
            return "未知的交易标的";
        }
        if (!validAmount(amount)) {
            return "数量无效：请输入 1 到 " + MAX_AMOUNT + " 之间的数量";
        }
        data.ensureActivated(level, asset, false);
        double unitPrice = Math.max(0.01, data.price(asset, false));
        long total = Money.fromDollars(unitPrice * amount);
        String name = MarketData.assetName(asset);
        if (!data.withdraw(player.getUUID(), total)) {
            return "余额不足：需要 " + Money.format(total);
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(asset));
        ItemStack stack = new ItemStack(item, (int) amount);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return "现货买入成功：" + name + " ×" + amount + "，已放入背包，共 " + Money.format(total);
    }

    private static String spotSell(ServerPlayer player, MarketData data, ServerLevel level, String asset, long amount) {
        if (!isValidAsset(asset)) {
            return "未知的交易标的";
        }
        if (!validAmount(amount)) {
            return "数量无效：请输入 1 到 " + MAX_AMOUNT + " 之间的数量";
        }
        if (!(player.containerMenu instanceof MarketMenu menu)
                || (menu.mode != 1 && menu.mode != 2 && menu.mode != 3 && menu.mode != 5)) {
            return "当前界面不支持现货交易";
        }
        data.ensureActivated(level, asset, false);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(asset));
        String name = MarketData.assetName(asset);
        Container storage = menu.marketInventory();
        long available = 0L;
        for (int i = 0; i < storage.getContainerSize(); i++) {
            ItemStack stack = storage.getItem(i);
            if (stack.is(item)) {
                available += stack.getCount();
            }
        }
        for (int i = 0; i < Math.min(36, player.getInventory().getContainerSize()); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                available += stack.getCount();
            }
        }
        if (available < amount) {
            return "可出售的 " + name + " 不足：存储区和背包内共只有 " + available;
        }
        long remaining = amount;
        for (int i = 0; i < storage.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = storage.getItem(i);
            if (stack.is(item)) {
                int take = (int) Math.min((long) stack.getCount(), remaining);
                storage.removeItem(i, take);
                remaining -= take;
            }
        }
        for (int i = 0; i < Math.min(36, player.getInventory().getContainerSize()) && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = (int) Math.min((long) stack.getCount(), remaining);
                player.getInventory().removeItem(i, take);
                remaining -= take;
            }
        }
        double unitPrice = Math.max(0.01, data.price(asset, false));
        long gross = Money.fromDollars(unitPrice * amount);
        // 卖出手续费：按物品稀有度等级收取（5.0% ~ 15.0%，等级越高越高）。
        RarityTier tier = RaritySources.tier(asset);
        long feeCents = Math.max(1L, Money.rate(gross, tier.sellFeeRate()));
        long net = Math.max(0L, gross - feeCents);
        data.deposit(player.getUUID(), net);
        return "现货卖出成功：" + name + " ×" + amount + "，成交 " + Money.format(gross)
                + "，手续费 " + Money.format(feeCents) + "（" + tier.displayName() + " " + tier.sellFeePercentText() + "）"
                + "，到账 " + Money.format(net);
    }

    private static int countDollars(ServerPlayer player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(GeneratedMod.DOLLAR.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** 右键做市商村民：打开做市商交易界面（mode 6，缓冲区内携带村民实体 ID）。 */
    public static String openBroker(ServerPlayer player, net.minecraft.world.entity.npc.Villager villager) {
        net.minecraft.core.BlockPos pos = villager.blockPosition();
        int brokerId = villager.getId();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> {
                    MarketMenu menu = new MarketMenu(id, inv, pos, 6);
                    menu.brokerId = brokerId;
                    return menu;
                }, Component.literal("券商交易")),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(6);
                    buf.writeVarInt(brokerId);
                });
        return "";
    }

    /** 统计玩家存储区（本界面容器）+ 背包中该物品的总数。 */
    private static long countOwned(ServerPlayer player, MarketMenu menu, Item item) {
        long available = 0L;
        Container storage = menu.marketInventory();
        for (int i = 0; i < storage.getContainerSize(); i++) {
            ItemStack stack = storage.getItem(i);
            if (stack.is(item)) {
                available += stack.getCount();
            }
        }
        for (int i = 0; i < Math.min(36, player.getInventory().getContainerSize()); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                available += stack.getCount();
            }
        }
        return available;
    }

    /** 从存储区与背包中扣除指定数量的物品（先存储区、后背包）。 */
    private static void takeOwned(ServerPlayer player, MarketMenu menu, Item item, long amount) {
        Container storage = menu.marketInventory();
        long remaining = amount;
        for (int i = 0; i < storage.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = storage.getItem(i);
            if (stack.is(item)) {
                int take = (int) Math.min((long) stack.getCount(), remaining);
                storage.removeItem(i, take);
                remaining -= take;
            }
        }
        for (int i = 0; i < Math.min(36, player.getInventory().getContainerSize()) && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = (int) Math.min((long) stack.getCount(), remaining);
                player.getInventory().removeItem(i, take);
                remaining -= take;
            }
        }
    }

    /**
     * 做市商成交：<b>买入按卖价 ask、卖出按买价 bid</b>，不收取额外手续费（价差即成本）。
     *
     * <p>报价锚定当日行情中间价，并按做市商自身的库存与钱包进行约束：
     * 库存不足按可成交数量成交，钱包不足时按可负担数量成交。</p>
     */
    private static String brokerTrade(ServerPlayer player, MarketData data, ServerLevel level,
                                      boolean buy, String asset, long amount) {
        if (!isValidAsset(asset)) {
            return "未知的交易标的";
        }
        if (!validAmount(amount)) {
            return "数量无效：请输入 1 到 " + MAX_AMOUNT + " 之间的数量";
        }
        if (!(player.containerMenu instanceof MarketMenu menu) || menu.mode != 6 || menu.brokerId <= 0) {
            return "当前界面不支持做市商交易";
        }
        if (!(level.getEntity(menu.brokerId) instanceof net.minecraft.world.entity.npc.Villager broker)) {
            return "找不到该券商";
        }
        data.ensureActivated(level, asset, false);
        cn.blockforge.generated.generatedmod.broker.BrokerData brokerData =
                cn.blockforge.generated.generatedmod.broker.BrokerData.of(broker);
        RarityTier tier = RaritySources.tier(asset);
        double mid = Math.max(0.01, data.price(asset, false));
        String name = MarketData.assetName(asset);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(asset));

        if (buy) {
            int stock = brokerData.stockOf(asset);
            if (stock <= 0) {
                return "券商暂无 " + name + " 库存（可先卖给它一些）";
            }
            long deal = Math.min(amount, stock);
            double ask = cn.blockforge.generated.generatedmod.broker.BrokerQuotes.ask(mid, tier);
            long total = Money.fromDollars(ask * deal);
            if (!data.withdraw(player.getUUID(), total)) {
                return "余额不足：需要 " + Money.format(total);
            }
            ItemStack stack = new ItemStack(item, (int) deal);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            brokerData.addStock(asset, -(int) deal);
            brokerData.wallet += total;
            brokerData.cost = Math.max(0L, brokerData.cost - total);
            brokerData.save(broker);
            cn.blockforge.generated.generatedmod.broker.BrokerName.refresh(broker, brokerData);
            return "券商买入成功：" + name + " ×" + deal + "，按卖价 " + Money.price(ask)
                    + " 成交，共 " + Money.format(total)
                    + (deal < amount ? "（券商库存不足，仅成交 " + deal + "）" : "");
        }

        long available = countOwned(player, menu, item);
        if (available < amount) {
            return "可出售的 " + name + " 不足：存储区和背包内共只有 " + available;
        }
        double bid = cn.blockforge.generated.generatedmod.broker.BrokerQuotes.bid(mid, tier);
        long revenue = Money.fromDollars(bid * amount);
        if (brokerData.wallet < revenue) {
            long affordable = bid <= 0.0 ? 0L : (long) Math.floor(Money.toDollars(brokerData.wallet) / bid);
            if (affordable <= 0L) {
                return "券商资金不足（钱包 " + Money.format(brokerData.wallet) + "）";
            }
            amount = Math.min(amount, affordable);
            revenue = Money.fromDollars(bid * amount);
        }
        takeOwned(player, menu, item, amount);
        data.deposit(player.getUUID(), revenue);
        brokerData.addStock(asset, (int) amount);
        brokerData.wallet -= revenue;
        brokerData.cost += revenue;
        brokerData.save(broker);
        cn.blockforge.generated.generatedmod.broker.BrokerName.refresh(broker, brokerData);
        return "券商卖出成功：" + name + " ×" + amount + "，按买价 " + Money.price(bid)
                + " 成交，到账 " + Money.format(revenue);
    }

    private static String openMenu(ServerPlayer player, int mode, String title) {
        if (!(player.containerMenu instanceof MarketMenu menu)) {
            return "当前不在交易界面";
        }
        BlockPos pos = menu.pos;
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new MarketMenu(id, inv, pos, mode), Component.literal(title)),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(mode);
                });
        return "";
    }

    private static String openPhoneHub(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new MarketMenu(id, inv, pos, 3), Component.literal("MCphone 市场扩展")),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(3);
                });
        return "";
    }

    private static BlockPos parsePos(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** 打开机箱 27 格存储界面（机箱交互动画结束后调用）。 */
    private static String openTowerStorage(ServerPlayer player, String asset) {
        BlockPos pos = parsePos(asset);
        if (pos == null || !(player.level().getBlockEntity(pos)
                instanceof cn.blockforge.generated.mod3ce985ee.ComputerTowerBlockEntity)) {
            return "";
        }
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new MarketMenu(id, inv, pos, 4), Component.literal("机箱存储")),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(4);
                });
        return "";
    }

    /** 打开显示器控制台（须紧邻至少一个机箱；动画结束后调用）。 */
    private static String openMonitor(ServerPlayer player, String asset) {
        BlockPos pos = parsePos(asset);
        if (pos == null) {
            return "";
        }
        boolean nearby = false;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            if (player.level().getBlockEntity(pos.relative(direction))
                    instanceof cn.blockforge.generated.mod3ce985ee.ComputerTowerBlockEntity) {
                nearby = true;
                break;
            }
        }
        if (!nearby) {
            player.displayClientMessage(Component.literal("附近没有机箱，显示器无法操作"), true);
            return "";
        }
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inv, p) -> new MarketMenu(id, inv, pos, 5), Component.literal("显示器控制台")),
                buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeByte(5);
                });
        cn.blockforge.generated.generatedmod.AutoTradeManager.sendSync(player, pos);
        return "";
    }
}
