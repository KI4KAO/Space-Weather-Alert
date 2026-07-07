package net.ki4kao.spaceweatheralert;

import android.content.Context;
import android.content.SharedPreferences;

/** Small wrapper around SharedPreferences for app settings + seed-then-watch state. */
public class Prefs {
    private static final String FILE = "swa_prefs";
    private static final String KEY_NOTIF = "notifications_enabled";
    private static final String KEY_LAST_FLARE = "last_notified_flare";
    private static final String KEY_SEEDED = "flare_seeded";

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean notificationsEnabled(Context c) {
        return sp(c).getBoolean(KEY_NOTIF, true);
    }

    public static void setNotificationsEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_NOTIF, v).apply();
    }

    public static String lastNotifiedFlare(Context c) {
        return sp(c).getString(KEY_LAST_FLARE, "");
    }

    public static void setLastNotifiedFlare(Context c, String v) {
        sp(c).edit().putString(KEY_LAST_FLARE, v == null ? "" : v).apply();
    }

    public static boolean seeded(Context c) {
        return sp(c).getBoolean(KEY_SEEDED, false);
    }

    public static void setSeeded(Context c, boolean v) {
        sp(c).edit().putBoolean(KEY_SEEDED, v).apply();
    }
}
