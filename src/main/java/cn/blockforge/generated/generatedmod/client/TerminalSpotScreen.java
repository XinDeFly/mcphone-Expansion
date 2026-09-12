package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TerminalSpotScreen extends AbstractSpotScreen {
    public TerminalSpotScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("公共市场终端"));
    }
}
