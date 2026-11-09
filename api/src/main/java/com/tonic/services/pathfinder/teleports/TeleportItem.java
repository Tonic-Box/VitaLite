package com.tonic.services.pathfinder.teleports;

import com.tonic.data.wrappers.PlayerEx;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.InventoryAPI;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.Quest;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.coords.WorldPoint;
import org.apache.commons.lang3.ArrayUtils;

public enum TeleportItem
{
    // Tabs
    WATERBIRTH_TELEPORT_TAB(new WorldPoint(2546, 3757, 0), "Break", null, ItemID.LUNAR_TABLET_WATERBIRTH_TELEPORT),
    KHAZARD_TELEPORT_TAB(new WorldPoint(2637, 3166, 0), "Break", null, ItemID.LUNAR_TABLET_KHAZARD_TELEPORT),
    VARROCK_TELEPORT_TAB(new WorldPoint(3212, 3424, 0), "Break", null, ItemID.POH_TABLET_VARROCKTELEPORT),
    LUMBRIDGE_TELEPORT_TAB(new WorldPoint(3225, 3219, 0), "Break", null, ItemID.POH_TABLET_LUMBRIDGETELEPORT),
    FALADOR_TELEPORT_TAB(new WorldPoint(2966, 3379, 0), "Break", null, ItemID.POH_TABLET_FALADORTELEPORT),
    CAMELOT_TELEPORT_TAB(new WorldPoint(2757, 3479, 0), "Break", null, ItemID.POH_TABLET_CAMELOTTELEPORT),
    ARDOUGNE_TELEPORT_TAB(new WorldPoint(2661, 3300, 0), "Break", Quest.PLAGUE_CITY, ItemID.POH_TABLET_ARDOUGNETELEPORT),
    WEST_ARDOUGNE_TELEPORT_TAB(new WorldPoint(2500, 3290, 0), "Break", null, ItemID.TELETAB_WESTARDY),
    RIMMINGTON_TELEPORT_TAB(new WorldPoint(2954, 3224, 0), "Break", null, ItemID.NZONE_TELETAB_RIMMINGTON),
    TAVERLEY_TELEPORT_TAB(new WorldPoint(2894, 3465, 0), "Break", null, ItemID.NZONE_TELETAB_TAVERLEY),
    RELLEKKA_TELEPORT_TAB(new WorldPoint(2668, 3631, 0), "Break", null, ItemID.NZONE_TELETAB_RELLEKKA),
    BRIMHAVEN_TELEPORT_TAB(new WorldPoint(2758, 3178, 0), "Break", null, ItemID.NZONE_TELETAB_BRIMHAVEN),
    POLLNIVNEACH_TELEPORT_TAB(new WorldPoint(3340, 3004, 0), "Break", null, ItemID.NZONE_TELETAB_POLLNIVNEACH),
    YANILLE_TELEPORT_TAB(new WorldPoint(2544, 3095, 0), "Break", null, ItemID.NZONE_TELETAB_YANILLE),
    HOSIDIUS_TELEPORT_TAB(new WorldPoint(1744, 3517, 0), "Break", null, ItemID.NZONE_TELETAB_KOUREND),
    SALVE_GRAVEYARD_TELEPORT_TAB(new WorldPoint(3432, 3460, 0), "Break", null, ItemID.TELETAB_SALVE),
    DRAYNOR_MANOR_TELEPORT_TAB(new WorldPoint(3109, 3350, 0), "Break", null, ItemID.TELETAB_DRAYNOR),
    FENKENSTRAINS_CASTLE_TELEPORT_TAB(new WorldPoint(3549, 3530, 0), "Break", null, ItemID.TELETAB_FENK),

