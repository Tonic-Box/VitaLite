package com.tonic.data.locatables;

import com.tonic.api.entities.NpcAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.queries.NpcQuery;
import com.tonic.services.pathfinder.Pathfinder;
import com.tonic.services.pathfinder.Walker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

/**
 * Enum of important NPCs and their locations
 */
@AllArgsConstructor
@Getter
public enum NpcLocations {
    //varrock
    ROMEO("Romeo", new WorldPoint(3212, 3424, 0)),
    JULIET("Juliet", new WorldPoint(3158, 3426, 1)),
    FATHER_LAWRENCE("Father Lawrence", new WorldPoint(3254, 3480, 0)),
    APOTHECARY("Apothecary", new WorldPoint(3192, 3403, 0)),
    AUBURY("Aubury", new WorldPoint(3253, 3401, 0)),
    KING_ROALD("King Roald", new WorldPoint(3220, 3472, 0)),
    SHOP_KEEPER_VARROCK("Shop Keeper", new WorldPoint(3217, 3415, 0)),
    BARTENDER_BLUE_MOON("Bartender", new WorldPoint(3226, 3396, 0)),
    DR_HARLOW("Dr Harlow", new WorldPoint(3223, 3396, 0)),
    CHEMIST_VARROCK("Chemist", new WorldPoint(2932, 3212, 0)),
    HOPS_VARROCK("Hops", new WorldPoint(3269, 3390, 0)),
    DA_VINCI_VARROCK("Da Vinci", new WorldPoint(3269, 3390, 0)),
    CHANCY_VARROCK("Chancy", new WorldPoint(3269, 3390, 0)),
    ASYFF("Asyff", new WorldPoint(3280, 3397, 0)),
    ARIS("Aris", new WorldPoint(3205, 3424, 0)),
    SIR_PRYSIN("Sir Prysin", new WorldPoint(3206, 3472, 0)),
    CAPTAIN_ROVIN("Captain Rovin", new WorldPoint(3204, 3496, 2)),
    RELDO("Reldo", new WorldPoint(3210, 3494, 0)),
    BOB_BARTER("Bob Barter", new WorldPoint(3157, 3481, 0)),
    MURKY_MATT("Murky Matt", new WorldPoint(3172, 3481, 0)),
    ORLANDO_SMITH("Orlando Smith", new WorldPoint(1759, 4956, 0)),
    CURATOR_HAIG_HALEN("Curator Haig Halen", new WorldPoint(3255, 3448, 0)),
    WILOUGH("Wilough", new WorldPoint(3219, 3433, 0)),
    ALINA("Alina", new WorldPoint(3224, 3426, 0)),
    NOAH("Noah", new WorldPoint(3224, 3426, 0)),
    TREZNOR("Treznor", new WorldPoint(3226, 3458, 0)),
    SERGEANT_TOBYN("Sergeant Tobyn", new WorldPoint(3211, 3436, 0)),
    SUS_GUARD_1("Guard", new WorldPoint(3229, 3426, 0)),
    MARLO("Marlo", new WorldPoint(3238, 3472, 0)),
    OLD_MAN_YARLO("Old Man Yarlo", new WorldPoint(3241, 3395, 0)),
    SAWMILL_OPERATOR("Sawmill Operator", new WorldPoint(3302, 3491, 0)),
    CHARLIE_THE_TRAMP("Charlie the Tramp", new WorldPoint(3210, 3392, 0)),
    COOK_BLUE_MOON("Cook", new WorldPoint(3229, 3398, 0)),
    BARAEK("Baraek", new WorldPoint(3216, 3434, 0)),
    STRAVEN("Straven", new WorldPoint(3246, 9781, 0)),
    KATRINE("Katrine", new WorldPoint(3187, 3386, 0)),
    GUILDMASTER("Guildmaster", new WorldPoint(3191, 3361, 0)),

    //lighthouse
    LARRISSA("Larrissa", new WorldPoint(2507, 3634, 0)),

