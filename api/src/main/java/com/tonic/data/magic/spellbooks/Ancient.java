package com.tonic.data.magic.spellbooks;

import com.tonic.Static;
import com.tonic.data.magic.MagicCast;
import com.tonic.data.magic.Rune;
import com.tonic.data.magic.RuneRequirement;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.spellbooks.SpellbookEnums.AncientSpell;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

public enum Ancient implements Spell
{
    EDGEVILLE_HOME_TELEPORT(
            0, InterfaceID.MagicSpellbook.TELEPORT_HOME_ZAROS, AncientSpell.EDGEVILLE_HOME_TELEPORT.index),

    SMOKE_RUSH(
            50, InterfaceID.MagicSpellbook.SMOKE_RUSH, AncientSpell.SMOKE_RUSH.index,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)),

    SHADOW_RUSH(
            52, InterfaceID.MagicSpellbook.SHADOW_RUSH, AncientSpell.SHADOW_RUSH.index,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(1, Rune.SOUL)),

    PADDEWWA_TELEPORT(
            54, InterfaceID.MagicSpellbook.ZAROSTELEPORT1, AncientSpell.PADDEWWA_TELEPORT.index,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(2, Rune.LAW)),

    BLOOD_RUSH(
            56, InterfaceID.MagicSpellbook.BLOOD_RUSH, AncientSpell.BLOOD_RUSH.index,
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(1, Rune.BLOOD)),

    ICE_RUSH(
            58, InterfaceID.MagicSpellbook.ICE_RUSH, AncientSpell.ICE_RUSH.index,
            new RuneRequirement(2, Rune.WATER),
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)),

    SENNTISTEN_TELEPORT(
            60, InterfaceID.MagicSpellbook.ZAROSTELEPORT2, AncientSpell.SENNTISTEN_TELEPORT.index,
            new RuneRequirement(2, Rune.LAW),
            new RuneRequirement(1, Rune.SOUL)),

    SMOKE_BURST(
            62, InterfaceID.MagicSpellbook.SMOKE_BURST, AncientSpell.SMOKE_BURST.index,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(4, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)),

    SHADOW_BURST(
            64, InterfaceID.MagicSpellbook.SHADOW_BURST, AncientSpell.SHADOW_BURST.index,
            new RuneRequirement(1, Rune.AIR),
            new RuneRequirement(4, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.SOUL)),

    BLOOD_BURST(
            68, InterfaceID.MagicSpellbook.BLOOD_BURST, AncientSpell.BLOOD_BURST.index,
            new RuneRequirement(2, Rune.CHAOS),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)),

    ICE_BURST(
            70, InterfaceID.MagicSpellbook.ICE_BURST, AncientSpell.ICE_BURST.index,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(4, Rune.CHAOS),
            new RuneRequirement(2, Rune.DEATH)),

    SMOKE_BLITZ(
            74, InterfaceID.MagicSpellbook.SMOKE_BLITZ, AncientSpell.SMOKE_BLITZ.index,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.FIRE),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)),

    SHADOW_BLITZ(
            76, InterfaceID.MagicSpellbook.SHADOW_BLITZ, AncientSpell.SHADOW_BLITZ.index,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD),
            new RuneRequirement(2, Rune.SOUL)),

    BLOOD_BLITZ(
            80, InterfaceID.MagicSpellbook.BLOOD_BLITZ, AncientSpell.BLOOD_BLITZ.index,
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(4, Rune.BLOOD)),

    ICE_BLITZ(
            82, InterfaceID.MagicSpellbook.ICE_BLITZ, AncientSpell.ICE_BLITZ.index,
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(2, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)),

    SMOKE_BARRAGE(
            86, InterfaceID.MagicSpellbook.SMOKE_BARRAGE, AncientSpell.SMOKE_BARRAGE.index,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)),

    SHADOW_BARRAGE(
            88, InterfaceID.MagicSpellbook.SHADOW_BARRAGE, AncientSpell.SHADOW_BARRAGE.index,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD),
            new RuneRequirement(3, Rune.SOUL)),

    BLOOD_BARRAGE(
            92, InterfaceID.MagicSpellbook.BLOOD_BARRAGE, AncientSpell.BLOOD_BARRAGE.index,
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(4, Rune.BLOOD),
            new RuneRequirement(1, Rune.SOUL)),

    ICE_BARRAGE(
            94, InterfaceID.MagicSpellbook.ICE_BARRAGE, AncientSpell.ICE_BARRAGE.index,
            new RuneRequirement(6, Rune.WATER),
            new RuneRequirement(4, Rune.DEATH),
            new RuneRequirement(2, Rune.BLOOD)),

    GHORROCK_TELEPORT(
            96, InterfaceID.MagicSpellbook.ZAROSTELEPORT8, AncientSpell.GHORROCK_TELEPORT.index,
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(2, Rune.LAW));


    private static final int BOOK = 1; // Ancient spellbook index

    private final int level;
    private final int interfaceId;
    private final int autoCastWidgetIndex;
    @Getter
    private final RuneRequirement[] requirements;
    private final int spellIndex;

    Ancient(int level, int interfaceId, int spellIndex, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.requirements = requirements;
        this.autoCastWidgetIndex = -1;
        this.spellIndex = spellIndex;
    }

    Ancient(int level, int interfaceId, int spellIndex, int autoCastWidgetIndex, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.requirements = requirements;
        this.autoCastWidgetIndex = autoCastWidgetIndex;
        this.spellIndex = spellIndex;
    }

    @Override
    public boolean canCast()
    {
        return Static.invoke(() -> MagicCast.canCastAncient(spellIndex));
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