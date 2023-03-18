package nl.rkslot.music_player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerNotificationChannels();
    }

    @Override
    public void onSaveInstanceState(final Bundle bundle) {
        super.onSaveInstanceState(bundle);

        final Button syncButton = findViewById(R.id.sync);
        bundle.putBoolean("syncButtonEnabled", syncButton.isEnabled());
    }

    @Override
    public void onRestoreInstanceState(final Bundle bundle) {
        super.onRestoreInstanceState(bundle);

        final Button syncButton = findViewById(R.id.sync);
        syncButton.setEnabled(bundle.getBoolean("syncButtonEnabled"));
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private MusicPlayerApp getApp() {
        return (MusicPlayerApp) this.getApplicationContext();
    }

    public void onSettingsButtonClick(final View view) {
        final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onSyncButtonClick(final View view) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String username = prefs.getString("username", "");
        final String password = prefs.getString("password", "");
        final String directory = prefs.getString("directory", "");

        if (username.isEmpty()) {
            Toast.makeText(this, "Username not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Password not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        if (directory.isEmpty()) {
            Toast.makeText(this, "Directory not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(directory);
//        this.getApp().startSyncTask(this.getSyncTaskCallbacks(), username, password, uri);
        Intent intent = new Intent(this, SyncService.class);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("directory_uri", uri);
        this.getApp().startForegroundService(intent);
    }

    private void registerNotificationChannels() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
            if (!channel.getId().equals(MusicPlayerApp.NOTIFICATION_CHANNEL_SYNC_PROGRESS)) {
                notificationManager.deleteNotificationChannel(channel.getId());
            }
        }

        NotificationChannel channel = new NotificationChannel(MusicPlayerApp.NOTIFICATION_CHANNEL_SYNC_PROGRESS, "Sync progress", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

}