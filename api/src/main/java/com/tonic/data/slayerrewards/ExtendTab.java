package com.tonic.data.slayerrewards;

import com.tonic.api.widgets.SlayerRewardsAPI;
import lombok.RequiredArgsConstructor;

public class ExtendTab extends AbstractTabImpl
{
    @Override
    public RewardsTab getTab() {
        return RewardsTab.EXTEND;
    }

    @RequiredArgsConstructor
    public enum Extends
    {
        NEED_MORE_DARKNESS(4, 100),
        ANKOU_VERY_MUCH(8, 100),
        SUQ_A_NOTHER_ONE(9, 100),
        FIRE_AND_DARKNESS(10, 50),
        PEDAL_TO_THE_METALS(11, 200),
        SPIRITUAL_FERVOUR(12, 100),
        AUGMENT_MY_ABBIES(13, 100),
        ITS_DARK_IN_HERE(14, 100),
        GREATER_CHALLENGE(15, 100),
        BLEED_ME_DRY(20, 75),
        SMELL_YA_LATER(21, 100),
        BIRDS_OF_A_FEATHER(22, 100),
        HORRORIFIC(24, 100),
        TO_DUST_YOU_SHALL_RETURN(25, 100),
        WYVER_NOTHER_ONE(26, 100),
        GET_SMASHED(27, 100),
        NECHS_PLEASE(28, 100),
        KRACK_ON(29, 100),
        GET_SCABARIGHT_ON_IT(36, 50),
        WYVER_NOTHER_TWO(39, 100),
        BASILONGER(46, 100),
        MORE_AT_STAKE(49, 100),
        REVENENENENENANTS(53, 100),
        MORE_EYES_THAN_SENSE(55, 150),
        UN_RESTRAINING_ORDER(57, 100),
        CAN_OF_WYRMS(58, 100),
        LETS_STAY_ALL_AQUANITE(59, 100),
        GRYPHON_AND_ON(63, 50),
        I_SEE_DRAGONS(65, 100)
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
