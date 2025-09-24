package com.tonic.services.pathfinder.requirements;

import com.tonic.Static;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

import java.util.Set;

@Value
public class QuestRequirement implements Requirement
{
    Quest quest;
    Set<QuestState> states;

    public QuestRequirement(Quest quest, QuestState... states)
    {
        this.quest = quest;
        this.states = Set.of(states);
    }

    public QuestRequirement(Quest quest, Set<QuestState> states)
    {
        this.quest = quest;
        this.states = states;
    }

    @Override
    public Boolean get()
    {
        Client client = Static.getClient();
        return states.contains(quest.getState(client));
    }
}
