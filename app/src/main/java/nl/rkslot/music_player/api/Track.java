package nl.rkslot.music_player.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Track implements Parcelable {

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
        this.year = JsonHelper.getNullableInteger(json, "year");
    }

    protected Track(Parcel in) {
        playlist = in.readParcelable(Playlist.class.getClassLoader());
        requestHandler = in.readParcelable(RequestHandler.class.getClassLoader());
        path = in.readString();
        splitPath = in.createStringArray();
        mtime = in.readLong();
        display = in.readString();
        duration = in.readInt();
        tags = in.createStringArrayList();
        title = in.readString();
        artists = in.createStringArrayList();
        album = in.readString();
        albumArtist = in.readString();
        if (in.readByte() == 0) {
            year = null;
        } else {
            year = in.readInt();
        }
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

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

    private static final Set<Character> ILLEGAL_CHARS = new HashSet<>(Arrays.asList('|', '\\', '?', '*', '<', '"', ':', '>', '+', '[', ']', '/', '\''));

    public String getLocalFileName() {
        StringBuilder name = new StringBuilder();
        for (char c : this.display.toCharArray()) {
            if (!ILLEGAL_CHARS.contains(c)) {
                name.append(c);
            }
        }
        name.append(".mp3");
        return name.toString();
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
    public Integer getYear() {
        return this.year;
    }

    public void downloadAudioToStream(OutputStream output) throws IOException {
        String encodedPath = URLEncoder.encode(this.getPath(), StandardCharsets.UTF_8.toString());
        HttpURLConnection conn = this.requestHandler.openConnection("get_track?path=" + encodedPath + "&type=mp3_with_metadata");
        this.requestHandler.readResponseToOutputStream(conn, output);
    }

    public byte[] downloadAudio() throws IOException {
        String encodedPath = URLEncoder.encode(this.getPath(), StandardCharsets.UTF_8.toString());
        HttpURLConnection conn = this.requestHandler.openConnection("get_track?path=" + encodedPath + "&type=mp3_with_metadata");
        return this.requestHandler.readResponseToBytes(conn);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(playlist, 0);
        dest.writeParcelable(requestHandler, 0);
        dest.writeString(path);
        dest.writeStringArray(splitPath);
        dest.writeLong(mtime);
        dest.writeString(display);
        dest.writeInt(duration);
        dest.writeStringList(tags);
        dest.writeString(title);
        dest.writeStringList(artists);
        dest.writeString(album);
        dest.writeString(albumArtist);
        if (year == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(year);
        }
    }
}
