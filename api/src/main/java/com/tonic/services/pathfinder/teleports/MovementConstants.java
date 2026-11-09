package com.tonic.services.pathfinder.teleports;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import org.apache.commons.lang3.tuple.Pair;
import java.util.List;
import java.util.Map;

public class MovementConstants
{
    public static final WorldArea WILDERNESS_ABOVE_GROUND = new WorldArea(2944, 3523, 448, 448, 0);
    public static final WorldArea WILDERNESS_UNDERGROUND = new WorldArea(2944, 9918, 320, 442, 0);
    public static final Map<Integer, Pair<WorldPoint, WorldPoint>> DRAYNOR_MANOR_BASEMENT_DOORS = Map.of(
            VarbitID.ERNESTDOOR_4TO7, Pair.of(new WorldPoint(3108, 9757, 0), new WorldPoint(3108, 9759, 0)),
            VarbitID.ERNESTDOOR_4TO5, Pair.of(new WorldPoint(3104, 9760, 0), new WorldPoint(3106, 9760, 0)),
            VarbitID.ERNESTDOOR_5TO8, Pair.of(new WorldPoint(3102, 9757, 0), new WorldPoint(3102, 9759, 0)),
            VarbitID.ERNESTDOOR_5TO6, Pair.of(new WorldPoint(3101, 9760, 0), new WorldPoint(3098, 9760, 0)),
            VarbitID.ERNESTDOOR_3TO6, Pair.of(new WorldPoint(3097, 9762, 0), new WorldPoint(3097, 9764, 0)),
            VarbitID.ERNESTDOOR_2TO3, Pair.of(new WorldPoint(3099, 9765, 0), new WorldPoint(3101, 9765, 0)),
            VarbitID.ERNESTDOOR_1TO2, Pair.of(new WorldPoint(3104, 9765, 0), new WorldPoint(3106, 9765, 0)),
            VarbitID.ERNESTDOOR_2TO5, Pair.of(new WorldPoint(3102, 9764, 0), new WorldPoint(3102, 9762, 0)),
            VarbitID.ERNESTDOOR_8TO9, Pair.of(new WorldPoint(3101, 9755, 0), new WorldPoint(3099, 9755, 0))
    );
    public static WorldPoint HOUSE_POINT = new WorldPoint(10000, 4000, 1);

    public static final int[] RING_OF_DUELING = new int[]
            {
                    ItemID.RING_OF_DUELING_8,
                    ItemID.RING_OF_DUELING_7,
                    ItemID.RING_OF_DUELING_6,
                    ItemID.RING_OF_DUELING_5,
                    ItemID.RING_OF_DUELING_4,
                    ItemID.RING_OF_DUELING_3,
                    ItemID.RING_OF_DUELING_2,
                    ItemID.RING_OF_DUELING_1,
            };

    public static final int[] GAMES_NECKLACE = new int[]
            {
                    ItemID.NECKLACE_OF_MINIGAMES_8,
                    ItemID.NECKLACE_OF_MINIGAMES_7,
                    ItemID.NECKLACE_OF_MINIGAMES_6,
                    ItemID.NECKLACE_OF_MINIGAMES_5,
                    ItemID.NECKLACE_OF_MINIGAMES_4,
                    ItemID.NECKLACE_OF_MINIGAMES_3,
                    ItemID.NECKLACE_OF_MINIGAMES_2,
                    ItemID.NECKLACE_OF_MINIGAMES_1,
            };

    public static final int[] COMBAT_BRACELET = new int[]
            {
                    ItemID.JEWL_BRACELET_OF_COMBAT_1,
                    ItemID.JEWL_BRACELET_OF_COMBAT_2,
                    ItemID.JEWL_BRACELET_OF_COMBAT_3,
                    ItemID.JEWL_BRACELET_OF_COMBAT_4,
                    ItemID.JEWL_BRACELET_OF_COMBAT_5,
                    ItemID.JEWL_BRACELET_OF_COMBAT_6,
            };

    public static final int[] RING_OF_WEALTH = new int[]
            {
                    ItemID.RING_OF_WEALTH_5,
                    ItemID.RING_OF_WEALTH_4,
                    ItemID.RING_OF_WEALTH_3,
                    ItemID.RING_OF_WEALTH_2,
                    ItemID.RING_OF_WEALTH_1,
            };

