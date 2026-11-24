package com.tonic.services.pathfinder.collections;

import java.util.Arrays;

/**
 * The search's queue: a growable ring of tiles taken in the order they were queued, where each level is one step further from the start, and a lane of tiles held until the search is a given number of levels deep, joining at the end of that level in the order they were held.
 */
public final class HybridIntQueue
{
    private static final int FIRST_HELD = 64;

    private int[] data;
    private int capacityMask;
    private int head;
    private int tail;

    private int level;
    private int levelEnd = -1;

    private int[] heldValues = new int[FIRST_HELD];
    private long[] heldKeys = new long[FIRST_HELD];
    private int heldCount;
    private int heldSequence;

    /**
     * @param capacity how many tiles the ring holds before it first grows
     * @throws IllegalArgumentException if capacity is not positive
     */
    public HybridIntQueue(int capacity)
    {
        int adjusted = nextPowerOfTwo(capacity);
        data = new int[adjusted];
        capacityMask = adjusted - 1;
    }

    /**
     * @return how many tiles the ring holds now, held ones not counted
     */
    public int size()
    {
        return (tail - head) & capacityMask;
    }

    /**
     * @return how many tiles the ring can hold before it next grows
     */
    public int capacity()
    {
        return data.length - 1;
    }

    /**
     * Queues a tile behind the ones already queued, growing the ring when it is full.
     *
     * @param value the tile
     */
    public void enqueue(int value)
    {
        if (((tail + 1) & capacityMask) == head)
        {
            grow();
        }
        data[tail] = value;
        tail = (tail + 1) & capacityMask;
    }

    /**
     * Empties the queue and its held tiles and starts again from level nought, keeping the ring's size.
     */
    public void reset()
    {
        head = 0;
        tail = 0;
        level = 0;
        levelEnd = -1;
        heldCount = 0;
        heldSequence = 0;
    }

    private void grow()
    {
        int size = size();
        int[] grown = new int[data.length << 1];
        for (int i = 0; i < size; i++)
        {
            grown[i] = data[(head + i) & capacityMask];
        }
        if (levelEnd >= 0)
        {
            levelEnd = (levelEnd - head) & capacityMask;
        }
        data = grown;
        capacityMask = grown.length - 1;
        head = 0;
        tail = size;
    }

    /**
     * Holds a tile until the search is a number of levels deep, then queues it at the end of that level.
     *
     * @param value the tile
     * @param at the level it joins at, counted from the start
     */
    public void enqueueAtLevel(int value, int at)
    {
        if (heldCount == heldValues.length)
        {
            heldValues = Arrays.copyOf(heldValues, heldCount << 1);
            heldKeys = Arrays.copyOf(heldKeys, heldCount << 1);
        }
        heldValues[heldCount] = value;
        heldKeys[heldCount] = (long) at << Integer.SIZE | (heldSequence++ & 0xFFFFFFFFL);
        siftUp(heldCount);
        heldCount++;
    }

    /**
     * @return how many levels deep the search is: the level of the tile last taken out
     */
    public int level()
    {
        return level;
    }

    /**
     * Takes out the next tile, starting a new level once the current one is used up and letting in the held tiles
     * that wait for it.
     *
     * @return the tile, or -1 when nothing is queued or held
     */
    public int dequeue()
    {
        if (levelEnd < 0 || head == levelEnd)
        {
            if (levelEnd >= 0)
            {
                level++;
            }
            if (head == tail && heldCount > 0)
            {
                level = Math.max(level, heldLevel(0));
            }
            releaseUpTo(level);
            levelEnd = tail;
        }
        if (head == tail)
        {
            return -1;
        }
        int value = data[head];
        head = (head + 1) & capacityMask;
        return value;
    }

    /**
     * @return whether nothing is queued or held
     */
    public boolean isEmpty()
    {
        return head == tail && heldCount == 0;
    }

    private void releaseUpTo(int reached)
    {
        while (heldCount > 0 && heldLevel(0) <= reached)
        {
            enqueue(heldValues[0]);
            heldCount--;
            if (heldCount > 0)
            {
                heldValues[0] = heldValues[heldCount];
                heldKeys[0] = heldKeys[heldCount];
                siftDown(0);
            }
        }
    }

    private int heldLevel(int index)
    {
        return (int) (heldKeys[index] >> Integer.SIZE);
    }

    private void siftUp(int index)
    {
        while (index > 0)
        {
            int parent = (index - 1) >>> 1;
            if (heldKeys[index] >= heldKeys[parent])
            {
                return;
            }
            swap(index, parent);
            index = parent;
        }
    }

    private void siftDown(int index)
    {
        int child;
        while ((child = (index << 1) + 1) < heldCount)
        {
            if (child + 1 < heldCount && heldKeys[child + 1] < heldKeys[child])
            {
                child++;
            }
            if (heldKeys[index] <= heldKeys[child])
            {
                return;
            }
            swap(index, child);
            index = child;
        }
    }

    private void swap(int a, int b)
    {
        int value = heldValues[a];
        heldValues[a] = heldValues[b];
        heldValues[b] = value;
        long key = heldKeys[a];
        heldKeys[a] = heldKeys[b];
        heldKeys[b] = key;
    }

    private static int nextPowerOfTwo(int n)
    {
        if (n <= 0)
        {
            throw new IllegalArgumentException("capacity must be positive");
        }
        return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
    }
}
