package com.tonic.data.magic;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.InventoryID;

public final class MagicCast
{
    private static final int SCRIPT_MAGIC_HAS_RUNES = 2620;
    private static final int SPELLBOOK_ENUM = 1981;
    private static final int[] SPELLBOOKS = {1982, 1983, 1984, 1985};

    public static boolean canCastBySpellIndex(int spellbookIndex, int spellIndex)
    {
        Client c = Static.getClient();

        EnumComposition spellsEnum = c.getEnum(SPELLBOOKS[spellbookIndex]);
        int spellItemId = spellsEnum.getIntValue(spellIndex);

        ItemContainer eq = c.getItemContainer(InventoryID.WORN);
        int weapon;
        int shield;

        if (eq != null)
        {
            Item w = eq.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
            Item s = eq.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
            if (w != null) weapon = w.getId();
            else {
                weapon = -1;
            }
            if (s != null) shield = s.getId();
            else {
                shield = -1;
            }
        } else {
            shield = -1;
            weapon = -1;
        }
        c.runScript(SCRIPT_MAGIC_HAS_RUNES, spellItemId, weapon, shield);

        int[] stack = c.getIntStack();
        int size = c.getIntStackSize();
        return stack[size - 1] == 1;
    }


    public static boolean canCastStandard(int spellIndex)
    {
        return canCastBySpellIndex(0, spellIndex);
    }
    public static boolean canCastAncient(int spellIndex)
    {
        return canCastBySpellIndex(1, spellIndex);
    }
    public static boolean canCastLunar(int spellIndex)
    {
        return canCastBySpellIndex(2, spellIndex);
    }
    public static boolean canCastNecromancy(int spellIndex)
    {
        return canCastBySpellIndex(3, spellIndex);
    }
}
