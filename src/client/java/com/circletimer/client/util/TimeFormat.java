package com.circletimer.client.util;

import java.util.Locale;

public final class TimeFormat {
    private TimeFormat() {
    }

    public static String format(long millis) {
        long clamped = Math.max(0L, millis);
        long centis = (clamped % 1000L) / 10L;
        long totalSeconds = clamped / 1000L;
        long seconds = totalSeconds % 60L;
        long minutes = (totalSeconds / 60L) % 60L;
        long hours = totalSeconds / 3600L;

        if (hours > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, seconds, centis);
        }
        return String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, centis);
    }
}
