package com.tonic.data.magic.spellbooks;

import net.runelite.api.gameval.InterfaceID;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each spell's index is its key in the spellbook's cache enum: the key whose spell object names the
 * spell's spellbook component in param 596. The keys are from the 2026-11-03 cache (revision 242).
 */
class SpellIndexTest
{
    // enum 1982
    private static final Map<Integer, Integer> STANDARD = Map.ofEntries(
            entry(InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD, 0),
            entry(InterfaceID.MagicSpellbook.WIND_STRIKE, 2),
            entry(InterfaceID.MagicSpellbook.CONFUSE, 3),
            entry(InterfaceID.MagicSpellbook.WATER_STRIKE, 5),
            entry(InterfaceID.MagicSpellbook.EARTH_STRIKE, 7),
            entry(InterfaceID.MagicSpellbook.WEAKEN, 8),
            entry(InterfaceID.MagicSpellbook.FIRE_STRIKE, 9),
            entry(InterfaceID.MagicSpellbook.BONES_BANANAS, 10),
            entry(InterfaceID.MagicSpellbook.WIND_BOLT, 11),
            entry(InterfaceID.MagicSpellbook.CURSE, 12),
            entry(InterfaceID.MagicSpellbook.BIND, 13),
            entry(InterfaceID.MagicSpellbook.LOW_ALCHEMY, 14),
            entry(InterfaceID.MagicSpellbook.WATER_BOLT, 15),
            entry(InterfaceID.MagicSpellbook.VARROCK_TELEPORT, 16),
            entry(InterfaceID.MagicSpellbook.EARTH_BOLT, 17),
            entry(InterfaceID.MagicSpellbook.LUMBRIDGE_TELEPORT, 18),
            entry(InterfaceID.MagicSpellbook.TELEGRAB, 19),
            entry(InterfaceID.MagicSpellbook.FIRE_BOLT, 20),
            entry(InterfaceID.MagicSpellbook.FALADOR_TELEPORT, 21),
            entry(InterfaceID.MagicSpellbook.CRUMBLE_UNDEAD, 22),
            entry(InterfaceID.MagicSpellbook.WIND_BLAST, 23),
            entry(InterfaceID.MagicSpellbook.SUPERHEAT, 25),
            entry(InterfaceID.MagicSpellbook.CAMELOT_TELEPORT, 26),
            entry(InterfaceID.MagicSpellbook.WATER_BLAST, 27),
            entry(InterfaceID.MagicSpellbook.IBAN_BLAST, 28),
            entry(InterfaceID.MagicSpellbook.SNARE, 29),
            entry(InterfaceID.MagicSpellbook.MAGIC_DART, 30),
            entry(InterfaceID.MagicSpellbook.ARDOUGNE_TELEPORT, 31),
            entry(InterfaceID.MagicSpellbook.EARTH_BLAST, 32),
            entry(InterfaceID.MagicSpellbook.HIGH_ALCHEMY, 33),
            entry(InterfaceID.MagicSpellbook.CHARGE_WATER_ORB, 34),
            entry(InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT, 36),
            entry(InterfaceID.MagicSpellbook.FIRE_BLAST, 37),
            entry(InterfaceID.MagicSpellbook.CHARGE_EARTH_ORB, 38),
            entry(InterfaceID.MagicSpellbook.BONES_PEACHES, 39),
            entry(InterfaceID.MagicSpellbook.SARADOMIN_STRIKE, 40),
            entry(InterfaceID.MagicSpellbook.CLAWS_OF_GUTHIX, 41),
            entry(InterfaceID.MagicSpellbook.FLAMES_OF_ZAMORAK, 42),
            entry(InterfaceID.MagicSpellbook.TROLLHEIM_TELEPORT, 43),
            entry(InterfaceID.MagicSpellbook.WIND_WAVE, 44),
            entry(InterfaceID.MagicSpellbook.CHARGE_FIRE_ORB, 45),
            entry(InterfaceID.MagicSpellbook.APE_TELEPORT, 46),
            entry(InterfaceID.MagicSpellbook.WATER_WAVE, 47),
            entry(InterfaceID.MagicSpellbook.CHARGE_AIR_ORB, 48),
            entry(InterfaceID.MagicSpellbook.VULNERABILITY, 49),
            entry(InterfaceID.MagicSpellbook.TELEPORT_TO_YOUR_HOUSE, 50),
            entry(InterfaceID.MagicSpellbook.KOUREND_TELEPORT, 52),
            entry(InterfaceID.MagicSpellbook.EARTH_WAVE, 53),
            entry(InterfaceID.MagicSpellbook.ENFEEBLE, 54),
            entry(InterfaceID.MagicSpellbook.TELEOTHER_LUMBRIDGE, 55),
            entry(InterfaceID.MagicSpellbook.FIRE_WAVE, 56),
            entry(InterfaceID.MagicSpellbook.ENTANGLE, 57),
            entry(InterfaceID.MagicSpellbook.STUN, 58),
            entry(InterfaceID.MagicSpellbook.CHARGE, 59),
            entry(InterfaceID.MagicSpellbook.WIND_SURGE, 60),
            entry(InterfaceID.MagicSpellbook.TELEOTHER_FALADOR, 61),
            entry(InterfaceID.MagicSpellbook.WATER_SURGE, 62),
            entry(InterfaceID.MagicSpellbook.TELEPORT_BLOCK, 63),
            entry(InterfaceID.MagicSpellbook.BOUNTY_TARGET, 64),
            entry(InterfaceID.MagicSpellbook.TELEOTHER_CAMELOT, 65),
            entry(InterfaceID.MagicSpellbook.EARTH_SURGE, 66),
            entry(InterfaceID.MagicSpellbook.FIRE_SURGE, 67),
            entry(InterfaceID.MagicSpellbook.FORTIS_TELEPORT, 68),
            entry(InterfaceID.MagicSpellbook.ENCHANT_1, 6),  // Jewellery Enchant
            entry(InterfaceID.MagicSpellbook.ENCHANT_2, 6),  // Jewellery Enchant
            entry(InterfaceID.MagicSpellbook.ENCHANT_3, 6),  // Jewellery Enchant
            entry(InterfaceID.MagicSpellbook.ENCHANT_4, 6),  // Jewellery Enchant
            entry(InterfaceID.MagicSpellbook.ENCHANT_5, 6),  // Jewellery Enchant
            entry(InterfaceID.MagicSpellbook.ENCHANT_6, 6),  // Jewellery Enchant
            entry(InterfaceID.MagicSpellbook.ENCHANT_7, 6)  // Jewellery Enchant
    );

