package cn.blockforge.generated.generatedmod.menu;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.AutoTradeManager;
import cn.blockforge.generated.generatedmod.data.AssetQuote;
import cn.blockforge.generated.generatedmod.data.MarketData;
import cn.blockforge.generated.generatedmod.data.MarketSnapshot;
import cn.blockforge.generated.mod3ce985ee.ComputerTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarketMenu extends AbstractContainerMenu {
    public final BlockPos pos;
public final int mode;
    /** 做市商界面（mode 6）对应的村民实体 ID；其它模式为 -1。 */
    public int brokerId = -1;
    public final Inventory playerInventory;
    private Container marketInventory;
    /** 显示器控制台（mode 5）：可切换绑定到所选机箱容器的 45 个槽位。 */
    private final List<SwappableSlot> towerSlots = new ArrayList<>();
    /**
     * mode 5 当前可见的机箱存储格数（9 + 9×内存条数）：
     * 服务端由绑定容器实际容量得出；客户端由 TowerSyncPacket 同步得出（客户端没有服务端容器数据）。
     */
    private int towerVisibleSlots = ComputerTowerBlockEntity.BASE_CAPACITY;
    /** 显示器控制台机箱存储视口显示的行数（固定 4 行，超出部分滚动）。 */
    public static final int TOWER_VIEW_ROWS = 4;
    /** 显示器控制台机箱存储视口的列数（与槽位布局一致；渲染时按列数换算索引）。 */
    public static final int TOWER_VIEW_COLS = 9;
    /** 显示器控制台机箱存储视口几何（界面渲染与命中测试共用，勿各自硬编码）。 */
    public static final int TOWER_VIEW_X = 206;
    public static final int TOWER_VIEW_Y = 40;
    public static final int TOWER_VIEW_PITCH = 18;
    public static final int TOWER_VIEW_W = TOWER_VIEW_COLS * TOWER_VIEW_PITCH - 2;
    public static final int TOWER_VIEW_H = TOWER_VIEW_ROWS * TOWER_VIEW_PITCH - 2;
    /** 视口当前起始行（0 起，按行滚动）。 */
    private int towerScrollRow = 0;
    private MarketSnapshot snapshot = MarketSnapshot.empty();
    private static final Map<String, AssetQuote> QUOTES = new HashMap<>();
    private static String lastSpotAsset = "";
    private static int quoteRevision = 0;

    public MarketMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), buf.readByte());
    }

    public MarketMenu(int id, Inventory inventory, BlockPos pos, int mode) {
        super(GeneratedMod.MARKET_MENU.get(), id);
        this.pos = pos;
        this.mode = mode;
        this.playerInventory = inventory;
        int storageSize = switch (mode) {
            case 1 -> 36;                 // 现货终端存储
            case 4 -> ComputerTowerBlockEntity.ELEMENT_SLOTS;   // 机箱存储元件槽（4 格）
            case 5 -> ComputerTowerBlockEntity.MAX_CAPACITY;    // 显示器控制台最多 45 格
            default -> 27;
        };
        Container found = null;
        boolean serverSide = inventory.player.level() instanceof ServerLevel;
        if (inventory.player.level() instanceof ServerLevel serverLevel
                && serverLevel.getBlockEntity(pos) instanceof Container container) {
            found = container;
        }
        if (mode == 3) {
            // 手机现货页 27 格独立存储：服务端持久化（存于 MarketData），客户端为同步镜像。
            this.marketInventory = inventory.player.level() instanceof ServerLevel serverLevel
                    ? MarketData.get(serverLevel).storage(inventory.player.getUUID())
                    : new SimpleContainer(storageSize);
        } else if (mode == 5) {
            // 显示器控制台没有独立存储：45 个槽位绑定到玩家选择的机箱容器（初始为占位容器，
            // 可见格数由 towerVisibleSlots 控制，未选择机箱时只显示基础 9 格；
            // 客户端占位容器仅作同步渲染镜像，因此放行放置校验——权威判定在服务端）。
            this.marketInventory = new PlaceholderContainer(ComputerTowerBlockEntity.MAX_CAPACITY, !serverSide);
        } else if (mode == 4) {
            // 机箱存储元件界面：8 个内存条插槽（主板 CPU 左右两侧）+ 玩家背包；只接受内存条。
            this.marketInventory = found instanceof ComputerTowerBlockEntity tower
                    ? tower.elementView() : new PlaceholderContainer(storageSize, !serverSide);
        } else {
            this.marketInventory = found == null ? new SimpleContainer(storageSize) : found;
        }
        if (mode == 1 || mode == 2 || mode == 6) {
            for (int i = 0; i < storageSize; i++) {
                addSlot(new Slot(marketInventory, i, 158 + (i % 9) * 18, 36 + (i / 9) * 18));
            }
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(inventory, col + row * 9 + 9, 158 + col * 18, 116 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col, 158 + col * 18, 176));
            }
        } else if (mode == 3) {
            // 手机现货页槽位布局：9×3 独立存储 + 4px 分隔 + 9×4 背包，16px 格距（相对 leftPos/topPos）。
            // cellY 由 44 上移到 34：让最后一排（快捷栏）在 GUI 坐标 y≈152 结束，
            // 避免遮挡页面下方 y≈162 起的「物品名 / 价格 / 拥有数量」信息（槽位渲染与
            // 点击命中均以本坐标为准，因此上移后两者依旧对齐）。
            int cellX = 182;
            int cellY = 34;
            int pitch = 16;
            for (int i = 0; i < storageSize; i++) {
                addSlot(new Slot(marketInventory, i, cellX + (i % 9) * pitch, cellY + (i / 9) * pitch));
            }
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    // 第 4 行（快捷栏）与背包 3 行之间留 4px 间隔，与玩家正常打开背包一致
                    addSlot(new Slot(inventory, row < 3 ? col + row * 9 + 9 : col,
                            cellX + col * pitch, cellY + 52 + row * pitch + (row == 3 ? 4 : 0)));
                }
            }
        } else if (mode == 4 || mode == 5) {
            // 桌面式槽位布局（18px）：机箱存储元件界面 / 显示器控制台。
            // mode 4：240×210 窗口，左侧边 4 个元件槽竖直排列（x=12, y=26 起），玩家背包右下方；
            // mode 5：控制台 520×280，中列 9×5 机箱存储（含隐藏行）+ 9×4 背包（cellX=206, cellY=48）。
            if (mode == 4) {
                // 主板式界面：槽位坐标按「内容基准尺寸」布局（与 GUI 缩放无关，
                // 渲染时由 ScaledDeviceScreen 连同外框一起缩放，槽位与画面始终对齐；
                // contentSize() 为纯数值，服务端同样可安全调用）
                int[] cs = cn.blockforge.generated.mod3ce985ee.ComputerTowerElementLayout.contentSize();
                cn.blockforge.generated.mod3ce985ee.ComputerTowerElementLayout.Layout l =
                        cn.blockforge.generated.mod3ce985ee.ComputerTowerElementLayout.of(cs[0], cs[1]);
                // 主板 CPU 左右两侧各 4 条内存插槽（服务器级 8 槽）
                for (int i = 0; i < storageSize; i++) {
                    addSlot(new Slot(marketInventory, i, l.elementSlotX(i), l.ramY));
                }
                // 玩家背包 9×4（金属面板区域；快捷栏行与背包行之间留 4px 间隔）
                for (int row = 0; row < 4; row++) {
                    for (int col = 0; col < 9; col++) {
                        addSlot(new Slot(inventory, row < 3 ? col + row * 9 + 9 : col,
                                l.cellX + col * 18, l.cellY + row * 18 + (row == 3 ? 4 : 0)));
                    }
                }
            } else {
                // 显示器控制台：机箱存储固定 4 行视口（TOWER_VIEW_ROWS × TOWER_VIEW_COLS 格，超出部分滚动查看）+ 背包 9×4
                int cellX = TOWER_VIEW_X;
                int cellY = TOWER_VIEW_Y;
                int pitch = TOWER_VIEW_PITCH;
                for (int i = 0; i < TOWER_VIEW_ROWS * TOWER_VIEW_COLS; i++) {
                    SwappableSlot slot = new SwappableSlot(this, marketInventory, i,
                            cellX + (i % TOWER_VIEW_COLS) * pitch, cellY + (i / TOWER_VIEW_COLS) * pitch);
                    towerSlots.add(slot);
                    addSlot(slot);
                }
                // 背包 9×4：位于机箱存储视口下方（快捷栏行与背包行之间留 4px 间隔）
                int playerY = cellY + TOWER_VIEW_ROWS * pitch + 4;
                for (int row = 0; row < 4; row++) {
                    for (int col = 0; col < 9; col++) {
                        addSlot(new Slot(inventory, row < 3 ? col + row * 9 + 9 : col,
                                cellX + col * pitch, playerY + row * pitch));
                    }
                }
                this.applyTowerScroll();
            }
        }
    }

    /** 机箱存储元件界面（mode 4）：取出元件时若会隐藏物品则提示并拦截。 */
    @Override
    public void clicked(int slotId, int dragType, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (mode == 4 && slotId >= 0 && slotId < ComputerTowerBlockEntity.ELEMENT_SLOTS
                && this.marketInventory instanceof ComputerTowerBlockEntity.ElementHolder holder
                && !holder.getItem(slotId).isEmpty() && !holder.canRemoveElementCheck(slotId)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "无法移除存储元件：缩减容量后将有物品超出容量，请先取出那些物品"));
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    /** 服务端：当前绑定（SET_TARGET 所选）的机箱容器；客户端为 null（占位容器由同步包填充）。 */
    private Container boundTower;

    /** 服务端：打开本控制台的玩家（内容变化时主动补发同步包）。 */
    private net.minecraft.server.level.ServerPlayer viewer;
    /** 上次推送时机箱的变更计数。 */
    private long pushedRevision = -1L;

    public Container boundTower() {
        return boundTower;
    }

    /**
     * 每刻调用（服务端）：机箱存储容量/内容发生变化时刷新可见格数并主动补发同步包。
     *
     * <p>放置/取出物品走的是原版点击流程，不会触发 TowerActionPacket，
     * 因此必须在此检测变更并推送，客户端才能拿到全部行（含内存条扩展出的行）的物品图标。
     * 同时把可见格数与容器真实容量对齐——否则内存条被拔掉后，仍会被允许放入超出容量的格子，
     * 而原版此时已把物品从光标取走，物品就会凭空消失。</p>
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (mode != 5 || this.viewer == null || this.boundTower == null) {
            return;
        }
        boolean dirty = false;
        int size = Math.max(ComputerTowerBlockEntity.BASE_CAPACITY,
                Math.min(ComputerTowerBlockEntity.MAX_CAPACITY, this.boundTower.getContainerSize()));
        if (size != this.towerVisibleSlots) {
            this.towerVisibleSlots = size;
            this.towerScrollRow = Math.min(this.towerScrollRow, maxScrollRow());
            this.applyTowerScroll();
            dirty = true;
        }
        if (this.boundTower instanceof ComputerTowerBlockEntity tower) {
            long current = tower.revision();
            if (current != this.pushedRevision) {
                this.pushedRevision = current;
                dirty = true;
            }
        }
        if (dirty) {
            AutoTradeManager.sendSync(this.viewer, this.pos, this.towerScrollRow);
        }
    }

    /** 将显示器控制台槽位绑定到指定机箱容器（服务端），并同步物品与可见格数到客户端。 */
    public void bindTowerContainer(Container container, net.minecraft.server.level.ServerPlayer viewer) {
        if (mode != 5 || container == null) {
            return;
        }
        this.viewer = viewer;
        this.boundTower = container;
        this.pushedRevision = container instanceof ComputerTowerBlockEntity tower ? tower.revision() : -1L;
        this.towerVisibleSlots = Math.max(ComputerTowerBlockEntity.BASE_CAPACITY,
                Math.min(ComputerTowerBlockEntity.MAX_CAPACITY, container.getContainerSize()));
        for (SwappableSlot slot : towerSlots) {
            slot.bind(container);
        }
        this.applyTowerScroll();
        this.broadcastChanges();
    }

    /** 客户端：以服务端同步的机箱存储物品刷新占位容器（绝对索引，供槽位与滑动渲染读取）。 */
    public void setTowerStoredItems(List<ItemStack> items) {
        if (mode != 5) {
            return;
        }
        int limit = Math.min(items.size(), this.marketInventory.getContainerSize());
        for (int i = 0; i < limit; i++) {
            this.marketInventory.setItem(i, items.get(i));
        }
    }

    /** 设置机箱存储的可见格数（客户端收到同步包后调用）。 */
    public void setTowerVisibleSlots(int slots) {
        if (mode != 5) {
            return;
        }
        this.towerVisibleSlots = Math.max(ComputerTowerBlockEntity.BASE_CAPACITY,
                Math.min(ComputerTowerBlockEntity.MAX_CAPACITY, slots));
        this.towerScrollRow = Math.min(this.towerScrollRow, maxScrollRow());
        this.applyTowerScroll();
    }

    /** 当前机箱存储可见格数。 */
    public int towerVisibleSlots() {
        return towerVisibleSlots;
    }

    /** 视口最大起始行。 */
    public int maxScrollRow() {
        int rows = (towerVisibleSlots + TOWER_VIEW_COLS - 1) / TOWER_VIEW_COLS;
        return Math.max(0, rows - TOWER_VIEW_ROWS);
    }

    /** 当前视口起始行。 */
    public int towerScrollRow() {
        return towerScrollRow;
    }

    /** 设置视口起始行（客户端滚动时本地调用，服务端由 SCROLL 包设置）。 */
    public void setTowerScrollRow(int row) {
        if (mode != 5) {
            return;
        }
        int clamped = Math.max(0, Math.min(maxScrollRow(), row));
        if (clamped == this.towerScrollRow) {
            return;
        }
        this.towerScrollRow = clamped;
        this.applyTowerScroll();
        this.broadcastChanges();
    }

    /** 按当前滚动行更新视口槽位映射的容器索引。 */
    private void applyTowerScroll() {
        for (int i = 0; i < towerSlots.size(); i++) {
            towerSlots.get(i).setContainerIndex(towerScrollRow * TOWER_VIEW_COLS + i);
        }
    }

    /** 当客户端视口滚动时对齐显示内容（绝对值取容器元素，供滑动渲染使用）。 */
    public ItemStack towerItemAt(int containerIndex) {
        if (containerIndex < 0 || containerIndex >= this.marketInventory.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return this.marketInventory.getItem(containerIndex);
    }

    /** 客户端滑动动画期间隐藏视口槽位渲染与命中（仅客户端设置；服务端默认 false）。 */
    private boolean towerViewportHidden;

    public void setTowerViewportHidden(boolean hidden) {
        this.towerViewportHidden = hidden;
    }

    /** 可在绑定的机箱容器之间切换的槽位（显示器控制台复用同一槽位集合）。 */
    public static final class SwappableSlot extends Slot {
        private final MarketMenu owner;
        private Container bound;
        /** 当前映射的容器索引（随视口滚动变化）。 */
        private int containerIndex;

        public SwappableSlot(MarketMenu owner, Container initial, int index, int x, int y) {
            super(initial, index, x, y);
            this.owner = owner;
            this.bound = initial;
            this.containerIndex = index;
        }

        public void bind(Container container) {
            if (container != null) {
                this.bound = container;
            }
        }

        public void setContainerIndex(int containerIndex) {
            this.containerIndex = Math.max(0, containerIndex);
        }

        @Override
        public ItemStack getItem() {
            return this.bound.getItem(this.containerIndex);
        }

        @Override
        public void set(ItemStack stack) {
            this.bound.setItem(this.containerIndex, stack);
        }

        @Override
        public void setChanged() {
            this.bound.setChanged();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 三重校验：可见容量内、容器真实容量内（防止拔掉内存条后仍能放入导致物品被吞）、容器本身允许
            return this.containerIndex < this.owner.towerVisibleSlots()
                    && this.containerIndex < this.bound.getContainerSize()
                    && this.bound.canPlaceItem(this.containerIndex, stack);
        }

        @Override
        public boolean isActive() {
            return super.isActive() && !this.owner.towerViewportHidden
                    && this.containerIndex < this.owner.towerVisibleSlots();
        }

        @Override
        public ItemStack remove(int amount) {
            return this.bound.removeItem(this.containerIndex, amount);
        }

        @Override
        public boolean hasItem() {
            return !this.bound.getItem(this.containerIndex).isEmpty();
        }

        @Override
        public int getMaxStackSize() {
            return this.bound.getMaxStackSize();
        }

        @Override
        public int getContainerSlot() {
            return this.bound.getContainerSize() > this.containerIndex ? this.containerIndex : -1;
        }
    }

    /**
     * 未绑定真实容器时的占位容器。
     *
     * <p>{@code mirror=true}（客户端镜像）时放行放置校验：客户端的本地校验不是权威，
     * 若在此返回 false，原版「按住右键拖拽逐个摆放」「拖拽分堆」等操作会在客户端被直接拦掉；
     * 服务端仍会按真实容器规则裁决（未选择机箱时由服务端的占位容器拒绝，不会吞物品）。</p>
     */
    private static final class PlaceholderContainer extends SimpleContainer {
        private final boolean mirror;

        PlaceholderContainer(int size, boolean mirror) {
            super(size);
            this.mirror = mirror;
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return this.mirror;
        }
    }

    public Container marketInventory() {
        return marketInventory;
    }

    public MarketSnapshot snapshot() {
        return snapshot;
    }

    public void setSnapshot(MarketSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public AssetQuote quote(String asset) {
        return quote(asset, false);
    }

    public AssetQuote quote(String asset, boolean futures) {
        return QUOTES.get(quoteKey(asset, futures));
    }

    public void putQuote(String asset, AssetQuote quote) {
        putQuote(asset, false, quote);
    }

    public void putQuote(String asset, boolean futures, AssetQuote quote) {
        QUOTES.put(quoteKey(asset, futures), quote);
        quoteRevision++;
    }

    private static String quoteKey(String asset, boolean futures) {
        return (futures ? "F:" : "S:") + asset;
    }

    public int quoteCount() {
        return QUOTES.size();
    }

    public int quoteRevision() {
        return quoteRevision;
    }

    public static String lastSpotAsset() {
        return lastSpotAsset;
    }

    public static void setLastSpotAsset(String asset) {
        lastSpotAsset = asset == null ? "" : asset;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (mode < 1 || mode > 5) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack original = slot.getItem();
            result = original.copy();
            // mode 5：机箱存储槽位为固定的 45 格（含需滚动查看的行），存取范围按全部机箱槽位计算
            int storageSize = mode == 5 ? towerSlots.size() : this.marketInventory.getContainerSize();
            if (index < storageSize) {
                if (!this.moveItemStackTo(original, storageSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (mode == 5) {
                // 快捷移动优先放入**当前可见的 4 行**，放不下才占用需要滚动的其它行，
                // 避免物品"瞬间消失"（其实是被放到了看不见的行里）
                int first = Math.max(0, this.towerScrollRow * 9);
                int last = Math.min(storageSize, first + TOWER_VIEW_ROWS * 9);
                if (!this.moveItemStackTo(original, first, last, false)
                        && !this.moveItemStackTo(original, 0, storageSize, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(original, 0, storageSize, false)) {
                return ItemStack.EMPTY;
            }
            if (original.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
