package com.tonic.api.widgets;

import com.tonic.Static;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * PrayerAPI provides methods to interact with and manage prayers in the game.
 * It allows checking prayer levels, toggling prayers, managing quick prayers,
 * and retrieving the highest available prayers based on the player's level.
 */
@Getter
public enum PrayerAPI {
    /**
     * Thick Skin (Level 1, Defence).
     */
    THICK_SKIN(VarbitID.PRAYER_THICKSKIN, 5.0, InterfaceID.Prayerbook.PRAYER1, 1, 0),
    /**
     * Burst of Strength (Level 4, Strength).
     */
    BURST_OF_STRENGTH(VarbitID.PRAYER_BURSTOFSTRENGTH, 5.0, InterfaceID.Prayerbook.PRAYER2, 4, 1),
    /**
     * Clarity of Thought (Level 7, Attack).
     */
    CLARITY_OF_THOUGHT(VarbitID.PRAYER_CLARITYOFTHOUGHT, 5.0, InterfaceID.Prayerbook.PRAYER3, 7, 2),
    /**
     * Sharp Eye (Level 8, Ranging).
     */
    SHARP_EYE(VarbitID.PRAYER_SHARPEYE, 5.0, InterfaceID.Prayerbook.PRAYER19, 8, 18),
    /**
     * Mystic Will (Level 9, Magic).
     */
    MYSTIC_WILL(VarbitID.PRAYER_MYSTICWILL, 5.0, InterfaceID.Prayerbook.PRAYER22, 9, 19),
    /**
     * Rock Skin (Level 10, Defence).
     */
    ROCK_SKIN(VarbitID.PRAYER_ROCKSKIN, 10.0, InterfaceID.Prayerbook.PRAYER4, 10, 3),
    /**
     * Superhuman Strength (Level 13, Strength).
     */
    SUPERHUMAN_STRENGTH(VarbitID.PRAYER_SUPERHUMANSTRENGTH, 10.0, InterfaceID.Prayerbook.PRAYER5, 13, 4),
    /**
     * Improved Reflexes (Level 16, Attack).
     */
    IMPROVED_REFLEXES(VarbitID.PRAYER_IMPROVEDREFLEXES, 10.0, InterfaceID.Prayerbook.PRAYER6, 16, 5),
    /**
     * Rapid Restore (Level 19, Stats).
     */
    RAPID_RESTORE(VarbitID.PRAYER_RAPIDRESTORE, 60.0 / 36.0, InterfaceID.Prayerbook.PRAYER7, 19, 6),
    /**
     * Rapid Heal (Level 22, Hitpoints).
     */
    RAPID_HEAL(VarbitID.PRAYER_RAPIDHEAL, 60.0 / 18, InterfaceID.Prayerbook.PRAYER8, 22, 7),
    /**
     * Protect Item (Level 25).
     */
    PROTECT_ITEM(VarbitID.PRAYER_PROTECTITEM, 60.0 / 18, InterfaceID.Prayerbook.PRAYER9, 25, 8),
    /**
     * Hawk Eye (Level 26, Ranging).
     */
    HAWK_EYE(VarbitID.PRAYER_HAWKEYE, 10.0, InterfaceID.Prayerbook.PRAYER20, 26, 20),
    /**
     * Mystic Lore (Level 27, Magic).
     */
    MYSTIC_LORE(VarbitID.PRAYER_MYSTICLORE, 10.0, InterfaceID.Prayerbook.PRAYER23, 27, 21),
    /**
     * Steel Skin (Level 28, Defence).
     */
    STEEL_SKIN(VarbitID.PRAYER_STEELSKIN, 20.0, InterfaceID.Prayerbook.PRAYER10, 28, 9),
    /**
     * Ultimate Strength (Level 31, Strength).
     */
    ULTIMATE_STRENGTH(VarbitID.PRAYER_ULTIMATESTRENGTH, 20.0, InterfaceID.Prayerbook.PRAYER11, 31, 10),
    /**
     * Incredible Reflexes (Level 34, Attack).
     */
    INCREDIBLE_REFLEXES(VarbitID.PRAYER_INCREDIBLEREFLEXES, 20.0, InterfaceID.Prayerbook.PRAYER12, 34, 11),
    /**
     * Protect from Magic (Level 37).
     */
    PROTECT_FROM_MAGIC(VarbitID.PRAYER_PROTECTFROMMAGIC, 20.0, InterfaceID.Prayerbook.PRAYER13, 37, 12),
    /**
     * Protect from Missiles (Level 40).
     */
    PROTECT_FROM_MISSILES(VarbitID.PRAYER_PROTECTFROMMISSILES, 20.0, InterfaceID.Prayerbook.PRAYER14, 40, 13),
    /**
     * Protect from Melee (Level 43).
     */
    PROTECT_FROM_MELEE(VarbitID.PRAYER_PROTECTFROMMELEE, 20.0, InterfaceID.Prayerbook.PRAYER15, 43, 14),
    /**
     * Eagle Eye (Level 44, Ranging).
     */
    EAGLE_EYE(VarbitID.PRAYER_EAGLEEYE, 20.0, InterfaceID.Prayerbook.PRAYER21, 44, 22),
    /**
     * Mystic Might (Level 45, Magic).
     */
    MYSTIC_MIGHT(VarbitID.PRAYER_MYSTICMIGHT, 20.0, InterfaceID.Prayerbook.PRAYER24, 45, 23),
    /**
     * Retribution (Level 46).
     */
    RETRIBUTION(VarbitID.PRAYER_RETRIBUTION, 5.0, InterfaceID.Prayerbook.PRAYER16, 46, 15),
    /**
     * Redemption (Level 49).
     */
    REDEMPTION(VarbitID.PRAYER_REDEMPTION, 10.0, InterfaceID.Prayerbook.PRAYER17, 49, 16),
    /**
     * Smite (Level 52).
     */
    SMITE(VarbitID.PRAYER_SMITE, 30.0, InterfaceID.Prayerbook.PRAYER18, 52, 17),
    /**
     * Preserve (Level 55).
     */
    PRESERVE(VarbitID.PRAYER_PRESERVE, 60.0 / 18, InterfaceID.Prayerbook.PRAYER29, 55, 28),
    /**
     * Chivalry (Level 60, Defence/Strength/Attack).
     */
    CHIVALRY(VarbitID.PRAYER_CHIVALRY, 40.0, InterfaceID.Prayerbook.PRAYER26, 60, 25),
    /**
     * Piety (Level 70, Defence/Strength/Attack).
     */
    PIETY(VarbitID.PRAYER_PIETY, 40.0, InterfaceID.Prayerbook.PRAYER27, 70, 26),
    /**
     * Rigour (Level 74, Ranging/Damage/Defence).
     */
    RIGOUR(VarbitID.PRAYER_RIGOUR, 40.0, InterfaceID.Prayerbook.PRAYER25, 74, 24),
    /**
     * Augury (Level 77, Magic/Magic Def./Defence).
     */
    AUGURY(VarbitID.PRAYER_AUGURY, 40.0, InterfaceID.Prayerbook.PRAYER28, 77, 27);

