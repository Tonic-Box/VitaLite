package com.tonic.data.magic;

import com.tonic.Static;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

@Getter
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

    public int getQuantity()
    {
        return Static.invoke(() -> MagicCast.runeCount(runeId));
    }
}