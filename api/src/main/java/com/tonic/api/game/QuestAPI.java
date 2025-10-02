package com.tonic.api.game;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import java.util.HashMap;
import java.util.Map;

/**
 * Quest API
 */
public class QuestAPI
{
    /**
     * Get the state of a quest
     * @param quest The quest to check
     * @return The state of the quest
     */
    public static QuestState getState(Quest quest) {
        final Client client = Static.getClient();
        return Static.invoke(() -> quest.getState(client));
    }

    /**
     * Get a map of all quests and their states
     * @return A map of all quests and their states
     */
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

    /**
     * Check if a quest is completed
     * @param quest The quest to check
     * @return True if the quest is completed, false otherwise
     */
    public static boolean isCompleted(Quest quest) {
        return getState(quest) == QuestState.FINISHED;
    }

    /**
     * Check if a quest is in progress
     * @param quest The quest to check
     * @return True if the quest is in progress, false otherwise
     */
    public static boolean isInProgress(Quest quest) {
        return getState(quest) == QuestState.IN_PROGRESS;
    }

    /**
     * Check if a quest is not started
     * @param quest The quest to check
     * @return True if the quest is not started, false otherwise
     */
    public static boolean isNotStarted(Quest quest) {
        return getState(quest) == QuestState.NOT_STARTED;
    }

    /**
     * Check if a quest is in one of the given states
     * @param quest The quest to check
     * @param states The states to check against
     * @return True if the quest is in one of the given states, false otherwise
     */
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
