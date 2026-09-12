package cn.blockforge.generated.mod3ce985ee.client;

import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * 电脑开机动画界面：显示设备外壳（与功能界面共用 ComputerLayout 几何，保证内容始终在屏幕范围内），
 * 动画结束后（约 15 刻）自动请求打开对应功能
 * （机箱 → 27 格存储；显示器 → 机箱控制台）。
 */
public final class ComputerScreen extends Screen {
    private final boolean monitor;
    private final BlockPos pos;
    private ComputerLayout.Device device;
    private int glowTicks;
    private int ticks;
    private boolean sent;

    public ComputerScreen(boolean monitor, BlockPos pos) {
        super(Component.empty());
        this.monitor = monitor;
        this.pos = pos;
    }

    @Override
    protected void init() {
        this.device = ComputerLayout.Device.of(this.width, this.height, this.monitor);
        this.glowTicks = 12;
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.18F, monitor ? 1.55F : 1.35F);
        }
    }

    @Override
    public void tick() {
        if (glowTicks > 0) {
            glowTicks--;
        }
        ticks++;
        if (ticks == 15 && !sent && this.pos != null) {
            sent = true;
            Network.sendToServer(new MarketActionPacket(
                    monitor ? MarketActionPacket.OPEN_MONITOR : MarketActionPacket.OPEN_TOWER_STORAGE,
                    this.pos.toShortString(), 0));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        ComputerLayout.drawCasing(graphics, this.device);
        ComputerLayout.Rect s = this.device.screen();
        if (glowTicks > 0) {
            int glow = Math.min(90, glowTicks * 7);
            graphics.fill(s.x + 3, s.y + 3, s.x + s.w - 3, s.y + s.h - 3, (glow << 24) | 0x27A9C8);
        }
        // 屏幕内顶部：开机进度条 + 提示
        if (ticks < 15) {
            graphics.fill(s.x + 26, s.y + 16, s.x + 86, s.y + 20, 0xFF68717D);
            if (ticks > 2) {
                graphics.drawString(this.font, this.monitor ? "显示器开机中…" : "机箱开机中…",
                        s.x + 26, s.y + 28, 0xFF8FA3BF);
            }
        } else {
            graphics.drawString(this.font, "即将打开" + (this.monitor ? "机箱控制台" : "机箱存储") + "…",
                    s.x + 26, s.y + 28, 0xFF8FA3BF);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.12F, monitor ? 1.15F : 1.0F);
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
