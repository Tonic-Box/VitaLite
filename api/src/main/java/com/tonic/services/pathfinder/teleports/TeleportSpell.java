package com.tonic.services.pathfinder.teleports;

import com.tonic.api.widgets.MagicAPI;
import com.tonic.data.magic.SpellBook;
import com.tonic.data.magic.spellbooks.Standard;
import com.tonic.data.wrappers.PlayerEx;
import com.tonic.util.WorldPointUtil;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;

public enum TeleportSpell {
    LUMBRIDGE_HOME_TELEPORT(Standard.HOME_TELEPORT, new WorldPoint(3222, 3218, 0), null),
    VARROCK_TELEPORT(Standard.VARROCK_TELEPORT, new WorldPoint(3212, 3424, 0), null),
    LUMBRIDGE_TELEPORT(Standard.LUMBRIDGE_TELEPORT, new WorldPoint(3222, 3218, 0), null),
    FALADOR_TELEPORT(Standard.FALADOR_TELEPORT, new WorldPoint(2966, 3378, 0), null),
    CAMELOT_TELEPORT(Standard.CAMELOT_TELEPORT, new WorldPoint(2757, 3479, 0), null),
    KOUREND_TELEPORT(Standard.TELEPORT_TO_KOUREND, new WorldPoint(1643, 3672, 0), Quest.CLIENT_OF_KOUREND),
    ARDOUGNE_TELEPORT(Standard.ARDOUGNE_TELEPORT, new WorldPoint(2661, 3300, 0), Quest.PLAGUE_CITY),
    CIVITAS_ILLA_FORTIS(Standard.CIVITAS_ILLA_FORTIS, new WorldPoint(1681, 3132, 0), Quest.TWILIGHTS_PROMISE),
    WATCHTOWER_TELEPORT(Standard.WATCHTOWER_TELEPORT, new WorldPoint(2931, 4712, 2), Quest.WATCHTOWER),
    TROLLHEIM_TELEPORT(Standard.TROLLHEIM_TELEPORT, new WorldPoint(2891, 3680, 0), Quest.EADGARS_RUSE);

    @Getter
    private final Standard spell;

    @Getter
    private final WorldPoint destination;

    private final Quest questRequirement;

    TeleportSpell(Standard spell, WorldPoint destination, Quest questRequirement) {
        this.spell = spell;
        this.destination = destination;
        this.questRequirement = questRequirement;
    }

    public boolean canCast() {
        if (this == LUMBRIDGE_HOME_TELEPORT) {
            return SpellBook.isOnStandardSpellbook() && !MagicAPI.isHomeTeleportOnCooldown();
        }

        return spell.canCast();
    }

    public int distanceFromPlayer() {
        WorldPoint playerLocation = PlayerEx.getLocal().getWorldPoint();

        if (playerLocation == null) {
            return Integer.MAX_VALUE;
        }

        return playerLocation.distanceTo(destination);
    }

    public static TeleportSpell getHomeTeleport() {
        return LUMBRIDGE_HOME_TELEPORT;
    }
}