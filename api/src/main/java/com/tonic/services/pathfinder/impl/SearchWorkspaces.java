package com.tonic.services.pathfinder.impl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The workspaces searches borrow, shared by every thread without locking: at most MOST_IDLE are kept between searches, and none that grew past what a long walk needs, so a rare whole-world search does not hold its memory afterwards.
 */
final class SearchWorkspaces
{
    static final int MOST_IDLE = 2;
    static final int MOST_RETAINED_TILES = 2_000_000;
    static final int MOST_RETAINED_QUEUE = 1 << 20;

    private static final ConcurrentLinkedQueue<SearchWorkspace> IDLE = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger IDLE_COUNT = new AtomicInteger();

    private SearchWorkspaces()
    {
    }

    /**
     * @return an idle workspace, emptied, or a new one when none is idle
     */
    static SearchWorkspace borrow()
    {
        SearchWorkspace workspace = IDLE.poll();
        if (workspace == null)
        {
            return new SearchWorkspace();
        }
        IDLE_COUNT.decrementAndGet();
        return workspace;
    }

    /**
     * Hands a workspace back once its search is done: emptied and kept when there is room and it did not grow too large, else left for the collector.
     *
     * @param workspace the workspace
     */
    static void release(SearchWorkspace workspace)
    {
        boolean retainable = workspace.visited.size() <= MOST_RETAINED_TILES && workspace.queue.capacity() <= MOST_RETAINED_QUEUE;
        if (!retainable || IDLE_COUNT.incrementAndGet() > MOST_IDLE)
        {
            if (retainable)
            {
                IDLE_COUNT.decrementAndGet();
            }
            return;
        }
        workspace.reset();
        IDLE.offer(workspace);
    }
}
