package nl.rkslot.music_player;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import nl.rkslot.music_player.api.Playlist;
import nl.rkslot.music_player.api.RequestHandler;
import nl.rkslot.music_player.api.Track;

public class SyncTask implements Runnable {

    private static final String MUSIC_BASE_DIR = Environment.DIRECTORY_MUSIC + "/MusicPlayer/";

    interface Callbacks {

        void onStart();
        void setProgress(int progress, int maxProgress);
        void log(CharSequence message);
        void onFinish();

    }

    private final @NonNull Context context;
    private @Nullable Callbacks callbacks;
    private final @NonNull String username;
    private final @NonNull String password;
    private final @NonNull DocumentFile baseDirectory;

    SyncTask(final @NonNull Context applicationContext,
             final @Nullable Callbacks callbacks,
             final @NonNull String username,
             final @NonNull String password,
             final @NonNull Uri baseDirectoryUri) {
        this.context = applicationContext;
        this.callbacks = callbacks;
        this.username = username;
        this.password = password;
        this.baseDirectory = DocumentFile.fromTreeUri(this.context, baseDirectoryUri);
    }

    public void setCallbacks(final @Nullable Callbacks callbacks) {
        this.callbacks = callbacks;
    }

//    private void storeTrack(final Track track) throws IOException {
//        final String androidPath;
//        final String fileName;
//        {
//            String[] splitPath = track.getPath().split("/");
//            String directoryPath = String.join("/", Arrays.copyOfRange(splitPath, 0, splitPath.length - 1));
//            androidPath = MUSIC_BASE_DIR + directoryPath + '/';
//            fileName = splitPath[splitPath.length - 1] + ".mp3"; // TODO remove original extension
//        }
//
//        final ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.Audio.AudioColumns.IS_DOWNLOAD, true);
//        contentValues.put(MediaStore.Audio.AudioColumns.IS_DRM, false);
//        contentValues.put(MediaStore.Audio.AudioColumns.IS_MUSIC, true);
//        contentValues.put(MediaStore.Audio.AudioColumns.DURATION, track.getDuration());
//        if (track.getTags().size() > 0) {
//            contentValues.put(MediaStore.Audio.AudioColumns.GENRE, track.getTags().get(0));
//        }
//        if (track.getTitle() != null) {
//            contentValues.put(MediaStore.Audio.AudioColumns.TITLE, track.getTitle());
//        }
//        if (track.getArtists() != null) {
//            contentValues.put(MediaStore.Audio.AudioColumns.ARTIST, String.join("; ", track.getArtists()));
//        }
//        if (track.getAlbum() != null) {
//            contentValues.put(MediaStore.Audio.AudioColumns.ALBUM, track.getAlbum());
//        }
//        if (track.getTrackNumber() != null) {
//            contentValues.put(MediaStore.Audio.AudioColumns.CD_TRACK_NUMBER, track.getTrackNumber());
//        }
//        if (track.getYear() != null) {
//            contentValues.put(MediaStore.Audio.AudioColumns.YEAR, track.getYear());
//        }
//
//        final ContentResolver contentResolver = this.context.getContentResolver();
//
//        // Determine whether file already exists
//
//        final String[] projection = new String[]{MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.DATE_MODIFIED};
//        final String selection = MediaStore.Audio.AudioColumns.RELATIVE_PATH + " = ? AND " + MediaStore.Audio.AudioColumns.DISPLAY_NAME + " = ?";
//        final String[] selectionArgs = new String[]{androidPath, fileName};
//        try (Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)) {
//            if (cursor.moveToNext()) {
//                // It exists already! Only update content values.
//                final long id = cursor.getLong(0);
//
//                long localModificationTime = cursor.getLong(1);
//                // According Android documentation, DATE_MODIFIED is in milliseconds. Though, my
//                // Pixel 6a returns seconds? Apparently it differs depending on the device!
//                // https://developer.android.com/reference/android/provider/MediaStore.MediaColumns#DATE_MODIFIED
//                // https://stackoverflow.com/a/44846386/4833737
//                // Assume all timestamps above 946684800000 millis (2000-01-01T00:00Z) are in milliseconds.
//                if (localModificationTime > 946684800000L) {
//                    localModificationTime /= 1000;
//                }
//
//                if (localModificationTime >= track.getModificationTime()) {
//                    if (this.callbacks != null) {
//                        this.callbacks.log("Up to date: " + track.getDisplay());
//                    }
//                    return;
//                }
//
//                final String where = MediaStore.Audio.AudioColumns._ID + " = ?";
//                final String[] whereArgs = new String[]{String.valueOf(id)};
//                contentResolver.update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues, where, whereArgs);
//
//                if (this.callbacks != null) {
//                    this.callbacks.log("Replacing: " + track.getDisplay());
//                }
//
//                final Uri fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
//                try (OutputStream output = contentResolver.openOutputStream(fileUri)) {
//                    track.downloadAudio(output);
//                }
//
//                return;
//            }
//        }
//
//        // Let Android know where to store the file
//
//        contentValues.put(MediaStore.Audio.AudioColumns.RELATIVE_PATH, androidPath);
//        contentValues.put(MediaStore.Audio.AudioColumns.DISPLAY_NAME, fileName);
//
//        final Uri fileUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
//
//        if (this.callbacks != null) {
//            this.callbacks.log("Downloading: " + track.getDisplay());
//        }
//
//        try (OutputStream output = contentResolver.openOutputStream(fileUri)) {
//            track.downloadAudio(output);
//        }
//    }

//    private void deleteOldTracks(final List<Track> tracksToKeep) {
//        final Set<String> trackPathsToKeep = tracksToKeep.stream()
//                .map(Track::getPath)
//                .collect(Collectors.toSet());
//
//        final ContentResolver contentResolver = this.context.getContentResolver();
//
//        final String[] projection = new String[]{MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.RELATIVE_PATH, MediaStore.Audio.AudioColumns.DISPLAY_NAME};
//        try (Cursor cursor = contentResolver.query(this.baseDirectoryUri, projection, null, null, null)) {
//            while (cursor.moveToNext()) {
//                final long id = cursor.getLong(0);
//                final String relativePath = cursor.getString(1);
//                final String displayName = cursor.getString(2);
//
//                System.out.println(relativePath + displayName);
//
//                if (!relativePath.startsWith(MUSIC_BASE_DIR) && this.callbacks != null) {
//                    this.callbacks.log("Not ours: " + relativePath + displayName);
//                }
//
//                if (relativePath.length() >= MUSIC_BASE_DIR.length()) {
//                    String trackPath = relativePath.substring(MUSIC_BASE_DIR.length()) + displayName.substring(0, displayName.length() - 4);
//
//                    if (trackPathsToKeep.contains(trackPath)) {
////                        if (this.callbacks != null) {
////                            this.callbacks.log("Keeping: " + relativePath + displayName);
////                        }
//                        continue;
//                    }
//                }
//
//                if (this.callbacks != null) {
//                    this.callbacks.log("Deleting: " + relativePath + displayName);
//                }
//
//                final Uri fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
//                contentResolver.delete(fileUri, null);
//            }
//        }
//    }

