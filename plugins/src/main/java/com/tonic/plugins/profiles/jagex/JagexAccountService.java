package com.tonic.plugins.profiles.jagex;

import com.google.gson.JsonObject;
import com.tonic.plugins.profiles.jagex.model.JagCharacter;
import com.tonic.plugins.profiles.jagex.model.JagLoginToken;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JagexAccountService {

    private static final String AUTH_ENDPOINT = "https://account.jagex.com/oauth2/auth";
    private static final String CLIENT_ID = "1fddee4e-b100-4f4e-b2b0-097f9088f9d2";

    private final JagexHttpServer httpServer = new JagexHttpServer();
    private int state = 0;

    public CompletableFuture<JagLoginToken> requestLoginToken() {
        final CompletableFuture<JagLoginToken> loginToken = new CompletableFuture<>();
        Thread loginTokenThread = new Thread(() -> {
            try {
                CompletableFuture<JagexHttpServer.OAuth2Response> oAuthTokenFuture = requestIdToken();
                JagexHttpServer.OAuth2Response oAuthToken = oAuthTokenFuture.get(2, TimeUnit.MINUTES);
                JsonObject session = JagexTokenExchange.requestJxSessionInformation(oAuthToken.getIdToken());
                if (session == null) {
                    loginToken.complete(null);
                    return;
                }

                String sessionId = session.get("sessionId").getAsString();

                if (sessionId == null) {
                    loginToken.complete(null);
                    return;
                }

                JagCharacter[] characters = JagexTokenExchange.requestJxAccountInformation(sessionId);

                if (characters != null) {
                    loginToken.complete(new JagLoginToken(
                            sessionId,
                            characters
                    ));
                }
            } catch (IOException | URISyntaxException | TimeoutException | InterruptedException |
                     ExecutionException e) {
//                e.printStackTrace();
            }

            loginToken.complete(null);
        });
        loginTokenThread.start();
        return loginToken;
    }

    public CompletableFuture<JagexHttpServer.OAuth2Response> requestIdToken()
            throws IOException, URISyntaxException {
        int currentState = state++;
        String url = String.format(
                "%s?response_type=id_token+code&client_id=%s&nonce=00000000&state=%08d&prompt=login&scope=openid+offline",
                AUTH_ENDPOINT, CLIENT_ID, currentState
        );

        CompletableFuture<JagexHttpServer.OAuth2Response> future = httpServer.waitForResponse(currentState);
        Desktop.getDesktop().browse(new URI(url));
        return future;
    }

    public void startServer() throws IOException {
        if (httpServer.isOnline()) {
            return;
        }
        httpServer.start();
    }

    public void shutdownServer() {
        if (!httpServer.isOnline()) {
            return;
        }
        httpServer.stop();
    }
}