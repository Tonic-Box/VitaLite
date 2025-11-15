package com.tonic.services.pathfinder.transports.data;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.util.handler.StepHandler;
import com.tonic.queries.TileObjectQuery;
import com.tonic.util.handler.HandlerBuilder;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.WorldPointUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public enum CanoeStation
{
    LUMBRIDGE(new WorldPoint(3243, 3235, 0), 42401807),
    CHAMPION_GUILD(new WorldPoint(3204, 3343, 0), 42401808),
    BARBARIAN_VILLAGE(new WorldPoint(3112, 3409, 0), 42401809),
    EDGEVILLE(new WorldPoint(3132, 3508, 0), 42401806)
    ;

    private final WorldPoint location;
    private final int WidgetId;

    public StepHandler travelTo(CanoeStation destination, Canoe canoe)
    {
        return HandlerBuilder.get()
                .add(0, () -> {
                    TileObjectEx station = new TileObjectQuery()
                            .withName("Canoe Station")
                            .sortNearest()
                            .first();
                    TileObjectAPI.interact(station, "Chop-down");
                })
                .add(1, () -> 2)
                .add(2, () -> {
                    TileObjectEx station = new TileObjectQuery()
                            .withAction("Shape-Canoe")
                            .sortNearest()
                            .first();
                    if(station != null)
                    {
                        TileObjectAPI.interact(station, "Shape-Canoe");
                        return 3;
                    }
                    return 2;
                })
                .addDelayUntil(3, () -> WidgetAPI.get(416, 3) != null)
                .add(4, () -> WidgetAPI.interact(1, canoe.getWidgetId(), 0))
                .add(5, () -> {
                    TileObjectEx station = new TileObjectQuery()
                            .withPartialAction("Float ")
                            .sortNearest()
                            .first();
                    if(station != null)
                    {
                        TileObjectAPI.interact(station, "Float ");
                        return 6;
                    }
                    return 5;
                })
                .add(6, () -> {

                    TileObjectEx station = new TileObjectQuery()
                            .withPartialAction("Paddle ")
                            .sortNearest()
                            .first();
                    if(station != null)
                    {
                        TileObjectAPI.interact(station, "Paddle ");
                        return 7;
                    }
                    return 6;
                })
                .addDelayUntil(7, () -> WidgetAPI.get(647, 13) != null)
                .add(8, () -> WidgetAPI.interact(1, destination.getWidgetId(), 0))
                .addDelay(9, 29)
                .build();
    }

    public static List<Transport> getTravelMatrix()
    {
        List<Transport> transports = new ArrayList<>();

        CanoeStation[] stations = values();
        for (CanoeStation start : stations)
        {
            int startOrdinal = start.ordinal();
            for (Canoe canoe : Canoe.values())
            {
                int distance = canoe.getDistance();

                for (CanoeStation end : stations)
                {
                    int endOrdinal = end.ordinal();
                    if (endOrdinal == startOrdinal)
                    {
                        continue;
                    }

                    int ordinalDiff = Math.abs(endOrdinal - startOrdinal);
                    if (ordinalDiff <= distance)
                    {
                        transports.add(
                                new Transport(
                                        WorldPointUtil.compress(start.getLocation()),
                                        WorldPointUtil.compress(end.getLocation()),
                                        6,
                                        10,
                                        29,
                                        start.travelTo(end, canoe),
                                        canoe.getRequirements(),
                                        -1
                                )
                        );
                    }
                }
            }
        }

        return transports;
    }

    @Data
    public static class TavelMatrix
    {
        private CanoeStation start;
        private CanoeStation end;
        private Canoe canoe;
    }
}