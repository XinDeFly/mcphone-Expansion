package cn.blockforge.generated.generatedmod.api.rarity;

import net.minecraft.ChatFormatting;

/**
 * 稀有度等级（7 级）：与「Rarity Core」模组的等级体系对齐。
 *
 * <p>等级 1~7 依次为 普通 / 稀有 / 罕见 / 史诗 / 传说 / 神话 / 唯一，
 * 同时决定本模组的两项经济参数：<b>基准价区间</b>与<b>日波动上限</b>（涨跌停幅度）。</p>
 *
 * <p>等级数值含义与 Rarity Core 一致（1 最低、7 最高），颜色取自其默认样式配置。</p>
 */
public enum RarityTier {

    /** 1 级 · 普通 */
    COMMON(1, "普通", 0xCCCCCC, ChatFormatting.GRAY, 30, 100, 0.10),
    /** 2 级 · 稀有 */
    UNCOMMON(2, "稀有", 0x55FF55, ChatFormatting.GREEN, 101, 300, 0.16),
    /** 3 级 · 罕见 */
    RARE(3, "罕见", 0x55FFFF, ChatFormatting.AQUA, 301, 1000, 0.22),
    /** 4 级 · 史诗 */
    EPIC(4, "史诗", 0xFF55FF, ChatFormatting.LIGHT_PURPLE, 1001, 3000, 0.32),
    /** 5 级 · 传说 */
    LEGENDARY(5, "传说", 0xFFCC00, ChatFormatting.GOLD, 3001, 10000, 0.45),
    /** 6 级 · 神话 */
    MYTHIC(6, "神话", 0xFF6666, ChatFormatting.RED, 10001, 30000, 0.70),
    /** 7 级 · 唯一 */
    UNIQUE(7, "唯一", 0xFF3333, ChatFormatting.DARK_RED, 30001, 100000, 1.00);

    /** 最低等级（与 Rarity Core 一致）。 */
    public static final int MIN_LEVEL = 1;
    /** 最高等级（与 Rarity Core 一致）。 */
    public static final int MAX_LEVEL = 7;

    private final int level;
    private final String displayName;
    private final int rgb;
    private final ChatFormatting chatColor;
    private final int priceMin;
    private final int priceMax;
    private final double volatility;

    RarityTier(int level, String displayName, int rgb, ChatFormatting chatColor,
               int priceMin, int priceMax, double volatility) {
        this.level = level;
        this.displayName = displayName;
        this.rgb = rgb;
        this.chatColor = chatColor;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.volatility = volatility;
    }

    /** 等级数值（1~7）。 */
    public int level() {
        return this.level;
    }

    /** 中文等级名（普通 / 稀有 / … / 唯一）。 */
    public String displayName() {
        return this.displayName;
    }

    /** 等级配色（RGB，取自 Rarity Core 默认样式）。 */
    public int rgb() {
        return this.rgb;
    }

    /** 等级配色（原版聊天格式，用于组件文本）。 */
    public ChatFormatting chatColor() {
        return this.chatColor;
    }

    /** 基准价下界。 */
    public int priceMin() {
        return this.priceMin;
    }

    /** 基准价上界。 */
    public int priceMax() {
        return this.priceMax;
    }

    /** 日波动上限（涨跌停幅度）。 */
    public double volatility() {
        return this.volatility;
    }

    /** 按等级取值（超范围自动夹紧到 1~7）。 */
    public static RarityTier of(int level) {
        int clamped = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        return values()[clamped - 1];
    }
}
