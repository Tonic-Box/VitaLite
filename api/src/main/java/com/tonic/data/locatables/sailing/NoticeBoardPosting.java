package com.tonic.data.locatables.sailing;

import com.tonic.Static;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.util.TextUtil;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

@RequiredArgsConstructor
public enum NoticeBoardPosting
{
    ONE(0),
    TWO(6),
    THREE(12),
    FOUR(18),
    FIVE(24),
    SIX(30),
    SEVEN(36),
    EIGHT(42)
    ;

    private final int startIndex;

    public boolean accept()
    {
        if(!WidgetAPI.isVisible(InterfaceID.PortTaskBoard.CONTAINER) || getTaskData().isActive())
        {
            return false;
        }
        WidgetAPI.interact(1, InterfaceID.PortTaskBoard.CONTAINER, startIndex, -1);
        DialogueAPI.resumeNumericDialogue(1);
        return true;
    }

    public boolean cancel()
    {
        if(!WidgetAPI.isVisible(InterfaceID.PortTaskBoard.CONTAINER) || !getTaskData().isActive())
        {
            return false;
        }
        WidgetAPI.interact(1, InterfaceID.PortTaskBoard.CONTAINER, startIndex, -1);
        DialogueAPI.resumeNumericDialogue(1);
        return true;
    }

    public String getTaskName()
    {
        return Static.invoke(() -> {
            Widget noticeBoard = WidgetAPI.get(InterfaceID.PortTaskBoard.CONTAINER);
            if(noticeBoard == null)
            {
                return null;
            }

            Widget nameWidget = noticeBoard.getChild(startIndex);
            if(nameWidget == null)
            {
                return null;
            }

            return TextUtil.sanitize(nameWidget.getName());
        });
    }

    public CourierTaskData getTaskData()
    {
        return CourierTaskData.fromName(getTaskName());
    }

    public boolean hasLevelFor()
    {
        return SkillAPI.getLevel(Skill.SAILING) >= getRequiredLevel();
    }

    public int getRequiredLevel()
    {
        return Static.invoke(() -> {
            Widget noticeBoard = WidgetAPI.get(InterfaceID.PortTaskBoard.CONTAINER);
            if(noticeBoard == null)
            {
                return -1;
            }

            Widget reqWidget = noticeBoard.getChild(startIndex + 2);
            if(reqWidget == null)
            {
                return -1;
            }
            return Integer.parseInt(reqWidget.getText());
        });
    }
}