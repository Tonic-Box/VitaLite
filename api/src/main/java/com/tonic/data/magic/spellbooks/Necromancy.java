package com.tonic.data.magic.spellbooks;

import com.tonic.Static;
import com.tonic.data.magic.MagicCast;
import com.tonic.data.magic.Rune;
import com.tonic.data.magic.RuneRequirement;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.spellbooks.SpellbookEnums.ArceuusSpell;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

public enum Necromancy implements Spell
{
    ARCEUUS_HOME_TELEPORT(
            1, InterfaceID.MagicSpellbook.TELEPORT_HOME_ARCEUUS, ArceuusSpell.ARCEUUS_HOME_TELEPORT.index),

    BASIC_REANIMATION(
            16, InterfaceID.MagicSpellbook.REANIMATION_BASIC, ArceuusSpell.BASIC_REANIMATION.index,
            new RuneRequirement(4, Rune.BODY),
            new RuneRequirement(2, Rune.NATURE)),

    ADEPT_REANIMATION(
            41, InterfaceID.MagicSpellbook.REANIMATION_ADEPT, ArceuusSpell.ADEPT_REANIMATION.index,
            new RuneRequirement(4, Rune.BODY),
            new RuneRequirement(3, Rune.NATURE),
            new RuneRequirement(1, Rune.SOUL)),

    EXPERT_REANIMATION(
            72, InterfaceID.MagicSpellbook.REANIMATION_EXPERT, ArceuusSpell.EXPERT_REANIMATION.index,
            new RuneRequirement(1, Rune.BLOOD),
            new RuneRequirement(3, Rune.NATURE),
            new RuneRequirement(2, Rune.SOUL)),

    MASTER_REANIMATION(
            90, InterfaceID.MagicSpellbook.REANIMATION_MASTER, ArceuusSpell.MASTER_REANIMATION.index,
            new RuneRequirement(2, Rune.BLOOD),
            new RuneRequirement(4, Rune.NATURE),
            new RuneRequirement(4, Rune.SOUL)),

    ARCEUUS_LIBRARY_TELEPORT(
            6, InterfaceID.MagicSpellbook.TELEPORT_ARCEUUS_LIBRARY, ArceuusSpell.ARCEUUS_LIBRARY_TELEPORT.index,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(1, Rune.LAW)),

    DRAYNOR_MANOR_TELEPORT(
            17, InterfaceID.MagicSpellbook.TELEPORT_DRAYNOR_MANOR, ArceuusSpell.DRAYNOR_MANOR_TELEPORT.index,
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(1, Rune.LAW)),

    BATTLEFRONT_TELEPORT(
            23, InterfaceID.MagicSpellbook.TELEPORT_BATTLEFRONT, ArceuusSpell.BATTLEFRONT_TELEPORT.index,
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(1, Rune.LAW)),

    MIND_ALTAR_TELEPORT(
            28, InterfaceID.MagicSpellbook.TELEPORT_MIND_ALTAR, ArceuusSpell.MIND_ALTAR_TELEPORT.index,
            new RuneRequirement(2, Rune.MIND),
            new RuneRequirement(1, Rune.LAW)),

    RESPAWN_TELEPORT(
            34, InterfaceID.MagicSpellbook.TELEPORT_RESPAWN, ArceuusSpell.RESPAWN_TELEPORT.index,
            new RuneRequirement(1, Rune.SOUL),
            new RuneRequirement(1, Rune.LAW)),

    SALVE_GRAVEYARD_TELEPORT(
            40, InterfaceID.MagicSpellbook.TELEPORT_SALVE_GRAVEYARD, ArceuusSpell.SALVE_GRAVEYARD_TELEPORT.index,
            new RuneRequirement(2, Rune.SOUL),
            new RuneRequirement(1, Rune.LAW)),

    FENKENSTRAINS_CASTLE_TELEPORT(
            48, InterfaceID.MagicSpellbook.TELEPORT_FENKENSTRAIN_CASTLE, ArceuusSpell.FENKENSTRAINS_CASTLE_TELEPORT.index,
            new RuneRequirement(1, Rune.EARTH),
            new RuneRequirement(1, Rune.SOUL),
            new RuneRequirement(1, Rune.LAW)),

    WEST_ARDOUGNE_TELEPORT(
            61, InterfaceID.MagicSpellbook.TELEPORT_WEST_ARDOUGNE, ArceuusSpell.WEST_ARDOUGNE_TELEPORT.index,
            new RuneRequirement(2, Rune.SOUL),
            new RuneRequirement(2, Rune.LAW)),

