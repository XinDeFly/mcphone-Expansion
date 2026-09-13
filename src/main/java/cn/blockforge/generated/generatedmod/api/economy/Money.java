package cn.blockforge.generated.generatedmod.api.economy;

import java.util.Locale;

/**
 * 可复用的货币工具：<b>内部一律以「分」为单位的 long 存储</b>，对外按两位小数显示。
 *
 * <p>为什么用「分」而不是 double：手续费、股息、盯市盈亏都会产生小数金额，
 * 用整数分可以避免浮点误差累积；显示时再折算为两位小数即可。</p>
 *
 * <pre>
 *   long cents = Money.fromDollars(12.345);   // 1235（四舍五入到分）
 *   Money.format(cents);                      // "$12.35"
 *   Money.formatPlain(cents);                 // "12.35"
 *   Money.price(1234.5);                      // "$1234.50"（价格显示同样两位小数）
 * </pre>
 */
public final class Money {

    /** 一元 = 100 分。 */
    public static final long CENTS_PER_UNIT = 100L;

    private Money() {
    }

    /** 美元金额（double）→ 分（四舍五入）。 */
    public static long fromDollars(double dollars) {
        if (Double.isNaN(dollars) || Double.isInfinite(dollars)) {
            return 0L;
        }
        return Math.round(dollars * CENTS_PER_UNIT);
    }

    /** 分 → 美元金额（double，仅用于需要参与浮点运算的场合）。 */
    public static double toDollars(long cents) {
        return cents / (double) CENTS_PER_UNIT;
    }

    /** 按比例计费（如手续费的百分数），返回分。 */
    public static long rate(long cents, double rate) {
        return Math.round(cents * rate);
    }

    /** 格式化带 $ 前缀的两位小数文本，例如 {@code $1,234.50} 不带千分位：{@code $1234.50}。 */
    public static String format(long cents) {
        return "$" + formatPlain(cents);
    }

    /** 两位小数文本（不带 $），例如 {@code 1234.50}。 */
    public static String formatPlain(long cents) {
        boolean negative = cents < 0;
        long abs = Math.abs(cents);
        String text = String.format(Locale.ROOT, "%d.%02d", abs / CENTS_PER_UNIT, abs % CENTS_PER_UNIT);
        return negative ? "-" + text : text;
    }

    /** 价格（double 美元）→ 两位小数文本（不带 $），用于「实时价格」类显示。 */
    public static String pricePlain(double dollars) {
        return String.format(Locale.ROOT, "%.2f", dollars);
    }

    /** 价格（double 美元）→ 带 $ 的两位小数文本。 */
    public static String price(double dollars) {
        return "$" + pricePlain(dollars);
    }

    /** 百分比文本（保留一位小数），例如 {@code 6.5%}。 */
    public static String percent(double rate) {
        return String.format(Locale.ROOT, "%.1f%%", rate * 100.0);
    }
}
