package com.tonic.services.pathfinder.transports.data;

import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.TileObjectEx;
import com.tonic.queries.TileObjectQuery;
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

    public void travelTo(CanoeStation destination, Canoe canoe)
    {
        TileObjectEx station = new TileObjectQuery<>()
                .withName("Canoe Station")
                .sortNearest()
                .first();
        TileObjectAPI.interact(station, "Chop-down");
        Delays.tick();

        station = new TileObjectQuery<>()
                .withAction("Shape-Canoe")
                .sortNearest()
                .first();

        while(station == null)
        {
            Delays.tick();
            station = new TileObjectQuery<>()
                    .withAction("Shape-Canoe")
                    .sortNearest()
                    .first();
        }

        TileObjectAPI.interact(station, "Shape-Canoe");
        Delays.waitUntil(() -> WidgetAPI.get(416, 3) != null);
        WidgetAPI.interact(1, canoe.getWidgetId(), 0);

        station = new TileObjectQuery<>()
                .withPartialAction("Float ")
                .sortNearest()
                .first();

        while(station == null)
        {
            Delays.tick();
            station = new TileObjectQuery<>()
                    .withPartialAction("Float ")
                    .sortNearest()
                    .first();
        }
        TileObjectAPI.interact(station, "Float ");
        Delays.tick(3);

        TileObjectAPI.interact(station, "Paddle Canoe");
        Delays.waitUntil(() -> WidgetAPI.get(647, 13) != null);
        WidgetAPI.interact(1, destination.getWidgetId(), 0);
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
                        List<Runnable> actions = new ArrayList<>();
                        actions.add(() -> start.travelTo(end, canoe));
                        transports.add(
                                new Transport(
                                        WorldPointUtil.compress(start.getLocation()),
                                        WorldPointUtil.compress(end.getLocation()),
                                        6,
                                        10,
                                        29,
                                        actions,
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