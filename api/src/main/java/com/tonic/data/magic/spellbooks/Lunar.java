package com.tonic.data.magic.spellbooks;

import com.tonic.Static;
import com.tonic.api.game.QuestAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.MagicAPI;
import com.tonic.data.magic.MagicCast;
import com.tonic.data.magic.Rune;
import com.tonic.data.magic.RuneRequirement;
import com.tonic.data.magic.Spell;
import com.tonic.data.magic.spellbooks.SpellbookEnums.LunarSpell;
import lombok.Getter;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

import static com.tonic.data.magic.SpellBook.LUNAR;

public enum Lunar implements Spell
{
    LUNAR_HOME_TELEPORT(
            0, InterfaceID.MagicSpellbook.TELEPORT_HOME_LUNAR, LunarSpell.LUNAR_HOME_TELEPORT.index),

    BAKE_PIE(
            66, InterfaceID.MagicSpellbook.BAKE_PIE, LunarSpell.BAKE_PIE.index,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(1, Rune.ASTRAL)),

    CURE_PLANT(
            66, InterfaceID.MagicSpellbook.CURE_PLANT, LunarSpell.CURE_PLANT.index,
            new RuneRequirement(8, Rune.EARTH),
            new RuneRequirement(1, Rune.ASTRAL)),

    NPC_CONTACT(
            66, InterfaceID.MagicSpellbook.NPC_CONTACT, LunarSpell.NPC_CONTACT.index,
            new RuneRequirement(2, Rune.AIR),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(1, Rune.ASTRAL)),

