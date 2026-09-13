package cn.blockforge.generated.generatedmod.api.rarity;

import net.minecraft.ChatFormatting;

/**
 * 稀有度等级（7 级）：与「Rarity Core」模组的等级体系对齐。
 *
 * <p>等级 1~7 依次为 普通 / 稀有 / 罕见 / 史诗 / 传说 / 神话 / 唯一，
 * 同时决定本模组的三项经济参数：<b>基准价区间</b>、<b>日波动上限</b>与<b>卖出手续费率</b>。</p>
 *
 * <p>三者的规律一致：<b>等级越高，数值与所占区间越大</b>。</p>
 * <ul>
 *   <li>基准价：最低级下界 $10 → 最高级上界 $10000，区间宽度逐级放大；</li>
 *   <li>日波动（涨跌停幅度）：10% → 150%，逐级放大；</li>
 *   <li>卖出手续费：5.0% → 15.0%，逐级放大（含小数）。</li>
 * </ul>
 *
 * <p>等级数值含义与 Rarity Core 一致（1 最低、7 最高），颜色取自其默认样式配置。</p>
 */
public enum RarityTier {

    /** 1 级 · 普通：$10–30（宽 20），波动 10%，手续费 5.0% */
    COMMON(1, "普通", 0xCCCCCC, ChatFormatting.GRAY, 10, 30, 0.10, 0.050),
    /** 2 级 · 稀有：$31–70（宽 40），波动 20%，手续费 6.5% */
    UNCOMMON(2, "稀有", 0x55FF55, ChatFormatting.GREEN, 31, 70, 0.20, 0.065),
    /** 3 级 · 罕见：$71–150（宽 80），波动 35%，手续费 8.0% */
    RARE(3, "罕见", 0x55FFFF, ChatFormatting.AQUA, 71, 150, 0.35, 0.080),
    /** 4 级 · 史诗：$151–400（宽 250），波动 55%，手续费 9.5% */
    EPIC(4, "史诗", 0xFF55FF, ChatFormatting.LIGHT_PURPLE, 151, 400, 0.55, 0.095),
    /** 5 级 · 传说：$401–1000（宽 600），波动 80%，手续费 11.0% */
    LEGENDARY(5, "传说", 0xFFCC00, ChatFormatting.GOLD, 401, 1000, 0.80, 0.110),
    /** 6 级 · 神话：$1001–3000（宽 2000），波动 110%，手续费 13.0% */
    MYTHIC(6, "神话", 0xFF6666, ChatFormatting.RED, 1001, 3000, 1.10, 0.130),
    /** 7 级 · 唯一：$3001–10000（宽 7000），波动 150%，手续费 15.0% */
    UNIQUE(7, "唯一", 0xFF3333, ChatFormatting.DARK_RED, 3001, 10000, 1.50, 0.150);

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
    private final double sellFeeRate;

    RarityTier(int level, String displayName, int rgb, ChatFormatting chatColor,
               int priceMin, int priceMax, double volatility, double sellFeeRate) {
        this.level = level;
        this.displayName = displayName;
        this.rgb = rgb;
        this.chatColor = chatColor;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.volatility = volatility;
        this.sellFeeRate = sellFeeRate;
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

    /** 卖出手续费率（现货交易按当前价格收取；0.065 = 6.5%）。 */
    public double sellFeeRate() {
        return this.sellFeeRate;
    }

    /** 卖出手续费率的百分比文本（保留一位小数，如 {@code 6.5%}）。 */
    public String sellFeePercentText() {
        return String.format(java.util.Locale.ROOT, "%.1f%%", this.sellFeeRate * 100.0);
    }

    /** 按等级取值（超范围自动夹紧到 1~7）。 */
    public static RarityTier of(int level) {
        int clamped = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        return values()[clamped - 1];
    }
}
