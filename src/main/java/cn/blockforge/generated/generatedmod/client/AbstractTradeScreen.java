package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.api.client.TextInputGuard;
import cn.blockforge.generated.generatedmod.api.client.NoteText;
import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import cn.blockforge.generated.generatedmod.api.economy.Money;

public abstract class AbstractTradeScreen extends AbstractContainerScreen<MarketMenu> {
    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_HEIGHT = 20;
    private static final int MAX_RESULTS = 100;
    private static final long MAX_AMOUNT = 9_999_999L;
    private static final int CHART_X = 176;
    private static final int CHART_Y = 50;
    private static final int CHART_W = 168;
    private static final int CHART_H = 90;

    protected int selected;
    protected int scrollOffset;
    protected long amount = 1L;
    private final List<String> matches = new ArrayList<>();
    private final List<Button> rowButtons = new ArrayList<>();
    private final Map<Button, String> rowAssets = new HashMap<>();
    private EditBox searchBox;
    private EditBox amountBox;
    private String searchText = "";
    private String lastStatus = "";
    private int lastQuoteRevision = -1;
    private String lastRequestedAsset = "";
    private long lastRequestedDay = -1;
    private long lastSyncedDay = -1;
    private long lastSeenDayTime = -1;

    protected AbstractTradeScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 360;
        this.imageHeight = 250;
    }

    protected abstract void trade(boolean buy);

    protected abstract String holdingLabel(AssetQuote quote);

    protected abstract int sellable(AssetQuote quote);

    protected abstract long holdingsOf(AssetQuote quote);

    protected abstract boolean futuresBoard();

    protected int matchCount() {
        return this.matches.size();
    }

    protected String matchAsset(int index) {
        return index >= 0 && index < this.matches.size() ? this.matches.get(index) : "";
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 36, 150, 16, Component.literal("搜索物品/拼音"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.literal("支持中文/英文搜索…"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("买入"), b -> trade(true))
                .bounds(this.leftPos + 8, this.topPos + 178, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("卖出"), b -> trade(false))
                .bounds(this.leftPos + 112, this.topPos + 178, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("返回"), b -> back())
                .bounds(this.leftPos + 216, this.topPos + 178, 136, 20).build());

        this.amountBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 202, 64, 20, Component.literal("数量"));
        this.amountBox.setMaxLength(7);
        this.amountBox.setHint(Component.literal("数量"));
        this.amountBox.setValue("1");
        this.amountBox.setResponder(this::onAmountChanged);
        this.addRenderableWidget(this.amountBox);
        this.addStepButtons();
        this.rebuildRows();
    }

    private void addStepButtons() {
        int y = this.topPos + 202;
        this.addRenderableWidget(Button.builder(Component.literal("-100"), b -> changeAmount(-100))
                .bounds(this.leftPos + 76, y, 28, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> changeAmount(-10))
                .bounds(this.leftPos + 106, y, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> changeAmount(-1))
                .bounds(this.leftPos + 132, y, 22, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> changeAmount(1))
                .bounds(this.leftPos + 156, y, 22, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> changeAmount(10))
                .bounds(this.leftPos + 180, y, 24, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+100"), b -> changeAmount(100))
                .bounds(this.leftPos + 206, y, 28, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("最大"), b -> setMaxAmount())
                .bounds(this.leftPos + 236, y, 38, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("最小"), b -> setMinAmount())
                .bounds(this.leftPos + 276, y, 38, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("已拥有"), b -> setOwnedAmount())
                .bounds(this.leftPos + 316, y, 36, 20).build());
    }

    private void onAmountChanged(String text) {
        String value = text == null ? "" : text;
        String clean = value.replaceAll("[^0-9]", "");
        if (!clean.equals(value)) {
            this.amountBox.setValue(clean);
            return;
        }
        this.amount = clean.isEmpty() ? 0L : Math.min(MAX_AMOUNT, Long.parseLong(clean));
    }

    private void changeAmount(long delta) {
        this.amount = Math.max(0L, Math.min(MAX_AMOUNT, this.amount + delta));
        this.amountBox.setValue(Long.toString(this.amount));
    }

    private void setMaxAmount() {
        net.minecraft.world.item.Item item = ItemIndex.item(selectedAsset());
        this.amount = item == null ? 64L : Math.max(1L, item.getMaxStackSize());
        this.amountBox.setValue(Long.toString(this.amount));
    }

    private void setMinAmount() {
        this.amount = 1L;
        this.amountBox.setValue("1");
    }

    private void setOwnedAmount() {
        String asset = selectedAsset();
        if (asset.isEmpty() && !this.matches.isEmpty()) {
            this.selected = 0;
            asset = this.matches.get(0);
            this.requestQuote(asset);
        }
        AssetQuote quote = asset.isEmpty() ? null : this.menu.quote(asset, futuresBoard());
        this.amount = Math.min(MAX_AMOUNT, Math.max(1L, quote == null ? 1L : holdingsOf(quote)));
        this.amountBox.setValue(Long.toString(this.amount));
    }

    private AssetQuote selectedQuote() {
        String asset = selectedAsset();
        return asset.isEmpty() ? null : this.menu.quote(asset, futuresBoard());
    }

    private void back() {
        if (this.minecraft != null && this.minecraft.player != null) {
            if (this.minecraft.player.containerMenu == this.menu) {
                this.minecraft.setScreen(new MarketScreen(this.menu, this.menu.playerInventory, Component.literal("便捷交易平台")));
            } else {
                this.minecraft.setScreen(null);
            }
        }
    }

    @Override
    public void onClose() {
        back();
    }

    private void onSearchChanged(String text) {
        this.searchText = text == null ? "" : text.trim();
        this.scrollOffset = 0;
        this.selected = 0;
        this.rebuildRows();
    }

    private List<String> filteredAssets() {
        // 统一搜索实现：中文名 + 拼音全拼 + 拼音首字母（不再匹配英文物品 ID）
        return new ArrayList<>(ItemIndex.search(this.searchText, MAX_RESULTS));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 文本框编辑中：吞掉会误触发的游戏快捷键（E 关界面、Q 丢弃、F 副手、数字键交换等），
        // 字母/数字仍由 charTyped 正常写入
        if (TextInputGuard.consumeHotkeysWhileEditing(keyCode, scanCode, this.searchBox, this.amountBox)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    private void rebuildRows() {
        for (Button button : this.rowButtons) {
            this.removeWidget(button);
        }
        this.rowButtons.clear();
        this.rowAssets.clear();
        this.matches.clear();
        this.matches.addAll(filteredAssets());
        if (this.selected >= this.matches.size()) {
            this.selected = 0;
        }
        int maxOffset = Math.max(0, this.matches.size() - VISIBLE_ROWS);
        if (this.scrollOffset > maxOffset) {
            this.scrollOffset = maxOffset;
        }
        int y = this.topPos + 56;
        for (int i = this.scrollOffset; i < Math.min(this.matches.size(), this.scrollOffset + VISIBLE_ROWS); i++) {
            String asset = this.matches.get(i);
            Button button = Button.builder(Component.literal(rowLabel(asset)), b -> select(asset))
                    .bounds(this.leftPos + 8, y + (i - this.scrollOffset) * ROW_HEIGHT, 150, 18)
                    .build();
            this.rowButtons.add(button);
            this.rowAssets.put(button, asset);
            this.addRenderableWidget(button);
        }
    }

    private String rowLabel(String asset) {
        String name = ItemIndex.displayName(asset);
        AssetQuote quote = this.menu.quote(asset, futuresBoard());
        if (quote == null) {
            return name + "  $—";
        }
        String change = String.format(Locale.ROOT, "%+.1f%%", quote.change * 100.0);
        return name + "  " + Money.price(quote.price) + "  " + change;
    }

    private void select(String asset) {
        this.selected = this.matches.indexOf(asset);
        this.requestQuote(asset);
    }

    private String selectedAsset() {
        return this.selected >= 0 && this.selected < this.matches.size() ? this.matches.get(this.selected) : "";
    }

    private void requestQuote(String asset) {
        if (asset != null && !asset.isEmpty()) {
            Network.sendToServer(new MarketActionPacket(
                    futuresBoard() ? MarketActionPacket.REQUEST_FUTURES_QUOTE : MarketActionPacket.REQUEST_QUOTE,
                    asset, 0));
        }
    }

    private void ensureQuoteRequested() {
        String asset = selectedAsset();
        if (asset.isEmpty()) {
            return;
        }
        long day = this.menu.snapshot().day();
        if (!(this.lastRequestedAsset.equals(asset) && this.lastRequestedDay == day)) {
            this.lastRequestedAsset = asset;
            this.lastRequestedDay = day;
            this.requestQuote(asset);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xff101827);
        graphics.fill(this.leftPos + 6, this.topPos + 6, this.leftPos + this.imageWidth - 6, this.topPos + 30, 0xff1d2b45);
        graphics.fill(this.leftPos + 8, this.topPos + 54, this.leftPos + 158, this.topPos + 174, 0xff18243a);
        graphics.fill(this.leftPos + 168, this.topPos + 34, this.leftPos + this.imageWidth - 8, this.topPos + 172, 0xff18243a);
        this.drawChart(graphics);
        this.drawItemIcons(graphics);
    }

    private void drawChart(GuiGraphics graphics) {
        AssetQuote quote = selectedQuote();
        if (quote == null || quote.history == null || quote.history.length < 2) {
            return;
        }
        int cx = this.leftPos + CHART_X;
        int cy = this.topPos + CHART_Y;
        int cw = CHART_W;
        int ch = CHART_H;
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
        graphics.fill(cx, cy, cx + cw, cy + ch, 0xff0d1524);
        graphics.fill(cx, cy, cx + cw, cy + 1, 0xff3b526f);
        graphics.fill(cx, cy + ch - 1, cx + cw, cy + ch, 0xff3b526f);
        graphics.fill(cx, cy, cx + 1, cy + ch, 0xff3b526f);
        for (int g = 1; g <= 3; g++) {
            graphics.fill(cx + 1, cy + ch * g / 4, cx + cw, cy + ch * g / 4 + 1, 0xff1a2b44);
        }

        double column = (double) (cw - 2) / (quote.history.length - 1);
        for (int i = 0; i < quote.history.length; i++) {
            int x = cx + 1 + (int) Math.round(i * column);
            if (i > 0) {
                boolean up = quote.history[i] >= quote.history[i - 1];
                int y0 = cy + ch - 1 - (int) Math.round((quote.history[i - 1] - min) * (ch - 3) / (max - min));
                int y1 = cy + ch - 1 - (int) Math.round((quote.history[i] - min) * (ch - 3) / (max - min));
                int top = Math.min(y0, y1);
                int bottom = Math.max(y0, y1);
                int color = up ? 0xffe74c3c : 0xff2ecc71;
                graphics.fill(x - 3, top, x + 3, bottom + 1, color);
            }
            int y = cy + ch - 1 - (int) Math.round((quote.history[i] - min) * (ch - 3) / (max - min));
            graphics.fill(x - 1, y - 1, x + 1, y + 1,
                    i == quote.history.length - 1 ? 0xff3bd18b : 0xffcbd5e1);
        }
    }

    private void drawItemIcons(GuiGraphics graphics) {
        int y = this.topPos + 56;
        for (int i = this.scrollOffset; i < Math.min(this.matches.size(), this.scrollOffset + VISIBLE_ROWS); i++) {
            ItemStack stack = new ItemStack(ItemIndex.item(this.matches.get(i)));
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, this.leftPos + 11, y + (i - this.scrollOffset) * ROW_HEIGHT + 1);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 10, 10, 0xffffff);
        MarketSnapshot snapshot = this.menu.snapshot();
        String wallet = "钱包: " + Money.format(snapshot.balance());
        int walletX = this.imageWidth - 8 - this.font.width(wallet);
        String time = MarketUi.headerTime(snapshot.dayTime());
        graphics.drawString(this.font, time, walletX - 6 - this.font.width(time), 10, 0xff8fa3bf);
        graphics.drawString(this.font, wallet, walletX, 10, 0xff3bd18b);

        String asset = selectedAsset();
        if (!asset.isEmpty()) {
            graphics.drawString(this.font, "近 15 日走势", 176, 36, 0xff8fa3bf);
            AssetQuote quote = this.menu.quote(asset, futuresBoard());
            String name = ItemIndex.displayName(asset);
            if (quote == null) {
                graphics.drawString(this.font, name + "  行情加载中…", 176, 142, 0xffb8c7df);
            } else {
                graphics.drawString(this.font, name + "  现价 " + Money.price(quote.price), 176, 142, 0xffffffff);
                String change = String.format(Locale.ROOT, "涨跌 %+.1f%%", quote.change * 100.0);
                graphics.drawString(this.font, change, 176, 154, quote.change >= 0 ? 0xffff6b6b : 0xff5cda8a);
                NoteText.draw(graphics, this.font, this.holdingLabel(quote), 176, 166, 0xffb8c7df);
            }
        }

        String status = snapshot.status();
        if (status != null && !status.isEmpty()) {
            List<FormattedCharSequence> lines = this.font.split(Component.literal(status), this.imageWidth - 20);
            int y = this.imageHeight - 8 - (lines.size() - 1) * 10;
            for (FormattedCharSequence line : lines) {
                graphics.drawString(this.font, line, 10, y, 0xffffb4b4);
                y += 10;
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        String asset = selectedAsset();
        AssetQuote quote = asset.isEmpty() ? null : this.menu.quote(asset, futuresBoard());
        if (quote != null && quote.history != null && quote.history.length >= 2) {
            double column = (double) (CHART_W - 2) / (quote.history.length - 1);
            int index = (int) Math.round((mouseX - (this.leftPos + CHART_X + 1)) / column);
            index = Math.max(0, Math.min(quote.history.length - 1, index));
            int centerX = this.leftPos + CHART_X + 1 + (int) Math.round(index * column);
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
            boolean near;
            if (index > 0) {
                int y0 = this.topPos + CHART_Y + CHART_H - 1
                        - (int) Math.round((quote.history[index - 1] - min) * (CHART_H - 3) / (max - min));
                int y1 = this.topPos + CHART_Y + CHART_H - 1
                        - (int) Math.round((quote.history[index] - min) * (CHART_H - 3) / (max - min));
                int top = Math.min(y0, y1) - 3;
                int bottom = Math.max(y0, y1) + 3;
                near = Math.abs(mouseX - centerX) <= 5 && mouseY >= top && mouseY <= bottom;
            } else {
                int y = this.topPos + CHART_Y + CHART_H - 1
                        - (int) Math.round((quote.history[0] - min) * (CHART_H - 3) / (max - min));
                near = Math.abs(mouseX - centerX) <= 5 && Math.abs(mouseY - y) <= 5;
            }
            if (near) {
                int daysAgo = quote.history.length - 1 - index;
                String date = daysAgo == 0 ? "今天" : "前" + daysAgo + "天";
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal("近 15 日 · " + date).withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal("价格: " + Money.price(quote.history[index])).withStyle(ChatFormatting.YELLOW));
                if (index == 0) {
                    lines.add(Component.literal("基准日").withStyle(ChatFormatting.GRAY));
                } else {
                    double previous = quote.history[index - 1];
                    double change = previous <= 0 ? 0.0 : (quote.history[index] - previous) / previous * 100.0;
                    String text = String.format(Locale.ROOT, "较前日 %+.1f%%", change);
                    lines.add(Component.literal(text).withStyle(change >= 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
                }
                graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        for (Button button : this.rowButtons) {
            if (button.isHovered()) {
                String hoveredAsset = this.rowAssets.get(button);
                if (hoveredAsset != null) {
                    graphics.renderComponentTooltip(this.font, quoteLines(hoveredAsset, true, futuresBoard()), mouseX, mouseY);
                }
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    public static List<Component> quoteLines(String asset, boolean includeName, boolean futures) {
        List<Component> lines = new ArrayList<>();
        if (includeName) {
            lines.add(Component.literal(ItemIndex.displayName(asset)));
            lines.add(Component.literal("稀有度: " + ItemIndex.rarityTier(asset)).withStyle(ItemIndex.rarityColor(asset)));
        }
        lines.add(Component.literal("ID: " + asset).withStyle(ChatFormatting.GRAY));
        MarketMenu menu = null;
        if (net.minecraft.client.Minecraft.getInstance().player != null
                && net.minecraft.client.Minecraft.getInstance().player.containerMenu instanceof MarketMenu m) {
            menu = m;
        }
        AssetQuote quote = menu == null ? null : menu.quote(asset, futures);
        if (quote == null) {
            lines.add(Component.literal("行情加载中…").withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(Component.literal("当日行情").withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("现价: " + Money.price(quote.price)).withStyle(ChatFormatting.YELLOW));
        String change = String.format(Locale.ROOT, "涨跌: %+.1f%%", quote.change * 100.0);
        lines.add(Component.literal(change).withStyle(quote.change >= 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        lines.add(NoteText.styled("股票持仓: " + quote.stockTotal + "（今日锁定 " + quote.stockLocked + "）").withStyle(ChatFormatting.AQUA));
        if (quote.futureQty == 0) {
            lines.add(Component.literal("无期货持仓").withStyle(ChatFormatting.GRAY));
        } else {
            String direction = quote.futureQty > 0 ? "做多" : "做空";
            lines.add(Component.literal("期货: " + quote.futureQty + " " + direction
                    + "，开仓 " + Money.price(quote.futureEntry)
                    + "，保证金 " + Money.price(quote.futureMargin)
                    + "，到期 第 " + quote.futureExpiryDay + " 天").withStyle(ChatFormatting.AQUA));
        }
        return lines;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, this.matches.size() - VISIBLE_ROWS);
        if (maxOffset > 0 && mouseX >= this.leftPos + 8 && mouseX <= this.leftPos + 158
                && mouseY >= this.topPos + 54 && mouseY <= this.topPos + 174) {
            if (delta < 0) {
                this.scrollOffset = Math.min(maxOffset, this.scrollOffset + 1);
            } else if (delta > 0) {
                this.scrollOffset = Math.max(0, this.scrollOffset - 1);
            }
            this.rebuildRows();
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        MarketSnapshot snapshot = this.menu.snapshot();
        if (!snapshot.status().equals(this.lastStatus)) {
            this.lastStatus = snapshot.status();
            this.rebuildRows();
        }
        int quoteRevision = this.menu.quoteRevision();
        if (quoteRevision != this.lastQuoteRevision) {
            this.lastQuoteRevision = quoteRevision;
            this.rebuildRows();
        }
        this.ensureQuoteRequested();
        long day = snapshot.day();
        long dayTime = snapshot.dayTime();
        boolean timeJumped = this.lastSeenDayTime != -1 && Math.abs(dayTime - this.lastSeenDayTime) > 20;
        if (day != this.lastSyncedDay || timeJumped) {
            this.lastSyncedDay = day;
            for (String id : this.matches) {
                this.requestQuote(id);
            }
        }
        this.lastSeenDayTime = dayTime;
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