    RIMMINGTON_TAB(new WorldPoint(2954, 3223, 0), "Break", null, ItemID.NZONE_TELETAB_RIMMINGTON),
    TAVERLEY_TAB(new WorldPoint(2894, 3466, 0), "Break", null, ItemID.NZONE_TELETAB_TAVERLEY),
    POLLNIVNEACH_TAB(new WorldPoint(3339, 3004, 0), "Break", null, ItemID.NZONE_TELETAB_POLLNIVNEACH),
    HOSIDIUS_TAB(new WorldPoint(1744, 3518, 0), "Break", null, ItemID.NZONE_TELETAB_KOUREND),
    RELLEKKA_TAB(new WorldPoint(2671, 3631, 0), "Break", null, ItemID.NZONE_TELETAB_RELLEKKA),
    BRIMHAVEN_TAB(new WorldPoint(2758, 3177, 0), "Break", null, ItemID.NZONE_TELETAB_BRIMHAVEN),
    YANILLE_TAB(new WorldPoint(2545, 3096, 0), "Break", null, ItemID.NZONE_TELETAB_YANILLE),
    TROLLHEIM_TAB(new WorldPoint(2890, 3679, 0), "Break", null, ItemID.NZONE_TELETAB_TROLLHEIM),
    PRIFDDINAS_TAB(new WorldPoint(3239, 6076, 0), "Break", null, ItemID.NZONE_TELETAB_PRIFDDINAS),

    // Scrolls
    FELDIP_HILLS_TELEPORT(new WorldPoint(2541, 2925, 0), "Teleport", null, ItemID.TELEPORTSCROLL_FELDIP),
    PISCATORIS_TELEPORT(new WorldPoint(2342, 3647, 0), "Teleport", null, ItemID.TELEPORTSCROLL_PISCATORIS),
    MORTTON_TELEPORT(new WorldPoint(3488, 3288, 0), "Teleport", null, ItemID.TELEPORTSCROLL_MORTTON),
    DIGSITE_TELEPORT(new WorldPoint(3324, 3412, 0), "Teleport", null, ItemID.TELEPORTSCROLL_DIGSITE),
    NARDAH_TELEPORT(new WorldPoint(3420, 2917, 0), "Teleport", null, ItemID.TELEPORTSCROLL_NARDAH),
    LUMBERYARD_TELEPORT(new WorldPoint(3302, 3486, 0), "Teleport", null, ItemID.TELEPORTSCROLL_LUMBERYARD),
    TAI_BWO_TELEPORT(new WorldPoint(2789, 3066, 0), "Teleport",Quest.TAI_BWO_WANNAI_TRIO, ItemID.TELEPORTSCROLL_TAIBWO),
    ZULANDRA_TELEPORT(new WorldPoint(2197, 3055, 0), "Teleport", null, ItemID.TELEPORTSCROLL_ZULANDRA),
    IORWERTH_TELEPORT(new WorldPoint(2194, 3258, 0), "Teleport", null, ItemID.TELEPORTSCROLL_ELF),
    LUNAR_ISLE_TELEPORT(new WorldPoint(2093, 3912, 0), "Teleport", null, ItemID.TELEPORTSCROLL_LUNARISLE),
    MOS_LEHARMLESS_TELEPORT(new WorldPoint(3701, 2996, 0), "Teleport", null, ItemID.TELEPORTSCROLL_MOSLES),
    PEST_CONTROL_TELEPORT(new WorldPoint(2657, 2660, 0), "Teleport", null, ItemID.TELEPORTSCROLL_PESTCONTROL),
    KEY_MASTER_TELEPORT(new WorldPoint(2686, 9882, 0), "Teleport", null, ItemID.TELEPORTSCROLL_CERBERUS),
    REVENANT_CAVE_TELEPORT(new WorldPoint(3127, 3833, 0), "Teleport", null, ItemID.TELEPORTSCROLL_REVENANTS),
    WATSON_TELEPORT(new WorldPoint(1645, 3579, 0), "Teleport", null, ItemID.TELEPORTSCROLL_WATSON),

    // Misc consumables
    WEISS_ICY_BASALT(new WorldPoint(2846, 3940, 0), "Weiss", Quest.MAKING_FRIENDS_WITH_MY_ARM, ItemID.WEISS_TELEPORT_BASALT),
    TROLL_STRONGHOLD(new WorldPoint(2838, 3693, 0), "Troll Stronghold", Quest.MAKING_FRIENDS_WITH_MY_ARM, ItemID.STRONGHOLD_TELEPORT_BASALT),
    TROLL_STRONGHOLD_OUTSIDE(new WorldPoint(2844, 3693, 0), "Troll Stronghold", Quest.MAKING_FRIENDS_WITH_MY_ARM, ItemID.STRONGHOLD_TELEPORT_BASALT),

