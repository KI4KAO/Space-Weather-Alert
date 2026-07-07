package net.ki4kao.spaceweatheralert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Context app = context.getApplicationContext();
            NotificationHelper.ensureChannel(app);
            AlarmScheduler.scheduleNext(app);
        }
    }
}
