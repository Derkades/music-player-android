package nl.rkslot.music_player.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Playlist implements Parcelable {

    private final RequestHandler requestHandler;
    private final String name;
    private final int trackCount;
    private final boolean favorite;
    private final boolean writable;
    private final List<Track> tracks;

    Playlist(RequestHandler requestHandler, JsonObject json) {
        this.requestHandler = requestHandler;

        this.name = json.get("name").getAsString();
        this.trackCount = json.get("track_count").getAsInt();
        this.favorite = json.get("favorite").getAsBoolean();
        this.writable = json.get("write").getAsBoolean();

        this.tracks = new ArrayList<>();
        for (JsonElement elem : json.getAsJsonArray("tracks")) {
            this.tracks.add(new Track(this, this.requestHandler, elem.getAsJsonObject()));
        }
    }

    protected Playlist(Parcel in) {
        requestHandler = in.readParcelable(RequestHandler.class.getClassLoader());
        name = in.readString();
        trackCount = in.readInt();
        favorite = in.readByte() != 0;
        writable = in.readByte() != 0;
        tracks = in.createTypedArrayList(Track.CREATOR);
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    public String getName() {
        return this.name;
    }

    public int getTrackCount() {
        return this.trackCount;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public boolean isWritable() {
        return this.writable;
    }

    public List<Track> getTracks() {
        return this.tracks;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(requestHandler, flags);
        dest.writeString(name);
        dest.writeInt(trackCount);
        dest.writeByte((byte) (favorite ? 1 : 0));
        dest.writeByte((byte) (writable ? 1 : 0));
        dest.writeTypedList(tracks);
    }

}
