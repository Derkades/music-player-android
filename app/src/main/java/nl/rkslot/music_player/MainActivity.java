package nl.rkslot.music_player;

import android.app.Activity;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    private final ActivityResultLauncher<Intent> directorySelected = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                System.out.println("Done!");
                System.out.println(result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    System.out.println(data.getData().toString());

                    Uri baseDirectoryUri = data.getData();




                }
            });


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

    private SyncTask.Callbacks getSyncTaskCallbacks() {
        final ProgressBar progressBar = findViewById(R.id.syncProgressBar);
        final TextView textView = findViewById(R.id.log);
        final Button syncButton = findViewById(R.id.sync);

        return new SyncTask.Callbacks() {

            @Override
            public void onStart() {
                syncButton.post(() -> syncButton.setEnabled(false));
                progressBar.post(() -> {
                    progressBar.setIndeterminate(true);
                });
            }

            @Override
            public void setProgress(int progress, int maxProgress) {
                progressBar.post(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(progress);
                    progressBar.setMax(maxProgress);
                });
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