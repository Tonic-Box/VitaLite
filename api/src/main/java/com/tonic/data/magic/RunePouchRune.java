package com.tonic.data.magic;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;

import java.util.Map;

import static net.runelite.api.gameval.ItemID.*;

public enum RunePouchRune
{
    AIR(1, AIRRUNE),
    WATER(2, WATERRUNE),
    EARTH(3, EARTHRUNE),
    FIRE(4, FIRERUNE),
    MIND(5, MINDRUNE),
    CHAOS(6, CHAOSRUNE),
    DEATH(7, DEATHRUNE),
    BLOOD(8, BLOODRUNE),
    COSMIC(9, COSMICRUNE),
    NATURE(10, NATURERUNE),
    LAW(11, LAWRUNE),
    BODY(12, BODYRUNE),
    SOUL(13, SOULRUNE),
    ASTRAL(14, ASTRALRUNE),
    MIST(15, MISTRUNE),
    MUD(16, MUDRUNE),
    DUST(17, DUSTRUNE),
    LAVA(18, LAVARUNE),
    STEAM(19, STEAMRUNE),
    SMOKE(20, SMOKERUNE),
    WRATH(21, WRATHRUNE),
    SUNFIRE(22, SUNFIRERUNE),
    AETHER(23, AETHERRUNE);

    @Getter
    private final int id;
    @Getter
    private final int itemId;

    private static final Map<Integer, RunePouchRune> runes;

    static
    {
        ImmutableMap.Builder<Integer, RunePouchRune> builder = new ImmutableMap.Builder<>();
        for (RunePouchRune rune : values())
        {
            builder.put(rune.getId(), rune);
        }
        runes = builder.build();
    }

    RunePouchRune(int id, int itemId)
    {
        this.id = id;
        this.itemId = itemId;
    }

    public static RunePouchRune getRune(int varbit)
    {
        return runes.get(varbit);
    }

    public String getName()
    {
        String name = this.name();
        name = name.substring(0, 1) + name.substring(1).toLowerCase();
        return name;
    }
}