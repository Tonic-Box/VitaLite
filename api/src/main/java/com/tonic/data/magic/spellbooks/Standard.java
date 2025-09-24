package com.tonic.data.magic.spellbooks;

import com.tonic.Static;
import com.tonic.api.game.QuestAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.MagicAPI;
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
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD,
            false
    ),
    VARROCK_TELEPORT(
            25,
            InterfaceID.MagicSpellbook.VARROCK_TELEPORT,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(1, Rune.LAW)
    ),
    GRAND_EXCHANGE_TELEPORT(
            25,
            InterfaceID.MagicSpellbook.VARROCK_TELEPORT,
            true,
            VarbitID.VARROCK_DIARY_MEDIUM_COMPLETE,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(1, Rune.LAW)
    ),
    LUMBRIDGE_TELEPORT(
            31,
            InterfaceID.MagicSpellbook.LUMBRIDGE_TELEPORT,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.LAW)
    ),
    FALADOR_TELEPORT(
            37,
            InterfaceID.MagicSpellbook.FALADOR_TELEPORT,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.LAW)
    ),
    TELEPORT_TO_HOUSE(
            40,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD,
            true,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.LAW)
    ),
    CAMELOT_TELEPORT(
            45,
            InterfaceID.MagicSpellbook.CAMELOT_TELEPORT,
            true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.LAW)
    ),
    SEERS_TELEPORT(
            45,
            InterfaceID.MagicSpellbook.CAMELOT_TELEPORT,
            true,
            Varbits.DIARY_KANDARIN_HARD,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.LAW)
    ),
    ARDOUGNE_TELEPORT(
            51,
            InterfaceID.MagicSpellbook.ARDOUGNE_TELEPORT,
            true,
            Quest.PLAGUE_CITY,
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(2, Rune.LAW)
    ),
    WATCHTOWER_TELEPORT(
            58,
            InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT,
            true,
            Quest.WATCHTOWER,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.LAW)
    ),

    YANILLE_TELEPORT(
            58,
            InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT,
            true,
            Varbits.DIARY_ARDOUGNE_HARD,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.LAW)
    ),
    TROLLHEIM_TELEPORT(
            61,
            InterfaceID.MagicSpellbook.TROLLHEIM_TELEPORT,
            true,
            Quest.EADGARS_RUSE,
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELEPORT_TO_APE_ATOLL(
            64,
            InterfaceID.MagicSpellbook.APE_TELEPORT,
            true,
            Quest.RECIPE_FOR_DISASTER__KING_AWOWOGEI,
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELEPORT_TO_KOUREND(
            69,
            InterfaceID.MagicSpellbook.KOUREND_TELEPORT,
            true,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(2, Rune.SOUL),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELEOTHER_LUMBRIDGE(
            74,
            InterfaceID.MagicSpellbook.TELEOTHER_LUMBRIDGE,
            true,
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.LAW),
            new RuneRequirement(1, Rune.SOUL)
    ),
    TELEOTHER_FALADOR(
            82,
            InterfaceID.MagicSpellbook.TELEOTHER_FALADOR,
            true,
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.LAW),
            new RuneRequirement(1, Rune.SOUL)
    ),
    TELEPORT_TO_BOUNTY_TARGET(
            85,
            InterfaceID.MagicSpellbook.BOUNTY_TARGET,
            true,
            new RuneRequirement(1, Rune.CHAOS),
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.LAW)
    ),
    TELEOTHER_CAMELOT(
            90,
            InterfaceID.MagicSpellbook.TELEOTHER_CAMELOT,
            true,
            new RuneRequirement(1, Rune.LAW),
            new RuneRequirement(2, Rune.SOUL)
    ),

    // Strike spells
    WIND_STRIKE(
            1,
            InterfaceID.MagicSpellbook.WIND_STRIKE,
            1,
            false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.MIND)
    ),
    WATER_STRIKE(
            5,
            InterfaceID.MagicSpellbook.WATER_STRIKE,
            2,
            false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.MIND)
    ),
    EARTH_STRIKE(
            9,
            InterfaceID.MagicSpellbook.EARTH_STRIKE,
            3,
            false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(1, Rune.MIND)
    ),
    FIRE_STRIKE(
            13,
            InterfaceID.MagicSpellbook.FIRE_STRIKE,
            4,
            false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(1, Rune.MIND)
    ),

    // Bolt spells
    WIND_BOLT(
            17,
            InterfaceID.MagicSpellbook.WIND_BOLT,
            5,
            false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    WATER_BOLT(
            23,
            InterfaceID.MagicSpellbook.WATER_BOLT,
            6,
            false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    EARTH_BOLT(
            29,
            InterfaceID.MagicSpellbook.EARTH_BOLT,
            7,
            false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(3, Rune.EARTH),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    FIRE_BOLT(
            35,
            InterfaceID.MagicSpellbook.FIRE_BOLT,
            8,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(1, Rune.CHAOS)
    ),

    // Blast spells
    WIND_BLAST(
            41,
            InterfaceID.MagicSpellbook.WIND_BLAST,
            9,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.DEATH)
    ),
    WATER_BLAST(
            47,
            InterfaceID.MagicSpellbook.WATER_BLAST,
            10,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.DEATH)
    ),
    EARTH_BLAST(
            53,
            InterfaceID.MagicSpellbook.EARTH_BLAST,
            11,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(4, Rune.EARTH),
            new RuneRequirement(1, Rune.DEATH)
    ),
    FIRE_BLAST(
            59,
            InterfaceID.MagicSpellbook.FIRE_BLAST,
            12,
            false,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.DEATH)
    ),

    // Wave spells
    WIND_WAVE(
            62,
            InterfaceID.MagicSpellbook.WIND_WAVE,
            13,
            true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    WATER_WAVE(
            65,
            InterfaceID.MagicSpellbook.WATER_WAVE,
            14,
            true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(7, Rune.WATER),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    EARTH_WAVE(
            70,
            InterfaceID.MagicSpellbook.EARTH_WAVE,
            15,
            true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(7, Rune.EARTH),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    FIRE_WAVE(
            75,
            InterfaceID.MagicSpellbook.FIRE_WAVE,
            16,
            true,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(7, Rune.FIRE),
            new RuneRequirement(1, Rune.BLOOD)
    ),

    // Surge spells
    WIND_SURGE(
            81,
            InterfaceID.MagicSpellbook.WIND_SURGE,
            48,
            true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(1, Rune.WRATH)
    ),
    WATER_SURGE(
            85,
            InterfaceID.MagicSpellbook.WATER_SURGE,
            49,
            true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(1, Rune.WRATH)
    ),
    EARTH_SURGE(
            90,
            InterfaceID.MagicSpellbook.EARTH_SURGE,
            50,
            true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.WRATH)
    ),
    FIRE_SURGE(
            95,
            InterfaceID.MagicSpellbook.FIRE_SURGE,
            51,
            true,
            new RuneRequirement(7, Rune.AIR),
            new RuneRequirement(10, Rune.FIRE),
            new RuneRequirement(1, Rune.WRATH)
    ),

    // God spells
    SARADOMIN_STRIKE(
            60,
            InterfaceID.MagicSpellbook.SARADOMIN_STRIKE,
            true,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    CLAWS_OF_GUTHIX(
            60,
            InterfaceID.MagicSpellbook.CLAWS_OF_GUTHIX,
            true,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    FLAMES_OF_ZAMORAK(
            60,
            InterfaceID.MagicSpellbook.FLAMES_OF_ZAMORAK,
            true,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(2, Rune.BLOOD)
    ),

    // Other combat spells
    CRUMBLE_UNDEAD(
            39,
            InterfaceID.MagicSpellbook.CRUMBLE_UNDEAD,
            false,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(1, Rune.CHAOS)
    ),
    IBAN_BLAST(
            50,
            InterfaceID.MagicSpellbook.IBAN_BLAST,
            true,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.DEATH)
    ),
    MAGIC_DART(
            50,
            InterfaceID.MagicSpellbook.MAGIC_DART,
            true,
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(4, Rune.MIND)
    ),

    // Curse spells
    CONFUSE(
            3,
            InterfaceID.MagicSpellbook.CONFUSE,
            false,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.BODY)
    ),
    WEAKEN(
            11,
            InterfaceID.MagicSpellbook.WEAKEN,
            false,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.BODY)
    ),
    CURSE(
            19,
            InterfaceID.MagicSpellbook.CURSE,
            false,
            new RuneRequirement(3, Rune.EARTH),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(1, Rune.BODY)
    ),
    BIND(
            20,
            InterfaceID.MagicSpellbook.BIND,
            false,
            new RuneRequirement(3, Rune.EARTH),
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(2, Rune.NATURE)
    ),
    SNARE(
            50,
            InterfaceID.MagicSpellbook.SNARE,
            false,
            new RuneRequirement(4, Rune.EARTH),
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(3, Rune.NATURE)
    ),
    VULNERABILITY(
            66,
            InterfaceID.MagicSpellbook.VULNERABILITY,
            true,
            new RuneRequirement(5, Rune.EARTH),
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(1, Rune.SOUL)
    ),
    ENFEEBLE(
            73,
            InterfaceID.MagicSpellbook.ENFEEBLE,
            true,
            new RuneRequirement(8, Rune.EARTH),
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(1, Rune.SOUL)
    ),
    ENTANGLE(
            79,
            InterfaceID.MagicSpellbook.ENTANGLE,
            true,
            new RuneRequirement(5, Rune.EARTH),
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(4, Rune.NATURE)
    ),
    STUN(
            80,
            InterfaceID.MagicSpellbook.STUN,
            true,
            new RuneRequirement(12, Rune.EARTH),
            new RuneRequirement(12, Rune.WATER),
            new RuneRequirement(1, Rune.SOUL)
    ),
    TELE_BLOCK(
            85,
            InterfaceID.MagicSpellbook.TELEPORT_BLOCK,
            false,
            new RuneRequirement(1, Rune.CHAOS),
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.LAW)
    ),

    // Support spells
    CHARGE(
            80,
            InterfaceID.MagicSpellbook.CHARGE,
            true,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(3, Rune.BLOOD)
    ),

    // Utility spells
    BONES_TO_BANANAS(
            15,
            InterfaceID.MagicSpellbook.BONES_BANANAS,
            false,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(1, Rune.NATURE)
    ),
    LOW_LEVEL_ALCHEMY(
            21,
            InterfaceID.MagicSpellbook.LOW_ALCHEMY,
            false,
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(1, Rune.NATURE)
    ),
    SUPERHEAT_ITEM(
            43,
            InterfaceID.MagicSpellbook.SUPERHEAT,
            false,
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(1, Rune.NATURE)
    ),
    HIGH_LEVEL_ALCHEMY(
            55,
            InterfaceID.MagicSpellbook.HIGH_ALCHEMY,
            false,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.NATURE)
    ),
    BONES_TO_PEACHES(
            60,
            InterfaceID.MagicSpellbook.BONES_PEACHES,
            true,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(2, Rune.NATURE)
    ),

    // Enchantment spells
    LVL_1_ENCHANT(
            7,
            InterfaceID.MagicSpellbook.ENCHANT_1,
            false,
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_2_ENCHANT(
            27,
            InterfaceID.MagicSpellbook.ENCHANT_2,
            false,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_3_ENCHANT(
            49,
            InterfaceID.MagicSpellbook.ENCHANT_3,
            false,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    CHARGE_WATER_ORB(
            56,
            InterfaceID.MagicSpellbook.CHARGE_WATER_ORB,
            true,
            new RuneRequirement(30, Rune.WATER),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    LVL_4_ENCHANT(
            57,
            InterfaceID.MagicSpellbook.ENCHANT_4,
            false,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    CHARGE_EARTH_ORB(
            60,
            InterfaceID.MagicSpellbook.CHARGE_EARTH_ORB,
            true,
            new RuneRequirement(30, Rune.EARTH),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    CHARGE_FIRE_ORB(
            63,
            InterfaceID.MagicSpellbook.CHARGE_FIRE_ORB,
            true,
            new RuneRequirement(30, Rune.FIRE),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    CHARGE_AIR_ORB(
            66,
            InterfaceID.MagicSpellbook.CHARGE_AIR_ORB,
            true,
            new RuneRequirement(30, Rune.AIR),
            new RuneRequirement(3, Rune.COSMIC)
    ),
    LVL_5_ENCHANT(
            68,
            InterfaceID.MagicSpellbook.ENCHANT_5,
            true,
            new RuneRequirement(15, Rune.EARTH),
            new RuneRequirement(15, Rune.WATER),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_6_ENCHANT(
            87,
            InterfaceID.MagicSpellbook.ENCHANT_6,
            true,
            new RuneRequirement(20, Rune.EARTH),
            new RuneRequirement(20, Rune.FIRE),
            new RuneRequirement(1, Rune.COSMIC)
    ),
    LVL_7_ENCHANT(
            93,
            InterfaceID.MagicSpellbook.ENCHANT_7,
            true,
            new RuneRequirement(20, Rune.BLOOD),
            new RuneRequirement(20, Rune.SOUL),
            new RuneRequirement(1, Rune.COSMIC)
    ),

    // Other spells
    TELEKINETIC_GRAB(
            31,
            InterfaceID.MagicSpellbook.TELEGRAB,
            false,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.LAW)
    ),
    ;

    private final int level;
    private final int interfaceId;
    private final int autoCastWidgetIndex;
    private final boolean members;
    @Getter
    private final RuneRequirement[] requirements;
    private final Quest questRequirement;
    private final int varbitRequirement;

    Standard(int level, int interfaceId, boolean members, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = null;
        this.varbitRequirement = -1;
        this.autoCastWidgetIndex = -1;
    }

    Standard(int level, int interfaceId, boolean members, Quest questRequirement, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = questRequirement;
        this.varbitRequirement = -1;
        this.autoCastWidgetIndex = -1;
    }

    Standard(int level, int interfaceId, boolean members, int varbitRequirement, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = null;
        this.varbitRequirement = varbitRequirement;
        this.autoCastWidgetIndex = -1;
    }

    Standard(int level, int interfaceId, int autoCastWidgetIndex, boolean members, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = null;
        this.varbitRequirement = -1;
        this.autoCastWidgetIndex = autoCastWidgetIndex;
    }

    Standard(int level, int interfaceId, int autoCastWidgetIndex, boolean members, Quest questRequirement, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = questRequirement;
        this.varbitRequirement = -1;
        this.autoCastWidgetIndex = autoCastWidgetIndex;
    }

    Standard(int level, int interfaceId, int autoCastWidgetIndex, boolean members, int varbitRequirement, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.members = members;
        this.requirements = requirements;
        this.questRequirement = null;
        this.varbitRequirement = varbitRequirement;
        this.autoCastWidgetIndex = autoCastWidgetIndex;
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
        if (getCurrent() != STANDARD)
        {
            return false;
        }

            /*if (members && !Worlds.inMembersWorld())
            {
                return false;
            }*/

        if (this == HOME_TELEPORT)
        {
            return MagicAPI.isHomeTeleportOnCooldown();
        }

            /*if (level > Skills.getLevel(Skill.MAGIC) || level > Skills.getBoostedLevel(Skill.MAGIC))
            {
                return false;
            }*/

        // these teleports require using a scroll to unlock
        if (this == ARDOUGNE_TELEPORT && VarAPI.getVarp(165) < 30)
        {
            return false;
        }

//        if (this == WATCHTOWER_TELEPORT) // && Game...
//        {
//            // return false;
//        }
//
//        if (this == TELEPORT_TO_KOUREND) // && Game...
//        {
//            // return false;
//        }

        return hasRequirements() && haveEquipment() && haveItem() && haveRunesAvailable();
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
