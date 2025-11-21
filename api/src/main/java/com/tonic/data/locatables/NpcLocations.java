package com.tonic.data.locatables;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.Walker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Enum of important NPCs and their locations
 */
@AllArgsConstructor
@Getter
public enum NpcLocations {
    // ============================================
    // ASGARNIA
    // ============================================

    // --- BURTHORPE ---
    DENULTH("Denulth", new WorldPoint(2896, 3529, 0)),
    DUNSTAN("Dunstan", new WorldPoint(2919, 3574, 0)),
    EOHRIC("Eohric", new WorldPoint(2900, 3568, 1)),
    GRACE("Grace", new WorldPoint(3051, 4962, 1)),
    HAROLD("Harold", new WorldPoint(2906, 3540, 1)),
    MARTIN_THWAIT("Martin Thwait", new WorldPoint(3049, 4973, 1)),
    SABA("Saba", new WorldPoint(2269, 4755, 0)),
    BURTHROPE_SAM("Sam", new WorldPoint(2201, 4942, 0)),
    TENZING("Tenzing", new WorldPoint(2821, 3555, 0)),
    TOSTIG("Tostig", new WorldPoint(2907, 3537, 0)),
    TURAEL("Turael", new WorldPoint(2930, 3536, 0)),
    WISTAN("Wistan", new WorldPoint(2926, 3544, 0)),

    // --- DWARVEN MINE ---
    BELONA("Belona", new WorldPoint(3017, 9723, 0)),
    DROGO_DWARF("Drogo dwarf", new WorldPoint(3039, 9848, 0)),
    DWARF("Dwarf", new WorldPoint(2997, 9832, 0)),
    HENDOR("Hendor", new WorldPoint(3031, 9745, 0)),
    HURA("Hura", new WorldPoint(3003, 9799, 0)),
    NULODION("Nulodion", new WorldPoint(3011, 3453, 0)),
    NURMOF("Nurmof", new WorldPoint(2995, 9849, 0)),
    PROSPECTOR_PERCY("Prospector Percy", new WorldPoint(3755, 5666, 0)),
    YARSUL("Yarsul", new WorldPoint(3030, 9745, 0)),

    // --- ENTRANA ---
    FRANCIS("Francis", new WorldPoint(2818, 3340, 0)),
    FRINCOS("Frincos", new WorldPoint(2807, 3345, 0)),

    // --- FALADOR ---
    BURNTOF("Burntof", new WorldPoint(2956, 3368, 0)),
    CASSIE("Cassie", new WorldPoint(2976, 3383, 0)),
    CECILIA("Cecilia", new WorldPoint(2991, 3383, 0)),
    DORIC("Doric", new WorldPoint(2951, 3450, 0)),
    DUSURI("Dusuri", new WorldPoint(3022, 3342, 0)),
    ELSTAN("Elstan", new WorldPoint(3049, 3306, 0)),
    EMILY("Emily", new WorldPoint(2956, 3370, 0)),
    FLYNN("Flynn", new WorldPoint(2951, 3387, 0)),
    HAIRDRESSER("Hairdresser", new WorldPoint(2946, 3379, 0)),
    HERQUIN("Herquin", new WorldPoint(2944, 3336, 0)),
    HESKELL("Heskel", new WorldPoint(3001, 3374, 0)),
    PARTY_PETE("Party Pete", new WorldPoint(3049, 3374, 0)),
    SARAH("Sarah", new WorldPoint(3039, 3292, 0)),
    SHOP_KEEPER_FALADOR("Shop Keeper", new WorldPoint(2957, 3385, 0)),
    SIR_AMIK_VARZE("Sir Amik Varze", new WorldPoint(2959, 3337, 2)),
    SIR_TIFFY_CASHIEN("Sir Tiffy Cashien", new WorldPoint(2997, 3374, 0)),
    SQUIRE_FALLY_CASTLE("Squire", new WorldPoint(2975, 3342, 0)),
    WAYNE("Wayne", new WorldPoint(2969, 3310, 0)),

    // --- ICE MOUNTAIN ---
    ORACLE("Oracle", new WorldPoint(3013, 3500, 0)),
    WILLOW("Willow", new WorldPoint(3003, 3434, 0)),
    WILLOW_DUNGEON_ENTRANCE("Willow", new WorldPoint(2995, 3494, 0)),

    // --- PORT SARIM ---
    BETTY("Betty", new WorldPoint(3012, 3259, 0)),
    BRIAN("Brian", new WorldPoint(3027, 3250, 0)),
    CABIN_BOY_HERBERT("Cabin Boy Herbert", new WorldPoint(3054, 3246, 0)),
    CAPTAIN_TOBIAS("Captain Tobias", new WorldPoint(3028, 3212, 0)),
    CAPTAIN_TOCK_PRE_QUEST("Captain Tock", new WorldPoint(3031, 3273, 0)),
    FRIZZY_SKERNIP("Frizzy Skernip", new WorldPoint(3056, 3258, 0)),
    GERRANT("Gerrant", new WorldPoint(3014, 3225, 0)),
    GRUM("Grum", new WorldPoint(3013, 3247, 0)),
    KLARENSE("Klarense", new WorldPoint(3046, 3203, 0)),
    NED_BOAT("Ned", new WorldPoint(3048, 3208, 1)),
    REDBEARD_FRANK("Redbeard Frank", new WorldPoint(3054, 3252, 0)),
    VEOS_PORT_SARIM("Veos", new WorldPoint(3054, 3246, 0)),
    WORMBRAIN("Wormbrain", new WorldPoint(3012, 3189, 0)),
    WYDIN("Wydin", new WorldPoint(3015, 3206, 0)),
    BARTENDER_PORT_SARIM("Bartender", new WorldPoint(3045, 3256, 0)),
    TRADER_STAN("Trader Stan", new WorldPoint(3039, 3192, 0)),
    SARIM_TRADER_CREWMEMBER("Trader Crewmember", new WorldPoint(3037, 3193, 0)),

