package com.tonic.services.profiler.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Thread-safe ring buffer for memory-bounded sample storage
 * Automatically overwrites oldest entries when capacity is reached
 *
 * @param <T> Type of samples to store
 */
public class RingBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private int writeIndex = 0;
    private long totalWritten = 0;
    private long droppedCount = 0;
    private final Object lock = new Object();

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * Add item to buffer, overwriting oldest if full
     */
    public void add(T item) {
        synchronized (lock) {
            if (totalWritten >= capacity) {
                droppedCount++;
            }

            buffer[writeIndex] = item;
            writeIndex = (writeIndex + 1) % capacity;
            totalWritten++;
        }
    }

    /**
     * Get all items in chronological order (oldest to newest)
     */
    @SuppressWarnings("unchecked")
    public List<T> getAll() {
        synchronized (lock) {
            int count = (int) Math.min(totalWritten, capacity);
            List<T> result = new ArrayList<>(count);

            if (count == 0) {
                return result;
            }

            int start = (totalWritten >= capacity) ? writeIndex : 0;
            for (int i = 0; i < count; i++) {
                int index = (start + i) % capacity;
                result.add((T) buffer[index]);
            }

            return result;
        }
    }

    /**
     * Iterate over all items without creating a list (memory efficient)
     */
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<T> consumer) {
        synchronized (lock) {
            int count = (int) Math.min(totalWritten, capacity);
            if (count == 0) return;

            int start = (totalWritten >= capacity) ? writeIndex : 0;
            for (int i = 0; i < count; i++) {
                int index = (start + i) % capacity;
                consumer.accept((T) buffer[index]);
            }
        }
    }

    /**
     * Get the N most recent items in chronological order
     */
    @SuppressWarnings("unchecked")
    public List<T> getRecent(int count) {
        synchronized (lock) {
            if (count <= 0) {
                return new ArrayList<>();
            }

            int actualCount = (int) Math.min(count, Math.min(totalWritten, capacity));
            List<T> result = new ArrayList<>(actualCount);

            if (actualCount == 0) {
                return result;
            }

            int storageSize = (int) Math.min(totalWritten, capacity);
            int start = (totalWritten >= capacity) ? writeIndex : 0;
            int offset = storageSize - actualCount;

            for (int i = 0; i < actualCount; i++) {
                int index = (start + offset + i) % capacity;
                result.add((T) buffer[index]);
            }

            return result;
        }
    }

    /**
     * Get the most recent item, or null if empty
     */
    @SuppressWarnings("unchecked")
    public T getLatest() {
        synchronized (lock) {
            if (totalWritten == 0) {
                return null;
            }
            int lastIndex = (writeIndex - 1 + capacity) % capacity;
            return (T) buffer[lastIndex];
        }
    }

    /**
     * Get current number of items in buffer
     */
    public int size() {
        synchronized (lock) {
            return (int) Math.min(totalWritten, capacity);
        }
    }

    /**
     * Get maximum capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Get total number of items ever written
     */
    public long getTotalWritten() {
        synchronized (lock) {
            return totalWritten;
        }
    }

    /**
     * Get number of dropped items (oldest items that were overwritten)
     */
    public long getDroppedCount() {
        synchronized (lock) {
            return droppedCount;
        }
    }

    /**
     * Check if buffer is empty
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return totalWritten == 0;
        }
    }

    /**
     * Check if buffer is at full capacity
     */
    public boolean isFull() {
        synchronized (lock) {
            return totalWritten >= capacity;
        }
    }

    /**
     * Clear all items from buffer
     */
    public void clear() {
        synchronized (lock) {
            for (int i = 0; i < capacity; i++) {
                buffer[i] = null;
            }
            writeIndex = 0;
            totalWritten = 0;
            droppedCount = 0;
        }
    }

    /**
     * Get buffer statistics
     */
    public Stats getStats() {
        synchronized (lock) {
            return new Stats(
                capacity,
                (int) Math.min(totalWritten, capacity),
                totalWritten,
                droppedCount,
                isFull()
            );
        }
    }

    /**
     * Buffer statistics
     */
    public static class Stats {
        public final int capacity;
        public final int currentSize;
        public final long totalWritten;
        public final long droppedCount;
        public final boolean isFull;

        public Stats(int capacity, int currentSize, long totalWritten, long droppedCount, boolean isFull) {
            this.capacity = capacity;
            this.currentSize = currentSize;
            this.totalWritten = totalWritten;
            this.droppedCount = droppedCount;
            this.isFull = isFull;
        }

        @Override
        public String toString() {
            return String.format("RingBuffer[size=%d/%d, written=%d, dropped=%d]",
                currentSize, capacity, totalWritten, droppedCount);
        }
    }
}
