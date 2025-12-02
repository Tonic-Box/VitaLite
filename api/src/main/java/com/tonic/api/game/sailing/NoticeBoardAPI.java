package com.tonic.api.game.sailing;

import com.tonic.Static;
import com.tonic.api.entities.TileObjectAPI;
import com.tonic.api.widgets.WidgetAPI;
import com.tonic.data.locatables.sailing.NoticeBoardPosting;
import com.tonic.data.wrappers.TileObjectEx;
import com.tonic.queries.abstractions.AbstractQuery;
import net.runelite.api.gameval.InterfaceID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * API for interacting with the Sailing Notice Board.
 */
public class NoticeBoardAPI
{
    public static NoticeBoardQuery query()
    {
        return new NoticeBoardQuery();
    }

    /**
     * Gets the best available task on the Notice Board that the player meets the level requirements for.
     *
     * @return The best available NoticeBoardPosting task, or null if none are available.
     */
    public static NoticeBoardPosting getBestTask()
    {
        return query()
                .hasLevelFor()
                .getHighestLevelTask();
    }
    /**
     * Gets a list of all available tasks on the Notice Board that the player meets the level requirements for.
     *
     * @return A list of available NoticeBoardPosting tasks.
     */
    public static List<NoticeBoardPosting> getAvailableTasks()
    {
        return query()
                .hasLevelFor()
                .withAccepted(false)
                .collect();
    }

    /**
     * Gets a list of all available tasks on the Notice Board that the player meets the level requirements for.
     *
     * @return A list of available NoticeBoardPosting tasks.
     */
    public static List<NoticeBoardPosting> getAcceptedTasks()
    {
        return query()
                .withAccepted(true)
                .collect();
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
                return availableTasks;
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

    public static boolean openNoticeBoard()
    {
        TileObjectEx board = TileObjectAPI.search()
                .withName("Notice board")
                .nearest();

        if(board == null)
        {
            return false;
        }

        board.interact("Inspect");
        return true;
    }

    public static boolean isOpen()
    {
        return WidgetAPI.isVisible(InterfaceID.PortTaskBoard.CONTAINER);
    }

    public static void closeNoticeBoard()
    {
        if(!isOpen())
            return;

        WidgetAPI.closeInterface();
    }

    public static class NoticeBoardQuery extends AbstractQuery<NoticeBoardPosting, NoticeBoardQuery>
    {
        public NoticeBoardQuery()
        {
            super(getAllTasks());
        }

        public NoticeBoardQuery withAccepted(boolean accepted)
        {
            return removeIf(posting -> posting.isAccepted() != accepted);
        }

        public NoticeBoardQuery withMinimumLevel(int level)
        {
            return removeIf(posting -> posting.getRequiredLevel() < level);
        }

        public NoticeBoardQuery withMaximumLevel(int level)
        {
            return removeIf(posting -> posting.getRequiredLevel() > level);
        }

        public NoticeBoardQuery hasLevelFor()
        {
            return removeIf(posting -> !posting.hasLevelFor());
        }

        public NoticeBoardQuery lacksLevelFor()
        {
            return removeIf(NoticeBoardPosting::hasLevelFor);
        }

        public NoticeBoardQuery fromCurrentPort()
        {
            return keepIf(NoticeBoardPosting::startsFromCurrentPort);
        }

        public NoticeBoardQuery notFromCurrentPort()
        {
            return removeIf(NoticeBoardPosting::startsFromCurrentPort);
        }

        public NoticeBoardQuery sortHighestLevelFirst()
        {
            return sort((a, b) -> Integer.compare(b.getRequiredLevel(), a.getRequiredLevel()));
        }

        public NoticeBoardQuery sortLowestLevelFirst()
        {
            return sort(Comparator.comparingInt(NoticeBoardPosting::getRequiredLevel));
        }

        public NoticeBoardPosting getHighestLevelTask()
        {
            return sortHighestLevelFirst().first();
        }

        public NoticeBoardPosting getLowestLevelTask()
        {
            return sortLowestLevelFirst().first();
        }
    }
}
