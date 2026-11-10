package com.tonic.data.magic;

import com.tonic.Static;
import com.tonic.api.game.ClientScriptAPI;
import net.runelite.api.Client;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ParamID;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

public final class MagicCast
{
    private static final int SCRIPT_MAGIC_HAS_RUNES = 2620;
    private static final int SCRIPT_SPELL_CASTABLE = 2614;
    private static final int SCRIPT_SPELL_UNLOCKED = 4131;
    private static final int SCRIPT_HOME_TELEPORT_UNTIMED = 9712;
    private static final int SCRIPT_RUNE_COUNT = 19;
    private static final int PARAM_SPELLBOOK = 336;
    private static final int ANY_SPELLBOOK = -1;
    private static final int ICON_SIZE = 24;
    private static final int HOME_TELEPORT_MINUTES = 30;
    private static final int MINIGAME_TELEPORT_MINUTES = 20;

    public static boolean canCastBySpellIndex(int spellbookIndex, int spellIndex)
    {
        Client c = Static.getClient();

        int spell = spellAt(c, spellbookIndex, spellIndex);
        if (spell == -1)
        {
            return false;
        }

        ItemComposition definition = c.getItemDefinition(spell);
        int book = definition.getIntValue(PARAM_SPELLBOOK);
        if (book != ANY_SPELLBOOK && book != c.getVarbitValue(VarbitID.SPELLBOOK))
        {
            return false;
        }

        int component = definition.getIntValue(ParamID.SPELL_BUTTON);
        if (coolingDown(c, component))
        {
            return false;
        }

        if (book == ANY_SPELLBOOK && run(c, SCRIPT_SPELL_UNLOCKED, spell) != 1)
        {
            return false;
        }

        if (c.getWidget(component) == null)
        {
            return c.getBoostedSkillLevel(Skill.MAGIC) >= definition.getIntValue(ParamID.SPELL_LEVELREQ)
                    && run(c, SCRIPT_SPELL_UNLOCKED, spell) == 1
                    && hasRunes(c, spell);
        }

        ClientScriptAPI.runScript(SCRIPT_SPELL_CASTABLE, spell, component, ICON_SIZE);
        return c.getIntStack()[c.getIntStackSize() - 2] == 1;
    }

    public static boolean hasRunesBySpellIndex(int spellbookIndex, int spellIndex)
    {
        Client c = Static.getClient();
        int spell = spellAt(c, spellbookIndex, spellIndex);
        return spell != -1 && hasRunes(c, spell);
    }

    private static int spellAt(Client c, int spellbookIndex, int spellIndex)
    {
        return c.getEnum(c.getEnum(EnumID.SPELLBOOKS).getIntValue(spellbookIndex)).getIntValue(spellIndex);
    }

    public static int runeCount(int runeId)
    {
        Client c = Static.getClient();
        return run(c, SCRIPT_RUNE_COUNT, runeId, worn(c, EquipmentInventorySlot.WEAPON), worn(c, EquipmentInventorySlot.SHIELD));
    }

    private static boolean hasRunes(Client c, int spell)
    {
        return run(c, SCRIPT_MAGIC_HAS_RUNES, spell, worn(c, EquipmentInventorySlot.WEAPON), worn(c, EquipmentInventorySlot.SHIELD)) == 1;
    }

