package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.TClient;
import net.runelite.api.Client;
import net.runelite.api.GameState;

public class LoginService {
    /**
     * How an attempt to log back in to the client's current account went.
     */
    public enum CurrentLogin
    {
        /**
         * The login was started.
         */
        STARTED,
        /**
         * The client is not on the login screen.
         */
        NOT_ON_LOGIN_SCREEN,
        /**
         * The client holds neither a Jagex character nor a username to log in with.
         */
        NO_ACCOUNT
    }

    public static boolean login(String username, String password, boolean shouldLogin) {
        Client client = Static.getClient();
        TClient tClient = (TClient) client;
        return Static.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                return false;
            }

            // A legacy account is a Jagex account of nothing, as RuneLite's own Jagex Auth panel switches to one
            client.switchJagexAccount(null, null, null);

            tClient.setLoginIndex(2);

            client.setUsername(username);
            client.setPassword(password);

            if (shouldLogin)
            {
                client.setGameState(GameState.LOGGING_IN);
            }
            return true;
        });
    }

    public static boolean login(String sessionId, String characterId, String displayName, boolean shouldLogin) {
        Client client = Static.getClient();
        TClient tClient = (TClient) client;
        return Static.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                return false;
            }
            client.setUsername("");
            client.setPassword("");
            client.switchJagexAccount(sessionId, characterId, displayName);

            if (shouldLogin)
            {
                tClient.useJagexAccountLoginType();
                client.setGameState(GameState.LOGGING_IN);
            }
            return true;
        });
    }

    /**
     * Logs back in to the account the client was last on: its Jagex character, as the login screen's Play does, else the username the login screen still holds, with whatever password it holds. Blocks until the client thread has run it.
     *
     * @return how the attempt went
     */
    public static CurrentLogin loginCurrent() {
        Client client = Static.getClient();
        TClient tClient = (TClient) client;
        return Static.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN)
            {
                return CurrentLogin.NOT_ON_LOGIN_SCREEN;
            }
            String characterId = tClient.getCharacterId();
            if (characterId != null && !characterId.isEmpty())
            {
                tClient.useJagexAccountLoginType();
                client.setGameState(GameState.LOGGING_IN);
                return CurrentLogin.STARTED;
            }
            String username = client.getUsername();
            if (username == null || username.isEmpty())
            {
                return CurrentLogin.NO_ACCOUNT;
            }
            tClient.setLoginIndex(2);
            client.setGameState(GameState.LOGGING_IN);
            return CurrentLogin.STARTED;
        });
    }
}