    HARMONY_ISLAND_TELEPORT(
            65, InterfaceID.MagicSpellbook.TELEPORT_HARMONY_ISLAND, ArceuusSpell.HARMONY_ISLAND_TELEPORT.index,
            new RuneRequirement(1, Rune.NATURE),
            new RuneRequirement(1, Rune.SOUL),
            new RuneRequirement(1, Rune.LAW)),

    CEMETERY_TELEPORT(
            71, InterfaceID.MagicSpellbook.TELEPORT_CEMETERY, ArceuusSpell.CEMETERY_TELEPORT.index,
            new RuneRequirement(1, Rune.BLOOD),
            new RuneRequirement(1, Rune.SOUL),
            new RuneRequirement(1, Rune.LAW)),

    BARROWS_TELEPORT(
            83, InterfaceID.MagicSpellbook.TELEPORT_BARROWS, ArceuusSpell.BARROWS_TELEPORT.index,
            new RuneRequirement(1, Rune.BLOOD),
            new RuneRequirement(2, Rune.SOUL),
            new RuneRequirement(2, Rune.LAW)),

    APE_ATOLL_TELEPORT(
            90, InterfaceID.MagicSpellbook.TELEPORT_APE_ATOLL_DUNGEON, ArceuusSpell.APE_ATOLL_TELEPORT.index,
            new RuneRequirement(2, Rune.BLOOD),
            new RuneRequirement(2, Rune.SOUL),
            new RuneRequirement(2, Rune.LAW)),

    GHOSTLY_GRASP(
            35, InterfaceID.MagicSpellbook.GHOSTLY_GRASP, ArceuusSpell.GHOSTLY_GRASP.index,
            new RuneRequirement(4, Rune.AIR),
            new RuneRequirement(1, Rune.CHAOS)),

    SKELETAL_GRASP(
            56, InterfaceID.MagicSpellbook.SKELETAL_GRASP, ArceuusSpell.SKELETAL_GRASP.index,
            new RuneRequirement(8, Rune.EARTH),
            new RuneRequirement(1, Rune.DEATH)),

    UNDEAD_GRASP(
            79, InterfaceID.MagicSpellbook.UNDEAD_GRASP, ArceuusSpell.UNDEAD_GRASP.index,
            new RuneRequirement(12, Rune.FIRE),
            new RuneRequirement(1, Rune.BLOOD)),

    INFERIOR_DEMONBANE(
            44, InterfaceID.MagicSpellbook.INFERIOR_DEMONBANE, ArceuusSpell.INFERIOR_DEMONBANE.index,
            new RuneRequirement(4, Rune.FIRE),
            new RuneRequirement(1, Rune.CHAOS)),

    SUPERIOR_DEMONBANE(
            62, InterfaceID.MagicSpellbook.SUPERIOR_DEMONBANE, ArceuusSpell.SUPERIOR_DEMONBANE.index,
            new RuneRequirement(8, Rune.FIRE),
            new RuneRequirement(1, Rune.SOUL)),

    DARK_DEMONBANE(
            82, InterfaceID.MagicSpellbook.DARK_DEMONBANE, ArceuusSpell.DARK_DEMONBANE.index,
            new RuneRequirement(12, Rune.FIRE),
            new RuneRequirement(2, Rune.SOUL)),

    LESSER_CORRUPTION(
            64, InterfaceID.MagicSpellbook.LESSER_CORRUPTION, ArceuusSpell.LESSER_CORRUPTION.index,
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(2, Rune.SOUL)),

    GREATER_CORRUPTION(
            85, InterfaceID.MagicSpellbook.GREATER_CORRUPTION, ArceuusSpell.GREATER_CORRUPTION.index,
            new RuneRequirement(1, Rune.BLOOD),
            new RuneRequirement(3, Rune.SOUL)),

