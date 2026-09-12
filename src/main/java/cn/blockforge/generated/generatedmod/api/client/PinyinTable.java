package cn.blockforge.generated.generatedmod.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.Locale;
import java.util.Optional;

/**
 * 可复用的「汉字 → 拼音」工具（客户端）。
 *
 * <p>数据来自模组内置的轻量拼音表 {@code assets/generated_mod/pinyin.txt}
 * （每行 {@code 汉字:无声调拼音}，覆盖 CJK 基本区 U+4E00–U+9FFF，约 2.1 万条），
 * 首次使用时惰性加载，供任意搜索框实现拼音检索（全拼 / 首字母）。</p>
 *
 * <p>使用方式：</p>
 * <pre>
 *   PinyinTable.toPinyin("铁块")   // "tiekuai"
 *   PinyinTable.toInitials("铁块") // "tk"
 *   PinyinTable.hasHan("铁块")     // true
 * </pre>
 */
public final class PinyinTable {

    /** CJK 基本区起始码点。 */
    private static final char HAN_START = '\u4E00';
    private static final char HAN_END = '\u9FFF';
    /** 拼音表资源位置（模组命名空间下的根文件）。 */
    private static final ResourceLocation TABLE =
            new ResourceLocation("generated_mod", "pinyin.txt");

    /** 汉字码点 → 拼音（下标 = 码点 - HAN_START；未收录为 null）。 */
    private static String[] syllables;
    private static boolean loaded;

    private PinyinTable() {
    }

    /** 是否包含汉字（用于判断是否需要拼音转换）。 */
    public static boolean hasHan(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= HAN_START && c <= HAN_END) {
                return true;
            }
        }
        return false;
    }

    /** 单个汉字 → 无声调拼音；非汉字或未收录返回 null。 */
    public static String syllable(char c) {
        ensureLoaded();
        if (c < HAN_START || c > HAN_END || syllables == null) {
            return null;
        }
        return syllables[c - HAN_START];
    }

    /**
     * 整串转全拼：汉字取拼音、其余字符原样保留（统一小写、忽略空白）。
     *
     * <p>例：{@code "铁块"} → {@code "tiekuai"}；{@code "TNT 炸药"} → {@code "tntzayao"}。</p>
     */
    public static String toPinyin(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String py = syllable(c);
            if (py != null) {
                out.append(py);
            } else if (!Character.isWhitespace(c)) {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    /**
     * 整串转拼音首字母：汉字取拼音首字母，其余字符原样保留（统一小写、忽略空白）。
     *
     * <p>例：{@code "铁块"} → {@code "tk"}；{@code "钻石剑"} → {@code "zsj"}。</p>
     */
    public static String toInitials(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String py = syllable(c);
            if (py != null && !py.isEmpty()) {
                out.append(py.charAt(0));
            } else if (!Character.isWhitespace(c)) {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    /** 规范化查询串：小写、去空白，便于与全拼/首字母/中文名比对。 */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT).trim();
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (!Character.isWhitespace(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** 惰性加载拼音表（失败时退化为"不支持拼音"，不影响其它功能）。 */
    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        String[] table = new String[HAN_END - HAN_START + 1];
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return;
            }
            Optional<Resource> resource = minecraft.getResourceManager().getResource(TABLE);
            if (resource.isEmpty()) {
                return;
            }
            try (BufferedReader reader = resource.get().openAsReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int split = line.indexOf(':');
                    if (split <= 0 || split + 1 >= line.length()) {
                        continue;
                    }
                    char han = line.charAt(0);
                    if (han < HAN_START || han > HAN_END) {
                        continue;
                    }
                    String pinyin = line.substring(split + 1).trim();
                    if (!pinyin.isEmpty()) {
                        table[han - HAN_START] = pinyin;
                    }
                }
            }
        } catch (Exception ignored) {
            // 读取失败时保持空表：搜索仍可用中文名匹配
        }
        syllables = table;
    }
}
