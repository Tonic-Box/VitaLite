package com.tonic.plugins.profiles.jagex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.tonic.plugins.profiles.jagex.model.JagCharacter;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Jagex auth service's game-session API as the launcher uses it: create a game session from a sign-in's
 * id token and list the session's characters. Requests carry the launcher's browser headers and share a
 * cookie jar, so Cloudflare's clearance cookies persist across calls.
 */
public class JagexAuthClient {
    public static final String LIVE_BASE_URL = "https://auth.jagex.com/";
    static final String SESSIONS_PATH = "game-session/v1/sessions";
    static final String ACCOUNTS_PATH = "game-session/v1/accounts";
    static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    static final String[][] PRESET_HEADERS = {
            {"User-Agent", USER_AGENT},
            {"Accept", "application/json, text/plain, */*"},
            {"Accept-Language", "en-US,en;q=0.9"},
            {"sec-ch-ua", "\"Chromium\";v=\"124\", \"Not-A.Brand\";v=\"99\""},
            {"sec-ch-ua-mobile", "?0"},
            {"sec-ch-ua-platform", "\"Windows\""},
            {"Sec-Fetch-Site", "same-origin"},
            {"Sec-Fetch-Mode", "cors"},
            {"Sec-Fetch-Dest", "empty"},
    };
    private static final String[] CHALLENGE_MARKERS = {
            "challenge-platform", "cf_chl_", "Just a moment", "/cdn-cgi/challenge", "__cf_chl",
    };
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final URI sessions;
    private final URI accounts;
    private final HttpClient http;

    public JagexAuthClient() {
        this(LIVE_BASE_URL);
    }

    /**
     * A client against another base URL, such as a test double of the service.
     *
     * @param baseUrl the service's base URL
     */
    public JagexAuthClient(String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.sessions = URI.create(base + SESSIONS_PATH);
        this.accounts = URI.create(base + ACCOUNTS_PATH);
        this.http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
    }

    /**
     * Exchanges a sign-in's id token for a game session id.
     *
     * @param idToken the id token the browser sign-in returned
     * @return the session id
     * @throws JagexAuthException when the service refuses, cannot be reached or is blocked by Cloudflare
     * @throws IOException when the thread is interrupted
     */
    public String createSession(String idToken) throws IOException, JagexAuthException {
        JsonObject body = new JsonObject();
        body.addProperty("idToken", idToken);
        HttpResponse<String> response = send(HttpRequest.newBuilder(sessions)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)));
        return stringField(response.body(), "sessionId", "session reply");
    }

    /**
     * Lists a game session's characters, in the service's order.
     *
     * @param sessionId the session id
     * @return the characters
     * @throws JagexAuthException when the service refuses (an expired session is a 401 or 403), cannot be
     *                            reached or is blocked by Cloudflare
     * @throws IOException when the thread is interrupted
     */
    public List<JagCharacter> listCharacters(String sessionId) throws IOException, JagexAuthException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(accounts)
                .header("Authorization", "Bearer " + sessionId)
                .GET());
        return parseCharacters(response.body());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, JagexAuthException {
        request.timeout(TIMEOUT);
        for (String[] header : PRESET_HEADERS) {
            request.setHeader(header[0], header[1]);
        }
        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw JagexAuthException.unreachable(e.getMessage() == null ? e.toString() : e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while talking to the Jagex auth service", e);
        }
        if (isChallenge(response.statusCode(), response.headers().firstValue("cf-mitigated"), response.body())) {
            throw JagexAuthException.cloudflare(response.statusCode());
        }
        if (response.statusCode() != 200) {
            throw JagexAuthException.rejected(response.statusCode());
        }
        return response;
    }

    static boolean isChallenge(int status, Optional<String> cfMitigated, String body) {
        if (cfMitigated.isPresent() && cfMitigated.get().trim().equalsIgnoreCase("challenge")) {
            return true;
        }
        if (status != 403 && status != 429 && status != 503) {
            return false;
        }
        for (String marker : CHALLENGE_MARKERS) {
            if (body != null && body.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    static String stringField(String json, String field, String what) throws JagexAuthException {
        JsonElement value;
        try {
            JsonElement root = new JsonParser().parse(json);
            value = root.isJsonObject() ? root.getAsJsonObject().get(field) : null;
        } catch (JsonParseException e) {
            throw JagexAuthException.unreachable("malformed " + what);
        }
        if (value == null || !value.isJsonPrimitive() || value.getAsString().isEmpty()) {
            throw JagexAuthException.unreachable(what + " without a " + field);
        }
        return value.getAsString();
    }

    static List<JagCharacter> parseCharacters(String json) throws JagexAuthException {
        JsonElement root;
        try {
            root = new JsonParser().parse(json);
        } catch (JsonParseException e) {
            throw JagexAuthException.unreachable("malformed accounts reply");
        }
        if (!root.isJsonArray()) {
            throw JagexAuthException.unreachable("accounts reply is not a list");
        }
        List<JagCharacter> characters = new ArrayList<>();
        for (JsonElement element : (JsonArray) root) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            JsonElement id = entry.get("accountId");
            if (id == null || !id.isJsonPrimitive()) {
                throw JagexAuthException.unreachable("account without an accountId");
            }
            JsonElement name = entry.get("displayName");
            characters.add(new JagCharacter(name == null || name.isJsonNull() ? null : name.getAsString(), id.getAsString()));
        }
        return characters;
    }
}