    //edgeville
    VANNAKA("Vannaka", new WorldPoint(3145, 9914, 0)),
    MARLEY("Marley", new WorldPoint(3090, 3470, 0)),
    OZIACH("Oziach", new WorldPoint(3070, 3516, 0)),

    //paterdomus
    DREZEL_JAILED("Drezel", new WorldPoint(3415, 3489, 2)),
    DREZEL_FREE("Drezel", new WorldPoint(3438, 9897, 0)),

    //lumbridge
    SHOP_KEEPER_LUMBRIDGE("Shop Keeper", new WorldPoint(3213, 3245, 0)),
    FRED_THE_FARMER("Fred the Farmer", new WorldPoint(3190, 3273, 0)),
    DUKE_LUMBRIDGE("Duke", new WorldPoint(3209, 3222, 1)),
    COOK_LUMBRIDGE("Cook", new WorldPoint(3207, 3213, 0)),
    VEOS_LUMBRIDGE("Veos", new WorldPoint(3228, 3241, 0)),
    MELEE_COMBAT_TUTOR("Melee Combat Tutor", new WorldPoint(3218, 3239, 0)),
    FATHER_AERECK("Father Aereck", new WorldPoint(3244, 3209, 0)),
    FATHER_URHNEY("Father Urhney", new WorldPoint(3147, 3174, 0)),
    FAYETH("Fayeth", new WorldPoint(3190, 3232, 0)),

    //al karid
    CHANCELLOR_HASSAN("Chancellor Hassan", new WorldPoint(3300, 3164, 0)),
    OSMAN("Osman", new WorldPoint(3291, 3180, 0)),
    SHANTAY("Shantay", new WorldPoint(3304, 3123, 0)),
    RUG_MERCHANT_SHANTAY_PASS("Rug Merchant", new WorldPoint(3309, 3109, 0)),
    JARR("Jarr", new WorldPoint(3303, 3121, 0)),

    //Khazard
    SHOP_KEEPER_KHAZARD("Shop keeper", new WorldPoint(2656, 3153, 0)),

    //Rimmington
    CHEMIST_RIMMINGTON("Chemist", new WorldPoint(2932, 3212, 0)),
    HOPS_RIMMINGTON("Hops", new WorldPoint(2929, 3218, 0)),
    DA_VINCI_RIMMINGTON("Da Vinci", new WorldPoint(2929, 3218, 0)),
    CHANCY_RIMMINGTON("Chancy", new WorldPoint(2929, 3218, 0)),
    HETTY("Hetty", new WorldPoint(2966, 3206, 0)),
    THURGO("Thurgo", new WorldPoint(2997, 3145, 0)),
    CAPTAIN_TOCK_RIMMINGTON("Captain Tock", new WorldPoint(2911, 3226, 0)),

    //barbarian outpost
    GUNNJORN("Gunnjorn", new WorldPoint(2539, 3549, 0)),
    BARBARIAN_GUARD("Barbarian guard", new WorldPoint(2544, 3570, 0)),
    //TODO: sort these bartenders relative to their respective locations
    BARTENDER_SEERS_VILLAGE("Bartender", new WorldPoint(2691, 3493, 0)),
    BARTENDER_GNOME_STRONGHOLD("Blurberry", new WorldPoint(2479, 3488, 1)),
    BARTENDER_ARDOUGNE("Bartender", new WorldPoint(2574, 3320, 0)),
    BARTENDER_YANILLE("Bartender", new WorldPoint(2556, 3078, 0)),
    BARTENDER_PORT_SARIM("Bartender", new WorldPoint(3045, 3256, 0)),
    BARTENDER_MUSA_POINT("Zembo", new WorldPoint(2928, 3144, 0)),
    BARTENDER_JOLLY_BOAR("Bartender", new WorldPoint(3280, 3488, 0)),
    BARTENDER_BRIMHAVEN("Bartender", new WorldPoint(2796, 3155, 0)),
    EMILY("Emily", new WorldPoint(2956, 3370, 0)), //she goes into falador

