package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.api.client.TextInputGuard;
import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import cn.blockforge.generated.generatedmod.api.economy.Money;

public abstract class AbstractSpotScreen extends AbstractContainerScreen<MarketMenu> {
    private static final int VISIBLE_ROWS = 6;
    private static final int ROW_HEIGHT = 20;
    private static final int MAX_RESULTS = 100;
    private static final long MAX_AMOUNT = 9_999_999L;

    private int selected;
    private int scrollOffset;
    private long amount = 1L;
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

    protected AbstractSpotScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 340;
        this.imageHeight = 258;
    }

    @Override
    public void onClose() {
        if (this.menu.mode == 2) {
            Network.sendToServer(new MarketActionPacket(MarketActionPacket.OPEN_HUB, "", 0));
        } else {
            super.onClose();
        }
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 36, 136, 16, Component.literal("搜索物品/拼音"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.literal("支持中文/英文搜索…"));
        this.searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.literal("现货买入"), b -> spot(true))
                .bounds(this.leftPos + 158, this.topPos + 220, 86, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("现货卖出"), b -> spot(false))
                .bounds(this.leftPos + 252, this.topPos + 220, 86, 20).build());

        this.amountBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 198, 56, 18, Component.literal("数量"));
        this.amountBox.setMaxLength(7);
        this.amountBox.setHint(Component.literal("数量"));
        this.amountBox.setValue("1");
        this.amountBox.setResponder(this::onAmountChanged);
        this.addRenderableWidget(this.amountBox);
        this.addStepButtons();
        this.rebuildRows();
        String last = MarketMenu.lastSpotAsset();
        int lastIndex = this.matches.indexOf(last);
        if (lastIndex >= 0) {
            this.selected = lastIndex;
        }
    }

    private void addStepButtons() {
        int y = this.topPos + 198;
        this.addRenderableWidget(Button.builder(Component.literal("-100"), b -> changeAmount(-100))
                .bounds(this.leftPos + 68, y, 26, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("-10"), b -> changeAmount(-10))
                .bounds(this.leftPos + 96, y, 24, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("-1"), b -> changeAmount(-1))
                .bounds(this.leftPos + 122, y, 22, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+1"), b -> changeAmount(1))
                .bounds(this.leftPos + 146, y, 22, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+10"), b -> changeAmount(10))
                .bounds(this.leftPos + 170, y, 24, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+100"), b -> changeAmount(100))
                .bounds(this.leftPos + 196, y, 26, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("最大"), b -> setMaxAmount())
                .bounds(this.leftPos + 224, y, 36, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("最小"), b -> setMinAmount())
                .bounds(this.leftPos + 262, y, 36, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("已拥有"), b -> setOwnedAmount())
                .bounds(this.leftPos + 300, y, 36, 18).build());
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
        Item item = ItemIndex.item(selectedAsset());
        this.amount = item == null ? 64L : Math.max(1L, item.getMaxStackSize());
        this.amountBox.setValue(Long.toString(this.amount));
    }

    private void setMinAmount() {
        this.amount = 1L;
        this.amountBox.setValue("1");
    }

    private void setOwnedAmount() {
        this.amount = Math.min(MAX_AMOUNT, Math.max(1L, ownedCount(selectedAsset())));
        this.amountBox.setValue(Long.toString(this.amount));
    }

    protected long ownedCount(String asset) {
        return storageCount(asset) + playerInventoryCount(asset);
    }

    private long playerInventoryCount(String asset) {
        if (asset.isEmpty()) {
            return 0L;
        }
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

    private long storageCount(String asset) {
        if (asset.isEmpty()) {
            return 0L;
        }
        Item item = ItemIndex.item(asset);
        if (item == null) {
            return 0L;
        }
        long count = 0L;
        for (int i = 0; i < this.menu.marketInventory().getContainerSize(); i++) {
            ItemStack stack = this.menu.marketInventory().getItem(i);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void spot(boolean buy) {
        String asset = selectedAsset();
        if (!asset.isEmpty()) {
            // 做市商界面（mode 6）走 BROKER_BUY / BROKER_SELL：按卖价 ask 买入、按买价 bid 卖出
            byte action;
            if (this.menu.mode == 6) {
                action = buy ? MarketActionPacket.BROKER_BUY : MarketActionPacket.BROKER_SELL;
            } else {
                action = buy ? MarketActionPacket.BUY_SPOT : MarketActionPacket.SELL_SPOT;
            }
            Network.sendToServer(new MarketActionPacket(action, asset, this.amount));
        }
    }

    private String selectedAsset() {
        return this.selected >= 0 && this.selected < this.matches.size() ? this.matches.get(this.selected) : "";
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
                    .bounds(this.leftPos + 8, y + (i - this.scrollOffset) * ROW_HEIGHT, 142, 18)
                    .build();
            this.rowButtons.add(button);
            this.rowAssets.put(button, asset);
            this.addRenderableWidget(button);
        }
    }

    private String rowLabel(String asset) {
        String name = ItemIndex.displayName(asset);
        AssetQuote quote = this.menu.quote(asset, false);
        if (quote == null) {
            return name + "  $—";
        }
        String change = String.format(Locale.ROOT, "%+.1f%%", quote.change * 100.0);
        if (this.menu.mode == 6) {
            // 做市商界面：行内显示「买价↓ / 卖价↑」（做市商收货价 / 出货价）
            cn.blockforge.generated.generatedmod.api.rarity.RarityTier tier =
                    cn.blockforge.generated.generatedmod.api.rarity.RaritySources.tier(asset);
            return name + "  ↓" + Money.pricePlain(cn.blockforge.generated.generatedmod.broker.BrokerQuotes.bid(quote.price, tier))
                    + " ↑" + Money.pricePlain(cn.blockforge.generated.generatedmod.broker.BrokerQuotes.ask(quote.price, tier));
        }
        return name + "  " + Money.price(quote.price) + "  " + change;
    }

    private void select(String asset) {
        this.selected = this.matches.indexOf(asset);
        MarketMenu.setLastSpotAsset(asset);
        this.requestQuote(asset);
    }

    private void requestQuote(String asset) {
        if (asset != null && !asset.isEmpty()) {
            Network.sendToServer(new MarketActionPacket(MarketActionPacket.REQUEST_QUOTE, asset, 0));
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
        graphics.fill(this.leftPos + 6, this.topPos + 4, this.leftPos + this.imageWidth - 6, this.topPos + 34, 0xff1d2b45);
        graphics.fill(this.leftPos + 6, this.topPos + 34, this.leftPos + 152, this.topPos + 178, 0xff18243a);
        graphics.fill(this.leftPos + 154, this.topPos + 34, this.leftPos + this.imageWidth - 6, this.topPos + 194, 0xff16213a);

        int y = this.topPos + 56;
        for (int i = this.scrollOffset; i < Math.min(this.matches.size(), this.scrollOffset + VISIBLE_ROWS); i++) {
            ItemStack stack = new ItemStack(ItemIndex.item(this.matches.get(i)));
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, this.leftPos + 11, y + (i - this.scrollOffset) * ROW_HEIGHT + 1);
            }
        }

        for (Slot slot : this.menu.slots) {
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy, 0xff3b526f);
            graphics.fill(sx - 1, sy + 16, sx + 17, sy + 17, 0xff3b526f);
            graphics.fill(sx - 1, sy, sx, sy + 16, 0xff3b526f);
            graphics.fill(sx + 16, sy, sx + 17, sy + 16, 0xff3b526f);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 10, 5, 0xffffffff);
        MarketSnapshot snapshot = this.menu.snapshot();
        String wallet = "钱包: " + Money.format(snapshot.balance());
        int walletX = this.imageWidth - 8 - this.font.width(wallet);
        String time = MarketUi.headerTime(snapshot.dayTime());
        graphics.drawString(this.font, time, walletX - 6 - this.font.width(time), 5, 0xff8fa3bf);
        graphics.drawString(this.font, wallet, walletX, 5, 0xff3bd18b);

        String asset = selectedAsset();
        AssetQuote quote = asset.isEmpty() ? null : this.menu.quote(asset, false);
        if (quote == null) {
            if (!asset.isEmpty()) {
                graphics.drawString(this.font, ItemIndex.displayName(asset), 10, 178, 0xffb8c7df);
                graphics.drawString(this.font, "行情加载中…", 10, 189, 0xff8fa3bf);
            }
        } else {
            graphics.drawString(this.font, ItemIndex.displayName(asset), 10, 178, 0xffffffff);
            if (this.menu.mode == 6) {
                // 做市商界面：双列报价 + 价差（不显示中间价；成交按 bid/ask，无额外手续费）
                cn.blockforge.generated.generatedmod.api.rarity.RarityTier tier =
                        cn.blockforge.generated.generatedmod.api.rarity.RaritySources.tier(asset);
                String bid = "买价 " + Money.price(cn.blockforge.generated.generatedmod.broker.BrokerQuotes.bid(quote.price, tier));
                String ask = "卖价 " + Money.price(cn.blockforge.generated.generatedmod.broker.BrokerQuotes.ask(quote.price, tier));
                graphics.drawString(this.font, bid, 10, 189, 0xff5cda8a);
                graphics.drawString(this.font, ask, 10 + this.font.width(bid) + 14, 189, 0xffff6b6b);
                graphics.drawString(this.font, "价差 " + cn.blockforge.generated.generatedmod.broker.BrokerQuotes.spreadText(tier)
                        + "  （" + tier.displayName() + "）", 10, 200, 0xffe0c07a);
            } else {
                String price = "现价 " + Money.price(quote.price)
                        + String.format(Locale.ROOT, "  (%+.1f%%)", quote.change * 100.0);
                graphics.drawString(this.font, price, 10, 189, quote.change >= 0 ? 0xffff6b6b : 0xff5cda8a);
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
        Slot slot = this.hoveredSlot;
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            List<Component> lines = new ArrayList<>();
            if (this.minecraft.player != null) {
                lines.addAll(stack.getTooltipLines(this.minecraft.player, TooltipFlag.Default.NORMAL));
            } else {
                lines.add(stack.getHoverName());
            }
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            lines.add(1, Component.literal("稀有度: " + ItemIndex.rarityTier(id)).withStyle(ItemIndex.rarityColor(id)));
            lines.addAll(AbstractTradeScreen.quoteLines(id, false, false));
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        for (Button button : this.rowButtons) {
            if (button.isHovered()) {
                String asset = this.rowAssets.get(button);
                if (asset != null) {
                    graphics.renderComponentTooltip(this.font, AbstractTradeScreen.quoteLines(asset, true, false), mouseX, mouseY);
                }
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, this.matches.size() - VISIBLE_ROWS);
        if (maxOffset > 0 && mouseX >= this.leftPos + 6 && mouseX <= this.leftPos + 152
                && mouseY >= this.topPos + 34 && mouseY <= this.topPos + 178) {
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
