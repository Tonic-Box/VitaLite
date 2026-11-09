package com.tonic.data;

import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

@Getter
public enum Tab {
    COMBAT_TAB(VarbitID.STONE_COMBAT_KEY, 0),
    EXP_TAB(VarbitID.STONE_STATS_KEY, 1),
    QUESTS_TAB(VarbitID.STONE_JOURNAL_KEY, 2),
    INVENTORY_TAB(VarbitID.STONE_INV_KEY, 3),
    EQUIPMENT_TAB(VarbitID.STONE_WORN_KEY, 4),
    PRAYER_TAB(VarbitID.STONE_PRAYER_KEY, 5),
    SPELLBOOK_TAB(VarbitID.STONE_MAGIC_KEY, 6),
    CLAN_TAB(VarbitID.STONE_CLANCHAT_KEY, 7),
    FRIENDS_TAB(VarbitID.STONE_FRIENDS_KEY, 9),
    SETTINGS_TAB(VarbitID.STONE_OPTIONS1_KEY, 11),
    EMOTES_TAB(VarbitID.STONE_OPTIONS2_KEY, 12),
    MUSIC_TAB(VarbitID.STONE_MUSIC_KEY, 13),
    LOGOUT_TAB(VarbitID.STONE_LOGOUT_KEY, 10),
    ACCOUNT_TAB(VarbitID.STONE_ACCOUNT_KEY, 8);

    private final int hotkeyVarbit;
    private final int tabVarbit;

    Tab(int hotkeyVarbit, int tabVarbit)
    {
        this.hotkeyVarbit = hotkeyVarbit;
        this.tabVarbit = tabVarbit;
    }
}