package cn.blockforge.generated.mod3ce985ee;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 电脑机箱方块实体：动态容量存储（9 + 9×存储元件 格，最多 45 格）+ 自动化交易配置。
 * 存储空间通过机箱左侧的 4 个存储元件槽扩展（每个存储元件增加一排 9 格）；
 * 存储区兼容漏斗/管道等任意容器存取，元件槽只接受存储元件道具。
 * 自动化引擎见 {@code generatedmod.AutoTradeManager}。
 */
public final class ComputerTowerBlockEntity extends BlockEntity implements Container {

    /** 存储元件槽数量（服务器级 8 槽：CPU 左右各 4）。 */
    public static final int ELEMENT_SLOTS = 8;
    /** 基础存储容量（一排 9 格）。 */
    public static final int BASE_CAPACITY = 9;
    /** 每个存储元件扩展的格数（一排）。 */
    public static final int ROW_SIZE = 9;
    /** 最大容量：9 + 8×9 = 81。 */
    public static final int MAX_CAPACITY = BASE_CAPACITY + ELEMENT_SLOTS * ROW_SIZE;

    /** 机箱角色。 */
    public static final int ROLE_STORAGE = 0;
    public static final int ROLE_BUY = 1;
    public static final int ROLE_SELL = 2;
    /** 红石模式。 */
    public static final int REDSTONE_IGNORE = 0;
    public static final int REDSTONE_POWERED = 1;
    public static final int REDSTONE_UNPOWERED = 2;
    /** 规则触发时机。 */
    public static final int TRIGGER_NEXT_UPDATE = 0;
    public static final int TRIGGER_ON_PRICE = 1;
    public static final int TRIGGER_ON_REDSTONE = 2;

    /** 单条自动交易规则。 */
    public static final class TowerRule {
        public String itemId = "";
        public int action;          // 0=购入 1=出售
        public long threshold;      // 目标价 $
        public int amount = 1;      // 每次数量
        public byte trigger = TRIGGER_NEXT_UPDATE;
        public boolean enabled = true;
        public long lastFired = -1; // 上次执行日（每日最多一次）
    }

    /**
     * 存储物品：**固定按最大容量 81 分配**。
     *
     * <p>容量（{@link #capacity()}）随内存条增减动态变化，但底层列表始终是满容量，
     * 这样扩容出的行（索引 ≥9）无需"先写入再扩容"就能直接读取/取出/放入，
     * 不会出现「图标不显示、物品取不出来」的空档。</p>
     */
    private NonNullList<ItemStack> items = NonNullList.withSize(MAX_CAPACITY, ItemStack.EMPTY);
    private final NonNullList<ItemStack> elementItems = NonNullList.withSize(ELEMENT_SLOTS, ItemStack.EMPTY);
    private final List<TowerRule> rules = new ArrayList<>();
    private UUID owner;
    private int role = ROLE_STORAGE;
    private int redstone = REDSTONE_IGNORE;

