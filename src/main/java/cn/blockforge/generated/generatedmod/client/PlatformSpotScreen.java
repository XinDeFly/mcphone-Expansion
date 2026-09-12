package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PlatformSpotScreen extends AbstractSpotScreen {
    public PlatformSpotScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("现货交易"));
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                        Component.literal("返回"), b -> Network.sendToServer(
                                new MarketActionPacket(MarketActionPacket.OPEN_HUB, "", 0)))
                .bounds(this.leftPos + 8, this.topPos + 220, 80, 20).build());
    }
}
