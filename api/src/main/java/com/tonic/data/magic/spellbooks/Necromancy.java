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

public enum Necromancy implements Spell
{
    // Teleport spells
    ARCEUUS_HOME_TELEPORT(
                1,
                InterfaceID.MagicSpellbook.TELEPORT_HOME_ARCEUUS
        ),
    ARCEUUS_LIBRARY_TELEPORT(
                6,
                InterfaceID.MagicSpellbook.TELEPORT_ARCEUUS_LIBRARY,
                new RuneRequirement(2, Rune.EARTH),
                new RuneRequirement(1, Rune.LAW)
        ),
    DRAYNOR_MANOR_TELEPORT(
                17,
            InterfaceID.MagicSpellbook.TELEPORT_DRAYNOR_MANOR,
                new RuneRequirement(1, Rune.EARTH),
                new RuneRequirement(1, Rune.WATER),
                new RuneRequirement(1, Rune.LAW)
        ),
    BATTLEFRONT_TELEPORT(
                23,
                InterfaceID.MagicSpellbook.TELEPORT_BATTLEFRONT,
                new RuneRequirement(1, Rune.EARTH),
                new RuneRequirement(1, Rune.FIRE),
                new RuneRequirement(1, Rune.LAW)
        ),
    MIND_ALTAR_TELEPORT(
                28,
                InterfaceID.MagicSpellbook.TELEPORT_MIND_ALTAR,
                new RuneRequirement(2, Rune.MIND),
                new RuneRequirement(1, Rune.LAW)
        ),
    RESPAWN_TELEPORT(
                34,
                InterfaceID.MagicSpellbook.TELEPORT_RESPAWN,
                new RuneRequirement(1, Rune.SOUL),
                new RuneRequirement(1, Rune.LAW)
        ),
    SALVE_GRAVEYARD_TELEPORT(
                40,
                InterfaceID.MagicSpellbook.TELEPORT_SALVE_GRAVEYARD,
                new RuneRequirement(2, Rune.SOUL),
                new RuneRequirement(1, Rune.LAW)
        ),
    FENKENSTRAINS_CASTLE_TELEPORT(
                48,
                InterfaceID.MagicSpellbook.TELEPORT_FENKENSTRAIN_CASTLE,
                new RuneRequirement(1, Rune.EARTH),
                new RuneRequirement(1, Rune.SOUL),
                new RuneRequirement(1, Rune.LAW)
        ),
    WEST_ARDOUGNE_TELEPORT(
                61,
                InterfaceID.MagicSpellbook.TELEPORT_WEST_ARDOUGNE,
                new RuneRequirement(2, Rune.SOUL),
                new RuneRequirement(2, Rune.LAW)
        ),
    HARMONY_ISLAND_TELEPORT(
                65,
                InterfaceID.MagicSpellbook.TELEPORT_HARMONY_ISLAND,
                new RuneRequirement(1, Rune.NATURE),
                new RuneRequirement(1, Rune.SOUL),
                new RuneRequirement(1, Rune.LAW)
        ),
    CEMETERY_TELEPORT(
                71,
                InterfaceID.MagicSpellbook.TELEPORT_CEMETERY,
                new RuneRequirement(1, Rune.BLOOD),
                new RuneRequirement(1, Rune.SOUL),
                new RuneRequirement(1, Rune.LAW)
        ),
    BARROWS_TELEPORT(
                83,
                InterfaceID.MagicSpellbook.TELEPORT_BARROWS,
                new RuneRequirement(1, Rune.BLOOD),
                new RuneRequirement(2, Rune.SOUL),
                new RuneRequirement(2, Rune.LAW)
        ),
    APE_ATOLL_TELEPORT(
                90,
                InterfaceID.MagicSpellbook.TELEPORT_APE_ATOLL_DUNGEON,
                new RuneRequirement(2, Rune.BLOOD),
                new RuneRequirement(2, Rune.SOUL),
                new RuneRequirement(2, Rune.LAW)
        ),

