package com.tonic.services.pathfinder.transports.data;

import com.tonic.Static;
import com.tonic.services.pathfinder.requirements.*;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

@Getter
public enum CharterShip
{
    CIVITAS_ILLA_FORTIS(
            new WorldPoint(1742, 3136, 0), new WorldPoint(1747, 3136, 1),
            new QuestRequirement(Quest.CHILDREN_OF_THE_SUN, Set.of(QuestState.FINISHED)),
            new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARBIT, VarbitID.ZEAH_PLAYERHASVISITED, 1)
    ),
    BRIMHAVEN(new WorldPoint(2760, 3236, 0), new WorldPoint(2763, 3238, 1)),
    CATHERBY(new WorldPoint(2796, 3414, 0), new WorldPoint(2792, 3417, 1)),
    CORSAIR_COVE(
            new WorldPoint(2586, 2851, 0), new WorldPoint(2592, 2851, 1),
            new QuestRequirement(Quest.THE_CORSAIR_CURSE, QuestState.FINISHED)
    ),
    LANDS_END(
            new WorldPoint(1499, 3403, 0), new WorldPoint(1493, 3403, 1),
            new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARBIT, VarbitID.ZEAH_PLAYERHASVISITED, 1)
    ),
    MUSA_POINT(new WorldPoint(2954, 3157, 0), new WorldPoint(2957, 3158, 1)),
    PORT_KHAZARD(new WorldPoint(2674, 3146, 0), new WorldPoint(2674, 3141, 1)),
    PORT_PHASMATYS(
            new WorldPoint(3701, 3503, 0), new WorldPoint(3705, 3503, 1),
            new QuestRequirement(Quest.PRIEST_IN_PERIL, QuestState.FINISHED)
    ),
    PORT_PISCARILIUS(
            new WorldPoint(1807, 3679, 0), new WorldPoint(1811, 3679, 1),
            new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARBIT, VarbitID.ZEAH_PLAYERHASVISITED, 1)
    ),
    PORT_SARIM(new WorldPoint(3039, 3193, 0), new WorldPoint(3038, 3189, 1)),
    ALDARIN(
            new WorldPoint(1453, 2968, 0), new WorldPoint(1458, 2968, 1),
            new QuestRequirement(Quest.CHILDREN_OF_THE_SUN, QuestState.FINISHED),
            new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARBIT, VarbitID.ZEAH_PLAYERHASVISITED, 1)
    ),
    SUNSET_COAST(
            new WorldPoint(1514, 2974, 0), new WorldPoint(1514, 2968, 1),
            new QuestRequirement(Quest.CHILDREN_OF_THE_SUN, QuestState.FINISHED),
            new VarRequirement(Comparison.GREATER_THAN_EQUAL, VarType.VARBIT, VarbitID.ZEAH_PLAYERHASVISITED, 1)
    ),
    PORT_TYRAS(
            new WorldPoint(2145, 3122, 0), new WorldPoint(2142, 3125, 1),
            new QuestRequirement(Quest.UNDERGROUND_PASS, QuestState.FINISHED)
    ),
    PRIFDDINAS(
            new WorldPoint(2160, 3329, 0), new WorldPoint(2157, 3333, 1),
            new QuestRequirement(Quest.SONG_OF_THE_ELVES, QuestState.FINISHED)
    )

    ;

    CharterShip(WorldPoint location, WorldPoint arival, Requirement... requirements)
    {
        this.location = location;
        this.arival = arival;
        this.requirements = new Requirements();
        this.requirements.addRequirements(requirements);
        this.requirements.addRequirement(new ItemRequirement(false, 8000, ItemID.COINS_995));
        this.requirements.addRequirement(new WorldRequirement(true));
    }

    private final WorldPoint location;
    private final WorldPoint arival;
    private final Requirements requirements;
    private final int widgetId = 57999364;
    public int getIndex()
    {
        return Static.invoke(() -> {
            Client client = Static.getClient();
            Widget widget = client.getWidget(widgetId);
            if(widget == null)
                return 0;
            for(Widget child : Objects.requireNonNull(widget.getChildren()))
            {
                String[] actions = child.getActions();
                if(actions == null || actions.length != 1)
                    continue;

                String name = name().toLowerCase().replace("_", " ");
                String entryName = TextUtil.sanitize(actions[0].toLowerCase().replace("'", ""));
                if(!entryName.contains(name))
                    continue;
                return child.getIndex();
            }
            return 0;
        });
    }
}