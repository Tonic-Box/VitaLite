package com.tonic.services.pathfinder.teleports;

import com.tonic.Static;
import com.tonic.api.widgets.MagicAPI;
import com.tonic.data.magic.SpellBook;
import com.tonic.data.magic.spellbooks.Standard;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

public enum TeleportSpell {
    LUMBRIDGE_HOME_TELEPORT(Standard.HOME_TELEPORT, new WorldPoint(3222, 3218, 0)),
    VARROCK_TELEPORT(Standard.VARROCK_TELEPORT, new WorldPoint(3212, 3424, 0)),
    LUMBRIDGE_TELEPORT(Standard.LUMBRIDGE_TELEPORT, new WorldPoint(3222, 3218, 0)),
    FALADOR_TELEPORT(Standard.FALADOR_TELEPORT, new WorldPoint(2966, 3378, 0)),
    CAMELOT_TELEPORT(Standard.CAMELOT_TELEPORT, new WorldPoint(2757, 3479, 0)),
    KOUREND_TELEPORT(Standard.TELEPORT_TO_KOUREND, new WorldPoint(1643, 3672, 0)),
    ARDOUGNE_TELEPORT(Standard.ARDOUGNE_TELEPORT, new WorldPoint(2661, 3300, 0)),
    CIVITAS_ILLA_FORTIS(Standard.CIVITAS_ILLA_FORTIS, new WorldPoint(1681, 3132, 0)),
    WATCHTOWER_TELEPORT(Standard.WATCHTOWER_TELEPORT, new WorldPoint(2931, 4712, 2)),
    TROLLHEIM_TELEPORT(Standard.TROLLHEIM_TELEPORT, new WorldPoint(2891, 3680, 0));

    @Getter
    private final Standard spell;

    @Getter
    private final WorldPoint destination;

    TeleportSpell(Standard spell, WorldPoint destination) {
        this.spell = spell;
        this.destination = destination;
    }

    public boolean canCast() {
        if (this == LUMBRIDGE_HOME_TELEPORT) {
            return SpellBook.isOnStandardSpellbook() && !MagicAPI.isHomeTeleportOnCooldown();
        }

        return spell.canCast();
    }

    public int distanceFromPlayer() {
        WorldPoint playerLocation = Static.invoke(() -> WorldPointUtil.fromInstance(PlayerEx.getLocal().getWorldPoint()));

        if (playerLocation == null) {
            return Integer.MAX_VALUE;
        }

        return playerLocation.distanceTo(destination);
    }

    public static TeleportSpell getHomeTeleport() {
        return LUMBRIDGE_HOME_TELEPORT;
    }
}