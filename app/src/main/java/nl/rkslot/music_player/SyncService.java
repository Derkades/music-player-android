package nl.rkslot.music_player;

import static android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID;
import static android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED;
import static android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE;
import static android.provider.DocumentsContract.Document.MIME_TYPE_DIR;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nl.rkslot.music_player.api.Playlist;
import nl.rkslot.music_player.api.RequestHandler;
import nl.rkslot.music_player.api.Track;

public class SyncService extends Service {

    private String username;
    private String password;
    private Uri baseDirectoryUri;

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    private int progress;
    private int progressMax;

    private long previousNotificationTime = 0;
    private String previousNotificationDescription = null;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // TODO split up sync into multiple messages (one for every file to be downloaded), so it can be cancelled partway through
            try {
                final RequestHandler requestHandler = new RequestHandler();
                requestHandler.login(username, password);
                final List<Playlist> playlists = requestHandler.getPlaylists();

                final Collection<Track> tracks = playlists.stream()
                        .filter(Playlist::isFavorite)
                        .flatMap(p -> p.getTracks().stream())
                        .collect(Collectors.toSet());

                progressMax = tracks.size();

                scanDirectory("", tracks, 0);

                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Notification getNotification(String description) {
        String title = this.progress != 0 ? ("Downloading " + this.progress + "/" + this.progressMax) : "Preparing download";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MusicPlayerApp.NOTIFICATION_CHANNEL_SYNC_PROGRESS);
        builder.setContentTitle(title)
                .setContentText(description)
                .setSmallIcon(R.drawable.music);

        builder.setProgress(this.progressMax, this.progress, this.progressMax == 0);
        builder.setOngoing(true);

        Intent cancelIntent = new Intent(this, NotificationCancelReceiver.class);
        PendingIntent cancelPendingIntent =
                PendingIntent.getBroadcast(this, 0, cancelIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.music, "Nutteloze knop", cancelPendingIntent);

        return builder.build();
    }

    private void updateProgress(String description) {
        progress++;
        if (System.currentTimeMillis() - previousNotificationTime < 500 && description.equals(previousNotificationDescription)) {
            return;
        }
        previousNotificationTime = System.currentTimeMillis();
        previousNotificationDescription = description;
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        Notification notification = getNotification(description);
        notificationManager.notify(MusicPlayerApp.NOTIFICATION_ID_SYNC_PROGRESS, notification);
    }

    private String uriName(Uri uri) {
        String[] split = uri.getLastPathSegment().split("/");
        return split[split.length - 1];
    }

    private void scanDirectory(String path,
                               Collection<Track> tracksInCurrentDirectoryAndChildren,
                               int level) throws IOException {
        Log.i("SyncService", "Scanning directory: " + path);

        // Organize files by base directory

        // Map of directory names and the files that go inside that directory tree
        Multimap<String, Track> directoriesToCreateAndScan = MultimapBuilder.hashKeys().arrayListValues().build();
        // Map of device file names and the tracks they correspond to
        Map<String, Track> filesToCreateAndDownload = new HashMap<>();

        for (Track track : tracksInCurrentDirectoryAndChildren) {
            if (track.getSplitPath().length == level + 1) {
                String deviceFileName = track.getLocalFileName();
                // File should be created in current directory
                filesToCreateAndDownload.put(deviceFileName, track);
                continue;
            }
            directoriesToCreateAndScan.put(track.getSplitPath()[level], track);
        }

        String parentId = DocumentsContract.getDocumentId(baseDirectoryUri) + "/" + path;
        Uri queryUri = DocumentsContract.buildChildDocumentsUriUsingTree(baseDirectoryUri, parentId);
        final String[] projection = new String[] {COLUMN_DOCUMENT_ID, COLUMN_LAST_MODIFIED, COLUMN_MIME_TYPE};
        try (Cursor cursor = this.getContentResolver().query(queryUri, projection, null, null, null)) {
            while (cursor.moveToNext()) {
                String documentId = cursor.getString(0);
                Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(baseDirectoryUri, documentId);
                String name = uriName(documentUri);
                long lastModified = cursor.getLong(1) / 1000;
                boolean isDirectory = cursor.getString(2).equals(MIME_TYPE_DIR);

                if (directoriesToCreateAndScan.containsKey(name)) {
                    // This directory entry is in the map. That means
                    // that it is a directory on the server, and should be a directory on the device.
                    if (isDirectory) {
                        // Directory on server also exists on the device. Also scan this directory.
                        Collection<Track> tracksInSubdirectory = directoriesToCreateAndScan.removeAll(name);
                        scanDirectory(path + "/" + name, tracksInSubdirectory, level + 1);
                        continue;
                    }
                } else if (filesToCreateAndDownload.containsKey(name)) {
                    Track track = filesToCreateAndDownload.get(name);

                    // Is it a file, and up to date?
                    if (!isDirectory && lastModified >= track.getModificationTime()) {
                        // File is up to date
                        filesToCreateAndDownload.remove(name); // File does not need to be downloaded
                        updateProgress("");
                        continue;
                    }
                }

                Log.v("SyncService", "Delete file or directory: " + name);
                DocumentsContract.deleteDocument(this.getContentResolver(), documentUri);
            }
        }

        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(baseDirectoryUri, parentId);

        // Download missing files
        for (Map.Entry<String, Track> entry : filesToCreateAndDownload.entrySet()) {
            // File is missing and needs to be created and downloaded.
            String name = entry.getKey();
            Track track = entry.getValue();
            Log.v("SyncService", "Download file: " + name);

            updateProgress("Downloading: " + name);
            byte[] fileContents = track.downloadAudio();
            // Only create the empty file after we know for sure the audio has been downloaded successfully
            Uri uri = DocumentsContract.createDocument(this.getContentResolver(), parentUri, "audio/mp3", name);
            try (OutputStream output = this.getContentResolver().openOutputStream(uri)) {
                output.write(fileContents);
            }
        }

        // Create missing directories, and download missing files inside those directories
        for (String name : directoriesToCreateAndScan.keySet()) {
            // Directory does not exist yet, we need to create it
            Log.v("SyncService", "Create directory: " + name);

            DocumentsContract.createDocument(this.getContentResolver(), parentUri, MIME_TYPE_DIR, name);

            Collection<Track> tracksInSubDir = directoriesToCreateAndScan.get(name);
            scanDirectory(path + "/" + name, tracksInSubDir, level + 1);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.v("SyncService", "onCreate");
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        HandlerThread thread = new HandlerThread("SyncService", Process.THREAD_PRIORITY_LESS_FAVORABLE);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        Log.v("SyncService", "onDestroy");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("SyncService", "onStartCommand");
        this.progress = 0;
        this.progressMax = 0;
        Notification notification = getNotification("Retrieving track list");
        this.startForeground(MusicPlayerApp.ID_FOREGROUND_SERVICE_SYNC, notification);

        this.username = intent.getStringExtra("username");
        this.password = intent.getStringExtra("password");
        Uri uri = intent.getParcelableExtra("directory_uri");
        this.baseDirectoryUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

}
