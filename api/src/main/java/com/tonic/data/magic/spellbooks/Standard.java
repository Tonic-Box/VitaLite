package com.tonic.data.magic.spellbooks;

import com.tonic.Static;
import com.tonic.api.game.QuestAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.MagicAPI;
import com.tonic.data.magic.MagicCast;
import com.tonic.data.magic.Rune;
import com.tonic.data.magic.RuneRequirement;
import com.tonic.data.magic.Spell;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

import static com.tonic.data.magic.SpellBook.*;

public enum Standard implements Spell
{
    HOME_TELEPORT(
            0, InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD, SpellbookEnums.StandardSpell.LUMBRIDGE_HOME_TELEPORT.index, false
    ),
    VARROCK_TELEPORT(
            25, InterfaceID.MagicSpellbook.VARROCK_TELEPORT, SpellbookEnums.StandardSpell.VARROCK_TELEPORT.index, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(1, Rune.LAW)
    ),
    GRAND_EXCHANGE_TELEPORT(
            25, InterfaceID.MagicSpellbook.VARROCK_TELEPORT, SpellbookEnums.StandardSpell.VARROCK_TELEPORT.index, true, VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(1, Rune.LAW)
    ),
    LUMBRIDGE_TELEPORT(
            31, InterfaceID.MagicSpellbook.LUMBRIDGE_TELEPORT, SpellbookEnums.StandardSpell.LUMBRIDGE_TELEPORT.index, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.LAW)
    ),
    FALADOR_TELEPORT(
            37, InterfaceID.MagicSpellbook.FALADOR_TELEPORT, SpellbookEnums.StandardSpell.FALADOR_TELEPORT.index, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.LAW)
    ),
    TELEPORT_TO_HOUSE(
            40, InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD, SpellbookEnums.StandardSpell.TELEPORT_TO_HOUSE.index, true,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.LAW)
    ),
    CAMELOT_TELEPORT(
            45, InterfaceID.MagicSpellbook.CAMELOT_TELEPORT, SpellbookEnums.StandardSpell.CAMELOT_TELEPORT.index, true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.LAW)
    ),
    SEERS_TELEPORT(
            45, InterfaceID.MagicSpellbook.CAMELOT_TELEPORT, SpellbookEnums.StandardSpell.CAMELOT_TELEPORT.index, true, Varbits.DIARY_KANDARIN_HARD,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.LAW)
    ),
    ARDOUGNE_TELEPORT(
            51, InterfaceID.MagicSpellbook.ARDOUGNE_TELEPORT, SpellbookEnums.StandardSpell.ARDOUGNE_TELEPORT.index, true, Quest.PLAGUE_CITY,
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(2, Rune.LAW)
    ),
    WATCHTOWER_TELEPORT(
            58, InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT, SpellbookEnums.StandardSpell.WATCHTOWER_TELEPORT.index, true, Quest.WATCHTOWER,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.LAW)
    ),
    YANILLE_TELEPORT(
            58, InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT, SpellbookEnums.StandardSpell.WATCHTOWER_TELEPORT.index, true, Varbits.DIARY_ARDOUGNE_HARD,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.LAW)
    ),
    TROLLHEIM_TELEPORT(
            61, InterfaceID.MagicSpellbook.TROLLHEIM_TELEPORT, SpellbookEnums.StandardSpell.TROLLHEIM_TELEPORT.index, true, Quest.EADGARS_RUSE,
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELEPORT_TO_APE_ATOLL(
            64, InterfaceID.MagicSpellbook.APE_TELEPORT, SpellbookEnums.StandardSpell.APE_ATOLL_TELEPORT.index, true, Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI,
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELEPORT_TO_KOUREND(
            69, InterfaceID.MagicSpellbook.KOUREND_TELEPORT, SpellbookEnums.StandardSpell.KOUREND_CASTLE_TELEPORT.index, true,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(2, Rune.SOUL),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELEOTHER_LUMBRIDGE(
            74, InterfaceID.MagicSpellbook.TELEOTHER_LUMBRIDGE, SpellbookEnums.StandardSpell.TELEOTHER_LUMBRIDGE.index, true,
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.LAW),
            new RuneRequirement(1, Rune.SOUL)
    ),
    TELEOTHER_FALADOR(
            82, InterfaceID.MagicSpellbook.TELEOTHER_FALADOR, SpellbookEnums.StandardSpell.TELEOTHER_FALADOR.index, true,
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.LAW),
            new RuneRequirement(1, Rune.SOUL)
    ),
    TELEPORT_TO_BOUNTY_TARGET(
            85, InterfaceID.MagicSpellbook.BOUNTY_TARGET, SpellbookEnums.StandardSpell.TELEPORT_TO_TARGET.index, true,
            new RuneRequirement(1, Rune.CHAOS),
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.LAW)
    ),
    TELEOTHER_CAMELOT(
            90, InterfaceID.MagicSpellbook.TELEOTHER_CAMELOT, SpellbookEnums.StandardSpell.TELEOTHER_CAMELOT.index, true,
            new RuneRequirement(1, Rune.LAW),
            new RuneRequirement(2, Rune.SOUL)
    ),

    WIND_STRIKE(
            1, InterfaceID.MagicSpellbook.WIND_STRIKE, SpellbookEnums.StandardSpell.WIND_STRIKE.index, 1, false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.MIND)
    ),
    WATER_STRIKE(
            5, InterfaceID.MagicSpellbook.WATER_STRIKE, SpellbookEnums.StandardSpell.WATER_STRIKE.index, 2, false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.MIND)
    ),
    EARTH_STRIKE(
            9, InterfaceID.MagicSpellbook.EARTH_STRIKE, SpellbookEnums.StandardSpell.EARTH_STRIKE.index, 3, false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(1, Rune.MIND)
    ),
    FIRE_STRIKE(
            13, InterfaceID.MagicSpellbook.FIRE_STRIKE, SpellbookEnums.StandardSpell.FIRE_STRIKE.index, 4, false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(1, Rune.MIND)
    ),

    WIND_BOLT(
            17, InterfaceID.MagicSpellbook.WIND_BOLT, SpellbookEnums.StandardSpell.WIND_BOLT.index, 5, false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    WATER_BOLT(
            23, InterfaceID.MagicSpellbook.WATER_BOLT, SpellbookEnums.StandardSpell.WATER_BOLT.index, 6, false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    EARTH_BOLT(
            29, InterfaceID.MagicSpellbook.EARTH_BOLT, SpellbookEnums.StandardSpell.EARTH_BOLT.index, 7, false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(3, Rune.EARTH),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    FIRE_BOLT(
            35, InterfaceID.MagicSpellbook.FIRE_BOLT, SpellbookEnums.StandardSpell.FIRE_BOLT.index, 8, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(1, Rune.CHAOS)
    ),

    WIND_BLAST(
            41, InterfaceID.MagicSpellbook.WIND_BLAST, SpellbookEnums.StandardSpell.WIND_BLAST.index, 9, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.DEATH)
    ),
    WATER_BLAST(
            47, InterfaceID.MagicSpellbook.WATER_BLAST, SpellbookEnums.StandardSpell.WATER_BLAST.index, 10, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.DEATH)
    ),
    EARTH_BLAST(
            53, InterfaceID.MagicSpellbook.EARTH_BLAST, SpellbookEnums.StandardSpell.EARTH_BLAST.index, 11, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(4, Rune.EARTH),
            new RuneRequirement(1, Rune.DEATH)
    ),
    FIRE_BLAST(
            59, InterfaceID.MagicSpellbook.FIRE_BLAST, SpellbookEnums.StandardSpell.FIRE_BLAST.index, 12, false,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.DEATH)
    ),

    WIND_WAVE(
            62, InterfaceID.MagicSpellbook.WIND_WAVE, SpellbookEnums.StandardSpell.WIND_WAVE.index, 13, true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    WATER_WAVE(
            65, InterfaceID.MagicSpellbook.WATER_WAVE, SpellbookEnums.StandardSpell.WATER_WAVE.index, 14, true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(7, Rune.WATER),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    EARTH_WAVE(
            70, InterfaceID.MagicSpellbook.EARTH_WAVE, SpellbookEnums.StandardSpell.EARTH_WAVE.index, 15, true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(7, Rune.EARTH),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    FIRE_WAVE(
            75, InterfaceID.MagicSpellbook.FIRE_WAVE, SpellbookEnums.StandardSpell.FIRE_WAVE.index, 16, true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(7, Rune.FIRE),
            new RuneRequirement(1, Rune.BLOOD)
    ),

    WIND_SURGE(
            81, InterfaceID.MagicSpellbook.WIND_SURGE, SpellbookEnums.StandardSpell.WIND_SURGE.index, 48, true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(1, Rune.WRATH)
    ),
    WATER_SURGE(
            85, InterfaceID.MagicSpellbook.WATER_SURGE, SpellbookEnums.StandardSpell.WATER_SURGE.index, 49, true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(1, Rune.WRATH)
    ),
    EARTH_SURGE(
            90, InterfaceID.MagicSpellbook.EARTH_SURGE, SpellbookEnums.StandardSpell.EARTH_SURGE.index, 50, true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.WRATH)
    ),
    FIRE_SURGE(
            95, InterfaceID.MagicSpellbook.FIRE_SURGE, SpellbookEnums.StandardSpell.FIRE_SURGE.index, 51, true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(10, Rune.FIRE),
            new RuneRequirement(1, Rune.WRATH)
    ),

    SARADOMIN_STRIKE(
            60, InterfaceID.MagicSpellbook.SARADOMIN_STRIKE, SpellbookEnums.StandardSpell.SARADOMIN_STRIKE.index, true,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    CLAWS_OF_GUTHIX(
            60, InterfaceID.MagicSpellbook.CLAWS_OF_GUTHIX, SpellbookEnums.StandardSpell.CLAWS_OF_GUTHIX.index, true,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    FLAMES_OF_ZAMORAK(
            60, InterfaceID.MagicSpellbook.FLAMES_OF_ZAMORAK, SpellbookEnums.StandardSpell.FLAMES_OF_ZAMORAK.index, true,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(2, Rune.BLOOD)
    ),

    CRUMBLE_UNDEAD(
            39, InterfaceID.MagicSpellbook.CRUMBLE_UNDEAD, SpellbookEnums.StandardSpell.CRUMBLE_UNDEAD.index, false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    IBAN_BLAST(
            50, InterfaceID.MagicSpellbook.IBAN_BLAST, SpellbookEnums.StandardSpell.IBAN_BLAST.index, true,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.DEATH)
    ),
    MAGIC_DART(
            50, InterfaceID.MagicSpellbook.MAGIC_DART, SpellbookEnums.StandardSpell.MAGIC_DART.index, true,
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(4, Rune.MIND)
    ),

    CONFUSE(
            3, InterfaceID.MagicSpellbook.CONFUSE, SpellbookEnums.StandardSpell.CONFUSE.index, false,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.BODY)
    ),
    WEAKEN(
            11, InterfaceID.MagicSpellbook.WEAKEN, SpellbookEnums.StandardSpell.WEAKEN.index, false,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.BODY)
    ),
    CURSE(
            19, InterfaceID.MagicSpellbook.CURSE, SpellbookEnums.StandardSpell.CURSE.index, false,
            new RuneRequirement(3, Rune.EARTH),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(1, Rune.BODY)
    ),
    BIND(
            20, InterfaceID.MagicSpellbook.BIND, SpellbookEnums.StandardSpell.BIND.index, false,
            new RuneRequirement(3, Rune.EARTH),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(2, Rune.NATURE)
    ),
    SNARE(
            50, InterfaceID.MagicSpellbook.SNARE, SpellbookEnums.StandardSpell.SNARE.index, false,
            new RuneRequirement(4, Rune.EARTH),
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(3, Rune.NATURE)
    ),
    VULNERABILITY(
            66, InterfaceID.MagicSpellbook.VULNERABILITY, SpellbookEnums.StandardSpell.VULNERABILITY.index, true,
            new RuneRequirement(5, Rune.EARTH),
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(1, Rune.SOUL)
    ),
    ENFEEBLE(
            73, InterfaceID.MagicSpellbook.ENFEEBLE, SpellbookEnums.StandardSpell.ENFEEBLE.index, true,
            new RuneRequirement(8, Rune.EARTH),
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(1, Rune.SOUL)
    ),
    ENTANGLE(
            79, InterfaceID.MagicSpellbook.ENTANGLE, SpellbookEnums.StandardSpell.ENTANGLE.index, true,
            new RuneRequirement(5, Rune.EARTH),
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(4, Rune.NATURE)
    ),
    STUN(
            80, InterfaceID.MagicSpellbook.STUN, SpellbookEnums.StandardSpell.STUN.index, true,
            new RuneRequirement(12, Rune.EARTH),
            new RuneRequirement(12, Rune.WATER),
            new RuneRequirement(1, Rune.SOUL)
    ),
    TELE_BLOCK(
            85, InterfaceID.MagicSpellbook.TELEPORT_BLOCK, SpellbookEnums.StandardSpell.TELE_BLOCK.index, false,
            new RuneRequirement(1, Rune.CHAOS),
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.LAW)
    ),

    CHARGE(
            80, InterfaceID.MagicSpellbook.CHARGE, SpellbookEnums.StandardSpell.TELE_BLOCK.index, true,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(3, Rune.BLOOD)
    ),

    BONES_TO_BANANAS(
            15, InterfaceID.MagicSpellbook.BONES_BANANAS, SpellbookEnums.StandardSpell.BONES_TO_BANANAS.index, false,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(1, Rune.NATURE)
    ),
    LOW_LEVEL_ALCHEMY(
            21, InterfaceID.MagicSpellbook.LOW_ALCHEMY, SpellbookEnums.StandardSpell.LOW_LEVEL_ALCHEMY.index, false,
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(1, Rune.NATURE)
    ),
    SUPERHEAT_ITEM(
            43, InterfaceID.MagicSpellbook.SUPERHEAT, SpellbookEnums.StandardSpell.SUPERHEAT_ITEM.index, false,
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(1, Rune.NATURE)
    ),
    HIGH_LEVEL_ALCHEMY(
            55, InterfaceID.MagicSpellbook.HIGH_ALCHEMY, SpellbookEnums.StandardSpell.HIGH_LEVEL_ALCHEMY.index, false,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.NATURE)
    ),
    BONES_TO_PEACHES(
            60, InterfaceID.MagicSpellbook.BONES_PEACHES, SpellbookEnums.StandardSpell.BONES_TO_PEACHES.index, true,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(2, Rune.NATURE)
    ),

    LVL_1_ENCHANT(
            7, InterfaceID.MagicSpellbook.ENCHANT_1, SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index, false,
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_2_ENCHANT(
            27, InterfaceID.MagicSpellbook.ENCHANT_2, SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index, false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_3_ENCHANT(
            49, InterfaceID.MagicSpellbook.ENCHANT_3, SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index, false,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    CHARGE_WATER_ORB(
            56, InterfaceID.MagicSpellbook.CHARGE_WATER_ORB, SpellbookEnums.StandardSpell.CHARGE_WATER_ORB.index, true,
            new RuneRequirement(30, Rune.WATER),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    LVL_4_ENCHANT(
            57, InterfaceID.MagicSpellbook.ENCHANT_4, SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index, false,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    CHARGE_EARTH_ORB(
            60, InterfaceID.MagicSpellbook.CHARGE_EARTH_ORB, SpellbookEnums.StandardSpell.CHARGE_EARTH_ORB.index, true,
            new RuneRequirement(30, Rune.EARTH),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    CHARGE_FIRE_ORB(
            63, InterfaceID.MagicSpellbook.CHARGE_FIRE_ORB, SpellbookEnums.StandardSpell.CHARGE_FIRE_ORB.index, true,
            new RuneRequirement(30, Rune.FIRE),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    CHARGE_AIR_ORB(
            66, InterfaceID.MagicSpellbook.CHARGE_AIR_ORB, SpellbookEnums.StandardSpell.CHARGE_AIR_ORB.index, true,
            new RuneRequirement(30, Rune.AIR),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    LVL_5_ENCHANT(
            68, InterfaceID.MagicSpellbook.ENCHANT_5, SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index, true,
            new RuneRequirement(15, Rune.EARTH),
            new RuneRequirement(15, Rune.WATER),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_6_ENCHANT(
            87, InterfaceID.MagicSpellbook.ENCHANT_6, SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index, true,
            new RuneRequirement(20, Rune.EARTH),
            new RuneRequirement(20, Rune.FIRE),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_7_ENCHANT(
            93, InterfaceID.MagicSpellbook.ENCHANT_7, SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index, true,
            new RuneRequirement(20, Rune.BLOOD),
            new RuneRequirement(20, Rune.SOUL),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    TELEKINETIC_GRAB(
            31, InterfaceID.MagicSpellbook.TELEGRAB, SpellbookEnums.StandardSpell.TELEKINETIC_GRAB.index, false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.LAW)
    );

    private static final int BOOK = 0;
    private final int level;
    private final int interfaceId;
    private final int autoCastWidgetIndex;
    private final boolean members;
    @Getter
    private final RuneRequirement[] requirements;
    private final Quest questRequirement;
    private final int varbitRequirement;
    private final int spellIndex;

    Standard(int level, int interfaceId, int spellIndex, boolean members, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = null;
        this.varbitRequirement = -1;
        this.autoCastWidgetIndex = -1;
        this.spellIndex = spellIndex;
    }

    Standard(int level, int interfaceId, int spellIndex, boolean members, Quest questRequirement, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = questRequirement;
        this.varbitRequirement = -1;
        this.autoCastWidgetIndex = -1;
        this.spellIndex = spellIndex;
    }

    Standard(int level, int interfaceId, int spellIndex, boolean members, int varbitRequirement, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = null;
        this.varbitRequirement = varbitRequirement;
        this.autoCastWidgetIndex = -1;
        this.spellIndex = spellIndex;
    }

    Standard(int level, int interfaceId, int spellIndex, int autoCastWidgetIndex, boolean members, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = null;
        this.varbitRequirement = -1;
        this.autoCastWidgetIndex = autoCastWidgetIndex;
        this.spellIndex = spellIndex;
    }



    @Override
    public int getAutoCastIndex()
    {
        return autoCastWidgetIndex;
    }

    @Override
    public int getAction()
    {
        if (this != VARROCK_TELEPORT && this != CAMELOT_TELEPORT && this != WATCHTOWER_TELEPORT &&
                this != GRAND_EXCHANGE_TELEPORT && this != SEERS_TELEPORT && this != YANILLE_TELEPORT)
        {
            return 1;
        }

        if (this == VARROCK_TELEPORT || this == GRAND_EXCHANGE_TELEPORT)
        {
            return getAction(VarbitID.VARROCK_GE_TELEPORT, this, VARROCK_TELEPORT, GRAND_EXCHANGE_TELEPORT);
        }

        if (this == CAMELOT_TELEPORT || this == SEERS_TELEPORT)
        {
            return getAction(VarbitID.SEERS_CAMELOT_TELEPORT, this, CAMELOT_TELEPORT, SEERS_TELEPORT);
        }

        if (this == WATCHTOWER_TELEPORT || this == YANILLE_TELEPORT)
        {
            return getAction(VarbitID.YANILLE_TELEPORT_LOCATION, this, WATCHTOWER_TELEPORT, YANILLE_TELEPORT);
        }

        return 1;
    }

    private int getAction(int varbit, Standard spell, Standard baseSpell, Standard variantSpell)
    {
        var config = VarAPI.getVar(varbit);
        if (config == 0)
        {
            // if the config is 0 then the spell is in the default config
            // so the base action is 0 and variant is 1
            return spell == baseSpell ? 1 : 2;
        }

        // if the config has been swapped
        // the variant action is 2 and the base action is 1
        return spell == variantSpell ? 3 : 2;
    }

    @Override
    public int getLevel()
    {
        return level;
    }

    @Override
    public int getWidget()
    {
        return interfaceId;
    }

    @Override
    public boolean canCast()
    {
        return Static.invoke(() -> MagicCast.canCastStandard(spellIndex));
    }

    public boolean haveRunesAvailable()
    {
        for (RuneRequirement req : requirements)
        {
            if (!req.meetsRequirements())
            {
                return false;
            }
        }

        return true;
    }

    public boolean haveEquipment()
    {
        switch (this)
        {
            case IBAN_BLAST:
                return EquipmentAPI.isEquipped(i ->
                        i.getId() == ItemID.IBANS_STAFF || i.getId() == ItemID.IBANS_STAFF_1410 || i.getId() == ItemID.IBANS_STAFF_U
                );
            case MAGIC_DART:
                return EquipmentAPI.isEquipped(i ->
                        i.getId() == ItemID.SLAYERS_STAFF_E || i.getId() == ItemID.SLAYERS_STAFF || i.getId() == ItemID.STAFF_OF_THE_DEAD ||
                                i.getId() == ItemID.STAFF_OF_THE_DEAD_23613 || i.getId() == ItemID.TOXIC_STAFF_OF_THE_DEAD || i.getId() == ItemID.STAFF_OF_LIGHT ||
                                i.getId() == ItemID.STAFF_OF_BALANCE
                );
            case SARADOMIN_STRIKE:
                return EquipmentAPI.isEquipped(i ->
                        i.getId() == ItemID.SARADOMIN_STAFF || i.getId() == ItemID.STAFF_OF_LIGHT
                );
            case FLAMES_OF_ZAMORAK:
                return EquipmentAPI.isEquipped(i ->
                        i.getId() == ItemID.ZAMORAK_STAFF || i.getId() == ItemID.STAFF_OF_THE_DEAD || i.getId() == ItemID.STAFF_OF_THE_DEAD_23613
                                || i.getId() == ItemID.TOXIC_STAFF_OF_THE_DEAD
                );
            case CLAWS_OF_GUTHIX:
                return EquipmentAPI.isEquipped(i ->
                        i.getId() == ItemID.GUTHIX_STAFF || i.getId() == ItemID.VOID_KNIGHT_MACE || i.getId() == ItemID.STAFF_OF_BALANCE
                );
            default:
                return true;
        }
    }

    public boolean haveItem()
    {
        switch (this)
        {
            case TELEPORT_TO_APE_ATOLL:
                return InventoryAPI.contains(ItemID.BANANA);
            case CHARGE_AIR_ORB:
            case CHARGE_WATER_ORB:
            case CHARGE_EARTH_ORB:
            case CHARGE_FIRE_ORB:
                return InventoryAPI.contains(ItemID.UNPOWERED_ORB);
            default:
                return true;
        }
    }

    public boolean hasRequirements()
    {
        Client client = Static.getClient();
        if (questRequirement == null && varbitRequirement == -1)
        {
            return true;
        }

        if (questRequirement != null && QuestAPI.getState(questRequirement) != QuestState.FINISHED)
        {
            return false;
        }

        if (varbitRequirement != -1 && VarAPI.getVar(varbitRequirement) != 1)
        {
            return false;
        }

        return true;
    }
}
