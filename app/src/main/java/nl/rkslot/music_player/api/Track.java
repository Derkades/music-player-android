package nl.rkslot.music_player.api;

import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Track {

    private final RequestHandler requestHandler;
    private final Playlist playlist;
    private final String path;
    private final String[] splitPath;
    private final long mtime;
    private final String display;
    private final int duration;
    private final List<String> tags;
    private final @Nullable String title;
    private final @Nullable List<String> artists;
    private final @Nullable String album;
    private final @Nullable String albumArtist;
    private final @Nullable Integer trackNumber;
    private final @Nullable Integer year;

    Track(Playlist playlist, RequestHandler requestHandler, JsonObject json) {
        this.playlist = playlist;
        this.requestHandler = requestHandler;

        this.path = json.get("path").getAsString();
        this.splitPath = path.split("/");
        this.mtime = json.get("mtime").getAsLong();
        this.display = json.get("display").getAsString();
        this.duration = json.get("duration").getAsInt();
        this.tags = JsonHelper.getStringList(json.getAsJsonArray("tags"));
        this.title = JsonHelper.getNullableString(json, "title");
        this.artists = JsonHelper.getNullableStringList(json, "artists");
        this.album = JsonHelper.getNullableString(json, "album");
        this.albumArtist = JsonHelper.getNullableString(json, "album_artist");
        this.trackNumber = JsonHelper.getNullableInteger(json, "track_number");
        this.year = JsonHelper.getNullableInteger(json, "year");
    }

    public Playlist getPlaylist() {
        return this.playlist;
    }

    public String getPath() {
        return this.path;
    }

    public String[] getSplitPath() {
        return this.splitPath;
    }

    public long getModificationTime() {
        return this.mtime;
    }

    public String getDisplay() {
        return this.display;
    }

    public int getDuration() {
        return this.duration;
    }

    public List<String> getTags() {
        return this.tags;
    }

    @Nullable
    public String getTitle() {
        return this.title;
    }

    @Nullable
    public List<String> getArtists() {
        return this.artists;
    }

    @Nullable
    public String getAlbum() {
        return this.album;
    }

    @Nullable
    public String getAlbumArtist() {
        return this.albumArtist;
    }

    @Nullable
    public Integer getTrackNumber() {
        return this.trackNumber;
    }

    @Nullable
    public Integer getYear() {
        return this.year;
    }

    public void downloadAudio(OutputStream output) throws IOException {
        String encodedPath = URLEncoder.encode(this.getPath(), StandardCharsets.UTF_8.toString());
        HttpURLConnection conn = this.requestHandler.openConnection("get_track?path=" + encodedPath + "&type=mp3_with_metadata");
        this.requestHandler.readResponseToOutputStream(conn, output);
    }

}