    // --- RIMMINGTON ---
    BRIAN_RIMMINGTON("Brian", new WorldPoint(2956, 3203, 0)),
    CAPTAIN_TOCK_RIMMINGTON("Captain Tock", new WorldPoint(2911, 3226, 0)),
    CHANCY_RIMMINGTON("Chancy", new WorldPoint(2929, 3218, 0)),
    CHEMIST_RIMMINGTON("Chemist", new WorldPoint(2932, 3212, 0)),
    DA_VINCI_RIMMINGTON("Da Vinci", new WorldPoint(2929, 3218, 0)),
    HETTY("Hetty", new WorldPoint(2966, 3206, 0)),
    HOPS_RIMMINGTON("Hops", new WorldPoint(2929, 3218, 0)),
    ROMMIK("Rommik", new WorldPoint(2946, 3206, 0)),
    SHOP_KEEPER_RIMMINGTON("Shop keeper", new WorldPoint(2947, 3217, 0)),
    TARIA("Taria", new WorldPoint(2942, 3225, 0)),
    THURGO("Thurgo", new WorldPoint(2997, 3145, 0)),

    // --- TAVERLEY ---
    ALAIN("Alain", new WorldPoint(2932, 3441, 0)),
    BOY_WITCHES_HOUSE("Boy", new WorldPoint(2928, 3455, 0)),
    GAIUS("Gaius", new WorldPoint(2884, 3448, 0)),
    JATIX("Jatix", new WorldPoint(2901, 3429, 0)),
    KAQEMEEX("Kaqemeex", new WorldPoint(2924, 3485, 0)),
    SANFEW("Sanfew", new WorldPoint(2898, 3427, 1)),

    // --- BARBARIAN VILLAGE ---
    ATLAS("Atlas", new WorldPoint(3077, 3437, 0)),
    CHECKAL("Checkal", new WorldPoint(3086, 3415, 0)),
    PEKSA("Peksa", new WorldPoint(3073, 3428, 0)),

    // --- SHIP YARD ---
    FOREMAN_SHIP_YARD("Foreman", new WorldPoint(3001, 3044, 0)),
    GLO_CARANOCK("G.L.O. Caranock", new WorldPoint(2954, 3025, 0)),

    // --- OTHER ASGARNIA ---
    VESTRI("Vestri", new WorldPoint(2820, 3489, 0)),

    // ============================================
    // FREMENNIK PROVINCE
    // ============================================

    // --- BARBARIAN OUTPOST ---
    BARBARIAN_GUARD("Barbarian guard", new WorldPoint(2544, 3570, 0)),
    GUNNJORN("Gunnjorn", new WorldPoint(2539, 3549, 0)),

    // --- BAXTORIAN FALLS ---
    ALMERA("Almera", new WorldPoint(2523, 3495, 0)),
    HUDON("Hudon", new WorldPoint(2512, 3481, 0)),

    // --- JATIZSO ---
    FLOSI_DALKSSON("Flosi Dalksson", new WorldPoint(2418, 3813, 0)),
    HRING_HRING("Hring Hring", new WorldPoint(2399, 3797, 0)),
    KEEPA_KETTILON("Keepa Kettilon", new WorldPoint(2418, 3817, 0)),
    RAUM_URDA_STEIN("Raum Urda-Stein", new WorldPoint(2396, 3797, 0)),
    SKULI_MYRKA("Skuli Myrka", new WorldPoint(2394, 3804, 0)),

    // --- LIGHTHOUSE ---
    JOSSIK("Jossik", new WorldPoint(2518, 4635, 0)),
    LARRISSA("Larrissa", new WorldPoint(2507, 3634, 0)),

    // --- RELLEKKA ---
    RELLEKKA_FISH_MONGER("Fish monger", new WorldPoint(2649, 3675, 0)),
    FUR_TRADER("Fur trader", new WorldPoint(2642, 3675, 0)),
    SIGMUND_THE_MERCHANT("Sigmund The Merchant", new WorldPoint(2641, 3679, 0)),
    SKULGRIMEN("Skulgrimen", new WorldPoint(2662, 3693, 0)),
    THORA_THE_BARKEEP("Thora the Barkeep", new WorldPoint(2662, 3673, 0)),
    YRSA("Yrsa", new WorldPoint(2622, 3672, 0)),

    // ============================================
    // KELDAGRIM AND TROLL COUNTRY
    // ============================================

    // --- KELDAGRIM ---
    GULLDAMAR("Gulldamar", new WorldPoint(2885, 10196, 0)),
    VERMUNDI("Vermundi", new WorldPoint(2887, 10191, 0)),
    HIRKO("Hirko", new WorldPoint(2885, 10202, 0)),
    NOLAR("Nolar", new WorldPoint(2885, 10208, 0)),
    HERVI("Hervi", new WorldPoint(2888, 10212, 0)),
    RANDIVOR("Randivor", new WorldPoint(2893, 10212, 0)),
    VIGR("Vigr", new WorldPoint(2872, 10209, 0)),
    ORDAN("Ordan", new WorldPoint(1936, 4966, 0)),
    JORZIK("Jorzik", new WorldPoint(1941, 4969, 0)),
    TATI("Tati", new WorldPoint(2925, 10211, 0)),
    GUNSLIK("Gunslik", new WorldPoint(2869, 10191, 0)),
    STONEMASON("Stonemason", new WorldPoint(2849, 10185, 0)),
    SARO("Saro", new WorldPoint(2825, 10198, 0)),
    SANTIRI("Santiri", new WorldPoint(2828, 10227, 0)),
    AGMUNDI("Agmundi", new WorldPoint(2867, 10211, 0)),

    // ============================================
    // VARLAMORE
    // ============================================

    // --- ALDARIN ---
    FAUSTUS("Faustus", new WorldPoint(1419, 2979, 0)),
    ALDARIN_SHOPKEEPER("Shopkeeper", new WorldPoint(1414, 2973, 0)),
    ALDARIN_TRADER_CREWMEMBER("Trader Crewmember", new WorldPoint(1454, 2967, 0)),
    ALDARIN_BARTENDER("Bartender", new WorldPoint(1376, 2927, 0)),
    ERCOS("Ercos", new WorldPoint(1367, 2939, 0)),
    ICHTA("Ichta", new WorldPoint(1387, 2869, 0)),
    NECTO("Necto", new WorldPoint(1399, 2867, 0)),
    MISTROCK_BARTENDER("Bartender", new WorldPoint(1388, 2858, 0)),
    ANTONIUS("Antonius", new WorldPoint(1361, 2922, 0)),
    TOCI("Toci", new WorldPoint(1428, 2975, 0)),

