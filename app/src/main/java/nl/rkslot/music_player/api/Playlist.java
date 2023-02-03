package nl.rkslot.music_player.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Playlist {

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

}
