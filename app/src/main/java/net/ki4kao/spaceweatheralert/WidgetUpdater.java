package net.ki4kao.spaceweatheralert;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs a single update cycle off the main thread: fetch flare data, refresh every
 * placed widget, and (if enabled) fire a notification for a newly detected M+ flare.
 */
public class WidgetUpdater {

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();

    /** @param finish optional Runnable invoked when the async work completes (e.g. pendingResult.finish). */
    public static void runCycle(final Context appCtx, final Runnable finish) {
        POOL.execute(() -> {
            try {
                FlareData.Result r = FlareData.fetchAll();
                pushToWidgets(appCtx, r);
                if (r.ok) maybeNotify(appCtx, r);
            } catch (Throwable ignored) {
            } finally {
                if (finish != null) finish.run();
            }
        });
    }

    private static void pushToWidgets(Context ctx, FlareData.Result r) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName cn = new ComponentName(ctx, FlareWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        if (ids == null || ids.length == 0) return;

        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_flare);

        rv.setTextViewText(R.id.widget_current, r.ok ? safe(r.currentClass) : "--");
        rv.setTextViewText(R.id.widget_flux,
                r.currentFlux.isEmpty() ? "GOES X-ray" : (r.currentFlux + " W/m²"));

        if (r.hasMFlare) {
            rv.setTextViewText(R.id.widget_last_class, safe(r.mClass));
            rv.setTextViewText(R.id.widget_last_time, FlareData.prettyTime(r.mTime));
        } else {
            rv.setTextViewText(R.id.widget_last_class, r.ok ? "none / 7d" : "—");
            rv.setTextViewText(R.id.widget_last_time, r.ok ? "no M+ in 7 days" : "tap to open");
        }

        String upd = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date());
        rv.setTextViewText(R.id.widget_updated, "upd " + upd);

        // Tap widget -> open app
        Intent open = new Intent(ctx, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);
        rv.setOnClickPendingIntent(R.id.widget_root, pi);

        for (int id : ids) mgr.updateAppWidget(id, rv);
    }

    private static void maybeNotify(Context ctx, FlareData.Result r) {
        if (!r.hasMFlare) return;

        // Seed on very first run so we don't alert for a pre-existing flare.
        if (!Prefs.seeded(ctx)) {
            Prefs.setSeeded(ctx, true);
            Prefs.setLastNotifiedFlare(ctx, r.mKey);
            return;
        }
        if (r.mKey.equals(Prefs.lastNotifiedFlare(ctx))) return; // already handled
        Prefs.setLastNotifiedFlare(ctx, r.mKey);

        if (!Prefs.notificationsEnabled(ctx)) return;

        Bitmap img = FlareData.fetchBitmap(FlareData.SDO_512);
        NotificationHelper.notifyFlare(ctx, r.mClass, r.mTime, img);
    }

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "--" : s;
    }

    /** Fire a test notification immediately using live SDO imagery. */
    public static void testNotification(final Context appCtx) {
        POOL.execute(() -> {
            Bitmap img = FlareData.fetchBitmap(FlareData.SDO_512);
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                    .format(new java.util.Date());
            NotificationHelper.notifyFlare(appCtx, "M1.0 (TEST)", now, img);
        });
    }
}
