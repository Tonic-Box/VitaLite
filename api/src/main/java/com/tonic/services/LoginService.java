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

            tClient.setSessionId(null);
            tClient.setCharacterId(null);
            tClient.setDisplayName(null);

            tClient.setAccountTypeLegacy();

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
            tClient.setAccountTypeJagex();
            tClient.setLoginIndex(10);
            tClient.setSessionId(sessionId);
            tClient.setCharacterId(characterId);
            tClient.setDisplayName(displayName);

            if (shouldLogin)
            {
                client.setGameState(GameState.LOGGING_IN);
            }
            return true;
        });
    }
}