    public static final int[] AMULET_OF_GLORY = new int[]
            {
                    ItemID.AMULET_OF_GLORY_6,
                    ItemID.AMULET_OF_GLORY_5,
                    ItemID.AMULET_OF_GLORY_4,
                    ItemID.AMULET_OF_GLORY_3,
                    ItemID.AMULET_OF_GLORY_2,
                    ItemID.AMULET_OF_GLORY_1,
                    ItemID.AMULET_OF_GLORY_INF,
            };

    public static final int[] NECKLACE_OF_PASSAGE = new int[]
            {
                    ItemID.NECKLACE_OF_PASSAGE_1,
                    ItemID.NECKLACE_OF_PASSAGE_2,
                    ItemID.NECKLACE_OF_PASSAGE_3,
                    ItemID.NECKLACE_OF_PASSAGE_4,
                    ItemID.NECKLACE_OF_PASSAGE_5,
            };

    public static final int[] BURNING_AMULET = new int[]
            {
                    ItemID.BURNING_AMULET_5,
                    ItemID.BURNING_AMULET_4,
                    ItemID.BURNING_AMULET_3,
                    ItemID.BURNING_AMULET_2,
                    ItemID.BURNING_AMULET_1,
            };

    public static final int[] XERICS_TALISMAN = new int[]
            {
                    ItemID.XERIC_TALISMAN
            };

    public static final int[] SLAYER_RING = new int[]
            {
                    ItemID.SLAYER_RING_ETERNAL,
                    ItemID.SLAYER_RING_8,
                    ItemID.SLAYER_RING_7,
                    ItemID.SLAYER_RING_6,
                    ItemID.SLAYER_RING_5,
                    ItemID.SLAYER_RING_4,
                    ItemID.SLAYER_RING_3,
                    ItemID.SLAYER_RING_2,
                    ItemID.SLAYER_RING_1,
            };

    public static final int[] DIGSITE_PENDANT = new int[]
            {
                    ItemID.NECKLACE_OF_DIGSITE_5,
                    ItemID.NECKLACE_OF_DIGSITE_4,
                    ItemID.NECKLACE_OF_DIGSITE_3,
                    ItemID.NECKLACE_OF_DIGSITE_2,
                    ItemID.NECKLACE_OF_DIGSITE_1,
            };

    public static final int[] DRAKANS_MEDALLION = new int[]
            {
                    ItemID.DRAKANS_MEDALLION
            };

    public static final int[] SKILLS_NECKLACE = new int[]
            {
                    ItemID.JEWL_NECKLACE_OF_SKILLS_6,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_5,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_4,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_3,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_2,
                    ItemID.JEWL_NECKLACE_OF_SKILLS_1,
            };

    public static final int[] TELEPORT_CRYSTAL = new int[]
            {
                    ItemID.PRIF_TELEPORT_CRYSTAL,
                    ItemID.MOURNING_TELEPORT_CRYSTAL_5,
                    ItemID.MOURNING_TELEPORT_CRYSTAL_4,
                    ItemID.MOURNING_TELEPORT_CRYSTAL_5,
                    ItemID.MOURNING_TELEPORT_CRYSTAL_2,
                    ItemID.MOURNING_TELEPORT_CRYSTAL_1
            };

    public static final int[] ENCHANTED_LYRE = new int[]
            {
                    ItemID.MAGIC_STRUNG_LYRE_INFINITE,
                    ItemID.MAGIC_STRUNG_LYRE_5,
                    ItemID.MAGIC_STRUNG_LYRE_4,
                    ItemID.MAGIC_STRUNG_LYRE_3,
                    ItemID.MAGIC_STRUNG_LYRE_2,
                    ItemID.MAGIC_STRUNG_LYRE
            };

