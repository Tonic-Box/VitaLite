package com.tonic.data.slayerrewards;

import com.tonic.Static;
import com.tonic.api.game.GameAPI;
import com.tonic.api.widgets.WidgetAPI;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

@RequiredArgsConstructor
@Getter
public enum RewardsTab {
    UNLOCK(0),
    EXTEND(2),
    BUY(4),
    TASKS(5)
    ;

    private final int index;

    public boolean isOpen()
    {
        Widget w = WidgetAPI.get(InterfaceID.SlayerRewards.TABS, index + 1);
        return Static.invoke(() -> Integer.toString(w.getTextColor(), 16).equals("ff981f"));
    }

    public void open()
    {
        if(!isOpen())
        {
            GameAPI.invokeMenuAction(
                    1,
                    MenuAction.CC_OP.getId(),
                    index,
                    InterfaceID.SlayerRewards.TABS,
                    -1
            );
        }
    }
}
