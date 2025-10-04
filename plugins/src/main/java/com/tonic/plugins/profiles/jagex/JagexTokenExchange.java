package com.tonic.plugins.profiles.jagex;

import com.google.gson.JsonObject;
import com.tonic.plugins.profiles.jagex.model.JagCharacter;
import com.tonic.plugins.profiles.util.GsonUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.nio.charset.StandardCharsets;


public class JagexTokenExchange {
    private static final String GAME_SESSION_URI = "https://auth.jagex.com/game-session/v1/sessions";
    private static final String GAME_ACCOUNT_URI = "https://auth.jagex.com/game-session/v1/accounts";

    public static JsonObject requestJxSessionInformation(String jwt) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),
                new String(("{\"idToken\":\"" + jwt + "\"}").getBytes(), StandardCharsets.UTF_8));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(GAME_SESSION_URI)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return null;
            }

            String res = response.body().string();

            return GsonUtil.GSON.fromJson(res, JsonObject.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static JagCharacter[] requestJxAccountInformation(String jwt) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(GAME_ACCOUNT_URI)
                .header("Authorization", "Bearer " + jwt)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                return null;
            }
            String r = response.body().string();
            return GsonUtil.GSON.fromJson(r, JagCharacter[].class);
        } catch (Exception ignored) {
            return null;
        }
    }
}