    public static final int[] SLASH_ITEMS = new int[]
            {
                    ItemID.KNIFE,
                    ItemID.WILDERNESS_SWORD_EASY,
                    ItemID.WILDERNESS_SWORD_MEDIUM,
                    ItemID.WILDERNESS_SWORD_HARD,
                    ItemID.WILDERNESS_SWORD_ELITE,
                    ItemID.ADAMANT_LONGSWORD,
                    ItemID.ADAMANT_SCIMITAR,
                    ItemID.WEARABLE_SAW,
                    ItemID.ARCLIGHT,
                    ItemID.ARKAN_BLADE,
                    ItemID.BLACK_LONGSWORD,
                    ItemID.BLACK_SCIMITAR,
                    ItemID.BLADE_OF_SAELDOR,
                    ItemID.BLADE_OF_SAELDOR_INFINITE,
                    ItemID.BLISTERWOOD_SICKLE,
                    ItemID.FALADIAN_SWORD,
                    ItemID.OLAF2_BRINE_SABRE,
                    ItemID.BRONZE_LONGSWORD,
                    ItemID.BRONZE_SCIMITAR,
                    ItemID.HUNDRED_CLEAVER,
                    ItemID.DEADMAN_BLIGHTED_VOIDWAKER,
                    ItemID.DARKLIGHT,
                    ItemID.DRAGON_LONGSWORD,
                    ItemID.BH_DRAGON_LONGSWORD_IMBUE,
                    ItemID.BH_DRAGON_LONGSWORD_CORRUPTED,
                    ItemID.DRAGON_SCIMITAR,
                    ItemID.BH_DRAGON_SCIMITAR_CORRUPTED,
                    ItemID.DRAGON_SCIMITAR_ORNAMENT,
                    ItemID.EMBERLIGHT,
                    ItemID.SILVER_SICKLE_EMERALD,
                    ItemID.SILVER_SICKLE_ENCHANTED,
                    ItemID.SILVER_SICKLE_RUBY_ENCHANTED,
                    ItemID.EXCALIBUR,
                    ItemID.VIKING_SWORD,
                    ItemID.RUNE_SCIMITAR_GOLD,
                    ItemID.GRANITE_LONGSWORD,
                    ItemID.FEVER_CUTLASS_BRASSHAND_HARRY,
                    ItemID.IRON_LONGSWORD,
                    ItemID.IRON_SCIMITAR,
                    ItemID.IRON_SICKLE,
                    ItemID.MACHETTE_JADE,
                    ItemID.KATANA,
                    ItemID.FEVER_CUTLASS_UNLUCKY_JENKINS_LUCKY_CUTLASS,
                    ItemID.MACHETTE,
                    ItemID.MITHRIL_LONGSWORD,
                    ItemID.MITHRIL_SCIMITAR,
                    ItemID.MACHETTE_OPAL,
                    ItemID.MACHETTE_REDTOPAZ,
                    ItemID.SILVER_SICKLE_RUBY,
                    ItemID.RUNE_LONGSWORD,
                    ItemID.RUNE_SCIMITAR,
                    ItemID.SILVER_SICKLE,
                    ItemID.SILVER_SICKLE_BLESSED,
                    ItemID.SILVERLIGHT,
                    ItemID.HUNDRED_SKEWER,
                    ItemID.DEADMAN_STARTER_SWORD,
                    ItemID.STEEL_LONGSWORD,
                    ItemID.STEEL_SCIMITAR,
                    ItemID.SULPHUR_BLADES,
                    ItemID.TZHAAR_KNIFE,
                    ItemID.BH_VESTAS_LONGSWORD,
                    ItemID.VESTAS_LONGSWORD_BH,
                    ItemID.VOIDWAKER,
                    ItemID.DEADMAN_VOIDWAKER,
                    ItemID.WHITE_LONGSWORD,
                    ItemID.WHITE_SCIMITAR,
                    ItemID.HUNDRED_SPOON
            };

