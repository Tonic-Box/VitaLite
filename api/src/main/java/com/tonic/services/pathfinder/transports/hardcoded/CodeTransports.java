package com.tonic.services.pathfinder.transports.hardcoded;

import com.tonic.services.pathfinder.transports.Transport;

import java.util.function.Consumer;

/**
 * Every code-defined transport family, loaded in a fixed order.
 */
public final class CodeTransports
{
    private CodeTransports()
    {
    }

    /**
     * Adds every edge of every code-defined family.
     *
     * @param add receives each edge
     */
    public static void load(Consumer<Transport> add)
    {
        FairyRings.load(add);
        Quetzals.load(add);
        GnomeGliders.load(add);
        SpiritTrees.load(add);
        Minecarts.load(add);
        CharterShips.load(add);
        Canoes.load(add);
        DwarvenCarts.load(add);
        Barnaby.load(add);
        ShantayPass.load(add);
        LumbridgeSwampCave.load(add);
        BoatAndPlank.load(add);
        SlashWebs.load(add);
        ItemOnObjectCrossings.load(add);
        Trapdoors.load(add);
        Waterbirth.load(add);
        MagicCarpets.load(add);
        BrineRatCavern.load(add);
        GnomeStrongholdGate.load(add);
    }
}
