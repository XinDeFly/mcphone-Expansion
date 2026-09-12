package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.api.client.NoteText;
import cn.blockforge.generated.generatedmod.api.client.ScaledDeviceScreen;
import cn.blockforge.generated.generatedmod.api.client.ScrollViewport;
import cn.blockforge.generated.generatedmod.api.client.TextInputGuard;
import cn.blockforge.generated.generatedmod.api.client.UiDraw;
import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import cn.blockforge.generated.generatedmod.menu.MarketMenu;
import cn.blockforge.generated.generatedmod.network.MarketActionPacket;
import cn.blockforge.generated.generatedmod.network.Network;
import cn.blockforge.generated.generatedmod.network.TowerActionPacket;
import cn.blockforge.generated.generatedmod.network.TowerSyncPacket;
import cn.blockforge.generated.mod3ce985ee.ComputerBlock;
import cn.blockforge.generated.mod3ce985ee.ComputerTowerBlockEntity;
import cn.blockforge.generated.mod3ce985ee.client.ComputerLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 显示器控制台（桌面式）：全部内容绘制在显示器外壳的屏幕内
 * （外壳几何与开机动画界面一致）。
 * 左侧为与现货交易相同的界面，中列为机箱存储 + 玩家背包格，
 * 右侧为机箱控制面板（机箱选择、角色/红石/触发时机/规则设置，全部选项按钮带 1px 描边）。
 * 内容按基准尺寸绘制，由 {@link ScaledDeviceScreen} 统一缩放——
 * 随「视频设置 → 界面尺寸」与外框一同缩放（含槽位、物品图标与输入框控件）。
 */
public final class MonitorScreen extends ScaledDeviceScreen<MarketMenu> {

    /** 客户端待应用的机箱同步缓存（同步包可能早于界面到达）。 */
    public static TowerSyncPacket pending;

    /** 内容基准尺寸：与设备几何同源（见 {@link ComputerLayout}），不另行硬编码。 */
    private static final int CONTENT_W = ComputerLayout.MONITOR_CONTENT_W;
    private static final int CONTENT_H = ComputerLayout.MONITOR_CONTENT_H;
    private static final int LIST_X = 8;
    private static final int LIST_W = 182;
    private static final int VISIBLE_ROWS = 8;
    private static final int ROW_H = 16;
    private static final long MAX_AMOUNT = 9_999_999L;

    /** 机箱方向选择九宫格：方向顺序与《通用机械》一致（上/左/前/右/后/下），单元格为 [列, 行]。 */
    private static final String[] TOWER_DIRS = {"上", "左", "前", "右", "后", "下"};
    private static final int[][] TOWER_GRID_CELLS = {{1, 0}, {0, 1}, {1, 1}, {2, 1}, {0, 2}, {1, 2}};
    private static final int TOWER_BTN_W = 26;
    private static final int TOWER_BTN_H = 13;
    private static final int TOWER_PITCH_X = 30;
    private static final int TOWER_PITCH_Y = 15;
    private static final int TOWER_GRID_X = 400;
    private static final int TOWER_GRID_Y = 44;
    /** 机箱存储滚动视口（几何常量与 MarketMenu 共用，平滑上下滑动）。 */
    private final ScrollViewport towerView = new ScrollViewport(MarketMenu.TOWER_VIEW_X, MarketMenu.TOWER_VIEW_Y,
            MarketMenu.TOWER_VIEW_W, MarketMenu.TOWER_VIEW_H, MarketMenu.TOWER_VIEW_ROWS,
            MarketMenu.TOWER_VIEW_PITCH, ScrollViewport.DEFAULT_SLIDE_MS);

    private EditBox searchBox;
    private EditBox amountBox;
    private EditBox thresholdBox;
    private EditBox ruleAmountBox;
    private final List<String> matches = new ArrayList<>();
    private String searchText = "";
    private int selected;
    private int scrollOffset;
    private long amount = 1L;

    private TowerSyncPacket sync;
    private int selectedTower = -1;
    /** 已选机箱的位置（用于在同步包到达后稳定恢复选择，不受列表顺序/瞬时同步影响）。 */
    private long selectedTowerPos = Long.MIN_VALUE;
    private int editDirection = 1;   // 0=购入 1=出售
    private int editTrigger;         // 0=更新后 1=达价 2=红石

    /** 机箱存储视口滚动动画状态（由 ScrollViewport 内部管理）。 */
    private boolean draggingScrollbar;
    /** 规则操作反馈文字与时间（保存/删除/启停后显示数秒）。 */
    private String ruleFeedback = "";
    private long ruleFeedbackAt;

