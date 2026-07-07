package net.ki4kao.spaceweatheralert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String DASHBOARD_URL = "https://ki4kao.net/app1/index.html";
    private static final int REQ_NOTIF = 55;

    private WebView web;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        NotificationHelper.ensureChannel(getApplicationContext());
        AlarmScheduler.scheduleNext(getApplicationContext());
        requestNotifPermissionIfNeeded();

        web = findViewById(R.id.webview);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.setWebViewClient(new WebViewClient());
        web.loadUrl(DASHBOARD_URL);
    }

    private void requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem t = menu.findItem(R.id.action_toggle_notif);
        boolean on = Prefs.notificationsEnabled(this);
        t.setChecked(on);
        t.setTitle(on ? "Notifications: On" : "Notifications: Off");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_toggle_notif) {
            boolean now = !Prefs.notificationsEnabled(this);
            Prefs.setNotificationsEnabled(this, now);
            if (now) {
                requestNotifPermissionIfNeeded();
                AlarmScheduler.scheduleNext(getApplicationContext());
            }
            Toast.makeText(this, now ? "Flare notifications enabled"
                    : "Flare notifications disabled", Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();
            return true;
        } else if (id == R.id.action_test) {
            WidgetUpdater.testNotification(getApplicationContext());
            Toast.makeText(this, "Sending test notification…", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_refresh) {
            if (web != null) web.reload();
            WidgetUpdater.runCycle(getApplicationContext(), null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF && grantResults.length > 0
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notifications are off in system settings", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