    private void downloadToFile(Track track, DocumentFile musicFile) {
        try {
            try (OutputStream output = this.context.getContentResolver().openOutputStream(musicFile.getUri())) {
                track.downloadAudio(output);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void log(String message) {
        if (this.callbacks != null) {
            this.callbacks.log(message);
        }
    }

    private void log(String message, DocumentFile file) {
        if (this.callbacks != null) {
            String filePath = file.getUri().getPath();
            String basePath = this.baseDirectory.getUri().getPath();
            this.callbacks.log(message + ": " + filePath.substring(basePath.length() + 1));
        }
    }

    private void downloadMissing(DocumentFile currentDirectory,
                                 Collection<Track> tracksInCurrentDirectoryAndChildren,
                                 int level,
                                 Runnable onProgress) {
        // Organize files by base directory

        // Map of directory names and the files that go inside that directory tree
        Multimap<String, Track> directoriesToCreateAndScan = MultimapBuilder.hashKeys().arrayListValues().build();

        // Map of device file names and the tracks they correspond to
        Map<String, Track> filesToCreateAndDownload = new HashMap<>();

        for (Track track : tracksInCurrentDirectoryAndChildren) {
            if (track.getSplitPath().length == level + 1) {
                String deviceFileName = track.getDisplay() + ".mp3";
                // File should be created in current directory
                filesToCreateAndDownload.put(deviceFileName, track);
                continue;
            }
            directoriesToCreateAndScan.put(track.getSplitPath()[level], track);
        }

        // Existing directory entries. Any files or directories in this set do not need to
        // be downloaded, created or scanned.
        Set<String> existingEntryNames = new HashSet<>();

        for (DocumentFile entry : currentDirectory.listFiles()) {
            if (directoriesToCreateAndScan.containsKey(entry.getName())) {
                // This directory entry is in the map. That means
                // that it is a directory on the server, and should be a directory on the device.
                if (entry.isFile()) {
                    // It's a file on the device. Delete it. A new directory will be created later.
                    log("Delete file (should be directory)", entry);
                    entry.delete();
                    continue;
                }

                // Directory on server also exists on the device. Also scan this directory.
                log("Check directory", entry);
                Collection<Track> tracksInSubdirectory = directoriesToCreateAndScan.get(entry.getName());
                downloadMissing(entry, tracksInSubdirectory, level + 1, onProgress);

                existingEntryNames.add(entry.getName()); // Does not need to be created and scanned later
                continue;
            }

            if (entry.isDirectory()) {
                // Entry is a directory, but not in the 'tracksByDirectory' map, so it should
                // not exist. Delete it.
                log("Delete directory", entry);
                entry.delete();
                continue;
            }

            // Directory entry turns out to be a file. Should it exist?

            Track track = filesToCreateAndDownload.get(entry.getName());
            if (track == null) {
                // No, file should not exist
                log("Delete file", entry);
                entry.delete();
                continue;
            }

            // Is file up to date?
            if (entry.lastModified() > track.getModificationTime() * 1000) {
                // File is up to date
                onProgress.run();

                existingEntryNames.add(entry.getName()); // File does not need to be downloaded
                continue;
            }

            // File has been changed on server needs to be re-downloaded
            log("Replace", entry);
            onProgress.run();
            existingEntryNames.add(entry.getName()); // File does not need to be downloaded
        }

        // Download missing files
        for (String deviceFileName : filesToCreateAndDownload.keySet()) {
            if (existingEntryNames.contains(deviceFileName)) {
                // File already exists and is up-to-date (ensured by earlier code)
                continue;
            }

            // File is missing and needs to be created and downloaded.
            Track track = filesToCreateAndDownload.get(deviceFileName);
            DocumentFile file = currentDirectory.createFile("audio/mp3", deviceFileName);
            log("Download", file);
            downloadToFile(track, file);
            onProgress.run();
        }

        // Create missing directories, and download missing files inside those directories
        for (String directoryName : directoriesToCreateAndScan.keySet()) {
            if (existingEntryNames.contains(directoryName)) {
                // Directory already exists and has already been scanned by the code above
                continue;
            }

            // Directory does not exist yet, we need to create it
            DocumentFile directory = currentDirectory.createDirectory(directoryName);

            // Explore this subdirectory
            Collection<Track> tracksInSubdirectory = directoriesToCreateAndScan.get(directoryName);
            log("Created directory, now check", directory);
            downloadMissing(directory, tracksInSubdirectory, level + 1, onProgress);
        }
    }

    @Override
    public void run() {
        try {
            if (this.callbacks != null) {
                this.callbacks.onStart();
                this.callbacks.log("Syncing...");
            }

            System.out.println();

            final RequestHandler requestHandler = new RequestHandler();
            requestHandler.login(username, password);
            final List<Playlist> playlists = requestHandler.getPlaylists();

            final List<Track> tracks = playlists.stream()
                    .filter(Playlist::isFavorite)
                    .flatMap(p -> p.getTracks().stream())
                    .collect(Collectors.toList());

            Runnable onProgress = new Runnable() {
                private int progress = 0;
                @Override
                public void run() {
                    progress++;
                    if (SyncTask.this.callbacks != null) {
                        SyncTask.this.callbacks.setProgress(progress, tracks.size() - 1);
                    }
                }
            };

            downloadMissing(this.baseDirectory, tracks, 0, onProgress);

            this.callbacks.log("Done!");
        } catch (Exception e) {
            e.printStackTrace();
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            if (this.callbacks != null) {
                callbacks.log("ERROR! " + sw);
            }
        }

        if (this.callbacks != null) {
            callbacks.onFinish();
        }
    }

}
