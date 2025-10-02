package com.tonic.api.threaded;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.TabsAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.Tab;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.http.api.worlds.WorldType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Threaded Minigames API
 */
public class Minigames
{
    private static final Supplier<Widget> MINIGAMES_TAB_BUTTON = () -> WidgetAPI.get(707, 6);
    private static final Supplier<Widget> MINIGAMES_DESTINATION = () -> WidgetAPI.get(76, 11);
    private static final Supplier<Widget> MINIGAMES_TELEPORT_BUTTON = () -> WidgetAPI.get(160, 30);

    private static final Set<Quest> NMZ_QUESTS = Set.of(
            Quest.THE_ASCENT_OF_ARCEUUS,
            Quest.CONTACT,
            Quest.THE_CORSAIR_CURSE,
            Quest.THE_DEPTHS_OF_DESPAIR,
            Quest.DESERT_TREASURE_I,
            Quest.DRAGON_SLAYER_I,
            Quest.DREAM_MENTOR,
            Quest.FAIRYTALE_I__GROWING_PAINS,
            Quest.FAMILY_CREST,
            Quest.FIGHT_ARENA,
            Quest.THE_FREMENNIK_ISLES,
            Quest.GETTING_AHEAD,
            Quest.THE_GRAND_TREE,
            Quest.THE_GREAT_BRAIN_ROBBERY,
            Quest.GRIM_TALES,
            Quest.HAUNTED_MINE,
            Quest.HOLY_GRAIL,
            Quest.HORROR_FROM_THE_DEEP,
            Quest.IN_SEARCH_OF_THE_MYREQUE,
            Quest.LEGENDS_QUEST,
            Quest.LOST_CITY,
            Quest.LUNAR_DIPLOMACY,
            Quest.MONKEY_MADNESS_I,
            Quest.MOUNTAIN_DAUGHTER,
            Quest.MY_ARMS_BIG_ADVENTURE,
            Quest.ONE_SMALL_FAVOUR,
            Quest.RECIPE_FOR_DISASTER,
            Quest.ROVING_ELVES,
            Quest.SHADOW_OF_THE_STORM,
            Quest.SHILO_VILLAGE,
            Quest.SONG_OF_THE_ELVES,
            Quest.TALE_OF_THE_RIGHTEOUS,
            Quest.TREE_GNOME_VILLAGE,
            Quest.TROLL_ROMANCE,
            Quest.TROLL_STRONGHOLD,
            Quest.VAMPYRE_SLAYER,
            Quest.WHAT_LIES_BELOW,
            Quest.WITCHS_HOUSE
    );

    /**
     * Checks if the player can currently use minigame teleport.
     *
     * @return true if the player can use minigame teleport, false otherwise
     */
    public static boolean canTeleport()
    {
        return getLastMinigameTeleportUsage().plus(20, ChronoUnit.MINUTES).isBefore(Instant.now());
    }

    /**
     * Attempts to teleport to the specified minigame destination.
     *
     * @param destination The destination to teleport to
     * @return true if the teleport was initiated, false otherwise
     */
    public static boolean teleport(Destination destination)
    {
        if (!canTeleport())
        {
            Logger.warn("Tried to minigame teleport, but it's on cooldown.");
            return false;
        }

        Widget minigamesTeleportButton = MINIGAMES_TELEPORT_BUTTON.get();
        List<Integer> teleportGraphics = List.of(800, 802, 803, 804);

        open();

        if (isOpen() && minigamesTeleportButton != null)
        {
            Client client = Static.getClient();

            if (Destination.getCurrent() != destination)
            {
                ClientScriptAPI.runScript(124, destination.index);
                Delays.tick();
            }

            if (teleportGraphics.contains(client.getLocalPlayer().getGraphic()))
            {
                return false;
            }

            WidgetAPI.interact(1, 4980766, destination.index);
            Delays.tick();
            return true;
        }

        return false;
    }

