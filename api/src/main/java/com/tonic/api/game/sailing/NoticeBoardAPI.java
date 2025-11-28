package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.game.SkillAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.locatables.sailing.CourierTaskData;
import com.tonic.data.locatables.sailing.NoticeBoardPosting;
import com.tonic.util.TextUtil;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * API for interacting with the Sailing Notice Board.
 */
public class NoticeBoardAPI
{
    /**
     * Gets a list of all available tasks on the Notice Board that the player meets the level requirements for.
     *
     * @return A list of available NoticeBoardPosting tasks.
     */
    public static List<NoticeBoardPosting> getAvailableTasks()
    {
        return Static.invoke(() -> {
            List<NoticeBoardPosting> availableTasks = new ArrayList<>();
            if(!WidgetAPI.isVisible(InterfaceID.PortTaskBoard.CONTAINER))
            {
                return null;
            }
            for(NoticeBoardPosting posting : NoticeBoardPosting.values())
            {
                if(posting.getTaskData() != null && posting.hasLevelFor() && !posting.getTaskData().isActive())
                {
                    availableTasks.add(posting);
                }
            }
            return availableTasks;
        });
    }

    /**
     * Gets a list of all available tasks on the Notice Board that the player meets the level requirements for.
     *
     * @return A list of available NoticeBoardPosting tasks.
     */
    public static List<NoticeBoardPosting> getAcceptedTasks()
    {
        return Static.invoke(() -> {
            List<NoticeBoardPosting> availableTasks = new ArrayList<>();
            if(!WidgetAPI.isVisible(InterfaceID.PortTaskBoard.CONTAINER))
            {
                return null;
            }
            for(NoticeBoardPosting posting : NoticeBoardPosting.values())
            {
                if(posting.getTaskData() != null && posting.getTaskData().isActive())
                {
                    availableTasks.add(posting);
                }
            }
            return availableTasks;
        });
    }

    /**
     * Gets a list of all tasks on the Notice Board, regardless of level requirements or active status.
     *
     * @return A list of all NoticeBoardPosting tasks.
     */
    public static List<NoticeBoardPosting> getAllTasks()
    {
        return Static.invoke(() -> {
            List<NoticeBoardPosting> availableTasks = new ArrayList<>();
            if(!WidgetAPI.isVisible(InterfaceID.PortTaskBoard.CONTAINER))
            {
                return null;
            }
            for(NoticeBoardPosting posting : NoticeBoardPosting.values())
            {
                if(posting.getTaskData() != null)
                {
                    availableTasks.add(posting);
                }
            }
            return availableTasks;
        });
    }
}
