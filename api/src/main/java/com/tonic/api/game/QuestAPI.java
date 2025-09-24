package com.tonic.api.game;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

import java.util.HashMap;
import java.util.Map;

public class QuestAPI
{
    public static QuestState getState(Quest quest) {
        final Client client = Static.getClient();
        return Static.invoke(() -> quest.getState(client));
    }

    public static Map<String, QuestState> getQuests() {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            Map<String,QuestState> map = new HashMap<>();
            for(Quest quest : Quest.values())
            {
                map.put(quest.getName(), quest.getState(client));
            }
            return map;
        });
    }

    public static boolean isCompleted(Quest quest) {
        return getState(quest) == QuestState.FINISHED;
    }

    public static boolean isInProgress(Quest quest) {
        return getState(quest) == QuestState.IN_PROGRESS;
    }

    public static boolean isNotStarted(Quest quest) {
        return getState(quest) == QuestState.NOT_STARTED;
    }

    public static boolean hasState(Quest quest, QuestState... states)
    {
        QuestState currentState = getState(quest);
        for (QuestState state : states) {
            if (currentState == state) {
                return true;
            }
        }
        return false;
    }
}
