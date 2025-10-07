package com.tonic.services.pathfinder.collections;

public class IntQueue {
    private int[] data;
    private int head;
    private int tail;
    private final int capacity;

    public IntQueue(final int capacity) {
        data = new int[capacity];
        head = 0;
        tail = 0;
        this.capacity = capacity;
    }

    public void enqueue(final int value) {
        data[tail++] = value;
    }

    public int dequeue() {
        return data[head++];
    }

    public boolean isEmpty() {
        return head == tail;
    }

    public void clear() {
        head = 0;
        tail = 0;
        data = new int[capacity];
    }
}