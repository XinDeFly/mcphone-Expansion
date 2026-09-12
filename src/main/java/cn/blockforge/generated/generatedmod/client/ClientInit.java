package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private ClientInit() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.ScreenConstructor<MarketMenu, AbstractContainerScreen<MarketMenu>> constructor =
                    (menu, inventory, title) -> {
            if (menu.mode == 1) {
                return new TerminalSpotScreen(menu, inventory, title);
            }
            if (menu.mode == 2) {
                return new PlatformSpotScreen(menu, inventory, title);
            }
            if (menu.mode == 3) {
                return new cn.blockforge.generated.generatedmod.client.phone.PhoneLandscapeScreen(menu, inventory, title);
            }
            if (menu.mode == 4) {
                return new TowerStorageScreen(menu, inventory, title);
            }
            if (menu.mode == 5) {
                return new MonitorScreen(menu, inventory, title);
            }
            return new MarketScreen(menu, inventory, title);
            };
            MenuScreens.register(GeneratedMod.MARKET_MENU.get(), constructor);
        });
    }
}
