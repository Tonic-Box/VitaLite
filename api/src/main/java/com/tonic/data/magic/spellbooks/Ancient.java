package com.tonic.data.magic.spellbooks;

import com.tonic.api.game.GameAPI;
import com.tonic.api.threaded.WorldsAPI;
import com.tonic.api.widgets.MagicAPI;
import com.tonic.data.magic.Rune;
import com.tonic.data.magic.RuneRequirement;
import com.tonic.data.magic.Spell;
import lombok.Getter;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import static com.tonic.data.magic.SpellBook.*;

public enum Ancient implements Spell
{
    // Teleport spells
    EDGEVILLE_HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_ZAROS
    ),
    PADDEWWA_TELEPORT(
            54,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT1,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(2, Rune.LAW)
    ),
    SENNTISTEN_TELEPORT(
            60,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT2,
            new RuneRequirement(2, Rune.LAW),
            new RuneRequirement(1, Rune.SOUL)
    ),
    KHARYRLL_TELEPORT(
            66,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT3,
            new RuneRequirement(2, Rune.LAW),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    LASSAR_TELEPORT(
            72,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT4,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(2, Rune.LAW)
    ),
    DAREEYAK_TELEPORT(
            78,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT5,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(2, Rune.LAW)
    ),
    CARRALLANGER_TELEPORT(
            84,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT6,
            new RuneRequirement(2, Rune.LAW),
            new RuneRequirement(2, Rune.SOUL)
    ),
    BOUNTY_TARGET_TELEPORT(
            85,
            InterfaceID.MagicSpellbook.BOUNTY_TARGET,
            new RuneRequirement(1, Rune.CHAOS),
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.LAW)
    ),
    ANNAKARL_TELEPORT(
            90,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT7,
            new RuneRequirement(2, Rune.LAW),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    GHORROCK_TELEPORT(
            96,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT8,
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(2, Rune.LAW)
    ),

    // Rush Spells
    SMOKE_RUSH(
            50,
            InterfaceID.MagicSpellbook.SMOKE_RUSH,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)
    ),
    SHADOW_RUSH(
            52,
            InterfaceID.MagicSpellbook.SHADOW_RUSH,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(1, Rune.SOUL)
    ),
    BLOOD_RUSH(
            56,
            InterfaceID.MagicSpellbook.BLOOD_RUSH,
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(1, Rune.BLOOD)
    ),
    ICE_RUSH(
            58,
            InterfaceID.MagicSpellbook.ICE_RUSH,
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)
    ),

    // Burst Spells
    SMOKE_BURST(
            62,
            InterfaceID.MagicSpellbook.SMOKE_BURST,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(4, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)
    ),
    SHADOW_BURST(
            64,
            InterfaceID.MagicSpellbook.SHADOW_BURST,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(4, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.SOUL)
    ),
    BLOOD_BURST(
            68,
            InterfaceID.MagicSpellbook.BLOOD_BURST,
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    ICE_BURST(
            70,
            InterfaceID.MagicSpellbook.ICE_BURST,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(4, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)
    ),

    // Blitz Spells
    SMOKE_BLITZ(
            74,
            InterfaceID.MagicSpellbook.SMOKE_BLITZ,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    SHADOW_BLITZ(
            76,
            InterfaceID.MagicSpellbook.SHADOW_BLITZ,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD),
            new RuneRequirement(2, Rune.SOUL)
    ),
    BLOOD_BLITZ(
            80,
            InterfaceID.MagicSpellbook.BLOOD_BLITZ,
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(4, Rune.BLOOD)
    ),
    ICE_BLITZ(
            82,
            InterfaceID.MagicSpellbook.ICE_BLITZ,
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)
    ),

    // Barrage Spells
    SMOKE_BARRAGE(
            86,
            InterfaceID.MagicSpellbook.SMOKE_BARRAGE,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)
    ),
    SHADOW_BARRAGE(
            88,
            InterfaceID.MagicSpellbook.SHADOW_BARRAGE,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD),
            new RuneRequirement(3, Rune.SOUL)
    ),
    BLOOD_BARRAGE(
            92,
            InterfaceID.MagicSpellbook.BLOOD_BARRAGE,
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(4, Rune.BLOOD),
            new RuneRequirement(1, Rune.SOUL)
    ),
    ICE_BARRAGE(
            94,
            InterfaceID.MagicSpellbook.ICE_BARRAGE,
            new RuneRequirement(6, Rune.WATER),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)
    );

    private final int level;
    private final int interfaceId;
    private final int autoCastWidgetIndex;
    @Getter
    private final RuneRequirement[] requirements;

    Ancient(int level, int interfaceId, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.requirements = requirements;
        this.autoCastWidgetIndex = -1;
    }

    Ancient(int level, int interfaceId, int autoCastWidgetIndex, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.requirements = requirements;
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
        return interfaceId;
    }

    @Override
    public int getAutoCastIndex()
    {
        return autoCastWidgetIndex;
    }

    public boolean canCast()
    {
        if (getCurrent() != ANCIENT)
        {
            return false;
        }

            if (!WorldsAPI.inMembersWorld())
            {
                return false;
            }

        if (this == EDGEVILLE_HOME_TELEPORT)
        {
            return MagicAPI.isHomeTeleportOnCooldown();
        }

        if (level > GameAPI.getRealSkillLevel(Skill.MAGIC) || level > GameAPI.getRealSkillLevel(Skill.MAGIC))
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