package com.tonic.data.magic;

import com.tonic.Static;
import com.tonic.api.TClient;
import com.tonic.api.game.VarAPI;
import com.tonic.api.threaded.Delays;
import com.tonic.api.widgets.BankAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.InventoryAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.util.ThreadPool;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

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

    private static final int POUCH_CHILD_ID = 4;
    private static final int POUCH_SLOT_COUNT = 4;
    private static final int INVENTORY_CHILD_ID = 7;
    private static final int INVENTORY_SLOT_COUNT = 28;
    private static final int BANKSIDE_RUNE_POUCH_CHILD_ID = 3;
    private static final int BANKSIDE_RUNE_POUCH_CHILD_INDEX = 0;
    private static final int CONFIGURE_MODE_CHILD_ID = 20;

    RunePouch(int pouchId) {
        this.pouchId = pouchId;
        this.has4Slots = false;
    }

    RunePouch(int pouchId, boolean has4Slots) {
        this.pouchId = pouchId;
        this.has4Slots = has4Slots;
    }

    public void open(){
        InventoryAPI.interact(this.pouchId, "Open");
    }

    public void storeOneInPouch(Rune rune){
        storeOneInPouch(rune.getRuneId());
    }

    public void storeOneInPouch(int runeItemId){
        storeInPouch(runeItemId, "Store-1");
    }

    public void storeFiveInPouch(Rune rune){
        storeFiveInPouch(rune.getRuneId());
    }

    public void storeFiveInPouch(int runeItemId){
        storeInPouch(runeItemId, "Store-5");
    }

    public void storeXInPouch(Rune rune, int amount){
        storeXInPouch(rune.getRuneId(), amount);
    }

    public void storeXInPouch(int runeItemId, int amount){
        if(amount < 0){
            return;
        }

        for(int slot = 0; slot < INVENTORY_SLOT_COUNT; slot++){
            Widget widget = WidgetAPI.get(InterfaceID.RUNE_POUCH, INVENTORY_CHILD_ID, slot);
            if(widget == null || widget.getItemId() != runeItemId || widget.getItemQuantity() <= 0)
                continue;

            WidgetAPI.interact(widget, "Store-X");
            DialogueAPI.resumeNumericDialogue(amount);
            return;
        }
    }

    public void storeAllInPouch(Rune rune){
        storeAllInPouch(rune.getRuneId());
    }

    public void storeAllInPouch(int runeItemId){
        storeInPouch(runeItemId, "Store-All");
    }

    public void takeOneFromPouch(Rune rune){
        takeOneFromPouch(rune.getRuneId());
    }

    public void takeOneFromPouch(int runeItemId){
        takeFromPouch(runeItemId, "Withdraw-1");
    }

    public void takeFiveFromPouch(Rune rune){
        takeFiveFromPouch(rune.getRuneId());
    }

    public void takeFiveFromPouch(int runeItemId){
        takeFromPouch(runeItemId, "Withdraw-5");
    }

    public void takeXFromPouch(Rune rune, int amount){
        takeXFromPouch(rune.getRuneId(), amount);
    }

    public void takeXFromPouch(int runeItemId, int amount){
        if(amount < 0){
            return;
        }

        for(int slot = 0; slot < POUCH_SLOT_COUNT; slot++){
            Widget widget = WidgetAPI.get(InterfaceID.RUNE_POUCH, POUCH_CHILD_ID, slot);
            if(widget == null || widget.getItemId() != runeItemId || widget.getItemQuantity() <= 0)
                continue;

            WidgetAPI.interact(widget, "Withdraw-X");
            DialogueAPI.resumeNumericDialogue(amount);
            return;
        }
    }

    public void takeAllFromPouch(Rune rune){
        takeAllFromPouch(rune.getRuneId());
    }

    public void takeAllFromPouch(int runeItemId){
        takeFromPouch(runeItemId, "Withdraw-All");
    }

    public void emptyIntoBank(){
        if(!BankAPI.isOpen()){
            return;
        }

        Widget widget = WidgetAPI.get(InterfaceID.BANKSIDE, BANKSIDE_RUNE_POUCH_CHILD_ID, BANKSIDE_RUNE_POUCH_CHILD_INDEX);
        if(widget == null || widget.getItemId() != this.pouchId){
            return;
        }

        WidgetAPI.interact(widget, "Empty");
    }

    public void withdrawOneFromBank(Rune rune){
        withdrawOneFromBank(rune.getRuneId());
    }

    public void withdrawOneFromBank(int runeItemId){
        withdrawFromBank(runeItemId, 1);
    }

    public void withdrawFiveFromBank(Rune rune){
        withdrawFiveFromBank(rune.getRuneId());
    }

    public void withdrawFiveFromBank(int runeItemId){
        withdrawFromBank(runeItemId, 5);
    }

    public void withdrawTenFromBank(Rune rune){
        withdrawTenFromBank(rune.getRuneId());
    }

    public void withdrawTenFromBank(int runeItemId){
        withdrawFromBank(runeItemId, 10);
    }

    public void withdrawXFromBank(Rune rune, int amount){
        withdrawXFromBank(rune.getRuneId(), amount);
    }

    public void withdrawXFromBank(int runeItemId, int amount){
        if(amount < 0){
            return;
        }

        withdrawFromBank(runeItemId, amount);
    }

    public void withdrawAllFromBank(Rune rune){
        withdrawAllFromBank(rune.getRuneId());
    }

    public void withdrawAllFromBank(int runeItemId){
        withdrawFromBank(runeItemId, -1);
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

    private static void storeInPouch(int runeItemId, String action){
        for(int slot = 0; slot < INVENTORY_SLOT_COUNT; slot++){
            Widget widget = WidgetAPI.get(InterfaceID.RUNE_POUCH, INVENTORY_CHILD_ID, slot);
            if(widget == null || widget.getItemId() != runeItemId || widget.getItemQuantity() <= 0)
                continue;

            WidgetAPI.interact(widget, action);
            return;
        }
    }

    private static void takeFromPouch(int runeItemId, String action){
        for(int slot = 0; slot < POUCH_SLOT_COUNT; slot++){
            Widget widget = WidgetAPI.get(InterfaceID.RUNE_POUCH, POUCH_CHILD_ID, slot);
            if(widget == null || widget.getItemId() != runeItemId || widget.getItemQuantity() <= 0)
                continue;

            WidgetAPI.interact(widget, action);
            return;
        }
    }

    private void withdrawFromBank(int runeItemId, int amount){
        TClient client = Static.getClient();
        if(client != null && client.isClientThread()){
            ThreadPool.submit(() -> this.withdrawFromBank(runeItemId, amount));
            return;
        }

        if(!switchToConfigureMode()){
            return;
        }

        BankAPI.withdraw(runeItemId, amount, false);
    }

    private boolean switchToConfigureMode(){
        if(!BankAPI.isOpen()){
            return false;
        }

        if(isInConfigureMode()){
            return true;
        }

        Widget widget = WidgetAPI.get(InterfaceID.BANKSIDE, BANKSIDE_RUNE_POUCH_CHILD_ID, BANKSIDE_RUNE_POUCH_CHILD_INDEX);
        if(widget == null || widget.getItemId() != this.pouchId){
            return false;
        }

        WidgetAPI.interact(widget, "Configure");

        TClient client = Static.getClient();
        if(client != null && client.isClientThread()){
            return false;
        }

        if(!Delays.waitUntil(RunePouch::isInConfigureMode, 2000)){
            return false;
        }

        return true;
    }

    private static boolean isInConfigureMode(){
        return WidgetAPI.isVisible(InterfaceID.BANKSIDE, CONFIGURE_MODE_CHILD_ID);
    }
}
