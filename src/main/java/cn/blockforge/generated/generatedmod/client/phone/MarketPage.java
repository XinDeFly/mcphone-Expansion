package cn.blockforge.generated.generatedmod.client.phone;

import cn.blockforge.generated.generatedmod.api.client.ScrollableText;
import cn.blockforge.generated.generatedmod.api.client.phone.ILandscapePage;
import cn.blockforge.generated.generatedmod.api.client.phone.LandscapePhoneChassis;
import cn.blockforge.generated.generatedmod.client.AbstractTradeScreen;
import cn.blockforge.generated.generatedmod.client.HelpScreen;
import cn.blockforge.generated.generatedmod.client.ItemIndex;
import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.november.mcphone.core.client.PhoneScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 方块金融横屏页面：行情中心主界面 + 股票 / 期货 / 现货三大交易板块。
 * 基于公共底盘 {@link LandscapePhoneChassis} 绘制与交互。
 */
public final class MarketPage implements ILandscapePage {

    private static final int MAX_RESULTS = 100;
    private static final long MAX_AMOUNT = 9_999_999L;
    private static final int RECENT_LIMIT = 8;
    /** 现货页槽位单元边长（9 列 × 7 行 = 27 存储 + 36 背包）。 */
    private static final int SPOT_CELL = 16;
    /** 底部数量按钮：文案、宽度与间距（保证长数字完整包裹）。 */
    private static final String[] AMOUNT_LABELS = {"-100", "-10", "-1", "+1", "+10", "+100", "最大", "最小", "已拥有"};
    private static final int[] AMOUNT_WS = {28, 20, 16, 16, 20, 28, 22, 22, 30};
    private static final int AMOUNT_GAP = 3;
    private static final int CONTROL_H = 16;
    private static final List<String> RECENT = new ArrayList<>();

    private enum Page { HUB, STOCK, FUTURES, SPOT, HELP }

    private final MarketMenu menu;
    private LandscapePhoneChassis chassis;
    private Font font;
    private Page page = Page.HUB;

    private Rectangle stockButton;
    private Rectangle futuresButton;
    private Rectangle spotButton;
    private Rectangle backButton;
    private Rectangle helpButton;
    private Slot hoveredSlot;
    /** 使用说明页滚动文本（复用公共控件）。 */
    private final ScrollableText helpText = new ScrollableText();

    private EditBox searchBox;
    private EditBox amountBox;
    private final List<String> matches = new ArrayList<>();
    private String searchText = "";
    private int selected;
    private int scrollOffset;
    private long amount = 1L;
    private long lastRequestedDay = -1L;
    private String lastRequestedAsset = "";
    private long lastSyncedDay = -1L;

    public MarketPage(MarketMenu menu) {
        this.menu = menu;
    }