    // enum 1983
    private static final Map<Integer, Integer> ANCIENT = Map.ofEntries(
            entry(InterfaceID.MagicSpellbook.TELEPORT_HOME_ZAROS, 0),
            entry(InterfaceID.MagicSpellbook.SMOKE_RUSH, 2),
            entry(InterfaceID.MagicSpellbook.SHADOW_RUSH, 3),
            entry(InterfaceID.MagicSpellbook.ZAROSTELEPORT1, 4),
            entry(InterfaceID.MagicSpellbook.BLOOD_RUSH, 5),
            entry(InterfaceID.MagicSpellbook.ICE_RUSH, 6),
            entry(InterfaceID.MagicSpellbook.ZAROSTELEPORT2, 7),
            entry(InterfaceID.MagicSpellbook.SMOKE_BURST, 8),
            entry(InterfaceID.MagicSpellbook.SHADOW_BURST, 9),
            entry(InterfaceID.MagicSpellbook.BLOOD_BURST, 11),
            entry(InterfaceID.MagicSpellbook.ICE_BURST, 12),
            entry(InterfaceID.MagicSpellbook.SMOKE_BLITZ, 14),
            entry(InterfaceID.MagicSpellbook.SHADOW_BLITZ, 15),
            entry(InterfaceID.MagicSpellbook.BLOOD_BLITZ, 17),
            entry(InterfaceID.MagicSpellbook.ICE_BLITZ, 18),
            entry(InterfaceID.MagicSpellbook.SMOKE_BARRAGE, 21),
            entry(InterfaceID.MagicSpellbook.SHADOW_BARRAGE, 22),
            entry(InterfaceID.MagicSpellbook.BLOOD_BARRAGE, 24),
            entry(InterfaceID.MagicSpellbook.ICE_BARRAGE, 25),
            entry(InterfaceID.MagicSpellbook.ZAROSTELEPORT8, 26)
    );

