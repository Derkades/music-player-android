package nl.rkslot.music_player.api;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JsonHelper {

    static List<String> getStringList(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsString).collect(Collectors.toList());
    }

    static @Nullable List<String> getNullableStringList(JsonObject object, String key) {
        JsonElement elem = object.get(key);
        return elem.isJsonNull() ? null : getStringList(elem.getAsJsonArray());
    }

    static @Nullable String getNullableString(JsonObject object, String key) {
        JsonElement elem = object.get(key);
        return elem.isJsonNull() ? null : elem.getAsString();
    }

    static @Nullable Integer getNullableInteger(JsonObject object, String key) {
        JsonElement elem = object.get(key);
        return elem.isJsonNull() ? null : elem.getAsInt();
    }

}
