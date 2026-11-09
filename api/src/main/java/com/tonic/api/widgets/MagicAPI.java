package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.SpellBook;
import com.tonic.data.wrappers.*;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import org.apache.commons.lang3.ArrayUtils;

import java.time.Instant;

/**
 * MagicAPI provides utility methods for managing magic-related functionalities,
 * including autocasting spells and handling home teleport cooldowns.
 */
public class MagicAPI
{
    private static final int[] AUTOCAST_WEAPON_CATEGORIES = {18, 21, 22};
    private static final int HOME_TELEPORT_COOLDOWN_MINUTES = 30;

    /**
     * Checks if autocasting is currently enabled.
     *
     * @return true if autocasting is enabled, false otherwise.
     */
    public static boolean isAutoCasting()
    {
        return VarAPI.getVar(VarbitID.AUTOCAST_SET) == 1;
    }

    public static boolean isAutoCastUsable()
    {
        return ArrayUtils.contains(AUTOCAST_WEAPON_CATEGORIES, VarAPI.getVar(VarbitID.COMBAT_WEAPON_CATEGORY));
    }

    public static boolean isDefensiveCasting()
    {
        return isAutoCasting() && VarAPI.getVar(VarbitID.AUTOCAST_DEFMODE) == 1;
    }

    /**
     * Retrieves the timestamp of the last home teleport usage.
     *
     * @return An Instant representing the last home teleport usage time.
     */
    public static Instant getLastHomeTeleportUsage()
    {
        return Instant.ofEpochSecond(VarAPI.getVarp(VarPlayerID.AIDE_TELE_TIMER) * 60L);
    }

    /**
     * Checks if the home teleport is currently on cooldown.
     *
     * @return true if the home teleport is on cooldown, false otherwise.
     */
    public static boolean isHomeTeleportOnCooldown()
    {
        return VarAPI.getVarp(VarPlayerID.DATE_MINUTES) - VarAPI.getVarp(VarPlayerID.AIDE_TELE_TIMER) < HOME_TELEPORT_COOLDOWN_MINUTES;
    }

    /**
     * Sets the best available spell for autocasting based on the player's magic level.
     */
    public static void setBestAutoCast()
    {
        Spell bestSpell = Static.invoke(MagicAPI::comparator);
        bestSpell.setAutoCast();
    }

    /**
     * Casts the specified spell if it is valid and can be cast.
     *
     * @param spell The spell to be cast.
     */
    public static void cast(Spell spell)
    {
        if(spell == null || !spell.canCast())
            return;

        spell.cast();
    }

    /**
     * Casts the specified spell on a target if it is valid and can be cast.
     *
     * @param spell  The spell to be cast.
     * @param target The NPC target for the spell.
     */
    public static void cast(Spell spell, NpcEx target)
    {
        if (spell == null || target == null || !spell.canCast())
            return;

        spell.castOn(target);
    }

    /**
     * Casts the specified spell on an item if it is valid and can be cast.
     *
     * @param spell The spell to be cast.
     * @param item  The item target for the spell.
     */
    public static void cast(Spell spell, ItemEx item)
    {
        if (spell == null || item == null || !spell.canCast())
            return;

        spell.castOn(item);
    }

    /**
     * Casts the specified spell on a player if it is valid and can be cast.
     *
     * @param spell  The spell to be cast.
     * @param player The player target for the spell.
     */
    public static void cast(Spell spell, PlayerEx player)
    {
        if (spell == null || player == null || !spell.canCast())
            return;

        spell.castOn(player);
    }

    /**
     * Casts the specified spell on a tile object if it is valid and can be cast.
     *
     * @param spell      The spell to be cast.
     * @param tileObject The tile object target for the spell.
     */
    public static void cast(Spell spell, TileObjectEx tileObject)
    {
        if (spell == null || tileObject == null || !spell.canCast())
            return;

        spell.castOn(tileObject);
    }

    /**
     * Casts the specified spell on a ground item if it is valid and can be cast.
     *
     * @param spell    The spell to be cast.
     * @param tileItem The ground item target for the spell.
     */
    public static void cast(Spell spell, TileItemEx tileItem)
    {
        if (spell == null || tileItem == null || !spell.canCast())
            return;

        spell.castOn(tileItem);
    }

    /**
     * Compares available offensive spells and returns the best one that can be cast
     * based on the player's current magic level.
     *
     * @return The best available Spell that can be cast, or null if none are available.
     */
    public static Spell comparator() {
        int level = SkillAPI.getLevel(Skill.MAGIC);
        Spell spell = null;
        for(Spell s : SpellBook.getCurrentOffensiveSpells()) {
            if(s.getLevel() > level)
                continue;

            if(!s.canCast())
                continue;

            if(spell != null && s.getLevel() < spell.getLevel())
                continue;

            spell = s;
        }

        return spell;
    }
}