    public MonitorScreen(MarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.literal("显示器控制台"));
    }

    @Override
    protected void init() {
        super.init();
        // 设备几何 + 内容缩放（内容按显示器屏幕基准尺寸布局，渲染时整体缩放）
        initDevice(true, ComputerLayout.MONITOR_CONTENT_W, ComputerLayout.MONITOR_CONTENT_H);

        this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 28, 180, 14, Component.literal("搜索/拼音"));
        this.searchBox.setMaxLength(32);
        this.searchBox.setHint(Component.literal("拼音/中文…"));
        this.searchBox.setResponder(text -> {
            this.searchText = text == null ? "" : text.trim();
            this.scrollOffset = 0;
            this.selected = 0;
            this.rebuildMatches();
        });
        this.addRenderableWidget(this.searchBox);

        this.amountBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 212, 46, 14, Component.literal("数量"));
        this.amountBox.setMaxLength(7);
        this.amountBox.setValue("1");
        this.amountBox.setResponder(this::onAmountChanged);
        this.addRenderableWidget(this.amountBox);

        this.thresholdBox = new EditBox(this.font, this.leftPos + 404, this.topPos + 188, 110, 14, Component.literal("阈值"));
        this.thresholdBox.setMaxLength(8);
        this.thresholdBox.setValue("1");
        this.addRenderableWidget(this.thresholdBox);

        this.ruleAmountBox = new EditBox(this.font, this.leftPos + 404, this.topPos + 210, 110, 14, Component.literal("数量"));
        this.ruleAmountBox.setMaxLength(6);
        this.ruleAmountBox.setValue("1");
        this.addRenderableWidget(this.ruleAmountBox);

        this.rebuildMatches();
        // 机箱存储视口：滚动请求只通知服务端；槽位映射一律以服务端回执为准
        // （客户端若先行改映射，与服务端存在时间差，会导致放入的物品落到错误行）
        this.towerView.onScrollRequest(row -> {
            this.getMenu().setTowerViewportHidden(true);
            Network.sendToServer(new TowerActionPacket(TowerActionPacket.SCROLL, 0L, row, 0, 0, "", 0, true));
        });
        // 滑动结束：若服务端回执尚未到位（映射未同步），补发一次（幂等，不本地改映射）
        this.towerView.onRowChanged(row -> {
            if (this.getMenu().towerScrollRow() != row) {
                Network.sendToServer(new TowerActionPacket(TowerActionPacket.SCROLL, 0L, row, 0, 0, "", 0, true));
            }
        });
        this.towerView.setContentRows((this.getMenu().towerVisibleSlots() + MarketMenu.TOWER_VIEW_COLS - 1) / MarketMenu.TOWER_VIEW_COLS);
        if (pending != null) {
            this.applySync(pending);
            pending = null;
        }
    }

    private void onAmountChanged(String text) {
        String clean = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (!clean.equals(text)) {
            this.amountBox.setValue(clean);
            return;
        }
        this.amount = clean.isEmpty() ? 0L : Math.min(MAX_AMOUNT, Long.parseLong(clean));
    }

    /** 滑动动画推进（每帧调用；结束时复位槽位可见性，避免空帧闪烁）。 */
    private void updateSlide() {
        if (!this.towerView.update()) {
            this.getMenu().setTowerViewportHidden(false);
        }
    }

    public void applySync(TowerSyncPacket packet) {
        this.sync = packet;
        // 依据已选机箱位置恢复选择（不依赖列表顺序，也不因瞬时同步而丢失选择）
        this.selectedTower = -1;
        if (this.selectedTowerPos != Long.MIN_VALUE) {
            for (int i = 0; i < packet.towers().size(); i++) {
                if (packet.towers().get(i).pos == this.selectedTowerPos) {
                    this.selectedTower = i;
                    break;
                }
            }
        }
        // 已选机箱：按同步的容量刷新可见存储格数与视口行数（9 + 9×内存条数），
        // 并以服务端同步的物品确定性刷新图标（不依赖容器槽位同步时序）
        if (this.selectedTower >= 0) {
            TowerSyncPacket.TowerInfo picked = packet.towers().get(this.selectedTower);
            int capacity = picked.capacity;
            this.getMenu().setTowerVisibleSlots(capacity);
            this.getMenu().setTowerStoredItems(picked.stored);
            this.towerView.setContentRows((capacity + MarketMenu.TOWER_VIEW_COLS - 1) / MarketMenu.TOWER_VIEW_COLS);
            // 滚动行以服务端为唯一权威：无论是否在滑动中都套用，
            // 保证「客户端点到的格子」与「服务端存入的格子」始终是同一个位置
            this.getMenu().setTowerScrollRow(packet.scrollRow());
            if (this.towerView.slideProgress() < 0.0F) {
                this.towerView.setScrollRow(this.getMenu().towerScrollRow());
            }
        }
    }

    private void rebuildMatches() {
        this.matches.clear();
        // 统一搜索实现：中文名 + 拼音全拼 + 拼音首字母（不再匹配英文物品 ID）
        this.matches.addAll(ItemIndex.search(this.searchText, 100));
        if (this.selected >= this.matches.size()) {
            this.selected = 0;
        }
    }

    private String selectedAsset() {
        return this.selected >= 0 && this.selected < this.matches.size() ? this.matches.get(this.selected) : "";
    }

    private void requestQuote(String asset) {
        if (asset != null && !asset.isEmpty()) {
            Network.sendToServer(new MarketActionPacket(MarketActionPacket.REQUEST_QUOTE, asset, 0));
        }
    }

    private void trade(boolean buy) {
        String asset = selectedAsset();
        if (!asset.isEmpty()) {
            Network.sendToServer(new MarketActionPacket(buy ? MarketActionPacket.BUY_SPOT : MarketActionPacket.SELL_SPOT,
                    asset, this.amount));
        }
    }

    private void sendTowerAction(byte action, TowerSyncPacket.TowerInfo tower, int value, long threshold, int ruleAmount, String itemId, int trigger, boolean enabled) {
        if (tower == null) {
            return;
        }
        Network.sendToServer(new TowerActionPacket(action, tower.pos, value, threshold, ruleAmount,
                itemId == null ? "" : itemId, trigger, enabled));
    }

    // ---- 渲染 ----

    @Override
    protected void renderDeviceCasing(GuiGraphics graphics) {
        // 显示器外壳（bezel + 屏幕底色；真实坐标，不随内容缩放）
        ComputerLayout.drawCasing(graphics, device());
    }

    @Override
    protected void beforeContentRender() {
        // 机箱存储视口滑动动画推进（内容渲染前）
        updateSlide();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 内容面板底色
        graphics.fill(this.leftPos, this.topPos, this.leftPos + CONTENT_W, this.topPos + CONTENT_H, 0xff101827);
        // 三列标题条
        graphics.fill(this.leftPos + 6, this.topPos + 4, this.leftPos + 198, this.topPos + 24, 0xff1d2b45);
        graphics.fill(this.leftPos + 204, this.topPos + 4, this.leftPos + 368, this.topPos + 24, 0xff1d2b45);
        graphics.fill(this.leftPos + 376, this.topPos + 4, this.leftPos + 514, this.topPos + 24, 0xff1d2b45);
        // 槽位描边（仅当前容量内启用的槽位；超出容量的隐藏行不绘制）
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy, 0xff3b526f);
            graphics.fill(sx - 1, sy + 16, sx + 17, sy + 17, 0xff3b526f);
            graphics.fill(sx - 1, sy, sx, sy + 16, 0xff3b526f);
            graphics.fill(sx + 16, sy, sx + 17, sy + 17, 0xff3b526f);
        }
        drawHotbarDivider(graphics);
    }

    /** 背包与快捷栏之间的分隔线（与玩家正常打开背包时一致的明显区分）。 */
    private void drawHotbarDivider(GuiGraphics g) {
        int n = this.menu.slots.size();
        if (n < 36) {
            return;
        }
        int hotbarTop = Integer.MAX_VALUE;
        int invBottom = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        for (int i = n - 9; i < n; i++) {
            Slot slot = this.menu.slots.get(i);
            hotbarTop = Math.min(hotbarTop, slot.y);
            minX = Math.min(minX, slot.x);
            maxX = Math.max(maxX, slot.x + 16);
        }
        for (int i = n - 36; i < n - 9; i++) {
            Slot slot = this.menu.slots.get(i);
            invBottom = Math.max(invBottom, slot.y + 16);
        }
        if (hotbarTop > invBottom && minX <= maxX) {
            int y = this.topPos + (invBottom + hotbarTop) / 2;
            g.fill(this.leftPos + minX, y, this.leftPos + maxX, y + 1, 0xFF3B526F);
        }
    }

    /** 描边控件（复用 UiDraw）。 */
    private void box(GuiGraphics graphics, int x, int y, int w, int h, boolean hovered) {
        UiDraw.borderedBox(graphics, x, y, w, h, hovered);
    }

    private boolean hit(int x, int y, int w, int h, int mx, int my) {
        return UiDraw.hit(x, y, w, h, mx, my);
    }

    /** 暗淡空位（该方向没有机箱）。 */
    private void emptyCell(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + TOWER_BTN_W, y + TOWER_BTN_H, 0xFF101827);
        graphics.fill(x, y, x + TOWER_BTN_W, y + 1, 0xFF23303F);
        graphics.fill(x, y + TOWER_BTN_H - 1, x + TOWER_BTN_W, y + TOWER_BTN_H, 0xFF23303F);
        graphics.fill(x, y, x + 1, y + TOWER_BTN_H, 0xFF23303F);
        graphics.fill(x + TOWER_BTN_W - 1, y, x + TOWER_BTN_W, y + TOWER_BTN_H, 0xFF23303F);
    }

    /** 按方向标签查找机箱在同步列表中的索引（无则 -1）。 */
    private int towerIndexByLabel(List<TowerSyncPacket.TowerInfo> towers, String label) {
        for (int i = 0; i < towers.size(); i++) {
            if (this.towerLabel(towers.get(i).pos).equals(label)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 注意：父类已对 pose 执行 translate(leftPos, topPos)，此处全部使用面板相对坐标。
        int mx = mouseX - this.leftPos;
        int my = mouseY - this.topPos;

        graphics.drawString(this.font, "现货交易区", 10, 9, 0xffb8c7df);
        graphics.drawString(this.font, this.title, 208, 9, 0xffffffff);
        graphics.drawString(this.font, "机箱控制", 380, 9, 0xffffffff);

        // ---- 左侧：现货交易 ----
        for (int i = this.scrollOffset; i < Math.min(this.matches.size(), this.scrollOffset + VISIBLE_ROWS); i++) {
            String asset = this.matches.get(i);
            int y = 48 + (i - this.scrollOffset) * ROW_H;
            Rectangle rect = new Rectangle(LIST_X, y, LIST_W, ROW_H);
            if (i == this.selected) {
                graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, 0xFF1E3A50);
            }
            if (rect.contains(mx, my)) {
                graphics.fill(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height, 0x33FFFFFF);
            }
            ItemStack stack = new ItemStack(ItemIndex.item(asset));
            if (!stack.isEmpty()) {
                var pose = graphics.pose();
                pose.pushPose();
                pose.translate(rect.x + 1, rect.y + 2, 0.0F);
                pose.scale(0.75F, 0.75F, 1.0F);
                graphics.renderItem(stack, 0, 0);
                pose.popPose();
            }
            AssetQuote quote = this.menu.quote(asset, false);
            String label = ItemIndex.displayName(asset) + (quote == null ? "  $—"
                    : "  $" + Math.round(quote.price) + String.format(Locale.ROOT, " %+.1f%%", quote.change * 100.0));
            graphics.drawString(this.font, trim(label, LIST_W - 14), rect.x + 13, rect.y + 4, 0xffe5e9ee);
        }
        // 选中物品信息
        String asset = selectedAsset();
        if (!asset.isEmpty()) {
            AssetQuote quote = this.menu.quote(asset, false);
            graphics.drawString(this.font, ItemIndex.displayName(asset), 8, 184,
                    quote == null ? 0xff8fa3bf : 0xffffffff);
            if (quote != null) {
                graphics.drawString(this.font, "现价 $" + Math.round(quote.price)
                                + String.format(Locale.ROOT, "  (%+.1f%%)", quote.change * 100.0),
                        8, 196, quote.change >= 0 ? 0xffff6b6b : 0xff5cda8a);
            }
        }
        // 数量步进按钮
        int x = 58;
        int y = 212;
        String[] steps = {"−10", "−1", "+1", "+10"};
        for (int i = 0; i < steps.length; i++) {
            box(graphics, x, y, 22, 14, hit(x, y, 22, 14, mx, my));
            graphics.drawString(this.font, steps[i], x + (22 - this.font.width(steps[i])) / 2, y + 3, 0xffe5e9ee);
            x += 24;
        }
        box(graphics, x, y, 32, 14, hit(x, y, 32, 14, mx, my));
        graphics.drawString(this.font, "最大", x + (32 - this.font.width("最大")) / 2, y + 3, 0xffe5e9ee);
        // 买卖按钮
        box(graphics, 8, 244, 76, 18, hit(8, 244, 76, 18, mx, my));
        graphics.drawString(this.font, "买入", 8 + (76 - this.font.width("买入")) / 2, 248, 0xffffffff);
        box(graphics, 90, 244, 76, 18, hit(90, 244, 76, 18, mx, my));
        graphics.drawString(this.font, "卖出", 90 + (76 - this.font.width("卖出")) / 2, 248, 0xffffffff);

        // ---- 中列：机箱存储 ----
        String boundText;
        if (this.sync == null) {
            boundText = "机箱存储: 同步中…";
        } else {
            TowerSyncPacket.TowerInfo picked = this.selectedTower >= 0 && this.selectedTower < this.sync.towers().size()
                    ? this.sync.towers().get(this.selectedTower) : null;
            boundText = picked == null ? "机箱存储: 未选择 — 点击右侧机箱按钮"
                    : "机箱存储: " + this.towerLabel(picked.pos);
        }
        graphics.drawString(this.font, trim(boundText, 158), 206, 190, 0xff8fa3bf);
        drawTowerScrollbar(graphics);
        drawSlide(graphics);

        // ---- 右侧：机箱控制面板 ----
        MarketSnapshot snapshot = this.menu.snapshot();
        graphics.drawString(this.font, "钱包 $" + snapshot.balance(), 382, 28, 0xffffd86b);

        if (this.sync == null) {
            graphics.drawString(this.font, "机箱配置加载中…", 382, 44, 0xff8fa3bf);
            return;
        }
        List<TowerSyncPacket.TowerInfo> towers = this.sync.towers();
        // 机箱方向选择：《通用机械》式九宫格（上/左/前/右/后/下），无机箱的方向显示为暗淡空位
        for (int d = 0; d < TOWER_DIRS.length; d++) {
            int tx = TOWER_GRID_X + TOWER_GRID_CELLS[d][0] * TOWER_PITCH_X;
            int ty = TOWER_GRID_Y + TOWER_GRID_CELLS[d][1] * TOWER_PITCH_Y;
            int index = towerIndexByLabel(towers, TOWER_DIRS[d]);
            boolean selected = index >= 0 && index == this.selectedTower;
            if (index < 0) {
                emptyCell(graphics, tx, ty);
                graphics.drawString(this.font, TOWER_DIRS[d],
                        tx + (TOWER_BTN_W - this.font.width(TOWER_DIRS[d])) / 2, ty + 3, 0xFF3A4A5C);
                continue;
            }
            box(graphics, tx, ty, TOWER_BTN_W, TOWER_BTN_H,
                    selected || hit(tx, ty, TOWER_BTN_W, TOWER_BTN_H, mx, my));
            if (selected) {
                // 当前显示的机箱：明显的呼吸闪烁（深青蓝 ↔ 中等亮度青蓝，避免过亮刺眼）
                float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 300.0);
                int body = lerpColor(0xFF072B42, 0xFF4FA6C8, pulse);
                int edge = lerpColor(0xFF2A6E8E, 0xFF8FCBE0, pulse);
                graphics.fill(tx - 2, ty - 2, tx + TOWER_BTN_W + 2, ty + TOWER_BTN_H + 2, edge);
                graphics.fill(tx - 1, ty - 1, tx + TOWER_BTN_W + 1, ty + TOWER_BTN_H + 1, body);
                graphics.fill(tx, ty, tx + TOWER_BTN_W, ty + TOWER_BTN_H, body);
                graphics.fill(tx, ty, tx + TOWER_BTN_W, ty + 1, edge);
                graphics.fill(tx, ty + TOWER_BTN_H - 1, tx + TOWER_BTN_W, ty + TOWER_BTN_H, edge);
                graphics.fill(tx, ty, tx + 1, ty + TOWER_BTN_H, edge);
                graphics.fill(tx + TOWER_BTN_W - 1, ty, tx + TOWER_BTN_W, ty + TOWER_BTN_H, edge);
            }
            graphics.drawString(this.font, TOWER_DIRS[d],
                    tx + (TOWER_BTN_W - this.font.width(TOWER_DIRS[d])) / 2, ty + 3,
                    selected ? 0xFFFFFF : 0xffb8c7df);
        }
        TowerSyncPacket.TowerInfo tower = this.selectedTower >= 0 && this.selectedTower < towers.size()
                ? towers.get(this.selectedTower) : null;
        if (tower == null) {
            graphics.drawString(this.font, "点击上方按钮选择机箱", 382, 96, 0xff8fa3bf);
            return;
        }
        String roleName = tower.role == ComputerTowerBlockEntity.ROLE_BUY ? "购入"
                : tower.role == ComputerTowerBlockEntity.ROLE_SELL ? "出售" : "存储";
        graphics.drawString(this.font, "选中(" + this.towerLabel(tower.pos) + "): " + roleName
                + (tower.redstone == 1 ? " ·需信号" : tower.redstone == 2 ? " ·需无信号" : ""),
                382, 96, 0xffb8c7df);

        // 选项行：标题 + 3 个 36px 按钮（购入/出售/存储；忽略/需信号/无信号；更新后/达价/红石）
        String[] roles = {"购入", "出售", "存储"};
        int[] roleVals = {ComputerTowerBlockEntity.ROLE_BUY, ComputerTowerBlockEntity.ROLE_SELL, ComputerTowerBlockEntity.ROLE_STORAGE};
        drawOptionRow(graphics, "角色", roles, roleIdx -> tower.role == roleVals[roleIdx], mx, my, 106);
        String[] reds = {"忽略", "需信号", "无信号"};
        drawOptionRow(graphics, "红石", reds, roleIdx -> tower.redstone == roleIdx, mx, my, 124);
        String[] triggers = {"更新后", "达价", "红石"};
        drawOptionRow(graphics, "触发", triggers, roleIdx -> this.editTrigger == roleIdx, mx, my, 142);

        // 规则编辑（当前列表选中物品）
        String editItem = selectedAsset();
        graphics.drawString(this.font, "物品: " + trim(ItemIndex.displayName(editItem), 120), 382, 158, 0xffe5e9ee);
        graphics.drawString(this.font, "方向", 382, 170, 0xff8fa3bf);
        String[] dirs = {"买入", "卖出"};
        for (int i = 0; i < dirs.length; i++) {
            int rx = 402 + i * 56;
            box(graphics, rx, 168, 52, 14, this.editDirection == i || hit(rx, 168, 52, 14, mx, my));
            graphics.drawString(this.font, dirs[i], rx + (52 - this.font.width(dirs[i])) / 2, 171,
                    this.editDirection == i ? 0xffffff : 0xffb8c7df);
        }
        graphics.drawString(this.font, "阈值", 382, 190, 0xff8fa3bf);
        graphics.drawString(this.font, "数量", 382, 212, 0xff8fa3bf);
        // 保存/删除/启用
        box(graphics, 382, 232, 62, 14, hit(382, 232, 62, 14, mx, my));
        graphics.drawString(this.font, "保存规则", 382 + (62 - this.font.width("保存规则")) / 2, 235, 0xffffffff);
        box(graphics, 448, 232, 34, 14, hit(448, 232, 34, 14, mx, my));
        graphics.drawString(this.font, "删除", 448 + (34 - this.font.width("删除")) / 2, 235, 0xffddb4b4);
        box(graphics, 486, 232, 28, 14, hit(486, 232, 28, 14, mx, my));
        graphics.drawString(this.font, "启停", 486 + (28 - this.font.width("启停")) / 2, 235, 0xffb8c7df);
        // 已配置规则摘要（最多 3 条，含方向/阈值/数量/触发时机/启停状态）
        int ry = 250;
        int shown = 0;
        for (TowerSyncPacket.RuleInfo rule : tower.rules) {
            if (shown >= 3) {
                break;
            }
            graphics.drawString(this.font, ruleSummary(rule), 382, ry,
                    rule.enabled ? 0xffc8d3e0 : 0xff8fa3bf);
            ry += 11;
            shown++;
        }
        if (tower.rules.size() > shown) {
            NoteText.draw(graphics, this.font, "…共 " + tower.rules.size() + " 条规则（滚动查看上限）", 382, ry, 0xff8fa3bf);
        }
        // 规则操作反馈（保存/删除/启停后 6 秒内显示完整参数）
        if (!this.ruleFeedback.isEmpty() && System.currentTimeMillis() - this.ruleFeedbackAt < 6000L) {
            NoteText.draw(graphics, this.font, trim(this.ruleFeedback, 132), 382, 268, 0xff8ce8a8);
        }
    }

    /** 单条规则的详细文字：序号/物品/方向/阈值/数量/触发时机/状态。 */
    private String ruleSummary(TowerSyncPacket.RuleInfo rule) {
        String trigger = switch (rule.trigger) {
            case ComputerTowerBlockEntity.TRIGGER_ON_PRICE -> "达价";
            case ComputerTowerBlockEntity.TRIGGER_ON_REDSTONE -> "红石";
            default -> "更新后";
        };
        return trim(ItemIndex.displayName(rule.itemId) + " " + (rule.action == 1 ? "卖出" : "买入")
                + (rule.action == 1 ? "≥" : "≤") + rule.threshold + " ×" + rule.amount
                + " · " + trigger + (rule.enabled ? " · 启用" : " · 已停用"), 138);
    }

    /** 记录一条规则操作反馈（显示 6 秒）。 */
    private void setRuleFeedback(String text) {
        this.ruleFeedback = text == null ? "" : text;
        this.ruleFeedbackAt = System.currentTimeMillis();
    }

    /** 机箱存储视口滑条（复用 ScrollViewport）。 */
    private void drawTowerScrollbar(GuiGraphics graphics) {
        this.towerView.renderScrollbar(graphics, UiDraw.CONTROL_BG, 0xFF5C9CC8, UiDraw.BORDER);
    }

    /**
     * 滚动切换时的平滑上下滑动：交给可复用的 {@link ScrollViewport#renderSlide} 绘制，
     * 这里只提供「单元格怎么画」（1px 描边 + 物品图标 + 数量角标）。
     */
    private void drawSlide(GuiGraphics graphics) {
        if (this.towerView.slideProgress() < 0.0F) {
            // 空闲：由 updateSlide 复位槽位可见性，父类渲染正常槽位
            return;
        }
        this.towerView.renderSlide(graphics, (this.getMenu().towerVisibleSlots() + MarketMenu.TOWER_VIEW_COLS - 1) / MarketMenu.TOWER_VIEW_COLS,
                MarketMenu.TOWER_VIEW_COLS, UiDraw.BORDER, UiDraw.BORDER,
                contentScale(), this.leftPos, this.topPos,
                (g, index, cx, cy) -> {
                    ItemStack stack = this.getMenu().towerItemAt(index);
                    if (!stack.isEmpty()) {
                        g.renderItem(stack, cx, cy);
                        g.renderItemDecorations(this.font, stack, cx, cy);
                    }
                });
    }

    /** 颜色线性插值（复用 UiDraw）。 */
    private static int lerpColor(int from, int to, float t) {
        return UiDraw.lerpColor(from, to, t);
    }

    /** 机箱相对显示器的位置：中文单字 上/下/左/右/前/后（以站在显示器正面的观察者视角）。 */
    private String towerLabel(long towerPos) {
        BlockPos monitor = this.menu.pos;
        BlockPos tower = BlockPos.of(towerPos);
        int dx = tower.getX() - monitor.getX();
        int dy = tower.getY() - monitor.getY();
        int dz = tower.getZ() - monitor.getZ();
        if (dy > 0) {
            return "上";
        }
        if (dy < 0) {
            return "下";
        }
        Direction d = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST
                : dz > 0 ? Direction.SOUTH : dz < 0 ? Direction.NORTH : null;
        if (d == null) {
            return "?";
        }
        Direction facing = Direction.NORTH;
        if (this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockState(this.menu.pos).hasProperty(ComputerBlock.FACING)) {
            facing = this.minecraft.level.getBlockState(this.menu.pos).getValue(ComputerBlock.FACING);
        }
        if (d == facing) {
            return "前";
        }
        if (d == facing.getOpposite()) {
            return "后";
        }
        // 观察者左手 = 显示器朝向顺时针旋转 90°
        Direction left = clockwise(facing);
        return d == left ? "左" : "右";
    }

    /** 环绕 Y 轴顺时针旋转 90°。 */
    private static Direction clockwise(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> direction;
        };
    }

    /** 一行「标题 + N 个 36px 选项按钮」，y 为按钮顶边；全部按钮带 1px 描边。 */
    private void drawOptionRow(GuiGraphics graphics, String caption, String[] options,
                               java.util.function.IntPredicate isSelected, int mx, int my, int y) {
        graphics.drawString(this.font, caption, 382, y + 3, 0xff8fa3bf);
        for (int i = 0; i < options.length; i++) {
            int rx = 400 + i * 39;
            boolean on = isSelected.test(i);
            box(graphics, rx, y, 36, 14, on || hit(rx, y, 36, 14, mx, my));
            if (on) {
                graphics.fill(rx, y, rx + 36, y + 14, 0xFF1E3A50);
                graphics.fill(rx, y, rx + 36, y + 1, 0xFF3B526F);
                graphics.fill(rx, y + 13, rx + 36, y + 14, 0xFF3B526F);
                graphics.fill(rx, y, rx + 1, y + 14, 0xFF3B526F);
                graphics.fill(rx + 35, y, rx + 36, y + 14, 0xFF3B526F);
            }
            graphics.drawString(this.font, options[i], rx + (36 - this.font.width(options[i])) / 2, y + 3,
                    on ? 0xffffff : 0xffb8c7df);
        }
    }

    private static String trim(String text, int maxWidth) {
        if (text.length() <= maxWidth / 6 + 2) {
            return text;
        }
        return text.substring(0, Math.max(1, maxWidth / 6)) + "…";
    }

    // ---- 交互 ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 自定义命中测试使用内容坐标；走原版容器交互时用 containerMouseClicked（坐标已换算）
        double cx = contentX(mouseX);
        double cy = contentY(mouseY);
        if (button != 0) {
            return containerMouseClicked(cx, cy, button);
        }
        int x = (int) cx;
        int y = (int) cy;
        int rx = x - this.leftPos;
        int ry = y - this.topPos;
        // 机箱存储视口滑条（复用 ScrollViewport）
        if (this.towerView.mouseClicked(rx, ry)) {
            this.draggingScrollbar = true;
            return true;
        }
        // 滑动动画期间屏蔽视口内点击（避免槽位映射中途交互错位）
        if (this.towerView.slideProgress() >= 0.0F && this.towerView.contains(rx, ry)) {
            return true;
        }
        // 列表行选择
        for (int i = this.scrollOffset; i < Math.min(this.matches.size(), this.scrollOffset + VISIBLE_ROWS); i++) {
            Rectangle rect = new Rectangle(LIST_X, 48 + (i - this.scrollOffset) * ROW_H, LIST_W, ROW_H);
            if (rect.contains(rx, ry)) {
                this.selected = i;
                this.requestQuote(this.matches.get(i));
                return true;
            }
        }
        // 数量步进
        int sx = 58;
        int sy = 212;
        long[] steps = {-10, -1, 1, 10};
        for (long step : steps) {
            if (hit(sx, sy, 22, 14, rx, ry)) {
                this.amount = Math.max(0L, Math.min(MAX_AMOUNT, this.amount + step));
                this.amountBox.setValue(Long.toString(this.amount));
                return true;
            }
            sx += 24;
        }
        if (hit(sx, sy, 32, 14, rx, ry)) {
            var item = ItemIndex.item(selectedAsset());
            this.amount = item == null ? 64L : Math.max(1L, item.getMaxStackSize());
            this.amountBox.setValue(Long.toString(this.amount));
            return true;
        }
        if (hit(8, 244, 76, 18, rx, ry)) {
            this.trade(true);
            return true;
        }
        if (hit(90, 244, 76, 18, rx, ry)) {
            this.trade(false);
            return true;
        }
        // 机箱面板
        if (this.sync != null) {
            List<TowerSyncPacket.TowerInfo> towers = this.sync.towers();
            for (int d = 0; d < TOWER_DIRS.length; d++) {
                int tx = TOWER_GRID_X + TOWER_GRID_CELLS[d][0] * TOWER_PITCH_X;
                int ty = TOWER_GRID_Y + TOWER_GRID_CELLS[d][1] * TOWER_PITCH_Y;
                if (hit(tx, ty, TOWER_BTN_W, TOWER_BTN_H, rx, ry)) {
                    int index = towerIndexByLabel(towers, TOWER_DIRS[d]);
                    if (index >= 0) {
                        this.selectedTower = index;
                        this.selectedTowerPos = towers.get(index).pos;
                        // 按该机箱的实际容量立即刷新可见存储格数与视口行数，并请求服务端绑定存储
                        this.getMenu().setTowerVisibleSlots(towers.get(index).capacity);
                        this.towerView.setContentRows((towers.get(index).capacity + MarketMenu.TOWER_VIEW_COLS - 1) / MarketMenu.TOWER_VIEW_COLS);
                        this.sendTowerAction(TowerActionPacket.SET_TARGET, towers.get(index), 0, 0, 0, null, 0, true);
                    }
                    return true;
                }
            }
            TowerSyncPacket.TowerInfo tower = this.selectedTower >= 0 && this.selectedTower < towers.size()
                    ? towers.get(this.selectedTower) : null;
            if (tower != null) {
                int[] roleVals = {ComputerTowerBlockEntity.ROLE_BUY, ComputerTowerBlockEntity.ROLE_SELL, ComputerTowerBlockEntity.ROLE_STORAGE};
                for (int i = 0; i < roleVals.length; i++) {
                    if (hit(400 + i * 39, 106, 36, 14, rx, ry)) {
                        this.sendTowerAction(TowerActionPacket.SET_ROLE, tower, roleVals[i], 0, 0, null, 0, true);
                        return true;
                    }
                }
                for (int i = 0; i < 3; i++) {
                    if (hit(400 + i * 39, 124, 36, 14, rx, ry)) {
                        this.sendTowerAction(TowerActionPacket.SET_REDSTONE, tower, i, 0, 0, null, 0, true);
                        return true;
                    }
                }
                for (int i = 0; i < 3; i++) {
                    if (hit(400 + i * 39, 142, 36, 14, rx, ry)) {
                        this.editTrigger = i;
                        return true;
                    }
                }
                for (int i = 0; i < 2; i++) {
                    if (hit(402 + i * 56, 168, 52, 14, rx, ry)) {
                        this.editDirection = i;
                        return true;
                    }
                }
                if (hit(382, 232, 62, 14, rx, ry)) {
                    String item = selectedAsset();
                    if (!item.isEmpty()) {
                        long threshold = parseLong(this.thresholdBox.getValue(), 1L);
                        int ruleAmount = (int) Math.max(1L, Math.min(999999L, parseLong(this.ruleAmountBox.getValue(), 1L)));
                        this.sendTowerAction(TowerActionPacket.SET_RULE, tower, this.editDirection, threshold, ruleAmount,
                                item, this.editTrigger, true);
                        // 详细反馈：物品 / 方向 / 阈值 / 每次数量 / 触发时机 / 红石条件
                        String trigger = switch (this.editTrigger) {
                            case ComputerTowerBlockEntity.TRIGGER_ON_PRICE -> "价格达到阈值时";
                            case ComputerTowerBlockEntity.TRIGGER_ON_REDSTONE -> "红石信号满足时";
                            default -> "每日价格更新后";
                        };
                        setRuleFeedback("已保存规则：" + ItemIndex.displayName(item)
                                + " " + (this.editDirection == 1 ? "卖出" : "买入")
                                + (this.editDirection == 1 ? " 价格 ≥ " : " 价格 ≤ ") + threshold
                                + " 时每次 " + ruleAmount + " 个 · 触发：" + trigger);
                    } else {
                        setRuleFeedback("保存失败：请先在左侧列表选择物品");
                    }
                    return true;
                }
                if (hit(448, 232, 34, 14, rx, ry)) {
                    String item = selectedAsset();
                    this.sendTowerAction(TowerActionPacket.REMOVE_RULE, tower, 0, 0, 0, item, 0, true);
                    setRuleFeedback(item.isEmpty() ? "删除失败：请先选择物品"
                            : "已删除规则：" + ItemIndex.displayName(item));
                    return true;
                }
                if (hit(486, 232, 28, 14, rx, ry)) {
                    String item = selectedAsset();
                    boolean matched = false;
                    for (TowerSyncPacket.RuleInfo rule : tower.rules) {
                        if (rule.itemId.equals(item)) {
                            this.sendTowerAction(TowerActionPacket.SET_RULE, tower, rule.action, rule.threshold,
                                    rule.amount, rule.itemId, rule.trigger, !rule.enabled);
                            setRuleFeedback((rule.enabled ? "已停用规则：" : "已启用规则：")
                                    + ItemIndex.displayName(item)
                                    + (rule.enabled ? "（机箱将不再自动执行）" : "（机箱将自动执行）"));
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        setRuleFeedback(item.isEmpty() ? "启停失败：请先选择物品"
                                : "启停失败：该物品尚无规则，请先保存规则");
                    }
                    return true;
                }
            }
        }
        return containerMouseClicked(cx, cy, button);
    }

    private static long parseLong(String text, long fallback) {
        try {
            return Long.parseLong(text.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingScrollbar
                && this.towerView.mouseDragged(contentX(mouseX) - this.leftPos, contentY(mouseY) - this.topPos)) {
            return true;
        }
        return containerMouseDragged(contentX(mouseX), contentY(mouseY), button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.towerView.mouseReleased();
        this.draggingScrollbar = false;
        return containerMouseReleased(contentX(mouseX), contentY(mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double cx = contentX(mouseX);
        double cy = contentY(mouseY);
        // 机箱存储视口滚动（平滑上下滑动，复用 ScrollViewport）
        if (this.towerView.mouseScrolled(cx - this.leftPos, cy - this.topPos, delta)) {
            return true;
        }
        if (cx >= this.leftPos + LIST_X && cx <= this.leftPos + LIST_X + LIST_W
                && cy >= this.topPos + 48 && cy <= this.topPos + 48 + VISIBLE_ROWS * ROW_H) {
            int maxOffset = Math.max(0, this.matches.size() - VISIBLE_ROWS);
            if (delta < 0) {
                this.scrollOffset = Math.min(maxOffset, this.scrollOffset + 1);
            } else if (delta > 0) {
                this.scrollOffset = Math.max(0, this.scrollOffset - 1);
            }
            return true;
        }
        return containerMouseScrolled(cx, cy, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 文本框编辑中：吞掉会误触发的游戏快捷键（E 关界面、Q 丢弃、F 副手、数字键交换等），
        // 字母/数字仍由 charTyped 正常写入
        if (TextInputGuard.consumeHotkeysWhileEditing(keyCode, scanCode,
                this.searchBox, this.amountBox, this.thresholdBox, this.ruleAmountBox)) {
            return true;
        }
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)
                || this.amountBox.keyPressed(keyCode, scanCode, modifiers)
                || this.thresholdBox.keyPressed(keyCode, scanCode, modifiers)
                || this.ruleAmountBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.charTyped(codePoint, modifiers)
                || this.amountBox.charTyped(codePoint, modifiers)
                || this.thresholdBox.charTyped(codePoint, modifiers)
                || this.ruleAmountBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