    public static final List<Pair<WorldPoint, WorldPoint>> SLASH_WEB_POINTS = List.<Pair<WorldPoint, WorldPoint>>of(
            Pair.of(new WorldPoint(3031, 3852, 0), new WorldPoint(3029, 3852, 0)),
            Pair.of(new WorldPoint(3148, 3727, 0), new WorldPoint(3146, 3727, 0)),
            Pair.of(new WorldPoint(3147, 3728, 0), new WorldPoint(3147, 3726, 0)),
            Pair.of(new WorldPoint(3164, 3736, 0), new WorldPoint(3162, 3736, 0)),
            Pair.of(new WorldPoint(3163, 3737, 0), new WorldPoint(3163, 3735, 0)),
            Pair.of(new WorldPoint(3183, 3734, 0), new WorldPoint(3183, 3732, 0)),
            Pair.of(new WorldPoint(3158, 3952, 0), new WorldPoint(3158, 3950, 0)),
            Pair.of(new WorldPoint(3210, 9899, 0), new WorldPoint(3210, 9897, 0)),
            Pair.of(new WorldPoint(3115, 3860, 0), new WorldPoint(3115, 3858, 0)),
            Pair.of(new WorldPoint(3093, 3957, 0), new WorldPoint(3091, 3957, 0)),
            Pair.of(new WorldPoint(3096, 3957, 0), new WorldPoint(3094, 3957, 0)),
            Pair.of(new WorldPoint(3105, 3959, 0), new WorldPoint(3105, 3957, 0)),
            Pair.of(new WorldPoint(3106, 3959, 0), new WorldPoint(3106, 3957, 0)),
            Pair.of(new WorldPoint(2654, 9767, 0), new WorldPoint(2654, 9765, 0)),
            Pair.of(new WorldPoint(2566, 3124, 0), new WorldPoint(2564, 3124, 0)),
            Pair.of(new WorldPoint(2565, 3125, 0), new WorldPoint(2565, 3123, 0)),
            Pair.of(new WorldPoint(2569, 3119, 0), new WorldPoint(2569, 3117, 0)),
            Pair.of(new WorldPoint(2570, 3119, 0), new WorldPoint(2570, 3117, 0)),
            Pair.of(new WorldPoint(2574, 3125, 0), new WorldPoint(2574, 3123, 0)),
            Pair.of(new WorldPoint(2631, 9248, 0), new WorldPoint(2629, 9248, 0)),
            Pair.of(new WorldPoint(2632, 9264, 0), new WorldPoint(2630, 9264, 0)),
            Pair.of(new WorldPoint(2628, 9231, 1), new WorldPoint(2628, 9229, 1)),
            Pair.of(new WorldPoint(2629, 9239, 1), new WorldPoint(2629, 9237, 1)),
            Pair.of(new WorldPoint(2647, 9118, 0), new WorldPoint(2647, 9116, 0)),
            Pair.of(new WorldPoint(2638, 9092, 1), new WorldPoint(2638, 9090, 1)),
            Pair.of(new WorldPoint(2653, 9124, 1), new WorldPoint(2653, 9122, 1)),
            Pair.of(new WorldPoint(2663, 9110, 1), new WorldPoint(2663, 9108, 1)),
            Pair.of(new WorldPoint(2633, 9200, 0), new WorldPoint(2633, 9198, 0)),
            Pair.of(new WorldPoint(2646, 9190, 0), new WorldPoint(2644, 9190, 0)),
            Pair.of(new WorldPoint(2648, 9199, 0), new WorldPoint(2648, 9197, 0)),
            Pair.of(new WorldPoint(2662, 9206, 0), new WorldPoint(2662, 9204, 0)),
            Pair.of(new WorldPoint(2666, 9160, 0), new WorldPoint(2664, 9160, 0)),
            Pair.of(new WorldPoint(2668, 9194, 0), new WorldPoint(2666, 9194, 0)),
            Pair.of(new WorldPoint(2541, 9069, 1), new WorldPoint(2539, 9069, 1)),
            Pair.of(new WorldPoint(2547, 9064, 1), new WorldPoint(2547, 9062, 1)),
            Pair.of(new WorldPoint(2551, 9054, 1), new WorldPoint(2551, 9052, 1)),
            Pair.of(new WorldPoint(2555, 9039, 1), new WorldPoint(2553, 9039, 1)),
            Pair.of(new WorldPoint(2604, 9273, 1), new WorldPoint(2602, 9273, 1)),
            Pair.of(new WorldPoint(2618, 9211, 1), new WorldPoint(2618, 9209, 1)),
            Pair.of(new WorldPoint(2620, 9205, 1), new WorldPoint(2620, 9203, 1)),
            Pair.of(new WorldPoint(2571, 9051, 0), new WorldPoint(2569, 9051, 0)),
            Pair.of(new WorldPoint(2570, 9052, 0), new WorldPoint(2570, 9050, 0)),
            Pair.of(new WorldPoint(2599, 9080, 1), new WorldPoint(2597, 9080, 1)),
            Pair.of(new WorldPoint(2608, 9079, 1), new WorldPoint(2606, 9079, 1)),
            Pair.of(new WorldPoint(2610, 9047, 1), new WorldPoint(2610, 9045, 1)),
            Pair.of(new WorldPoint(2613, 9057, 1), new WorldPoint(2613, 9055, 1)),
            Pair.of(new WorldPoint(2619, 9071, 1), new WorldPoint(2617, 9071, 1)),
            Pair.of(new WorldPoint(2618, 9072, 1), new WorldPoint(2618, 9070, 1)),
            Pair.of(new WorldPoint(2674, 9039, 0), new WorldPoint(2674, 9037, 0)),
            Pair.of(new WorldPoint(2633, 9049, 1), new WorldPoint(2633, 9047, 1)),
            Pair.of(new WorldPoint(2639, 9062, 1), new WorldPoint(2637, 9062, 1)),
            Pair.of(new WorldPoint(2638, 9063, 1), new WorldPoint(2638, 9061, 1)),
            Pair.of(new WorldPoint(2645, 9056, 1), new WorldPoint(2643, 9056, 1)),
            Pair.of(new WorldPoint(2655, 9073, 1), new WorldPoint(2653, 9073, 1)),
            Pair.of(new WorldPoint(2654, 9074, 1), new WorldPoint(2654, 9072, 1)),
            Pair.of(new WorldPoint(2657, 9082, 1), new WorldPoint(2655, 9082, 1)),
            Pair.of(new WorldPoint(2676, 9074, 1), new WorldPoint(2674, 9074, 1)),
            Pair.of(new WorldPoint(2678, 9061, 1), new WorldPoint(2678, 9059, 1)),
            Pair.of(new WorldPoint(2678, 9068, 1), new WorldPoint(2678, 9066, 1)),
            Pair.of(new WorldPoint(1833, 9945, 0), new WorldPoint(1833, 9943, 0)),
            Pair.of(new WorldPoint(1841, 9934, 0), new WorldPoint(1841, 9932, 0)),
            Pair.of(new WorldPoint(1843, 9933, 0), new WorldPoint(1841, 9933, 0)),
            Pair.of(new WorldPoint(1842, 9934, 0), new WorldPoint(1842, 9932, 0)),
            Pair.of(new WorldPoint(1849, 9935, 0), new WorldPoint(1849, 9933, 0)),
            Pair.of(new WorldPoint(1850, 9935, 0), new WorldPoint(1850, 9933, 0)),
            Pair.of(new WorldPoint(1848, 9919, 0), new WorldPoint(1846, 9919, 0)),
            Pair.of(new WorldPoint(1847, 9920, 0), new WorldPoint(1847, 9918, 0))
    );

