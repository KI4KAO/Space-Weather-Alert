package net.ki4kao.spaceweatheralert;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    public static final String CHANNEL_ID = "flare_alerts";
    private static final int NOTIF_ID = 4201;

    /** Custom sound bundled as res/raw/not.wav (added at build time from ki4kao.net). */
    public static Uri soundUri(Context ctx) {
        return Uri.parse("android.resource://" + ctx.getPackageName() + "/" + R.raw.not);
    }

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Solar Flare Alerts", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("M-class and above solar flare notifications");
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        ch.setSound(soundUri(ctx), attrs);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 400, 200, 400});
        nm.createNotificationChannel(ch);
    }

    /**
     * Post an M/X flare notification with the SDO 131 image.
     * @param bitmap SDO image, may be null.
     */
    public static void notifyFlare(Context ctx, String flareClass, String isoTime, Bitmap bitmap) {
        ensureChannel(ctx);

        boolean isX = FlareData.rank(flareClass) >= 4;
        String title = (isX ? "X-CLASS SOLAR FLARE" : "M-CLASS SOLAR FLARE");
        String text = flareClass + " flare — peak " + FlareData.prettyTime(isoTime);

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_flare)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi);

        // Sound for pre-Oreo (channel carries it on 26+).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            b.setSound(soundUri(ctx));
            b.setVibrate(new long[]{0, 400, 200, 400});
        }

        if (bitmap != null) {
            b.setLargeIcon(bitmap);
            b.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon((Bitmap) null)
                    .setSummaryText(text));
        } else {
            b.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        }

        try {
            NotificationManagerCompat.from(ctx).notify(NOTIF_ID, b.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS not granted; silently skip.
        }
    }
}