    //barbarian village
    CHECKAL("Checkal", new WorldPoint(3086, 3415, 0)),
    ATLAS("Atlas", new WorldPoint(3077, 3437, 0)),

    //baxtorian falls
    ALMERA("Almera", new WorldPoint(2523, 3495, 0)),
    HUDON("Hudon", new WorldPoint(2512, 3481, 0)),
    SIR_AMIK_VARZE("Sir Amik Varze", new WorldPoint(2959, 3337, 2)),
    SIR_TIFFY_CASHIEN("Sir Tiffy Cashien", new WorldPoint(2997, 3374, 0)),

    //falador
    DORIC("Doric", new WorldPoint(2951, 3450, 0)),
    SQUIRE_FALLY_CASTLE("Squire", new WorldPoint(2975, 3342, 0)),
    HESKELL("Heskel", new WorldPoint(3001, 3374, 0)),
    BURNTOF("Burntof", new WorldPoint(2956, 3368, 0)),
    CECILIA("Cecilia", new WorldPoint(2991, 3383, 0)),
    PARTY_PETE("Party Pete", new WorldPoint(3049, 3374, 0)),
    HAIRDRESSER("Hairdresser", new WorldPoint(2946, 3379, 0)),
    SARAH("Sarah", new WorldPoint(3039, 3292, 0)),

    //goblin village
    GENERAL_WARTFACE("General Wartface", new WorldPoint(2957, 3511, 0)),

    //Hemenster
    GRANDPA_JACK("Grandpa", new WorldPoint(2649, 3452, 0)),

    //gnome village
    ELKOY("Elkoy", new WorldPoint(2503, 3192, 0)),
    GOLRIE("Golrie", new WorldPoint(2515, 9579, 0)),

    //East Ardy
    BAKER_EAST_ARDOUGNE("Baker", new WorldPoint(2670, 3310, 0)),
    EDMOND_EAST_ARDOUGNE("Edmond", new WorldPoint(2568, 3333, 0)),
    EDMOND_UNDER_GROUND("Edmond", new WorldPoint(2517, 9755, 0)),
    ALRENA("Alrena", new WorldPoint(2572, 3334, 0)),
    ELENA("Elena", new WorldPoint(2591, 3338, 0)),
    JERICO("Jerico", new WorldPoint(2609, 3324, 0)),
    OMART("Omart", new WorldPoint(2559, 3267, 0)),
    KING_LATHAS("King Lathas", new WorldPoint(2577, 3293, 1)),
    WIZARD_CROMPERTY("Wizard Cromperty", new WorldPoint(2682, 3325, 0)),
    TINDEL_MARCHANT("Tindel Marchant", new WorldPoint(2677, 3151, 0)),
    PROBITA("Probita", new WorldPoint(2621, 3294, 0)),
    SILK_MERCHANT("Silk merchant", new WorldPoint(2656, 3300, 0)),
    TWO_PINTS("Two-pints", new WorldPoint(2574, 3320, 0)),

    //exam centre/digsite
    EXAMINER("Examiner", new WorldPoint(3360, 3343, 0)),
    ARCHAEOLOGICAL_EXPERT("Terry Balando", new WorldPoint(3352, 3336, 0)),
    PANNING_GUIDE("Panning guide", new WorldPoint(3377, 3378, 0)),
    STUDENT_NW("Student", new WorldPoint(3346, 3420, 0)),
    STUDENT_NE("Student", new WorldPoint(3371, 3418, 0)),
    STUDENT_S("Student", new WorldPoint(3362, 3397, 0)),
    DOUG_DEEPING("Doug Deeping", new WorldPoint(3352, 9819, 0)),

    //West Ardy
    JETHICK("Jethick", new WorldPoint(2537, 3305, 0)),
    CLERK_WEST_ARDOUGNE("Clerk", new WorldPoint(2527, 3317, 0)),
    BRAVEK("Bravek", new WorldPoint(2534, 3314, 0)),

    // Ardougne Monestary
    BROTHER_OMAD("Brother Omad", new WorldPoint(2605, 3209, 0)),
    BROTHER_CEDRIC("Brother Cedric", new WorldPoint(2615, 3255, 0)),

