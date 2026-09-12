package cn.blockforge.generated.generatedmod.network;

import cn.blockforge.generated.generatedmod.client.MonitorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 显示器控制台同步包（S2C）：携带显示器相邻机箱的配置列表。 */
public final class TowerSyncPacket {

    public static final class RuleInfo {
        public final String itemId;
        public final int action;
        public final long threshold;
        public final int amount;
        public final int trigger;
        public final boolean enabled;

        public RuleInfo(String itemId, int action, long threshold, int amount, int trigger, boolean enabled) {
            this.itemId = itemId;
            this.action = action;
            this.threshold = threshold;
            this.amount = amount;
            this.trigger = trigger;
            this.enabled = enabled;
        }
    }

    public static final class TowerInfo {
        public final long pos;
        public final int role;
        public final int redstone;
        /** 机箱当前存储容量（9 + 9×内存条数）；客户端据此显示对应行数槽位。 */
        public final int capacity;
        public final List<RuleInfo> rules = new ArrayList<>();
        /** 机箱存储区物品（绝对索引 0..capacity-1）：客户端图标确定性同步，不再依赖容器槽位同步时序。 */
        public final List<ItemStack> stored = new ArrayList<>();

        public TowerInfo(long pos, int role, int redstone, int capacity) {
            this.pos = pos;
            this.role = role;
            this.redstone = redstone;
            this.capacity = capacity;
        }
    }

    private final List<TowerInfo> towers = new ArrayList<>();
    /** 显示器控制台机箱存储视口的起始行（服务端为准，客户端用于校准）。 */
    private int scrollRow;

    public void setScrollRow(int scrollRow) {
        this.scrollRow = Math.max(0, scrollRow);
    }

    public int scrollRow() {
        return scrollRow;
    }

    public static void encode(TowerSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.towers.size());
        for (TowerInfo tower : packet.towers) {
            buf.writeLong(tower.pos);
            buf.writeByte(tower.role);
            buf.writeByte(tower.redstone);
            buf.writeVarInt(tower.capacity);
            buf.writeVarInt(tower.rules.size());
            for (RuleInfo rule : tower.rules) {
                buf.writeUtf(rule.itemId);
                buf.writeByte(rule.action);
                buf.writeLong(rule.threshold);
                buf.writeVarInt(rule.amount);
                buf.writeByte(rule.trigger);
                buf.writeBoolean(rule.enabled);
            }
            buf.writeVarInt(tower.stored.size());
            for (ItemStack stack : tower.stored) {
                buf.writeItem(stack);
            }
        }
        buf.writeVarInt(packet.scrollRow);
    }

    public static TowerSyncPacket decode(FriendlyByteBuf buf) {
        TowerSyncPacket packet = new TowerSyncPacket();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            TowerInfo tower = new TowerInfo(buf.readLong(), buf.readByte(), buf.readByte(), buf.readVarInt());
            int ruleCount = buf.readVarInt();
            for (int j = 0; j < ruleCount; j++) {
                tower.rules.add(new RuleInfo(buf.readUtf(256), buf.readByte(), buf.readLong(),
                        buf.readVarInt(), buf.readByte(), buf.readBoolean()));
            }
            int storedCount = buf.readVarInt();
            for (int j = 0; j < storedCount; j++) {
                tower.stored.add(buf.readItem());
            }
            packet.towers.add(tower);
        }
        packet.scrollRow = buf.readVarInt();
        return packet;
    }

    public List<TowerInfo> towers() {
        return towers;
    }

    /** 客户端接收：交给当前打开的显示器控制台（若未打开则缓存，打开时应用）。 */
    public static void handle(TowerSyncPacket packet, net.minecraftforge.network.NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof MonitorScreen screen) {
                screen.applySync(packet);
            } else {
                MonitorScreen.pending = packet;
            }
        });
        ctx.setPacketHandled(true);
    }
}
