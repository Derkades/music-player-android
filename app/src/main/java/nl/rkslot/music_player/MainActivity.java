package nl.rkslot.music_player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
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

        final TextView logTextView = findViewById(R.id.log);
        bundle.putCharSequence("logText", logTextView.getText());

        final ProgressBar progressBar = findViewById(R.id.syncProgressBar);
        bundle.putBoolean("progressBarIndeterminate", progressBar.isIndeterminate());
        bundle.putInt("progressBarProgress", progressBar.getProgress());
        bundle.putInt("progressBarMax", progressBar.getMax());
    }

    @Override
    public void onRestoreInstanceState(final Bundle bundle) {
        super.onRestoreInstanceState(bundle);

        final Button syncButton = findViewById(R.id.sync);
        syncButton.setEnabled(bundle.getBoolean("syncButtonEnabled"));

        final TextView logTextView = findViewById(R.id.log);
        logTextView.setText(bundle.getCharSequence("logText"));

        final ProgressBar progressBar = findViewById(R.id.syncProgressBar);
        progressBar.setIndeterminate(bundle.getBoolean("progressBarIndeterminate"));
        progressBar.setProgress(bundle.getInt("progressBarProgress"));
        progressBar.setMax(bundle.getInt("progressBarMax"));
    }

    @Override
    public void onStop() {
        super.onStop();

        final SyncTask task = this.getApp().getSyncTask();
        if (task != null) {
            task.setCallbacks(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final SyncTask task = this.getApp().getSyncTask();
        if (task != null) {
            task.setCallbacks(this.getSyncTaskCallbacks());
        }
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
        this.getApp().startSyncTask(this.getSyncTaskCallbacks(), username, password, uri);
    }

    private static final String NOTIFICATION_ID_SYNC_PROGRESS = "sync_progress";

    private void registerNotificationChannels() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
            if (!channel.getId().equals(NOTIFICATION_ID_SYNC_PROGRESS)) {
                notificationManager.deleteNotificationChannel(channel.getId());
            }
        }

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_ID_SYNC_PROGRESS, "Sync progress", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(channel);
    }

    private SyncTask.Callbacks getSyncTaskCallbacks() {
        final ProgressBar progressBar = findViewById(R.id.syncProgressBar);
        final TextView textView = findViewById(R.id.log);
        final Button syncButton = findViewById(R.id.sync);

        return new SyncTask.Callbacks() {

            private int previousProgress = -1;

            private void sendNotification(int progress, int progressMax, boolean indeterminate) {
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, NOTIFICATION_ID_SYNC_PROGRESS);
                builder.setContentTitle("Downloading")
                        .setContentText("Synchronising music from music player")
                        .setSmallIcon(R.drawable.music);

                // Issue the initial notification with zero progress
                builder.setProgress(progressMax, progress, indeterminate);
                notificationManager.notify(1, builder.build());
            }

            @Override
            public void onStart() {
                syncButton.post(() -> syncButton.setEnabled(false));
                progressBar.post(() -> {
                    progressBar.setIndeterminate(true);
                });

                runOnUiThread(() -> {
                    sendNotification(0, 0, true);
                });
            }

            @Override
            public void setProgress(int progress, int progressMax) {
                progressBar.post(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(progress);
                    progressBar.setMax(progressMax);
                });

                if (progress != this.previousProgress) {
                    runOnUiThread(() -> {
                        sendNotification(progress, progressMax, false);
                    });
                    this.previousProgress = progress;
                }
            }

            @Override
            public void log(CharSequence message) {
                System.out.println(message);
                textView.post(() -> {
                    ScrollView scrollView = findViewById(R.id.scrollView2);
                    textView.append(message);
                    textView.append("\n");
                    scrollView.fullScroll(View.FOCUS_DOWN);
                });
            }

            @Override
            public void onFinish() {
                syncButton.post(() -> syncButton.setEnabled(true));
                progressBar.post(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(0);
                });
            }
        };
    }

}