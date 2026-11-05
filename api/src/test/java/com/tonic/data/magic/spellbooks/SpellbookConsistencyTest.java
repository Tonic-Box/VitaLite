package com.tonic.data.magic.spellbooks;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellbookConsistencyTest
{
    @Test
    void eachSpellComponentHasOneIndexAndEachIndexOneComponent()
    {
        // The enchant spells share the Jewellery Enchant entry of the standard spellbook enum.
        check(Standard.values(), SpellbookEnums.StandardSpell.JEWELLERY_ENCHANT.index);
        check(Ancient.values(), -1);
        check(Lunar.values(), -1);
        check(Necromancy.values(), -1);
    }

    private static void check(Enum<?>[] spells, int sharedIndex)
    {
        Map<Integer, Enum<?>> byComponent = new HashMap<>();
        Map<Integer, Enum<?>> byIndex = new HashMap<>();
        for (Enum<?> spell : spells)
        {
            int component = field(spell, "interfaceId");
            int index = field(spell, "spellIndex");
            Enum<?> sameComponent = byComponent.putIfAbsent(component, spell);
            if (sameComponent != null)
            {
                assertEquals(field(sameComponent, "spellIndex"), index,
                        spell + " and " + sameComponent + " use the same component with different indices");
            }
            if (index == sharedIndex)
            {
                continue;
            }
            Enum<?> sameIndex = byIndex.putIfAbsent(index, spell);
            if (sameIndex != null)
            {
                assertEquals(field(sameIndex, "interfaceId"), component,
                        spell + " and " + sameIndex + " use index " + index + " for different components");
            }
        }
    }

    private static int field(Enum<?> spell, String name)
    {
        try
        {
            Field field = spell.getDeclaringClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(spell);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError(e);
        }
    }
}
