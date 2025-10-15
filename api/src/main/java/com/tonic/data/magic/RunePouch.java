package com.tonic.data.magic;

import com.tonic.api.game.VarAPI;
import com.tonic.api.widgets.InventoryAPI;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

import java.util.ArrayList;
import java.util.List;

public enum RunePouch {
    RUNE_POUCH(ItemID.BH_RUNE_POUCH),
    RUNE_POUCH_LMS(ItemID.BR_RUNE_REPLACEMENT), // ?
    RUNE_POUCH_L(ItemID.BH_RUNE_POUCH_TROUVER),
    DIVINE_RUNE_POUCH(ItemID.DIVINE_RUNE_POUCH, true),
    DIVINE_RUNE_POUCH_L(ItemID.DIVINE_RUNE_POUCH_TROUVER, true);

    private final int pouchId;
    private final boolean has4Slots;

    RunePouch(int pouchId) {
        this.pouchId = pouchId;
        this.has4Slots = false;
    }

    RunePouch(int pouchId, boolean has4Slots) {
        this.pouchId = pouchId;
        this.has4Slots = has4Slots;
    }

    public int getQuantityOfRune(Rune rune){
        var size = this.has4Slots ? 4 : 3;
        for(int i = 0; i < size; i++){
            var pRune = VarAPI.getVar(PouchRunes.get(i));
            // the slot in the pouch is empty
            if(pRune == 0) continue;

            // the pRune is not the itemId
            int runeItemId = RunePouchRune.getRune(pRune).getItemId();

            // TODO: support combination runes
            if(runeItemId != rune.getRuneId()) continue;
            return VarAPI.getVar(PouchAmounts.get(i));
        }

        return 0;
    }

    public static RunePouch getRunePouch(){
        var pouch = InventoryAPI.getItem(i -> RunePouch.AllPouches.contains(i.getId()));
        if(pouch == null) return null;

        for (var runePouch : RunePouch.values()) {
            if(runePouch.pouchId == pouch.getId()){
                return runePouch;
            }
        }

        return null;
    }

    private static final List<Integer> AllPouches = new ArrayList<>() {
        {
            add(ItemID.BH_RUNE_POUCH);
            add(ItemID.BR_RUNE_REPLACEMENT);
            add(ItemID.BH_RUNE_POUCH_TROUVER);
            add(ItemID.DIVINE_RUNE_POUCH);
            add(ItemID.DIVINE_RUNE_POUCH_TROUVER);
        }
    };

    private static final List<Integer> PouchRunes = new ArrayList<>(){
        {
            add(VarbitID.RUNE_POUCH_TYPE_1);
            add(VarbitID.RUNE_POUCH_TYPE_2);
            add(VarbitID.RUNE_POUCH_TYPE_3);
            add(VarbitID.RUNE_POUCH_TYPE_4);
        }
    };

    private static final List<Integer> PouchAmounts = new ArrayList<>(){
        {
            add(VarbitID.RUNE_POUCH_QUANTITY_1);
            add(VarbitID.RUNE_POUCH_QUANTITY_2);
            add(VarbitID.RUNE_POUCH_QUANTITY_3);
            add(VarbitID.RUNE_POUCH_QUANTITY_4);
        }
    };
}