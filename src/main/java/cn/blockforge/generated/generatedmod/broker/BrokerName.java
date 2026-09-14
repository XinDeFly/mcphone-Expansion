package cn.blockforge.generated.generatedmod.broker;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;

/**
 * 做市商名字牌：显示「村民原名 + 换行 + 钱包余额」两行。
 *
 * <p>采用原版自定义名（{@code setCustomName}）实现 —— 自定义名会随实体元数据同步到客户端，
 * 因此无需额外的网络包；名字中的换行由原版名字渲染支持。</p>
 */
public final class BrokerName {

    private BrokerName() {
    }

    /** 刷新该村民的名字牌（原名下方显示当前钱包余额）。 */
    public static void refresh(Villager villager, BrokerData data) {
        if (data.baseName == null || data.baseName.isEmpty()) {
            Component vanilla = villager.getName();
            data.baseName = vanilla == null ? "券商" : vanilla.getString();
        }
        villager.setCustomName(data.displayName());
        villager.setCustomNameVisible(true);
    }
}
