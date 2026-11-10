package com.tonic.services.profiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe circular buffer for storing fixed number of metric snapshots
 * Automatically overwrites oldest data when capacity is reached
 */
public class CircularBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private int head = 0;
    private int size = 0;
    private final Object lock = new Object();

    public CircularBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * Add item to buffer, overwriting oldest if full
     */
    public void add(T item) {
        synchronized (lock) {
            buffer[head] = item;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        }
    }

    /**
     * Get all items in chronological order (oldest to newest)
     */
    @SuppressWarnings("unchecked")
    public List<T> getAll() {
        synchronized (lock) {
            List<T> result = new ArrayList<>(size);
            if (size == 0) {
                return result;
            }

            int start = (size < capacity) ? 0 : head;
            for (int i = 0; i < size; i++) {
                int index = (start + i) % capacity;
                result.add((T) buffer[index]);
            }
            return result;
        }
    }

    /**
     * Get current number of items in buffer
     */
    public int size() {
        synchronized (lock) {
            return size;
        }
    }

    /**
     * Get maximum capacity of buffer
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Check if buffer is empty
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return size == 0;
        }
    }

    /**
     * Check if buffer is at full capacity
     */
    public boolean isFull() {
        synchronized (lock) {
            return size == capacity;
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
            head = 0;
            size = 0;
        }
    }
}