    // --- AUBURNVALE ---
    AUBURNVALE_SHOPKEEPER("Shopkeeper", new WorldPoint(1381, 3351, 0)),
    SEBAMO("Sebamo", new WorldPoint(1413, 3343, 0)),
    LUNAMI("Lunami", new WorldPoint(1400, 3341, 0)),

    // --- CAM TORUM ---

    // --- CIVITAS ILLA FORTIS ---
    ARTIMA("Artima", new WorldPoint(1767, 3103, 0)),
    COBADO("Cobado", new WorldPoint(1703, 3113, 0)),
    FORTIS_BARTENDER("Bartender", new WorldPoint(1711, 3116, 0)),
    FORTIS_FUR_MERCHANT("Fur Merchant", new WorldPoint(1674, 3110, 0)),
    FORTIS_SILK_MERCHANT("Silk Merchant", new WorldPoint(1676, 3113, 0)),
    FORTIS_BAKER("Baker", new WorldPoint(1687, 3110, 0)),
    FORTIS_SPICE_MERCHANT("Spice Merchant", new WorldPoint(1686, 3100, 0)),
    FORTIS_GEM_MERCHANT("Gem Merchant", new WorldPoint(1674, 3102, 0)),
    FLORIA("Floria", new WorldPoint(1659, 3100, 0)),
    FORTIS_SHOPKEEPER("Shopkeeper", new WorldPoint(1666, 3120, 0)),
    FORTIS_BLACKSMITH("Blacksmith", new WorldPoint(1656, 3141, 0)),
    FORTIS_TRADER_CREWMEMBER("Trader Crewmember", new WorldPoint(1743, 3135, 0)),

    // --- KASTORI ---
    EHECATL("Ehecatl", new WorldPoint(1351, 3055, 0)),
    AMEYALLI("Ameyalli", new WorldPoint(1357, 3061, 0)),
    TZIUHTLA("Tziuhtla", new WorldPoint(1363, 3031, 0)),
    SULISAL("Sulisal", new WorldPoint(1375, 3036, 0)),
    KASTORI_SHOPKEEPER("Shopkeeper", new WorldPoint(1374, 3046, 0)),

    // --- NEMUS RETREAT ---
    AUB("Aub", new WorldPoint(1363, 3317, 0)),

    // --- OUTER FORTIS ---
    OUTER_FORTIS_SHOPKEEPER("Shopkeeper", new WorldPoint(1721, 3064, 0)),
    SPIKE("Spike", new WorldPoint(1773, 3059, 0)),
    OUTER_FORTIS_BARTENDER("Bartender", new WorldPoint(1723, 3077, 0)),

    // --- QUETZACALLI GORGE ---
    QUETZACALLI_SHOPKEEPER("Shopkeeper", new WorldPoint(1518, 3222, 0)),
    QUETZACALLI_BARTENDER("Bartender", new WorldPoint(1499, 3224, 0)),

    // --- SALVAGER OVERLOOK ---
    SALIUS("Salius", new WorldPoint(1620, 3290, 0)),
    SALVAGER_SHOPKEEPER("Shopkeeper", new WorldPoint(1626, 3287, 0)),

    // --- SUNSET COAST ---
    SUNSET_COAST_SHOPKEEPER("Shopkeeper", new WorldPoint(1515, 2986, 0)),
    THURID("Thurid", new WorldPoint(1515, 2994, 0)),
    SUNSET_COAST_TRADER_CREWMEMBER("Trader Crewmember", new WorldPoint(1515, 2973, 0)),
    PICARIA("Picaria", new WorldPoint(1558, 2959, 0)),

    // --- TAL TEKLAN ---
    KING("King", new WorldPoint(1223, 3120, 0)),
    TEICUH("Teicuh", new WorldPoint(1212, 3118, 0)),
    XOCHITL("Xochitl", new WorldPoint(1204, 3119, 0)),
    ARCUANI("Arcuani", new WorldPoint(1211, 3095, 0)),
    TAL_TEKLAN_SHOPKEEPER("Shopkeeper", new WorldPoint(1243, 3109, 0)),

    // --- OTHER VARLAMORE ---
    AGELUS("Agelus", new WorldPoint(1593, 3103, 0)),
    METLA("Metla", new WorldPoint(1741, 2976, 0)),
    HARMINIA("Harminia", new WorldPoint(1584, 3100, 0)),

    // ============================================
    // GREAT KOUREND
    // ============================================

    // --- KOUREND CASTLE ---
    KOUREND_GEM_MERCHANT("Gem merchant", new WorldPoint(1633, 3684, 0)),
    KOUREND_BAKER("Baker", new WorldPoint(1640, 3684, 0)),

    // --- HOSIDIUS ---
    HORACE("Horace", new WorldPoint(1771, 3588, 0)),
    LOGAVA("Logava", new WorldPoint(1768, 3598, 0)),
    VANNAH("Vannah", new WorldPoint(1764, 3593, 0)),
    HOSIDIUS_RICHARD("Richard", new WorldPoint(1741, 3612, 0)),
    MARISI("Marisi", new WorldPoint(1733, 3556, 0)),

    // --- LOVAKENGJ ---
    MUNTY("Munty", new WorldPoint(1551, 3752, 0)),
    FUGGY("Fuggy", new WorldPoint(1568, 3758, 0)),
    THIRUS("Thirus", new WorldPoint(1516, 3834, 0)),
    TOOTHY("Toothy", new WorldPoint(1453, 3858, 0)),

    // --- PORT PISCARILIUS ---
    LEENZ("Leenz", new WorldPoint(1805, 3725, 0)),
    VEOS_PORT_PISCARILIUS("Veos", new WorldPoint(1825, 3690, 0)),
    FRANKIE("Frankie", new WorldPoint(1832, 3717, 0)),
    KENELME("Kenelme", new WorldPoint(1783, 3758, 0)),
    TYNAN("Tynan", new WorldPoint(1840, 3783, 0)),
    WARRENS_SHOP_KEEPER("Shop keeper", new WorldPoint(1775, 10146, 0)),
    WARRENS_FISH_MONGER("Fish monger", new WorldPoint(1761, 10145, 0)),

    // --- ARCEUUS ---
    REGATH("Regath", new WorldPoint(1720, 3724, 0)),
    FILAMINA("Filamina", new WorldPoint(1668, 3727, 0)),
    THYRIA("Thyria", new WorldPoint(1721, 3745, 0)),

