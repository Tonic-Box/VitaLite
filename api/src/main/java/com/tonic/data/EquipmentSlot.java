package com.tonic.data;

import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of equipment slots in the inventory.
 */
@Getter
public enum EquipmentSlot
{
    HEAD(0, InterfaceID.Wornitems.SLOT0),
    CAPE(1, InterfaceID.Wornitems.SLOT1),
    AMULET(2, InterfaceID.Wornitems.SLOT2),
    WEAPON(3, InterfaceID.Wornitems.SLOT3),
    BODY(4, InterfaceID.Wornitems.SLOT4),
    SHIELD(5, InterfaceID.Wornitems.SLOT5),
    LEGS(7, InterfaceID.Wornitems.SLOT7),
    GLOVES(9, InterfaceID.Wornitems.SLOT9),
    BOOTS(10, InterfaceID.Wornitems.SLOT10),
    RING(12, InterfaceID.Wornitems.SLOT12),
    AMMO(13, InterfaceID.Wornitems.SLOT13);

    private final int slotIdx;
    private final int widgetId;

    EquipmentSlot(int slotIdx, int widgetId)
    {
        this.slotIdx = slotIdx;
        this.widgetId = widgetId;
    }

    private static final Map<Integer, EquipmentSlot> map;

    static {
        map = new HashMap<>();
        for (EquipmentSlot v : EquipmentSlot.values()) {
            map.put(v.slotIdx, v);
        }
    }

    public static EquipmentSlot findBySlot(int i) {
        return map.get(i);
    }
}