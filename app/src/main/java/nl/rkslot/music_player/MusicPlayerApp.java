package nl.rkslot.music_player;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicPlayerApp extends Application {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private SyncTask syncTask;

    public void startSyncTask(SyncTask.Callbacks callbacks, String username, String password, Uri baseDirectoryUri) {
        this.syncTask = new SyncTask(this, callbacks, username, password, baseDirectoryUri);
        this.executorService.submit(this.syncTask);
    }

    public @Nullable SyncTask getSyncTask() {
        return this.syncTask;
    }
}
