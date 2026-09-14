package cn.blockforge.generated.generatedmod.broker;

import cn.blockforge.generated.generatedmod.api.rarity.RarityTier;

/**
 * 可复用的做市商报价规则：按稀有度确定价差，并给出买价 bid / 卖价 ask。
 *
 * <p>报价锚定当日行情中间价（做市商不自己定价）：</p>
 * <pre>
 *   bid = mid × (1 − spread / 2)     // 玩家卖给做市商的成交价
 *   ask = mid × (1 + spread / 2)     // 玩家从做市商买入的成交价
 * </pre>
 *
 * <p>价差率按稀有度递增（波动越大 → 做市商承担的风险越大 → 价差越宽）。
 * 阶段③将在此基础上加入「库存滑动」：库存偏离目标值时整体下压/抬升报价。</p>
 */
public final class BrokerQuotes {

    /** 价差率表（下标 = 等级 − 1）：普通 1.0% … 唯一 10.0%。 */
    private static final double[] SPREAD = {0.010, 0.020, 0.035, 0.050, 0.065, 0.080, 0.100};

    private BrokerQuotes() {
    }

    /** 该稀有度的价差率（如 0.018 = 1.8%）。 */
    public static double spread(RarityTier tier) {
        int index = Math.max(RarityTier.MIN_LEVEL, Math.min(RarityTier.MAX_LEVEL, tier.level())) - 1;
        return SPREAD[index];
    }

    /** 做市商买价（玩家卖出成交价）。 */
    public static double bid(double mid, RarityTier tier) {
        return mid * (1.0 - spread(tier) / 2.0);
    }

    /** 做市商卖价（玩家买入成交价）。 */
    public static double ask(double mid, RarityTier tier) {
        return mid * (1.0 + spread(tier) / 2.0);
    }

    /** 价差文本（如 {@code 1.8%}）。 */
    public static String spreadText(RarityTier tier) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", spread(tier) * 100.0);
    }
}