    // enum 1984
    private static final Map<Integer, Integer> LUNAR = Map.ofEntries(
            entry(InterfaceID.MagicSpellbook.TELEPORT_HOME_LUNAR, 0),
            entry(InterfaceID.MagicSpellbook.BAKE_PIE, 2),
            entry(InterfaceID.MagicSpellbook.CURE_PLANT, 3),
            entry(InterfaceID.MagicSpellbook.NPC_CONTACT, 4),
            entry(InterfaceID.MagicSpellbook.CURE_OTHER, 5),
            entry(InterfaceID.MagicSpellbook.TELE_MOONCLAN, 6),
            entry(InterfaceID.MagicSpellbook.TELE_GROUP_MOONCLAN, 7),
            entry(InterfaceID.MagicSpellbook.CURE_ME, 8),
            entry(InterfaceID.MagicSpellbook.TELE_WATERBIRTH, 9),
            entry(InterfaceID.MagicSpellbook.TELE_GROUP_WATERBIRTH, 10),
            entry(InterfaceID.MagicSpellbook.CURE_GROUP, 11),
            entry(InterfaceID.MagicSpellbook.STAT_SPY, 12),
            entry(InterfaceID.MagicSpellbook.TELE_BARB_OUT, 13),
            entry(InterfaceID.MagicSpellbook.TELE_GROUP_BARBARIAN, 14),
            entry(InterfaceID.MagicSpellbook.SPIN_FLAX, 15),
            entry(InterfaceID.MagicSpellbook.SUPERGLASS, 16),
            entry(InterfaceID.MagicSpellbook.TAN_LEATHER, 17),
            entry(InterfaceID.MagicSpellbook.TELE_KHAZARD, 18),
            entry(InterfaceID.MagicSpellbook.TELE_GROUP_KHAZARD, 19),
            entry(InterfaceID.MagicSpellbook.STRING_JEWEL, 20),
            entry(InterfaceID.MagicSpellbook.REST_POT_SHARE, 21),
            entry(InterfaceID.MagicSpellbook.MAGIC_IMBUE, 22),
            entry(InterfaceID.MagicSpellbook.FERTILE_SOIL, 23),
            entry(InterfaceID.MagicSpellbook.STREN_POT_SHARE, 24),
            entry(InterfaceID.MagicSpellbook.TELE_FISH, 25),
            entry(InterfaceID.MagicSpellbook.TELE_GROUP_FISHING_GUILD, 26),
            entry(InterfaceID.MagicSpellbook.TELE_CATHER, 27),
            entry(InterfaceID.MagicSpellbook.TELE_GROUP_CATHERBY, 28),
            entry(InterfaceID.MagicSpellbook.RECHARGE_DRAGONSTONE, 29),
            entry(InterfaceID.MagicSpellbook.TELE_GHORROCK, 30),
            entry(InterfaceID.MagicSpellbook.TELE_GROUP_GHORROCK, 31),
            entry(InterfaceID.MagicSpellbook.ENERGY_TRANS, 32),
            entry(InterfaceID.MagicSpellbook.HEAL_OTHER, 33),
            entry(InterfaceID.MagicSpellbook.VENGEANCE_OTHER, 34),
            entry(InterfaceID.MagicSpellbook.VENGEANCE, 35),
            entry(InterfaceID.MagicSpellbook.HEAL_GROUP, 36),
            entry(InterfaceID.MagicSpellbook.MONSTER_EXAMINE, 37),
            entry(InterfaceID.MagicSpellbook.HUMIDIFY, 38),
            entry(InterfaceID.MagicSpellbook.OURANIA_TELEPORT, 39),
            entry(InterfaceID.MagicSpellbook.HUNTER_KIT, 40),
            entry(InterfaceID.MagicSpellbook.DREAM, 41),
            entry(InterfaceID.MagicSpellbook.PLANK_MAKE, 42),
            entry(InterfaceID.MagicSpellbook.SPELLBOOK_SWAP, 43),
            entry(InterfaceID.MagicSpellbook.GEOMANCY, 44),
            entry(InterfaceID.MagicSpellbook.BOUNTY_TARGET, 45)
    );

