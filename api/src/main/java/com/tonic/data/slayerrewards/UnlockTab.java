package com.tonic.data.slayerrewards;

import com.tonic.api.widgets.SlayerRewardsAPI;
import lombok.RequiredArgsConstructor;

public class UnlockTab extends AbstractTabImpl
{
    @Override
    public RewardsTab getTab() {
        return RewardsTab.UNLOCK;
    }

    @RequiredArgsConstructor
    public enum Unlocks
    {
        GARGOYLE_SMASHER(0, 120),
        SLUG_SALTER(1, 10),
        REPTILE_FREEZER(2, 10),
        SHROOM_SPRAYER(3, 110),
        MALEVOLENT_MMASQUERADE(5, 400),
        RING_BLING(6150, 150),
        BROADER_FLETCHING(7, 300),
        SEEING_RED(34, 50),
        WATCH_THE_BRIDE(17, 80),
        HOT_STUFF(18, 100),
        LIKE_A_BOSS(19, 200),
        REPTILE_GOT_RIPPED(30, 75),
        KING_BLACK_BONNET(31, 1000),
        KALPHITE_KHAT(32, 1000),
        UNHOLY_HELMET(33, 1000),
        DARK_MANTLE(38, 1000),
        BIGGER_AND_BADDER(35, 50),
        DULY_NOTED(37, 200),
        UNDEAD_HEAD(42, 1000),
        STOP_THE_WYVERN(43, 500),
        DOUBLE_TROUBLE(44, 500),
        USE_MORE_HEAD(45, 1000),
        EYE_SEE_YOU(56, 1000),
        BASILOCKED(47, 80),
        TWISTED_VISION(48, 1000),
        ACTUAL_VAMPIRE_SLAYER(50, 80),
        TASK_STORAGE(51, 500),
        I_WILDY_MORE_SLAYER(52, 0),
        WARPED_REALITY(54, 60)
        ;
        private final int index;
        private final int cost;

        public boolean canBuy()
        {
            return cost <= SlayerRewardsAPI.getPoints();
        }

        public boolean buy()
        {
            if(canBuy())
            {
                confirm(index);
                return true;
            }
            return false;
        }
    }
}