    // Reusable
    ROYAL_SEED_POD(new WorldPoint(2465, 3495, 0), "Commune", Quest.MONKEY_MADNESS_II, ItemID.MM2_ROYAL_SEED_POD),
    ECTOPHIAL(new WorldPoint(3659, 3523, 0), "Empty", null, ItemID.ECTOPHIAL),
    TELEPORT_CRYSTAL_LLETYA(new WorldPoint(2330, 3172, 0), "Lletya", null, MovementConstants.TELEPORT_CRYSTAL),
    TELEPORT_CRYSTAL_PRIFDDINAS(new WorldPoint(3264, 6065, 0), "Prifddinas", Quest.SONG_OF_THE_ELVES, MovementConstants.TELEPORT_CRYSTAL),
    QUETZAL_WHISTLE(new WorldPoint(1585,3053,0),"Signal", Quest.CHILDREN_OF_THE_SUN, MovementConstants.QUETZAL_WHISTLE),

    // TODO move these to teleport loader since they are equippable...
    //	CHAMPIONS_GUILD_CHRONICLE(new WorldPoint(3202, 3357, 0), "Champions' Guild", null, ItemID.CHRONICLE),
    RELLEKKKA_LYRE(new WorldPoint(2664, 3643, 0), "Rellekka", Quest.THE_FREMENNIK_TRIALS, MovementConstants.ENCHANTED_LYRE),
    WATERBIRTH_ISLAND_LYRE(new WorldPoint(2550, 3756, 0), "Waterbirth Island", Quest.THE_FREMENNIK_TRIALS, MovementConstants.ENCHANTED_LYRE),
    NEITIZNOT_LYRE(new WorldPoint(2336, 3801, 0), "Neitiznot", Quest.THE_FREMENNIK_TRIALS, MovementConstants.ENCHANTED_LYRE),
    JATIZSO_LYRE(new WorldPoint(2409, 3809, 0), "Jatizso", Quest.THE_FREMENNIK_TRIALS, MovementConstants.ENCHANTED_LYRE),

    // Diary
    ARDOUGNE_CLOAK(new WorldPoint(2606, 3222, 0), "Monastery Teleport", null, MovementConstants.ARDOUGNE_CLOAK)
    ;

    private final WorldPoint destination;
    private final Quest requirement;
    private final int[] itemIds;
    private final String action;

    TeleportItem(WorldPoint destination, String action, Quest requirement, int... itemIds)
    {
        this.destination = destination;
        this.requirement = requirement;
        this.itemIds = itemIds;
        this.action = action;
    }

    public WorldPoint getDestination()
    {
        return destination;
    }

    public int[] getItemId()
    {
        return itemIds;
    }

    public String getAction()
    {
        return action;
    }

    public boolean canUse()
    {
        return hasRequirements() && InventoryAPI.getItem(i -> ArrayUtils.contains(itemIds, i.getId())) != null;
    }

    public boolean hasRequirements()
    {
        boolean hasQuestReqs = true; //(requirement == null || Quests.getState(requirement) == QuestState.FINISHED);
        switch (this)
        {
            case JATIZSO_LYRE:
            case NEITIZNOT_LYRE:
                return hasQuestReqs && VarAPI.getVar(VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE) > 0;
            case WATERBIRTH_ISLAND_LYRE:
                return hasQuestReqs && VarAPI.getVar(VarbitID.FREMENNIK_DIARY_HARD_COMPLETE) > 0;
            case TROLL_STRONGHOLD:
                return false; //hasQuestReqs && Skills.getLevel(Skill.AGILITY) >= 73 && Vars.getBit(VarbitID.FREMENNIK_DIARY_HARD_COMPLETE) > 0;
            case ARDOUGNE_TELEPORT_TAB:
                return hasQuestReqs && VarAPI.getVarp(165) >= 30;
            case SALVE_GRAVEYARD_TELEPORT_TAB:
                return hasQuestReqs && VarAPI.getVarp(302) >= 61;
            case QUETZAL_WHISTLE:
                //cannot be used from within the hunter guild
                int regionID = PlayerEx.getLocal().getWorldPoint().getRegionID();
                return hasQuestReqs && regionID != 6191 && regionID != 6291;
            default:
                return hasQuestReqs;
        }
    }
}