    CURE_OTHER(
            66, InterfaceID.MagicSpellbook.CURE_OTHER, LunarSpell.CURE_OTHER.index,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(1, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    MOONCLAN_TELEPORT(
            69, InterfaceID.MagicSpellbook.TELE_MOONCLAN, LunarSpell.MOONCLAN_TELEPORT.index,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    TELE_GROUP_MOONCLAN(
            70, InterfaceID.MagicSpellbook.TELE_GROUP_MOONCLAN, LunarSpell.TELE_GROUP_MOONCLAN.index,
            new RuneRequirement(4, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    CURE_ME(
            66, InterfaceID.MagicSpellbook.CURE_ME, LunarSpell.CURE_ME.index,
            new RuneRequirement(2, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    WATERBIRTH_TELEPORT(
            72, InterfaceID.MagicSpellbook.TELE_WATERBIRTH, LunarSpell.WATERBIRTH_TELEPORT.index,
            new RuneRequirement(1, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    TELE_GROUP_WATERBIRTH(
            73, InterfaceID.MagicSpellbook.TELE_GROUP_WATERBIRTH, LunarSpell.TELE_GROUP_WATERBIRTH.index,
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    CURE_GROUP(
            66, InterfaceID.MagicSpellbook.CURE_GROUP, LunarSpell.CURE_GROUP.index,
            new RuneRequirement(2, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)),

    STAT_SPY(
            66, InterfaceID.MagicSpellbook.STAT_SPY, LunarSpell.STAT_SPY.index,
            new RuneRequirement(5, Rune.BODY),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL)),

    BARBARIAN_TELEPORT(
            75, InterfaceID.MagicSpellbook.TELE_BARB_OUT, LunarSpell.BARBARIAN_TELEPORT.index,
            new RuneRequirement(3, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)),

    TELE_GROUP_BARBARIAN(
            76, InterfaceID.MagicSpellbook.TELE_GROUP_BARBARIAN, LunarSpell.TELE_GROUP_BARBARIAN.index,
            new RuneRequirement(6, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)),

    SPIN_FLAX(
            66, InterfaceID.MagicSpellbook.SPIN_FLAX, LunarSpell.SPIN_FLAX.index,
            new RuneRequirement(5, Rune.AIR),
            new RuneRequirement(1, Rune.ASTRAL),
            new RuneRequirement(2, Rune.NATURE)),

    SUPERGLASS_MAKE(
            66, InterfaceID.MagicSpellbook.SUPERGLASS, LunarSpell.SUPERGLASS_MAKE.index,
            new RuneRequirement(10, Rune.AIR),
            new RuneRequirement(6, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL)),

    TAN_LEATHER(
            66, InterfaceID.MagicSpellbook.TAN_LEATHER, LunarSpell.TAN_LEATHER.index,
            new RuneRequirement(5, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.NATURE)),

    KHAZARD_TELEPORT(
            78, InterfaceID.MagicSpellbook.TELE_KHAZARD, LunarSpell.KHAZARD_TELEPORT.index,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)),

    TELE_GROUP_KHAZARD(
            79, InterfaceID.MagicSpellbook.TELE_GROUP_KHAZARD, LunarSpell.TELE_GROUP_KHAZARD.index,
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(2, Rune.LAW)),

    STRING_JEWELLERY(
            66, InterfaceID.MagicSpellbook.STRING_JEWEL, LunarSpell.STRING_JEWELLERY.index,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(5, Rune.WATER),
            new RuneRequirement(2, Rune.ASTRAL)),

    STAT_RESTORE_POT_SHARE(
            66, InterfaceID.MagicSpellbook.REST_POT_SHARE, LunarSpell.STAT_RESTORE_POT_SHARE.index,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL)),

    MAGIC_IMBUE(
            66, InterfaceID.MagicSpellbook.MAGIC_IMBUE, LunarSpell.MAGIC_IMBUE.index,
            new RuneRequirement(7, Rune.WATER),
            new RuneRequirement(7, Rune.FIRE),
            new RuneRequirement(2, Rune.ASTRAL)),

    FERTILE_SOIL(
            66, InterfaceID.MagicSpellbook.FERTILE_SOIL, LunarSpell.FERTILE_SOIL.index,
            new RuneRequirement(15, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(2, Rune.NATURE)),

    BOOST_POTION_SHARE(
            66, InterfaceID.MagicSpellbook.STREN_POT_SHARE, LunarSpell.BOOST_POTION_SHARE.index,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(12, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL)),

    FISHING_GUILD_TELEPORT(
            85, InterfaceID.MagicSpellbook.TELE_FISH, LunarSpell.FISHING_GUILD_TELEPORT.index,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)),

    TELE_GROUP_FISHING_GUILD(
            86, InterfaceID.MagicSpellbook.TELE_GROUP_FISHING_GUILD, LunarSpell.TELE_GROUP_FISHING_GUILD.index,
            new RuneRequirement(14, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)),

    CATHERBY_TELEPORT(
            87, InterfaceID.MagicSpellbook.TELE_CATHER, LunarSpell.CATHERBY_TELEPORT.index,
            new RuneRequirement(10, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)),

    TELE_GROUP_CATHERBY(
            88, InterfaceID.MagicSpellbook.TELE_GROUP_CATHERBY, LunarSpell.TELE_GROUP_CATHERBY.index,
            new RuneRequirement(15, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)),

    RECHARGE_DRAGONSTONE(
            66, InterfaceID.MagicSpellbook.RECHARGE_DRAGONSTONE, LunarSpell.RECHARGE_DRAGONSTONE.index,
            new RuneRequirement(4, Rune.WATER),
            new RuneRequirement(1, Rune.ASTRAL),
            new RuneRequirement(1, Rune.SOUL)),

    ICE_PLATEAU_TELEPORT(
            89, InterfaceID.MagicSpellbook.TELE_GHORROCK, LunarSpell.ICE_PLATEAU_TELEPORT.index,
            new RuneRequirement(8, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)),

    TELE_GROUP_ICE_PLATEAU(
            90, InterfaceID.MagicSpellbook.TELE_GROUP_GHORROCK, LunarSpell.TELE_GROUP_ICE_PLATEAU.index,
            new RuneRequirement(16, Rune.WATER),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW)),

    ENERGY_TRANSFER(
            66, InterfaceID.MagicSpellbook.ENERGY_TRANS, LunarSpell.ENERGY_TRANSFER.index,
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(1, Rune.NATURE),
            new RuneRequirement(2, Rune.LAW)),

    HEAL_OTHER(
            66, InterfaceID.MagicSpellbook.HEAL_OTHER, LunarSpell.HEAL_OTHER.index,
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.LAW),
            new RuneRequirement(1, Rune.BLOOD)),

    VENGEANCE_OTHER(
            66, InterfaceID.MagicSpellbook.VENGEANCE_OTHER, LunarSpell.VENGEANCE_OTHER.index,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(2, Rune.DEATH)),

    VENGEANCE(
            66, InterfaceID.MagicSpellbook.VENGEANCE, LunarSpell.VENGEANCE.index,
            new RuneRequirement(10, Rune.EARTH),
            new RuneRequirement(4, Rune.ASTRAL),
            new RuneRequirement(2, Rune.DEATH)),

    HEAL_GROUP(
            66, InterfaceID.MagicSpellbook.HEAL_GROUP, LunarSpell.HEAL_GROUP.index,
            new RuneRequirement(4, Rune.ASTRAL),
            new RuneRequirement(6, Rune.LAW),
            new RuneRequirement(3, Rune.BLOOD)),

    MONSTER_EXAMINE(
            66, InterfaceID.MagicSpellbook.MONSTER_EXAMINE, LunarSpell.MONSTER_EXAMINE.index,
            new RuneRequirement(1, Rune.MIND),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(1, Rune.ASTRAL)),

    HUMIDIFY(
            66, InterfaceID.MagicSpellbook.HUMIDIFY, LunarSpell.HUMIDIFY.index,
            new RuneRequirement(3, Rune.WATER),
            new RuneRequirement(1, Rune.FIRE),
            new RuneRequirement(1, Rune.ASTRAL)),

    OURANIA_TELEPORT(
            71, InterfaceID.MagicSpellbook.OURANIA_TELEPORT, LunarSpell.OURANIA_TELEPORT.index,
            new RuneRequirement(6, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    HUNTER_KIT(
            66, InterfaceID.MagicSpellbook.HUNTER_KIT, LunarSpell.HUNTER_KIT.index,
            new RuneRequirement(2, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL)),

    DREAM(
            66, InterfaceID.MagicSpellbook.DREAM, LunarSpell.DREAM.index,
            new RuneRequirement(5, Rune.BODY),
            new RuneRequirement(1, Rune.COSMIC),
            new RuneRequirement(2, Rune.ASTRAL)),

    PLANK_MAKE(
            66, InterfaceID.MagicSpellbook.PLANK_MAKE, LunarSpell.PLANK_MAKE.index,
            new RuneRequirement(15, Rune.EARTH),
            new RuneRequirement(2, Rune.ASTRAL),
            new RuneRequirement(1, Rune.NATURE)),

    SPELLBOOK_SWAP(
            66, InterfaceID.MagicSpellbook.SPELLBOOK_SWAP, LunarSpell.SPELLBOOK_SWAP.index,
            new RuneRequirement(2, Rune.COSMIC),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(1, Rune.LAW)),

    GEOMANCY(
            66, InterfaceID.MagicSpellbook.GEOMANCY, LunarSpell.GEOMANCY.index,
            new RuneRequirement(8, Rune.EARTH),
            new RuneRequirement(3, Rune.ASTRAL),
            new RuneRequirement(3, Rune.NATURE)),

    TELEPORT_TO_TARGET(
            85, InterfaceID.MagicSpellbook.BOUNTY_TARGET, LunarSpell.TELEPORT_TO_TARGET.index,
            new RuneRequirement(1, Rune.CHAOS),
            new RuneRequirement(1, Rune.DEATH),
            new RuneRequirement(1, Rune.LAW));

    private static final int BOOK = 2; // Lunar spellbook index

    private final int level;
    private final int interfaceId;
    private final int autoCastWidgetIndex;
    @Getter
    private final RuneRequirement[] requirements;
    private final int spellIndex;

    Lunar(int level, int interfaceId, int spellIndex, RuneRequirement... requirements)
    {
        this.level = level;
        this.interfaceId = interfaceId;
        this.requirements = requirements;
        this.autoCastWidgetIndex = -1;
        this.spellIndex = spellIndex;
    }

    Lunar(int level, int interfaceId, int spellIndex, int autoCastWidgetIndex, RuneRequirement... requirements)
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
        return Static.invoke(() -> MagicCast.canCastLunar(spellIndex));
    }

    @Override
    public int getAction() {
        return 1;
    }

    @Override
    public int getLevel() { return level; }

    @Override
    public int getWidget() { return interfaceId; }

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