    public static final int[] geTransports = {
            ItemID.POH_TABLET_VARROCKTELEPORT,
            ItemID.JEWL_NECKLACE_OF_SKILLS_1,
            ItemID.JEWL_NECKLACE_OF_SKILLS_2,
            ItemID.JEWL_NECKLACE_OF_SKILLS_3,
            ItemID.JEWL_NECKLACE_OF_SKILLS_4,
            ItemID.JEWL_NECKLACE_OF_SKILLS_5,
            ItemID.JEWL_NECKLACE_OF_SKILLS_6,
            ItemID.JEWL_BRACELET_OF_COMBAT_1,
            ItemID.JEWL_BRACELET_OF_COMBAT_2,
            ItemID.JEWL_BRACELET_OF_COMBAT_3,
            ItemID.JEWL_BRACELET_OF_COMBAT_4,
            ItemID.JEWL_BRACELET_OF_COMBAT_5,
            ItemID.JEWL_BRACELET_OF_COMBAT_6,
            ItemID.RING_OF_WEALTH_1,
            ItemID.RING_OF_WEALTH_2,
            ItemID.RING_OF_WEALTH_3,
            ItemID.RING_OF_WEALTH_4,
            ItemID.RING_OF_WEALTH_5,
            ItemID.RING_OF_WEALTH_I1,
            ItemID.RING_OF_WEALTH_I2,
            ItemID.RING_OF_WEALTH_I3,
            ItemID.RING_OF_WEALTH_I4,
            ItemID.RING_OF_WEALTH_I5,
            ItemID.TELEPORTSCROLL_LUMBERYARD,
            ItemID.AMULET_OF_GLORY_INF,
            ItemID.AMULET_OF_GLORY_1,
            ItemID.AMULET_OF_GLORY_2,
            ItemID.AMULET_OF_GLORY_3,
            ItemID.AMULET_OF_GLORY_4,
            ItemID.AMULET_OF_GLORY_5,
            ItemID.AMULET_OF_GLORY_6,
            ItemID.POH_TABLET_LUMBRIDGETELEPORT
    };

    public static final int[] QUETZAL_WHISTLE = {
            ItemID.HG_QUETZALWHISTLE_BASIC,
            ItemID.HG_QUETZALWHISTLE_ENHANCED,
            ItemID.HG_QUETZALWHISTLE_PERFECTED
    };

    public static final int[] ARDOUGNE_CLOAK = {
            ItemID.ARDY_CAPE_EASY,
            ItemID.ARDY_CAPE_MEDIUM,
            ItemID.ARDY_CAPE_HARD,
            ItemID.ARDY_CAPE_ELITE
    };
}