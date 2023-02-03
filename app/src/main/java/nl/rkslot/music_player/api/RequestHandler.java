package nl.rkslot.music_player.api;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RequestHandler {
    private static final String BASE_URL = "https://music.rkslot.nl/";
//    private static final String BASE_URL = "http://10.0.1.3:8080/";
    private static final String USER_AGENT = "Music-Player-Android";

    private @Nullable String authToken;

    public RequestHandler() {}

    HttpURLConnection openConnection(String path) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(this.BASE_URL + path).openConnection();
        conn.setRequestProperty("User-Agent", USER_AGENT);
        if (this.authToken != null) {
            conn.setRequestProperty("Cookie", "token=" + this.authToken);
        }
        conn.setDoInput(true);
        return conn;
    }

    void jsonPost(HttpURLConnection conn, JsonObject json) throws IOException {
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream output = conn.getOutputStream()) {
            output.write(json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    void checkResponseCode(HttpURLConnection conn) throws IOException {
        if (conn.getResponseCode() != 200) {
            throw new IOException("Got error response code: " + conn.getResponseCode());
        }
    }

    JsonObject readResponseAsJson(HttpURLConnection conn) throws IOException {
        checkResponseCode(conn);
        String response;
        try (InputStream input = conn.getInputStream()) {
            response = IOUtils.toString(input, StandardCharsets.UTF_8);
        }
        try {
            return JsonParser.parseString(response).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid json: " + response);
            throw new RuntimeException(e);
        }
    }

    byte[] readResponseToBytes(HttpURLConnection conn) throws IOException {
        checkResponseCode(conn);
        try (InputStream input = conn.getInputStream()) {
            return IOUtils.toByteArray(input);
        }
    }

    void readResponseToOutputStream(HttpURLConnection conn, OutputStream output) throws IOException {
        checkResponseCode(conn);
        try (InputStream input = conn.getInputStream()) {
            IOUtils.copy(input, output);
        }
    }

    public void login(String username, String password) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", password);

        HttpURLConnection conn = this.openConnection("login");
        this.jsonPost(conn, json);
        final JsonObject response = this.readResponseAsJson(conn);
        this.authToken = response.get("token").getAsString();
        System.out.println("Obtained session token: " + this.authToken);
    }

    public List<Playlist> getPlaylists() throws IOException {
        HttpURLConnection conn = this.openConnection("track_list");
        final JsonObject response = this.readResponseAsJson(conn);
        System.out.println(response.toString());
        List<Playlist> playlists = new ArrayList<>();
        for (JsonElement elem : response.getAsJsonArray("playlists")) {
            playlists.add(new Playlist(this, elem.getAsJsonObject()));
        }
        return playlists;
    }

//    public byte[] getOriginalFile(Track track) throws IOException {
//        String encodedPath = URLEncoder.encode(track.getPath(), StandardCharsets.UTF_8.toString());
//        HttpURLConnection conn = this.openConnection("files_download?path=" + encodedPath);
//        return this.readResponseToBytes(conn);
//    }

}
