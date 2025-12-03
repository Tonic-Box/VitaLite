package com.tonic.data.magic;

import com.tonic.Static;
import com.tonic.api.game.VarAPI;
import com.tonic.data.magic.spellbooks.Ancient;
import com.tonic.data.magic.spellbooks.Standard;
import net.runelite.api.gameval.VarbitID;

import java.util.*;

public enum SpellBook
{
    STANDARD(0),
    ANCIENT(1),
    LUNAR(2),
    NECROMANCY(3);

    public static final Map<SpellBook,Set<Spell>> OFFENSIVE_SPELLS = new HashMap<>();

    private final int varbitValue;

    SpellBook(int varbitValue)
    {
        this.varbitValue = varbitValue;
    }

    public static SpellBook getCurrent()
    {
        return Static.invoke(() ->
                Arrays.stream(values())
                        .filter(x -> VarAPI.getVar(VarbitID.SPELLBOOK) == x.varbitValue)
                        .findFirst()
                        .orElse(null)
        );
    }

    public static Set<Spell> getCurrentOffensiveSpells()
    {
        return OFFENSIVE_SPELLS.getOrDefault(getCurrent(), Collections.emptySet());
    }

    static
    {

        Set<Spell> standard = new HashSet<>();
        standard.add(Standard.WIND_STRIKE);
        standard.add(Standard.WATER_STRIKE);
        standard.add(Standard.EARTH_STRIKE);
        standard.add(Standard.FIRE_STRIKE);
        standard.add(Standard.WIND_BOLT);
        standard.add(Standard.WATER_BOLT);
        standard.add(Standard.EARTH_BOLT);
        standard.add(Standard.FIRE_BOLT);
        standard.add(Standard.WIND_BLAST);
        standard.add(Standard.WATER_BLAST);
        standard.add(Standard.EARTH_BLAST);
        standard.add(Standard.FIRE_BLAST);
        standard.add(Standard.WIND_WAVE);
        standard.add(Standard.WATER_WAVE);
        standard.add(Standard.EARTH_WAVE);
        standard.add(Standard.FIRE_WAVE);
        OFFENSIVE_SPELLS.put(STANDARD, standard);

        Set<Spell> ancient = new HashSet<>();
        ancient.add(Ancient.SMOKE_RUSH);
        ancient.add(Ancient.SHADOW_RUSH);
        ancient.add(Ancient.BLOOD_RUSH);
        ancient.add(Ancient.ICE_RUSH);
        ancient.add(Ancient.SMOKE_BURST);
        ancient.add(Ancient.SHADOW_BURST);
        ancient.add(Ancient.BLOOD_BURST);
        ancient.add(Ancient.ICE_BURST);
        ancient.add(Ancient.SMOKE_BLITZ);
        ancient.add(Ancient.SHADOW_BLITZ);
        ancient.add(Ancient.BLOOD_BLITZ);
        ancient.add(Ancient.ICE_BLITZ);
        ancient.add(Ancient.SMOKE_BARRAGE);
        ancient.add(Ancient.SHADOW_BARRAGE);
        ancient.add(Ancient.BLOOD_BARRAGE);
        ancient.add(Ancient.ICE_BARRAGE);
        OFFENSIVE_SPELLS.put(ANCIENT, ancient);
    }
}
