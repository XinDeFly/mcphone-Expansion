package cn.blockforge.generated.generatedmod.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ItemIndex {
    private static List<String> cachedIds;
    private static List<String> cachedNamesLower;
    /** 每个物品的中文名全拼（如 "铁块" → "tiekuai"）。 */
    private static List<String> cachedPinyin;
    /** 每个物品的中文名拼音首字母（如 "铁块" → "tk"）。 */
    private static List<String> cachedInitials;
    private static Map<String, String> chineseNames = Map.of();

    private ItemIndex() {
    }

    public static List<String> itemIds() {
        ensureLoaded();
        return cachedIds;
    }

    public static List<String> displayNamesLower() {
        ensureLoaded();
        return cachedNamesLower;
    }

    /**
     * 物品搜索（各界面搜索框共用）：
     * <ol>
     *   <li>中文名包含（保持原有中文检索）；</li>
     *   <li><b>拼音全拼</b>包含（如 {@code tiekuai} / {@code zSSJ}）；</li>
     *   <li><b>拼音首字母</b>包含（如 {@code tk} / {@code zsj}）。</li>
     * </ol>
     * 查询串会统一小写并去除空白；**不再匹配英文物品 ID**（按需求移除英文搜索）。
     *
     * @param query 查询串（可为空，返回全部的前 limit 项）
     * @param limit 结果上限（≤0 表示不限制）
     */
    public static List<String> search(String query, int limit) {
        ensureLoaded();
        String q = cn.blockforge.generated.generatedmod.api.client.PinyinTable.normalize(query);
        List<String> result = new ArrayList<>();
        int max = limit > 0 ? limit : cachedIds.size();
        boolean ascii = isAsciiLetters(q);
        for (int i = 0; i < cachedIds.size() && result.size() < max; i++) {
            if (q.isEmpty() || matches(i, q, ascii)) {
                result.add(cachedIds.get(i));
            }
        }
        return result;
    }

    /** 判断查询串是否为纯拉丁字母/数字（用于决定是否做拼音匹配）。 */
    private static boolean isAsciiLetters(String q) {
        if (q.isEmpty()) {
            return false;
        }
        for (int i = 0; i < q.length(); i++) {
            char c = q.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == 'v';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean matches(int index, String query, boolean asciiQuery) {
        if (cachedNamesLower.get(index).contains(query)) {
            return true;
        }
        if (!asciiQuery) {
            return false;
        }
        // 拼音全拼 / 首字母（仅当查询串是拉丁字母或数字时才可能命中）
        return cachedPinyin.get(index).contains(query) || cachedInitials.get(index).contains(query);
    }

    private static void ensureLoaded() {
        if (cachedIds != null) {
            return;
        }
        chineseNames = loadChineseNames();
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> pinyin = new ArrayList<>();
        List<String> initials = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (key != null) {
                String display = displayName(key.toString());
                ids.add(key.toString());
                names.add(display.toLowerCase(Locale.ROOT));
                pinyin.add(cn.blockforge.generated.generatedmod.api.client.PinyinTable.toPinyin(display));
                initials.add(cn.blockforge.generated.generatedmod.api.client.PinyinTable.toInitials(display));
            }
        }
        cachedIds = List.copyOf(ids);
        cachedNamesLower = List.copyOf(names);
        cachedPinyin = List.copyOf(pinyin);
        cachedInitials = List.copyOf(initials);
    }

    private static Map<String, String> loadChineseNames() {
        Map<String, String> result = new HashMap<>();
        try {
            Minecraft minecraft = Minecraft.getInstance();
            Optional<Resource> resource = minecraft.getResourceManager()
                    .getResource(new ResourceLocation("minecraft", "lang/zh_cn.json"));
            if (resource.isPresent()) {
                try (Reader reader = resource.get().openAsReader()) {
                    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            result.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to the current client language if the bundled Chinese file is unavailable.
        }
        return result;
    }

    public static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
    }

    public static String displayName(String id) {
        Item item = item(id);
        if (item == null) {
            return id;
        }
        String chinese = chineseNames.get(item.getDescriptionId());
        return chinese != null ? chinese : Component.translatable(item.getDescriptionId()).getString();
    }

    /**
     * 稀有度等级名（7 级：普通 / 稀有 / 罕见 / 史诗 / 传说 / 神话 / 唯一）。
     *
     * <p>数据来源自动降级：已安装「Rarity Core」→ 采用其评级；未安装 → 本模组内置稀有度快照 →
     * 再退回原版 4 档稀有度映射。详见 {@link cn.blockforge.generated.generatedmod.api.rarity.RaritySources}。</p>
     */
    public static String rarityTier(String id) {
        return cn.blockforge.generated.generatedmod.api.rarity.RaritySources.tierName(id);
    }

    /** 稀有度等级配色（与 7 级体系一致）。 */
    public static ChatFormatting rarityColor(String id) {
        return cn.blockforge.generated.generatedmod.api.rarity.RaritySources.tier(id).chatColor();
    }
}
