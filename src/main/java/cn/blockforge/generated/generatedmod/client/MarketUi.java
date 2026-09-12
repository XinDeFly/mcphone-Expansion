package cn.blockforge.generated.generatedmod.client;

import java.util.Locale;

public final class MarketUi {
    private MarketUi() {
    }

    public static String timeText(long dayTime) {
        long ticks = ((dayTime % 24000L) + 24000L) % 24000L;
        int hour = (int) ((ticks / 1000L + 6L) % 24L);
        int minute = (int) ((ticks % 1000L) * 60L / 1000L);
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    public static String remainingText(long dayTime) {
        long ticks = 24000L - (((dayTime % 24000L) + 24000L) % 24000L);
        if (ticks == 24000L) {
            ticks = 0L;
        }
        long hour = ticks / 1000L;
        long minute = (ticks % 1000L) * 60L / 1000L;
        return String.format(Locale.ROOT, "%d:%02d", hour, minute);
    }

    public static String headerTime(long dayTime) {
        return "时间 " + timeText(dayTime) + " · 距更新 " + remainingText(dayTime);
    }
}