    // Gertrude's Cat
    GERTRUDE("Gertrude", new WorldPoint(3151, 3410, 0)),
    GERTRUDES_CAT("Gertrude's cat", new WorldPoint(3309, 3510, 1)),

    //Witchaven
    CAROLINE("Caroline", new WorldPoint(2716, 3303, 0)),
    HOLGART("Holgart", new WorldPoint(2719, 3305, 0)),

    //dwarven black guard camp
    NULODION("Nulodion", new WorldPoint(3011, 3453, 0)),

    //fishing platform
    KENNITH("Kennith", new WorldPoint(2765, 3286, 1)),
    HOLGART_FISHING_PLATFORM("Holgart", new WorldPoint(2783, 3276, 0)),
    HOLGART_STRANDED("Holgart", new WorldPoint(2799, 3320, 0)),
    KENT("Kent", new WorldPoint(2795, 3321, 0)),

    //dwarf cannon camp
    CAPTAIN_LAWGOF("Captain Lawgof", new WorldPoint(2567, 3458, 0)),


    //port sarim
    VEOS_PORT_SARIM("Veos", new WorldPoint(3054, 3246, 0)),
    REDBEARD_FRANK("Redbeard Frank", new WorldPoint(3054, 3252, 0)),
    WYDIN("Wydin", new WorldPoint(3015, 3206, 0)),
    CAPTAIN_TOCK_PRE_QUEST("Captain Tock", new WorldPoint(3031, 3273, 0)),
    CAPTAIN_TOBIAS("Captain Tobias", new WorldPoint(3028, 3212, 0)),
    WORMBRAIN("Wormbrain", new WorldPoint(3012, 3189, 0)),
    KLARENSE("Klarense", new WorldPoint(3046, 3203, 0)),
    NED_BOAT("Ned", new WorldPoint(3048, 3208, 1)),

    //piscarilius
    VEOS_PORT_PISCARILIUS("Veos", new WorldPoint(1825, 3690, 0)),
    LEENZ("Leenz", new WorldPoint(1805, 3725, 0)),
    REGATH("Regath", new WorldPoint(1720, 3724, 0)),
    MUNTY("Munty", new WorldPoint(1551, 3752, 0)),
    JENNIFER("Jennifer", new WorldPoint(1519, 3589, 0)),
    HORACE("Horace", new WorldPoint(1771, 3588, 0)),

    //draynor
    MORGAN("Morgan", new WorldPoint(3098, 3269, 0)),
    VERONICA("Veronica", new WorldPoint(3110, 3329, 0)),
    PROFESSOR_ODDENSTEIN("Professor Oddenstein", new WorldPoint(3109, 3365, 2)),
    AVA("Ava", new WorldPoint(3094, 3357, 0)),
    UNDEAD_TREE("Undead tree", new WorldPoint(3108, 3344, 0)),
    WITCH_DRAYNOR_MANOR("Witch", new WorldPoint(3099, 3366, 0)),
    NED_DRAYNOR("Ned", new WorldPoint(3100, 3258, 0)),
    AGGIE("Aggie", new WorldPoint(3087, 3258, 0)),
    LADY_KELI("Lady Keli", new WorldPoint(3128, 3245, 0)),
    LEELA("Leela", new WorldPoint(3111, 3262, 0)),
    JOE_GUARD("Joe", new WorldPoint(3128, 3246, 0)),
    PRINCE_ALI_JAIL("Prince Ali", new WorldPoint(3123, 3242, 0)),

    //undead chicken farm
    ALICE("Alice", new WorldPoint(3629, 3525, 0)),
    MALCOLM("Malcolm", new WorldPoint(3621, 3528, 0)),

    //mory
    OLD_CRONE("Old crone", new WorldPoint(3461, 3558, 0)),

    //wizzards tower
    WIZARD_MIZGOG("Wizard Mizgog", new WorldPoint(3104, 3163, 2)),
    WIZARD_TRAIBORN("Wizard Traiborn", new WorldPoint(3111, 3162, 1)),

