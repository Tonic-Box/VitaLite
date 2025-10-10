package com.tonic.api.widgets;

import com.tonic.api.game.SkillAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.SpellBook;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * MagicAPI provides utility methods for managing magic-related functionalities,
 * including autocasting spells and handling home teleport cooldowns.
 */
public class MagicAPI
{
    private static final int AUTOCAST_VARP = 108;
    private static final int AUTOCAST_USABLE_VARP = 843;

    /**
     * Checks if autocasting is currently enabled.
     *
     * @return true if autocasting is enabled, false otherwise.
     */
    public static boolean isAutoCasting()
    {
        return VarAPI.getVarp(AUTOCAST_VARP) != 0;
    }

    public static boolean isAutoCastUsable()
    {
        return VarAPI.getVarp(AUTOCAST_USABLE_VARP) == 18;
    }

    public static boolean isDefensiveCasting()
    {
        return !isAutoCasting() && VarAPI.getVar(VarbitID.AUTOCAST_DEFMODE) == 1;
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
        return getLastHomeTeleportUsage().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now());
    }

    /**
     * Sets the best available spell for autocasting based on the player's magic level.
     */
    public static void setBestAutoCast()
    {
        Spell bestSpell = comparator();
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