    @Override
    public void init(LandscapePhoneChassis chassis, Font font) {
        this.chassis = chassis;
        this.font = font;
        int cx = chassis.contentX();
        int cy = chassis.contentY();
        int cw = chassis.contentW();
        int ch = chassis.contentH();
        int bw = (cw - 20) / 3;
        this.stockButton = new Rectangle(cx + 6, cy + ch - 30, bw, 22);
        this.futuresButton = new Rectangle(cx + 13 + bw, cy + ch - 30, bw, 22);
        this.spotButton = new Rectangle(cx + 20 + bw * 2, cy + ch - 30, bw, 22);
        this.backButton = new Rectangle(cx, chassis.topPos() + 18, 44, 13);
        this.helpButton = new Rectangle(cx + 2, chassis.topPos() + 20, 44, 16);
        this.helpText.setLines(HelpScreen.HELP_LINES, font, chassis.contentW() - 12);

        this.searchBox = new EditBox(font, cx, cy + 32, 100, 12, Component.literal("搜索/拼音"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.literal("拼音/中文…"));
        this.searchBox.setResponder(text -> {
            this.searchText = text == null ? "" : text.trim();
            this.scrollOffset = 0;
            this.selected = 0;
            this.rebuildMatches();
        });

        this.amountBox = new EditBox(font, cx, cy + ch - 31, 46, 16, Component.literal("数量"));
        this.amountBox.setMaxLength(7);
        this.amountBox.setValue("1");
        this.amountBox.setResponder(text -> {
            String clean = text == null ? "" : text.replaceAll("[^0-9]", "");
            if (!clean.equals(text)) {
                this.amountBox.setValue(clean);
                return;
            }
            this.amount = clean.isEmpty() ? 0L : Math.min(MAX_AMOUNT, Long.parseLong(clean));
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.ensureQuotes();
        if (this.page == Page.HUB) {
            this.renderHub(graphics, mouseX, mouseY);
        } else if (this.page == Page.HELP) {
            this.renderHelp(graphics, mouseX, mouseY);
        } else {
            this.renderBoard(graphics, mouseX, mouseY);
        }
        // 手持中/拖拽中的物品显示在鼠标处（原版容器行为）。
        ItemStack carried = this.menu.getCarried();
        if (this.page == Page.SPOT && !carried.isEmpty()) {
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
        }
    }

    private void renderHub(GuiGraphics graphics, int mouseX, int mouseY) {
        long t = System.currentTimeMillis() - this.chassis.startMillis();
        // 金币动画裁剪到手机屏幕内；金币会在触碰底部黑线前淡出完毕。
        graphics.enableScissor(this.chassis.leftPos() + 20, this.chassis.topPos() + 16,
                this.chassis.leftPos() + 334, this.chassis.topPos() + 225);
        this.renderFallingCoins(graphics, t);
        graphics.disableScissor();
        int cx = this.chassis.contentX();
        int cy = this.chassis.contentY();
        int cw = this.chassis.contentW();
        int ch = this.chassis.contentH();
        float pulse = (float) (0.5 + 0.5 * Math.sin(t / 450.0));
        int titleColor = lerpColor(0xFFD4AF37, 0xFFFFF2B8, pulse);
        String title = "世界金融中心";
        float scale = 1.7F + 0.10F * pulse;
        float tilt = (float) Math.sin(t / 700.0) * 4.0F;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(cx + cw / 2.0F, cy + 12, 0.0F);
        pose.mulPose(Axis.ZP.rotationDegrees(tilt));
        pose.scale(scale, scale, 1.0F);
        graphics.drawString(this.font, title, -this.font.width(title) / 2, -5, titleColor);
        pose.popPose();
        MarketSnapshot snapshot = this.menu.snapshot();
        // 独立「使用说明」按钮（界面左上角），余额排在按钮下方。
        this.renderBoardButton(graphics, "使用说明", this.helpButton, mouseX, mouseY);
        String info = "钱包 $" + snapshot.balance();
        graphics.drawString(this.font, info, cx + 2, chassis.topPos() + 40, 0xFFFFD86B);
        if (this.helpButton.contains(mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(Component.literal("使用说明与交易帮助")), mouseX, mouseY);
        }
        this.renderHubButton(graphics, "股票", this.stockButton, mouseX, mouseY);
        this.renderHubButton(graphics, "期货", this.futuresButton, mouseX, mouseY);
        this.renderHubButton(graphics, "现货交易", this.spotButton, mouseX, mouseY);
        String status = snapshot.status();
        if (status != null && !status.isEmpty()) {
            graphics.drawString(this.font, trim(status, cw - 4), cx + 2, cy + ch - 52, 0xFFFFB4B4);
        }
    }

    private void renderFallingCoins(GuiGraphics graphics, long t) {
        int cx = this.chassis.contentX();
        int cy = this.chassis.contentY();
        int cw = this.chassis.contentW();
        int ch = this.chassis.contentH();
        double fallHeight = ch + 20;
        for (int i = 0; i < 8; i++) {
            java.util.Random seed = new java.util.Random(i * 2654435761L);
            double phase = seed.nextDouble() * fallHeight;
            double position = (t / 22.0 + phase) % fallHeight;
            long cycle = (long) ((t / 22.0 + phase) / fallHeight);
            java.util.Random random = new java.util.Random((cycle * 31L + i) * 2654435761L);
            int x = cx + 4 + random.nextInt(cw - 24);
            int y = cy + (int) position;
            // 金币生成时淡入（前 24px 内透明度渐增）。
            double fadeIn = Math.min(1.0, (y - cy) / 24.0);
            // 金币底缘距底部黑边 24px 即开始淡出，触碰到黑线时完全消失。
            int edge = this.chassis.topPos() + 225;
            double fade = 1.0;
            int bottom = y + 6;
            if (bottom > edge - 24) {
                fade = Math.max(0.0, (edge - bottom) / 24.0);
            }
            drawCoin(graphics, x, y, (int) Math.round((90 + (i * 17) % 90) * fadeIn * fade));
        }
    }

    private static void drawCoin(GuiGraphics graphics, int x, int y, int alpha) {
        drawCircle(graphics, x + 6, y + 6, 6, (alpha << 24) | 0xB8860B);
        drawCircle(graphics, x + 6, y + 6, 5, (alpha << 24) | 0xF2A900);
        drawCircle(graphics, x + 5, y + 5, 2, ((alpha + 25) << 24) | 0xFFD86B);
    }

    private static void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            graphics.fill(centerX - dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color);
        }
    }

    private void renderHubButton(GuiGraphics graphics, String label, Rectangle rect, int mouseX, int mouseY) {
        boolean hovered = rect.contains(mouseX, mouseY);
        graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, hovered ? 0xFF24445A : 0xFF132230);
        // 与交易界面按钮一致的 1px 边框。
        graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + 1, 0xFF3B526F);
        graphics.fill(rect.x, rect.y + rect.height - 1, rect.x + rect.width, rect.y + rect.height, 0xFF3B526F);
        graphics.fill(rect.x, rect.y, rect.x + 1, rect.y + rect.height, 0xFF3B526F);
        graphics.fill(rect.x + rect.width - 1, rect.y, rect.x + rect.width, rect.y + rect.height, 0xFF3B526F);
        graphics.drawString(this.font, label, rect.x + (rect.width - this.font.width(label)) / 2,
                rect.y + 7, 0xFFFFFFFF);
    }

    private void renderHelp(GuiGraphics graphics, int mouseX, int mouseY) {
        int cx = this.chassis.contentX();
        int cy = this.chassis.contentY();
        int cw = this.chassis.contentW();
        // 标题栏（金色配色，与交易板块同布局）。
        int hx1 = this.chassis.leftPos() + 20;
        int hx2 = this.chassis.leftPos() + 334;
        int hy0 = this.chassis.topPos() + 16;
        graphics.fill(hx1, hy0, hx2, hy0 + 15, 0xFF1A2634);
        graphics.fill(hx1, hy0, hx2, hy0 + 2, 0xFFD4AF37);
        this.renderBoardButton(graphics, "‹ 返回", this.backButton, mouseX, mouseY);
        graphics.drawString(this.font, "使用说明", cx + 48, hy0 + 5, 0xFFFFFFFF);

        // 条目文本：复用公共滚动文本控件（自动折行、右侧滑条、底部淡出）。
        this.helpText.render(graphics, this.font, cx + 2, cy + 32, cw - 2, 146, 18, 0xFF0C1445);
    }

    private void renderBoard(GuiGraphics graphics, int mouseX, int mouseY) {
        int cx = this.chassis.contentX();
        int cy = this.chassis.contentY();
        int cw = this.chassis.contentW();
        int ch = this.chassis.contentH();

        int titleColor = switch (this.page) {
            case STOCK -> 0xFFB03A3A;
            case FUTURES -> 0xFF3A6DB0;
            default -> 0xFF2E8B57;
        };
        // 标题栏背景板与配色线条：紧贴手机上边框，左端延到手机左边框、右端延到功能条蒙版；
        // 返回按钮、标题、时间与余额保持原横向位置不动。
        int hx1 = this.chassis.leftPos() + 20;
        int hx2 = this.chassis.leftPos() + 334;
        int hy0 = this.chassis.topPos() + 16;
        graphics.fill(hx1, hy0, hx2, hy0 + 15, 0xFF1A2634);
        graphics.fill(hx1, hy0, hx2, hy0 + 2, titleColor);
        this.renderBoardButton(graphics, "‹ 返回", this.backButton, mouseX, mouseY);
        String title = switch (this.page) {
            case STOCK -> "股票交易";
            case FUTURES -> "期货交易";
            default -> "现货交易";
        };
        graphics.drawString(this.font, title, cx + 48, hy0 + 5, 0xFFFFFFFF);
        MarketSnapshot snapshot = this.menu.snapshot();
        String info = "$" + snapshot.balance() + " " + timeShort(snapshot.dayTime());
        graphics.drawString(this.font, info, cx + cw - this.font.width(info), hy0 + 5, 0xFFD8E0EA);

        boolean spot = this.page == Page.SPOT;
        int listW = spot ? 150 : 100;
        // 左侧搜索列表背景蒙版：与右侧功能条一致的底色，宽度覆盖最近点击（8×15px）与物品列表。
        int panelRight = cx + Math.max(listW, RECENT_LIMIT * 15) + 4;
        graphics.fill(cx - 4, cy + 14, panelRight, cy + 146, 0xFF13193D);

        String recentHover = this.renderRecent(graphics, mouseX, mouseY);
        this.searchBox.render(graphics, mouseX, mouseY, 0.0F);

        int rows = 8;
        int rowH = 12;
        int listY = cy + 46;
        String hoveredAsset = null;
        for (int i = this.scrollOffset; i < Math.min(this.matches.size(), this.scrollOffset + rows); i++) {
            String asset = this.matches.get(i);
            int y = listY + (i - this.scrollOffset) * rowH;
            Rectangle rect = new Rectangle(cx, y, listW, rowH);
            if (rect.contains(mouseX, mouseY)) {
                hoveredAsset = asset;
            }
            if (i == this.selected) {
                graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, 0xFF1E3A50);
            }
            ItemStack icon = new ItemStack(ItemIndex.item(asset));
            if (!icon.isEmpty()) {
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(rect.x + 1, rect.y + 1, 0.0F);
                pose.scale(0.7F, 0.7F, 1.0F);
                graphics.renderItem(icon, 0, 0);
                pose.popPose();
            }
            AssetQuote quote = this.quote(asset);
            graphics.drawString(this.font, trim(rowLabel(asset, quote), listW - 16), rect.x + 12, rect.y + 2,
                    quote == null ? 0xFF8FA3BF : 0xFFE5E9EE);
        }

        if (spot) {
            this.renderSpotSlots(graphics, mouseX, mouseY);
        } else {
            this.renderChart(graphics, cx + 125, cy + 15, cw - 125, 110, mouseX, mouseY);
        }

        String asset = selectedAsset();
        AssetQuote quote = asset.isEmpty() ? null : this.quote(asset);
        // 持仓/价格信息右移到列表蒙版之外，避免压住蒙版。
        int infoX = spot ? cx + 158 : cx + 130;
        int infoY = cy + (spot ? 135 : 127);
        if (quote != null) {
            String name = ItemIndex.displayName(asset);
            graphics.drawString(this.font, trim(name, 70), infoX, infoY, 0xFFFFFFFF);
            String price = "$" + Math.round(quote.price) + String.format(Locale.ROOT, " %+.1f%%", quote.change * 100.0);
            graphics.drawString(this.font, price, infoX + 74, infoY, quote.change >= 0 ? 0xFFFF6B6B : 0xFF5CDA8A);
            String holding = this.page == Page.STOCK ? "持仓 " + quote.stockTotal
                    : this.page == Page.FUTURES ? "净持仓 " + quote.futureQty : "拥有 " + ownedSpot(asset);
            graphics.drawString(this.font, holding, infoX, infoY + 12, 0xFFB8C7DF);
        }

        this.renderControls(graphics, mouseX, mouseY);
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            this.renderSlotTooltip(graphics, this.hoveredSlot, mouseX, mouseY);
        } else if (hoveredAsset != null) {
            graphics.renderComponentTooltip(this.font, this.itemTooltip(hoveredAsset), mouseX, mouseY);
        } else if (recentHover != null) {
            graphics.renderComponentTooltip(this.font, this.itemTooltip(recentHover), mouseX, mouseY);
        }
    }

    private String renderRecent(GuiGraphics graphics, int mouseX, int mouseY) {
        int cx = this.chassis.contentX();
        int cy = this.chassis.contentY();
        String hovered = null;
        for (int i = 0; i < RECENT.size() && i < RECENT_LIMIT; i++) {
            String asset = RECENT.get(i);
            int x = cx + i * 15;
            int y = cy + 16;
            ItemStack stack = new ItemStack(ItemIndex.item(asset));
            if (!stack.isEmpty()) {
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(x + 1, y + 1, 0.0F);
                pose.scale(0.65F, 0.65F, 1.0F);
                graphics.renderItem(stack, 0, 0);
                pose.popPose();
            }
            if (mouseX >= x && mouseX < x + 15 && mouseY >= y && mouseY < y + 12) {
                hovered = asset;
            }
        }
        return hovered;
    }

    private void renderSpotSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        this.hoveredSlot = null;
        // 存储与背包均为纯格子（1px 描边），无外框包边（参考桌面终端样式），两区以间隙分隔。
        for (Slot slot : this.menu.slots) {
            int sx = this.chassis.leftPos() + slot.x;
            int sy = this.chassis.topPos() + slot.y;
            boolean hovered = mouseX >= sx && mouseX < sx + SPOT_CELL && mouseY >= sy && mouseY < sy + SPOT_CELL;
            graphics.fill(sx, sy, sx + SPOT_CELL, sy + SPOT_CELL, 0xFF0E1720);
            graphics.fill(sx, sy, sx + SPOT_CELL, sy + 1, 0xFF3B526F);
            graphics.fill(sx, sy + SPOT_CELL - 1, sx + SPOT_CELL, sy + SPOT_CELL, 0xFF3B526F);
            graphics.fill(sx, sy, sx + 1, sy + SPOT_CELL, 0xFF3B526F);
            graphics.fill(sx + SPOT_CELL - 1, sy, sx + SPOT_CELL, sy + SPOT_CELL, 0xFF3B526F);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                PoseStack pose = graphics.pose();
                pose.pushPose();
                pose.translate(sx + 2, sy + 2, 0.0F);
                pose.scale(0.75F, 0.75F, 1.0F);
                graphics.renderItem(stack, 0, 0);
                pose.popPose();
            }
            if (hovered) {
                this.hoveredSlot = slot;
                graphics.fill(sx, sy, sx + SPOT_CELL, sy + SPOT_CELL, 0x33FFFFFF);
            }
        }
    }

    private Slot slotAt(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            int sx = this.chassis.leftPos() + slot.x;
            int sy = this.chassis.topPos() + slot.y;
            if (mouseX >= sx && mouseX < sx + SPOT_CELL && mouseY >= sy && mouseY < sy + SPOT_CELL) {
                return slot;
            }
        }
        return null;
    }

    private void renderSlotTooltip(GuiGraphics graphics, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lines.addAll(stack.getTooltipLines(mc.player, TooltipFlag.Default.NORMAL));
        } else {
            lines.add(stack.getHoverName());
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        lines.add(1, Component.literal("稀有度: " + ItemIndex.rarityTier(id)).withStyle(ItemIndex.rarityColor(id)));
        lines.addAll(AbstractTradeScreen.quoteLines(id, false, false));
        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private void renderChart(GuiGraphics graphics, int x, int y, int w, int h, int mouseX, int mouseY) {
        String asset = selectedAsset();
        AssetQuote quote = asset.isEmpty() ? null : this.quote(asset);
        graphics.fill(x, y, x + w + 5, y + h, 0xFF0D1524);
        if (quote == null || quote.history == null || quote.history.length < 2) {
            graphics.drawString(this.font, "行情加载中…", x + 3, y + h / 2 - 4, 0xFF8FA3BF);
            return;
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double value : quote.history) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        if (max - min < 1e-9) {
            max += 1.0;
            min -= 1.0;
        }
        // 红绿柱整体左移：占满背景板内宽（两端留 4px），使「当天」柱完整落在板内。
        double column = (double) (w - 8) / (quote.history.length - 1);
        for (int i = 0; i < quote.history.length; i++) {
            int bx = x + 4 + (int) Math.round(i * column);
            int by = y + h - 1 - (int) Math.round((quote.history[i] - min) * (h - 3) / (max - min));
            if (i > 0) {
                int prevY = y + h - 1 - (int) Math.round((quote.history[i - 1] - min) * (h - 3) / (max - min));
                boolean up = quote.history[i] >= quote.history[i - 1];
                int top = Math.min(prevY, by);
                int bottom = Math.max(prevY, by);
                graphics.fill(bx - 4, top, bx + 4, bottom + 1, up ? 0xFFE74C3C : 0xFF2ECC71);
                if (Math.abs(mouseX - bx) <= 4 && mouseY >= top - 3 && mouseY <= bottom + 3) {
                    int daysAgo = quote.history.length - 1 - i;
                    String date = daysAgo == 0 ? "今天" : "前" + daysAgo + "天";
                    double change = quote.history[i - 1] <= 0 ? 0.0
                            : (quote.history[i] - quote.history[i - 1]) / quote.history[i - 1] * 100.0;
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.literal("近 15 日 · " + date).withStyle(net.minecraft.ChatFormatting.GOLD));
                    lines.add(Component.literal("价格 $" + Math.round(quote.history[i])).withStyle(net.minecraft.ChatFormatting.YELLOW));
                    lines.add(Component.literal(String.format(Locale.ROOT, "较前日 %+.1f%%", change))
                            .withStyle(change >= 0 ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN));
                    graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                }
                // 左侧基准点（i=0）不显示；其余天数保留点位标记。
                graphics.fill(bx - 1, by - 1, bx + 1, by + 1,
                        i == quote.history.length - 1 ? 0xFF3BD18B : 0xFFCBD5E1);
            }
        }
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY) {
        int cx = this.chassis.contentX();
        int cy = this.chassis.contentY();
        int ch = this.chassis.contentH();
        int y1 = cy + ch - 31;
        int y2 = cy + ch - 8;
        this.amountBox.render(graphics, mouseX, mouseY, 0.0F);
        int x = cx + 50;
        for (int i = 0; i < AMOUNT_LABELS.length; i++) {
            int w = AMOUNT_WS[i];
            this.renderSmall(graphics, AMOUNT_LABELS[i], x, y1, w, CONTROL_H, mouseX, mouseY);
            x += w + AMOUNT_GAP;
        }
        int buyX = cx + this.chassis.contentW() - 126;
        this.renderSmall(graphics, "买入", buyX, y2, 60, CONTROL_H, mouseX, mouseY);
        this.renderSmall(graphics, "卖出", buyX + 66, y2, 60, CONTROL_H, mouseX, mouseY);
    }

    private void renderSmall(GuiGraphics graphics, String label, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        graphics.fill(x, y, x + w, y + h, hovered ? 0xFF2A4358 : 0xFF16212D);
        graphics.fill(x, y, x + w, y + 1, 0xFF3B526F);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF3B526F);
        graphics.fill(x, y, x + 1, y + h, 0xFF3B526F);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF3B526F);
        graphics.drawString(this.font, label, x + (w - this.font.width(label)) / 2, y + (h - 8) / 2, 0xFFE5E9EE);
    }

    private void renderBoardButton(GuiGraphics graphics, String label, Rectangle rect, int mouseX, int mouseY) {
        boolean hovered = rect.contains(mouseX, mouseY);
        graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, hovered ? 0xFF2A4358 : 0xFF16212D);
        // 与交易界面按钮一致的 1px 边框。
        graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + 1, 0xFF3B526F);
        graphics.fill(rect.x, rect.y + rect.height - 1, rect.x + rect.width, rect.y + rect.height, 0xFF3B526F);
        graphics.fill(rect.x, rect.y, rect.x + 1, rect.y + rect.height, 0xFF3B526F);
        graphics.fill(rect.x + rect.width - 1, rect.y, rect.x + rect.width, rect.y + rect.height, 0xFF3B526F);
        graphics.drawString(this.font, label, rect.x + 5, rect.y + 3, 0xFFE5E9EE);
    }

    private AssetQuote quote(String asset) {
        return this.menu.quote(asset, this.page == Page.FUTURES);
    }

    private String selectedAsset() {
        return this.selected >= 0 && this.selected < this.matches.size() ? this.matches.get(this.selected) : "";
    }

    private String rowLabel(String asset, AssetQuote quote) {
        String name = ItemIndex.displayName(asset);
        return quote == null ? name
                : name + " $" + Math.round(quote.price) + String.format(Locale.ROOT, " %+.1f%%", quote.change * 100.0);
    }

    private void rebuildMatches() {
        this.matches.clear();
        // 统一搜索实现：中文名 + 拼音全拼 + 拼音首字母（不再匹配英文物品 ID）
        this.matches.addAll(ItemIndex.search(this.searchText, MAX_RESULTS));
        if (this.selected >= this.matches.size()) {
            this.selected = 0;
        }
    }

    private void requestQuote(String asset) {
        if (asset != null && !asset.isEmpty()) {
            Network.sendToServer(new MarketActionPacket(
                    this.page == Page.FUTURES ? MarketActionPacket.REQUEST_FUTURES_QUOTE : MarketActionPacket.REQUEST_QUOTE,
                    asset, 0));
        }
    }

    private void ensureQuotes() {
        MarketSnapshot snapshot = this.menu.snapshot();
        long day = snapshot.day();
        if (day != this.lastSyncedDay) {
            this.lastSyncedDay = day;
            for (String id : this.matches) {
                this.requestQuote(id);
            }
        }
        String asset = selectedAsset();
        if (!asset.isEmpty() && (!asset.equals(this.lastRequestedAsset) || day != this.lastRequestedDay)) {
            this.lastRequestedAsset = asset;
            this.lastRequestedDay = day;
            this.requestQuote(asset);
        }
    }

    private long ownedSpot(String asset) {
        Item item = ItemIndex.item(asset);
        if (item == null) {
            return 0L;
        }
        long count = 0L;
        for (int i = 0; i < Math.min(36, this.menu.playerInventory.getContainerSize()); i++) {
            ItemStack stack = this.menu.playerInventory.getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void changeAmount(long delta) {
        this.amount = Math.max(0L, Math.min(MAX_AMOUNT, this.amount + delta));
        this.amountBox.setValue(Long.toString(this.amount));
    }

    private void maxAmount() {
        Item item = ItemIndex.item(selectedAsset());
        this.amount = item == null ? 64L : Math.max(1L, item.getMaxStackSize());
        this.amountBox.setValue(Long.toString(this.amount));
    }

    private void ownedAmount() {
        String asset = selectedAsset();
        if (asset.isEmpty() && !this.matches.isEmpty()) {
            this.selected = 0;
            asset = this.matches.get(0);
            this.requestQuote(asset);
        }
        if (this.page == Page.SPOT) {
            this.amount = Math.min(MAX_AMOUNT, Math.max(1L, ownedSpot(asset)));
        } else {
            AssetQuote quote = asset.isEmpty() ? null : this.quote(asset);
            long owned = 1L;
            if (quote != null) {
                owned = this.page == Page.STOCK ? quote.stockTotal : Math.abs(quote.futureQty);
            }
            this.amount = Math.min(MAX_AMOUNT, Math.max(1L, owned));
        }
        this.amountBox.setValue(Long.toString(this.amount));
    }

    private void trade(boolean buy) {
        String asset = selectedAsset();
        if (asset.isEmpty()) {
            return;
        }
        byte action = switch (this.page) {
            case STOCK -> buy ? MarketActionPacket.BUY_STOCK : MarketActionPacket.SELL_STOCK;
            case FUTURES -> buy ? MarketActionPacket.BUY_FUTURE : MarketActionPacket.SELL_FUTURE;
            default -> buy ? MarketActionPacket.BUY_SPOT : MarketActionPacket.SELL_SPOT;
        };
        Network.sendToServer(new MarketActionPacket(action, asset, this.amount));
    }

    private void goTo(Page target) {
        this.page = target;
        this.searchText = "";
        this.scrollOffset = 0;
        this.selected = 0;
        this.amount = 1L;
        if (this.amountBox != null) {
            this.amountBox.setValue("1");
        }
        if (this.searchBox != null) {
            this.searchBox.setValue("");
        }
        this.rebuildMatches();
        if (!this.matches.isEmpty()) {
            this.requestQuote(this.matches.get(0));
        }
    }

    private List<Component> itemTooltip(String asset) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(ItemIndex.displayName(asset)));
        lines.add(Component.literal("稀有度: " + ItemIndex.rarityTier(asset)).withStyle(ItemIndex.rarityColor(asset)));
        lines.add(Component.literal("ID: " + asset).withStyle(net.minecraft.ChatFormatting.GRAY));
        AssetQuote quote = quote(asset);
        if (quote == null) {
            lines.add(Component.literal("行情加载中…").withStyle(net.minecraft.ChatFormatting.GRAY));
        } else {
            lines.add(Component.literal("现价 $" + Math.round(quote.price)).withStyle(net.minecraft.ChatFormatting.YELLOW));
            lines.add(Component.literal(String.format(Locale.ROOT, "涨跌 %+.1f%%", quote.change * 100.0))
                    .withStyle(quote.change >= 0 ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.GREEN));
            if (this.page == Page.STOCK) {
                lines.add(Component.literal("股票持仓 " + quote.stockTotal).withStyle(net.minecraft.ChatFormatting.AQUA));
            } else if (this.page == Page.FUTURES) {
                lines.add(Component.literal("期货净持仓 " + quote.futureQty).withStyle(net.minecraft.ChatFormatting.AQUA));
            }
        }
        return lines;
    }

    private static String timeShort(long dayTime) {
        long ticks = ((dayTime % 24000L) + 24000L) % 24000L;
        int hour = (int) ((ticks / 1000L + 6L) % 24L);
        int minute = (int) ((ticks % 1000L) * 60L / 1000L);
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private static String trim(String text, int maxWidth) {
        if (text.length() <= maxWidth / 6 + 2) {
            return text;
        }
        return text.substring(0, Math.max(1, maxWidth / 6)) + "…";
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || this.chassis.isBusy()) {
            return false;
        }
        int x = (int) mouseX;
        int y = (int) mouseY;
        if (this.chassis.navBack().contains(x, y)) {
            this.backOneLevel();
            return true;
        }
        if (this.chassis.navHome().contains(x, y)) {
            this.returnToMcphoneDesktop();
            return true;
        }
        if (this.chassis.navTasks().contains(x, y)) {
            return true;
        }
        if (this.page == Page.HUB) {
            if (this.helpButton.contains(x, y)) {
                this.goTo(Page.HELP);
                return true;
            }
            if (this.stockButton.contains(x, y)) {
                this.goTo(Page.STOCK);
                return true;
            }
            if (this.futuresButton.contains(x, y)) {
                this.goTo(Page.FUTURES);
                return true;
            }
            if (this.spotButton.contains(x, y)) {
                this.goTo(Page.SPOT);
                return true;
            }
            return false;
        }
        if (this.page == Page.HELP) {
            if (this.backButton.contains(x, y)) {
                this.goTo(Page.HUB);
                return true;
            }
            return this.helpText.mouseClicked(mouseX, mouseY);
        }
        if (this.backButton.contains(x, y)) {
            this.goTo(Page.HUB);
            return true;
        }
        if (this.searchBox.mouseClicked(mouseX, mouseY, button) || this.amountBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int ix = this.chassis.contentX();
        int iy = this.chassis.contentY();
        for (int i = 0; i < RECENT.size() && i < RECENT_LIMIT; i++) {
            Rectangle recent = new Rectangle(ix + i * 15, iy + 16, 15, 12);
            if (recent.contains(x, y)) {
                String asset = RECENT.get(i);
                this.searchText = "";
                this.searchBox.setValue("");
                this.rebuildMatches();
                int index = this.matches.indexOf(asset);
                if (index >= 0) {
                    this.selected = index;
                }
                this.requestQuote(asset);
                return true;
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (this.page == Page.SPOT && mc != null && mc.gameMode != null && mc.player != null) {
            Slot slot = this.slotAt(x, y);
            if (slot != null) {
                mc.gameMode.handleInventoryMouseClick(this.menu.containerId, slot.index, button,
                        net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
                return true;
            }
        }
        int rows = 8;
        int rowH = 12;
        int listW = this.page == Page.SPOT ? 150 : 100;
        int listY = iy + 46;
        for (int i = this.scrollOffset; i < Math.min(this.matches.size(), this.scrollOffset + rows); i++) {
            Rectangle rect = new Rectangle(ix, listY + (i - this.scrollOffset) * rowH, listW, rowH);
            if (rect.contains(x, y)) {
                this.selected = i;
                addRecent(this.matches.get(i));
                this.requestQuote(this.matches.get(i));
                return true;
            }
        }

        int ch = this.chassis.contentH();
        int y1 = iy + ch - 31;
        int y2 = iy + ch - 8;
        int bx = ix + 50;
        if (hit(bx, y1, AMOUNT_WS[0], CONTROL_H, x, y)) { this.changeAmount(-100); return true; }
        bx += AMOUNT_WS[0] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[1], CONTROL_H, x, y)) { this.changeAmount(-10); return true; }
        bx += AMOUNT_WS[1] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[2], CONTROL_H, x, y)) { this.changeAmount(-1); return true; }
        bx += AMOUNT_WS[2] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[3], CONTROL_H, x, y)) { this.changeAmount(1); return true; }
        bx += AMOUNT_WS[3] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[4], CONTROL_H, x, y)) { this.changeAmount(10); return true; }
        bx += AMOUNT_WS[4] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[5], CONTROL_H, x, y)) { this.changeAmount(100); return true; }
        bx += AMOUNT_WS[5] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[6], CONTROL_H, x, y)) { this.maxAmount(); return true; }
        bx += AMOUNT_WS[6] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[7], CONTROL_H, x, y)) { this.amount = 1L; this.amountBox.setValue("1"); return true; }
        bx += AMOUNT_WS[7] + AMOUNT_GAP;
        if (hit(bx, y1, AMOUNT_WS[8], CONTROL_H, x, y)) { this.ownedAmount(); return true; }
        int buyX = ix + this.chassis.contentW() - 126;
        if (hit(buyX, y2, 60, CONTROL_H, x, y)) { this.trade(true); return true; }
        if (hit(buyX + 66, y2, 60, CONTROL_H, x, y)) { this.trade(false); return true; }
        return false;
    }

    private void backOneLevel() {
        if (this.page != Page.HUB) {
            this.goTo(Page.HUB);
        } else {
            this.returnToMcphoneDesktop();
        }
    }

    private void returnToMcphoneDesktop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        mc.player.closeContainer();
        PhoneScreenOpener.open(mc.player);
    }

    private static boolean hit(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void addRecent(String asset) {
        RECENT.remove(asset);
        RECENT.add(0, asset);
        while (RECENT.size() > RECENT_LIMIT) {
            RECENT.remove(RECENT.size() - 1);
        }
    }

    /** 本页是否有文本框正在编辑（供界面层做输入保护）。 */
    public boolean isEditingText() {
        if (this.page == Page.HUB || this.page == Page.HELP) {
            return false;
        }
        return this.searchBox.isFocused() || this.amountBox.isFocused();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.backOneLevel();
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (this.page == Page.SPOT && Screen.hasShiftDown() && this.hoveredSlot != null
                && this.hoveredSlot.hasItem() && mc != null && mc.gameMode != null && mc.player != null) {
            mc.gameMode.handleInventoryMouseClick(this.menu.containerId, this.hoveredSlot.index, 0,
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
            return true;
        }
        if (this.page != Page.HUB && this.page != Page.HELP
                && (this.searchBox.keyPressed(keyCode, scanCode, modifiers)
                || this.amountBox.keyPressed(keyCode, scanCode, modifiers))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.page != Page.HUB && this.page != Page.HELP
                && (this.searchBox.charTyped(codePoint, modifiers)
                || this.amountBox.charTyped(codePoint, modifiers))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.page == Page.HELP) {
            return this.helpText.mouseScrolled(delta);
        }
        if (this.page != Page.HUB && !this.matches.isEmpty()) {
            int maxOffset = Math.max(0, this.matches.size() - 8);
            if (delta < 0) {
                this.scrollOffset = Math.min(maxOffset, this.scrollOffset + 1);
            } else if (delta > 0) {
                this.scrollOffset = Math.max(0, this.scrollOffset - 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.page == Page.HELP && this.helpText.mouseDragged(mouseY)) {
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        this.helpText.mouseReleased();
    }
}
