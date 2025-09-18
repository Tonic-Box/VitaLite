package com.tonic.services.pathfinder.transports.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

@RequiredArgsConstructor
@Getter
public enum BarnabyShip
{
    ARDOUGNE(new WorldPoint(2681, 3275, 0), new WorldPoint(2683, 3268, 1), "Ardougne"),
    BRIMHAVEN(new WorldPoint(2772, 3231, 0), new WorldPoint(2775, 3233, 1), "Brimhaven"),
    RIMMINGTON(new WorldPoint(2915, 3226, 0), new WorldPoint(2915, 3221, 1), "Rimmington")
    ;

    private final WorldPoint location;
    private final WorldPoint arival;
    private final String option;
}