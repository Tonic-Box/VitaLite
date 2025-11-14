package com.tonic.data.magic;

import com.tonic.api.widgets.EquipmentAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.data.EquipmentSlot;
import com.tonic.data.wrappers.ItemEx;
import net.runelite.api.gameval.ItemID;

public enum Rune
{
    AIR(ItemID.AIRRUNE, "Air", "Smoke", "Mist", "Dust"),
    EARTH(ItemID.EARTHRUNE, "Earth", "Lava", "Mud", "Dust"),
    FIRE(ItemID.FIRERUNE, "Fire", "Lava", "Smoke", "Steam"),
    WATER(ItemID.WATERRUNE, "Water", "Mud", "Steam", "Mist"),
    MIND(ItemID.MINDRUNE, "Mind"),
    BODY(ItemID.BODYRUNE, "Body"),
    COSMIC(ItemID.COSMICRUNE, "Cosmic"),
    CHAOS(ItemID.CHAOSRUNE, "Chaos"),
    NATURE(ItemID.NATURERUNE, "Nature"),
    LAW(ItemID.LAWRUNE, "Law"),
    DEATH(ItemID.DEATHRUNE, "Death"),
    ASTRAL(ItemID.ASTRALRUNE, "Astral"),
    BLOOD(ItemID.BLOODRUNE, "Blood"),
    SOUL(ItemID.SOULRUNE, "Soul"),
    WRATH(ItemID.WRATHRUNE, "Wrath");

    private final int runeId;
    private final String[] runeNames;

    Rune(int runeId, String... runeNames)
    {
        this.runeId = runeId;
        this.runeNames = runeNames;
    }

    public String[] getRuneNames()
    {
        return runeNames;
    }

    public int getRuneId()
    {
        return runeId;
    }

    public int getQuantity()
    {
        if (hasUnlimitedRuneSource())
        {
            return Integer.MAX_VALUE;
        }

        var pouch = getCountInRunePouch();
        var invent = getCountInInventory();
        return pouch + invent;
    }

    private boolean hasUnlimitedRuneSource()
    {
        var wep = EquipmentAPI.fromSlot(EquipmentSlot.WEAPON);
        if(wep != null && Staff.isUnlimitedRuneSource(wep.getId(), this)){
            return true;
        }

        var shield = EquipmentAPI.fromSlot(EquipmentSlot.SHIELD);
        return shield != null && Tome.isUnlimitedRuneSource(shield.getId(), this);

        // TODO: or in fountain of rune area
    }

    private int getCountInRunePouch(){
        var pouch = RunePouch.getRunePouch();
        return pouch != null ? pouch.getQuantityOfRune(this)
                : 0;
    }

    private int getCountInInventory(){
        ItemEx item = InventoryAPI.getItem(runeId);
        return item != null ? item.getQuantity() : 0;
    }
}