    //taverly
    KAQEMEEX("Kaqemeex", new WorldPoint(2924, 3485, 0)),
    SANFEW("Sanfew", new WorldPoint(2898, 3427, 1)),
    BOY_WITCHES_HOUSE("Boy", new WorldPoint(2928, 3455, 0)),
    ALAIN("Alain", new WorldPoint(2932, 3441, 0)),

    //musa point
    ZEMBO("Zembo", new WorldPoint(2923, 3147, 0)),
    LUTHAS("Luthas", new WorldPoint(2938, 3152, 0)),

    //burthorpe
    DENULTH("Denulth", new WorldPoint(2896, 3529, 0)),
    EOHRIC("Eohric", new WorldPoint(2900, 3568, 1)),
    SABA("Saba", new WorldPoint(2269, 4755, 0)),
    TENZING("Tenzing", new WorldPoint(2821, 3555, 0)),
    DUNSTAN("Dunstan", new WorldPoint(2919, 3574, 0)),
    HAROLD("Harold", new WorldPoint(2906, 3540, 1)),
    TURAEL("Turael", new WorldPoint(2930, 3536, 0)),

    //hemenster
    BONZO("Bonzo", new WorldPoint(2641, 3438, 0)),

    //misc
    VESTRI("Vestri", new WorldPoint(2820, 3489, 0)),
    ARCHMAGE_WIZARDS_TOWER("Archmage", new WorldPoint(3105, 9571, 0)),
    HAZELMERE("Hazelmere", new WorldPoint(2676, 3087, 1)),

    //ship yard
    FOREMAN_SHIP_YARD("Foreman", new WorldPoint(3001, 3044, 0)),
    GLO_CARANOCK("G.L.O. Caranock", new WorldPoint(2954, 3025, 0)),

    //grand tree
    NIEVE("eve", new WorldPoint(2433, 3423, 0)), //`eve` to match both Nieve and Steve
    KING_NARNODE_SHAREEN("King Narnode Shareen", new WorldPoint(2465, 3494, 0)),
    KING_NARNODE_SHAREEN_DUNGEON("King Narnode Shareen", new WorldPoint(2465, 9895, 0)),
    GLOUGH_IN_HOME("Glough", new WorldPoint(2476, 3462, 1)),
    CHARLIE_PRISONER("Charlie", new WorldPoint(2465, 3495, 3)),
    CAPTAIN_ERRDO("Captain Errdo", new WorldPoint(2465, 3500, 3)),
    FEMI_GATE("Femi", new WorldPoint(2461, 3382, 0)),
    ANITA("Anita", new WorldPoint(2388, 3513, 1)),
    PRISSY_SCILLA("Prissy Scilla", new WorldPoint(2437, 3418, 0)),
    BOLONGO("Bolongo", new WorldPoint(2473, 3448, 0)),
    DAERO("Daero", new WorldPoint(2483, 3486, 1)),

    //farming guild
    ROSIE("Rosie", new WorldPoint(1232, 3733, 0)),
    NIKKIE("Nikkie", new WorldPoint(1243, 3757, 0)),

    //Lletya
    LILIWEN("Liliwen", new WorldPoint(2345, 3164, 0)),

    //catherby
    ELLENA("Ellena", new WorldPoint(2859, 3431, 0)),

    //brimhaven
    GARTH("Garth", new WorldPoint(2766, 3214, 0)),

    //tree gnome village
    GILETH("Gileth", new WorldPoint(2487, 3179, 0)),
    KING_BOLREN("King Bolren", new WorldPoint(2541, 3170, 0)),
    COMMANDER_MONTAI("Commander Montai", new WorldPoint(2524, 3208, 0)),
    TRACKER_GNOME_2("Tracker gnome 2", new WorldPoint(2524, 3256, 0)),
    TRACKER_GNOME_1("Tracker gnome 1", new WorldPoint(2505, 3262, 0)),
    TRACKER_GNOME_3("Tracker gnome 3", new WorldPoint(2498, 3233, 0)),
    KHAZARD_WARLORD("Khazard warlord", new WorldPoint(2456, 3301, 0)),

