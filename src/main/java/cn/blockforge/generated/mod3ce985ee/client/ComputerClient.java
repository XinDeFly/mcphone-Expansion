package cn.blockforge.generated.mod3ce985ee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ComputerClient {
    private ComputerClient() {
    }

    public static void openMonitor(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ComputerScreen(true, pos));
    }

    public static void openTower(BlockPos pos) {
        Minecraft.getInstance().setScreen(new ComputerScreen(false, pos));
    }
}