    // --- SHAYZIEN ---
    JENNIFER("Jennifer", new WorldPoint(1519, 3589, 0)),
    ROBYN("Robyn", new WorldPoint(1507, 3592, 0)),
    DARYL("Daryl", new WorldPoint(1540, 3556, 0)),
    OSWALD("Oswald", new WorldPoint(1550, 3562, 0)),
    SHERYL("Sheryl", new WorldPoint(1551, 3566, 0)),
    BLAIR("Blair", new WorldPoint(1566, 3571, 0)),
    BRIGET("Briget", new WorldPoint(1568, 3545, 0)),

    // ============================================
    // GUILDS
    // ============================================

    // --- CHAMPIONS' GUILD ---
    GUILDMASTER("Guildmaster", new WorldPoint(3191, 3361, 0)),
    SCAVVO("Scavvo", new WorldPoint(3194, 3353, 1)),
    VALAINE("Valaine", new WorldPoint(3194, 3359, 1)),

    // --- COOKS' GUILD ---
    ROMILY_WEAKLAX("Romily Weaklax", new WorldPoint(3138, 3448, 0)),

    // --- FISHING GUILD ---
    ROACHEY("Roachey", new WorldPoint(2593, 3403, 0)),

    // --- HUNTER GUILD ---
    PELLEM("Pellem", new WorldPoint(1565, 3034, 0)),
    IMIA("Imia", new WorldPoint(1563, 3059, 0)),

    // --- RANGING GUILD ---
    ARMOUR_SALESMAN("Armour salesman", new WorldPoint(2666, 3435, 0)),
    BOW_AND_ARROW_SALESMAN("Bow and Arrow salesman", new WorldPoint(2673, 3432, 0)),
    TICKET_MERCHANT("Ticket Merchant", new WorldPoint(2658, 3431, 0)),
    TRIBAL_WEAPON_SALESMAN("Tribal Weapon Salesman", new WorldPoint(2661, 3419, 0)),

    // --- WARRIORS' GUILD ---
    ANTON("Anton", new WorldPoint(2855, 3535, 1)),
    LIDIO("Lidio", new WorldPoint(2840, 3552, 0)),
    LILLY("Lilly", new WorldPoint(2844, 3552, 0)),

    // --- WIZARDS' GUILD ---
    WIZARD_AKUTHA("Wizard Akutha", new WorldPoint(2595, 3089, 1)),
    WIZARD_SININA("Wizard Sinina", new WorldPoint(2595, 3090, 1)),

    // --- WOODCUTTING GUILD ---
    PERRY("Perry", new WorldPoint(1654, 3500, 0)),
    WOODCUTING_GUILD_SAWMILL_OPERATOR("Sawmill operator", new WorldPoint(1623, 3500, 0)),

    // ============================================
    // KANDARIN
    // ============================================

    // --- CATHERBY ---
    ARHEIN("Arhein", new WorldPoint(2803, 3430, 0)),
    CANDLE_MAKER("Candle maker", new WorldPoint(2800, 3438, 0)),
    DANTAERA("Dantaera", new WorldPoint(2811, 3464, 0)),
    ELLENA("Ellena", new WorldPoint(2859, 3431, 0)),
    HARRY("Harry", new WorldPoint(2833, 3443, 0)),
    HICKTON("Hickton", new WorldPoint(2823, 3441, 0)),
    VANESSA("Vanessa", new WorldPoint(2820, 3463, 0)),

    // --- EAST ARDOUGNE ---
    AEMAD("Aemad", new WorldPoint(2613, 3294, 0)),
    ALRENA("Alrena", new WorldPoint(2572, 3334, 0)),
    BAKER_EAST_ARDOUGNE("Baker", new WorldPoint(2670, 3310, 0)),
    EDMOND_EAST_ARDOUGNE("Edmond", new WorldPoint(2568, 3333, 0)),
    EDMOND_UNDER_GROUND("Edmond", new WorldPoint(2517, 9755, 0)),
    ELENA("Elena", new WorldPoint(2591, 3338, 0)),
    EAST_ARDY_GEM_MERCHANT("Gem merchant", new WorldPoint(2669, 3303, 0)),
    JERICO("Jerico", new WorldPoint(2609, 3324, 0)),
    KING_LATHAS("King Lathas", new WorldPoint(2577, 3293, 1)),
    OMART("Omart", new WorldPoint(2559, 3267, 0)),
    PROBITA("Probita", new WorldPoint(2621, 3294, 0)),
    SILK_MERCHANT("Silk merchant", new WorldPoint(2656, 3300, 0)),
    SILVER_MERCHANT("Silver merchant", new WorldPoint(2658, 3316, 0)),
    SPICE_SELLER("Spice seller", new WorldPoint(2658, 3296, 0)),
    TINDEL_MARCHANT("Tindel Marchant", new WorldPoint(2677, 3151, 0)),
    TWO_PINTS("Two-pints", new WorldPoint(2574, 3320, 0)),
    WIZARD_CROMPERTY("Wizard Cromperty", new WorldPoint(2682, 3325, 0)),
    ZENESHA("Zenesha", new WorldPoint(2653, 3296, 0)),
    BARTENDER_ARDOUGNE("Bartender", new WorldPoint(2574, 3320, 0)),

    // --- SOUTHEAST ARDOUGNE ---
    TORRELL("Torrell", new WorldPoint(2616, 3226, 0)),

    // --- NORTH ARDOUGNE ---
    KRAGEN("Kragen", new WorldPoint(2669, 3375, 0)),
    NORTH_ARDY_RICHARD("Richard", new WorldPoint(2645, 3365, 0)),

    // --- WEST ARDOUGNE ---
    BRAVEK("Bravek", new WorldPoint(2534, 3314, 0)),
    CHADWELL("Chadwell", new WorldPoint(2464, 3285, 0)),
    CLERK_WEST_ARDOUGNE("Clerk", new WorldPoint(2527, 3317, 0)),
    JETHICK("Jethick", new WorldPoint(2537, 3305, 0)),

    // --- ARDOUGNE MONASTERY ---
    BROTHER_CEDRIC("Brother Cedric", new WorldPoint(2615, 3255, 0)),
    BROTHER_OMAD("Brother Omad", new WorldPoint(2605, 3209, 0)),

    // --- KING LATHAS'S TRAINING GROUND ---
    SHOP_KEEPER_LATHAS("Shop keeper", new WorldPoint(2514, 3385, 0)),