    // enum 1985
    private static final Map<Integer, Integer> NECROMANCY = Map.ofEntries(
            entry(InterfaceID.MagicSpellbook.TELEPORT_HOME_ARCEUUS, 0),
            entry(InterfaceID.MagicSpellbook.REANIMATION_BASIC, 2),
            entry(InterfaceID.MagicSpellbook.REANIMATION_ADEPT, 3),
            entry(InterfaceID.MagicSpellbook.REANIMATION_EXPERT, 4),
            entry(InterfaceID.MagicSpellbook.REANIMATION_MASTER, 5),
            entry(InterfaceID.MagicSpellbook.TELEPORT_ARCEUUS_LIBRARY, 6),
            entry(InterfaceID.MagicSpellbook.TELEPORT_DRAYNOR_MANOR, 7),
            entry(InterfaceID.MagicSpellbook.TELEPORT_MIND_ALTAR, 8),
            entry(InterfaceID.MagicSpellbook.TELEPORT_SALVE_GRAVEYARD, 9),
            entry(InterfaceID.MagicSpellbook.TELEPORT_FENKENSTRAIN_CASTLE, 10),
            entry(InterfaceID.MagicSpellbook.TELEPORT_WEST_ARDOUGNE, 11),
            entry(InterfaceID.MagicSpellbook.TELEPORT_HARMONY_ISLAND, 12),
            entry(InterfaceID.MagicSpellbook.TELEPORT_CEMETERY, 13),
            entry(InterfaceID.MagicSpellbook.TELEPORT_BARROWS, 14),
            entry(InterfaceID.MagicSpellbook.TELEPORT_APE_ATOLL_DUNGEON, 15),
            entry(InterfaceID.MagicSpellbook.TELEPORT_RESPAWN, 16),
            entry(InterfaceID.MagicSpellbook.RESURRECT_CROPS, 17),
            entry(InterfaceID.MagicSpellbook.TELEPORT_BATTLEFRONT, 18),
            entry(InterfaceID.MagicSpellbook.INFERIOR_DEMONBANE, 19),
            entry(InterfaceID.MagicSpellbook.SUPERIOR_DEMONBANE, 20),
            entry(InterfaceID.MagicSpellbook.DARK_DEMONBANE, 21),
            entry(InterfaceID.MagicSpellbook.MARK_OF_DARKNESS, 22),
            entry(InterfaceID.MagicSpellbook.GHOSTLY_GRASP, 23),
            entry(InterfaceID.MagicSpellbook.SKELETAL_GRASP, 24),
            entry(InterfaceID.MagicSpellbook.UNDEAD_GRASP, 25),
            entry(InterfaceID.MagicSpellbook.WARD_OF_ARCEUUS, 26),
            entry(InterfaceID.MagicSpellbook.LESSER_CORRUPTION, 27),
            entry(InterfaceID.MagicSpellbook.GREATER_CORRUPTION, 28),
            entry(InterfaceID.MagicSpellbook.DEMONIC_OFFERING, 29),
            entry(InterfaceID.MagicSpellbook.SINISTER_OFFERING, 30),
            entry(InterfaceID.MagicSpellbook.DEGRIME, 31),
            entry(InterfaceID.MagicSpellbook.SHADOW_VEIL, 32),
            entry(InterfaceID.MagicSpellbook.VILE_VIGOUR, 33),
            entry(InterfaceID.MagicSpellbook.DARK_LURE, 34),
            entry(InterfaceID.MagicSpellbook.DEATH_CHARGE, 35),
            entry(InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST, 36),
            entry(InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON, 37),
            entry(InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE, 38),
            entry(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST, 39),
            entry(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON, 40),
            entry(InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE, 41),
            entry(InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST, 42),
            entry(InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON, 43),
            entry(InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE, 44)
    );

    @Test
    void everySpellUsesTheEnumKeyOfItsComponent()
    {
        check(Standard.values(), STANDARD);
        check(Ancient.values(), ANCIENT);
        check(Lunar.values(), LUNAR);
        check(Necromancy.values(), NECROMANCY);
    }

    private static void check(Enum<?>[] spells, Map<Integer, Integer> keys)
    {
        for (Enum<?> spell : spells)
        {
            int component = field(spell, "interfaceId");
            assertEquals(keys.get(component), field(spell, "spellIndex"), spell + " has the wrong index");
        }
    }

    private static Integer field(Enum<?> spell, String name)
    {
        try
        {
            Field field = spell.getDeclaringClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(spell);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }
}