    RESURRECT_LESSER_GHOST(
            38, InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST, ArceuusSpell.RESURRECT_LESSER_GHOST.index,
            new RuneRequirement(10, Rune.AIR),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.MIND)),

    RESURRECT_LESSER_SKELETON(
            38, InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON, ArceuusSpell.RESURRECT_LESSER_SKELETON.index,
            new RuneRequirement(10, Rune.AIR),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.MIND)),

    RESURRECT_LESSER_ZOMBIE(
            38, InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE, ArceuusSpell.RESURRECT_LESSER_ZOMBIE.index,
            new RuneRequirement(10, Rune.AIR),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.MIND)),

    RESURRECT_SUPERIOR_GHOST(
            57, InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST, ArceuusSpell.RESURRECT_SUPERIOR_GHOST.index,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.DEATH)),

    RESURRECT_SUPERIOR_SKELETON(
            57, InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON, ArceuusSpell.RESURRECT_SUPERIOR_SKELETON.index,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.DEATH)),

    RESURRECT_SUPERIOR_ZOMBIE(
            57, InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE, ArceuusSpell.RESURRECT_SUPERIOR_ZOMBIE.index,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.DEATH)),

    RESURRECT_GREATER_GHOST(
            76, InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST, ArceuusSpell.RESURRECT_GREATER_GHOST.index,
            new RuneRequirement(10, Rune.FIRE),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.BLOOD)),

    RESURRECT_GREATER_SKELETON(
            76, InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON, ArceuusSpell.RESURRECT_GREATER_SKELETON.index,
            new RuneRequirement(10, Rune.FIRE),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.BLOOD)),

    RESURRECT_GREATER_ZOMBIE(
            76, InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE, ArceuusSpell.RESURRECT_GREATER_ZOMBIE.index,
            new RuneRequirement(10, Rune.FIRE),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(5, Rune.BLOOD)),

    DARK_LURE(
            50, InterfaceID.MagicSpellbook.DARK_LURE, ArceuusSpell.DARK_LURE.index,
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.NATURE)),

    MARK_OF_DARKNESS(
            59, InterfaceID.MagicSpellbook.MARK_OF_DARKNESS, ArceuusSpell.MARK_OF_DARKNESS.index,
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(1, Rune.SOUL)),

    WARD_OF_ARCEUUS(
            73, InterfaceID.MagicSpellbook.WARD_OF_ARCEUUS, ArceuusSpell.WARD_OF_ARCEUUS.index,
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(2, Rune.NATURE),
            new RuneRequirement(4, Rune.SOUL)),

    DEMONIC_OFFERING(
            84, InterfaceID.MagicSpellbook.DEMONIC_OFFERING, ArceuusSpell.DEMONIC_OFFERING.index,
            new RuneRequirement(1, Rune.SOUL),
            new RuneRequirement(1, Rune.WRATH)),

    SINISTER_OFFERING(
            92, InterfaceID.MagicSpellbook.SINISTER_OFFERING, ArceuusSpell.SINISTER_OFFERING.index,
            new RuneRequirement(1, Rune.BLOOD),
            new RuneRequirement(1, Rune.WRATH)),

    SHADOW_VEIL(
            47, InterfaceID.MagicSpellbook.SHADOW_VEIL, ArceuusSpell.SHADOW_VEIL.index,
            new RuneRequirement(5, Rune.EARTH),
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(5, Rune.COSMIC)),

    VILE_VIGOUR(
            66, InterfaceID.MagicSpellbook.VILE_VIGOUR, ArceuusSpell.VILE_VIGOUR.index,
            new RuneRequirement(3, Rune.AIR),
            new RuneRequirement(1, Rune.SOUL)),

    DEGRIME(
            70, InterfaceID.MagicSpellbook.DEGRIME, ArceuusSpell.DEGRIME.index,
            new RuneRequirement(4, Rune.EARTH),
            new RuneRequirement(2, Rune.NATURE)),

    RESURRECT_CROPS(
            78, InterfaceID.MagicSpellbook.RESURRECT_CROPS, ArceuusSpell.RESURRECT_CROPS.index,
            new RuneRequirement(25, Rune.EARTH),
            new RuneRequirement(8, Rune.BLOOD),
            new RuneRequirement(12, Rune.NATURE),
            new RuneRequirement(8, Rune.SOUL)),

    DEATH_CHARGE(
            80, InterfaceID.MagicSpellbook.DEATH_CHARGE, ArceuusSpell.DEATH_CHARGE.index,
            new RuneRequirement(1, Rune.BLOOD),
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.SOUL));

    private static final int BOOK = 3; // Arceuus spellbook index

    private final int level;
    private final int interfaceId;
    private final int autoCastWidgetIndex;
    @Getter
    private final RuneRequirement[] requirements;
    private final int spellIndex;

    Necromancy(int level, int interfaceId, int spellIndex, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.requirements = requirements;
        this.autoCastWidgetIndex = -1;
        this.spellIndex = spellIndex;
    }

    Necromancy(int level, int interfaceId, int spellIndex, int autoCastWidgetIndex, RuneRequirement... requirements)
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
        return Static.invoke(() -> MagicCast.canCastNecromancy(spellIndex));
    }

    @Override
    public int getAction()
    {
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
    public int getAutoCastIndex() { return autoCastWidgetIndex; }

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
