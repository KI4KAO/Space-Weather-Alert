package net.ki4kao.spaceweatheralert;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class FlareWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        Context app = context.getApplicationContext();
        NotificationHelper.ensureChannel(app);
        AlarmScheduler.scheduleNext(app);
        WidgetUpdater.runCycle(app, null);
    }

    @Override
    public void onEnabled(Context context) {
        Context app = context.getApplicationContext();
        AlarmScheduler.scheduleNext(app);
        WidgetUpdater.runCycle(app, null);
    }

    @Override
    public void onDisabled(Context context) {
        // Keep the alarm alive if the user still wants flare notifications;
        // otherwise stop the cycle to save battery.
        if (!Prefs.notificationsEnabled(context.getApplicationContext())) {
            AlarmScheduler.cancel(context.getApplicationContext());
        }
    }
}
