package net.ki4kao.spaceweatheralert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receives the 60s alarm tick: reschedule, then run an update cycle. */
public class UpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        // Reschedule immediately so the cadence continues even if this cycle is slow.
        AlarmScheduler.scheduleNext(app);

        final PendingResult pr = goAsync();
        WidgetUpdater.runCycle(app, pr::finish);
    }
}
