package com.tonic.data.trading;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.widgets.ShopAPI;
import com.tonic.data.locatables.NpcLocations;
import com.tonic.services.pathfinder.requirements.*;

import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;

import static com.tonic.data.trading.ShopRequirements.*;

/**
 * Enumeration of all shops in Old School RuneScape.
 * Shops are organized by region for easier navigation.
 */
public enum Shop {
    // ============================================
    // ASGARNIA
    // ============================================

    // --- BURTHORPE ---
    BURTHORPE_SUPPLIES(
            InventoryID.DEATH_GENERALSHOP,
            NpcLocations.WISTAN,
            new WorldRequirement(true)
    ),
    MARTIN_THWAITS_LOST_AND_FOUND(
            InventoryID.ROGUESDEN_SHOP_SKILLCAPE_TRIMMED,
            NpcLocations.MARTIN_THWAIT,
            new WorldRequirement(true)
    ),
    GRACES_GRACEFUL_CLOTHING(
            InventoryID.SKILL_GUIDE_AGILITY_COURSES,
            NpcLocations.GRACE,
            new WorldRequirement(true)
    ),
    THE_TOAD_AND_CHICKEN(
            InventoryID.DEATH_PUB,
            NpcLocations.TOSTIG,
            new WorldRequirement(true)
    ),
    SAM_THE_TOAD_AND_CHICKEN(
            InventoryID.DEATH_PUB,
            NpcLocations.BURTHROPE_SAM,
            new WorldRequirement(true)
    ),
    BURTHORPE_SLAYER_EQUIPMENT(
            InventoryID.SLAYERSHOP,
            NpcLocations.TURAEL,
            new WorldRequirement(true)
    ),

    // --- DWARVEN MINE ---
    DWARVEN_SHOPPING_STORE(
            InventoryID.GENERALDWARF,
            NpcLocations.DWARF
    ),
    CROSSBOW_SHOP_DWARVEN(
            InventoryID.XBOWS_SHOP,
            NpcLocations.HURA,
            new WorldRequirement(true)
    ),
    DROGOS_MINING_EMPORIUM(
            InventoryID.MININGSTORE,
            NpcLocations.DROGO_DWARF
    ),
    MULTICANNON_PARTS_FOR_SALE(
            InventoryID.MCANNONSHOP,
            NpcLocations.NULODION,
            new WorldRequirement(true)
    ),
    NURMOFS_PICKAXE_SHOP(
            InventoryID.PICKAXESHOP,
            NpcLocations.NURMOF
    ),
    PROSPECTOR_PERCYS_NUGGET_SHOP(
            InventoryID.SKILL_GUIDE_MINING_ORES,
            NpcLocations.PROSPECTOR_PERCY,
            new WorldRequirement(true)
    ),

