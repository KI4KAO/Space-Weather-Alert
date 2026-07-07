package net.ki4kao.spaceweatheralert;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Fetches and parses the GOES primary X-ray feeds from NOAA SWPC.
 *  - current flux class      -> xray-flares-latest.json (current_class)
 *  - last M-or-higher flare  -> xray-flares-7-day.json (scan for max_class >= M)
 */
public class FlareData {

    public static final String URL_LATEST =
            "https://services.swpc.noaa.gov/json/goes/primary/xray-flares-latest.json";
    public static final String URL_7DAY =
            "https://services.swpc.noaa.gov/json/goes/primary/xray-flares-7-day.json";
    public static final String SDO_512 =
            "https://sdo.gsfc.nasa.gov/assets/img/latest/latest_512_0131.jpg";

    /** Result of one fetch cycle. */
    public static class Result {
        public String currentClass = "--";
        public String currentFlux = "";
        public int satellite = 0;

        public boolean hasMFlare = false;
        public String mClass = "";      // e.g. "M1.8" or "X2.1"
        public String mTime = "";       // peak/max time (ISO)
        public String mKey = "";        // stable id for dedup (class + time)
        public boolean ok = false;
    }

    private static final String[] RANK = {"A", "B", "C", "M", "X"};

    public static int rank(String cls) {
        if (cls == null || cls.isEmpty()) return -1;
        char c = Character.toUpperCase(cls.charAt(0));
        for (int i = 0; i < RANK.length; i++) if (RANK[i].charAt(0) == c) return i;
        return -1;
    }

    public static Result fetchAll() {
        Result r = new Result();
        try {
            // ---- current flux from latest feed ----
            String latest = httpGet(URL_LATEST);
            if (latest != null) {
                JSONArray a = new JSONArray(latest);
                if (a.length() > 0) {
                    JSONObject o = a.getJSONObject(0);
                    r.currentClass = o.optString("current_class",
                            o.optString("max_class", "--"));
                    if (o.has("current_int_xrlong") && !o.isNull("current_int_xrlong")) {
                        r.currentFlux = String.format(Locale.US, "%.2e",
                                o.optDouble("current_int_xrlong"));
                    }
                    r.satellite = o.optInt("satellite", 0);

                    // the latest feed's own flare may already be M+
                    considerFlare(r, o);
                }
            }

            // ---- last M+ scan over 7-day feed ----
            String week = httpGet(URL_7DAY);
            if (week != null) {
                JSONArray a = new JSONArray(week);
                for (int i = 0; i < a.length(); i++) {
                    considerFlare(r, a.getJSONObject(i));
                }
            }
            r.ok = true;
        } catch (Exception e) {
            r.ok = false;
        }
        return r;
    }

    /** Keep the most recent M-or-higher flare seen so far. */
    private static void considerFlare(Result r, JSONObject o) {
        String cls = o.optString("max_class", "");
        if (rank(cls) < 3) return; // below M
        String t = o.optString("max_time", o.optString("begin_time", ""));
        if (t.isEmpty()) return;
        if (!r.hasMFlare || newer(t, r.mTime)) {
            r.hasMFlare = true;
            r.mClass = cls;
            r.mTime = t;
            r.mKey = cls + "@" + t;
        }
    }

    private static boolean newer(String a, String b) {
        long ta = parse(a), tb = parse(b);
        return ta > tb;
    }

    private static long parse(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        String s = iso.replace("T", " ").replace("Z", "").trim();
        String[] fmts = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"};
        for (String f : fmts) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(s);
                if (d != null) return d.getTime();
            } catch (Exception ignored) { }
        }
        return 0;
    }

    /** Human-friendly UTC time like "2026-07-07 14:32Z". */
    public static String prettyTime(String iso) {
        long t = parse(iso);
        if (t == 0) return iso == null ? "" : iso;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(t)) + "Z";
    }

    private static String httpGet(String urlStr) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("User-Agent", "KI4KAO-SpaceWeatherAlerts/1.0");
            c.setConnectTimeout(12000);
            c.setReadTimeout(15000);
            c.connect();
            if (c.getResponseCode() != 200) return null;
            InputStream is = c.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
            is.close();
            return bos.toString("UTF-8");
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    /** Download a bitmap (used for the SDO flare image in the notification). */
    public static Bitmap fetchBitmap(String urlStr) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(urlStr);
            c = (HttpURLConnection) url.openConnection();
            c.setRequestProperty("User-Agent", "KI4KAO-SpaceWeatherAlerts/1.0");
            c.setRequestProperty("Referer", "https://sdo.gsfc.nasa.gov/");
            c.setConnectTimeout(12000);
            c.setReadTimeout(15000);
            c.connect();
            if (c.getResponseCode() != 200) return null;
            InputStream is = c.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
