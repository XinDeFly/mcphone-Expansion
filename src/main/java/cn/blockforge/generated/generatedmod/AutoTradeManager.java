package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.api.economy.Money;
import cn.blockforge.generated.generatedmod.data.MarketData;
import cn.blockforge.generated.generatedmod.network.Network;
import cn.blockforge.generated.generatedmod.network.TowerActionPacket;
import cn.blockforge.generated.generatedmod.network.TowerSyncPacket;
import cn.blockforge.generated.mod3ce985ee.ComputerTowerBlockEntity;
import cn.blockforge.generated.mod3ce985ee.ComputerTowerBlockEntity.TowerRule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * 机箱自动化交易引擎（服务端）：
 * <ul>
 *   <li>每日价格更新后执行 {@link #TRIGGER_NEXT_UPDATE} 规则；</li>
 *   <li>每 20 刻扫描执行 {@code TRIGGER_ON_PRICE} / {@code TRIGGER_ON_REDSTONE} 规则（每日最多一次）；</li>
 *   <li>资金流走机箱绑定玩家的钱包（放置者）；</li>
 * </ul>
 */
public final class AutoTradeManager {

    private AutoTradeManager() {
    }

    /** 每日价格更新后的处理（由 MarketData.updateDaily 调用）。 */
    public static void onDailyUpdate(ServerLevel level) {
        scan(level, ComputerTowerBlockEntity.TRIGGER_NEXT_UPDATE);
    }

    /** 每 20 刻扫描（由 ServerEvents 调用）。 */
    public static void tick(ServerLevel level) {
        scan(level, ComputerTowerBlockEntity.TRIGGER_ON_PRICE);
        scan(level, ComputerTowerBlockEntity.TRIGGER_ON_REDSTONE);
    }

    private static void scan(ServerLevel level, int triggerType) {
        MarketData data = MarketData.get(level);
        String dim = level.dimension().location().toString();
        for (String key : ComputerTowerBlockEntity.activeKeys()) {
            int idx = key.indexOf('|');
            if (idx < 0 || !key.substring(0, idx).equals(dim)) {
                continue;
            }
            BlockPos pos = BlockPos.of(Long.parseLong(key.substring(idx + 1)));
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof ComputerTowerBlockEntity tower) {
                runRules(level, data, tower, triggerType);
            }
        }
    }

    private static void runRules(ServerLevel level, MarketData data, ComputerTowerBlockEntity tower, int triggerType) {
        if (tower.owner() == null) {
            return;
        }
        long day = level.getDayTime() / 24000L;
        for (TowerRule rule : List.copyOf(tower.rules())) {
            if (!rule.enabled || rule.trigger != triggerType) {
                continue;
            }
            // 角色闸门：出售规则仅在「出售」机箱执行，购入规则仅在「购入」机箱执行。
            if (rule.action == 1 && tower.role() != ComputerTowerBlockEntity.ROLE_SELL) {
                continue;
            }
            if (rule.action == 0 && tower.role() != ComputerTowerBlockEntity.ROLE_BUY) {
                continue;
            }
            if (rule.lastFired == day) {
                continue;
            }
            if (triggerType == ComputerTowerBlockEntity.TRIGGER_ON_REDSTONE && !redstoneOk(level, tower)) {
                continue;
            }
            if (triggerType == ComputerTowerBlockEntity.TRIGGER_ON_PRICE && !priceOk(data, tower, rule)) {
                continue;
            }
            execute(level, data, tower, rule, day);
        }
    }

    private static boolean redstoneOk(ServerLevel level, ComputerTowerBlockEntity tower) {
        boolean powered = level.getBestNeighborSignal(tower.getBlockPos()) > 0;
        return switch (tower.redstone()) {
            case ComputerTowerBlockEntity.REDSTONE_POWERED -> powered;
            case ComputerTowerBlockEntity.REDSTONE_UNPOWERED -> !powered;
            default -> true;
        };
    }

    private static boolean priceOk(MarketData data, ComputerTowerBlockEntity tower, TowerRule rule) {
        double price = data.price(rule.itemId, false);
        return rule.action == 1 ? price >= rule.threshold : price <= rule.threshold;
    }

    private static void execute(ServerLevel level, MarketData data, ComputerTowerBlockEntity tower, TowerRule rule, long day) {
        double price = Math.max(0.01, data.price(rule.itemId, false));
        String name = MarketData.assetName(rule.itemId);
        ServerPlayer owner = level.getServer() == null ? null : level.getServer().getPlayerList().getPlayer(tower.owner());
        if (rule.action == 1) {
            // 出售：从机箱取物品，按当日市价入钱包。
            int amount = Math.min(rule.amount, tower.countItems(rule.itemId));
            if (amount <= 0) {
                notify(owner, "机箱自动出售失败：" + name + "（机箱内数量不足）");
                return;
            }
            tower.removeItems(rule.itemId, amount);
            long revenue = Money.fromDollars(price * amount);
            data.deposit(tower.owner(), revenue);
            rule.lastFired = day;
            tower.touch();
            notify(owner, "机箱自动出售成功：" + name + " ×" + amount + "，到账 " + Money.format(revenue));
        } else {
            // 购入：从绑定玩家钱包扣款，物品入机箱。
            long cost = Money.fromDollars(price * rule.amount);
            if (!data.withdraw(tower.owner(), cost)) {
                notify(owner, "机箱自动购入失败：" + name + "（钱包余额不足，需要 " + Money.format(cost) + "）");
                return;
            }
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(rule.itemId)), 1);
            if (stack.isEmpty() || stack.is(Items.AIR)) {
                data.deposit(tower.owner(), cost);
                return;
            }
            int inserted = tower.insertItems(stack, rule.amount);
            if (inserted < rule.amount) {
                long refund = Money.fromDollars((rule.amount - inserted) * price);
                data.deposit(tower.owner(), refund);
            }
            rule.lastFired = day;
            tower.touch();
            notify(owner, "机箱自动购入成功：" + name + " ×" + inserted + "，花费 " + Money.format(Money.fromDollars(inserted * price)));
        }
    }

    private static void notify(ServerPlayer owner, String message) {
        if (owner != null) {
            owner.sendSystemMessage(Component.literal(message));
        }
    }

    // ---- 显示器操作 ----

    /** 客户端 TowerActionPacket 处理：修改机箱配置，并回发最新同步包。 */
    public static void handleAction(ServerPlayer player, TowerActionPacket packet) {
        ServerLevel level = (ServerLevel) player.level();
        // 视口滚动：pos 字段不指向机箱（客户端传 0），必须先于机箱校验处理，否则服务端永远收不到滚动
        if (packet.action == TowerActionPacket.SCROLL) {
            if (player.containerMenu instanceof cn.blockforge.generated.generatedmod.menu.MarketMenu menu
                    && menu.mode == 5) {
                menu.setTowerScrollRow(packet.value);
                sendSync(player, menu.pos, menu.towerScrollRow());
            }
            return;
        }
        BlockPos pos = BlockPos.of(packet.pos);
        if (!(level.getBlockEntity(pos) instanceof ComputerTowerBlockEntity tower)) {
            return;
        }
        // 同步包必须以「显示器」为中心扫描相邻机箱；packet.pos 是机箱自身位置，
        // 若直接用它扫描会得到错误（几乎为空）的机箱列表，导致客户端选择状态被清空。
        BlockPos syncPos = pos;
        if (player.containerMenu instanceof cn.blockforge.generated.generatedmod.menu.MarketMenu menu
                && menu.mode == 5) {
            syncPos = menu.pos;
        }
        switch (packet.action) {
            case TowerActionPacket.SET_ROLE -> tower.setRole(packet.value);
            case TowerActionPacket.SET_REDSTONE -> tower.setRedstone(packet.value);
            case TowerActionPacket.SET_RULE -> {
                TowerRule rule = new TowerRule();
                rule.itemId = packet.itemId;
                rule.action = packet.value;
                rule.threshold = Math.max(1L, packet.threshold);
                rule.amount = Math.max(1, Math.min(999_999, packet.amount));
                rule.trigger = (byte) packet.trigger;
                rule.enabled = packet.enabled;
                tower.setRule(rule);
            }
            case TowerActionPacket.REMOVE_RULE -> tower.removeRule(packet.itemId);
            case TowerActionPacket.SET_TARGET -> {
                // 显示器控制台：将菜单的存储视口绑定到所选机箱并同步物品
                // （同时记录玩家，机箱存储内容变化时由菜单主动补发同步包）
                if (player.containerMenu instanceof cn.blockforge.generated.generatedmod.menu.MarketMenu menu
                        && menu.mode == 5) {
                    menu.bindTowerContainer(tower, player);
                }
            }
            default -> {
            }
        }
        sendSync(player, syncPos);
    }

    /** 构建并发送指定显示器周围的机箱配置同步包。 */
    public static void sendSync(ServerPlayer player, BlockPos monitorPos) {
        sendSync(player, monitorPos, 0);
    }

    /** 构建并发送同步包（并携带当前视口滚动行，供客户端校准）。 */
    public static void sendSync(ServerPlayer player, BlockPos monitorPos, int scrollRow) {
        ServerLevel level = (ServerLevel) player.level();
        TowerSyncPacket packet = new TowerSyncPacket();
        packet.setScrollRow(scrollRow);
        for (Direction direction : Direction.values()) {
            BlockPos towerPos = monitorPos.relative(direction);
            if (level.getBlockEntity(towerPos) instanceof ComputerTowerBlockEntity tower) {
                TowerSyncPacket.TowerInfo info = new TowerSyncPacket.TowerInfo(towerPos.asLong(), tower.role(),
                        tower.redstone(), tower.getContainerSize());
                for (TowerRule rule : tower.rules()) {
                    info.rules.add(new TowerSyncPacket.RuleInfo(rule.itemId, rule.action, rule.threshold,
                            rule.amount, rule.trigger, rule.enabled));
                }
                // 携带存储区物品：客户端图标确定性同步（不依赖容器槽位同步包的时序/状态）
                for (int s = 0; s < tower.getContainerSize(); s++) {
                    info.stored.add(tower.getItem(s));
                }
                packet.towers().add(info);
            }
        }
        Network.sendToPlayer(player, packet);
    }
}