    // --- EXAM CENTRE / DIGSITE ---
    ARCHAEOLOGICAL_EXPERT("Terry Balando", new WorldPoint(3352, 3336, 0)),
    DOUG_DEEPING("Doug Deeping", new WorldPoint(3352, 9819, 0)),
    EXAMINER("Examiner", new WorldPoint(3360, 3343, 0)),
    PANNING_GUIDE("Panning guide", new WorldPoint(3377, 3378, 0)),
    STUDENT_NE("Student", new WorldPoint(3371, 3418, 0)),
    STUDENT_NW("Student", new WorldPoint(3346, 3420, 0)),
    STUDENT_S("Student", new WorldPoint(3362, 3397, 0)),

    // --- FISHING PLATFORM ---
    HOLGART_FISHING_PLATFORM("Holgart", new WorldPoint(2783, 3276, 0)),
    HOLGART_STRANDED("Holgart", new WorldPoint(2799, 3320, 0)),
    KENNITH("Kennith", new WorldPoint(2765, 3286, 1)),
    KENT("Kent", new WorldPoint(2795, 3321, 0)),

    // --- HEMENSTER ---
    BONZO("Bonzo", new WorldPoint(2641, 3438, 0)),
    GRANDPA_JACK("Grandpa", new WorldPoint(2649, 3452, 0)),

    // --- PORT KHAZARD ---
    SHOP_KEEPER_KHAZARD("Shop keeper", new WorldPoint(2656, 3153, 0)),

    // --- FIGHT ARENA ---
    HEAD_GUARD("Head Guard", new WorldPoint(2614, 3144, 0)),
    KHAZARD_BARMAN("Khazard Barman", new WorldPoint(2570, 3142, 0)),
    LADY_SERVIL("Lady Servil", new WorldPoint(2567, 3196, 0)),

    // --- DWARF CANNON CAMP ---
    CAPTAIN_LAWGOF("Captain Lawgof", new WorldPoint(2567, 3458, 0)),

    // --- TREE GNOME STRONGHOLD ---
    BARMAN("Barman", new WorldPoint(2482, 3492, 1)),
    BOLONGO("Bolongo", new WorldPoint(2473, 3448, 0)),
    CAPTAIN_ERRDO("Captain Errdo", new WorldPoint(2465, 3500, 3)),
    CHARLIE_PRISONER("Charlie", new WorldPoint(2465, 3495, 3)),
    DAERO("Daero", new WorldPoint(2483, 3486, 1)),
    FEMI_GATE("Femi", new WorldPoint(2461, 3382, 0)),
    GLOUGH_IN_HOME("Glough", new WorldPoint(2476, 3462, 1)),
    GNOME_WAITER("Gnome Waiter", new WorldPoint(2449, 3501, 1)),
    GULLUCK("Gulluck", new WorldPoint(2466, 3488, 2)),
    HECKEL_FUNCH("Heckel Funch", new WorldPoint(2492, 3488, 1)),
    HUDO("Hudo", new WorldPoint(2448, 3508, 1)),
    KING_NARNODE_SHAREEN("King Narnode Shareen", new WorldPoint(2465, 3494, 0)),
    KING_NARNODE_SHAREEN_DUNGEON("King Narnode Shareen", new WorldPoint(2465, 9895, 0)),
    NIEVE("Nieve", new WorldPoint(2433, 3423, 0)),
    PRISSY_SCILLA("Prissy Scilla", new WorldPoint(2437, 3418, 0)),
    ROMETTI("Rometti", new WorldPoint(2482, 3510, 1)),
    ANITA("Anita", new WorldPoint(2388, 3513, 1)),
    BARTENDER_GNOME_STRONGHOLD("Blurberry", new WorldPoint(2479, 3488, 1)),

    // --- TREE GNOME VILLAGE ---
    BOLKOY("Bolkoy", new WorldPoint(2526, 3163, 1)),
    COMMANDER_MONTAI("Commander Montai", new WorldPoint(2524, 3208, 0)),
    ELKOY("Elkoy", new WorldPoint(2503, 3192, 0)),
    GILETH("Gileth", new WorldPoint(2487, 3179, 0)),
    GOLRIE("Golrie", new WorldPoint(2515, 9579, 0)),
    KHAZARD_WARLORD("Khazard warlord", new WorldPoint(2456, 3301, 0)),
    KING_BOLREN("King Bolren", new WorldPoint(2541, 3170, 0)),
    TRACKER_GNOME_1("Tracker gnome 1", new WorldPoint(2505, 3262, 0)),
    TRACKER_GNOME_2("Tracker gnome 2", new WorldPoint(2524, 3256, 0)),
    TRACKER_GNOME_3("Tracker gnome 3", new WorldPoint(2498, 3233, 0)),

    // --- WITCHAVEN ---
    CAROLINE("Caroline", new WorldPoint(2716, 3303, 0)),
    EZEKIAL_LOVECRAFT("Ezekial Lovecraft", new WorldPoint(2732, 3294, 0)),
    HOLGART("Holgart", new WorldPoint(2719, 3305, 0)),

    // --- YANILLE ---
    ALECK("Aleck", new WorldPoint(2565, 3082, 0)),
    DOMINIC_ONION("Dominic Onion", new WorldPoint(2608, 3115, 0)),
    FRENITA("Frenita", new WorldPoint(2568, 3099, 0)),
    LEON("Leon", new WorldPoint(2567, 3085, 0)),
    SELENA("Selena", new WorldPoint(2577, 3099, 0)),
    BARTENDER_YANILLE("Bartender", new WorldPoint(2556, 3078, 0)),

    // --- SEERS' VILLAGE ---
    BARTENDER_SEERS_VILLAGE("Bartender", new WorldPoint(2691, 3493, 0)),

    // --- OTHER KANDARIN ---
    HAZELMERE("Hazelmere", new WorldPoint(2676, 3087, 1)),
    RASOLO("Rasolo", new WorldPoint(2530, 3439, 0)),

    // ============================================
    // KARAMJA
    // ============================================

    // --- BRIMHAVEN ---
    ALFONSE_THE_WAITER("Alfonse the waiter", new WorldPoint(2791, 3188, 0)),
    DAVON("Davon", new WorldPoint(2803, 3152, 0)),
    GARTH("Garth", new WorldPoint(2766, 3214, 0)),
    PRAISTAN_EBOLA("Praistan Ebola", new WorldPoint(2800, 3204, 0)),
    BARTENDER_BRIMHAVEN("Bartender", new WorldPoint(2796, 3155, 0)),

