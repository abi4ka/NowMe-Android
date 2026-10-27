package com.example.nowme.network;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.ResponseBody;
import retrofit2.Response;

/** Reads the backend's error code from bodies like {"error": "INVALID_CREDENTIALS"}. */
public final class ApiErrors {

    private ApiErrors() {
    }

    @Nullable
    public static String code(Response<?> response) {
        if (response == null) return null;
        try (ResponseBody body = response.errorBody()) {
            if (body == null) return null;
            JsonElement json = JsonParser.parseString(body.string());
            if (!json.isJsonObject()) return null;
            JsonObject object = json.getAsJsonObject();
            return object.has("error") && object.get("error").isJsonPrimitive()
                    ? object.get("error").getAsString()
                    : null;
        } catch (Exception e) {
            return null;
        }
    }
}