    /** 已加载机箱注册表（维度|pos），供自动化引擎扫描。 */
    private static final java.util.Set<String> ACTIVE =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public static java.util.Set<String> activeKeys() {
        return ACTIVE;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ACTIVE.add(level.dimension().location().toString() + "|" + worldPosition.asLong());
        }
    }

    public ComputerTowerBlockEntity(BlockPos pos, BlockState state) {
        super(GeneratedMod.COMPUTER_TOWER_BE.get(), pos, state);
    }

    // ---- 存储元件 ----

    /** 是否为存储元件道具（tag：generated_mod:storage_elements）。 */
    public static boolean isStorageElement(ItemStack stack) {
        return !stack.isEmpty() && stack.is(cn.blockforge.generated.generatedmod.GeneratedMod.STORAGE_ELEMENTS_TAG);
    }

    public ItemStack elementItem(int slot) {
        return slot >= 0 && slot < ELEMENT_SLOTS ? elementItems.get(slot) : ItemStack.EMPTY;
    }

    public void setElementItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < ELEMENT_SLOTS) {
            elementItems.set(slot, stack == null ? ItemStack.EMPTY : stack);
            setChanged();
        }
    }

    /** 已安装的存储元件数量（每个增加一排容量）。 */
    public int elementCount() {
        int count = 0;
        for (ItemStack stack : elementItems) {
            if (isStorageElement(stack)) {
                count++;
            }
        }
        return count;
    }

    /** 当前存储容量：9 + 9×元件数。 */
    public int capacity() {
        return Math.min(MAX_CAPACITY, BASE_CAPACITY + elementCount() * ROW_SIZE);
    }

    /**
     * 移除元件是否会隐藏物品：若缩减后的容量之上仍有物品则不允许移除（物品不丢失优先）。
     */
    public boolean canRemoveElement(int slot) {
        if (elementItem(slot).isEmpty()) {
            return false;
        }
        int after = Math.max(BASE_CAPACITY, capacity() - ROW_SIZE);
        for (int i = after; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 存储元件槽的容器视图（供机箱存储元件菜单使用）。 */
    public ElementHolder elementView() {
        return new ElementHolder(this);
    }

    /** 机箱存储元件槽容器视图：只可放入存储元件道具；取出受容量保护。 */
    public static final class ElementHolder implements Container {
        private final ComputerTowerBlockEntity tower;

        public ElementHolder(ComputerTowerBlockEntity tower) {
            this.tower = tower;
        }

        @Override
        public int getContainerSize() {
            return ELEMENT_SLOTS;
        }

        /** 供菜单点击拦截使用：取出该槽元件是否会隐藏物品。 */
        public boolean canRemoveElementCheck(int slot) {
            return tower.canRemoveElement(slot);
        }

        @Override
        public ItemStack getItem(int index) {
            return tower.elementItem(index);
        }

        @Override
        public ItemStack removeItem(int index, int amount) {
            ItemStack current = tower.elementItem(index);
            if (current.isEmpty()) {
                return ItemStack.EMPTY;
            }
            // 取出元件后若会隐藏物品则拒绝（物品不丢失优先）
            if (!tower.canRemoveElement(index)) {
                return ItemStack.EMPTY;
            }
            int take = Math.min(amount, current.getCount());
            ItemStack result = current.copy();
            result.setCount(take);
            ItemStack rest = current.copy();
            rest.shrink(take);
            tower.setElementItem(index, rest.isEmpty() ? ItemStack.EMPTY : rest);
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            ItemStack current = tower.elementItem(index);
            if (current.isEmpty() || !tower.canRemoveElement(index)) {
                return ItemStack.EMPTY;
            }
            tower.setElementItem(index, ItemStack.EMPTY);
            return current;
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            if (index >= 0 && index < ELEMENT_SLOTS) {
                tower.setElementItem(index, stack == null ? ItemStack.EMPTY : stack);
            }
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return isStorageElement(stack);
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < ELEMENT_SLOTS; i++) {
                if (!tower.elementItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void setChanged() {
            tower.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(tower, player);
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < ELEMENT_SLOTS; i++) {
                tower.setElementItem(i, ItemStack.EMPTY);
            }
        }
    }

    // ---- 机箱配置 ----

    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public int role() {
        return role;
    }

    public void setRole(int role) {
        if (role >= ROLE_STORAGE && role <= ROLE_SELL) {
            this.role = role;
            setChanged();
        }
    }

    public int redstone() {
        return redstone;
    }

    public void setRedstone(int redstone) {
        if (redstone >= REDSTONE_IGNORE && redstone <= REDSTONE_UNPOWERED) {
            this.redstone = redstone;
            setChanged();
        }
    }

    public List<TowerRule> rules() {
        return rules;
    }

    public TowerRule rule(String itemId) {
        for (TowerRule rule : rules) {
            if (rule.itemId.equals(itemId)) {
                return rule;
            }
        }
        return null;
    }

    public TowerRule setRule(TowerRule rule) {
        TowerRule existing = rule(rule.itemId);
        if (existing == null) {
            rules.add(rule);
        } else {
            existing.action = rule.action;
            existing.threshold = rule.threshold;
            existing.amount = rule.amount;
            existing.trigger = rule.trigger;
            existing.enabled = rule.enabled;
        }
        setChanged();
        return rule;
    }

    public void removeRule(String itemId) {
        rules.removeIf(rule -> rule.itemId.equals(itemId));
        setChanged();
    }

    /** 标记数据变更（供引擎在外部修改规则后调用）。 */
    public void touch() {
        setChanged();
    }

    /** 存储/配置变更计数：任何 setChanged 都会自增，供显示器控制台主动补发同步包。 */
    private long revision;

    public long revision() {
        return revision;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        revision++;
    }

    private static String idOf(ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    // ---- 物品统计（仅当前容量内） ----

    public int countItems(String itemId) {
        int count = 0;
        int limit = Math.min(capacity(), items.size());
        for (int i = 0; i < limit; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && idOf(stack).equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public int removeItems(String itemId, int amount) {
        int removed = 0;
        int limit = Math.min(capacity(), items.size());
        for (int i = 0; i < limit && removed < amount; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && idOf(stack).equals(itemId)) {
                int take = Math.min(stack.getCount(), amount - removed);
                stack.shrink(take);
                removed += take;
                if (stack.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }
            }
        }
        if (removed > 0) {
            setChanged();
        }
        return removed;
    }

    /** 尝试放入物品，返回实际放入数量（放不下的剩余不扣除；仅写入容量内）。 */
    public int insertItems(ItemStack stack, int amount) {
        int inserted = 0;
        ItemStack source = stack.copy();
        source.setCount(amount);
        int limit = capacity();
        while (items.size() < limit) {
            items.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < limit && !source.isEmpty(); i++) {
            ItemStack target = items.get(i);
            if (target.isEmpty()) {
                int put = Math.min(source.getCount(), source.getMaxStackSize());
                items.set(i, source.copy());
                items.get(i).setCount(put);
                source.shrink(put);
                inserted += put;
            } else if (ItemStack.isSameItemSameTags(target, source) && target.getCount() < target.getMaxStackSize()) {
                int put = Math.min(source.getCount(), target.getMaxStackSize() - target.getCount());
                target.grow(put);
                source.shrink(put);
                inserted += put;
            }
        }
        if (inserted > 0) {
            setChanged();
        }
        return inserted;
    }

    // ---- Container（存储区，容量 = 9 + 9×元件数） ----

    @Override
    public int getContainerSize() {
        return capacity();
    }

    @Override
    public boolean isEmpty() {
        int limit = Math.min(capacity(), items.size());
        for (int i = 0; i < limit; i++) {
            if (!items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean validIndex(int slot) {
        return slot >= 0 && slot < capacity() && slot < items.size();
    }

    @Override
    public ItemStack getItem(int slot) {
        return validIndex(slot) ? items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!validIndex(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!validIndex(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= capacity()) {
            return;
        }
        while (items.size() <= slot) {
            items.add(ItemStack.EMPTY);
        }
        items.set(slot, stack == null ? ItemStack.EMPTY : stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        int limit = Math.min(capacity(), items.size());
        for (int i = 0; i < limit; i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // ---- NBT ----

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            CompoundTag storage = tag.getCompound("Items");
            // 兼容旧版（27 格）：按 NBT 实际槽数扩容，超出容量的物品保留（安装元件后恢复）
            ListTag list = storage.getList("Items", Tag.TAG_COMPOUND);
            if (list.size() > items.size()) {
                items = NonNullList.withSize(list.size(), ItemStack.EMPTY);
            }
            ContainerHelper.loadAllItems(storage, items);
        }
        if (tag.contains("ElementSlots")) {
            CompoundTag elements = tag.getCompound("ElementSlots");
            ListTag list = elements.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(ELEMENT_SLOTS, list.size()); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.contains("Slot")) {
                    elementItems.set(entry.getInt("Slot"), ItemStack.of(entry));
                } else {
                    elementItems.set(i, ItemStack.of(entry));
                }
            }
        }
        owner = tag.contains("Owner") ? tag.getUUID("Owner") : null;
        role = tag.getInt("Role");
        redstone = tag.getInt("Redstone");
        rules.clear();
        ListTag list = tag.getList("Rules", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            CompoundTag entry = (CompoundTag) raw;
            TowerRule rule = new TowerRule();
            rule.itemId = entry.getString("Item");
            rule.action = entry.getInt("Action");
            rule.threshold = entry.getLong("Threshold");
            rule.amount = entry.getInt("Amount");
            rule.trigger = entry.getByte("Trigger");
            rule.enabled = entry.getBoolean("Enabled");
            rule.lastFired = entry.contains("LastFired") ? entry.getLong("LastFired") : -1;
            if (!rule.itemId.isEmpty()) {
                rules.add(rule);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!items.stream().allMatch(ItemStack::isEmpty)) {
            CompoundTag storage = new CompoundTag();
            ContainerHelper.saveAllItems(storage, items);
            tag.put("Items", storage);
        }
        if (!elementItems.stream().allMatch(ItemStack::isEmpty)) {
            CompoundTag elements = new CompoundTag();
            ListTag list = new ListTag();
            for (int i = 0; i < ELEMENT_SLOTS; i++) {
                ItemStack stack = elementItems.get(i);
                if (!stack.isEmpty()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt("Slot", i);
                    stack.save(entry);
                    list.add(entry);
                }
            }
            elements.put("Items", list);
            tag.put("ElementSlots", elements);
        }
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putInt("Role", role);
        tag.putInt("Redstone", redstone);
        ListTag list = new ListTag();
        for (TowerRule rule : rules) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Item", rule.itemId);
            entry.putInt("Action", rule.action);
            entry.putLong("Threshold", rule.threshold);
            entry.putInt("Amount", rule.amount);
            entry.putByte("Trigger", rule.trigger);
            entry.putBoolean("Enabled", rule.enabled);
            entry.putLong("LastFired", rule.lastFired);
            list.add(entry);
        }
        tag.put("Rules", list);
    }
}