    // --- MUSA POINT ---
    LUTHAS("Luthas", new WorldPoint(2938, 3152, 0)),
    SHOP_KEEPER_KARAMJA("Shop keeper", new WorldPoint(2906, 3147, 0)),
    ZEMBO("Zembo", new WorldPoint(2923, 3147, 0)),
    BARTENDER_MUSA_POINT("Zembo", new WorldPoint(2928, 3144, 0)),

    // --- SHILO VILLAGE ---
    MOSOL_REI("Mosol Rei", new WorldPoint(2879, 2950, 0)),

    // --- TAI BWO WANNAI ---
    JIMINUA("Jiminua", new WorldPoint(2762, 3122, 0)),
    TRUFITUS("Trufitus", new WorldPoint(2809, 3086, 0)),

    // --- MOR UL REK (TzHaar) ---
    TZHAAR_HUR_LEK("TzHaar-Hur-Lek", new WorldPoint(2463, 5148, 0)),
    TZHAAR_HUR_TEL("TzHaar-Hur-Tel", new WorldPoint(2479, 5148, 0)),
    TZHAAR_MEJ_ROH("TzHaar-Mej-Roh", new WorldPoint(2462, 5124, 0)),

    // ============================================
    // KEBOS LOWLANDS
    // ============================================

    // --- FARMING GUILD ---
    ALLANNA("Allanna", new WorldPoint(1250, 3735, 0)),
    AMELIA("Amelia", new WorldPoint(1246, 3739, 0)),
    FARMING_GUILD_GARDEN_SUPPLIER("Garden supplier", new WorldPoint(1247, 3735, 0)),
    LATLINK_FASTBELL("Latlink Fastbell", new WorldPoint(1253, 3753, 0)),
    NIKKIE("Nikkie", new WorldPoint(1243, 3757, 0)),
    ROSIE("Rosie", new WorldPoint(1232, 3733, 0)),
    TAYLOR("Taylor", new WorldPoint(1245, 3753, 0)),

    // --- MOUNT KARUULM ---
    KONAR_QUO_MATEN("Konar quo Maten", new WorldPoint(1309, 3785, 0)),
    LEKE_QUO_KERAN("Leke quo Keran", new WorldPoint(1299, 3796, 0)),

    // ============================================
    // KHARIDIAN DESERT
    // ============================================

    // --- AL KHARID ---
    ALI_MORRISANE("Ali Morrisane", new WorldPoint(3304, 3211, 0)),
    AYESHA("Ayesha", new WorldPoint(3316, 3205, 0)),
    CHANCELLOR_HASSAN("Chancellor Hassan", new WorldPoint(3300, 3164, 0)),
    DOMMIK("Dommik", new WorldPoint(3320, 3195, 0)),
    GEM_TRADER("Gem trader", new WorldPoint(3287, 3213, 0)),
    JARR("Jarr", new WorldPoint(3303, 3121, 0)),
    LOUIE_LEGS("Louie Legs", new WorldPoint(3316, 3173, 0)),
    OSMAN("Osman", new WorldPoint(3291, 3180, 0)),
    RANAEL("Ranael", new WorldPoint(3318, 3164, 0)),
    RUG_MERCHANT_SHANTAY_PASS("Rug Merchant", new WorldPoint(3309, 3109, 0)),
    SHANTAY("Shantay", new WorldPoint(3304, 3123, 0)),
    SHOP_KEEPER_AL_KHARID("Shop keeper", new WorldPoint(3316, 3183, 0)),
    ZEKE("Zeke", new WorldPoint(3289, 3188, 0)),

    // --- BANDIT CAMP ---
    BANDIT_SHOPKEEPER("Bandit shopkeeper", new WorldPoint(3176, 2986, 0)),

    // --- BEDABIN CAMP ---
    BEDABIN_NOMAD("Bedabin Nomad", new WorldPoint(3170, 3028, 0)),

    // --- NARDAH ---
    ARTIMEUS("Artimeus", new WorldPoint(3442, 2901, 0)),
    KAZEMDE("Kazemde", new WorldPoint(3416, 2907, 0)),
    ROKUH("Rokuh", new WorldPoint(3429, 2913, 0)),
    SEDDU("Seddu", new WorldPoint(3408, 2925, 0)),
    ZAHUR("Zahur", new WorldPoint(3425, 2909, 0)),

    // --- POLLNIVNEACH ---
    ALI_THE_BARMAN("Ali the Barman", new WorldPoint(3361, 2956, 0)),
    MARKET_SELLER("Market seller", new WorldPoint(3359, 2987, 0)),

    // --- OTHER KHARIDIAN DESERT ---
    SIMON_TEMPLETON("Simon Templeton", new WorldPoint(3343, 2827, 0)),
    WANDERER("Wanderer", new WorldPoint(3315, 2849, 0)),

    // ============================================
    // MISTHALIN
    // ============================================

