package com.tonic.data.magic.spellbooks;

import com.tonic.Static;
import com.tonic.api.game.GameAPI;
import com.tonic.api.game.QuestAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.MagicAPI;
import com.tonic.data.magic.Rune;
import com.tonic.data.magic.RuneRequirement;
import com.tonic.data.magic.Spell;
import lombok.Getter;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

import static com.tonic.data.magic.SpellBook.*;

public enum Lunar implements Spell
{
    // Teleport spells
    LUNAR_HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_LUNAR
    ),
    MOONCLAN_TELEPORT(
            69,
            InterfaceID.MagicSpellbook.TELE_MOONCLAN,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    TELE_GROUP_MOONCLAN(
            70,
            InterfaceID.MagicSpellbook.TELE_GROUP_MOONCLAN,
            new RuneRequirement(4, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    OURANIA_TELEPORT(
            71,
            InterfaceID.MagicSpellbook.OURANIA_TELEPORT,
            new RuneRequirement(6, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    WATERBIRTH_TELEPORT(
            72,
            InterfaceID.MagicSpellbook.TELE_WATERBIRTH,
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    TELE_GROUP_WATERBIRTH(
            73,
            InterfaceID.MagicSpellbook.TELE_GROUP_WATERBIRTH,
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    BARBARIAN_TELEPORT(
            75,
            InterfaceID.MagicSpellbook.TELE_BARB_OUT,
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELE_GROUP_BARBARIAN(
            76,
            InterfaceID.MagicSpellbook.TELE_GROUP_BARBARIAN,
            new RuneRequirement(6, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)
    ),
    KHAZARD_TELEPORT(
            78,
            InterfaceID.MagicSpellbook.TELE_KHAZARD,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)
    ),
    TELE_GROUP_KHAZARD(
            79,
            InterfaceID.MagicSpellbook.TELE_GROUP_KHAZARD,
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)
    ),
    FISHING_GUILD_TELEPORT(
            85,
            InterfaceID.MagicSpellbook.TELE_FISH,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)
    ),
    TELE_GROUP_FISHING_GUILD(
            86,
            InterfaceID.MagicSpellbook.TELE_GROUP_FISHING_GUILD,
            new RuneRequirement(14, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)
    ),
    CATHERBY_TELEPORT(
            87,
            InterfaceID.MagicSpellbook.TELE_CATHER,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)
    ),
    TELE_GROUP_CATHERBY(
            88,
            InterfaceID.MagicSpellbook.TELE_GROUP_CATHERBY,
            new RuneRequirement(15, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)
    ),
    ICE_PLATEAU_TELEPORT(
            89,
            InterfaceID.MagicSpellbook.TELE_GHORROCK,
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)
    ),
    TELE_GROUP_ICE_PLATEAU(
            90,
            InterfaceID.MagicSpellbook.TELE_GROUP_GHORROCK,
            new RuneRequirement(16, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)
    ),

    // Combat spells
    MONSTER_EXAMINE(
            66,
            InterfaceID.MagicSpellbook.MONSTER_EXAMINE,
            true,
            new RuneRequirement(1, Rune.MIND),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(1, Rune.ASTRAL)
    ),
    CURE_OTHER(
            66,
            InterfaceID.MagicSpellbook.CURE_OTHER,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    CURE_ME(
            66,
            InterfaceID.MagicSpellbook.CURE_ME,
            new RuneRequirement(2, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    CURE_GROUP(
            66,
            InterfaceID.MagicSpellbook.CURE_GROUP,
            new RuneRequirement(2, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)
    ),
    STAT_SPY(
            66,
            InterfaceID.MagicSpellbook.STAT_SPY,
            true,
            new RuneRequirement(5, Rune.BODY),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL)
    ),
    DREAM(
            66,
            InterfaceID.MagicSpellbook.DREAM,
            true,
            new RuneRequirement(5, Rune.BODY),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL)
    ),
    STAT_RESTORE_POT_SHARE(
            66,
            InterfaceID.MagicSpellbook.REST_POT_SHARE,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL)
    ),
    BOOST_POTION_SHARE(
            66,
            InterfaceID.MagicSpellbook.STREN_POT_SHARE,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(12, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL)
    ),
    ENERGY_TRANSFER(
            66,
            InterfaceID.MagicSpellbook.ENERGY_TRANS,
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(1, Rune.NATURE),
            new RuneRequirement(2, Rune.LAW)
    ),
    HEAL_OTHER(
            66,
            InterfaceID.MagicSpellbook.HEAL_OTHER,
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    VENGEANCE_OTHER(
            66,
            InterfaceID.MagicSpellbook.VENGEANCE_OTHER,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(2, Rune.DEATH)
    ),
    VENGEANCE(
            66,
            InterfaceID.MagicSpellbook.VENGEANCE,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(4, Rune.ASTRAL),
            new RuneRequirement(2, Rune.DEATH)
    ),
    HEAL_GROUP(
            66,
            InterfaceID.MagicSpellbook.HEAL_GROUP,
            new RuneRequirement(4, Rune.ASTRAL),
            new RuneRequirement(6, Rune.LAW),
            new RuneRequirement(3, Rune.BLOOD)
    ),

    // Utility spells
    BAKE_PIE(
            66,
            InterfaceID.MagicSpellbook.BAKE_PIE,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.ASTRAL)
    ),
    GEOMANCY(
            66,
            InterfaceID.MagicSpellbook.GEOMANCY,
            new RuneRequirement(8, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.NATURE)
    ),
    CURE_PLANT(
            66,
            InterfaceID.MagicSpellbook.CURE_PLANT,
            new RuneRequirement(8, Rune.EARTH),
            new RuneRequirement(1, Rune.ASTRAL)
    ),
    NPC_CONTACT(
            66,
            InterfaceID.MagicSpellbook.NPC_CONTACT,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(1, Rune.ASTRAL)
    ),
    HUMIDIFY(
            66,
            InterfaceID.MagicSpellbook.HUMIDIFY,
            true,
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(1, Rune.ASTRAL)
    ),
    HUNTER_KIT(
            66,
            InterfaceID.MagicSpellbook.HUNTER_KIT,
            true,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL)
    ),
    SPIN_FLAX(
            66,
            InterfaceID.MagicSpellbook.SPIN_FLAX,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.ASTRAL),
            new RuneRequirement(2, Rune.NATURE)
    ),
    SUPERGLASS_MAKE(
            66,
            InterfaceID.MagicSpellbook.SUPERGLASS,
            new RuneRequirement(10, Rune.AIR),
            new RuneRequirement(6, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL)
    ),
    TAN_LEATHER(
            66,
            InterfaceID.MagicSpellbook.TAN_LEATHER,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.NATURE)
    ),
    STRING_JEWELLERY(
            66,
            InterfaceID.MagicSpellbook.STRING_JEWEL,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL)
    ),
    MAGIC_IMBUE(
            66,
            InterfaceID.MagicSpellbook.MAGIC_IMBUE,
            new RuneRequirement(7, Rune.WATER),
            new RuneRequirement(7, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL)
    ),
    FERTILE_SOIL(
            66,
            InterfaceID.MagicSpellbook.FERTILE_SOIL,
            new RuneRequirement(15, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(2, Rune.NATURE)
    ),
    PLANK_MAKE(
            66,
            InterfaceID.MagicSpellbook.PLANK_MAKE,
            true,
            new RuneRequirement(15, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.NATURE)
    ),
    RECHARGE_DRAGONSTONE(
            66,
            InterfaceID.MagicSpellbook.RECHARGE_DRAGONSTONE,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(1, Rune.ASTRAL),
            new RuneRequirement(1, Rune.SOUL)
    ),
    SPELLBOOK_SWAP(
            66,
            InterfaceID.MagicSpellbook.SPELLBOOK_SWAP,
            true,
            new RuneRequirement(2, Rune.COSMIC),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)
    ),
    ;

    private final int level;
    private final int interfaceUd;
    private final int autoCastWidgetIndex;
    @Getter
    private final RuneRequirement[] requirements;
    private final boolean dreamMentorRequired;

    Lunar(int level, int interfaceUd, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceUd = interfaceUd;
        this.requirements = requirements;
        this.dreamMentorRequired = false;
        this.autoCastWidgetIndex = -1;
    }

    Lunar(int level, int interfaceUd, boolean dreamMentorRequired, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceUd = interfaceUd;
        this.requirements = requirements;
        this.dreamMentorRequired = dreamMentorRequired;
        this.autoCastWidgetIndex = -1;
    }

    Lunar(int level, int interfaceUd, int autoCastWidgetIndex, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceUd = interfaceUd;
        this.requirements = requirements;
        this.dreamMentorRequired = false;
        this.autoCastWidgetIndex = autoCastWidgetIndex;
    }

    Lunar(int level, int interfaceUd, int autoCastWidgetIndex, boolean dreamMentorRequired, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceUd = interfaceUd;
        this.requirements = requirements;
        this.dreamMentorRequired = dreamMentorRequired;
        this.autoCastWidgetIndex = autoCastWidgetIndex;
    }

    @Override
    public int getAction() {
        return 1;
    }

    @Override
    public int getLevel()
    {
        return level;
    }

    @Override
    public int getWidget()
    {
        return interfaceUd;
    }

    @Override
    public int getAutoCastIndex()
    {
        return autoCastWidgetIndex;
    }

    public boolean canCast()
    {
        if (getCurrent() != LUNAR)
        {
            return false;
        }

            /*if (!Worlds.inMembersWorld())
            {
                return false;
            }*/

        if (this == LUNAR_HOME_TELEPORT)
        {
            return MagicAPI.isHomeTeleportOnCooldown();
        }

        if (this.dreamMentorRequired && QuestAPI.getState(Quest.DREAM_MENTOR) != QuestState.FINISHED)
        {
            return false;
        }

        // some varbit for this
        if (this == OURANIA_TELEPORT) // && Game.getVar() ...
        {
            // return false;
        }

        if (VarAPI.getVar(VarbitID.FREMENNIK_DIARY_HARD_COMPLETE) == 0 && (this == TAN_LEATHER || this == RECHARGE_DRAGONSTONE))
        {
            return false;
        }

            if (level > GameAPI.getRealSkillLevel(Skill.MAGIC) || level > GameAPI.getBoostedSkillLevel(Skill.MAGIC))
            {
                return false;
            }

        return haveRunesAvailable();
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
}