    private static boolean coolingDown(Client c, int component)
    {
        switch (component)
        {
            case InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD:
            case InterfaceID.MagicSpellbook.TELEPORT_HOME_ZAROS:
            case InterfaceID.MagicSpellbook.TELEPORT_HOME_LUNAR:
            case InterfaceID.MagicSpellbook.TELEPORT_HOME_ARCEUUS:
            case InterfaceID.MagicSpellbook.LEAGUE_HOME_TELEPORT:
                return minutesSince(c, VarPlayerID.AIDE_TELE_TIMER) < HOME_TELEPORT_MINUTES && run(c, SCRIPT_HOME_TELEPORT_UNTIMED) == 0;
            case InterfaceID.MagicSpellbook.TELEPORT_MINIGAME_STANDARD:
            case InterfaceID.MagicSpellbook.TELEPORT_MINIGAME_ANCIENT:
            case InterfaceID.MagicSpellbook.TELEPORT_MINIGAME_LUNAR:
            case InterfaceID.MagicSpellbook.TELEPORT_MINIGAME_ARCEUUS:
                return minutesSince(c, VarPlayerID.SLUG2_REGIONUID) < MINIGAME_TELEPORT_MINUTES;
            case InterfaceID.MagicSpellbook.VENGEANCE:
            case InterfaceID.MagicSpellbook.VENGEANCE_OTHER:
                return c.getVarbitValue(VarbitID.VENGEANCE_TIMELIMIT) == 1;
            case InterfaceID.MagicSpellbook.HEAL_GROUP:
                return c.getVarbitValue(VarbitID.HEAL_GROUP_TIMELIMIT) == 1;
            case InterfaceID.MagicSpellbook.MAGIC_IMBUE:
                return c.getVarbitValue(VarbitID.MAGIC_IMBUE_ACTIVE) > 0;
            case InterfaceID.MagicSpellbook.LESSER_CORRUPTION:
            case InterfaceID.MagicSpellbook.GREATER_CORRUPTION:
                return c.getVarbitValue(VarbitID.ARCEUUS_CORRUPTION_COOLDOWN) == 1;
            case InterfaceID.MagicSpellbook.DARK_LURE:
                return c.getVarbitValue(VarbitID.ARCEUUS_DARK_LURE_COOLDOWN) == 1;
            case InterfaceID.MagicSpellbook.DEATH_CHARGE:
                return c.getVarbitValue(VarbitID.ARCEUUS_DEATH_CHARGE_COOLDOWN) == 1;
            case InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST:
            case InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON:
            case InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE:
            case InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST:
            case InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON:
            case InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE:
            case InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST:
            case InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON:
            case InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE:
                return c.getVarbitValue(VarbitID.ARCEUUS_RESURRECTION_COOLDOWN) == 1;
            case InterfaceID.MagicSpellbook.SHADOW_VEIL:
                return c.getVarbitValue(VarbitID.ARCEUUS_SHADOW_VEIL_COOLDOWN) == 1;
            case InterfaceID.MagicSpellbook.VILE_VIGOUR:
                return c.getVarbitValue(VarbitID.ARCEUUS_VILE_VIGOUR_COOLDOWN) == 1;
            case InterfaceID.MagicSpellbook.WARD_OF_ARCEUUS:
                return c.getVarbitValue(VarbitID.ARCEUUS_WARD_COOLDOWN) == 1;
            case InterfaceID.MagicSpellbook.DEMONIC_OFFERING:
            case InterfaceID.MagicSpellbook.SINISTER_OFFERING:
                return c.getVarbitValue(VarbitID.ARCEUUS_OFFERING_COOLDOWN) == 1;
            default:
                return false;
        }
    }

    private static int minutesSince(Client c, int varp)
    {
        return c.getVarpValue(VarPlayerID.DATE_MINUTES) - c.getVarpValue(varp);
    }

    private static int run(Client c, Object... script)
    {
        ClientScriptAPI.runScript(script);
        return c.getIntStack()[c.getIntStackSize() - 1];
    }

    private static int worn(Client c, EquipmentInventorySlot slot)
    {
        ItemContainer eq = c.getItemContainer(InventoryID.WORN);
        Item item = eq == null ? null : eq.getItem(slot.getSlotIdx());
        return item == null ? -1 : item.getId();
    }

    public static boolean canCastStandard(int spellIndex)
    {
        return canCastBySpellIndex(SpellBook.STANDARD.getIndex(), spellIndex);
    }
    public static boolean canCastAncient(int spellIndex)
    {
        return canCastBySpellIndex(SpellBook.ANCIENT.getIndex(), spellIndex);
    }
    public static boolean canCastLunar(int spellIndex)
    {
        return canCastBySpellIndex(SpellBook.LUNAR.getIndex(), spellIndex);
    }
    public static boolean canCastNecromancy(int spellIndex)
    {
        return canCastBySpellIndex(SpellBook.NECROMANCY.getIndex(), spellIndex);
    }
}