    // --- ENTRANA ---
    ENTRANA_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.FRANCIS,
            new WorldRequirement(true)
    ),
    FRINCOS_FABULOUS_HERB_STORE(
            InventoryID.HERBLORESHOP2,
            NpcLocations.FRINCOS,
            new WorldRequirement(true)
    ),

    // --- FALADOR ---
    FALADOR_GENERAL_STORE(
            InventoryID.GENERALSHOP4,
            NpcLocations.SHOP_KEEPER_FALADOR
    ),
    CASSIES_SHIELD_SHOP(
            InventoryID.SHIELDSHOP,
            NpcLocations.CASSIE
    ),
    DUSURIS_STAR_SHOP(
            InventoryID.MAGICTRAINING_INVENTORY,
            NpcLocations.DUSURI,
            new WorldRequirement(true)
    ),
    GARDEN_CENTRE_FALADOR(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.HESKELL,
            new WorldRequirement(true)
    ),
    FLYNNS_MACE_MARKET(
            InventoryID.MACESHOP,
            NpcLocations.FLYNN
    ),
    HERQUINS_GEMS(
            InventoryID.GEMSHOP,
            NpcLocations.HERQUIN
    ),
    WAYNES_CHAINS_CHAINMAIL_SPECIALIST(
            InventoryID.CHAINMAILSHOP,
            NpcLocations.WAYNE
    ),
    // TODO: Add INITIATE_TEMPLE_KNIGHT_ARMOURY - requires Recruitment Drive
    // TODO: Add WHITE_KNIGHT_ARMOURY - requires Wanted! + White Knight rank

    // --- PORT SARIM ---
    BETTYS_MAGIC_EMPORIUM(
            InventoryID.MAGICSHOP,
            NpcLocations.BETTY
    ),
    BRIANS_BATTLEAXE_BAZAAR(
            InventoryID.BATTLEAXESHOP,
            NpcLocations.BRIAN
    ),
    GERRANTS_FISHY_BUSINESS(
            InventoryID.FISHINGSHOP,
            NpcLocations.GERRANT
    ),
    GRUMS_GOLD_EXCHANGE(
            InventoryID.GOLDSHOP,
            NpcLocations.GRUM
    ),
    FOOD_STORE_PORT_SARIM(
            InventoryID.WYDINSTORE,
            NpcLocations.WYDIN
    ),

    // --- RIMMINGTON ---
    RIMMINGTON_GENERAL_STORE(
            InventoryID.GENERALSHOP6,
            NpcLocations.SHOP_KEEPER_RIMMINGTON
    ),
    BRIANS_ARCHERY_SUPPLIES(
            InventoryID.SALESMAN_RANGING,
            NpcLocations.BRIAN_RIMMINGTON
    ),
    ROMMIKS_CRAFTY_SUPPLIES(
            InventoryID.CRAFTINGSHOP_FREE,
            NpcLocations.ROMMIK
    ),

    // --- TAVERLEY ---
    JATIXS_HERBLORE_SHOP(
            InventoryID.HERBLORESHOP,
            NpcLocations.JATIX,
            new WorldRequirement(true)
    ),
    GAIUS_TWO_HANDED_SHOP(
            InventoryID._2HANDEDSHOP,
            NpcLocations.GAIUS,
            new WorldRequirement(true)
    ),

    // --- CAMDOZAAL ---
    // TODO: Add RAMARNOS_SHARD_EXCHANGE - requires Below Ice Mountain quest

    // --- OTHER ASGARNIA ---
    SARAHS_FARMING_SHOP(
            InventoryID.FARMING_SHOP_1,
            NpcLocations.SARAH,
            new WorldRequirement(true)
    ),

    // ============================================
    // FELDIP HILLS
    // ============================================

    // --- GU'TANOTH ---
    // TODO: Add DALS_GENERAL_OGRE_SUPPLIES - requires partial Watchtower
    // TODO: Add GRUDS_HERBLORE_STALL - requires partial Watchtower

    // --- JIGGIG ---
    // TODO: Add UGLUG_STUFFSIES - requires Zogre Flesh Eaters quest

    // ============================================
    // FREMENNIK PROVINCE
    // ============================================

    // --- ETCETERIA ---
    // TODO: Add ETCETERIA_FISH - requires Throne of Miscellania
    // TODO: Add ISLAND_GREENGROCER_ETCETERIA - requires Throne of Miscellania

    // --- JATIZSO ---
    ARMOUR_SHOP(
            InventoryID.FRISD_ARMOURSHOP,
            NpcLocations.RAUM_URDA_STEIN,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_ISLES)
    ),
    FLOSIS_FISHMONGERS(
            InventoryID.FRISD_FISHMONGER,
            NpcLocations.FLOSI_DALKSSON,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_ISLES)
    ),
    KEEPA_KETTILONS_STORE(
            InventoryID.FRISD_COOK,
            NpcLocations.KEEPA_KETTILON,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_ISLES)
    ),
    ORE_STORE(
            InventoryID.FRISD_ORESHOP,
            NpcLocations.HRING_HRING,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_ISLES)
    ),
    WEAPONS_GALORE(
            InventoryID.FRISD_WEAPONSHOP,
            NpcLocations.SKULI_MYRKA,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_ISLES)
    ),

    // --- LIGHTHOUSE ---
    // TODO: Add THE_LIGHTHOUSE_STORE - requires Horror from the Deep

    // --- LUNAR ISLE ---
    // TODO: Add MOON_CLAN_GENERAL_STORE - requires Lunar Diplomacy
    // TODO: Add BABA_YAGAS_MAGIC_SHOP - requires Lunar Diplomacy
    // TODO: Add MOON_CLAN_FINE_CLOTHES - requires Lunar Diplomacy

    // --- MISCELLANIA ---
    // TODO: Add MISCELLANIAN_GENERAL_STORE - requires Throne of Miscellania
    // TODO: Add THE_ESOTERICAN_ARMS - requires Throne of Miscellania
    // TODO: Add GREENGROCER_OF_MISCELLANIA - requires Throne of Miscellania
    // TODO: Add ISLAND_FISHMONGER_MISCELLANIA - requires Throne of Miscellania
    // TODO: Add MISCELLANIAN_CLOTHES_SHOP - requires Throne of Miscellania
    // TODO: Add MISCELLANIAN_FOOD_SHOP - requires Throne of Miscellania

    // --- NEITIZNOT ---
    // TODO: Add NEITIZNOT_SUPPLIES - requires The Fremennik Isles

    // --- RELLEKKA ---
    SIGMUND_THE_MERCHANT(
            InventoryID.VIKING_GENERAL_STORE,
            NpcLocations.SIGMUND_THE_MERCHANT,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_TRIALS)
    ),
    FREMENNIK_FISH_MONGER(
            InventoryID.VIKING_FISHMONGER,
            NpcLocations.FISH_MONGER,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_TRIALS)
    ),
    FREMENNIK_FUR_TRADER(
            InventoryID.VIKING_FURSHOP,
            NpcLocations.FUR_TRADER,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_TRIALS)
    ),
    RELLEKKA_LONGHALL_BAR(
            InventoryID.VIKING_BAR,
            NpcLocations.THORA_THE_BARKEEP,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_TRIALS)
    ),
    SKULGRIMENS_BATTLE_GEAR(
            InventoryID.VIKING_WEAPONS_SHOP,
            NpcLocations.SKULGRIMEN,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_TRIALS)
    ),
    YRSAS_ACCOUTREMENTS(
            InventoryID.VIKING_CLOTHES_SHOP,
            NpcLocations.YRSA,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_FREMENNIK_TRIALS)
    ),

    // ============================================
    // GREAT KOUREND
    // ============================================

    // --- KOUREND CASTLE ---
    KOUREND_CASTLE_GEM_STALL(
            InventoryID.KOURENDGEMSTALL,
            NpcLocations.KOUREND_GEM_MERCHANT,
            new WorldRequirement(true),
            KOUREND_VISITED_REQ
    ),
    KOUREND_CASTLE_BAKERS_STALL(
            InventoryID.BAKERY3,
            NpcLocations.KOUREND_BAKER,
            new WorldRequirement(true),
            KOUREND_VISITED_REQ
    ),

    // --- ARCEUUS ---
    // TODO: Add FILAMINAS_WARES - NPC and inventory ID needed
    // TODO: Add REGATHS_WARES - NPC and inventory ID needed
    // TODO: Add THYRIAS_WARES - NPC and inventory ID needed

    // --- HOSIDIUS ---
    // TODO: Add THE_GOLDEN_FIELD - NPC and inventory ID needed
    // TODO: Add LITTLE_SHOP_OF_HORACE - NPC and inventory ID needed
    // TODO: Add LOGAVA_GRICOLLER_COOKING - NPC and inventory ID needed
    // TODO: Add GRICOLLERS_COOKING_SUPPLIES - NPC and inventory ID needed
    // TODO: Add VANNAHS_FARMING_STALL - requires 50% Hosidius favor

    // --- LOVAKENGJ ---
    // TODO: Add THE_DEEPER_LODE - NPC and inventory ID needed
    // TODO: Add LITTLE_MUNTYS_LITTLE_SHOP - NPC and inventory ID needed
    // TODO: Add TOOTHYS_PICKAXES - NPC and inventory ID needed
    // TODO: Add THIRUS_URKARS_DYNAMITE - NPC and inventory ID needed

    // --- PORT PISCARILIUS ---
    // TODO: Add FRANKIES_FISHING_EMPORIUM - NPC and inventory ID needed
    // TODO: Add KENELMES_WARES - NPC and inventory ID needed
    // TODO: Add LEENZS_GENERAL_SUPPLIES - NPC and inventory ID needed
    // TODO: Add TYNANS_FISHING_SUPPLIES - NPC and inventory ID needed
    // TODO: Add WARRENS_FISH_MONGER - NPC and inventory ID needed
    // TODO: Add WARRENS_GENERAL_STORE - NPC and inventory ID needed

    // --- SHAYZIEN ---
    // TODO: Add THE_CLOAK_AND_STAGGER - NPC and inventory ID needed
    // TODO: Add BRIGETS_WEAPONS - NPC and inventory ID needed
    // TODO: Add BLAIRS_ARMOUR - NPC and inventory ID needed
    // TODO: Add DARYLS_RANGING_SURPLUS - NPC and inventory ID needed
    // TODO: Add JENNIFERS_GENERAL_SUPPLIES - NPC and inventory ID needed
    // TODO: Add SHAYZIEN_STYLES - NPC and inventory ID needed

    // ============================================
    // GUILDS
    // ============================================

    // --- CHAMPIONS' GUILD ---
    SCAVVOS_RUNE_STORE(
            InventoryID.RUNITESHOP,
            NpcLocations.SCAVVO,
            questPointsReq(32)
    ),
    VALAINES_SHOP_OF_CHAMPIONS(
            InventoryID.CHAMPIONSHOP,
            NpcLocations.VALAINE,
            questPointsReq(32)
    ),

    // --- COOKS' GUILD ---
    PIE_SHOP(
            InventoryID.SHOP_PIES,
            NpcLocations.ROMILY_WEAKLAX,
            new WorldRequirement(true),
            new SkillRequirement(Skill.COOKING, 32)
    ),

    // --- FISHING GUILD ---
    FISHING_GUILD_SHOP(
            InventoryID.FISHINGGUILD,
            NpcLocations.ROACHEY,
            new WorldRequirement(true),
            new SkillRequirement(Skill.FISHING, 68)
    ),

    // --- HEROES' GUILD ---
    // TODO: Add HAPPY_HEROES_HEMPORIUM - requires Heroes' Quest

    // --- HUNTER GUILD ---
    // TODO: Add IMIAS_SUPPLIES - requires Hunter Guild (Hunter & Rumours)
    // TODO: Add PELLEMS_FUR_STORE - requires Hunter Guild (Hunter & Rumours)

    // --- LEGENDS' GUILD ---
    // TODO: Add LEGENDS_GUILD_GENERAL_STORE - requires Legends' Quest
    // TODO: Add LEGENDS_GUILD_SHOP_USEFUL_ITEMS - requires Legends' Quest

    // --- MINING GUILD ---
    HENDORS_AWESOME_ORES(
            InventoryID.MGUILD_ORESHOP,
            NpcLocations.HENDOR,
            new SkillRequirement(Skill.MINING, 60)
    ),
    YARSULS_PRODIGIOUS_PICKAXES(
            InventoryID.MGUILD_PICKAXESHOP,
            NpcLocations.YARSUL,
            new SkillRequirement(Skill.MINING, 60)
    ),
    MINING_GUILD_MINERAL_EXCHANGE(
            InventoryID.MGUILD_REWARDSHOP,
            NpcLocations.BELONA,
            new WorldRequirement(true),
            new SkillRequirement(Skill.MINING, 60)
    ),

    // --- MYTHS' GUILD ---
    // TODO: Add MYTHICAL_CAPE_STORE - requires Dragon Slayer II
    // TODO: Add MYTHS_GUILD_ARMOURY - requires Dragon Slayer II
    // TODO: Add MYTHS_GUILD_HERBALIST - requires Dragon Slayer II
    // TODO: Add MYTHS_GUILD_WEAPONRY - requires Dragon Slayer II

    // --- RANGING GUILD ---
    AARONS_ARCHERY_APPENDAGES(
            InventoryID.RANGING_GUILD_ARMOURSHOP,
            NpcLocations.ARMOUR_SALESMAN,
            new WorldRequirement(true),
            new SkillRequirement(Skill.RANGED, 40)
    ),
    AUTHENTIC_THROWING_WEAPONS(
            InventoryID.RANGING_GUILD_TRIBALSHOP,
            NpcLocations.TRIBAL_WEAPON_SALESMAN,
            new WorldRequirement(true),
            new SkillRequirement(Skill.RANGED, 40)
    ),
    DARGAUDS_BOW_AND_ARROWS_TICKETS(
            InventoryID.RANGING_GUILD_BOWSHOP,
            NpcLocations.TICKET_MERCHANT,
            new WorldRequirement(true),
            new SkillRequirement(Skill.RANGED, 40)
    ),
    DARGAUDS_BOW_AND_ARROWS(
            InventoryID.RANGING_GUILD_BOWSHOP,
            NpcLocations.BOW_AND_ARROW_SALESMAN,
            new WorldRequirement(true),
            new SkillRequirement(Skill.RANGED, 40)
    ),

    // --- WARRIORS' GUILD ---
    WARRIOR_GUILD_ARMOURY(
            InventoryID.WARGUILD_ARMOUR_SHOP,
            NpcLocations.ANTON,
            new WorldRequirement(true),
            WARRIOR_GUILD_REQ
    ),
    WARRIOR_GUILD_FOOD_SHOP(
            InventoryID.WARGUILD_FOOD_SHOP,
            NpcLocations.LIDIO,
            new WorldRequirement(true),
            WARRIOR_GUILD_REQ
    ),
    WARRIOR_GUILD_POTION_SHOP(
            InventoryID.WARGUILD_POTION_SHOP,
            NpcLocations.LILLY,
            new WorldRequirement(true),
            WARRIOR_GUILD_REQ
    ),

    // --- WIZARDS' GUILD ---
    MAGIC_GUILD_STORE_MYSTIC_ROBES(
            InventoryID.MAGICGUILDSHOP2,
            NpcLocations.WIZARD_SININA,
            new WorldRequirement(true),
            new SkillRequirement(Skill.MAGIC, 66)
    ),
    MAGIC_GUILD_STORE_RUNES_STAVES(
            InventoryID.MAGICGUILDSHOP,
            NpcLocations.WIZARD_AKUTHA,
            new WorldRequirement(true),
            new SkillRequirement(Skill.MAGIC, 66),
            new QuestRequirement(Quest.THE_HAND_IN_THE_SAND)
    ),

    // --- WOODCUTTING GUILD ---
    WC_GUILD_CONSTRUCTION_SUPPLIES(
            InventoryID.POH_SAWMILL_SHOP,
            NpcLocations.WOODCUTING_GUILD_SAWMILL_OPERATOR,
            new WorldRequirement(true),
            new SkillRequirement(Skill.WOODCUTTING, 60)
    ),
    PERRYS_CHOP_CHOP_SHOP(
            InventoryID.SKILL_GUIDE_STRENGTH_WEAPONS_AND_ARMOUR,
            NpcLocations.PERRY,
            new WorldRequirement(true),
            new SkillRequirement(Skill.WOODCUTTING, 60)
    ),

    // ============================================
    // KANDARIN
    // ============================================

    // --- CATHERBY ---
    ARHEINS_GENERAL_GOODS(
            InventoryID.ARHEINSTORE,
            NpcLocations.ARHEIN,
            new WorldRequirement(true)
    ),
    CANDLE_SHOP(
            InventoryID.CANDLESHOP,
            NpcLocations.CANDLE_MAKER,
            new WorldRequirement(true)
    ),
    HARRYS_FISHING_SHOP(
            InventoryID.FISHINGSHOP2,
            NpcLocations.HARRY,
            new WorldRequirement(true)
    ),
    HICKTONS_ARCHERY_EMPORIUM(
            InventoryID.ARCHERYSHOP2_SKILLCAPE_TRIMMED,
            NpcLocations.HICKTON,
            new WorldRequirement(true)
    ),
    VANESSAS_FARMING_SHOP(
            InventoryID.FARMING_SHOP_2,
            NpcLocations.VANESSA,
            new WorldRequirement(true)
    ),
    CATHERBY_BAY_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.ELLENA,
            new WorldRequirement(true)
    ),
    CATHERBY_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.DANTAERA,
            new WorldRequirement(true)
    ),

    // --- EAST ARDOUGNE ---
    AEMADS_ADVENTURING_SUPPLIES(
            InventoryID.ADVENTURERSHOP,
            NpcLocations.AEMAD,
            new WorldRequirement(true)
    ),
    ARDOUGNE_BAKERS_STALL(
            InventoryID.BAKERY2,
            NpcLocations.BAKER_EAST_ARDOUGNE,
            new WorldRequirement(true)
    ),
    ARDOUGNE_FUR_STALL(
            InventoryID.FURSHOP,
            NpcLocations.FUR_TRADER,
            new WorldRequirement(true)
    ),
    ARDOUGNE_GEM_STALL(
            InventoryID.ARDOUGNEGEMSTALL,
            NpcLocations.GEM_MERCHANT,
            new WorldRequirement(true)
    ),
    ARDOUGNE_SILVER_STALL(
            InventoryID.SILVERSHOP,
            NpcLocations.SILVER_MERCHANT,
            new WorldRequirement(true)
    ),
    ARDOUGNE_SPICE_STALL(
            InventoryID.SPICESTALL,
            NpcLocations.SPICE_SELLER,
            new WorldRequirement(true)
    ),
    ZENESHAS_PLATE_MAIL_BODY_SHOP(
            InventoryID.TOPSHOP,
            NpcLocations.ZENESHA,
            new WorldRequirement(true)
    ),

    // --- SOUTHEAST ARDOUGNE ---
    ARDOUGNE_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.TORRELL,
            new WorldRequirement(true)
    ),

    // --- NORTH ARDOUGNE ---
    RICHARDS_FARMING_SHOP(
            InventoryID.FARMING_SHOP_3,
            NpcLocations.NORTH_ARDY_RICHARD,
            new WorldRequirement(true)
    ),
    NORTH_ARDOUGNE_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.KRAGEN,
            new WorldRequirement(true)
    ),

    // --- KING LATHAS'S TRAINING GROUND ---
    LATHAS_ARMOURY(
            InventoryID.LATHASTRAININGSTORE,
            NpcLocations.SHOP_KEEPER_LATHAS,
            new WorldRequirement(true),
            new QuestRequirement(Quest.KINGS_RANSOM)
    ),

    // --- PISCATORIS FISHING COLONY ---
    // TODO: Add ARNOLDS_ECLECTIC_SUPPLIES - requires Swan Song

    // --- PORT KHAZARD ---
    KHAZARD_GENERAL_STORE(
            InventoryID.KHAZARDSHOP,
            NpcLocations.SHOP_KEEPER_KHAZARD,
            new WorldRequirement(true)
    ),

    // --- TREE GNOME STRONGHOLD ---
    FUNCHS_FINE_GROCERIES(
            InventoryID.GNOMESHOP_HECK,
            NpcLocations.HECKEL_FUNCH,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    GIANNES_RESTAURANT(
            InventoryID.GIANNERESTAURANT,
            NpcLocations.GNOME_WAITER,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    GRAND_TREE_GROCERIES(
            InventoryID.GNOMESHOP_HUDO,
            NpcLocations.HUDO,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    GULLUCK_AND_SONS(
            InventoryID.GNOMESHOP_GULLUCK,
            NpcLocations.GULLUCK,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    FINE_FASHIONS(
            InventoryID.GNOMESHOP_ROMETTI,
            NpcLocations.ROMETTI,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    BLURBERRY_BAR(
            InventoryID.BLURBERRYBAR,
            NpcLocations.BARMAN,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    GNOME_SLAYER_EQUIPMENT(
            InventoryID.SLAYERSHOP,
            NpcLocations.NIEVE,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    SCILLA_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.PRISSY_SCILLA,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    GNOME_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.BOLONGO,
            new WorldRequirement(true),
            new QuestRequirement(Quest.TREE_GNOME_VILLAGE)
    ),
    // TODO: Add KING_NARNODES_ROYAL_SEED_PODS - requires Monkey Madness II

    // --- TREE GNOME VILLAGE ---
    BOLKOYS_VILLAGE_SHOP(
            InventoryID.GNOMESHOP,
            NpcLocations.BOLKOY,
            new WorldRequirement(true)
    ),

    // --- WEST ARDOUGNE ---
    WEST_ARDOUGNE_GENERAL_STORE(
            InventoryID.UPASSGENERALSHOP,
            NpcLocations.CHADWELL,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PLAGUE_CITY)
    ),

    // --- WITCHAVEN ---
    LOVECRAFTS_TACKLE(
            InventoryID.FISHINGSHOP,
            NpcLocations.EZEKIAL_LOVECRAFT,
            new WorldRequirement(true)
    ),

    // --- YANILLE ---
    ALECKS_HUNTER_EMPORIUM(
            InventoryID.HUNTING_SHOP_YANILLE,
            NpcLocations.ALECK,
            new WorldRequirement(true)
    ),
    LEONS_PROTOTYPE_CROSSBOW(
            InventoryID.SKILL_GUIDE_HUNTING_EAGLES,
            NpcLocations.LEON,
            new WorldRequirement(true)
    ),
    FRENITAS_COOKERY_SHOP(
            InventoryID.COOKERYSHOP,
            NpcLocations.FRENITA,
            new WorldRequirement(true)
    ),
    YANILLE_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.SELENA,
            new WorldRequirement(true)
    ),

    // --- OTHER KANDARIN ---
    RASOLO_THE_WANDERING_MERCHANT(
            InventoryID.RASOOLSHOP1,
            NpcLocations.RASOLO,
            new WorldRequirement(true)
    ),

    // ============================================
    // KARAMJA
    // ============================================

    // --- BRIMHAVEN ---
    DAVONS_AMULET_STORE(
            InventoryID.AMULETSHOP,
            NpcLocations.DAVON,
            new WorldRequirement(true)
    ),
    THE_SHRIMP_AND_PARROT(
            InventoryID.KARAMJA_FISHRESTAURANT,
            NpcLocations.ALFONSE_THE_WAITER,
            new WorldRequirement(true)
    ),
    PRAISTAN_EBOLA_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.PRAISTAN_EBOLA,
            new WorldRequirement(true)
    ),
    GARTH_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.GARTH,
            new WorldRequirement(true)
    ),

    // --- MUSA POINT ---
    KARAMJA_GENERAL_STORE(
            InventoryID.GENERALSHOP7,
            NpcLocations.SHOP_KEEPER_KARAMJA
    ),
    KARAMJA_WINES_SPIRITS_AND_BEERS(
            InventoryID.BOOZESHOP,
            NpcLocations.BARTENDER_MUSA_POINT
    ),

    // --- SHILO VILLAGE ---
    // TODO: Add OBLIS_GENERAL_STORE - requires Shilo Village
    // TODO: Add FERNAHEIS_FISHING_HUT - requires Shilo Village
    // TODO: Add SHILO_SLAYER_EQUIPMENT - requires Shilo Village
    // TODO: Add SHILO_SLAYER_REWARDS - requires Shilo Village

    // --- TAI BWO WANNAI ---
    JIMINUAS_JUNGLE_STORE(
            InventoryID.JUNGLESTORE,
            NpcLocations.JIMINUA,
            new WorldRequirement(true)
    ),
    // TODO: Add GABOOTYS_COOPERATIVE - requires Tai Bwo Wannai Trio
    // TODO: Add GABOOTYS_DRINKY_STORE - requires Tai Bwo Wannai Trio
    // TODO: Add TAMAYUS_SPEAR_STALL - requires Tai Bwo Wannai Trio
    // TODO: Add TIADECHES_KARAMBWAN_STALL - requires Tai Bwo Wannai Trio

    // --- MOR UL REK (TzHaar) ---
    TZHAAR_HUR_TELS_EQUIPMENT_STORE(
            InventoryID.TZHAAR_SHOP_EQUIPMENT,
            NpcLocations.TZHAAR_HUR_TEL,
            new WorldRequirement(true)
    ),
    TZHAAR_MEJ_ROHS_RUNE_STORE(
            InventoryID.TZHAAR_SHOP_RUNE,
            NpcLocations.TZHAAR_MEJ_ROH,
            new WorldRequirement(true)
    ),
    TZHAAR_HUR_LEKS_ORE_AND_GEM_STORE(
            InventoryID.TZHAAR_SHOP_OREANDGEM,
            NpcLocations.TZHAAR_HUR_LEK,
            new WorldRequirement(true)
    ),
    // TODO: Add TZHAAR_HUR_ZALS_EQUIPMENT_STORE - requires fire/inferno cape

    // ============================================
    // KEBOS LOWLANDS
    // ============================================

    // --- FARMING GUILD ---
    AMELIAS_SEED_SHOP(
            InventoryID.KEBOS_FARMING_SEED_SHOP,
            NpcLocations.AMELIA,
            new WorldRequirement(true),
            new SkillRequirement(Skill.FARMING, 45)
    ),
    FARMING_GUILD_GARDEN_CENTRE(
            InventoryID.KEBOS_POH_GARDEN_CENTRE,
            NpcLocations.FARMING_GUILD_GARDEN_SUPPLIER,
            new WorldRequirement(true),
            new SkillRequirement(Skill.FARMING, 45)
    ),
    ALLANNAS_FARMING_SHOP(
            InventoryID.KEBOS_FARMING_EQUIPMENT_SHOP,
            NpcLocations.ALLANNA,
            new WorldRequirement(true),
            new SkillRequirement(Skill.FARMING, 45)
    ),
    TAYLOR_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.TAYLOR,
            new WorldRequirement(true),
            new SkillRequirement(Skill.FARMING, 85)
    ),
    LATLINK_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.LATLINK_FASTBELL,
            new WorldRequirement(true),
            new SkillRequirement(Skill.FARMING, 85)
    ),
    NIKKIE_FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.NIKKIE,
            new WorldRequirement(true),
            new SkillRequirement(Skill.FARMING, 85)
    ),

    // --- MOUNT KARUULM ---
    MOUNT_KARUULM_SLAYER_EQUIPMENT(
            InventoryID.SLAYERSHOP,
            NpcLocations.KONAR_QUO_MATEN,
            new WorldRequirement(true)
    ),
    MOUNT_KARUULM_WEAPON_SHOP(
            InventoryID.KEBOS_WEAPON_SHOP,
            NpcLocations.LEKE_QUO_KERAN,
            new WorldRequirement(true)
    ),

    // ============================================
    // KHARIDIAN DESERT
    // ============================================

    // --- AL KHARID ---
    AL_KHARID_GENERAL_STORE(
            InventoryID.GENERALSHOP3,
            NpcLocations.SHOP_KEEPER_AL_KHARID
    ),
    ALIS_DISCOUNT_WARES(
            InventoryID.FEUD_MORRISANES,
            NpcLocations.ALI_MORRISANE,
            new WorldRequirement(true)
    ),
    DOMMIKS_CRAFTING_STORE(
            InventoryID.CRAFTINGSHOP2_FREE,
            NpcLocations.DOMMIK
    ),
    GEM_TRADER(
            InventoryID.GEMSHOP2,
            NpcLocations.GEM_TRADER
    ),
    LOUIES_ARMOURED_LEGS_BAZAAR(
            InventoryID.LEGSSHOP,
            NpcLocations.LOUIE_LEGS
    ),
    RANAELS_SUPER_SKIRT_STORE(
            InventoryID.SKIRTSHOP,
            NpcLocations.RANAEL
    ),
    SHANTAY_PASS_SHOP(
            InventoryID.SHANTAYSHOP,
            NpcLocations.SHANTAY,
            new WorldRequirement(true)
    ),
    ZEKES_SUPERIOR_SCIMITARS(
            InventoryID.SCIMITARSHOP,
            NpcLocations.ZEKE
    ),

    // --- BANDIT CAMP ---
    BANDIT_BARGAINS(
            InventoryID.DT_BANDIT_SHOP,
            NpcLocations.BANDIT_SHOPKEEPER,
            new WorldRequirement(true)
    ),
    // TODO: Add THE_BIG_HEIST_LODGE - requires Desert Treasure I

    // --- BEDABIN CAMP ---
    BEDABIN_VILLAGE_BARTERING(
            InventoryID.BEDABINCAMPSHOP,
            NpcLocations.BEDABIN_NOMAD,
            new WorldRequirement(true),
            new QuestRequirement(Quest.THE_TOURIST_TRAP)
    ),

    // --- EMIR'S ARENA ---
    // TODO: Add PVP_ARENA_REWARDS - special interface
    // TODO: Add SHOP_OF_DISTASTE - requires Enakhra's Lament

    // --- NARDAH ---
    NARDAH_GENERAL_STORE(
            InventoryID.GENERALSHOPNARDAH,
            NpcLocations.KAZEMDE,
            new WorldRequirement(true)
    ),
    NARDAH_HUNTER_SHOP(
            InventoryID.HUNTING_SHOP_NARDAH,
            NpcLocations.ARTIMEUS,
            new WorldRequirement(true)
    ),
    ROKS_CHOCS_BOX(
            InventoryID.CHOCICESHOPNARDAH,
            NpcLocations.ROKUH,
            new WorldRequirement(true)
    ),
    SEDDUS_ADVENTURERS_STORE(
            InventoryID.ARMOURSHOPNARDAH,
            NpcLocations.SEDDU,
            new WorldRequirement(true)
    ),

    // --- POLLNIVNEACH ---
    POLLNIVNEACH_GENERAL_STORE(
            InventoryID.POLLNIVNEACH_GENERALSTORE,
            NpcLocations.MARKET_SELLER,
            new WorldRequirement(true)
    ),
    THE_ASP_AND_SNAKE_BAR(
            InventoryID.FEUD_ALISPUB,
            NpcLocations.ALI_THE_BARMAN,
            new WorldRequirement(true)
    ),

    // --- RUINS OF UNKAH ---
    // TODO: Add ISHMAELS_FISH_HE_SELLS - requires Beneath Cursed Sands

    // --- SOPHANEM ---
    // TODO: Add BLADES_BY_URBI - requires Contact!
    // TODO: Add JAMILAS_CRAFT_STALL - requires Contact!
    // TODO: Add NATHIFAS_BAKE_STALL - requires Contact!
    // TODO: Add RAETUL_AND_COS_CLOTH - requires Contact!
    // TODO: Add THE_SPICE_IS_RIGHT - requires Contact!

    // ============================================
    // MISTHALIN
    // ============================================

    // --- DORGESH-KAAN ---
    // TODO: Add DORGESHKAAN_GENERAL_SUPPLIES - requires Death to Dorgeshuun
    // TODO: Add NARDOKS_BONE_WEAPONS - requires Death to the Dorgeshuun
    // TODO: Add MILTOGS_LAMPS - requires Death to the Dorgeshuun
    // TODO: Add RELDAKS_LEATHER_ARMOUR - requires Death to the Dorgeshuun

    // --- DRAYNOR VILLAGE ---
    AVAS_ODDS_AND_ENDS(
            InventoryID.ANMA_SHOP,
            NpcLocations.AVA,
            new WorldRequirement(true)
    ),
    DIANGOS_TOY_STORE(
            InventoryID.APRILFOOLSHORSESHOP,
            NpcLocations.DIANGO
    ),
    DRAYNOR_SEED_MARKET(
            InventoryID.SEED_STALL,
            NpcLocations.OLIVIA,
            new WorldRequirement(true)
    ),
    // TODO: Add FORESTRY_SHOP_DRAYNOR - special interface
    NEDS_HANDMADE_ROPE_100_WOOL(
            InventoryID.SKILL_GUIDE_CRAFTING_WEAVING,
            NpcLocations.NED_DRAYNOR
    ),
    F2P_WINE_SHOP(
            InventoryID.WINE_MERCHANT_FREE,
            NpcLocations.FORTUNATO
    ),
    WINE_SHOP(
            InventoryID.WINE_MERCHANT,
            NpcLocations.FORTUNATO,
            new WorldRequirement(true)
    ),

    // --- EDGEVILLE ---
    EDGEVILLE_GENERAL_STORE(
            InventoryID.GENERALSHOP5,
            NpcLocations.SHOP_KEEPER_EDGEVILLE
    ),
    // TODO: Add BOUNTY_HUNTER_SHOP - requires BH world
    OZIACH(
            InventoryID.RUNEPLATESHOP,
            NpcLocations.OZIACH
    ),
    VANNAKA_SLAYER_EQUIPMENT(
            InventoryID.SLAYERSHOP,
            NpcLocations.VANNAKA,
            new WorldRequirement(true)
    ),
    EDGEVILLE_SLAYER_EQUIPMENT(
            InventoryID.SLAYERSHOP,
            NpcLocations.KRYSTILIA,
            new WorldRequirement(true)
    ),

    // --- BARBARIAN VILLAGE ---
    HELMET_SHOP(
            InventoryID.HELMETSHOP,
            NpcLocations.PEKSA
    ),

    // --- LUMBRIDGE ---
    LUMBRIDGE_GENERAL_STORE(
            InventoryID.GENERALSHOP1,
            NpcLocations.SHOP_KEEPER_LUMBRIDGE
    ),
    BOBS_BRILLIANT_AXES(
            InventoryID.AXESHOP,
            NpcLocations.BOB
    ),
    // TODO: Add LEAGUES_REWARD_SHOP - requires League world
    // TODO: Add DEADMAN_REWARD_STORE - requires Deadman world
    // TODO: Add CULINAROMANCERS_CHEST - requires Recipe for Disaster subquests

    // --- VARROCK ---
    VARROCK_GENERAL_STORE(
            InventoryID.GENERALSHOP2,
            NpcLocations.SHOP_KEEPER_VARROCK
    ),
    AUBURYS_RUNE_SHOP(
            InventoryID.RUNESHOP,
            NpcLocations.AUBURY
    ),
    VARROCK_CONSTRUCTION_SUPPLIES(
            InventoryID.POH_SAWMILL_SHOP,
            NpcLocations.VARROCK_SAWMILL_OPERATOR,
            new WorldRequirement(true)
    ),
    FANCY_CLOTHES_STORE(
            InventoryID.FANCYCLOTHESSTORE,
            NpcLocations.ASYFF,
            new WorldRequirement(true)
    ),
    HORVIKS_ARMOUR_SHOP(
            InventoryID.ARMOURSHOP,
            NpcLocations.HORVIK
    ),
    LOWES_ARCHERY_EMPORIUM(
            InventoryID.ARCHERYSHOP,
            NpcLocations.LOWE
    ),
    // TODO: Add SPEEDRUNNING_REWARD_SHOP - special interface
    THESSALIAS_FINE_CLOTHES(
            InventoryID.CLOTHESHOP,
            NpcLocations.THESSALIA
    ),
    VARROCK_SWORDSHOP(
            InventoryID.SWORDSHOP,
            NpcLocations.SHOP_KEEPER_VARROCK_SWORDSHOP
    ),
    ZAFFS_SUPERIOR_STAFFS(
            InventoryID.STAFFSHOP,
            NpcLocations.ZAFF
    ),
    YE_OLDE_TEA_SHOPPE(
            InventoryID.TEASHOP,
            NpcLocations.TEA_SELLER,
            new WorldRequirement(true)
    ),

    // ============================================
    // MORYTANIA
    // ============================================

    // --- BURGH DE ROTT ---
    // TODO: Add AURELS_SUPPLIES - requires In Aid of the Myreque

    // --- CANIFIS ---
    GENERAL_STORE(
            InventoryID.WEREWOLFGENERALSTORE,
            NpcLocations.FIDELIO,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PRIEST_IN_PERIL)
    ),
    BARKERS_HABERDASHERY(
            InventoryID.WEREWOLFSTORE2,
            NpcLocations.BARKER,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PRIEST_IN_PERIL)
    ),
    RUFUS_MEAT_EMPORIUM(
            InventoryID.WEREWOLFSTORE1,
            NpcLocations.RUFUS,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PRIEST_IN_PERIL)
    ),
    CANIFIS_SLAYER_EQUIPMENT(
            InventoryID.SLAYERSHOP,
            NpcLocations.MAZCHNA,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PRIEST_IN_PERIL)
    ),

    // --- DARKMEYER ---
    // TODO: Add DARKMEYER_LANTERN_SHOP - requires Sins of the Father
    // TODO: Add DARKMEYER_GENERAL_STORE - requires Sins of the Father
    // TODO: Add DARKMEYER_MEAT_SHOP - requires Sins of the Father
    // TODO: Add DARKMEYER_SEAMSTRESS - requires Sins of the Father
    // TODO: Add MYSTERIOUS_HALLOWED_GOODS - requires Sins of the Father
    // TODO: Add MYSTERIOUS_STRANGER - requires Sins of the Father

    // --- MEIYERDITCH ---
    // TODO: Add IVANS_SUPPLIES - requires Darkness of Hallowvale
    // TODO: Add TRADER_SVENS_BLACK_MARKET - requires Darkness of Hallowvale

    // --- MORT'TON ---
    // TODO: Add RAZMIRE_GENERAL_STORE - requires Shades of Mort'ton
    // TODO: Add RAZMIRE_BUILDERS_MERCHANTS - requires Shades of Mort'ton

    // --- PORT PHASMATYS ---
    PORT_PHASMATYS_GENERAL_STORE(
            InventoryID.AHOY_GENERALSHOP,
            NpcLocations.GHOST_SHOPKEEPER,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PRIEST_IN_PERIL),
            GHOSTSPEAK_REQ
    ),
    // TODO: Add AK_HARANUS_EXOTIC_SHOP - appears after giving Old Crone tea

    // --- UNDEAD CHICKEN FARM ---
    ALICES_FARMING_SHOP(
            InventoryID.FARMING_SHOP_4,
            NpcLocations.ALICE,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PRIEST_IN_PERIL),
            GHOSTSPEAK_REQ
    ),
    FARMING_SUPPLIES(
            InventoryID.FARMER_SUPPLIES,
            NpcLocations.LYRA,
            new WorldRequirement(true),
            new QuestRequirement(Quest.PRIEST_IN_PERIL),
            GHOSTSPEAK_REQ
    ),

    // ============================================
    // TIRANNWN
    // ============================================

    // --- LLETYA ---
    // TODO: Add LLETYA_GENERAL_STORE - requires Mourning's End Part I
    // TODO: Add LLETYA_ARCHERY_SHOP - requires Mourning's End Part I
    // TODO: Add LLETYA_FOOD_STORE - requires Mourning's End Part I
    // TODO: Add LLETYA_SEAMSTRESS - requires Mourning's End Part I

    // --- TYRAS CAMP ---
    // TODO: Add QUARTERMASTERS_STORES - requires Regicide

    // --- PRIFDDINAS ---
    // TODO: Add AMLODDS_MAGICAL_SUPPLIES - requires Song of the Elves
    // TODO: Add ANEIRINS_ARMOUR - requires Song of the Elves
    // TODO: Add BRANWENS_FARMING_SHOP - requires Song of the Elves
    // TODO: Add CONSTRUCTION_SUPPLIES_PRIF - requires Song of the Elves
    // TODO: Add ELGANS_EXCEPTIONAL_STAFFS - requires Song of the Elves
    // TODO: Add FORESTRY_SHOP_PRIF - requires Song of the Elves
    // TODO: Add GUINEVERES_DYES - requires Song of the Elves
    // TODO: Add GWYNS_MINING_EMPORIUM - requires Song of the Elves
    // TODO: Add HEFIN_INN - requires Song of the Elves
    // TODO: Add IORWERTHS_ARMS - requires Song of the Elves
    // TODO: Add IWANS_MACES - requires Song of the Elves
    // TODO: Add LLIANNS_WARES - requires Song of the Elves
    // TODO: Add PRIFDDINAS_FOODSTUFFS - requires Song of the Elves
    // TODO: Add PRIFDDINAS_GEM_STALL - requires Song of the Elves
    // TODO: Add PRIFDDINAS_GENERAL_STORE - requires Song of the Elves
    // TODO: Add PRIFDDINAS_HERBAL_SUPPLIES - requires Song of the Elves
    // TODO: Add PRIFDDINAS_SILVER_STALL - requires Song of the Elves
    // TODO: Add PRIFDDINAS_SPICE_STALL - requires Song of the Elves
    // TODO: Add PRIFDDINAS_SEAMSTRESS - requires Song of the Elves
    // TODO: Add SIANS_RANGED_WEAPONRY - requires Song of the Elves

    // ============================================
    // KELDAGRIM AND TROLL COUNTRY
    // ============================================

    // --- KELDAGRIM ---
    // TODO: Add ARMOUR_STORE_KELDAGRIM - requires Giant Dwarf
    // TODO: Add CAREFREE_CRAFTING_STALL - requires Giant Dwarf
    // TODO: Add CROSSBOW_SHOP_KELDAGRIM - requires Giant Dwarf
    // TODO: Add GREEN_GEMSTONE_GEMS - requires Giant Dwarf
    // TODO: Add KELDAGRIMS_BEST_BREAD - requires Giant Dwarf
    // TODO: Add KJUTS_KEBABS - requires Giant Dwarf
    // TODO: Add ORE_SELLER_KELDAGRIM - requires Giant Dwarf
    // TODO: Add PICKAXE_IS_MINE - requires Giant Dwarf
    // TODO: Add SILVER_COG_SILVER_STALL - requires Giant Dwarf
    // TODO: Add VERMUNDIS_CLOTHES_STALL - requires Giant Dwarf
    // TODO: Add GUNSLIKS_ASSORTED_ITEMS - requires Giant Dwarf
    // TODO: Add AGMUNDI_QUALITY_CLOTHES - requires Giant Dwarf
    // TODO: Add KELDAGRIM_STONEMASON - requires Giant Dwarf
    // TODO: Add QUALITY_ARMOUR_SHOP - requires Giant Dwarf
    // TODO: Add QUALITY_WEAPONS_SHOP - requires Giant Dwarf
    // TODO: Add VIGRS_WARHAMMERS - requires Giant Dwarf

    // --- TROLL STRONGHOLD ---
    // TODO: Add LEPRECHAUN_LARRYS_FARMING - requires Troll Stronghold

    // ============================================
    // VARLAMORE
    // ============================================

    // --- ALDARIN ---
    // TODO: Add ALDARIN_GENERAL_STORE - requires Children of the Sun
    // TODO: Add FAUSTUS_FRUIT_AND_VEG - requires Children of the Sun
    // TODO: Add MISTROCK_MINING_SUPPLIES - requires Children of the Sun
    // TODO: Add MOONRISE_WINES - requires Children of the Sun
    // TODO: Add SHIELDS_OF_MISTROCK - requires Children of the Sun
    // TODO: Add STICK_YOUR_ORE_INN - requires Children of the Sun
    // TODO: Add SUNLIGHTS_SANCTUM - requires Children of the Sun
    // TODO: Add TOCIS_GEM_STORE - requires Children of the Sun

    // --- AUBURNVALE ---
    // TODO: Add AUBURNVALE_GENERAL_STORE - requires Children of the Sun
    // TODO: Add CONSTRUCTION_SUPPLIES_AUBURNVALE - Children of the Sun
    // TODO: Add LUNAMIS_AXE_SHOP - requires Children of the Sun
    // TODO: Add SEBAMOS_SUBLIME_STAFFS - requires Children of the Sun

    // --- CAM TORUM ---
    // TODO: Add CAM_TORUM_GENERAL_STORE - requires Children of the Sun
    // TODO: Add CAM_TORUM_BLACKSMITH - requires Children of the Sun
    // TODO: Add CONARAS_JEWELS - requires Children of the Sun
    // TODO: Add HUITAS_HERBAL_SUPPLIES - requires Children of the Sun
    // TODO: Add THE_LOST_PICKAXE - requires Children of the Sun
    // TODO: Add THE_RUNIC_EMPORIUM - requires Children of the Sun
    // TODO: Add TIZOROS_PICKAXES - requires Children of the Sun
    // TODO: Add YARNIOS_BAKED_GOODS - requires Children of the Sun

    // --- CIVITAS ILLA FORTIS ---
    // TODO: Add ARTIMAS_CRAFTING_SUPPLIES - requires Children of the Sun
    // TODO: Add COBADOS_GROCERIES - requires Children of the Sun
    // TODO: Add FLORIAS_FASHION - requires Children of the Sun
    // TODO: Add FORTIS_BAKERS_STALL - requires Children of the Sun
    // TODO: Add FORTIS_FUR_STALL - requires Children of the Sun
    // TODO: Add FORTIS_GEM_STALL - requires Children of the Sun
    // TODO: Add FORTIS_GENERAL_STORE - requires Children of the Sun
    // TODO: Add THE_FLAMING_ARROW - requires Children of the Sun
    // TODO: Add FORTIS_SILK_STALL - requires Children of the Sun
    // TODO: Add FORTIS_SPICE_STALL - requires Children of the Sun
    // TODO: Add FORTIS_BLACKSMITH - requires Children of the Sun

    // --- KASTORI ---
    // TODO: Add KASTORI_GENERAL_STORE - requires Children of the Sun
    // TODO: Add KASTORI_FARMING_SUPPLIES - requires Children of the Sun
    // TODO: Add SULISALS_FISHING_STORE - requires Children of the Sun

    // --- OUTER FORTIS ---
    // TODO: Add ATLAZORAS_REST - requires Children of the Sun
    // TODO: Add OUTER_FORTIS_GENERAL_STORE - requires Children of the Sun
    // TODO: Add SPIKES_SPIKES - requires Children of the Sun

    // --- QUETZACALLI GORGE ---
    // TODO: Add QUETZACALLI_GORGE_GENERAL - requires Children of the Sun
    // TODO: Add THE_WINDBREAKER - requires Children of the Sun

    // --- SALVAGER OVERLOOK ---
    // TODO: Add SALIUS_ARMOUR_SHOP - requires Children of the Sun
    // TODO: Add SALVAGER_OVERLOOK_GENERAL - requires Children of the Sun

    // --- SUNSET COAST ---
    // TODO: Add PICARIAS_FISHING_SHOP - requires Children of the Sun
    // TODO: Add SUNSET_COAST_GENERAL_STORE - requires Children of the Sun
    // TODO: Add THURIDS_BRAIN_BUCKETS - requires Children of the Sun

    // --- TAL TEKLAN ---
    // TODO: Add ARCUANIS_ARCHERY_SUPPLIES - requires Children of the Sun
    // TODO: Add DYES_TO_DIE_FOR - requires Children of the Sun
    // TODO: Add TAL_TEKLAN_GENERAL_STORE - requires Children of the Sun
    // TODO: Add TAL_TEKLAN_RUNE_SHOP - requires Children of the Sun

    // --- OTHER VARLAMORE ---
    // TODO: Add AGELUS_FARM_SHOP - requires Children of the Sun
    // TODO: Add STONECUTTER_SUPPLIES - requires Children of the Sun

    // ============================================
    // OTHER LOCATIONS
    // ============================================

    // --- APE ATOLL ---
    // TODO: Add IFABAS_GENERAL_STORE - requires Monkey Madness I
    // TODO: Add DAGAS_SCIMITAR_SMITHY - requires Monkey Madness I
    // TODO: Add HAMABS_CRAFTING_EMPORIUM - requires Monkey Madness I
    // TODO: Add OOBAPOHKS_JAVELIN_STORE - requires Monkey Madness I
    // TODO: Add SOLIHIBS_FOOD_STALL - requires Monkey Madness I
    // TODO: Add TUTABS_MAGICAL_MARKET - requires Monkey Madness I

    // --- FOSSIL ISLAND ---
    // TODO: Add FOSSIL_ISLAND_GENERAL_STORE - requires Bone Voyage
    // TODO: Add PETRIFIED_PETES_ORE_SHOP - requires Bone Voyage
    // TODO: Add MAIRINS_MARKET - requires Bone Voyage

    // --- MOS LE'HARMLESS ---
    // TODO: Add DODGY_MIKES_CLOTHING - requires Cabin Fever
    // TODO: Add SMITHING_SMITHS_SHOP - requires Cabin Fever
    // TODO: Add TWO_FEET_CHARLEYS_FISH - requires Cabin Fever
    // TODO: Add THE_OTHER_INN - requires Cabin Fever
    // TODO: Add HARPOON_JOES_RUM - requires Cabin Fever
    // TODO: Add HONEST_JIMMYS_STUFF - requires Cabin Fever

    // --- VOID KNIGHTS' OUTPOST ---
    VOID_KNIGHT_ARCHERY_STORE(
            InventoryID.PEST_ARCHERY_STORE,
            NpcLocations.ARCHERY_SQUIRE,
            new WorldRequirement(true)
    ),
    VOID_KNIGHT_GENERAL_STORE(
            InventoryID.PEST_GENERAL_STORE,
            NpcLocations.GENERAL_SQUIRE,
            new WorldRequirement(true)
    ),
    VOID_KNIGHT_MAGIC_STORE(
            InventoryID.PEST_RUNE_STORE,
            NpcLocations.RUNE_SQUIRE,
            new WorldRequirement(true)
    ),

    // --- WILDERNESS ---
    RICHARDS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP6,
            NpcLocations.RICHARD
    ),
    NEILS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP7,
            NpcLocations.NEIL
    ),
    BANDIT_DUTY_FREE(
            InventoryID.TAXFREE,
            NpcLocations.NOTERAZZO
    ),
    TONYS_PIZZA_BASES(
            InventoryID.PIZZABASESHOP,
            NpcLocations.FAT_TONY
    ),
    SAMS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP10,
            NpcLocations.SAM
    ),
    IANS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP2,
            NpcLocations.IAN
    ),
    EDMONDS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP8,
            NpcLocations.EDMOND
    ),
    WILLIAMS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP1,
            NpcLocations.WILLIAM
    ),
    LARRYS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP3,
            NpcLocations.LARRY
    ),
    SIMONS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP9,
            NpcLocations.SIMON
    ),
    BATTLE_RUNES(
            InventoryID.DARKRUNESHOP_UBER,
            NpcLocations.MAGE_OF_ZAMORAK,
            new WorldRequirement(true)
    ),
    DARRENS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP4,
            NpcLocations.DARREN,
            new WorldRequirement(true)
    ),
    EDWARDS_WILDERNESS_CAPE_SHOP(
            InventoryID.WILDERNESSCAPESHOP5,
            NpcLocations.EDWARD,
            new WorldRequirement(true)
    ),
    LUNDAILS_ARENA_SIDE_RUNE_SHOP(
            InventoryID.MAGEARENA_RUNESHOP,
            NpcLocations.LUNDAIL,
            new WorldRequirement(true)
    ),
    // TODO: Add JUSTINES_LAST_SHOPPER_STANDING - requires LMS minigame
    // TODO: Add MAGE_ARENA_STAFFS - requires Mage Arena I miniquest

    // --- ZANARIS (Lost City) ---
    ZANARIS_GENERAL_STORE(
            InventoryID.GENERALSHOP8,
            NpcLocations.FAIRY_SHOP_KEEPER,
            new WorldRequirement(true),
            new QuestRequirement(Quest.LOST_CITY)
    ),
    ZANARIS_SLAYER_EQUIPMENT(
            InventoryID.SLAYERSHOP,
            NpcLocations.CHAELDAR,
            new WorldRequirement(true),
            new QuestRequirement(Quest.LOST_CITY)
    ),
    IRKSOL(
            InventoryID.CHEAPRINGSHOP,
            NpcLocations.IRKSOL,
            new WorldRequirement(true),
            new QuestRequirement(Quest.LOST_CITY)
    ),
    JUKAT(
            InventoryID.DRAGONSWORDSHOP,
            NpcLocations.JUKAT,
            new WorldRequirement(true),
            new QuestRequirement(Quest.LOST_CITY)
    ),
    // TODO: Add FAIRY_FIXIT_ENCHANTMENT - requires Fairytale II

    // --- WHITE WOLF MOUNTAIN ---
    // TODO: Add CROSSBOW_SHOP_OTHER - requirement unknown

    // --- OTHER ---
    // TODO: Add TRADER_STANS_TRADING_POST - location variable
    ;

    private final int inventoryId;
    private final NpcLocations shopkeeper;
    private final Requirements requirements;

    /**
     * Constructs a Shop entry with the specified parameters.
     *
     * @param inventoryId the RuneLite inventory ID for this shop's interface
     * @param shopkeeper  the NPC location of the shopkeeper
     * @param reqs        optional requirements needed to access this shop
     */
    Shop(int inventoryId, NpcLocations shopkeeper, Requirement... reqs) {
        this.inventoryId = inventoryId;
        this.shopkeeper = shopkeeper;
        if (reqs != null && reqs.length > 0) {
            this.requirements = new Requirements();
            this.requirements.addRequirements(reqs);
        } else {
            this.requirements = null;
        }
    }

    /**
     * Gets the currently open shop by checking for active item containers.
     * <p>
     * Iterates through all defined shops to find which one has an active
     * ItemContainer in the client, indicating it is currently open.
     *
     * @return the currently open Shop, or null if no shop is open or if the
     *         open shop is not yet defined in this enum
     */
    public static Shop getCurrent() {
        return Static.invoke(() -> {
            Client client = Static.getClient();

            if (!ShopAPI.isOpen()) {
                return null;
            }

            // Iterate over all known shops to find active ItemContainer
            for (Shop shop : Shop.values()) {
                ItemContainer container = client.getItemContainer(shop.inventoryId);
                if (container != null) {
                    return shop;
                }
            }

            Logger.error("Shop is open but is not defined in Shop enum. " +
                    "Please add it before using ShopAPI on it.");
            return null;
        });
    }

    /**
     * Checks if the player meets this shop's requirements.
     * <p>
     * Requirements may include quests, skill levels, items, or other
     * conditions. Shops with no requirements always return true.
     *
     * @return true if the player meets all requirements to access this shop,
     *         false otherwise
     */
    public boolean canAccess() {
        return requirements == null || requirements.fulfilled();
    }

    /**
     * Gets the world location of this shop's shopkeeper NPC.
     *
     * @return the WorldPoint location of the shopkeeper, or null if no
     *         shopkeeper is defined
     */
    public WorldPoint getLocation() {
        return shopkeeper != null ? shopkeeper.getLocation() : null;
    }

    /**
     * Gets the RuneLite inventory ID for this shop's interface.
     *
     * @return the inventory ID used to identify this shop's container
     */
    public int getInventoryId() {
        return inventoryId;
    }

    /**
     * Gets the shopkeeper NPC location enum for this shop.
     *
     * @return the NpcLocations enum value for the shopkeeper
     */
    public NpcLocations getShopkeeper() {
        return shopkeeper;
    }

    /**
     * Gets the requirement object for this shop.
     *
     * @return the Requirements object, or null if there are no requirements
     */
    public Requirements getRequirements() {
        return requirements;
    }
}