    /**
     * Opens the minigames tab and the minigames teleport interface.
     * @return true if the minigames teleport interface is open, false otherwise
     */
    public static boolean open()
    {
        if (!isTabOpen())
        {
            ClientScriptAPI.switchTabs(Tab.CLAN_TAB);
            Delays.tick();
        }

        if (!isOpen())
        {
            Widget widget = MINIGAMES_TAB_BUTTON.get();
            if (WidgetAPI.isVisible(widget))
            {
                WidgetAPI.interact(widget, "Grouping");
                Delays.tick();
            }
        }
        Delays.tick();
        return isOpen();
    }

    /**
     * Checks if the minigames teleport interface is currently open.
     *
     * @return true if the minigames teleport interface is open, false otherwise
     */
    public static boolean isOpen()
    {
        return WidgetAPI.isVisible(MINIGAMES_TELEPORT_BUTTON.get());
    }

    /**
     * Checks if the minigames tab is currently open.
     *
     * @return true if the minigames tab is open, false otherwise
     */
    public static boolean isTabOpen()
    {
        return TabsAPI.isOpen(Tab.CLAN_TAB);
    }

    /**
     * Gets the time of the last minigame teleport usage.
     *
     * @return An {@link Instant} representing the time of the last minigame teleport usage
     */
    public static Instant getLastMinigameTeleportUsage()
    {
        return Instant.ofEpochSecond(VarAPI.getVarp(VarPlayer.LAST_MINIGAME_TELEPORT) * 60L);
    }

    @Getter
    @AllArgsConstructor
    public enum Destination
    {
        BARBARIAN_ASSAULT(1, "Barbarian Assault", new WorldPoint(2531, 3577, 0), false),
        BLAST_FURNACE(2, "Blast Furnace", new WorldPoint(2933, 10183, 0), true),
        BURTHORPE_GAMES_ROOM(3, "Burthorpe Games Room", new WorldPoint(2208, 4938, 0), true),
        CASTLE_WARS(4, "Castle Wars", new WorldPoint(2439, 3092, 0), false),
        CLAN_WARS(5, "Clan Wars", new WorldPoint(3151, 3636, 0), false),
        DAGANNOTH_KINGS(6, "Dagannoth Kings", null, true),
        FISHING_TRAWLER(7, "Fishing Trawler", new WorldPoint(2658, 3158, 0), true),
        GIANTS_FOUNDARY(8, "Giants' Foundry", new WorldPoint(3361, 3147, 0), true),
        GOD_WARS(9, "God Wars", null, true),
        GUARDIANS_OF_THE_RIFT(10, "Guardians of the Rift", new WorldPoint(3616, 9478, 0), true),
        LAST_MAN_STANDING(11, "Last Man Standing", new WorldPoint(3149, 3635, 0), false),
        MAGE_TRAINING_ARENA(12, "Nightmare Zone", new WorldPoint(3363, 3304, 0), true),
        NIGHTMARE_ZONE(13, "Nightmare Zone", new WorldPoint(2611, 3121, 0), true),
        PEST_CONTROL(14, "Pest Control", new WorldPoint(2653, 2655, 0), true),
        PLAYER_OWNED_HOUSES(15, "Player Owned Houses", null, false),
        RAT_PITS(16, "Rat Pits", new WorldPoint(3263, 3406, 0), true),
        SHADES_OF_MORTTON(17, "Shades of Mort'ton", new WorldPoint(3500, 3300, 0), true),
        SHIELD_OF_ARRAV(18, "Shield of Arrav", null, true),
        SHOOTING_STARS(19, "Shooting Stars", null, true),
        SOUL_WARS(20, "Soul Wars", new WorldPoint(2209, 2857, 0), true),
        THEATRE_OF_BLOOD(21, "Theatre of Blood", null, true),
        TITHE_FARM(22, "Tithe Farm", new WorldPoint(1793, 3501, 0), true),
        TROUBLE_BREWING(23, "Trouble Brewing", new WorldPoint(3811, 3021, 0), true),
        TZHAAR_FIGHT_PIT(24, "TzHaar Fight Pit", new WorldPoint(2402, 5181, 0), true),
        VOLCANIC_MINE(25, "Volcanic Mine", null, true),
        NONE(-1, "None", null, false);

