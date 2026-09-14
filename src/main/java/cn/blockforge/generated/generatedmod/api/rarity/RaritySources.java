package cn.blockforge.generated.generatedmod.api.rarity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.fml.ModList;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可复用的稀有度解析器：统一对外提供 <b>1~7 级</b>稀有度。
 *
 * <p>取值优先级（自动降级，缺谁都能跑）：</p>
 * <ol>
 *   <li><b>Rarity Core 模组</b>（modid {@code raritycore}）—— 已安装时直接调用其公开 API
 *       {@code RarityCoreAPI.getRarity(...)}，与游戏内显示的稀有度完全一致；</li>
 *   <li><b>内置快照</b> —— 未安装时读取本模组随包携带的稀有度快照
 *       （{@code data/generated_mod/rarity_snapshot.json}，由导出命令生成）；</li>
 *   <li><b>原版稀有度映射</b> —— 快照里没有的物品，按原版 4 档稀有度映射到 7 级。</li>
 * </ol>
 *
 * <p>全程使用反射访问第三方 API，且包在 try/catch 中：未安装 Rarity Core 时不会加载其类，
 * 也不会抛错。查询结果按物品 ID 缓存（{@link #reload()} 可清空）。</p>
 */
public final class RaritySources {

    /** Rarity Core 的 modid。 */
    public static final String RARITYCORE_MODID = "raritycore";
    /** 内置快照资源路径。 */
    public static final String SNAPSHOT_RESOURCE = "/data/generated_mod/rarity_snapshot.json";

    private static final Map<String, Integer> CACHE = new ConcurrentHashMap<>();

    private static volatile Map<String, Integer> snapshot;
    private static volatile boolean snapshotLoaded;
    private static volatile boolean coreChecked;
    private static volatile Method coreGetRarityStack;
    private static volatile Method coreGetRarityItem;
    private static volatile boolean coreRegistryChecked;
    private static volatile Method coreGetRegistryMap;

    private RaritySources() {
    }

    // ---- 对外查询 ----

    /** 查询物品的稀有度等级（1~7）。 */
    public static int level(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return RarityTier.MIN_LEVEL;
        }
        Integer cached = CACHE.get(itemId);
        if (cached != null) {
            return cached;
        }
        int value = resolve(itemId);
        CACHE.put(itemId, value);
        return value;
    }

    /** 查询物品堆的稀有度等级（1~7；会先尝试 Rarity Core，以便支持其 NBT 规则）。 */
    public static int level(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RarityTier.MIN_LEVEL;
        }
        Integer fromCore = fromCore(stack);
        if (fromCore != null) {
            return fromCore;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return level(id == null ? "" : id.toString());
    }

    /** 查询物品的稀有度档（含名称、颜色、价格区间与波动上限）。 */
    public static RarityTier tier(String itemId) {
        return RarityTier.of(level(itemId));
    }

    /** 查询物品堆的稀有度档。 */
    public static RarityTier tier(ItemStack stack) {
        return RarityTier.of(level(stack));
    }

    /** 稀有度中文名。 */
    public static String tierName(String itemId) {
        return tier(itemId).displayName();
    }

    /** 当前生效的数据来源名称（用于导出命令的诊断输出）。 */
    public static String activeSource() {
        if (coreAvailable()) {
            return "Rarity Core（已安装，直接采用其评级）";
        }
        if (!snapshot().isEmpty()) {
            return "内置稀有度快照（" + snapshot().size() + " 条）";
        }
        return "原版稀有度映射（未找到快照）";
    }

    /** 内置快照条目（物品 ID → 等级），只读副本。 */
    public static Map<String, Integer> snapshotEntries() {
        return new HashMap<>(snapshot());
    }

    /** 是否已加载 Rarity Core。 */
    public static boolean coreAvailable() {
        return coreMethod() != null;
    }

    /** 清空解析缓存（配置重载或导出后调用）。 */
    public static void reload() {
        CACHE.clear();
        synchronized (RaritySources.class) {
            snapshot = null;
            snapshotLoaded = false;
        }
    }

    // ---- 内部实现 ----

    private static int resolve(String itemId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(itemId));
        if (item == null) {
            return RarityTier.MIN_LEVEL;
        }
        Integer fromCore = fromCore(new ItemStack(item));
        if (fromCore != null) {
            return fromCore;
        }
        Integer fromSnapshot = snapshot().get(itemId);
        if (fromSnapshot != null && fromSnapshot >= RarityTier.MIN_LEVEL && fromSnapshot <= RarityTier.MAX_LEVEL) {
            return fromSnapshot;
        }
        return classifyLocally(new ItemStack(item));
    }

    /**
     * 本地稀有度分类（<b>仅前 4 级</b>）：供未安装 Rarity Core、且内置快照中也没有该物品时使用。
     *
     * <p>规则：先取原版 4 档稀有度作为基础等级（普通 1 / 罕见 2 / 稀有 3 / 史诗 4），
     * 再按物品自身特性加分，最后夹紧到 1~4 级：</p>
     * <ul>
     *   <li>不可堆叠（工具、装备、药水等）→ +1；</li>
     *   <li>带耐久（工具、武器、盔甲）→ +1。</li>
     * </ul>
     * <p>这样普通方块/材料多为 1 级，而铁砧、附魔台、钻石装备等会得到 3~4 级，
     * 与它们实际的获取难度相符，且不会越过前 4 级。</p>
     */
    public static int classifyLocally(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RarityTier.MIN_LEVEL;
        }
        int base = fromVanilla(stack.getItem().getRarity(stack));
        int bonus = 0;
        if (stack.getMaxStackSize() == 1) {
            bonus++;
        }
        if (stack.isDamageableItem()) {
            bonus++;
        }
        return Math.max(RarityTier.MIN_LEVEL, Math.min(4, base + bonus));
    }

    /** 原版 4 档稀有度映射到前 4 级（普通→1、罕见→2、稀有→3、史诗→4）。 */
    public static int fromVanilla(Rarity rarity) {
        if (rarity == null) {
            return RarityTier.MIN_LEVEL;
        }
        return switch (rarity) {
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 4;
            default -> RarityTier.MIN_LEVEL;
        };
    }

    /** 通过反射调用 Rarity Core（未安装或调用失败时返回 null 表示"继续降级"）。 */
    private static Integer fromCore(ItemStack stack) {
        Method method = coreMethod();
        if (method == null) {
            return null;
        }
        try {
            Object result = method.invoke(null, stack);
            if (result instanceof Integer value
                    && value >= RarityTier.MIN_LEVEL && value <= RarityTier.MAX_LEVEL) {
                return value;
            }
        } catch (Throwable ignored) {
            // 第三方 API 异常时静默降级到快照 / 原版稀有度
        }
        return null;
    }

    /**
     * 主动向 Rarity Core <b>批量请求</b>全部物品的稀有度（调用其 {@code getRegistryMap()}），
     * 写入本模组缓存。世界加载 / 服务器启动时调用，避免逐项惰性查询。
     *
     * @return 本次写入缓存的条目数；未安装 Rarity Core 或调用失败时返回 0
     */
    public static int prefetch() {
        Method method = coreRegistryMethod();
        if (method == null) {
            return 0;
        }
        try {
            Object result = method.invoke(null);
            if (result instanceof Map<?, ?> map) {
                int count = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof ResourceLocation id
                            && entry.getValue() instanceof Integer level
                            && level >= RarityTier.MIN_LEVEL && level <= RarityTier.MAX_LEVEL) {
                        CACHE.put(id.toString(), level);
                        count++;
                    }
                }
                return count;
            }
        } catch (Throwable ignored) {
            // 第三方 API 异常时静默跳过，后续仍可按单项惰性查询
        }
        return 0;
    }

    /** Rarity Core 的 {@code getRegistryMap()} 方法（未安装时为 null）。 */
    private static Method coreRegistryMethod() {
        if (!coreRegistryChecked) {
            synchronized (RaritySources.class) {
                if (!coreRegistryChecked) {
                    Method resolved = null;
                    if (ModList.get() != null && ModList.get().isLoaded(RARITYCORE_MODID)) {
                        try {
                            Class<?> api = Class.forName("org.yanbwe.raritycore.api.RarityCoreAPI");
                            resolved = api.getMethod("getRegistryMap");
                        } catch (Throwable ignored) {
                            resolved = null;
                        }
                    }
                    coreGetRegistryMap = resolved;
                    coreRegistryChecked = true;
                }
            }
        }
        return coreGetRegistryMap;
    }

    private static Method coreMethod() {
        if (!coreChecked) {
            synchronized (RaritySources.class) {
                if (!coreChecked) {
                    Method resolved = null;
                    if (ModList.get() != null && ModList.get().isLoaded(RARITYCORE_MODID)) {
                        try {
                            Class<?> api = Class.forName("org.yanbwe.raritycore.api.RarityCoreAPI");
                            resolved = api.getMethod("getRarity", ItemStack.class);
                        } catch (Throwable ignored) {
                            resolved = null;
                        }
                    }
                    coreGetRarityStack = resolved;
                    coreChecked = true;
                }
            }
        }
        return coreGetRarityStack;
    }

    /** 惰性加载内置快照。 */
    private static Map<String, Integer> snapshot() {
        if (!snapshotLoaded) {
            synchronized (RaritySources.class) {
                if (!snapshotLoaded) {
                    snapshot = loadSnapshot();
                    snapshotLoaded = true;
                }
            }
        }
        Map<String, Integer> current = snapshot;
        return current == null ? Map.of() : current;
    }

    private static Map<String, Integer> loadSnapshot() {
        Map<String, Integer> map = new HashMap<>();
        try (InputStream in = RaritySources.class.getResourceAsStream(SNAPSHOT_RESOURCE)) {
            if (in == null) {
                return map;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject items = root.has("items") && root.get("items").isJsonObject()
                        ? root.getAsJsonObject("items") : root;
                for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        map.put(entry.getKey(), entry.getValue().getAsInt());
                    }
                }
            }
        } catch (Throwable ignored) {
            // 快照缺失/损坏时按空表处理，自动降级到原版稀有度
        }
        return map;
    }
}