    //tutorial island
    GIELINOR_GUIDE("Gielinor Guide", new WorldPoint(3094, 3107, 0)),
    SURVIVAL_EXPERT("Survival Expert", new WorldPoint(3102, 3095, 0)),
    MASTER_CHEF("Master Chef", new WorldPoint(3076, 3084, 0)),
    QUEST_GUIDE("Quest Guide", new WorldPoint(3086, 3122, 0)),
    MINING_INSTRUCTOR("Mining Instructor", new WorldPoint(3080, 9504, 0)),
    COMBAT_INSTRUCTOR("Combat Instructor", new WorldPoint(3106, 9509, 0)),
    BANK_TUTOR("Banker", new WorldPoint(3122, 3123, 0)),
    ACCOUNT_GUIDE("Account Guide", new WorldPoint(3126, 3124, 0)),
    BROTHER_BRACE("Brother Brace", new WorldPoint(3125, 3106, 0)),
    MAGIC_INSTRUCTOR("Magic Instructor", new WorldPoint(3141, 3088, 0)),

    //cosair cove
    ITHOI_THE_NAVIGATOR("Ithoi the Navigator", new WorldPoint(2529, 2838, 1)),
    ARSEN_THE_THIEF("Arsen the Thief", new WorldPoint(2553, 2857, 1)),
    CABIN_BOY_COLIN("Cabin Boy Colin", new WorldPoint(2557, 2858, 1)),
    GNOCCI_THE_COOK("Gnocci the Cook", new WorldPoint(2546, 2862, 1)),
    CAPTAIN_TOCK_COSAIR("Captain Tock", new WorldPoint(2574, 2836, 1)),
    CHIEF_TESS("Chief Tess", new WorldPoint(2011, 9003, 1)),

    //tai bwo wannai
    TRUFITUS("Trufitus", new WorldPoint(2809, 3086, 0)),

    //shilo village
    MOSOL_REI("Mosol Rei", new WorldPoint(2879, 2950, 0)),

    //ice mountain
    WILLOW("Willow", new WorldPoint(3003, 3434, 0)),
    WILLOW_DUNGEON_ENTRANCE("Willow", new WorldPoint(2995, 3494, 0)),
    ORACLE("Oracle", new WorldPoint(3013, 3500, 0)),

    //fight arena
    LADY_SERVIL("Lady Servil", new WorldPoint(2567, 3196, 0)),
    HEAD_GUARD("Head Guard", new WorldPoint(2614, 3144, 0)),
    KHAZARD_BARMAN("Khazard Barman", new WorldPoint(2570, 3142, 0)),

    //lighthouse (HFTD)
    JOSSIK("Jossik", new WorldPoint(2518, 4635, 0)),

    //yannile
    ALECK("Aleck", new WorldPoint(2565, 3082, 0)),
    DOMINIC_ONION("Dominic Onion", new WorldPoint(2608, 3115, 0)),

    //nardah
    ZAHUR("Zahur", new WorldPoint(3425, 2909, 0)),

    //kharidian desert
    WANDERER("Wanderer", new WorldPoint(3315, 2849, 0)),
    SIMON_TEMPLETON("Simon Templeton", new WorldPoint(3343, 2827, 0)),

    //ape atoll
    GARKOR("Garkor", new WorldPoint(2805, 2762, 0)),
    ZOOKNOCK("Zooknock", new WorldPoint(2805, 9144, 0)),
    MONKEY_CHILD("Monkey Child", new WorldPoint(2744, 2795, 0)),

    //wintertodt
    BREWMA("Brew'ma", new WorldPoint(1634, 3986, 0)),

    ;

    private final String name;
    private final WorldPoint location;

    public void interact(String option) {
        Walker.walkTo(getLocation());
        NPC npc = new NpcQuery().withName(getName()).first();
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