    private static final PrayerAPI[] OVERHEAD_PRAYERS = {
        PrayerAPI.PROTECT_FROM_MAGIC,
        PrayerAPI.PROTECT_FROM_MELEE,
        PrayerAPI.PROTECT_FROM_MISSILES,
        PrayerAPI.RETRIBUTION,
        PrayerAPI.REDEMPTION,
        PrayerAPI.SMITE
    };

    private final int varbit;
    private final double drainRate;
    private final int interfaceId;
    private final int level;
    private final int quickPrayerIndex;

    PrayerAPI(int varbit, double drainRate, int interfaceId, int level, int quickPrayerIndex)
    {
        this.varbit = varbit;
        this.drainRate = drainRate;
        this.interfaceId = interfaceId;
        this.level = level;
        this.quickPrayerIndex = quickPrayerIndex;
    }

    /**
     * check if the player has the level for this prayer
     * @return bool
     */
    public boolean hasLevelFor()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getRealSkillLevel(Skill.PRAYER) >= level);
    }

    /**
     * check if this prayer is set as a quick prayer
     * @return bool
     */
    public boolean isQuickPrayer()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> (client.getVarbitValue(4102) & (int) Math.pow(2, quickPrayerIndex)) == Math.pow(2, quickPrayerIndex));
    }

    /**
     * set the quick prayers
     * @param prayers prayers
     */
    public static void setQuickPrayer(PrayerAPI... prayers)
    {
        WidgetAPI.interact(2, InterfaceID.Orbs.PRAYERBUTTON, -1, -1);
        for(PrayerAPI prayer : prayers)
        {
            if(prayer == null)
                continue;
            if(prayer.isQuickPrayer())
                continue;
            WidgetAPI.interact(1, InterfaceID.Quickprayer.BUTTONS, prayer.getQuickPrayerIndex(), -1);
        }
        WidgetAPI.interact(1, InterfaceID.Quickprayer.CLOSE, -1, -1);
    }

    /**
     * check if the quick prayers are enabled currently
     * @return bool
     */
    public static boolean isQuickPrayerEnabled()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarbitValue(VarbitID.QUICKPRAYER_ACTIVE) == 1);
    }

    /**
     * toggle quick prayer activation
     */
    public static void toggleQuickPrayer()
    {
        WidgetAPI.interact(1, InterfaceID.Orbs.PRAYERBUTTON, -1, -1);
    }

    /**
     * turn quick prayer on
     */
    public static void turnOnQuickPrayers()
    {
        if(!isQuickPrayerEnabled())
        {
            toggleQuickPrayer();
        }
    }

    /**
     * turn off quick prayers
     */
    public static void turnOffQuickPrayers()
    {
        if(isQuickPrayerEnabled())
        {
            toggleQuickPrayer();
        }
    }

    /**
     * @return The current active overhead prayer
     */
    public static PrayerAPI getActiveOverhead() {
        for (PrayerAPI prayer : PrayerAPI.values())
        {
            if (!prayer.isActive())
            {
                continue;
            }

            if (!ArrayUtils.contains(OVERHEAD_PRAYERS, prayer))
            {
                continue;
            }

            return prayer;
        }

        return null;
    }

    /**
     * Flicks the given prayers
     * @param prayers The prayers to flick
     */
    public static void flick(Collection<PrayerAPI> prayers)
    {
        PrayerAPI previous = getActiveOverhead();
        if (previous != null && !prayers.contains(previous) && previous.isActive())
        {
            //without this, you sometimes lose prayer points when switching protection prayers
            previous.toggle();
        }

        toggle(false, prayers);
        toggle(true, prayers);
    }

    private static void toggle(boolean skipValidate, Collection<PrayerAPI> prayers) {
        for (PrayerAPI prayer : prayers)
        {
            if (prayer.isActive() || skipValidate) //isActive states don't update until the next tick when flicking
            {
                prayer.toggle();
            }
        }
    }

    /**
     * Flicks the given prayers
     * @param prayers The prayers to flick
     */
    public static void flick(PrayerAPI... prayers)
    {
        flick(Set.of(prayers));
    }

    /**
     * flick quick prayers (turns on if off, then off -> on)
     */
    public static void flickQuickPrayer()
    {
        if(!isQuickPrayerEnabled())
        {
            turnOnQuickPrayers();
            return;
        }
        toggleQuickPrayer();
        toggleQuickPrayer();
    }

    /**
     * Turns on the prayer for the given client if it's not already active.
     *
     */
    public void turnOn()
    {
        if(isActive())
            return;
        toggle();
    }

    /**
     * Turns off the prayer for the given client if it's currently active.
     */
    public void turnOff()
    {
        if(!isActive())
            return;
        toggle();
    }

    /**
     * Toggles the prayer for the given client, only if the player meets the level requirements.
     */
    public void toggle()
    {
        Client client = Static.getClient();
        if(!hasLevelFor() || client.getBoostedSkillLevel(Skill.PRAYER) == 0)
            return;
        WidgetAPI.interact(1, interfaceId, -1, -1);
    }

    /**
     * Checks if the prayer is active for the given client.
     *
     * @return true if the prayer is active
     */
    public boolean isActive()
    {
        Client client = Static.getClient();
        return Static.invoke(() -> client.getVarbitValue(varbit) == 1);
    }

    /**
     * Returns the highest available ranged prayer for the given client.
     * @return the highest available ranged prayer, or null if none is available.
     */
    public static PrayerAPI getRangedPrayer()
    {
        return PrayerGroup.RANGED.getHighestAvailable();
    }

    /**
     * Returns the highest available magic prayer for the given client.
     * @return the highest available magic prayer, or null if none is available.
     */
    public static PrayerAPI getMagicPrayer()
    {
        return PrayerGroup.MAGIC.getHighestAvailable();
    }

    /**
     * Returns the highest available melee prayer for the given client (chivalry or piety).
     *
     * @return the highest available melee prayer, or null if none is available.
     */
    public static PrayerAPI getMeleePrayer()
    {
        return PrayerGroup.MELEE.getHighestAvailable();
    }

    /**
     * Returns the highest available melee attack prayer for the given client.
     *
     * @return the highest available melee attack prayer, or null if none is available.
     */
    public static PrayerAPI getMeleeAttackPrayer()
    {
        return PrayerGroup.MELEE_ATTACK.getHighestAvailable();
    }

    /**
     * Returns the highest available melee strength prayer for the given client.
     *
     * @return the highest available melee attack prayer, or null if none is available.
     */
    public static PrayerAPI getMeleeStrengthPrayer()
    {
        return PrayerGroup.MELEE_STRENGTH.getHighestAvailable();
    }

    /**
     * Returns the highest available melee defense prayer for the given client.
     *
     * @return the highest available melee attack prayer, or null if none is available.
     */
    public static PrayerAPI getMeleeDefensePrayer()
    {
        return PrayerGroup.MELEE_DEFENSE.getHighestAvailable();
    }

    /**
     * Disables all active prayers for the given client.
     */
    public static void disableAll()
    {
        for(PrayerAPI prayer : values())
        {
            prayer.turnOff();
        }
    }

    private enum PrayerGroup
    {
        RANGED(SHARP_EYE, HAWK_EYE, EAGLE_EYE, RIGOUR),
        MELEE(CHIVALRY, PIETY),
        MAGIC(MYSTIC_WILL, MYSTIC_LORE, MYSTIC_MIGHT, AUGURY),
        MELEE_STRENGTH(BURST_OF_STRENGTH, SUPERHUMAN_STRENGTH, ULTIMATE_STRENGTH),
        MELEE_ATTACK(CLARITY_OF_THOUGHT, IMPROVED_REFLEXES, INCREDIBLE_REFLEXES),
        MELEE_DEFENSE(THICK_SKIN, ROCK_SKIN, STEEL_SKIN),
        ;

        PrayerGroup(PrayerAPI... prayers)
        {
            prayerMap = prayers;
        }

        public PrayerAPI getHighestAvailable() {
            Client client = Static.getClient();
            int playerPrayerLevel = client.getRealSkillLevel(Skill.PRAYER);

            //to allot for lms
            int boostedLevel = client.getBoostedSkillLevel(Skill.PRAYER);
            if(boostedLevel == 99)
            {
                playerPrayerLevel = boostedLevel;
            }

            int finalPlayerPrayerLevel = playerPrayerLevel;
            return Arrays.stream(prayerMap)
                    .filter(prayer -> prayer.getLevel() <= finalPlayerPrayerLevel)
                    .reduce((a, b) -> b)
                    .orElse(null);
        }

        private final PrayerAPI[] prayerMap;
    }
}
