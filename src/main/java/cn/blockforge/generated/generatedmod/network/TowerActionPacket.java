package cn.blockforge.generated.generatedmod.network;

import net.minecraft.network.FriendlyByteBuf;

/** 显示器控制台操作包（C2S）：修改机箱角色/红石模式/自动规则。 */
public final class TowerActionPacket {

    public static final byte SET_ROLE = 0;
    public static final byte SET_REDSTONE = 1;
    public static final byte SET_RULE = 2;
    public static final byte REMOVE_RULE = 3;
    /** 显示器控制台：切换存储展示到指定机箱。 */
    public static final byte SET_TARGET = 4;
    /** 显示器控制台：机箱存储视口滚动（value = 起始行）。 */
    public static final byte SCROLL = 5;

    public final byte action;
    public final long pos;
    public final int value;
    public final long threshold;
    public final int amount;
    public final String itemId;
    public final int trigger;
    public final boolean enabled;

    public TowerActionPacket(byte action, long pos, int value, long threshold, int amount, String itemId, int trigger, boolean enabled) {
        this.action = action;
        this.pos = pos;
        this.value = value;
        this.threshold = threshold;
        this.amount = amount;
        this.itemId = itemId == null ? "" : itemId;
        this.trigger = trigger;
        this.enabled = enabled;
    }

    public static void encode(TowerActionPacket packet, FriendlyByteBuf buf) {
        buf.writeByte(packet.action);
        buf.writeLong(packet.pos);
        buf.writeInt(packet.value);
        buf.writeLong(packet.threshold);
        buf.writeInt(packet.amount);
        buf.writeUtf(packet.itemId);
        buf.writeByte(packet.trigger);
        buf.writeBoolean(packet.enabled);
    }

    public static TowerActionPacket decode(FriendlyByteBuf buf) {
        return new TowerActionPacket(buf.readByte(), buf.readLong(), buf.readInt(), buf.readLong(),
                buf.readInt(), buf.readUtf(256), buf.readByte(), buf.readBoolean());
    }
}
