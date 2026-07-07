package net.ki4kao.spaceweatheralert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Drives the 60-second update cycle via exact alarms (rescheduled on each fire). */
public class AlarmScheduler {

    public static final String ACTION_TICK = "net.ki4kao.spaceweatheralert.TICK";
    private static final int REQ = 7001;
    private static final long INTERVAL_MS = 60_000L;

    private static PendingIntent pendingIntent(Context ctx) {
        Intent i = new Intent(ctx, UpdateReceiver.class).setAction(ACTION_TICK);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, REQ, i, flags);
    }

    /** Schedule the next tick ~60s out. Called on app open, boot, widget update, and each tick. */
    public static void scheduleNext(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long triggerAt = System.currentTimeMillis() + INTERVAL_MS;
        PendingIntent pi = pendingIntent(ctx);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // Fall back to inexact if the OS withholds exact-alarm permission.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pendingIntent(ctx));
    }
}
