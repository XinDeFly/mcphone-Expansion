package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import cn.blockforge.generated.generatedmod.api.economy.Money;

public class MarketScreen extends AbstractContainerScreen<MarketMenu> {
    private static final int HELP_X = 8;
    private static final int HELP_Y = 8;
    private static final int HELP_SIZE = 18;
    private final long startTime = System.currentTimeMillis();

    public MarketScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 360;
        this.imageHeight = 190;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("股票"), b -> openStock())
                .bounds(this.leftPos + 8, this.topPos + 100, 108, 36).build());
        this.addRenderableWidget(Button.builder(Component.literal("期货"), b -> openFutures())
                .bounds(this.leftPos + 124, this.topPos + 100, 108, 36).build());
        this.addRenderableWidget(Button.builder(Component.literal("现货交易"), b -> Network.sendToServer(
                        new MarketActionPacket(MarketActionPacket.OPEN_SPOT, "", 0)))
                .bounds(this.leftPos + 240, this.topPos + 100, 112, 36).build());
    }

    private void openStock() {
        this.minecraft.setScreen(new StockScreen(this.menu, this.menu.playerInventory));
    }

    private void openFutures() {
        this.minecraft.setScreen(new FuturesScreen(this.menu, this.menu.playerInventory));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xff0a0e17);
        graphics.fill(this.leftPos + 6, this.topPos + 4, this.leftPos + this.imageWidth - 6, this.topPos + 88, 0xff111827);
        graphics.fill(this.leftPos + 6, this.topPos + 92, this.leftPos + this.imageWidth - 6, this.topPos + this.imageHeight - 6, 0xff0e1524);

        for (int i = 1; i < 14; i++) {
            graphics.fill(this.leftPos + i * 26, this.topPos + 92, this.leftPos + i * 26 + 1, this.topPos + this.imageHeight - 6, 0xff152033);
        }
        this.drawFallingBitcoins(graphics);
        this.drawCandlesticks(graphics);
        int hx = this.leftPos + HELP_X;
        int hy = this.topPos + HELP_Y;
        graphics.fill(hx, hy, hx + HELP_SIZE, hy + HELP_SIZE, 0xff111827);
        graphics.fill(hx - 1, hy - 1, hx + HELP_SIZE + 1, hy, 0xff4b5b6f);
        graphics.fill(hx - 1, hy + HELP_SIZE, hx + HELP_SIZE + 1, hy + HELP_SIZE + 1, 0xff4b5b6f);
        graphics.fill(hx - 1, hy, hx, hy + HELP_SIZE, 0xff4b5b6f);
        graphics.fill(hx + HELP_SIZE, hy, hx + HELP_SIZE + 1, hy + HELP_SIZE, 0xff4b5b6f);
    }

    private void drawFallingBitcoins(GuiGraphics graphics) {
        long t = System.currentTimeMillis() - this.startTime;
        double speed = 48.0;
        // 下落范围限制在面板内部（上下各留出边距），避免金币飘到界面之外。
        int top = this.topPos + 6;
        int bottom = this.topPos + this.imageHeight - 20;
        double fallHeight = Math.max(20, bottom - top);
        for (int i = 0; i < 10; i++) {
            java.util.Random seed = new java.util.Random(i * 2654435761L);
            double phase = seed.nextDouble() * fallHeight;
            double position = (t / speed + phase) % fallHeight;
            long cycle = (long) (position / fallHeight);
            int y = top + (int) (position % fallHeight);
            java.util.Random random = new java.util.Random((cycle * 31L + i) * 2654435761L);
            int x = this.leftPos + 8 + random.nextInt(this.imageWidth - 30);
            int alpha = 90 + (i * 17) % 110;
            this.drawBitcoin(graphics, x, y, alpha);
        }
    }

    private void drawBitcoin(GuiGraphics graphics, int x, int y, int alpha) {
        int outer = (alpha << 24) | 0xB8860B;
        int color = (alpha << 24) | 0xF2A900;
        int shine = ((alpha + 30) << 24) | 0xFFD86B;
        drawCircle(graphics, x + 8, y + 8, 8, outer);
        drawCircle(graphics, x + 8, y + 8, 7, color);
        drawCircle(graphics, x + 6, y + 6, 3, shine);
        graphics.drawString(this.font, "B", x + 5, y + 4, 0xff3b2400);
    }

    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            graphics.fill(centerX - dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
        }
    }

    private void drawCandlesticks(GuiGraphics graphics) {
        int top = this.topPos + 142;
        int bottom = this.topPos + 184;
        for (int i = 0; i < 12; i++) {
            int x = this.leftPos + 16 + i * 28;
            boolean up = (i * 5 + 1) % 4 != 0;
            int base = up ? 0xff2ecc71 : 0xffe74c3c;
            int bodyTop;
            int bodyBottom;
            if (i % 3 == 0) {
                bodyTop = top + 4;
                bodyBottom = bottom - 4;
            } else {
                bodyTop = up ? top + 10 + (i * 3) % 8 : top + 16 + (i * 5) % 10;
                bodyBottom = up ? bottom - 10 - (i * 4) % 8 : bottom - 8 - (i * 7) % 8;
            }
            int glow = (70 << 24) | base;
            graphics.fill(x - 3, bodyTop - 3, x + 9, bodyBottom + 3, glow);
            int wickTop = top + 3 + (i * 7) % 5;
            int wickBottom = bottom - 3 - (i * 5) % 5;
            graphics.fill(x + 2, wickTop, x + 4, wickBottom, base);
            graphics.fill(x, bodyTop, x + 6, bodyBottom, base);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, "!", HELP_X + (HELP_SIZE - this.font.width("!")) / 2,
                HELP_Y + 5, 0xffe5e7eb);
        long t = System.currentTimeMillis() - this.startTime;
        float pulse = (float) (0.5 + 0.5 * Math.sin(t / 450.0));
        int titleColor = lerpColor(0xFFD4AF37, 0xFFFFF2B8, pulse);
        float scale = 1.7f + 0.08f * pulse;
        float bob = (float) Math.sin(t / 700.0) * 2.0f;
        String title = "世界金融中心";
        float width = this.font.width(title) * scale;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate((this.imageWidth - width) / 2.0f, 6.0f + bob, 0.0f);
        pose.scale(scale, scale, 1.0f);
        graphics.drawString(this.font, title, 0, 0, titleColor);
        pose.popPose();

        MarketSnapshot snapshot = this.menu.snapshot();
        String wallet = "钱包: " + Money.format(snapshot.balance());
        String time = MarketUi.headerTime(snapshot.dayTime());
        graphics.drawString(this.font, time, 10, 44, 0xff8fa3bf);
        graphics.drawString(this.font, wallet, this.imageWidth - 8 - this.font.width(wallet), 44, 0xff3bd18b);

        String status = snapshot.status();
        if (status != null && !status.isEmpty()) {
            List<FormattedCharSequence> lines = this.font.split(Component.literal(status), this.imageWidth - 20);
            int y = this.imageHeight - 10 - (lines.size() - 1) * 10;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(this.font, line, 10, y, 0xffffb4b4);
                y += 10;
            }
        }
    }

    private boolean overHelp(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + HELP_X && mouseX <= this.leftPos + HELP_X + HELP_SIZE
                && mouseY >= this.topPos + HELP_Y && mouseY <= this.topPos + HELP_Y + HELP_SIZE;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overHelp(mouseX, mouseY)) {
            this.minecraft.setScreen(new HelpScreen(this.menu, this.menu.playerInventory));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (overHelp(mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.literal("帮助与功能提示")), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private static int lerpColor(int a, int b, float f) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * f);
        int g = Math.round(ag + (bg - ag) * f);
        int bl = Math.round(ab + (bb - ab) * f);
        return (0xFF << 24) | (r << 16) | (g << 8) | bl;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
