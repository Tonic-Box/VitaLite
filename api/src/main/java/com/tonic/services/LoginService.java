package com.tonic.services;

import com.tonic.Static;
import com.tonic.api.TClient;
import net.runelite.api.Client;
import net.runelite.api.GameState;

public class LoginService {
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
                client.setGameState(GameState.LOGGING_IN);
            }
            return true;
        });
    }
}