    // --- VARROCK ---
    APOTHECARY("Apothecary", new WorldPoint(3192, 3403, 0)),
    ARIS("Aris", new WorldPoint(3205, 3424, 0)),
    ASYFF("Asyff", new WorldPoint(3280, 3397, 0)),
    AUBURY("Aubury", new WorldPoint(3253, 3401, 0)),
    BARAEK("Baraek", new WorldPoint(3216, 3434, 0)),
    BOB_BARTER("Bob Barter", new WorldPoint(3157, 3481, 0)),
    CAPTAIN_ROVIN("Captain Rovin", new WorldPoint(3204, 3496, 2)),
    CHANCY_VARROCK("Chancy", new WorldPoint(3269, 3390, 0)),
    CHARLIE_THE_TRAMP("Charlie the Tramp", new WorldPoint(3210, 3392, 0)),
    CHEMIST_VARROCK("Chemist", new WorldPoint(2932, 3212, 0)),
    COOK_BLUE_MOON("Cook", new WorldPoint(3229, 3398, 0)),
    CURATOR_HAIG_HALEN("Curator Haig Halen", new WorldPoint(3255, 3448, 0)),
    DA_VINCI_VARROCK("Da Vinci", new WorldPoint(3269, 3390, 0)),
    DR_HARLOW("Dr Harlow", new WorldPoint(3223, 3396, 0)),
    DREVEN("Dreven", new WorldPoint(3181, 3354, 0)),
    FATHER_LAWRENCE("Father Lawrence", new WorldPoint(3254, 3480, 0)),
    HOPS_VARROCK("Hops", new WorldPoint(3269, 3390, 0)),
    HORVIK("Horvik", new WorldPoint(3229, 3440, 0)),
    JULIET("Juliet", new WorldPoint(3158, 3426, 1)),
    KATRINE("Katrine", new WorldPoint(3187, 3386, 0)),
    KING_ROALD("King Roald", new WorldPoint(3220, 3472, 0)),
    LOWE("Lowe", new WorldPoint(3233, 3423, 0)),
    MARLO("Marlo", new WorldPoint(3238, 3472, 0)),
    MURKY_MATT("Murky Matt", new WorldPoint(3172, 3481, 0)),
    OLD_MAN_YARLO("Old Man Yarlo", new WorldPoint(3241, 3395, 0)),
    ORLANDO_SMITH("Orlando Smith", new WorldPoint(1759, 4956, 0)),
    RELDO("Reldo", new WorldPoint(3210, 3494, 0)),
    ROMEO("Romeo", new WorldPoint(3212, 3424, 0)),
    VARROCK_SAWMILL_OPERATOR("Sawmill Operator", new WorldPoint(3302, 3491, 0)),
    SERGEANT_TOBYN("Sergeant Tobyn", new WorldPoint(3211, 3436, 0)),
    SHOP_KEEPER_VARROCK("Shop Keeper", new WorldPoint(3217, 3415, 0)),
    SHOP_KEEPER_VARROCK_SWORDSHOP("Shop keeper", new WorldPoint(3202, 3398, 0)),
    SIR_PRYSIN("Sir Prysin", new WorldPoint(3206, 3472, 0)),
    STRAVEN("Straven", new WorldPoint(3246, 9781, 0)),
    SUS_GUARD_1("Guard", new WorldPoint(3229, 3426, 0)),
    TEA_SELLER("Tea Seller", new WorldPoint(3271, 3411, 0)),
    THESSALIA("Thessalia", new WorldPoint(3203, 3417, 0)),
    TREZNOR("Treznor", new WorldPoint(3226, 3458, 0)),
    WILOUGH("Wilough", new WorldPoint(3219, 3433, 0)),
    ZAFF("Zaff", new WorldPoint(3202, 3433, 0)),
    ALINA("Alina", new WorldPoint(3224, 3426, 0)),
    NOAH("Noah", new WorldPoint(3224, 3426, 0)),
    BARTENDER_BLUE_MOON("Bartender", new WorldPoint(3226, 3396, 0)),

    // --- LUMBRIDGE ---
    BOB("Bob", new WorldPoint(3232, 3201, 0)),
    COOK_LUMBRIDGE("Cook", new WorldPoint(3207, 3213, 0)),
    DUKE_LUMBRIDGE("Duke", new WorldPoint(3209, 3222, 1)),
    FATHER_AERECK("Father Aereck", new WorldPoint(3244, 3209, 0)),
    FATHER_URHNEY("Father Urhney", new WorldPoint(3147, 3174, 0)),
    FAYETH("Fayeth", new WorldPoint(3190, 3232, 0)),
    FRED_THE_FARMER("Fred the Farmer", new WorldPoint(3190, 3273, 0)),
    MELEE_COMBAT_TUTOR("Melee Combat Tutor", new WorldPoint(3218, 3239, 0)),
    SHOP_KEEPER_LUMBRIDGE("Shop Keeper", new WorldPoint(3213, 3245, 0)),
    VEOS_LUMBRIDGE("Veos", new WorldPoint(3228, 3241, 0)),

    // --- DRAYNOR VILLAGE ---
    AGGIE("Aggie", new WorldPoint(3087, 3258, 0)),
    AVA("Ava", new WorldPoint(3094, 3357, 0)),
    DIANGO("Diango", new WorldPoint(3079, 3250, 0)),
    FORTUNATO("Fortunato", new WorldPoint(3084, 3253, 0)),
    JOE_GUARD("Joe", new WorldPoint(3128, 3246, 0)),
    LADY_KELI("Lady Keli", new WorldPoint(3128, 3245, 0)),
    LEELA("Leela", new WorldPoint(3111, 3262, 0)),
    MORGAN("Morgan", new WorldPoint(3098, 3269, 0)),
    NED_DRAYNOR("Ned", new WorldPoint(3100, 3258, 0)),
    OLIVIA("Olivia", new WorldPoint(3073, 3256, 0)),
    PRINCE_ALI_JAIL("Prince Ali", new WorldPoint(3123, 3242, 0)),
    PROFESSOR_ODDENSTEIN("Professor Oddenstein", new WorldPoint(3109, 3365, 2)),
    SPRIA("Spria", new WorldPoint(3089, 3266, 0)),
    UNDEAD_TREE("Undead tree", new WorldPoint(3108, 3344, 0)),
    VERONICA("Veronica", new WorldPoint(3110, 3329, 0)),
    WITCH_DRAYNOR_MANOR("Witch", new WorldPoint(3099, 3366, 0)),

    // --- EDGEVILLE ---
    KRYSTILIA("Krystilia", new WorldPoint(3109, 3516, 0)),
    MARLEY("Marley", new WorldPoint(3090, 3470, 0)),
    OZIACH("Oziach", new WorldPoint(3070, 3516, 0)),
    RICHARD("Richard", new WorldPoint(3098, 3519, 0)),
    SHOP_KEEPER_EDGEVILLE("Shop keeper", new WorldPoint(3084, 3511, 0)),
    VANNAKA("Vannaka", new WorldPoint(3145, 9914, 0)),

    // --- WIZARDS' TOWER ---
    ARCHMAGE_WIZARDS_TOWER("Archmage", new WorldPoint(3105, 9571, 0)),
    WIZARD_JALARAST("Wizard Jalarast", new WorldPoint(3107, 3161, 1)),
    WIZARD_MIZGOG("Wizard Mizgog", new WorldPoint(3104, 3163, 2)),
    WIZARD_TRAIBORN("Wizard Traiborn", new WorldPoint(3111, 3162, 1)),

    // --- PATERDOMUS ---
    DREZEL_FREE("Drezel", new WorldPoint(3438, 9897, 0)),
    DREZEL_JAILED("Drezel", new WorldPoint(3415, 3489, 2)),