        private final int index;
        private final String name;
        private final WorldPoint location;
        private final boolean members;

        /**
         * Checks if the player can use this minigame teleport destination.
         *
         * @return true if the player can use this minigame teleport destination, false otherwise
         */
        public boolean canUse()
        {
            if (!hasDestination())
            {
                return false;
            }

            if (members && !WorldsAPI.getCurrentWorld().getTypes().contains(WorldType.MEMBERS))
            {
                return false;
            }

            Client client = Static.getClient();
            switch (this)
            {
                case BURTHORPE_GAMES_ROOM:
                case CASTLE_WARS:
                case CLAN_WARS:
                case LAST_MAN_STANDING:
                case SOUL_WARS:
                case TZHAAR_FIGHT_PIT:
                case GIANTS_FOUNDARY:
                    return true;
                case BARBARIAN_ASSAULT:
                    return VarAPI.getVar(3251) >= 1;
                case BLAST_FURNACE:
                    return VarAPI.getVar(575) >= 1;
                case FISHING_TRAWLER:
                    return Static.invoke(() -> client.getRealSkillLevel(Skill.FISHING) >= 15);
                case GUARDIANS_OF_THE_RIFT:
                    return Quest.TEMPLE_OF_THE_EYE.getState(client) == QuestState.FINISHED;
                case NIGHTMARE_ZONE:
                    return NMZ_QUESTS.stream().filter(quest -> quest.getState(client) == QuestState.FINISHED).count() >= 5;
                case PEST_CONTROL:
                    return client.getLocalPlayer().getCombatLevel() >= 40;
                case RAT_PITS:
                    return Quest.RATCATCHERS.getState(client) == QuestState.FINISHED;
                case SHADES_OF_MORTTON:
                    return Quest.SHADES_OF_MORTTON.getState(client) == QuestState.FINISHED;
                case TROUBLE_BREWING:
                    return Quest.CABIN_FEVER.getState(client) == QuestState.FINISHED && Static.invoke(() -> client.getRealSkillLevel(Skill.COOKING) >= 40);
                case TITHE_FARM:
					return Static.invoke(() -> client.getRealSkillLevel(Skill.FARMING) >= 34) && (VarAPI.getVar(Varbits.KOUREND_FAVOR_HOSIDIUS) / 10) >= 100;
            }
            return false;
        }

        /**
         * Checks if this destination has a valid location.
         *
         * @return true if this destination has a valid location, false otherwise
         */
        public boolean hasDestination()
        {
            return location != null;
        }

        /**
         * Gets the currently selected minigame teleport destination.
         *
         * @return The currently selected minigame teleport destination, or {@link Destination#NONE} if none is selected
         */
        public static Destination getCurrent()
        {
            Widget selectedTeleport = MINIGAMES_DESTINATION.get();
            if (WidgetAPI.isVisible(selectedTeleport))
            {
                return byName(selectedTeleport.getText());
            }

            return NONE;
        }

        /**
         * Finds a minigame teleport destination by its name.
         *
         * @param name The name of the destination
         * @return The matching destination, or {@link Destination#NONE} if no match is found
         */
        public static Destination byName(String name)
        {
            return Arrays.stream(values())
                    .filter(x -> x.getName().equals(name))
                    .findFirst()
                    .orElse(NONE);
        }

        /**
         * Finds a minigame teleport destination by its index.
         *
         * @param index The index of the destination
         * @return The matching destination, or {@link Destination#NONE} if no match is found
         */
        public static Destination of(int index)
        {
            return Arrays.stream(values())
                    .filter(x -> x.getIndex() == index)
                    .findFirst()
                    .orElse(NONE);
        }
    }
}