    // Combat spells
    GHOSTLY_GRASP(
                35,
                InterfaceID.MagicSpellbook.GHOSTLY_GRASP,
                new RuneRequirement(4, Rune.AIR),
                new RuneRequirement(1, Rune.CHAOS)
        ),
    SKELETAL_GRASP(
                56,
                InterfaceID.MagicSpellbook.SKELETAL_GRASP,
                new RuneRequirement(8, Rune.EARTH),
                new RuneRequirement(1, Rune.DEATH)
        ),
    UNDEAD_GRASP(
                79,
                InterfaceID.MagicSpellbook.UNDEAD_GRASP,
                new RuneRequirement(12, Rune.FIRE),
                new RuneRequirement(1, Rune.BLOOD)
        ),
    INFERIOR_DEMONBANE(
                44,
                InterfaceID.MagicSpellbook.INFERIOR_DEMONBANE,
                new RuneRequirement(4, Rune.FIRE),
                new RuneRequirement(1, Rune.CHAOS)
        ),
    SUPERIOR_DEMONBANE(
                62,
                InterfaceID.MagicSpellbook.SUPERIOR_DEMONBANE,
                new RuneRequirement(8, Rune.FIRE),
                new RuneRequirement(1, Rune.SOUL)
        ),
    DARK_DEMONBANE(
                82,
                InterfaceID.MagicSpellbook.DARK_DEMONBANE,
                new RuneRequirement(12, Rune.FIRE),
                new RuneRequirement(2, Rune.SOUL)
        ),
    LESSER_CORRUPTION(
                64,
                InterfaceID.MagicSpellbook.LESSER_CORRUPTION,
                new RuneRequirement(1, Rune.DEATH),
                new RuneRequirement(2, Rune.SOUL)
        ),
    GREATER_CORRUPTION(
                85,
                InterfaceID.MagicSpellbook.GREATER_CORRUPTION,
                new RuneRequirement(1, Rune.BLOOD),
                new RuneRequirement(3, Rune.SOUL)
        ),
    RESURRECT_LESSER_GHOST(
                38,
                InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST,
                new RuneRequirement(10, Rune.AIR),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.MIND)
        ),
    RESURRECT_LESSER_SKELETON(
                38,
                InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON,
                new RuneRequirement(10, Rune.AIR),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.MIND)
        ),
    RESURRECT_LESSER_ZOMBIE(
                38,
                InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE,
                new RuneRequirement(10, Rune.AIR),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.MIND)
        ),
    RESURRECT_SUPERIOR_GHOST(
                57,
                InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST,
                new RuneRequirement(10, Rune.EARTH),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.DEATH)
        ),
    RESURRECT_SUPERIOR_SKELETON(
                57,
                InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON,
                new RuneRequirement(10, Rune.EARTH),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.DEATH)
        ),
    RESURRECT_SUPERIOR_ZOMBIE(
                57,
                InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE,
                new RuneRequirement(10, Rune.EARTH),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.DEATH)
        ),
    RESURRECT_GREATER_GHOST(
                76,
                InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST,
                new RuneRequirement(10, Rune.FIRE),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.BLOOD)
        ),
    RESURRECT_GREATER_SKELETON(
                76,
                InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON,
                new RuneRequirement(10, Rune.FIRE),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.BLOOD)
        ),
    RESURRECT_GREATER_ZOMBIE(
                76,
                InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE,
                new RuneRequirement(10, Rune.FIRE),
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(5, Rune.BLOOD)
        ),
    DARK_LURE(
                50,
                InterfaceID.MagicSpellbook.DARK_LURE,
                new RuneRequirement(1, Rune.DEATH),
                new RuneRequirement(1, Rune.NATURE)
        ),
    MARK_OF_DARKNESS(
                59,
                InterfaceID.MagicSpellbook.MARK_OF_DARKNESS,
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(1, Rune.SOUL)
        ),
    WARD_OF_ARCEUUS(
                73,
                InterfaceID.MagicSpellbook.WARD_OF_ARCEUUS,
                new RuneRequirement(1, Rune.COSMIC),
                new RuneRequirement(2, Rune.NATURE),
                new RuneRequirement(4, Rune.SOUL)
        ),

    // Utility spells
    BASIC_REANIMATION(
                16,
                InterfaceID.MagicSpellbook.REANIMATION_BASIC,
                new RuneRequirement(4, Rune.BODY),
                new RuneRequirement(2, Rune.NATURE)
        ),
    ADEPT_REANIMATION(
                41,
                InterfaceID.MagicSpellbook.REANIMATION_ADEPT,
                new RuneRequirement(4, Rune.BODY),
                new RuneRequirement(3, Rune.NATURE),
                new RuneRequirement(1, Rune.SOUL)
        ),
    EXPERT_REANIMATION(
                72,
                InterfaceID.MagicSpellbook.REANIMATION_EXPERT,
                new RuneRequirement(1, Rune.BLOOD),
                new RuneRequirement(3, Rune.NATURE),
                new RuneRequirement(2, Rune.SOUL)
        ),
    MASTER_REANIMATION(
                90,
                InterfaceID.MagicSpellbook.REANIMATION_MASTER,
                new RuneRequirement(2, Rune.BLOOD),
                new RuneRequirement(4, Rune.NATURE),
                new RuneRequirement(4, Rune.SOUL)
        ),
    DEMONIC_OFFERING(
                84,
                InterfaceID.MagicSpellbook.DEMONIC_OFFERING,
                new RuneRequirement(1, Rune.SOUL),
                new RuneRequirement(1, Rune.WRATH)
        ),
    SINISTER_OFFERING(
                92,
                InterfaceID.MagicSpellbook.SINISTER_OFFERING,
                new RuneRequirement(1, Rune.BLOOD),
                new RuneRequirement(1, Rune.WRATH)
        ),
    SHADOW_VEIL(
                47,
                InterfaceID.MagicSpellbook.SHADOW_VEIL,
                new RuneRequirement(5, Rune.EARTH),
                new RuneRequirement(5, Rune.FIRE),
                new RuneRequirement(5, Rune.COSMIC)
        ),
    VILE_VIGOUR(
                66,
                InterfaceID.MagicSpellbook.VILE_VIGOUR,
                new RuneRequirement(3, Rune.AIR),
                new RuneRequirement(1, Rune.SOUL)
        ),
    DEGRIME(
                70,
                InterfaceID.MagicSpellbook.DEGRIME,
                new RuneRequirement(4, Rune.EARTH),
                new RuneRequirement(2, Rune.NATURE)
        ),
    RESURRECT_CROPS(
                78,
                InterfaceID.MagicSpellbook.RESURRECT_CROPS,
                new RuneRequirement(25, Rune.EARTH),
                new RuneRequirement(8, Rune.BLOOD),
                new RuneRequirement(12, Rune.NATURE),
                new RuneRequirement(8, Rune.SOUL)
        ),
    DEATH_CHARGE(
                80,
                InterfaceID.MagicSpellbook.DEATH_CHARGE,
                new RuneRequirement(1, Rune.BLOOD),
                new RuneRequirement(1, Rune.DEATH),
                new RuneRequirement(1, Rune.SOUL)
        ),
    ;

    private final int level;
    private final int interfaceId;
    private final int autoCastWidgetIndex;
    @Getter
    private final RuneRequirement[] requirements;

    Necromancy(int level, int interfaceId, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.requirements = requirements;
        this.autoCastWidgetIndex = -1;
    }

    Necromancy(int level, int interfaceId, int autoCastWidgetIndex, RuneRequirement... requirements)
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
        if (getCurrent() != NECROMANCY)
        {
            return false;
        }

            if (!WorldsAPI.inMembersWorld())
            {
                return false;
            }

        if (this == ARCEUUS_HOME_TELEPORT)
        {
            return MagicAPI.isHomeTeleportOnCooldown();
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