    // --- OTHER MISTHALIN ---
    OLD_CRONE("Old crone", new WorldPoint(3461, 3558, 0)),
    GERTRUDE("Gertrude", new WorldPoint(3151, 3410, 0)),
    GERTRUDES_CAT("Gertrude's cat", new WorldPoint(3309, 3510, 1)),
    GENERAL_WARTFACE("General Wartface", new WorldPoint(2957, 3511, 0)),
    BARTENDER_JOLLY_BOAR("Bartender", new WorldPoint(3280, 3488, 0)),

    // ============================================
    // MORYTANIA
    // ============================================

    // --- CANIFIS ---
    BARKER("Barker", new WorldPoint(3500, 3501, 0)),
    FIDELIO("Fidelio", new WorldPoint(3477, 3493, 0)),
    MAZCHNA("Mazchna", new WorldPoint(3513, 3508, 0)),
    RUFUS("Rufus", new WorldPoint(3504, 3496, 0)),

    // --- PORT PHASMATYS ---
    GHOST_SHOPKEEPER("Ghost shopkeeper", new WorldPoint(3657, 3474, 0)),

    // --- UNDEAD CHICKEN FARM ---
    ALICE("Alice", new WorldPoint(3629, 3525, 0)),
    MALCOLM("Malcolm", new WorldPoint(3621, 3528, 0)),
    LYRA("Lyra", new WorldPoint(3598, 3532, 0)),

    // ============================================
    // TIRANNWN
    // ============================================

    // --- LLETYA ---
    LILIWEN("Liliwen", new WorldPoint(2345, 3164, 0)),

    // ============================================
    // WILDERNESS
    // ============================================

    EDMOND("Edmond", new WorldPoint(3084, 3814, 0)),
    FAT_TONY("Fat Tony", new WorldPoint(3038, 3705, 0)),
    IAN("Ian", new WorldPoint(2967, 3766, 0)),
    LARRY("Larry", new WorldPoint(3228, 3692, 0)),
    MAGE_OF_ZAMORAK("Mage of Zamorak", new WorldPoint(3107, 3559, 0)),
    NEIL("Neil", new WorldPoint(3024, 3707, 0)),
    NOTERAZZO("Noterazzo", new WorldPoint(3027, 3701, 0)),
    SAM("Sam", new WorldPoint(3047, 3630, 0)),
    SIMON("Simon", new WorldPoint(3239, 3636, 0)),
    DARREN("Darren", new WorldPoint(3014, 3948, 0)),
    WILLIAM("William", new WorldPoint(3169, 3879, 0)),
    EDWARD("Edward", new WorldPoint(3289, 3938, 0)),
    LUNDAIL("Lundail", new WorldPoint(2536, 4721, 0)),

    // ============================================
    // OTHER LOCATIONS
    // ============================================

    // --- APE ATOLL ---
    GARKOR("Garkor", new WorldPoint(2805, 2762, 0)),
    MONKEY_CHILD("Monkey Child", new WorldPoint(2744, 2795, 0)),
    ZOOKNOCK("Zooknock", new WorldPoint(2805, 9144, 0)),

    // --- CORSAIR COVE ---
    ARSEN_THE_THIEF("Arsen the Thief", new WorldPoint(2553, 2857, 1)),
    CABIN_BOY_COLIN("Cabin Boy Colin", new WorldPoint(2557, 2858, 1)),
    CAPTAIN_TOCK_COSAIR("Captain Tock", new WorldPoint(2574, 2836, 1)),
    CHIEF_TESS("Chief Tess", new WorldPoint(2011, 9003, 1)),
    GNOCCI_THE_COOK("Gnocci the Cook", new WorldPoint(2546, 2862, 1)),
    ITHOI_THE_NAVIGATOR("Ithoi the Navigator", new WorldPoint(2529, 2838, 1)),

    // --- PEST CONTROL ---
    ARCHERY_SQUIRE("Squire", new WorldPoint(2664, 2661, 0)),
    GENERAL_SQUIRE("Squire", new WorldPoint(2652, 2664, 0)),
    RUNE_SQUIRE("Squire", new WorldPoint(2658, 2654, 0)),

    // --- TUTORIAL ISLAND ---
    ACCOUNT_GUIDE("Account Guide", new WorldPoint(3126, 3124, 0)),
    BANK_TUTOR("Banker", new WorldPoint(3122, 3123, 0)),
    BROTHER_BRACE("Brother Brace", new WorldPoint(3125, 3106, 0)),
    COMBAT_INSTRUCTOR("Combat Instructor", new WorldPoint(3106, 9509, 0)),
    GIELINOR_GUIDE("Gielinor Guide", new WorldPoint(3094, 3107, 0)),
    MAGIC_INSTRUCTOR("Magic Instructor", new WorldPoint(3141, 3088, 0)),
    MASTER_CHEF("Master Chef", new WorldPoint(3076, 3084, 0)),
    MINING_INSTRUCTOR("Mining Instructor", new WorldPoint(3080, 9504, 0)),
    QUEST_GUIDE("Quest Guide", new WorldPoint(3086, 3122, 0)),
    SURVIVAL_EXPERT("Survival Expert", new WorldPoint(3102, 3095, 0)),

    // --- WINTERTODT ---
    BREWMA("Brew'ma", new WorldPoint(1634, 3986, 0)),

    // --- ZANARIS (Lost City) ---
    CHAELDAR("Chaeldar", new WorldPoint(2447, 4431, 0)),
    FAIRY_SHOP_KEEPER("Fairy shop keeper", new WorldPoint(2377, 4448, 0)),
    IRKSOL("Irksol", new WorldPoint(2481, 4472, 0)),
    JUKAT("Jukat", new WorldPoint(2484, 4451, 0)),
    ;

    private final String name;
    private final WorldPoint location;

    public void interact(String option) {
        Walker.walkTo(getLocation());
        NpcEx npc = new NpcQuery().withName(getName()).first();
        NpcAPI.interact(npc, option);
    }

    public void talkTo() {
        interact("Talk-to");
        while(!DialogueAPI.dialoguePresent())
        {
            Delays.tick();
        }
    }

    public void attack() {
        interact("Attack");
    }

    public void trade() {
        interact("Trade");
    }

    public static NpcLocations fromName(String name) {
        for (NpcLocations npc : values()) {
            if (npc.getName().equalsIgnoreCase(name)) {
                return npc;
            }
        }
        return null;
    }
}