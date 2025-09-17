package com.tonic.data;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

import java.util.HashMap;
import java.util.Map;

public class Quests {
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

    public static boolean isFinished(Quest quest) {
        Client client = Static.getClient();
        return quest.getState(client) == QuestState.FINISHED;
    }

    public static QuestState getState(Quest quest) {
        Client client = Static.getClient();
        return quest.getState(client);
    }
}
